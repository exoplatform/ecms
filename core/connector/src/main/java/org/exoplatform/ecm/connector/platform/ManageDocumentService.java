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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.FileUploadHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ManageDocumentService.java <br />
 *
 * The service which is used to perform some actions on a folder or a file, 
 * such as creating, deleting a folder/file, or uploading a file.
 * 
 * See methods for more api details.
 * GET: {portalname}/{restcontextname}/managedocument/getDrives/ <br /> 	
 * GET: {portalname}/{restcontextname}/managedocument/getFoldersAndFiles/ <br /> 	
 * GET: {portalname}/{restcontextname}/managedocument/createFolder/ <br /> 	
 * GET: {portalname}/{restcontextname}/managedocument/deleteFolderOrFile/ <br />} 	
 * GET: {portalname}/{restcontextname}/managedocument/uploadFile/upload/ <br /> 	
 * GET: {portalname}/{restcontextname}/managedocument/uploadFile/control/ <br />
 *     
 * @author Lai Trung Hieu <hieu.lai@exoplatform.com>
 * @since      6 Apr 2011
 * @copyright  eXo Platform SEA
 * 
 * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService
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
  
  private ManageDriveService    manageDriveService;
  
  private LinkManager linkManager;
  
  private NodeHierarchyCreator nodeHierarchyCreator;
  
  /** The file upload handler. */
  protected FileUploadHandler   fileUploadHandler;
  
  private enum DriveType {
    GENERAL, GROUP, PERSONAL
  }
  
  final static public String   EXO_MUSICFOLDER      = "exo:musicFolder";

  final static public String   EXO_VIDEOFOLDER      = "exo:videoFolder";

  final static public String   EXO_PICTUREFOLDER    = "exo:pictureFolder";

  final static public String   EXO_DOCUMENTFOLDER   = "exo:documentFolder";

  final static public String   EXO_SEARCHFOLDER     = "exo:searchFolder";

  final static public String   EXO_SYMLINK          = "exo:symlink";

  final static public String   EXO_PRIMARYTYPE      = "exo:primaryType";

  final static public String   EXO_TRASH_FOLDER     = "exo:trashFolder";

  final static public String   EXO_FAVOURITE_FOLDER = "exo:favoriteFolder";

  final static public String   NT_UNSTRUCTURED      = "nt:unstructured";

  final static public String   NT_FOLDER            = "nt:folder";
  
  final static public String[] SPECIFIC_FOLDERS = { EXO_MUSICFOLDER,
    EXO_VIDEOFOLDER, EXO_PICTUREFOLDER, EXO_DOCUMENTFOLDER, EXO_SEARCHFOLDER };

  private static final String  PRIVATE              = "Private";
  /**
   * Instantiates a new platform document selector.
   *
   * @param manageDriveService 
   */
  public ManageDocumentService(ManageDriveService manageDriveService, LinkManager linkManager,
                               NodeHierarchyCreator nodeHierarchyCreator) {
    this.manageDriveService = manageDriveService;
    this.linkManager = linkManager;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    fileUploadHandler = new FileUploadHandler();
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }
  
  /**
   * Get all drives by type (General, Group or Personal drive). 
   * 
   * @param driveType types of drive (General, Group, or Personal)
   * @param showPrivate show the Private drive or not. The default value is false
   * @return {@link Document} contains the drives
   * 
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.getDrives
   */
  @GET
  @Path("/getDrives/")
  @RolesAllowed("users")
  public Response getDrives(@QueryParam("driveType") String driveType,
                            @DefaultValue("false") @QueryParam("showPrivate") String showPrivate,
                            @DefaultValue("false") @QueryParam("showPrivate") String showPersonal) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    List<String> userRoles = getMemberships();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();

    Element rootElement = document.createElement("Folders");
    document.appendChild(rootElement);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    Set<String> groups = ConversationState.getCurrent().getIdentity().getGroups();
    List<DriveData> driveList = new ArrayList<DriveData>();
    if (DriveType.GENERAL.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getMainDrives(userId, userRoles);
    } else if (DriveType.GROUP.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getGroupDrives(userId, userRoles, new ArrayList<String>(groups));
    } else if (DriveType.PERSONAL.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getPersonalDrives(userId, userRoles);
    //remove Private drive
      String privateDrivePath = "";
      for (DriveData driveData : driveList) {
        if (PRIVATE.equals(driveData.getName())) {
          privateDrivePath = driveData.getHomePath();
          if (!Boolean.valueOf(showPrivate)) {
            driveList.remove(driveData);
            break;
          }
        }
      }
      //remove Personal Documents drive
      if (!Boolean.valueOf(showPersonal)) {
        for (DriveData driveData : driveList) {
          if (privateDrivePath.equals(driveData.getHomePath())) {
            driveList.remove(driveData);
            break;
          }
        }
      }      
    }
    rootElement.appendChild(buildXMLDriveNodes(document, driveList, driveType));
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cc).build();
  }

  /**
   * Get all folders and files which the current user can view.
   *
   * @param driveName the drive name 
   * @param workspaceName the workspace name
   * @param currentFolder the path to the folder to achieve its folders and file
   * @param showHidden show hidden items or not. The default value is false 
   *
   * @return {@link Document} contains the folders and files
   *
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.getFoldersAndFiles
   */
  @GET
  @Path("/getFoldersAndFiles/")
  @RolesAllowed("users")
  public Response getFoldersAndFiles(@QueryParam("driveName") String driveName,
                                     @QueryParam("workspaceName") String workspaceName,
                                     @QueryParam("currentFolder") String currentFolder,
                                     @DefaultValue("false") @QueryParam("showHidden") String showHidden) {
    try {
      Node node = getNode(driveName, workspaceName, currentFolder);
      return buildXMLResponseForChildren(node, driveName, currentFolder, Boolean.valueOf(showHidden));
    } catch (AccessDeniedException e) {
      if (log.isDebugEnabled()) {
        log.debug("Access is denied when perform get Folders and files: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    }
    catch (PathNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (log.isErrorEnabled()) {
        log.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();
      
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform get Folders and files: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }
  
  /**
   * Delete a folder or a file.
   * 
   * @param driveName the drive name
   * @param workspaceName the workspace name
   * @param itemPath the path to the folder/file. 
   *
   * @return {@link Response} ok if item is deleted
   *
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.deleteFolderOrFile
   */
  @GET
  @Path("/deleteFolderOrFile/")
  @RolesAllowed("users")
  public Response deleteFolderOrFile(@QueryParam("driveName") String driveName,
                                     @QueryParam("workspaceName") String workspaceName,
                                     @QueryParam("itemPath") String itemPath){
    try {     
      Node node = getNode(driveName, workspaceName, itemPath);
      Node parent = node.getParent();
      node.remove();
      parent.save();
      return Response.ok().cacheControl(cc).build();
    } catch (AccessDeniedException e) {
      if (log.isDebugEnabled()) {
        log.debug("Access is denied when perform delete folder or file: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    }
    catch (PathNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (log.isErrorEnabled()) {
        log.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();
      
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform delete Folder or file: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }
  
  /**
   * Create a new folder and return its information.
   *  
   * @param driveName the drive name
   * @param workspaceName the workspace name
   * @param currentFolder the path to the folder to which a child folder is add 
   * @param folderName the folder name show the Private drive or not.
   *
   * @return {@link Document} contains created folder
   *
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.createFolder
   */
  @GET
  @Path("/createFolder/")
  @RolesAllowed("users")
  public Response createFolder(@QueryParam("driveName") String driveName,
                               @QueryParam("workspaceName") String workspaceName,
                               @QueryParam("currentFolder") String currentFolder,
                               @QueryParam("folderName") String folderName) throws Exception {
    try {
      Node node = getNode(driveName, workspaceName, currentFolder);
      Node newNode = node.addNode(Text.escapeIllegalJcrChars(folderName),
                                  NodetypeConstant.NT_UNSTRUCTURED);
      node.save();
      Document document = createNewDocument();
      String childFolder = StringUtils.isEmpty(currentFolder) ? newNode.getName() : currentFolder.concat("/")
                                                                                                 .concat(newNode.getName());
      Element folderNode = createFolderElement(document, newNode, workspaceName, driveName, childFolder);
      document.appendChild(folderNode);
      return getResponse(document);
    } catch (AccessDeniedException e) {
      if (log.isDebugEnabled()) {
        log.debug("Access is denied when perform create folder: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    } catch (PathNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (log.isErrorEnabled()) {
        log.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE)
                     .entity(e.getMessage())
                     .cacheControl(cc)
                     .build();

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform create folder: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }
  
  /**
   * Upload a file to the server
   *
   * @param uploadId the Id of the upload resource 
   * @param servletRequest the servlet request
   *
   * @return the response
   *
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.uploadFile
   */
  @POST
  @Path("/uploadFile/upload/")
  @RolesAllowed("users")
//  @InputTransformer(PassthroughInputTransformer.class)
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response uploadFile(@Context HttpServletRequest servletRequest,
      @QueryParam("uploadId") String uploadId) throws Exception {
    return fileUploadHandler.upload(servletRequest, uploadId, null);
  }
  
  /**
   * Return the information about the upload status of a file (upload percentage, file name, and more).
   *
   * @param workspaceName the workspace name 
   * @param driveName the drive name
   * @param currentFolder the path to the current folder  
   * @param currentPortal the current site
   * @param action the action to perform (save, process and more)
   * @param language the language of the user
   * @param fileName the file name
   * @param uploadId the Id of the upload resource 
   *
   * @return the response
   *
   * @throws Exception the exception
   * 
   * @anchor ECMSref.DevelopersReferences.RestService_APIs_v1alpha1.ManageDocumentService.uploadFile
   */
  @GET
  @Path("/uploadFile/control/")
  @RolesAllowed("users")
  public Response processUpload(
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("driveName") String driveName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String siteName,
      @QueryParam("action") String action,
      @QueryParam("language") String language,
      @QueryParam("fileName") String fileName,
      @QueryParam("uploadId") String uploadId) throws Exception {
    try {
      if ((workspaceName != null) && (driveName != null) && (currentFolder != null)) {
        Node currentFolderNode = getNode(Text.escapeIllegalJcrChars(driveName),
                                         Text.escapeIllegalJcrChars(workspaceName),
                                         Text.escapeIllegalJcrChars(currentFolder));
        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        return createProcessUploadResponse(Text.escapeIllegalJcrChars(workspaceName),
                                           currentFolderNode,
                                           siteName,
                                           userId,
                                           action,
                                           language,
                                           Text.escapeIllegalJcrChars(fileName),
                                           uploadId);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform processUpload: ", e);
      }
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  private Response buildXMLResponseForChildren(Node node, String driveName, String currentFolder, boolean showHidden) throws Exception {
    Document document = createNewDocument();
    Element rootElement = createFolderElement(document,
                                              node,
                                              node.getSession().getWorkspace().getName(),
                                              driveName,
                                              currentFolder);
    Element folders = document.createElement("Folders");
    Element files = document.createElement("Files");
    Node referParentNode = node;
    if (node.isNodeType("exo:symlink") && node.hasProperty("exo:uuid") && node.hasProperty("exo:workspace")) {
      referParentNode = linkManager.getTarget(node);
    }
    for (NodeIterator iterator = referParentNode.getNodes(); iterator.hasNext();) {
      Node sourceNode = null;
      Node referNode = null;
      Node child = iterator.nextNode();
      if (child.isNodeType(FCKUtils.EXO_HIDDENABLE) && !showHidden)
        continue;
      if (child.isNodeType("exo:symlink") && child.hasProperty("exo:uuid") && child.hasProperty("exo:workspace")) {
        sourceNode = linkManager.getTarget(child);
      }
      referNode = sourceNode != null ? sourceNode : child;

      if (isFolder(referNode)) {
        String childFolder = StringUtils.isEmpty(currentFolder) ? child.getName() : currentFolder.concat("/")
                                                                                                     .concat(child.getName());
        Element folder = createFolderElement(document,
                                             referNode,
                                             referNode.getSession().getWorkspace().getName(),
                                             driveName,
                                             childFolder);
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
      rootElement.appendChild(files);
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

  private Element createFolderElement(Document document,
                                      Node child,
                                      String workspaceName,
                                      String driveName,
                                      String currentFolder) throws Exception {
    Element folder = document.createElement("Folder");
    boolean hasChild = false;
    boolean canRemove = true;
    boolean canAddChild = true;
    for (NodeIterator iterator = child.getNodes(); iterator.hasNext();) {
      if (isFolder(iterator.nextNode())) {
        hasChild = true;
        break;
      }
    }
    try {
      getSession(workspaceName).checkPermission(child.getPath(), PermissionType.REMOVE);
    } catch (Exception e) {
      canRemove = false;
    } 
    
    try {
      getSession(workspaceName).checkPermission(child.getPath(), PermissionType.ADD_NODE);
    } catch (Exception e) {
      canAddChild = false;
    }
    
    folder.setAttribute("name", child.getName());
    folder.setAttribute("title", Utils.getTitle(child));
    folder.setAttribute("path", child.getPath());
    folder.setAttribute("canRemove", String.valueOf(canRemove));
    folder.setAttribute("canAddChild", String.valueOf(canAddChild));
    folder.setAttribute("nodeType", getNodeTypeIcon(child));
    folder.setAttribute("workspaceName", workspaceName);
    folder.setAttribute("driveName", driveName);
    folder.setAttribute("currentFolder", currentFolder);
    folder.setAttribute("hasChild", String.valueOf(hasChild));
    folder.setAttribute("titlePath", createTitlePath(driveName, workspaceName, currentFolder));
      
    return folder;
  }

  private Element createFileElement(Document document,
                                    Node sourceNode,
                                    Node displayNode,
                                    String workspaceName) throws Exception {
    Element file = document.createElement("File");
    boolean canRemove = true;
    file.setAttribute("name", displayNode.getName());
    file.setAttribute("title", Utils.getTitle(displayNode));
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
    try {
      getSession(workspaceName).checkPermission(sourceNode.getPath(), PermissionType.REMOVE);
    } catch (Exception e) {
      canRemove = false;
    }
    file.setAttribute("canRemove", String.valueOf(canRemove));
    return file;
  }  

  private Node getNode(String driveName, String workspaceName, String currentFolder) throws Exception {
    Session session = getSession(workspaceName);
    String driveHomePath = manageDriveService.getDriveByName(Text.escapeIllegalJcrChars(driveName)).getHomePath();
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    String drivePath = Utils.getPersonalDrivePath(driveHomePath, userId);
    Node node = (Node) session.getItem(Text.escapeIllegalJcrChars(drivePath));
    if (StringUtils.isEmpty(currentFolder)) {
      return node;
    }
    for (String folder : currentFolder.split("/")) {
      node = node.getNode(folder);
      if (node.isNodeType("exo:symlink")) {
        node = linkManager.getTarget(node);
      }
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
  
  private Response getResponse(Document document) {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                   .cacheControl(cc)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
  
  /**
   * Build drive node from drive list.
   *
   * @param document the document
   * @param drivesList the drives list
   * @param driveType the drive type
   *
   * @return the element
   */
  private Element buildXMLDriveNodes(Document document, List<DriveData> drivesList, String driveType) throws Exception {
    Element folders = document.createElement("Folders");
    folders.setAttribute("name", driveType);
    for (DriveData drive : drivesList) {
      Element folder = document.createElement("Folder");
      folder.setAttribute("name", drive.getName());
      folder.setAttribute("nodeType", driveType + " " + drive.getName().replaceAll(" ", "_"));
      folder.setAttribute("workspaceName", drive.getWorkspace());
      folder.setAttribute("canAddChild", drive.getAllowCreateFolders());
      folders.appendChild(folder);
    }
    return folders;
  }
  
  /**
   * Gets the memberships.
   *
   * @param userId the user id
   *
   * @return the memberships
   *
   * @throws Exception the exception
   */
  private List<String> getMemberships() throws Exception {
    List<String> userMemberships = new ArrayList<String>();
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    userMemberships.add(userId);
    Collection<?> memberships = ConversationState.getCurrent().getIdentity().getMemberships();
    if (memberships == null || memberships.size() < 0)
      return userMemberships;
    Object[] objects = memberships.toArray();
    for (int i = 0; i < objects.length; i++) {
      MembershipEntry membership = (MembershipEntry) objects[i];
      String role = membership.getMembershipType() + ":" + membership.getGroup();
      userMemberships.add(role);
    }
    return userMemberships;
  }
  
  public static String getNodeTypeIcon(Node node) throws RepositoryException {
    StringBuilder str = new StringBuilder();
    if (node == null)
      return "";
    String nodeType = node.getPrimaryNodeType().getName();
    if (node.isNodeType(EXO_SYMLINK)) {
      LinkManager linkManager = (LinkManager) ExoContainerContext.getCurrentContainer()
                                                                 .getComponentInstanceOfType(LinkManager.class);
      try {
        nodeType = node.getProperty(EXO_PRIMARYTYPE).getString();
        node = linkManager.getTarget(node);
        if (node == null)
          return "";
      } catch (Exception e) {
        return "";
      }
    }
    if (node.isNodeType(EXO_TRASH_FOLDER)) {
      nodeType = EXO_TRASH_FOLDER;
    }
    if (node.isNodeType(EXO_FAVOURITE_FOLDER))
      nodeType = EXO_FAVOURITE_FOLDER;
    if (nodeType.equals(NT_UNSTRUCTURED) || nodeType.equals(NT_FOLDER)) {
      for (String specificFolder : SPECIFIC_FOLDERS) {
        if (node.isNodeType(specificFolder)) {
          nodeType = specificFolder;
          break;
        }
      }
    }
    str.append(nodeType);
    return str.toString();
  }
  
  /**
   * Creates the process upload response.
   *
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @param action the action
   * @param language the language
   * @param fileName the file name
   * @param uploadId the upload id
   * @param siteName the portal name
   * @param currentFolderNode the current folder node
   *
   * @return the response
   *
   * @throws Exception the exception
   */
  protected Response createProcessUploadResponse(String workspaceName,
                                                 Node currentFolderNode,
                                                 String siteName,
                                                 String userId,
                                                 String action,
                                                 String language,
                                                 String fileName,
                                                 String uploadId) throws Exception {
    if (FileUploadHandler.SAVE_ACTION.equals(action)) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setNoCache(true);
      return fileUploadHandler.saveAsNTFile(currentFolderNode, uploadId, fileName, language, siteName, userId);
    }
    return fileUploadHandler.control(uploadId, action);
  }
  
  private String createTitlePath(String driveName, String workspaceName, String currentFolder) throws Exception {
    String[] folders = currentFolder.split("/");
    StringBuilder sb = new StringBuilder();
    StringBuilder tempFolder = new StringBuilder();
    Node parentNode = getNode(driveName, workspaceName, "");
    if (StringUtils.isEmpty(currentFolder)) {
      return "";
    }
    for (int i = 0; i < folders.length; i++) {
      tempFolder = tempFolder.append(folders[i]);
      Node node = null;
      try {
        node = getNode(driveName, workspaceName, tempFolder.toString());
      } catch (PathNotFoundException e) {
        node = parentNode.getNode(folders[i]);
      }
      tempFolder = tempFolder.append("/");
      sb.append(Utils.getTitle(node));
      if (i != folders.length - 1) {
        sb.append("/");
      }
      parentNode = (node.isNodeType("exo:symlink")? linkManager.getTarget(node) : node);
    }
    return sb.toString();
  }
}
