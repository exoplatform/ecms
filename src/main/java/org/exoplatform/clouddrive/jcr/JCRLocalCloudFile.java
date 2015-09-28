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

import org.exoplatform.clouddrive.CloudFile;

import java.util.Calendar;

import javax.jcr.Node;

/**
 * A POJO providing information about a cloud file stored in JCR.
 */
public class JCRLocalCloudFile implements CloudFile {

  /**
   * Folder size by default is -1.
   */
  public static final long         FOLDER_SIZE = -1;

  private final String             path;

  private final String             id;

  private final String             title;

  private final String             link;

  private final String             editLink;

  private final String             previewLink;

  private final String             thumbnailLink;

  private final String             type, typeMode;

  private final String             lastUser;

  private final String             author;

  private final boolean            folder;

  private final long               size;

  // FYI transient fields will not appear in serialized forms like JSON object on client side

  private final transient Calendar createdDate;

  private final transient Calendar modifiedDate;

  private final transient Node     node;

  private final transient boolean  changed;

  /**
   * Local cloud file or folder (full internal constructor).
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param editLink {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param folder {@link Boolean}
   * @param node {@link Node}
   * @param changed {@link Boolean}
   */
  protected JCRLocalCloudFile(String path,
                              String id,
                              String title,
                              String link,
                              String editLink,
                              String previewLink,
                              String thumbnailLink,
                              String type,
                              String typeMode,
                              String lastUser,
                              String author,
                              Calendar createdDate,
                              Calendar modifiedDate,
                              boolean folder,
                              long size,
                              Node node,
                              boolean changed) {
    this.path = path;
    this.id = id;
    this.title = title;
    this.link = link;
    this.editLink = editLink;
    this.previewLink = previewLink;
    this.thumbnailLink = thumbnailLink;
    this.type = type;
    this.typeMode = typeMode;
    this.lastUser = lastUser;
    this.author = author;
    this.createdDate = createdDate;
    this.modifiedDate = modifiedDate;
    this.folder = folder;
    this.size = size;
    this.node = node;
    this.changed = changed;
  }

  /**
   * Local cloud file with edit link.
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param editLink {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param node {@link Node}
   * @param changed {@link Boolean}
   */
  public JCRLocalCloudFile(String path,
                           String id,
                           String title,
                           String link,
                           String editLink,
                           String previewLink,
                           String thumbnailLink,
                           String type,
                           String typeMode,
                           String lastUser,
                           String author,
                           Calendar createdDate,
                           Calendar modifiedDate,
                           long size,
                           Node node,
                           boolean changed) {
    this(path,
         id,
         title,
         link,
         editLink,
         previewLink,
         thumbnailLink,
         type,
         typeMode,
         lastUser,
         author,
         createdDate,
         modifiedDate,
         false,
         size,
         node,
         changed);
  }

  /**
   * Local cloud file without edit link.
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param node {@link Node}
   * @param changed {@link Boolean}
   */
  public JCRLocalCloudFile(String path,
                           String id,
                           String title,
                           String link,
                           String previewLink,
                           String thumbnailLink,
                           String type,
                           String typeMode,
                           String lastUser,
                           String author,
                           Calendar createdDate,
                           Calendar modifiedDate,
                           long size,
                           Node node,
                           boolean changed) {
    this(path,
         id,
         title,
         link,
         null, // editLink
         previewLink,
         thumbnailLink,
         type,
         typeMode,
         lastUser,
         author,
         createdDate,
         modifiedDate,
         false,
         size,
         node,
         changed);
  }

  /**
   * Local cloud folder (without edit, preview, thumbnail links, type mode and size).
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param type {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param node {@link Node}
   * @param changed {@link Boolean}
   */
  public JCRLocalCloudFile(String path,
                           String id,
                           String title,
                           String link,
                           String type,
                           String lastUser,
                           String author,
                           Calendar createdDate,
                           Calendar modifiedDate,
                           Node node,
                           boolean changed) {
    this(path,
         id,
         title,
         link,
         null, // editLink
         null, // previewLink,
         null, // thumbnailLink,
         type,
         null, // typeMode,
         lastUser,
         author,
         createdDate,
         modifiedDate,
         true,
         FOLDER_SIZE,
         node,
         changed);
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
   * @return the typeMode
   */
  public String getTypeMode() {
    return typeMode;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Calendar getModifiedDate() {
    return modifiedDate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFolder() {
    return folder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return isFolder() ? FOLDER_SIZE : size;
  }

  /**
   * JCR Node that represent this Cloud File in the storage. Returned {@link Node} instance can be treated
   * as valid only in a short time span - just after the operation on the file internally in
   * {@link JCRLocalCloudDrive}. Otherwise need check does the node's session valid (not expired for example).
   * 
   * @return the node that represent this Cloud File in the storage.
   */
  public Node getNode() {
    return node;
  }

  /**
   * Indicate does this Cloud File was changed (<code>true</code>) or read (<code>false</code>) from the
   * storage. Used internally only!
   * 
   * @return the changed flag
   */
  public boolean isChanged() {
    return changed;
  }
}
