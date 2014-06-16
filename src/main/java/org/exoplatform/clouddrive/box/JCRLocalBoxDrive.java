/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive.box;

import com.box.boxjavalibv2.dao.BoxEvent;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxSharedLink;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxEventRequestObject;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFileAPI;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.ConflictException;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.box.BoxAPI.EventsIterator;
import org.exoplatform.clouddrive.box.BoxAPI.ItemsIterator;
import org.exoplatform.clouddrive.box.BoxConnector.API;
import org.exoplatform.clouddrive.googledrive.GoogleDriveException;
import org.exoplatform.clouddrive.googledrive.GoogleUser;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.oauth2.UserTokenRefreshListener;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Local drive for Box.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRLocalBoxDrive.java 00000 Aug 30, 2013 pnedonosko $
 * 
 */
public class JCRLocalBoxDrive extends JCRLocalCloudDrive implements UserTokenRefreshListener {

  /**
   * Period to perform {@link FullSync} as a next sync request. See implementation of
   * {@link #getSyncCommand()}.
   */
  public static final long FULL_SYNC_PERIOD = 24 * 60 * 60 * 60 * 1000; // 24hrs

  /**
   * Connect algorithm for Box drive.
   */
  protected class Connect extends ConnectCommand {

    protected final BoxAPI api;

    protected Connect() throws RepositoryException, DriveRemovedException {
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      // call Events service before the actual fetch of Box files,
      // this will provide us a proper streamPosition to start sync from later
      EventsIterator eventsInit = api.getEvents(BoxEventRequestObject.STREAM_POSITION_NOW);

      BoxFolder boxRoot = fetchChilds(BoxAPI.BOX_ROOT_ID, rootNode);
      initBoxItem(rootNode, boxRoot); // init parent

      // actual drive URL (its root folder's id), see initDrive() also
      rootNode.setProperty("ecd:url", api.getLink(boxRoot));

      // sync stream
      // TODO run first sync here
      setChangeId(eventsInit.getNextStreamPosition());
      rootNode.setProperty("box:streamHistory", "");
    }

    protected BoxFolder fetchChilds(String fileId, Node parent) throws CloudDriveException,
                                                               RepositoryException {
      ItemsIterator items = api.getFolderItems(fileId);
      iterators.add(items);
      while (items.hasNext()) {
        BoxItem item = items.next();
        JCRLocalCloudFile localItem = updateItem(api, item, parent, null);
        if (localItem.isChanged()) {
          changed.add(localItem);
          if (localItem.isFolder()) {
            // go recursive to the folder
            fetchChilds(localItem.getId(), localItem.getNode());
          }
        } else {
          throw new BoxFormatException("Fetched item was not added to local drive storage");
        }
      }
      return items.parent;
    }
  }

  /**
   * Sync algorithm for Box drive based on all remote files traversing: we do
   * compare all remote files with locals by its Etag and fetch an item if the tags differ.
   */
  protected class FullSync extends SyncCommand {

    /**
     * Box API.
     */
    protected final BoxAPI api;

    /**
     * Create command for Box synchronization.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected FullSync() throws RepositoryException, DriveRemovedException {
      super();
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws RepositoryException, CloudDriveException {
      // real all local nodes of this drive
      readLocalNodes();

      // call Events service before the actual fetch of Box files,
      // this will provide us a proper streamPosition to start sync from later
      EventsIterator eventsInit = api.getEvents(BoxEventRequestObject.STREAM_POSITION_NOW);

      // sync with cloud
      BoxFolder boxRoot = syncChilds(BoxAPI.BOX_ROOT_ID, rootNode);
      initBoxItem(rootNode, boxRoot); // init parent

      // sync stream
      setChangeId(eventsInit.getNextStreamPosition());
      rootNode.setProperty("box:streamHistory", "");

      // remove local nodes of files not existing remotely, except of root
      nodes.remove(BoxAPI.BOX_ROOT_ID);
      for (Iterator<List<Node>> niter = nodes.values().iterator(); niter.hasNext()
          && !Thread.currentThread().isInterrupted();) {
        List<Node> nls = niter.next();
        niter.remove();
        for (Node n : nls) {
          String npath = n.getPath();
          if (notInRange(npath, removed)) {
            removed.add(npath);
            n.remove();
          }
        }
      }
    }

    protected BoxFolder syncChilds(String folderId, Node parent) throws RepositoryException,
                                                                CloudDriveException {
      ItemsIterator items = api.getFolderItems(folderId);
      iterators.add(items);
      while (items.hasNext() && !Thread.currentThread().isInterrupted()) {
        BoxItem item = items.next();

        // remove from map of local to mark the item as existing
        List<Node> existing = nodes.remove(item.getId());

        JCRLocalCloudFile localItem = updateItem(api, item, parent, null);
        if (localItem.isChanged()) {
          changed.add(localItem);

          // cleanup of this file located in another place (usecase of rename/move)
          // XXX this also assumes that Box doesn't support linking of files to other folders
          if (existing != null) {
            for (Iterator<Node> eiter = existing.iterator(); eiter.hasNext();) {
              Node enode = eiter.next();
              String path = localItem.getPath();
              String epath = enode.getPath();
              if (!epath.equals(path) && notInRange(epath, removed)) {
                removed.add(epath);
                enode.remove();
                eiter.remove();
              }
            }
          }
        }

        if (localItem.isFolder()) {
          // go recursive to the folder
          syncChilds(localItem.getId(), localItem.getNode());
        }
      }
      return items.parent;
    }

    /**
     * Execute full sync from current thread.
     */
    protected void execLocal() throws CloudDriveException, RepositoryException {
      // XXX we need this to be able run it from EventsSync.syncFiles()

      commandEnv.configure(this);

      super.exec();

      // at this point we know all changes already applied - we don't need history anymore
      fileHistory.clear();
      try {
        jcrListener.disable();
        String empty = "".intern();
        rootNode.setProperty("ecd:localHistory", empty);
        rootNode.setProperty("ecd:localChanges", empty);
        rootNode.save();
      } catch (Throwable e) {
        LOG.error("Error cleaning local history in " + title(), e);
      } finally {
        jcrListener.enable();
      }
    }
  }

