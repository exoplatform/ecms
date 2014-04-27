/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Local mirror of cloud drive to JCR sub-tree. All nodes of this sub-tree contain metadata such as name,
 * author, dates and link to actual drive on the cloud provider. <br/>
 * <p>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDrive.java 00000 Sep 7, 2012 pnedonosko $
 */
public abstract class CloudDrive {

  protected static final Log LOG = ExoLogger.getLogger(CloudDrive.class);

  /**
   * Cloud Drive command and its result. <br>
   */
  public interface Command {

    /**
     * Percentage of command completion.
     */
    final int COMPLETE = 100;

    /**
     * Return command completion in percents.
     * 
     * @return integer, a percent of 100.
     */
    int getProgress();

    /**
     * Answer with {@code true} if command complete. Note that done status will be {@code true} only after all
     * registered listeners will be fired about the command completion.
     * 
     * @return boolean flag, {@code true} if command complete, {@code false} otherwise.
     */
    boolean isDone();

    /**
     * System time when process started to execute.
     * 
     * @return long, time in milliseconds
     */
    long getStartTime();

    /**
     * System time when process finished to execute.
     * 
     * @return long, time in milliseconds
     */
    long getFinishTime();

    /**
     * Command name.
     * 
     * @return String
     */
    String getName();

    /**
     * Collection of files affect by the command. Call to this method will return unmodifiable view on actual
     * results and should be treated accordingly until the command will not be completed.
     * 
     * @return collection of {@link CloudFile} objects
     */
    Collection<CloudFile> getFiles();

    /**
     * Collection of file paths removed by the command. Call to this method will return unmodifiable view on
     * actual
     * results and should be treated accordingly until the command will not be completed.
     * 
     * @return collection of {@link String} file paths
     */
    Collection<String> getRemoved();

    /**
     * Wait for command completion.
     * 
     * @throws ExecutionException if command thrown an exception.
     * @throws InterruptedException if current thread was interrupted.
     */
    void await() throws ExecutionException, InterruptedException;
  }

  /**
   * Internal interface used for calculation of {@link Command} progress.
   */
  protected interface CommandProgress {
    /**
     * Complete work in abstract units.
     * 
     * @return long
     */
    long getComplete();

    /**
     * Available work to do in abstract units.
     * 
     * @return long
     */
    long getAvailable();
  }

  /**
   * Helper for listeners firing.
   */
  protected class ListenerSupport {

    Queue<CloudDriveListener> registry = new ConcurrentLinkedQueue<CloudDriveListener>();

    public void fireOnConnect(CloudDriveEvent event) {
      for (CloudDriveListener listener : registry) {
        try {
          listener.onConnect(event);
        } catch (Throwable th) {
          // nothing should prevent the connect at this point
          LOG.warn("Error firing onConnect listener on Cloud Drive '" + title() + "': " + th.getMessage(), th);
        }
      }
    }

    public void fireOnDisconnect(CloudDriveEvent event) {
      for (CloudDriveListener listener : registry) {
        try {
          listener.onDisconnect(event);
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error firing onDisconnect listener on Cloud Drive '" + title() + "': " + th.getMessage(),
                   th);
        }
      }
    }

    public void fireOnRemove(CloudDriveEvent event) {
      for (CloudDriveListener listener : registry) {
        try {
          listener.onRemove(event);
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error firing onRemove listener on Cloud Drive '" + title() + "': " + th.getMessage(), th);
        }
      }
    }

