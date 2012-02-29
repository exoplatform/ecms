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
   * Return template home of repository.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplatesHome(SessionProvider)} instead.
   * @param provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * @see                   Node
   * @throws Exception
   */
  @Deprecated
  public Node getTemplatesHome(String repository,SessionProvider provider) throws Exception ;

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
   * Return the path public template.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplatePathByAnonymous(boolean, String)} instead.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param repository      String
   *                        The name of repository
   * @throws Exception
   */
  @Deprecated
  public String getTemplatePathByAnonymous(boolean isDialog, String nodeTypeName, String repository) throws Exception;

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
   * @throws Exception
   */
  public String getTemplatePathByUser(boolean isDialog, String nodeTypeName, String userName) throws Exception ;

  /**
   * Return the template by user.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplatePathByUser(boolean, String, String)} instead.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param userName        String
   *                        The current user
   * @param repository      String
   *                        The name of repository
   * @see                   Node
   * @see                   Session
   * @throws Exception
   */
  @Deprecated
  public String getTemplatePathByUser(boolean isDialog,
                                      String nodeTypeName,
                                      String userName,
                                      String repository) throws Exception;

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
   * Return path template of the specified node.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplatePath(boolean, String, String)} instead.
   * @param isDialog        boolean
   *                        The boolean value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param templateName    String
   *                        The name of template
   * @param repository      String
   *                        The name of repository
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  @Deprecated
  public String getTemplatePath(boolean isDialog,
                                String nodeTypeName,
                                String templateName,
                                String repository) throws Exception;

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
   * Return template file of the specified node.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplate(String, String, String)} instead.
   * @param templateType    String
   *                        The string value which specify the type of template
   * @param nodeTypeName    String
   *                        The specify name of node type
   * @param templateName    String
   *                        The name of template
   * @param repository      String
   *                        The name of repository
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  @Deprecated
  public String getTemplate(String templateType,
                            String nodeTypeName,
                            String templateName,
                            String repository) throws Exception;

  /**
   * Insert a new template into NodeType by giving the following parameters.
   * @param isDialog            boolean
   *                            The boolean value which specify the type of template
   * @param nodeTypeName        String
   *                            The specify name of NodType
   * @param label               String
   *                            The label of the specified template
   * @param isDocumentTemplate  boolean
   *                            The boolean value which yes or no is DocumentTemplate
   * @param templateName        String
   *                            The name of template
   * @param roles               String[]
   *                            The roles of template
   * @param templateFile        String
   *                            The file of template
   * @param repository          String
   *                            The name of repository
   * @see                       Session
   * @see                       Node
   * @throws Exception
   */
  @Deprecated
  public String addTemplate(boolean isDialog,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            String templateFile,
                            String repository) throws Exception;

  /**
   * Insert a new template into NodeType by giving the following parameters
   *
   * @param templateType String The value which specify the type of template
   * @param nodeTypeName String The specify name of NodType
   * @param label String The label of the specified template
   * @param isDocumentTemplate boolean The boolean value which yes or no is
   *          DocumentTemplate
   * @param templateName String The name of template
   * @param roles String[] The roles of template
   * @param templateFile String The file of template
   * @param repository String The name of repository
   * @deprecated Since WCM 2.1 you should use
   *             {@link #addTemplate(String, String, String, boolean, String, String[], InputStream, String)}
   *             instead
   * @see Session
   * @see Node
   * @throws Exception
   */
  @Deprecated
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            String templateFile,
                            String repository) throws Exception;

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
   *
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use
   *             {@link #addTemplate(String, String, String, boolean, String, String[], InputStream)}
   *             instead.
   * @param templateType The value which specify the type of template
   * @param nodeTypeName The specify name of NodType
   * @param label The label of the specified template
   * @param isDocumentTemplate The boolean value which yes or no is
   *          DocumentTemplate
   * @param templateName The name of template
   * @param roles The roles of template
   * @param templateFile The file of template
   * @param repository The name of repository
   * @see Session
   * @see Node
   * @throws Exception
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile,
                            String repository) throws Exception;

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
   * Insert a new template into NodeType by giving the following parameters.
   *
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use
   *             {@link #addTemplate(String, String, String, boolean, String, String[], InputStream, Node)}
   *             instead.
   * @param templateType The value which specify the type of template
   * @param nodeTypeName The specify name of NodType
   * @param label The label of the specified template
   * @param isDocumentTemplate The boolean value which yes or no is
   *          DocumentTemplate
   * @param templateName The name of template
   * @param roles The roles of template
   * @param templateFile The file of template
   * @param repository The name of repository
   * @param templatesHome Node
   * @see Session
   * @see Node
   * @throws Exception
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile,
                            String repository,
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
   * Get set of folder type.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #buildDocumentTypePattern()} instead.
   * @param repository
   */
  public Set<String> getAllowanceFolderType(String repository);

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
   * Remove a template of NodeType by giving the following parameters.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #removeTemplate(String, String, String)} instead.
   * @param templateType      String
   *                          The value which specify the type of template
   * @param nodeTypeName      String
   *                          The specify name of NodType
   * @param templateName      String
   *                          The name of template
   * @param repository        String
   *                          The name of repository
   * @see                     Session
   * @see                     Node
   * @throws Exception
   */
  public void removeTemplate(String templateType,
                             String nodeTypeName,
                             String templateName,
                             String repository) throws Exception;

  /**
   * Return true if the given repository has document type named 'nodeTypeName'.
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @see                   SessionProvider
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public boolean isManagedNodeType(String nodeTypeName) throws Exception ;

  /**
   * Return true if the given repository has document type named 'nodeTypeName'.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #isManagedNodeType(String)} instead.
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param repository      String
   *                        The name of repository
   * @see                   SessionProvider
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public boolean isManagedNodeType(String nodeTypeName, String repository) throws Exception ;

  /**
   * Get all templates is document type of the specified repository.
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public List<String> getDocumentTemplates() throws Exception ;

  /**
   * Get all templates is document type of the specified repository.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getDocumentTemplates()} instead.
   * @param repository      String
   *                        The name of repository
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public List<String> getDocumentTemplates(String repository) throws Exception ;

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
   * Return all teamplate of the specified NodeType.
   *
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use
   *             {@link #getAllTemplatesOfNodeType(boolean, String, SessionProvider)}
   *             instead.
   * @param isDialog boolean The boolean value which specify the type of
   *          template
   * @param nodeTypeName String The name of NodeType
   * @param repository String The name of repository
   * @param provider SessionProvider The SessionProvider object is used to
   *          managed Sessions
   * @see SessionProvider
   * @see Node
   * @throws Exception
   */
  public NodeIterator getAllTemplatesOfNodeType(boolean isDialog,
                                                String nodeTypeName,
                                                String repository,
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
   * Removes the NodeType by giving the name of NodeType.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #removeManagedNodeType(String)} instead.
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param repository      String
   *                        The name of repository
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  public void removeManagedNodeType(String nodeTypeName, String repository) throws Exception ;

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
   * Return the label of the specified template by giving the following parameters.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplateLabel(String)} instead.
   * @param nodeTypeName    String
   *                        The specified name of NodeType
   * @param repository      String
   *                        The name of repository
   * @see                   SessionProvider
   * @see                   Node
   * @throws Exception
   */
  public String getTemplateLabel(String nodeTypeName, String repository)  throws Exception ;

  /**
   * Return roles of the specified template by giving the following parameters.
   * @param templateType    String
   *                        The value which specify the type of template
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param templateName    String
   *                        The name of template
   * @param repository      String
   *                        The name of repository
   * @deprecated Since WCM 2.1 you should use {@link #getTemplateRoles(Node)} instead.
   * @see                   Session
   * @see                   Node
   * @throws Exception
   */
  @Deprecated
  public String getTemplateRoles(String templateType,
                                 String nodeTypeName,
                                 String templateName,
                                 String repository) throws Exception;

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
   * Return template Node (Name of NodeType, Name of Template) by giving the following parameters.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getTemplateNode(String, String, String, SessionProvider)} instead.
   * @param templateType    String
   *                        The value which specify the type of template
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param templateName    String
   *                        The name of template
   * @param repository      String
   *                        The name of repository
   * @param provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * @see                   SessionProvider
   * @see                   Node
   * @throws Exception
   */
  public Node getTemplateNode(String templateType,
                              String nodeTypeName,
                              String templateName,
                              String repository,
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
   * Get all template that is configured in XML file of specified repository.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #init()} instead.
   * @param repository      String
   *                        The name of repository
   * @see                   TemplatePlugin
   * @throws Exception
   */
  public void init(String repository) throws Exception ;

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
   * @return  List<String>
   * @throws Exception
   */
  public List<String> getAllDocumentNodeTypes() throws Exception;

  /**
   * Get All Document NodeTypes of the specified repository.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getAllDocumentNodeTypes()} instead.
   * @param repository  String
   *                    The name of repository
   * @return  List<String>
   * @throws Exception
   */
  @Deprecated
  public List<String> getAllDocumentNodeTypes(String repository) throws Exception;

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
   * Get path of css file which included in view template.
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #getSkinPath(String, String, String)} instead.
   * @param nodeTypeName  String
   *                      The node type name
   * @param skinName      String
   *                      The name of css file
   * @param locale        String
   *                      The locale which specified by user
   * @param repository    String
   *                      The name of current repository
   * @return              String
   *                      The path of css file
   * @throws Exception
   */
  public String getSkinPath(String nodeTypeName, String skinName, String locale, String repository) throws Exception;

  /**
   * Build string of dialog form template base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildDialogForm(String nodeTypeName) throws Exception;

  /**
   * Build string of dialog form template base on properties of nodetype and repository
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #buildDialogForm(String)} instead.
   * @param nodeTypeName
   * @param repository
   * @return
   */
  public String buildDialogForm(String nodeTypeName, String repository) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildViewForm(String nodeTypeName) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype and repository
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #buildViewForm(String)} instead.
   * @param nodeTypeName
   * @param repository
   * @return
   */
  public String buildViewForm(String nodeTypeName, String repository) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype.
   * @param nodeTypeName
   * @return
   */
  public String buildStyleSheet(String nodeTypeName) throws Exception;

  /**
   * Build string of view template form base on properties of nodetype and repository
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #buildStyleSheet(String)} instead.
   * @param nodeTypeName node's type
   * @param repository
   * @return
   */
  public String buildStyleSheet(String nodeTypeName, String repository) throws Exception;

  /**
   * Insert a template into JCR database as an nt:file node. This method should be used for all the template types.
   * @param templateFolder The parent node which contains the template node
   * @param name The template's name
   * @param data The template's data
   * @param roles The template's roles
   */
  public String createTemplate(Node templateFolder, String name, InputStream data, String[] roles);

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

}
