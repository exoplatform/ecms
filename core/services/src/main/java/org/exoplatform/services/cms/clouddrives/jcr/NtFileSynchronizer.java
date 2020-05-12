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
package org.exoplatform.services.cms.clouddrives.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.CloudFileAPI;
import org.exoplatform.services.cms.clouddrives.CloudFileSynchronizer;
import org.exoplatform.services.cms.clouddrives.SkipSyncException;
import org.exoplatform.services.cms.clouddrives.SyncNotSupportedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Synchronizer handling nt:file and nt:folder nodetypes.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: NtFileSynchronizer.java 00000 Mar 21, 2014 pnedonosko $
 */
public class NtFileSynchronizer implements CloudFileSynchronizer {

  /** The Constant NODETYPES. */
  public static final String[] NODETYPES = new String[] { JCRLocalCloudDrive.NT_FILE, JCRLocalCloudDrive.NT_FOLDER };

  /** The Constant LOG. */
  protected static final Log   LOG       = ExoLogger.getLogger(NtFileSynchronizer.class);

  /**
   * Instantiates a new nt file synchronizer.
   */
  public NtFileSynchronizer() {
  }

  /**
   * {@inheritDoc}
   */
  public String[] getSupportedNodetypes() {
    return NODETYPES;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Node file) throws RepositoryException, SkipSyncException {
    if (file.isNodeType(JCRLocalCloudDrive.NT_FILE) || file.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      return true;
    } else if (file.isNodeType(JCRLocalCloudDrive.NT_RESOURCE)) {
      throw new SkipSyncException("Skip synchronization of " + JCRLocalCloudDrive.NT_RESOURCE);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public CloudFile create(Node file, CloudFileAPI api) throws RepositoryException, CloudDriveException {
    String title;
    try {
      title = api.getTitle(file);
    } catch (PathNotFoundException e) {
      try {
        title = file.getProperty("exo:name").getString();
      } catch (PathNotFoundException e1) {
        title = file.getName();
      }
    }

    CloudFile result;
    Calendar created = file.getProperty("jcr:created").getDate();

    if (file.isNodeType(JCRLocalCloudDrive.NT_FILE)) { // use JCR, this node not
                                                       // yet a cloud file
      Node resource = file.getNode("jcr:content");
      String mimeType = resource.getProperty("jcr:mimeType").getString();
      Calendar modified = resource.getProperty("jcr:lastModified").getDate();
      InputStream data = resource.getProperty("jcr:data").getStream();

      try {
        result = api.createFile(file, created, modified, mimeType, data);
        resource.setProperty("jcr:data", JCRLocalCloudDrive.DUMMY_DATA); // empty
                                                                         // data
                                                                         // to
                                                                         // zero
                                                                         // string
      } finally {
        try {
          data.close();
        } catch (IOException e) {
          LOG.warn("Error closing content stream of cloud file " + title + ": " + e.getMessage());
        }
      }
    } else if (file.isNodeType(JCRLocalCloudDrive.NT_FOLDER)) {
      result = api.createFolder(file, created);
      // traverse and create child files
      for (NodeIterator childs = file.getNodes(); childs.hasNext();) {
        create(childs.nextNode(), api);
      }
    } else {
      // it's smth not expected
      throw new SyncNotSupportedException("Unexpected type of created node in nt:file or nt:folder hierarchy: "
          + file.getPrimaryNodeType().getName() + ". Location: " + file.getPath());
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remove(String filePath, String fileId, boolean isFolder, CloudFileAPI api) throws CloudDriveException,
                                                                                            RepositoryException {
    if (isFolder) {
      return api.removeFolder(fileId);
    } else {
      return api.removeFile(fileId);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean trash(String filePath, String fileId, boolean isFolder, CloudFileAPI api) throws RepositoryException,
                                                                                           CloudDriveException {
    if (isFolder) {
      return api.trashFolder(fileId);
    } else {
      return api.trashFile(fileId);
    }
  }

  /**
   * {@inheritDoc}
   */
  public CloudFile untrash(Node file, CloudFileAPI api) throws RepositoryException, CloudDriveException {
    if (api.isFolder(file)) {
      return api.untrashFolder(file);
    } else {
      return api.untrashFile(file);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile update(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    if (api.isFolder(file)) { // use API to get a type of given node, it is
                              // already a cloud file
      Calendar modified;
      if (file.isNodeType(JCRLocalCloudDrive.EXO_DATETIME)) {
        modified = file.getProperty("exo:dateModified").getDate();
      } else {
        modified = Calendar.getInstance(); // will be "now"
      }
      // we don't traverse and update childs!
      return api.updateFolder(file, modified);
    } else if (api.isFile(file)) {
      Node resource = file.getNode("jcr:content");
      Calendar modified = resource.getProperty("jcr:lastModified").getDate();
      return api.updateFile(file, modified);
    } else {
      // it's smth not expected
      throw new SyncNotSupportedException("Unexpected type of updated node in nt:file or nt:folder hierarchy: "
          + file.getPrimaryNodeType().getName() + ". Location: " + file.getPath());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile updateContent(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    String title = api.getTitle(file);

    if (api.isFile(file)) { // use API, in accept() we already selected nt:file
      Node resource = file.getNode("jcr:content");
      String mimeType = resource.getProperty("jcr:mimeType").getString();
      Calendar modified = resource.getProperty("jcr:lastModified").getDate();
      InputStream data = resource.getProperty("jcr:data").getStream();

      try {
        return api.updateFileContent(file, modified, mimeType, data);
      } finally {
        try {
          data.close();
        } catch (IOException e) {
          LOG.warn("Error closing content stream of cloud file " + title + ": " + e.getMessage());
        }
      }
    } else {
      // it's smth not expected
      throw new SyncNotSupportedException("Unexpected type of updated node (content) in nt:file or nt:folder hierarchy: "
          + file.getPrimaryNodeType().getName() + ". Location: " + file.getPath());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile copy(Node srcFile, Node destFile, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    if (api.isFolder(destFile)) {
      return api.copyFolder(srcFile, destFile);
    } else if (api.isFile(destFile)) {
      return api.copyFile(srcFile, destFile);
    } else {
      // it's smth not expected
      throw new SyncNotSupportedException("Unexpected type of copied node in nt:file or nt:folder hierarchy: "
          + destFile.getPrimaryNodeType().getName() + ". Location: " + destFile.getPath());
    }
  }
}
