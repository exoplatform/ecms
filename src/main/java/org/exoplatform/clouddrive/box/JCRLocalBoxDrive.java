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

import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxSharedLink;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.box.BoxAPI.ItemsIterator;
import org.exoplatform.clouddrive.box.BoxConnector.API;
import org.exoplatform.clouddrive.googledrive.GoogleDriveException;
import org.exoplatform.clouddrive.googledrive.GoogleUser;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.oauth2.UserTokenRefreshListener;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
   * Connect algorithm for Box drive.
   */
  protected class BoxConnect extends ConnectCommand {

    final BoxAPI              api;

    /**
     * Actually open child iterators. Used for progress indicator.
     */
    final List<ItemsIterator> iterators = new ArrayList<ItemsIterator>();

    BoxConnect() throws RepositoryException, DriveRemovedException {
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getComplete() {
      int complete = 0;
      for (ItemsIterator child : iterators) {
        complete += child.fetched;
      }
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAvailable() {
      int available = 0;
      for (ItemsIterator child : iterators) {
        available += child.available;
      }
      // return always +7,5% more, average time for JCR save on mid-to-big drive
      return Math.round(available * 1.075f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      BoxFolder root = fetchChilds(BoxAPI.BOX_ROOT_ID, driveRoot);
      // actual drive URL (its root folder's id), see initDrive() also
      driveRoot.setProperty("ecd:url", api.getLink(root));
    }

    protected BoxFolder fetchChilds(String fileId, Node parent) throws CloudDriveException,
                                                               RepositoryException {
      ItemsIterator items = api.getFolderItems(fileId);
      iterators.add(items);
      while (items.hasNext()) {
        BoxItem item = items.next();
        Calendar created = api.parseDate(item.getCreatedAt());
        Calendar modified = api.parseDate(item.getModifiedAt());
        boolean isFolder = item instanceof BoxFolder;
        String id = item.getId();
        String name = item.getName();
        String type, link, embedLink, downloadLink;
        Node localNode;
        if (isFolder) {
          // add folder
          BoxFolder f = (BoxFolder) item;
          type = f.getType();
          link = embedLink = downloadLink = api.getLink(f);
          localNode = openFolder(id, name, parent);
          initFolder(localNode, id, name, type, // type=folder
                     link, // gf.getAlternateLink(),
                     f.getCreatedBy().getLogin(), // gf.getOwnerNames().get(0),
                     f.getModifiedBy().getLogin(), // gf.getLastModifyingUserName(),
                     created,
                     modified);
          // go recursive
          fetchChilds(id, localNode);
        } else {
          // file
          BoxFile f = (BoxFile) item;
          type = mimeTypes.getMimeType(name);
          link = downloadLink = api.getLink(f);
          embedLink = api.getEmbedLink(f);
          localNode = openFile(id, name, type, parent);
          // TODO for thumbnail we can use Thumbnail service
          // https://api.box.com/2.0/files/FILE_ID/thumbnail.png?min_height=256&min_width=256
          initFile(localNode, id, name, type, // mimetype
                   link, // gf.getAlternateLink(),
                   embedLink, // gf.getEmbedLink(),
                   downloadLink, // gf.getThumbnailLink(), // XXX not used
                   f.getCreatedBy().getLogin(), // gf.getOwnerNames().get(0),
                   f.getModifiedBy().getLogin(), // gf.getLastModifyingUserName(),
                   created,
                   modified);
          initBoxItem(localNode, item);
        }

        // XXX thumbnail link not used
        changed.add(new JCRLocalCloudFile(localNode.getPath(),
                                         id,
                                         name,
                                         link,
                                         embedLink,
                                         downloadLink,
                                         type,
                                         item.getCreatedBy().getLogin(),
                                         item.getModifiedBy().getLogin(),
                                         created,
                                         modified,
                                         isFolder));
      }
      initBoxItem(parent, items.parent); // finally init parent
      return items.parent;
    }
  }

  /**
   * Sync algorithm for Box drive. With the Box API we don't have a diff/changes feature, thus we do
   * compare all remote files by its Etag and fetch an item if the etags differ. <br>
   * // TODO use Events service to get file related events http://developers.box.com/docs/#events
   */
  protected class BoxSync extends SyncCommand {

    /**
     * Box API.
     */
    final BoxAPI              api;

    /**
     * Actually open child iterators. Used for progress indicator.
     */
    final List<ItemsIterator> iterators = new ArrayList<ItemsIterator>();

    /**
     * Create command for Box synchronization.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected BoxSync() throws RepositoryException, DriveRemovedException {
      super();
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getComplete() {
      int complete = 0;
      for (ItemsIterator child : iterators) {
        complete += child.fetched;
      }
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAvailable() {
      int available = 0;
      for (ItemsIterator child : iterators) {
        available += child.available;
      }
      // return always +7,5% more, average time for JCR save on mid-to-big drive
      return Math.round(available * 1.075f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws RepositoryException, CloudDriveException {
      syncChilds(BoxAPI.BOX_ROOT_ID, driveRoot);
    }

    protected void syncChilds(String folderId, Node parent) throws RepositoryException, CloudDriveException {
      ItemsIterator items = api.getFolderItems(folderId);
      iterators.add(items);

      // local nodes of this folder, after this sync we'll remove what will lie in this map
      Map<String, List<Node>> nodes = new LinkedHashMap<String, List<Node>>();
      readNodes(parent, nodes, false);

      while (items.hasNext()) {
        BoxItem item = items.next();

        // remove from map of local to mark the item as existing
        nodes.remove(item.getId());

        boolean isFolder = item instanceof BoxFolder;
        Calendar created = api.parseDate(item.getCreatedAt());
        Calendar modified = api.parseDate(item.getModifiedAt());
        String id = item.getId();
        String name = item.getName();
        String type, link, embedLink, downloadLink;
        Node localNode;
        if (isFolder) {
          // sync folder
          BoxFolder f = (BoxFolder) item;
          type = f.getType();
          localNode = openFolder(id, name, parent);
          if (localNode.isNew() || !localNode.hasProperty("box:etag")
              || !localNode.getProperty("box:etag").getString().equals(f.getEtag())) {
            link = embedLink = downloadLink = api.getLink(f);
            initFolder(localNode, id, name, type, // type=folder
                       link, // gf.getAlternateLink(),
                       f.getCreatedBy().getLogin(), // gf.getOwnerNames().get(0),
                       f.getModifiedBy().getLogin(), // gf.getLastModifyingUserName(),
                       created,
                       modified);
            changed.add(new JCRLocalCloudFile(localNode.getPath(),
                                             id,
                                             name,
                                             link,
                                             embedLink,
                                             downloadLink,
                                             type,
                                             item.getCreatedBy().getLogin(),
                                             item.getModifiedBy().getLogin(),
                                             created,
                                             modified,
                                             isFolder));
          }
          // go recursive for all sub-folders
          syncChilds(id, localNode);
        } else {
          // sync file
          BoxFile f = (BoxFile) item;
          type = mimeTypes.getMimeType(name);
          localNode = openFile(id, name, type, parent);
          if (localNode.isNew() || !localNode.hasProperty("box:etag")
              || !localNode.getProperty("box:etag").getString().equals(f.getEtag())) {
            // TODO for thumbnail we can use Thumbnail service
            // https://api.box.com/2.0/files/FILE_ID/thumbnail.png?min_height=256&min_width=256
            link = downloadLink = api.getLink(f);
            embedLink = api.getEmbedLink(f);
            initFile(localNode, id, name, type, // mimetype
                     link, // gf.getAlternateLink(),
                     embedLink, // gf.getEmbedLink(),
                     downloadLink, // gf.getThumbnailLink(), // XXX not used
                     f.getCreatedBy().getLogin(), // gf.getOwnerNames().get(0),
                     f.getModifiedBy().getLogin(), // gf.getLastModifyingUserName(),
                     created,
                     modified);
            initBoxItem(localNode, item);
            changed.add(new JCRLocalCloudFile(localNode.getPath(),
                                             id,
                                             name,
                                             link,
                                             embedLink,
                                             downloadLink,
                                             type,
                                             item.getCreatedBy().getLogin(),
                                             item.getModifiedBy().getLogin(),
                                             created,
                                             modified,
                                             isFolder));
          }
        }
      }

      // remove local nodes of files not existing remotely
      for (Iterator<List<Node>> niter = nodes.values().iterator(); niter.hasNext();) {
        List<Node> nls = niter.next();
        niter.remove();
        for (Node n : nls) {
          removed.add(n.getPath());
          n.remove();
        }
      }

      initBoxItem(parent, items.parent); // init parent
    }
  }

  
  
  protected final MimeTypeResolver mimeTypes = new MimeTypeResolver();
  
 // protected 

  /**
   * @param user
   * @param driveNode
   * @param sessionProviders
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public JCRLocalBoxDrive(CloudUser user, Node driveNode, SessionProviderService sessionProviders) throws CloudDriveException,
      RepositoryException {
    // TODO user isn't transparently synced with persistent state of the drive
    super(user, driveNode, sessionProviders);
  }

  protected JCRLocalBoxDrive(API apiBuilder,
                             BoxProvider provider,
                             Node driveNode,
                             SessionProviderService sessionProviders) throws RepositoryException,
      GoogleDriveException,
      CloudDriveException {
    // TODO user isn't transparently synced with persistent state of the drive
    super(loadUser(apiBuilder, provider, driveNode), driveNode, sessionProviders);
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
  private static BoxUser loadUser(API apiBuilder, BoxProvider provider, Node driveNode) throws RepositoryException,
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
  public void onUserTokenRefresh(UserToken token) throws BoxException {
    try {
      Node driveNode = rootNode();
      try {
        driveNode.setProperty("box:oauth2AccessToken", token.getAccessToken());
        driveNode.setProperty("box:oauth2RefreshToken", token.getRefreshToken());
        driveNode.setProperty("box:oauth2TokenExpirationTime", token.getExpirationTime());

        driveNode.save();
      } catch (RepositoryException e) {
        rollback(driveNode);
        throw new BoxException("Error updating access key: " + e.getMessage(), e);
      }
    } catch (DriveRemovedException e) {
      throw new BoxException("Error openning drive node: " + e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new BoxException("Error reading drive node: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
    return new BoxConnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SyncCommand getSyncCommand() throws DriveRemovedException,
                                        SyncNotSupportedException,
                                        RepositoryException {
    return new BoxSync();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SyncFileCommand getSyncFileCommand(Node file) throws DriveRemovedException,
                                                         SyncNotSupportedException,
                                                         RepositoryException {
    // file not supported
    throw new SyncNotSupportedException("File synchronization not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isSyncSupported(CloudFile cloudFile) {
    return false;
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
  public String getChangesLink() throws DriveRemovedException, CloudProviderException, RepositoryException {
    return getUser().api().getChangesLink();
  }
  

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateChangesLink() throws DriveRemovedException, CloudProviderException, RepositoryException {
    getUser().api().updateChangesLink();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkAccess() throws CloudDriveException {
    // Not used. Box API does this internally and fires UserTokenRefreshListener.
    // See UserTokenRefreshListener implementation in this local drive.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateAccess(CloudUser newUser) throws CloudDriveException, RepositoryException {
    getUser().updateToken(((BoxUser) newUser).getToken());
  }

  /**
   * Initialize Box's common specifics of files and folders.
   * 
   * @param localNode {@link Node}
   * @param item {@link BoxItem}
   * @throws RepositoryException
   */
  protected void initBoxItem(Node localNode, BoxItem item) throws RepositoryException {
    // Box properties, if null, ones will be removed by JCR core

    // Etag used for synchronization
    localNode.setProperty("box:etag", item.getEtag());

    // File/folder size
    // TODO exo's property to show the size: jcr:content's length?
    localNode.setProperty("box:size", item.getSize());

    // properties below not actually used by the Cloud Drive,
    // they are just for information available to PLF user
    localNode.setProperty("box:ownedBy", item.getOwnedBy().getLogin());
    localNode.setProperty("box:sequenceId", item.getSequenceId());
    localNode.setProperty("box:description", item.getDescription());

    BoxSharedLink shared = item.getSharedLink();
    if (shared != null) {
      localNode.setProperty("box:sharedAccess", shared.getAccess());
      localNode.setProperty("box:sharedCanDownload", shared.getPermissions().isCan_download());
    }
  }

}
