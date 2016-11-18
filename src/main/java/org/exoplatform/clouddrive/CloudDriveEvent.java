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
package org.exoplatform.clouddrive;

import java.util.Collection;
import java.util.Collections;

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

  /** The user. */
  protected final CloudUser             user;

  /** The workspace. */
  protected final String                workspace;

  /** The node path. */
  protected final String                nodePath;

  /** The changed. */
  protected final Collection<CloudFile> changed;

  /** The removed. */
  protected final Collection<String>    removed;

  /**
   * Create cloud drive event generated by given user on a drive folder connected to given workspace and node
   * path in JCR. Changed and removed file collections will be used "as is", the caller should care about its
   * "unmodification" state if required.
   * 
   * @param user {@link CloudUser}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @param changed {@link Collection} of {@link CloudFile} objects affected by cloud drive operation.
   * @param removed {@link Collection} of file paths removed by cloud drive operation.
   */
  public CloudDriveEvent(CloudUser user,
                         String workspace,
                         String nodePath,
                         Collection<CloudFile> changed,
                         Collection<String> removed) {
    this.user = user;
    this.workspace = workspace;
    this.nodePath = nodePath;
    this.changed = changed;
    this.removed = removed;
  }

  /**
   * Create cloud drive event generated by given user on a drive folder connected to given workspace and node
   * path in JCR. Changed and removed file collections will be set to empty for this event.
   * 
   * @param user {@link CloudUser}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   */
  public CloudDriveEvent(CloudUser user, String workspace, String nodePath) {
    this.user = user;
    this.workspace = workspace;
    this.nodePath = nodePath;
    this.changed = Collections.emptyList();
    this.removed = Collections.emptyList();
  }

  /**
   * Gets the user.
   *
   * @return affected user.
   */
  public CloudUser getUser() {
    return user;
  }

  /**
   * Gets the workspace.
   *
   * @return the drive's JCR node workspace name.
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Gets the node path.
   *
   * @return the drive's JCR node path, can be <code>null</code>.
   */
  public String getNodePath() {
    return nodePath;
  }

  /**
   * Gets the changed.
   *
   * @return the files changed by the event
   */
  public Collection<CloudFile> getChanged() {
    return changed;
  }

  /**
   * Gets the removed.
   *
   * @return the file paths removed by the event
   */
  public Collection<String> getRemoved() {
    return removed;
  }
}
