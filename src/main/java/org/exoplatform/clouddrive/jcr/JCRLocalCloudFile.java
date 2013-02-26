/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.clouddrive.jcr;

import java.util.Calendar;

import org.exoplatform.clouddrive.CloudFile;


/**
 * A POJO providing information about a cloud file stored in JCR.
 */
public class JCRLocalCloudFile implements CloudFile {

  private final String             path;

  private final String             id;

  private final String             title;

  private final String             link;

  private final String             previewLink;

  private final String             thumbnailLink;

  private final String             type;

  private final String             lastUser;

  private final String             author;

  private final transient Calendar createdDate;

  private final transient Calendar modifiedDate;

  private final boolean            isFolder;

  public JCRLocalCloudFile(String path,
                           String id,
                           String title,
                           String link,
                           String previewLink,
                           String thumbnailLink,
                           String type,
                           String lastUser,
                           String author,
                           Calendar createdDate,
                           Calendar modifiedDate,
                           boolean isFolder) {
    this.path = path;
    this.id = id;
    this.title = title;
    this.link = link;
    this.previewLink = previewLink;
    this.thumbnailLink = thumbnailLink;
    this.type = type;
    this.lastUser = lastUser;
    this.author = author;
    this.createdDate = createdDate;
    this.modifiedDate = modifiedDate;
    this.isFolder = isFolder;
  }

  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLink() {
    return link;
  }

  /**
   * @return the previewLink
   */
  public String getPreviewLink() {
    return previewLink;
  }

  /**
   * @inherritDoc
   */
  @Override
  public String getThumbnailLink() {
    return thumbnailLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLastUser() {
    return lastUser;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthor() {
    return author;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Calendar getCreatedDate() {
    return createdDate;
  }

  @Override
  public Calendar getModifiedDate() {
    return modifiedDate;
  }

  @Override
  public boolean isFolder() {
    return isFolder;
  }
}
