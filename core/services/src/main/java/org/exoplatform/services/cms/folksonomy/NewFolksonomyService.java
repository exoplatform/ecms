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

/**
 * Created by The eXo Platform SARL Author : Nguyen Anh Vu anhvurz90@gmail.com
 * Nov 13, 2009 10:52:05 AM
 */
public interface NewFolksonomyService {

  /**
   * Property name TAG_RATE_PROP
   */
  final public static String TAG_RATE_PROP   = "exo:styleRange";

  /**
   * Property name HTML_STYLE_PROP
   */
  final public static String HTML_STYLE_PROP = "exo:htmlStyle";

  final static public String EXO_TOTAL       = "exo:total";

  final static public String EXO_TAGGED      = "exo:tagged";

  final static public String EXO_UUID        = "exo:uuid";

  final static public String EXO_TAGSTYLE    = "exo:tagStyle";

  final static public int    PUBLIC          = 0;

  final static public int    GROUP           = 3;

  final static public int    SITE            = 2;

  final static public int    PRIVATE         = 1;

  /**
   * Add a private tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param repository Repository name
   * @param workspace Workspace name
   * @param userName User name
   * @throws Exception
   */
  @Deprecated
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String repository,
                            String workspace,
                            String userName) throws Exception;
  
  /**
   * Add a private tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param workspace Workspace name
   * @param userName User name
   * @throws Exception
   */
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String workspace,
                            String userName) throws Exception;  

  /**
   * Add a group tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param repository Repository name
   * @param workspace Workspace name
   * @param roles User roles
   * @throws Exception
   */
  @Deprecated
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String repository,
                           String workspace,
                           String[] roles) throws Exception;
  
  /**
   * Add a group tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param workspace Workspace name
   * @param roles User roles
   * @throws Exception
   */
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String workspace,
                           String[] roles) throws Exception;  

  /**
   * Add a public tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param treePath Path of folksonomy tree
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param repository Repository name
   * @param workspace Workspace name
   * @throws Exception
   */
  @Deprecated
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String repository,
                           String workspace) throws Exception;
  
  /**
   * Add a public tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param treePath Path of folksonomy tree
   * @param tagsName Array of tag name as the children of tree
   * @param documentNode Tagging this node by create a folksonomy link to node
   *          in tag
   * @param workspace Workspace name
   * @throws Exception
   */
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String workspace) throws Exception;  

  /**
   * Add a site tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param siteName Portal name
   * @param tagsName Array of tag name as the children of tree
   * @param node Tagging this node by create a folksonomy link to node
   *          in tag
   * @param repository Repository name
   * @param workspace Workspace name
   * @throws Exception
   */
  @Deprecated
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String repository,
                         String workspace) throws Exception;
  
  /**
   * Add a site tag to a document. A folksonomy link will be created in a tag
   * node
   *
   * @param siteName Portal name
   * @param tagsName Array of tag name as the children of tree
   * @param node Tagging this node by create a folksonomy link to node
   *          in tag
   * @param workspace Workspace name
   * @throws Exception
   */
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String workspace) throws Exception;  

  /**
   * Get all private tags
   *
   * @param userName User name
   * @param repository repository name
   * @param workspace Workspace name
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getAllPrivateTags(String userName, String repository, String workspace) throws Exception;

  /**
   * Get all private tags
   *
   * @param userName User name
   * @return List<Node>
   */
  public List<Node> getAllPrivateTags(String userName) throws Exception;
  
  /**
   * Get all public tags
   *
   * @param treePath Folksonomy tree path
   * @param repository Repository name
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllPublicTags(String treePath, String repository, String workspace) throws Exception;
  
  /**
   * Get all public tags
   *
   * @param treePath Folksonomy tree path
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  public List<Node> getAllPublicTags(String treePath, String workspace) throws Exception;  

  /**
   * Get all tags by groups
   *
   * @param roles Roles of user
   * @param repository Repository name
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllGroupTags(String[] roles, String repository, String workspace) throws Exception;
  
  /**
   * Get all tags by groups
   *
   * @param role Roles of user
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  public List<Node> getAllGroupTags(String[] role, String workspace) throws Exception;
  

  /**
   * Get all tags by group
   *
   * @param role Role of user
   * @param repository Repository name
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllGroupTags(String role, String repository, String workspace) throws Exception;
  
  /**
   * Get all tags by group
   *
   * @param role Role of user
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  public List<Node> getAllGroupTags(String role, String workspace) throws Exception;  

  /**
   * Get all tags of Site
   *
   * @param siteName Portal name
   * @param repository Repository name
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllSiteTags(String siteName, String repository, String workspace) throws Exception;
  
  /**
   * Get all tags of Site
   *
   * @param siteName Portal name
   * @param workspace Workspace name
   * @return List<Node>
   * @throws Exception
   */
  public List<Node> getAllSiteTags(String siteName, String workspace) throws Exception;  

  /**
   * Get all documents by tag
   */
  @Deprecated
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String repository,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception;
  
  /**
   * Get all documents by tag
   */
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception;  

  /**
   * Get HTML_STYLE_PROP property in styleName node in repository
   *
   * @param tagPath Tag path
   * @param repository Repository name
   * @param workspace Workspace name
   * @return value of property of styleName node
   * @throws Exception
   */
  @Deprecated
  public String getTagStyle(String tagPath, String repository, String workspace) throws Exception;
  
  /**
   * Get HTML_STYLE_PROP property in styleName node in repository
   *
   * @param tagPath Tag path
   * @param workspace Workspace name
   * @return value of property of styleName node
   * @throws Exception
   */
  public String getTagStyle(String tagPath, String workspace) throws Exception;

  /**
   * Update property TAG_RATE_PROP, HTML_STYLE_PROP following value tagRate,
   * htmlStyle for node in tagPath in repository
   *
   * @param styleName Style name
   * @param tagRange The range of tag numbers
   * @param htmlStyle Tag style
   * @param repository Repository name
   * @param workspace Workspace name
   * @throws Exception
   */
  @Deprecated
  public void addTagStyle(String styleName,
                          String tagRange,
                          String htmlStyle,
                          String repository,
                          String workspace) throws Exception;
  
  /**
   * Update property TAG_RATE_PROP, HTML_STYLE_PROP following value tagRate,
   * htmlStyle for node in tagPath in repository
   *
   * @param styleName Style name
   * @param tagRange The range of tag numbers
   * @param htmlStyle Tag style
   * @param workspace Workspace name
   * @throws Exception
   */
  public void addTagStyle(String styleName,
                          String tagRange,
                          String htmlStyle,
                          String workspace) throws Exception;  

  /**
   * Update property TAG_RATE_PROP, HTML_STYLE_PROP following value tagRate,
   * htmlStyle for node in tagPath in repository
   *
   * @param styleName Style name
   * @param tagRange The range of tag numbers
   * @param htmlStyle Tag style
   * @param repository Repository name
   * @param workspace Workspace name
   * @throws Exception
   */
  @Deprecated
  public void updateTagStyle(String styleName,
                             String tagRange,
                             String htmlStyle,
                             String repository,
                             String workspace) throws Exception;
  
  /**
   * Update property TAG_RATE_PROP, HTML_STYLE_PROP following value tagRate,
   * htmlStyle for node in tagPath in repository
   *
   * @param styleName Style name
   * @param tagRange The range of tag numbers
   * @param htmlStyle Tag style
   * @param workspace Workspace name
   * @throws Exception
   */
  public void updateTagStyle(String styleName,
                             String tagRange,
                             String htmlStyle,
                             String workspace) throws Exception;  

  /**
   * Get all tag style base of folksonomy tree
   *
   * @param repository Repository name
   * @param workspace Workspace name
   * @return List<Node> List tag styles
   * @throws Exception
   */
  @Deprecated
  public List<Node> getAllTagStyle(String repository, String workspace) throws Exception;
  
  /**
   * Get all tag style base of folksonomy tree
   *
   * @param workspace Workspace name
   * @return List<Node> List tag styles
   * @throws Exception
   */
  public List<Node> getAllTagStyle(String workspace) throws Exception;  

  /**
   * Init all TagStylePlugin with session in repository name
   *
   * @param repository repository name
   */
  @Deprecated
  public void init(String repository) throws Exception;
  
  /**
   * Init all TagStylePlugin with session in current repository
   *
   */
  public void init() throws Exception;  

  /**
   * Remove tag of given document
   *
   * @param tagPath tag's path
   * @param document Document which added a link to tagName
   * @param repository Repository name
   * @return
   * @throws Exception
   */
  @Deprecated
  public void removeTagOfDocument(String tagPath, Node document, String repository, String workspace) throws Exception;
  
  /**
   * Remove tag of given document
   *
   * @param tagPath tag's path
   * @param document Document which added a link to tagName
   * @return
   * @throws Exception
   */
  public void removeTagOfDocument(String tagPath, Node document, String workspace) throws Exception;

  /**
   * Remove tag
   *
   * @param tagPath Path of tag
   * @param repository Repository name
   * @param workspace Workspace name
   */
  @Deprecated
  public void removeTag(String tagPath, String repository, String workspace) throws Exception;
  
  /**
   * Remove tag
   *
   * @param tagPath Path of tag
   * @param workspace Workspace name
   */
  public void removeTag(String tagPath, String workspace) throws Exception;

  /**
   * Modify tag name
   *
   * @param tagPath Path of tag
   * @param newTagName New tag name
   * @param repository Repository name
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  @Deprecated
  public Node modifyTagName(String tagPath, String newTagName, String repository, String workspace) throws Exception;
  
  /**
   * Modify tag name
   *
   * @param tagPath Path of tag
   * @param newTagName New tag name
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  public Node modifyTagName(String tagPath, String newTagName, String workspace) throws Exception;  

  /**
   * Get all tags linked to given document
   *
   * @param documentNode Document node
   * @param repository Repository name
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  @Deprecated
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String repository, String workspace) throws Exception;
  
  /**
   * Get all tags linked to given document
   *
   * @param documentNode Document node
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String workspace) throws Exception;  

  /**
   * Get all tags linked to given document by scope
   *
   * @param scope scope of tag
   * @param value
   * @param documentNode Document node
   * @param repository Repository name
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  @Deprecated
  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String repository,
                                                   String workspace) throws Exception;
  
  /**
   * Get all tags linked to given document by scope
   *
   * @param scope scope of tag
   * @param documentNode Document node
   * @param workspace Workspace name
   * @return
   * @throws Exception
   */
  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String workspace) throws Exception;  

  /**
   * Remove all tags linked to children of given node
   *
   * @param node
   * @param repository
   * @param workspace
   * @param username
   * @param groups
   * @throws Exception
   */
  @Deprecated
  public void removeTagsOfNodeRecursively(Node node,
                                          String repository,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception;
  
  /**
   * Remove all tags linked to children of given node
   *
   * @param node
   * @param workspace
   * @param username
   * @throws Exception
   */
  public void removeTagsOfNodeRecursively(Node node,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception;  

  /**
   * Add given users or groups to tagPermissionList
   *
   * @param usersOrGroups
   */
  public void addTagPermission(String usersOrGroups);

  /**
   * Remove given users or groups from tagPermissionList
   *
   * @param usersOrGroups
   */
  public void removeTagPermission(String usersOrGroups);

  /**
   * Returns tagPermissionList
   */
  public List<String> getTagPermissionList();

  /**
   * Can edit tag or not?
   *
   * @param scope Scope of tag
   * @param memberships Memberships
   * @return true If it is possible
   * 
   * @see NewFolksonomyService#canEditTag(Node, int, List)
   * @see NewFolksonomyService#canEditTag(String, String, int, List)
   */
  @Deprecated
  public boolean canEditTag(int scope, List<String> memberships);
  
  /**
   * Can edit tag or not?
   * @param workspace
   * @param tagName
   * @param scope
   * @param memberships
   * @return
   * @throws Exception
   */
  public boolean canEditTag(String workspace, String tagName, int scope, List<String> memberships) throws Exception;
  
  /**
   * Can edit tag or not?
   * @param tagNode
   * @param scope
   * @param memberships
   * @return
   * @throws Exception
   */
  public boolean canEditTag(Node tagNode, int scope, List<String> memberships) throws Exception;

  /**
   * Get all tag names which start within given scope
   *
   * @param repository Repository
   * @param workspace Workspace
   * @param scope scope of tags
   * @param value value, according to scope, can be understood differently
   * @return true If it is possible
   */
  @Deprecated
  public List<String> getAllTagNames(String repository, String workspace, int scope, String value) throws Exception;
  
  /**
   * Get all tag names which start within given scope
   *
   * @param workspace Workspace
   * @param scope scope of tags
   * @param value value, according to scope, can be understood differently
   * @return true If it is possible
   */
  public List<String> getAllTagNames(String workspace, int scope, String value) throws Exception;  
}
