/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

package org.exoplatform.ecm.connector.clouddrives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudDriveMessage;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.CloudProvider;

/**
 * Drive representation that will be returned to clients. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveInfo.java 00000 10 Nov 2013 peter $
 */
public class DriveInfo {

  /** The provider. */
  final CloudProvider                 provider;

  /** The files. */
  final Map<String, CloudFile>        files;

  /** The removed. */
  final Collection<String>            removed;

  /** The messages. */
  final Collection<CloudDriveMessage> messages;

  /** The workspace. */
  final String                        workspace;

  /** The path. */
  final String                        path;

  /** The title. */
  final String                        title;

  /** The state. */
  final Object                        state;

  /** The connected. */
  final boolean                       connected;

  /**
   * Instantiates a new drive info.
   *
   * @param title the title
   * @param workspace the workspace
   * @param path the path
   * @param state the state
   * @param connected the connected
   * @param provider the provider
   * @param files the files
   * @param removed the removed
   * @param messages the messages
   */
  DriveInfo(String title,
            String workspace,
            String path,
            Object state,
            boolean connected,
            CloudProvider provider,
            Map<String, CloudFile> files,
            Collection<String> removed,
            Collection<CloudDriveMessage> messages) {
    this.title = title;
    this.workspace = workspace;
    this.path = path;
    this.state = state;
    this.connected = connected;
    this.provider = provider;
    this.files = files;
    this.messages = messages;
    this.removed = removed;
  }

  /**
   * Creates the.
   *
   * @param workspaces the workspaces
   * @param drive the drive
   * @param files the files
   * @param removed the removed
   * @param messages the messages
   * @return the drive info
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  static DriveInfo create(String workspaces,
                          CloudDrive drive,
                          Collection<CloudFile> files,
                          Collection<String> removed,
                          Collection<CloudDriveMessage> messages) throws RepositoryException, CloudDriveException {
    Map<String, CloudFile> driveFiles = new HashMap<String, CloudFile>();
    for (CloudFile cf : files) {
      driveFiles.put(cf.getPath(), cf);
    }
    return new DriveInfo(drive.getTitle(),
                         workspaces,
                         drive.getPath(),
                         drive.getState(),
                         drive.isConnected(),
                         drive.getUser().getProvider(),
                         driveFiles,
                         removed,
                         messages);
  }

  /**
   * Creates the.
   *
   * @param workspaces the workspaces
   * @param drive the drive
   * @param files the files
   * @param messages the messages
   * @return the drive info
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  static DriveInfo create(String workspaces,
                          CloudDrive drive,
                          Collection<CloudFile> files,
                          Collection<CloudDriveMessage> messages) throws RepositoryException, CloudDriveException {
    return create(workspaces, drive, files, new HashSet<String>(), messages);
  }

  /**
   * Creates the.
   *
   * @param workspaces the workspaces
   * @param drive the drive
   * @return the drive info
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  static DriveInfo create(String workspaces, CloudDrive drive) throws RepositoryException, CloudDriveException {
    return create(workspaces, drive, new ArrayList<CloudFile>(), new HashSet<String>(), new ArrayList<CloudDriveMessage>());
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public CloudProvider getProvider() {
    return provider;
  }

  /**
   * Gets the files.
   *
   * @return the files
   */
  public Map<String, CloudFile> getFiles() {
    return files;
  }

  /**
   * Gets the removed.
   *
   * @return the removed
   */
  public Collection<String> getRemoved() {
    return removed;
  }

  /**
   * Gets the messages.
   *
   * @return the messages
   */
  public Collection<CloudDriveMessage> getMessages() {
    return messages;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the state.
   *
   * @return the state
   */
  public Object getState() {
    return state;
  }

  /**
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Checks if is connected.
   *
   * @return true, if is connected
   */
  public boolean isConnected() {
    return connected;
  }

}
