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
package org.exoplatform.services.cms.folksonomy;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;

/**
 * This service is used to manage all tags and their styles.
 * Currently, it just supports adding/editing/removing the Private & Public tags.
 *
 * @LevelAPI Experimental
 */
public interface NewFolksonomyService {

  /** Property name TAG_RATE_PROP */
  final public static String TAG_RATE_PROP   = "exo:styleRange";

  /** Property name HTML_STYLE_PROP */
  final public static String HTML_STYLE_PROP = "exo:htmlStyle";

  /** Property name EXO_TOTAL */
  final static public String EXO_TOTAL       = "exo:total";

  /** Property name EXO_TAGGED */
  final static public String EXO_TAGGED      = "exo:tagged";

  /** Property name EXO_UUID */
  final static public String EXO_UUID        = "exo:uuid";

  /** Property name EXO_TAGSTYLE */
  final static public String EXO_TAGSTYLE    = "exo:tagStyle";

  /** Property name PUBLIC */
  final static public int    PUBLIC          = 0;

  /** Property name GROUP */
  final static public int    GROUP           = 3;

  /** Property name SITE */
  final static public int    SITE            = 2;

  /** Property name PRIVATE */
  final static public int    PRIVATE         = 1;

  /**
   * Add a private tag to a document. A folksonomy link will be created in a tag node.
   *
   * @param tagsName The array of tag name as the children of tree.
   * @param documentNode Tag this node by creating a folksonomy link to the node in the tag.
   * @param workspace The workspace name.
   * @param userName The user name.
   * @throws Exception The exception
   */
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String workspace,
                            String userName) throws Exception;

  /**
   * Add a group tag to a document. A folksonomy link will be created in a tag node.
   *
   * @param tagsName The array of tag name as the children of tree.
   * @param documentNode Tag this node by creating a folksonomy link to the node in tag.
   * @param workspace The workspace name.
   * @param roles The user roles.
   * @throws Exception The exception
   */
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String workspace,
                           String[] roles) throws Exception;

  /**
   * Add a public tag to a document. A folksonomy link will be created in a tag node.
   *
   * @param treePath The path of folksonomy tree.
   * @param tagsName The array of the tag name as the children of tree.
   * @param documentNode Tag this node by creating a folksonomy link to the node in the tag.
   * @param workspace The workspace name.
   * @throws Exception The exception
   */
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String workspace) throws Exception;

  /**
   * Add a site tag to a document. A folksonomy link will be created in a tag node
   *
   * @param siteName The portal name.
   * @param tagsName The array of the tag name as the children of tree.
   * @param node Tag this node by creating a folksonomy link to the node in tag.
   * @param workspace The workspace name.
   * @throws Exception The exception
   */
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String workspace) throws Exception;

  /**
   * Get all private tags.
   *
   * @param userName The user name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllPrivateTags(String userName) throws Exception;

  /**
   * Get all public tags.
   *
   * @param treePath The folksonomy tree path.
   * @param workspace The workspace name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllPublicTags(String treePath, String workspace) throws Exception;

  /**
   * Get all tags by groups
   *
   * @param role Roles of user
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllGroupTags(String[] role, String workspace) throws Exception;

  /**
   * Get all tags by groups.
   *
   * @param role The roles of user.
   * @param workspace The workspace name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllGroupTags(String role, String workspace) throws Exception;

  /**
   * Get all tags of Site.
   *
   * @param siteName The portal name.
   * @param workspace The workspace name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllSiteTags(String siteName, String workspace) throws Exception;

  /**
   * Get all documents which are stored in a tag and return a list of documents in a tag.
   *
   * @param tagPath The path of the tag.
   * @param workspace The workspace name.
   * @param sessionProvider The sessions provider.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception;

  /**
   * Get HTML_STYLE_PROP property in styleName node in the repository.
   *
   * @param tagPath The path of the tag.
   * @param workspace The workspace name.
   * @return The property's value of styleName node
   * @throws Exception The exception
   */
  public String getTagStyle(String tagPath, String workspace) throws Exception;

  /**
   * Update the properties TAG_RATE_PROP and HTML_STYLE_PROP,
   * following the values tagRate, htmlStyle for a node in tagPath in repository.
   *
   * @param styleName The style name.
   * @param tagRange he range of tag numbers.
   * @param htmlStyle The tag style.
   * @param workspace The workspace name.
   * @throws Exception The exception
   */
  public void addTagStyle(String styleName,
                          String tagRange,
                          String htmlStyle,
                          String workspace) throws Exception;

  /**
   * Update the properties TAG_RATE_PROP and HTML_STYLE_PROP,
   * following the value tagRate, htmlStyle for a node in tagPath in repository.
   *
   * @param styleName The style name.
   * @param tagRange he range of tag numbers.
   * @param htmlStyle The tag style.
   * @param workspace The workspace name.
   * @throws Exception The exception
   */
  public void updateTagStyle(String styleName,
                             String tagRange,
                             String htmlStyle,
                             String workspace) throws Exception;

  /**
   * Get all tag style bases of a folksonomy tree.
   *
   * @param workspace The workspace name.
   * @return List<Node> List of tag styles
   * @throws Exception The exception
   */
  public List<Node> getAllTagStyle(String workspace) throws Exception;

  /**
   * Initialize all TagStylePlugin with session in repository name.
   *
   * @throws Exception The exception
   */
  public void init() throws Exception;

  /**
   * Remove a tag of a given document.
   *
   * @param tagPath The path of the tag.
   * @param document The document which is added a link to tagName.
   * @throws Exception The exception
   */
  public void removeTagOfDocument(String tagPath, Node document, String workspace) throws Exception;

  /**
   * Remove a tag.
   *
   * @param tagPath The path of the tag.
   * @param workspace The workspace name.
   * @throws Exception The exception
   */
  public void removeTag(String tagPath, String workspace) throws Exception;

  /**
   * Modify the tag name.
   *
   * @param tagPath The path of the tag.
   * @param newTagName The new tag name.
   * @param workspace The workspace name.
   * @return Node
   * @throws Exception The exception
   */
  public Node modifyTagName(String tagPath, String newTagName, String workspace) throws Exception;
  
  /**
   * Modify the public tag name.
   *
   * @param tagPath The path of the tag.
   * @param newTagName The new tag name.
   * @param workspace The workspace name.
   * @param treeTagPath The path of the tree tag.
   * @return Node
   * @throws Exception The exception
   */
  public Node modifyPublicTagName(String tagPath, String newTagName, String workspace, String treeTagPath) throws Exception;  
  
  /**
   * Get all tags linked to a given document.
   *
   * @param documentNode The document node.
   * @param workspace The workspace name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String workspace) throws Exception;

  /**
   * Get all tags linked to a given document by scope.
   *
   * @param scope The tag's scope.
   * @param documentNode The document node.
   * @param workspace The workspace name.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String workspace) throws Exception;

  /**
   * Remove all tags linked to the child nodes of a given node.
   *
   * @param node The node.
   * @param workspace The workspace name.
   * @param username The user name.
   * @throws Exception The exception
   */
  public void removeTagsOfNodeRecursively(Node node,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception;

  /**
   * Add given users or groups to tagPermissionList.
   *
   * @param usersOrGroups The users or groups name.
   */
  public void addTagPermission(String usersOrGroups);

  /**
   * Remove given users or groups from tagPermissionList.
   *
   * @param usersOrGroups The users or groups name.
   */
  public void removeTagPermission(String usersOrGroups);

  /**
   * Return tagPermissionList.
   *
   * @return List<String>
   */
  public List<String> getTagPermissionList();

  /**
   * Set the permission to edit a tag for a user.
   *
   * @param scope The tag's scope.
   * @param memberships The memberships.
   * @return True if it is possible
   */
  public boolean canEditTag(int scope, List<String> memberships);

  /**
   * Get all tag names which start within a given scope.
   *
   * @param workspace Workspace
   * @param scope The tags' scope.
   * @param value The value, according to scope, can be understood differently.
   * @return True if it is possible
   * @throws Exception The exception
   */
  public List<String> getAllTagNames(String workspace, int scope, String value) throws Exception;
  
  /**
   * Gets DataDistributionType object
   *
   * @return the DataDistributionType object
   */
  public DataDistributionType getDataDistributionType();
    
}
