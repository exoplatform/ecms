package org.exoplatform.clouddrive.onedrive;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.microsoft.graph.models.extensions.DriveItem;

import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
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
  private static final Log LOG = ExoLogger.getLogger(JCRLocalOneDrive.class);
  protected final OneDriveAPI api = new OneDriveAPIImpl();


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

  class OneDriveConnectCommand extends ConnectCommand {
    /**
     * Connect command constructor.
     *
     * @throws RepositoryException   the repository exception
     * @throws DriveRemovedException the drive removed exception
     */
    OneDriveConnectCommand() throws RepositoryException, DriveRemovedException {
    }

    private void openAndInitFolder(DriveItem item, Node localFile) throws RepositoryException, CloudDriveException {
      Node fileNode = openFolder(item.id, item.name, localFile);
      initFolder(fileNode,
              item.id,
              item.name,
              item.oDataType,
              item.webUrl,
              item.name,
              "lastUser",
              Calendar.getInstance(),
              Calendar.getInstance());
      fetchFiles(item.id, fileNode);
    }
    private void openAndInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
      Node fileNode = openFile(item.id, item.name, localFile);
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

    /**
     * @param fileId    fileId at site
     * @param localFile jcr node
     * @throws CloudDriveException
     * @throws RepositoryException
     */
    private void fetchFiles(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
      // For Test Delete Root Node;
      //removeNode(rootNode());
      Node node = rootNode();
      removeLinks(node);
      node.remove();

      List<DriveItem> items = api.getChildren(fileId);
      for (DriveItem item : items) {
        if (item.folder != null) {
          openAndInitFolder(item, localFile);
        } else if (item.file != null) {
          openAndInitFile(item, localFile);
        }
      }
    }

    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      fetchFiles(null, driveNode);
    }


  }
  protected class OneDriveSyncCommand extends SyncCommand {

    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {

    }

    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException, InterruptedException {

    }
  }

  class OneDriveFileAPI extends AbstractFileAPI {


    @Override
    public boolean removeFile(String id) throws CloudDriveException, RepositoryException {
      api.removeFile(id);
      return true;
    }

    @Override
    public boolean removeFolder(String id) throws CloudDriveException, RepositoryException {
      api.removeFile(id);
      return false;
    }

    @Override
    public boolean isTrashSupported() {
      return true;
    }

    @Override
    public boolean trashFile(String id) throws CloudDriveException, RepositoryException {
      this.removeFile(id);
      return false;
    }

    @Override
    public boolean trashFolder(String id) throws CloudDriveException, RepositoryException {
      this.removeFolder(id);
      return false;
    }

    @Override
    public CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile createFile(Node fileNode, Calendar created, Calendar modified, String mimeType, InputStream content) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile updateFileContent(Node fileNode, Calendar modified, String mimeType, InputStream content) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException {
      return null;
    }

    @Override
    public CloudFile restore(String id, String path) throws CloudDriveException, RepositoryException {
      return null;
    }
  }
}
