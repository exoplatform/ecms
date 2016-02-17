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
package org.exoplatform.wcm.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.fckeditor.FCKFileHandler;
import org.exoplatform.ecm.connector.fckeditor.FCKFolderHandler;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Sep 10, 2008
 */
/**
 * The Class BaseConnector.
 */
public abstract class BaseConnector {

  /** The folder handler. */
  protected FCKFolderHandler                  folderHandler;

  /** The file handler. */
  protected FCKFileHandler                    fileHandler;

  /** The file upload handler. */
  protected FileUploadHandler                 fileUploadHandler;

  /** The repository service. */
  protected RepositoryService                 repositoryService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(BaseConnector.class.getName());

  /** The voting service. */
  protected VotingService votingService;

  /** The link manager. */
  protected LinkManager linkManager;

  /** The live portal manager service. */
  protected LivePortalManagerService          livePortalManagerService;

  /** The web schema config service. */
  protected WebSchemaConfigService            webSchemaConfigService;

  /** The Constant LAST_MODIFIED_PROPERTY. */
  protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  /**
   * Gets the root content storage.
   *
   * @param node the node
   * @return the root content storage
   * @throws Exception the exception
   */
  protected abstract Node getRootContentStorage(Node node) throws Exception;

  /**
   * Gets the content storage type.
   *
   * @return the content storage type
   * @throws Exception the exception
   */
  protected abstract String getContentStorageType() throws Exception;

  /**
   * Instantiates a new base connector.
   */
  public BaseConnector() {
    livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    webSchemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    votingService = WCMCoreUtils.getService(VotingService.class);
    linkManager = WCMCoreUtils.getService(LinkManager.class);

    ExoContainer container = ExoContainerContext.getCurrentContainer();
    folderHandler = new FCKFolderHandler(container);
    fileHandler = new FCKFileHandler(container);
    fileUploadHandler = new FileUploadHandler();
  }

