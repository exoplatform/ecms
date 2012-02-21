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
package org.exoplatform.wcm.connector.fckeditor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.BaseConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Jun 24, 2008
 */
@Path("/wcmLink/")
public class LinkConnector extends BaseConnector implements ResourceContainer {

  /** The link file handler. */
  private LinkFileHandler linkFileHandler;

  /** The log. */
  private static Log log = ExoLogger.getLogger(LinkFileHandler.class);

  /**
   * Instantiates a new link connector.
   *
   * @param container the container
   */
  public LinkConnector() {
    linkFileHandler = new LinkFileHandler();
  }

  /**
   * Gets the folders and files.
   *
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @param currentFolder the current folder
   * @param command the command
   * @param type the type
   * @param currentPortal the current portal
   *
   * @return the folders and files
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/getFoldersAndFiles/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response getFoldersAndFiles(@QueryParam("repositoryName") String repositoryName,
                                     @QueryParam("workspaceName") String workspaceName,
                                     @QueryParam("jcrPath") String jcrPath,
                                     @QueryParam("currentFolder") String currentFolder,
                                     @QueryParam("currentPortal") String currentPortal,
                                     @QueryParam("command") String command,
                                     @QueryParam("type") String type) throws Exception {
    try {
      Response response = buildXMLResponseOnExpand(currentFolder, currentPortal, workspaceName,
                                                   repositoryName, jcrPath, command);
      if (response != null)
        return response;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform getFoldersAndFiles: ", e);
      }
    }
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.wcm.connector.fckeditor.BaseConnector#buildXMLResponseOnExpand
   * (java.lang.String, java.lang.String, java.lang.String, java.lang.String,
   * java.lang.String)
   */
  protected Response buildXMLResponseOnExpand(String currentFolder,
                                              String runningPortal,
                                              String workspaceName,
                                              String repositoryName,
                                              String jcrPath,
                                              String command) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node sharedPortal = livePortalManagerService.getLiveSharedPortal(sessionProvider, repositoryName);
    Node currentPortalNode = getCurrentPortalNode(repositoryName,
                                                  jcrPath,
                                                  runningPortal,
                                                  sharedPortal);
    if (currentFolder.length() == 0 || "/".equals(currentFolder))
      return buildXMLResponseForRoot(currentPortalNode, sharedPortal, command);
    String currentPortalRelPath = "/" + currentPortalNode.getName() + "/";
    String sharePortalRelPath = "/" + sharedPortal.getName() + "/";
    if (!currentPortalNode.getPath().equals(sharedPortal.getPath())
        && currentFolder.startsWith(sharePortalRelPath)) {
      if (currentFolder.equals(sharePortalRelPath)) {
        return buildXMLResponseForPortal(sharedPortal, null, command);
      } else {
        Node currentContentStorageNode = getCorrectContentStorage(sharedPortal, null, currentFolder);
        return buildXMLResponseForContentStorage(currentContentStorageNode, command);
      }
    } else if (!currentPortalNode.getPath().equals(sharedPortal.getPath())
        && currentFolder.startsWith(currentPortalRelPath)) {
      return buildXMLResponseCommon(currentPortalNode, null, currentFolder, command);
    } else {
      return buildXMLResponseCommon(sharedPortal, null, currentFolder, command);
    }
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.wcm.connector.fckeditor.BaseConnector#
   * buildXMLResponseForContentStorage(javax.jcr.Node, java.lang.String)
   */
  protected Response buildXMLResponseForContentStorage(Node node, String command) throws Exception {
    Element rootElement = FCKUtils.createRootElement(command,
                                                     node,
                                                     folderHandler.getFolderType(node));
    Document document = rootElement.getOwnerDocument();
    Element folders = document.createElement("Foders");
    Element files = document.createElement("Files");
    for (NodeIterator iterator = node.getNodes(); iterator.hasNext();) {
      Node child = iterator.nextNode();
      if (child.isNodeType(FCKUtils.EXO_HIDDENABLE))
        continue;
      String folderType = folderHandler.getFolderType(child);
      if (folderType != null) {
        Element folder = folderHandler.createFolderElement(document, child, folderType);
        folders.appendChild(folder);
      }
      String sourceType = getContentStorageType();
      String fileType = linkFileHandler.getFileType(child, sourceType);
      if (fileType != null) {
        Element file = linkFileHandler.createFileElement(document, child, fileType);
        files.appendChild(file);
      }
    }
    rootElement.appendChild(folders);
    rootElement.appendChild(files);
    return getResponse(document);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.wcm.connector.fckeditor.BaseConnector#getRootContentStorage
   * (javax.jcr.Node)
   */
  @Override
  protected Node getRootContentStorage(Node parentNode) throws Exception {
    try {
      PortalFolderSchemaHandler folderSchemaHandler = webSchemaConfigService.
          getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
      return folderSchemaHandler.getLinkFolder(parentNode);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug(e);
      }
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.wcm.connector.fckeditor.BaseConnector#getContentStorageType
   * ()
   */
  @Override
  protected String getContentStorageType() throws Exception {
    return FCKUtils.LINK_TYPE;
  }
}
