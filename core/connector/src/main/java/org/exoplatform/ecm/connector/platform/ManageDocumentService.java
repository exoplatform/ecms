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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.NotYetCloudFileException;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.FileUploadHandler;

/**
 * This service is used to perform some actions on a folder or on a file, such as creating,
 * or deleting a folder/file, or uploading a file.
 *
 * @LevelAPI Provisional
 *
 * @anchor ManageDocumentService
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
  private static final Log LOG = ExoLogger.getLogger(ManageDocumentService.class.getName());
  
  /** Default folder name if the original is null */ 
  private static final String DEFAULT_NAME = "untitled";

  /** The manage drive service. */
  private final ManageDriveService    manageDriveService;

  /** The link manager. */
  private final LinkManager linkManager;
  
  /** The cloud drives. */
  private final CloudDriveService cloudDrives;

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
  
  /** The limit size of uploaded file. */
  private int limit;

  /**
   * Instantiates a document service.
   *
   * @param manageDriveService Instantiates a drive manager service.
   * @param linkManager Instantiates a link manager service.
   * @param cloudDrives the CloudDrives service
   * @param params the params
   */
  public ManageDocumentService(ManageDriveService manageDriveService,
                               LinkManager linkManager,
                               CloudDriveService cloudDrives,
                               InitParams params) {
    this.manageDriveService = manageDriveService;
    this.linkManager = linkManager;
    this.cloudDrives = cloudDrives;
    this.fileUploadHandler = new FileUploadHandler();
    this.cc = new CacheControl();
    this.cc.setNoCache(true);
    this.cc.setNoStore(true);
    this.limit = Integer.parseInt(params.getValueParam("upload.limit.size").getValue());
  }

  /**
   * Gets all drives by type (General, Group or Personal).
   *
   * @param driveType The types of drive (General, Group, or Personal).
   * @param showPrivate Shows the Private drive or not. The default value is false.
   * @param showPersonal Shows the Personal drive or not. The default value is false.
   * @return {@link Document} which contains the drives.
   * @throws Exception The exception
   *
   * @anchor ManageDocumentService.getDrives
   */
  @GET
  @Path("/getDrives/")
  @RolesAllowed("users")
  public Response getDrives(@QueryParam("driveType") String driveType,
                            @DefaultValue("false") @QueryParam("showPrivate") String showPrivate,
                            @DefaultValue("false") @QueryParam("showPersonal") String showPersonal) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    List<String> userRoles = getMemberships();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();

    Element rootElement = document.createElement("Folders");
    document.appendChild(rootElement);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    List<DriveData> driveList = new ArrayList<DriveData>();
    if (DriveType.GENERAL.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getMainDrives(userId, userRoles);
    } else if (DriveType.GROUP.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getGroupDrives(userId, userRoles);
    } else if (DriveType.PERSONAL.toString().equalsIgnoreCase(driveType)) {
      driveList = manageDriveService.getPersonalDrives(userId);
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
   * Gets all folders and files which can be viewed by the current user.
   *
   * @param driveName The drive name.
   * @param workspaceName The workspace name.
   * @param currentFolder The path to the folder to achieve its folders and files.
   * @param showHidden Shows the hidden items or not. The default value is false.
   * @return {@link Document} which contains the folders and files.
   *
   * @anchor ManageDocumentService.getFoldersAndFiles
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
      if (LOG.isDebugEnabled()) {
        LOG.debug("Access is denied when perform get Folders and files: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform get Folders and files: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Deletes a folder/file.
   *
   * @param driveName The drive name.
   * @param workspaceName The workspace name.
   * @param itemPath The path to the folder/file.
   * @return {@link Response} Returns the status of an item which has been deleted.
   *
   * @anchor ManageDocumentService.deleteFolderOrFile
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
      if (LOG.isDebugEnabled()) {
        LOG.debug("Access is denied when perform delete folder or file: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    }
    catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).cacheControl(cc).build();

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform delete Folder or file: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Creates a new folder and returns its information.
   *
   * @param driveName The drive name.
   * @param workspaceName The workspace name.
   * @param currentFolder The path to the folder where a child folder is added.
   * @param folderName The folder name.
   * @return {@link Document} which contains the created folder.
   * @throws Exception The exception
   *
   * @anchor ManageDocumentService.createFolder
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
      // The name automatically determined from the title according to the current algorithm.
      String name = Text.escapeIllegalJcrChars(org.exoplatform.services.cms.impl.Utils.cleanString(folderName));
      // Set default name if new title contain no valid character
      name = (StringUtils.isEmpty(name)) ? DEFAULT_NAME : name;
      Node newNode = node.addNode(name,
                                  NodetypeConstant.NT_UNSTRUCTURED);
      if (!newNode.hasProperty("exo:title")) {
        newNode.addMixin("exo:rss-enable");
      }
      newNode.setProperty("exo:title", folderName);
      node.save();
      Document document = createNewDocument();
      String childFolder = StringUtils.isEmpty(currentFolder) ? newNode.getName() : currentFolder.concat("/")
                                                                                                 .concat(newNode.getName());
      Element folderNode = createFolderElement(document, newNode, workspaceName, driveName, childFolder);
      document.appendChild(folderNode);
      return getResponse(document);
    } catch (AccessDeniedException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Access is denied when perform create folder: ", e);
      }
      return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).cacheControl(cc).build();
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Item is not found: ", e);
      }
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).cacheControl(cc).build();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository is error: ", e);
      }
      return Response.status(Status.SERVICE_UNAVAILABLE)
                     .entity(e.getMessage())
                     .cacheControl(cc)
                     .build();

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform create folder: ", e);
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Uploads a file to the server.
   *
   * @param uploadId The Id of the uploaded resource.
   * @param servletRequest The request.
   *
   * @return The response.
   *
   * @throws Exception The exception
   *
   * @anchor ManageDocumentService.uploadFile
   */
  @POST
  @Path("/uploadFile/upload/")
  @RolesAllowed("users")