    public void fireOnSynchronized(CloudDriveEvent event) {
      for (CloudDriveListener listener : registry) {
        try {
          listener.onSynchronized(event);
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error firing onSynchronized listener on Cloud Drive '" + title() + "': "
                       + th.getMessage(),
                   th);
        }
      }
    }

    public void fireOnError(CloudDriveEvent event, Throwable error, String operationName) {
      for (CloudDriveListener listener : registry) {
        try {
          listener.onError(event, error, operationName);
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error firing onError listener about '" + error.getMessage() + "' during " + operationName
              + " in cloud drive '" + title() + "': " + th.getMessage(), th);
        }
      }
    }

    public void invokeOnNew(CloudFile file) {
      for (CloudDriveListener listener : registry) {
        try {
          CloudDriveListener.FileChangeAction action = listener.getFileChangeAction();
          if (action != null) {
            action.onNew(file);
          }
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error calling onNew action of listener on Cloud Drive '" + title() + "': "
                       + th.getMessage(),
                   th);
        }
      }
    }

    public void invokeOnDelete(CloudFile file) {
      for (CloudDriveListener listener : registry) {
        try {
          CloudDriveListener.FileChangeAction action = listener.getFileChangeAction();
          if (action != null) {
            action.onRemove(file);
          }
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error calling onDelete action of listener on Cloud Drive '" + title() + "': "
                       + th.getMessage(),
                   th);
        }
      }
    }

    public void invokeOnUpdate(CloudFile prevFile, CloudFile newFile) {
      for (CloudDriveListener listener : registry) {
        try {
          CloudDriveListener.FileChangeAction action = listener.getFileChangeAction();
          if (action != null) {
            action.onUpdate(prevFile, newFile);
          }
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error calling onUpdate action of listener on Cloud Drive '" + title() + "': "
                       + th.getMessage(),
                   th);
        }
      }
    }

    public void invokeOnContent(CloudFile file) {
      for (CloudDriveListener listener : registry) {
        try {
          CloudDriveListener.FileChangeAction action = listener.getFileChangeAction();
          if (action != null) {
            action.onContent(file);
          }
        } catch (Throwable th) {
          // nothing should prevent at this point
          LOG.warn("Error calling onContent action of listener on Cloud Drive '" + title() + "': "
                       + th.getMessage(),
                   th);
        }
      }
    }
  }

  // *********** class body ************

  protected final ListenerSupport listeners = new ListenerSupport();

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return title() + " " + super.toString();
  }

  public void addListener(CloudDriveListener listener) {
    if (!listeners.registry.contains(listener)) {
      listeners.registry.add(listener);
    }
  }

  public void removeListener(CloudDriveListener listener) {
    listeners.registry.remove(listener);
  }

  /**
   * Return cloud user related to this Cloud Drive.
   * 
   * @return {@link CloudUser}
   */
  public abstract CloudUser getUser();

  /**
   * Cloud Drive title in storage.
   * 
   * @return {@link String} with title
   */
  public abstract String getTitle() throws DriveRemovedException, RepositoryException;

  /**
   * Link to the drive home.
   * 
   * @return {@link String}
   */
  public abstract String getLink() throws DriveRemovedException, RepositoryException;

  /**
   * Link to the drive's long-polling changes notification service. This kind of service optional and may not
   * be supported. If long-polling changes notification not supported then this link will be <code>null</code>
   * .
   * 
   * @return {@link String} a link to long-polling changes notification service or <code>null</code> if such
   *         service not supported.
   */
  public abstract String getChangesLink() throws DriveRemovedException,
                                         CloudProviderException,
                                         RepositoryException;

  /**
   * Update link to the drive's long-polling changes notification service. This kind of service optional and
   * may not be supported. If long-polling changes notification not supported then this method will do
   * nothing.
   * 
   */
  public abstract void updateChangesLink() throws DriveRemovedException,
                                          CloudProviderException,
                                          RepositoryException;

  /**
   * Local user related to this Cloud Drive.
   * 
   * @return {@link String}
   * @throws RepositoryException
   */
  public abstract String getLocalUser() throws DriveRemovedException, RepositoryException;

  /**
   * Initialization date of this Cloud Drive.
   * 
   * @return {@link Calendar}
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  public abstract Calendar getInitDate() throws DriveRemovedException, RepositoryException;

  /**
   * Date of currently established Cloud Drive connection.
   * 
   * @return {@link Calendar}
   * @throws RepositoryException
   * @throws DriveRemovedException
   * @throws NotConnectedException
   */
  public abstract Calendar getConnectDate() throws DriveRemovedException,
                                           NotConnectedException,
                                           RepositoryException;

  /**
   * Cloud Drive path in storage. It introduces storage depended identifier of the drive. <br/>
   * For JCR storage it's a drive Node path. It can be changed if drive node will be moved.
   * 
   * @return String with path in the store.
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  public abstract String getPath() throws DriveRemovedException, RepositoryException;

  /**
   * Unique identifier of the drive. The Id never changes.
   * 
   * @return String with id.
   * @throws RepositoryException
   */
  public abstract String getId() throws DriveRemovedException, RepositoryException;

  /**
   * Return file from local cloud drive.
   * 
   * @see #hasFile(String)
   * @return local cloud file in the drive.
   * @throws DriveRemovedException
   * @throws NotCloudFileException
   * @throws RepositoryException
   */
  public abstract CloudFile getFile(String path) throws DriveRemovedException,
                                                NotCloudFileException,
                                                RepositoryException;

  /**
   * Tells if file with given path exists in this cloud drive.
   * 
   * @param path String
   * @return boolean, {@code true} if given path denotes a cloud file from this drive, {@code false} otherwise
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  public abstract boolean hasFile(String path) throws DriveRemovedException, RepositoryException;

  /**
   * List of files on local cloud drive.
   * 
   * @return list of local cloud files in the drive.
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public abstract List<CloudFile> listFiles() throws DriveRemovedException,
                                             CloudDriveException,
                                             RepositoryException;

  /**
   * List of local cloud files on given as parent folder.
   * 
   * @param CloudFile parent folder
   * @return list of file is a folder, empty list otherwise.
   * @throws DriveRemovedException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  @Deprecated
  public abstract List<CloudFile> listFiles(CloudFile parent) throws DriveRemovedException,
                                                             CloudDriveException,
                                                             RepositoryException;

  /**
   * Connects cloud drive to local JCR storage. This method fetches metadata of remote files from the cloud
   * and adds records them in the local. Optionally it can fetch content of the file but this depends on the
   * cloud drive implementation. <br>
   * To check the state of the connect process use {@link CloudDrive#isConnected()}. To be informed in the
   * process register a listener to the drive {@link CloudDrive#addListener(CloudDriveListener)}. <br>
   * Method returns {@link Command} object what provides information about the connect process such as
   * progress in percents, timing and affected files available during the processing of the command. Connect
   * process will be started asynchronously in another thread and method return immediately.
   * 
   * @return {@link Command} describing the connect process
   * @see CloudDriveListener#onConnect(CloudDriveEvent)
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public abstract Command connect() throws CloudDriveException, RepositoryException;

  /**
   * Synchronize local storage with cloud drive. Refreshes metadata (and optionally a content) of the cloud
   * drive.<br/>
   * Drive may not support synchronization. In such case {@link SyncNotSupportedException} will be thrown.<br>
   * To check the state of the synchronization register a listener to drive
   * {@link CloudDrive#addListener(CloudDriveListener)}. <br>
   * Method returns {@link Command} object providing information about sychronization progress in
   * percents, timing and affected files available during the processing of the command. Synchronization
   * process will be started asynchronously in another thread and method return immediately.
   * 
   * @see CloudDriveListener#onSynchronized(CloudDriveEvent)
   * @see CloudDriveListener#getFileChangeAction()
   * @return {@link Command} describing the synchronization process
   * @throws SyncNotSupportedException if synchronization not supported
   * @throws DriveRemovedException
   * @throws CloudDriveException
   * @throws CloudDriveAccessException
   * @throws RepositoryException
   */
  public abstract Command synchronize() throws SyncNotSupportedException,
                                       DriveRemovedException,
                                       CloudDriveException,
                                       CloudDriveAccessException,
                                       RepositoryException;
  /**
   * Answers if drive is connected.
   * 
   * @return boolean, {@code true} if drive connected to local store, {@code false} otherwise.
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  public abstract boolean isConnected() throws DriveRemovedException, RepositoryException;

  // ********** internal stuff **********

  /**
   * Tells whether given node instance denotes this Cloud Drive. <br/>
   * A node denote a cloud drive if it represents this drive's storage root node and the drive connected. If
   * {@code includeFiles} is {@code true} then given node also will be tested whether it is a file on this
   * drive, and if it is, {@code true} will be returned.
   * 
   * @param node {@link Node}
   * @param includeFiles boolean, if {@code true} then given node also will be tested as a possible file of
   *          this drive and thus {@code true} will be returned for the file on this drive.
   * @return boolean, {@code true} if given object denotes this Cloud Drive, {@code false} otherwise.
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  protected abstract boolean isDrive(Node node, boolean includeFiles) throws DriveRemovedException,
                                                                     RepositoryException;

  /**
   * Disconnects cloud drive from local storage. Clean (remove) metadata of remote files from the local.
   * 
   * @throws DriveRemovedException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected abstract void disconnect() throws DriveRemovedException, CloudDriveException, RepositoryException;

  /**
   * Check access to the cloud provider using locally stored keys by authenticating with the cloud service.
   * 
   * @throws CloudDriveException if cloud provider error
   */
  protected abstract void checkAccess() throws CloudDriveException;

  /**
   * Renew access using given user credentials.
   * 
   * @param user {@link CloudUser}
   * @throws CloudDriveException if drive node was removed or cloud provider error
   * @throws RepositoryException if storage error
   */
  protected abstract void updateAccess(CloudUser user) throws CloudDriveException, RepositoryException;

  /**
   * Used internally for logger messages.
   * 
   * @return {@link String} with drive title.
   */
  protected abstract String title();

  /**
   * Configure environment for commands execution (optional).
   * 
   * @param env {@link CloudDriveEnvironment}
   * @param synchronizers collection of {@link CloudFileSynchronizer}, it will be used for file
   *          synchronization.
   */
  protected abstract void configure(CloudDriveEnvironment env, Collection<CloudFileSynchronizer> synchronizers);
}
