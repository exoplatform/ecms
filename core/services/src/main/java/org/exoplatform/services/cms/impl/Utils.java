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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * @author benjaminmestrallet
 */
public class Utils {
  private final static Log   LOG          = ExoLogger.getLogger(Utils.class);

  private static final String ILLEGAL_SEARCH_CHARACTERS= "\\!^()+{}[]:\"-";

  public static final String MAPPING_FILE = "mapping.properties";

  public static final String EXO_SYMLINK = "exo:symlink";
  
  private static LinkManager linkManager;
  private static TrashService trashService;
  private static NodeHierarchyCreator nodeHierarchyCreator;
  private static SessionProviderService sessionProviderService;
  
  static {
    linkManager = WCMCoreUtils.getService(LinkManager.class);
    trashService = WCMCoreUtils.getService(TrashService.class);
    nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    sessionProviderService = WCMCoreUtils.getService(SessionProviderService.class);
  }
    
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

    for (String uuid : mapHistoryValue.keySet()) {
      ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(versionHistorySourceStream));
      byte[] data = new byte[1024];
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ZipEntry entry = zipInputStream.getNextEntry();
      while (entry != null) {
        int available = -1;
        if (entry.getName().equals(uuid + ".xml")) {
          while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
            out.write(data, 0, available);
          }
          try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
            String value = mapHistoryValue.get(uuid);
            Node versionableNode = currentNode.getSession().getNodeByUUID(uuid);
            importHistory((NodeImpl) versionableNode,
                          inputStream,
                          getBaseVersionUUID(value),
                          getPredecessors(value),
                          getVersionHistory(value));
            currentNode.getSession().save();
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
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        } else {
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        }
      }
      out.close();
      zipInputStream.close();
    }
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
        } catch (Exception ex) {
          title = null;
        }
      }
    }
    if (title == null) {
      if (node.isNodeType("nt:frozenNode")) {
        String uuid = node.getProperty("jcr:frozenUuid").getString();
        Node originalNode = node.getSession().getNodeByUUID(uuid);
        title = originalNode.getName();
      } else {
        title = node.getName();
      }

    }
    return Text.unescapeIllegalJcrChars(title);
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

  private static String trashPath;
  private static String trashWorkspace;
  private static SessionProvider sessionProvider;
  
  public static void removeDeadSymlinks(Node node) throws Exception {
    if (isInTrash(node)) {
      return; 
    }
    trashPath = nodeHierarchyCreator.getJcrPath(BasePath.TRASH_PATH);
    trashWorkspace = WCMCoreUtils.getService(RepositoryService.class).getCurrentRepository().
                                  getConfiguration().getDefaultWorkspaceName();
    sessionProvider = sessionProviderService.getSystemSessionProvider(null);
  
    removeDeadSymlinksRecursively(node);
  }
  
  private static void removeDeadSymlinksRecursively(Node node) throws Exception {
    if (!node.isNodeType(EXO_SYMLINK)) {
      try {
          List<Node> symlinks = linkManager.getAllLinks(node, EXO_SYMLINK);
          for (Node symlink : symlinks) {
            synchronized (symlink) {
              trashService.moveToTrash(symlink, trashPath, trashWorkspace, sessionProvider, 1);
            }
          }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      try {
        List<Node> childNodes = new ArrayList<Node>();
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
          childNodes.add(iter.nextNode());
        }
        for (Node child : childNodes) {
          removeDeadSymlinksRecursively(child);
        }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
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
  
}
