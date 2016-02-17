/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.connector.fckeditor;

import java.text.SimpleDateFormat;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public class FCKFileHandler {

  private TemplateService templateService;

  private static final String[] IMAGE_MIMETYPE = {"image/gif", "image/jpeg", "image/bmp", "image/png", "image/tiff"};

  /**
   * Instantiates a new fCK file handler.
   *
   * @param container the container
   */
  public FCKFileHandler(ExoContainer container) {
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  /**
   * Gets the file type.
   *
   * @param node the node
   * @param resourceType the resource type
   * @return the file type
   * @throws Exception the exception
   */
  public String getFileType(final Node node, final String resourceType) throws Exception {
    if(FCKUtils.DOCUMENT_TYPE.equalsIgnoreCase(resourceType)) {
      return getDocumentType(node);
    }else if(FCKUtils.IMAGE_TYPE.equalsIgnoreCase(resourceType)) {
      return getImageType(node);
    }else if(FCKUtils.FLASH_TYPE.equalsIgnoreCase(resourceType)) {
      return getFlashType(node);
    }else if(FCKUtils.LINK_TYPE.equalsIgnoreCase(resourceType)) {
      return getLinkType(node);
    }
    return null;
  }

  /**
   * Gets the file url.
   *
   * @param file the file
   * @return the file url
   * @throws Exception the exception
   */
  protected String getFileURL(final Node file) throws Exception {
    return FCKUtils.createWebdavURL(file);
  }

  /**
   * Creates the file element for connector response looks like that {@code <File
   * name="" fileType="" dateCreated="" dateModified="" creator="" size=""
   * url="" />}.
   *
   * @param document the document
   * @param child the child
   * @param fileType the file type
   * @return the org.w3c.dom.Element element
   * @throws Exception the exception
   */
  public Element createFileElement(Document document, Node child, String fileType) throws Exception {
    Element file = document.createElement("File");
    file.setAttribute("name", child.getName());
    SimpleDateFormat formatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
    file.setAttribute("dateCreated", formatter.format(child.getProperty("exo:dateCreated").getDate().getTime()));
    file.setAttribute("dateModified", formatter.format(child.getProperty("exo:dateModified").getDate().getTime()));
    file.setAttribute("creator", child.getProperty("exo:owner").getString());
    file.setAttribute("fileType", fileType);
    file.setAttribute("url",getFileURL(child));
    if(child.isNodeType(FCKUtils.NT_FILE)) {
      long size = child.getNode("jcr:content").getProperty("jcr:data").getLength();
      file.setAttribute("size", "" + size / 1000);
    }else {
      file.setAttribute("size", "");
    }
    return file;
  }

  /**
   * Gets the document type.
   *
   * @param node the node
   * @return the document type
   * @throws Exception the exception
   */
  protected String getDocumentType(final Node node) throws Exception {
    if (node.isNodeType("exo:presentationable"))
      return node.getProperty("exo:presentationType").getString();
    String primaryType = node.getPrimaryNodeType().getName();
    if (templateService.getDocumentTemplates().contains(primaryType))
      return primaryType;
    return null;
  }

  /**
   * Gets the image type.
   *
   * @param node the node
   * @return the image type
   * @throws Exception the exception
   */
  protected String getImageType(final Node node) throws Exception {
    if(node.isNodeType("nt:file")) {
      String mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
      for(String s:IMAGE_MIMETYPE) {
        if(s.equals(mimeType)) {
          return node.getPrimaryNodeType().getName();
        }
      }
    }
    return null;
  }

  /**
   * Gets the flash type.
   *
   * @param node the node
   * @return the flash type
   * @throws Exception the exception
   */
  protected String getFlashType(final Node node) throws Exception {
    return null;
  }

  /**
   * Gets the link type.
   *
   * @param node the node
   * @return the link type
   * @throws Exception the exception
   */
  protected String getLinkType(final Node node) throws Exception {
    return "exo:link";
  }

}
