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
package org.exoplatform.services.wcm.search.base;

import org.exoplatform.commons.api.search.data.SearchResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 8, 2013  
 */
public class EcmsSearchResult extends SearchResult {

  private String fileType;
  private String nodePath;
  private List<String> tags;
  private Map<String, List<String>> breadcrumb;
  private LinkedHashMap<String, String> previewBreadcrumb;
  private String id;
  private String downloadUrl;
  private String version;
  private String size;
  private String drive;
  private String lastEditor;

  public EcmsSearchResult(String url,
                          String urlOnImage,
                          String title,
                          String excerpt,
                          String detail,
                          String imageUrl,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath) {
    this(url, urlOnImage, title, excerpt, detail, imageUrl, date, relevancy, fileType, nodePath, null);
  }

  public EcmsSearchResult(String url,
                          String urlOnImage,
                          String title,
                          List<String> tags,
                          String excerpt,
                          String detail,
                          String imageUrl,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath) {
    this(url, urlOnImage, title, excerpt, detail, imageUrl, date, relevancy, fileType, nodePath, null, tags);
  }

  public EcmsSearchResult(String url,
                          String urlOnImage,
                          String title,
                          String excerpt,
                          String detail,
                          String imageUrl,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath,
                          Map<String, List<String>> breadcrumb,
                          List<String> tags) {
    super(url, urlOnImage, title, excerpt, detail, imageUrl, date, relevancy);
    this.fileType = fileType;
    this.nodePath = nodePath;
    this.breadcrumb = breadcrumb;
    this.tags = tags;
  }

  public EcmsSearchResult(String url,
                          String urlOnImage,
                          String title,
                          String excerpt,
                          String detail,
                          String imageUrl,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath,
                          Map<String, List<String>> breadcrumb) {
    super(url, urlOnImage, title, excerpt, detail, imageUrl, date, relevancy);
    this.fileType = fileType;
    this.nodePath = nodePath;
    this.breadcrumb = breadcrumb;
  }
  
  public EcmsSearchResult(String url,
                          String title,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath,
                          LinkedHashMap<String, String> previewBreadcrumb,
                          String id,
                          String downloadUrl,
                          String version,
                          String size,
                          String drive,
                          String lastEditor) {
    super(url, title, null, null, null, date, relevancy);
    this.fileType = fileType;
    this.nodePath = nodePath;
    this.previewBreadcrumb = previewBreadcrumb;
    this.id = id;
    this.downloadUrl = downloadUrl;
    this.version = version;
    this.size = size;
    this.drive = drive;
    this.lastEditor = lastEditor;
  }

  public String getFileType() {
    return this.fileType;
  }
  
  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public List<String> getTags() {
    return this.tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getNodePath() {
    return nodePath;
  }
    
  public void setNodePath(String nodePath) {
    this.nodePath = nodePath;
  }

  public Map<String, List<String>> getBreadcrumb() {
    return breadcrumb;
  }

  public void setBreadcrumb(Map<String, List<String>> breadcrumb) {
    this.breadcrumb = breadcrumb;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
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
  public String getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
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
   * @return the lastEditor
   */
  public String getLastEditor() {
    return lastEditor;
  }

  /**
   * @param lastEditor the lastEditor to set
   */
  public void setLastEditor(String lastEditor) {
    this.lastEditor = lastEditor;
  }

  /**
   * @return the previewBreadcrumb
   */
  public LinkedHashMap<String, String> getPreviewBreadcrumb() {
    return previewBreadcrumb;
  }

  /**
   * @param previewBreadcrumb the previewBreadcrumb to set
   */
  public void setPreviewBreadcrumb(LinkedHashMap<String, String> previewBreadcrumb) {
    this.previewBreadcrumb = previewBreadcrumb;
  }
}
