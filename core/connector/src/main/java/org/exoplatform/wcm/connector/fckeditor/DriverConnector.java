/*
 * Copyright (C) 2003-2007 eXo Platform SEA.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.connector.BaseConnector;
import org.exoplatform.wcm.connector.FileUploadHandler;
import org.exoplatform.wcm.connector.handler.FCKFileHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Returns a list of drives/folders/documents in a specified location for a given user. Also, it processes the file uploading action.
 *
 * {{{{portalname}}}}: The name of portal.
 * {{{{restcontextname}}}}: The context name of REST web application which is deployed to the "{{{{portalname}}}}" portal.
 *
 * @LevelAPI Provisional
 * @anchor DriverConnector
 */
@Path("/wcmDriver/")
public class DriverConnector extends BaseConnector implements ResourceContainer {

  /** The Constant FILE_TYPE_WEBCONTENT. */
  public static final String FILE_TYPE_WEBCONTENT                        = "Web Contents";

  /** The Constant FILE_TYPE_DMSDOC. */
  public static final String FILE_TYPE_DMSDOC                        = "DMS Documents";

  /** The Constant FILE_TYPE_MEDIAS. */
  public static final String FILE_TYPE_MEDIAS                       = "Medias";

  /** The Constant FILE_TYPE_MEDIAS. */
  public static final String FILE_TYPE_ALL                       = "All";

  /** The Constant FILE_TYPE_IMAGE. */
  public static final String FILE_TYPE_IMAGE                       = "Image";

  /** The Constant MEDIA_MIMETYPE. */
  public static final String[] MEDIA_MIMETYPE = new String[]{"application", "image", "audio", "video"};

  /** The Constant MEDIA_MIMETYPE. */
  public static final String[] IMAGE_MIMETYPE = new String[]{"image"};

  public static final String TYPE_FOLDER = "folder";
  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(DriverConnector.class.getName());

  /** The limit. */
  private int limit;

  /** The file number limit on client side. */
  private int limitCountClient_ = 3;

  /** The file number limit on server side. */
  private int limitCountServer_ = 30;

  private ResourceBundleService resourceBundleService=null;
  private NodeFinder nodeFinder_ = null;
  private LinkManager linkManager_ = null;

  private String resourceBundleNames[];
  private ResourceBundle sharedResourceBundle=null;

  private Locale lang = Locale.ENGLISH;

  /**
   * Instantiates a new driver connector.
   *
   * @param params The init parameters.
   */
  public DriverConnector(InitParams params) {
    limit = Integer.parseInt(params.getValueParam("upload.limit.size").getValue());
    if (params.getValueParam("upload.limit.count.client") != null) {
      limitCountClient_ = Integer.parseInt(params.getValueParam("upload.limit.count.client").getValue());
    }
    if (params.getValueParam("upload.limit.count.server") != null) {
      limitCountServer_ = Integer.parseInt(params.getValueParam("upload.limit.count.server").getValue());
    }
  }

  /**
   * Gets the maximum size of the uploaded file.
   * @return The file size limit.
   */
  public int getLimitSize() { return limit; }

  /**
   * Gets the maximum number of files uploaded from the client side.
   * @return The maximum number of files uploaded on the client side.
   */
  public int getMaxUploadCount() { return limitCountClient_; }

  /**
   * Returns a list of drives for the current user.
   *
   * @param lang The language of the drive name.
   * @return The drives.
   * @throws Exception The exception
   *
   * @anchor DriverConnector.getDrivers
   */
  @GET
  @Path("/getDrivers/")
  @RolesAllowed("users")
  public Response getDrivers(@QueryParam("lang") String lang) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
    List<DriveData> listDriver = getDriversByUserId(userId);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();

    Element rootElement = document.createElement("Connector");
    document.appendChild(rootElement);

