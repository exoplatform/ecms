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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveAccessException;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveEnvironment;
import org.exoplatform.clouddrive.CloudDriveEvent;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;

import java.lang.ref.SoftReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
  public static final String        ECD_CLOUDDRIVE        = "ecd:cloudDrive";

  /**
   * File nodetype {@code ecd:cloudFile}.
   */
  public static final String        ECD_CLOUDFILE         = "ecd:cloudFile";

  /**
   * Folder nodetype {@code ecd:cloudFolder}, it extends file.
   */
  public static final String        ECD_CLOUDFOLDER       = "ecd:cloudFolder";

  /**
   * File resource nodetype {@code ecd:cloudFileResource}.
   */
  public static final String        ECD_CLOUDFILERESOURCE = "ecd:cloudFileResource";

  public static final String        EXO_DATETIME          = "exo:datetime";

  public static final String        EXO_MODIFY            = "exo:modify";

  public static final String        EXO_TRASHFOLDER       = "exo:trashFolder";

  public static final String        NT_FOLDER             = "nt:folder";

  public static final String        NT_FILE               = "nt:file";

  public static final String        NT_RESOURCE           = "nt:resource";

  public static final String        NT_UNSTRUCTURED       = "nt:unstructured";

  public static final String        DUMMY_DATA            = "";

  public static final String        USER_WORKSPACE        = "user.workspace";

  public static final String        USER_NODEPATH         = "user.nodePath";

  public static final String        USER_SESSIONPROVIDER  = "user.sessionProvider";

  /**
   * Command stub for not running or already done commands.
   */
  protected static final Command    ALREADY_DONE          = new AlreadyDone();

  protected static final DateFormat DATE_FORMAT           = new SimpleDateFormat("yyyyMMdd.HHmmss");

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
   * Perform actual removal of the drive from JCR on its move to the Trash.
   */
  class NodeRemoveHandler {
    class RemoveListener implements EventListener {
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
            finishRemove(session(), initialRootPath);
            return;
          }

          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            if (event.getType() == Event.NODE_REMOVED && initialRootPath.equals(event.getPath())) {
              removed = true; // set only if not exists
            }
          }

          checkRemove(driveRoot);
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive '" + title() + "' node move/remove event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    class AddListener implements EventListener {
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
            // node already deleted
            LOG.warn("Cloud Drive " + title() + " node already removed directly from JCR: " + e.getMessage());
            finishRemove(session, initialRootPath);
            return;
          }

          String rootPath = driveRoot.getPath();
          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            if (event.getType() == Event.NODE_ADDED && rootPath.equals(event.getPath())) {
              added = true;
            }
          }

          checkRemove(driveRoot);
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive " + title() + " node move/add event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    final String         initialRootPath;

    final RemoveListener removeListener;

    final AddListener    addListener;

    volatile boolean     removed = false;

    volatile boolean     added   = false;

    NodeRemoveHandler(String initialRootPath) {
      this.initialRootPath = initialRootPath;
      this.removeListener = new RemoveListener();
      this.addListener = new AddListener();
    }

    synchronized void checkRemove(Node driveRoot) throws RepositoryException {
      if (removed && added) {
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

            finishRemove(session, driveRoot.getPath());
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

    void finishRemove(Session session, String rootPath) {
      // reset flags and unregister both listeners from the Observation
      removed = added = false;

      try {
        removeJCRListener(session);
      } catch (RepositoryException e) {
        LOG.error("Error unregistering Cloud Drive '" + title() + "' node listeners: " + e.getMessage(), e);
      }

      // fire listeners
      listeners.fireOnRemove(new CloudDriveEvent(getUser(), rootWorkspace, rootPath));
    }
  }

  /**
   * Delete file on its move to the outside of the drive. (Need create symlink to actual drive's file). XXX
   * NOT USED!
   */
  @Deprecated
  class NodeMoveHandler {
    class FileNode {

      String path;

      String id;

      Node   node;

    }

    class RemoveListener implements EventListener {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        String userId = null; // for error messages
        try {
          Node driveRoot;
          try {
            driveRoot = rootNode();
          } catch (DriveRemovedException e) {
            LOG.error("Cloud Drive " + title() + " node was removed: " + e.getMessage());
            return;
          }

          List<FileNode> nodes = new ArrayList<FileNode>();
          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            if (event.getType() == Event.NODE_REMOVED && event.getPath().startsWith(initialRootPath)) {
              // if removed, lies on this drive path and has ecd:driveUUID pointing to this drive
              // String driveUUID = localNode.getProperty("ecd:driveUUID").getString();
              FileNode file = new FileNode();
              file.path = event.getPath();
              nodes.add(file);
            }
          }
          moved.set(nodes);

          // checkRemove(driveRoot);
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive " + title() + " node move/remove event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    class AddListener implements EventListener {
      /**
       * {@inheritDoc}
       */
      @Override
      public void onEvent(EventIterator events) {
        String userId = null; // for error messages
        try {
          Node driveRoot;
          try {
            driveRoot = rootNode();
          } catch (DriveRemovedException e) {
            LOG.error("Cloud Drive " + title() + " node was removed: " + e.getMessage());
            return;
          }

          String rootPath = driveRoot.getPath();
          List<Node> added = new ArrayList<Node>();
          while (events.hasNext()) {
            Event event = events.nextEvent();
            userId = event.getUserID();
            if (event.getType() == Event.NODE_ADDED && !event.getPath().startsWith(rootPath)) {
              // if removed, doesn't lie on this drive path and has ecd:driveUUID pointing to this drive
              // move the node back to the drive and create a symlink instead this node on the new location
              Item addedItem = driveRoot.getSession().getItem(event.getPath());
              if (addedItem.isNode()) {
                added.add((Node) addedItem);
              }
            }
          }

          if (!added.isEmpty()) {
            checkMove(added);
          }
        } catch (AccessDeniedException e) {
          // skip other users nodes
        } catch (RepositoryException e) {
          LOG.error("Error handling Cloud Drive " + title() + " node move/add event"
              + (userId != null ? " for user " + userId : ""), e);
        }
      }
    }

    final String                initialRootPath;

    final RemoveListener        removeListener;

    final AddListener           addListener;

    ThreadLocal<List<FileNode>> moved = new ThreadLocal<List<FileNode>>();

    NodeMoveHandler(String initialRootPath) {
      this.initialRootPath = initialRootPath;
      this.removeListener = new RemoveListener();
      this.addListener = new AddListener();
    }

    synchronized void addMoved(String path) throws RepositoryException {
      List<FileNode> nodes = moved.get();
      if (nodes == null) {
        nodes = new ArrayList<FileNode>();
        moved.set(nodes);
      }

      FileNode file = new FileNode();
      file.path = path;
      nodes.add(file);
    }

    synchronized void updateMoved(String path, Node node) throws RepositoryException {
      List<FileNode> nodes = moved.get();
      if (nodes == null) {
        nodes = new ArrayList<FileNode>();
        moved.set(nodes);
      }

      FileNode file = new FileNode();
      file.path = path;
      nodes.add(file);
    }

    synchronized void checkMove(List<Node> added) throws RepositoryException {
      List<FileNode> nodes = moved.get();
      if (nodes != null) {
        // TODO create symlink here
        // Session session = addedNode.getSession();
        // session.save();

        // reset
        // removed.set(null);
      }
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
    protected Node                         driveRoot;

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
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected AbstractCommand() throws RepositoryException, DriveRemovedException {

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
     * Start command execution. If command will fail due to provider error, the execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the throwing an exception.
     * 
     * @throws CloudDriveAccessException
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected void exec() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      startTime.set(System.currentTimeMillis());

      try {
        commandEnv.prepare(this);

        driveRoot = rootNode(); // init in actual runner thread

        startAction(JCRLocalCloudDrive.this);

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
                handleError(driveRoot, e, getName());
                throw e;
              } else {
                rollback(driveRoot);
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
              handleError(driveRoot, e, getName());
              throw e;
            }
          }
        }
      } catch (CloudDriveException e) {
        handleError(driveRoot, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RepositoryException e) {
        handleError(driveRoot, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } catch (RuntimeException e) {
        handleError(driveRoot, e, getName());
        commandEnv.fail(this, e);
        throw e;
      } finally {
        doneAction();
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
    protected Future<Command> execAsync() throws CloudDriveException {
      commandEnv.configure(this);

      Callable<Command> callable = new CommandCallable(this);

      return async = commandExecutor.submit(getName(), callable);
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
      return finishTime.get() > 0;
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
      if (async != null) {
        async.get();
      } // else do nothing - command already done
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
    protected void process() throws CloudDriveException, RepositoryException {
      // fetch all files to local storage
      fetchFiles();

      // connected drive properties
      driveRoot.setProperty("ecd:cloudUserId", getUser().getId());
      driveRoot.setProperty("ecd:cloudUserName", getUser().getUsername());
      driveRoot.setProperty("ecd:userEmail", getUser().getEmail());
      driveRoot.setProperty("ecd:connectDate", Calendar.getInstance());

      // mark as connected
      driveRoot.setProperty("ecd:connected", true);

      // and save the node
      driveRoot.save();

      // fire listeners
      listeners.fireOnConnect(new CloudDriveEvent(getUser(), rootWorkspace, driveRoot.getPath()));
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

    /**
     * Constructor for synchronization command.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected SyncCommand() throws RepositoryException, DriveRemovedException {
      super();
    }

    @Override
    public String getName() {
      return "synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveAccessException, CloudDriveException, RepositoryException {
      // synchronize
      try {
        syncFiles();

        // and save the drive node
        driveRoot.save();
      } finally {
        currentSync.set(null);
      }

      // fire listeners
      listeners.fireOnSynchronized(new CloudDriveEvent(getUser(), rootWorkspace, driveRoot.getPath()));
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
      String rootId = driveRoot.getProperty("ecd:id").getString();
      List<Node> rootList = new ArrayList<Node>();
      rootList.add(driveRoot);
      nodes.put(rootId, rootList);
      readNodes(driveRoot, nodes, true);

      this.nodes = nodes;
    }
  }

  /**
   * Single file synchronization processor for actual implementation of Cloud Drive and its storage.
   */
  protected abstract class SyncFileCommand extends AbstractCommand {

    final Node    fileNode;

    final boolean isFolder;

    /**
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    public SyncFileCommand(Node file) throws RepositoryException,
        DriveRemovedException,
        SyncNotSupportedException {
      super();

      Node parentNode = file.getParent();

      // TODO cleanup
      if (file.isNodeType(ECD_CLOUDFILE)) {
        // it's already existing cloud file, need sync its content
        // cloudFile = drive.openFile(file.getName(), null);
        isFolder = false;
      } else if (file.isNodeType(ECD_CLOUDFOLDER)) {
        // it's already existing cloud folder, need sync its properties
        // cloudFile = drive.openFile(file.getName(), null);
        isFolder = true;
      } else if (file.isNodeType(ECD_CLOUDFILERESOURCE)) {
        // it's resource subnode of the ecd:cloudDrive
        // need sync on the parent
        file = parentNode;
        parentNode = file.getParent();
        isFolder = false;
        // cloudFile = drive.openFile(file.getName(), null);
      } else if (file.isNodeType(NT_FILE)) {
        // it's new local JCR node - upload it to the cloud
        //String mimeType = file.getNode("jcr:content").getProperty("jcr:mimeType").getString();
        isFolder = false;
        // cloudFile = drive.openFile(file.getName(), mimeType);
      } else if (file.isNodeType(NT_RESOURCE)) {
        // it's resource of new local JCR node - upload this nt:file to the cloud
        //String mimeType = file.getProperty("jcr:mimeType").getString();
        file = parentNode;
        parentNode = file.getParent();
        isFolder = false;
        // cloudFile = drive.openFile(file.getName(), mimeType);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return "file synchronization";
    }
  }

  // *********** variables ***********

  /**
   * Support for JCR actions. To do not fire on synchronization (our own modif) methods.
   */
  protected static final ThreadLocal<CloudDrive>        actionDrive    = new ThreadLocal<CloudDrive>();

  protected final Transliterator                        accentsConverter;

  protected final String                                rootWorkspace;

  protected final ManageableRepository                  repository;

  protected final SessionProviderService                sessionProviders;

  protected final CloudUser                             user;

  protected final String                                rootUUID;

  protected final ThreadLocal<SoftReference<Node>>      rootNodeHolder;

  protected final NodeRemoveHandler                     handler;

  /**
   * Currently active connect command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<ConnectCommand>       currentConnect = new AtomicReference<ConnectCommand>();

  /**
   * Currently active synchronization command. Used to control concurrency in Cloud Drive.
   */
  protected final AtomicReference<SyncCommand>          currentSync    = new AtomicReference<SyncCommand>();

  /**
   * Managed queue of commands.
   */
  protected final CommandPoolExecutor                   commandExecutor;

  /**
   * Environment for commands execution.
   */
  protected final CloudDriveEnvironment commandEnv     = new ExoJCREnvironment();

  /**
   * Title has special care. It used in error logs and an attempt to read <code>exo:title</code> property can
   * cause another {@link RepositoryException}. Thus need it pre-cached in the variable and try to read the
   * <code>exo:title</code> property each time, but if not successful use this one cached.
   */
  private String                                        titleCached;

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
    this.accentsConverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

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

    // add drive removal listener
    this.handler = addJCRListener(driveNode);
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
          throw new NotCloudFileException("Node '" + path + "' is not a Cloud Drive file.");
        }
        return readFile(fileNode);
      } else {
        throw new NotCloudFileException("Item at path '" + path
            + "' is Property and cannot be read as Cloud Drive file.");
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
   * @param driveRoot a {@link Node} to initialize
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    Session session = driveNode.getSession();

    driveNode.addMixin(ECD_CLOUDDRIVE);
    if (!driveNode.hasProperty("exo:title")) {
      // default title
      driveNode.setProperty("exo:title", titleCached = getUser().getProvider().getName() + " - "
          + getUser().getEmail());
    } else {
      titleCached = driveNode.getProperty("exo:title").getString();
    }

    driveNode.setProperty("ecd:connected", false);
    // know who actually initialized the drive
    driveNode.setProperty("ecd:localUserName", session.getUserID());
    driveNode.setProperty("ecd:initDate", Calendar.getInstance());
    // TODO how to store provider properly? need store its API version?
    driveNode.setProperty("ecd:provider", getUser().getProvider().getId());

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
  public Command connect(boolean async) throws CloudDriveException, RepositoryException {
    if (isConnected()) {
      // already connected
      return ALREADY_DONE;
    } else {
      ConnectCommand connect = currentConnect.get();
      if (connect == null) {
        synchronized (currentConnect) {
          // check again in synchronized block!
          connect = currentConnect.get();
          if (connect == null) {
            connect = getConnectCommand();
            currentConnect.set(connect);
            connect.execAsync();
          }
        }
      } // else, connect already active

      // connect already active, wait if it's not async request
      if (!async) {
        try {
          connect.await();
        } catch (InterruptedException e) {
          LOG.warn("Caller of connect command interrupted.", e);
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          Throwable err = e.getCause();
          if (err instanceof CloudDriveException) {
            throw (CloudDriveException) err;
          } else if (err instanceof RepositoryException) {
            throw (RepositoryException) err;
          } else if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
          } else {
            throw new UndeclaredThrowableException(err, "Error connecting drive: " + err.getMessage());
          }
        }
      }

      return connect;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Command connect() throws DriveRemovedException,
                                       CloudDriveException,
                                       RepositoryException {
    return connect(false);
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
   * Factory method to create an actual implementation of {@link SyncCommand} command.
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
   * Factory method to create an actual implementation of {@link SyncFileCommand} command.
   * 
   * @param file {@link Node} to synchronize
   * @throws DriveRemovedException
   * @throws SyncNotSupportedException
   * @throws RepositoryException
   * @return {@link SyncFileCommand} instance
   */
  protected abstract SyncFileCommand getSyncFileCommand(Node file) throws DriveRemovedException,
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
            Node existing = niter.nextNode();
            if (existing.isNodeType(ECD_CLOUDFILE)) {
              existing.remove();
            } else {
              LOG.warn("Not a cloud file detected " + existing.getPath()
                  + ". Such files should not be in cloud drive: " + rootNode.getPath() + ".");
            }
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
  public Command synchronize(boolean async) throws SyncNotSupportedException,
                                           DriveRemovedException,
                                           CloudDriveException,
                                           CloudDriveAccessException,
                                           RepositoryException {
    if (isConnected()) {
      checkAccess();

      SyncCommand sync = currentSync.get();
      if (sync == null) {
        synchronized (currentSync) {
          // check again in synchronized block!
          sync = currentSync.get();
          if (sync == null) {
            sync = getSyncCommand();
            currentSync.set(sync);
            sync.execAsync();
          }
        }
      } // else, sync already active

      // sync already active, wait if it's not async request
      if (!async) {
        try {
          sync.await();
        } catch (InterruptedException e) {
          LOG.warn("Caller of synchronization command interrupted.", e);
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          Throwable err = e.getCause();
          if (err instanceof CloudDriveException) {
            // LOG.error("Cloud Drive error during synchronization: " + err.getMessage(), err);
            throw (CloudDriveException) err;
          } else if (err instanceof RepositoryException) {
            throw (RepositoryException) err;
          } else if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
          } else {
            // LOG.error("Error synchronizing drive: " + err.getMessage(), err);
            throw new UndeclaredThrowableException(err, "Error synchronizing drive: " + err.getMessage());
          }
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
  @Override
  public Command synchronize() throws SyncNotSupportedException, CloudDriveException, RepositoryException {

    return synchronize(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command synchronize(Node file) throws SyncNotSupportedException,
                                       NotConnectedException,
                                       CloudDriveException,
                                       RepositoryException {
    if (isConnected()) {
      final String filePath = file.getPath();
      final String rootPath = rootNode().getPath();
      if (filePath.equals(rootPath)) {
        // sync whole drive and refresh asked object
        return synchronize();
      } else if (filePath.startsWith(rootPath)) {
        SyncFileCommand sync = getSyncFileCommand(file);
        sync.execAsync();
        return sync;
      } else {
        throw new SyncNotSupportedException("Synchronization not supported for not cloud drive file: "
            + filePath);
      }
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
          // XXX it's workaround to prevent JCR Observation NPE
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
    Node localNode;
    String parentPath = parent.getPath();
    String title = cleanName(fileTitle);
    String nodeName = title;

    int siblingNumber = 1;
    do {
      try {
        localNode = parent.getNode(nodeName);
        // should be ecd:cloudFile or ecd:cloudFolder, note: folder already extends the file NT
        if (localNode.isNodeType(ECD_CLOUDFILE)) {
          if (fileId.equals(localNode.getProperty("ecd:id").getString())) {
            break; // we found node
          } else {
            // find new name for the local file
            StringBuilder newName = new StringBuilder();
            newName.append(title);
            newName.append('-');
            newName.append(siblingNumber);
            nodeName = newName.toString();
            siblingNumber++;
          }
        } else {
          throw new CloudDriveException("Cannot open cloud file. Another node exists in drive storage with the same name: "
              + parentPath + "/" + nodeName);
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it
        localNode = parent.addNode(nodeName, nodeType);
        break;
      }
    } while (true);

    return localNode;
  }

  protected Node openFile(String fileId, String fileTitle, String fileType, Node parent) throws RepositoryException,
                                                                                        CloudDriveException {
    Node localNode = openNode(fileId, fileTitle, parent, NT_FILE);

    // create content for new not complete node
    if (localNode.isNew() && !localNode.hasNode("jcr:content")) {
      Node content = localNode.addNode("jcr:content", NT_RESOURCE);
      setContent(content, fileType); // empty data by default
    }

    return localNode;
  }

  protected Node openFolder(String folderId, String folderTitle, Node parent) throws RepositoryException,
                                                                             CloudDriveException {
    return openNode(folderId, folderTitle, parent, NT_FOLDER);
  }

  /**
   * Move node with its subtree in scope of existing JCR session. If a node already exists at destination and
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
  protected Node moveNode(String id, String title, Node source, Node destParent) throws RepositoryException,
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
        LOG.warn("Not a cloud file detected " + cn.getPath());
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

  protected void setContent(Node content, String mimetype) throws RepositoryException {
    content.setProperty("jcr:data", DUMMY_DATA); // reset local content
  }

  /**
   * Clean up string for JCR compatible name.
   * 
   * @param String str
   * @return String - JCR compatible name of local file
   */
  protected String cleanName(String str) {
    str = accentsConverter.transliterate(str.trim());
    // the character ? seems to not be changed to d by the transliterate function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
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
  protected NodeRemoveHandler addJCRListener(Node driveNode) throws RepositoryException {
    NodeRemoveHandler handler = new NodeRemoveHandler(driveNode.getPath());
    ObservationManager observation = driveNode.getSession().getWorkspace().getObservationManager();
    observation.addEventListener(handler.removeListener,
                                 Event.NODE_REMOVED,
                                 driveNode.getParent().getPath(),
                                 false,
                                 null,
                                 null,
                                 true);
    observation.addEventListener(handler.addListener,
                                 Event.NODE_ADDED,
                                 null,
                                 false,
                                 null,
                                 new String[] { EXO_TRASHFOLDER },
                                 true);
    return handler;
  }

  /**
   * Remove Observation listener to removal from parent and addition to the Trash.
   * 
   * @param session {@link Session}
   * @throws RepositoryException
   */
  protected void removeJCRListener(Session session) throws RepositoryException {
    ObservationManager observation = session.getWorkspace().getObservationManager();
    observation.removeEventListener(handler.removeListener);
    observation.removeEventListener(handler.addListener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configure(CloudDriveEnvironment env) {
    commandEnv.chain(env);
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
}
