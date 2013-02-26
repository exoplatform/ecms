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

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveAccessException;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveEvent;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;

import com.ibm.icu.text.Transliterator;

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
  }

  /**
   * XXX Experimental. Not used.
   */
  @Deprecated
  class BufferedNodeIterator implements NodeIterator {

    final List<Node> buff    = new ArrayList<Node>();

    NodeIterator     nodes;

    int              buffPos = 0;

    BufferedNodeIterator(NodeIterator nodes) {
      this.nodes = nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void skip(long skipNum) {
      if (nodes != null) {
        for (int i = 0; i < skipNum; i++) {
          buff.add(nodes.nextNode());
        }
      } else {
        buffPos += skipNum;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() {
      if (nodes != null) {
        return nodes.getSize();
      } else {
        return buff.size();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPosition() {
      if (nodes != null) {
        return nodes.getPosition();
      } else {
        return buffPos;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
      if (nodes != null) {
        return nodes.hasNext();
      } else {
        return buffPos < buff.size() - 1;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object next() {
      return nextNode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node nextNode() {
      if (nodes != null) {
        Node n = nodes.nextNode();
        buff.add(n);
        if (!nodes.hasNext()) {
          nodes = null;
        }
        return n;
      } else {
        if (buffPos == buff.size()) {
          throw new NoSuchElementException("Buffer has no more Nodes.");
        }
        Node n = buff.get(buffPos);
        buffPos++;
        return n;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
      if (nodes != null) {
        nodes.remove();
      } else {
        if (buffPos == buff.size()) {
          throw new IllegalStateException("End of the buffer reached.");
        }
        buff.remove(buffPos);
      }
    }

    /**
     * Reset iterator position to begin of the nodes set.
     */
    void reset() {
      buffPos = 0;
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
          Node driveRoot;
          try {
            driveRoot = rootNode();
          } catch (DriveRemovedException e) {
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
          Node driveRoot;
          try {
            driveRoot = rootNode();
          } catch (DriveRemovedException e) {
            // node already deleted
            LOG.warn("Cloud Drive " + title() + " node already removed directly from JCR: " + e.getMessage());
            finishRemove(session(), initialRootPath);
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
        ObservationManager manager = session.getWorkspace().getObservationManager();
        manager.removeEventListener(removeListener);
        manager.removeEventListener(addListener);
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
  protected class CommandRunnable implements Runnable {
    final AbstractCommand   command;

    /**
     * Support of asynchronous execution.
     */
    final Thread            runner;

    /**
     * ConversationState for asynchronous execution.
     */
    final ConversationState conversation;

    CommandRunnable(AbstractCommand command, ConversationState conversation) throws CloudDriveException {
      this.conversation = conversation;
      this.command = command;
      this.runner = new Thread(this, title() + " connect "
          + DATE_FORMAT.format(Calendar.getInstance().getTime()));
    }

    /**
     * Start asynchronous execution.
     */
    void start() {
      runner.start();
    }

    @Override
    public void run() {
      // initialize this thread:

      // set correct user's ConversationState
      ConversationState.setCurrent(conversation);
      // set correct SessionProvider
      sessionProviders.setSessionProvider(null, new SessionProvider(conversation));

      try {
        command.exec();
      } catch (CloudDriveException e) {
        LOG.error(e.getMessage(), e);
      } catch (Throwable e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Basic command pattern.
   */
  protected abstract class AbstractCommand implements Command, CommandProgress {

    /**
     * Target JCR node.
     */
    protected final Node             driveRoot;

    /**
     * Local files affected by the command.
     */
    protected final Queue<CloudFile> result = new ConcurrentLinkedQueue<CloudFile>();

    /**
     * Progress indicator in percents.
     */
    protected volatile int           progressReported;

    /**
     * Time of command start.
     */
    protected volatile long          startTime;

    /**
     * Time of command finish.
     */
    protected volatile long          finishTime;

    /**
     * Asynchronous execution support.
     */
    protected CommandRunnable        async;

    /**
     * Base command constructor.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected AbstractCommand() throws RepositoryException, DriveRemovedException {
      this.driveRoot = rootNode();
    }

    /**
     * Command verb used for error messages.
     * 
     * @return {@link String}
     */
    protected abstract String getCommandVerb();

    /**
     * Processing logic.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    protected abstract void process() throws CloudDriveException, RepositoryException;

    /**
     * Start command execution. If command will fail due to provider error, the execution will be retried
     * {@link CloudDriveConnector#PROVIDER_REQUEST_ATTEMPTS} times before the throwing an exception.
     * 
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    void exec() throws CloudDriveException, RepositoryException {
      startTime = System.currentTimeMillis();

      try {
        startAction(JCRLocalCloudDrive.this);

        int attemptNumb = 1;
        while (true) {
          try {
            process();
            return;
          } catch (CloudProviderException e) {
            // in case of provider errors rollback and re-try an attempt
            if (getUser().getProvider().retryOnProviderError()) {
              attemptNumb++;
              if (attemptNumb > CloudDriveConnector.PROVIDER_REQUEST_ATTEMPTS) {
                handleError(driveRoot, e, getCommandVerb());
                throw e;
              } else {
                rollback(driveRoot);
                try {
                  Thread.sleep(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT);
                } catch (InterruptedException ie) {
                  LOG.warn("Interrupted while waiting for a next attempt of " + getCommandVerb() + ": "
                      + ie.getMessage());
                }
                LOG.warn("Error running " + getCommandVerb() + " command: " + e.getMessage()
                    + ". Rolled back and running next attempt.");
              }
            } else {
              handleError(driveRoot, e, getCommandVerb());
              throw e;
            }
          } catch (CloudDriveException e) {
            handleError(driveRoot, e, getCommandVerb());
            throw e;
          } catch (RepositoryException e) {
            handleError(driveRoot, e, getCommandVerb());
            throw e;
          } catch (RuntimeException e) {
            handleError(driveRoot, e, getCommandVerb());
            throw e;
          }
        }
      } finally {
        doneAction();
        finishTime = System.currentTimeMillis();
      }
    }

    /**
     * Start command execution asynchronously using {@link #exec()} method inside {@link CommandRunnable}. Any
     * exception will be stored to {@link CommandRunnable#error}.
     * 
     * @throws CloudDriveException if no ConversationState set in caller thread.
     */
    void execAsync() throws CloudDriveException {
      ConversationState conversation = ConversationState.getCurrent();
      if (conversation == null) {
        throw new CloudDriveException("Error to " + getCommandVerb() + " drive for user "
            + getUser().getEmail() + ". User identity not set.");
      }

      async = new CommandRunnable(this, conversation);
      async.start();
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
        if (current >= progressReported) {
          progressReported = current;
        } // else
          // progress cannot be smaller of already reported one
          // do nothing and wait for next portion of work done

        return progressReported;
      }
    }

    /**
     * @inherritDoc
     */
    @Override
    public boolean isDone() {
      return finishTime > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartTime() {
      return startTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFinishTime() {
      return finishTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CloudFile> getFiles() {
      return Collections.unmodifiableCollection(result);
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
    protected String getCommandVerb() {
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
     * Existing files being synchronized with cloud.
     */
    protected final Set<Node>         synced = new HashSet<Node>();

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
    protected String getCommandVerb() {
      return "synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException {
      // synchronize
      syncFiles();

      // and save the drive node
      driveRoot.save();

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
      readNodes(driveRoot, nodes);

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
        String mimeType = file.getNode("jcr:content").getProperty("jcr:mimeType").getString();
        isFolder = false;
        // cloudFile = drive.openFile(file.getName(), mimeType);
      } else if (file.isNodeType(NT_RESOURCE)) {
        // it's resource of new local JCR node - upload this nt:file to the cloud
        String mimeType = file.getProperty("jcr:mimeType").getString();
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
    protected String getCommandVerb() {
      return "file synchronization";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub

    }
  }

  // *********** variables ***********

  /**
   * Support for JCR actions. To do not fire on synchronization (our own modif) methods.
   */
  protected static final ThreadLocal<CloudDrive>   actionDrive = new ThreadLocal<CloudDrive>();

  protected final Transliterator                   accentsConverter;

  protected final String                           rootWorkspace;

  protected final ManageableRepository             repository;

  protected final SessionProviderService           sessionProviders;

  protected final CloudUser                        user;

  protected final String                           rootUUID;

  protected final ThreadLocal<SoftReference<Node>> rootNodeHolder;

  /**
   * Title has special care. It used in error logs and an attempt to read <code>exo:title</code> property can
   * cause another {@link RepositoryException}. Thus need it pre-cached in the variable and try to read the
   * <code>exo:title</code> property each time, but if not successful use this one cached.
   */
  private String                                   titleCached;

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
    addRemoveListener(driveNode);
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
      ConnectCommand connect = getConnectCommand();
      if (async) {
        connect.execAsync();
      } else {
        connect.exec();
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
          // TODO do we need this?
          // handleError(rootNode, e, "disconnect");
          throw e;
        } catch (RuntimeException e) {
          rollback(rootNode);
          // TODO do we need this?
          // handleError(rootNode, e, "disconnect");
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
      // fetch all files' matadata to local storage
      checkAccess();
      try {
        startAction(this);

        SyncCommand sync = getSyncCommand();
        if (async) {
          sync.execAsync();
        } else {
          sync.exec();
        }

        return sync;
      } finally {
        doneAction();
      }
    } else {
      throw new NotConnectedException("Cloud drive '" + title() + "' not connected.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Command synchronize() throws SyncNotSupportedException,
                                           CloudDriveException,
                                           RepositoryException {

    return synchronize(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Command synchronize(Node file) throws SyncNotSupportedException,
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
        sync.exec();
        return sync;
      } else {
        throw new SyncNotSupportedException("Synchronization not supported for not cloud drive file: "
            + file.getPath());
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
    rollback(rootNode);
    try {
      listeners.fireOnError(new CloudDriveEvent(getUser(), rootWorkspace, rootNode.getPath()), error);
    } catch (Throwable e) {
      LOG.warn("Error firing error '" + error.getMessage() + "' "
          + (commandDescription != null ? "of " + commandDescription + " command " : "")
          + "to listeners on Cloud Drive '" + title() + "':" + e.getMessage());
    }
  }

  /**
   * Does rollback of drive Node changes and fire onError event to listeners.
   * 
   * @param rootNode {@link Node}
   * @param error {@link Throwable}
   */
  void handleError(Node rootNode, Throwable error) {
    handleError(rootNode, error, null);
  }

  protected Node openFile(String fileId, String fileTitle, String fileType, Node parent) throws RepositoryException,
                                                                                        CloudDriveException {
    Node localNode;
    Node content;
    String parentPath = parent.getPath();
    String title = cleanName(fileTitle);
    String nodeName = title;

    int siblingNumber = 1;
    do {
      try {
        localNode = parent.getNode(nodeName);
        String localFileId = localNode.getProperty("ecd:id").getString();
        if (fileId.equals(localFileId)) {
          // the same file
          if (!localNode.isNodeType(ECD_CLOUDFILE)) {
            // oops, seems we cannot do anything - fail it up
            String existingPath;
            try {
              existingPath = parent.getNode(nodeName).getPath();
            } catch (RepositoryException e1) {
              LOG.error("Cannot read existing non cloud file node from the local storage of cloud drive"
                  + parentPath + ". Node name '" + nodeName + "'", e1);
              existingPath = "<" + e1.getMessage() + ">";
            }
            throw new CloudDriveException("Error cannecting cloud file '" + fileTitle
                + "'. Node with the same name exists and it is not a cloud drive file " + existingPath
                + ". If it's local user file - remove it and refresh the drive.");
          }
          break;
        } else {
          // find new name for the local file
          StringBuilder newName = new StringBuilder();
          newName.append(title);
          newName.append('-');
          newName.append(siblingNumber);
          nodeName = newName.toString();
          siblingNumber++;
        }
      } catch (PathNotFoundException e) {
        localNode = null;
        break;
      }
    } while (true);

    boolean newContent;
    if (localNode == null) {
      // no such node exists, add it
      localNode = parent.addNode(nodeName, NT_FILE);
      // file's content - nt:resource node
      content = localNode.addNode("jcr:content", NT_RESOURCE);
      newContent = true;
    } else {
      // file's content - nt:resource node
      try {
        content = localNode.getNode("jcr:content");
        newContent = false;
      } catch (PathNotFoundException e) {
        content = localNode.addNode("jcr:content", NT_RESOURCE);
        newContent = true;
      }
    }

    if (newContent) {
      setContent(content, fileType); // empty data by default
    }

    return localNode;
  }

  protected Node openFolder(String folderId, String folderTitle, Node parent) throws RepositoryException,
                                                                             CloudDriveException {
    // algorithms similar to openFile
    Node localNode;
    String parentPath = parent.getPath();
    String title = cleanName(folderTitle);
    String nodeName = title;

    int siblingNumber = 1;
    do {
      try {
        localNode = parent.getNode(nodeName);
        String localFolderId = localNode.getProperty("ecd:id").getString();
        if (folderId.equals(localFolderId)) {
          // the same folder
          if (!localNode.isNodeType(ECD_CLOUDFOLDER)) {
            // oops, seems we cannot do anything - fail it up
            String existingPath;
            try {
              existingPath = parent.getNode(nodeName).getPath();
            } catch (RepositoryException e1) {
              LOG.error("Cannot read existing non cloud folder node from the local storage of cloud drive"
                  + parentPath + ". Node name '" + nodeName + "'", e1);
              existingPath = "<" + e1.getMessage() + ">";
            }
            throw new CloudDriveException("Error cannecting cloud folder '" + folderTitle
                + "'. Node with the same name exists and it is not a cloud drive folder " + existingPath
                + ". If it's local user folder - remove it and refresh the drive.");
          }
          break;
        } else {
          // find new name for the local folder
          StringBuilder newName = new StringBuilder();
          newName.append(title);
          newName.append('-');
          newName.append(siblingNumber);
          nodeName = newName.toString();
          siblingNumber++;
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it
        localNode = parent.addNode(nodeName, NT_FOLDER);
        break;
      }
    } while (true);

    return localNode;
  }

  /**
   * Move node with its subtree in scope of existing JCR session.
   * 
   * @param node {@link Node}
   * @param destName {@link String} a new name of the Node
   * @param destParent {@link Node} a new parent
   * @return a {@link Node} from the destination
   * @throws RepositoryException
   */
  protected Node moveNode(Node node, String destName, Node destParent) throws RepositoryException {
    Session session = destParent.getSession();
    String nodeName = findNodeName(destName, destParent.getPath(), session);
    String destPath = destParent.getPath() + "/" + nodeName;
    session.move(node.getPath(), destPath);
    return node; // node will reflect a new destination
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
   * 
   * @param fileTitle {@link String}
   * @param parent {@link Node}
   * @return String with non existing name.
   * @throws RepositoryException
   */
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
   * Read all local nodes of the drive mapped by fileId.
   * 
   * @return Map with all nodes of the drive.
   * @throws RepositoryException if JCR error happen
   */
  protected void readNodes(Node parent, Map<String, List<Node>> nodes) throws RepositoryException {
    // TODO do we need caching of nodes?
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
        if (cn.isNodeType(ECD_CLOUDFOLDER)) {
          readNodes(cn, nodes);
        }
      } else {
        LOG.warn("Not a cloud file detected " + cn.getPath());
      }
    }
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
    // TODO if (mimetype == null && mimetype.indexOf("text") >= 0) {
    content.setProperty("jcr:data", DUMMY_DATA); // reset local content
  }

  /**
   * Clean up string for JCR compatible name.
   * 
   * @param String str
   * @return String - JCR compatible name of local file
   */
  protected String cleanName(String str) {
    // TODO cleanup
    str = accentsConverter.transliterate(str.trim());
    // LOG.info("str: " + str);
    // the character ? seems to not be changed to d by the transliterate
    // function
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
        // if (c == ' ') {
        // if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
        // cleanedStr.deleteCharAt(i--);
        // } else {
        // c = '-';
        // cleanedStr.setCharAt(i, c);
        // }
        // continue;
        // }
        // if (i > 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        if (c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
          cleanedStr.deleteCharAt(i);
          cleanedStr.insert(i, '_');
          continue;
        } else if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '.' || c == '-' || c == '_')) {
          cleanedStr.deleteCharAt(i--);
          continue;
        }
        // if (i > 0 && c == '-' && cleanedStr.charAt(i - 1) == '-')
        // cleanedStr.deleteCharAt(i--);
      }
    }
    // String clean = cleanedStr.toString().toLowerCase();
    // String clean = cleanedStr.toString();
    // if (clean.endsWith("-")) {
    // clean = clean.substring(0, clean.length() - 1);
    // }
    return cleanedStr.toString();
  }

  /**
   * Internal access to Cloud Drive title without throwing an Exception.
   * 
   * @return {@link String}
   */
  protected String title() {
    // XXX call to getTitle() can cause recursion if drive removed.
    // At the other hand the drive title can be changed only if local drive object will be
    // recreated, thus it is static for some instance of the drive.

    return titleCached;
  }

  /**
   * Add Observation listener to removal from parent and addition to the Trash.
   * 
   * @param driveNode {@link Node}
   * @throws RepositoryException
   */
  protected void addRemoveListener(Node driveNode) throws RepositoryException {
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
}
