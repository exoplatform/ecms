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
package org.exoplatform.clouddrive.ecms.rest;

import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.rest.LinkedCloudFile;

/**
 * Wraps fields from {@link CloudFile} shared with other users and offers its
 * path (as a path of that file {@link Node} symlink) and a link for opening
 * this file in eXo Platform.<br>
 * NOTE: we cannot wrap instance of another another {@link CloudFile} as it
 * leads to StackOverflowError in WS JsonGeneratorImpl.<br>
 * Created by The eXo Platform SAS.<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LinkedCloudFile.java 00000 Jan 24, 2013 pnedonosko $
 */
public class SharedCloudFile extends LinkedCloudFile {

  /** The is symlink. */
  private final boolean isSymlink;

  /** The open link. */
  private final String  openLink;
  
  // FYI transient fields will not appear in serialized forms like JSON object
  // on client side
  /** The created date. */
  final transient Calendar createdDate = null;

  /** The modified date. */
  final transient Calendar modifiedDate = null;

  /**
   * Instantiates a new linked cloud file.
   *
   * @param file the file
   * @param path the path
   * @param openLink the open link
   */
  public SharedCloudFile(CloudFile file, String path, String openLink) {
    super(file, path);
    this.isSymlink = !file.getPath().equals(path);
    this.openLink = openLink;
  }

  /**
   * @return the openLink
   */
  public String getOpenLink() {
    return openLink;
  }

  /**
   * Checks if is symlink.
   *
   * @return true, if is symlink
   */
  public boolean isSymlink() {
    return isSymlink;
  }
}
