package org.exoplatform.clouddrive.onedrive;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.FileSystemInfo;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionRequestBuilder;

import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.oauth2.UserTokenRefreshListener;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JCRLocalOneDrive extends JCRLocalCloudDrive implements UserTokenRefreshListener {
  /**
   *
   */
  private static final Log    LOG = ExoLogger.getLogger(JCRLocalOneDrive.class);

  protected final OneDriveAPI api = new OneDriveAPI();

  protected JCRLocalOneDrive(CloudUser user,
                             Node driveNode,
                             SessionProviderService sessionProviders,
                             NodeFinder finder,
                             ExtendedMimeTypeResolver mimeTypes)
      throws CloudDriveException,
      RepositoryException {
    super(user, driveNode, sessionProviders, finder, mimeTypes);
  }

  @Override
  protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
    LOG.info("getConnectCommand()");
    return new OneDriveConnectCommand();
  }

  @Override
  protected SyncCommand getSyncCommand() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
    LOG.info("getSyncCommand()");
    return new OneDriveSyncCommand();
  }

  @Override
  protected CloudFileAPI createFileAPI() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
    LOG.info("createFileAPI()");
    return new OneDriveFileAPI();
  }

  @Override
  protected Long readChangeId() throws CloudDriveException, RepositoryException {
    LOG.info("readChangeId()");
    return 0L;
  }

  @Override
  protected void saveChangeId(Long id) throws CloudDriveException, RepositoryException {
    LOG.info("saveChangeId()");
  }

  @Override
  public CloudUser getUser() {
    LOG.info("getUser()");
    return this.user;
  }

  @Override
  protected void refreshAccess() throws CloudDriveException {
    LOG.info("refreshAccess()");
  }

  @Override
  protected void updateAccess(CloudUser user) throws CloudDriveException, RepositoryException {
    LOG.info("------");
  }

  @Override
  public void onUserTokenRefresh(UserToken token) throws CloudDriveException {
    LOG.info("------");
  }

  @Override
  public void onUserTokenRemove() throws CloudDriveException {
    LOG.info("------");
  }

  private void initFolderByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
    initFolder(fileNode, item.id, item.name, "type", item.webUrl, "", "", Calendar.getInstance(), Calendar.getInstance());

  }

  private void initFileByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
    initFile(fileNode,
             item.id,
             item.name,
             "",
             item.webUrl,
             "",
             "",
             "",
             "",
             item.createdDateTime,
             item.lastModifiedDateTime,
             item.size);
  }

  private JCRLocalCloudFile createCloudFolderByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
    return new JCRLocalCloudFile(fileNode.getPath(),
                                 item.id,
                                 item.name,
                                 item.webUrl,
                                 "type",
                                 "lastUser",
                                 "author",
                                 Calendar.getInstance(),
                                 Calendar.getInstance(),
                                 fileNode,
                                 true);
  }

  private JCRLocalCloudFile createCloudFileByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
    return new JCRLocalCloudFile(fileNode.getPath(),
                                 item.id,
                                 item.name,
                                 item.webUrl,
                                 item.oDataType,
                                 "lastUser",
                                 "author",
                                 Calendar.getInstance(),
                                 Calendar.getInstance(),
                                 fileNode,
                                 true);
  }

  class OneDriveConnectCommand extends ConnectCommand {
    /**
     * Connect command constructor.
     *
     * @throws RepositoryException the repository exception
     * @throws DriveRemovedException the drive removed exception
     */
    OneDriveConnectCommand() throws RepositoryException, DriveRemovedException {
    }

    private JCRLocalCloudFile openAndInitFolder(DriveItem item, Node localFile) throws RepositoryException, CloudDriveException {
      Node fileNode = openFolder(item.id, item.name, localFile);
      initFolderByDriveItem(fileNode, item);
      fetchFiles(item.id, fileNode);
      return createCloudFolderByDriveItem(fileNode, item);
    }

    private JCRLocalCloudFile openAndInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
      Node fileNode = openFile(item.id, item.name, localFile);
      initFileByDriveItem(fileNode, item);
      return createCloudFileByDriveItem(fileNode, item);

    }

    /**
     * @param fileId fileId at site
     * @param localFile jcr node
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    private void fetchFiles(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
      // For Test Delete Root Node;
      // removeNode(rootNode());
      // Node node = rootNode();
      // removeLinks(node);
      // node.remove();
      List<DriveItem> items = api.getChildren(fileId);
      for (DriveItem item : items) {
        // if(!isConnected(fileId, item.id)){
        JCRLocalCloudFile jcrLocalCloudFile = null;
        if (item.folder != null) {
          jcrLocalCloudFile = openAndInitFolder(item, localFile);
        } else if (item.file != null) {
          jcrLocalCloudFile = openAndInitFile(item, localFile);
        }
        // if (jcrLocalCloudFile != null) {
        // addConnected(fileId,jcrLocalCloudFile);
        // }
        // }

      }
    }

      @Override
      protected void fetchFiles() throws CloudDriveException, RepositoryException {
          rootNode(true).setProperty("ecd:id", api.getRoot().id);
          fetchFiles(null, driveNode);
      }

  }

  protected class OneDriveSyncCommand extends SyncCommand {

    public List<DriveItem> getLastChangesAndUpdateDeltaToken() throws RepositoryException {
      String deltaToken = getDeltaToken();
      if (deltaToken == null || deltaToken.isEmpty()) {
        deltaToken = "latest";
      }
      List<DriveItem> changes = new ArrayList<>();
      IDriveItemDeltaCollectionPage deltaCollectionPage = api.delta(deltaToken);
      IDriveItemDeltaCollectionRequestBuilder nextPage;
      while (true) {
        changes.addAll(deltaCollectionPage.getCurrentPage());
        nextPage = deltaCollectionPage.getNextPage();
        if (nextPage == null) {
          deltaToken = extractDeltaToken(deltaCollectionPage.deltaLink());
          break;
        }
        deltaCollectionPage = nextPage.buildRequest().get();
      }
      saveDeltaToken(deltaToken);
      return changes;
    }

    void saveDeltaToken(String deltaToken) throws RepositoryException {
        driveNode.setProperty("onedrive:changeToken",deltaToken);

    }

    String getDeltaToken() throws RepositoryException {
        if(driveNode.hasProperty("onedrive:changeToken")) {
            return driveNode.getProperty("onedrive:changeToken").getString();
        }
        return null;
    }
    private String extractDeltaToken(String deltaLink) {
      return deltaLink.substring(deltaLink.indexOf("=") + 1);
    }



    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {
          LOG.info("preSaveChunk()");
    }

      @Override
      protected void syncFiles() throws CloudDriveException, RepositoryException, InterruptedException {
        LOG.info("syncFiles()");
          List<DriveItem> changes = getLastChangesAndUpdateDeltaToken();
          Map<String, DriveItem> changesMap = changes.stream()
                  .collect(Collectors.toMap(driveItem -> driveItem.id, driveItem -> driveItem));
          Set<String> tracked = new HashSet<>();
          for (DriveItem driveItem : changes) {
              if (driveItem.file != null) {
                  if (driveItem.deleted == null) {
                      Node fileNode = findNode(driveItem.id);
                      if (fileNode == null) { // add
                          updateParentHierarchy(fileNode, tracked);
                          fileNode = openFile(driveItem.id, driveItem.name, findNode(driveItem.parentReference.id));
                          initFileByDriveItem(fileNode, driveItem);
                      } else { // update
                            fileNode.setProperty("exo:title", driveItem.name);
                          // renamed, moved

                      }
                  }else{
                    // delete
                  }
              }
          }


      }

      private void updateParentHierarchy(Node fileNode, Set<String> tracked) {
        // update Value
        // set item tracked
      }
  }

  class OneDriveFileAPI extends AbstractFileAPI {

    @Override
    public boolean removeFile(String id) throws CloudDriveException, RepositoryException {
      LOG.info("removeFile(): ");
      api.removeFile(id);
      return true;
    }

    @Override
    public boolean removeFolder(String id) throws CloudDriveException, RepositoryException {
      LOG.info("removeFolder(): ");
      api.removeFolder(id);
      return true;
    }

    @Override
    public boolean isTrashSupported() {
      return true;
    }

    @Override
    public boolean trashFile(String id) throws CloudDriveException, RepositoryException {
      LOG.info("trashFile(): ");
      this.removeFile(id);
      return false;
    }

    @Override
    public boolean trashFolder(String id) throws CloudDriveException, RepositoryException {
      LOG.info("trashFolder(): ");
      this.removeFolder(id);
      return false;
    }

    @Override
    public CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException {
      LOG.info("untrashFile(): ");
      return null;
    }

    @Override
    public CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException {
      LOG.info("untrashFolder(): ");
      return null;
    }

    private String extractAppropriateOneDrivePath(Node fileNode) throws RepositoryException {
      StringBuilder oneDrivePath = new StringBuilder();
      while (fileNode != null && fileNode.hasProperty("exo:title")) {
        oneDrivePath.insert(0, "/" + getTitle(fileNode));
        fileNode = fileNode.getParent();
      }

      String path = oneDrivePath.substring(oneDrivePath.indexOf("/", oneDrivePath.indexOf("/") + 1) + 1);

      LOG.info("OneDrivePath: " + path);
      return path;
    }

    @Override
    public CloudFile createFile(Node fileNode,
                                Calendar created,
                                Calendar modified,
                                String mimeType,
                                InputStream content) throws CloudDriveException, RepositoryException {
      LOG.info("Create File Path : " + fileNode.getPath() + "\n" + "Create File Name: " + getTitle(fileNode));

      String path = extractAppropriateOneDrivePath(fileNode);
      LOG.info("One Drive Path: " + path);
      DriveItem createdDriveItem = api.insert(path,
                                              getTitle(fileNode),
                                              created,
                                              modified,
                                              mimeType,
                                              content);
      if (createdDriveItem != null) {
        initFileByDriveItem(fileNode, createdDriveItem);
        return createCloudFileByDriveItem(fileNode, createdDriveItem);
      }
      return null;
    }

    @Override
    public CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException {
      LOG.info("createFolder(): ");
      String parentId = getParentId(folderNode);
      DriveItem createdFolder = api.createFolder(parentId, getTitle(folderNode), created);
      initFolderByDriveItem(folderNode, createdFolder);
      return createCloudFolderByDriveItem(folderNode, createdFolder);
    }

    @Override
    public CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException {
      LOG.info("copyFile(): ");
      DriveItem file = api.copyFile(getParentId(destFileNode), getTitle(destFileNode), getId(srcFileNode));
      initFileByDriveItem(destFileNode, file);
      return createCloudFolderByDriveItem(destFileNode, file);
    }

    @Override
    public CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException {
      LOG.info("copyFolder(): ");
      DriveItem folder = api.copyFolder(getParentId(destFolderNode), getTitle(destFolderNode), getId(srcFolderNode));
      if (folder != null) {
        initFolderByDriveItem(destFolderNode, folder);
        return createCloudFolderByDriveItem(destFolderNode, folder);
      }
      return null;
    }

    @Override
    public CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException {
      LOG.info("updateFile(): ");
      DriveItem modifiedItem = updateItem(fileNode, modified);
      initFileByDriveItem(fileNode, modifiedItem);
      return createCloudFileByDriveItem(fileNode, modifiedItem);
    }

    @Override
    public CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException {
      LOG.info("updateFolder(): ");
      DriveItem modifiedItem = updateItem(folderNode, modified);
      initFolderByDriveItem(folderNode, modifiedItem);
      return createCloudFolderByDriveItem(folderNode, modifiedItem);
    }

    @Override
    public CloudFile updateFileContent(Node fileNode,
                                       Calendar modified,
                                       String mimeType,
                                       InputStream content) throws CloudDriveException, RepositoryException {
      LOG.info("updateFileContent(): ");
      DriveItem updatedDriveItem = api.updateFileContent(extractAppropriateOneDrivePath(fileNode),
                                                         getTitle(fileNode),
                                                         null,
                                                         modified,
                                                         mimeType,
                                                         content);
      if (updatedDriveItem != null) {
        initFileByDriveItem(fileNode, updatedDriveItem);
        return createCloudFileByDriveItem(fileNode, updatedDriveItem);
      }
      return null;
    }

    @Override
    public CloudFile restore(String id, String path) throws CloudDriveException, RepositoryException {
      LOG.info("restore(): ");
      return null;
    }

    private DriveItem updateItem(Node itemNode, Calendar modified) throws RepositoryException {
      DriveItem driveItemModifiedFields = prepareModifiedDriveItem(itemNode, modified);
      api.updateFile(driveItemModifiedFields);
      return api.getItem(driveItemModifiedFields.id);
    }

    private DriveItem prepareModifiedDriveItem(Node fileNode, Calendar modified) throws RepositoryException {
      DriveItem driveItem = new DriveItem();
      driveItem.id = getId(fileNode);
      driveItem.name = getTitle(fileNode);
      driveItem.parentReference = new ItemReference();
      String parentId = getParentId(fileNode);
      if (parentId == null || parentId.isEmpty()) {
        parentId = api.getRoot().id;
      }
      driveItem.parentReference.id = parentId;
      driveItem.fileSystemInfo = new FileSystemInfo();
      driveItem.fileSystemInfo.lastModifiedDateTime = modified;
      return driveItem;
    }
  }
}
