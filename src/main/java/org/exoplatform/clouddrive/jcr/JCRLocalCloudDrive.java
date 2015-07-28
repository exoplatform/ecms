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
import org.exoplatform.clouddrive.CannotConnectDriveException;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveEnvironment;
import org.exoplatform.clouddrive.CloudDriveEvent;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveManager;
import org.exoplatform.clouddrive.CloudDriveMessage;
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
public abstract class JCRLocalCloudDrive extends CloudDrive implements CloudDriveStorage {

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

  public static final String     MIX_REFERENCEABLE     = "mix:referenceable";

  public static final String     ECD_LOCALFORMAT       = "ecd:localFormat";

  public static final double     CURRENT_LOCALFORMAT   = 1.1d;

  public static final long       HISTORY_EXPIRATION    = 1000 * 60 * 60 * 24 * 8; // 8 days

  public static final int        HISTORY_MAX_LENGTH    = 1000;                    // 1000 file modification

  public static final String     DUMMY_DATA            = "".intern();

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
     * {@inheritDoc}
     */
    @Override
    public boolean hasChanges() {
      return false;
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
     * @inherritDoc
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
            if (fileId.equals(fileAPI.getId(file)) && rootUUID.equals(file.getProperty("ecd:driveUUID").getString())) {
              file.remove();
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

    class DriveChangesListener extends BaseCloudDriveListener implements EventListener {

      final ThreadLocal<AtomicLong> lock = new ThreadLocal<AtomicLong>();

      void disable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(1));
        } else {
          requests.incrementAndGet();
        }
      }

