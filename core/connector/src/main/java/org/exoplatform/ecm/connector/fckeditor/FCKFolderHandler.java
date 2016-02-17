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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public class FCKFolderHandler {
  private TemplateService templateService;

  private FCKMessage      fckMessage;

  public FCKFolderHandler(ExoContainer container) {
    templateService = WCMCoreUtils.getService(TemplateService.class);
    fckMessage = new FCKMessage();
  }

  public String getFolderType(final Node node) throws Exception {
    // need use a service to get extended folder type for the node
    NodeType nodeType = node.getPrimaryNodeType();
    String primaryType = nodeType.getName();
    if (templateService.getDocumentTemplates().contains(primaryType))
      return null;
    if (FCKUtils.NT_UNSTRUCTURED.equals(primaryType) || FCKUtils.NT_FOLDER.equals(primaryType))
      return primaryType;
    if (nodeType.isNodeType(FCKUtils.NT_UNSTRUCTURED) || nodeType.isNodeType(FCKUtils.NT_FOLDER)) {
      // check if the nodetype is exo:videoFolder...
      return primaryType;
    }
    return primaryType;
  }

  public String getFolderURL(final Node folder) throws Exception {
    return FCKUtils.createWebdavURL(folder);
  }

  /**
   * Creates the folder element for connector response look like {@code <folder name=""
   * url="" folderType="" />}
   *
   * @param document the document
   * @param child the child
   * @param folderType the folder type
   * @return the org.w3c.dom.Element element
   * @throws Exception the exception
   */
  public Element createFolderElement(Document document, Node child, String folderType)
      throws Exception {
    Element folder = document.createElement("Folder");
    folder.setAttribute("name", child.getName());
    folder.setAttribute("url", getFolderURL(child));
    folder.setAttribute("folderType", folderType);
    return folder;
  }

  public Response createNewFolder(Node currentNode, String newFolderName, String language)
      throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    Document document = null;
    DateFormat dateFormat = new SimpleDateFormat(FCKUtils.IF_MODIFIED_SINCE_DATE_FORMAT);
    if (currentNode != null) {
      if (!FCKUtils.hasAddNodePermission(currentNode)) {
        Object[] args = { currentNode.getPath() };
        document = fckMessage.createMessage(FCKMessage.FOLDER_PERMISSION_CREATING, FCKMessage.ERROR,
            language, args);
        return Response.ok(document, new MediaType("text", "xml"))
                       .cacheControl(cacheControl)
                       .header(FCKUtils.LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      }
      if (currentNode.hasNode(newFolderName)) {
        Object[] args = { currentNode.getPath(), newFolderName };
        document = fckMessage.createMessage(FCKMessage.FOLDER_EXISTED, FCKMessage.ERROR, language,
            args);
        return Response.ok(document, new MediaType("text", "xml"))
                       .cacheControl(cacheControl)
                       .header(FCKUtils.LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      }
      currentNode.addNode(newFolderName, FCKUtils.NT_FOLDER);
      currentNode.getSession().save();

      Element rootElement = FCKUtils.createRootElement("createFolder", currentNode,
          getFolderType(currentNode));
      document = rootElement.getOwnerDocument();
      Element errorElement = document.createElement("Message");
      errorElement.setAttribute("number", Integer.toString(FCKMessage.FOLDER_CREATED));
      errorElement.setAttribute("text", fckMessage.getMessage(FCKMessage.FOLDER_CREATED, null,
          language));
      errorElement.setAttribute("type", FCKMessage.ERROR);
      rootElement.appendChild(errorElement);
      return Response.ok(document, new MediaType("text", "xml"))
                     .cacheControl(cacheControl)
                     .header(FCKUtils.LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    }

    document = fckMessage.createMessage(FCKMessage.FOLDER_NOT_CREATED, FCKMessage.ERROR, language, null);
    return Response.ok(document, new MediaType("text", "xml"))
                   .cacheControl(cacheControl)
                   .header(FCKUtils.LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
}
