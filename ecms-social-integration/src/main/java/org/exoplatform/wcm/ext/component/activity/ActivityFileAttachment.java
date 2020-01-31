/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.activity;

import javax.jcr.Node;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.wcm.core.NodeLocation;

public class ActivityFileAttachment {

  private String             contentName;

  private String             imagePath;

  private String             mimeType;

  private String             nodeUUID;

  private String             state;

  private String             contentLink;

  private String             author;

  private String             dateCreated;

  private String             lastModified;

  private Node               contentNode;

  private NodeLocation       nodeLocation;

  private DriveData          docDrive;

  private String             docTypeName;

  private String             docTitle;

  private String             docVersion;

  private String             docSummary;

  public String              docPath;

  public String              repository;

  public String              workspace;

  private Boolean            isSymlink;

  private String             webdavURL;

  public String getContentName() {
    return contentName;
  }

  public ActivityFileAttachment setContentName(String contentName) {
    this.contentName = contentName;
    return this;
  }

  public String getImagePath() {
    return imagePath;
  }

  public ActivityFileAttachment setImagePath(String imagePath) {
    this.imagePath = imagePath;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public ActivityFileAttachment setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public String getNodeUUID() {
    return nodeUUID;
  }

  public ActivityFileAttachment setNodeUUID(String nodeUUID) {
    this.nodeUUID = nodeUUID;
    return this;
  }

  public String getState() {
    return state;
  }

  public ActivityFileAttachment setState(String state) {
    this.state = state;
    return this;
  }

  public String getContentLink() {
    return contentLink;
  }

  public ActivityFileAttachment setContentLink(String contentLink) {
    this.contentLink = contentLink;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public ActivityFileAttachment setAuthor(String author) {
    this.author = author;
    return this;
  }

  public String getDateCreated() {
    return dateCreated;
  }

  public ActivityFileAttachment setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
    return this;
  }

  public String getLastModified() {
    return lastModified;
  }

  public ActivityFileAttachment setLastModified(String lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public Node getContentNode() {
    return contentNode;
  }

  public ActivityFileAttachment setContentNode(Node contentNode) {
    this.contentNode = contentNode;
    return this;
  }

  public NodeLocation getNodeLocation() {
    if (nodeLocation == null && StringUtils.isNotBlank(repository) && StringUtils.isNotBlank(workspace)
        && (StringUtils.isNotBlank(docPath) || StringUtils.isNotBlank(nodeUUID))) {
      nodeLocation = new NodeLocation(repository, workspace, docPath, nodeUUID, false);
    }
    return nodeLocation;
  }

  public ActivityFileAttachment setNodeLocation(NodeLocation nodeLocation) {
    this.nodeLocation = nodeLocation;
    return this;
  }

  public DriveData getDocDrive() {
    return docDrive;
  }

  public ActivityFileAttachment setDocDrive(DriveData docDrive) {
    this.docDrive = docDrive;
    return this;
  }

  public String getDocTypeName() {
    return docTypeName;
  }

  public ActivityFileAttachment setDocTypeName(String docTypeName) {
    this.docTypeName = docTypeName;
    return this;
  }

  public String getDocTitle() {
    return docTitle;
  }

  public ActivityFileAttachment setDocTitle(String docTitle) {
    this.docTitle = docTitle;
    return this;
  }

  public String getDocVersion() {
    return docVersion;
  }

  public ActivityFileAttachment setDocVersion(String docVersion) {
    this.docVersion = docVersion;
    return this;
  }

  public String getDocSummary() {
    return docSummary;
  }

  public ActivityFileAttachment setDocSummary(String docSummary) {
    this.docSummary = docSummary;
    return this;
  }

  public String getDocPath() {
    return docPath == null ? (nodeLocation == null ? null : nodeLocation.getPath()) : docPath;
  }

  public ActivityFileAttachment setDocPath(String docPath) {
    this.docPath = docPath;
    return this;
  }

  public String getRepository() {
    return StringUtils.isBlank(repository) ? (nodeLocation == null ? null : nodeLocation.getRepository()) : repository;
  }

  public ActivityFileAttachment setRepository(String repository) {
    this.repository = repository;
    return this;
  }

  public String getWorkspace() {
    return StringUtils.isBlank(workspace) ? (nodeLocation == null ? null : nodeLocation.getWorkspace()) : workspace;
  }

  public ActivityFileAttachment setWorkspace(String workspace) {
    this.workspace = workspace;
    return this;
  }

  public boolean isSymlink() {
    return isSymlink == null ? false : isSymlink;
  }

  public ActivityFileAttachment setSymlink(Boolean isSymlink) {
    this.isSymlink = isSymlink;
    return this;
  }

  public void setWebdavURL(String webdavURL) {
    this.webdavURL = webdavURL;
  }

  public String getWebdavURL() {
    return webdavURL;
  }
}
