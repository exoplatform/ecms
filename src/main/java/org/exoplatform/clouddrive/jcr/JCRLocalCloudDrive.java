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
package org.exoplatform.clouddrive.jcr;

import com.ibm.icu.text.Transliterator;

import org.exoplatform.clouddrive.BaseCloudDriveListener;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveAccessException;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveEnvironment;
import org.exoplatform.clouddrive.CloudDriveEvent;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudFileAPI;
import org.exoplatform.clouddrive.CloudFileSynchronizer;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.CommandPoolExecutor;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.SkipSyncException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;

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
import java.util.LinkedList;
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

/**
 * JCR storage for local cloud drive. Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRLocalCloudDrive.java 00000 Sep 13, 2012 pnedonosko $
 */
public abstract class JCRLocalCloudDrive extends CloudDrive {

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

  public static final String     EXO_DATETIME          = "exo:datetime";

  public static final String     EXO_MODIFY            = "exo:modify";

  public static final String     EXO_TRASHFOLDER       = "exo:trashFolder";

  public static final String     NT_FOLDER             = "nt:folder";

  public static final String     NT_FILE               = "nt:file";

  public static final String     NT_RESOURCE           = "nt:resource";

  public static final String     NT_UNSTRUCTURED       = "nt:unstructured";

  public static final String     ECD_LOCALFORMAT       = "ecd:localFormat";

  public static final double     CURRENT_LOCALFORMAT   = 1.1d;

  public static final String     DUMMY_DATA            = "";

  public static final String     USER_WORKSPACE        = "user.workspace";

  public static final String     USER_NODEPATH         = "user.nodePath";

  public static final String     USER_SESSIONPROVIDER  = "user.sessionProvider";

  /**
   * Command stub for not running or already done commands.
   */
  protected static final Command ALREADY_DONE          = new AlreadyDone();

  /**
   * Command for not processing commands.
   */
  static class AlreadyDone implements Command {

    final long time = System.currentTimeMillis();

    /**
     * @inherritDoc
     */
    @Override
    public int getProgress() {
      return COMPLETE;
    }

    /**
     * @inherritDoc
     */
    @Override
    public boolean isDone() {
      return true;
    }

    /**
     * @inherritDoc
     */
    @Override
    public Collection<CloudFile> getFiles() {
      return Collections.emptyList();
    }

