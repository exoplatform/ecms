package org.exoplatform.clouddrive.onedrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.graph.http.GraphError;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.FileSystemInfo;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.SharingLink;
import com.microsoft.graph.models.extensions.Subscription;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.SkipSyncException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.oauth2.UserTokenRefreshListener;
import org.exoplatform.clouddrive.onedrive.OneDriveAPI.HashSetCompatibleDriveItem;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JCRLocalOneDrive extends JCRLocalCloudDrive implements UserTokenRefreshListener {
  
  class DriveItemInfo {
    private String link;
  
    private String previewLink;
  
    private String lastModifiedUserName;
  
    private String createdUserName;
  
    public DriveItemInfo(String link, String previewLink, String lastModifiedUserName, String createdUserName) {
      this.link = link;
      this.previewLink = previewLink;
      this.lastModifiedUserName = lastModifiedUserName;
      this.createdUserName = createdUserName;
    }
  
    public String getLink() {
      return link;
    }
  
    public void setLink(String link) {
      this.link = link;
    }
  
    public String getPreviewLink() {
      return previewLink;
    }
  
    public void setPreviewLink(String previewLink) {
      this.previewLink = previewLink;
    }
  
    public String getLastModifiedUserName() {
      return lastModifiedUserName;
    }
  
    public void setLastModifiedUserName(String lastModifiedUserName) {
      this.lastModifiedUserName = lastModifiedUserName;
    }
  
    public String getCreatedUserName() {
      return createdUserName;
    }
  
    public void setCreatedUserName(String createdUserName) {
      this.createdUserName = createdUserName;
    }
  }

  public class OneDriveState implements FilesState {
    
    final Subscription   subscription;

    private final String rootId;

    public OneDriveState(Subscription subscription, String rootId) {
      this.subscription = subscription;
      this.rootId = rootId;
    }

    @Override
    public Collection<String> getUpdating() {
      return state.getUpdating();
    }

    @Override
    public boolean isUpdating(String fileIdOrPath) {
      return state.isUpdating(fileIdOrPath);
    }

    @Override
    public boolean isNew(String fileIdOrPath) {
      return state.isNew(fileIdOrPath);

    }

    public String getUrl() {
      return subscription.notificationUrl;
    }

    public long getExpirationDateTime() {
      return this.subscription.expirationDateTime.getTimeInMillis();
    }

    public Subscription getSubscription() {
      return null;
    }

    public String getCreatorId() {
      return rootId;
    }
  }

  private static final Log LOG = ExoLogger.getLogger(JCRLocalOneDrive.class);

  protected JCRLocalOneDrive(CloudUser user,
                             Node driveNode,
                             SessionProviderService sessionProviders,
                             NodeFinder finder,
                             ExtendedMimeTypeResolver mimeTypes)
      throws CloudDriveException,
      RepositoryException {
    super(user, driveNode, sessionProviders, finder, mimeTypes);
    getUser().api().getStoredToken().addListener(this);
  }

  protected JCRLocalOneDrive(OneDriveConnector.API apiBuilder,
                             OneDriveProvider provider,
                             Node driveNode,
                             SessionProviderService sessionProviders,
                             NodeFinder finder,
                             ExtendedMimeTypeResolver mimeTypes)
      throws RepositoryException,
      CloudDriveException,
      IOException {
    super(loadUser(apiBuilder, provider, driveNode), driveNode, sessionProviders, finder, mimeTypes);
    getUser().api().getStoredToken().addListener(this);
  }

  protected static OneDriveUser loadUser(OneDriveConnector.API apiBuilder,
                                         OneDriveProvider provider,
                                         Node driveNode) throws RepositoryException, CloudDriveException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> loadUser from {}", driveNode.getPath());
    }
    
    String username = driveNode.getProperty("ecd:cloudUserName").getString();
    String email = driveNode.getProperty("ecd:userEmail").getString();
    String userId = driveNode.getProperty("ecd:cloudUserId").getString();
    String accessToken = driveNode.getProperty("onedrive:oauth2AccessToken").getString();
    String refreshToken;
    try {
      refreshToken = driveNode.getProperty("onedrive:oauth2RefreshToken").getString();
    } catch (PathNotFoundException e) {
      LOG.warn("Refresh token not found for {}[{}]", userId, email);
      refreshToken = null;
    }
    long expirationTime = driveNode.getProperty("onedrive:oauth2TokenExpirationTime").getLong();
    if (LOG.isDebugEnabled()) {
      LOG.debug("<< loadUser from {}, refreshToken={}", driveNode.getPath(), refreshToken);
    }
    OneDriveAPI driveAPI = apiBuilder.load(refreshToken, accessToken, expirationTime).build();
    return new OneDriveUser(userId, username, email, provider, driveAPI);
  }

  @Override
  protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
    return new OneDriveConnectCommand();
  }

  @Override
  protected SyncCommand getSyncCommand() {
    return new OneDriveSyncCommand();
  }

  @Override
  public OneDriveState getState() throws DriveRemovedException,
                                  RefreshAccessException,
                                  CloudProviderException,
                                  RepositoryException {
    return new OneDriveState(getUser().api().getSubscription(), getUser().api().getRootId());
  }

  @Override
  protected OneDriveFileAPI createFileAPI() {
    return new OneDriveFileAPI();
  }

  @Override
  protected Long readChangeId() {
    return System.currentTimeMillis();
  }

  @Override
  protected void saveChangeId(Long id) {
  }

  @Override
  public OneDriveUser getUser() {
    return (OneDriveUser) this.user;
  }

  @Override
  protected void refreshAccess() throws CloudDriveException {

  }

  @Override
  protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    super.initDrive(driveNode);
    driveNode.setProperty("ecd:id", getUser().api().getRootId());
  }

  @Override
  protected void updateAccess(CloudUser newUser) throws CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> updateAccess for {}[{}]", newUser.getId(), newUser.getUsername());
    }
    getUser().api().updateToken(((OneDriveUser) newUser).api().getStoredToken());
  }

  @Override
  public void onUserTokenRefresh(UserToken token) throws CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> onUserTokenRefresh {}", token.getAccessToken());
    }
    try {
      jcrListener.disable();
      Node driveNode = rootNode();
      try {
        driveNode.setProperty("onedrive:oauth2AccessToken", token.getAccessToken());
        driveNode.setProperty("onedrive:oauth2RefreshToken", token.getRefreshToken());
        driveNode.setProperty("onedrive:oauth2TokenExpirationTime", token.getExpirationTime());
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
  public void onUserTokenRemove() throws CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> onUserTokenRemove");
    }
    try {
      jcrListener.disable();
      Node driveNode = rootNode();
      try {
        if (driveNode.hasProperty("onedrive:oauth2AccessToken")) {
          driveNode.getProperty("onedrive:oauth2AccessToken").remove();
        }
        if (driveNode.hasProperty("onedrive:oauth2RefreshToken")) {
          driveNode.getProperty("onedrive:oauth2RefreshToken").remove();
        }
        if (driveNode.hasProperty("onedrive:oauth2TokenExpirationTime")) {
          driveNode.getProperty("onedrive:oauth2TokenExpirationTime").remove();
        }

        driveNode.save();
      } catch (RepositoryException e) {
        rollback(driveNode);
        throw new CloudDriveException("Error removing access key: " + e.getMessage(), e);
      }
    } catch (DriveRemovedException e) {
      throw new CloudDriveException("Error openning drive node: " + e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new CloudDriveException("Error reading drive node: " + e.getMessage(), e);
    } finally {
      jcrListener.enable();
    }
  }

  private void initFolderByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
    String lastModifiedUserName = "";
    String createdUserName = "";
    if (item.lastModifiedBy != null && item.lastModifiedBy.user != null) {
      lastModifiedUserName = item.lastModifiedBy.user.displayName;
    }
    if (item.createdBy != null && item.createdBy.user != null) {
      createdUserName = item.createdBy.user.displayName;
    }

    initFolder(fileNode,
               item.id,
               item.name,
               "folder",
               item.webUrl,
               createdUserName,
               lastModifiedUserName,
               item.createdDateTime,
               item.lastModifiedDateTime);
  }

  private void initWebUrlForImage(SharingLink link) {
    String base64Url = Base64.getEncoder().encodeToString(link.webUrl.getBytes(StandardCharsets.UTF_8));
    // TODO can we use something from JVM?
    String preparedBase64Url = "u!" + StringUtils.stripEnd(base64Url, "=").replace("/", "_").replace("+", "-");
    link.webUrl = "https://api.onedrive.com/v1.0/shares/" + preparedBase64Url + "/root/content";
  }

  private SharingLink createViewLink(DriveItem item) throws OneDriveException {
    return getUser().api().createLink(item.id, "view");
  }

  private SharingLink createEmbedLink(DriveItem item) throws OneDriveException {
    SharingLink link = getUser().api().createLink(item.id, "embed");
    if (item.file != null && item.file.mimeType.startsWith("image")) {
      initWebUrlForImage(link);
    }
    return link;
  }

  private String              accountType;          // business or personal

  private static final String PERSONAL = "personal";

  private static final String BUSINESS = "business";

  synchronized SharingLink createLink(DriveItem item) throws OneDriveException {
    // TODO there is a possibility to delete/update public links,
    // and also use temporary links
    if (BUSINESS.equals(accountType)) {
      return getUser().api().createLink(item.id, "view");
    } else if (PERSONAL.equals(accountType)) {
      return createEmbedLink(item);
    }
    try {
      this.accountType = PERSONAL;
      return createEmbedLink(item);
    } catch (GraphServiceException ex) {
      GraphError graphError = ex.getServiceError();
      if (graphError != null && StringUtils.containsIgnoreCase(graphError.message, "Link type must be either")) {
        this.accountType = BUSINESS;
        return createViewLink(item);
      }
      throw ex;
    }
  }

  /**
   * Creates additional fields, because different types of account can be used
   */
  private DriveItemInfo prepareAdditionalDriveItemFields(DriveItem item) throws OneDriveException {
    String link = "";
    String previewLink = "";
    String lastModifiedUserName = "";
    String createdUserName = "";
    if (item.lastModifiedBy != null && item.lastModifiedBy.user != null) {
      lastModifiedUserName = item.lastModifiedBy.user.displayName;
    }
    if (item.createdBy != null && item.createdBy.user != null) {
      createdUserName = item.createdBy.user.displayName;
    }
    SharingLink sharingLink = createLink(item);
    if (sharingLink.type.equalsIgnoreCase("embed")) { // personal account
      // TODO cleanup
      // link = item.webUrl;
      // previewLink = sharingLink.webUrl;
      link = "personal=" + sharingLink.webUrl;
      previewLink = item.webUrl;
    } else if (sharingLink.type.equalsIgnoreCase("view")) { // business account
      link = "business=" + sharingLink.webUrl;
    }
    return new DriveItemInfo(link, previewLink, lastModifiedUserName, createdUserName);
  }

  JCRLocalCloudFile initCreateFile(Node fileNode, DriveItem item) throws OneDriveException, RepositoryException {
    DriveItemInfo driveItemAdditionalInfo = prepareAdditionalDriveItemFields(item);
    initFileByDriveItem(fileNode, item, driveItemAdditionalInfo);
    return createCloudFile(fileNode, item, driveItemAdditionalInfo);
  }

  void initFileByDriveItem(Node fileNode,
                           DriveItem item,
                           DriveItemInfo driveItemAdditionalInfo) throws RepositoryException, OneDriveException {
    // TODO cleanup
    /*
     * if (item.lastModifiedBy != null && item.lastModifiedBy.user != null) { lastModifiedUserName =
     * item.lastModifiedBy.user.displayName; } if (item.createdBy != null && item.createdBy.user != null) { createdUserName =
     * item.createdBy.user.displayName; } SharingLink sharingLink = createLink(item); if
     * (sharingLink.type.equalsIgnoreCase("embed")) { // personal account link = item.webUrl; previewLink = sharingLink.webUrl; }
     * else if (sharingLink.type.equalsIgnoreCase("view")) { // business account link = sharingLink.webUrl; }
     */
    initFile(fileNode,
             item.id,
             item.name,
             item.file.mimeType,
             driveItemAdditionalInfo.getLink(),
             driveItemAdditionalInfo.getPreviewLink(),
             null, // TODO thumbnail link: may be something better can be here?
             driveItemAdditionalInfo.getCreatedUserName(),
             driveItemAdditionalInfo.getLastModifiedUserName(),
             item.createdDateTime,
             item.lastModifiedDateTime,
             item.size);
  }

  private JCRLocalCloudFile createCloudFolder(Node fileNode, DriveItem item) throws RepositoryException {
    String lastModifiedUserName = "";
    String createdUserName = "";
    if (item.lastModifiedBy != null && item.lastModifiedBy.user != null) {
      lastModifiedUserName = item.lastModifiedBy.user.displayName;
    }
    if (item.createdBy != null && item.createdBy.user != null) {
      createdUserName = item.createdBy.user.displayName;
    }

    return new JCRLocalCloudFile(fileNode.getPath(),
                                 item.id,
                                 item.name,
                                 item.webUrl,
                                 "folder",
                                 lastModifiedUserName,
                                 createdUserName,
                                 item.createdDateTime,
                                 item.lastModifiedDateTime,
                                 fileNode,
                                 true);
  }

  private JCRLocalCloudFile createCloudFile(Node fileNode,
                                            DriveItem item,
                                            DriveItemInfo driveItemAdditionalInfo) throws RepositoryException {
    return new JCRLocalCloudFile(fileNode.getPath(),
                                 item.id,
                                 item.name,
                                 driveItemAdditionalInfo.getLink(),
                                 driveItemAdditionalInfo.getPreviewLink(),
                                 null, // TODO thumbnail link 
                                 item.file.mimeType,
                                 null, // TODO type mode
                                 driveItemAdditionalInfo.getLastModifiedUserName(),
                                 driveItemAdditionalInfo.getCreatedUserName(),
                                 item.createdDateTime,
                                 item.lastModifiedDateTime,
                                 item.size,
                                 fileNode,
                                 true);
  }

  class OneDriveConnectCommand extends ConnectCommand {

    private final OneDriveAPI api;

    OneDriveConnectCommand() throws RepositoryException, DriveRemovedException {
      this.api = getUser().api();
    }

    private JCRLocalCloudFile openInitFolder(Map<String, Set<HashSetCompatibleDriveItem>> itemChildren,
                                             DriveItem item,
                                             Node localFile) throws RepositoryException, CloudDriveException {
      Node fileNode = openFolder(item.id, item.name, localFile);
      initFolderByDriveItem(fileNode, item);
      fetchFiles(itemChildren, item.id, fileNode);
      return createCloudFolder(fileNode, item);
    }

    private JCRLocalCloudFile openInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
      Node fileNode = openFile(item.id, item.name, localFile);
      return initCreateFile(fileNode, item);
    }

    private void fetchFiles(Map<String, Set<HashSetCompatibleDriveItem>> itemChildren,
                            String fileId,
                            Node localFile) throws CloudDriveException, RepositoryException {
      final Set<HashSetCompatibleDriveItem> children = itemChildren.get(fileId);
      if (children != null) {
        OneDriveAPI.SimpleChildIterator childIterator = api.getSimpleChildIterator(children);
        iterators.add(childIterator);
        while (childIterator.hasNext()) {
          DriveItem item = childIterator.next().getItem();
          if (!isConnected(fileId, item.id)) {
            JCRLocalCloudFile localCloudFile;
            if (item.folder != null) {
              localCloudFile = openInitFolder(itemChildren, item, localFile);
            } else {
              localCloudFile = openInitFile(item, localFile);
            }
            addConnected(fileId, localCloudFile);
          }
        }
      }
    }

    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      String rootId = api.getRootId();
      final OneDriveAPI.DeltaDriveFiles allFiles = api.getAllFiles();
      final List<DriveItem> items = allFiles.getItems();

      // parentId -> children
      Map<String, Set<HashSetCompatibleDriveItem>> itemChildren = new HashMap<>();

      items.forEach((item) -> {
        String parentId = item.parentReference.id;
        Set<HashSetCompatibleDriveItem> children = itemChildren.computeIfAbsent(parentId, k -> new HashSet<>());
        final HashSetCompatibleDriveItem hashSetCompatibleDriveItem = new HashSetCompatibleDriveItem(item);
        children.remove(hashSetCompatibleDriveItem);
        children.add(hashSetCompatibleDriveItem);
      });

      fetchFiles(itemChildren, rootId, driveNode);

      driveNode.setProperty("onedrive:changeToken", allFiles.getDeltaToken());
    }
  }

  protected class OneDriveSyncCommand extends SyncCommand {

    private final OneDriveAPI           api;

    private OneDriveAPI.ChangesIterator changes;

    OneDriveSyncCommand() {
      this.api = getUser().api();
    }

    void saveDeltaToken(String deltaToken) throws RepositoryException {
      driveNode.setProperty("onedrive:changeToken", deltaToken);
    }

    String getDeltaToken() throws RepositoryException {
      if (driveNode.hasProperty("onedrive:changeToken")) {
        return driveNode.getProperty("onedrive:changeToken").getString();
      }
      return null;
    }

    @Override
    protected void preSaveChunk() {
    }

    private void addFileNode(DriveItem driveItem, Node parentNode) throws RepositoryException, CloudDriveException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Add File(): id:  " + driveItem.id + " name: " + driveItem.name + "parentId: " + driveItem.parentReference.id);
      }
      Node fileNode = openFile(driveItem.id, driveItem.name, parentNode);

      JCRLocalCloudFile jcrLocalCloudFile = initCreateFile(fileNode, driveItem);
      // TODO make better list creation, w/o
      // "The serializable class does not declare a static final serialVersionUID"
      this.nodes.put(driveItem.id, new ArrayList<Node>() {
        {
          add(fileNode);
        }
      });
      addChanged(jcrLocalCloudFile);
    }

    private void fetchChilds(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
      OneDriveAPI.ChildIterator childIterator = api.getChildIterator(fileId);
      while (childIterator.hasNext()) {
        DriveItem item = childIterator.next();
        if (item.folder != null) {
          Node folderNode = addFolderNode(item, localFile);
          fetchChilds(item.id, folderNode);
        } else if (item.file != null) {
          addFileNode(item, localFile);
        }
      }
    }

    private void updateNode(DriveItem driveItem, Node fileNode) throws RepositoryException, CloudDriveException {
      List<Node> destParentNodes = this.nodes.get(driveItem.parentReference.id);
      if (api.getRootId().equals(driveItem.id))
        return;
      if (destParentNodes == null || destParentNodes.isEmpty()) {
        syncNext();
        destParentNodes = this.nodes.get(driveItem.parentReference.id);
      }
      Node destParentNode = destParentNodes.get(0);
      if (!fileAPI.getParentId(fileNode).equals(driveItem.parentReference.id)
          || !fileAPI.getTitle(fileNode).equals(driveItem.name)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Must be moved, name={}", driveItem.name);
        }
        try {
          Node node = moveFile(driveItem.id, driveItem.name, fileNode, destParentNode);
          JCRLocalCloudFile localCloudFile;
          if (node != null) {
            if (driveItem.folder != null) { // folder
              initFolderByDriveItem(node, driveItem);
              localCloudFile = createCloudFolder(node, driveItem);
            } else { // file
              // initFileByDriveItem(fileNode, driveItem);
              // jcrLocalCloudFile = createCloudFile(node, driveItem);
              localCloudFile = initCreateFile(node, driveItem);
            }
            addChanged(localCloudFile);
          }
        } catch (Throwable ex) {
          // TODO catch Throwable? Too rough, we have to know better what may come here
          // TODO Should we warn/error to the log?
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> Try move node after exception {}", ex.getMessage());
          }
          deleteItem(driveItem.id);
          DriveItem item = api.getItem(driveItem.id);
          if (item.file != null) { // file
            if (LOG.isDebugEnabled()) {
              LOG.debug("try move file");
            }
            addFileNode(driveItem, destParentNode);
          } else {// folder
            if (LOG.isDebugEnabled()) {
              LOG.debug("try move folder");
            }
            Node folderNode = addFolderNode(driveItem, destParentNode);
            fetchChilds(item.id, folderNode);
          }
        }
      } else if (!fileAPI.getTitle(fileNode).equals(driveItem.name)) {
        // TODO Nothing for this case?
        // if (LOG.isDebugEnabled()) {
        // LOG.debug("must be renamed, name= " + driveItem.name);
        // }
        // try {
        // Node node = moveFile(driveItem.id, driveItem.name, fileNode,
        // destParentNode);
        // JCRLocalCloudFile jcrLocalCloudFile;
        // if (node != null) {
        // if (driveItem.folder != null) { // folder
        // initFolderByDriveItem(node, driveItem);
        // jcrLocalCloudFile = createCloudFolder(node, driveItem);
        // } else { // file
        // initFileByDriveItem(fileNode, driveItem);
        // jcrLocalCloudFile = createCloudFile(node, driveItem);
        // }
        // addChanged(jcrLocalCloudFile);
        // }
        // } catch (Throwable ex) {
        // if (LOG.isDebugEnabled()) {
        // LOG.debug("try rename node after exception");
        // }
        // deleteItem(driveItem.id);
        // DriveItem item = api.getItem(driveItem.id);
        // if (item.file != null) { // file
        // if (LOG.isDebugEnabled()) {
        // LOG.debug("try move file");
        // }
        // addFileNode(driveItem, destParentNode);
        // } else {// folder
        // if (LOG.isDebugEnabled()) {
        // LOG.debug("try move folder");
        // }
        // Node folderNode = addFolderNode(driveItem, destParentNode);
        // fetchChilds(item.id, folderNode);
        // }
        // }
      }
    }

    public void syncNext() throws CloudDriveException, RepositoryException {
      while (changes.hasNext()) {
        DriveItem driveItem = changes.next();
        if (driveItem.file != null) {
          if (driveItem.deleted == null) {
            List<Node> nodes = null;
            if (this.nodes.containsKey(driveItem.id)) {
              nodes = this.nodes.get(driveItem.id);
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Nodes size: {} for item with id {}", nodes.size(), driveItem.id);
              }
            }

            if (nodes == null || nodes.size() == 0) { // add
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Add file id: {}, name: {}, parentId: {}",
                          driveItem.id,
                          driveItem.name,
                          driveItem.parentReference.id);
              }
              Node parentNode = this.nodes.get(driveItem.parentReference.id).get(0);
              addFileNode(driveItem, parentNode);
            } else { // update
              Node fileNode = nodes.get(0);
              updateNode(driveItem, fileNode);
            }
          } else {
            deleteItem(driveItem.id);
          }
        } else { // folder
          if (driveItem.deleted == null) {
            List<Node> nodes = null;
            if (this.nodes.containsKey(driveItem.id)) {
              nodes = this.nodes.get(driveItem.id);
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Folder nodes size: {} for item with id {}", nodes.size(), driveItem.id);
              }
            }
            if (nodes == null || nodes.size() == 0) { // add
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Add folder id: {}, name: {}, parentId: {}",
                          driveItem.id,
                          driveItem.name,
                          driveItem.parentReference.id);
              }
              Node parentNode = this.nodes.get(driveItem.parentReference.id).get(0);
              addFolderNode(driveItem, parentNode);
            } else {
              Node fileNode = nodes.get(0);
              updateNode(driveItem, fileNode);
            }
          } else {
            deleteItem(driveItem.id);
          }
        }
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

    private void sync(int numOfAttemptsInCaseOfFailure) throws RepositoryException, CloudDriveException {
      String deltaToken = getDeltaToken();
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Sync files with deltatoken {}", deltaToken);
      }
      // TODO need this?
      // if (deltaToken == null) {
      // deltaToken = "latest";
      // }
      changes = api.changes(deltaToken);
      iterators.add(changes);
      if (changes.hasNext()) {
        try {
          readLocalNodes();
          syncNext();
          saveDeltaToken(changes.getDeltaToken());
        } catch (Throwable ex) {
          // TODO catch Throwable? Too rough, we have to know better what may come here
          // TODO Should we warn/error to the log?
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> Try update all drive after exception {}", ex.getMessage());
          }
          // remove all nodes
          nodes.remove(api.getRootId());
          for (Iterator<List<Node>> niter = nodes.values().iterator(); niter.hasNext()
              && !Thread.currentThread().isInterrupted();) {
            List<Node> nls = niter.next();
            niter.remove();
            for (Node n : nls) {
              String npath = n.getPath();
              if (notInRange(npath, getRemoved())) {
                // remove file links outside the drive, then the node itself
                removeNode(n);
                addRemoved(npath);
              }
            }
          }

          saveDeltaToken("ALL");
          if (numOfAttemptsInCaseOfFailure > 0) {
            sync(--numOfAttemptsInCaseOfFailure);
          }
        }
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Save delta token if no changes");
        }
        saveDeltaToken(changes.getDeltaToken());
      }
    }

    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException {
      sync(3);
    }

    private Node addFolderNode(DriveItem driveItem, Node parentNode) throws CloudDriveException, RepositoryException {
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Add folder id: {}, name: {}, parentId: {}",
                  driveItem.id,
                  driveItem.name,
                  driveItem.parentReference.id);
      }
      Node folderNode = openFolder(driveItem.id, driveItem.name, parentNode);
      initFolderByDriveItem(folderNode, driveItem);
      // TODO array list
      this.nodes.put(driveItem.id, new ArrayList<Node>() {
        {
          add(folderNode);
        }
      });
      JCRLocalCloudFile jcrLocalCloudFile = createCloudFolder(folderNode, driveItem);
      addChanged(jcrLocalCloudFile);
      return folderNode;
    }

    private void deleteItem(String itemId) throws CloudDriveException, RepositoryException {
      List<Node> existing = nodes.remove(itemId);
      if (existing != null) {
        for (Node en : existing) {
          removeLocalNode(en);
        }
      }
    }
  }

  class OneDriveFileAPI extends AbstractFileAPI {

    private OneDriveAPI api;

    OneDriveFileAPI() {
      this.api = getUser().api();
    }

    @Override
    public boolean removeFile(String id) {
      try {
        api.removeFile(id);
      } catch (Throwable ex) {
        // TODO what kind of ex here? Catch exact classes
        LOG.warn("Error removing remote file {}", id, ex);
        // TODO return false? or catch it higher?
      }
      return true;
    }

    @Override
    public boolean removeFolder(String id) {
      try {
        api.removeFolder(id);
      } catch (Throwable ex) {
        // TODO what kind of ex here? Catch exact classes
        LOG.warn("Error removing remote folder {}", id, ex);
        // TODO return false? or catch it higher?
      }
      return true;
    }

    @Override
    public boolean isTrashSupported() {
      return false;
    }

    @Override
    public boolean trashFile(String id) throws CloudDriveException {
      throw new SyncNotSupportedException("Trash not supported");
    }

    @Override
    public boolean trashFolder(String id) throws CloudDriveException {
      throw new SyncNotSupportedException("Trash not supported");
    }

    @Override
    public CloudFile untrashFile(Node fileNode) throws CloudDriveException {
      throw new SyncNotSupportedException("Trash not supported");
    }

    @Override
    public CloudFile untrashFolder(Node fileNode) throws CloudDriveException {
      throw new SyncNotSupportedException("Trash not supported");
    }

    @Deprecated // TODO cleanup
    private String extractAppropriateOneDrivePath(Node fileNode) throws RepositoryException {
      StringBuilder oneDrivePath = new StringBuilder();
      while (fileNode != null && fileNode.hasProperty("exo:title")) {
        oneDrivePath.insert(0, "/" + getTitle(fileNode));
        fileNode = fileNode.getParent();
      }

      String path = oneDrivePath.substring(oneDrivePath.indexOf("/", oneDrivePath.indexOf("/") + 1) + 1);
      if (LOG.isDebugEnabled()) {
        LOG.debug("OneDrivePath: " + path);
      }
      return path;
    }

    @Override
    public CloudFile createFile(Node fileNode,
                                Calendar created,
                                Calendar modified,
                                String mimeType,
                                InputStream content) throws RepositoryException, CloudDriveException {
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Create file name: {} path: {}", getTitle(fileNode), fileNode.getPath());
      }
      DriveItem createdDriveItem = null;
      try {
        createdDriveItem = api.insert(getParentId(fileNode), getTitle(fileNode), created, modified, content, "rename");
      } catch (Throwable ex) {
        // TODO what kind of ex here? Catch exact classes
        throw new OneDriveException("Error occured while upload file", ex);
      }
      try {
        return initCreateFile(fileNode, createdDriveItem);
      } catch (Throwable e) {
        // TODO what kind of ex here? Catch exact classes
        // TODO what we solve by this? Further changes remotely will go to connected drive and fail
        LOG.error("Error creating file {}", fileNode, e);
        fileNode.remove();
        throw new SkipSyncException("Error occurred storing data locally while loading a file");
      }
    }

    @Override
    public CloudFile createFolder(Node folderNode, Calendar created) throws RepositoryException {
      String parentId = getParentId(folderNode);
      DriveItem createdFolder = null;
      createdFolder = api.createFolder(parentId, getTitle(folderNode), created);

      initFolderByDriveItem(folderNode, createdFolder);
      return createCloudFolder(folderNode, createdFolder);
    }

    @Override
    public CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException {
      try {
        DriveItem file = api.copyFile(getParentId(destFileNode), getTitle(destFileNode), getId(srcFileNode));
        // initFileByDriveItem(destFileNode, file);
        // return createCloudFolder(destFileNode, file);
        return initCreateFile(destFileNode, file);
      } catch (Throwable e) {
        // TODO what kind of ex here? Catch exact classes
        // TODO what we solve by this? Further changes remotely will go to connected drive and fail 
        LOG.error("Error copying file {} to {}", srcFileNode, destFileNode, e);
        throw new SkipSyncException("Error during copy file");
      }
    }

    private void fetchChilds(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
      OneDriveAPI.ChildIterator childIterator = api.getChildIterator(fileId);
      while (childIterator.hasNext()) {
        DriveItem item = childIterator.next();
        if (item.folder != null) {
          openInitFolder(item, localFile);
        } else if (item.file != null) {
          openInitFile(item, localFile);
        }
      }
    }

    private JCRLocalCloudFile openInitFolder(DriveItem item, Node localFile) throws RepositoryException, CloudDriveException {
      Node fileNode = openFolder(item.id, item.name, localFile);
      initFolderByDriveItem(fileNode, item);
      fetchChilds(item.id, fileNode);
      return createCloudFolder(fileNode, item);
    }

    private JCRLocalCloudFile openInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
      Node fileNode = openFile(item.id, item.name, localFile);
      // initFileByDriveItem(fileNode, item);
      // return createCloudFile(fileNode, item);
      return initCreateFile(fileNode, item);
    }

    @Override
    public CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws RepositoryException, CloudDriveException {
      try {
        DriveItem folder = api.copyFolder(getParentId(destFolderNode), getTitle(destFolderNode), getId(srcFolderNode));
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Copy to folder {}. Trying to delete local subtree...", folder.name);
        }

        // TODO Is it a good idea to delete everything on the destination here, shouldn't it be already empty? 
        NodeIterator nodeIterator = destFolderNode.getNodes();
        while (nodeIterator.hasNext()) {
          Node n = nodeIterator.nextNode();
          removeNode(n);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Local subtree deleted for {}", folder.name);
        }

        initFolderByDriveItem(destFolderNode, folder);
        fetchChilds(folder.id, destFolderNode);
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Local subtree fetched for {}", folder.name);
        }

        return createCloudFolder(destFolderNode, folder);
      } catch (Throwable e) {
        // TODO what kind of ex here? Catch exact classes
        // TODO what we solve by this? Further changes remotely will go to connected drive and fail 
        LOG.error("Error copying folder {} to {}", srcFolderNode, destFolderNode, e);
        throw new SkipSyncException("unable to copy folder");
      }
    }

    // TODO cleanup
    // private void initSubtree(Node folderNode, DriveItem driveItem) throws
    // RepositoryException {
    // initFolderByDriveItem(folderNode, driveItem);
    //
    // for (NodeIterator niter = folderNode.getNodes(); niter.hasNext(); ) {
    // Node node = niter.nextNode();
    // String onedrivePath = extractAppropriateOneDrivePath(node);
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("initsubtree() onedrivepath = " + onedrivePath);
    // }
    // driveItem = api.getItemByPath(onedrivePath);
    // if (isFolder(node)) { //folder
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("copyFolder(): initFolderByDriveITem");
    // }
    // initSubtree(node, driveItem);
    // } else { //file
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("copyFolder(): initFolderByDriveITem");
    // }
    // initFileByDriveItem(node, driveItem);
    // }
    //
    // }
    // }
    
    @Override
    public CloudFile updateFile(Node fileNode, Calendar modified) throws RepositoryException, SkipSyncException {
      try {
        DriveItem modifiedItem = updateItem(fileNode, modified);
        // initFileByDriveItem(fileNode, modifiedItem);
        // return createCloudFile(fileNode, modifiedItem);
        return initCreateFile(fileNode, modifiedItem);
      } catch (Throwable e) {
        LOG.error("Error updating file {}", fileNode, e);
        
        throw new SkipSyncException("error during updateFile");
      }
    }

    @Override
    public CloudFile updateFolder(Node folderNode, Calendar modified) throws RepositoryException, SkipSyncException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("updateFolder(): ");
      }
      try {
        DriveItem modifiedItem = updateItem(folderNode, modified);
        initFolderByDriveItem(folderNode, modifiedItem);
        return createCloudFolder(folderNode, modifiedItem);
      } catch (Throwable e) {
        LOG.error("failed to update folder", e);
        // TODO what we solve by this? Further changes remotely will go to connected drive and fail
        throw new SkipSyncException("failed to update folder");
      }
    }

    @Override
    public CloudFile updateFileContent(Node fileNode,
                                       Calendar modified,
                                       String mimeType,
                                       InputStream content) throws CloudDriveException, RepositoryException {
      try {
        DriveItem updatedDriveItem = api.updateFileContent(getId(fileNode), null, modified, content);
        // initFileByDriveItem(fileNode, updatedDriveItem);
        // return createCloudFile(fileNode, updatedDriveItem);
        return initCreateFile(fileNode, updatedDriveItem);
      } catch (Throwable e) {
        LOG.error("Failed to update file content {}", fileNode, e);
        // TODO what we solve by this? Further changes remotely will go to connected drive and fail
        throw new SkipSyncException("failed to update file content");
      }
    }

    @Override
    public CloudFile restore(String id, String path) throws SyncNotSupportedException {
      throw new SyncNotSupportedException("Restore not supported");
    }

    private DriveItem updateItem(Node itemNode, Calendar modified) throws RepositoryException, URISyntaxException {
      DriveItem driveItemModifiedFields = prepareModifiedDriveItem(itemNode, modified);
      return api.updateFile(driveItemModifiedFields);
    }

    private DriveItem prepareModifiedDriveItem(Node fileNode, Calendar modified) throws RepositoryException {
      DriveItem driveItem = new DriveItem();
      driveItem.id = getId(fileNode);
      driveItem.name = getTitle(fileNode);
      driveItem.parentReference = new ItemReference();
      String parentId = getParentId(fileNode);
      if (parentId == null || parentId.isEmpty()) {
        parentId = api.getRootId();
      }
      driveItem.parentReference.id = parentId;
      driveItem.fileSystemInfo = new FileSystemInfo();
      driveItem.fileSystemInfo.lastModifiedDateTime = modified;
      return driveItem;
    }
  }
}
