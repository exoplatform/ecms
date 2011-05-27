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
package org.exoplatform.ecm.connector.platform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieu.lai@exoplatform.com
 * 6 Apr 2011  
 */

@Path("/managedocument/")
public class ManageDocumentService implements ResourceContainer {
  
  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  /** The Constant LAST_MODIFIED_PROPERTY. */
  protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The cache control. */
  private final CacheControl    cc;

  /** The log. */
  private static Log            log                  = ExoLogger.getLogger(ManageDocumentService.class);

  /**
   * Instantiates a new platform document selector.
   *
   * @param params the params
   */
  public ManageDocumentService(InitParams params) {
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  /**
   * Gets the folders and files of node, node can be user root node or children node
   *
   * @param workspaceName the workspace name
   * @param nodePath the path to current node
   * @param isFolderOnly <code>true</code> if get folders only, otherwise get folders and files
   *
   * @return {@link Document} contains the folders and files
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/getFoldersAndFiles/")
  public Response getFoldersAndFiles(@QueryParam("workspaceName") String workspaceName,
                                     @QueryParam("nodePath") String nodePath,
                                     @QueryParam("isFolderOnly") boolean isFolderOnly){
    try {
      Node node = getNode(workspaceName, nodePath);
      return buildXMLResponseForChildren(node, isFolderOnly);
    } catch (AccessDeniedException e) {
      log.debug("Access is denied when perform get Folders and files: ", e);
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    }
    catch (PathNotFoundException e) {
      log.debug("Item is not found: ", e);
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      log.error("Repository is error: ", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();
      
    } catch (Exception e) {
      log.error("Error when perform get Folders and files: ", e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }
  
  /**
   * Delete a folder or a file.
   * 
   * @param workspaceName the workspace name
   * @param nodePath path to node to delete
   *
   * @return {@link Response} ok if item is deleted
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/deleteFolderOrFile/")
  public Response deleteFolderOrFile(@QueryParam("workspaceName") String workspaceName,
                                     @QueryParam("nodePath") String nodePath){
    try {     
      Node node = getNode(workspaceName, nodePath);
      Node parent = node.getParent();
      node.remove();
      parent.save();
      return Response.ok().cacheControl(cc).build();
    } catch (AccessDeniedException e) {
      log.debug("Access is denied when perform delete folder or file: ", e);
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    }
    catch (PathNotFoundException e) {
      log.debug("Item is not found: ", e);
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      log.error("Repository is error: ", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();
      
    } catch (Exception e) {
      log.error("Error when perform delete Folder or file: ", e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }
  
  /**
   * Create new folder.
   *  
   * @param workspaceName the workspace name
   * @param parentPath path to current folder
   * @param folderName the name of folder to create
   *
   * @return {@link Document} contains created folder
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/createFolder/")
  public Response createFolder(@QueryParam("workspaceName") String workspaceName,
                               @QueryParam("parentPath") String parentPath,
                               @QueryParam("folderName") String folderName) throws Exception {
    try {
      Node node = getNode(workspaceName, parentPath);
      Node newNode = node.addNode(Text.escapeIllegalJcrChars(folderName),
                                  NodetypeConstant.NT_UNSTRUCTURED);
      node.save();
      Document document = createNewDocument();
      Element folderNode = createFolderElement(document, newNode, workspaceName);
      document.appendChild(folderNode);
      return getResponse(document);
    } catch (AccessDeniedException e) {
      log.debug("Access is denied when perform create folder: ", e);
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    } catch (PathNotFoundException e) {
      log.debug("Item is not found: ", e);
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      log.error("Repository is error: ", e);
      return Response.status(Status.SERVICE_UNAVAILABLE)
                     .entity(e.getMessage())
                     .cacheControl(cc)
                     .build();

    } catch (Exception e) {
      log.error("Error when perform create folder: ", e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }  

  private Response buildXMLResponseForChildren(Node node, boolean isFolderOnly) throws Exception {
    Session session = node.getSession();
    String workspaceName = session.getWorkspace().getName();
    Document document = createNewDocument();
    Element rootElement = createFolderElement(document, node, workspaceName);
    Element folders = document.createElement("Folders");
    Element files = document.createElement("Files");
    Node sourceNode = null;
    Node referNode = null;
    for (NodeIterator iterator = node.getNodes(); iterator.hasNext();) {
      Node child = iterator.nextNode();
      if (child.isNodeType("exo:symlink") && child.hasProperty("exo:uuid") && child.hasProperty("exo:workspace")) {
        String sourceWs = child.getProperty("exo:workspace").getString();
        Session sourceSession = getSession(sourceWs);
        sourceNode = sourceSession.getNodeByUUID(child.getProperty("exo:uuid").getString());
      }
      referNode = sourceNode != null ? sourceNode : child;

      if (isFolder(referNode)) {
        Element folder = createFolderElement(document, referNode, 
                                             referNode.getSession().getWorkspace().getName());
        folders.appendChild(folder);
      } else   if (isFile(referNode)) {
        Element file = createFileElement(document, referNode, child, 
                                         referNode.getSession().getWorkspace().getName());
        files.appendChild(file);
      } else {
        continue;
      }
    }
    rootElement.appendChild(folders);
    if (!isFolderOnly) {
      rootElement.appendChild(files);
    }
    document.appendChild(rootElement);
    return getResponse(document);
  }

  private boolean isFolder(Node checkNode) throws RepositoryException {
    return checkNode.isNodeType(NodetypeConstant.NT_FOLDER)
        || checkNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED);
  }
  
  private boolean isFile(Node checkNode) throws RepositoryException {
    return checkNode.isNodeType(NodetypeConstant.NT_FILE);
  }

  private Element createFolderElement(Document document, Node child, String workspaceName) throws Exception {
    Element folder = document.createElement("Folder");
    boolean hasChild = false;
    for (NodeIterator iterator = child.getNodes(); iterator.hasNext();) {
      if (isFolder(iterator.nextNode())) {
        hasChild = true;
        break;
      }
    }
    folder.setAttribute("name", child.getName());
    folder.setAttribute("folderType", child.getPrimaryNodeType().getName());
    folder.setAttribute("path", child.getPath());
    folder.setAttribute("workspaceName", workspaceName);
    folder.setAttribute("hasChild", String.valueOf(hasChild));
    return folder;
  }

  private Element createFileElement(Document document,
                                    Node sourceNode,
                                    Node displayNode,
                                    String workspaceName) throws Exception {
    Element file = document.createElement("File");
    file.setAttribute("name", displayNode.getName());
    file.setAttribute("workspaceName", workspaceName);
    SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                                                                                         SimpleDateFormat.SHORT);
    file.setAttribute("dateCreated", formatter.format(sourceNode.getProperty("exo:dateCreated")
                                                                .getDate()
                                                                .getTime()));
    if (sourceNode.hasProperty("exo:dateModified")) {
      file.setAttribute("dateModified", formatter.format(sourceNode.getProperty("exo:dateModified")
                                                                   .getDate()
                                                                   .getTime()));
    } else {
      file.setAttribute("dateModified", null);
    }
    file.setAttribute("creator", sourceNode.getProperty("exo:owner").getString());
    file.setAttribute("path", displayNode.getPath());
    if (sourceNode.isNodeType("nt:file")) {
      Node content = sourceNode.getNode("jcr:content");
      file.setAttribute("nodeType", content.getProperty("jcr:mimeType").getString());
    } else {
      file.setAttribute("nodeType", sourceNode.getPrimaryNodeType().getName());
    }
    
    long size = sourceNode.getNode("jcr:content").getProperty("jcr:data").getLength();
    file.setAttribute("size", "" + size);
    return file;
  }  

  private Node getNode(String workspaceName, String nodePath) throws Exception {
    Session session = getSession(workspaceName);
    Node node;
    if (nodePath.trim().equals("/")) {
      node = session.getRootNode();
    } else {
      NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
      if (nodePath.indexOf("${userId}") > -1) {
        String userId = Util.getPortalRequestContext().getRemoteUser();
        String rootTreeOfSpecialDriver = Utils.getPersonalDrivePath(nodePath , userId);
        nodePath = rootTreeOfSpecialDriver;
      }
      node = (Node) nodeFinder.getItem(workspaceName, nodePath);
    }
    return node;
  }
  
  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    ManageableRepository manageableRepository = getCurrentRepository();
    return sessionProvider.getSession(workspaceName, manageableRepository);
  }
  
  private Document createNewDocument() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.newDocument();
  }
  
  private ManageableRepository getCurrentRepository() throws RepositoryException {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    return repositoryService.getCurrentRepository();
  }
  
  private <T> T getApplicationComponent(Class<T> type) {
    return type.cast(ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(type));
  }
  
  private Response getResponse(Document document) {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                   .cacheControl(cc)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
}
