/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.impl;

import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author benjaminmestrallet
 */
public class Utils {
  private final static Log   LOG          = ExoLogger.getLogger(Utils.class.getName());

  private static final String ILLEGAL_SEARCH_CHARACTERS= "\\!^()+{}[]:\"-";

  public static final String MAPPING_FILE = "mapping.properties";

  public static final String EXO_SYMLINK = "exo:symlink";

  public static final String PRIVATE = "Private";

  public static final String PUBLIC = "Public";


  public static final long KB = 1024L;
  public static final long MB = 1024L*KB;
  public static final long GB = 1024L*MB;

  public static Node makePath(Node rootNode, String path, String nodetype)
      throws PathNotFoundException, RepositoryException {
    return makePath(rootNode, path, nodetype, null);
  }

  @SuppressWarnings("unchecked")
  public static Node makePath(Node rootNode, String path, String nodetype, Map permissions)
      throws PathNotFoundException, RepositoryException {
    String[] tokens = path.split("/") ;
    Node node = rootNode;
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if(token.length() > 0) {
        if(node.hasNode(token)) {
          node = node.getNode(token) ;
        } else {
          node = node.addNode(token, nodetype);
          node.getSession().save();
          node = (Node)node.getSession().getItem(node.getPath());
          if (node.canAddMixin("exo:privilegeable")){
            node.addMixin("exo:privilegeable");
          }
          if(permissions != null){
            ((ExtendedNode)node).setPermissions(permissions);
          }
        }
      }
    }
    rootNode.save();
    return node;
  }

  /**
   * this function used to process import version history for a node
   *
   * @param currentNode
   * @param versionHistorySourceStream
   * @param mapHistoryValue
   * @throws Exception
   */
  public static void processImportHistory(Node currentNode,
                                          InputStream versionHistorySourceStream,
                                          Map<String, String> mapHistoryValue) throws Exception {
    //read stream, get the version history data & keep it inside a map
    Map<String, byte[]> mapVersionHistoryData = getVersionHistoryData (versionHistorySourceStream);

    //import one by one
    for (String uuid : mapHistoryValue.keySet()) {
      for (String name : mapVersionHistoryData.keySet()) {
        if (name.equals(uuid + ".xml")) {
          try {
            byte[] versionHistoryData = mapVersionHistoryData.get(name);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(versionHistoryData);
            String value = mapHistoryValue.get(uuid);
            Node versionableNode = currentNode.getSession().getNodeByUUID(uuid);
            importHistory((NodeImpl) versionableNode,
                          inputStream,
                          getBaseVersionUUID(value),
                          getPredecessors(value),
                          getVersionHistory(value));
            currentNode.getSession().save();
            break;
          } catch (ItemNotFoundException item) {
            currentNode.getSession().refresh(false);
            if (LOG.isErrorEnabled()) {
              LOG.error("Can not found versionable node" + item, item);
            }
          } catch (Exception e) {
            currentNode.getSession().refresh(false);
            if (LOG.isErrorEnabled()) {
              LOG.error("Import version history failed " + e, e);
            }
          }
        }
      }
    }
  }

  /**
   * This function is used to get the version history data which is kept inside the xml files
   * @param versionHistorySourceStream
   * @return a map saving version history data with format: [file name, version history data]
   * @throws IOException
   */
  private static Map<String, byte[]> getVersionHistoryData (InputStream versionHistorySourceStream) throws IOException {
    Map<String, byte[]> mapVersionHistoryData = new HashMap<String, byte[]>();
    ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(versionHistorySourceStream));
    byte[] data = new byte[1024];
    ZipEntry entry = zipInputStream.getNextEntry();
    while (entry != null) {
      //get binary data inside the zip entry
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int available = -1;
      while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
        out.write(data, 0, available);
      }

      //save data into map
      mapVersionHistoryData.put(entry.getName(), out.toByteArray());

      //go to next entry
      out.close();
      zipInputStream.closeEntry();
      entry = zipInputStream.getNextEntry();
    }

    zipInputStream.close();
    return mapVersionHistoryData;
  }

  /**
   * do import a version into a node
   *
   * @param versionableNode
   * @param versionHistoryStream
   * @param baseVersionUuid
   * @param predecessors
   * @param versionHistory
   * @throws RepositoryException
   * @throws IOException
   */
  private static void importHistory(NodeImpl versionableNode,
                                    InputStream versionHistoryStream,
                                    String baseVersionUuid,
                                    String[] predecessors,
                                    String versionHistory) throws RepositoryException, IOException {
    VersionHistoryImporter versionHistoryImporter = new VersionHistoryImporter(versionableNode,
                                                                               versionHistoryStream,
                                                                               baseVersionUuid,
                                                                               predecessors,
                                                                               versionHistory);
    versionHistoryImporter.doImport();
  }

  /**
   * get data from the version history file
   *
   * @param importHistorySourceStream
   * @return
   * @throws Exception
   */
  public static Map<String, String> getMapImportHistory(InputStream importHistorySourceStream) throws Exception {
    ZipInputStream zipInputStream = new ZipInputStream(importHistorySourceStream);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] data = new byte[1024];
    ZipEntry entry = zipInputStream.getNextEntry();
    Map<String, String> mapHistoryValue = new HashMap<String, String>();
    while (entry != null) {
      int available = -1;
      if (entry.getName().equals(MAPPING_FILE)) {
        while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
          out.write(data, 0, available);
        }
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String strLine;
        // Read File Line By Line
        while ((strLine = br.readLine()) != null) {
          // Put the history information into list
          if (strLine.indexOf("=") > -1) {
            mapHistoryValue.put(strLine.split("=")[0], strLine.split("=")[1]);
          }
        }
        // Close the input stream
        inputStream.close();
        zipInputStream.closeEntry();
        break;
      }
      entry = zipInputStream.getNextEntry();
    }
    out.close();
    zipInputStream.close();
    return mapHistoryValue;
  }

  private static String getBaseVersionUUID(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[1];
  }

  private static String[] getPredecessors(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    String strPredecessors = arrHistoryValue[1];
    if (strPredecessors.indexOf(",") > -1) {
      return strPredecessors.split(",");
    }
    return new String[] { strPredecessors };
  }

  private static String getVersionHistory(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[0];
  }

  public static String getPersonalDrivePath(String parameterizedDrivePath, String userId) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    return StringUtils.replaceOnce(parameterizedDrivePath,
                                   nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH) + "/${userId}",
                                   userNode.getPath());
  }

  public static List<PropertyDefinition> getProperties(Node node) throws Exception {
    List<PropertyDefinition> properties = new ArrayList<PropertyDefinition>();
    NodeType nodetype = node.getPrimaryNodeType() ;
    Collection<NodeType> types = new ArrayList<NodeType>() ;
    types.add(nodetype) ;
    NodeType[] mixins = node.getMixinNodeTypes() ;
    if (mixins != null) types.addAll(Arrays.asList(mixins)) ;
    for(NodeType nodeType : types) {
      for(PropertyDefinition property : nodeType.getPropertyDefinitions()) {
        String name = property.getName();
        if(!name.equals("exo:internalUse")&& !property.isProtected()&& !node.hasProperty(name)) {
          properties.add(property);
        }
      }
    }
    return properties;
  }

  public static boolean isInTrash(Node node) throws RepositoryException {
    TrashService trashService = WCMCoreUtils.getService(TrashService.class);
    return trashService.isInTrash(node);
  }

  /**
   * Gets the title.
   *
   * @param node the node
   * @return the title
   * @throws Exception the exception
   */
  public static String getTitle(Node node) throws Exception {
    String title = null;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getValue().getString();
    } else if (node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        } catch (PathNotFoundException ex) {
          title = null;
        } catch(ValueFormatException ex) {
          title = null;
        } catch(RepositoryException ex) {
          title = null;
        } catch(ArrayIndexOutOfBoundsException ex) {
          title = null;
        }
      }
    }
    if (StringUtils.isBlank(title)) {
      if (node.isNodeType("nt:frozenNode")) {
        String uuid = node.getProperty("jcr:frozenUuid").getString();
        Node originalNode = node.getSession().getNodeByUUID(uuid);
        title = originalNode.getName();
      } else {
        title = node.getName();
      }

    }
    return StringEscapeUtils.escapeHtml(Text.unescapeIllegalJcrChars(title));
  }

  public static String escapeIllegalCharacterInQuery(String query) {
    String ret = query;
    if(ret != null) {
      for (char c : ILLEGAL_SEARCH_CHARACTERS.toCharArray()) {
        ret = ret.replace(c + "", "\\" + c);
      }
    }
    return ret;
  }

	/**
	 * Remove the symlink of a deleted node
	 * @param node The deleted node
	 * @throws Exception
	 */
	public static void removeSymlinks(Node node) throws Exception {
		LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
		List<Node> symlinks = linkManager.getAllLinks(node, EXO_SYMLINK);
		for (Node symlink : symlinks) {
			symlink.remove();
		}
	}

  /**
   * Remove deleted Symlink from Trash
   * @param node
   * @throws Exception
   */
  private static void removeDeadSymlinksFromTrash(Node node) throws Exception {
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    List<Node> symlinks = linkManager.getAllLinks(node, EXO_SYMLINK);
    for (Node symlink : symlinks) {
      symlink.remove();
    }
  }
  
  /**
   * Remove all the link of a deleted node
   * @param     : node
   * @param     : keepInTrash true if the link will be move to trash, otherwise set by false
   * @throws    : Exception
   */
  public static void removeDeadSymlinks(Node node, boolean keepInTrash) throws Exception {
    if (isInTrash(node)) {
      removeDeadSymlinksFromTrash(node);
      return;
    }
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    TrashService trashService = WCMCoreUtils.getService(TrashService.class);
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(node);

    try {
      while (!queue.isEmpty()) {
        node = queue.poll();
        if (!node.isNodeType(EXO_SYMLINK)) {
          try {
            List<Node> symlinks = linkManager.getAllLinks(node, EXO_SYMLINK, sessionProvider);
            // Before removing symlinks, We order symlinks by name descending, index descending.
            // Example: symlink[3],symlink[2], symlink[1] to avoid the case that
            // the index of same name symlink automatically changed to increasing one by one
            Collections.sort(symlinks, new Comparator<Node>()
                             {
              @Override
              public int compare(Node node1, Node node2) {
                try {
                  String name1 = node1.getName();
                  String name2 = node2.getName();
                  if (name1.equals(name2)) {
                    int index1 = node1.getIndex();
                    int index2 = node2.getIndex();
                    return -1 * ((Integer)index1).compareTo(index2);
                  }
                  return -1 * name1.compareTo(name2);
                } catch (RepositoryException e) {
                  return 0;
                }
              }
                             });

            for (Node symlink : symlinks) {
              synchronized (symlink) {
                if (keepInTrash) {
                  trashService.moveToTrash(symlink, sessionProvider, 1);
                }else {
                  symlink.remove();
                }
              }
            }
          } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(e.getMessage());
            }
          }
          for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            queue.add(iter.nextNode());
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    } finally {
      sessionProvider.close();
    }
  }

  public static void removeDeadSymlinks(Node node) throws Exception {
    removeDeadSymlinks(node, true);
  }

  public static Node getChildOfType(Node node, String childType) throws Exception {
    if (node == null) {
      return null;
    }
    NodeIterator iter = node.getNodes();
    while (iter.hasNext()) {
      Node child = iter.nextNode();
      if (child.isNodeType(childType)) {
        return child;
      }
    }
    return null;
  }

  public static boolean hasChild(Node node, String childType) throws Exception {
    return (getChildOfType(node, childType) != null);
  }

  /**
   * Get Service Log Content Node of specific service.
   *
   * @param serviceName
   * @return
   * @throws Exception
   */
  private static Node getServiceLogContentNode(SessionProvider systemProvider, String serviceName, String logType) throws Exception {
    // Get workspace and session where store service log
    ManageableRepository repository = WCMCoreUtils.getRepository();
    Session session =
        systemProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
    Node serviceLogContentNode = null;

    if (session.getRootNode().hasNode("exo:services")) {
      // Get service folder
      Node  serviceFolder = session.getRootNode().getNode("exo:services");

      // Get service node
      Node serviceNode = serviceFolder.hasNode(serviceName) ?
                                                             serviceFolder.getNode(serviceName) : serviceFolder.addNode(serviceName, NodetypeConstant.NT_UNSTRUCTURED);

                                                             // Get log node of service
                                                             String serviceLogName =  serviceName + "_" + logType;
                                                             Node serviceLogNode = serviceNode.hasNode(serviceLogName) ?
                                                                                                                        serviceNode.getNode(serviceLogName) : serviceNode.addNode(serviceLogName, NodetypeConstant.NT_FILE);

                                                                                                                        // Get service log content
                                                                                                                        if (serviceLogNode.hasNode(NodetypeConstant.JCR_CONTENT)) {
                                                                                                                          serviceLogContentNode = serviceLogNode.getNode(NodetypeConstant.JCR_CONTENT);
                                                                                                                        } else {
                                                                                                                          serviceLogContentNode = serviceLogNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
                                                                                                                          serviceLogContentNode.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
                                                                                                                          serviceLogContentNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, MediaType.TEXT_PLAIN);
                                                                                                                          serviceLogContentNode.setProperty(NodetypeConstant.JCR_DATA, StringUtils.EMPTY);
                                                                                                                          serviceLogContentNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new Date().getTime());
                                                                                                                        }
    }
    session.save();
    return serviceLogContentNode;
  }
  /**
   * Get Service Log Content Node of specific service.
   *
   * @param serviceName
   * @return
   * @throws Exception
   */
  
  public static Node getServiceLogContentNode(String serviceName, String logType) throws Exception {
    return getServiceLogContentNode(WCMCoreUtils.getSystemSessionProvider(), serviceName, logType);
  }

  /**
   * Get all the templates which have been added into the system
   * @param className Simple name of class.
   * @param id The unique value which used to build service log name.
   * @param skipActivities To skip raising activities on activity stream.
   * @return A Set of templates name which have been added.
   * @throws Exception
  */
  public static Set<String> getAllEditedConfiguredData(String className, String id, boolean skipActivities) throws Exception {
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, skipActivities);
      HashSet<String> editedConfigTemplates = new HashSet<String>();
      Node serviceLogContentNode= getServiceLogContentNode(systemProvider, className, id);
      if (serviceLogContentNode != null) {
        String logData = serviceLogContentNode.getProperty(NodetypeConstant.JCR_DATA).getString();
        editedConfigTemplates.addAll(Arrays.asList(logData.split(";")));
      }
      return editedConfigTemplates;
    } finally {
      systemProvider.close();
    }
  }

  /**
   * Keep the name of templates in jcr:data property at the first time loaded.
   * @param template Name of template which will be kept in jcr:data property
   * @param className A simple class name
   * @param id The unique value which used to build service log name.
   * @param skipActivities To skip raising activities on activity stream.
   * @throws Exception
 */
  public static void addEditedConfiguredData(String template, String className, String id, boolean skipActivities) throws Exception {
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, skipActivities);
      Node serviceLogContentNode = getServiceLogContentNode(systemProvider, className, id);
      if (serviceLogContentNode != null) {
        String logData = serviceLogContentNode.getProperty(NodetypeConstant.JCR_DATA).getString();
        if (StringUtils.isEmpty(logData)) logData = template;
        else if (logData.indexOf(template) == -1) logData = logData.concat(";").concat(template);
        serviceLogContentNode.setProperty(NodetypeConstant.JCR_DATA, logData);
        serviceLogContentNode.getSession().save();
      }
    } finally {
      systemProvider.close();
    }
  }

  public static void removeEditedConfiguredData(String template,
                                                String className,
                                                String id,
                                                boolean skipActivities) throws Exception {
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      DocumentContext.getCurrent()
                     .getAttributes()
                     .put(DocumentContext.IS_SKIP_RAISE_ACT, skipActivities);
      Node serviceLogContentNode = getServiceLogContentNode(systemProvider, className, id);
      if (serviceLogContentNode == null)
        return;
      String logData = serviceLogContentNode.getProperty(NodetypeConstant.JCR_DATA).getString();
      if (StringUtils.isNotBlank(logData)) {
        logData = ";".concat(logData).replace(";".concat(template), StringUtils.EMPTY);
        logData = StringUtils.substring(logData, 1);
        serviceLogContentNode.setProperty(NodetypeConstant.JCR_DATA, logData);
        serviceLogContentNode.getSession().save();
      }
    } finally {
      systemProvider.close();
    }
  }

  public static String getObjectId(String nodePath) throws UnsupportedEncodingException {
    return URLEncoder.encode(nodePath.replaceAll("'", "\\\\'"), "utf-8");
  }

  /**
   * Clean string.
   *
   * @param str the str
   *
   * @return the string
   */
  public static String cleanString(String str) {
    Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
    str = accentsconverter.transliterate(str);
    //the character ? seems to not be changed to d by the transliterate function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
    // delete special character
    for(int i = 0; i < cleanedStr.length(); i++) {
      char c = cleanedStr.charAt(i);
      if(c == ' ') {
        if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
          cleanedStr.deleteCharAt(i--);
        } else {
          c = '-';
          cleanedStr.setCharAt(i, c);
        }
        continue;
      }
      if(i > 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        cleanedStr.deleteCharAt(i--);
        continue;
      }
      if(i > 0 && c == '-' && cleanedStr.charAt(i-1) == '-')
        cleanedStr.deleteCharAt(i--);
    }
    while (StringUtils.isNotEmpty(cleanedStr.toString()) && !Character.isLetterOrDigit(cleanedStr.charAt(0))) {
      cleanedStr.deleteCharAt(0);
    }
    String clean = cleanedStr.toString().toLowerCase();
    if (clean.endsWith("-")) {
      clean = clean.substring(0, clean.length()-1);
    }

    return clean;
  }
  
  /**
   * Clean string. Replace specialChar by "-"
   *
   * @param str the str
   *
   * @return the string
   */

  public static String cleanName(String oldName) {
    if (oldName == null || oldName == "") return oldName;
    String specialChar = "[]/'\":;";
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < oldName.length(); i++) {
      char currentChar = oldName.charAt(i);
      if (specialChar.indexOf(currentChar) > -1) {
        ret.append('-');
      } else {
        ret.append(currentChar);
      }
    }
    return ret.toString();
  }
  public static List<String> getMemberships() throws Exception {
    List<String> userMemberships = new ArrayList<String>();
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if (StringUtils.isNotEmpty(userId)) {
      userMemberships.add(userId);
      Collection<MembershipEntry> memberships = getUserMembershipsFromIdentityRegistry(userId);
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
    if (currentUserIdentity == null) {
      return Collections.<MembershipEntry>emptySet();
    } else {
      return currentUserIdentity.getMemberships();
    }
  }

  public static String getNodeTypeIcon(Node node, String appended, String mode)
      throws RepositoryException {
    if (node == null)
      return "";

    // Primary node type
    String nodeType = node.getPrimaryNodeType().getName();

    // Get real node if node is symlink
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    if (linkManager.isLink(node)) {
      try {
        nodeType = node.getProperty(NodetypeConstant.EXO_PRIMARYTYPE).getString();
        node = linkManager.getTarget(node);
        if (node == null)
          return "";
      } catch (Exception e) {
        return "";
      }
    }

    if (node.isNodeType(NodetypeConstant.EXO_TRASH_FOLDER)) {
      nodeType = NodetypeConstant.EXO_TRASH_FOLDER;
    }
    else if (node.isNodeType(NodetypeConstant.EXO_FAVOURITE_FOLDER)) {
      nodeType = NodetypeConstant.EXO_FAVOURITE_FOLDER;
    }
    else if (nodeType.equals(NodetypeConstant.NT_UNSTRUCTURED) || nodeType.equals(NodetypeConstant.NT_FOLDER)) {
      if ((PRIVATE.equals(node.getName()) || PUBLIC.equals(node.getName()))
           && node.getParent().isNodeType("exo:userFolder")) {
          nodeType = String.format("exo:%sFolder", node.getName().toLowerCase());
      } else {
        for (String specificFolder : NodetypeConstant.SPECIFIC_FOLDERS) {
          if (node.isNodeType(specificFolder)) {
            nodeType = specificFolder;
            break;
          }
        }
      }
    }

    nodeType = nodeType.replace(':', '_');

    // Default css class
    String defaultCssClass;
    if (node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED) || node.isNodeType(NodetypeConstant.NT_FOLDER)) {
      defaultCssClass = "Folder";
    } else if (node.isNodeType(NodetypeConstant.NT_FILE)) {
      defaultCssClass = "File";
    } else {
      defaultCssClass = nodeType;
    }
    defaultCssClass = defaultCssClass.concat("Default");

    StringBuilder str = new StringBuilder();
    str.append(appended);
    str.append(defaultCssClass);
    str.append(" ");
    str.append(appended);
    str.append(nodeType);
    if (mode != null && mode.equalsIgnoreCase("Collapse"))
      str.append(' ').append(mode).append(appended).append(nodeType);
    if (node.isNodeType(NodetypeConstant.NT_FILE)) {
      if (node.hasNode(NodetypeConstant.JCR_CONTENT)) {
        Node jcrContentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
        str.append(' ').append(appended).append(
                                                jcrContentNode.getProperty(NodetypeConstant.JCR_MIMETYPE).getString().toLowerCase().replaceAll(
                                                                                                                                               "/|\\.", ""));
      }
    }
    return str.toString();
  }

  public static String getNodeTypeIcon(Node node, String appended)
      throws RepositoryException {
    return getNodeTypeIcon(node, appended, null);
  }

  /**
   * Check if a node is document type.
   * @param node
   * @return true: is document; false: not document
   * @throws Exception
   */
  public static boolean isDocument(Node node) throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    if (templateService==null) return false;
    List<String> documentTypeList = templateService.getDocumentTemplates();
    if (documentTypeList==null) return false;
    for (String documentType : documentTypeList) {
      if (node.getPrimaryNodeType().isNodeType(documentType)) {
        return true;
      }
    }
    return false;
  }
  /**
   * get Last Modify date of jcr:content of a node
   * 
   * @param node
   * @return Last Modify date of jcr:content of a node
   * @throws Exception
   */
  public static String getJcrContentLastModified(Node node) throws Exception {
    String lastModified = "";
    if(node.hasProperty("jcr:content/jcr:lastModified")){
      lastModified = node.getProperty("jcr:content/jcr:lastModified").getString();
    }else if(node.hasProperty("jcr:content/exo:dateModified")){
      lastModified =node.getProperty("jcr:content/exo:dateModified").getString();
    }else if(node.hasProperty("jcr:content/exo:lastModifiedDate")){
      lastModified =node.getProperty("jcr:content/exo:lastModifiedDate").getString();
    }
    return lastModified;
  }
  
  /**
   * gets the file size in friendly format
   * @param node the file node
   * @return the file size
   * @throws Exception
   */
  public static String fileSize(Node node) throws Exception {
    if (node == null || !node.isNodeType("nt:file")) {
      return "";
    }
    StringBuffer ret = new StringBuffer();
    ret.append(" - ");
    long size = 0;
    try {
      size = node.getProperty("jcr:content/jcr:data").getLength();
    } catch (Exception e) {
      LOG.error("Can not get file size", e);
    }
    long byteSize = size % KB;
    long kbSize = (size % MB) / KB;
    long mbSize = (size % GB) / MB;
    long gbSize = size / GB;

    if (gbSize >= 1) {
      ret.append(gbSize).append(refine(mbSize)).append(" GB");
    } else if (mbSize >= 1) {
      ret.append(mbSize).append(refine(kbSize)).append(" MB");
    } else if (kbSize > 1) {
      ret.append(kbSize).append(refine(byteSize)).append(" KB");
    } else {
      ret.append("1 KB");
    }
    return ret.toString();
  }

  public static boolean isSupportThumbnailView(String mimeType) {
    List<String> thumbnailMimeTypes = new ArrayList<String>();
    List<ComponentPlugin> componentPlugins = WCMCoreUtils.getService(ThumbnailService.class).getComponentPlugins();
    for (ComponentPlugin plugin : componentPlugins) {
      if (plugin instanceof ThumbnailPlugin) {
        thumbnailMimeTypes.addAll(((ThumbnailPlugin) plugin).getMimeTypes());
      }
    }
    return thumbnailMimeTypes.contains(mimeType);
  }

  /**
   * refines the size up to 3 digits, add '0' in front if necessary.
   * @param size the size
   * @return the size in 3 digit format
   */
  private static String refine(long size) {
    if (size == 0) {
      return "";
    }
    String strSize = String.valueOf(size);
    while (strSize.length() < 3) {
      strSize = "0" + strSize;
    }
    return "," + Math.round(Double.valueOf(Integer.valueOf(strSize) / 100.0));
  }
  
  /* check if a node is folder 
   * @param node node to check
   * @return folder or not
   */
  public static boolean isFolder(Node node) throws RepositoryException  {
    return node.isNodeType(NodetypeConstant.NT_FOLDER)
        || node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED);
 }

}
