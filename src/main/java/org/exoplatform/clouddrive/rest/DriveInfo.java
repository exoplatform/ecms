/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

package org.exoplatform.clouddrive.rest;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.RefreshAccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Resource what will be returned to clients. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveInfo.java 00000 10 Nov 2013 peter $
 */
public class DriveInfo {

  final CloudProvider          provider;

  final Map<String, CloudFile> files;

  final Collection<String>     removed;

  final String                 workspace;

  final String                 path;

  final String                 user;

  final String                 email;

  final String                 title;

  final Object                 state;

  final boolean                connected;

  DriveInfo(String title,
            String workspace,
            String path,
            String user,
            String email,
            Object state,
            boolean connected,
            CloudProvider provider,
            Map<String, CloudFile> files,
            Collection<String> removed) {
    this.title = title;
    this.workspace = workspace;
    this.path = path;
    this.user = user;
    this.email = email;
    this.state = state;
    this.connected = connected;
    this.provider = provider;
    this.files = files;
    this.removed = removed;
  }

  static DriveInfo create(String workspaces,
                          CloudDrive drive,
                          Collection<CloudFile> files,
                          Collection<String> removed) throws DriveRemovedException,
                                                     CloudProviderException,
                                                     RepositoryException,
                                                     RefreshAccessException {
    Map<String, CloudFile> driveFiles = new HashMap<String, CloudFile>();
    for (CloudFile cf : files) {
      driveFiles.put(cf.getPath(), cf);
    }
    return new DriveInfo(drive.getTitle(),
                         workspaces,
                         drive.getPath(),
                         drive.getUser().getUsername(),
                         drive.getUser().getEmail(),
                         drive.getState(),
                         drive.isConnected(),
                         drive.getUser().getProvider(),
                         driveFiles,
                         removed);
  }

  static DriveInfo create(String workspaces, CloudDrive drive, Collection<CloudFile> files) throws DriveRemovedException,
                                                                                           CloudProviderException,
                                                                                           RepositoryException,
                                                                                           RefreshAccessException {
    return create(workspaces, drive, files, new HashSet<String>());
  }

  static DriveInfo create(String workspaces, CloudDrive drive) throws DriveRemovedException,
                                                              CloudProviderException,
                                                              RepositoryException,
                                                              RefreshAccessException {
    return create(workspaces, drive, new ArrayList<CloudFile>(), new HashSet<String>());
  }

  public CloudProvider getProvider() {
    return provider;
  }

  public Map<String, CloudFile> getFiles() {
    return files;
  }

  public Collection<String> getRemoved() {
    return removed;
  }

  public String getPath() {
    return path;
  }

  public String getUser() {
    return user;
  }

  public String getEmail() {
    return email;
  }

  public Object getState() {
    return state;
  }

  public String getTitle() {
    return title;
  }

  public String getWorkspace() {
    return workspace;
  }

  public boolean isConnected() {
    return connected;
  }

}