//  @InputTransformer(PassthroughInputTransformer.class)
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response uploadFile(@Context HttpServletRequest servletRequest,
      @QueryParam("uploadId") String uploadId) throws Exception {
    return fileUploadHandler.upload(servletRequest, uploadId, limit);
  }

  /**
   * Returns information about the upload status of a file, such as the upload percentage, the file name, and more.
   *
   * @param workspaceName The workspace name.
   * @param driveName The drive name.
   * @param currentFolder The path to the current folder.
   * @param currentPortal The name of the current site.
   * @param action The action to perform (saving, processing, and more).
   * @param language The language of the user.
   * @param fileName The name of the file.
   * @param uploadId The Id of the uploaded resource.
   * @return The response.
   * @throws Exception The exception
   *
   * @anchor ManageDocumentService.processUpload
   */
  @GET
  @Path("/uploadFile/control/")
  @RolesAllowed("users")
  public Response processUpload(
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("driveName") String driveName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String currentPortal,
      @QueryParam("action") String action,
      @QueryParam("language") String language,
      @QueryParam("fileName") String fileName,
      @QueryParam("uploadId") String uploadId,
      @QueryParam("existenceAction") String existenceAction) throws Exception {
    try {
      if ((workspaceName != null) && (driveName != null) && (currentFolder != null)) {
        Node currentFolderNode = getNode(Text.escapeIllegalJcrChars(driveName),
                                         Text.escapeIllegalJcrChars(workspaceName),
                                         Text.escapeIllegalJcrChars(currentFolder));
        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        return createProcessUploadResponse(Text.escapeIllegalJcrChars(workspaceName),
                                           currentFolderNode,
                                           currentPortal,
                                           userId,
                                           action,
                                           language,
                                           Text.escapeIllegalJcrChars(fileName),
                                           uploadId, existenceAction);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform processUpload: ", e);
      }
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  private Response buildXMLResponseForChildren(Node node,
                                               String driveName,
                                               String currentFolder,
                                               boolean showHidden) throws Exception {
    Document document = createNewDocument();
    Element rootElement = createFolderElement(document, node, node.getSession().getWorkspace().getName(), 
                                              driveName, currentFolder);
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
      String workspaceName = referNode.getSession().getWorkspace().getName();
      
      if (isFolder(referNode)) {
        // Get current folder from folder path to fix same name problem (ECMS-3586)
        String folderPath = child.getPath();
        folderPath = folderPath.substring(folderPath.lastIndexOf("/") + 1, folderPath.length());
        String childFolder = StringUtils.isEmpty(currentFolder) ? folderPath : currentFolder.concat("/").concat(folderPath);
        Element folder = createFolderElement(document, child, workspaceName, driveName, childFolder);
        folders.appendChild(folder);
      } else if (isFile(referNode)) {
        Element file = createFileElement(document, referNode, child, workspaceName);
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
                                      Node node,
                                      String workspaceName,
                                      String driveName,
                                      String currentFolder) throws Exception {
    Element folder = document.createElement("Folder");
    boolean hasChild = false;
    boolean canRemove = true;
    boolean canAddChild = true;
    for (NodeIterator iterator = node.getNodes(); iterator.hasNext();) {
      if (isFolder(iterator.nextNode())) {
        hasChild = true;
        break;
      }
    }
    Session session = getSession(workspaceName);
    try {
      session.checkPermission(node.getPath(), PermissionType.REMOVE);
    } catch (Exception e) {
      canRemove = false;
    }
    try {
      session.checkPermission(node.getPath(), PermissionType.ADD_NODE);
    } catch (Exception e) {
      canAddChild = false;
    }
    folder.setAttribute("name", node.getName());
    folder.setAttribute("title", Utils.getTitle(node));
    folder.setAttribute("path", node.getPath());
    folder.setAttribute("canRemove", String.valueOf(canRemove));
    folder.setAttribute("canAddChild", String.valueOf(canAddChild));
    folder.setAttribute("nodeType", getNodeTypeIcon(node));
    folder.setAttribute("workspaceName", workspaceName);
    folder.setAttribute("driveName", driveName);
    folder.setAttribute("currentFolder", currentFolder);
    folder.setAttribute("hasChild", String.valueOf(hasChild));
    folder.setAttribute("titlePath", createTitlePath(driveName, workspaceName, currentFolder));
    
    // is folder public (available to any user)
    boolean isPublic = PermissionUtil.canAnyAccess(node);
    folder.setAttribute("isPublic", String.valueOf(isPublic));

    CloudDrive cloudDrive = cloudDrives.findDrive(node);
    CloudFile cloudFile = null;
    if (cloudDrive != null && cloudDrive.isConnected()) {
      // It's connected Cloud Drive or its sub-folder
      try {
        if (cloudDrive.isDrive(node)) {
          folder.setAttribute("isCloudDrive", Boolean.TRUE.toString());
          folder.setAttribute("isConnected", Boolean.TRUE.toString());
          folder.setAttribute("cloudProvider", cloudDrive.getUser().getProvider().getId());
        } else {
          cloudFile = cloudDrive.getFile(node.getPath());
          if (cloudFile.isConnected()) {
            if (cloudFile.isFolder()) {
              folder.setAttribute("isCloudFile", Boolean.TRUE.toString());
              folder.setAttribute("isConnected", Boolean.TRUE.toString());
            } // otherwise we don't want show them
          } else {
            folder.setAttribute("isCloudFile", Boolean.TRUE.toString());
            folder.setAttribute("isConnected", Boolean.FALSE.toString());
          }
        }
      } catch (NotYetCloudFileException e) {
        folder.setAttribute("isCloudFile", Boolean.TRUE.toString());
        folder.setAttribute("isConnected", Boolean.FALSE.toString());
      } catch (CloudDriveException e) {
        LOG.warn("Error reading cloud folder {}: {}", node.getPath(), e.getMessage());
      }
    } 
    if (cloudFile == null) {
      // It's local storage folder
    }
    return folder;
  }

  private Element createFileElement(Document document,
                                    Node sourceNode,
                                    Node displayNode,
                                    String workspaceName) throws Exception {
    Element file = document.createElement("File");
    AutoVersionService autoVersionService=WCMCoreUtils.getService(AutoVersionService.class);
    boolean canRemove = true;
    String sourcePath = sourceNode.getPath();
    file.setAttribute("name", Utils.getTitle(displayNode));
    file.setAttribute("title", Utils.getTitle(displayNode));
    file.setAttribute("workspaceName", workspaceName);
    file.setAttribute("id", sourceNode.getUUID()); 
    file.setAttribute("path", displayNode.getPath());
    file.setAttribute("isVersioned", String.valueOf(sourceNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)));
    file.setAttribute("isVersionSupport", String.valueOf(autoVersionService.isVersionSupport(sourcePath, workspaceName)));
    if (sourceNode.isNodeType("nt:file")) {
      Node content = sourceNode.getNode("jcr:content");
      file.setAttribute("nodeType", content.getProperty("jcr:mimeType").getString());
    } else {
      file.setAttribute("nodeType", sourceNode.getPrimaryNodeType().getName());
    }
    try {
      getSession(workspaceName).checkPermission(sourcePath, PermissionType.REMOVE);
    } catch (Exception e) {
      canRemove = false;
    }
    file.setAttribute("canRemove", String.valueOf(canRemove));
    SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                                                                                         SimpleDateFormat.SHORT);
    
    // is document public (available to any user)
    boolean isPublic = PermissionUtil.canAnyAccess(sourceNode);
    file.setAttribute("isPublic", String.valueOf(isPublic));
    
    CloudDrive cloudDrive = cloudDrives.findDrive(sourceNode);
    CloudFile cloudFile = null;
    if (cloudDrive != null && cloudDrive.isConnected() && !cloudDrive.isDrive(sourceNode)) {
      // It's connected Cloud Drive file
      try {
        cloudFile = cloudDrive.getFile(sourceNode.getPath());
        if (cloudFile.isConnected()) {
          if (!cloudFile.isFolder()) {
            file.setAttribute("isCloudFile", Boolean.TRUE.toString());
            file.setAttribute("isConnected", Boolean.TRUE.toString());
            file.setAttribute("dateCreated", formatter.format(cloudFile.getCreatedDate().getTime()));
            file.setAttribute("dateModified", formatter.format(cloudFile.getModifiedDate().getTime()));
            file.setAttribute("lastModifier", cloudFile.getLastUser());
            file.setAttribute("creator", cloudFile.getAuthor());
            file.setAttribute("size", String.valueOf(cloudFile.getSize()));  
          } // otherwise we don't want show it
        } else {
          file.setAttribute("isCloudFile", Boolean.TRUE.toString());
          file.setAttribute("isConnected", Boolean.FALSE.toString());
        }
      } catch (NotYetCloudFileException e) {
        file.setAttribute("isCloudFile", Boolean.TRUE.toString());
        file.setAttribute("isConnected", Boolean.FALSE.toString());
      } catch (CloudDriveException e) {
        LOG.warn("Error reading cloud file {}: {}", sourceNode.getPath(), e.getMessage());
      }
    } 
    if (cloudFile == null) {
      // It's local storage file
      file.setAttribute("dateCreated", formatter.format(sourceNode.getProperty("exo:dateCreated").getDate().getTime()));
      if (sourceNode.hasProperty("exo:dateModified")) {
        file.setAttribute("dateModified", formatter.format(sourceNode.getProperty("exo:dateModified").getDate().getTime()));
      } else {
        file.setAttribute("dateModified", null);
      }
      if (sourceNode.hasProperty("exo:lastModifier")) {
        file.setAttribute("lastModifier", sourceNode.getProperty("exo:lastModifier").getString());
      } else {
        file.setAttribute("lastModifier", null);
      }
      file.setAttribute("creator", sourceNode.getProperty("exo:owner").getString());
      long size = sourceNode.getNode("jcr:content").getProperty("jcr:data").getLength();
      file.setAttribute("size", String.valueOf(size));
    }
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
      if (node.hasNode(folder)){
        node = node.getNode(folder);
        if (node.isNodeType(NodetypeConstant.EXO_SYMLINK)) node = linkManager.getTarget(node);
      } else if (node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        node = linkManager.getTarget(node).getNode(folder);
      } else {
        return node;
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
   * @param document the document.
   * @param drivesList the drives list.
   * @param driveType the drive type.
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
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
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
   * @param workspaceName the workspace name.
   * @param userId The user Id.
   * @param action The action.
   * @param language The language.
   * @param fileName The file name.
   * @param uploadId The upload Id.
   * @param siteName The portal name.
   * @param currentFolderNode The current folder node.
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
                                                 String uploadId,
                                                 String existenceAction) throws Exception {
    if (FileUploadHandler.SAVE_ACTION.equals(action)) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setNoCache(true);
      DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, true);
      return fileUploadHandler.saveAsNTFile(workspaceName, currentFolderNode, uploadId, fileName, language, siteName, userId, existenceAction);
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