  /**
   * Builds the xml response on expand.
   *
   * @param currentFolder the current folder
   * @param runningPortal The current portal instance
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @param command the command
   * @return the response
   * @throws Exception the exception
   */
  protected Response buildXMLResponseOnExpand(String currentFolder,
                                              String runningPortal,
                                              String workspaceName,
                                              String jcrPath,
                                              String command) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node sharedPortalNode = livePortalManagerService.getLiveSharedPortal(sessionProvider);
    Node activePortalNode = getCurrentPortalNode(jcrPath,
                                                 runningPortal,
                                                 sharedPortalNode);
    if (currentFolder.length() == 0 || "/".equals(currentFolder))
      return buildXMLResponseForRoot(activePortalNode, sharedPortalNode, command);
    String currentPortalRelPath = "/" + activePortalNode.getName() + "/";
    String sharePortalRelPath = "/" + sharedPortalNode.getName() + "/";
    Node webContent = getWebContent(workspaceName, jcrPath);
    if (!activePortalNode.getPath().equals(sharedPortalNode.getPath())
        && currentFolder.startsWith(sharePortalRelPath)) {
      if (currentFolder.equals(sharePortalRelPath)) {
        return buildXMLResponseForPortal(sharedPortalNode, null, command);
      }
      Node currentContentStorageNode = getCorrectContentStorage(sharedPortalNode,
          null,
          currentFolder);
      return buildXMLResponseForContentStorage(currentContentStorageNode, command);
    } else if (!activePortalNode.getPath().equals(sharedPortalNode.getPath())
        && currentFolder.startsWith(currentPortalRelPath)) {
      return buildXMLResponseCommon(activePortalNode, webContent, currentFolder, command);
    } else {
      return buildXMLResponseCommon(sharedPortalNode, webContent, currentFolder, command);
    }
  }

  /**
   * Builds the xml response common.
   *
   * @param activePortal the active portal
   * @param webContent the web content
   * @param currentFolder the current folder
   * @param command the command
   * @return the response
   * @throws Exception the exception
   */
  protected Response buildXMLResponseCommon(Node activePortal,
                                            Node webContent,
                                            String currentFolder,
                                            String command) throws Exception {
    String activePortalRelPath = "/" + activePortal.getName() + "/";
    if (currentFolder.equals(activePortalRelPath))
      return buildXMLResponseForPortal(activePortal, webContent, command);
    if (webContent != null) {
      String webContentRelPath = activePortalRelPath + webContent.getName() + "/";
      if (currentFolder.startsWith(webContentRelPath)) {
        if (currentFolder.equals(webContentRelPath))
          return buildXMLResponseForPortal(webContent, null, command);
        Node contentStorageOfWebContent = getCorrectContentStorage(activePortal,
                                                                   webContent,
                                                                   currentFolder);
        return buildXMLResponseForContentStorage(contentStorageOfWebContent, command);
      }
    }
    Node correctContentStorage = getCorrectContentStorage(activePortal, null, currentFolder);
    return buildXMLResponseForContentStorage(correctContentStorage, command);
  }

  /**
   * Builds the xml response for root.
   *
   * @param currentPortal the current portal
   * @param sharedPortal the shared portal
   * @param command the command
   * @return the response
   * @throws Exception the exception
   */
  protected Response buildXMLResponseForRoot(Node currentPortal, Node sharedPortal, String command) throws Exception {
    Document document = null;
    Node rootNode = currentPortal.getSession().getRootNode();
    Element rootElement = FCKUtils.createRootElement(command,
                                                     rootNode,
                                                     rootNode.getPrimaryNodeType().getName());
    document = rootElement.getOwnerDocument();
    Element folders = document.createElement("Folders");
    Element files = document.createElement("Files");
    Element sharedPortalElement = null;
    Element currentPortalElement = null;
    if (sharedPortal != null) {
      sharedPortalElement = folderHandler.createFolderElement(document,
                                                              sharedPortal,
                                                              sharedPortal.getPrimaryNodeType()
                                                                          .getName());
      folders.appendChild(sharedPortalElement);
    }
    if (currentPortal != null && !currentPortal.getPath().equals(sharedPortal.getPath())) {
      currentPortalElement = folderHandler.createFolderElement(document,
                                                               currentPortal,
                                                               currentPortal.getPrimaryNodeType()
                                                                            .getName());
      folders.appendChild(currentPortalElement);
    }
    rootElement.appendChild(folders);
    rootElement.appendChild(files);
    return getResponse(document);
  }

  /**
   * Builds the xml response for portal.
   *
   * @param node the node
   * @param webContent the web content
   * @param command the command
   * @return the response
   * @throws Exception the exception
   */
  protected Response buildXMLResponseForPortal(Node node, Node webContent, String command) throws Exception {
    Node storageNode = getRootContentStorage(node);
    Element rootElement = FCKUtils.createRootElement(command,
                                                     node,
                                                     folderHandler.getFolderType(node));
    Document document = rootElement.getOwnerDocument();
    Element folders = document.createElement("Folders");
    Element files = document.createElement("Files");
    Element storageElement = folderHandler.createFolderElement(document,
                                                               storageNode,
                                                               storageNode.getPrimaryNodeType()
                                                                          .getName());
    folders.appendChild(storageElement);
    Element webContentElement = null;
    if (webContent != null) {
      webContentElement = folderHandler.createFolderElement(document,
                                                            webContent,
                                                            webContent.getPrimaryNodeType()
                                                                      .getName());
      folders.appendChild(webContentElement);
    }
    rootElement.appendChild(folders);
    rootElement.appendChild(files);
    return getResponse(document);
  }

  /**
   * Builds the xml response for content storage.
   *
   * @param node the node
   * @param command the command
   * @return the response
   * @throws Exception the exception
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
      String fileType = fileHandler.getFileType(child, sourceType);
      if (fileType != null) {
        Element file = fileHandler.createFileElement(document, child, fileType);
        files.appendChild(file);
      }
    }
    rootElement.appendChild(folders);
    rootElement.appendChild(files);
    return getResponse(document);
  }

  protected Node getCorrectContentStorage(Node activePortal, Node webContent, String currentFolder) throws Exception {
    if (currentFolder == null || currentFolder.trim().length() == 0)
      return null;
    Node rootContentStorage = null;
    String rootContentStorageRelPath = null;
    if (activePortal != null && webContent == null) {
      rootContentStorage = getRootContentStorage(activePortal);
      rootContentStorageRelPath = "/" + activePortal.getName() + "/" + rootContentStorage.getName()
          + "/";
    } else if (activePortal != null && webContent != null) {
      rootContentStorage = getRootContentStorage(webContent);
      rootContentStorageRelPath = "/" + activePortal.getName() + "/" + webContent.getName() + "/"
          + rootContentStorage.getName() + "/";
    }
    if (currentFolder.equals(rootContentStorageRelPath))
      return rootContentStorage;
    try {
      String correctStorageRelPath = currentFolder.replace(rootContentStorageRelPath, "");
      correctStorageRelPath = correctStorageRelPath.substring(0, correctStorageRelPath.length() - 1);
      return rootContentStorage.getNode(correctStorageRelPath);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the response.
   *
   * @param document the document
   * @return the response
   */
  protected Response getResponse(Document document) {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  /**
   * Gets the jcr content.
   *
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @return the jcr content
   * @throws Exception the exception
   */
  protected Node getContent(String workspaceName,
                            String jcrPath,
                            String NodeTypeFilter,
                            boolean isSystemSession) throws Exception {
    if (jcrPath == null || jcrPath.trim().length() == 0)
      return null;
    try {
      SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider()
                                                       : WCMCoreUtils.getUserSessionProvider();
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspaceName, repository);
      Node content = (Node) session.getItem(jcrPath);
      if (content.isNodeType("exo:taxonomyLink")) {
        content = linkManager.getTarget(content);
      }

      if (NodeTypeFilter==null || (NodeTypeFilter!=null && content.isNodeType(NodeTypeFilter)) )
        return content;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform getContent: ", e);
      }
    }
    return null;
  }
  
  /**
   * Gets the jcr content.
   *
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @return the jcr content
   * @throws Exception the exception
   */
  protected Node getContent(String workspaceName, String jcrPath) throws Exception {
    return getContent(workspaceName, jcrPath, null, true);
  }  

  /**
   * Gets the web content.
   *
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @return the web content
   * @throws Exception the exception
   */
  protected Node getWebContent(String workspaceName, String jcrPath) throws Exception {
    return getContent(workspaceName, jcrPath, "exo:webContent", true);
  }

  protected Node getCurrentPortalNode(String jcrPath,
                                      String runningPortal,
                                      Node sharedPortal) throws Exception {
    if (jcrPath == null || jcrPath.length() == 0)
      return null;
    Node currentPortal = null;
    List<Node> livePortaNodes = new ArrayList<Node>();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      livePortaNodes = livePortalManagerService.getLivePortals(sessionProvider);
      if (sharedPortal != null)
        livePortaNodes.add(sharedPortal);
      for (Node portalNode : livePortaNodes) {
        String portalPath = portalNode.getPath();
        if (jcrPath.startsWith(portalPath))
          currentPortal = portalNode;
      }
      if (currentPortal == null)
        currentPortal = livePortalManagerService.getLivePortal(sessionProvider, runningPortal);
      return currentPortal;
    } catch (Exception e) {
      return null;
    }
  }

}