  protected class FileAPI extends AbstractFileAPI {

    /**
     * Box service API.
     */
    protected final BoxAPI api;

    FileAPI() {
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createFile(Node fileNode,
                             Calendar created,
                             Calendar modified,
                             String mimeType,
                             InputStream content) throws CloudDriveException, RepositoryException {

      String parentId = getParentId(fileNode);
      String title = getTitle(fileNode);
      BoxFile file;
      try {
        file = api.createFile(parentId, title, created, content);
      } catch (ConflictException e) {
        // XXX we assume name as factor of equality here and make local file to reflect the cloud side
        BoxFile existing = null;
        ItemsIterator files = api.getFolderItems(parentId);
        while (files.hasNext()) {
          BoxItem item = files.next();
          if (item instanceof BoxFile && title.equals(item.getName())) {
            existing = (BoxFile) item;
            break;
          }
        }
        if (existing == null) {
          throw e; // we cannot do anything at this level
        } else {
          file = existing;
          // and erase local file data here
          // TODO check data checksum before erasing to ensure it is actually the same
          if (fileNode.hasNode("jcr:content")) {
            fileNode.getNode("jcr:content").setProperty("jcr:data", DUMMY_DATA); // empty data by default
          }
        }
      }

      String id = file.getId();
      String name = file.getName();
      String link = api.getLink(file);
      String embedLink = api.getEmbedLink(file);
      String createdBy = file.getCreatedBy().getLogin();
      String modifiedBy = file.getModifiedBy().getLogin();

      initFile(fileNode, id, name, findMimetype(name), link, embedLink, //
               link, // downloadLink
               createdBy, // author
               modifiedBy, // lastUser
               created,
               modified);
      initBoxItem(fileNode, file);

      return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createFolder(Node folderNode, Calendar created) throws CloudDriveException,
                                                                 RepositoryException {

      String parentId = getParentId(folderNode);
      String title = getTitle(folderNode);
      BoxFolder folder;
      try {
        folder = api.createFolder(getParentId(folderNode), getTitle(folderNode), created);
      } catch (ConflictException e) {
        // XXX we assume name as factor of equality here
        BoxFolder existing = null;
        ItemsIterator files = api.getFolderItems(parentId);
        while (files.hasNext()) {
          BoxItem item = files.next();
          if (item instanceof BoxFolder && title.equals(item.getName())) {
            existing = (BoxFolder) item;
            break;
          }
        }
        if (existing == null) {
          throw e; // we cannot do anything at this level
        } else {
          folder = existing;
        }
      }

      String id = folder.getId();
      String name = folder.getName();
      String type = folder.getType();
      String link = api.getLink(folder);
      String createdBy = folder.getCreatedBy().getLogin();
      String modifiedBy = folder.getModifiedBy().getLogin();

      initFolder(folderNode, id, name, type, //
                 link, // link
                 createdBy, // author
                 modifiedBy, // lastUser
                 created,
                 created); // created as modified here
      initBoxItem(folderNode, folder);
      return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException {
      // Update existing file metadata and parent (location).
      BoxFile file = api.updateFile(getParentId(fileNode), getId(fileNode), getTitle(fileNode), modified);
      if (file != null) {
        try {
          String id = file.getId();
          String name = file.getName();
          String link = api.getLink(file);
          String embedLink = api.getEmbedLink(file);
          String createdBy = file.getCreatedBy().getLogin();
          Calendar created = api.parseDate(file.getCreatedAt());
          modified = api.parseDate(file.getModifiedAt());
          String modifiedBy = file.getModifiedBy().getLogin();

          initFile(fileNode, id, name, findMimetype(name), link, embedLink, //
                   link, // downloadLink
                   createdBy, // author
                   modifiedBy, // lastUser
                   created,
                   modified);
          initBoxItem(fileNode, file);
        } catch (ParseException e) {
          throw new BoxFormatException("Error parsing date of file " + fileNode.getPath(), e);
        }
      } // else file wasn't changed actually
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFolder(Node folderNode, Calendar modified) throws CloudDriveException,
                                                                RepositoryException {

      // Update existing folder metadata and parent (location).
      BoxFolder folder = api.updateFolder(getParentId(folderNode),
                                          getId(folderNode),
                                          getTitle(folderNode),
                                          modified);
      if (folder != null) {
        try {
          String id = folder.getId();
          String name = folder.getName();
          String link = api.getLink(folder);
          String type = folder.getType();
          String createdBy = folder.getCreatedBy().getLogin();
          Calendar created = api.parseDate(folder.getCreatedAt());
          modified = api.parseDate(folder.getModifiedAt());
          String modifiedBy = folder.getModifiedBy().getLogin();

          initFolder(folderNode, id, name, type, //
                     link, // link
                     createdBy, // author
                     modifiedBy, // lastUser
                     created,
                     modified);
          initBoxItem(folderNode, folder);
        } catch (ParseException e) {
          throw new BoxFormatException("Error parsing date of folder " + folderNode.getPath(), e);
        }
      } // else folder wasn't changed actually
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFileContent(Node fileNode, Calendar modified, String mimeType, InputStream content) throws CloudDriveException,
                                                                                                         RepositoryException {
      // Update existing file content and its metadata.
      BoxFile file = api.updateFileContent(getParentId(fileNode),
                                           getId(fileNode),
                                           getTitle(fileNode),
                                           modified,
                                           content);
      try {
        String id = file.getId();
        String name = file.getName();
        String link = api.getLink(file);
        String embedLink = api.getEmbedLink(file);
        String createdBy = file.getCreatedBy().getLogin();
        Calendar created = api.parseDate(file.getCreatedAt());
        modified = api.parseDate(file.getModifiedAt());
        String modifiedBy = file.getModifiedBy().getLogin();

        initFile(fileNode, id, name, findMimetype(name), link, embedLink, //
                 link, // downloadLink
                 createdBy, // author
                 modifiedBy, // lastUser
                 created,
                 modified);
        initBoxItem(fileNode, file);
      } catch (ParseException e) {
        throw new BoxFormatException("Error parsing date of file " + fileNode.getPath(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException,
                                                               RepositoryException {
      BoxFile file = api.copyFile(getId(srcFileNode), getParentId(destFileNode), getTitle(destFileNode));
      try {
        String id = file.getId();
        String name = file.getName();
        String link = api.getLink(file);
        String embedLink = api.getEmbedLink(file);
        String createdBy = file.getCreatedBy().getLogin();
        String modifiedBy = file.getModifiedBy().getLogin();
        Calendar created = api.parseDate(file.getCreatedAt());
        Calendar modified = api.parseDate(file.getModifiedAt());

        initFile(destFileNode, id, name, findMimetype(name), link, embedLink, //
                 link, // downloadLink
                 createdBy, // author
                 modifiedBy, // lastUser
                 created,
                 modified);
        initBoxItem(destFileNode, file);
        return id;
      } catch (ParseException e) {
        throw new BoxFormatException("Error parsing date of file " + destFileNode.getPath(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException,
                                                                     RepositoryException {
      BoxFolder folder = api.copyFolder(getId(srcFolderNode),
                                        getParentId(destFolderNode),
                                        getTitle(destFolderNode));
      try {
        String id = folder.getId();
        String name = folder.getName();
        String type = folder.getType();
        String link = api.getLink(folder);
        String createdBy = folder.getCreatedBy().getLogin();
        String modifiedBy = folder.getModifiedBy().getLogin();
        Calendar created = api.parseDate(folder.getCreatedAt());
        Calendar modified = api.parseDate(folder.getModifiedAt());

        initFolder(destFolderNode, id, name, type, //
                   link, // link
                   createdBy, // author
                   modifiedBy, // lastUser
                   created,
                   modified);
        initBoxItem(destFolderNode, folder);
        return id;
      } catch (ParseException e) {
        throw new BoxFormatException("Error parsing date of folder " + destFolderNode.getPath(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFile(String id) throws CloudDriveException, RepositoryException {
      api.deleteFile(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFolder(String id) throws CloudDriveException, RepositoryException {
      api.deleteFolder(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trashFile(String id) throws CloudDriveException, RepositoryException {
      BoxFile trashed = api.trashFile(id);
      return trashed.getItemStatus().equals(BoxAPI.BOX_ITEM_STATE_TRASHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trashFolder(String id) throws CloudDriveException, RepositoryException {
      BoxFolder trashed = api.trashFolder(id);
      return trashed.getItemStatus().equals(BoxAPI.BOX_ITEM_STATE_TRASHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean untrashFile(Node fileNode) throws CloudDriveException, RepositoryException {
      BoxFile untrashed = api.untrashFile(fileAPI.getId(fileNode), fileAPI.getTitle(fileNode));
      return !untrashed.getItemStatus().equals(BoxAPI.BOX_ITEM_STATE_TRASHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean untrashFolder(Node folderNode) throws CloudDriveException, RepositoryException {
      BoxFolder untrashed = api.untrashFolder(fileAPI.getId(folderNode), fileAPI.getTitle(folderNode));
      return !untrashed.getItemStatus().equals(BoxAPI.BOX_ITEM_STATE_TRASHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrashSupported() {
      return true;
    }
  }

  /**
   * Sync algorithm for Box drive based on drive changes obtained from Events service
   * http://developers.box.com/docs/#events.
   * 
   */
  protected class EventsSync extends SyncCommand {

    /**
     * Box API.
     */
    protected final BoxAPI                         api;

    /**
     * Currently applied history of the drive storage.
     */
    protected final Set<String>                    history        = new LinkedHashSet<String>();

    /**
     * New history with currently applied event ids.
     */
    protected final Set<String>                    newHistory     = new LinkedHashSet<String>();

    /**
     * Queue of events postponed due to not existing parent or source.
     */
    protected final LinkedList<BoxEvent>           postponed      = new LinkedList<BoxEvent>();

    /**
     * Applied items in latest state mapped by item id.
     */
    protected final Map<String, JCRLocalCloudFile> applied        = new LinkedHashMap<String, JCRLocalCloudFile>();

    /**
     * Undeleted events by item id.
     */
    protected final Map<String, BoxItem>           undeleted      = new LinkedHashMap<String, BoxItem>();

    /**
     * Ids of removed items.
     */
    protected final Set<String>                    removedIds     = new LinkedHashSet<String>();

    /**
     * Events from Box to apply.
     */
    protected EventsIterator                       events;

    protected BoxEvent                             nextEvent;

    protected BoxEvent                             lastPostponed;

    protected int                                  prevPostponedNumber, postponedNumber;

    /**
     * Counter of applied events in this Sync. Used for multi-pass looping over the events.
     */
    protected int                                  appliedCounter = 0;

    /**
     * Counter of events read from Box service. Used for multi-pass looping over the events.
     */
    protected int                                  readCounter    = 0;

    /**
     * Create command for Box synchronization.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected EventsSync() throws RepositoryException, DriveRemovedException {
      super();
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException {
      long localStreamPosition = getChangeId();

      // buffer all items,
      // apply them in proper order (taking in account parent existence),
      // remove already applied (check by event id in history),
      // apply others to local nodes
      // save just applied events as history
      events = api.getEvents(localStreamPosition);
      iterators.add(events);

      // Local history, it contains something applied in previous sync, it can be empty if it was full sync.
      for (String es : rootNode.getProperty("box:streamHistory").getString().split(";")) {
        history.add(es);
      }

      // XXX Box API tells about Events service:
      // Events will occasionally arrive out of order. For example a file-upload might show up
      // before the Folder-create event. You may need to buffer events and apply them in a logical
      // order.
      while (hasNextEvent()) {
        BoxEvent event = nextEvent();
        BoxTypedObject source = event.getSource();
        if (source instanceof BoxItem) {
          BoxItem item = (BoxItem) source;
          String eventType = event.getEventType();

          String id = item.getId();
          String name = item.getName();
          String sequenceId = item.getSequenceId();

          if (LOG.isDebugEnabled()) {
            LOG.debug("> " + eventType + ": " + id + " " + name + " " + sequenceId);
          }

          // find parent id
          String parentId;
          if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_TRASH)) {
            if (hasRemoved(id)) {
              // Handle removed locally and returned from cloud side
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Returned file removal " + id + " " + name);
              }
              cleanRemoved(id);
              continue; // this item was removed locally
            }

            BoxFolder itemParent = item.getParent();
            if (itemParent != null) {
              parentId = itemParent.getId();
            } else {
              // it is a child file of already deleted/trashed folder, need wait for a parent removal,
              remove(id, null);
              continue;
            }
          } else {
            BoxFolder itemParent = item.getParent();
            if (itemParent != null) {
              parentId = itemParent.getId();
            } else {
              // this user cannot access the parent of this item, as we already skipped trashed above,
              // it is something what we don't expect here, postpone and continue the cycle: finally it will
              // run full sync if no parent will be found.
              postpone(event);
              continue;
            }
          }

          // find parent node
          Node parent;
          if (BoxAPI.BOX_ROOT_ID.equals(parentId)) {
            parent = rootNode;
          } else {
            JCRLocalCloudFile local = applied(parentId);
            if (local != null) {
              parent = local.getNode();
              // XXX workaround bug in JCR, otherwise it may load previously deleted node from persistence
              // what will lead to NPE later
              parent.getParent().getNodes();
            } else {
              parent = findNode(parentId); // can be null
              if (parent == null) {
                // parent not (yet) found or was removed, we postpone the event and wait for it in the order
                // and fail at the end if will be not applied.
                // XXX special logic for childs removal of already removed parent, JCR removes all together
                // with the parent - we skip such events.
                if (!(isRemoved(parentId) && eventType.equals(BoxEvent.EVENT_TYPE_ITEM_TRASH))) {
                  postpone(event);
                } // else parent node not found and it is already removed: we skip it.
                continue;
              }
            }
          }

          try {
            if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_CREATE)
                || eventType.equals(BoxEvent.EVENT_TYPE_ITEM_UPLOAD)) {
              if (hasUpdated(id)) {
                // this item was created/modified locally
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> Returned file creation/modification " + id + " " + name);
                }
                cleanUpdated(id);
              } else {
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> File create/modify " + id + " " + name);
                }
                apply(updateItem(api, item, parent, null));
              }
            } else if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_MOVE)
                || eventType.equals(BoxEvent.EVENT_TYPE_ITEM_RENAME)) {
              if (hasUpdated(id)) {
                // this item was moved/renamed locally
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> Returned file move/rename " + id + " " + name);
                }
                cleanUpdated(id);
              } else {
                BoxItem undelete = undeleted(id);
                if (undelete != null) {
                  // apply undelete here if ITEM_MOVE appeared after ITEM_UNDELETE_VIA_TRASH,
                  // it's not JCR item move actually - we just add a new node using name from this event
                  if (LOG.isDebugEnabled()) {
                    LOG.debug(">> File undeleted " + id + " " + name);
                  }
                  apply(updateItem(api, item, parent, null));
                } else {
                  // move node
                  Node sourceNode;
                  JCRLocalCloudFile local = applied(id);
                  if (local != null) {
                    // using transient change from this events order for this item,
                    sourceNode = local.getNode();
                  } else {
                    // try search in persisted storage by file id
                    sourceNode = findNode(id);
                  }
                  if (sourceNode != null) {
                    if (fileAPI.getTitle(sourceNode).equals(name)
                        && fileAPI.getParentId(sourceNode).equals(parentId)) {
                      // file node already has required name and parent
                      // apply(updateItem(api, item, parent, sourceNode)); // no need to update it
                    } else {
                      if (LOG.isDebugEnabled()) {
                        LOG.debug(">> File move/rename " + id + " " + name);
                      }
                      // XXX workaround bug in JCR (see also above in this method), otherwise it may load
                      // previously moved node from persistence what will lead to PathNotFoundException (on
                      // move/rename) in moveFile()
                      parent.getNodes();

                      Node destNode = moveFile(id, name, sourceNode, parent);
                      apply(updateItem(api, item, parent, destNode));
                    }
                  } else {
                    // else, wait for appearance of source node in following events,
                    // here we also covering ITEM_MOVE as part of undeleted item events if the move
                    // appeared first.
                    postpone(event);
                  }
                }
              }
            } else if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_TRASH)) {
              // FYI removed locally checked above in this iteration
              Node node = readNode(parent, name, id);
              if (node != null) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> File removal " + id + " " + name);
                }
                String path = node.getPath();
                node.remove();
                remove(id, path);
              } else {
                // wait for a target node appearance in following events
                postpone(event);
              }
            } else if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_UNDELETE_VIA_TRASH)) {
              if (hasUpdated(id)) {
                // this item was untrashed locally
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> Returned file untrash " + id + " " + name);
                }
                cleanUpdated(id);
              } else {
                // undeleted folder will appear with its files, but in undefined order!
                // for undeleted item we also will have ITEM_MOVE to a final restored destination
                // here we already have a parent node, but ensure we have a "place" for undeleted item
                Node place = readNode(parent, name, id);
                if (place == null) {
                  if (LOG.isDebugEnabled()) {
                    LOG.debug(">> File untrash " + id + " " + name);
                  }
                  apply(updateItem(api, item, parent, null));
                } else if (fileAPI.getTitle(place).equals(name)
                    && fileAPI.getParentId(place).equals(parentId)) {
                  // this file already exists in the drive, may be it is a child of untrashed
                } else {
                  // another item already there, wait for ITEM_MOVE with actual "place" for the item
                  // XXX ITEM_MOVE not observed in Box's move changes at Apr 27 2014
                  undelete(item);
                  postpone(event);
                }
              }
            } else if (eventType.equals(BoxEvent.EVENT_TYPE_ITEM_COPY)) {
              if (hasUpdated(id)) {
                // this item was copied locally
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> Returned file copy " + id + " " + name);
                }
                cleanUpdated(id);
              } else {
                if (LOG.isDebugEnabled()) {
                  LOG.debug(">> File copy " + id + " " + name);
                }
                JCRLocalCloudFile local = applied(id);
                if (local != null) {
                  // using transient change from this events order for this item
                  Node sourceNode = local.getNode();
                  if (fileAPI.getTitle(sourceNode).equals(name)
                      && fileAPI.getParentId(sourceNode).equals(parentId)) {
                    // file node already has required name and parent, may be it is a child of copied
                  } else {
                    Node destNode = copyNode(sourceNode, parent);
                    apply(updateItem(api, item, parent, destNode));
                  }
                } else {
                  // read the only copied node from the Box
                  local = updateItem(api, item, parent, null);
                  if (local.isFolder()) {
                    // and fetch child files
                    try {
                      fetchChilds(local.getId(), local.getNode());
                      apply(local);
                    } catch (NotFoundException e) {
                      // this node not found...
                      local.getNode().remove();
                      remove(id, local.getPath());
                      if (LOG.isDebugEnabled()) {
                        LOG.debug("Copied node already removed in cloud - remove it locally. "
                            + e.getMessage());
                      }
                    }
                  } else {
                    apply(local);
                  }
                }
              }
            } else {
              LOG.warn("Skipped unexpected change from Box Event: " + eventType);
            }
          } catch (PathNotFoundException e) {
            // here is a possible inconsistency between events flow and actual state due to previous JCR
            // errors (e.g. unexpectedly stopped storage), thus we break, postpone and let full sync fix the
            // drive
            postpone(event);
            break;
          }
        } else {
          LOG.warn("Skipping non Item in Box events: " + source);
        }
      }

      if (hasPostponed()) {
        // EventsSync cannot solve all changes, need run FullSync
        LOG.warn("Not all events applied for Box sync. Running full sync.");

        // rollback everything from this sync
        rollback(rootNode);

        // we need full sync in this case
        FullSync fullSync = new FullSync();
        fullSync.execLocal();

        changed.clear();
        changed.addAll(fullSync.getFiles());
        removed.clear();
        removed.addAll(fullSync.getRemoved());
      } else {
        // save history
        // TODO consider for saving the history of several hours or even a day
        StringBuilder newHistoryData = new StringBuilder();
        for (Iterator<String> eriter = newHistory.iterator(); eriter.hasNext();) {
          newHistoryData.append(eriter.next());
          if (eriter.hasNext()) {
            newHistoryData.append(';');
          }
        }
        rootNode.setProperty("box:streamHistory", newHistoryData.toString());

        // update sync position
        setChangeId(events.getNextStreamPosition());
      }
    }

    protected BoxFolder fetchChilds(String fileId, Node parent) throws CloudDriveException,
                                                               RepositoryException {
      ItemsIterator items = api.getFolderItems(fileId);
      iterators.add(items);
      while (items.hasNext()) {
        BoxItem item = items.next();
        JCRLocalCloudFile localItem = updateItem(api, item, parent, null);
        if (localItem.isChanged()) {
          apply(localItem);
          if (localItem.isFolder()) {
            // go recursive to the folder
            fetchChilds(localItem.getId(), localItem.getNode());
          }
        }
      }
      return items.parent;
    }

    protected BoxEvent readEvent() throws CloudDriveException {
      while (events.hasNext()) {
        BoxEvent next = events.next();

        // keep in new history all we get from the Box API, it can be received in next sync also
        newHistory.add(next.getId());

        if (!history.contains(next.getId())) {
          readCounter++;
          return next;
        }
      }
      return null;
    }

    protected boolean hasNextEvent() throws CloudDriveException {
      // condition of next: if Box events not yet full read or we have postponed and their number decreases
      // from cycle to cycle over the whole queue.
      if (nextEvent != null) {
        return true;
      }
      nextEvent = readEvent();
      if (nextEvent != null) {
        return true;
      }
      return postponed.size() > 0 && (lastPostponed != null ? prevPostponedNumber > postponedNumber : true);
    }

    protected BoxEvent nextEvent() throws NoSuchElementException, CloudDriveException {
      BoxEvent event = null;
      if (nextEvent != null) {
        event = nextEvent;
        nextEvent = null;
      } else {
        event = readEvent();
      }

      if (event != null) {
        return event;
      }

      if (postponed.size() > 0) {
        if (lastPostponed == null) {
          lastPostponed = postponed.getLast(); // init marker of postponed queue end
          postponedNumber = readCounter - appliedCounter; // init number of postponed
          prevPostponedNumber = Integer.MAX_VALUE; // need this for hasNextEvent() logic
        }

        BoxEvent firstPostponed = postponed.poll();
        // we store number of postponed on each iteration over the postponed queue, if number doesn't go down
        // then we cannot apply other postponed we have and need run FullSync.
        if (firstPostponed == lastPostponed) {
          prevPostponedNumber = postponedNumber;
          postponedNumber = readCounter - appliedCounter; // next number of postponed
        }
        return firstPostponed;
      }

      throw new NoSuchElementException("No more events.");
    }

    protected void postpone(BoxEvent event) {
      postponed.add(event);
    }

    protected boolean hasPostponed() {
      if (postponed.size() > 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Not resolved Box events >>>> ");
          for (BoxEvent e : postponed) {
            BoxTypedObject source = e.getSource();
            if (source instanceof BoxItem) {
              BoxItem item = (BoxItem) source;
              LOG.info(e.getEventType() + ": " + item.getId() + " " + item.getName() + " "
                  + item.getSequenceId());
            }
          }
          LOG.debug("<<<<");
        }
      }
      return postponed.size() > 0;
    }

    protected BoxItem undeleted(String itemId) {
      return undeleted.get(itemId);
    }

    protected void undelete(BoxItem item) {
      undeleted.put(item.getId(), item);
    }

    protected void apply(JCRLocalCloudFile local) {
      if (local.isChanged()) {
        applied.put(local.getId(), local);
        removed.remove(local.getPath());
        removedIds.remove(local.getId());
        changed.add(local);
        appliedCounter++;
      }
    }

    protected JCRLocalCloudFile applied(String itemId) {
      return applied.get(itemId);
    }

    protected JCRLocalCloudFile remove(String itemId, String itemPath) {
      appliedCounter++;
      if (itemPath != null) {
        removed.add(itemPath);
      }
      removedIds.add(itemId);
      return applied.remove(itemId);
    }

    @Deprecated
    protected boolean isRemovedPath(String path) {
      for (String rpath : removed) {
        if (path.startsWith(rpath)) {
          return true;
        }
      }
      return false;
    }

    protected boolean isRemoved(String itemId) {
      return removedIds.contains(itemId);
    }
  }

  protected final MimeTypeResolver mimeTypes = new MimeTypeResolver();

  /**
   * @param user
   * @param driveNode
   * @param sessionProviders
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public JCRLocalBoxDrive(BoxUser user,
                          Node driveNode,
                          SessionProviderService sessionProviders,
                          NodeFinder finder) throws CloudDriveException, RepositoryException {
    super(user, driveNode, sessionProviders, finder);
    getUser().api().getToken().addListener(this);
  }

  protected JCRLocalBoxDrive(API apiBuilder,
                             BoxProvider provider,
                             Node driveNode,
                             SessionProviderService sessionProviders,
                             NodeFinder finder) throws RepositoryException,
      GoogleDriveException,
      CloudDriveException {
    super(loadUser(apiBuilder, provider, driveNode), driveNode, sessionProviders, finder);
    getUser().api().getToken().addListener(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    super.initDrive(driveNode);

    driveNode.setProperty("ecd:id", BoxAPI.BOX_ROOT_ID);
    // XXX dummy URL here, an actual one will be set during files fetching in BoxConnect
    driveNode.setProperty("ecd:url", "https://box.com/");
  }

  /**
   * Load user from the drive Node.
   * 
   * @param apiBuilder {@link API} API builder
   * @param provider {@link BoxProvider}
   * @param driveNode {@link Node} root of the drive
   * @return {@link GoogleUser}
   * @throws RepositoryException
   * @throws BoxException
   * @throws CloudDriveException
   */
  protected static BoxUser loadUser(API apiBuilder, BoxProvider provider, Node driveNode) throws RepositoryException,
                                                                                         BoxException,
                                                                                         CloudDriveException {
    String username = driveNode.getProperty("ecd:cloudUserName").getString();
    String email = driveNode.getProperty("ecd:userEmail").getString();
    String userId = driveNode.getProperty("ecd:cloudUserId").getString();

    String accessToken = driveNode.getProperty("box:oauth2AccessToken").getString();
    String refreshToken;
    try {
      refreshToken = driveNode.getProperty("box:oauth2RefreshToken").getString();
    } catch (PathNotFoundException e) {
      refreshToken = null;
    }
    long expirationTime = driveNode.getProperty("box:oauth2TokenExpirationTime").getLong();

    BoxAPI driveAPI = apiBuilder.load(refreshToken, accessToken, expirationTime).build();

    return new BoxUser(userId, username, email, provider, driveAPI);
  }

  /**
   * {@inheritDoc}
   * 
   * @throws BoxException
   */
  @Override
  public void onUserTokenRefresh(UserToken token) throws CloudDriveException {
    try {
      jcrListener.disable();
      Node driveNode = rootNode();
      try {
        driveNode.setProperty("box:oauth2AccessToken", token.getAccessToken());
        driveNode.setProperty("box:oauth2RefreshToken", token.getRefreshToken());
        driveNode.setProperty("box:oauth2TokenExpirationTime", token.getExpirationTime());

        driveNode.save();
      } catch (RepositoryException e) {
        rollback(driveNode);
        throw new CloudDriveException("Error updating access key: " + e.getMessage(), e);
      }
    } catch (DriveRemovedException e) {
      throw new CloudDriveException("Error openning drive node: " + e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new CloudDriveException("Error reading drive node: " + e.getMessage(), e);
    } finally {
      jcrListener.enable();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
    return new Connect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SyncCommand getSyncCommand() throws DriveRemovedException,
                                        SyncNotSupportedException,
                                        RepositoryException {

    Calendar now = Calendar.getInstance();
    Calendar last = rootNode().getProperty("box:streamDate").getDate();

    // XXX we force a full sync (a whole drive traversing) each defined period.
    // We do this for a case when Box will not provide a full history for files connected long time ago and
    // weren't synced day by day (Box drive was rarely used).
    // Their doc tells: Box does not store all events for all time on your account. We store somewhere between
    // 2 weeks and 2 months of events.
    if (now.getTimeInMillis() - last.getTimeInMillis() < FULL_SYNC_PERIOD) {
      return new EventsSync();
    } else {
      return new FullSync();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudFileAPI createFileAPI() throws DriveRemovedException,
                                        SyncNotSupportedException,
                                        RepositoryException {
    return new FileAPI();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long readChangeId() throws RepositoryException, CloudDriveException {
    try {
      return rootNode().getProperty("box:streamPosition").getLong();
    } catch (PathNotFoundException e) {
      throw new CloudDriveException("Change id not found for the drive " + title());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveChangeId(Long id) throws CloudDriveException, RepositoryException {
    Node driveNode = rootNode();
    // will be saved in a single save of the drive command (sync)
    driveNode.setProperty("box:streamPosition", id);
    driveNode.setProperty("box:streamDate", Calendar.getInstance());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BoxUser getUser() {
    return (BoxUser) user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getChangesLink() throws DriveRemovedException,
                                CloudProviderException,
                                RepositoryException,
                                RefreshAccessException {
    return getUser().api().getChangesLink();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateChangesLink() throws DriveRemovedException,
                                 CloudProviderException,
                                 RepositoryException,
                                 RefreshAccessException {
    getUser().api().updateChangesLink();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void refreshAccess() throws CloudDriveException {
    // Not used. Box API does this internally and fires UserTokenRefreshListener.
    // See UserTokenRefreshListener implementation in this local drive.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateAccess(CloudUser newUser) throws CloudDriveException, RepositoryException {
    getUser().api().updateToken(((BoxUser) newUser).api().getToken());
  }

  /**
   * Initialize Box's common specifics of files and folders.
   * 
   * @param localNode {@link Node}
   * @param item {@link BoxItem}
   * @throws RepositoryException
   * @throws BoxException
   */
  protected void initBoxItem(Node localNode, BoxItem item) throws RepositoryException, BoxException {
    // Box properties, if null, ones will be removed by JCR core

    // Etag and sequence_id used for synchronization
    localNode.setProperty("box:etag", item.getEtag());
    try {
      String sequenceIdStr = item.getSequenceId();
      if (sequenceIdStr != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(">>> initBoxItem: " + localNode.getPath() + " = " + item.getId() + " " + item.getName()
              + " " + item.getSequenceId());
        }
        localNode.setProperty("box:sequenceId", Long.parseLong(sequenceIdStr));
      } // else, it's null (root or trash)
    } catch (NumberFormatException e) {
      throw new BoxException("Error parsing sequence_id of " + localNode.getPath(), e);
    }

    // File/folder size
    // TODO exo's property to show the size: jcr:content's length?
    localNode.setProperty("box:size", item.getSize());

    // properties below not actually used by the Cloud Drive,
    // they are just for information available to PLF user
    localNode.setProperty("box:ownedBy", item.getOwnedBy().getLogin());
    localNode.setProperty("box:description", item.getDescription());

    BoxSharedLink shared = item.getSharedLink();
    if (shared != null) {
      localNode.setProperty("box:sharedAccess", shared.getAccess());
      localNode.setProperty("box:sharedCanDownload", shared.getPermissions().isCan_download());
    }
  }

  protected String findMimetype(String fileName) {
    String name = fileName.toUpperCase().toLowerCase();
    String ext = name.substring(name.lastIndexOf(".") + 1);
    if (ext.equals(BoxAPI.BOX_WEBDOCUMENT_EXT)) {
      return BoxAPI.BOX_WEBDOCUMENT_MIMETYPE;
    } else {
      return mimeTypes.getMimeType(fileName);
    }
  }

  /**
   * Update or create a local node of Cloud File. If the node is <code>null</code> then it will be open on the
   * given parent and created if not already exists.
   * 
   * @param api {@link BoxAPI}
   * @param item {@link BoxItem}
   * @param parent {@link Node}
   * @param node {@link Node}, can be <code>null</code>
   * @return {@link JCRLocalCloudFile}
   * @throws RepositoryException for storage errors
   * @throws CloudDriveException for drive or format errors
   */
  protected JCRLocalCloudFile updateItem(BoxAPI api, BoxItem item, Node parent, Node node) throws RepositoryException,
                                                                                          CloudDriveException {
    try {
      String id = item.getId();
      String name = item.getName();
      boolean isFolder = item instanceof BoxFolder;
      String type = isFolder ? item.getType() : findMimetype(name);

      long sequenceId = getSequenceId(item);

      // read/create local node if not given
      if (node == null) {
        if (isFolder) {
          node = openFolder(id, name, parent);
        } else {
          node = openFile(id, name, type, parent);
        }
      }

      boolean changed = node.isNew()
          || (sequenceId >= 0 && node.getProperty("box:sequenceId").getLong() < sequenceId)
          || !node.getProperty("box:etag").getString().equals(item.getEtag());

      Calendar created = api.parseDate(item.getCreatedAt());
      Calendar modified = api.parseDate(item.getModifiedAt());
      String createdBy = item.getCreatedBy().getLogin();
      String modifiedBy = item.getModifiedBy().getLogin();

      String link, embedLink, downloadLink;
      if (isFolder) {
        link = embedLink = downloadLink = api.getLink(item);
        if (changed) {
          initFolder(node, id, name, type, // type=folder
                     link, // gf.getAlternateLink(),
                     createdBy, // gf.getOwnerNames().get(0),
                     modifiedBy, // gf.getLastModifyingUserName(),
                     created,
                     modified);
          initBoxItem(node, item);
        }
      } else {
        // TODO for thumbnail we can use Thumbnail service
        // https://api.box.com/2.0/files/FILE_ID/thumbnail.png?min_height=256&min_width=256
        link = downloadLink = api.getLink(item);
        embedLink = api.getEmbedLink(item);
        if (changed) {
          initFile(node, id, name, type, // mimetype
                   link, // gf.getAlternateLink(),
                   embedLink, // gf.getEmbedLink(),
                   downloadLink, // gf.getThumbnailLink(),
                   createdBy, // gf.getOwnerNames().get(0),
                   modifiedBy, // gf.getLastModifyingUserName(),
                   created,
                   modified);
          initBoxItem(node, item);
        }
      }
      return new JCRLocalCloudFile(node.getPath(),
                                   id,
                                   name,
                                   link,
                                   embedLink,
                                   downloadLink,
                                   type,
                                   createdBy,
                                   modifiedBy,
                                   created,
                                   modified,
                                   isFolder,
                                   node,
                                   changed);
    } catch (ParseException e) {
      throw new BoxFormatException("Error parsing date of " + parent.getPath() + "/" + item.getName(), e);
    }
  }

  protected boolean notInRange(String path, Collection<String> range) {
    for (String p : range) {
      if (path.startsWith(p)) {
        return false;
      }
    }
    return true;
  }

  protected Long getSequenceId(BoxItem item) throws BoxFormatException {
    // TODO do we need use Etag in conjunction with sequence_id? they mean almost the same in Box API.
    try {
      String sequenceIdStr = item.getSequenceId();
      if (sequenceIdStr != null) {
        return Long.parseLong(sequenceIdStr);
      } else {
        return -1l; // for null (root or trash)
      }
    } catch (NumberFormatException e) {
      throw new BoxFormatException("Error parsing sequence_id of " + item.getId() + " " + item.getName(), e);
    }
  }
}
