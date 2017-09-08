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
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;

/**
 * Manages all tags and their styles.
 * Currently, it just supports adding/editing/removing private and public tags.
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
   * Adds a private tag to a document. A folksonomy link will be created in the tag node.
   *
   * @param tagsName The array of tag names.
   * @param documentNode The document node to which the private tag is added.
   * @param workspace Name of the workspace that contains the document node.
   * @param userName The user who added the private tag.
   * @throws Exception The exception
   */
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String workspace,
                            String userName) throws Exception;

  /**
   * Adds a group tag to a document. A folksonomy link will be created in the tag node.
   *
   * @param tagsName The array of tag names.
   * @param documentNode The document node to which the group tag is added.
   * @param workspace Name of the workspace that contains the document node.
   * @param roles Roles of the user who added the group tag.
   * @throws Exception The exception
   */
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String workspace,
                           String[] roles) throws Exception;

  /**
   * Adds a public tag to a document. A folksonomy link will be created in the tag node.
   *
   * @param treePath Path of the folksonomy tree.
   * @param tagsName The array of the tag names.
   * @param documentNode The document node to which the public tag is added.
   * @param workspace Name of the workspace that contains the document node.
   * @throws Exception The exception
   */
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String workspace) throws Exception;

  /**
   * Adds a site tag to a document. A folksonomy link will be created in the tag node.
   *
   * @param siteName The site name.
   * @param tagsName The array of tag names.
   * @param node The document node to which the site tag is added.
   * @param workspace Name of the workspace that contains the document node.
   * @throws Exception The exception
   */
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String workspace) throws Exception;

  /**
   * Gets all private tags of a given user.
   *
   * @param userName Name of the given user.
   * @return The list of private tags.
   * @throws Exception The exception
   */
  public List<Node> getAllPrivateTags(String userName) throws Exception;

  /**
   * Gets all public tags.
   *
   * @param treePath Path of the folksonomy tree.
   * @param workspace Name of the workspace that contains public tags.
   * @return The list of public tags.
   * @throws Exception The exception
   */
  public List<Node> getAllPublicTags(String treePath, String workspace) throws Exception;

  /**
   * Gets all tags of groups that a given user belongs to.
   *
   * @param role Roles of the given user.
   * @param workspace Name of the workspace that contains tags.
   * @return The tags of groups.
   * @throws Exception The exception
   */
  public List<Node> getAllGroupTags(String[] role, String workspace) throws Exception;

  /**
   * Gets all tags of a group that a given user belongs to.
   *
   * @param role Roles of the given user.
   * @param workspace Name of the workspace that contains tags.
   * @return The tags of the given group.
   * @throws Exception The exception
   */
  public List<Node> getAllGroupTags(String role, String workspace) throws Exception;

  /**
   * Gets all site tags.
   *
   * @param siteName The site name.
   * @param workspace Name of the workspace that contains the site tags.
   * @return The list of site tags.
   * @throws Exception The exception
   */
  public List<Node> getAllSiteTags(String siteName, String workspace) throws Exception;

  /**
   * Gets all documents which are marked with given tags and that are located in a selected path.
   * 
   * @param selectedPath Parent path to filter nodes
   * @param tagPaths list of tags JCR paths
   * @param workspace the workspace of resulted nodes
   * @param sessionProvider use session provider to query JCR
   * 
   * @return the filtered {@link List} of {@link Node}
   * 
   * @throws Exception
   */
  List<Node> getAllDocumentsByTagsAndPath(String selectedPath,
                                           Set<String> tagPaths,
                                           String workspace,
                                           SessionProvider sessionProvider) throws Exception;

  /**
   * Gets all documents which are marked with a given tag.
   *
   * @param tagPath Path of the given tag.
   * @param workspace Name of the workspace that contains the given tag.
   * @param sessionProvider The session provider.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception;

  /**
   * Gets a tag style.
   *
   * @param tagPath Path to the tag.
   * @param workspace Name of the workspace that contains the tag.
   * @return Style values of the tag.
   * @throws Exception The exception
   */
  public String getTagStyle(String tagPath, String workspace) throws Exception;

  /**
   * Adds a tag style.
   *
   * @param styleName Name of the tag style.
   * @param tagRange The number of times the tag is used for the tag style.
   * @param htmlStyle The tag style.
   * @param workspace Name of the workspace that contains the tag style.
   * @throws Exception The exception
   */
  public void addTagStyle(String styleName,
                          String tagRange,
                          String htmlStyle,
                          String workspace) throws Exception;

  /**
   * Updates a tag style.
   *
   * @param styleName Name of the tag style.
   * @param tagRange The number of times the tag is used for the tag style.
   * @param htmlStyle The tag style.
   * @param workspace Name of the workspace that contains the tag style.
   * @throws Exception The exception
   */
  public void updateTagStyle(String styleName,
                             String tagRange,
                             String htmlStyle,
                             String workspace) throws Exception;

  /**
   * Gets all tag styles of a folksonomy tree.
   *
   * @param workspace Name of the workspace that contains the tag styles.
   * @return The tag styles.
   * @throws Exception The exception
   */
  public List<Node> getAllTagStyle(String workspace) throws Exception;

  /**
   * Initializes all tag style plugins.
   *
   * @throws Exception The exception
   */
  public void init() throws Exception;

  /**
   * Initializes the predefined tag permission list
   * @throws Exception
   */
  public void initTagPermissionListCache() throws Exception;

  /**
   * Removes tag from a given document.
   *
   * @param tagPath Path of the tag.
   * @param document The document from which the tag is removed.
   * @throws Exception The exception
   */
  public void removeTagOfDocument(String tagPath, Node document, String workspace) throws Exception;

  /**
   * Removes a tag.
   *
   * @param tagPath Path of the tag.
   * @param workspace Name of the workspace that contains the removed tag.
   * @throws Exception The exception
   */
  public void removeTag(String tagPath, String workspace) throws Exception;

  /**
   * Renames a tag.
   *
   * @param tagPath Path of the tag.
   * @param newTagName New name of the tag.
   * @param workspace Name of the workspace that contains the renamed tag.
   * @return The renamed tag.
   * @throws Exception The exception
   */
  public Node modifyTagName(String tagPath, String newTagName, String workspace) throws Exception;
  
  /**
   * Renames a public tag.
   *
   * @param tagPath Path of the public tag.
   * @param newTagName New name of the public tag.
   * @param workspace Name of the workspace that contains the renamed public tag.
   * @param treeTagPath Path of the folksonomy tree.
   * @return The renamed public tag.
   * @throws Exception The exception
   */
  public Node modifyPublicTagName(String tagPath, String newTagName, String workspace, String treeTagPath) throws Exception;  
  
  /**
   * Gets all tags linked to a given document.
   *
   * @param documentNode The document node.
   * @param workspace Name of the workspace that contains all tags.
   * @return The list of tags.
   * @throws Exception The exception
   */
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String workspace) throws Exception;

  /**
   * Get all tags linked to a given document by scope.
   *
   * @param scope The tag's scope.
   * @param documentNode The document node.
   * @param workspace Name of the workspace that contains all tags.
   * @return The list of tags.
   * @throws Exception The exception
   */
  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String workspace) throws Exception;

  /**
   * Removes all tags linked to the child nodes of a given node.
   *
   * @param node The given node.
   * @param workspace Name of the workspace that contains all tags.
   * @param username The user who removed all tags.
   * @throws Exception The exception
   */
  public void removeTagsOfNodeRecursively(Node node,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception;

  /**
   * Adds a given user or group to the list of tag permissions.
   *
   * @param usersOrGroups Name of the given user or group.
   */
  public void addTagPermission(String usersOrGroups);

  /**
   * Removes a given user or group from the list of tag permissions.
   *
   * @param usersOrGroups Name of the user or group.
   */
  public void removeTagPermission(String usersOrGroups);

  /**
   * Gets a list of users and groups who have the tag permission.
   *
   * @return The list of users and groups.
   */
  public List<String> getTagPermissionList();

  /**
   * Checks if a given user has the "edit tag" permission.
   *
   * @param scope The tag's scope.
   * @param memberships The memberships.
   * @return "True" if the given user has the "edit tag" permission. Otherwise, it returns "false".
   */
  public boolean canEditTag(int scope, List<String> memberships);

  /**
   * Gets names of all tags under a given scope.
   *
   * @param workspace Name of the workspace that contains the tags.
   * @param scope The tags' scope.
   * @param value Path of the folksonomy tree.
   * @return The tag names.
   * @throws Exception The exception
   */
  public List<String> getAllTagNames(String workspace, int scope, String value) throws Exception;
  
  /**
   * Gets a type of data distribution.
   *
   * @return The type of data distribution.
   */
  public DataDistributionType getDataDistributionType();

}
