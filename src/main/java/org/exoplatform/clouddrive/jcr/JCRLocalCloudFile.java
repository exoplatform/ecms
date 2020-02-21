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
package org.exoplatform.clouddrive.jcr;

import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.clouddrive.LocalCloudFile;

/**
 * A POJO providing information about a cloud file stored in JCR.
 */
public class JCRLocalCloudFile extends LocalCloudFile {

  /**
   * Folder size by default is -1.
   */
  public static final long         FOLDER_SIZE = -1;

  /** The path. */
  private final String             path;

  /** The id. */
  private final String             id;

  /** The title. */
  private final String             title;

  /** The link. */
  private final String             link;

  /** The edit link. */
  private final String             editLink;

  /** The preview link. */
  private final String             previewLink;

  /** The thumbnail link. */
  private final String             thumbnailLink;

  /** The type mode. */
  private final String             type, typeMode;

  /** The last user. */
  private final String             lastUser;

  /** The author. */
  private final String             author;

  /** The folder. */
  private final boolean            folder;

  /** The size. */
  private final long               size;

  // FYI transient fields will not appear in serialized forms like JSON object
  // on client side
  /** The created date. */
  private final transient Calendar createdDate;

  /** The modified date. */
  private final transient Calendar modifiedDate;

  /** The node. */
  private final transient Node     node;

  /** The changed. */
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
   * @param size the size
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
   * @param size the size
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
   * @param size the size
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

  /**
   * {@inheritDoc}
   */
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
   * Gets the preview link.
   *
   * @return the previewLink
   */
  public String getPreviewLink() {
    return previewLink;
  }

  /**
   * Gets the thumbnail link.
   *
   * @return the thumbnail link
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
   * Gets the type mode.
   *
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
   * JCR Node that represent this Cloud File in the storage. Returned {@link Node} instance can be treated as valid only in a
   * short time span - just after the operation on the file internally in {@link JCRLocalCloudDrive}. Otherwise need check does
   * the node's session valid (not expired for example).<br>
   * Take in account that the node can be obtained via a system session and so all changes over it will be done on behalf of
   * system user.
   *
   * @return the node that represent this Cloud File in the storage.
   */
  @Override
  public Node getNode() {
    return node;
  }

  /**
   * Indicate does this Cloud File was changed (<code>true</code>) or read (<code>false</code>) from the storage. Used internally
   * only! Indicate does this Cloud File was changed (<code>true</code>) or read (<code>false</code>) from the storage. Used
   * internally only!
   *
   * @return the changed flag
   */
  public boolean isChanged() {
    return changed;
  }

}
