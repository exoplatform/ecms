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
package org.exoplatform.services.cms.templates;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.templates.impl.TemplatePlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;


/**
 * @author benjaminmestrallet
 */
public interface TemplateService {

  static final public String DIALOGS = "dialogs";
  static final public String VIEWS = "views";
  static final public String SKINS = "skins";
  static final public String DEFAULT_DIALOG = "dialog1";
  static final public String DEFAULT_VIEW = "view1";
  static final public String DEFAULT_SKIN = "Stylesheet-lt";

  static final String[] UNDELETABLE_TEMPLATES = {DEFAULT_DIALOG, DEFAULT_VIEW};

  static final public String DEFAULT_DIALOGS_PATH = "/" + DIALOGS + "/" + DEFAULT_DIALOG;
  static final public String DEFAULT_VIEWS_PATH = "/" + VIEWS + "/" + DEFAULT_VIEW;

  static final public String NT_UNSTRUCTURED = "nt:unstructured" ;
  static final public String DOCUMENT_TEMPLATE_PROP = "isDocumentTemplate" ;
  static final public String TEMPLATE_LABEL = "label" ;

  static final public String RTL = "rtl";
  static final public String LTR = "ltr";

  /**
   * Return path of default template by giving the following parameters.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The name of NodeType
   */
  public String getDefaultTemplatePath(boolean isDialog, String nodeTypeName) ;

  /**
   * Return template home of current repository.
   * @param provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * @see                   Node
   * @throws Exception
   */
  public Node getTemplatesHome(SessionProvider provider) throws Exception ;

  /**
   * Return path template of the specified node.
   * @param node            Node
   *                        The specified node
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @see                   Node
   * @throws Exception
   */
  public String getTemplatePath(Node node, boolean isDialog) throws Exception ;


  /**
   * Return the path public template.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @throws Exception
   */
  public String getTemplatePathByAnonymous(boolean isDialog, String nodeTypeName) throws Exception;

  /**
   * Return the template by user.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param userName        String
   *                        The current user
   * @see                   Node
   * @see                   Session
   * @throws RepositoryException
   */
  public String getTemplatePathByUser(boolean isDialog, String nodeTypeName, String userName) throws RepositoryException;

  /**
   * Return path template of the specified node.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param templateName    String
   *                        The name of template
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public String getTemplatePath(boolean isDialog, String nodeTypeName, String templateName) throws Exception ;

  /**
   * Return template file of the specified node.
   * @param templateType    String
   *                        The string value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param templateName    String
   *                        The name of template
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public String getTemplate(String templateType, String nodeTypeName, String templateName) throws Exception ;

  /**
   * Insert a new template into NodeType by giving the following parameters.
   * @param templateType        The value which specify the type of template
   * @param nodeTypeName        The specify name of NodType
   * @param label               The label of the specified template
   * @param isDocumentTemplate  The boolean value which yes or no is DocumentTemplate
   * @param templateName        The name of template
   * @param roles               The roles of template
   * @param templateFile        The file of template
   * @see                       Session
   * @see                       Node
   * @throws Exception
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile) throws Exception;

  /**
   * Insert a new template into NodeType by giving the following parameters.
   * @param templateType        The value which specify the type of template
   * @param nodeTypeName        The specify name of NodType
   * @param label               The label of the specified template
   * @param isDocumentTemplate  The boolean value which yes or no is DocumentTemplate
   * @param templateName        The name of template
   * @param roles               The roles of template
   * @param templateFile        The file of template
   * @param templatesHome       Node
   * @see                       Session
   * @see                       Node
   * @throws Exception
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile,
                            Node templatesHome) throws Exception;

  /**
   * This method is used to filter types of NodeType which is added in folder.
   * @param filterPlugin      Content filer plugin
   * @throws Exception
   */
  public void addContentTypeFilterPlugin(ContentTypeFilterPlugin filterPlugin) throws Exception;

  /**
   * Get set of folder type.
   */
  public Set<String> getAllowanceFolderType();

  /**
   * Remove a template of NodeType by giving the following parameters.
   * @param templateType      String
   *                          The value which specify the type of template
   * @param nodeTypeName      String
   *                          The specify name of NodType
   * @param templateName      String
   *                          The name of template
   * @see                     Session
   * @see                     Node
   * @throws Exception
   */
  public void removeTemplate(String templateType, String nodeTypeName, String templateName) throws Exception;

  /**
   * Return true if the given repository has document type named 'nodeTypeName'.
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @see                   SessionProvider
   * @see                   Session
   * @see                   Node
   * @throws RepositoryException
   */
  public boolean isManagedNodeType(String nodeTypeName) throws RepositoryException ;

