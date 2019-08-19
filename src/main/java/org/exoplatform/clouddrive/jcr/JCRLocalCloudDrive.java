/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.jcr;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import com.ibm.icu.text.Transliterator;

import org.exoplatform.clouddrive.BaseCloudDriveListener;
import org.exoplatform.clouddrive.CannotConnectDriveException;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveEnvironment;
import org.exoplatform.clouddrive.CloudDriveEvent;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveManager;
import org.exoplatform.clouddrive.CloudDriveMessage;
import org.exoplatform.clouddrive.CloudDriveSecurity;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudFileAPI;
import org.exoplatform.clouddrive.CloudFileSynchronizer;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.ConflictException;
import org.exoplatform.clouddrive.ConstraintException;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.DriveTrashedException;
import org.exoplatform.clouddrive.FileTrashRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.NotYetCloudFileException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.RetryLaterException;
import org.exoplatform.clouddrive.SkipChangeException;
import org.exoplatform.clouddrive.SkipSyncException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.ThreadExecutor;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive.JCRListener.AddTrashListener;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive.JCRListener.DriveChangesListener;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.clouddrive.utils.IdentityHelper;
import org.exoplatform.clouddrive.viewer.ContentReader;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;

/**
 * JCR storage for local cloud drive. Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRLocalCloudDrive.java 00000 Sep 13, 2012 pnedonosko $
 */
public abstract class JCRLocalCloudDrive extends CloudDrive implements CloudDriveStorage, CloudDriveSecurity {

  /**
   * Drive nodetype {@code ecd:cloudDrive}.
   */
  public static final String     ECD_CLOUDDRIVE        = "ecd:cloudDrive";

  /**
   * File nodetype {@code ecd:cloudFile}.
   */
  public static final String     ECD_CLOUDFILE         = "ecd:cloudFile";

  /**
   * Folder nodetype {@code ecd:cloudFolder}, it extends file.
   */
  public static final String     ECD_CLOUDFOLDER       = "ecd:cloudFolder";

  /**
   * File resource nodetype {@code ecd:cloudFileResource}.
   */
  public static final String     ECD_CLOUDFILERESOURCE = "ecd:cloudFileResource";

  /**
   * Nodetype-marker of ignored nodes inside cloud drive (for technical nodes).
   */
  public static final String     ECD_IGNORED           = "ecd:ignored";

  /** The Constant EXO_DATETIME. */
  public static final String     EXO_DATETIME          = "exo:datetime";

  /** The Constant EXO_MODIFY. */
  public static final String     EXO_MODIFY            = "exo:modify";

  /** The Constant EXO_TRASHFOLDER. */
  public static final String     EXO_TRASHFOLDER       = "exo:trashFolder";

  /** The Constant EXO_THUMBNAILS. */
  public static final String     EXO_THUMBNAILS        = "exo:thumbnails";

  /** The Constant EXO_THUMBNAIL. */
  public static final String     EXO_THUMBNAIL         = "exo:thumbnail";

  /** The Constant NT_FOLDER. */
  public static final String     NT_FOLDER             = "nt:folder";

  /** The Constant NT_FILE. */
  public static final String     NT_FILE               = "nt:file";

  /** The Constant NT_RESOURCE. */
  public static final String     NT_RESOURCE           = "nt:resource";

  /** The Constant NT_UNSTRUCTURED. */
  public static final String     NT_UNSTRUCTURED       = "nt:unstructured";

  /** The Constant MIX_REFERENCEABLE. */
  public static final String     MIX_REFERENCEABLE     = "mix:referenceable";

  /** The Constant MIX_VERSIONABLE. */
  public static final String     MIX_VERSIONABLE       = "mix:versionable";

  /** The Constant ECD_LOCALFORMAT. */
  public static final String     ECD_LOCALFORMAT       = "ecd:localFormat";

  /** The Constant CURRENT_LOCALFORMAT. */
  public static final double     CURRENT_LOCALFORMAT   = 1.1d;

  /** The Constant HISTORY_EXPIRATION - 8 days. */
  public static final long       HISTORY_EXPIRATION    = 1000 * 60 * 60 * 24 * 8;

  /** The Constant HISTORY_MAX_LENGTH. */
  public static final int        HISTORY_MAX_LENGTH    = 1000;

  /**
   * Number of files, after reaching it, a command can save the drive (was 30
   * before Feb 2018).
   */
  public static final int        COMMAND_CHANGES_CHUNK = 15;

  /** The Constant DUMMY_DATA. */
  public static final String     DUMMY_DATA            = "".intern();

  /** The Constant USER_WORKSPACE. */
  public static final String     USER_WORKSPACE        = "user.workspace";

  /** The Constant USER_NODEPATH. */
  public static final String     USER_NODEPATH         = "user.nodePath";

  /** The Constant USER_SESSIONPROVIDER. */
  public static final String     USER_SESSIONPROVIDER  = "user.sessionProvider";

  /**
   * Command stub for not running or already done commands.
   */
  protected static final Command ALREADY_DONE          = new AlreadyDone();

  /**
   * Command for not processing commands.
   */
  static class AlreadyDone implements Command {

    /** The time. */
    final long time = System.currentTimeMillis();

    /**
     * Gets the progress.
     *
     * @return the progress
     */
    @Override
    public int getProgress() {
      return COMPLETE;
    }

    /**
     * Checks if is done.
     *
     * @return true, if is done
     */
    @Override
    public boolean isDone() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChanges() {
      return false;
    }

    /**
     * Gets the files.
     *
     * @return the files
     */
    @Override
    public Collection<CloudFile> getFiles() {
      return Collections.emptyList();
    }

    /**
     * Gets the removed.
     *
     * @return the removed
     */
    @Override
    public Collection<String> getRemoved() {
      return Collections.emptyList();
    }