      void enable() {
        AtomicLong requests = lock.get();
        if (requests == null) {
          lock.set(requests = new AtomicLong(0));
        } else if (requests.get() > 0) {
          requests.decrementAndGet();
        }
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

              if (eventPath.endsWith("jcr:mixinTypes") || eventPath.endsWith("jcr:content") || eventPath.indexOf("ecd:") >= 0
                  || eventPath.indexOf("/exo:thumbnails") > 0) {
                // XXX hardcoded undesired system stuff to skip
                continue;
              }

              if (event.getType() == Event.NODE_REMOVED) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Node removed. User: " + event.getUserID() + ". Path: " + eventPath);
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
                } // otherwise it's ignored file or not removal (may be a move or ordering)
              } else {
                if (event.getType() == Event.NODE_ADDED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Node added. User: " + event.getUserID() + ". Path: " + eventPath);
                  }
                  changes.add(new FileChange(eventPath, FileChange.CREATE));
                } else if (event.getType() == Event.PROPERTY_CHANGED) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Node property changed. User: " + event.getUserID() + ". Path: " + eventPath);
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

    final AddTrashListener     trashListener;

    final DriveChangesListener changesListener;

    volatile boolean           trashed = false;

    volatile boolean           added   = false;

    JCRListener(String initialRootPath) {
      this.initialRootPath = initialRootPath;
      this.removeListener = new RemoveDriveListener();
      this.trashListener = new AddTrashListener();
      this.changesListener = new DriveChangesListener();
    }

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
                    Thread.sleep(1000 * 2); // wait a bit for ECMS actions
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
          // should not happen in current implementation, but possible in general
          // do nothing!
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
    protected final Queue<CloudFile>         changed          = new ConcurrentLinkedQueue<CloudFile>();

    /**
     * Local file paths deleted by the command.
     */
    protected final Queue<String>            removed          = new ConcurrentLinkedQueue<String>();

    /**
     * Messages generated by command.
     */
    protected final Queue<CloudDriveMessage> messages         = new ConcurrentLinkedQueue<CloudDriveMessage>();

    /**
     * Target JCR node. Will be initialized in exec() method (in actual runner thread).
     */
    protected Node                           rootNode;

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
     * Asynchronous execution support.
     */
    protected Future<Command>                async;

    protected ExoJCRSettings                 settings;

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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new StringBuilder().append(super.toString()).append(" (").append(title()).append(")").toString();
    }

    /**
     * Processing logic.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     * @throws InterruptedException
     */
    protected abstract void process() throws CloudDriveException, RepositoryException, InterruptedException;

    /**
     * Finalization logic that should be done always on the command end. Note that this method will be called
     * once even if command will be retried due to know errors.
     * 
     */
    protected abstract void always();

    /**
     * Start command execution. If command will fail due to provider error, the execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the throwing an exception.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected final void exec() throws CloudDriveException, RepositoryException {
      startTime.set(System.currentTimeMillis());

      driveCommands.add(this);

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
        LOG.error("Runtime error. Drive " + getName() + " canceled", e);
        throw e;
      } finally {
        always();
        doneAction();
        jcrListener.enable();
        commandEnv.cleanup(this); // cleanup environment
        finishTime.set(System.currentTimeMillis());
        driveCommands.remove(this);
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
      // we remove the same messages (may have a place for several updates of the same file)
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
      // reset all possible previous attempts metadata
      rootNode.setProperty("ecd:localChanges", DUMMY_DATA);
      rootNode.setProperty("ecd:localHistory", DUMMY_DATA);
      rootNode.setProperty("ecd:connected", false);
      rootNode.save();

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void always() {
      currentConnect.set(noConnect); // clean current, see connect()
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
    protected void process() throws CloudDriveException, RepositoryException, InterruptedException {
      boolean hasChanges;
      syncLock.writeLock().lock(); // write-lock acquired exclusively by single threads (drive sync)
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
        if (hasChanges) {
          // and save the drive node
          rootNode.save();
        }
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
        listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath(), changed, removed));
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
     * Prepare files synchronization from cloud provider to local JCR. It includes applying of local changes
     * to remote provider if such had a place. Provider implementations may add other logic to this method.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     * @throws InterruptedException
     */
    protected void preSyncFiles() throws CloudDriveException, RepositoryException, InterruptedException {
      List<FileChange> changes = savedChanges();
      if (changes.size() > 0) {
        // run sync in this thread and w/o locking the syncLock
        SyncFilesCommand localChanges = new SyncFilesCommand(changes);
        localChanges.sync(rootNode);
      }
    }

    /**
     * Synchronize files from cloud provider to local JCR.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     * @throws InterruptedException
     */
    protected abstract void syncFiles() throws CloudDriveException, RepositoryException, InterruptedException;

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

    static final String    NAME     = "files synchronization";

    /**
     * This command updating files. Will be removed at the processing end.
     */
    final List<String>     updating = new ArrayList<String>(); // use List to track number of updates

    final List<FileChange> changes;

    SyncFilesCommand(List<FileChange> changes) {
      this.changes = changes;

      // updating status of the drive
      for (FileChange change : changes) {
        String path = change.path; // path of file node or its property here
        initUpdating(path);
        updating.add(path);
        String id = change.fileId; // file id will be available for removals only
        if (id != null) {
          initUpdating(id);
          updating.add(id);
        }
      }
    }

    /**
     * Mark the change's file as updating in the drive. Note {@link FileChange} should be accepted before
     * calling this method for actual file path, otherwise it will be <code>null</code>.
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
        syncLock.readLock().lock(); // read-lock can be acquired by multiple threads (file syncs)
        try {
          // save observed changes to the drive store, they will be reset in case of success in sync()
          saveChanges(changes);
          // apply changes
          sync(rootNode);
        } finally {
          syncLock.readLock().unlock();
        }

        // fire listeners afterwards and only if actual changes have a place
        if (hasChanges()) {
          listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath(), changed, removed));
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
      // nothing
    }

    void sync(Node driveNode) throws RepositoryException, CloudDriveException, InterruptedException {
      try {
        // Changes to skip in the history
        List<FileChange> skipped = new ArrayList<FileChange>();

        // compress the changes list:
        // * skip not supported items/ignored
        // * reduce number of creation/updates of the same path (make single file update for its properties
        // update)
        // * perform prerequisites check and throw exception if required
        // FYI this compression is file centric and doesn't assume that some property update
        // might change constraints for later file update/copy/removal, if such case will be actual
        // then need change this logic to accept such properties update.

        // collection of accepted, natural order important!
        Map<String, FileChange> accepted = new LinkedHashMap<String, FileChange>();
        for (Iterator<FileChange> chiter = changes.iterator(); chiter.hasNext()
            && !Thread.currentThread().isInterrupted();) {
          FileChange change = chiter.next();

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
                    // ignore previous creation of just removed file, skip this removal: cloud should not be
                    // affected
                    skipped.add(accepted.remove(path));
                    skipped.add(change);
                    continue;
                  } else if (FileChange.UPDATE.equals(prevChange) || FileChange.UPDATE_CONTENT.equals(prevChange)) {
                    // ignore previous updates of just removed file
                    skipped.add(accepted.remove(path));
                  }
                } else if (FileChange.CREATE.equals(change.changeType) && FileChange.REMOVE.equals(prevChange)) {
                  // FYI actual for complex uses of JCR, when the same location was removed and added in
                  // single save here we need keep both, but Map cannot accept same keys (paths),
                  // we need to copy the accepted map and store the removal with a new (different) key
                  // then creation will be added in the end of the order below
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
                    // ignore previous update and use this content update (it also should update metadata)
                    skipped.add(accepted.remove(path));
                  } else if (FileChange.CREATE.equals(prevChange)) {
                    // skip content update for just created, actual content will be used by the creation
                    skipped.add(change);
                    continue;
                  }
                }
              }
              // else:
              // * if REMOVE then UPDATE, not possible in JCR
              accepted.put(path, change);
            } else {
              // else - ignore the change
              skipped.add(change);
            }
          } catch (AccessDeniedException e) {
            // special logic for a case when drive/file was moved to eXo Trash during the sync preparation
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
              // XXX workaround: hardcode ignorance of exo:thumbnails here also,
              // it's possible that thumbnails' child nodes will disappear, thus we ignore them
              skipped.add(change);
            } else {
              throw e;
            }
          }
        }

        Set<String> ignoredPaths = new HashSet<String>(); // for not supported by sync

        Collection<FileChange> acceptedChanges = accepted.values();

        next: for (Iterator<FileChange> chiter = acceptedChanges.iterator(); chiter.hasNext()
            && !Thread.currentThread().isInterrupted();) {
          FileChange change = chiter.next();
          String changePath = change.filePath;
          for (String ipath : ignoredPaths) {
            if (changePath.startsWith(ipath)) {
              skipped.add(change);
              continue next; // skip parts of ignored (not supported by sync) nodes
            }
          }

          try {
            change.apply();
            if (FileChange.REMOVE.equals(change.changeType)) {
              removed.add(change.getPath());
            } else {
              CloudFile cfile = change.getFile();
              if (cfile != null) {
                changed.add(cfile);
              }
            }
          } catch (SyncNotSupportedException e) {
            // remember to skip sub-files, this exception handled by this
            ignoredPaths.add(changePath);
            skipped.add(change);
          } catch (SkipChangeException e) {
            // remember to skip sub-files and inform user, this exception handled by this
            ignoredPaths.add(changePath);
            skipped.add(change);
            messages.add(new CloudDriveMessage(CloudDriveMessage.Type.WARN, e.getMessage()));
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
              // it was existing and need add to the cloud, but already removed locally - ignore it
              skipped.add(change);
              LOG.warn("[2] Ignoring already removed item creation: " + changePath, e);
            } else if (change.changeType.equals(FileChange.UPDATE)) {
              Node existing = findNode(change.fileId);
              if (existing != null) {
                // file name change when fixNameConflict() was used and moved the node - ignore this change
                skipped.add(change);
                LOG.warn("[2] Item already updated (file renamed) " + changePath + " belongs to " + existing.getPath()
                    + ". Change faced with this: " + e.getMessage());
              }
            } else if (e.getMessage().indexOf("/exo:thumbnails") > 0 && changePath.indexOf("/exo:thumbnails") > 0) {
              // XXX hardcode ignorance of exo:thumbnails here also,
              // it's possible that thumbnails' child nodes will disappear, thus we ignore them
              ignoredPaths.add(changePath);
              skipped.add(change);
            } else {
              throw e;
            }
          } catch (AccessDeniedException e) {
            // special logic for a case when drive/file was moved to eXo Trash during the sync processing
            if (change != null && change.node != null && isInTrash(change.node)) {
              skipped.add(change);
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

        // commit changes store saved in the drive to the history, omit skipped
        commitChanges(acceptedChanges, skipped);

        // help GC
        accepted.clear();
        skipped.clear();
        ignoredPaths.clear();
      } finally {
        // complete and cleanup after drive node save
        changes.clear();
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
    public Collection<String> findParents(Node fileNode) throws DriveRemovedException, RepositoryException {
      return findParents(getId(fileNode));
    }

    protected Collection<String> findParents(String fileId) throws DriveRemovedException, RepositoryException {
      Set<String> parentIds = new LinkedHashSet<String>();
      for (Node fn : findNodes(Arrays.asList(fileId))) {
        Node p = fn.getParent(); // parent it is a cloud file or a cloud drive
        parentIds.add(fileAPI.getId(p));
      }
      return Collections.unmodifiableCollection(parentIds);
    }

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
   * Cloud File change. It is used for keeping a file change request and applying it in current thread.
   * File change differs to a {@link Command} that the command offers public API with an access to the
   * command state and progress. A command also runs asynchronously in a dedicated thread. The file change is
   * for internal use (can be used within a command).
   */
  protected class FileChange {

    public static final String  REMOVE         = "D";

    public static final String  CREATE         = "A";

    public static final String  UPDATE         = "U";

    /**
     * Used internally in {@link FileChange} only as result of UPDATE of a file content property.
     */
    public static final String  UPDATE_CONTENT = "C";

    final CountDownLatch        applied        = new CountDownLatch(1);

    final boolean               isFolder;

    final String                path;

    final CloudFileSynchronizer synchronizer;

    String                      changeType;

    String                      filePath;

    String                      fileId;

    String                      changeId;

    /**
     * Referenceable file UUID for removal (optional for other operations).
     */
    String                      fileUUID;

    /**
     * Target file node. Should be initialized in {@link #init(Set)} in worker thread.
     */
    Node                        node;

    /**
     * Cloud file produced by the change. Can be <code>null</code> for removal.
     */
    CloudFile                   file;

    /**
     * Constructor for newly observed change when file {@link Node} available in the context. Used for file
     * removals observed via {@link CloudDriveManager} in high-level apps (ECMS and other).
     * 
     * @param path
     * @param fileId
     * @param isFolder
     * @param changeType
     * @param synchronizer
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    FileChange(String path, String fileId, boolean isFolder, String changeType, CloudFileSynchronizer synchronizer)
        throws CloudDriveException, RepositoryException {
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

    /**
     * File change with postponed decision for file synchronizer implementation. It is used for observed
     * creation and update events. See {@link DriveChangesListener}.
     * 
     * @param path
     * @param changeType
     * @throws RepositoryException
     * @throws CloudDriveException
     */
    FileChange(String path, String changeType) throws RepositoryException, CloudDriveException {
      this(path, null, false, changeType, null);
    }

    void setFileUUID(String fileUUID) {
      this.fileUUID = fileUUID;
    }

    String getPath() {
      return filePath;
    }

    CloudFile getFile() {
      return file;
    }

    /**
     * Prepare and check if it should be accepted as a change in cloud drive: find the file node if
     * creation/update, ensure synchronizer available for removal.
     * 
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws CloudDriveException
     * @throws SyncNotSupportedException
     * @throws InterruptedException
     */
    boolean accept() throws DriveRemovedException,
                     CloudDriveException,
                     PathNotFoundException,
                     RepositoryException,
                     InterruptedException {
      if (REMOVE.equals(changeType)) {
        if (synchronizer == null) { // this check for a case, it should not be null by the logic
          throw new SyncNotSupportedException("Synchronization not available for file removal: " + path);
        }
        this.filePath = path; // fileId should be already initialized for removal
        return true;
      } else {
        Session session = session();
        Item item = session.getItem(path); // reading in user session by initial path
        Node node = null;
        if (item.isNode()) {
          // node added or changed
          node = (Node) item;
          if (fileAPI.isFile(node)) {
            // for creation/update of already cloud files, need check does it belong to this drive
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
            // it is an usecase for content update of existing cloud file from local drive
            return false;
          }
        } else {
          // property changed: we support only updates, new properties will be ignored
          Node parentNode = item.getParent();
          if (UPDATE.equals(changeType)) {
            if (fileAPI.isFile(parentNode)) {
              // file metadata update (date, creator etc)
              node = parentNode;
              fileId = fileAPI.getId(node);
            } else if (fileAPI.isFileResource(parentNode)) {
              // file content update
              // logic based on nt:file structure: theNtFile/jcr:content/jcr:data
              // TODO detect content update more precisely (by exact property name in synchronizer)
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
     * @throws RepositoryException
     * @throws CloudDriveException
     * @throws SyncNotSupportedException
     * @throws DriveRemovedException
     * @throws InterruptedException
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
              // if file was actually trashed in ECMS, we need remove it from there
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
                    // Find the srcFile location. Note that the same file can exists in several places
                    // on some drives (e.g. Google, CMIS), but actual source location has no big matter
                    // - we only need a source for a copy/move.
                    // In case of copy, while not synchronized, two files with the same ID exist locally.
                    Node srcFile = null;
                    String srcPath = fileCopies.remove(fileId);
                    if (srcPath != null) {
                      // we have exact "copy" request
                      try {
                        Item srcItem = node.getSession().getItem(srcPath); // reading in user session
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
              // skip this file (it can be a part of top level NT supported by the sync)
            }
          } // else null file - file not recognized - skip it
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
     * 
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
     * Wait for this file and its sub-tree changes in other threads, wait exclusively to let the existing to
     * finish and then set the lock do not let a new to apply before or during this change.
     * 
     * @throws InterruptedException if other tasks working with this file were interrupted
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
              LOG.debug("<<< Done " + other.filePath);
            }
          }
        }
        for (FileChange c : fileChanges.values()) {
          if (c != this && c.filePath.startsWith(lockedPath)) {
            LOG.info(">>>> Waiting for child " + c.filePath);
            c.await();
            LOG.info("<<<< Done " + c.filePath);
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
        fileChanges.remove(filePath, this);
      } finally {
        applied.countDown();
      }
    }

    private void remove() throws PathNotFoundException, CloudDriveException, RepositoryException, InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Remove file " + fileId + " " + filePath);
      }

      begin();

      try {
        synchronizer.remove(filePath, fileId, isFolder, fileAPI);
      } catch (NotFoundException e) {
        // file already doesn't exist remotely... could be removed by outside or by error,
        // in any case it is not a reason to break the process here
        LOG.warn("File not found in cloud for file removal " + filePath + ". " + e.getMessage());
      } catch (ConstraintException e) {
        // local file cannot be removed remotely and we should keep local state as before the removal -
        // finally we don't want break other changes and should omit this file (or folder and its sub-tree).
        LOG.warn("Constraint violation while synchronizing cloud file removal: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file " + filePath);

        // Restore the file from cloud side.
        fileAPI.restore(fileId, filePath);

        // throw to the caller to add the file to ignored list in this SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Removed file restored.", e);
      }

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

    private void trash() throws PathNotFoundException, CloudDriveException, RepositoryException, InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trash file " + fileId + " " + filePath);
      }

      begin();

      FileTrashing confirmation;
      FileTrashing existing = fileTrash.putIfAbsent(fileId, confirmation = new FileTrashing());
      if (existing != null) {
        confirmation = existing; // will wait for already posted trash confirmation
      } // else, will wait for just posted trash

      try {
        synchronizer.trash(filePath, fileId, isFolder, fileAPI);
      } catch (FileTrashRemovedException e) {
        // file was permanently deleted on cloud provider - remove it locally also
        // indeed, thus approach may lead to removal of not the same file from the Trash due to
        // same-name-siblings ordering in case of several files with the same name trashed.
        confirmation.remove();
      } catch (NotFoundException e) {
        // file not found on the cloud side - remove it locally also
        confirmation.remove();
      } catch (ConstraintException e) {
        // local file cannot be trashed remotely and we should keep local state as before the trashing -
        // finally we don't want break other changes and should omit this file (or folder and its sub-tree).
        LOG.warn("Constraint violation while synchronizing cloud file trash: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file " + filePath);

        // Restore the file from cloud side.
        fileAPI.restore(fileId, filePath);

        // throw to the caller to add the file to ignored list in this SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Removed file restored.", e);
      }

      try {
        confirmation.complete();
      } finally {
        fileTrash.remove(fileId, confirmation);
      }
    }

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
      do {
        try {
          file = synchronizer(node).untrash(node, fileAPI);
          filePath = file.getPath();
          node.setProperty("ecd:trashed", (String) null); // clean the marker set in AddTrashListener
          break;
        } catch (ConflictException e) {
          // if untrash conflicted, it's the same name already in use and we guess a new name for the
          // untrashed
          fixNameConflict(node);
        } catch (ConstraintException e) {
          // we have constraint violation in cloud service, local file cannot be untrashed
          // remotely and we should keep local state as before the update - finally we don't want break
          // other changes and should omit this file (or folder and its sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file untrash: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() + ". " : "") + "Restoring local file state " + filePath);

          // Restore the file from cloud side.
          // As result of restoration untrashed file can be removed locally to reflect the remote drive -
          // it is not a data lost for an user, as his data in cloud already properly represented locally.
          fileAPI.restore(fileId, filePath);

          // throw to the caller to add the file to ignored list in this SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Drive state refreshed.", e);
        }
      } while (true);
    }

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
      do {
        try {
          file = synchronizer(node).update(node, fileAPI);
          filePath = file.getPath();
          break;
        } catch (ConflictException e) {
          // if update conflicted, it's the same name already in use and we guess a new name for the updated
          fixNameConflict(node);
        } catch (ConstraintException e) {
          // local file cannot be updated remotely and we should keep local state as before the update -
          // finally we don't want break other changes and should omit this file (or folder and its
          // sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file update. " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file state " + filePath);

          // Restore the file from cloud side.
          CloudFile restored = fileAPI.restore(fileId, filePath);
          // check with actual file path here!
          if (restored == null || !restored.getPath().equals(node.getPath())) {
            // it is restored file rename/move - destination file should be removed locally
            node.remove();
          }

          // throw to the caller to add the file to ignored list in this SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Local file restored.", e);
        }
      } while (true);
    }

    private void updateContent() throws SkipSyncException,
                                 SyncNotSupportedException,
                                 CloudDriveException,
                                 RepositoryException,
                                 InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Update content of file " + fileId + " " + filePath);
      }

      begin();

      try {
        file = synchronizer(node).updateContent(node, fileAPI);
      } catch (ConstraintException e) {
        // local file cannot be updated remotely and we should keep local state as before the update - finally
        // we don't want break other changes application and should omit this file (or folder and its
        // sub-tree).
        LOG.warn("Constraint violation while synchronizing cloud file content update: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Restoring local file state " + filePath);

        // Restore the file from cloud side.
        fileAPI.restore(fileId, filePath);

        // throw to the caller to add the file to ignored list in this SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Local file content not synchronized.", e);
      }
    }

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
      do {
        try {
          file = synchronizer(node).copy(srcFile, node, fileAPI);
          filePath = file.getPath();
          break;
        } catch (ConflictException e) {
          // if copy conflicted, it's the same name already in use and we guess a new name for the updated
          fixNameConflict(node);
        } catch (ConstraintException e) {
          // local file cannot be copied remotely and we should remove destination file locally - finally we
          // don't want break other changes and should omit this file (or folder and its sub-tree).
          LOG.warn("Constraint violation while synchronizing cloud file copy: " + e.getMessage() + ". "
              + (e.getCause() != null ? e.getCause().getMessage() : "") + ". Removing the copied file locally " + filePath);

          // we restore local state by removing just copied file
          node.remove();

          // throw to the caller to add the file to ignored list in this SyncFilesCommand
          throw new SkipChangeException(e.getMessage() + ". Locally copied file removed.", e);
        }
      } while (true);

      // update file id to actual after a copy on cloud side
      fileId = file.getId();
    }

    private void create() throws SkipSyncException,
                          SyncNotSupportedException,
                          CloudDriveException,
                          RepositoryException,
                          InterruptedException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Create file " + filePath);
      }

      begin();

      // refresh to see other thread changes
      // TODO such refresh could be applied for all change types after the begin() call,
      // it may improve long-running sets of files when JCR session become outdated
      // fix this together with splitting on per-file save in next versions
      try {
        node.refresh(true);
      } catch (InvalidItemStateException e) {
        LOG.warn("Cannot create already removed file. " + e.getMessage());
        throw new SkipSyncException("Skip creation of already removed file. " + e.getMessage());
      }

      try {
        file = synchronizer(node).create(node, fileAPI);
        filePath = file.getPath();
      } catch (NotFoundException e) {
        // parent not found in the cloud... it can be already removed there and not yet synced
        LOG.warn("Parent not found in cloud for file creation " + filePath + ". " + e.getMessage());
        // remove node locally, as for parent it should be removed by the drive sync
        node.remove();
        return;
      } catch (ConflictException e) {
        // if creation conflicted, it's the same name already in use and we guess a new name for the file
        fixNameConflict(node);
      } catch (ConstraintException e) {
        // local node cannot be added remotely and can be removed/moved locally by an user - finally we don't
        // want break other changes and should omit this file (or folder and its sub-tree).
        // we keep user node locally "as is", user later can try Push it to the cloud manually
        LOG.warn("Constraint violation while synchronizing cloud file creation: " + e.getMessage() + ". "
            + (e.getCause() != null ? e.getCause().getMessage() : "") + ". File exists only locally " + filePath);

        // throw to the caller to add the file to ignored list in this SyncFilesCommand
        throw new SkipChangeException(e.getMessage() + ". Local file cannot be synchronized.", e);
      }

      // we can know the id after the sync
      fileId = file.getId();

      if (LOG.isDebugEnabled()) {
        LOG.debug("Created file " + fileId + " " + filePath);
      }
    }
  }

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

    public boolean isNew(String fileIdOrPath) {
      return JCRLocalCloudDrive.this.isNew(fileIdOrPath);
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

  protected final SyncCommand                             noSync              = new NoSyncCommand();

  /**
   * Currently active synchronization command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<SyncCommand>            currentSync         = new AtomicReference<SyncCommand>(noSync);

  /**
   * Synchronization lock used by the whole drive sync and local files changes synchronization.
   */
  protected final ReadWriteLock                           syncLock            = new ReentrantReadWriteLock(true);

  /**
   * File changes currently processing by the drive. Used for locking purpose to maintain consistency.
   */
  protected final ConcurrentHashMap<String, FileChange>   fileChanges         = new ConcurrentHashMap<String, FileChange>();

  /**
   * Path or/and Ids of currently synchronizing files with counter of how many times it proceeds to sync. When
   * counter become zero it should be removed. Used for informational purpose (for UI etc).
   */
  protected final ConcurrentHashMap<String, AtomicLong>   updating            = new ConcurrentHashMap<String, AtomicLong>();

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
   * Messages generated by last executed {@link SyncFilesCommand} for next command that will be returned to an
   * user (next sync).
   */
  protected final Queue<CloudDriveMessage>                syncFilesMessages   = new ConcurrentLinkedQueue<CloudDriveMessage>();

  /**
   * Files updated by last executed {@link SyncFilesCommand} for next command that will be returned to an
   * user (next sync).
   */
  protected final Queue<CloudFile>                        syncFilesChanged    = new ConcurrentLinkedQueue<CloudFile>();

  /**
   * Drive commands active currently {@link Command}. Used for awaiting the drive readiness (not accurate, for
   * tests or information purpose only).
   */
  protected final Queue<Command>                          driveCommands       = new ConcurrentLinkedQueue<Command>();

  /**
   * Default drive state. See {@link #getState()}.
   */
  protected DriveState                                    state               = new DriveState();

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
                               NodeFinder finder,
                               ExtendedMimeTypeResolver mimeTypes) throws CloudDriveException, RepositoryException {

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
  public String getLink() throws DriveRemovedException, NotConnectedException, RepositoryException {
    Node rootNode = rootNode();
    try {
      return rootNode.getProperty("ecd:url").getString();
    } catch (PathNotFoundException e) {
      if (rootNode.getProperty("ecd:connected").getBoolean()) {
        throw e;
      } else {
        throw new NotConnectedException("Drive not connected " + titleCached);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getId() throws DriveRemovedException, NotConnectedException, RepositoryException {
    Node rootNode = rootNode();
    try {
      return rootNode.getProperty("ecd:id").getString();
    } catch (PathNotFoundException e) {
      if (rootNode.getProperty("ecd:connected").getBoolean()) {
        throw e;
      } else {
        throw new NotConnectedException("Drive not connected " + titleCached);
      }
    }
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
   * @inherritDoc
   */
  @Override
  public boolean hasFile(String path) throws DriveRemovedException, RepositoryException {
    Node driveNode = rootNode();
    try {
      Item target = finder.findItem(driveNode.getSession(), path); // take symlinks in account
      String nodePath = target.getPath();
      String drivePath = driveNode.getPath();
      if (nodePath.length() > drivePath.length() && nodePath.startsWith(drivePath)) {
        if (target.isNode()) {
          // here we check that the node is of cloud file type and not ignored
          return fileNode((Node) target) != null;
        }
      }
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Path not found in drive " + title() + ": " + e.getMessage());
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
                                                     NotCloudDriveException,
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
      throw new NotCloudDriveException("File '" + parentPath + "' does not belong to '" + title() + "' Cloud Drive.");
    }
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
    String drivePath = rootNode().getPath();
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

  // ************* abstract ****************

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
   * @param node {@link Node} to move
   * @throws DriveRemovedException
   * @throws SyncNotSupportedException
   * @throws RepositoryException
   * @return {@link CloudFileAPI} instance
   */
  protected abstract CloudFileAPI createFileAPI() throws DriveRemovedException,
                                                  SyncNotSupportedException,
                                                  RepositoryException;

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
      rootNode.setProperty("exo:title", titleCached = getUser().createDriveTitle());
    } else {
      titleCached = rootNode.getProperty("exo:title").getString();
    }

    rootNode.setProperty("ecd:connected", false);
    // know who actually initialized the drive
    rootNode.setProperty("ecd:localUserName", session.getUserID());
    rootNode.setProperty("ecd:initDate", Calendar.getInstance());
    // FIXME how to store provider properly? need store its API version?
    rootNode.setProperty("ecd:provider", getUser().getProvider().getId());

    // dummy id and url here, actual will be set during the connect
    rootNode.setProperty("ecd:id", DUMMY_DATA);
    rootNode.setProperty("ecd:url", DUMMY_DATA);

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
   * Disconnect drive connected to given node. This method doesn't check if the node represents the drive root
   * node. This method also doesn't fire onDisconnect event to drive listeners.
   * 
   * @param rootNode {@link Node}
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  private synchronized void disconnect(Node rootNode) throws CloudDriveException, RepositoryException {
    try {
      try {
        // save disconnected status immediately
        rootNode.setProperty("ecd:connected", false);
        rootNode.getProperty("ecd:connected").save(); // only this property

        // remove all existing cloud files
        for (NodeIterator niter = rootNode.getNodes(); niter.hasNext();) {
          niter.nextNode().remove();
        }

        rootNode.save();
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
  public FilesState getState() throws DriveRemovedException,
                               RefreshAccessException,
                               CloudProviderException,
                               RepositoryException {
    return state;
  }

  // ============== JCR impl specific methods ==============

  /**
   * Init cloud file removal as planned. Initialized file will be later used by the drive listener when
   * removal will be saved in JCR. This method created for use from JCR pre-remove action. If node isn't a
   * cloud file or already ignored, this method will ignore it (has no effect).
   * 
   * @param node {@link Node} a node representing a file in the drive.
   * @throws SyncNotSupportedException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected void initRemove(Node node) throws SyncNotSupportedException, CloudDriveException, RepositoryException {
    // Note: this method also can be invoked via RemoveCloudFileAction on file trashing in Trash service of
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
      }
      // we may replace something previous in the map, just ignore it and rely on latest initialized
      planned.put(path, remove);
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
    fileCopies.put(fileId, filePath);
  }

  CloudFileSynchronizer synchronizer(Node file) throws RepositoryException, SkipSyncException, SyncNotSupportedException {
    for (CloudFileSynchronizer s : fileSynchronizers) {
      if (s.accept(file)) {
        return s;
      }
    }
    throw new SyncNotSupportedException("Synchronization not supported for file type " + file.getPrimaryNodeType().getName()
        + " in node " + file.getPath());
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

  private boolean hasChanged(String fileId, String... changeTypes) throws RepositoryException, CloudDriveException {
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

  private void cleanChanged(String fileId, String... changeTypes) throws RepositoryException, CloudDriveException {
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
  protected synchronized void saveChanges(List<FileChange> changes) throws RepositoryException, CloudDriveException {

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
   * Commit given changes to the drive local history but omit skipped. These changes also will be removed from
   * the local changes (from previously saved and not yet applied).
   * 
   * @param changes {@link Collection} of {@link FileChange} changes to move from changes store to the history
   * @param skipped {@link Collection} of {@link FileChange} changes that should be removed from changes store
   *          but
   *          not added to the history
   * @throws RepositoryException
   * @throws CloudDriveException
   * @see #saveChanges(List)
   * @see #rollbackChanges(List)
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
            for (FileChange sch : skipped) {
              if (ch.startsWith(sch.changeId)) {
                continue next; // omit this change as it was skipped
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
   * Rollback (remove) all changes from the drive local changes store. This method will save nothing to
   * local history of already applied changes.<br>
   * This method should be used when we exactly know that all changes saved but not committed to the history
   * are not actual anymore, e.g. in case when we do a full sync with remote side.
   * Such changes can be a result of failed {@link SyncFilesCommand} and rollback caused by a
   * {@link SyncCommand}.
   * 
   * @throws RepositoryException
   * @throws CloudDriveException
   * 
   * @see #saveChanges(List)
   * @see #commitChanges(List)
   * @see #rollbackChanges(List)
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
   * Rollback (remove) given changes from the drive local changes store. This method will save nothing to
   * local history of already applied changes.<br>
   * This method should be used when we exactly know that given changes saved but not committed to the history
   * are not actual anymore, e.g. in case when we restore a file/sub-tree.
   * Such changes can be a result of failed {@link SyncFilesCommand} and rollback caused by restoration.
   * 
   * @param changes {@link List} of {@link FileChange}
   * @throws RepositoryException
   * @throws CloudDriveException
   * 
   * @see #saveChanges(List)
   * @see #commitChanges(List)
   * @see #rollbackAllChanges()
   */
  @Deprecated
  // not used
  protected synchronized void rollbackChanges(List<FileChange> changes) throws RepositoryException, CloudDriveException {

    Node driveNode = rootNode();
    StringBuilder store = new StringBuilder();

    // remove already applied local changes and collect applied as history
    try {
      Property localChanges = driveNode.getProperty("ecd:localChanges");
      String current = localChanges.getString();
      if (current.length() > 0) { // actually it should be greater of about 15 chars
        next: for (String ch : current.split("\n")) {
          if (ch.length() > 0) {
            for (FileChange fch : changes) {
              if (ch.startsWith(fch.changeId)) {
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

    return new FileChange(changeId, path, fileId, isFolder, changeType, syncClass != null ? synchronizer(syncClass) : null);
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
    // FIXME indeed, it was possible to get interrupted thread for Box connector,
    // as result, drive change Id wasn't properly set and further work blocked.

    // save in persistent store
    saveChangeId(id);

    // maintain runtime cache and sequencer
    currentChangeId.set(id);
    fileChangeSequencer.addAndGet(1 - fileChangeSequencer.get()); // reset sequencer
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
  protected boolean isDrive(String workspace, String path, boolean includeFiles) throws DriveRemovedException,
                                                                                 RepositoryException {
    Node driveNode = rootNode(true);
    if (driveNode.getSession().getWorkspace().getName().equals(workspace)) {
      try {
        Item target = finder.findItem(driveNode.getSession(), path); // take symlinks in account
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
      } catch (ItemNotFoundException e) {
        // file not found at the given path - it is not a drive
        if (LOG.isDebugEnabled()) {
          LOG.debug("Path not found in drive " + title() + ": " + e.getMessage());
        }
      }
    }
    return false;
  }

  protected boolean isSameDrive(Node anotherNode) throws RepositoryException {
    if (anotherNode.isNodeType(ECD_CLOUDDRIVE)) {
      // we cannot compare ecd:id here as they can be equal for some providers (e.g. Box)
      return this.rootUUID.equals(anotherNode.getUUID());
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
    return rootNode(false);
  }

  /**
   * Drive's storage root node opened in JCR session.
   * 
   * @param systemSession {@link Boolean} if <code>true</code> then root node will be open in system session,
   *          <code>false</code> - in current user session.
   * @return {@link Node}
   * @throws RepositoryException
   */
  protected Node rootNode(boolean systemSession) throws DriveRemovedException, RepositoryException {
    SoftReference<Node> rootNodeRef = rootNodeHolder.get();
    Node rootNode;
    if (rootNodeRef != null) {
      rootNode = rootNodeRef.get();
      ConversationState cs = ConversationState.getCurrent();
      if (rootNode != null && rootNode.getSession().isLive() && cs != null
          && IdentityHelper.isUserMatch(rootNode.getSession().getUserID(), cs.getIdentity().getUserId())) {
        try {
          // FYI as more light alternative rootNode.getIndex() can be used to
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

    Session session = systemSession ? systemSession() : session();
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
            + (commandName != null ? "of " + commandName + " command " : "") + "to listeners on Cloud Drive '" + title()
            + "':" + e.getMessage());
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
      LOG.warn("Error firing error '" + error.getMessage() + "' "
          + (commandName != null ? "of " + commandName + " command " : "") + "to listeners on Cloud Drive '" + title() + "':"
          + e.getMessage());
    }
  }

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
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already extends the file NT
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
            // node not cleaned, this node stays only local (creation in progress or ignored),
            // and we need a new name for the file node
            StringBuilder newName = new StringBuilder();
            newName.append(baseName);
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
        if (!titleTried) {
          // Feb 17 2015: try by original title (usecase with '+' in name of uploaded file and Box in PLF 4.1)
          titleTried = true;
          try {
            if (parent.hasNode(fileTitle)) {
              name = fileTitle;
              continue;
            }
          } catch (RepositoryException te) {
            // assume any JCR error as not acceptable name
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
    // FIXME try avoid do this, or at least do this in single place (JCR listener?)
    return IdentityHelper.ensureOwned(node, systemSession());
  }

  protected Node openFile(String fileId, String fileTitle, Node parent) throws RepositoryException, CloudDriveException {
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
      } // else, node just cleaned (removed)
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
  protected Node readNode(Node parent, String fileTitle, String fileId) throws RepositoryException, CloudDriveException {
    Node node;
    String baseName = nodeName(fileTitle);
    String name = baseName;
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
   * Find local nodes using JCR SQL query by array of file ids. Note, it will search only persisted nodes -
   * not saved
   * files cannot be found in JCR.
   * 
   * @param ids {@link Collection} of {@link String}
   * @return {@link Collection} of nodes
   * @throws RepositoryException
   * @throws DriveRemovedException
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

    Query q = qm.createQuery("SELECT * FROM " + ECD_CLOUDFILE + " WHERE " + idstmt + " AND jcr:path LIKE '"
        + rootNode.getPath() + "/%'", Query.SQL);
    QueryResult qr = q.execute();

    for (NodeIterator niter = qr.getNodes(); niter.hasNext();) {
      res.add(niter.nextNode());
    }

    return res;
  }

  protected JCRLocalCloudFile readFile(Node fileNode) throws RepositoryException {
    String title = fileAPI.getTitle(fileNode);
    boolean isFolder = fileNode.isNodeType(ECD_CLOUDFOLDER);
    String type = fileNode.getProperty("ecd:type").getString();
    String typeMode = isFolder ? null : mimeTypes.getMimeTypeMode(type, title);
    String link = link(fileNode);
    // folder has no preview/edit links by definition (we rely on ECMS doc views)
    String previewLink = isFolder ? null : previewLink(fileNode);
    String editLink = isFolder ? null : editLink(link, fileNode);
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
                                 fileNode.getProperty("ecd:lastUser").getString(),
                                 fileNode.getProperty("ecd:author").getString(),
                                 fileNode.getProperty("ecd:created").getDate(),
                                 fileNode.getProperty("ecd:modified").getDate(),
                                 isFolder,
                                 size,
                                 fileNode,
                                 false);
  }

  /**
   * Init or update Cloud File structure on local JCR node.
   * 
   * @param fileNode {@link Node}
   * @param title
   * @param id
   * @param type
   * @param link
   * @param previewLink, optional, can be null
   * @param thumbnailLink, optional, can be null
   * @param author
   * @param lastUser
   * @param created
   * @param modified
   * @throws RepositoryException
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
   * @param node {@link Node}
   * @throws RepositoryException
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
    // we do tolerantly: set when value available, this also avoid removing existing by a null value
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
      node.setProperty("exo:dateCreated", created);
      node.setProperty("exo:dateModified", modified);
    }

    if (node.isNodeType(EXO_MODIFY)) {
      node.setProperty("exo:lastModifiedDate", modified);
      node.setProperty("exo:lastModifier", lastUser);
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
   * @throws RepositoryException
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

  /**
   * Return given node if it describes connected cloud file. If it is not yet
   * cloud file or was just cleaned (removed) the method will return <code>null</code>.
   * 
   * @param node {@link Node} local node
   * @return {@link Node} local node or <code>null</code> if file cleaned (removed)
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
    if (openingParenthesesPos > 0 && closingParenthesesPos > 0 && closingParenthesesPos > openingParenthesesPos + 1) {
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
      String newName = nodeName(newTitle);
      if (!parent.hasNode(newName)) {
        session.move(file.getPath(), parent.getPath() + "/" + newName);
        file.setProperty("exo:title", newTitle);
        if (file.hasProperty("exo:name")) {
          file.setProperty("exo:name", newTitle);
        }
        break;
      }
    } while (true);
  }

  /**
   * Return provider specific link for a file preview. By default this method will try read value of
   * <code>ecd:previewUrl</code> property and if not such property exists <code>null</code> will be returned.
   * Actual connector implementation may override this logic.
   * 
   * @param fileNode {@link String} cloud file node
   * @return String with a link should be used for file preview.
   * @throws RepositoryException
   */
  protected String previewLink(Node fileNode) throws RepositoryException {
    try {
      return fileNode.getProperty("ecd:previewUrl").getString();
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  /**
   * Return provider specific link for a file thumbnail. By default this method will try read value of
   * <code>ecd:thumbnailUrl</code> property and if not such property exists <code>null</code> will be
   * returned.
   * Actual connector implementation may override this logic.
   * 
   * @param fileNode {@link String} cloud file node
   * @return String with a link should be used for file thumbnail.
   * @throws RepositoryException
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
   * Return provider specific link for file content download or access. By default this method will try read
   * value of <code>ecd:url</code> property and if not such property exists {@link PathNotFoundException} will
   * be thrown.
   * Actual connector implementation may override its logic.
   * 
   * @param fileNode {@link String} existing link to a cloud file or <code>null</code> if editing not
   *          supported
   * @return String with a link should be used for file editing.
   * @throws RepositoryException
   */
  protected String link(Node fileNode) throws PathNotFoundException, RepositoryException {
    return fileNode.getProperty("ecd:url").getString();
  }

  /**
   * Read edit link for a cloud file denoted by given node. By default edit link is <code>null</code> -
   * editing not supported.
   * 
   * @param fileLink {@link String} file link, can be used to build edit link by the connector implementation
   * @param fileNode {@link Node}
   * @return {@link String} an URL to edit cloud file or <code>null</code> if editing not supported
   * @throws RepositoryException
   */
  protected String editLink(String fileLink, Node fileNode) throws RepositoryException {
    return null;
  }

  /**
   * Read cloud file size from given node. If size not available then -1 will be returned.
   * 
   * @param fileNode {@link Node}
   * @return {@link Long} file size in bytes or -1 if size not available
   * @throws RepositoryException
   */
  protected long size(Node fileNode) throws RepositoryException {
    try {
      return fileNode.getProperty("ecd:size").getLong();
    } catch (PathNotFoundException e) {
      return -1;
    }
  }

  protected boolean isUpdating(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() > 0;
    }
    return false;
  }

  protected boolean isNew(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() == 0;
    }
    return false;
  }

  protected boolean isNewOrUpdating(String key) {
    AtomicLong counter = updating.get(key);
    if (counter != null) {
      return counter.longValue() >= 0;
    }
    return false;
  }

  protected boolean initUpdating(String key) {
    AtomicLong existingCounter = updating.putIfAbsent(key, new AtomicLong(0));
    boolean res;
    if (existingCounter != null) {
      res = existingCounter.longValue() == 0;
    } else {
      res = true;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("> initUpdating " + key + " " + res);
    }
    return res;
  }

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
      LOG.debug("> addUpdating " + key + " " + counter);
    }
    return counter;
  }

  protected boolean removeUpdating(String key) {
    AtomicLong counter = updating.get(key);
    boolean res = false;
    if (counter != null) {
      if (counter.decrementAndGet() <= 0) {
        // FYI AtomicLong has no special equals() method, thus value will not be checked the removal
        res = updating.remove(key, counter);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("< removeUpdating " + key + " " + (counter != null ? counter.longValue() : ""));
    }
    return res;
  }

  /**
   * Check if given drive node is belong to this user already. This method assumes that given node already of
   * cloud drive nodetype (ecd:cloudDrive).
   * 
   * @param user {@link String}
   * @param driveNode {@link Node}
   * @throws RepositoryException
   * @throws CannotConnectDriveException
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
        try {
          Item target = finder.findItem(node.getSession(), node.getPath()); // take symlinks in account
          if (target.isNode()) {
            node = (Node) target;
            // 22.05.2014 removed check: && fileNode(node) != null
            return node.getPath().startsWith(driveNode.getPath());
          }
        } catch (PathNotFoundException e) {
          // file not found at the given path - it is not a drive
          if (LOG.isDebugEnabled()) {
            LOG.debug("Path not found in drive " + title() + ": " + e.getMessage());
          }
        }
      }
    }
    return false;
  }

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
   * Construct a cloud file node name from a file title. This method should be used everywhere for cloud nodes
   * creation or modification and reading.<br>
   * Connector implementation may override this logic when required (e.g. for path based file IDs).
   * 
   * @return String with cloud file node name
   */
  protected String nodeName(String title) {
    return cleanName(title);
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
   * Make JCR compatible item name.
   * 
   * @param String str
   * @return String - JCR compatible name of local file
   */
  public static String cleanName(String name) {
    String str = accentsConverter.transliterate(name.trim());
    // the character ? seems to not be changed to d by the transliterate function
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
   * Check if given node isn't a node in eXo Trash folder and throw {@link DriveRemovedException} if it is.
   * 
   * @param node {@link Node}
   * @throws RepositoryException
   * @throws DriveRemovedException if given node in the eXo Trash.
   */
  public static void checkNotTrashed(Node node) throws RepositoryException, DriveRemovedException {
    if (node.getParent().isNodeType(EXO_TRASHFOLDER)) {
      throw new DriveTrashedException("Drive " + node.getPath() + " was moved to Trash.");
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

}
