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
import org.exoplatform.clouddrive.ConflictException;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.FileTrashRemovedException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.NotYetCloudFileException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.SkipSyncException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive.JCRListener.AddTrashListener;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive.JCRListener.DriveChangesListener;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.clouddrive.utils.IdentityHelper;
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

  public static final String     EXO_THUMBNAILS        = "exo:thumbnails";

  public static final String     EXO_THUMBNAIL         = "exo:thumbnail";

  public static final String     NT_FOLDER             = "nt:folder";

  public static final String     NT_FILE               = "nt:file";

  public static final String     NT_RESOURCE           = "nt:resource";

  public static final String     NT_UNSTRUCTURED       = "nt:unstructured";

  public static final String     ECD_LOCALFORMAT       = "ecd:localFormat";

  public static final double     CURRENT_LOCALFORMAT   = 1.1d;

  public static final long       HISTORY_EXPIRATION    = 1000 * 60 * 60 * 24 * 8; // 8 days

  public static final int        HISTORY_MAX_LENGTH    = 1000;                   // 1000 file modification

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

  class FileTrashing {
    private final CountDownLatch latch  = new CountDownLatch(1);

    /**
     * Path of a file in Trash folder. Will be initialized by {@link AddTrashListener} if it is a real
     * trashing, <code>null</code> otherwise.
     */
    private String               trashPath;

    /**
     * Cloud file id. Will be initialized by {@link AddTrashListener} if it is a real
     * trashing, <code>null</code> otherwise.
     */
    private String               fileId;

    private boolean              remove = false;

    void confirm(String path, String fileId) {
      this.trashPath = path;
      this.fileId = fileId;
      latch.countDown();
    }

    void remove() {
      remove = true;
    }

    void complete() throws InterruptedException, RepositoryException {
      // Remove trashed file if asked explicitly, but first wait for 60 sec for trashing confirmation.
      // If not confirmed - the file cannot be removed from the Trash locally (we don't know path and/or id).
      latch.await(60, TimeUnit.SECONDS);
      if (remove) {
        removeTrashed();
      }
    }

    private void removeTrashed() throws RepositoryException {
      if (trashPath != null && fileId != null) {
        Session session = systemSession();

        Item trashed = session.getItem(trashPath);
        Node trash;
        if (trashed.isNode()) {
          Node file = (Node) trashed;
          if (fileAPI.isFile(file)) {
            if (fileId.equals(fileAPI.getId(file))
                && rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
              file.remove();
              session.save();
              return;
            }
          }
          trash = trashed.getParent();
        } else {
          trash = trashed.getParent().getParent();
        }

        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE ecd:id=" + fileId
            + " AND jcr:path LIKE '" + trash.getPath() + "/%'", Query.SQL);
        QueryResult qr = q.execute();
        for (NodeIterator niter = qr.getNodes(); niter.hasNext();) {
          Node file = niter.nextNode();
          if (rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
            file.remove();
          }
        }

        session.save();
      }
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
                      if (LOG.isDebugEnabled()) {
                        LOG.debug("Cloud Drive trashed " + path);
                      }
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
                } else if (fileAPI.isFile(node)) {
                  // file trashed, but accept only this drive files (jcr listener will be fired for all
                  // existing drives)
                  if (rootUUID.equals(node.getProperty("ecd:driveUUID").getString())) {
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Cloud drive item trashed " + path);
                    }

                    // mark the file node to be able properly untrash it,
                    node.setProperty("ecd:trashed", true);
                    node.save();

                    // confirm file trashing (for FileChange NODE_REMOVED changes)
                    // this change also happens on node reordering in Trash when untrashing the same name
                    String fileId = fileAPI.getId(node);
                    FileTrashing confirmation;
                    FileTrashing existing = fileTrash.putIfAbsent(fileId, confirmation = new FileTrashing());
                    if (existing != null) {
                      existing.confirm(path, fileId); // confirm already posted trash
                    } else {
                      confirmation.confirm(path, fileId); // confirm just posted trash
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

    class DriveChangesListener extends BaseCloudDriveListener implements EventListener {

      final ThreadLocal<AtomicLong> lock = new ThreadLocal<AtomicLong>();

      void disable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(0));
        }
        requests.incrementAndGet();
      }

      void enable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(0));
        }
        requests.decrementAndGet();
      }

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
            while (events.hasNext()) {
              Event event = events.nextEvent();
              String eventPath = event.getPath();

              if (eventPath.endsWith("jcr:mixinTypes") || eventPath.endsWith("jcr:content")
                  || eventPath.indexOf("ecd:") >= 0 || eventPath.indexOf("/exo:thumbnails") > 0) {
                continue; // XXX hardcoded undesired system stuff to skip
              }

              if (event.getType() == Event.NODE_REMOVED) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Cloud file removed " + eventPath);
                }
                // Removal should be initiated by initRemove() before this event happening and in the same
                // thread.
                // For direct JCR removals this should be done by RemoveCloudFileAction. Then this removal
                // will succeed.
                // In case of move to Trash, it should be fully handled by AddTrashListener, it will run a
                // dedicated command for it.
                // If removal not initiated (in this thread), then it's move/rename - it will be handled by
                // NODE_ADDED below.

                // check if it is not a direct JCR remove or ECMS trashing (in this thread)
                Map<String, FileChange> removed = fileRemovals.get();
                if (removed != null) {
                  FileChange remove = removed.remove(eventPath);
                  if (remove != null) {
                    changes.add(remove);
                  }
                } // otherwise it's not removal (may be a move or ordering)
              } else {
                if (event.getType() == Event.NODE_ADDED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud file added. User: " + event.getUserID() + ". Path: " + eventPath);
                  }
                  changes.add(new FileChange(eventPath, FileChange.CREATE));
                } else if (event.getType() == Event.PROPERTY_CHANGED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Cloud file property changed. User: " + event.getUserID() + ". Path: "
                        + eventPath);
                  }
                  changes.add(new FileChange(eventPath, FileChange.UPDATE));
                } // otherwise, we skip the event
              }
            }

            if (changes.size() > 0) {
              // start files sync
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
      command.exec();
      return command;
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

    protected final Map<Command, ExoJCRSettings> config = Collections.synchronizedMap(new HashMap<Command, ExoJCRSettings>());

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
      super.prepare(command);
      ExoJCRSettings settings = config.get(command);
      if (settings != null) {
        settings.prevConversation = ConversationState.getCurrent();
        ConversationState.setCurrent(settings.conversation);

        // set correct container
        settings.prevContainer = ExoContainerContext.getCurrentContainerIfPresent();
        ExoContainerContext.setCurrentContainer(settings.container);

        // set correct SessionProvider
        settings.prevSessions = sessionProviders.getSessionProvider(null);
        SessionProvider sp = new SessionProvider(settings.conversation);
        sessionProviders.setSessionProvider(null, sp);
      } else {
        throw new CloudDriveException(this.getClass().getName() + " setting not configured for " + command
            + " to be prepared.");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup(Command command) throws CloudDriveException {
      ExoJCRSettings settings = config.get(command);
      if (settings != null) {
        ConversationState.setCurrent(settings.prevConversation);
        ExoContainerContext.setCurrentContainer(settings.prevContainer);
        SessionProvider sp = sessionProviders.getSessionProvider(null);
        sessionProviders.setSessionProvider(null, settings.prevSessions);
        sp.close();
      } else {
        throw new CloudDriveException(this.getClass().getName() + " setting not configured for " + command
            + " to be cleaned.");
      }
      super.cleanup(command);
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    /**
     * Processing logic.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     * @throws InterruptedException
     */
    protected abstract void process() throws CloudDriveAccessException,
                                     CloudDriveException,
                                     RepositoryException,
                                     InterruptedException;

    /**
     * Start command execution. If command will fail due to provider error, the execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the throwing an exception.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected final void exec() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      startTime.set(System.currentTimeMillis());

      try {
        commandEnv.prepare(this); // prepare environment

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
            if (!Thread.currentThread().isInterrupted() && getUser().getProvider().retryOnProviderError()) {
              attemptNumb++;
              if (attemptNumb > CloudDriveConnector.PROVIDER_REQUEST_ATTEMPTS) {
                throw e;
              } else {
                rollback(rootNode);
                LOG.warn("Error running " + getName() + " command. " + e.getMessage()
                    + ". Rolled back and will run next attempt in "
                    + CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT + "ms.");
                Thread.sleep(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT);
              }
            } else {
              throw e;
            }
          }
        }

        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Drive " + getName() + " interrupted in " + title());
        } else {
          LOG.warn("Drive " + getName() + " finished unexpectedly.");
        }
      } catch (CloudDriveException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RepositoryException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (InterruptedException e) {
        // special case: the command canceled
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        Thread.currentThread().interrupt();
        throw new CloudDriveException("Drive " + getName() + " canceled", e);
      } catch (RuntimeException e) {
        handleError(rootNode, e, getName());
        commandEnv.fail(this, e);
        e.printStackTrace();
        throw e;
      } finally {
        doneAction();
        jcrListener.enable();
        commandEnv.cleanup(this); // cleanup environment
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
      try {
        return async = commandExecutor.submit(getName(), new CommandCallable(this));
      } catch (InterruptedException e) {
        LOG.warn("Command executor interrupted and cannot submit " + getName() + " for drive " + title()
            + ". " + e.getMessage());
        Thread.currentThread().interrupt();
        throw new CloudDriveException("Drive " + getName() + " interrupted for " + title(), e);
      }
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
    protected void process() throws CloudDriveException, RepositoryException, InterruptedException {
      // fetch all files to local storage
      fetchFiles();

      // check before saving the result
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Drive connection interrupted for " + title());
      }

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
    protected void process() throws CloudDriveAccessException,
                            CloudDriveException,
                            RepositoryException,
                            InterruptedException {
      syncLock.writeLock().lock(); // write-lock acquired exclusively by single threads (drive sync)
      try {
        // don't do drive sync with not applied previous file changes
        List<FileChange> changes = savedChanges();
        if (changes.size() > 0) {
          // run sync in this thread and w/o locking the syncLock
          new SyncFilesCommand(changes).sync(rootNode);
        }

        syncFiles();

        // check before saving the result
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Drive synchronization interrupted for " + title());
        }

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
      String rootId = fileAPI.getId(rootNode);
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

  protected class SyncFilesCommand extends AbstractCommand {

    static final String    NAME = "files synchronization";

    final List<FileChange> changes;

    SyncFilesCommand(List<FileChange> changes) {
      this.changes = changes;
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
    protected void process() throws CloudDriveAccessException,
                            CloudDriveException,
                            RepositoryException,
                            InterruptedException {
      if (isConnected()) {
        // wait for whole drive sync
        syncLock.readLock().lock(); // read-lock can be acquired by multiple threads (file syncs)
        try {
          // save observed changes to the drive store, they will be reset in case of success in sync()
          saveChanges(changes);

          sync(rootNode);
        } finally {
          syncLock.readLock().unlock();
        }

        // TODO fire listeners afterwards with detailed event?
        // listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()));
      } else {
        LOG.warn("Cannot synchronize file in cloud drive '" + title() + "': drive not connected");
      }
    }

    void sync(Node driveNode) throws RepositoryException, CloudDriveException, InterruptedException {
      Set<String> ignoredPaths = new HashSet<String>(); // for not supported by sync
      Set<String> updated = new HashSet<String>(); // to reduce update changes of the same file

      next: for (Iterator<FileChange> chiter = changes.iterator(); chiter.hasNext()
          && !Thread.currentThread().isInterrupted();) {
        FileChange change = chiter.next();
        for (String ipath : ignoredPaths) {
          if (change.getPath().startsWith(ipath)) {
            continue next; // skip parts of ignored (not supported by sync) nodes
          }
        }

        try {
          change.initUpdated(updated);
          change.apply();
        } catch (SyncNotSupportedException e) {
          // remember to skip sub-files
          ignoredPaths.add(change.getPath());
        } catch (PathNotFoundException e) {
          if (change.changeType.equals(FileChange.REMOVE)) {
            // well... it is already removed - ignore it
            LOG.warn("Ignoring already removed item removal: " + change.fileId + " " + change.getPath(), e);
          } else if (change.changeType.equals(FileChange.CREATE)) {
            // well... it was existing and need add to the cloud, but already removed locally - ignore it
            LOG.warn("Ignoring already removed item creation: " + change.getPath(), e);
          } else if (e.getMessage().indexOf("/exo:thumbnails") > 0
              && change.getPath().indexOf("/exo:thumbnails") > 0) {
            // XXX hardcode ignorance of exo:thumbnails here also,
            // it's possible that thumbnails' child nodes will disappear, thus we ignore them
            ignoredPaths.add(change.getPath());
          } else {
            throw e;
          }
        }
      }

      // check before saving the result
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Files synchronization interrupted in " + title());
      }

      driveNode.save(); // save the drive

      // reset changes store saved in the drive
      commitChanges(changes);
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
    public boolean isIgnored(Node node) throws RepositoryException {
      return node.isNodeType(ECD_IGNORED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ignoreFile(Node node) throws RepositoryException {
      if (!node.isNodeType(ECD_IGNORED)) {
        node.addMixin(ECD_IGNORED);
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
    public Collection<String> findParents(Node fileNode) throws DriveRemovedException, RepositoryException {
      Set<String> parentIds = new LinkedHashSet<String>();
      for (NodeIterator niter = findNodes(Arrays.asList(getId(fileNode))); niter.hasNext();) {
        Node p = niter.nextNode().getParent(); // parent it is a cloud file or a cloud drive
        parentIds.add(p.getProperty("ecd:id").getString());
      }
      return Collections.unmodifiableCollection(parentIds);
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

    final boolean              isFolder;

    final String               changeType;

    String                     changeId;

    String                     filePath;

    String                     fileId;

    /**
     * Referenceable file UUID for removal (optional for other operations).
     */
    String                     fileUUID;

    FileChange                 next;

    CloudFileSynchronizer      synchronizer;

    Set<String>                updated;

    /**
     * Constructor for newly observed change. See {@link DriveChangesListener}.
     * 
     * @param path
     * @param fileId
     * @param isFolder
     * @param changeType
     * @param synchronizer
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    FileChange(String path,
               String fileId,
               boolean isFolder,
               String changeType,
               CloudFileSynchronizer synchronizer) throws CloudDriveException, RepositoryException {
      this.changeId = nextChangeId();
      this.path = path;
      this.fileId = fileId;
      this.isFolder = isFolder;
      this.changeType = changeType;
      this.synchronizer = synchronizer;
    }

    /**
     * Constructor for resumed change. See {@link #parseChanges()}.
     * 
     * @param changeId
     * @param path
     * @param fileId
     * @param isFolder
     * @param changeType
     * @param synchronizer
     * @throws DriveRemovedException
     * @throws RepositoryException
     */
    FileChange(String changeId,
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

    FileChange(String filePath, String changeType) throws RepositoryException, CloudDriveException {
      this(filePath, null, false, changeType, null);
    }

    void setFileUUID(String fileUUID) {
      this.fileUUID = fileUUID;
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

    String getPath() {
      if (filePath != null) {
        return filePath;
      } else {
        return path;
      }
    }

    /**
     * Apply the change to target file.
     * 
     * @throws RepositoryException
     * @throws CloudDriveException
     * @throws SyncNotSupportedException
     * @throws DriveRemovedException
     * @throws InterruptedException
     */
    void apply() throws DriveRemovedException, CloudDriveException, RepositoryException, InterruptedException {
      if (applied.getCount() > 0) {
        try {
          String changeName = null;
          if (REMOVE.equals(changeType)) {
            if (synchronizer != null) {
              // #1 if trash not supported - delete the file,
              // #2 trash otherwise;
              // #3 if trash not confirmed in time - delete the file finally
              if (fileAPI.isTrashSupported()) {
                changeName = "trash";
                trash();
              } else {
                changeName = "remove";
                remove();
              }
            } else {
              throw new SyncNotSupportedException("Synchronization not available for file removal: " + path);
            }
          } else {
            try {
              Session session = session();
              Item item = session.getItem(path); // reading in user session
              Node file = null;
              try {
                if (item.isNode()) {
                  file = ensureOwned((Node) item);
                  if (CREATE.equals(changeType)) {
                    if (fileAPI.isFile(file)) {
                      // for creation of already cloud files, need check does it belong to this drive
                      if (rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
                        if (file.hasProperty("ecd:trashed")) {
                          changeName = "untrash";
                          untrash(file);
                        } else {
                          // it is a move or copy inside the drive...
                          String srcFileId = fileAPI.getId(file);
                          // Find the srcFile location. Note that the same file can exists in several places
                          // on some drives (e.g. Google), but actual source location has no big matter - we
                          // only need a source for a copy.
                          Node srcFile = null;
                          String srcPath = fileCopies.remove(srcFileId);
                          if (srcPath != null) {
                            // we have exact "copy" request
                            Item srcItem = session.getItem(srcPath); // reading in user session
                            if (srcItem.isNode()) {
                              srcFile = (Node) srcItem;
                            } else {
                              LOG.warn("Copy's source path points to a Property " + srcPath);
                            }
                          } else {
                            // find any file with the same id, but different instance
                            for (NodeIterator niter = findNodes(Arrays.asList(srcFileId)); niter.hasNext();) {
                              Node f = niter.nextNode();
                              if (!file.isSame(f)) {
                                srcFile = f;
                              }
                            }
                          }
                          if (srcFile == null) {
                            changeName = "move/rename";
                            update(file);
                          } else {
                            changeName = "copy";
                            copy(srcFile, file);
                          }
                        }
                      } else {
                        throw new SyncNotSupportedException("Cannot add file from other cloud drive "
                            + file.getPath());
                      }
                    } else if (fileAPI.isFileResource(file)) {
                      // skip file's resource, it will be handled within a file
                    } else {
                      changeName = "creation";
                      create(file);
                    }
                  }
                } else {
                  file = item.getParent();
                  if (UPDATE.equals(changeType)) {
                    if (fileAPI.isFile(file)) {
                      changeName = "update";
                      update(file);
                    } else if (fileAPI.isFileResource(file)) {
                      // TODO detect content update more precisely (by exact property name in synchronizer)
                      changeName = "content update";
                      file = file.getParent();
                      updateContent(file);
                    }
                  }
                }
              } catch (SyncNotSupportedException e) {
                if (file != null) { // always will be not null
                  if (!fileAPI.isIgnored(file)) {
                    // if sync not supported, it's not supported NT: ignore the node
                    LOG.warn("Cannot synchronize cloud file " + changeName + ": " + e.getMessage()
                        + ". Ignoring the file.");
                    try {
                      fileAPI.ignoreFile(file);
                    } catch (Throwable t) {
                      LOG.error("Error ignoring not a cloud item " + getPath(), t);
                    }
                    throw e; // throw to upper code
                  } else {
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Synchronization not available for ignored cloud item " + changeName + ": "
                          + getPath());
                    }
                  }
                }
              }
            } catch (SkipSyncException e) {
              // skip this file (it can be a part of top level NT supported by the sync)
            }
          }
        } finally {
          complete();
        }

        // apply chained
        if (next != null) {
          next.apply();
        }
      }
    }

    // ******* internal *********

    /**
     * Wait for the change completion.
     * 
     * @throws InterruptedException if this change thread was interrupted
     * 
     */
    private void await() throws InterruptedException {
      while (applied.getCount() > 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(">>>> Await " + getPath());
        }
        applied.await();
      }
    }

    /**
     * Wait for this file and its sub-tree changes in other threads, wait exclusively to let the existing to
     * finish and then set the lock do not let a new to apply before or during this change.
     * 
     * @throws InterruptedException if other tasks working with this file were interrupted
     */
    private void begin() throws InterruptedException {
      synchronized (fileChanges) {
        String lockedPath = getPath();
        FileChange other = fileChanges.putIfAbsent(lockedPath, this);
        if (other != this) {
          other = fileChanges.put(lockedPath, this);
          if (other != this) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(">>> Waiting for " + other.getPath());
            }
            other.await();
            if (LOG.isDebugEnabled()) {
              LOG.debug("<<< Done " + other.getPath());
            }
          }
        }
        for (FileChange c : fileChanges.values()) {
          if (c != this && c.getPath().startsWith(lockedPath)) {
            LOG.info(">>>> Waiting for child " + c.getPath());
            c.await();
            LOG.info("<<<< Done " + c.getPath());
          }
        }
      }
    }

    /**
     * Remove the lock set in {@link #begin()} method and add this changed file id to fileChanged map.
     * 
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    private void complete() throws PathNotFoundException, RepositoryException, CloudDriveException {
      try {
        fileChanges.remove(getPath(), this);
      } finally {
        applied.countDown();
      }
    }

    private void init(Node file) throws RepositoryException, CloudDriveException {
      if (fileId == null) {
        fileId = fileAPI.getId(file);
      }
      if (filePath == null) {
        filePath = file.getPath();
      }
    }

    private void remove() throws PathNotFoundException,
                         CloudDriveException,
                         RepositoryException,
                         InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Remove file " + path + " " + fileId);
      }

      begin();

      synchronizer.remove(path, fileId, isFolder, fileAPI);

      if (fileUUID != null) {
        // remove also file links (e.g. ECMS symlinks)
        for (Node linked : finder.findLinked(session(), fileUUID)) {
          Node parent = ensureOwned(linked.getParent());
          ensureOwned(linked).remove();
          // save immediately: in case of future error (cloud or JCR), the file node already removed in the
          // drive
          parent.save();
        }
      }
    }

    private void trash() throws PathNotFoundException,
                        CloudDriveException,
                        RepositoryException,
                        InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trash file " + path + " " + fileId);
      }

      begin();

      FileTrashing confirmation;
      FileTrashing existing = fileTrash.putIfAbsent(fileId, confirmation = new FileTrashing());
      if (existing != null) {
        confirmation = existing; // will wait for already posted trash confirmation
      } // else, will wait for just posted trash

      try {
        synchronizer.trash(path, fileId, isFolder, fileAPI);
      } catch (FileTrashRemovedException e) {
        // file was permanently deleted on cloud provider - remove it locally also
        // indeed, thus approach may lead to removal of not the same file from the Trash due to
        // same-name-siblings ordering in case of several files with the same name trashed.
        confirmation.remove();
      } catch (NotFoundException e) {
        // file not found on the cloud side - remove it locally also
        confirmation.remove();
      }

      try {
        confirmation.complete();
      } finally {
        fileTrash.remove(fileId, confirmation);
      }
    }

    private void untrash(Node file) throws SkipSyncException,
                                   SyncNotSupportedException,
                                   CloudDriveException,
                                   RepositoryException,
                                   InterruptedException {
      init(file);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Untrash file " + filePath + " " + fileId);
      }

      begin();

      // we do in a loop to handle name conflicts (by renaming the file)
      do {
        try {
          synchronizer(file).untrash(file, fileAPI);
          break;
        } catch (ConflictException e) {
          // if untrash conflicted, it's the same name already in use and we guess a new name for the
          // untrashed
          fixNameConflict(file);
        }
      } while (true);

      file.setProperty("ecd:trashed", (String) null); // clean the marker set in AddTrashListener
    }

    private void update(Node file) throws SkipSyncException,
                                  SyncNotSupportedException,
                                  CloudDriveException,
                                  RepositoryException,
                                  InterruptedException {
      init(file);
      if (!isUpdated(fileId)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Update file " + filePath + " " + fileId);
        }

        begin();

        // It's possible we need to rename item to a 'cleanName' of the title,
        // as the ECMS uses another name format for JCR nodes.
        // April 22, this problem solved by searching by file id in sync algo.

        // we do in a loop to handle name conflicts (by renaming the file)
        do {
          try {
            synchronizer(file).update(file, fileAPI);
            break;
          } catch (ConflictException e) {
            // if update conflicted, it's the same name already in use and we guess a new name for the updated
            fixNameConflict(file);
          }
        } while (true);

        addUpdated(fileId);
      } // else, we skip a change of already listed for update file
    }

    private void copy(Node srcFile, Node destFile) throws SkipSyncException,
                                                  SyncNotSupportedException,
                                                  CloudDriveException,
                                                  RepositoryException,
                                                  InterruptedException {

      init(destFile);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Copy file " + srcFile.getPath() + " -> " + filePath + " " + fileId);
      }

      begin();

      // we do in a loop to handle name conflicts (by renaming the destFile)
      do {
        try {
          synchronizer(destFile).copy(srcFile, destFile, fileAPI);
          break;
        } catch (ConflictException e) {
          // if copy conflicted, it's the same name already in use and we guess a new name for the updated
          fixNameConflict(destFile);
        }
      } while (true);

      // update file id to actual after a copy on cloud side
      fileId = fileAPI.getId(destFile);

      addUpdated(fileId);
    }

    private void updateContent(Node file) throws SkipSyncException,
                                         SyncNotSupportedException,
                                         CloudDriveException,
                                         RepositoryException,
                                         InterruptedException {
      init(file);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Update content of file " + filePath + " " + fileId);
      }

      begin();

      synchronizer(file).updateContent(file, fileAPI);
    }

    private void create(Node file) throws SkipSyncException,
                                  SyncNotSupportedException,
                                  CloudDriveException,
                                  RepositoryException,
                                  InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Create file " + path);
      }

      filePath = file.getPath(); // actual file path

      begin();

      // refresh to see other thread changes
      // FIXME such refresh should be applied for all change types after the begin() call,
      try {
        file.refresh(true);
      } catch (InvalidItemStateException e) {
        LOG.warn("Creating file already removed. " + e.getMessage());
        throw new SkipSyncException("Skip creation of already removed file. " + e.getMessage());
      }

      if (fileAPI.isFile(file)) {
        // if already a cloud file... file already created by another thread
        if (!rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
          throw new SyncNotSupportedException("Cannot add file created in another cloud drive "
              + file.getPath());
        }
      } else {
        try {
          // Property creation = file.setProperty("ecd:creation", DUMMY_DATA);
          // file.save(); // TODO save creation flag immediately to see it in fail handler
          // if creation will fail, it will be handled by later on drive sync
          synchronizer(file).create(file, fileAPI);
          // creation.remove();
        } catch (NotFoundException e) {
          // XXX parent not found in the cloud... it can be already removed there and not yet synced
          filePath = file.getPath();
          LOG.warn("Parent not found in cloud for file creation " + filePath + ". " + e.getMessage());
          // remove node locally, as for parent it should be removed by the drive sync
          file.remove();
          return;
        }
      }

      // file path can be changed after the creation (by fixNameConflict())
      filePath = file.getPath();
      // we can know the id after the sync
      fileId = fileAPI.getId(file);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Created file " + filePath + " " + fileId);
      }
    }

    private boolean isUpdated(String fileId) {
      return updated != null ? updated.contains(fileId) : false;
    }

    private void addUpdated(String fileId) {
      if (updated != null) {
        updated.add(fileId);
      }
    }
  }

  // *********** variables ***********

  /**
   * Support for JCR actions. To do not fire on synchronization (our own modif) methods.
   */
  protected static final ThreadLocal<CloudDrive>          actionDrive         = new ThreadLocal<CloudDrive>();

  protected static final Transliterator                   accentsConverter    = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  protected final String                                  rootWorkspace;

  protected final ManageableRepository                    repository;

  protected final SessionProviderService                  sessionProviders;

  protected final CloudUser                               user;

  protected final String                                  rootUUID;

  protected final ThreadLocal<SoftReference<Node>>        rootNodeHolder;

  protected final JCRListener                             jcrListener;

  protected final ConnectCommand                          noConnect           = new NoConnectCommand();

  /**
   * Currently active connect command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<ConnectCommand>         currentConnect      = new AtomicReference<ConnectCommand>(noConnect);

  protected final SyncCommand                             noSyncCommand       = new NoSyncCommand();

  /**
   * Currently active synchronization command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<SyncCommand>            currentSync         = new AtomicReference<SyncCommand>(noSyncCommand);

  /**
   * Synchronization lock used by the whole drive sync and local files changes synchronization.
   */
  protected final ReadWriteLock                           syncLock            = new ReentrantReadWriteLock(true);

  /**
   * File changes currently processing by the drive. Used for locking purpose to maintain consistency.
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
   * File changes already committed locally but not yet merged with cloud provider.
   * Format: FILE_ID = [CHANGE_DATA_1, CHANGE_DATA_2,... CHANGE_DATA_N]
   */
  protected final ConcurrentHashMap<String, Set<String>>  fileHistory         = new ConcurrentHashMap<String, Set<String>>();

  /**
   * Current drive change id. It is the last actual identifier of a change applied.
   */
  protected final AtomicLong                              currentChangeId     = new AtomicLong(-1);

  /**
   * Incrementing sequence generator for local file changes identification.
   */
  protected final AtomicLong                              fileChangeSequencer = new AtomicLong(1);

  /**
   * Managed queue of commands.
   */
  protected final CommandPoolExecutor                     commandExecutor;

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
   * Title has special care. It used in error logs and an attempt to read <code>exo:title</code> property can
   * cause another {@link RepositoryException}. Thus need it pre-cached in the variable and try to read the
   * <code>exo:title</code> property each time, but if not successful use this one cached.
   */
  private String                                          titleCached;

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
  protected JCRLocalCloudDrive(CloudUser user,
                               Node driveNode,
                               SessionProviderService sessionProviders,
                               NodeFinder finder) throws CloudDriveException, RepositoryException {

    this.user = user;
    this.sessionProviders = sessionProviders;
    this.finder = finder;

    this.commandExecutor = CommandPoolExecutor.getInstance();

    Session session = driveNode.getSession();
    this.repository = (ManageableRepository) session.getRepository();
    this.rootWorkspace = session.getWorkspace().getName();

    boolean existing;
    // ensure given node has required nodetypes
    if (driveNode.isNodeType(ECD_CLOUDDRIVE)) {
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

    this.fileAPI = createFileAPI();

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
                                       NotYetCloudFileException,
                                       RepositoryException {
    Node driveNode = rootNode();
    Item target = finder.findItem(driveNode.getSession(), path); // take symlinks in account
    String nodePath = target.getPath();
    String drivePath = driveNode.getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      if (target.isNode()) {
        Node fileNode = fileNode((Node) target);
        if (fileNode != null) {
          return readFile(fileNode);
        } else {
          throw new NotYetCloudFileException("Node '" + path + "' is not a cloud file or marked as ignored.");
        }
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
    Item target = finder.findItem(driveNode.getSession(), path); // take symlinks in account
    String nodePath = target.getPath();
    String drivePath = driveNode.getPath();
    if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
      if (target.isNode()) {
        // here we check that the node is of cloud file type
        return fileNode((Node) target) != null;
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
      // default title
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

    // set current format of the drive
    rootNode.setProperty(ECD_LOCALFORMAT, CURRENT_LOCALFORMAT);
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
                              RepositoryException {

    // if other sync in progress, use that process (as a current)
    // if no process, start a new sync process
    // if file sync in progress, wait for it and then start a new sync

    if (isConnected()) {
      refreshAccess();

      SyncCommand sync;
      if (!currentSync.compareAndSet(noSyncCommand, sync = getSyncCommand())) {
        synchronized (currentSync) {
          SyncCommand existingSync = currentSync.get();
          if (existingSync != noSyncCommand) {
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
      throw new NotConnectedException("Cloud drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isConnected() throws DriveRemovedException, RepositoryException {
    return rootNode().getProperty("ecd:connected").getBoolean();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDrive(Node node) throws DriveRemovedException, RepositoryException {
    return isDrive(node, false);
  }

  // ============== JCR impl specific methods ==============

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
  protected void initRemove(Node file) throws SyncNotSupportedException,
                                      CloudDriveException,
                                      RepositoryException {
    // Note: this method also can be invoked via RemoveCloudFileAction on file trashing in Trash service of
    // the ECMS

    final String filePath = file.getPath();
    FileChange remove = new FileChange(filePath,
                                       fileAPI.getId(file),
                                       fileAPI.isFolder(file),
                                       FileChange.REMOVE,
                                       synchronizer(file));
    // remember UUID for links removal
    if (file.isNodeType("mix:referenceable")) {
      remove.setFileUUID(file.getUUID());
    }

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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initCopy(Node file, Node destParent) throws RepositoryException, CloudDriveException {
    final String filePath = file.getPath();
    final String fileId = fileAPI.getId(file);
    final String destPath = destParent.getPath();
    // FileChange copy = new FileChange(file.getPath(), fileId, fileAPI.isFolder(file), FileChange.CREATE,
    // null);
    fileCopies.put(fileId, filePath);
    // TODO cleanup
    // Map<String, String> planned = fileCopies.get(fileId);
    // if (planned != null) {
    // FileChange existing = planned.get(fileId);
    // if (existing == null) {
    // planned.put(fileId, copy);
    // } else {
    // // we can have more that one copy of the same file if they will occur faster of its sync
    // FileChange next = copy;
    // boolean canChain = true;
    // while (next != null && canChain) {
    // canChain = next.fileId != null && copy.fileId != null ? next.fileId != copy.fileId : true;
    // next = next.next != null ? next.next : null;
    // }
    // if (canChain) {
    // // we don't check returned status as all this happens in single thread and the existing change
    // // cannot be already applied at this point
    // existing.chain(copy);
    // }
    // }
    // } else {
    // planned = new HashMap<String, FileChange>();
    // planned.put(filePath, copy);
    // fileCopies.set(planned);
    // }
  }

  CloudFileSynchronizer synchronizer(Node file) throws RepositoryException,
                                               SkipSyncException,
                                               SyncNotSupportedException {
    for (CloudFileSynchronizer s : fileSynchronizers) {
      if (s.accept(file)) {
        return s;
      }
    }
    throw new SyncNotSupportedException("Synchronization not supported for file type "
        + file.getPrimaryNodeType().getName() + " in node " + file.getPath());
  }

  CloudFileSynchronizer synchronizer(Class<?> clazz) throws RepositoryException,
                                                    SkipSyncException,
                                                    SyncNotSupportedException {
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

  private boolean hasChanged(String fileId, String... changeTypes) throws RepositoryException,
                                                                  CloudDriveException {
    Set<String> changes = fileHistory.get(fileId);
    if (changes != null) {
      final long changeId = getChangeId();
      for (String changeType : changeTypes) {
        return changes.contains(changeType + changeId);
      }
    }
    return false;
  }

  protected boolean hasUpdated(String fileId) throws RepositoryException, CloudDriveException {
    return hasChanged(fileId, FileChange.UPDATE, FileChange.CREATE);
  }

  protected boolean hasRemoved(String fileId) throws RepositoryException, CloudDriveException {
    return hasChanged(fileId, FileChange.REMOVE);
  }

  private void cleanChanged(String fileId, String... changeTypes) throws RepositoryException,
                                                                 CloudDriveException {
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

  protected void cleanUpdated(String fileId) throws RepositoryException, CloudDriveException {
    cleanChanged(fileId, FileChange.UPDATE, FileChange.CREATE);
  }

  protected void cleanRemoved(String fileId) throws RepositoryException, CloudDriveException {
    cleanChanged(fileId, FileChange.REMOVE);
  }

  @Deprecated
  private Collection<String> getChanged(String fileId) {
    Set<String> changes = fileHistory.get(fileId);
    if (changes != null) {
      return changes;
    }
    return Collections.emptyList();
  }

  protected String nextChangeId() throws RepositoryException, CloudDriveException {
    Long driveChangeId = getChangeId();
    return driveChangeId.toString() + '-' + fileChangeSequencer.getAndIncrement();
  }

  /**
   * Save given file changes to the drive store of not yet applied local changes.
   * 
   * @param changes {@link List} of {@link FileChange}
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  protected synchronized void saveChanges(List<FileChange> changes) throws RepositoryException,
                                                                   CloudDriveException {

    // <T><F><PATH><ID><SYNC_CLASS>|...
    // where ID and SYNC_CLASS can be empty
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
   * Commit given changes to the drive local history. These changes also will be removed from the local
   * changes (from previously saved and not yet applied).
   * 
   * @param changes {@link List} of {@link FileChange}
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  protected synchronized void commitChanges(List<FileChange> changes) throws RepositoryException,
                                                                     CloudDriveException {

    Node driveNode = rootNode();
    Long timestamp = System.currentTimeMillis();
    StringBuilder store = new StringBuilder();
    StringBuilder history = new StringBuilder();

    // remove already applied local changes and collect applied as history
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be greater of about 15 chars
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
            store.append(ch);
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
    // TODO implement history cleanup on clean* methods call
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
        LOG.warn("Cannot cache file change with null file id: " + ch.changeType + ", " + ch.getPath());
      }
    }
  }

  /**
   * Return saved but not yet applied local changes in this drive.
   * 
   * @return {@link List} of {@link FileChange}
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  protected synchronized List<FileChange> savedChanges() throws RepositoryException, CloudDriveException {
    // About file trashing: we cannot resume fileTrash map as it is populated in runtime from another
    // thread.
    // As side-effect, all trashed and permanently removed in cloud files will stay in local Trash.
    // In case if user decide restore them, it will depend on sync algo of particular cloud provider.
    // Actual content, that stored in the cloud, will be lost and not due to this code (due to the
    // cloud provider).

    List<FileChange> changes = new ArrayList<FileChange>();
    // <T><F><PATH><ID><SYNC_CLASS>|...
    // where ID and SYNC_CLASS can be empty
    Node driveNode = rootNode();
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be longer of about 15
        LOG.info("Applying stored local changes in " + title());
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
   * Load committed history of the drive to runtime cache.
   * 
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  protected synchronized void loadHistory() throws RepositoryException, CloudDriveException {
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

  private FileChange parseChange(String ch) throws CloudDriveException, RepositoryException {
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

    return new FileChange(changeId,
                          path,
                          fileId,
                          isFolder,
                          changeType,
                          syncClass != null ? synchronizer(syncClass) : null);
  }

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

  protected void setChangeId(Long id) throws CloudDriveException, RepositoryException {
    // only not interrupted (canceled) thread can do this
    if (!Thread.currentThread().isInterrupted()) {
      // save in persistent store
      saveChangeId(id);

      // maintain runtime cache and sequencer
      currentChangeId.set(id);
      fileChangeSequencer.addAndGet(1 - fileChangeSequencer.get()); // reset sequencer
    } else {
      LOG.warn("Ignored attempt to set drive change id by interrupted thread.");
    }
  }

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
  protected boolean isDrive(Node node, boolean includeFiles) throws DriveRemovedException,
                                                            RepositoryException {
    Node driveNode = rootNode();
    if (driveNode.getSession().getWorkspace().getName().equals(node.getSession().getWorkspace().getName())) {
      if (isConnected() && isSameDrive(driveNode, node)) {
        return true;
      } else if (includeFiles) {
        Item target = finder.findItem(node.getSession(), node.getPath()); // take symlinks in account
        if (target.isNode()) {
          node = (Node) target;
          // XXX 22.05.2014 removed check: && fileNode(node) != null
          return node.getPath().startsWith(driveNode.getPath());
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isDrive(String workspace, String path, boolean includeFiles) throws DriveRemovedException,
                                                                                RepositoryException {
    Node driveNode = rootNode();
    if (driveNode.getSession().getWorkspace().getName().equals(workspace)) {
      Item target = finder.findItem(driveNode.getSession(), path); // take symlinks in account
      if (target.isNode()) {
        Node node = (Node) target;
        if (isConnected() && isSameDrive(driveNode, node)) {
          return true;
        } else if (includeFiles) {
          // XXX 22.05.2014 removed check: && fileNode(node) != null
          return node.getPath().startsWith(driveNode.getPath());
        }
      }
    }
    return false;
  }

  protected boolean isSameDrive(Node driveNode, Node anotherNode) throws RepositoryException {
    if (anotherNode.isNodeType(ECD_CLOUDDRIVE)) {
      return driveNode.getProperty("ecd:id")
                      .getString()
                      .equals(anotherNode.getProperty("ecd:id").getString());
    }
    return false;
  }

  /**
   * Return {@link Node} instance representing some cloud file, i.e. if given Node is of acceptable type.
   * 
   * @param node {@link Node}
   * @return {@link Node}
   * @throws RepositoryException
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
          && IdentityHelper.isUserMatch(rootNode.getSession().getUserID(), cs.getIdentity().getUserId())) {
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
   * @param commandName {@link String}
   */
  void handleError(Node rootNode, Throwable error, String commandName) {
    String rootPath = null;
    if (rootNode != null) {
      try {
        rootPath = rootNode.getPath();
      } catch (RepositoryException e) {
        LOG.warn("Error reading drive root '" + e.getMessage() + "' "
            + (commandName != null ? "of " + commandName + " command " : "")
            + "to listeners on Cloud Drive '" + title() + "':" + e.getMessage());
      }

      if (commandName.equals("connect")) {
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
      listeners.fireOnError(new CloudDriveEvent(getUser(), rootWorkspace, rootPath), error, commandName);
    } catch (Throwable e) {
      LOG.warn("Error firing error '" + error.getMessage() + "' "
          + (commandName != null ? "of " + commandName + " command " : "") + "to listeners on Cloud Drive '"
          + title() + "':" + e.getMessage());
    }
  }

  private Node openNode(String fileId, String fileTitle, Node parent, String nodeType) throws RepositoryException,
                                                                                      CloudDriveException {
    Node node;
    String cleanName = cleanName(fileTitle);
    String name = cleanName;
    String internalName = null;

    int siblingNumber = 1;
    do {
      try {
        node = parent.getNode(name);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already extends the file NT
        if (fileAPI.isFile(node)) {
          if (fileId.equals(fileAPI.getId(node))) {
            // we found node...
            ensureOwned(node);
            break;
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
          // try cleanup this existing local node
          if (getOrCleanFileNode(node) == null && parent.hasNode(name)) {
            // node not cleaned, this node stays only local (creation in progress or ignored),
            // and we need a new name for the file node
            StringBuilder newName = new StringBuilder();
            newName.append(cleanName);
            newName.append('-');
            newName.append(siblingNumber);
            name = newName.toString();
            siblingNumber++;
          }
          // else, node cleaned and we try with current name one more time,
          // it should lead to PathNotFoundException and new node will be created by the try-catch below
        }
      } catch (PathNotFoundException e) {
        if (internalName == null) {
          internalName = name;
          // try NodeFinder name (storage specific, e.g. ECMS)
          String finderName = finder.cleanName(fileTitle);
          // finder doesn't work with single symbol names when they are special characters (like '.') 
          if (finderName.length() > 1) {
            name = finderName;
            continue;
          }
        }
        // no such node exists, add it using internalName created by CD's cleanName()
        node = parent.addNode(internalName, nodeType);
        break;
      }
    } while (true);

    return node;
  }

  /**
   * Next nasty thing here: ensure the node is owned by the drive user, it MUST!
   * 
   * @param node {@link Node}
   * @return {@link Node} fixed node (same as in the given in the parameter)
   * @throws RepositoryException
   * @throws PathNotFoundException
   */
  private Node ensureOwned(Node node) throws RepositoryException {
    // TODO try avoid do this, or at least do this in single place (JCR listener?)
    return IdentityHelper.ensureOwned(node, systemSession());
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
   * Read local nodes from the drive folder to a map by file Id. It's possible that for a single Id we have
   * several files (in different parents usually). This can be possible when Cloud Drive provider supports
   * linking or tagging/labeling where tag/label is a folder (e.g. Google Drive).
   * 
   * @param parent {@link Node}
   * @param nodes {@link Map} of {@link List} objects to fill with the parent's child nodes
   * @param deep boolean, if <code>true</code> read nodes recursive, <code>false</code> read only direct
   *          child nodes.
   * @throws RepositoryException if JCR error happen
   * @throws CloudDriveException
   * @throws SkipSyncException
   */
  protected void readNodes(Node parent, Map<String, List<Node>> nodes, boolean deep) throws RepositoryException {
    for (NodeIterator niter = parent.getNodes(); niter.hasNext();) {
      Node node = getOrCleanFileNode(niter.nextNode());
      if (node != null) {
        String fileId = fileAPI.getId(node);
        List<Node> nodeList = nodes.get(fileId);
        if (nodeList == null) {
          nodeList = new ArrayList<Node>();
          nodes.put(fileId, nodeList);
        }
        nodeList.add(node);
        if (deep && fileAPI.isFolder(node)) {
          readNodes(node, nodes, deep);
        }
      } // else, node just cleaned (removed) or should be ignored
    }
  }

  /**
   * Read cloud file node from the given parent using the file title and its id. Algorithm of this method is
   * the same as in {@link #openNode(String, String, Node, String)} but this method doesn't create a node if
   * it doesn't exist.
   * 
   * @param parent {@link Node} parent
   * @param fileTitle {@link String}
   * @param fileId {@link String}
   * @return {@link Node}
   * @throws RepositoryException
   */
  protected Node readNode(Node parent, String fileTitle, String fileId) throws RepositoryException,
                                                                       CloudDriveException {
    Node node;
    String cleanName = cleanName(fileTitle);
    String name = cleanName;
    String internalName = null;

    int siblingNumber = 1;
    do {
      try {
        node = parent.getNode(name);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already extends the file NT
        if (fileAPI.isFile(node)) {
          if (fileId.equals(fileAPI.getId(node))) {
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
      return ensureOwned(nodes.nextNode());
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
    Node content = localNode.getNode("jcr:content");
    if (!content.isNodeType(ECD_CLOUDFILERESOURCE)) {
      content.addMixin(ECD_CLOUDFILERESOURCE);
    }

    // nt:resource
    content.setProperty("jcr:mimeType", type);
    content.setProperty("jcr:lastModified", modified);

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
                                 new String[] { ECD_CLOUDDRIVE },
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

  /**
   * Do nasty thing: remove predefined node (nodetypes) from the drive. This method doesn't check if node
   * belongs to the drive (it assumes it does). The method also doesn't check if the node ignored (it assumes
   * it doesn't).<br/>
   * This method doesn't throw any exception: operation will be performed in system session and saved
   * immediately. In case of a problem it will be logged to system log.
   * 
   * @param node {@link Node}
   * @return boolean, <code>true</code> if node was removed, <code>false</code> otherwise
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

  @Deprecated
  protected Node createIfNotCloudFile(Node node) throws RepositoryException, CloudDriveException {
    if (!fileAPI.isFile(node)) {
      if (!tryCreateCloudFile(node)) {
        return null;
      }
    }
    return node;
  }

  /**
   * Try create file in the cloud from local node. Method assumes this not not An attempt will be performed if
   * this node not yet ignored
   * and cannot be cleaned (removed). If synchronization not supported for the node type, then this node will
   * be marked as ignored.<br/>
   * This method designed for use when a node found in the drive and it is not a cloud file yet (e.g. in
   * result of previous synchronization error). Such node can be found during a next sync initiated by client.
   * 
   * @param node {@link Node} local node, not a cloud file yet
   * @return boolean, <code>true</code> if cloud file was successfully created, <code>false</code> otherwise
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  @Deprecated
  protected boolean tryCreateCloudFile(Node node) throws RepositoryException, CloudDriveException {
    if (!fileAPI.isIgnored(node)) {
      final String path = node.getPath();
      LOG.warn("Not a cloud file detected " + path);

      if (!cleanup(node)) {
        // try convert node to cloud file
        // we do in a loop to handle name conflicts (by renaming this node)
        do {
          try {
            // This node can be just added or be already in process by file sync, or by periodical drive
            // sync invoking readNodes() in // and we are here.
            // Use FileChange to leverage file locking during the creation.
            new FileChange(path, FileChange.CREATE).apply();
            return true;
          } catch (SyncNotSupportedException e) {
            LOG.warn("Cannot create file in cloud - it will be ignored: " + path + " ("
                + node.getPrimaryNodeType().getName() + "). " + e.getMessage());
            try {
              node.refresh(false); // rollback possible changes
              fileAPI.ignoreFile(node); // and ignore the node
            } catch (Throwable t) {
              LOG.error("Error ignoring not a cloud item " + path, t);
            }
            break;
          } catch (ConflictException e) {
            // such file already exists in the cloud - rename local to avoid conflicts
            fixNameConflict(node); // >>> TODO is it correct??
          } catch (SkipSyncException e) {
            // skip this file (it can be a part of top level NT supported by the sync)
            break;
          } catch (InterruptedException e) {
            throw new CloudDriveException("File creation interrupted " + path, e);
          }
        } while (true);
      }
    }
    return false;
  }

  /**
   * Return given node if it describes connected cloud file. If it is not yet
   * cloud file, node should be ignored, or was just cleaned (removed) the method will return
   * <code>null</code>.
   * 
   * @param node {@link Node} local node
   * @return {@link Node} local node or <code>null</code> if file cleaned (removed) or should be ignored
   * @throws RepositoryException
   */
  protected Node getOrCleanFileNode(Node node) throws RepositoryException {
    if (fileAPI.isFile(node)) {
      return ensureOwned(node);
    } else if (fileAPI.isIgnored(node)) {
      return null;
    } else {
      final String path = node.getPath();
      LOG.warn("Not a cloud file detected " + path);
      // we don't try convert node to cloud file -
      // this should be done by a next drive sync command
      if (!cleanup(node)) {
        ensureOwned(node);
      }
    }
    return null;
  }

  private void fixNameConflict(Node file) throws RepositoryException {
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
    if (openingParenthesesPos > 0 && closingParenthesesPos > 0
        && closingParenthesesPos > openingParenthesesPos + 1) {
      try {
        index = Integer.valueOf(baseTitle.substring(openingParenthesesPos, closingParenthesesPos));
      } catch (NumberFormatException e) {
        index = 1;
      }
    } else {
      index = 1;
    }
    do {
      String newTitle = baseTitle + " (" + index++ + ")";
      if (baseExt != null) {
        newTitle += "." + baseExt;
      }
      String newName = cleanName(newTitle);
      if (!parent.hasNode(newName)) {
        session.move(file.getPath(), parent.getPath() + "/" + newName);
        file.setProperty("exo:title", newTitle);
        break;
      }
    } while (true);
  }

  // ============== abstract ==============

  /**
   * Read id of the latest cloud change applied to this drive. The id will be read from the drive store, that
   * is provider specific.
   * 
   * @return {@link Long}
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  protected abstract Long readChangeId() throws CloudDriveException, RepositoryException;

  /**
   * Save last cloud change applied to this drive. The id will be written to the drive store, that is provider
   * specific.
   * 
   * @param id {@link Long}
   * @throws DriveRemovedException
   * @throws RepositoryException
   */
  protected abstract void saveChangeId(Long id) throws CloudDriveException, RepositoryException;

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
  @Deprecated
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
          node.setProperty(ECD_LOCALFORMAT, CURRENT_LOCALFORMAT); // set current version
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