    /**
     * Gets the messages.
     *
     * @return the messages
     */
    @Override
    public Collection<CloudDriveMessage> getMessages() {
      return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartTime() {
      return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFinishTime() {
      return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void await() {
      // already done
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "complete";
    }
  }

  /**
   * The Class FileTrashing.
   */
  class FileTrashing {

    /** The latch. */
    private final CountDownLatch latch  = new CountDownLatch(1);

    /**
     * Path of a file in Trash folder. Will be initialized by
     * {@link AddTrashListener} if it is a real trashing, <code>null</code>
     * otherwise.
     */
    private String               trashPath;

    /**
     * Cloud file id. Will be initialized by {@link AddTrashListener} if it is a
     * real trashing, <code>null</code> otherwise.
     */
    private String               fileId;

    /** The remove. */
    private boolean              remove = false;

    /**
     * Confirm.
     *
     * @param path the path
     * @param fileId the file id
     */
    void confirm(String path, String fileId) {
      this.trashPath = path;
      this.fileId = fileId;
      latch.countDown();
    }

    /**
     * Removes the.
     */
    void remove() {
      remove = true;
    }

    /**
     * Complete.
     *
     * @throws InterruptedException the interrupted exception
     * @throws RepositoryException the repository exception
     */
    void complete() throws InterruptedException, RepositoryException {
      // Remove trashed file if asked explicitly, but first wait for 60 sec for
      // trashing confirmation.
      // If not confirmed - the file cannot be removed from the Trash locally
      // (we don't know path and/or id).
      latch.await(60, TimeUnit.SECONDS);
      if (remove) {
        removeTrashed();
      }
    }

    /**
     * Removes the trashed.
     *
     * @throws RepositoryException the repository exception
     */
    private void removeTrashed() throws RepositoryException {
      if (trashPath != null && fileId != null) {
        Session session = systemSession();

        Item trashed = session.getItem(trashPath);
        Node trash;
        if (trashed.isNode()) {
          Node file = (Node) trashed;
          if (fileAPI.isFile(file)) {
            if (fileId.equals(fileAPI.getId(file)) && rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
              removeNode(file);
              session.save();
              return;
            }
          }
          trash = trashed.getParent();
        } else {
          trash = trashed.getParent().getParent();
        }

        // otherwise try find and remove all trashed with this file id
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE ecd:id=" + fileId + " AND jcr:path LIKE '"
            + trash.getPath() + "/%'", Query.SQL);
        QueryResult qr = q.execute();
        for (NodeIterator niter = qr.getNodes(); niter.hasNext();) {
          Node file = niter.nextNode();
          if (rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
            removeNode(file);
          }
        }

        session.save();
      }
    }
  }

  /**
   * Perform actual removal of the drive from JCR on its move to the Trash.
   * Initialize cloud files trashing similarly as for removal.
   */
  public class JCRListener {

    /**
     * The listener interface for receiving removeDrive events. The class that
     * is interested in processing a removeDrive event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's <code>addRemoveDriveListener<code>
     * method. When the removeDrive event occurs, that object's appropriate
     * method is invoked.
     *
     * @see RemoveDriveEvent
     */
    class RemoveDriveListener implements EventListener {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        String userId = null; // for error messages
        try {
          Session session = systemSession();
          Node driveRoot;
          try {
            driveRoot = session.getNodeByUUID(rootUUID);
          } catch (ItemNotFoundException e) {
            // already removed
            LOG.warn("Cloud Drive '" + title() + "' node already removed directly from JCR: " + e.getMessage());
            finishTrashed(session, initialRootPath);
            return;
          }

          // we assume only NODE_REMOVED events here
          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            if (initialRootPath.equals(event.getPath())) {
              trashed = true; // set only if not exists
            }
          }

          checkTrashed(driveRoot);
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive '" + title() + "' node move/remove event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    /**
     * The listener interface for receiving addTrash events. The class that is
     * interested in processing a addTrash event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's <code>addAddTrashListener<code> method. When the addTrash
     * event occurs, that object's appropriate method is invoked.
     *
     * @see AddTrashEvent
     */
    class AddTrashListener implements EventListener {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        String userId = null; // for error messages
        try {
          Session session = systemSession();
          Node driveRoot = null; // will be calculated once
          // we assume NODE_ADDED events only
          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            String path = event.getPath();
            try {
              Item item = session.getItem(path);
              if (item.isNode()) {
                Node node = (Node) item;
                if (node.isNodeType(ECD_CLOUDDRIVE)) {
                  // drive trashed
                  if (driveRoot == null) {
                    try {
                      driveRoot = session.getNodeByUUID(rootUUID);
                    } catch (ItemNotFoundException e) {
                      // node already deleted
                      LOG.warn("Cloud Drive " + title() + " node already removed directly from JCR: " + e.getMessage());
                      finishTrashed(session, initialRootPath);
                      continue;
                    }
                  }

                  String rootPath = driveRoot.getPath();
                  if (rootPath.equals(path)) {
                    added = true;
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Cloud Drive trashed " + path);
                    }
                  }
                  checkTrashed(driveRoot);
                } else if (fileAPI.isFile(node) && !fileAPI.isIgnored(node)) {
                  // file trashed, but accept only this drive files (jcr
                  // listener will be fired for all
                  // existing drives)
                  if (rootUUID.equals(node.getProperty("ecd:driveUUID").getString())) {
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Cloud drive item trashed " + path);
                    }

                    // mark the file node to be able properly untrash it,
                    node.setProperty("ecd:trashed", true);
                    node.save();

                    // confirm file trashing (for FileChange NODE_REMOVED
                    // changes)
                    // this change also happens on node reordering in Trash when
                    // untrashing the same name
                    String fileId = fileAPI.getId(node);
                    FileTrashing confirmation;
                    FileTrashing existing = fileTrash.putIfAbsent(fileId, confirmation = new FileTrashing());
                    if (existing != null) {
                      confirmation = existing; // work with already posted trash
                    } // else, work with just posted trash
                    confirmation.confirm(path, fileId);

                    if (!fileAPI.isTrashSupported()) {
                      if (LOG.isDebugEnabled()) {
                        LOG.debug("Cloud drive item in Trash will be removed permanently " + path);
                      }
                      confirmation.remove();
                    }
                  }
                }
              } else {
                LOG.warn("Item in Trash not a node:" + path);
              }
            } catch (PathNotFoundException e) {
              // already deleted from JCR
              LOG.warn("Cloud item already deleted directly from JCR: " + path);
            }
          }
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive " + title() + " item move to Trash event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    /**
     * The listener interface for receiving driveChanges events. The class that
     * is interested in processing a driveChanges event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's <code>addDriveChangesListener<code>
     * method. When the driveChanges event occurs, that object's appropriate
     * method is invoked.
     *
     * @see DriveChangesEvent
     */
    class DriveChangesListener extends BaseCloudDriveListener implements EventListener {

      /**
       * The Class DelayedStart.
       */
      class DelayedStart implements Runnable {

        final long delay;

        /**
         * Instantiates a new delayed start.
         *
         * @param delay the expires in
         */
        DelayedStart(long delay) {
          super();
          this.delay = delay;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
          try {
            Thread.sleep(delay);
            SyncFilesCommand delayed = delayedChanges.getAndSet(null);
            if (delayed != null) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("> Starting delayed files synchronization of " + title());
              }
              // Execute delayed sync, if have an one, in the current thread
              delayed.exec();
            }
          } catch (InterruptedException e) {
            LOG.warn("Failed to wait for delayed file synchronization in cloud drive '" + title() + "'", e);
            Thread.currentThread().interrupt(); // restore the interrupted state
          } catch (CloudDriveException e) {
            LOG.error("Error starting delayed file synchronization in cloud drive '" + title() + "'", e);
          } catch (RepositoryException e) {
            LOG.error("Error applyting delayed file synchronization in cloud drive '" + title() + "'", e);
          }
        }
      }

      /** The lock. */
      final ThreadLocal<AtomicLong>           lock           = new ThreadLocal<AtomicLong>();

      /**
       * Changes delayed because of guessing that it's a move operation and we
       * want wait for its node exo:title and exo:name will be updated in next
       * session save.
       */
      final AtomicReference<SyncFilesCommand> delayedChanges = new AtomicReference<>();

      /**
       * Disable.
       */
      void disable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(1));
        } else {
          requests.incrementAndGet();
        }
      }

      /**
       * Enable.
       */
      void enable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(0));
        } else if (requests.get() > 0) {
          requests.decrementAndGet();
        }
      }

      /**
       * Enabled.
       *
       * @return true, if successful
       */
      boolean enabled() {
        AtomicLong requests = lock.get();
        return requests == null || requests.get() == 0;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        if (enabled()) {
          try {
            List<FileChange> changes = new ArrayList<FileChange>();
            boolean moveGuessed = false;
            Set<String> addedNodes = new HashSet<>();
            while (events.hasNext()) {
              Event event = events.nextEvent();
              String eventPath = event.getPath();

              if (eventPath.endsWith("/jcr:mixinTypes") || eventPath.endsWith("/jcr:content") || eventPath.indexOf("/ecd:") >= 0
                  || eventPath.indexOf("/exo:thumbnails") > 0) {
                // XXX hardcoded undesired system stuff to skip
                continue;
              }

              if (event.getType() == Event.NODE_REMOVED) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Node removed. User: " + event.getUserID() + ". Path: " + eventPath);
                }
                // Removal should be initiated by initRemove() before this event
                // happening and in the same
                // thread.
                // For direct JCR removals this should be done by
                // RemoveCloudFileAction. Then this removal
                // will succeed.
                // In case of move to Trash, it should be fully handled by
                // AddTrashListener, it will run a
                // dedicated command for it.
                // If removal not initiated (in this thread), then it's
                // move/rename - it will be handled by
                // NODE_ADDED below.

                // check if it is not a direct JCR remove or ECMS trashing (in
                // this thread)
                FileChange remove;
                Map<String, FileChange> removed = fileRemovals.get();
                if (removed != null && (remove = removed.remove(eventPath)) != null) {
                  changes.add(remove);
                } else {
                  // otherwise it's ignored file or not removal (may be a move
                  // or ordering)
                  moveGuessed = true;
                }
              } else {
                if (event.getType() == Event.NODE_ADDED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Node added. User: " + event.getUserID() + ". Path: " + eventPath);
                  }
                  if (moveGuessed) {
                    addedNodes.add(eventPath);
                  }
                  changes.add(new FileChange(eventPath, FileChange.CREATE));
                } else if (event.getType() == Event.PROPERTY_CHANGED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Node property changed. User: " + event.getUserID() + ". Path: " + eventPath);
                  }
                  if (moveGuessed && (eventPath.endsWith("/exo:name") || eventPath.endsWith("/exo:title"))
                      && addedNodes.contains(parentPath(eventPath))) {
                    // It's move (still guessed) but in single changes command,
                    // so we don't need delay it
                    moveGuessed = false;
                  }
                  changes.add(new FileChange(eventPath, FileChange.UPDATE));
                } // otherwise, we skip the event
              }
            }

            if (changes.size() > 0) {
              if (moveGuessed) {
                SyncFilesCommand delayed = new SyncFilesCommand(changes);
                // prepare env from this thread here, required for
                // DelayedStart.run()
                commandEnv.configure(delayed);
                SyncFilesCommand prevDelayed = delayedChanges.getAndSet(delayed);
                if (prevDelayed != null) {
                  // start previous delayed sync right now
                  prevDelayed.start();
                }
                // Try run these changes with a delay in a second, if another
                // onEvent() will not start them
                // sooner (see else-block below).
                // May 9 2018: In PLF4.4 delayed start will actually work for
                // move between different parents.
                // As for rename, the delayed changes, will be synced sooner by
                // a next session save that will
                // result in else-block work and merging two saves into a single
                // sync.
                workerExecutor.submit(new DelayedStart(1250));
              } else {
                SyncFilesCommand delayed = delayedChanges.getAndSet(null);
                if (delayed != null) {
                  // if have delayed changes we'll apply them with the current:
                  // merge delayed (first) and the current one (after the
                  // delayed)
                  delayed.changes.addAll(changes);
                  delayed.start();
                } else {
                  // start all changes files sync
                  new SyncFilesCommand(changes).start();
                }
              }
            } else {
              SyncFilesCommand delayed = delayedChanges.getAndSet(null);
              if (delayed != null) {
                // start previous delayed sync
                delayed.start();
              }
            }
          } catch (CloudDriveException e) {
            LOG.error("Error starting file synchronization in cloud drive '" + title() + "'", e);
          } catch (RepositoryException e) {
            LOG.error("Error reading cloud file for synchronization in cloud drive '" + title() + "'", e);
          }
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void onError(CloudDriveEvent event, Throwable error, String operationName) {
        // act on file sync errors only
        if (operationName.equals(SyncFilesCommand.NAME)) {
          if (error instanceof RefreshAccessException) {
            Throwable cause = error.getCause();
            LOG.error("Error running " + operationName + " in drive " + title() + ". " + error.getMessage()
                + (cause != null ? ". " + cause.getMessage() : ""));
          } else {
            LOG.error("Error running " + operationName + " in drive " + title(), error);
          }
        }
      }
    }

    /** The initial root path. */
    final String               initialRootPath;

    /** The remove listener. */
    final RemoveDriveListener  removeListener;

    /** The trash listener. */
    final AddTrashListener     trashListener;

    /** The changes listener. */
    final DriveChangesListener changesListener;

    /** The trashed. */
    volatile boolean           trashed = false;

    /** The added. */
    volatile boolean           added   = false;

    /**
     * Instantiates a new JCR listener.
     *
     * @param initialRootPath the initial root path
     */
    JCRListener(String initialRootPath) {
      this.initialRootPath = initialRootPath;
      this.removeListener = new RemoveDriveListener();
      this.trashListener = new AddTrashListener();
      this.changesListener = new DriveChangesListener();
    }

    /**
     * Check trashed.
     *
     * @param driveRoot the drive root
     * @throws RepositoryException the repository exception
     */
    synchronized void checkTrashed(Node driveRoot) throws RepositoryException {
      if (trashed && added) {
        try {
          if (driveRoot.getParent().isNodeType(EXO_TRASHFOLDER)) {
            // drive in the Trash
            finishTrashed(driveRoot.getSession(), driveRoot.getPath());

            // disconnect and remove with a delay in another thread
            workerExecutor.submit(new Runnable() {
              /**
               * {@inheritDoc}
               */
              @Override
              public void run() {
                boolean interrupted = false;
                try {
                  final Session session = systemSession();
                  final Node driveRoot = session.getNodeByUUID(rootUUID);

                  try {
                    Thread.sleep(2000); // wait a bit for ECMS actions
                  } catch (InterruptedException e) {
                    LOG.warn("Cloud Drive remover interrupted " + e.getMessage());
                    interrupted = true;
                  }

                  startAction(JCRLocalCloudDrive.this);

                  try {
                    disconnect(driveRoot); // disconnect under system session!
                  } catch (Throwable e) {
                    // error of disconnect - don't care much here
                    LOG.error("Error disconnecting Cloud Drive " + title() + " before its removal. " + e.getMessage(), e);
                  }

                  try {
                    driveRoot.remove();
                    session.save();
                    LOG.info("Cloud Drive " + title() + " successfully removed from the Trash.");
                  } catch (ItemNotFoundException e) {
                    // node already deleted
                    LOG.warn("Cloud Drive " + title() + " node already removed directly from JCR: " + e.getMessage());
                  }

                } catch (Throwable e) {
                  LOG.error("Error removing node of Cloud Drive " + title() + ". " + e.getMessage(), e);
                } finally {
                  doneAction(); // done in this thread

                  if (interrupted) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
          }
        } catch (ItemNotFoundException e) {
          // drive is in root of the workspace
          // should not happen in current implementation, but possible in
          // general
          // do nothing!
        }
      }
    }

    /**
     * Finish trashed.
     *
     * @param session the session
     * @param rootPath the root path
     */
    void finishTrashed(Session session, String rootPath) {
      // reset flags and unregister both listeners from the Observation
      trashed = added = false;

      try {
        removeJCRListener(session);
      } catch (RepositoryException e) {
        LOG.error("Error unregistering Cloud Drive '" + title() + "' node listeners: " + e.getMessage(), e);
      }

      // fire listeners
      listeners.fireOnRemove(new CloudDriveEvent(getUser(), rootWorkspace, rootPath));
    }

    /**
     * Enable.
     */
    public void enable() {
      changesListener.enable();
    }

    /**
     * Disable.
     */
    public void disable() {
      changesListener.disable();
    }
  }

  /**
   * Asynchronous runner for {@link Command}.
   */
  protected class CommandCallable implements Callable<Command> {

    /** The command. */
    final AbstractCommand command;

    /**
     * Instantiates a new command callable.
     *
     * @param command the command
     * @throws CloudDriveException the cloud drive exception
     */
    CommandCallable(AbstractCommand command) throws CloudDriveException {
      this.command = command;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Command call() throws Exception {
      command.exec();
      return command;
    }
  }

  /**
   * The Class ExoJCRSettings.
   */
  protected class ExoJCRSettings {

    /** The conversation. */
    final ConversationState conversation;

    /** The container. */
    final ExoContainer      container;

    /** The prev conversation. */
    ConversationState       prevConversation;

    /** The prev container. */
    ExoContainer            prevContainer;

    /** The prev sessions. */
    SessionProvider         prevSessions;

    /**
     * Instantiates a new exo JCR settings.
     *
     * @param conversation the conversation
     * @param container the container
     */
    ExoJCRSettings(ConversationState conversation, ExoContainer container) {
      this.conversation = conversation;
      this.container = container;
    }
  }

  /**
   * Setup environment for commands execution in eXo JCR Container.
   */
  protected class ExoJCREnvironment extends CloudDriveEnvironment {

    /** The config. */
    protected final Map<Command, ExoJCRSettings> config = Collections.synchronizedMap(new HashMap<Command, ExoJCRSettings>());

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Command command) throws CloudDriveException {
      ConversationState conversation = ConversationState.getCurrent();
      if (conversation == null) {
        throw new CloudDriveException("Error to " + command.getName() + " drive for user " + getUser().getEmail()
            + ". Conversation state not set.");
      }

      config.put(command, new ExoJCRSettings(conversation, ExoContainerContext.getCurrentContainer()));

      super.configure(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(Command command) throws CloudDriveException {
      ExoJCRSettings settings = config.get(command);
      if (settings != null) {
        settings.prevConversation = ConversationState.getCurrent();
        ConversationState.setCurrent(settings.conversation);

        // set correct container
        settings.prevContainer = ExoContainerContext.getCurrentContainerIfPresent();
        ExoContainerContext.setCurrentContainer(settings.container);

        // Begin lifecycle
        RequestLifeCycle.begin(settings.container);

        // set correct SessionProvider
        settings.prevSessions = sessionProviders.getSessionProvider(null);
        sessionProviders.setSessionProvider(null, new SessionProvider(settings.conversation));
      } else {
        throw new CloudDriveException(this.getClass().getName() + " setting not configured for " + command + " to be prepared.");
      }
      // Super (chained env) prepare after this env as they may depends on the
      // current
      super.prepare(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup(Command command) throws CloudDriveException {
      // Let super (chained env) cleanup first as it may depend on this env
      CloudDriveException superError = null;
      RuntimeException superRuntimeError = null;
      try {
        super.cleanup(command);
      } catch (CloudDriveException cde) {
        superError = cde;
      } catch (RuntimeException ue) {
        superRuntimeError = ue;
      } finally {
        ExoJCRSettings settings = config.remove(command); // May 8 2018, was
                                                          // get()
        if (settings != null) {
          SessionProvider sp = sessionProviders.getSessionProvider(null);
          RequestLifeCycle.end();
          sessionProviders.setSessionProvider(null, settings.prevSessions);
          ExoContainerContext.setCurrentContainer(settings.prevContainer);
          ConversationState.setCurrent(settings.prevConversation);
          sp.close();
        } else {
          String message = this.getClass().getName() + " setting not configured for " + command + " to be cleaned";
          if (superError != null) {
            LOG.warn(message + ". But another error raised: " + superError.getMessage());
            throw superError;
          }
          if (superRuntimeError != null) {
            LOG.warn(message + ". But runtime error raised: " + superRuntimeError.getMessage());
            throw superRuntimeError;
          }
          throw new CloudDriveException(message);
        }
      }
    }
  }

  /**
   * Basic command pattern.
   */
  protected abstract class AbstractCommand implements Command, CommandProgress {

    /**
     * Messages generated by command.
     */
    protected final Queue<CloudDriveMessage> messages         = new ConcurrentLinkedQueue<CloudDriveMessage>();

    /**
     * Target JCR node. Will be initialized in exec() method (in actual runner
     * thread).
     */
    protected Node                           driveNode;

    /**
     * Progress indicator in percents.
     */
    protected final AtomicInteger            progressReported = new AtomicInteger();

    /**
     * Time of command start.
     */
    protected final AtomicLong               startTime        = new AtomicLong();

    /**
     * Time of command finish.
     */
    protected final AtomicLong               finishTime       = new AtomicLong();

    /**
     * Actually open item iterators. Used for progress indicator.
     */
    protected final List<ChunkIterator<?>>   iterators        = new ArrayList<ChunkIterator<?>>();

    /**
     * Local files changed by the command (not accessible by overriding commands
     * - use related methods instead).
     */
    private final Queue<CloudFile>           changed          = new ConcurrentLinkedQueue<CloudFile>();

    /**
     * Local file paths deleted by the command (not accessible by overriding
     * commands - use related methods instead).
     */
    private final Queue<String>              removed          = new ConcurrentLinkedQueue<String>();

    /**
     * Counter of already saved changed and removed files.
     */
    private int                              saved            = 0;

    /**
     * Number of attempts to process the command (see {@link #exec()}).
     */
    private int                              attemptNumb      = 0;

    /**
     * Asynchronous execution support.
     */
    protected Future<Command>                async;

    /** The settings. */
    protected ExoJCRSettings                 settings;

    /**
     * Base command constructor.
     */
    protected AbstractCommand() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new StringBuilder().append(super.toString()).append(" (").append(title()).append(")").toString();
    }

    /**
     * Save the drive's JCR node with currently changed files.
     *
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected void save() throws RepositoryException, CloudDriveException {
      driveNode.save();
    }

    /**
     * Save the drive's JCR node if number of changed files reached a threshold
     * for a chunk.
     *
     * @return <code>true</code> if save was performed, <code>false</code>
     *         otherwise
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     * @see COMMAND_CHANGES_CHUNK
     */
    private boolean saveChunk() throws RepositoryException, CloudDriveException {
      int changedNumber = changed.size() + removed.size();
      if (changedNumber - saved > COMMAND_CHANGES_CHUNK) {
        preSaveChunk();
        save();
        saved = changedNumber;
        return true;
      }
      return false;
    }

    /**
     * Adds the changed.
     *
     * @param file the file
     * @return true, if successful
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected boolean addChanged(CloudFile file) throws RepositoryException, CloudDriveException {
      boolean r = changed.add(file);
      saveChunk();
      return r;
    }

    /**
     * Removes the changed.
     *
     * @param file the file
     * @return true, if successful
     */
    protected boolean removeChanged(CloudFile file) {
      return changed.remove(file);
    }

    /**
     * Checks if is changed.
     *
     * @param file the file
     * @return true, if is changed
     */
    protected boolean isChanged(CloudFile file) {
      return changed.contains(file);
    }

    /**
     * Replace changed.
     *
     * @param files the files
     */
    protected void replaceChanged(Collection<CloudFile> files) {
      changed.clear();
      changed.addAll(files);
    }

    /**
     * Adds the removed.
     *
     * @param path the path
     * @return true, if successful
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected boolean addRemoved(String path) throws RepositoryException, CloudDriveException {
      boolean r = removed.add(path);
      saveChunk();
      return r;
    }

    /**
     * Removes the removed.
     *
     * @param path the path
     * @return true, if successful
     */
    protected boolean removeRemoved(String path) {
      return removed.remove(path);
    }

    /**
     * Checks if is removed.
     *
     * @param path the path
     * @return true, if is removed
     */
    protected boolean isRemoved(String path) {
      return removed.contains(path);
    }

    /**
     * Replace removed.
     *
     * @param paths the paths
     */
    protected void replaceRemoved(Collection<String> paths) {
      removed.clear();
      removed.addAll(paths);
    }

    /**
     * {@inheritDoc}
     */
    public int getAttempts() {
      return attemptNumb;
    }

    /**
     * Reset the command for a next attempt.
     */
    private void reset() {
      iterators.clear(); // clear iterators
    }

    /**
     * Processing logic.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    protected abstract void process() throws CloudDriveException, RepositoryException, InterruptedException;

    /**
     * Finalization logic that should be done always on the command end. Note
     * that this method will be called once even if command will be retried due
     * to know errors.
     */
    protected abstract void always();

    /**
     * It is a method where data or action specific to a connector can be
     * applied when need of save will happened in {@link #saveChunk()}.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     */
    protected abstract void preSaveChunk() throws CloudDriveException, RepositoryException;

    /**
     * Start command execution. If command will fail due to provider error, the
     * execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the
     * throwing an exception.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     */
    protected final void exec() throws CloudDriveException, RepositoryException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("> Running drive " + getName() + " command of " + title());
      }

      startTime.set(System.currentTimeMillis());
      driveCommands.add(this);
      try {
        commandEnv.prepare(this); // prepare environment
        jcrListener.disable();
        startAction(JCRLocalCloudDrive.this);
        driveNode = rootNode(); // init in actual runner thread

        while (true && !Thread.currentThread().isInterrupted()) {
          try {
            process();
            return;
          } catch (CloudProviderException e) {
            // in case of provider errors rollback and re-try an attempt
            if (!Thread.currentThread().isInterrupted() && getUser().getProvider().retryOnProviderError()) {
              attemptNumb++;
              if (attemptNumb > CloudDriveConnector.PROVIDER_REQUEST_ATTEMPTS) {
                throw e;
              } else {
                rollback(driveNode);
                // TODO Do we need to re-init for a case of InvalidItemStateException?
                // driveNode = rootNode();
                reset();
                LOG.warn("Error running " + getName() + " command of " + title() + ". " + e.getMessage()
                    + ". Rolled back and will run next attempt in " + CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT
                    + "ms.", e);
                Thread.sleep(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT);
              }
            } else {
              throw e;
            }
          }
        }

        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Drive " + getName() + " command interrupted for " + title());
        } else {
          LOG.warn("Drive " + getName() + " command of " + title() + " finished unexpectedly.");
        }
      } catch (CloudDriveException e) {
        handleError(driveNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RepositoryException e) {
        handleError(driveNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (InterruptedException e) {
        // special case: the command canceled
        handleError(driveNode, e, getName());
        commandEnv.fail(this, e);
        Thread.currentThread().interrupt();
        throw new CloudDriveException("Drive " + getName() + " canceled", e);
      } catch (RuntimeException e) {
        handleError(driveNode, e, getName());
        commandEnv.fail(this, e);
        LOG.error("Runtime error. Drive " + getName() + " canceled. " + e.getMessage());
        throw e;
      } catch (Throwable e) {
        handleError(driveNode, e, getName());
        commandEnv.fail(this, e);
        LOG.error("Unexpected error. Drive " + getName() + " canceled. " + e.getMessage(), e);
        throw e;
      } finally {
        always();
        doneAction();
        jcrListener.enable();
        commandEnv.cleanup(this); // cleanup environment
        driveCommands.remove(this);
        finishTime.set(System.currentTimeMillis());

        if (LOG.isDebugEnabled()) {
          LOG.debug("< Ended drive " + getName() + " command for " + title() + " in " + (finishTime.get() - startTime.get())
              + "ms.");
        }
      }
    }

    /**
     * Start command execution asynchronously using {@link #exec()} method
     * inside {@link CommandCallable}. Any exception if happened will be thrown
     * by resulting {@link Future}.
     *
     * @return {@link Future} associated with this command.
     * @throws CloudDriveException if no ConversationState set in caller thread.
     */
    Future<Command> start() throws CloudDriveException {
      commandEnv.configure(this);
      return async = workerExecutor.submit(new CommandCallable(this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getComplete() {
      int complete = 0;
      for (ChunkIterator<?> child : iterators) {
        complete += child.getFetched();
      }
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
      int available = 0;
      for (ChunkIterator<?> child : iterators) {
        available += child.getAvailable();
      }
      // return always +7,5% more, average time for JCR save on mid-to-big drive
      return Math.round(available * 1.075f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProgress() {
      if (isDone()) {
        return COMPLETE;
      } else {
        int current = Math.round((getComplete() * 100f) / getAvailable());
        int reported = progressReported.get();
        if (current >= reported) {
          progressReported.set(reported = current);
        } // else
          // progress cannot be smaller of already reported one
          // do nothing and wait for next portion of work done
        return reported;
      }
    }

    /**
     * Checks if is done.
     *
     * @return true, if is done
     */
    @Override
    public boolean isDone() {
      return getFinishTime() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartTime() {
      return startTime.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFinishTime() {
      return finishTime.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChanges() {
      return changed.size() > 0 || removed.size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CloudFile> getFiles() {
      return Collections.unmodifiableCollection(changed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getRemoved() {
      return Collections.unmodifiableCollection(removed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void await() throws ExecutionException, InterruptedException {
      if (async != null && !async.isDone() && !async.isCancelled()) {
        async.get();
      } // else do nothing - command already done or was canceled
    }

    /**
     * {@inheritDoc}
     */
    public Collection<CloudDriveMessage> getMessages() {
      // we remove the same messages (may have a place for several updates of
      // the same file)
      // rely on CloudDriveMessage hashCode/equals:
      Set<CloudDriveMessage> unique = new LinkedHashSet<CloudDriveMessage>();
      for (CloudDriveMessage m : messages) {
        unique.add(m);
      }
      return unique;
    }
  }

  /**
   * Connect command.
   */
  protected abstract class ConnectCommand extends AbstractCommand {

    /**
     * File IDs with their parent IDs of added to the drive but not yet saved
     * (by chunk saving).
     */
    private Map<String, Set<String>> connecting = new HashMap<String, Set<String>>();

    /**
     * File IDs with their parent IDs already added and saved in the drive (by
     * chunk saving).
     */
    private Map<String, Set<String>> connected  = new HashMap<String, Set<String>>();

    /**
     * Connect command constructor.
     *
     * @throws RepositoryException the repository exception
     * @throws DriveRemovedException the drive removed exception
     */
    protected ConnectCommand() throws RepositoryException, DriveRemovedException {
      super();
    }

    /**
     * Fetch actual files from cloud provider to local JCR.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     */
    protected abstract void fetchFiles() throws CloudDriveException, RepositoryException;

    /**
     * NOT SUPPORTED! Don't use this method within Connect commands - use
     * {@link #addConnected(String, CloudFile)} instead.
     *
     * @param file the file
     * @return true, if successful
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    @Override
    protected final boolean addChanged(CloudFile file) throws RepositoryException, CloudDriveException {
      throw new IllegalStateException("Adding changed files not supported by " + getName() + " command! Use addConnected().");
    }

    /**
     * Add just connected file and associate it with a given parent ID. This
     * method should be used instead of {@link #addChanged(CloudFile)}.
     *
     * @param parentId the parent id
     * @param file the file
     * @return true, if successful
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected boolean addConnected(String parentId, CloudFile file) throws RepositoryException, CloudDriveException {
      boolean r = super.addChanged(file);
      if (r) {
        connecting.computeIfAbsent(file.getId(), k -> new HashSet<String>()).add(parentId);
      }
      return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {
      // Rollout a chunk of connecting files to connected set
      connected.putAll(connecting);
      connecting.clear();
    }

    /**
     * Check if a file with given ID and parent ID is already connected by this
     * command.
     *
     * @param parentId {@link String} parent ID, assumed never <code>null</code>
     * @param fileId {@link String} file ID
     * @return <code>true</code> if file already connected, <code>false</code>
     *         otherwise
     */
    protected boolean isConnected(String parentId, String fileId) {
      Set<String> connectedParents = connected.get(fileId);
      return connectedParents != null && connectedParents.contains(parentId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "connect";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException, InterruptedException {
      // reset all possible previous attempts metadata
      driveNode.setProperty("ecd:localChanges", DUMMY_DATA);
      driveNode.setProperty("ecd:localHistory", DUMMY_DATA);
      driveNode.setProperty("ecd:connected", false);
      save();

      // fetch all files to local storage
      fetchFiles();

      // check before saving the result
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Drive connection interrupted for " + title());
      }

      // connected drive properties
      driveNode.setProperty("ecd:cloudUserId", getUser().getId());
      driveNode.setProperty("ecd:cloudUserName", getUser().getUsername());
      driveNode.setProperty("ecd:userEmail", getUser().getEmail());
      driveNode.setProperty("ecd:connectDate", Calendar.getInstance());

      // mark as connected
      driveNode.setProperty("ecd:connected", true);

      // and save the drive
      save();

      // fire listeners
      listeners.fireOnConnect(new CloudDriveEvent(getUser(), rootWorkspace, driveNode.getPath()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void always() {
      currentConnect.set(noConnect); // clean current, see connect()
    }
  }

  /**
   * The Class NoConnectCommand.
   */
  protected final class NoConnectCommand extends ConnectCommand {

    /**
     * Instantiates a new no connect command.
     *
     * @throws RepositoryException the repository exception
     * @throws DriveRemovedException the drive removed exception
     */
    NoConnectCommand() throws RepositoryException, DriveRemovedException {
      super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      // nothing
    }
  }

  /**
   * Synchronization processor for actual implementation of Cloud Drive and its
   * storage.
   */
  protected abstract class SyncCommand extends AbstractCommand {
    /**
     * Existing locally files (single file can be mapped to several parents).
     */
    protected Map<String, List<Node>> nodes;

    /**
     * UUIDs of file nodes linked in other places in the JCR workspace (e.g.
     * ECMS symlinks shared with other users). These IDs should be collected by
     * calling method {@link #removeLinks(Node)} and the links will be removed
     * on a next chunk save in {@link #save()}.
     */
    protected Set<String>             linkedNodes = new HashSet<String>();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException, InterruptedException {
      boolean hasChanges;
      syncLock.writeLock().lock(); // write-lock acquired exclusively by single
                                   // threads (drive sync)
      try {
        // don't do drive sync with not applied previous local file changes
        preSyncFiles();

        // sync with remote provider
        syncFiles();

        // check before saving the result
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Drive synchronization interrupted for " + title());
        }

        hasChanges = hasChanges();

        // save the drive even if no changes found (we may need to save change
        // ID for later tracking etc)
        save();
      } finally {
        // transfer all available messages to an user, then unlock
        for (Iterator<CloudDriveMessage> miter = syncFilesMessages.iterator(); miter.hasNext();) {
          messages.add(miter.next());
          miter.remove();
        }

        // unlock
        syncLock.writeLock().unlock();

        // help GC
        if (nodes != null) {
          nodes.clear();
        }
      }

      // fire listeners afterwards and only if actual changes have a place
      if (hasChanges) {
        listeners.fireOnSynchronized(new CloudDriveEvent(getUser(),
                                                         rootWorkspace,
                                                         driveNode.getPath(),
                                                         getFiles(),
                                                         getRemoved()));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void always() {
      currentSync.set(noSync); // clean current, see synchronize()
    }

    /**
     * Prepare files synchronization from cloud provider to local JCR. It
     * includes applying of local changes to remote provider if such had a
     * place. Provider implementations may add other logic to this method.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    protected void preSyncFiles() throws CloudDriveException, RepositoryException, InterruptedException {
      List<FileChange> changes = savedChanges();
      if (changes.size() > 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Applying stored local changes in " + title());
        }
        // run sync in this thread and w/o locking the syncLock
        SyncFilesCommand localChanges = new SyncFilesCommand(changes);
        localChanges.initDriveNode(driveNode);
        localChanges.sync();
      }
    }

    /**
     * Synchronize files from cloud provider to local JCR.
     *
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    protected abstract void syncFiles() throws CloudDriveException, RepositoryException, InterruptedException;

    /**
     * Traverse all child nodes from the drive's local {@link Node} to
     * {@link #nodes} tree. Note that single file can be mapped to several
     * parents. <br>
     * This method and its resulting tree can be used in actual algorithms for
     * merging of remote and local changes. But call it "on-demand", only when
     * have changes to merge, otherwise it will add undesired JCR load (due to a
     * whole subtree traversing).
     *
     * @throws RepositoryException the repository exception
     */
    protected void readLocalNodes() throws RepositoryException {
      Map<String, List<Node>> nodes = new LinkedHashMap<String, List<Node>>();
      String rootId = fileAPI.getId(driveNode);
      List<Node> rootList = new ArrayList<Node>();
      rootList.add(driveNode);
      nodes.put(rootId, rootList);
      readNodes(driveNode, nodes, true);

      this.nodes = nodes;
    }

    /**
     * Removes the node with its links and cached children in local nodes map.
     * The removed node path will be added to collection of removed in this
     * command.
     *
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected void removeLocalNode(Node node) throws RepositoryException, CloudDriveException {
      String npath = node.getPath();
      if (nodes != null) {
        for (Iterator<List<Node>> cnliter = nodes.values().iterator(); cnliter.hasNext()
            && !Thread.currentThread().isInterrupted();) {
          List<Node> cnl = cnliter.next();
          for (Iterator<Node> ecniter = cnl.iterator(); ecniter.hasNext();) {
            Node cn = ecniter.next();
            if (cn.getPath().startsWith(npath)) {
              ecniter.remove();
            }
          }
          if (cnl.size() == 0) {
            cnliter.remove();
          }
        }
      }
      // explicitly remove file links outside the drive, then the node itself
      removeNode(node);
      addRemoved(npath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save() throws RepositoryException, CloudDriveException {
      // first save the drive changes
      super.save();
      // remove affected links (e.g. ECMS symlinks) outside the drive
      Session session = session();
      for (String fileUUID : linkedNodes) {
        JCRLocalCloudDrive.this.removeLinks(session, fileUUID);
      }
    }
  }

  /**
   * A stub of Synchronization process meaning "no sync currently". Used in
   * synchronize() method.
   */
  protected final class NoSyncCommand extends SyncCommand {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException {
      // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {
      // nothing
    }
  }

  /**
   * The Class SyncFilesCommand.
   */
  protected class SyncFilesCommand extends AbstractCommand {

    /** The Constant NAME. */
    static final String    NAME                  = "files synchronization";

    /** Max allowed number of retry attempts for change application. */
    static final int       RETRY_ATTEMPTS_MAX    = 3;

    /**
     * Max allowed timeout to wait before a retry attempt, it's
     * {@value #RETRY_TIMEOUT_MAX} milliseconds.
     */
    static final long      RETRY_TIMEOUT_MAX     = 60000;

    /**
     * Default timeout to wait before a retry attempt, it's
     * {@value #RETRY_TIMEOUT_DEFAULT} milliseconds.
     */
    static final long      RETRY_TIMEOUT_DEFAULT = 2000;

    /**
     * This command updating files. Will be removed at the processing end.
     */
    final List<String>     updating              = new ArrayList<String>();    // use
                                                                               // List
                                                                               // to
                                                                               // track
                                                                               // number
                                                                               // of
                                                                               // updates

    /**
     * Applied changes for saving in a chunk. See {@link #sync()} and
     * {@link #save()}.
     */
    final List<FileChange> applied               = new ArrayList<FileChange>();

    /**
     * Changes to skip in the history when saving in a chunk.
     */
    final List<FileChange> skipped               = new ArrayList<FileChange>();

    /** The changes. */
    final List<FileChange> changes;

    /**
     * Instantiates a new sync files command.
     *
     * @param changes the changes
     */
    SyncFilesCommand(List<FileChange> changes) {
      this.changes = changes;

      // updating status of the drive
      for (FileChange change : changes) {
        String path = change.path; // path of file node or its property here
        initUpdating(path);
        updating.add(path);
        String id = change.fileId; // file id will be available for removals
                                   // only
        if (id != null) {
          initUpdating(id);
          updating.add(id);
        }
      }
    }

    /**
     * Inits the drive node.
     *
     * @param driveNode the drive node
     */
    void initDriveNode(Node driveNode) {
      this.driveNode = driveNode;
    }

    /**
     * Mark the change's file as updating in the drive. Note {@link FileChange}
     * should be accepted before calling this method for actual file path,
     * otherwise it will be <code>null</code>.
     *
     * @param ch {@link FileChange}
     */
    void updating(FileChange ch) {
      String path = ch.filePath;
      updating.add(path);
      addUpdating(path);
      String id = ch.fileId;
      if (id != null) {
        updating.add(id);
        addUpdating(id);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException, InterruptedException {
      if (isConnected()) {
        // wait for whole drive sync
        syncLock.readLock().lock(); // read-lock can be acquired by multiple
                                    // threads (file syncs)
        try {
          // save observed changes to the drive store, they will be reset in
          // case of success in sync()
          if (getAttempts() == 0) {
            saveChanges(changes); // save only in first attempt
          }
          // apply changes
          sync();
        } finally {
          syncLock.readLock().unlock();
        }

        // fire listeners afterwards and only if actual changes have a place
        if (hasChanges()) {
          listeners.fireOnSynchronized(new CloudDriveEvent(getUser(),
                                                           rootWorkspace,
                                                           driveNode.getPath(),
                                                           getFiles(),
                                                           getRemoved()));
        }
      } else {
        LOG.warn("Cannot synchronize file in cloud drive '" + title() + "': drive not connected");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void always() {
      // clean changes on the command end only, this will let next attempts what
      // to work on
      changes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {
      // Files sync doesn't have a pre-save logic by default.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save() throws RepositoryException, CloudDriveException {
      super.save();
      if (applied.size() > 0 || skipped.size() > 0) {
        // Commit also just saved part of changes to the drive history, omit
        // skipped
        commitChanges(applied, skipped);
        applied.clear();
        skipped.clear();
      }
    }

    /**
     * Sync.
     *
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     * @throws InterruptedException the interrupted exception
     */
    void sync() throws RepositoryException, CloudDriveException, InterruptedException {
      try {
        // compress the changes list:
        // * skip not supported items/ignored
        // * reduce number of creation/updates of the same path (make single
        // file update for its properties update)
        // * perform prerequisites check and throw exception if required
        // FYI this compression is file centric and doesn't assume that some
        // property update might change constraints for later file update/copy/removal, if such
        // case will be actual then need change this logic to accept such properties update.

        // collection of accepted, natural order important!
        Map<String, FileChange> accepted = new LinkedHashMap<String, FileChange>();
        // set copied nodes to skip their sub-nodes
        Set<String> copied = new LinkedHashSet<String>();
        for (Iterator<FileChange> chiter = changes.iterator(); chiter.hasNext() && !Thread.currentThread().isInterrupted();) {
          FileChange change = chiter.next();

          // ensure our change isn't applied by other command (e.g. drive sync
          // caused files sync of saved changes) as syncLock.readLock will be unlocked while waiting the
          // next attempt, this can be possible.
          if (getAttempts() > 0 && !hasChange(change)) {
            continue; // skip already applied change
          }

          try {
            if (change.accept()) {
              // updating with actual file path/id, but only if it is not already tracked
              updating(change);
              String path = change.filePath; // actual file path here!

              FileChange previous = accepted.get(path);
              if (previous != null) {
                String prevChange = previous.changeType;
                if (FileChange.UPDATE.equals(change.changeType) && (FileChange.CREATE.equals(prevChange)
                    || FileChange.UPDATE.equals(prevChange) || FileChange.UPDATE_CONTENT.equals(prevChange))) {
                  // skip updates of just created/updated
                  skipped.add(change);
                  continue;
                } else if (FileChange.REMOVE.equals(change.changeType)) {
                  if (FileChange.CREATE.equals(prevChange)) {
                    // ignore previous creation of just removed file, skip this
                    // removal: cloud should not be affected
                    skipped.add(accepted.remove(path));
                    skipped.add(change);
                    continue;
                  } else if (FileChange.UPDATE.equals(prevChange) || FileChange.UPDATE_CONTENT.equals(prevChange)) {
                    // ignore previous updates of just removed file
                    skipped.add(accepted.remove(path));
                  }
                } else if (FileChange.CREATE.equals(change.changeType) && FileChange.REMOVE.equals(prevChange)) {
                  // FYI actual for complex uses of JCR, when the same location
                  // was removed and added in single save here we need keep both, but Map cannot accept
                  // same keys (paths), we need to copy the accepted map and store the removal with
                  // a new (different) key then creation will be added in the end of the order below
                  Map<String, FileChange> newOrder = new LinkedHashMap<String, FileChange>();
                  for (Map.Entry<String, FileChange> ae : accepted.entrySet()) {
                    if (ae.getValue() == previous) {
                      // our removal with new key, it will not be affected by path search anymore
                      newOrder.put(previous.fileId + path, previous);
                    } else {
                      newOrder.put(ae.getKey(), ae.getValue());
                    }
                  }
                  accepted = newOrder;
                } else if (FileChange.UPDATE_CONTENT.equals(change.changeType)) {
                  if (FileChange.UPDATE.equals(prevChange)) {
                    // ignore previous update and use this content update (it
                    // also should update metadata)
                    skipped.add(accepted.remove(path));
                  } else if (FileChange.CREATE.equals(prevChange)) {
                    // skip content update for just created, actual content will
                    // be used by the creation
                    skipped.add(change);
                    continue;
                  }
                }
              }
              // handle copy of folders here: skip sub-tree
              if (FileChange.CREATE.equals(change.changeType) && change.fileId != null) {
                // creation and already cloud file - it's copy inside the drive,
                String copiedParent = null;
                for (String copyPath : copied) {
                  if (path.length() > copyPath.length() && path.startsWith(copyPath)) {
                    copiedParent = copyPath;
                    break;
                  }
                }
                if (copiedParent != null) {
                  // skip sub-tree of already accepted for copy node
                  skipped.add(change);
                  continue;
                } else {
                  copied.add(path);
                }
              }
              // otherwise accept:
              // * if REMOVE then UPDATE, not possible in JCR
              accepted.put(path, change);
            } else {
              // else - ignore the change
              skipped.add(change);
            }
          } catch (AccessDeniedException e) {
            // special logic for a case when drive/file was moved to eXo Trash
            // during the sync preparation
            if (change != null && change.node != null && isInTrash(change.node)) {
              skipped.add(change);
            } else {
              throw e;
            }
          } catch (PathNotFoundException e) {
            if (change.changeType.equals(FileChange.REMOVE)) {
              // it is already removed - ignore it
              skipped.add(change);
              LOG.warn("Ignoring already removed item removal: " + change.fileId + " " + change.path, e);
            } else if (change.changeType.equals(FileChange.CREATE)) {
              // it was existing and need add to the cloud, but already removed locally - ignore it
              skipped.add(change);
              LOG.warn("Ignoring already removed item creation: " + change.path, e);
            } else if (change.changeType.equals(FileChange.UPDATE)) {
              Node existing = findNode(change.fileId);
              if (existing != null) {
                // file name change when fixNameConflict() was used and moved the node
                LOG.warn("Item already updated (file renamed) " + change.path + " belongs to " + existing.getPath()
                    + ". Change faced with this: " + e.getMessage());
              } else {
                // XXX workaround: may be it is a part of move
                LOG.warn("Item already removed (moved?) " + change.path + ". Change faced with this: " + e.getMessage());
              }
              skipped.add(change);
            } else if (e.getMessage().indexOf("/exo:thumbnails") > 0 && change.path.indexOf("/exo:thumbnails") > 0) {
              // XXX workaround: hardcode ignorance of exo:thumbnails here also, it's possible that
              // thumbnails' child nodes will disappear, thus we ignore them
              skipped.add(change);
            } else {
              throw e;
            }
          }
        }

        Set<String> ignoredPaths = new HashSet<String>(); // for not supported by sync

        next: for (Iterator<FileChange> chiter = accepted.values().iterator(); chiter.hasNext()
            && !Thread.currentThread().isInterrupted();) {
          FileChange change = chiter.next();
          String changePath = change.filePath;
          for (String ipath : ignoredPaths) {
            if (changePath.startsWith(ipath)) {
              skipped.add(change);
              continue next; // skip parts of ignored (not supported by sync) nodes
            }
          }

          boolean applying = true;
          int attempts = 0;
          // Do in loop to be able handle RetryLaterException
          do {
            applying = false;
            try {
              if (FileChange.UPDATE.equals(change.changeType)) {
                Node node = change.node;
                if (node != null && !node.isNew()) {
                  if (node.isNodeType(MIX_VERSIONABLE)) {
                    // XXX Dec 1, 2015 - we don't support versioned nodes for the moment
                    node.removeMixin(MIX_VERSIONABLE);
                  }
                }
              }
              change.apply();
              applied.add(change);
              if (FileChange.REMOVE.equals(change.changeType)) {
                addRemoved(change.filePath);
              } else {
                CloudFile cfile = change.file;
                if (cfile != null) {
                  addChanged(cfile);
                }
              }
            } catch (SyncNotSupportedException e) {
              // remember to skip sub-files, this exception handled by this
              ignoredPaths.add(changePath);
              skipped.add(change);
            } catch (SkipChangeException e) {
              // remember to skip sub-files and inform user, this exception
              // handled by this
              ignoredPaths.add(changePath);
              skipped.add(change);
              messages.add(new CloudDriveMessage(CloudDriveMessage.Type.WARN, e.getMessage()));
            } catch (RetryLaterException e) {
              // This logic is for retrying a single change at cloud side caused
              // by explicit need raised
              // by provider connector code.
              // It's not a CloudProviderException, which is a different
              // approach to retry a whole command in
              // AbstractCommand.exec() without using a provider's timeout (to
              // wait before a retry).
              // In this case we don't rollback the local node, thus
              // preparations (like name normalization
              // already in JCR session transient state.
              if (attempts > RETRY_ATTEMPTS_MAX) {
                throw e; // we already did retry maximum allowed times - throw
                         // the error higher
              } else {
                // wait and do next attempt in the loop
                attempts++;
                long timeout = e.getTimeout();
                if (timeout < 0 || timeout > RETRY_TIMEOUT_MAX) {
                  timeout = RETRY_TIMEOUT_MAX;
                } else if (timeout == 0) {
                  timeout = RETRY_TIMEOUT_DEFAULT;
                }
                LOG.warn("File change retry requested for [" + change.changeType + "] " + changePath + " in " + getName()
                    + " command of " + title() + ". " + e.getMessage() + ". Will run retry in: " + timeout + "ms.");
                Thread.sleep(timeout); // InterruptedException expected here
                applying = true;
              }
            } catch (PathNotFoundException e) {
              // XXX it is a copy of the catch from accept-loop above:
              // need study when this exception can be ignored in apply-loop
              if (LOG.isDebugEnabled()) {
                LOG.debug("Unexpected PathNotFoundException for " + changePath + ": " + e.getMessage());
              }
              if (change.changeType.equals(FileChange.REMOVE)) {
                // it is already removed - ignore it
                skipped.add(change);
                LOG.warn("[2] Ignoring already removed item removal: " + change.fileId + " " + changePath, e);
              } else if (change.changeType.equals(FileChange.CREATE)) {
                // it was existing and need add to the cloud, but already
                // removed locally - ignore it
                skipped.add(change);
                LOG.warn("[2] Ignoring already removed item creation: " + changePath, e);
              } else if (change.changeType.equals(FileChange.UPDATE)) {
                Node existing = findNode(change.fileId);
                if (existing != null) {
                  // file name change when fixNameConflict() was used and moved
                  // the node - ignore this change
                  skipped.add(change);
                  LOG.warn("[2] Item already updated (file renamed) " + changePath + " belongs to " + existing.getPath()
                      + ". Change faced with this: " + e.getMessage());
                }
              } else if (e.getMessage().indexOf("/exo:thumbnails") > 0 && changePath.indexOf("/exo:thumbnails") > 0) {
                // XXX hardcode ignorance of exo:thumbnails here also,
                // it's possible that thumbnails' child nodes will disappear,
                // thus we ignore them
                ignoredPaths.add(changePath);
                skipped.add(change);
              } else {
                throw e;
              }
            } catch (AccessDeniedException e) {
              // special logic for a case when drive/file was moved to eXo Trash
              // during the sync processing
              if (change != null && change.node != null && isInTrash(change.node)) {
                skipped.add(change);
              } else {
                throw e;
              }
            }
          } while (applying && !Thread.currentThread().isInterrupted());
        }

        // check before saving the result
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Files synchronization interrupted in " + title());
        }

        save(); // save the drive (data obtained from the cloud when submitted
                // local changes)

        // help GC
        accepted.clear();
        ignoredPaths.clear();
      } finally {
        // complete and clean updating after drive node save
        for (String key : updating) {
          removeUpdating(key);
        }
        updating.clear();

        // move messages to global queue
        if (messages.size() > 0) {
          syncFilesMessages.addAll(messages);
          messages.clear();
        }
      }
    }
  }

  /**
   * Basic implementation of CloudFileAPI support.
   */
  protected abstract class AbstractFileAPI implements CloudFileAPI {

    /**
     * Root path.
     *
     * @return the string
     * @throws DriveRemovedException the drive removed exception
     * @throws RepositoryException the repository exception
     */
    protected String rootPath() throws DriveRemovedException, RepositoryException {
      return rootNode().getPath();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFolder(Node node) throws RepositoryException {
      return node.isNodeType(ECD_CLOUDFOLDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFile(Node node) throws RepositoryException {
      return node.isNodeType(ECD_CLOUDFILE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFileResource(Node node) throws RepositoryException {
      return node.isNodeType(ECD_CLOUDFILERESOURCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDrive(Node node) throws RepositoryException {
      return node.isNodeType(ECD_CLOUDDRIVE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnored(Node node) throws RepositoryException {
      return node.isNodeType(ECD_IGNORED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignore(Node node) throws RepositoryException {
      if (node.isNodeType(ECD_IGNORED)) {
        return false;
      } else {
        node.addMixin(ECD_IGNORED);
        return true;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unignore(Node node) throws RepositoryException {
      if (node.isNodeType(ECD_IGNORED)) {
        node.removeMixin(ECD_IGNORED);
        return true;
      } else {
        return false;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:id").getString();
    }

    /**
     * Sets the id.
     *
     * @param fileNode the file node
     * @param id the id
     * @throws RepositoryException the repository exception
     */
    protected void setId(Node fileNode, String id) throws RepositoryException {
      fileNode.setProperty("ecd:id", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("exo:title").getString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentId(Node fileNode) throws RepositoryException {
      Node parent = fileNode.getParent();
      return parent.getProperty("ecd:id").getString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthor(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:author").getString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLastUser(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:lastUser").getString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Calendar getCreated(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:created").getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Calendar getModified(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:modified").getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Node fileNode) throws RepositoryException {
      return fileNode.getProperty("ecd:type").getString();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> findParents(String fileId) throws DriveRemovedException, RepositoryException {
      Set<String> parentIds = new LinkedHashSet<String>();
      for (Node fn : findNodes(Arrays.asList(fileId))) {
        Node p = fn.getParent(); // parent it is a cloud file or a cloud drive
        parentIds.add(fileAPI.getId(p));
      }
      return Collections.unmodifiableCollection(parentIds);
    }

    /**
     * Find parent nodes.
     *
     * @param fileId the file id
     * @return the collection
     * @throws DriveRemovedException the drive removed exception
     * @throws RepositoryException the repository exception
     */
    protected Collection<Node> findParentNodes(String fileId) throws DriveRemovedException, RepositoryException {
      Set<Node> parents = new LinkedHashSet<Node>();
      for (Node fn : findNodes(Arrays.asList(fileId))) {
        Node p = fn.getParent(); // parent it is a cloud file or a cloud drive
        parents.add(p);
      }
      return Collections.unmodifiableCollection(parents);
    }
  }

  /**
   * Cloud File change. It is used for keeping a file change request and
   * applying it in current thread. File change differs to a {@link Command}
   * that the command offers public API with an access to the command state and
   * progress. A command also runs asynchronously in a dedicated thread. The
   * file change is for internal use (can be used within a command).
   */
  protected class FileChange {

    /** The Constant REMOVE. */
    public static final String            REMOVE                = "D";

    /** The Constant CREATE. */
    public static final String            CREATE                = "A";

    /** The Constant UPDATE. */
    public static final String            UPDATE                = "U";

    /**
     * Used internally in {@link FileChange} only as result of UPDATE of a file
     * content property.
     */
    public static final String            UPDATE_CONTENT        = "C";

    /**
     * Maximum allowed number of attempts for a file change to try fix a
     * conflict resolution. As conflict resolution based on file name change
     * (incremental), this means that only first {@value #CONFLICT_ATTEMPTS_MAX}
     * rename indexes will be possible. See fixNameConflict(node) for details.
     */
    public static final int               CONFLICT_ATTEMPTS_MAX = 100;

    /** The applied. */
    protected final CountDownLatch        applied               = new CountDownLatch(1);

    /** The is folder. */
    protected final boolean               isFolder;

    /** The path. */
    protected final String                path;

    /** The synchronizer. */
    protected final CloudFileSynchronizer synchronizer;

    /** The change type. */
    protected String                      changeType;

    /** The file path. */
    protected String                      filePath;

    /** The file id. */
    protected String                      fileId;

    /** The change id. */
    protected String                      changeId;

    /**
     * Referenceable file UUID for removal (optional for other operations).
     */
    protected String                      fileUUID;

    /**
     * Target file node. Should be initialized in worker thread.
     */
    protected Node                        node;

    /**
     * Cloud file produced by the change. Can be <code>null</code> for removal.
     */
    protected CloudFile                   file;

    /**
     * Constructor for newly observed change when file {@link Node} available in
     * the context. Used for file removals observed via
     * {@link CloudDriveManager} in high-level apps (ECMS and other).
     *
     * @param path the path
     * @param fileId the file id
     * @param isFolder the is folder
     * @param changeType the change type
     * @param synchronizer the synchronizer
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     */
    protected FileChange(String path, String fileId, boolean isFolder, String changeType, CloudFileSynchronizer synchronizer)
        throws CloudDriveException, RepositoryException {
      this.changeId = nextChangeId();
      this.path = path;
      this.fileId = fileId;
      this.isFolder = isFolder;
      this.changeType = changeType;
      this.synchronizer = synchronizer;
    }

    /**
     * Constructor for resumed change.
     *
     * @param changeId the change id
     * @param path the path
     * @param fileId the file id
     * @param isFolder the is folder
     * @param changeType the change type
     * @param synchronizer the synchronizer
     */
    protected FileChange(String changeId,
                         String path,
                         String fileId,
                         boolean isFolder,
                         String changeType,
                         CloudFileSynchronizer synchronizer) {
      this.changeId = changeId;
      this.path = path;
      this.fileId = fileId;
      this.isFolder = isFolder;
      this.changeType = changeType;
      this.synchronizer = synchronizer;
    }

    /**
     * File change with postponed decision for file synchronizer implementation.
     * It is used for observed creation and update events. See
     * {@link DriveChangesListener}.
     *
     * @param path the path
     * @param changeType the change type
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected FileChange(String path, String changeType) throws RepositoryException, CloudDriveException {
      this(path, null, false, changeType, null);
    }

    /**
     * Checks if is folder.
     *
     * @return the isFolder
     */
    public boolean isFolder() {
      return isFolder;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
      return path;
    }

    /**
     * Gets the synchronizer.
     *
     * @return the synchronizer
     */
    public CloudFileSynchronizer getSynchronizer() {
      return synchronizer;
    }

    /**
     * Gets the change type.
     *
     * @return the changeType
     */
    public String getChangeType() {
      return changeType;
    }

    /**
     * Gets the file path.
     *
     * @return the filePath
     */
    public String getFilePath() {
      return filePath;
    }

    /**
     * Gets the file id.
     *
     * @return the fileId
     */
    public String getFileId() {
      return fileId;
    }

    /**
     * Gets the change id.
     *
     * @return the changeId
     */
    public String getChangeId() {
      return changeId;
    }

    /**
     * Gets the file UUID.
     *
     * @return the fileUUID
     */
    public String getFileUUID() {
      return fileUUID;
    }

    /**
     * Gets the node.
     *
     * @return the node
     */
    public Node getNode() {
      return node;
    }

    /**
     * Gets the file.
     *
     * @return the file
     */
    public CloudFile getFile() {
      return file;
    }

    /**
     * Number of the change was applied (should not be more than 1).
     *
     * @return how many times the change applied
     */
    public long getApplied() {
      return applied.getCount();
    }

    /**
     * Sets the file UUID.
     *
     * @param fileUUID the new file UUID
     */
    void setFileUUID(String fileUUID) {
      this.fileUUID = fileUUID;
    }

    /**
     * Prepare and check if it should be accepted as a change in cloud drive:
     * find the file node if creation/update, ensure synchronizer available for
     * removal.
     *
     * @return true, if successful
     * @throws DriveRemovedException the drive removed exception
     * @throws CloudDriveException the cloud drive exception
     * @throws PathNotFoundException the path not found exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    boolean accept() throws DriveRemovedException,
                     CloudDriveException,
                     PathNotFoundException,
                     RepositoryException,
                     InterruptedException {
      if (REMOVE.equals(changeType)) {
        if (synchronizer == null) { // this check for a case, it should not be
                                    // null by the logic
          throw new SyncNotSupportedException("Synchronization not available for file removal: " + path);
        }
        this.filePath = path; // fileId should be already initialized for
                              // removal
        return true;
      } else {
        Session session = session();
        Item item = session.getItem(path); // reading in user session by initial
                                           // path
        Node node = null;
        if (item.isNode()) {
          // node added or changed
          node = (Node) item;
          if (fileAPI.isFile(node)) {
            // for creation/update of already cloud files, need check does it
            // belong to this drive
            if (rootUUID.equals(node.getProperty("ecd:driveUUID").getString())) {
              fileId = fileAPI.getId(node);
            } else {
              // this file should be ignored
              if (!fileAPI.isIgnored(node)) {
                LOG.warn("Cannot add or update file from other cloud drive " + path + ". Ignoring the file.");
                try {
                  fileAPI.ignore(node);
                } catch (Throwable t) {
                  LOG.error("Error ignoring file from other drive " + path, t);
                }
                return false;
              }
            }
          } else if (fileAPI.isFileResource(node)) {
            // skip file resources which will be handled within the file
            // it is an usecase for content update of existing cloud file from
            // local drive
            return false;
          }
        } else {
          // property changed: we support only updates, new properties will be
          // ignored
          Node parentNode = item.getParent();
          if (UPDATE.equals(changeType)) {
            if (fileAPI.isFile(parentNode)) {
              // file metadata update (date, creator etc)
              node = parentNode;
              fileId = fileAPI.getId(node);
            } else if (fileAPI.isFileResource(parentNode)) {
              // file content update
              // logic based on nt:file structure:
              // theNtFile/jcr:content/jcr:data
              // TODO detect content update more precisely (by exact property
              // name in synchronizer)
              parentNode = parentNode.getParent();
              if (fileAPI.isFile(parentNode)) {
                changeType = UPDATE_CONTENT;
                node = parentNode;
                fileId = fileAPI.getId(node);
              }
            }
          }
        }

        // work only with not ignored nodes
        if (node != null) {
          if (!fileAPI.isIgnored(node)) {
            this.node = node; // file node
            this.filePath = node.getPath(); // actual file path
            return true;
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Synchronization not available for ignored cloud item (" + changeType + "): " + path);
            }
          }
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Skip file node (" + changeType + "): " + path);
          }
        }
      }
      return false;
    }

    /**
     * Apply the change to target file.
     *
     * @throws DriveRemovedException the drive removed exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    void apply() throws DriveRemovedException, CloudDriveException, RepositoryException, InterruptedException {
      if (applied.getCount() > 0) {
        try {
          if (REMOVE.equals(changeType)) {
            // #1 if trash not supported - delete the file,
            // #2 trash otherwise;
            // #3 if trash not confirmed in time - delete the file finally
            if (fileAPI.isTrashSupported()) {
              trash();
            } else {
              remove();
              // if file was actually trashed in ECMS, we need remove it from
              // there
              FileTrashing confirmation = fileTrash.get(fileId);
              if (confirmation != null) {
                try {
                  confirmation.complete();
                } finally {
                  fileTrash.remove(fileId, confirmation);
                }
              }
            }
          } else if (node != null) {
            node = ensureOwned(node);
            try {
              if (CREATE.equals(changeType)) {
                if (fileId != null) {
                  // if fileId exists - it is already a cloud file
                  if (node.hasProperty("ecd:trashed")) {
                    untrash();
                  } else {
                    // it is a move or copy inside the drive...
                    // Find the srcFile location. Note that the same file can
                    // exists in several places
                    // on some drives (e.g. Google, CMIS), but actual source
                    // location has no big matter
                    // - we only need a source for a copy/move.
                    // In case of copy, while not synchronized, two files with
                    // the same ID exist locally.
                    Node srcFile = null;
                    String srcPath = fileCopies.remove(fileId);
                    if (srcPath != null) {
                      // we have exact "copy" request
                      try {
                        Item srcItem = node.getSession().getItem(srcPath); // reading
                                                                           // in
                                                                           // user
                                                                           // session
                        if (srcItem.isNode()) {
                          srcFile = (Node) srcItem;
                        } else {
                          LOG.warn("Copy's source path points to a Property " + srcPath);
                        }
                      } catch (PathNotFoundException e) {
                        LOG.warn("Copy source node not found: " + srcPath, e);
                      }
                    } else if (srcFile == null) {
                      // find any file with the same id, but different instance
                      for (Node n : findNodes(Arrays.asList(fileId))) {
                        if (!node.isSame(n)) {
                          srcFile = n;
                        }
                      }
                    }
                    if (srcFile == null) {
                      update();
                    } else {
                      copy(srcFile);
                    }
                  }
                } else {
                  create();
                }
              } else if (UPDATE.equals(changeType)) {
                update();
              } else if (UPDATE_CONTENT.equals(changeType)) {
                updateContent();
              }
            } catch (SyncNotSupportedException e) {
              // if sync not supported, it's not supported NT: ignore the node
              LOG.warn("Cannot synchronize cloud file (" + changeType + "): " + e.getMessage() + ". Ignoring the file.");
              try {
                fileAPI.ignore(node);
              } catch (Throwable t) {
                LOG.error("Error ignoring not a cloud item " + filePath, t);
              }
              throw e; // throw to upper code
            } catch (SkipSyncException e) {
              // skip this file (it can be a part of top level NT supported by
              // the sync)
            }
          } // else null file - file not recognized - skip it
          // mark this change as applied
          applied.countDown();
        } finally {
          complete();
        }
      }
    }

    // ******* internal *********

    /**
     * Wait for the change completion.
     *
     * @throws InterruptedException if this change thread was interrupted
     */
    private void await() throws InterruptedException {
      while (applied.getCount() > 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(">>>> Await " + filePath);
        }
        applied.await();
      }
    }

    /**
     * Wait for this file and its sub-tree changes in other threads, wait
     * exclusively to let the existing to finish and then set the lock do not
     * let a new to apply before or during this change.
     *
     * @throws InterruptedException if other tasks working with this file were
     *           interrupted
     */
    private void begin() throws InterruptedException {
      synchronized (fileChanges) {
        final String lockedPath = filePath;
        FileChange other = fileChanges.putIfAbsent(lockedPath, this);
        if (other != this) {
          other = fileChanges.put(lockedPath, this);
          if (other != this) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(">>> Waiting for " + other.filePath);
            }
            other.await();
            if (LOG.isDebugEnabled()) {
              LOG.debug("<<< Done for " + other.filePath);
            }
          }
        }
        for (FileChange c : fileChanges.values()) {
          if (c != this && c.filePath.startsWith(lockedPath)) {
            LOG.info(">>> Waiting for child " + c.filePath);
            c.await();
            LOG.info("<<< Done for child " + c.filePath);
          }
        }
      }
    }

    /**
     * Remove the lock set in {@link #begin()} method by removing this changed
     * file from fileChanges map.
     *
     * @throws PathNotFoundException the path not found exception
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    private void complete() throws PathNotFoundException, RepositoryException, CloudDriveException {
      fileChanges.remove(filePath, this);
    }

    /**
     * Removes the.
     *
     * @throws PathNotFoundException the path not found exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void remove() throws PathNotFoundException, CloudDriveException, RepositoryException, InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Remove file " + fileId + " " + filePath);
      }

      begin();

      try {
        synchronizer.remove(filePath, fileId, isFolder, fileAPI);
      } catch (NotFoundException e) {
        // file already doesn't exist remotely... could be removed by outside or
        // by error,
        // in any case it is not a reason to break the process here
        LOG.warn("File not found in cloud for file removal " + filePath + ". " + e.getMessage());
      } catch (ConstraintException e) {
        // local file cannot be removed remotely and we should keep local state
        // as before the removal -
        // finally we don't want break other changes and should omit this file
        // (or folder and its sub-tree).
        LOG.warn("Constraint violation while synchronizing cloud file removal: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file " + filePath);

        // Restore the file from cloud side.
        fileAPI.restore(fileId, filePath);

        // throw to the caller to add the file to ignored list in this
        // SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Removed file restored.", e);
      }

      if (fileUUID != null) {
        // remove also file links (e.g. ECMS symlinks)
        removeLinks(session(), fileUUID);
      }
    }

    /**
     * Trash.
     *
     * @throws PathNotFoundException the path not found exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void trash() throws PathNotFoundException, CloudDriveException, RepositoryException, InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trash file " + fileId + " " + filePath);
      }

      begin();

      FileTrashing confirmation;
      FileTrashing existing = fileTrash.putIfAbsent(fileId, confirmation = new FileTrashing());
      if (existing != null) {
        confirmation = existing; // will wait for already posted trash
                                 // confirmation
      } // else, will wait for just posted trash

      try {
        synchronizer.trash(filePath, fileId, isFolder, fileAPI);
      } catch (FileTrashRemovedException e) {
        // file was permanently deleted on cloud provider - remove it locally
        // also
        // indeed, thus approach may lead to removal of not the same file from
        // the Trash due to
        // same-name-siblings ordering in case of several files with the same
        // name trashed.
        confirmation.remove();
      } catch (NotFoundException e) {
        // file not found on the cloud side - remove it locally also
        confirmation.remove();
      } catch (ConstraintException e) {
        // local file cannot be trashed remotely and we should keep local state
        // as before the trashing -
        // finally we don't want break other changes and should omit this file
        // (or folder and its sub-tree).
        LOG.warn("Constraint violation while synchronizing cloud file trash: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file " + filePath);

        // Restore the file from cloud side.
        fileAPI.restore(fileId, filePath);

        // throw to the caller to add the file to ignored list in this
        // SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Removed file restored.", e);
      }

      try {
        confirmation.complete();
      } finally {
        fileTrash.remove(fileId, confirmation);
      }
    }

    /**
     * Untrash.
     *
     * @throws SkipSyncException the skip sync exception
     * @throws SyncNotSupportedException the sync not supported exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void untrash() throws SkipSyncException,
                           SyncNotSupportedException,
                           CloudDriveException,
                           RepositoryException,
                           InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Untrash file " + fileId + " " + filePath);
      }

      begin();

      // we do in a loop to handle name conflicts (by renaming the file)
      int attempts = 0;
      do {
        attempts++;
        try {
          file = synchronizer(node).untrash(node, fileAPI);
          filePath = file.getPath();
          node.setProperty("ecd:trashed", (String) null); // clean the marker
                                                          // set in
                                                          // AddTrashListener
          break;
        } catch (NotFoundException e) {
          // similar effect as for create: trashed file or its parent not found
          // in the cloud... it can be
          // already removed there and not yet synced
          LOG.warn("Trashed cloud file or its destination parent not found for untrashing " + filePath + ". " + e.getMessage());
          // remove node locally with its links, as for parent it should be
          // removed by the drive sync
          removeNode(node);
          break;
        } catch (ConflictException e) {
          // if untrash conflicted, it's the same name already in use and we
          // guess a new name for the the
          // untrashed item
          if (attempts <= CONFLICT_ATTEMPTS_MAX) {
            fixNameConflict(node);
          } else {
            fileAPI.restore(fileId, filePath);
            throw new SkipChangeException(e.getMessage() + ". Local file restored.", e);
          }
        } catch (ConstraintException e) {
          // we have constraint violation in cloud service, local file cannot be
          // untrashed
          // remotely and we should keep local state as before the update -
          // finally we don't want break
          // other changes and should omit this file (or folder and its
          // sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file untrash: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() + ". " : "") + "Restoring local file state " + filePath);

          // Restore the file from cloud side.
          // As result of restoration untrashed file can be removed locally to
          // reflect the remote drive -
          // it is not a data lost for an user, as his data in cloud already
          // properly represented locally.
          fileAPI.restore(fileId, filePath);

          // throw to the caller to add the file to ignored list in this
          // SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Drive state refreshed.", e);
        }
      } while (true);
    }

    /**
     * Update.
     *
     * @throws SkipSyncException the skip sync exception
     * @throws SyncNotSupportedException the sync not supported exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void update() throws SkipSyncException,
                          SyncNotSupportedException,
                          CloudDriveException,
                          RepositoryException,
                          InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Update file " + fileId + " " + filePath);
      }

      begin();

      // we do in a loop to handle name conflicts (by renaming the file)
      int attempts = 0;
      do {
        attempts++;
        try {
          file = synchronizer(node).update(node, fileAPI);
          filePath = file.getPath();
          break;
        } catch (ConflictException e) {
          // if update conflicted, it's the same name already in use and we
          // guess a new name for the updated
          if (attempts <= CONFLICT_ATTEMPTS_MAX) {
            fixNameConflict(node);
          } else {
            fileAPI.restore(fileId, filePath);
            throw new SkipChangeException(e.getMessage() + ". Local file restored.", e);
          }
        } catch (ConstraintException e) {
          // local file cannot be updated remotely and we should keep local
          // state as before the update -
          // finally we don't want break other changes and should omit this file
          // (or folder and its
          // sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file update. " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file state " + filePath);

          // Restore the file from cloud side.
          fileAPI.restore(fileId, filePath);

          // throw to the caller to add the file to ignored list in this
          // SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Local file restored.", e);
        }
      } while (true);
    }

    /**
     * Update content.
     *
     * @throws SkipSyncException the skip sync exception
     * @throws SyncNotSupportedException the sync not supported exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void updateContent() throws SkipSyncException,
                                 SyncNotSupportedException,
                                 CloudDriveException,
                                 RepositoryException,
                                 InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Update content of file " + fileId + " " + filePath);
      }

      begin();

      int attempts = 0;
      do {
        attempts++;
        try {
          file = synchronizer(node).updateContent(node, fileAPI);
          break;
        } catch (ConflictException e) {
          // XXX this should not happen in most of cases, but some cloud
          // providers can create file by direct
          // upload of a content (e.g. Dropbox) and so may raise such errors if
          // the name already exists
          // if update conflicted, it's the same name already in use and we
          // guess a new name for the updated
          if (attempts <= CONFLICT_ATTEMPTS_MAX) {
            fixNameConflict(node);
          } else {
            fileAPI.restore(fileId, filePath);
            throw new SkipChangeException(e.getMessage() + ". Local file content not synchronized.", e);
          }
        } catch (ConstraintException e) {
          // local file cannot be updated remotely and we should keep local
          // state as before the update -
          // finally
          // we don't want break other changes application and should omit this
          // file (or folder and its
          // sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file content update: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file state " + filePath);

          try {
            // If ID can be read - it's already a cloud file (and it's an
            // update)
            String localId = fileAPI.getId(node);
            // Restore the file from cloud side.
            fileAPI.restore(localId, filePath);
          } catch (PathNotFoundException e1) {
            // This file isn't yet a cloud file (it's file created), thus we
            // keep it as-is (so user will be
            // able manually "Push" it via menu.
          }

          // throw to the caller to add the file to ignored list in this
          // SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Local file content not synchronized.", e);
        }
      } while (true);
    }

    /**
     * Copy.
     *
     * @param srcFile the src file
     * @throws SkipSyncException the skip sync exception
     * @throws SyncNotSupportedException the sync not supported exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void copy(Node srcFile) throws SkipSyncException,
                                    SyncNotSupportedException,
                                    CloudDriveException,
                                    RepositoryException,
                                    InterruptedException {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Copy file " + fileId + " " + srcFile.getPath() + " -> " + filePath);
      }

      begin();

      // we do in a loop to handle name conflicts (by renaming the destFile)
      int attempts = 0;
      do {
        attempts++;
        try {
          file = synchronizer(node).copy(srcFile, node, fileAPI);
          filePath = file.getPath();
          break;
        } catch (NotFoundException e) {
          LOG.warn("Source or destination not found for cloud file copy " + filePath + ". " + e.getMessage());
          // remove node locally, as for destination parent or source it should
          // be removed by the drive sync
          removeNode(node);
          return;
        } catch (ConflictException e) {
          // if copy conflicted, it's the same name already in use and we guess
          // a new name for the updated
          if (attempts <= CONFLICT_ATTEMPTS_MAX) {
            fixNameConflict(node);
          } else {
            removeNode(node);
            throw new SkipChangeException(e.getMessage() + ". Locally copied file removed.", e);
          }
        } catch (ConstraintException e) {
          // local file cannot be copied remotely and we should remove
          // destination file locally - finally we
          // don't want break other changes and should omit this file (or folder
          // and its sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file copy: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Removing the copied file locally " + filePath);

          // we restore local state by removing just copied file
          removeNode(node);

          // throw to the caller to add the file to ignored list in this
          // SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Locally copied file removed.", e);
        }
      } while (true);

      // update file id to actual after a copy on cloud side
      fileId = file.getId();
    }

    /**
     * Creates the.
     *
     * @throws SkipSyncException the skip sync exception
     * @throws SyncNotSupportedException the sync not supported exception
     * @throws CloudDriveException the cloud drive exception
     * @throws RepositoryException the repository exception
     * @throws InterruptedException the interrupted exception
     */
    private void create() throws SkipSyncException,
                          SyncNotSupportedException,
                          CloudDriveException,
                          RepositoryException,
                          InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Create file " + filePath);
      }

      begin();

      // ensure we see other thread changes (e.g. if this node already removed)
      try {
        node.getIndex(); // FYI use refresh(true) will remove pending changes
      } catch (InvalidItemStateException e) {
        LOG.warn("Cannot create already removed file. " + e.getMessage());
        throw new SkipSyncException("Skip creation of already removed file. " + e.getMessage());
      }

      int attempts = 0;
      do {
        attempts++;
        try {
          file = synchronizer(node).create(node, fileAPI);
          filePath = file.getPath();
          break;
        } catch (NotFoundException e) {
          // parent not found in the cloud... it can be already removed there
          // and not yet synced
          LOG.warn("Parent not found cloud file creation " + filePath + ". " + e.getMessage());
          // remove node locally, as for parent it should be removed by the
          // drive sync
          removeNode(node);
          return;
        } catch (ConflictException e) {
          // if creation conflicted, it's the same name already in use and we
          // guess a new name for the file
          if (attempts <= CONFLICT_ATTEMPTS_MAX) {
            fixNameConflict(node);
          } else {
            throw new SkipChangeException(e.getMessage() + ". Local file cannot be synchronized.", e);
          }
        } catch (ConstraintException e) {
          // local node cannot be added remotely and can be removed/moved
          // locally by an user - finally we
          // don't
          // want break other changes and should omit this file (or folder and
          // its sub-tree).
          // we keep user node locally "as is", user later can try Push it to
          // the cloud manually
          LOG.warn("Constraint violation while synchronizing cloud file creation: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". File exists only locally " + filePath);

          // throw to the caller to add the file to ignored list in this
          // SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Local file cannot be synchronized.", e);
        }
      } while (true);

      // we can know the id after the sync
      fileId = file.getId();

      if (LOG.isDebugEnabled()) {
        LOG.debug("Created file " + fileId + " " + filePath);
      }
    }
  }

  /**
   * The Class DriveState.
   */
  protected class DriveState implements FilesState {
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getUpdating() {
      return Collections.unmodifiableCollection(updating.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUpdating(String fileIdOrPath) {
      return JCRLocalCloudDrive.this.isUpdating(fileIdOrPath);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNew(String fileIdOrPath) {
      return JCRLocalCloudDrive.this.isNew(fileIdOrPath);
    }
  }

  // *********** variables ***********

  /**
   * Support for JCR actions. To do not fire on synchronization (our own modif)
   * methods.
   */
  protected static final ThreadLocal<CloudDrive>          actionDrive         = new ThreadLocal<CloudDrive>();

  /** The Constant accentsConverter. */
  protected static final Transliterator                   accentsConverter    =
                                                                           Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  /** The root workspace. */
  protected final String                                  rootWorkspace;

  /** The repository. */
  protected final ManageableRepository                    repository;

  /** The session providers. */
  protected final SessionProviderService                  sessionProviders;

  /** The user. */
  protected final CloudUser                               user;

  /** The root UUID. */
  protected final String                                  rootUUID;

  /** The root node holder. */
  protected final ThreadLocal<SoftReference<Node>>        rootNodeHolder;

  /** The root system node holder. */
  protected final ThreadLocal<SoftReference<Node>>        rootSystemNodeHolder;

  /** The jcr listener. */
  protected final JCRListener                             jcrListener;

  /** The no connect. */
  protected final ConnectCommand                          noConnect           = new NoConnectCommand();

  /**
   * Currently active connect command. Used to control concurrency in Cloud
   * Drive.
   */
  protected final AtomicReference<ConnectCommand>         currentConnect      = new AtomicReference<ConnectCommand>(noConnect);

  /** The no sync. */
  protected final SyncCommand                             noSync              = new NoSyncCommand();

  /**
   * Currently active synchronization command. Used to control concurrency in
   * Cloud Drive.
   */
  protected final AtomicReference<SyncCommand>            currentSync         = new AtomicReference<SyncCommand>(noSync);

  /**
   * Delayed files synchronization for a case when file move guessed by
   * JCRListener.
   */
  protected final AtomicReference<SyncFilesCommand>       delayedSyncFiles    = new AtomicReference<SyncFilesCommand>();

  /**
   * Synchronization lock used by the whole drive sync and local files changes
   * synchronization.
   */
  protected final ReadWriteLock                           syncLock            = new ReentrantReadWriteLock(true);

  /**
   * File changes currently processing by the drive. Used for locking purpose to
   * maintain consistency.
   */
  protected final ConcurrentHashMap<String, FileChange>   fileChanges         = new ConcurrentHashMap<String, FileChange>();

  /**
   * Maintain file removal requests (direct removal in JCR).
   */
  protected final ThreadLocal<Map<String, FileChange>>    fileRemovals        = new ThreadLocal<Map<String, FileChange>>();

  /**
   * Maintain file copy requests (initiated by external apps).
   */
  protected final ConcurrentHashMap<String, String>       fileCopies          = new ConcurrentHashMap<String, String>();

  /**
   * Maintain file trashing requests (move to Trash folder).
   */
  protected final ConcurrentHashMap<String, FileTrashing> fileTrash           = new ConcurrentHashMap<String, FileTrashing>();

  /**
   * File changes already committed locally but not yet merged with cloud
   * provider. Format: FILE_ID = [CHANGE_DATA_1, CHANGE_DATA_2,...
   * CHANGE_DATA_N]
   */
  protected final ConcurrentHashMap<String, Set<String>>  fileHistory         = new ConcurrentHashMap<String, Set<String>>();

  /**
   * Path or/and Ids of currently synchronizing files with counter of how many
   * times it proceeds to sync. When counter become zero it should be removed.
   * Used for informational purpose (for UI etc).
   */
  protected final ConcurrentHashMap<String, AtomicLong>   updating            = new ConcurrentHashMap<String, AtomicLong>();

  /**
   * Current drive change id. It is the last actual identifier of a change
   * applied.
   */
  protected final AtomicLong                              currentChangeId     = new AtomicLong(-1);

  /**
   * Incrementing sequence generator for local file changes identification.
   */
  protected final AtomicLong                              fileChangeSequencer = new AtomicLong(1);

  /**
   * Managed queue of commands.
   */
  protected final ThreadExecutor                          workerExecutor      = ThreadExecutor.getInstance();

  /**
   * Environment for commands execution.
   */
  protected final CloudDriveEnvironment                   commandEnv          = new ExoJCREnvironment();

  /**
   * Synchronizers for file synchronization.
   */
  protected final Set<CloudFileSynchronizer>              fileSynchronizers   = new LinkedHashSet<CloudFileSynchronizer>();

  /**
   * Singleton of {@link CloudFileAPI}.
   */
  protected final CloudFileAPI                            fileAPI;

  /**
   * Node finder facade on actual storage implementation.
   */
  protected final NodeFinder                              finder;

  /**
   * Mime type resolver.
   */
  protected final ExtendedMimeTypeResolver                mimeTypes;

  /**
   * Messages generated by last executed {@link SyncFilesCommand} for next
   * command that will be returned to an user (next sync).
   */
  protected final Queue<CloudDriveMessage>                syncFilesMessages   = new ConcurrentLinkedQueue<CloudDriveMessage>();

  /**
   * Drive commands active currently {@link Command}. Used for awaiting the
   * drive readiness (not accurate, for tests or information purpose only).
   */
  protected final Queue<Command>                          driveCommands       = new ConcurrentLinkedQueue<Command>();

  /**
   * Default drive state. See {@link #getState()}.
   */
  protected DriveState                                    state               = new DriveState();

  /**
   * Title has special care. It used in error logs and an attempt to read
   * <code>exo:title</code> property can cause another
   * {@link RepositoryException}. Thus need it pre-cached in the variable and
   * try to read the <code>exo:title</code> property each time, but if not
   * successful use this one cached.
   */
  private String                                          titleCached;

  /**
   * Create JCR backed {@link CloudDrive}. This method used for both newly
   * connecting drives and ones loading from the JCR node. If storage error will
   * happen all pending changes will be rolled back before throwing the
   * exception.
   *
   * @param user the user
   * @param driveNode {@link Node} - existing node
   * @param sessionProviders {@link SessionProviderService}
   * @param finder the finder
   * @param mimeTypes the mime types
   * @throws CloudDriveException if error on cloud provider side happen
   * @throws RepositoryException if storage error happen.
   */
  protected JCRLocalCloudDrive(CloudUser user,
                               Node driveNode,
                               SessionProviderService sessionProviders,
                               NodeFinder finder,
                               ExtendedMimeTypeResolver mimeTypes)
      throws CloudDriveException, RepositoryException {

    this.user = user;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.mimeTypes = mimeTypes;

    Session session = driveNode.getSession();
    this.repository = (ManageableRepository) session.getRepository();
    this.rootWorkspace = session.getWorkspace().getName();

    this.fileAPI = createFileAPI();

    boolean existing;
    // ensure given node has required nodetypes
    if (driveNode.isNodeType(ECD_CLOUDDRIVE)) {
      // ensure this existing CD node is of the same remote drive
      ensureSame(user, driveNode);

      if (driveNode.hasProperty("exo:title")) {
        titleCached = driveNode.getProperty("exo:title").getString();
      }
      existing = true;
    } else {
      try {
        initDrive(driveNode);
        driveNode.save();
      } catch (RepositoryException e) {
        rollback(driveNode);
        throw e;
      } catch (RuntimeException e) {
        rollback(driveNode);
        throw e;
      }
      existing = false;
    }

    this.rootUUID = driveNode.getUUID();
    this.rootNodeHolder = new ThreadLocal<SoftReference<Node>>();
    this.rootNodeHolder.set(new SoftReference<Node>(driveNode));
    this.rootSystemNodeHolder = new ThreadLocal<SoftReference<Node>>();

    // add drive trash listener
    this.jcrListener = addJCRListener(driveNode);
    this.addListener(jcrListener.changesListener); // listen for errors here

    if (existing) {
      // load history of local changes
      loadHistory();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getProperty("exo:title").getString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLink() throws DriveRemovedException, NotConnectedException, RepositoryException {
    Node rootNode = rootNode(true);
    try {
      return rootNode.getProperty("ecd:url").getString();
    } catch (PathNotFoundException e) {
      if (rootNode.getProperty("ecd:connected").getBoolean()) {
        throw e;
      } else {
        throw new NotConnectedException("Drive not connected " + title());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getId() throws DriveRemovedException, NotConnectedException, RepositoryException {
    Node rootNode = rootNode(true);
    try {
      return rootNode.getProperty("ecd:id").getString();
    } catch (PathNotFoundException e) {
      if (rootNode.getProperty("ecd:connected").getBoolean()) {
        throw e;
      } else {
        throw new NotConnectedException("Drive not connected " + title());
      }
    }
  }

  /**
   * Gets the local user.
   *
   * @return the local user
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  public String getLocalUser() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getProperty("ecd:localUserName").getString();
  }

  /**
   * Gets the inits the date.
   *
   * @return the inits the date
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  public Calendar getInitDate() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getProperty("ecd:initDate").getDate();
  }

  /**
   * {@inheritDoc}
   */
  public String getPath() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getPath();
  }

  /**
   * {@inheritDoc}
   */
  public String getWorkspace() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getSession().getWorkspace().getName();
  }

  /**
   * Gets the connect date.
   *
   * @return the connect date
   * @throws DriveRemovedException the drive removed exception
   * @throws NotConnectedException the not connected exception
   * @throws RepositoryException the repository exception
   */
  public Calendar getConnectDate() throws DriveRemovedException, NotConnectedException, RepositoryException {
    if (isConnected()) {
      return rootNode(true).getProperty("ecd:connectDate").getDate();
    } else {
      throw new NotConnectedException("Drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command connect() throws CloudDriveException, RepositoryException {
    if (isConnected()) {
      // already connected
      return ALREADY_DONE;
    } else {
      ConnectCommand connect;
      if (currentConnect.compareAndSet(noConnect, connect = getConnectCommand())) {
        connect.start();
      } else {
        ConnectCommand existingConnect = currentConnect.get();
        if (existingConnect != noConnect) {
          connect = existingConnect; // use already active
        } else {
          connect.start();
        }
      }
      return connect;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile getFile(String path) throws DriveRemovedException,
                                        NotCloudDriveException,
                                        NotCloudFileException,
                                        NotYetCloudFileException,
                                        RepositoryException {
    Node driveNode = rootNode(true);
    // take symlinks in account
    Item target = finder.findItem(driveNode.getSession(), path);
    String nodePath = target.getPath();
    String drivePath = driveNode.getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      if (target.isNode()) {
        Node fileNode = fileNode((Node) target);
        if (fileNode != null) {
          return readFile(fileNode);
        } else {
          if (isNewOrUpdating(nodePath)) {
            throw new NotYetCloudFileException("Node '" + path + "' is creating in cloud but not yet a cloud file.");
          } else {
            throw new NotCloudFileException("Node '" + path + "' is not a cloud file.");
          }
        }
      } else {
        throw new NotCloudFileException("Item at path '" + path + "' is Property and cannot be treatet as cloud file.");
      }
    } else {
      if (nodePath.equals(drivePath)) {
        throw new NotCloudFileException("Item at path '" + path + "' is a drive root.");
      } else {
        throw new NotCloudDriveException("Item at path '" + path + "' does not belong to Cloud Drive '" + title() + "'");
      }
    }
  }

  /**
   * Checks for file.
   *
   * @param path the path
   * @return true, if successful
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  @Override
  public boolean hasFile(String path) throws DriveRemovedException, RepositoryException {
    Node driveNode = rootNode(true);
    try {
      // take symlinks in account
      Item target = finder.findItem(driveNode.getSession(), path);
      String nodePath = target.getPath();
      String drivePath = driveNode.getPath();
      if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
        if (target.isNode()) {
          // here we check that the node is of cloud file type and not ignored
          return fileNode((Node) target) != null;
        }
      }
    } catch (PathNotFoundException | ItemNotFoundException e) {
      // PathNotFoundException: file not found at the given path
      // ItemNotFoundException: symlink points to not existing node in the drive
      if (LOG.isDebugEnabled()) {
        LOG.debug("File not found in drive " + title() + ": " + path + ". " + e.getMessage());
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CloudFile> listFiles() throws DriveRemovedException, CloudDriveException, RepositoryException {
    return listFiles(rootNode(true));
  }

  // ****** CloudDriveStorage ******

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocal(Node node) throws RepositoryException, DriveRemovedException {
    String nodePath = node.getPath();
    String drivePath = rootNode(true).getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      return !fileAPI.isFile(node) && !fileAPI.isIgnored(node) && !isUpdating(nodePath);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isIgnored(Node node) throws RepositoryException,
                                      NotCloudDriveException,
                                      NotCloudFileException,
                                      DriveRemovedException {
    String nodePath = node.getPath();
    String drivePath = rootNode(true).getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      return fileAPI.isIgnored(node);
    } else {
      if (nodePath.equals(drivePath)) {
        throw new NotCloudFileException("Item at path " + nodePath + " is a drive root.");
      } else {
        throw new NotCloudDriveException("Not in cloud drive " + nodePath);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean ignore(Node node) throws RepositoryException,
                                   DriveRemovedException,
                                   NotCloudDriveException,
                                   NotCloudFileException {
    String nodePath = node.getPath();
    String drivePath = rootNode().getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      if (fileAPI.isFile(node)) {
        boolean res = fileAPI.ignore(node);
        if (res) {
          node.save(); // save ignore (mixin)
        }
        return res;
      } else {
        throw new NotCloudFileException("Not cloud file " + drivePath);
      }
    } else {
      throw new NotCloudDriveException("Not in cloud drive " + drivePath);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unignore(Node node) throws RepositoryException,
                                     NotCloudDriveException,
                                     DriveRemovedException,
                                     NotCloudFileException {
    String nodePath = node.getPath();
    String drivePath = rootNode().getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      if (fileAPI.isFile(node)) {
        boolean res = fileAPI.unignore(node);
        if (res) {
          node.save(); // save unignore (mixin)
        }
        return res;
      } else {
        throw new NotCloudFileException("Not cloud file " + drivePath);
      }
    } else {
      throw new NotCloudFileException("Not in cloud drive " + drivePath);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean create(Node node) throws RepositoryException,
                                   NotCloudDriveException,
                                   DriveRemovedException,
                                   CloudDriveException {
    String nodePath = node.getPath();
    String drivePath = rootNode().getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      boolean res;
      if (fileAPI.isFile(node)) {
        // already cloud file
        res = false;
      } else {
        if (fileAPI.isIgnored(node)) {
          res = false;
        } else if (!isUpdating(nodePath)) {
          // push file creation in the cloud from this node
          List<FileChange> changes = new ArrayList<FileChange>();
          changes.add(new FileChange(nodePath, FileChange.CREATE));
          new SyncFilesCommand(changes).start();
          res = true;
        } else {
          res = true; // already creating
        }
      }
      return res;
    } else {
      if (nodePath.equals(drivePath)) {
        throw new NotCloudFileException("Item at path " + nodePath + " is a drive root.");
      } else {
        throw new NotCloudDriveException("Not in cloud drive " + nodePath);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command getCurentCommand() {
    Command cmd = currentConnect.get();
    if (cmd == noConnect) {
      cmd = currentSync.get();
    }
    return cmd;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void await() throws ExecutionException, InterruptedException {
    for (Command c : driveCommands) {
      c.await();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <R> R localChange(Change<R> change) throws NotCloudDriveException,
                                             DriveRemovedException,
                                             RepositoryException,
                                             CloudDriveException {
    try {
      jcrListener.disable();
      return change.apply();
    } finally {
      jcrListener.enable();
    }
  }

  /**
   * {@inheritDoc}
   */
  public ContentReader getFileContent(String fileId) throws RepositoryException, CloudDriveException {
    // by default we don't support content reading from cloud provider
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public ContentReader getFilePreview(String fileId) throws RepositoryException, CloudDriveException {
    // by default we don't support file preview
    return null;
  }

  // ****** CloudDriveSecurity ******

  /**
   * {@inheritDoc}
   */
  @Override
  public void shareFile(Node fileNode, String... users) throws RepositoryException, CloudDriveException {
    throw new CloudDriveException("Sharing not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unshareFile(Node fileNode, String... users) throws RepositoryException, CloudDriveException {
    throw new CloudDriveException("Sharing not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharingSupported() {
    return false;
  }

  // ****** abstract ******

  /**
   * Factory method to create an actual implementation of {@link ConnectCommand}
   * command.
   *
   * @return {@link ConnectCommand} instance
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  protected abstract ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException;

  /**
   * Factory method to create an instance of {@link SyncCommand} command.
   *
   * @return {@link SyncCommand} instance
   * @throws DriveRemovedException the drive removed exception
   * @throws SyncNotSupportedException the sync not supported exception
   * @throws RepositoryException the repository exception
   */
  protected abstract SyncCommand getSyncCommand() throws DriveRemovedException, SyncNotSupportedException, RepositoryException;

  /**
   * Factory method to create a instance of {@link CloudFileAPI} supporting
   * exact cloud provider.
   *
   * @return {@link CloudFileAPI} instance
   * @throws DriveRemovedException the drive removed exception
   * @throws SyncNotSupportedException the sync not supported exception
   * @throws RepositoryException the repository exception
   */
  protected abstract CloudFileAPI createFileAPI() throws DriveRemovedException, SyncNotSupportedException, RepositoryException;

  // ********* internals ***********

  /**
   * Internal initialization of newly connecting drive node.
   *
   * @param rootNode a {@link Node} to initialize
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  protected void initDrive(Node rootNode) throws CloudDriveException, RepositoryException {
    rootNode.addMixin(ECD_CLOUDDRIVE);
    if (!rootNode.hasProperty("exo:title")) {
      // default title
      rootNode.setProperty("exo:title", titleCached = getUser().createDriveTitle());
    } else {
      titleCached = rootNode.getProperty("exo:title").getString();
    }

    rootNode.setProperty("ecd:connected", false);
    // know who actually initialized the drive
    rootNode.setProperty("ecd:localUserName", currentUserName());
    rootNode.setProperty("ecd:initDate", Calendar.getInstance());
    // FIXME how to store provider properly? need store its API version?
    rootNode.setProperty("ecd:provider", getUser().getProvider().getId());

    // dummy id and url here, actual will be set during the connect
    rootNode.setProperty("ecd:id", DUMMY_DATA);
    rootNode.setProperty("ecd:url", DUMMY_DATA);

    // set current format of the drive
    rootNode.setProperty(ECD_LOCALFORMAT, CURRENT_LOCALFORMAT);
  }

  /**
   * List files.
   *
   * @param parentNode the parent node
   * @return the list
   * @throws RepositoryException the repository exception
   */
  protected List<CloudFile> listFiles(Node parentNode) throws RepositoryException {
    List<CloudFile> files = new ArrayList<CloudFile>();
    NodeIterator fileNodes = parentNode.getNodes();
    while (fileNodes.hasNext()) {
      Node fileNode = fileNodes.nextNode();
      if (fileNode.isNodeType(ECD_CLOUDFILE)) {
        CloudFile local = readFile(fileNode);
        files.add(local);
        if (local.isFolder()) {
          files.addAll(listFiles(fileNode)); // traverse all drive recursive
        }
      }
    }
    return files;
  }

  /**
   * Disconnect drive connected to given node. This method doesn't check if the
   * node represents the drive root node. This method also doesn't fire
   * onDisconnect event to drive listeners.
   *
   * @param driveNode {@link Node}
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  private synchronized void disconnect(Node driveNode) throws CloudDriveException, RepositoryException {
    try {
      try {
        // save disconnected status immediately
        driveNode.setProperty("ecd:connected", false);
        driveNode.getProperty("ecd:connected").save(); // only this property

        // remove all existing cloud files
        for (NodeIterator niter = driveNode.getNodes(); niter.hasNext();) {
          // TODO this will not remove links of files in subtrees
          Node node = niter.nextNode();
          removeLinks(node);
          node.remove();
        }

        driveNode.save();
      } catch (RepositoryException e) {
        rollback(driveNode);
        throw e;
      } catch (RuntimeException e) {
        rollback(driveNode);
        throw e;
      }
    } catch (ItemNotFoundException e) {
      // it is already removed
      throw new DriveRemovedException("Drive '" + title() + "' was removed.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void disconnect() throws CloudDriveException, RepositoryException {
    // mark as disconnected and clean local storage
    if (isConnected()) {
      Node driveRoot = rootNode();
      disconnect(driveRoot);

      // finally fire listeners
      listeners.fireOnDisconnect(new CloudDriveEvent(getUser(), rootWorkspace, driveRoot.getPath()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command synchronize() throws SyncNotSupportedException, DriveRemovedException, CloudDriveException, RepositoryException {

    // if other sync in progress, use that process (as a current)
    // if no process, start a new sync process
    // if file sync in progress, wait for it and then start a new sync

    if (isConnected()) {
      // XXX real synchronization can be only issued by the drive owner, others
      // see the current state
      // It is not the best UX for shared folders as they can change the
      // structure but until the owner will
      // not open it - it will reflect the last synced state. Indeed more
      // efficient logic need to be
      // implemented respecting the cloud provider sharing capabilities
      // (possible in dedicated connectors).
      String currentUser = currentUserName();
      String driveOwner = rootNode(true).getProperty("ecd:localUserName").getString();
      if (driveOwner.equals(currentUser)) {
        refreshAccess();

        SyncCommand sync;
        if (!currentSync.compareAndSet(noSync, sync = getSyncCommand())) {
          synchronized (currentSync) {
            SyncCommand existingSync = currentSync.get();
            if (existingSync != noSync) {
              return existingSync; // return existing
            } else {
              currentSync.set(sync); // force created sync as current
            }
          }
        }

        // start the sync finally
        sync.start();

        return sync;
      } else {
        return currentSync.get();
      }
    } else {
      throw new NotConnectedException("Cloud drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isConnected() throws DriveRemovedException, RepositoryException {
    return rootNode(true).getProperty("ecd:connected").getBoolean();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDrive(Node node) throws DriveRemovedException, RepositoryException {
    Node driveNode = rootNode(true);
    if (driveNode.getSession().getWorkspace().getName().equals(node.getSession().getWorkspace().getName())) {
      return isSameDrive(node);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FilesState getState() throws DriveRemovedException, RefreshAccessException, CloudProviderException, RepositoryException {
    return state;
  }

  // ============== JCR impl specific methods ==============

  /**
   * Init cloud file removal as planned. Initialized file will be later used by
   * the drive listener when removal will be saved in JCR. This method created
   * for use from JCR pre-remove action. If node isn't a cloud file or already
   * ignored, this method will ignore it (has no effect).
   *
   * @param node {@link Node} a node representing a file in the drive.
   * @throws SyncNotSupportedException the sync not supported exception
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  protected void initRemove(Node node) throws SyncNotSupportedException, CloudDriveException, RepositoryException {
    // Note: this method also can be invoked via RemoveCloudFileAction on file
    // trashing in Trash service of
    // the ECMS

    // don't act on ignored files
    if (!fileAPI.isIgnored(node) && fileAPI.isFile(node)) {
      final String path = node.getPath();
      final String id = fileAPI.getId(node);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Init file removal " + id + " " + path);
      }

      FileChange remove = new FileChange(path, id, fileAPI.isFolder(node), FileChange.REMOVE, synchronizer(node));
      // remember UUID for links removal
      if (node.isNodeType(MIX_REFERENCEABLE)) {
        remove.setFileUUID(node.getUUID());
      }

      Map<String, FileChange> planned = fileRemovals.get();
      if (planned == null) {
        planned = new ConcurrentHashMap<String, FileChange>();
        fileRemovals.set(planned);
      }
      // we may replace something previous in the map, just ignore it and rely
      // on latest initialized
      planned.put(path, remove);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initCopy(Node file, Node destParent) throws RepositoryException, CloudDriveException {
    final String filePath = file.getPath();
    final String fileId = fileAPI.getId(file);
    fileCopies.put(fileId, filePath);
  }

  /**
   * Synchronizer.
   *
   * @param file the file
   * @return the cloud file synchronizer
   * @throws RepositoryException the repository exception
   * @throws SkipSyncException the skip sync exception
   * @throws SyncNotSupportedException the sync not supported exception
   */
  CloudFileSynchronizer synchronizer(Node file) throws RepositoryException, SkipSyncException, SyncNotSupportedException {
    for (CloudFileSynchronizer s : fileSynchronizers) {
      if (s.accept(file)) {
        return s;
      }
    }
    throw new SyncNotSupportedException("Synchronization not supported for file type " + file.getPrimaryNodeType().getName()
        + " in node " + file.getPath());
  }

  /**
   * Synchronizer.
   *
   * @param clazz the clazz
   * @return the cloud file synchronizer
   * @throws RepositoryException the repository exception
   * @throws SkipSyncException the skip sync exception
   * @throws SyncNotSupportedException the sync not supported exception
   */
  CloudFileSynchronizer synchronizer(Class<?> clazz) throws RepositoryException, SkipSyncException, SyncNotSupportedException {
    for (CloudFileSynchronizer s : fileSynchronizers) {
      if (clazz.isAssignableFrom(s.getClass())) {
        return s;
      }
    }

    if (LostRemovalSynchronizer.class.equals(clazz)) {
      return new LostRemovalSynchronizer();
    }

    throw new SyncNotSupportedException("Synchronizer cannot be found " + clazz.getName());
  }

  /**
   * Adds the changed.
   *
   * @param fileId the file id
   * @param changeType the change type
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  private void addChanged(String fileId, String changeType) throws RepositoryException, CloudDriveException {
    Set<String> changes = fileHistory.get(fileId);
    if (changes == null) {
      changes = new LinkedHashSet<String>();
      Set<String> existing = fileHistory.putIfAbsent(fileId, changes);
      if (existing != null) {
        changes = existing;
      }
    }
    changes.add(changeType + getChangeId());
  }

  /**
   * Checks for changed.
   *
   * @param fileId the file id
   * @param changeTypes the change types
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  private boolean hasChanged(String fileId, String... changeTypes) throws RepositoryException, CloudDriveException {
    Set<String> changes = fileHistory.get(fileId);
    if (changes != null) {
      final long changeId = getChangeId(); // last synchronized change in the
                                           // drive
      for (String changeType : changeTypes) {
        if (changes.contains(changeType + changeId)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks for updated.
   *
   * @param fileId the file id
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected boolean hasUpdated(String fileId) throws RepositoryException, CloudDriveException {
    return hasChanged(fileId, FileChange.UPDATE, FileChange.CREATE);
  }

  /**
   * Checks for removed.
   *
   * @param fileId the file id
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected boolean hasRemoved(String fileId) throws RepositoryException, CloudDriveException {
    return hasChanged(fileId, FileChange.REMOVE);
  }

  /**
   * Clean changed.
   *
   * @param fileId the file id
   * @param changeTypes the change types
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  private void cleanChanged(String fileId, String... changeTypes) throws RepositoryException, CloudDriveException {
    // FYI this only removes in runtime history, but drive's ecd:localHistory
    // property will contain
    // such changes - they'll be removed only when expired by next call of
    // commitChanges() method.
    Set<String> changes = fileHistory.get(fileId);
    if (changes != null) {
      final long changeId = getChangeId();
      for (String changeType : changeTypes) {
        changes.remove(changeType + changeId);
      }
      if (changes.size() == 0) {
        synchronized (changes) {
          if (changes.size() == 0) {
            fileHistory.remove(fileId);
          }
        }
      }
    }
  }

  /**
   * Clean updated.
   *
   * @param fileId the file id
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected void cleanUpdated(String fileId) throws RepositoryException, CloudDriveException {
    cleanChanged(fileId, FileChange.UPDATE, FileChange.CREATE);
  }

  /**
   * Clean removed.
   *
   * @param fileId the file id
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected void cleanRemoved(String fileId) throws RepositoryException, CloudDriveException {
    cleanChanged(fileId, FileChange.REMOVE);
  }

  /**
   * Next change id.
   *
   * @return the string
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected String nextChangeId() throws RepositoryException, CloudDriveException {
    Long driveChangeId = getChangeId();
    return driveChangeId.toString() + '-' + fileChangeSequencer.getAndIncrement();
  }

  /**
   * Save given file changes to the drive store of not yet applied local
   * changes.
   *
   * @param changes {@link List} of {@link FileChange}
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected synchronized void saveChanges(List<FileChange> changes) throws RepositoryException, CloudDriveException {
    // <CID>=<T><F><PATH_LEN><PATH><I<ID>><S<SYNC_CLASS>>\n...
    // where I<ID> and S<SYNC_CLASS> can be empty

    StringBuilder store = new StringBuilder();
    for (FileChange ch : changes) {
      store.append(ch.changeId);
      store.append('=');
      store.append(ch.changeType);
      store.append(ch.isFolder ? 'Y' : 'N');
      store.append(String.format("%010d", ch.path.length()));
      store.append(ch.path);
      if (ch.fileId != null) {
        if (ch.fileId.length() > 9999) {
          throw new CloudDriveException("File id too long (greater of 4 digits): " + ch.fileId);
        }
        store.append('I');
        store.append(String.format("%04d", ch.fileId.length()));
        store.append(ch.fileId);
      }
      if (ch.synchronizer != null) {
        store.append('S');
        store.append(ch.synchronizer.getClass().getName());
      }
      store.append('\n'); // store always ends with separator
    }
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) {
        store.insert(0, current);
      }
      localChanges.setValue(store.toString());
      localChanges.save();
    } catch (PathNotFoundException e) {
      // no local changes saved yet
      driveNode.setProperty("ecd:localChanges", store.toString());
      driveNode.save();
    }
  }

  /**
   * Commit given changes to the drive local history but omit skipped. These
   * changes also will be removed from the local changes (from previously saved
   * and not yet applied).
   *
   * @param changes {@link Collection} of {@link FileChange} changes to move
   *          from changes store to the history
   * @param skipped {@link Collection} of {@link FileChange} changes that should
   *          be removed from changes store but not added to the history
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   * @see #saveChanges(List)
   * @see #rollbackAllChanges()
   */
  protected synchronized void commitChanges(Collection<FileChange> changes,
                                            Collection<FileChange> skipped) throws RepositoryException, CloudDriveException {
    Node driveNode = rootNode();
    Long timestamp = System.currentTimeMillis();
    StringBuilder store = new StringBuilder();
    StringBuilder history = new StringBuilder();

    // remove already applied local changes and collect applied as history
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be greater of about 15
                                  // chars
        next: for (String ch : current.split("\n")) {
          if (ch.length() > 0) {
            for (FileChange fch : changes) {
              if (ch.startsWith(fch.changeId)) {
                // history changes prefixed with timestamp of commit for rotation
                history.append(timestamp.toString());
                history.append(':');
                history.append(ch); // reuse already formatted change record
                history.append('\n');
                continue next; // omit this change as it is already applied
              }
            }
            for (FileChange sch : skipped) {
              if (ch.startsWith(sch.changeId)) {
                continue next; // omit this change as it was skipped
              }
            }
            store.append(ch); // still keep this changes in local changes
            store.append('\n'); // store always ends with separator
          }
        }
      }
      localChanges.setValue(store.toString());
      localChanges.save();
    } catch (PathNotFoundException e) {
      // no local changes saved yet
      driveNode.setProperty("ecd:localChanges", store.toString());
      driveNode.save();
    }

    // save the local history of already applied changes
    try {
      StringBuilder currentHistory = new StringBuilder();
      Property localHistory = driveNode.getProperty("ecd:localHistory");
      String current = localHistory.getString();
      if (current.length() > 0) {
        String[] fchs = current.split("\n");
        for (int i = fchs.length > HISTORY_MAX_LENGTH ? fchs.length - HISTORY_MAX_LENGTH : 0; i > fchs.length; i++) {
          String ch = fchs[i];
          if (ch.length() > 0) {
            int cindex = ch.indexOf(':');
            try {
              long chTimestamp = Long.parseLong(ch.substring(0, cindex));
              if (timestamp - chTimestamp < HISTORY_EXPIRATION) {
                currentHistory.append(ch);
                currentHistory.append('\n');
              }
            } catch (NumberFormatException e) {
              throw new CloudDriveException("Error parsing change timestamp: " + ch, e);
            }
          }
        }
      }
      if (currentHistory.length() > 0) {
        history.insert(0, currentHistory.toString());
      }
      localHistory.setValue(history.toString());
      localHistory.save();
    } catch (PathNotFoundException e) {
      // no local history saved yet
      driveNode.setProperty("ecd:localHistory", history.toString());
      driveNode.save();
    }

    // store applied changes in runtime cache
    for (FileChange ch : changes) {
      if (ch.fileId != null) {
        addChanged(ch.fileId, ch.changeType);
      } else {
        LOG.warn("Cannot cache file change with null file id: " + ch.changeType + ", " + ch.path);
      }
    }
  }

  /**
   * Rollback (remove) all changes from the drive local changes store. This
   * method will save nothing to local history of already applied changes.<br>
   * This method should be used when we exactly know that all changes saved but
   * not committed to the history are not actual anymore, e.g. in case when we
   * do a full sync with remote side. Such changes can be a result of failed
   * {@link SyncFilesCommand} and rollback caused by a {@link SyncCommand}.
   *
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   * @see #saveChanges(List)
   * @see #commitChanges(Collection, Collection)
   * @see #rollbackAllChanges()
   */
  protected synchronized void rollbackAllChanges() throws RepositoryException, CloudDriveException {
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      localChanges.setValue(DUMMY_DATA); // empty string
      localChanges.save();
    } catch (PathNotFoundException e) {
      // no local changes saved yet
    }
  }

  /**
   * Return saved but not yet applied local changes in this drive.
   *
   * @return {@link List} of {@link FileChange}
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected List<FileChange> savedChanges() throws RepositoryException, CloudDriveException {
    // TODO read and return only those changes that aren't currently processing
    // by current SyncFilesCommand(s)

    List<FileChange> changes = new ArrayList<FileChange>();
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be longer of about 15
        for (String ch : current.split("\n")) {
          if (ch.length() > 0) {
            changes.add(parseChange(ch));
          }
        }
      }
    } catch (PathNotFoundException e) {
      // no local changes saved - do nothing
    }
    return changes;
  }

  /**
   * Check if given change is a stored local change in the drive (not yet
   * applied remotely).
   *
   * @param change the change
   * @return <code>true</code> if given change stored locally (not yet applied
   *         remotely), <code>false</code> otherwise
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected boolean hasChange(FileChange change) throws RepositoryException, CloudDriveException {
    // <CID>=<T><F><PATH_LEN><PATH><I<ID>><S<SYNC_CLASS>>\n...
    // where I<ID> and S<SYNC_CLASS> can be empty
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be longer of about 15
        return current.indexOf(change.changeId + "=") >= 0;
      }
    } catch (PathNotFoundException e) {
      // no local changes saved - no change
    }
    return false;
  }

  /**
   * Load committed history of the drive to runtime cache.
   *
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected void loadHistory() throws RepositoryException, CloudDriveException {
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localHistory");
      String current = localChanges.getString();
      if (current.length() > 0) {
        LOG.info("Loading local history of " + title());
        for (String ch : current.split("\n")) {
          if (ch.length() > 0) {
            loadChanged(ch);
          }
        }
      }
    } catch (PathNotFoundException e) {
      // no local changes saved - do nothing
    }
  }

  /**
   * Parses the change.
   *
   * @param ch the ch
   * @return the file change
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  private FileChange parseChange(String ch) throws CloudDriveException, RepositoryException {
    // <CID>=<T><F><PATH_LEN><PATH><I<ID>><S<SYNC_CLASS>>\n...
    // where I<ID> and S<SYNC_CLASS> can be empty

    int cindex = ch.indexOf('=');
    String changeId = ch.substring(0, cindex);

    String changeType = new String(new char[] { ch.charAt(++cindex) });
    boolean isFolder = ch.charAt(cindex++) == 'Y' ? true : false;

    String path;
    try {
      cindex++;
      int pathIndex = cindex + 10;
      int pathLen = Integer.parseInt(ch.substring(cindex, pathIndex));
      cindex = pathIndex + pathLen;
      path = ch.substring(pathIndex, cindex);
    } catch (NumberFormatException e) {
      throw new CloudDriveException("Cannot parse path from local changes: " + ch, e);
    }

    String fileId;
    if (cindex < ch.length() && ch.charAt(cindex) == 'I') {
      try {
        cindex++;
        int idIndex = cindex + 4; // 4 - ID len's len
        int idLen = Integer.parseInt(ch.substring(cindex, idIndex));
        cindex = idIndex + idLen;
        fileId = ch.substring(idIndex, cindex);
      } catch (NumberFormatException e) {
        throw new CloudDriveException("Cannot parse file id from local changes: " + ch, e);
      }
    } else {
      fileId = null;
    }

    Class<?> syncClass;
    if (cindex < ch.length() && ch.charAt(cindex) == 'S') {
      try {
        syncClass = Class.forName(ch.substring(cindex + 1)); // +1 for S
      } catch (ClassNotFoundException e) {
        LOG.warn("Cannot find stored synchronizer class from local changes: " + ch, e);
        syncClass = LostRemovalSynchronizer.class;
      }
    } else {
      syncClass = null;
    }

    return new FileChange(changeId, path, fileId, isFolder, changeType, syncClass != null ? synchronizer(syncClass) : null);
  }

  /**
   * Load changed.
   *
   * @param ch the ch
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  private void loadChanged(String ch) throws CloudDriveException, RepositoryException {
    int cindex = ch.indexOf('=');

    // skip changeId
    String changeType = new String(new char[] { ch.charAt(++cindex) });
    cindex++; // skip isFolder

    // skip path
    try {
      cindex++;
      int pathIndex = cindex + 10;
      int pathLen = Integer.parseInt(ch.substring(cindex, pathIndex));
      cindex = pathIndex + pathLen;
    } catch (NumberFormatException e) {
      throw new CloudDriveException("Cannot parse path from local changes: " + ch, e);
    }

    String fileId;
    if (cindex < ch.length() && ch.charAt(cindex) == 'I') {
      try {
        cindex++;
        int idIndex = cindex + 4; // 4 - ID len's len
        int idLen = Integer.parseInt(ch.substring(cindex, idIndex));
        cindex = idIndex + idLen;
        fileId = ch.substring(idIndex, cindex);
      } catch (NumberFormatException e) {
        throw new CloudDriveException("Cannot parse file id from local changes: " + ch, e);
      }

      // add changed to runtime cache
      if (fileId != null) {
        addChanged(fileId, changeType);
      } else {
        LOG.warn("Cannot load file change with null file id: " + ch);
      }
    }
  }

  /**
   * Sets the change id.
   *
   * @param id the new change id
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  protected void setChangeId(Long id) throws CloudDriveException, RepositoryException {
    // FIXME indeed, it was possible to get interrupted thread for Box
    // connector,
    // as result, drive change Id wasn't properly set and further work blocked.

    // save in persistent store
    saveChangeId(id);

    // maintain runtime cache and sequencer
    currentChangeId.set(id);
    fileChangeSequencer.addAndGet(1 - fileChangeSequencer.get()); // reset
                                                                  // sequencer
  }

  /**
   * Gets the change id.
   *
   * @return the change id
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected Long getChangeId() throws RepositoryException, CloudDriveException {
    Long id = currentChangeId.get();
    if (id < 0) {
      synchronized (currentChangeId) {
        id = currentChangeId.get();
        if (id < 0) {
          id = readChangeId();
        }
      }
    }
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isDrive(String workspace, String path, boolean includeFiles) throws DriveRemovedException,
                                                                                 RepositoryException {
    Node driveNode = rootNode(true);
    if (driveNode.getSession().getWorkspace().getName().equals(workspace)) {
      try {
        // take symlinks in account
        Item target = finder.findItem(driveNode.getSession(), path);
        if (target.isNode()) {
          Node node = (Node) target;
          if (isConnected()) {
            if (isSameDrive(node)) {
              return true;
            } else if (includeFiles) {
              // 22.05.2014 removed check: && fileNode(node) != null
              return node.getPath().startsWith(driveNode.getPath());
            }
          }
        }
      } catch (PathNotFoundException | ItemNotFoundException e) {
        // PathNotFoundException: file not found at the given path - it is not a
        // drive
        // ItemNotFoundException: symlink points to not existing node in the
        // drive - it is not a drive
        if (LOG.isDebugEnabled()) {
          LOG.debug("File not found in drive " + title() + ": " + path + ". " + e.getMessage());
        }
      }
    }
    return false;
  }

  /**
   * Checks if is same drive.
   *
   * @param anotherNode the another node
   * @return true, if is same drive
   * @throws RepositoryException the repository exception
   */
  protected boolean isSameDrive(Node anotherNode) throws RepositoryException {
    if (anotherNode.isNodeType(ECD_CLOUDDRIVE)) {
      // we cannot compare ecd:id here as they can be equal for some providers
      // (e.g. Box)
      return this.rootUUID.equals(anotherNode.getUUID());
    }
    return false;
  }

  /**
   * Return {@link Node} instance representing some cloud file, i.e. if given
   * Node is of acceptable type.
   *
   * @param node {@link Node}
   * @return {@link Node}
   * @throws RepositoryException the repository exception
   */
  protected Node fileNode(Node node) throws RepositoryException {
    Node fileNode = getOrCleanFileNode(node);
    if (fileNode != null) {
      return fileNode;
    } else {
      Node parent;
      if (fileAPI.isFileResource(node) && fileAPI.isFile(parent = node.getParent())) {
        return ensureOwned(parent);
      } else {
        return null;
      }
    }
  }

  /**
   * System session.
   *
   * @return the session
   * @throws LoginException the login exception
   * @throws NoSuchWorkspaceException the no such workspace exception
   * @throws RepositoryException the repository exception
   */
  protected Session systemSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      return ssp.getSession(rootWorkspace, repository);
    }
    throw new RepositoryException("Cannot get system session provider.");
  }

  /**
   * Session.
   *
   * @return the session
   * @throws LoginException the login exception
   * @throws NoSuchWorkspaceException the no such workspace exception
   * @throws RepositoryException the repository exception
   */
  protected Session session() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    if (sp != null) {
      return sp.getSession(rootWorkspace, repository);
    }
    throw new RepositoryException("Cannot get session provider.");
  }

  /**
   * Drive's storage root node opened in user session.
   *
   * @return {@link Node}
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  protected Node rootNode() throws DriveRemovedException, RepositoryException {
    return rootNode(false);
  }

  /**
   * Drive's storage root node opened in JCR session.
   *
   * @param systemSession {@link Boolean} if <code>true</code> then root node
   *          will be open in system session, <code>false</code> - in current
   *          user session.
   * @return {@link Node}
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  protected Node rootNode(boolean systemSession) throws DriveRemovedException, RepositoryException {
    if (systemSession) {
      SoftReference<Node> rootNodeRef = rootSystemNodeHolder.get();
      Node rootNode;
      if (rootNodeRef != null) {
        rootNode = rootNodeRef.get();
        if (rootNode != null && rootNode.getSession().isLive() && isPrivilegedUser(rootNode.getSession().getUserID())) {
          try {
            // FYI as more light alternative rootNode.getIndex() can be used to
            // force state check, but refresh is good for long living nodes (and
            // soft ref will do long live until memory will be available)
            // XXX Node.refresh(true) discards added mixin on the drive
            // sub-nodes (ecd:cloudFile on a file node)
            rootNode.getIndex();
            return rootNode;
          } catch (InvalidItemStateException e) {
            // probably root node already removed
            throw new DriveRemovedException("Drive " + title() + " was removed.", e);
          } catch (RepositoryException e) {
            // if JCR error - need new node instance
          }
        }
      }
      try {
        rootNode = systemSession().getNodeByUUID(rootUUID);
      } catch (ItemNotFoundException e) {
        // it is already removed
        throw new DriveRemovedException("Drive " + title() + " was removed.", e);
      }
      rootSystemNodeHolder.set(new SoftReference<Node>(rootNode));
      return rootNode;
    } else {
      SoftReference<Node> rootNodeRef = rootNodeHolder.get();
      Node rootNode;
      if (rootNodeRef != null) {
        rootNode = rootNodeRef.get();
        String currentUser = currentUserName();
        if (rootNode != null && rootNode.getSession().isLive() && currentUser != null
            && currentUser.equals(rootNode.getSession().getUserID())) {
          try {
            rootNode.getIndex(); // validate the state
            return rootNode;
          } catch (InvalidItemStateException e) {
            // probably root node already removed
            throw new DriveRemovedException("Drive " + title() + " was removed.", e);
          } catch (RepositoryException e) {
            // if JCR error - need new node instance
          }
        }
      }
      try {
        rootNode = session().getNodeByUUID(rootUUID);
      } catch (ItemNotFoundException e) {
        // it is already removed
        throw new DriveRemovedException("Drive " + title() + " was removed.", e);
      }
      rootNodeHolder.set(new SoftReference<Node>(rootNode));
      return rootNode;
    }
  }

  /**
   * Rollback all changes made to JCR node storage of the drive. Used in public
   * API methods.
   *
   * @param rootNode the root node
   */
  protected void rollback(Node rootNode) {
    try {
      // cleanup if smth goes wrong
      rootNode.refresh(false);
    } catch (RepositoryException e) {
      LOG.warn("Error rolling back the changes on drive '" + title() + "': " + e.getMessage());
    }
  }

  /**
   * Does rollback of drive Node changes and fire onError event to listeners.
   *
   * @param rootNode {@link Node}
   * @param error {@link Throwable}
   * @param commandName {@link String}
   */
  void handleError(Node rootNode, Throwable error, String commandName) {
    String rootPath = null;
    if (rootNode != null) {
      try {
        rootPath = rootNode.getPath();
      } catch (RepositoryException e) {
        LOG.warn("Error reading drive root '" + e.getMessage() + "' "
            + (commandName != null ? "of " + commandName + " command " : "") + "to listeners on Cloud Drive '" + title() + "':"
            + e.getMessage());
      }

      if (commandName.equals("connect")) {
        try {
          // XXX it's workaround to prevent NPE in JCR Observation
          removeJCRListener(rootNode.getSession());
        } catch (Throwable e) {
          LOG.warn("Error removing observation listener on connect error '" + error.getMessage() + "' " + " on Cloud Drive '"
              + title() + "':" + e.getMessage());
        }
      }

      rollback(rootNode);
    }

    try {
      listeners.fireOnError(new CloudDriveEvent(getUser(), rootWorkspace, rootPath), error, commandName);
    } catch (Throwable e) {
      LOG.warn("Error firing error '" + error.getMessage() + "' " + (commandName != null ? "of " + commandName + " command " : "")
          + "to listeners on Cloud Drive '" + title() + "':" + e.getMessage());
    }
  }

  /**
   * Open node.
   *
   * @param fileId the file id
   * @param fileTitle the file title
   * @param parent the parent
   * @param nodeType the node type
   * @return the node
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  private Node openNode(String fileId, String fileTitle, Node parent, String nodeType) throws RepositoryException,
                                                                                       CloudDriveException {
    Node node;
    String baseName = nodeName(fileTitle);
    String name = baseName;
    String internalName = null;
    boolean titleTried = false;

    int siblingNumber = 1;
    do {
      try {
        node = parent.getNode(name);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already
        // extends the file NT
        if (fileAPI.isFile(node)) {
          if (fileId.equals(fileAPI.getId(node))) {
            // we found node...
            ensureOwned(node);
            break;
          } else {
            // find new name for the local file
            StringBuilder newName = new StringBuilder();
            newName.append(baseName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          }
        } else {
          // try cleanup this existing local node
          if (getOrCleanFileNode(node) == null && parent.hasNode(name)) {
            // node not cleaned, this node stays only local (creation in
            // progress or ignored),
            // and we need a new name for the file node
            StringBuilder newName = new StringBuilder();
            newName.append(baseName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          }
          // else, node cleaned and we try with current name one more time,
          // it should lead to PathNotFoundException and new node will be
          // created by the try-catch below
        }
      } catch (PathNotFoundException e) {
        if (internalName == null) {
          internalName = name;
          // try NodeFinder name (storage specific, e.g. ECMS)
          String finderName = finder.cleanName(fileTitle);
          // finder doesn't work with single symbol names when they are special
          // characters (like '.')
          if (finderName.length() > 1) {
            name = finderName;
            continue;
          }
        }
        if (!titleTried) {
          // Feb 17 2015: try by original title (usecase with '+' in name of
          // uploaded file and Box in PLF 4.1)
          titleTried = true;
          try {
            if (parent.hasNode(fileTitle)) {
              name = fileTitle;
              continue;
            }
          } catch (Throwable te) {
            // assume any error as not acceptable name: it may be
            // RepositoryException or
            // NumberFormatException (e.g. when name in form TEXT[TEXT]) as well
            // as other exceptions.
          }
        }

        // no such node exists, add it using internalName created by CD's
        // cleanName()
        node = parent.addNode(internalName, nodeType);
        break;
      }
    } while (true);

    return node;
  }

  /**
   * Next nasty thing here: ensure the node is owned by the drive user, it
   * MUST!.
   *
   * @param node {@link Node}
   * @return {@link Node} fixed node (same as in the given in the parameter)
   * @throws RepositoryException the repository exception
   */
  private Node ensureOwned(Node node) throws RepositoryException {
    // FIXME try avoid do this, or at least do this in single place (JCR
    // listener?)
    return IdentityHelper.ensureOwned(node, systemSession());
  }

  /**
   * Open file.
   *
   * @param fileId the file id
   * @param fileTitle the file title
   * @param parent the parent
   * @return the node
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected Node openFile(String fileId, String fileTitle, Node parent) throws RepositoryException, CloudDriveException {
    Node localNode = openNode(fileId, fileTitle, parent, NT_FILE);

    // create content for new not complete node
    if (localNode.isNew() && !localNode.hasNode("jcr:content")) {
      Node content = localNode.addNode("jcr:content", NT_RESOURCE);
      content.setProperty("jcr:data", DUMMY_DATA); // empty data by default
    }

    return localNode;
  }

  /**
   * Open folder.
   *
   * @param folderId the folder id
   * @param folderTitle the folder title
   * @param parent the parent
   * @return the node
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected Node openFolder(String folderId, String folderTitle, Node parent) throws RepositoryException, CloudDriveException {
    return openNode(folderId, folderTitle, parent, NT_FOLDER);
  }

  /**
   * Move file node with its subtree in scope of existing JCR session. If a node
   * already exists at destination and its id and title the same as given, then
   * move will not be performed and existing node will be returned.
   *
   * @param id {@link String} a file id of the Node
   * @param title {@link String} a new name of the Node
   * @param source {@link Node}
   * @param destParent {@link Node} a new parent
   * @return a {@link Node} from the destination
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected Node moveFile(String id, String title, Node source, Node destParent) throws RepositoryException, CloudDriveException {

    Node place = openNode(id, title, destParent, NT_FILE); // nt:file here, it
                                                           // will be removed
                                                           // anyway
    if (place.isNew() && !place.hasProperty("ecd:id")) {
      // this node was just created in openNode method, use its name as
      // destination name
      String nodeName = place.getName();
      removeLinks(place);
      place.remove(); // clean the place

      Session session = destParent.getSession();
      String destPath = destParent.getPath() + "/" + nodeName;
      session.move(source.getPath(), destPath);
      source.refresh(true);
      return source; // node will reflect a new destination
    } // else node with such id and title already exists at destParent

    return place;
  }

  /**
   * Copy file node with its subtree in scope of existing JCR session.
   *
   * @param node {@link Node}
   * @param destParent {@link Node}
   * @return a {@link Node} from the destination
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  protected Node copyFile(Node node, Node destParent) throws RepositoryException, CloudDriveException {
    // copy Node, with its ID and title
    String id = fileAPI.getId(node);
    String title = fileAPI.getTitle(node);
    Node place = openNode(id, title, destParent, NT_FILE); // nt:file here, it
                                                           // will be removed
                                                           // anyway
    if (place.isNew() && !place.hasProperty("ecd:id")) {
      // this node was just created in openNode method, use its name as
      // destination name
      // which already will have an index-suffix to deal with same-name siblings
      // on own way
      String nodeName = place.getName();
      removeLinks(place);
      place.remove(); // clean the place (just created or not cloud file (yet?))

      // FYI We add a node, don't make JCR copy of existing, to manage naming as
      // done by openNode() above
      Node nodeCopy = destParent.addNode(nodeName, node.getPrimaryNodeType().getName());
      for (NodeType mixin : node.getMixinNodeTypes()) {
        String mixinName = mixin.getName();
        if (!nodeCopy.isNodeType(mixinName)) { // check if not already set by
                                               // JCR actions
          nodeCopy.addMixin(mixin.getName());
        }
      }
      // copy its properties
      for (PropertyIterator piter = node.getProperties(); piter.hasNext();) {
        Property ep = piter.nextProperty();
        PropertyDefinition pdef = ep.getDefinition();
        if (!pdef.isProtected()) {
          // if not protected, copy it
          if (pdef.isMultiple()) {
            nodeCopy.setProperty(ep.getName(), ep.getValues());
          } else {
            nodeCopy.setProperty(ep.getName(), ep.getValue());
          }
        }
      }
      // copy child nodes
      for (NodeIterator niter = node.getNodes(); niter.hasNext();) {
        Node ecn = niter.nextNode();
        NodeDefinition ndef = ecn.getDefinition();
        if (!ndef.isProtected()) {
          // if not protected, copy it recursive
          copyFile(ecn, nodeCopy);
        }
      }
      return nodeCopy;
    } // else node with such id and title already exists at destParent
    return place;
  }

  /**
   * Read local nodes from the drive folder to a map by file Id. It's possible
   * that for a single Id we have several files (in different parents usually).
   * This can be possible when Cloud Drive provider supports linking or
   * tagging/labeling where tag/label is a folder (e.g. Google Drive).
   *
   * @param parent {@link Node}
   * @param nodes {@link Map} of {@link List} objects to fill with the parent's
   *          child nodes
   * @param deep boolean, if <code>true</code> read nodes recursive,
   *          <code>false</code> read only direct child nodes.
   * @throws RepositoryException if JCR error happen
   */
  protected void readNodes(Node parent, Map<String, List<Node>> nodes, boolean deep) throws RepositoryException {
    for (NodeIterator niter = parent.getNodes(); niter.hasNext();) {
      Node node = getOrCleanFileNode(niter.nextNode());
      if (node != null) {
        String fileId = fileAPI.getId(node);
        nodes.computeIfAbsent(fileId, k -> new ArrayList<>()).add(node);
        if (deep && fileAPI.isFolder(node)) {
          readNodes(node, nodes, deep);
        }
      } // else, node just cleaned (removed)
    }
  }

  /**
   * Read cloud file node from the given parent using the file title and its id.
   * Algorithm of this method similar to
   * {@link #openNode(String, String, Node, String)} but this method doesn't
   * create a node if it doesn't exist.
   *
   * @param parent {@link Node} parent
   * @param fileTitle {@link String}
   * @param fileId {@link String}, if <code>null</code> then first found by name
   *          will be returned
   * @return {@link Node}
   * @throws RepositoryException the repository exception
   */
  protected Node readNode(Node parent, String fileTitle, String fileId) throws RepositoryException {
    Node node;
    String baseName = nodeName(fileTitle);
    String name = baseName;
    String internalName = null;

    int siblingNumber = 1;
    do {
      try {
        node = parent.getNode(name);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already
        // extends the file NT
        if (fileAPI.isFile(node)) {
          if (fileId == null || fileId.equals(fileAPI.getId(node))) {
            break; // we found node
          } else {
            // find new name for the local file
            StringBuilder newName = new StringBuilder();
            newName.append(baseName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          }
        } else {
          // not a cloud file with this name
          LOG.warn("Not a cloud file under clodu drive folder: " + node.getPath());
          return null;
        }
      } catch (PathNotFoundException e) {
        if (internalName == null) {
          // try NodeFinder name (storage specific, e.g. ECMS)
          internalName = name;
          name = finder.cleanName(fileTitle);
          if (name.length() > 0) {
            continue;
          }
        }
        // no such node
        return null;
      }
    } while (true);

    return node;
  }

  /**
   * Find local node using JCR SQL query by file id. Note, it will search only
   * persisted nodes - not saved files cannot be found in JCR.
   *
   * @param id {@link String}
   * @return {@link Node}
   * @throws RepositoryException the repository exception
   * @throws DriveRemovedException the drive removed exception
   */
  protected Node findNode(String id) throws RepositoryException, DriveRemovedException {

    Node rootNode = rootNode();
    if (fileAPI.getId(rootNode).equals(id)) {
      return rootNode;
    } else {
      QueryManager qm = rootNode.getSession().getWorkspace().getQueryManager();
      Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE ecd:id='" + id + "' AND jcr:path LIKE '"
          + rootNode.getPath() + "/%'", Query.SQL);
      QueryResult qr = q.execute();
      NodeIterator nodes = qr.getNodes();
      if (nodes.hasNext()) {
        return ensureOwned(nodes.nextNode());
      }
    }
    return null;
  }

  /**
   * Find local nodes using JCR SQL query by array of file ids. Note, it will
   * search only persisted nodes - not saved files cannot be found in JCR.
   *
   * @param ids {@link Collection} of {@link String}
   * @return {@link Collection} of nodes
   * @throws RepositoryException the repository exception
   * @throws DriveRemovedException the drive removed exception
   */
  protected Collection<Node> findNodes(Collection<String> ids) throws RepositoryException, DriveRemovedException {

    Set<Node> res = new LinkedHashSet<Node>();

    Node rootNode = rootNode();
    if (ids.contains(fileAPI.getId(rootNode))) {
      res.add(rootNode);
    }

    QueryManager qm = rootNode.getSession().getWorkspace().getQueryManager();

    StringBuilder idstmt = new StringBuilder();
    for (Iterator<String> ii = ids.iterator(); ii.hasNext();) {
      String id = ii.next();
      idstmt.append("ecd:id='");
      idstmt.append(id);
      idstmt.append('\'');
      if (ii.hasNext()) {
        idstmt.append(" OR ");
      }
    }

    Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE " + idstmt + " AND jcr:path LIKE '" + rootNode.getPath()
        + "/%'", Query.SQL);
    QueryResult qr = q.execute();

    for (NodeIterator niter = qr.getNodes(); niter.hasNext();) {
      res.add(niter.nextNode());
    }

    return res;
  }

  /**
   * Read cloud file from given node for use outside the drive. Note that
   * returned {@link JCRLocalCloudFile} instance will not have set its node
   * instance.
   *
   * @param fileNode the file node
   * @return the JCR local cloud file
   * @throws RepositoryException the repository exception
   */
  protected JCRLocalCloudFile readFile(Node fileNode) throws RepositoryException {
    String title = fileAPI.getTitle(fileNode);
    boolean isFolder = fileNode.isNodeType(ECD_CLOUDFOLDER);
    String type = fileNode.getProperty("ecd:type").getString();
    String typeMode = isFolder ? null : mimeTypes.getMimeTypeMode(type, title);
    String link = link(fileNode);
    // folder has no preview/edit links by definition (we rely on ECMS doc
    // views)
    String previewLink = isFolder ? null : previewLink(type, fileNode);
    String editLink = isFolder ? null : editLink(link, type, fileNode);
    long size = size(fileNode);

    return new JCRLocalCloudFile(fileNode.getPath(),
                                 fileAPI.getId(fileNode),
                                 title,
                                 link,
                                 editLink,
                                 previewLink,
                                 thumbnailLink(fileNode),
                                 type,
                                 typeMode,
                                 fileAPI.getLastUser(fileNode),
                                 fileAPI.getAuthor(fileNode),
                                 fileAPI.getCreated(fileNode),
                                 fileAPI.getModified(fileNode),
                                 isFolder,
                                 size,
                                 fileNode,
                                 false);
  }

  /**
   * Init or update Cloud File structure on local JCR node.
   *
   * @param fileNode {@link Node}
   * @param title the title
   * @param id the id
   * @param type the type
   * @param link the link
   * @param previewLink the preview link
   * @param thumbnailLink the thumbnail link
   * @param author the author
   * @param lastUser the last user
   * @param created the created
   * @param modified the modified
   * @param size the size
   * @throws RepositoryException the repository exception
   */
  protected void initFile(Node fileNode,
                          String title,
                          String id,
                          String type,
                          String link,
                          String previewLink,
                          String thumbnailLink,
                          String author,
                          String lastUser,
                          Calendar created,
                          Calendar modified,
                          long size) throws RepositoryException {
    // ecd:cloudFile
    if (!fileNode.isNodeType(ECD_CLOUDFILE)) {
      fileNode.addMixin(ECD_CLOUDFILE);
      if (modified == null) { // we do default for new nodes
        modified = Calendar.getInstance();
      }
    }

    initCommon(fileNode, title, id, type, link, author, lastUser, created, modified);

    // File size
    fileNode.setProperty("ecd:size", size);

    // ecd:cloudFileResource
    Node content = fileNode.getNode("jcr:content");
    if (!content.isNodeType(ECD_CLOUDFILERESOURCE)) {
      content.addMixin(ECD_CLOUDFILERESOURCE);
    }

    // nt:resource
    content.setProperty("jcr:mimeType", type);
    content.setProperty("jcr:lastModified", modified);

    // optional properties, if null, ones will be removed by JCR core
    fileNode.setProperty("ecd:previewUrl", previewLink);

    // since 1.1.0-RC2 we use dedicated ecd:thumbnailUrl
    fileNode.setProperty("ecd:downloadUrl", (String) null);
    fileNode.setProperty("ecd:thumbnailUrl", thumbnailLink);
  }

  /**
   * Init or update Cloud Folder structure on local JCR node.
   *
   * @param localNode {@link Node}
   * @param id the id
   * @param title the title
   * @param type the type
   * @param link the link
   * @param author the author
   * @param lastUser the last user
   * @param created the created
   * @param modified the modified
   * @throws RepositoryException the repository exception
   */
  protected void initFolder(Node localNode,
                            String id,
                            String title,
                            String type,
                            String link,
                            String author,
                            String lastUser,
                            Calendar created,
                            Calendar modified) throws RepositoryException {
    // exo:cloudFolder
    if (!localNode.isNodeType(ECD_CLOUDFOLDER)) {
      localNode.addMixin(ECD_CLOUDFOLDER);
    }

    initCommon(localNode, id, title, type, link, author, lastUser, created, modified);
  }

  /**
   * Init or update Cloud File or Folder properties on local JCR node. This
   * method assumes all mixins are set on the local node.
   *
   * @param node {@link Node}
   * @param id the id
   * @param title the title
   * @param type the type
   * @param link the link
   * @param author the author
   * @param lastUser the last user
   * @param created the created
   * @param modified the modified
   * @throws RepositoryException the repository exception
   */
  protected void initCommon(Node node,
                            String id,
                            String title,
                            String type,
                            String link,
                            String author,
                            String lastUser,
                            Calendar created,
                            Calendar modified) throws RepositoryException {
    node.setProperty("exo:title", title);
    if (node.hasProperty("exo:name")) {
      node.setProperty("exo:name", title);
    }
    node.setProperty("ecd:id", id);
    node.setProperty("ecd:driveUUID", rootUUID);
    // we do tolerantly: set when value available, this also avoid removing
    // existing by a null value
    if (type != null) {
      node.setProperty("ecd:type", type);
    }
    if (link != null) {
      node.setProperty("ecd:url", link);
    }
    if (author != null) {
      node.setProperty("ecd:author", author);
    }
    if (lastUser != null) {
      node.setProperty("ecd:lastUser", lastUser);
    }
    if (created != null) {
      node.setProperty("ecd:created", created);
    }
    if (modified != null) {
      node.setProperty("ecd:modified", modified);
    }
    node.setProperty("ecd:synchronized", Calendar.getInstance());

    if (node.isNodeType(EXO_DATETIME)) {
      if (created != null) {
        node.setProperty("exo:dateCreated", created);
      }
      if (modified != null) {
        node.setProperty("exo:dateModified", modified);
      }
    }

    if (node.isNodeType(EXO_MODIFY)) {
      if (modified != null && lastUser != null) {
        node.setProperty("exo:lastModifiedDate", modified);
        node.setProperty("exo:lastModifier", lastUser);
      }
    }
  }

  /**
   * Internal access to Cloud Drive title without throwing an Exception.
   *
   * @return {@link String}
   */
  protected String title() {
    return titleCached;
  }

  /**
   * Add Observation listener to removal from parent and addition to the Trash.
   *
   * @param driveNode {@link Node}
   * @return NodeRemoveHandler
   * @throws RepositoryException the repository exception
   */
  protected JCRListener addJCRListener(Node driveNode) throws RepositoryException {
    JCRListener handler = new JCRListener(driveNode.getPath());
    ObservationManager observation = driveNode.getSession().getWorkspace().getObservationManager();
    observation.addEventListener(handler.removeListener,
                                 Event.NODE_REMOVED,
                                 driveNode.getParent().getPath(),
                                 false,
                                 null,
                                 null,
                                 false);
    observation.addEventListener(handler.trashListener,
                                 Event.NODE_ADDED,
                                 null,
                                 false,
                                 null,
                                 new String[] { EXO_TRASHFOLDER },
                                 false);
    // listen to supported NTs only
    Set<String> supported = new LinkedHashSet<String>();
    for (CloudFileSynchronizer s : fileSynchronizers) {
      for (String nt : s.getSupportedNodetypes()) {
        supported.add(nt);
      }
    }
    observation.addEventListener(handler.changesListener,
                                 Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, //
                                 driveNode.getPath(),
                                 true,
                                 null,
                                 supported.size() > 0 ? supported.toArray(new String[supported.size()]) : null,
                                 false);
    return handler;
  }

  /**
   * Remove Observation listeners for this cloud drive.
   *
   * @param session {@link Session}
   * @throws RepositoryException the repository exception
   */
  protected void removeJCRListener(Session session) throws RepositoryException {
    ObservationManager observation = session.getWorkspace().getObservationManager();
    observation.removeEventListener(jcrListener.removeListener);
    observation.removeEventListener(jcrListener.trashListener);
    observation.removeEventListener(jcrListener.changesListener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configure(CloudDriveEnvironment env, Collection<CloudFileSynchronizer> synchronizers) {
    commandEnv.chain(env);
    fileSynchronizers.addAll(synchronizers);
  }

  /**
   * Do nasty thing: remove predefined node (nodetypes) from the drive. This
   * method doesn't check if node belongs to the drive (it assumes it does). The
   * method also doesn't check if the node ignored (it assumes it doesn't).<br>
   * This method doesn't throw any exception: operation will be performed in
   * system session and saved immediately. In case of a problem it will be
   * logged to system log.
   *
   * @param node {@link Node}
   * @return boolean, <code>true</code> if node was removed, <code>false</code>
   *         otherwise
   */
  protected boolean cleanup(Node node) {
    try {
      // XXX hardcoded Thumbnails nodetypes
      if (node.isNodeType(EXO_THUMBNAILS) || node.isNodeType(EXO_THUMBNAIL)) {
        String path = node.getPath();
        Item cleanIt = systemSession().getItem(path);
        Node parent = cleanIt.getParent();
        cleanIt.remove();
        parent.save();
        LOG.info("Not a cloud file node removed from the drive: " + path);
        return true;
      }
    } catch (Throwable e) {
      LOG.error("Error removing not a cloud file node", e);
    }
    return false;
  }

  /**
   * Return given node if it describes connected cloud file. If it is not yet
   * cloud file or was just cleaned (removed) the method will return
   * <code>null</code>.
   *
   * @param node {@link Node} local node
   * @return {@link Node} local node or <code>null</code> if file cleaned
   *         (removed)
   * @throws RepositoryException if storage error happened
   */
  protected Node getOrCleanFileNode(Node node) throws RepositoryException {
    if (fileAPI.isFile(node)) {
      return ensureOwned(node);
    } else if (fileAPI.isIgnored(node)) {
      return null;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Not a cloud file detected " + node.getPath());
      }
      // we don't try convert node to cloud file -
      // this should be done by a drive sync command
      if (!cleanup(node)) {
        ensureOwned(node);
      }
    }
    return null;
  }

  /**
   * Fix name conflict by renaming to "fileName (INDEX).ext" where
   * <code>INDEX</code> will be incremented if such one already exists.
   *
   * @param file the file node
   * @throws RepositoryException the repository exception
   */
  protected void fixNameConflict(Node file) throws RepositoryException {
    Session session = file.getSession();
    Node parent = file.getParent();
    String title = fileAPI.getTitle(file);
    // define name and ext of the file
    String baseTitle, baseExt;
    String[] titleParts = title.split("\\.");
    if (titleParts.length >= 2) {
      baseTitle = titleParts[0];
      baseExt = titleParts[titleParts.length - 1];
    } else {
      baseTitle = title;
      baseExt = null;
    }
    // define already existing index (from previous fixes)
    int index;
    int openingParenthesesPos = baseTitle.lastIndexOf('(');
    int closingParenthesesPos = baseTitle.lastIndexOf(')');
    if (openingParenthesesPos > 0 && closingParenthesesPos > 0 && closingParenthesesPos > openingParenthesesPos) {
      try {
        index = Integer.valueOf(baseTitle.substring(++openingParenthesesPos, closingParenthesesPos));
        baseTitle = baseTitle.substring(0, openingParenthesesPos);
      } catch (NumberFormatException e) {
        index = 1;
      }
    } else {
      index = 1;
    }
    do {
      StringBuilder titleBuilder = new StringBuilder(baseTitle).append(" (").append(index++).append(')');
      if (baseExt != null) {
        titleBuilder.append('.').append(baseExt);
      }
      String newTitle = titleBuilder.toString();
      String newName = nodeName(newTitle);
      if (!parent.hasNode(newName)) {
        session.move(file.getPath(), new StringBuilder(parent.getPath()).append('/').append(newName).toString());
        file.setProperty("exo:title", newTitle);
        if (file.hasProperty("exo:name")) {
          file.setProperty("exo:name", newTitle);
        }
        break;
      }
    } while (true);
  }

  /**
   * Return provider specific link for a file preview. By default this method
   * will try read value of <code>ecd:previewUrl</code> property and if not such
   * property exists <code>null</code> will be returned. Actual connector
   * implementation may override this logic.
   *
   * @param type {@link String} file mime type or <code>null</code>
   * @param fileNode {@link String} cloud file node
   * @return String with a link should be used for file preview.
   * @throws RepositoryException the repository exception
   */
  protected String previewLink(String type, Node fileNode) throws RepositoryException {
    try {
      return fileNode.getProperty("ecd:previewUrl").getString();
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  /**
   * Return provider specific link for a file thumbnail. By default this method
   * will try read value of <code>ecd:thumbnailUrl</code> property and if not
   * such property exists <code>null</code> will be returned. Actual connector
   * implementation may override this logic.
   *
   * @param fileNode {@link String} cloud file node
   * @return String with a link should be used for file thumbnail.
   * @throws RepositoryException the repository exception
   */
  protected String thumbnailLink(Node fileNode) throws RepositoryException {
    String link;
    try {
      link = fileNode.getProperty("ecd:thumbnailUrl").getString();
    } catch (PathNotFoundException e) {
      try {
        // prior 1.1.0-RC2 we have used ecd:downloadUrl for thumbnails
        link = fileNode.getProperty("ecd:downloadUrl").getString();
      } catch (PathNotFoundException e1) {
        link = null;
      }
    }
    return link;
  }

  /**
   * Return provider specific link for file content download or access. By
   * default this method will try read value of <code>ecd:url</code> property
   * and if not such property exists {@link PathNotFoundException} will be
   * thrown. Actual connector implementation may override its logic.
   *
   * @param fileNode {@link String} existing link to a cloud file or
   *          <code>null</code> if editing not supported
   * @return String with a link should be used for file editing.
   * @throws PathNotFoundException the path not found exception
   * @throws RepositoryException the repository exception
   */
  protected String link(Node fileNode) throws PathNotFoundException, RepositoryException {
    return fileNode.getProperty("ecd:url").getString();
  }

  /**
   * Read edit link for a cloud file denoted by given node. By default edit link
   * is <code>null</code> - editing not supported.
   *
   * @param fileLink {@link String} file link, can be used to build edit link by
   *          the connector implementation
   * @param type {@link String} file mime type or <code>null</code>
   * @param fileNode {@link Node}
   * @return {@link String} an URL to edit cloud file or <code>null</code> if
   *         editing not supported
   * @throws RepositoryException the repository exception
   */
  protected String editLink(String fileLink, String type, Node fileNode) throws RepositoryException {
    return null;
  }

  /**
   * Read cloud file size from given node. If size not available then -1 will be
   * returned.
   *
   * @param fileNode {@link Node}
   * @return {@link Long} file size in bytes or -1 if size not available
   * @throws RepositoryException the repository exception
   */
  protected long size(Node fileNode) throws RepositoryException {
    try {
      return fileNode.getProperty("ecd:size").getLong();
    } catch (PathNotFoundException e) {
      return -1;
    }
  }

  /**
   * Checks if is updating.
   *
   * @param key the key
   * @return true, if is updating
   */
  protected boolean isUpdating(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() > 0;
    }
    return false;
  }

  /**
   * Checks if is new.
   *
   * @param key the key
   * @return true, if is new
   */
  protected boolean isNew(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() == 0;
    }
    return false;
  }

  /**
   * Checks if is new or updating.
   *
   * @param key the key
   * @return true, if is new or updating
   */
  protected boolean isNewOrUpdating(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() >= 0;
    }
    return false;
  }

  /**
   * Inits the updating.
   *
   * @param key the key
   * @return true, if successful
   */
  protected boolean initUpdating(String key) {
    AtomicLong existingCounter = updating.putIfAbsent(key, new AtomicLong(0));
    boolean res;
    if (existingCounter != null) {
      res = existingCounter.longValue() == 0;
    } else {
      res = true;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> initUpdating " + key + " " + res);
    }
    return res;
  }

  /**
   * Adds the updating.
   *
   * @param key the key
   * @return the long
   */
  protected long addUpdating(String key) {
    AtomicLong newCounter;
    AtomicLong existingCounter = updating.putIfAbsent(key, newCounter = new AtomicLong(1));
    long counter;
    if (existingCounter != null) {
      counter = existingCounter.incrementAndGet();
    } else {
      counter = newCounter.longValue();
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> addUpdating " + key + " " + counter);
    }
    return counter;
  }

  /**
   * Removes the updating.
   *
   * @param key the key
   * @return true, if successful
   */
  protected boolean removeUpdating(String key) {
    AtomicLong counter = updating.get(key);
    boolean res = false;
    if (counter != null) {
      if (counter.decrementAndGet() <= 0) {
        // FYI AtomicLong has no special equals() method, thus value will not be
        // checked the removal
        res = updating.remove(key, counter);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("<< removeUpdating " + key + " " + (counter != null ? counter.longValue() : ""));
    }
    return res;
  }

  /**
   * Check if given drive node is belong to this user already. This method
   * assumes that given node already of cloud drive nodetype (ecd:cloudDrive).
   *
   * @param user {@link String}
   * @param driveNode {@link Node}
   * @throws RepositoryException the repository exception
   * @throws CannotConnectDriveException the cannot connect drive exception
   */
  protected void ensureSame(CloudUser user, Node driveNode) throws RepositoryException, CannotConnectDriveException {
    try {
      boolean res = driveNode.getProperty("ecd:cloudUserId").getString().equals(getUser().getId())
          && driveNode.getProperty("ecd:cloudUserName").getString().equals(getUser().getUsername())
          && driveNode.getProperty("ecd:userEmail").getString().equals(getUser().getEmail());
      if (!res) {
        LOG.warn("Cannot connect drive. Node " + driveNode.getPath() + " was connected to another user/drive.");
        throw new CannotConnectDriveException("Node already initialized by another user " + driveNode.getName());
      }
    } catch (PathNotFoundException e) {
      // if something not found it's not fully initialized drive
      throw new CannotConnectDriveException("Mandatory drive property not found: " + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isInDrive(Node node) throws DriveRemovedException, RepositoryException {
    Node driveNode = rootNode(true);
    if (driveNode.getSession().getWorkspace().getName().equals(node.getSession().getWorkspace().getName())) {
      if (isSameDrive(node)) {
        return true;
      } else {
        String path = node.getPath();
        try {
          // take symlinks in account
          Item target = finder.findItem(node.getSession(), path);
          if (target.isNode()) {
            node = (Node) target;
            // 22.05.2014 removed check: && fileNode(node) != null
            return node.getPath().startsWith(driveNode.getPath());
          }
        } catch (PathNotFoundException | ItemNotFoundException e) {
          // PathNotFoundException: file not found at the given path - it is not
          // a drive
          // ItemNotFoundException: symlink points to not existing node in the
          // drive - it is not a drive
          if (LOG.isDebugEnabled()) {
            LOG.debug("File not found in drive " + title() + ": " + path + ". " + e.getMessage());
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if is in trash.
   *
   * @param node the node
   * @return true, if is in trash
   */
  protected boolean isInTrash(Node node) {
    try {
      String nodePath = node.getPath();
      Node nodeParent = systemSession().getItem(nodePath).getParent();
      if (nodeParent.isNodeType(EXO_TRASHFOLDER)) {
        // file already in eXo Trash - skip it
        if (LOG.isDebugEnabled()) {
          LOG.debug("File in Trash " + nodePath);
        }
        return true;
      }
    } catch (Throwable t) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error reading node caused check for Trash " + node);
      }
    }
    return false;
  }

  /**
   * Construct a cloud file node name from a file title. This method should be
   * used everywhere for cloud nodes creation or modification and reading.<br>
   * Connector implementation may override this logic when required (e.g. for
   * path based file IDs).
   *
   * @param title the title
   * @return String with cloud file node name
   */
  protected String nodeName(String title) {
    return cleanName(title);
  }

  // ============== abstract ==============

  /**
   * Read id of the latest cloud change applied to this drive. The id will be
   * read from the drive store, that is provider specific.
   *
   * @return {@link Long}
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  protected abstract Long readChangeId() throws CloudDriveException, RepositoryException;

  /**
   * Save last cloud change applied to this drive. The id will be written to the
   * drive store, that is provider specific.
   *
   * @param id {@link Long}
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  protected abstract void saveChangeId(Long id) throws CloudDriveException, RepositoryException;

  // ============== static ================

  /**
   * Make JCR compatible item name.
   *
   * @param name the name
   * @return String - JCR compatible name of local file
   */
  public static String cleanName(String name) {
    String str = accentsConverter.transliterate(name.trim());
    // the character ? seems to not be changed to d by the transliterate
    // function
    StringBuilder cleanedStr = new StringBuilder(str.trim());
    // delete special character
    if (cleanedStr.length() == 1) {
      char c = cleanedStr.charAt(0);
      if (c == '.' || c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
        // any -> _<NEXNUM OF c>
        cleanedStr.deleteCharAt(0);
        cleanedStr.append('_');
        cleanedStr.append(Integer.toHexString(c).toUpperCase());
      }
    } else {
      for (int i = 0; i < cleanedStr.length(); i++) {
        char c = cleanedStr.charAt(i);
        if (c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
          cleanedStr.deleteCharAt(i);
          cleanedStr.insert(i, '_');
        } else if (!(Character.isLetterOrDigit(c) || Character.isWhitespace(c) || c == '.' || c == '-' || c == '_')) {
          cleanedStr.deleteCharAt(i--);
        }
      }
    }
    return cleanedStr.toString().trim(); // finally trim also
  }

  /**
   * Block other JCR extension actions.
   *
   * @param drive {@link CloudDrive}
   */
  static void startAction(CloudDrive drive) {
    actionDrive.set(drive);
  }

  /**
   * Check if can run a JCR extension action.
   *
   * @param drive {@link CloudDrive}
   * @return {@code true} if action can be accepted for this thread
   */
  static boolean acceptAction(CloudDrive drive) {
    return drive != null && drive != actionDrive.get();
  }

  /**
   * Allow other JCR extension actions.
   */
  static void doneAction() {
    actionDrive.remove();
  }

  /**
   * Check if given node isn't a node in eXo Trash folder and throw
   * {@link DriveRemovedException} if it is.
   *
   * @param node {@link Node}
   * @throws RepositoryException the repository exception
   * @throws DriveRemovedException if given node in the eXo Trash.
   */
  public static void checkNotTrashed(Node node) throws RepositoryException, DriveRemovedException {
    if (node.getParent().isNodeType(EXO_TRASHFOLDER)) {
      throw new DriveTrashedException("Drive " + node.getPath() + " was moved to Trash.");
    }
  }

  /**
   * Migrate Cloud Drive root node naming from title based (PROVIDER_NAME -
   * user@email) to transliterated title (for ECMS compatibility). Note that
   * node will be saved during the migration: all transient changes will be
   * saved as well. If given node not a root of cloud drive this method will do
   * nothing.
   *
   * @param node {@link Node}
   * @throws RepositoryException the repository exception
   */
  public static void migrateName(Node node) throws RepositoryException {
    if (node.isNodeType(ECD_CLOUDDRIVE)) {
      // root node has a property (not exactly defined): ecd:localFormat. It is
      // a string with version of
      // format.
      boolean upgrade;
      try {
        double localFormat = node.getProperty(ECD_LOCALFORMAT).getDouble();
        if (localFormat == CURRENT_LOCALFORMAT) {
          upgrade = false;
        } else {
          // local format unknown
          LOG.warn("Local format unknown: " + localFormat + ". Supported format: " + CURRENT_LOCALFORMAT + " or lower.");
          upgrade = false;
        }
      } catch (PathNotFoundException e) {
        // property not found - format not defined, assuming old format 1.0
        upgrade = true;
      }

      if (upgrade) {
        // ******** upgrade from 1.0 to 1.1 ********
        // move root node to transliterated name, downgrade not supported
        String name = node.getName();
        Session session = node.getSession();
        try {
          session.move(node.getPath(), node.getParent().getPath() + '/' + cleanName(name));
          node.setProperty(ECD_LOCALFORMAT, CURRENT_LOCALFORMAT); // set current
                                                                  // version
          session.save();
        } catch (RepositoryException e) {
          try {
            session.refresh(false);
          } catch (RepositoryException re) {
            LOG.warn("Error rolling back the session during root node migration: " + re);
          }
          throw e;
        }
      }
    } else {
      LOG.warn("Not a Cloud Drive root node: " + node.getPath());
    }
  }

  /**
   * Current user name obtained from current {@link ConversationState}.
   *
   * @return String with user name or <code>null</code> if no current
   *         conversation state set
   */
  protected String currentUserName() {
    ConversationState cs = ConversationState.getCurrent();
    return cs != null ? cs.getIdentity().getUserId() : null;
  }

  /**
   * Removes all the links of node with given UUID.
   *
   * @param session the JCR session
   * @param fileUUID the file UUID
   * @throws RepositoryException
   * @throws AccessDeniedException
   * @throws ItemNotFoundException
   */
  protected void removeLinks(Session session, String fileUUID) throws ItemNotFoundException,
                                                               AccessDeniedException,
                                                               RepositoryException {
    for (Node linked : finder.findLinked(session, fileUUID)) {
      Node parent = ensureOwned(linked.getParent());
      ensureOwned(linked).remove();
      // save immediately: in case of future error (cloud or JCR), the file link
      // will be already removed
      parent.save();
    }
  }

  /**
   * Track the file links referenced its node in current JCR workspace (e.g.
   * ECMS symlinks). Call this method before the node removal and save. Actual
   * removal of the links will be performed by the caller.
   *
   * @param node the node
   * @throws ItemNotFoundException the item not found exception
   * @throws AccessDeniedException the access denied exception
   * @throws UnsupportedRepositoryOperationException the unsupported repository
   *           operation exception
   * @throws RepositoryException the repository exception
   */
  protected void removeLinks(Node node) throws ItemNotFoundException,
                                        AccessDeniedException,
                                        UnsupportedRepositoryOperationException,
                                        RepositoryException {
    if (node.isNodeType(MIX_REFERENCEABLE)) {
      // explicitly remove file links outside the drive
      removeLinks(node.getSession(), node.getUUID());
    }
  }

  /**
   * Removes the file node if it's not already ignored. This also will remove
   * links of the node if it's {@value #MIX_REFERENCEABLE}.
   *
   * @param node the node
   * @throws RepositoryException the repository exception
   */
  protected void removeNode(Node node) throws RepositoryException {
    if (!fileAPI.isIgnored(node)) {
      try {
        removeLinks(node);
        node.remove();
      } catch (PathNotFoundException e) {
        // already removed
      }
    }
  }

  /**
   * Extract parent path from a given.
   *
   * @param path the path
   * @return the string
   */
  protected String parentPath(String path) {
    if (path.isEmpty() || (path.length() == 1 && path.charAt(0) == '/')) {
      return null;
    }
    String parentPath;
    int parentEndIndex = path.lastIndexOf('/');
    if (parentEndIndex > 0 && parentEndIndex == path.length() - 1) {
      // for cases with ending slash (e.g. /my/path/ and we need /my parent)
      parentEndIndex = path.lastIndexOf('/', parentEndIndex - 1);
    }
    if (parentEndIndex > 0) {
      parentPath = path.substring(0, parentEndIndex);
    } else {
      // it's root node as parent: should not happen with drives in user folders
      parentPath = null;
    }
    return parentPath;
  }

  /**
   * Check if is privileged user (root or system account).
   *
   * @param userId the user id
   * @return true, if is privileged user
   */
  protected boolean isPrivilegedUser(String userId) {
    return IdentityHelper.SYSTEM_USER_ID.equals(userId) || IdentityHelper.ROOT_USER_ID.equals(userId);
  }
}
