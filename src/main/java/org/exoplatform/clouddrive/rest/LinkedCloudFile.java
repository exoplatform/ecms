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

import org.exoplatform.clouddrive.CloudFile;

import java.util.Calendar;

import javax.jcr.Node;

/**
 * Wraps fields from another {@link CloudFile} and replace its path with a path of that file {@link Node}
 * symlink node.<br>
 * NOTE: we cannot wrap instance of another another {@link CloudFile} as it leads to StackOverflowError in WS
 * JsonGeneratorImpl. Created by The eXo Platform SAS.<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LinkedCloudFile.java 00000 Jan 24, 2013 pnedonosko $
 */
public class LinkedCloudFile implements CloudFile {

  private final String             id;

  private final String             title;

  private final String             link;

  private final String             previewLink;

  private final String             editLink;

  private final String             thumbnailLink;

  private final String             type, typeMode;

  private final String             lastUser;

  private final String             author;

  private final transient Calendar createdDate;

  private final transient Calendar modifiedDate;

  private final boolean            folder;

  private final String             path;

  private final boolean            isSymlink;

  public LinkedCloudFile(CloudFile file, String path) {
    this.id = file.getId();
    this.title = file.getTitle();
    this.link = file.getLink();
    this.previewLink = file.getPreviewLink();
    this.editLink = file.getEditLink();
    this.thumbnailLink = file.getThumbnailLink();
    this.type = file.getType();
    this.typeMode = file.getTypeMode();
    this.lastUser = file.getLastUser();
    this.author = file.getAuthor();
    this.folder = file.isFolder();
    this.createdDate = file.getCreatedDate();
    this.modifiedDate = file.getModifiedDate();
    this.path = path;
    this.isSymlink = true;
  }

  /**
   * @return the isSymlink
   */
  public boolean isSymlink() {
    return isSymlink;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return link;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditLink() {
    return editLink;
  }

  /**
   * @return the previewLink
   */
  public String getPreviewLink() {
    return previewLink;
  }

  /**
   * @return the thumbnailLink
   */
  public String getThumbnailLink() {
    return thumbnailLink;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTypeMode() {
    return typeMode;
  }

  /**
   * @return the lastUser
   */
  public String getLastUser() {
    return lastUser;
  }

  /**
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * @return the createdDate
   */
  public Calendar getCreatedDate() {
    return createdDate;
  }

  /**
   * @return the modifiedDate
   */
  public Calendar getModifiedDate() {
    return modifiedDate;
  }

  /**
   * @return the isFolder
   */
  public boolean isFolder() {
    return folder;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }
}