    /**
     * @inherritDoc
     */
    @Override
    public Collection<String> getRemoved() {
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
    public void await() throws InterruptedException {
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
   * Perform actual removal of the drive from JCR on its move to the Trash. Initialize cloud files trashing
   * similarly as for removal.
   */
  public class JCRListener {
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
            LOG.warn("Cloud Drive '" + title() + "' node already removed directly from JCR: "
                + e.getMessage());
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

    class AddTrashListener implements EventListener {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        String userId = null; // for error messages
        try {
          Session session = systemSession();
          // List<FileChange> trashedFiles = new ArrayList<FileChange>();
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
                      LOG.info("Cloud Drive trashed " + path); // TODO cleanup
                    } catch (ItemNotFoundException e) {
                      // node already deleted
                      LOG.warn("Cloud Drive " + title() + " node already removed directly from JCR: "
                          + e.getMessage());
                      finishTrashed(session, initialRootPath);
                      continue;
                    }
                  }

                  String rootPath = driveRoot.getPath();
                  if (rootPath.equals(path)) {
                    added = true;
                  }
                  checkTrashed(driveRoot);
                } else if (node.isNodeType(ECD_CLOUDFILE)) {
                  // file trashed, but accept only this drive files (jcr listener will be fired for all
                  // existing drives)
                  if (rootUUID.equals(node.getProperty("ecd:driveUUID").getString())) {
                    LOG.info("Cloud item trashed " + path); // TODO cleanup
                    // trashedFiles.add(new FileChange(path, FileChange.TRASH));

                    // mark the file node to be able properly untrash it,
                    node.setProperty("ecd:trashed", true);
                    node.save();

                    // confirm file trashing (for FileChange NODE_REMOVED changes)
                    // TODO should be expirable, e.g. for 10min
                    // this change also happens on node reordering in Trash when untrashing the same name
                    CountDownLatch confirmation;
                    CountDownLatch existing = fileTrash.putIfAbsent(fileAPI.getId(node),
                                                                    confirmation = new CountDownLatch(1));
                    if (existing != null) {
                      existing.countDown(); // confirm already posted trash
                    } else {
                      confirmation.countDown(); // confirm just posted trash
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
          // if (trashedFiles.size() > 0) {
          // // run files sync in a dedicated command (thread),
          // // DriveChangesListener will listen for all errors.
          //
          // new SyncFilesCommand(trashedFiles).start();
          // }
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive " + title() + " item move to Trash event"
              + (userId != null ? " for user " + userId : ""), e);
        } catch (CloudDriveException e) {
          LOG.error("Error synchronizing cloud file(s) move to Trash", e);
        }
      }
    }

    class DriveChangesListener extends BaseCloudDriveListener implements EventListener {

      final ThreadLocal<Boolean> lock = new ThreadLocal<Boolean>();

      void disable() {
        lock.set(true);
      }

      void enable() {
        lock.set(false);
      }

      boolean enabled() {
        Boolean ready = lock.get();
        return ready == null || !ready.booleanValue();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        if (enabled()) {
          try {
            List<FileChange> changes = new ArrayList<FileChange>();
            Session session = session(); // user session
            while (events.hasNext()) {
              Event event = events.nextEvent();
              String eventPath = event.getPath();

              if (eventPath.endsWith("jcr:mixinTypes") || eventPath.endsWith("jcr:content")) {
                continue; // XXX hardcoded undesired system stuff to skip
              }

              if (event.getType() == Event.NODE_REMOVED) {
                LOG.info("Cloud file removed " + eventPath); // TODO cleanup
                // Removal should be initiated by initRemove() before this event happening and in the same
                // thread.
                // For direct JCR removals this should be done by RemoveCloudFileAction. Then this removal
                // will
                // succeed.
                // In case of move to Trash, it should be fully handled by AddTrashListener, it will run a
                // dedicated command for it.
                // If removal not initiated (in this thread), then it's
                // move/rename - it will be handled by NODE_ADDED below.

                // check if it is not a direct JCR remove (in this thread)
                Map<String, FileChange> removed = fileRemovals.get();
                if (removed != null) {
                  FileChange remove = removed.remove(eventPath);
                  if (remove != null) {
                    changes.add(remove);
                  }
                } // otherwise it's not removal (may be a move or ordering)
              } else {
                if (event.getType() == Event.NODE_ADDED) {
                  // TODO info -> debug
                  LOG.info("Cloud file added. User: " + event.getUserID() + ". Path: " + eventPath);
                  changes.add(new FileChange(eventPath, FileChange.CREATE));
                } else if (event.getType() == Event.PROPERTY_CHANGED) {
                  // TODO info -> debug
                  LOG.info("Cloud file property changed. User: " + event.getUserID() + ". Path: " + eventPath);
                  changes.add(new FileChange(eventPath, FileChange.UPDATE));
                } // otherwise, we skip the event
              }
            }

            if (changes.size() > 0) {
              new SyncFilesCommand(changes).start();
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
      public void onError(CloudDriveEvent event, Throwable error) {
        // act on errors only
        LOG.error("Error synchronizing drive " + title(), error);
      }
    }

    final String               initialRootPath;

    final RemoveDriveListener  removeListener;

    final AddTrashListener     addListener;

    final DriveChangesListener changesListener;

    volatile boolean           trashed = false;

    volatile boolean           added   = false;

    JCRListener(String initialRootPath) {
      this.initialRootPath = initialRootPath;
      this.removeListener = new RemoveDriveListener();
      this.addListener = new AddTrashListener();
      this.changesListener = new DriveChangesListener();
    }

    synchronized void checkTrashed(Node driveRoot) throws RepositoryException {
      if (trashed && added) {
        if (driveRoot.getParent().isNodeType(EXO_TRASHFOLDER)) {
          // drive in the Trash, so disconnect and remove it from the JCR
          Session session = driveRoot.getSession();
          try {
            startAction(JCRLocalCloudDrive.this);
            try {
              disconnect();
            } catch (Throwable e) {
              // error of disconnect - don't care much here
              LOG.error("Error disconnecting Cloud Drive " + title() + " before its removal. "
                            + e.getMessage(),
                        e);
            }

            finishTrashed(session, driveRoot.getPath());
          } finally {
            // ...just JCR
            driveRoot.remove();
            session.save();
            doneAction();
            LOG.info("Cloud Drive " + title() + " successfully removed from the Trash.");
          }
        }
      }
    }

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

    public void enable() {
      changesListener.enable();
    }

    public void disable() {
      changesListener.disable();
    }
  }

  /**
   * Asynchronous runner for {@link Command}.
   */
  protected class CommandCallable implements Callable<Command> {
    final AbstractCommand command;

    CommandCallable(AbstractCommand command) throws CloudDriveException {
      this.command = command;
    }

    @Override
    public Command call() throws Exception {
      commandEnv.prepare(command);
      try {
        command.exec();
        return command;
      } finally {
        // restore previous settings
        commandEnv.cleanup(command);
      }
    }
  }

  protected class ExoJCRSettings {
    final ConversationState conversation;

    final ExoContainer      container;

    ConversationState       prevConversation;

    ExoContainer            prevContainer;

    SessionProvider         prevSessions;

    ExoJCRSettings(ConversationState conversation, ExoContainer container) {
      this.conversation = conversation;
      this.container = container;
    }
  }

  /**
   * Setup environment for commands execution in eXo JCR Container.
   */
  protected class ExoJCREnvironment extends CloudDriveEnvironment {

    protected final Map<Command, ExoJCRSettings> config = new HashMap<Command, ExoJCRSettings>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Command command) throws CloudDriveException {
      ConversationState conversation = ConversationState.getCurrent();
      if (conversation == null) {
        throw new CloudDriveException("Error to " + getName() + " drive for user " + getUser().getEmail()
            + ". User identity not set.");
      }

      config.put(command, new ExoJCRSettings(conversation, ExoContainerContext.getCurrentContainer()));

      super.configure(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(Command command) throws CloudDriveException {
      ExoJCRSettings sessings = config.get(command);

      sessings.prevConversation = ConversationState.getCurrent();
      ConversationState.setCurrent(sessings.conversation);

      // set correct container
      sessings.prevContainer = ExoContainerContext.getCurrentContainerIfPresent();
      ExoContainerContext.setCurrentContainer(sessings.container);

      // set correct SessionProvider
      sessings.prevSessions = sessionProviders.getSessionProvider(null);
      SessionProvider sp = new SessionProvider(sessings.conversation);
      sessionProviders.setSessionProvider(null, sp);

      super.prepare(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup(Command command) throws CloudDriveException {
      super.cleanup(command);

      ExoJCRSettings sessings = config.get(command);

      ConversationState.setCurrent(sessings.prevConversation);
      ExoContainerContext.setCurrentContainer(sessings.prevContainer);
      SessionProvider sp = sessionProviders.getSessionProvider(null);
      sessionProviders.setSessionProvider(null, sessings.prevSessions);
      sp.close();
    }
  }

  /**
   * Basic command pattern.
   */
  protected abstract class AbstractCommand implements Command, CommandProgress {

    /**
     * Local files changed by the command.
     */
    protected final Queue<CloudFile>       changed          = new ConcurrentLinkedQueue<CloudFile>();

    /**
     * Local file paths deleted by the command.
     */
    protected final Queue<String>          removed          = new ConcurrentLinkedQueue<String>();

    /**
     * Target JCR node. Will be initialized in exec() method (in actual runner thread).
     */
    protected Node                         rootNode;

    /**
     * Progress indicator in percents.
     */
    protected final AtomicInteger          progressReported = new AtomicInteger();

    /**
     * Time of command start.
     */
    protected final AtomicLong             startTime        = new AtomicLong();

    /**
     * Time of command finish.
     */
    protected final AtomicLong             finishTime       = new AtomicLong();

    /**
     * Actually open item iterators. Used for progress indicator.
     */
    protected final List<ChunkIterator<?>> iterators        = new ArrayList<ChunkIterator<?>>();

    /**
     * Asynchronous execution support.
     */
    protected Future<Command>              async;

    protected ExoJCRSettings               settings;

    /**
     * Base command constructor.
     * 
     */
    protected AbstractCommand() {
    }

    /**
     * Processing logic.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void process() throws CloudDriveAccessException,
                                     CloudDriveException,
                                     RepositoryException;

    /**
     * Wait for other processes before the command execution in {@link #exec()}.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void waitProcess() throws CloudDriveException, RepositoryException;

    /**
     * Start command execution. If command will fail due to provider error, the execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the throwing an exception.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected void exec() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      waitProcess();

      startTime.set(System.currentTimeMillis());

      try {
        commandEnv.prepare(this);

        jcrListener.disable();

        startAction(JCRLocalCloudDrive.this);

        rootNode = rootNode(); // init in actual runner thread

        int attemptNumb = 0;
        while (true && !Thread.currentThread().isInterrupted()) {
          try {
            process();
            return;
          } catch (CloudProviderException e) {
            // in case of provider errors rollback and re-try an attempt
            if (getUser().getProvider().retryOnProviderError()) {
              attemptNumb++;
              if (attemptNumb > CloudDriveConnector.PROVIDER_REQUEST_ATTEMPTS) {
                handleError(rootNode, e, getName());
                throw e;
              } else {
                rollback(rootNode);
                try {
                  Thread.sleep(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT);
                } catch (InterruptedException ie) {
                  LOG.warn("Interrupted while waiting for a next attempt of " + getName() + ": "
                      + ie.getMessage());
                  Thread.currentThread().interrupt();
                }
                LOG.warn("Error running " + getName() + " command: " + e.getMessage()
                    + ". Rolled back and running next attempt.");
              }
            } else {
              handleError(rootNode, e, getName());
              throw e;
            }
          }
        }
      } catch (CloudDriveException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RepositoryException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RuntimeException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } finally {
        doneAction();
        jcrListener.enable();
        commandEnv.cleanup(this);
        finishTime.set(System.currentTimeMillis());
      }
    }

    /**
     * Start command execution asynchronously using {@link #exec()} method inside {@link CommandCallable}. Any
     * exception if happened will be thrown by resulting {@link Future}.
     * 
     * @return {@link Future} associated with this command.
     * @throws CloudDriveException if no ConversationState set in caller thread.
     */
    Future<Command> start() throws CloudDriveException {
      commandEnv.configure(this);
      return async = commandExecutor.submit(getName(), new CommandCallable(this));
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
          reported = current;
        } // else
          // progress cannot be smaller of already reported one
          // do nothing and wait for next portion of work done

        progressReported.set(reported);
        return reported;
      }
    }

    /**
     * @inherritDoc
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
  }

  /**
   * Connect command.
   */
  protected abstract class ConnectCommand extends AbstractCommand {

    /**
     * Connect command constructor.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected ConnectCommand() throws RepositoryException, DriveRemovedException {
      super();
    }

    /**
     * Fetch actual files from cloud provider to local JCR.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void fetchFiles() throws CloudDriveException, RepositoryException;

    @Override
    public String getName() {
      return "connect";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitProcess() {
      // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException {
      // fetch all files to local storage
      fetchFiles();

      // connected drive properties
      rootNode.setProperty("ecd:cloudUserId", getUser().getId());
      rootNode.setProperty("ecd:cloudUserName", getUser().getUsername());
      rootNode.setProperty("ecd:userEmail", getUser().getEmail());
      rootNode.setProperty("ecd:connectDate", Calendar.getInstance());

      // mark as connected
      rootNode.setProperty("ecd:connected", true);

      // and save the node
      rootNode.save();

      // fire listeners
      listeners.fireOnConnect(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()));
    }
  }

  protected final class NoConnectCommand extends ConnectCommand {

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
   * Synchronization processor for actual implementation of Cloud Drive and its storage.
   */
  protected abstract class SyncCommand extends AbstractCommand {
    /**
     * Existing locally files (single file can be mapped to several parents).
     */
    protected Map<String, List<Node>> nodes;

    @Override
    public String getName() {
      return "synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitProcess() throws CloudDriveException, RepositoryException {
      // wait for the drive files sync in process()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      syncLock.writeLock().lock(); // write-lock acquired exclusively by single threads (drive sync)
      try {
        syncFiles();

        // and save the drive node
        rootNode.save();
      } finally {
        currentSync.set(noSyncCommand); // clean current, see synchronize()
        syncLock.writeLock().unlock();
      }

      // fire listeners afterwards
      listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()));
    }

    /**
     * Synchronize files from cloud provider to local JCR.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void syncFiles() throws CloudDriveException, RepositoryException;

    /**
     * Traverse all child nodes from the drive's local {@link Node} to {@link #nodes} tree. Note that single
     * file can be mapped to several parents. <br>
     * This method and its resulting tree can be used in actual algorithms for merging of remote and local
     * changes. But call it "on-demand", only when have changes to merge, otherwise it will add undesired JCR
     * load (due to a whole subtree traversing).
     * 
     * @throws RepositoryException
     */
    protected void readLocalNodes() throws RepositoryException {
      Map<String, List<Node>> nodes = new LinkedHashMap<String, List<Node>>();
      String rootId = rootNode.getProperty("ecd:id").getString();
      List<Node> rootList = new ArrayList<Node>();
      rootList.add(rootNode);
      nodes.put(rootId, rootList);
      readNodes(rootNode, nodes, true);

      this.nodes = nodes;
    }
  }

  /**
   * A stub of Synchronization process meaning "no sync currently". Used in synchronize() method.
   */
  protected final class NoSyncCommand extends SyncCommand {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException {
      // nothing
    }
  }

  /**
   * Single file synchronization processor for actual implementation of Cloud Drive and its storage.
   */
  @Deprecated
  protected abstract class SyncFileCommand extends AbstractCommand {

    protected final Node    fileNode;

    protected final boolean isFolder;

    protected final boolean isNew;

    SyncFileCommand() {
      super();
      fileNode = null;
      isFolder = false;
      isNew = false;
    }

    /**
     * @throws RepositoryException
     */
    public SyncFileCommand(Node file) throws RepositoryException, SyncNotSupportedException {

      // TODO upload support for locally created nodes
      // for already cloud file run sync of its parent, for a folder use this folder
      // for a new file/folder add it on the cloud provider side

      if (file.isNodeType(ECD_CLOUDFILE)) {
        // it's already existing cloud file, need sync its content
        // cloudFile = drive.openFile(file.getName(), null);
        isFolder = false;
        isNew = false;
      } else if (file.isNodeType(ECD_CLOUDFOLDER)) {
        // it's already existing cloud folder, need sync its properties
        // cloudFile = drive.openFile(file.getName(), null);
        isFolder = true;
        isNew = false;
      } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
        // it's resource subnode of the ecd:cloudDrive
        // need sync on the parent
        file = file.getParent();
        isFolder = false;
        isNew = false;
        // cloudFile = drive.openFile(file.getName(), null);
      } else if (file.isNodeType(NT_FILE)) {
        // it's new local JCR node - upload it to the cloud
        isFolder = false;
        isNew = true;
        // mark a node as a file for sync
        // initFile(file);

        // String mimeType = file.getNode("jcr:content").getProperty("jcr:mimeType").getString();
        // cloudFile = drive.openFile(file.getName(), mimeType);
      } else if (file.isNodeType(NT_RESOURCE)) {
        // it's resource of new local JCR node - upload this nt:file to the cloud
        file = file.getParent();
        isFolder = false;
        isNew = true;
        // mark a node as a file for sync
        // initFile(file);

        // String mimeType = file.getProperty("jcr:mimeType").getString();
        // cloudFile = drive.openFile(file.getName(), mimeType);
      } else if (file.isNodeType(NT_FOLDER)) {
        // it's new local JCR node (folder) - upload it to the cloud
        isFolder = true;
        isNew = true;
        // mark a node as a folder for sync
        // initFile(file);
      } else if (file.isNodeType(NT_UNSTRUCTURED)) {
        // TODO it's new local JCR node - upload free form file to the cloud
        throw new SyncNotSupportedException("Synchronization not supported for "
            + file.getPrimaryNodeType().getName() + " node: " + file.getPath());
      } else {
        // unsupported nodetype
        throw new SyncNotSupportedException("Synchronization not supported for "
            + file.getPrimaryNodeType().getName() + " node: " + file.getPath());
      }

      this.fileNode = file;
    }

    // TODO cleanup
    // void initFile(Node file) throws RepositoryException {
    // if (!file.isNodeType(ECD_NEWFILE)) {
    // file.addMixin(ECD_NEWFILE);
    // }
    // file.setProperty("ecd:syncStage", SYNC_NEWFILE_INIT);
    // file.save();
    // }
    //
    // void processFile(Node file) throws RepositoryException {
    // file.setProperty("ecd:syncStage", SYNC_NEWFILE_INPROGRESS);
    // file.save();
    // }
    //
    // void cleanFile(Node file) throws RepositoryException {
    // if (file.isNodeType(ECD_NEWFILE)) {
    // file.setProperty("ecd:syncStage", (String) null); // remove property
    // file.removeMixin(ECD_NEWFILE);
    // file.save();
    // }
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitProcess() throws CloudDriveException, RepositoryException {
      // wait for whole drive sync
      // TODO cleanup
      // SyncCommand driveSync = currentSync.get();
      // if (driveSync != noSyncCommand) {
      // // the file's drive sync already in progress, wait for it
      // try {
      // driveSync.await();
      // } catch (InterruptedException e) {
      // LOG.warn("Interrupted while waiting for a drive sync: " + e.getMessage());
      // Thread.currentThread().interrupt();
      // } catch (ExecutionException e) {
      // // we skip this error and will proceed with the current command
      // LOG.warn("Error while waiting for a drive sync: " + e.getMessage());
      // }
      // }
      //
      // // wait for sub-files sync
      // waitFileSync(fileNode.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      CloudFileSynchronizer sync = null;
      for (CloudFileSynchronizer s : fileSynchronizers) {
        if (s.accept(fileNode)) {
          sync = s;
          break;
        }
      }

      if (sync != null) {
        // synchronize file
        try {
          // if (isNew) {
          // processFile(fileNode);
          // }

          // sync.synchronize(fileNode, this);

          // and save the drive node
          fileNode.save();

          // if (isNew) {
          // cleanFile(fileNode);
          // }
        } finally {
          // currentSyncFiles.remove(fileNode.getPath(), this); // release the lock, see synchronize(Node)
        }

        // fire listeners TODO a dedicated method for file sync?
        listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, fileNode.getPath()));
      } else {
        // not supported file type for synchronization
        LOG.warn("Not supported file for synchronization: " + fileNode.getPath());
      }
    }
  }

  protected class SyncFilesCommand extends AbstractCommand {

    final List<FileChange> changes;

    SyncFilesCommand(List<FileChange> changes) {
      this.changes = changes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "files synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      if (isConnected()) {
        // wait for whole drive sync
        syncLock.readLock().lock(); // read-lock can be acquired by multiple threads (file syncs)

        try {
          Set<String> ignoredPaths = new HashSet<String>(); // for not supported by sync
          Set<String> updated = new HashSet<String>(); // to reduce update changes of the same file

          next: for (FileChange change : changes) {
            for (String ipath : ignoredPaths) {
              if (change.getPath().startsWith(ipath)) {
                continue next; // skip parts of ignored (not supported by sync) nodes
              }
            }

            try {
              change.initUpdated(updated);
              change.apply();

              // if (change.eventType == Event.NODE_REMOVED) {
              // eventName = "removal";
              // LOG.info("Cloud file removal " + change.filePath); // TODO cleanup
              // // Removal should be initiated by initRemove() before this event happening and in the same
              // // thread.
              // // For direct JCR removals this should be done by RemoveCloudFileAction. Then this remove
              // will
              // // succeed and return true.
              // // In case of move to Trash, it should be fully handled by TrashHandler, it sets
              // // ecd:trashed and call trash().
              // // If removal not initiated (in this thread) the remove will return false, then it's
              // // move/rename - it will be handled by NODE_ADDED below.
              //
              // // #1 check if it is not a direct JCR remove (in this thread)
              // // FileChange remove = removed.remove(filePath);
              // // if (remove != null) {
              // // remove.apply();
              // // }
              //
              // // #3 in case of timeout of FileTrash, we assume it is not a file removal at all (file could
              // // be moved)
              // change.apply();
              // } else {
              // Item item = session.getItem(filePath);
              // try {
              // if (item.isNode()) {
              // Node file = (Node) item;
              // if (event.getType() == Event.NODE_ADDED) {
              // if (file.isNodeType(ECD_CLOUDFILE)) {
              // if (file.hasProperty("ecd:trashed")) {
              // eventName = "untrash";
              // new FileUntrash(file).apply();
              // } else {
              // eventName = "move";
              // // it's already existing cloud file or folder, thus it is move/rename
              // new FileUpdate(file).apply();
              // }
              // } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
              // // skip file's resource, it will be handled within a file
              // } else {
              // eventName = "creation";
              // file = cleanNode(file, fileAPI.getTitle(file));
              // new FileCreate(file).apply();
              // }
              // }
              // } else {
              // if (event.getType() == Event.PROPERTY_CHANGED) {
              // LOG.info(">>> Cloud file property changed " + filePath); // TODO cleanup
              // Node file = item.getParent();
              // if (file.isNodeType(ECD_CLOUDFILE)) {
              // eventName = "update";
              // new FileUpdate(file).apply();
              // } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
              // eventName = "content update";
              // new FileContentUpdate(file).apply();
              // }
              // }
              // }
            } catch (SyncNotSupportedException e) {
              // remember to skip sub-files
              ignoredPaths.add(change.getPath());
            }
          }

          // wait for trashes if have ones
          for (FileChange change : changes) {
            change.finishTrashed();
          }

          rootNode.save(); // save the drive
        } finally {
          syncLock.readLock().unlock();
        }

        // TODO fire listeners afterwards with detailed event?
        // listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()));
      } else {
        LOG.warn("Cannot synchronize file in cloud drive '" + title() + "': drive not connected");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitProcess() throws CloudDriveException, RepositoryException {
      // wait for whole drive sync
      // TODO clenaup
      // SyncCommand driveSync = currentSync.get();
      // if (driveSync != noSyncCommand) {
      // // the file's drive sync already in progress, wait for it
      // try {
      // driveSync.await();
      // } catch (InterruptedException e) {
      // LOG.warn("Interrupted while waiting for a drive sync: " + e.getMessage());
      // Thread.currentThread().interrupt();
      // } catch (ExecutionException e) {
      // // we skip this error and will proceed with the current command
      // LOG.warn("Error while waiting for a drive sync: " + e.getMessage());
      // }
      // }
    }
  }

  /**
   * Basic implementation of CloudFileAPI support.
   */
  protected abstract class AbstractFileAPI implements CloudFileAPI {

    protected String rootPath() throws DriveRemovedException, RepositoryException {
      return rootNode().getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId(Node fileNode) throws CloudDriveException, RepositoryException {
      return fileNode.getProperty("ecd:id").getString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle(Node fileNode) throws CloudDriveException, RepositoryException {
      String filePath = fileNode.getPath();
      if (filePath.startsWith(rootNode().getPath())) {
        try {
          return fileNode.getProperty("exo:title").getString();
        } catch (PathNotFoundException e) {
          return fileNode.getName();
        }
      } else {
        throw new NotCloudFileException("Not cloud file: " + filePath);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentId(Node fileNode) throws CloudDriveException, RepositoryException {
      Node parent = fileNode.getParent();
      String parentPath = parent.getPath();
      if (parentPath.startsWith(rootNode().getPath())) {
        return parent.getProperty("ecd:id").getString();
      } else {
        throw new NotCloudFileException("Not cloud file: " + parentPath);
      }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> findParents(Node fileNode) throws RepositoryException, CloudDriveException {
      Set<String> parentIds = new LinkedHashSet<String>();
      for (NodeIterator niter = findNodes(Arrays.asList(getId(fileNode))); niter.hasNext();) {
        Node p = niter.nextNode().getParent(); // parent it is a cloud file or a cloud drive
        parentIds.add(p.getProperty("ecd:id").getString());
      }
      return Collections.unmodifiableCollection(parentIds);
    }
  }

  /**
   * Base support for Cloud File operations.
   */
  @Deprecated
  protected abstract class FileCommand extends AbstractCommand {

    protected final String                fileId;

    protected final String                filePath;

    protected final boolean               isFolder;

    protected final CloudFileAPI          fileApi;

    protected final CloudFileSynchronizer synchronizer;

    protected FileCommand                 next;

    public FileCommand(Node file, CloudFileAPI fileApi) throws SyncNotSupportedException,
        RepositoryException,
        SkipSyncException {
      this.fileApi = fileApi;

      boolean cloudFile = false;
      if (file.isNodeType(ECD_CLOUDFOLDER)) {
        // it's already existing cloud folder, need sync its properties
        this.isFolder = true;
        cloudFile = true;
      } else if (file.isNodeType(ECD_CLOUDFILE)) {
        // it's already existing cloud file, need sync its content
        this.isFolder = false;
        cloudFile = true;
      } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
        // it's resource subnode of the ecd:cloudDrive
        // need sync on the parent
        file = file.getParent();
        this.isFolder = false;
        cloudFile = true;
      } else {
        // not cloud file yet
        this.isFolder = false;
        // TODO cleanup
        // throw new SyncNotSupportedException("Synchronization not supported for file type "
        // + file.getPrimaryNodeType().getName() + " in node: " + file.getPath());
      }

      this.filePath = file.getPath();
      if (cloudFile) {
        this.fileId = file.getProperty("ecd:id").getString();
      } else {
        this.fileId = null;
      }

      CloudFileSynchronizer sync = null;
      for (CloudFileSynchronizer s : fileSynchronizers) {
        if (s.accept(file)) {
          sync = s;
          break;
        }
      }
      if (sync != null) {
        this.synchronizer = sync;
      } else {
        throw new SyncNotSupportedException("Synchronization not supported for file type "
            + file.getPrimaryNodeType().getName() + " in node: " + filePath);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitProcess() throws CloudDriveException, RepositoryException {
      // wait for whole drive sync
      // TODO cleanup
      // SyncCommand driveSync = currentSync.get();
      // if (driveSync != noSyncCommand) {
      // // the file's drive sync already in progress, wait for it
      // try {
      // driveSync.await();
      // } catch (InterruptedException e) {
      // LOG.warn("Interrupted while waiting for a drive sync: " + e.getMessage());
      // Thread.currentThread().interrupt();
      // } catch (ExecutionException e) {
      // // we skip this error and will proceed with the current command
      // LOG.warn("Error while waiting for a drive sync: " + e.getMessage());
      // }
      // }
      //
      // // wait for sub-files sync
      // waitFileSync(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void await() throws ExecutionException, InterruptedException {
      super.await();
      if (next != null) {
        next.await();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getComplete() {
      long complete = super.getComplete();
      if (next != null) {
        complete += next.getComplete();
      }
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
      long available = super.getAvailable();
      if (next != null) {
        available += next.getAvailable();
      }
      return available;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFinishTime() {
      long finishTime = super.getFinishTime();
      if (next != null) {
        finishTime = next.getFinishTime();
      }
      return finishTime;
    }

    /**
     * Chain given command as a next after this command.
     * 
     * @param next {@link FileCommand}
     * @return <code>true</code> if command was successfully added, <code>false</code> if given command
     *         already in the chain
     */
    protected boolean chain(FileCommand next) {
      if (this != next) {
        if (this.next == null) {
          this.next = next;
          return true;
        } else {
          return this.next.chain(next);
        }
      }
      return false;
    }

    /**
     * Return {@link Node} instance for the command's cloud file.
     * 
     * @return {@link Node} instance obtained under current user in current thread
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    protected Node fileNode() throws RepositoryException, CloudDriveException {
      Session session = session(); // user session
      Item item = session.getItem(filePath);
      if (item.isNode()) {
        return (Node) item;
      } else {
        throw new CloudDriveException("Item not a node: " + filePath);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void process() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      // process this file first, then all chained files
      try {
        processFile();
      } finally {
        finishFileSync(filePath, this);
      }

      // fire listeners?
      // TODO dedicated event for update? but it is already possible via JCR observation/action
      // TODO fire once on the observation events handling done
      // listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, filePath));

      // chained commands
      if (next != null) {
        next.process();
      }
    }

    /**
     * Processing logic for current cloud file.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void processFile() throws CloudDriveAccessException,
                                         CloudDriveException,
                                         RepositoryException;
  }

  /**
   * Cloud file removal processor.
   */
  @Deprecated
  protected class RemoveFileCommand extends FileCommand {

    /**
     * @throws SyncNotSupportedException
     * @throws RepositoryException
     * @throws SkipSyncException
     */
    protected RemoveFileCommand(Node file, CloudFileAPI fileApi) throws RepositoryException,
        SyncNotSupportedException,
        SkipSyncException {
      super(file, fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processFile() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      synchronizer.remove(filePath, fileId, fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file removal";
    }
  }

  /**
   * Cloud file addition processor.
   */
  @Deprecated
  protected class CreateFileCommand extends FileCommand {

    /**
     * @throws SyncNotSupportedException
     * @throws RepositoryException
     * @throws SkipSyncException
     */
    protected CreateFileCommand(Node file, CloudFileAPI fileApi) throws RepositoryException,
        SyncNotSupportedException,
        SkipSyncException {
      super(file, fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processFile() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      synchronizer.create(fileNode(), fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file creation";
    }
  }

  /**
   * Cloud file update processor.
   */
  @Deprecated
  protected class UpdateFileCommand extends FileCommand {

    /**
     * @throws RepositoryException
     * @throws SkipSyncException
     */
    protected UpdateFileCommand(Node file, CloudFileAPI fileApi) throws RepositoryException,
        SyncNotSupportedException,
        SkipSyncException {
      super(file, fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processFile() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      synchronizer.update(fileNode(), fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file update";
    }
  }

  /**
   * Cloud file content update processor.
   */
  @Deprecated
  protected class UpdateFileContentCommand extends FileCommand {

    /**
     * @throws RepositoryException
     * @throws SkipSyncException
     */
    protected UpdateFileContentCommand(Node file, CloudFileAPI fileApi) throws RepositoryException,
        SyncNotSupportedException,
        SkipSyncException {
      super(file, fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processFile() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      synchronizer.updateContent(fileNode(), fileApi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file content update";
    }
  }

  /**
   * Cloud File change. It is used for keeping a file change request and applying it in current thread.
   * File change differs to a {@link Command} that the command offers public API with an access to the
   * command state and progress. A command also runs asynchronously in a dedicated thread. The file change is
   * for internal use (can be used within a command).
   */
  protected class FileChange {

    public static final String REMOVE  = "D";

    public static final String CREATE  = "A";

    public static final String UPDATE  = "U";

    final CountDownLatch       applied = new CountDownLatch(1);

    final String               path;

    String                     filePath;

    String                     fileId;

    CountDownLatch             trashConformation;

    FileChange                 next;

    CloudFileSynchronizer      synchronizer;

    String                     changeType;

    Set<String>                updated;

    /**
     * Identifies the change for fileChanged map. Will be initialized on the change completion.
     */
    String                     changeId;

    FileChange(String path, String fileId, String changeType, CloudFileSynchronizer synchronizer) {
      this.path = path;
      this.changeType = changeType;
      this.fileId = fileId;
      this.synchronizer = synchronizer;
    }

    FileChange(String filePath, String fileId, String changeType) {
      this(filePath, fileId, changeType, null);
    }

    FileChange(String filePath, String changeType) {
      this(filePath, null, changeType, null);
    }

    void initUpdated(Set<String> updated) {
      this.updated = updated;
    }

    /**
     * Chain given change as a next after this one.
     * 
     * @param next {@link FileChange}
     * @return <code>true</code> if change was successfully added, <code>false</code> if given change
     *         already in the chain or the change already applied.
     */
    boolean chain(FileChange next) {
      if (applied.getCount() > 0) {
        if (this != next) {
          if (this.next == null) {
            this.next = next;
            return true;
          } else {
            return this.next.chain(next);
          }
        }
      }
      return false;
    }

    String getChangeId() {
      return changeId;
    }

    String getPath() {
      if (path != null) {
        return path;
      } else {
        return path;
      }
    }

    /**
     * Wait for the change completion.
     * 
     */
    private void await() {
      while (applied.getCount() > 0) {
        try {
          LOG.info(">>>> Await " + getPath()); // TODO cleanup
          applied.await();
        } catch (InterruptedException e) {
          LOG.warn("Caller of file change interrupted.", e);
          Thread.currentThread().interrupt();
        }
      }
    }

    /**
     * Wait for this file and its sub-tree changes in other threads, wait exclusively to let the existing to
     * finish and then set the lock do not let a new to apply before or during this change.
     */
    private void lock() {
      synchronized (fileChanges) {
        String lockedPath = getPath();
        FileChange other = fileChanges.putIfAbsent(lockedPath, this);
        if (other != this) {
          other = fileChanges.put(lockedPath, this);
          if (other != this) {
            LOG.info(">>> Waiting for " + other.getPath()); // TODO cleanup
            other.await();
            LOG.info("<<< Done " + other.getPath());
          }
        }
        for (FileChange c : fileChanges.values()) {
          if (c != this && c.getPath().startsWith(lockedPath)) {
            LOG.info(">>> Waiting for " + c.getPath());
            c.await();
            LOG.info("<<< Done " + c.getPath());
          }
        }
      }
    }

    /**
     * Remove the lock set in {@link #lock()} method and add this changed file id to fileChanged map.
     * 
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    private void complete() throws PathNotFoundException, RepositoryException, CloudDriveException {
      fileChanges.remove(getPath(), this);
      if (changeId != null) {
        addChanged(fileId, changeId);
      }
      applied.countDown();
    }

    private void init(Node file) throws RepositoryException, CloudDriveException {
      if (fileId == null) {
        fileId = fileAPI.getId(file);
      }
      if (filePath == null) {
        filePath = file.getPath();
      }
    }

    private void finishTrashed() throws CloudDriveException, RepositoryException {
      if (trashConformation != null) {
        try {
          // wait for 60 sec for the trash confirmation
          if (!trashConformation.await(60, TimeUnit.SECONDS)) {
            // if not confirmed in the time - remove the file
            removeFile();
          }
        } catch (InterruptedException e) {
          LOG.error("Error waiting for file trashing confirmation: " + e.getMessage());
          Thread.currentThread().interrupt();
        } finally {
          fileTrash.remove(fileId, trashConformation);
          complete();
        }
      }
    }

    private void removeFile() throws PathNotFoundException, CloudDriveException, RepositoryException {
      // TODO need also remove the file node in Trash, it is with empty data and cannot be untrashed
      LOG.info("Remove file " + path + " " + fileId); // TODO cleanup
      lock();

      synchronizer.remove(path, fileId, fileAPI);

      changeId = changeType == CREATE ? UPDATE : changeType;
    }

    private void trashFile() throws PathNotFoundException, CloudDriveException, RepositoryException {
      LOG.info("Trash file " + path + " " + fileId); // TODO cleanup
      lock();

      synchronizer.trash(path, fileId, fileAPI);
      CountDownLatch confirmation;
      CountDownLatch existing = fileTrash.putIfAbsent(fileId, confirmation = new CountDownLatch(1));
      if (existing != null) {
        confirmation = existing; // will wait for already posted trash
      } // else, will wait for just posted trash
      trashConformation = confirmation;

      changeId = changeType == CREATE ? UPDATE : changeType;
    }

    private void untrashFile(Node file) throws SkipSyncException,
                                       SyncNotSupportedException,
                                       CloudDriveException,
                                       RepositoryException {
      init(file);
      LOG.info("Untrash file " + filePath + " " + fileId); // TODO cleanup

      lock();

      synchronizer(file).untrash(file, fileAPI);
      file.setProperty("ecd:trashed", (String) null); // clean the marker set in AddTrashListener

      changeId = changeType == CREATE ? UPDATE : changeType;
    }

    private void update(Node file) throws SkipSyncException,
                                  SyncNotSupportedException,
                                  CloudDriveException,
                                  RepositoryException {
      init(file);
      if (!isUpdated(fileId)) {
        LOG.info("Update file " + filePath + " " + fileId); // TODO cleanup

        lock();

        synchronizer(file).update(file, fileAPI);

        changeId = changeType == CREATE ? UPDATE : changeType;
        // modified = getModified(file);

        addUpdated(fileId);
      } // else, we skip a change of already listed for update file
    }

    private void updateContent(Node file) throws SkipSyncException,
                                         SyncNotSupportedException,
                                         CloudDriveException,
                                         RepositoryException {
      init(file);
      LOG.info("Update content of file " + filePath + " " + fileId); // TODO cleanup

      lock();

      synchronizer(file).updateContent(file, fileAPI);

      changeId = changeType == CREATE ? UPDATE : changeType;
    }

    private void create(Node file) throws SkipSyncException,
                                  SyncNotSupportedException,
                                  CloudDriveException,
                                  RepositoryException {
      LOG.info("Create file " + path); // TODO cleanup

      file = cleanNode(file, fileAPI.getTitle(file));
      filePath = file.getPath(); // actual node path

      lock();

      synchronizer(file).create(file, fileAPI);
      // we can know the id after the sync
      fileId = fileAPI.getId(file);

      LOG.info("Created file " + file.getPath() + " " + fileId); // TODO cleanup

      changeId = changeType == CREATE ? UPDATE : changeType;
    }

    private boolean isUpdated(String fileId) {
      return updated != null ? updated.contains(fileId) : false;
    }

    private void addUpdated(String fileId) {
      if (updated != null) {
        updated.add(fileId);
      }
    }

    @Deprecated
    private String fileId(Node file) throws CloudDriveException, RepositoryException {
      if (fileId == null) {
        return fileId = fileAPI.getId(file);
      } else {
        return fileId;
      }
    }

    @Deprecated
    private Node node() throws PathNotFoundException, RepositoryException, CloudDriveException {
      Item item = session().getItem(path);
      if (item.isNode()) {
        return (Node) item;
      } else {
        throw new CloudDriveException("Not a Node " + path);
      }
    }

    @Deprecated
    private Calendar getModified(Node file) throws RepositoryException {
      try {
        return file.getProperty("ecd:modified").getDate();
      } catch (PathNotFoundException e) {
        return null;
      }
    }

    /**
     * Apply the change to target file.
     * 
     * @throws RepositoryException
     * @throws CloudDriveException
     * @throws SyncNotSupportedException
     * @throws DriveRemovedException
     */
    void apply() throws DriveRemovedException, CloudDriveException, RepositoryException {
      if (applied.getCount() > 0) {
        try {
          String changeName = null;
          if (changeType == REMOVE) {
            if (synchronizer != null) {
              // #1 if trash not supported - delete the file,
              // #2 trash otherwise;
              // #3 if trash not confirmed in time - delete the file finally
              if (fileAPI.isTrashSupported()) {
                changeName = "trash";
                trashFile();
              } else {
                changeName = "remove";
                removeFile();
              }
            } else {
              throw new SyncNotSupportedException("Synchronization not available for file removal: " + path);
            }
          } else {
            try {
              Item item = session().getItem(path); // reading in user session
              Node file = null;
              try {
                if (item.isNode()) {
                  file = (Node) item;
                  if (changeType == CREATE) {
                    if (file.isNodeType(ECD_CLOUDFILE)) {
                      if (file.hasProperty("ecd:trashed")) {
                        changeName = "untrash";
                        untrashFile(file);
                      } else {
                        changeName = "move";
                        // it's already existing cloud file or folder, thus it is move/rename
                        update(file);
                      }
                    } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
                      // skip file's resource, it will be handled within a file
                    } else {
                      changeName = "creation";
                      create(file);
                    }
                  }
                } else {
                  file = item.getParent();
                  if (changeType == UPDATE) {
                    if (file.isNodeType(ECD_CLOUDFILE)) {
                      changeName = "update";
                      update(file);
                    } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
                      // TODO detect content update more precisely (by exact property name in synchronizer)
                      changeName = "content update";
                      file = file.getParent();
                      updateContent(file);
                    }
                  }
                }
              } catch (SyncNotSupportedException e) {
                if (file != null) { // always will be not null
                  if (!file.isNodeType(ECD_IGNORED)) {
                    // if sync not supported, it's not supported NT: ignore the node
                    // TODO warn -> debug
                    LOG.warn("Cannot synchronize cloud file " + changeName + ": " + e.getMessage()
                        + ". Ignoring the file.");
                    try {
                      ignoreFile(file);
                    } catch (Throwable t) {
                      LOG.error("Error ignoring not a cloud item " + getPath(), t);
                    }
                    throw e; // throw to upper code
                  } else {
                    LOG.info("Synchronization not available for ignored cloud item " + changeName + ": "
                        + getPath()); // TODO info -> debug
                  }
                }
              }
            } catch (SkipSyncException e) {
              // skip this file (it can be a part of top level NT supported by the sync)
            } catch (PathNotFoundException e) {
              LOG.error("Cannot find an item of cloud file for synchronization: " + getPath() + ": "
                  + e.getMessage());
            }
          }
        } finally {
          if (trashConformation == null) {
            complete();
          }
        }

        // apply chained
        if (next != null) {
          next.apply();
        }
      }
    }
  }

  /**
   * Cloud File change. It is used for keeping a file change request and applying it in current thread.
   * File change differs to a {@link Command} that the command offers public API with an access to the
   * command state and progress. A command also runs asynchronously in a dedicated thread. The file change is
   * for internal use (can be used within a command).
   */
  protected abstract class FileChange0 {

    // protected final Node file;

    // protected final String fileId;

    final String          filePath;

    // protected final CloudFileSynchronizer synchronizer;

    final CountDownLatch  applied = new CountDownLatch(1);

    protected FileChange0 next;

    /**
     */
    public FileChange0(String filePath) {
      this.filePath = filePath;
    }

    Node node() throws PathNotFoundException, RepositoryException, CloudDriveException {
      Item item = session().getItem(filePath);
      if (item.isNode()) {
        return (Node) item;
      } else {
        throw new CloudDriveException("Not a Node " + filePath);
      }
    }

    String fileId() throws PathNotFoundException, RepositoryException, CloudDriveException {
      Node file = node();
      if (file.isNodeType(ECD_CLOUDFILE)) {
        return file.getProperty("ecd:id").getString();
      }
      return null;
    }

    String filePath() {
      return filePath;
    }

    CloudFileSynchronizer synchronizer() throws PathNotFoundException,
                                        RepositoryException,
                                        SyncNotSupportedException,
                                        CloudDriveException {
      Node file = node();
      for (CloudFileSynchronizer s : fileSynchronizers) {
        if (s.accept(file)) {
          return s;
        }
      }
      throw new SyncNotSupportedException("Synchronization not supported for file type "
          + file.getPrimaryNodeType().getName() + " in node: " + filePath);
    }

    /**
     * Chain given change as a next after this one.
     * 
     * @param next {@link FileChange}
     * @return <code>true</code> if change was successfully added, <code>false</code> if given change
     *         already in the chain or the change already applied.
     */
    protected boolean chain(FileChange0 next) {
      if (applied.getCount() > 0) {
        if (this != next) {
          if (this.next == null) {
            this.next = next;
            return true;
          } else {
            return this.next.chain(next);
          }
        }
      }
      return false;
    }

    /**
     * Wait for the change completion.
     * 
     */
    private void await() {
      while (applied.getCount() > 0) {
        try {
          applied.await();
        } catch (InterruptedException e) {
          LOG.warn("Caller of file change interrupted.", e);
          Thread.currentThread().interrupt();
        }
      }
    }

    /**
     * Apply this and its chained changes.
     * 
     * @throws DriveRemovedException
     * @throws SyncNotSupportedException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    private void applyChange() throws DriveRemovedException, CloudDriveException, RepositoryException {
      change();
      // and chained
      if (next != null) {
        next.applyChange();
      }
    }

    /**
     * Apply the change to target file.
     * 
     * @throws RepositoryException
     * @throws CloudDriveException
     * @throws SyncNotSupportedException
     * @throws DriveRemovedException
     */
    protected final void apply() throws DriveRemovedException, CloudDriveException, RepositoryException {
      if (applied.getCount() > 0) {
        try {
          // wait for this file and its sub-tree changes in other threads,
          // wait exclusively to let the existing to finish and do not let a new to apply before this change
          // synchronized (fileChanges) {
          // fileChanges.putIfAbsent(filePath, this);
          // for (FileChange c : fileChanges.values()) {
          // if (c != this && c.filePath.startsWith(filePath)) {
          // c.await();
          // }
          // }
          // }

          applyChange();
        } finally {
          fileChanges.remove(filePath, this);
          applied.countDown();
        }
      }
    }

    protected abstract void change() throws DriveRemovedException, CloudDriveException, RepositoryException;
  }

  // protected class FileCreate extends FileChange {
  //
  // FileCreate(String filePath) throws SyncNotSupportedException, RepositoryException, SkipSyncException {
  // super(filePath);
  // }
  //
  // /**
  // * {@inheritDoc}
  // */
  // @Override
  // protected void change() throws DriveRemovedException, CloudDriveException, RepositoryException {
  // synchronizer().create(node(), fileAPI);
  // }
  // }
  //
  // protected class FileTrash extends FileChange {
  //
  // FileTrash(String filePath) throws SyncNotSupportedException, RepositoryException, SkipSyncException {
  // super(filePath);
  // }
  //
  // /**
  // * {@inheritDoc}
  // */
  // @Override
  // protected void change() throws DriveRemovedException, CloudDriveException, RepositoryException {
  // Node file = node();
  // synchronizer().trash(file, fileAPI);
  // // mark the file node to be able properly untrash it,
  // file.setProperty("ecd:trashed", true);
  // }
  // }
  //
  // protected class FileUntrash extends FileChange {
  //
  // FileUntrash(String filePath) throws SyncNotSupportedException, RepositoryException, SkipSyncException {
  // super(filePath);
  // }
  //
  // /**
  // * {@inheritDoc}
  // */
  // @Override
  // protected void change() throws DriveRemovedException, CloudDriveException, RepositoryException {
  // Node file = node();
  // Property trashed = file.getProperty("ecd:trashed");
  // synchronizer().untrash(file, fileAPI);
  // trashed.remove(); // finally clean the marker
  // }
  // }
  //
  // protected class FileUpdate extends FileChange {
  //
  // FileUpdate(String filePath) throws SyncNotSupportedException, RepositoryException, SkipSyncException {
  // super(filePath);
  // }
  //
  // /**
  // * {@inheritDoc}
  // */
  // @Override
  // protected void change() throws DriveRemovedException, CloudDriveException, RepositoryException {
  // synchronizer().update(node(), fileAPI);
  // }
  // }
  //
  // protected class FileContentUpdate extends FileChange {
  //
  // FileContentUpdate(String filePath) throws SyncNotSupportedException,
  // RepositoryException,
  // SkipSyncException {
  // super(filePath);
  // }
  //
  // /**
  // * {@inheritDoc}
  // */
  // @Override
  // protected void change() throws DriveRemovedException, CloudDriveException, RepositoryException {
  // synchronizer().updateContent(node(), fileAPI);
  // }
  // }

  // *********** variables ***********

  /**
   * Support for JCR actions. To do not fire on synchronization (our own modif) methods.
   */
  protected static final ThreadLocal<CloudDrive>            actionDrive       = new ThreadLocal<CloudDrive>();

  protected static final Transliterator                     accentsConverter  = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  protected final String                                    rootWorkspace;

  protected final ManageableRepository                      repository;

  protected final SessionProviderService                    sessionProviders;

  protected final CloudUser                                 user;

  protected final String                                    rootUUID;

  protected final ThreadLocal<SoftReference<Node>>          rootNodeHolder;

  protected final JCRListener                               jcrListener;

  protected final ConnectCommand                            noConnect         = new NoConnectCommand();

  /**
   * Currently active connect command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<ConnectCommand>           currentConnect    = new AtomicReference<ConnectCommand>(noConnect);

  protected final SyncCommand                               noSyncCommand     = new NoSyncCommand();

  /**
   * Currently active synchronization command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<SyncCommand>              currentSync       = new AtomicReference<SyncCommand>(noSyncCommand);

  /**
   * Currently active file synchronization commands. Used to control concurrency in Cloud Drive.
   */
  @Deprecated
  protected final ConcurrentHashMap<String, FileCommand>    currentSyncFiles  = new ConcurrentHashMap<String, FileCommand>();

  protected final ReadWriteLock                             syncLock          = new ReentrantReadWriteLock(true);

  protected final ConcurrentHashMap<String, FileChange>     fileChanges       = new ConcurrentHashMap<String, FileChange>();

  protected final ThreadLocal<Map<String, FileChange>>      fileRemovals      = new ThreadLocal<Map<String, FileChange>>();

  protected final ConcurrentHashMap<String, CountDownLatch> fileTrash         = new ConcurrentHashMap<String, CountDownLatch>();

  /**
   * File changes already applied locally but not yet aligned with from-cloud synchronization.
   * Format: FILE_ID = [CHANGE_DATA_1, CHANGE_DATA_2,... CHANGE_DATA_N]
   */
  protected final ConcurrentHashMap<String, Set<String>>    fileChanged       = new ConcurrentHashMap<String, Set<String>>();

  /**
   * Managed queue of commands.
   */
  protected final CommandPoolExecutor                       commandExecutor;

  /**
   * Environment for commands execution.
   */
  protected final CloudDriveEnvironment                     commandEnv        = new ExoJCREnvironment();

  /**
   * Synchronizers for file synchronization.
   */
  protected final Set<CloudFileSynchronizer>                fileSynchronizers = new LinkedHashSet<CloudFileSynchronizer>();

  /**
   * Singleton of {@link CloudFileAPI}.
   */
  protected final CloudFileAPI                              fileAPI;

  /**
   * Title has special care. It used in error logs and an attempt to read <code>exo:title</code> property can
   * cause another {@link RepositoryException}. Thus need it pre-cached in the variable and try to read the
   * <code>exo:title</code> property each time, but if not successful use this one cached.
   */
  private String                                            titleCached;

  /**
   * Create JCR backed {@link CloudDrive}. This method used for both newly connecting drives and ones loading
   * from the JCR node. If storage error will happen all pending changes will be rolled back before throwing
   * the exception.
   * 
   * @param driveNode {@link Node} - existing node
   * @param sessionProviders {@link SessionProviderService}
   * @throws CloudDriveException if error on cloud provider side happen
   * @throws RepositoryException if storage error happen.
   */
  protected JCRLocalCloudDrive(CloudUser user, Node driveNode, SessionProviderService sessionProviders) throws CloudDriveException,
      RepositoryException {

    this.user = user;
    this.sessionProviders = sessionProviders;

    this.commandExecutor = CommandPoolExecutor.getInstance();

    Session session = driveNode.getSession();
    this.repository = (ManageableRepository) session.getRepository();
    this.rootWorkspace = session.getWorkspace().getName();

    // ensure given node has required nodetypes
    if (driveNode.isNodeType(ECD_CLOUDDRIVE)) {
      if (driveNode.hasProperty("exo:title")) {
        titleCached = driveNode.getProperty("exo:title").getString();
      }
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
    }

    this.rootUUID = driveNode.getUUID();
    this.rootNodeHolder = new ThreadLocal<SoftReference<Node>>();
    this.rootNodeHolder.set(new SoftReference<Node>(driveNode));

    this.fileAPI = createFileAPI();

    // add drive trash listener
    this.jcrListener = addJCRListener(driveNode);
    this.addListener(jcrListener.changesListener); // listen for errors here
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("exo:title").getString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLink() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:url").getString();
  }

  /**
   * {@inheritDoc}
   */
  public String getId() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:id").getString();
  }

  /**
   * @inherritDoc
   */
  public String getLocalUser() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:localUserName").getString();

  }

  /**
   * @inherritDoc
   */
  public Calendar getInitDate() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:initDate").getDate();
  }

  /**
   * {@inheritDoc}
   */
  public String getPath() throws DriveRemovedException, RepositoryException {
    return rootNode().getPath();
  }

  /**
   * @inherritDoc
   */
  public Calendar getConnectDate() throws DriveRemovedException, NotConnectedException, RepositoryException {
    if (isConnected()) {
      return rootNode().getProperty("ecd:connectDate").getDate();
    } else {
      throw new NotConnectedException("Drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile getFile(String path) throws DriveRemovedException,
                                       NotCloudFileException,
                                       RepositoryException {
    Node driveNode = rootNode();
    if (path.startsWith(driveNode.getPath())) {
      Item item = driveNode.getSession().getItem(path);
      if (item.isNode()) {
        Node fileNode = fileNode((Node) item);
        if (fileNode == null) {
          throw new NotCloudFileException("Node '" + path + "' is not a cloud file or maked as ignored.");
        }
        return readFile(fileNode);
      } else {
        throw new NotCloudFileException("Item at path '" + path
            + "' is Property and cannot be read as cloud file.");
      }
    } else {
      throw new NotCloudFileException("Item at path '" + path + "' does not belong to Cloud Drive '"
          + title() + "'");
    }
  }

  /**
   * @inherritDoc
   */
  @Override
  public boolean hasFile(String path) throws DriveRemovedException, RepositoryException {
    Node driveNode = rootNode();
    if (path.startsWith(driveNode.getPath())) {
      Item item = driveNode.getSession().getItem(path);
      if (item.isNode()) {
        return fileNode((Node) item) != null;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CloudFile> listFiles() throws DriveRemovedException, CloudDriveException, RepositoryException {
    return listFiles(rootNode());
  }

  /**
   * @inherritDoc
   */
  @Override
  @Deprecated
  // TODO not used
  public List<CloudFile> listFiles(CloudFile parent) throws DriveRemovedException,
                                                    NotCloudFileException,
                                                    RepositoryException {
    String parentPath = parent.getPath();
    Node driveNode = rootNode();
    if (parentPath.startsWith(driveNode.getPath())) {
      Item item = driveNode.getSession().getItem(parentPath);
      if (item.isNode()) {
        return listFiles((Node) item);
      } else {
        throw new NotCloudFileException("Item at path '" + parentPath
            + "' is Property and cannot be read as Cloud Drive file.");
      }
    } else {
      throw new NotCloudFileException("File '" + parentPath + "' does not belong to '" + title()
          + "' Cloud Drive.");
    }
  }

  // ********* internals ***********

  /**
   * Internal initialization of newly connecting drive node.
   * 
   * @param rootNode a {@link Node} to initialize
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected void initDrive(Node rootNode) throws CloudDriveException, RepositoryException {
    Session session = rootNode.getSession();

    rootNode.addMixin(ECD_CLOUDDRIVE);
    if (!rootNode.hasProperty("exo:title")) {
      // default title TODO
      rootNode.setProperty("exo:title", titleCached = rootTitle(getUser()));
    } else {
      titleCached = rootNode.getProperty("exo:title").getString();
    }

    rootNode.setProperty("ecd:connected", false);
    // know who actually initialized the drive
    rootNode.setProperty("ecd:localUserName", session.getUserID());
    rootNode.setProperty("ecd:initDate", Calendar.getInstance());
    // TODO how to store provider properly? need store its API version?
    rootNode.setProperty("ecd:provider", getUser().getProvider().getId());

    // XXX ecd:id and ecd:url should be set in actual impl of the drive where they are known
  }

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

      // TODO cleanup
      // ConnectCommand connect = currentConnect.get();
      // if (connect == null) {
      // synchronized (currentConnect) {
      // // check again in synchronized block!
      // connect = currentConnect.get();
      // if (connect == null) {
      // connect = getConnectCommand();
      // currentConnect.set(connect);
      // connect.start();
      // }
      // }
      // } // else, connect already active

      // TODO cleanup connect already active, wait if it's not async request
      // if (!async) {
      // try {
      // connect.await();
      // } catch (InterruptedException e) {
      // LOG.warn("Caller of connect command interrupted.", e);
      // Thread.currentThread().interrupt();
      // } catch (ExecutionException e) {
      // Throwable err = e.getCause();
      // if (err instanceof CloudDriveException) {
      // throw (CloudDriveException) err;
      // } else if (err instanceof RepositoryException) {
      // throw (RepositoryException) err;
      // } else if (err instanceof RuntimeException) {
      // throw (RuntimeException) err;
      // } else {
      // throw new UndeclaredThrowableException(err, "Error connecting drive: " + err.getMessage());
      // }
      // }
      // }

      return connect;
    }
  }

  /**
   * Factory method to create an actual implementation of {@link ConnectCommand} command.
   * 
   * @throws DriveRemovedException
   * @throws RepositoryException
   * @return {@link ConnectCommand} instance
   */
  protected abstract ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException;

  /**
   * Factory method to create an instance of {@link SyncCommand} command.
   * 
   * @throws DriveRemovedException
   * @throws SyncNotSupportedException
   * @throws RepositoryException
   * @return {@link SyncCommand} instance
   */
  protected abstract SyncCommand getSyncCommand() throws DriveRemovedException,
                                                 SyncNotSupportedException,
                                                 RepositoryException;

  /**
   * Factory method to create a instance of {@link CloudFileAPI} supporting exact cloud provider.
   * 
   * @param file {@link Node} to move
   * @throws DriveRemovedException
   * @throws SyncNotSupportedException
   * @throws RepositoryException
   * @return {@link CloudFileAPI} instance
   */
  protected abstract CloudFileAPI createFileAPI() throws DriveRemovedException,
                                                 SyncNotSupportedException,
                                                 RepositoryException;

  /**
   * {@inheritDoc}
   */
  @Override
  protected synchronized void disconnect() throws CloudDriveException, RepositoryException {
    // mark as disconnected and clean local storage
    if (isConnected()) {
      try {
        Node rootNode = rootNode();
        try {
          // stop commands pool
          commandExecutor.stop();

          rootNode.setProperty("ecd:connected", false);

          // remove all existing cloud files
          for (NodeIterator niter = rootNode.getNodes(); niter.hasNext();) {
            niter.nextNode().remove();
          }

          rootNode.save();

          // finally fire listeners
          listeners.fireOnDisconnect(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()));
        } catch (RepositoryException e) {
          rollback(rootNode);
          throw e;
        } catch (RuntimeException e) {
          rollback(rootNode);
          throw e;
        }
      } catch (ItemNotFoundException e) {
        // it is already removed
        throw new DriveRemovedException("Drive '" + title() + "' was removed.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command synchronize() throws SyncNotSupportedException,
                              DriveRemovedException,
                              CloudDriveException,
                              CloudDriveAccessException,
                              RepositoryException {

    // if other sync in progress, use that process (as a current)
    // if no process, start a new sync process
    // if file sync in progress, wait for it and then start a new sync

    if (isConnected()) {
      checkAccess();

      SyncCommand sync;
      if (currentSync.compareAndSet(noSyncCommand, sync = getSyncCommand())) {
        sync.start();
      } else {
        SyncCommand existingSync = currentSync.get();
        if (existingSync != noSyncCommand) {
          sync = existingSync; // use existing
        } else {
          sync.start();
        }
      }

      return sync;
    } else {
      throw new NotConnectedException("Cloud drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Command synchronize(Node file) throws SyncNotSupportedException,
                                       NotConnectedException,
                                       CloudDriveException,
                                       RepositoryException {
    // if drive sync already in progress - wait for its finish, then start this file sync
    // if another file sync in progress and it is a file from this file hierarchy, then wait for it and then
    // start this file sync
    // if another file sync in progress and it is a file not from this file hierarchy, then start file sync
    // immediately
    // if the same file in progress, use that process (as a current)

    if (isConnected()) {
      throw new SyncNotSupportedException("Synchronization not supported for single cloud file: "
          + file.getPath() + ". Use the whole drive synchronization instead.");

      // TODO cleanup
      // final String filePath = file.getPath();
      // final String rootPath = rootNode().getPath();
      // if (filePath.equals(rootPath)) {
      // // sync whole drive
      // return synchronize();
      // } else if (filePath.startsWith(rootPath)) {
      // // it's file sync
      // SyncFileCommand sync;
      // SyncFileCommand existingSync = currentSyncFiles.putIfAbsent(filePath, sync =
      // getSyncFileCommand(file));
      // if (existingSync != null) {
      // sync = existingSync;
      // } else {
      // sync.start();
      // }
      // return sync;
      // } else {
      // throw new
      // SyncNotSupportedException("Synchronization not supported for a file outside of cloud drive: "
      // + filePath);
      // }
    } else {
      throw new NotConnectedException("Cloud drive not connected. Cannot synchronize " + file.getPath());
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isConnected() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:connected").getBoolean();
  }

  // ============== JCR impl specific methods ==============

  // /**
  // * Create cloud file from given node.
  // *
  // * @param file {@link Node} a node representing a new cloud file in the drive.
  // * @return {@link FileChange}
  // * @throws SyncNotSupportedException
  // * @throws CloudDriveException
  // * @throws RepositoryException
  // */
  // @Deprecated
  // protected FileChange create(Node file) throws SyncNotSupportedException,
  // CloudDriveException,
  // RepositoryException {
  // // before we will proceed with a change, we have to clean the file node name
  // // we do this before the change creation and registration by its path in fileChanges map
  // file = cleanNode(file, fileAPI.getTitle(file));
  //
  // FileCreate change = new FileCreate(file);
  // change.apply();
  // return change;
  // // TODO cleanup
  // // CreateFileCommand create = getCreateFileCommand(file);
  // // return startFileSync(filePath, create);
  // }
  //
  // /**
  // * Untrash cloud file from given node.
  // *
  // * @param file {@link Node} a node representing trashed cloud file in Trash folder.
  // * @return {@link FileChange}
  // * @throws SyncNotSupportedException
  // * @throws CloudDriveException
  // * @throws RepositoryException
  // */
  // @Deprecated
  // protected FileChange untrash(Node file) throws SyncNotSupportedException,
  // CloudDriveException,
  // RepositoryException {
  // FileUntrash change = new FileUntrash(file);
  // change.apply();
  // return change;
  // }
  //
  // /**
  // * Update cloud file from given node.
  // *
  // * @param file {@link Node} a node representing a cloud file in the drive.
  // * @return {@link FileChange}
  // * @throws SyncNotSupportedException
  // * @throws CloudDriveException
  // * @throws RepositoryException
  // */
  // @Deprecated
  // protected FileChange update(Node file) throws SyncNotSupportedException,
  // CloudDriveException,
  // RepositoryException {
  // FileUpdate change = new FileUpdate(file);
  // change.apply();
  // return change;
  // // TODO cleanup
  // // UpdateFileCommand create = getUpdateFileCommand(file);
  // // return startFileSync(filePath, create);
  // }
  //
  // /**
  // * Update cloud file content from given node.
  // *
  // * @param file {@link Node} a node representing a cloud file in the drive.
  // * @return {@link FileChange}
  // * @throws SyncNotSupportedException
  // * @throws CloudDriveException
  // * @throws RepositoryException
  // */
  // @Deprecated
  // protected FileChange updateContent(Node file) throws SyncNotSupportedException,
  // NotConnectedException,
  // CloudDriveException,
  // RepositoryException {
  // FileContentUpdate change = new FileContentUpdate(file);
  // change.apply();
  // return change;
  // // TODO cleanup
  // // UpdateFileContentCommand create = getUpdateFileContentCommand(file);
  // // return startFileSync(filePath, create);
  // }

  /**
   * Init cloud file removal as planned, this command will complete on parent node save. This method created
   * for use from JCR pre-remove action.
   * 
   * @param file {@link Node} a node representing a file in the drive.
   * @return {@link FileChange}
   * @throws SyncNotSupportedException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected FileChange initRemove(Node file) throws SyncNotSupportedException,
                                            CloudDriveException,
                                            RepositoryException {
    // Note: this method also can be invoked via RemoveCloudFileAction on file trashing in Trash service of
    // the ECMS

    final String filePath = file.getPath();
    FileChange remove = new FileChange(file.getPath(),
                                       fileAPI.getId(file),
                                       FileChange.REMOVE,
                                       synchronizer(file));
    Map<String, FileChange> planned = fileRemovals.get();
    if (planned != null) {
      FileChange existing = planned.get(filePath);
      if (existing == null) {
        planned.put(filePath, remove);
      } else {
        // usually we will not have more that one removal of the same path,
        // but in case of removal from parent with same-name-siblings it's still possible
        // chain if only this file id not yet chained to the existing change
        FileChange next = remove;
        boolean canChain = true;
        while (next != null && canChain) {
          canChain = next.fileId != null && remove.fileId != null ? next.fileId != remove.fileId : true;
          next = next.next != null ? next.next : null;
        }
        if (canChain) {
          // we don't check returned status as all this happens in single thread and the existing change
          // cannot be already applied at this point
          existing.chain(remove);
        }
      }
    } else {
      planned = new HashMap<String, FileChange>();
      planned.put(filePath, remove);
      fileRemovals.set(planned);
    }
    return remove;
  }

  /**
   * Remove cloud file associated with given node path or trash the file if moved to Trash. File removal
   * should be planned previously in {@link #initRemove(Node)} method or file should be trashed in
   * {@link #confirmTrashed(Node)}.
   * 
   * @param filePath {@link String} path to a node representing the file in the drive.
   * @return <code>true</code> if file was successfully removed or trashed, <code>false</code> if file removal
   *         wasn't previously initiated
   * @throws SyncNotSupportedException
   * @throws NotConnectedException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  @Deprecated
  protected boolean remove(String filePath) throws CloudDriveException, RepositoryException {

    // // #1 check if it is not a direct JCR remove
    // Map<String, FileRemove> planned = fileRemovals.get();
    // if (planned != null) {
    // FileChange remove = planned.remove(filePath);
    // if (remove != null) {
    // remove.apply();
    // return true;
    // }
    // }
    //
    // // #2 apply if it is a move to Trash
    // // put dummy FileTrash here
    // FileTrash trash = new FileTrash(filePath);
    // FileTrash existing = trashed.putIfAbsent(filePath, trash);
    // if (existing != null) {
    // existing.apply();
    // } else {
    // trash.apply();
    // }
    //
    // #3 in case of timeout of FileTrash, we assume it is not a file removal at all (file could be moved)
    return true;
  }

  CloudFileSynchronizer synchronizer(Node file) throws RepositoryException,
                                               SkipSyncException,
                                               SyncNotSupportedException,
                                               CloudDriveException {
    for (CloudFileSynchronizer s : fileSynchronizers) {
      if (s.accept(file)) {
        return s;
      }
    }
    throw new SyncNotSupportedException("Synchronization not supported for file type "
        + file.getPrimaryNodeType().getName() + " in node " + file.getPath());
  }

  protected void addChanged(String fileId, String changeType) throws DriveRemovedException,
                                                             RepositoryException {
    Set<String> changes = fileChanged.get(fileId);
    if (changes == null) {
      changes = new LinkedHashSet<String>();
      Set<String> existing = fileChanged.putIfAbsent(fileId, changes);
      if (existing != null) {
        changes = existing;
      }
    }
    changes.add(changeType + getChangeId());
  }

  protected boolean hasChanged(String fileId, String changeType) throws DriveRemovedException,
                                                                RepositoryException {
    Set<String> changes = fileChanged.get(fileId);
    if (changes != null) {
      return changes.contains(changeType + getChangeId());
    }
    return false;
  }

  protected void removeChanged(String fileId, String changeType) throws DriveRemovedException,
                                                                RepositoryException {
    Set<String> changes = fileChanged.get(fileId);
    if (changes != null) {
      changes.remove(changeType + getChangeId());
      if (changes.size() == 0) {
        synchronized (changes) {
          if (changes.size() == 0) {
            fileChanged.remove(fileId);
          }
        }
      }
    }
  }

  protected Collection<String> getChanged(String fileId) {
    Set<String> changes = fileChanged.get(fileId);
    if (changes != null) {
      return changes;
    }
    return Collections.emptyList();
  }

  /**
   * Start command of given file synchronization.
   * 
   * @param filePath {@link String}
   * @param sync {@link FileCommand}
   * @return {@link FileCommand} actually executing command
   * @throws CloudDriveException
   */
  @Deprecated
  protected FileCommand startFileSync(String filePath, FileCommand sync) throws CloudDriveException {
    FileCommand existing = currentSyncFiles.putIfAbsent(filePath, sync);
    if (existing != null) {
      existing.chain(sync);
      return existing;
    } else {
      sync.start();
      return sync;
    }
  }

  /**
   * Finish command of given file synchronization.
   * 
   * @param filePath {@link String}
   * @param sync {@link FileCommand}
   * @return {@link FileCommand} a command is executing currently for given file path, it can be a next
   *         from chained or an one started by another thread
   * @throws CloudDriveException
   */
  @Deprecated
  protected FileCommand finishFileSync(String filePath, FileCommand sync) throws CloudDriveException {
    if (sync.next != null) {
      if (currentSyncFiles.replace(filePath, sync, sync.next)) {
        sync.next.start();
        return sync.next;
      } else {
        // this should not happen, we will start the next anyway (XXX workaround for a case)
        sync.next.start();
        LOG.warn("Cannot replace completed file sync with the chained one. Target file: " + filePath);
      }
    } else if (currentSyncFiles.remove(filePath, sync)) {
      return null;
    }
    return currentSyncFiles.get(filePath);
  }

  /**
   * Wait for all files sync completion in this drive or its sub-folder pointed by given path.
   * 
   * @param filePath {@link String}
   * @throws CloudDriveException
   */
  @Deprecated
  protected void waitFileSync(String filePath) throws CloudDriveException {
    // wait for the drive files sync
    long now = System.currentTimeMillis();
    List<FileCommand> predecessors = new ArrayList<FileCommand>();
    for (Map.Entry<String, FileCommand> se : currentSyncFiles.entrySet()) {
      FileCommand c = se.getValue();
      long cst = c.startTime.get();
      // wait only for this or sub-files and for already started (cst>0) commands
      if (se.getKey().startsWith(filePath) && cst > 0 && cst <= now) {
        // a file from this file (folder or drive) hierarchy already in progress, will wait for it
        predecessors.add(c);
      }
    }
    if (!predecessors.isEmpty()) {
      // wait for fixed set of previous commands with a lock,
      // such lock not very efficient, but a better way will be only using locking by path prefixes (sub-tree)
      synchronized (currentSyncFiles) {
        for (FileCommand c : predecessors) {
          try {
            c.await();
          } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for a file sync: " + e.getMessage());
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            // we skip this error and will proceed with the current command
            LOG.warn("Error while waiting for a file sync: " + e.getMessage());
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isDrive(Node node, boolean includeFiles) throws DriveRemovedException,
                                                            RepositoryException {
    Node driveNode = rootNode();
    return driveNode.getProperty("ecd:connected").getBoolean()
        && (driveNode.isSame(node) || (includeFiles ? node.getPath().startsWith(driveNode.getPath())
            && fileNode(node) != null : false));
  }

  /**
   * Return {@link Node} instance representing some cloud file, i.e. if given Node is of acceptable type.
   * 
   * @param node {@link Node}
   * @return {@link Node}
   * @throws RepositoryException
   */
  protected Node fileNode(Node node) throws RepositoryException {
    Node parent;
    if (node.isNodeType(ECD_CLOUDFILE)) {
      return node;
    } else if (node.isNodeType(ECD_CLOUDFILERESOURCE)
        && (parent = node.getParent()).isNodeType(ECD_CLOUDFILE)) {
      return parent;
    } else {
      return null;
    }
  }

  protected Session systemSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
    if (ssp != null) {
      return ssp.getSession(rootWorkspace, repository);
    }
    throw new RepositoryException("Cannot get session provider.");
  }

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
   * @throws RepositoryException
   */
  protected Node rootNode() throws DriveRemovedException, RepositoryException {
    SoftReference<Node> rootNodeRef = rootNodeHolder.get();
    Node rootNode;
    if (rootNodeRef != null) {
      rootNode = rootNodeRef.get();
      ConversationState cs = ConversationState.getCurrent();
      if (rootNode != null && rootNode.getSession().isLive() && cs != null
          && rootNode.getSession().getUserID().equals(cs.getIdentity().getUserId())) {
        try {
          // XXX as more light alternative rootNode.getIndex() can be used to
          // force state check, but refresh is good for long living nodes (and
          // soft ref will do long live until memory will be available)
          rootNode.refresh(true);
          return rootNode;
        } catch (InvalidItemStateException e) {
          // probably root node already removed
          throw new DriveRemovedException("Drive " + title() + " was removed.", e);
        } catch (RepositoryException e) {
          // if JCR error - need new node instance
        }
      }
    }

    Session session = session();
    try {
      rootNode = session.getNodeByUUID(rootUUID);
    } catch (ItemNotFoundException e) {
      // it is already removed
      throw new DriveRemovedException("Drive " + title() + " was removed.", e);
    }
    rootNodeHolder.set(new SoftReference<Node>(rootNode));
    return rootNode;
  }

  /**
   * Rollback all changes made to JCR node storage of the drive. Used in public API methods.
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
   * @param commandDescription {@link String}
   */
  void handleError(Node rootNode, Throwable error, String commandDescription) {
    String rootPath = null;
    if (rootNode != null) {
      try {
        rootPath = rootNode.getPath();
      } catch (RepositoryException e) {
        LOG.warn("Error reading drive root '" + e.getMessage() + "' "
            + (commandDescription != null ? "of " + commandDescription + " command " : "")
            + "to listeners on Cloud Drive '" + title() + "':" + e.getMessage());
      }

      if (commandDescription.equals("connect")) {
        try {
          // XXX it's workaround to prevent NPE in JCR Observation
          removeJCRListener(rootNode.getSession());
        } catch (Throwable e) {
          LOG.warn("Error removing observation listener on connect error '" + error.getMessage() + "' "
              + " on Cloud Drive '" + title() + "':" + e.getMessage());
        }
      }

      rollback(rootNode);
    }

    try {
      listeners.fireOnError(new CloudDriveEvent(getUser(), rootWorkspace, rootPath), error);
    } catch (Throwable e) {
      LOG.warn("Error firing error '" + error.getMessage() + "' "
          + (commandDescription != null ? "of " + commandDescription + " command " : "")
          + "to listeners on Cloud Drive '" + title() + "':" + e.getMessage());
    }
  }

  private Node openNode(String fileId, String fileTitle, Node parent, String nodeType) throws RepositoryException,
                                                                                      CloudDriveException {
    Node node;
    String parentPath = parent.getPath();
    String cleanName = cleanName(fileTitle);
    String name = cleanName;

    int siblingNumber = 1;
    do {
      try {
        node = parent.getNode(name);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already extends the file NT
        if (node.isNodeType(ECD_CLOUDFILE)) {
          if (fileId.equals(node.getProperty("ecd:id").getString())) {
            break; // we found node
          } else {
            // find new name for the local file
            StringBuilder newName = new StringBuilder();
            newName.append(cleanName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          }
        } else {
          try {
            synchronizer(node).create(node, fileAPI);
            break;
          } catch (SyncNotSupportedException e) {
            throw new CloudDriveException("Cannot open cloud file. Another node exists in the drive with "
                + "the same name and synchronization not supported for its nodetype ("
                + node.getPrimaryNodeType().getName() + "): " + parentPath + "/" + name, e);
          }
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it
        node = parent.addNode(name, nodeType);
        break;
      }
    } while (true);

    return node;
  }

  protected Node openFile(String fileId, String fileTitle, String fileType, Node parent) throws RepositoryException,
                                                                                        CloudDriveException {
    Node localNode = openNode(fileId, fileTitle, parent, NT_FILE);

    // create content for new not complete node
    if (localNode.isNew() && !localNode.hasNode("jcr:content")) {
      Node content = localNode.addNode("jcr:content", NT_RESOURCE);
      content.setProperty("jcr:data", DUMMY_DATA); // empty data by default
    }

    return localNode;
  }

  protected Node openFolder(String folderId, String folderTitle, Node parent) throws RepositoryException,
                                                                             CloudDriveException {
    return openNode(folderId, folderTitle, parent, NT_FOLDER);
  }

  /**
   * Move file with its subtree in scope of existing JCR session. If a node already exists at destination and
   * its id and title the same as given, then move will not be performed and existing node will be returned.
   * 
   * @param id {@link String} a file id of the Node
   * @param title {@link String} a new name of the Node
   * @param source {@link Node}
   * @param destParent {@link Node} a new parent
   * @return a {@link Node} from the destination
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  protected Node moveFile(String id, String title, Node source, Node destParent) throws RepositoryException,
                                                                                CloudDriveException {

    Node place = openNode(id, title, destParent, NT_FILE); // nt:file here, it will be removed anyway
    if (place.isNew() && !place.hasProperty("ecd:id")) {
      // this node was just created in openNode method, use its name as destination name
      String nodeName = place.getName();
      place.remove(); // clean the place

      Session session = destParent.getSession();
      String destPath = destParent.getPath() + "/" + nodeName;
      session.move(source.getPath(), destPath);
      return source; // node will reflect a new destination
    } // else node with such id and title already exists at destParent

    return place;
  }

  /**
   * Copy node with its subtree in scope of existing JCR session.
   * 
   * @param node {@link Node}
   * @param destParent {@link Node}
   * @return a {@link Node} from the destination
   * @throws RepositoryException
   */
  protected Node copyNode(Node node, Node destParent) throws RepositoryException {
    // copy Node
    Node nodeCopy = destParent.addNode(node.getName(), node.getPrimaryNodeType().getName());
    for (NodeType mixin : node.getMixinNodeTypes()) {
      String mixinName = mixin.getName();
      if (!nodeCopy.isNodeType(mixinName)) { // check if not already set by JCR actions
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
        copyNode(ecn, nodeCopy);
      }
    }
    return nodeCopy;
  }

  /**
   * Find a new non existing name for node in local JCR.
   * TODO cleanup
   * 
   * @param fileTitle {@link String}
   * @param parent {@link Node}
   * @return String with non existing name.
   * @throws RepositoryException
   */
  @Deprecated
  protected String findNodeName(String fileTitle, String parentPath, Session session) throws RepositoryException {
    String title = cleanName(fileTitle);
    String nodeName = title;
    int siblingNumber = 1;
    do {
      if (session.itemExists(parentPath + "/" + nodeName)) {
        // need another name
        StringBuilder newName = new StringBuilder();
        newName.append(title);
        newName.append('-');
        newName.append(siblingNumber);
        nodeName = newName.toString();
        siblingNumber++;
      } else {
        // we can use this name
        return nodeName;
      }
    } while (true);
  }

  /**
   * Read local nodes from the drive folder to a map by file Id. It's possible that for a single Id we have
   * several files (in different parents usually). This can be possible when Cloud Drive provider supports
   * linking or tagging/labeling where tag/label is a folder (e.g. Google Drive).
   * 
   * @param parent {@link Node}
   * @param nodes {@link Map} of {@link List} objects to fill with the parent's child nodes
   * @param deep boolean, if <code>true</code> read nodes recursive, <code>false</code> read only direct
   *          child nodes.
   * @throws RepositoryException if JCR error happen
   */
  protected void readNodes(Node parent, Map<String, List<Node>> nodes, boolean deep) throws RepositoryException {
    for (NodeIterator niter = parent.getNodes(); niter.hasNext();) {
      Node cn = niter.nextNode();
      if (cn.isNodeType(ECD_CLOUDFILE)) {
        String cnid = cn.getProperty("ecd:id").getString();
        List<Node> nodeList = nodes.get(cnid);
        if (nodeList == null) {
          nodeList = new ArrayList<Node>();
          nodes.put(cnid, nodeList);
        }
        nodeList.add(cn);
        if (deep && cn.isNodeType(ECD_CLOUDFOLDER)) {
          readNodes(cn, nodes, deep);
        }
      } else {
        if (!cn.isNodeType(ECD_IGNORED)) {
          LOG.warn("Not a cloud file detected " + cn.getPath());
        }
      }
    }
  }

  /**
   * Read local node from the given parent using file title and its id.
   * 
   * @param parent {@link Node} parent
   * @param title {@link String}
   * @param id {@link String}
   * @return {@link Node}
   * @throws RepositoryException
   */
  protected Node readNode(Node parent, String title, String id) throws RepositoryException {
    String name = cleanName(title);
    try {
      Node n = parent.getNode(name);
      if (n.isNodeType(ECD_CLOUDFILE) && id.equals(n.getProperty("ecd:id").getString())) {
        return n;
      }
    } catch (PathNotFoundException e) {
    }

    // will try find among childs with ending wildcard *
    for (NodeIterator niter = parent.getNodes(name + "*"); niter.hasNext();) {
      Node n = niter.nextNode();
      if (n.isNodeType(ECD_CLOUDFILE) && id.equals(n.getProperty("ecd:id").getString())) {
        return n;
      }
    }

    return null;
  }

  /**
   * Find local node using JCR SQL query by file id. Note, it will search only persisted nodes - not saved
   * files cannot be found in JCR.
   * 
   * @param id {@link String}
   * @return {@link Node}
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  protected Node findNode(String id) throws RepositoryException, DriveRemovedException {

    Node rootNode = rootNode();
    QueryManager qm = rootNode.getSession().getWorkspace().getQueryManager();
    Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE ecd:id='" + id
        + "' AND jcr:path LIKE '" + rootNode.getPath() + "/%'", Query.SQL);
    QueryResult qr = q.execute();
    NodeIterator nodes = qr.getNodes();
    if (nodes.hasNext()) {
      return nodes.nextNode();
    }

    return null;
  }

  /**
   * Find local nodes using JCR SQL query by array of file ids. Note, it will search only persisted nodes -
   * not saved
   * files cannot be found in JCR.
   * 
   * @param ids {@link Collection} of {@link String}
   * @return {@link NodeIterator}
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  protected NodeIterator findNodes(Collection<String> ids) throws RepositoryException, DriveRemovedException {

    Node rootNode = rootNode();
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

    Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE " + idstmt + " AND jcr:path LIKE '"
        + rootNode.getPath() + "/%'", Query.SQL);
    QueryResult qr = q.execute();
    return qr.getNodes();
  }

  protected JCRLocalCloudFile readFile(Node fileNode) throws RepositoryException {
    String fileUrl = fileNode.getProperty("ecd:url").getString();
    String previewUrl;
    try {
      previewUrl = fileNode.getProperty("ecd:previewUrl").getString();
    } catch (PathNotFoundException e) {
      previewUrl = null;
    }
    String downloadUrl;
    try {
      downloadUrl = fileNode.getProperty("ecd:downloadUrl").getString();
    } catch (PathNotFoundException e) {
      downloadUrl = null;
    }
    return new JCRLocalCloudFile(fileNode.getPath(),
                                 fileNode.getProperty("ecd:id").getString(),
                                 fileNode.getProperty("exo:title").getString(),
                                 fileUrl,
                                 previewUrl,
                                 downloadUrl,
                                 fileNode.getProperty("ecd:type").getString(),
                                 fileNode.getProperty("ecd:lastUser").getString(),
                                 fileNode.getProperty("ecd:author").getString(),
                                 fileNode.getProperty("ecd:created").getDate(),
                                 fileNode.getProperty("ecd:modified").getDate(),
                                 fileNode.isNodeType(ECD_CLOUDFOLDER));
  }

  /**
   * Init or update Cloud File structure on local JCR node.
   * 
   * @param localNode {@link Node}
   * @param title
   * @param id
   * @param type
   * @param link
   * @param previewLink, optional, can be null
   * @param downloadLink, optional, can be null
   * @param author
   * @param lastUser
   * @param created
   * @param modified
   * @throws RepositoryException
   */
  protected void initFile(Node localNode,
                          String title,
                          String id,
                          String type,
                          String link,
                          String previewLink,
                          String downloadLink,
                          String author,
                          String lastUser,
                          Calendar created,
                          Calendar modified) throws RepositoryException {
    // ecd:cloudFile
    if (!localNode.isNodeType(ECD_CLOUDFILE)) {
      localNode.addMixin(ECD_CLOUDFILE);
    }

    initCommon(localNode, title, id, type, link, author, lastUser, created, modified);

    // ecd:cloudFileResource
    // try {
    Node content = localNode.getNode("jcr:content");
    if (!content.isNodeType(ECD_CLOUDFILERESOURCE)) {
      content.addMixin(ECD_CLOUDFILERESOURCE);
    }

    // nt:resource
    content.setProperty("jcr:mimeType", type);
    content.setProperty("jcr:lastModified", modified);
    // } catch (PathNotFoundException e) {
    // no jcr:content TODO
    // }

    // optional properties, if null, ones will be removed by JCR core
    localNode.setProperty("ecd:previewUrl", previewLink);
    localNode.setProperty("ecd:downloadUrl", downloadLink);
  }

  /**
   * Init or update Cloud Folder structure on local JCR node.
   * 
   * @param cfolder {@link CloudFile}
   * @param localNode {@link Node}
   * @throws RepositoryException
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
   * Init or update Cloud File or Folder properties on local JCR node. This method assumes all mixins are set
   * on the local node.
   * 
   * @param cfile {@link CloudFile}
   * @param localNode {@link Node}
   * @throws RepositoryException
   */
  protected void initCommon(Node localNode,
                            String id,
                            String title,
                            String type,
                            String link,
                            String author,
                            String lastUser,
                            Calendar created,
                            Calendar modified) throws RepositoryException {
    localNode.setProperty("exo:title", title);
    localNode.setProperty("ecd:id", id);
    localNode.setProperty("ecd:driveUUID", rootUUID);
    localNode.setProperty("ecd:type", type);
    localNode.setProperty("ecd:url", link);
    localNode.setProperty("ecd:author", author);
    localNode.setProperty("ecd:lastUser", lastUser);
    localNode.setProperty("ecd:created", created);
    localNode.setProperty("ecd:modified", modified);
    localNode.setProperty("ecd:synchronized", Calendar.getInstance());

    if (localNode.isNodeType(EXO_DATETIME)) {
      localNode.setProperty("exo:dateCreated", created);
      localNode.setProperty("exo:dateModified", modified);
    }

    if (localNode.isNodeType(EXO_MODIFY)) {
      localNode.setProperty("exo:lastModifiedDate", modified);
      localNode.setProperty("exo:lastModifier", lastUser);
    }
  }

  /**
   * Mark given file as ignored by adding ecd:ignored mixin to it.
   * 
   * @param file {@link Node}
   * @throws RepositoryException
   */
  protected void ignoreFile(Node file) throws RepositoryException {
    if (!file.isNodeType(ECD_IGNORED)) {
      file.addMixin(ECD_IGNORED);
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
   * @throws RepositoryException
   * @return NodeRemoveHandler
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
    observation.addEventListener(handler.addListener,
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
    observation.addEventListener(handler.changesListener, Event.NODE_ADDED | Event.NODE_REMOVED
        | Event.PROPERTY_CHANGED, //
                                 driveNode.getPath(),
                                 true,
                                 null,
                                 supported.size() > 0 ? supported.toArray(new String[supported.size()])
                                                     : null,
                                 false);
    return handler;
  }

  /**
   * Remove Observation listeners for this cloud drive.
   * 
   * @param session {@link Session}
   * @throws RepositoryException
   */
  protected void removeJCRListener(Session session) throws RepositoryException {
    ObservationManager observation = session.getWorkspace().getObservationManager();
    observation.removeEventListener(jcrListener.removeListener);
    observation.removeEventListener(jcrListener.addListener);
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

  // ============== abstract ==============

  /**
   * Answer whether a content synchronization of given cloud file supported.
   * 
   * @param cloudFile {@link CloudFile}
   * @return boolean true - if synchronization supported
   */
  protected abstract boolean isSyncSupported(CloudFile cloudFile);

  /**
   * An id of the latest cloud change applied to this drive.
   * 
   * @return {@link String}
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  protected abstract String getChangeId() throws DriveRemovedException, RepositoryException;

  // ============== static ================

  /**
   * Clean up string for JCR compatible name.
   * 
   * @param String str
   * @return String - JCR compatible name of local file
   */
  protected static String cleanName(String name) {
    String str = accentsConverter.transliterate(name.trim());
    // the character ? seems to not be changed to d by the transliterate function
    StringBuilder cleanedStr = new StringBuilder(str.trim());
    // delete special character
    if (cleanedStr.length() == 1) {
      char c = cleanedStr.charAt(0);
      if (c == '.' || c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"'
          || c == '|') {
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
   * Clean given node by renaming it to JCR compatible name. Prefixed names (with ':') will not be cleaned to
   * avoid damaging system nodes.
   * 
   * @param node {@link Node}
   * @param title {@link String}
   * @return {@link Node} cleaned node, can be the same if its name already clean or prefixed.
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  public static Node cleanNode(Node node, String title) throws RepositoryException, CloudDriveException {
    String name = node.getName();
    if (name.indexOf(":") < 0) { // handle only not-prefixed JCR names (to avoid renaming system nodes)
      String cleanName = cleanName(title);
      if (!cleanName.equals(node.getName())) {
        // need rename file to a clean name
        Node parent = node.getParent();
        int siblingNumber = 1;
        name = cleanName;
        do {
          try {
            parent.getNode(name);
            // find new name for the node
            StringBuilder newName = new StringBuilder();
            newName.append(cleanName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          } catch (PathNotFoundException e) {
            // no such node exists, rename to this name
            Session session = parent.getSession();
            session.move(node.getPath(), parent.getPath() + "/" + name);
            break;
          }
        } while (true);
      }
    }
    return node; // node will reflect new destination in case of move
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
   * Check if given node isn't a node in eXo Trash folder and throw {@link DriveRemovedException} if it is.
   * 
   * @param node {@link Node}
   * @throws RepositoryException
   * @throws DriveRemovedException if given node in the eXo Trash.
   */
  public static void checkTrashed(Node node) throws RepositoryException, DriveRemovedException {
    if (node.getParent().isNodeType(EXO_TRASHFOLDER)) {
      throw new DriveRemovedException("Drive " + node.getPath() + " was moved to Trash.");
    }
  }

  /**
   * Migrate Cloud Drive root node naming from title based (PROVIDER_NAME - user@email) to transliterated
   * title (for ECMS compatibility). Note that node will be saved during the migration: all transient changes
   * will be saved as well. If given node not a root of cloud drive this method will do nothing.
   * 
   * @param node {@link Node}
   * @throws RepositoryException
   */
  public static void migrateName(Node node) throws RepositoryException {
    if (node.isNodeType(ECD_CLOUDDRIVE)) {
      // root node has a property (not exactly defined): ecd:localFormat. It is a string with version of
      // format.
      boolean upgrade;
      try {
        double localFormat = node.getProperty(ECD_LOCALFORMAT).getDouble();
        if (localFormat == CURRENT_LOCALFORMAT) {
          upgrade = false;
        } else {
          // local format unknown
          LOG.warn("Local format unknown: " + localFormat + ". Supported format: " + CURRENT_LOCALFORMAT
              + " or lower.");
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
   * Create a name for Cloud Drive root node.
   * 
   * @param user {@link CloudUser}
   * @return String with a text of root node for given user
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  public static String rootName(CloudUser user) throws RepositoryException, DriveRemovedException {
    return cleanName(rootTitle(user));
  }

  /**
   * Create a title for Cloud Drive root node.
   * 
   * @param user {@link CloudUser}
   * @return String with a text of root node for given user
   * @throws RepositoryException
   * @throws DriveRemovedException
   */
  public static String rootTitle(CloudUser user) throws RepositoryException, DriveRemovedException {
    return user.getProvider().getName() + " - " + user.getEmail();
  }
}
