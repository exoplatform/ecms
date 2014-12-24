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
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveMessage;
import org.exoplatform.clouddrive.CloudFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Drive representation that will be returned to clients. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveInfo.java 00000 10 Nov 2013 peter $
 */
public class DriveInfo {

  final ProviderInfo                  provider;

  final Map<String, CloudFile>        files;

  final Collection<String>            removed;

  final Collection<CloudDriveMessage> messages;

  final String                        workspace;

  final String                        path;

  final String                        title;

  final Object                        state;

  final boolean                       connected;

  DriveInfo(String title,
            String workspace,
            String path,
            Object state,
            boolean connected,
            ProviderInfo provider,
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

  static DriveInfo create(String workspaces,
                          CloudDrive drive,
                          Collection<CloudFile> files,
                          Collection<String> removed,
                          Collection<CloudDriveMessage> messages) throws RepositoryException,
                                                                 CloudDriveException {
    Map<String, CloudFile> driveFiles = new HashMap<String, CloudFile>();
    for (CloudFile cf : files) {
      driveFiles.put(cf.getPath(), cf);
    }
    return new DriveInfo(drive.getTitle(),
                         workspaces,
                         drive.getPath(),
                         drive.getState(),
                         drive.isConnected(),
                         new ProviderInfo(drive.getUser()),
                         driveFiles,
                         removed,
                         messages);
  }

  static DriveInfo create(String workspaces,
                          CloudDrive drive,
                          Collection<CloudFile> files,
                          Collection<CloudDriveMessage> messages) throws RepositoryException,
                                                                 CloudDriveException {
    return create(workspaces, drive, files, new HashSet<String>(), messages);
  }

  static DriveInfo create(String workspaces, CloudDrive drive) throws RepositoryException,
                                                              CloudDriveException {
    return create(workspaces,
                  drive,
                  new ArrayList<CloudFile>(),
                  new HashSet<String>(),
                  new ArrayList<CloudDriveMessage>());
  }

  public ProviderInfo getProvider() {
    return provider;
  }

  public Map<String, CloudFile> getFiles() {
    return files;
  }

  public Collection<String> getRemoved() {
    return removed;
  }

  public Collection<CloudDriveMessage> getMessages() {
    return messages;
  }

  public String getPath() {
    return path;
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
