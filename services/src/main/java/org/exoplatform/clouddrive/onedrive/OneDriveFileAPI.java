package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudFileAPI;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;

public class OneDriveFileAPI implements CloudFileAPI {
  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(OneDriveFileAPI.class);

  @Override
  public String getId(Node fileNode) throws RepositoryException {
    LOG.info("getId()");
    return null;
  }

  @Override
  public String getTitle(Node fileNode) throws RepositoryException {
    LOG.info("getTitle()");
    return null;
  }

  @Override
  public String getParentId(Node fileNode) throws RepositoryException {

    return null;
  }

  @Override
  public Calendar getCreated(Node fileNode) throws RepositoryException {
    return null;
  }

  @Override
  public Calendar getModified(Node fileNode) throws RepositoryException {
    return null;
  }

  @Override
  public String getAuthor(Node fileNode) throws RepositoryException {
    return null;
  }

  @Override
  public String getLastUser(Node fileNode) throws RepositoryException {
    return null;
  }

  @Override
  public String getType(Node fileNode) throws RepositoryException {
    return null;
  }

  @Override
  public Collection<String> findParents(String id) throws DriveRemovedException, RepositoryException {
    return null;
  }

  @Override
  public boolean isDrive(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean isFolder(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean isFile(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean isFileResource(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean isIgnored(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean ignore(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean unignore(Node node) throws RepositoryException {
    return false;
  }

  @Override
  public boolean removeFile(String id) throws CloudDriveException, RepositoryException {
    return false;
  }

  @Override
  public boolean removeFolder(String id) throws CloudDriveException, RepositoryException {
    return false;
  }

  @Override
  public boolean isTrashSupported() {
    return false;
  }

  @Override
  public boolean trashFile(String id) throws CloudDriveException, RepositoryException {
    return false;
  }

  @Override
  public boolean trashFolder(String id) throws CloudDriveException, RepositoryException {
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
  public CloudFile createFile(Node fileNode,
                              Calendar created,
                              Calendar modified,
                              String mimeType,
                              InputStream content) throws CloudDriveException, RepositoryException {
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
  public CloudFile updateFileContent(Node fileNode,
                                     Calendar modified,
                                     String mimeType,
                                     InputStream content) throws CloudDriveException, RepositoryException {
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