  /**
   * Get all templates is document type of the specified repository.
   * @see                   Session
   * @see                   Node
   * @throws RepositoryException
   */
  public List<String> getDocumentTemplates() throws RepositoryException ;

  /**
   * Return all teamplate of the specified NodeType.
   *
   * @param isDialog boolean The boolean value which specify the type of
   *          template
   * @param nodeTypeName String The name of NodeType
   * @param provider SessionProvider The SessionProvider object is used to
   *          managed Sessions
   * @see SessionProvider
   * @see Node
   * @throws Exception
   */
  public NodeIterator getAllTemplatesOfNodeType(boolean isDialog,
                                                String nodeTypeName,
                                                SessionProvider provider) throws Exception;

  /**
   * Removes the NodeType by giving the name of NodeType.
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public void removeManagedNodeType(String nodeTypeName) throws Exception ;

  /**
   * Return the label of the specified template by giving the following parameters.
   * @param nodeTypeName    String
   *                        The specified name of NodeType
   * @see                   SessionProvider
   * @see                   Node
   * @throws Exception
   */
  public String getTemplateLabel(String nodeTypeName)  throws Exception ;

  /**
   * Return template Node (Name of NodeType, Name of Template) by giving the following parameters.
   * @param templateType    String
   *                        The value which specify the type of template
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param templateName    String
   *                        The name of template
   * @param provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * @see                   SessionProvider
   * @see                   Node
   * @throws Exception
   */
  public Node getTemplateNode(String templateType,
                              String nodeTypeName,
                              String templateName,
                              SessionProvider provider) throws Exception;

  /**
   * Return CreationableContent Types to the given node.
   * @param node          The specified node
   * @see                 Node
   * @throws Exception
   */
  public List<String> getCreationableContentTypes(Node node) throws Exception;

  /**
   * Get all template that is configured in XML file of current repository.
   * @see                   TemplatePlugin
   * @throws Exception
   */
  public void init() throws Exception ;

  /**
   * Remove all templates cached.
   *
   */
  public void removeAllTemplateCached();

  /**
   * Remove cache of template.
   * @param templatePath String
   *                     jcr path of template
   * @throws Exception
   */
  public void removeCacheTemplate(String templatePath) throws Exception;


  /**
   * Get All Document NodeTypes of the current repository.
   * @return  all document nodetypes
   */
  public List<String> getAllDocumentNodeTypes() throws PathNotFoundException, RepositoryException;

  /**
   * Get path of css file which included in view template.
   * @param nodeTypeName  String
   *                      The node type name
   * @param skinName      String
   *                      The name of css file
   * @param locale        String
   *                      The locale which specified by user
   * @return              String
   *                      The path of css file
   * @throws Exception
   */
  public String getSkinPath(String nodeTypeName, String skinName, String locale) throws Exception;

  /**
   * Build string of dialog form template base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildDialogForm(String nodeTypeName) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildViewForm(String nodeTypeName) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildStyleSheet(String nodeTypeName) throws Exception;

  /**
   * Insert a template into JCR database as an nt:file node. This method should be used for all the template types.
   * @param templateFolder The parent node which contains the template node
   * @param name The template's name
   * @param data The template's data
   * @param roles The template's roles
   */
  @Deprecated
  public String createTemplate(Node templateFolder, String name, InputStream data, String[] roles);

  /**
   * Insert a template into JCR database as an nt:file node. This method should be used for all the template types.
   * @param templateFolder The parent node which contains the template node
   * @param title The template title
   * @param templateName The template's name
   * @param data The template's data
   * @param roles The template's roles
   */
  public String createTemplate(Node templateFolder, String title, String templateName, InputStream data, String[] roles);

  /**
   * Update a template inside JCR database. This method should be used for all the template types.
   * @param template The template node
   * @param data The template's data
   * @param roles The template's roles
   */
  public String updateTemplate(Node template, InputStream data, String[] roles);

  /**
   * Get a template from JCR database. This method should be used for all the template types.
   * @param template The template node
   */
  public String getTemplate(Node template);

  /**
   * Get roles from a template. This method should be used for all the template types.
   * @param template The template node
   */
  public String getTemplateRoles(Node template);

  /**
   * gets all node types configured
   * @return
   */
  public Set<String> getAllConfiguredNodeTypes();

  /**
   * Gets all node types configured whose templates were edited.
   * @return list of String
   */
  public Set<String> getAllEditedConfiguredNodeTypes() throws Exception;

}
