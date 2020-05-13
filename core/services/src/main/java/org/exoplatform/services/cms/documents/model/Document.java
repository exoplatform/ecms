/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.documents.model;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public class Document {
  private String   id;

  private String   name;

  private String   title;

  private String   path;

  private String   workspace;

  private String   state;

  private String   author;
  
  private String drive;
  
  private String mimeType;

  private String   lastEditor;

  private Calendar lastModified;

  private Calendar dateCreated;
  
  private Date dateModified;
  
  private LinkedHashMap<String, String> breadCrumb;
  
  private String openUrl;
  
  private String downloadUrl;
  
  private int version;
  
  private String size;
  
  public Document(String id,
                  String title,
                  String path,
                  String drive,
                  String mimeType,
                  Date dateModified,
                  LinkedHashMap<String, String> breadCrumb,
                  String openUrl,
                  String downloadUrl,
                  int version,
                  String size,
                  String lastEditor) {
    this.id = id;
    this.title = title;
    this.path = path;
    this.drive = drive;
    this.mimeType = mimeType;
    this.dateModified = dateModified;
    this.breadCrumb = breadCrumb;
    this.openUrl = openUrl;
    this.downloadUrl = downloadUrl;
    this.version = version;
    this.size = size;
    this.lastEditor = lastEditor;
    
  }

  public Document(String id,
                  String name,
                  String title,
                  String path,
                  String workspace,
                  String state,
                  String author,
                  String lastEditor,
                  Calendar lastModified,
                  Calendar dateCreated) {
    this.id = id;
    this.title = title;
    this.path = path;
    this.workspace = workspace;
    this.state = state;
    this.author = author;
    this.lastEditor = lastEditor;
    this.lastModified = lastModified;
    this.dateCreated = dateCreated;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getLastEditor() {
    return lastEditor;
  }

  public void setLastEditor(String lastEditor) {
    this.lastEditor = lastEditor;
  }

  public Calendar getLastModified() {
    return lastModified;
  }

  public void setLastModified(Calendar lastModified) {
    this.lastModified = lastModified;
  }

  public Calendar getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Calendar dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the drive
   */
  public String getDrive() {
    return drive;
  }

  /**
   * @param drive the drive to set
   */
  public void setDrive(String drive) {
    this.drive = drive;
  }

  /**
   * @return the mimeType
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * @return the dateModified
   */
  public Date getDateModified() {
    return dateModified;
  }

  /**
   * @param dateModified the dateModified to set
   */
  public void setDateModified(Date dateModified) {
    this.dateModified = dateModified;
  }

  /**
   * @param mimeType the mimeType to set
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * @return the breadCrumb
   */
  public LinkedHashMap<String, String> getBreadCrumb() {
    return breadCrumb;
  }

  /**
   * @param breadCrumb the breadCrumb to set
   */
  public void setBreadCrumb(LinkedHashMap<String, String> breadCrumb) {
    this.breadCrumb = breadCrumb;
  }

  /**
   * @return the openUrl
   */
  public String getOpenUrl() {
    return openUrl;
  }

  /**
   * @param openUrl the openUrl to set
   */
  public void setOpenUrl(String openUrl) {
    this.openUrl = openUrl;
  }

  /**
   * @return the downloadUrl
   */
  public String getDownloadUrl() {
    return downloadUrl;
  }

  /**
   * @param downloadUrl the downloadUrl to set
   */
  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  /**
   * @return the version
   */
  public int getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * @return the size
   */
  public String getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(String size) {
    this.size = size;
  }
}
