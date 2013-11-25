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
package org.exoplatform.clouddrive;


/**
 * A POJO with information for {@link CloudDriveListener}.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveEvent.java 00000 Jan 3, 2013 pnedonosko $
 * 
 */
public class CloudDriveEvent {

  protected final CloudUser             user;

  protected final String                workspace;

  protected final String                nodePath;

  public CloudDriveEvent(CloudUser user, String workspace, String nodePath) {
    this.user = user;
    this.workspace = workspace;
    this.nodePath = nodePath;
  }

  /**
   * @return affected user
   */
  public CloudUser getUser() {
    return user;
  }

  /**
   * @return the drive's JCR node workspace name
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * @return the drive's JCR node path, can be <code>null</code>
   */
  public String getNodePath() {
    return nodePath;
  }
}
