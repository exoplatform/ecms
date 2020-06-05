/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.exodrive;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.CloudFileAPI;
import org.exoplatform.services.cms.clouddrives.CloudUser;
import org.exoplatform.services.cms.clouddrives.DriveRemovedException;
import org.exoplatform.services.cms.clouddrives.NotFoundException;
import org.exoplatform.services.cms.clouddrives.SyncNotSupportedException;
import org.exoplatform.services.cms.clouddrives.exodrive.service.ExoDriveException;
import org.exoplatform.services.cms.clouddrives.exodrive.service.ExoDriveRepository;
import org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudFile;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.clouddrives.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

/**
 * Exo Drive for internal use. <br>
 * !NOT TESTED! after CLDINT-879 rework. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRLocalExoDrive.java 00000 Oct 4, 2012 pnedonosko $
 */
public class JCRLocalExoDrive extends JCRLocalCloudDrive {

  class Connect extends ConnectCommand {

    volatile int complete, available;

    public Connect() throws RepositoryException, DriveRemovedException {
      super();
    }

    /**
     * @inherritDoc
     */
    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      try {
        List<FileStore> files = service.listFiles(user.getUsername());
        available = files.size();
        for (FileStore f : files) {
          Node localNode = openFile(f.getId(), f.getName(), driveNode);
          initFile(localNode,
                   f.getId(),
                   f.getName(),
                   f.getType(),
                   f.getLink(),
                   f.getLink(),
                   f.getLink(),
                   f.getAuthor(),
                   f.getAuthor(),
                   f.getCreateDate(),
                   f.getModifiedDate(),
                   0);
          // FYI it is a sole parent in this connector
          addConnected(DUMMY_DATA, new JCRLocalCloudFile(localNode.getPath(),
                                                         f.getId(),
                                                         f.getName(),
                                                         f.getType(),
                                                         null,
                                                         f.getLink(),
                                                         f.getLink(),
                                                         f.getLink(),
                                                         f.getAuthor(),
                                                         f.getAuthor(),
                                                         f.getCreateDate(),
                                                         f.getModifiedDate(),
                                                         0,
                                                         localNode,
                                                         false));
          complete++;
        }
      } catch (ExoDriveException e) {
        throw new CloudDriveException("Cannot list files for user " + user.getUsername(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getComplete() {
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
      return available;
    }
  }

  class Sync extends SyncCommand {

    volatile int complete, available;

    public Sync() throws RepositoryException, DriveRemovedException {
      super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preSaveChunk() throws CloudDriveException, RepositoryException {
      // nothing for this provider
    }

    /**
     * @inherritDoc
     */
    @Override
    protected void syncFiles() throws CloudDriveException, RepositoryException {
      // XXX the same logic as in ExoDriveConnect
      try {
        List<FileStore> files = service.listFiles(user.getUsername());
        available = files.size();
        for (FileStore f : files) {
          Node localNode = openFile(f.getId(), f.getName(), driveNode);
          initFile(localNode,
                   f.getId(),
                   f.getName(),
                   f.getType(),
                   f.getLink(),
                   f.getLink(),
                   f.getLink(),
                   f.getAuthor(),
                   f.getAuthor(),
                   f.getCreateDate(),
                   f.getModifiedDate(),
                   0);

          addChanged(new JCRLocalCloudFile(localNode.getPath(),
                                           f.getId(),
                                           f.getName(),
                                           f.getType(),
                                           null,
                                           f.getLink(),
                                           f.getLink(),
                                           f.getLink(),
                                           f.getAuthor(),
                                           f.getAuthor(),
                                           f.getCreateDate(),
                                           f.getModifiedDate(),
                                           0,
                                           localNode,
                                           true));
          complete++;
        }
      } catch (ExoDriveException e) {
        throw new CloudDriveException("Cannot list files for user " + user.getUsername(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getComplete() {
      return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
      return available;
    }
  }

  protected class FileAPI extends AbstractFileAPI {

    String filePath(Node fileNode) throws RepositoryException, CloudDriveException {
      String nodePath = fileNode.getPath();
      String drivePath = rootPath();
      int si = drivePath.length() + 1; // w/o a next file separator
      if (nodePath.startsWith(drivePath) && nodePath.length() > si) {
        return nodePath.substring(si);
      } else {
        throw new CloudDriveException("Not a drive file " + nodePath);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile createFile(Node fileNode,
                                Calendar created,
                                Calendar modified,
                                String mimeType,
                                InputStream content) throws CloudDriveException, RepositoryException {
      try {
        FileStore fs = service.create(user.getUsername(), filePath(fileNode), mimeType, created);
        fs.write(content);
        return new JCRLocalCloudFile(fileNode.getPath(),
                                     fs.getId(),
                                     fs.getName(),
                                     fs.getLink(),
                                     previewLink(null, fileNode),
                                     fs.getLink(),
                                     fs.getType(),
                                     mimeTypes.getMimeTypeMode(fs.getType(), fs.getName()),
                                     fs.getLastUser(),
                                     fs.getAuthor(),
                                     fs.getCreateDate(),
                                     fs.getModifiedDate(),
                                     0,
                                     fileNode,
                                     true);
      } catch (ExoDriveException e) {
        throw new CloudDriveException("Error creating cloud file " + getTitle(fileNode), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException {
      try {
        FileStore fs = service.create(user.getUsername(), filePath(folderNode), FileStore.TYPE_FOLDER, created);
        return new JCRLocalCloudFile(folderNode.getPath(),
                                     fs.getId(),
                                     fs.getName(),
                                     fs.getLink(),
                                     fs.getType(),
                                     fs.getLastUser(),
                                     fs.getAuthor(),
                                     fs.getCreateDate(),
                                     fs.getModifiedDate(),
                                     folderNode,
                                     true);
      } catch (ExoDriveException e) {
        throw new CloudDriveException("Error creating cloud folder " + getTitle(folderNode), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException {
      // TODO
      // try {
      // FileStore fs = service.create(user.getUsername(), filePath(fileNode),
      // mimeType, created);
      // } catch (ExoDriveException e) {
      // throw new CloudDriveException("Error creating cloud file " +
      // getTitle(fileNode), e);
      // }
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile updateFileContent(Node fileNode,
                                       Calendar modified,
                                       String mimeType,
                                       InputStream content) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeFile(String id) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeFolder(String id) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trashFile(String id) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trashFolder(String id) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrashSupported() {
      // TODO Auto-generated method stub
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFile restore(String id, String path) throws NotFoundException, CloudDriveException, RepositoryException {
      throw new SyncNotSupportedException("Restore not supported");
    }
  }

  /**
   * Local service.
   */
  protected final ExoDriveRepository service;

  /**
   * For newly connecting drives.
   * 
   * @param drive
   * @param params
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public JCRLocalExoDrive(ExoDriveUser user,
                          ExoDriveRepository service,
                          SessionProviderService sessionProviders,
                          NodeFinder finder,
                          ExtendedMimeTypeResolver mimeTypes,
                          Node driveNode)
      throws CloudDriveException, RepositoryException {
    super(user, driveNode, sessionProviders, finder, mimeTypes);
    this.service = service;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    super.initDrive(driveNode);

    driveNode.setProperty("ecd:id", driveNode.getName());
    driveNode.setProperty("ecd:url", "#");
  }

  /**
   * For drives loading from local Node.
   * 
   * @param drive
   * @param params
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  public JCRLocalExoDrive(ExoDriveRepository service,
                          ExoDriveProvider provider,
                          SessionProviderService sessionProviders,
                          NodeFinder finder,
                          ExtendedMimeTypeResolver mimeTypes,
                          Node driveNode)
      throws CloudDriveException, RepositoryException {
    super(new ExoDriveUser(driveNode.getProperty("ecd:cloudUserName").getString(),
                           driveNode.getProperty("ecd:userEmail").getString(),
                           provider),
          driveNode,
          sessionProviders,
          finder,
          mimeTypes);
    this.service = service;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExoDriveUser getUser() {
    return (ExoDriveUser) user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long readChangeId() throws DriveRemovedException, RepositoryException {
    return Long.MIN_VALUE; // not maintained as used by single user
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveChangeId(Long id) throws CloudDriveException, RepositoryException {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void refreshAccess() throws CloudDriveException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateAccess(CloudUser user) throws CloudDriveException {
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
  protected SyncCommand getSyncCommand() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
    return new Sync();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudFileAPI createFileAPI() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
    return new FileAPI();
  }
}
