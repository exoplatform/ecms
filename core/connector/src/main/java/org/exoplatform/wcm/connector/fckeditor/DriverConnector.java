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

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
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
import org.exoplatform.services.cms.impl.Utils;

/**
 * Created by The eXo Platform SEA
 * Author : Do Dang Thang
 * thang.do@exoplatform.com
 * Sep 7, 2009
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
  
  /** The Constant MEDIA_MIMETYPE. */
  public static final String[] MEDIA_MIMETYPE = new String[]{"application", "image", "audio", "video"};
  
  /** The log. */
  private static Log log = ExoLogger.getLogger(DriverConnector.class);

  /** The limit. */
  private int limit;
  
  private PortalContainer manager;
  
  private OrganizationService organizationService=null; 
  
  private ResourceBundleService resourceBundleService=null;
  
  private String resourceBundleNames[];
  private ResourceBundle sharedResourceBundle=null;
  
  private Locale lang = Locale.ENGLISH;
  /**
   * Instantiates a new driver connector.
   * 
   * @param container the container
   * @param params the params
   */
  public DriverConnector(InitParams params) {
    limit = Integer.parseInt(params.getValueParam("upload.limit.size").getValue());
    manager = PortalContainer.getInstance() ;
  }
	
  /**
   * Gets the drivers.
   * 
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param userId the user id
   * 
   * @return the drivers
   * 
   * @throws Exception the exception
   */
  @GET
  @Path("/getDrivers/")
  public Response getDrivers(@QueryParam("lang") String lang) throws Exception {    
    String repositoryName = WCMCoreUtils.getRepository(null).getConfiguration().getName();
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
    List<DriveData> listDriver = getDriversByUserId(repositoryName, userId);  	
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.newDocument();
    
    Element rootElement = document.createElement("Connector");
    document.appendChild(rootElement);

    rootElement.setAttribute("isUpload", "false");
    rootElement.appendChild(appendDrivers(document, generalDrivers(listDriver), "General Drives", lang));
    rootElement.appendChild(appendDrivers(document, groupDrivers(listDriver, userId), "Group Drives", lang));
    rootElement.appendChild(appendDrivers(document, personalDrivers(listDriver, userId), "Personal Drives", lang));
    
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /**
   * Gets the folders and files.
   * 
   * @param driverName the driver name
   * @param currentFolder the current folder
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param filterBy the filter by
   * @param userId the user id
   * 
   * @return the folders and files
   * 
   * @throws Exception the exception
   */
  @GET
  @Path("/getFoldersAndFiles/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response getFoldersAndFiles(
  		@QueryParam("driverName") String driverName,
  		@QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String currentPortal,
  		@QueryParam("repositoryName") String repositoryName,
  		@QueryParam("workspaceName") String workspaceName,
  		@QueryParam("filterBy") String filterBy)
  		throws Exception {
    try {
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer()
      	.getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getRepository(repositoryName);
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      ManageDriveService manageDriveService = (ManageDriveService)ExoContainerContext.getCurrentContainer()
      	.getComponentInstanceOfType(ManageDriveService.class);
      
      String driverHomePath = manageDriveService.getDriveByName(Text.escapeIllegalJcrChars(driverName), 
          Text.escapeIllegalJcrChars(repositoryName)).getHomePath();
      String itemPath = driverHomePath
                        + ((currentFolder != null && !"".equals(currentFolder) && !driverHomePath.endsWith("/")) ? "/" : "")
                        + currentFolder;
      ConversationState conversationState = ConversationState.getCurrent();
      String userId = conversationState.getIdentity().getUserId();      
      itemPath = StringUtils.replaceOnce(itemPath, "${userId}", userId);
      Node node = (Node)session.getItem(Text.escapeIllegalJcrChars(itemPath));
      return buildXMLResponseForChildren(node, null, repositoryName, filterBy, session, currentPortal, Text.escapeIllegalJcrChars(driverName));

    } catch (Exception e) {
      log.error("Error when perform getFoldersAndFiles: ", e);
    }    
    
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }
	

  /**
   * Upload file.
   * 
   * @param inputStream the input stream
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param currentFolder the current folder
   * @param jcrPath the jcr path
   * @param uploadId the upload id
   * @param language the language
   * @param contentType the content type
   * @param contentLength the content length
   * @param currentPortal the current portal
   * @param driverName the driver name
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  @POST
  @Path("/uploadFile/upload/")
//  @InputTransformer(PassthroughInputTransformer.class)
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response uploadFile(@Context HttpServletRequest servletRequest,
      @QueryParam("uploadId") String uploadId) throws Exception {
  	return fileUploadHandler.upload(servletRequest, uploadId, limit);
  }

  /**
   * Process upload.
   * 
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param currentFolder the current folder
   * @param jcrPath the jcr path
   * @param action the action
   * @param language the language
   * @param fileName the file name
   * @param uploadId the upload id
   * @param siteName the current portal
   * @param driverName the driver name
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  @GET
  @Path("/uploadFile/control/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response processUpload(
	  @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("driverName") String driverName,
      @QueryParam("currentFolder") String currentFolder,
      @QueryParam("currentPortal") String siteName,
      @QueryParam("userId") String userId,
      @QueryParam("jcrPath") String jcrPath,
      @QueryParam("action") String action,
      @QueryParam("language") String language,
      @QueryParam("fileName") String fileName,
      @QueryParam("uploadId") String uploadId) throws Exception {
    try {
      if ((repositoryName != null) && (workspaceName != null) && (driverName!=null) && (currentFolder!=null)) {
        Node currentFolderNode = getParentFolderNode(repositoryName, Text.escapeIllegalJcrChars(workspaceName), 
            Text.escapeIllegalJcrChars(driverName), Text.escapeIllegalJcrChars(currentFolder));
        return createProcessUploadResponse(repositoryName, Text.escapeIllegalJcrChars(workspaceName), currentFolderNode, 
            siteName, userId, Text.escapeIllegalJcrChars(jcrPath), action, language, fileName, uploadId);
      }
    } catch (Exception e) {
      log.error("Error when perform processUpload: ", e);
    }
    
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }
  
  /**
   * Gets the drivers by user id.
   * 
   * @param repoName the repo name
   * @param userId the user id
   * 
   * @return the drivers by user id
   * 
   * @throws Exception the exception
   */
  private List<DriveData> getDriversByUserId(String repoName, String userId) throws Exception {    
    ManageDriveService driveService = (ManageDriveService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ManageDriveService.class);
    List<String> userRoles = getMemberships(userId);
    return driveService.getDriveByUserRoles(repoName, userId, userRoles);
    /*
    List<DriveData> driveList = new ArrayList<DriveData>();
    List<DriveData> allDrives = driveService.getAllDrives(repoName);
    Set<DriveData> temp = new HashSet<DriveData>();
    if (userId != null) {
      // We will improve ManageDrive service to allow getAllDriveByUser
      for (DriveData driveData : allDrives) {
        String[] allPermission = driveData.getAllPermissions();
        boolean flag = false;
        for (String permission : allPermission) {
          if (permission.equalsIgnoreCase("${userId}")) {
            temp.add(driveData);
            flag = true;
            break;
          }
          if (permission.equalsIgnoreCase("*")) {
            temp.add(driveData);
            flag = true;
            break;
          }
          if (flag)
            continue;
          for (String rolse : userRoles) {
            if (driveData.hasPermission(allPermission, rolse)) {
              temp.add(driveData);
              break;
            }
          }
        }
      }
    } else {
      for (DriveData driveData : allDrives) {
        String[] allPermission = driveData.getAllPermissions();
        for (String permission : allPermission) {
          if (permission.equalsIgnoreCase("*")) {
            temp.add(driveData);
            break;
          }
        }
      }
    }
    
    for(Iterator<DriveData> iterator = temp.iterator();iterator.hasNext();) {
      driveList.add(iterator.next());
    }
    Collections.sort(driveList);
    return driveList;
    */
  }
  
	/**
	 * Append drivers.
	 * 
	 * @param document the document
	 * @param driversList the drivers list
	 * @param groupName the group name
	 * 
	 * @return the element
	 */
  private Element appendDrivers(Document document, List<DriveData> driversList, String groupName, String lang) throws Exception {
    Element folders = document.createElement("Folders");
    folders.setAttribute("name", resolveDriveLabel(groupName, lang));
    folders.setAttribute("isUpload", "false");
    for (DriveData driver : driversList) {      
      String repository = WCMCoreUtils.getRepository(null).getConfiguration().getName();
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
	  } catch (MissingResourceException e) {}
	  return name;
  }
	
  /**
   * Personal drivers.
   * 
   * @param driveList the drive list
   * 
   * @return the list< drive data>
   */
  private List<DriveData> personalDrivers(List<DriveData> driveList, String userId) {
    List<DriveData> personalDrivers = new ArrayList<DriveData>();
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator)
    	ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
    String userPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    for(DriveData drive : driveList) {
      String driveHomePath = StringUtils.replaceOnce(drive.getHomePath(), "${userId}", userId);
      if(driveHomePath.startsWith(userPath + "/" + userId)) {
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
   * @param userId the user id
   * 
   * @return the list< drive data>
   * 
   * @throws Exception the exception
   */
  private List<DriveData> groupDrivers(List<DriveData> driverList, String userId) throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator)
    	ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
    List<DriveData> groupDrivers = new ArrayList<DriveData>();
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    List<String> groups = getGroups(userId);
    for(DriveData drive : driverList) {
      if(drive.getHomePath().startsWith(groupPath)) {
        for(String group : groups) {
          if(drive.getHomePath().equals(groupPath + group)) {
            groupDrivers.add(drive);
            break;
          }
        }
      } 
    }
    Collections.sort(groupDrivers);
    return groupDrivers;
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
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator)
    	ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
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
  	if (organizationService==null) {
  		organizationService = WCMCoreUtils.getService(OrganizationService.class);
  	}
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
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityRegistry identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
    Identity currentUserIdentity = identityRegistry.getIdentity(authenticatedUser);
    return currentUserIdentity.getMemberships();
  }

  /**
   * Gets the groups.
   * 
   * @param userId the user id
   * 
   * @return the groups
   * 
   * @throws Exception the exception
   */
  private List<String> getGroups(String userId) throws Exception {
  	if (organizationService==null) {
  		organizationService = WCMCoreUtils.getService(OrganizationService.class);
  	}
    ((ComponentRequestLifecycle) organizationService).startRequest(manager);
    List<String> groupList = new ArrayList<String> ();
    Collection<?> groups = organizationService.getGroupHandler().findGroupsOfUser(userId);
    Object[] objects = groups.toArray();
    for(int i = 0; i < objects.length; i ++ ){
      Group group = (Group)objects[i];
      String groupPath = null;
      if(group.getParentId() == null || group.getParentId().length() == 0) groupPath = "/" + group.getGroupName(); 
      else groupPath = group.getParentId() + "/" + group.getGroupName(); 
      groupList.add(groupPath);
    }
    return groupList;
  }

  /**
   * Builds the xml response for children.
   * 
   * @param node the node
   * @param command the command
   * @param repositoryName the repository name
   * @param filterBy the filter by
   * @param session the session
   * @param currentPortal TODO
   * @return the response
   * 
   * @throws Exception the exception
   */
  private Response buildXMLResponseForChildren(
  		Node node, String command, String repositoryName, String filterBy, Session session, String currentPortal) throws Exception {  	
  	return buildXMLResponseForChildren(node, command, repositoryName, filterBy, session, currentPortal, null);
  }
  private Response buildXMLResponseForChildren(
	  		Node node, String command, String repositoryName, String filterBy, Session session, String currentPortal, String nodeDriveName) throws Exception {
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

	  	for (NodeIterator iterator = node.getNodes(); iterator.hasNext();) {
	  		Node child = iterator.nextNode();
	  		String fileType = null;

	  		if (child.isNodeType(FCKUtils.EXO_HIDDENABLE))
	  			continue;

	  		if(child.isNodeType("exo:symlink") && child.hasProperty("exo:uuid")) {
	  			sourceNode = session.getNodeByUUID(child.getProperty("exo:uuid").getString());
                        } else {
				sourceNode = child;
	  		}

	  		checkNode = sourceNode != null ? sourceNode : child;

	  		if (isFolder(checkNode, repositoryName)) {
	  			Element folder = createFolderElement(
	  					document, checkNode, checkNode.getPrimaryNodeType().getName(), child.getName(), nodeDriveName);
	  			folders.appendChild(folder);
	  		}

	  		if (FILE_TYPE_ALL.equals(filterBy) && (checkNode.isNodeType(NodetypeConstant.EXO_WEBCONTENT) || checkNode.isNodeType(NodetypeConstant.EXO_ARTICLE) || !isFolder(checkNode, repositoryName))) {
	  		  fileType = FILE_TYPE_ALL;
	  		}
	  		
	  		if (FILE_TYPE_WEBCONTENT.equals(filterBy)) {
	  			if(checkNode.isNodeType(NodetypeConstant.EXO_WEBCONTENT) || checkNode.isNodeType(NodetypeConstant.EXO_ARTICLE)) {
	  				fileType = FILE_TYPE_WEBCONTENT;
	  			}
	  		}

	  		if (FILE_TYPE_MEDIAS.equals(filterBy) && isMediaType(checkNode, repositoryName)){
	  			fileType = FILE_TYPE_MEDIAS;
	  		} 

	  		if (FILE_TYPE_DMSDOC.equals(filterBy) && isDMSDocument(checkNode, repositoryName)) {
	  			fileType = FILE_TYPE_DMSDOC;
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
  private boolean isFolder(Node checkNode, String repositoryName) throws RepositoryException {
    try {
      if (isDocument(checkNode, repositoryName)) {
        return false;
      }
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage());
      }
    }
    
    return 
      checkNode.isNodeType(FCKUtils.NT_UNSTRUCTURED)
        || checkNode.isNodeType(FCKUtils.NT_FOLDER)
        || checkNode.isNodeType(NodetypeConstant.EXO_TAXONOMY);
  }
  
  /**
   * Checks if is dMS document.(not including free layout webcontent & media & article)
   * 
   * @param node the node
   * @param repositoryName the repository name
   * 
   * @return true, if is dMS document
   * 
   * @throws Exception the exception
   */
  private boolean isDMSDocument(Node node, String repositoryName) throws Exception {
  	TemplateService templateService = (TemplateService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(TemplateService.class);
  	List<String> dmsDocumentListTmp = templateService.getDocumentTemplates(repositoryName);
  	List<String> dmsDocumentList = new ArrayList<String>();
  	dmsDocumentList.addAll(dmsDocumentListTmp);
  	dmsDocumentList.remove(NodetypeConstant.EXO_WEBCONTENT);
  	dmsDocumentList.remove(NodetypeConstant.EXO_ARTICLE);
  	for (String documentType : dmsDocumentList) {
	    if (node.getPrimaryNodeType().isNodeType(documentType)
	    		&& !isMediaType(node, repositoryName)
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
   * @param repositoryName repository Name
   * @return true: is document, false: not document;
   * @throws Exception
   */
  private boolean isDocument(Node node, String repositoryName) throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    List<String> documentTypeList = templateService.getDocumentTemplates(repositoryName);
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
   * @param repository the repository
   * 
   * @return true, if is media type
   */
  private boolean isMediaType(Node node, String repository){
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
   * Creates the upload file response.
   * 
   * @param inputStream the input stream
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param runningPortalName the running portal name
   * @param jcrPath the jcr path
   * @param uploadId the upload id
   * @param language the language
   * @param contentType the content type
   * @param contentLength the content length
   * @param currentFolderNode the current folder node
   * @param limit the limit
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  @Deprecated
  protected Response createUploadFileResponse(InputStream inputStream,
                                              String repositoryName,
                                              String workspaceName,
                                              Node currentFolderNode,
                                              String runningPortalName,
                                              String jcrPath,
                                              String uploadId,
                                              String language,
                                              String contentType,
                                              String contentLength,
                                              int limit) throws Exception {
    return fileUploadHandler.upload(uploadId,
                                    contentType,
                                    Double.parseDouble(contentLength),
                                    inputStream,
                                    currentFolderNode,
                                    language,
                                    limit);
  }

  /**
   * Creates the process upload response.
   * 
   * @param repositoryName the repository name
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
  protected Response createProcessUploadResponse(String repositoryName,
                                                 String workspaceName,
                                                 Node currentFolderNode,
                                                 String siteName,
                                                 String userId,
                                                 String jcrPath,
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
  
  /**
   * Gets the parent folder node.
   * 
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param driverName the driver name
   * @param currentFolder the current folder
   * 
   * @return the parent folder node
   * 
   * @throws Exception the exception
   */
  private Node getParentFolderNode(
  		String repositoryName, String workspaceName, String driverName, String currentFolder) throws Exception{
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer()
    	.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getRepository(repositoryName);
    Session session = sessionProvider.getSession(workspaceName, manageableRepository);
    ManageDriveService manageDriveService = (ManageDriveService)ExoContainerContext.getCurrentContainer()
    	.getComponentInstanceOfType(ManageDriveService.class);
    
    try {
	    return (Node)session.getItem(
	    		manageDriveService.getDriveByName(driverName, repositoryName).getHomePath()
	        + ((currentFolder != null && currentFolder.length() != 0) ? "/" : "")
	        + currentFolder);
    } catch (Exception e) {
	    return null;
    }
  }
  
  /**
   * Creates the folder element.
   * 
   * @param document the document
   * @param child the child
   * @param folderType the folder type
   * @param childName the child name
   * 
   * @return the element
   * 
   * @throws Exception the exception
   */
  private Element createFolderElement(Document document, Node child, String folderType, String childName) throws Exception {
  	return createFolderElement(document, child, folderType, childName, null);
  }
  private Element createFolderElement(Document document, Node child, String folderType, String childName, String nodeDriveName) throws Exception {
	  	Element folder = document.createElement("Folder");	  		
	  	folder.setAttribute("name", childName);
	  	folder.setAttribute("title", Utils.getTitle(child));
	  	folder.setAttribute("url", FCKUtils.createWebdavURL(child));
	  	folder.setAttribute("folderType", folderType);
	  	folder.setAttribute("path", child.getPath());
	  	folder.setAttribute("isUpload", "true");
	  	if (nodeDriveName!=null && nodeDriveName.length()>0) folder.setAttribute("nodeDriveName", nodeDriveName);
	  	return folder;
	  }
}