    rootElement.setAttribute("isUpload", "false");
    rootElement.appendChild(appendDrivers(document, generalDrivers(listDriver), "General Drives", lang));
    rootElement.appendChild(appendDrivers(document, groupDrivers(listDriver), "Group Drives", lang));
    rootElement.appendChild(appendDrivers(document, personalDrivers(listDriver, userId), "Personal Drives", lang));

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
   * Returns all folders and files in a given location.
   *
   * @param driverName The drive name.
   * @param currentFolder The current folder.
   * @param currentPortal The current portal.
   * @param repositoryName The repository name.
   * @param workspaceName The workspace name.
   * @param filterBy The type of filter.
   * @return The folders and files.
   * @throws Exception The exception
   *
   * @anchor DriverConnector.getFoldersAndFiles
   */
  @GET
  @Path("/getFoldersAndFiles/")
  @RolesAllowed("users")
  public Response getFoldersAndFiles(
      @QueryParam("driverName") String driverName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String currentPortal,
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("filterBy") String filterBy,
      @QueryParam("type") String type)
      throws Exception {
    try {
      RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
      ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);

      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      DriveData drive = manageDriveService.getDriveByName(Text.escapeIllegalJcrChars(driverName));
      workspaceName = drive.getWorkspace();
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);

      String driverHomePath = drive.getHomePath();
      String itemPath = driverHomePath
                        + ((currentFolder != null && !"".equals(currentFolder) && !driverHomePath.endsWith("/")) ? "/" : "")
                        + currentFolder;
      ConversationState conversationState = ConversationState.getCurrent();
      String userId = conversationState.getIdentity().getUserId();
      itemPath = Utils.getPersonalDrivePath(itemPath, userId);
      Node node = (Node)session.getItem(Text.escapeIllegalJcrChars(itemPath));
      return buildXMLResponseForChildren(node,
                                         null,
                                         filterBy,
                                         session,
                                         currentPortal,
                                         Text.escapeIllegalJcrChars(driverName), type);

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform getFoldersAndFiles: ", e);
      }
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }


  /**
   * Checks if the drive can upload a new file.
   *
   * @return Response containing the status indicating if upload is available.
   * @throws Exception
   * @anchor DriverConnector.checkUploadAvailable
   */
  @GET
  @Path("/uploadFile/checkUploadAvailable/")
  @RolesAllowed("users")
  public Response checkUploadAvailable() throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    String msg = fileUploadHandler.getUploadingFileCount() < limitCountServer_ ? "uploadAvailable" : "uploadNotAvailable";
    return Response.ok(createDOMResponse(msg), MediaType.TEXT_XML)
                    .cacheControl(cacheControl)
                    .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                    .build();
  }

  /**
   * Uploads a file.
   *
   * @param uploadId The Id of the uploaded file.
   * @return The response.
   * @throws Exception The exception
   *
   * @anchor DriverConnector.uploadFile
   */
  @POST
  @Path("/uploadFile/upload/")
  @RolesAllowed("users")
  public Response uploadFile(@Context HttpServletRequest servletRequest,
      @QueryParam("uploadId") String uploadId) throws Exception {
    //check if number of file uploading is greater than the limit
//    if (fileUploadHandler.getUploadingFileCount() >= limitCountServer_) {
//      CacheControl cacheControl = new CacheControl();
//      cacheControl.setNoCache(true);
//      DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
//      return Response.ok(createDOMResponse("uploadNotAvailable"), MediaType.TEXT_XML)
//                      .cacheControl(cacheControl)
//                      .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
//                      .build();
//    }
    return fileUploadHandler.upload(servletRequest, uploadId, limit);
  }

  /**
   * Check the status of uploading a file, such as aborting, deleting or progressing the file.
   *
   * @param repositoryName The repository name.
   * @param workspaceName The workspace name.
   * @param driverName The drive name.
   * @param currentFolder The current folder.
   * @param currentPortal The portal name.
   * @param language The language file.
   * @param fileName The file name.
   * @return The response
   * @throws Exception The exception
   *
   * @anchor DriverConnector.checkExistence
   */
  @GET
  @Path("/uploadFile/checkExistence/")
  @RolesAllowed("users")
  public Response checkExistence(
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("driverName") String driverName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String currentPortal,
      @QueryParam("language") String language,
      @QueryParam("fileName") String fileName) throws Exception {
    try {
      // Check file existence
      Node currentFolderNode = getParentFolderNode(workspaceName,
                                                   Text.escapeIllegalJcrChars(driverName),
                                                   Text.escapeIllegalJcrChars(currentFolder));

      return fileUploadHandler.checkExistence(currentFolderNode, fileName);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform processUpload: ", e);
      }
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }
  
  /**
   * Clean file name
   * 
   * @param fileName original file name
   * @return the response
   */
  
  
  @GET
  @Path("/uploadFile/cleanName")
  @RolesAllowed("users")
  public Response cleanName (@QueryParam("fileName") String fileName) throws Exception{
    return fileUploadHandler.cleanName(fileName);
  }

  /**
   * Controls the process of uploading a file, such as aborting, deleting or progressing the file.
   *
   * @param repositoryName The repository name.
   * @param workspaceName The workspace name.
   * @param driverName The drive name.
   * @param currentFolder The current folder.
   * @param currentPortal The current portal.
   * @param userId The user identity.
   * @param jcrPath The path of the file.
   * @param action The action.
   * @param language The language.
   * @param fileName The file name.
   * @param uploadId The Id of upload.
   * @param existenceAction Checks if an action exists or not.
   * @return The response.
   * @throws Exception The exception
   *
   * @anchor DriverConnector.processUpload
   */
  @GET
  @Path("/uploadFile/control/")
  @RolesAllowed("users")
  public Response processUpload(
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("driverName") String driverName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String currentPortal,
      @QueryParam("userId") String userId,
      @QueryParam("jcrPath") String jcrPath,
      @QueryParam("action") String action,
      @QueryParam("language") String language,
      @QueryParam("fileName") String fileName,
      @QueryParam("uploadId") String uploadId,
      @QueryParam("existenceAction") String existenceAction) throws Exception {
    try {
      // Check upload status
      Response msgResponse = fileUploadHandler.checkStatus(uploadId, language);
      if (msgResponse != null) return msgResponse;

      if ((repositoryName != null) && (workspaceName != null) && (driverName != null)
          && (currentFolder != null)) {
        ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
        workspaceName = workspaceName != null ? workspaceName :
                                                manageDriveService.getDriveByName(Text.escapeIllegalJcrChars(driverName))
                                                                  .getWorkspace();

        Node currentFolderNode = getParentFolderNode(workspaceName,
                                                     Text.escapeIllegalJcrChars(driverName),
                                                     Text.escapeIllegalJcrChars(currentFolder));
        fileName = Text.escapeIllegalJcrChars(fileName);
        return createProcessUploadResponse(workspaceName,
                                           currentFolderNode,
                                           currentPortal,
                                           userId,
                                           Text.escapeIllegalJcrChars(jcrPath),
                                           action,
                                           language,
                                           fileName,
                                           uploadId,
                                           existenceAction);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform processUpload: ", e);
      }
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /**
   * Gets the drives by user Id.
   *
   * @param userId the user Id
   *
   * @return the drives by user Id
   *
   * @throws Exception the exception
   */
  private List<DriveData> getDriversByUserId(String userId) throws Exception {
    ManageDriveService driveService = WCMCoreUtils.getService(ManageDriveService.class);
    List<String> userRoles = getMemberships(userId);
    return driveService.getDriveByUserRoles(userId, userRoles);
  }

  /**
   * Appends drives.
   *
   * @param document The document.
   * @param driversList The drivers list.
   * @param groupName The group name.
   *
   * @return The element.
   */
  private Element appendDrivers(Document document,
                                List<DriveData> driversList,
                                String groupName,
                                String lang) throws Exception {
    Element folders = document.createElement("Folders");
    folders.setAttribute("name", resolveDriveLabel(groupName, lang));
    folders.setAttribute("isUpload", "false");
    for (DriveData driver : driversList) {
      String repository = WCMCoreUtils.getRepository().getConfiguration().getName();
      String workspace  = driver.getWorkspace();
      String path = driver.getHomePath();
      String name = driver.getName();
      Element folder = document.createElement("Folder");
      NodeLocation nodeLocation = new NodeLocation(repository, workspace, path);
      Node driveNode = NodeLocation.getNodeByLocation(nodeLocation);
      if(driveNode == null) continue;
      folder.setAttribute("name", name);
      folder.setAttribute("label", resolveDriveLabel(name, lang));
      folder.setAttribute("url", FCKUtils.createWebdavURL(driveNode));
      folder.setAttribute("folderType", "exo:drive");
      folder.setAttribute("path", path);
      folder.setAttribute("repository", repository);
      folder.setAttribute("workspace", workspace);
      folder.setAttribute("isUpload", "true");
      folder.setAttribute("hasFolderChild", String.valueOf(this.hasFolderChild(driveNode)));
      folder.setAttribute("nodeTypeCssClass", Utils.getNodeTypeIcon(driveNode, "uiIcon16x16"));

      folders.appendChild(folder);
    }
    return folders;
  }

  private String resolveDriveLabel(String name, String lang) {
    if (resourceBundleService ==null) {
      resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      resourceBundleNames = resourceBundleService.getSharedResourceBundleNames();
      sharedResourceBundle = resourceBundleService.getResourceBundle(resourceBundleNames, this.lang);
    }
    try {
      if(!this.lang.getLanguage().equals(lang)){
        this.lang = new Locale(lang);
        sharedResourceBundle = resourceBundleService.getResourceBundle(resourceBundleNames, this.lang);
      }
      return sharedResourceBundle.getString("ContentSelector.title." + name.replaceAll(" ", ""));
    } catch (MissingResourceException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(e.getMessage());
      }
    }
    return name;
  }

  /**
   * Personal drivers.
   *
   * @param driveList the drive list
   *
   * @return the list< drive data>
   * @throws Exception
   */
  private List<DriveData> personalDrivers(List<DriveData> driveList, String userId) throws Exception {
    List<DriveData> personalDrivers = new ArrayList<DriveData>();
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    for(DriveData drive : driveList) {
      String driveHomePath = Utils.getPersonalDrivePath(drive.getHomePath(), userId);
      if(driveHomePath.startsWith(userNode.getPath())) {
        drive.setHomePath(driveHomePath);
        personalDrivers.add(drive);
      }
    }
    Collections.sort(personalDrivers);
    return personalDrivers;
  }

  /**
   * Group drivers.
   *
   * @param driverList the driver list
   * @return the list< drive data>
   * @throws Exception the exception
   */
  private List<DriveData> groupDrivers(List<DriveData> driverList) throws Exception {
    ManageDriveService driveService = WCMCoreUtils.getService(ManageDriveService.class);
    String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
    List<String> userRoles = this.getMemberships(currentUserId);
    return driveService.getGroupDrives(currentUserId, userRoles);
  }

  /**
   * General drivers.
   *
   * @param driverList the driver list
   *
   * @return the list< drive data>
   *
   * @throws Exception the exception
   */
  private List<DriveData> generalDrivers(List<DriveData> driverList) throws Exception {
    List<DriveData> generalDrivers = new ArrayList<DriveData>();
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    String userPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    for(DriveData drive : driverList) {
      if((!drive.getHomePath().startsWith(userPath) && !drive.getHomePath().startsWith(groupPath))
          || drive.getHomePath().equals(userPath)) {
        generalDrivers.add(drive);
      }
    }
    return generalDrivers;
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
  private List<String> getMemberships(String userId) throws Exception {
    List<String> userMemberships = new ArrayList<String> ();
    userMemberships.add(userId);
    // here we must retrieve memberships of the user using the
    // IdentityRegistry Service instead of Organization Service to
    // allow JAAS based authorization
    Collection<MembershipEntry> memberships = getUserMembershipsFromIdentityRegistry(userId);
    if (memberships != null) {
      for (MembershipEntry membership : memberships) {
        String role = membership.getMembershipType() + ":" + membership.getGroup();
        userMemberships.add(role);
      }
    }

    return userMemberships;
  }

  /**
   * this method retrieves memberships of the user having the given id using the
   * IdentityRegistry service instead of the Organization service to allow JAAS
   * based authorization
   *
   * @param authenticatedUser the authenticated user id
   * @return a collection of MembershipEntry
   */
  private static Collection<MembershipEntry> getUserMembershipsFromIdentityRegistry(String authenticatedUser) {
    IdentityRegistry identityRegistry = WCMCoreUtils.getService(IdentityRegistry.class);
    Identity currentUserIdentity = identityRegistry.getIdentity(authenticatedUser);
    return currentUserIdentity.getMemberships();
  }

  private Response buildXMLResponseForChildren(Node node,
                                               String command,
                                               String filterBy,
                                               Session session,
                                               String currentPortal,
                                               String nodeDriveName,
                                               String type) throws Exception {
      TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      Element rootElement = FCKUtils.createRootElement(command, node, folderHandler.getFolderType(node));
      NodeList nodeList = rootElement.getElementsByTagName("CurrentFolder");
      Element currentFolder = (Element) nodeList.item(0);
      currentFolder.setAttribute("isUpload", "true");
      Document document = rootElement.getOwnerDocument();
      Element folders = document.createElement("Folders");
      folders.setAttribute("isUpload", "true");
      Element files = document.createElement("Files");
      files.setAttribute("isUpload", "true");
      Node sourceNode = null;
      Node checkNode = null;
      List<Node> childList = new ArrayList<Node>();
      for (NodeIterator iterator = node.getNodes(); iterator.hasNext();) {
        childList.add(iterator.nextNode());
      }
      Collections.sort(childList,new NodeTitleComparator());
      for (Node child:childList) {
        String fileType = null;
        if (child.isNodeType(FCKUtils.EXO_HIDDENABLE))
          continue;
        if(TYPE_FOLDER.equals(type) && templateService.isManagedNodeType(child.getPrimaryNodeType().getName()))
          continue;

        if(child.isNodeType("exo:symlink") && child.hasProperty("exo:uuid")) {
          LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
          sourceNode = linkManager.getTarget(child);
        } else {
          sourceNode = child;
        }

        checkNode = sourceNode != null ? sourceNode : child;

        if (isFolder(checkNode)) {
          // Get node name from node path to fix same name problem (ECMS-3586)
          String nodePath = child.getPath();
          Element folder = createFolderElement(document, checkNode, checkNode.getPrimaryNodeType().getName(),
                        nodePath.substring(nodePath.lastIndexOf("/") + 1, nodePath.length()), nodeDriveName);
          folders.appendChild(folder);
        }

      if (FILE_TYPE_ALL.equals(filterBy)
          && (checkNode.isNodeType(NodetypeConstant.EXO_WEBCONTENT) || !isFolder(checkNode))) {
        fileType = FILE_TYPE_ALL;
      }

        if (FILE_TYPE_WEBCONTENT.equals(filterBy)) {
          if(checkNode.isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
            fileType = FILE_TYPE_WEBCONTENT;
          }
        }

        if (FILE_TYPE_MEDIAS.equals(filterBy) && isMediaType(checkNode)){
          fileType = FILE_TYPE_MEDIAS;
        }

        if (FILE_TYPE_DMSDOC.equals(filterBy) && isDMSDocument(checkNode)) {
          fileType = FILE_TYPE_DMSDOC;
        }

        if (FILE_TYPE_IMAGE.equals(filterBy) && isImageType(checkNode)) {
            fileType = FILE_TYPE_IMAGE;
          }

        if (fileType != null) {
          Element file = FCKFileHandler.createFileElement(document, fileType, checkNode, child, currentPortal, linkManager);
          files.appendChild(file);
        }
      }

      rootElement.appendChild(folders);
      rootElement.appendChild(files);
      return getResponse(document);
    }
  /**
   * Checks if is folder and is not web content.
   *
   * @param checkNode the check node
   *
   * @return true, if is folder and is not web content
   *
   * @throws RepositoryException the repository exception
   */
  private boolean isFolder(Node checkNode) throws RepositoryException {
    try {
      if (isDocument(checkNode)) return false;
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return
        checkNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)
        || checkNode.isNodeType(NodetypeConstant.NT_FOLDER)
        || checkNode.isNodeType(NodetypeConstant.EXO_TAXONOMY);
  }

  /**
   * Check if specific node has child which is type of nt:folder or nt:unstructured.
   *
   * @param checkNode The node to be checked
   * @return True if the folder has some child.
   * @throws Exception
   */
  private boolean hasFolderChild(Node checkNode) throws Exception {
    return (Utils.hasChild(checkNode, NodetypeConstant.NT_UNSTRUCTURED)
              || Utils.hasChild(checkNode, NodetypeConstant.NT_FOLDER));
  }

  /**
   * Checks if is dMS document.(not including free layout webcontent & media & article)
   *
   * @param node the node
   *
   * @return true, if is dMS document
   *
   * @throws Exception the exception
   */
  private boolean isDMSDocument(Node node) throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    List<String> dmsDocumentListTmp = templateService.getDocumentTemplates();
    List<String> dmsDocumentList = new ArrayList<String>();
    dmsDocumentList.addAll(dmsDocumentListTmp);
    dmsDocumentList.remove(NodetypeConstant.EXO_WEBCONTENT);
    for (String documentType : dmsDocumentList) {
      if (node.getPrimaryNodeType().isNodeType(documentType)
          && !isMediaType(node)
          && !node.isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if specific node is document
   *
   * @param node specific Node
   * @return true: is document, false: not document
   * @throws RepositoryException
   */
  private boolean isDocument(Node node) throws RepositoryException {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    List<String> documentTypeList = templateService.getDocumentTemplates();
    documentTypeList.remove(NodetypeConstant.EXO_WEBCONTENT);
    for (String documentType : documentTypeList) {
      if (node.getPrimaryNodeType().isNodeType(documentType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if is media type.
   *
   * @param node the node
   *
   * @return true, if is media type
   */
  private boolean isMediaType(Node node){
    String mimeType = "";

    try {
      mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    } catch (Exception e) {
      return false;
    }

    for(String type: MEDIA_MIMETYPE) {
      if(mimeType.contains(type)){
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if is image type.
   *
   * @param node the node
   *
   * @return true, if is image type
   */
  private boolean isImageType(Node node){
    String mimeType = "";

    try {
      mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    } catch (Exception e) {
      return false;
    }

    for(String type: IMAGE_MIMETYPE) {
      if(mimeType.contains(type)){
        return true;
      }
    }

    return false;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.connector.BaseConnector#getContentStorageType()
   */
  @Override
  protected String getContentStorageType() throws Exception {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.connector.BaseConnector#getRootContentStorage(javax.jcr.Node)
   */
  @Override
  protected Node getRootContentStorage(Node node) throws Exception {
    try {
      PortalFolderSchemaHandler folderSchemaHandler = webSchemaConfigService
      .getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
      return folderSchemaHandler.getImagesFolder(node);
    } catch (Exception e) {
      WebContentSchemaHandler webContentSchemaHandler = webSchemaConfigService
      .getWebSchemaHandlerByType(WebContentSchemaHandler.class);
      return webContentSchemaHandler.getImagesFolders(node);
    }
  }

  /**
   *
   * @param workspaceName The workspace name.
   * @param currentFolderNode The current folder.
   * @param siteName The portal name.
   * @param userId  The user Id.
   * @param jcrPath The path of the file.
   * @param action The action.
   * @param language The language.
   * @param fileName The file name.
   * @param uploadId The Id of upload.
   * @param existenceAction Checks if an action exists.
   * @return the response
   * @throws Exception the exception
   */
  protected Response createProcessUploadResponse(String workspaceName,
                                                 Node currentFolderNode,
                                                 String siteName,
                                                 String userId,
                                                 String jcrPath,
                                                 String action,
                                                 String language,
                                                 String fileName,
                                                 String uploadId,
                                                 String existenceAction) throws Exception {
    if (FileUploadHandler.SAVE_ACTION.equals(action)) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setNoCache(true);
      return fileUploadHandler.saveAsNTFile(currentFolderNode, uploadId, fileName, language, siteName, userId, existenceAction);
    }
    return fileUploadHandler.control(uploadId, action);
  }

  /**
   * Gets the parent folder node.
   *
   * @param workspaceName the workspace name
   * @param driverName the driver name
   * @param currentFolder the current folder
   *
   * @return the parent folder node
   *
   * @throws Exception the exception
   */
  private Node getParentFolderNode(String workspaceName, String driverName, String currentFolder) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    Session session = sessionProvider.getSession(workspaceName, manageableRepository);
    ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
    DriveData driveData = manageDriveService.getDriveByName(driverName);
    String parentPath = (driveData != null ? driveData.getHomePath() : "");
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    if(driveData != null &&
       driveData.getHomePath().startsWith(nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH) + "/${userId}")) {
       parentPath = Utils.getPersonalDrivePath(driveData.getHomePath(),
                                               ConversationState.getCurrent().getIdentity().getUserId());
    };
    parentPath += ((currentFolder != null && currentFolder.length() != 0) ? "/" : "") + currentFolder;
    parentPath = parentPath.replace("//", "/");
    //return result;
    return getTargetNode(session, parentPath);
  }

  private Node getTargetNode(Session session, String path) throws Exception {
    Node node = null;
    if (linkManager_ == null) {
      linkManager_ = WCMCoreUtils.getService(LinkManager.class);
    }
    if (nodeFinder_ == null) {
      nodeFinder_ = WCMCoreUtils.getService(NodeFinder.class);
    }
    node = (Node)nodeFinder_.getItem(session, path, true);
    return node;
  }

  private Element createFolderElement(Document document,
                                      Node child,
                                      String folderType,
                                      String childName,
                                      String nodeDriveName) throws Exception {
      Element folder = document.createElement("Folder");
      folder.setAttribute("name", childName.replaceAll("%", "%25"));
      folder.setAttribute("title", Utils.getTitle(child).replaceAll("%", "%25"));
      folder.setAttribute("url", FCKUtils.createWebdavURL(child));
      folder.setAttribute("folderType", folderType);
      folder.setAttribute("path", child.getPath());
      folder.setAttribute("isUpload", "true");
      folder.setAttribute("hasFolderChild", String.valueOf(this.hasFolderChild(child)));
      folder.setAttribute("nodeTypeCssClass", Utils.getNodeTypeIcon(child, "uiIcon16x16"));


      if (nodeDriveName!=null && nodeDriveName.length()>0) folder.setAttribute("nodeDriveName", nodeDriveName);
      return folder;
    }


  /**
   * returns a DOMSource object containing given message
   * @param message the message
   * @return DOMSource object
   * @throws Exception
   */
  private DOMSource createDOMResponse(String message) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();
    Element rootElement = doc.createElement(message);
    doc.appendChild(rootElement);
    return new DOMSource(doc);
  }

  private static class NodeTitleComparator implements Comparator<Node>{
    @Override
    public int compare(Node node1, Node node2) {
      try{
        String titleNode1 = Utils.getTitle(node1);
        String titleNode2 = Utils.getTitle(node2);
        return titleNode1.compareToIgnoreCase(titleNode2) ;
      }catch (Exception e) {
        return 0;
      }
    }
  }
}