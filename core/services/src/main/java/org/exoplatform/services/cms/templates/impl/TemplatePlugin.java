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
package org.exoplatform.services.cms.templates.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class TemplatePlugin extends BaseComponentPlugin {

  static final public String   DIALOGS                    = "dialogs";

  static final public String   VIEWS                      = "views";

  static final public String   SKINS                      = "skins";

  static final public String   DEFAULT_DIALOG             = "dialog1";

  static final public String   DEFAULT_VIEW               = "view1";

  static final String[]        UNDELETABLE_TEMPLATES      = { DEFAULT_DIALOG, DEFAULT_VIEW };

  static final public String   DEFAULT_DIALOGS_PATH       = "/" + DIALOGS + "/" + DEFAULT_DIALOG;

  static final public String   DEFAULT_VIEWS_PATH         = "/" + VIEWS + "/" + DEFAULT_VIEW;

  static final public String   NT_UNSTRUCTURED            = "nt:unstructured";

  static final public String   DOCUMENT_TEMPLATE_PROP     = "isDocumentTemplate";

  static final public String   TEMPLATE_LABEL             = "label";

  public static final String[] EXO_ROLES_DEFAULT          = new String[] { "*" };

  private static final String  NAME;

  private static final String  COMMENT_TEMPLATE;

  private static final String  HEADER_VIEW;

  private static final String  JAVA_HEADER_VIEW;

  private static final String  DEF_FIELD_PROPERTY;

  private static final String  FIELD_PROPERTY;

  private static final String  START_DIALOG_FORM;

  private static final String  END_DIALOG_FORM;

  private static final String  TD_LABEL;

  private static final String  TD_COMPONENT;

  private static final String  START_TABLE;

  private static final String  END_TABLE;

  private static final String  START_TR;

  private static final String  END_TR;

  private static final String  CHECK_PROPERTY;

  private static final String  GET_PROPERTY;

  private static final String  START_JAVA;

  private static final String  END_JAVA;

  private static final String  DEFAULT_CSS;
  
  private static final String  JCR_PRIMARY_TYPE = "jcr:primaryType";
  
  private static final String  JCR_MIXIN_TYPES = "jcr:mixinTypes";
  
  private RepositoryService repositoryService_;
  private ConfigurationManager  configManager_;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private String cmsTemplatesBasePath_ ;
  private InitParams params_ ;
  private String storedLocation_ ;
  private boolean autoCreateInNewRepository_=false;
  private Log log = ExoLogger.getLogger("Templateplugin") ;
  private Set<String> configuredNodeTypes;

  private TemplateService templateService;

  static {

    COMMENT_TEMPLATE = "<%\n// Generate template for nodetype automatically\n%>\n";

    HEADER_VIEW = new StringBuilder("<style>\n")
                    .append("\t<% _ctx.include(uicomponent.getTemplateSkin(\"${NodeType}\", \"Stylesheet\")); %>\n")
                    .append("</style>\n").toString();

    JAVA_HEADER_VIEW = new StringBuilder("<%\n\tdef node = uicomponent.getNode();\n")
                        .append("\tdef name = node.getName();\n")
                        .append("\tdef values;\n")
                        .append("\tdef valueDisplay;\n%>").toString();

    DEF_FIELD_PROPERTY = "\t\t\t\t\t<%\n\t\t\t\t\t\tString[] fieldProperty; \n\t\t\t\t\t%>\n";

    FIELD_PROPERTY = "fieldProperty";

    START_DIALOG_FORM = new StringBuilder("<div class=\"UIForm FormLayout FormScrollLayout\">\n")
                        .append("\t<% uiform.begin();\n")
                        .append("\t   /* start render action*/\n")
                        .append("\t   if (uiform.isShowActionsOnTop()) uiform.processRenderAction();\n")
                        .append("\t   /* end render action*/\n")
                        .append("\t%> \n")
                        .append("\t\t<div class=\"HorizontalLayout\">\n").toString();

    NAME = new StringBuilder("\n\t\t\t\t\t<%\n")
            .append("\t\t\t\t\t\tString[] fieldName = [\"jcrPath=/node\", \"editable=if-null\", \"validate=empty,name\"];\n")
            .append("\t\t\t\t\t\tuicomponent.addTextField(\"name\", fieldName);\n")
            .append("\t\t\t\t\t%>\n").toString();

    START_TABLE = "\n\t\t\t<table class=\"UIFormGrid\">\n";

    START_TR = "\n\t\t\t\t<tr>\n";

    END_TR = "\t\t\t\t</tr>";

    START_JAVA = "\n\t\t\t\t<%\n";

    END_JAVA = "\n\t\t\t\t%>";

    CHECK_PROPERTY = "\t\t\t\t if (node.hasProperty(\"${propertyname}\")) {";

    GET_PROPERTY = "node.getProperty(\"${propertyname}\")";

    TD_LABEL = new StringBuilder("").append("\t\t\t\t\t<td class=\"FieldLabel\">")
                .append("<%=_ctx.appRes(\"${nodetypename}.dialog.label.${propertyname}\")%>")
                .append("</td>").toString();

    TD_COMPONENT = new StringBuilder("\n").append("\t\t\t\t\t<td class=\"FieldComponent\">")
                    .append("${contentcomponent}").append("\t\t\t\t\t</td>\n").toString();

    END_TABLE = "\n\t\t\t</table>\n";

    END_DIALOG_FORM = new StringBuilder("").append("\t\t</div>\n")
                        .append("\t<% /* start render action*/\n")
                        .append("\t   if (!uiform.isShowActionsOnTop()) uiform.processRenderAction();\n")
                        .append("\t   /* end render action*/\n")
                        .append("\t   uiform.end();\n\t%>\n").append("</div>").toString();

    DEFAULT_CSS = new StringBuilder(".UIFormGrid {")
                    .append("\n\tborder:1px solid #B7B7B7;")
                    .append("\n\tborder-collapse:collapse;")
                    .append("\n\tmargin:auto;")
                    .append("\n\tpadding-left:1px;")
                    .append("\n\ttable-layout:fixed;")
                    .append("\n}")
                    .append("\n\n.UIFormGrid .FieldLabel {")
                    .append("\n\tfont-weight:bold;")
                    .append("\n\twidth:auto;")
                    .append("\n}")
                    .append("\n\n.UIFormGrid td {")
                    .append("\n\tborder-left:1px solid #CCCCCC;")
                    .append("\n\tborder-right:1px solid #CCCCCC;")
                    .append("\n\tborder-top:1px solid #FFFFFF;")
                    .append("\n\tbackground:#F3F3F3 none repeat scroll 0 0;")
                    .append("\n\theight:20px;")
                    .append("\n\tline-height:20px;")
                    .append("\n\tpadding:4px;")
                    .append("\n}").toString();
  }

  /**
   * DMS configuration which used to store informations
   */
  private DMSConfiguration dmsConfiguration_;

  public TemplatePlugin(InitParams params,
                        RepositoryService jcrService,
                        ConfigurationManager configManager,
                        NodeHierarchyCreator nodeHierarchyCreator,
                        DMSConfiguration dmsConfiguration) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositoryService_ = jcrService;
    configManager_ = configManager;
    cmsTemplatesBasePath_ = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_TEMPLATES_PATH);
    params_ = params;
    ValueParam locationParam = params_.getValueParam("storedLocation") ;
    storedLocation_ = locationParam.getValue();
    ValueParam param = params_.getValueParam("autoCreateInNewRepository");
    if(param!=null) {
      autoCreateInNewRepository_ = Boolean.parseBoolean(param.getValue());
    }
    dmsConfiguration_ = dmsConfiguration;
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  public void init() throws Exception {
    configuredNodeTypes = new HashSet<String>();    
    importPredefineTemplates() ;
  }

  /**
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #init()} instead.
   * @param repository
   * @throws Exception
   */
  @Deprecated
  public void init(String repository) throws Exception {
    if(autoCreateInNewRepository_) {
      importPredefineTemplates() ;
    }
  }

  @SuppressWarnings("unchecked")
  private void addTemplate(TemplateConfig templateConfig, Node templatesHome, String storedLocation) throws Exception {
    NodeTypeManager ntManager = templatesHome.getSession().getWorkspace().getNodeTypeManager() ;
    NodeTypeIterator nodetypeIter = ntManager.getAllNodeTypes();
    List<String> listNodeTypeName = new ArrayList<String>();
    while (nodetypeIter.hasNext()) {
      NodeType n1 = nodetypeIter.nextNodeType();
      listNodeTypeName.add(n1.getName());
    }
    List nodetypes = templateConfig.getNodeTypes();
    TemplateConfig.NodeType nodeType = null ;
    Iterator iter = nodetypes.iterator() ;
    while(iter.hasNext()) {
      nodeType = (TemplateConfig.NodeType) iter.next();
      if (!listNodeTypeName.contains(nodeType.getNodetypeName())) {
        if (log.isErrorEnabled()) {
          log.error("The nodetype: " + nodeType.getNodetypeName() + " doesn't exist!");
        }
        continue;
      }
      Node nodeTypeHome = null;
      nodeTypeHome = Utils.makePath(templatesHome, nodeType.getNodetypeName(), NT_UNSTRUCTURED);
      if(nodeType.getDocumentTemplate())
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, true) ;
      else
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, false) ;

      nodeTypeHome.setProperty(TEMPLATE_LABEL, nodeType.getLabel()) ;

      List dialogs = nodeType.getReferencedDialog();
      addNode(storedLocation, nodeType, dialogs, DIALOGS, templatesHome);

      List views = nodeType.getReferencedView();
      addNode(storedLocation, nodeType, views, VIEWS, templatesHome);

      List skins = nodeType.getReferencedSkin();
      if(skins != null) {
        addNode(storedLocation, nodeType, skins, SKINS, templatesHome);
      }
      configuredNodeTypes.add(nodeType.getNodetypeName());
    }
  }

  public void setBasePath(String basePath) { cmsTemplatesBasePath_ = basePath ; }

  @SuppressWarnings("unchecked")
  private void importPredefineTemplates() throws Exception {
    ManageableRepository repository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    String workspace = dmsRepoConfig.getSystemWorkspace();
    Session session = repository.getSystemSession(workspace) ;
    Node templatesHome = Utils.makePath(session.getRootNode(), cmsTemplatesBasePath_, NT_UNSTRUCTURED);
    TemplateConfig templateConfig = null ;
    Iterator<ObjectParameter> iter = params_.getObjectParamIterator() ;
    while(iter.hasNext()) {
      Object object = iter.next().getObject() ;
      if(!(object instanceof TemplateConfig)) {
        break ;
      }
      templateConfig = (TemplateConfig)object ;
      addTemplate(templateConfig,templatesHome,storedLocation_) ;
    }
    session.logout();
  }

  @SuppressWarnings("unchecked")
  private void addNode(String basePath, TemplateConfig.NodeType nodeType, List templates, String templateType,
      Node templatesHome)  throws Exception {
    for (Iterator iterator = templates.iterator(); iterator.hasNext();) {
      TemplateConfig.Template template = (TemplateConfig.Template) iterator.next();
      String templateFileName = template.getTemplateFile();
      String path = basePath + templateFileName;
      InputStream in = configManager_.getInputStream(path);
      String nodeName = templateFileName.substring(templateFileName.lastIndexOf("/") + 1, templateFileName.indexOf("."));
      Node nodeTypeHome = null;
      if (!templatesHome.hasNode(nodeType.getNodetypeName())) {
        nodeTypeHome = Utils.makePath(templatesHome, nodeType.getNodetypeName(), NT_UNSTRUCTURED);
      } else {
        nodeTypeHome = templatesHome.getNode(nodeType.getNodetypeName());
      }
      Node specifiedTemplatesHome = null;
      try {
        specifiedTemplatesHome = nodeTypeHome.getNode(templateType);
      } catch(PathNotFoundException e) {
        specifiedTemplatesHome = Utils.makePath(nodeTypeHome, templateType, NT_UNSTRUCTURED);
      }
      if(!specifiedTemplatesHome.hasNode(nodeName)) {
        templateService.addTemplate(templateType,
                                    nodeType.getNodetypeName(),
                                    nodeType.getLabel(),
                                    nodeType.getDocumentTemplate(),
                                    nodeName,
                                    template.getParsedRoles(),
                                    in,
                                    templatesHome);
      }
    }
  }

  /**
   * Build default style sheet
   * @param nodeType
   * @return
   */
  public String buildStyleSheet(NodeType nodeType) {
    return COMMENT_TEMPLATE.concat(DEFAULT_CSS);
  }

  /**
   * Build string of dialog form template base on properties of nodetype
   * @param nodeType
   * @return
   */
  public String buildDialogForm(NodeType nodeType) throws ValueFormatException, RepositoryException {
    StringBuilder buildDialogForm = new StringBuilder(COMMENT_TEMPLATE);
    buildDialogForm.append(START_DIALOG_FORM).append(START_TABLE);
    buildDialogForm.append(START_TR);
    buildDialogForm.append(DEF_FIELD_PROPERTY);
    buildDialogForm.append(TD_LABEL.replace("${nodetypename}", nodeType.getName())
                                   .replace(":", "_")
                                   .replace("${propertyname}", "name"));
    buildDialogForm.append(TD_COMPONENT.replace("${contentcomponent}", NAME));
    buildDialogForm.append(END_TR);
    buildDialogForm.append(buildDialogNodeType(nodeType));
    buildDialogForm.append(END_TABLE);
    buildDialogForm.append(END_DIALOG_FORM);
    return buildDialogForm.toString();
  }

  /**
   * Build string of dialog template base on properties of nodetype
   * @param nodeType
   * @return
   */
  private String buildDialogNodeType(NodeType nodeType) throws ValueFormatException, RepositoryException {
    return buildDialogNodeType(nodeType, "/node/");
  }

  /**
   * Build string of dialog template base on properties of nodetype
   * @param nodeType
   * @return
   */
  private String buildDialogNodeType(NodeType nodeType, String jcrPath) throws ValueFormatException,
                                                                       RepositoryException {
    StringBuilder buildDialogNodeType = new StringBuilder();
    StringBuilder componentField;
    String propertyNameFormat;
    String propertyPath;
    String propertyId;
    StringBuilder params;
    StringBuilder validate;
    StringBuilder defaultValues;
    Value[] defaultValuesArr;
    // Render all property defined in NodeType
    PropertyDefinition[] prodefs = nodeType.getPropertyDefinitions();
    for (PropertyDefinition prodef : prodefs) {
      // Dismiss all auto created property
      String propertyName = prodef.getName();
      if (prodef.isAutoCreated() || "*".equals(propertyName)
          || JCR_PRIMARY_TYPE.equals(propertyName) || JCR_MIXIN_TYPES.equals(propertyName))
        continue;
      propertyNameFormat = propertyName.replace(":", "_");
      propertyPath = jcrPath.concat(propertyName);
      propertyId = propertyPath.replace(":", "_");
      componentField = new StringBuilder("\n\t\t\t\t\t\tuicomponent.addTextField(\"").append(propertyId)
                                                                                     .append("\", ");
      validate = new StringBuilder("validate=");
      buildDialogNodeType.append(START_TR);
      buildDialogNodeType.append(TD_LABEL.replace("${nodetypename}", nodeType.getName())
                                         .replace(":", "_")
                                         .replace("${propertyname}", propertyNameFormat));
      params = new StringBuilder("\t\t\t\t\t\t").append(FIELD_PROPERTY)
                                                .append(" = [\"jcrPath=")
                                                .append(propertyPath)
                                                .append("\"");
      if (prodef.isMultiple()) {
        params.append(", \"multiValues=true\"");
      }

      if (prodef.isMandatory()) {
        validate.append("empty,");
      }

      // Select component field base on required type
      switch (prodef.getRequiredType()) {

      case PropertyType.BOOLEAN :
        params.append(", \"options=true,false\"");
        componentField = new StringBuilder("\n\t\t\t\t\t\tuicomponent.addSelectBoxField(\"").append(propertyId)
                                                                                            .append("\", ");
        break;

      case PropertyType.STRING :
        break;

      case PropertyType.DATE :
        validate.append("datetime,");
        params.append(", \"options=displaytime\", \"visible=true\"");
        componentField = new StringBuilder("\n\t\t\t\t\t\tuicomponent.addCalendarField(\"").append(propertyId)
                                                                                           .append("\", ");
        break;

      case PropertyType.LONG :
        validate.append("number,");
        break;

      case PropertyType.DOUBLE :
        validate.append("number,");
        break;

      case PropertyType.REFERENCE :
        params.append(", \"reference=true\", \"editable=false\"");
        break;

      case PropertyType.BINARY :
        componentField = new StringBuilder("\n\t\t\t\t\t\tuicomponent.addUploadField(\"").append(propertyId)
                                                                                         .append("\", ");
        break;

      default:
        break;
      }

      defaultValuesArr = prodef.getDefaultValues();
      if (defaultValuesArr != null) {
        defaultValues = new StringBuilder("defaultValues=");
        for(Value value : defaultValuesArr) {
          defaultValues.append(value.getString()).append(",");
        }
        if (defaultValues.indexOf(",") > -1) {
          params.append(", \"").append(defaultValues.deleteCharAt(defaultValues.length() - 1)).append("\"");
        }
      }

      if (validate.indexOf(",") > -1) {
        params.append(", \"").append(validate.deleteCharAt(validate.length() - 1)).append("\"");
      }

      params.append("];");
      componentField.append(FIELD_PROPERTY).append(");");
      buildDialogNodeType.append(TD_COMPONENT.replace("${contentcomponent}",
                                                      START_JAVA.concat(params.append(componentField)
                                                                              .append(END_JAVA)
                                                                              .append("\n")
                                                                              .toString())));
      buildDialogNodeType.append(END_TR);
    }

    // Render all property for all Child Node if have by call recursive
    NodeDefinition[] childdefs = nodeType.getChildNodeDefinitions();
    for (NodeDefinition childdef : childdefs) {
      if (childdef != null) {
        for (NodeType requiredNodeType : childdef.getRequiredPrimaryTypes()) {
          if (childdef.getName().equals("*")) {
            jcrPath = jcrPath.concat(childdef.getRequiredPrimaryTypes()[0].getName()).concat("/");
          } else {
            jcrPath = jcrPath.concat(childdef.getName()).concat("/");
          }
          buildDialogNodeType.append(buildDialogNodeType(requiredNodeType, jcrPath));
        }
      }
    }
    return buildDialogNodeType.toString();
  }

  /**
   * Build string of view template form base on properties of nodetype
   * @param nodeType
   * @return
   */
  public String buildViewForm(NodeType nodeType) {
    StringBuilder buildViewForm = new StringBuilder(COMMENT_TEMPLATE);
    buildViewForm.append(HEADER_VIEW.replace("${NodeType}", nodeType.getName()));
    buildViewForm.append(JAVA_HEADER_VIEW);
    buildViewForm.append("\n\t\t<div id=\"$uicomponent.id\">");
    buildViewForm.append(START_TABLE);
    buildViewForm.append(START_TR);
    buildViewForm.append(TD_LABEL.replace("${nodetypename}", nodeType.getName())
                                 .replace(":", "_")
                                 .replace("${propertyname}", "name"));
    buildViewForm.append(TD_COMPONENT.replace("${contentcomponent}\t\t\t\t\t", "${name}"));
    buildViewForm.append(END_TR);
    buildViewForm.append(buildViewNodeType(nodeType));
    buildViewForm.append(END_TABLE);
    buildViewForm.append("\t\t</div>");
    return buildViewForm.toString();
  }

  /**
   * Build string of view template base on properties of nodetype
   * @param nodeType
   * @return
   */
  private String buildViewNodeType(NodeType nodeType) {
    StringBuilder buildViewNodeType = new StringBuilder();
    String label = TD_LABEL.replace("${nodetypename}", nodeType.getName().replace(":", "_"));
    PropertyDefinition[] prodefs = nodeType.getPropertyDefinitions();
    for (PropertyDefinition prodef : prodefs) {
      buildViewNodeType.append(START_JAVA).append(CHECK_PROPERTY.replace("${propertyname}", prodef.getName())).append(END_JAVA);
      buildViewNodeType.append(START_TR);
      buildViewNodeType.append(label.replace("${propertyname}", prodef.getName().replace(":", "_")));
      buildViewNodeType.append(START_JAVA);
      if (prodef.getRequiredType() == PropertyType.BINARY) {
        if (prodef.isMultiple()) {
          buildViewNodeType.append("\t\t\t\t\t// Render for multi value;\n");
          buildViewNodeType.append("\t\t\t\t\tvalues = ")
                           .append(GET_PROPERTY.replace("${propertyname}", prodef.getName()))
                           .append(".getValues()")
                           .append(";\n");
          buildViewNodeType.append("\t\t\t\t\tvalueDisplay = \"\";\n");
          buildViewNodeType.append("\t\t\t\t\tfor(value in values) {\n" );
          buildViewNodeType.append("\t\t\t\t\t\tvalueDisplay += \"BINARY DATA\" + \",\";\n" );
          buildViewNodeType.append("\t\t\t\t\t}\n" );
          buildViewNodeType.append("\t\t\t\t\tif (valueDisplay.length() > 0 && valueDisplay.indexOf(\",\") > -1) "
              + "valueDisplay = valueDisplay.substring(0, valueDisplay.length() - 1);");
          
        } else {
          buildViewNodeType.append("\t\t\t\t\t// Render for single value;\n");
          buildViewNodeType.append("\t\t\t\t\tvalueDisplay = \"BINARY DATA\"");
        }
      } else {
        if (prodef.isMultiple()) {
          buildViewNodeType.append("\t\t\t\t\t// Render for multi value;\n");
          buildViewNodeType.append("\t\t\t\t\tvalues = ")
                           .append(GET_PROPERTY.replace("${propertyname}", prodef.getName()))
                           .append(".getValues()")
                           .append(";\n");
          buildViewNodeType.append("\t\t\t\t\tvalueDisplay = \"\";\n");
          buildViewNodeType.append("\t\t\t\t\tfor(value in values) {\n" );
          buildViewNodeType.append("\t\t\t\t\t\tvalueDisplay += value.getString() + \",\";\n" );
          buildViewNodeType.append("\t\t\t\t\t}\n" );
          buildViewNodeType.append("\t\t\t\t\tif (valueDisplay.length() > 0 && valueDisplay.indexOf(\",\") > -1) "
              + "valueDisplay = valueDisplay.substring(0, valueDisplay.length() - 1);");
          
        } else {
          buildViewNodeType.append("\t\t\t\t\t// Render for single value;\n");
          buildViewNodeType.append("\t\t\t\t\tvalueDisplay = ")
                           .append(GET_PROPERTY.replace("${propertyname}", prodef.getName()))
                           .append(".getString();");
        }
      }
      buildViewNodeType.append(END_JAVA);
      buildViewNodeType.append(TD_COMPONENT.replace("${contentcomponent}\t\t\t\t\t", "${valueDisplay}"));
      buildViewNodeType.append(END_TR);
      buildViewNodeType.append(START_JAVA).append("\t\t\t\t\t}").append(END_JAVA);
    }
    return buildViewNodeType.toString();
  }

  public Set<String> getAllConfiguredNodeTypes() {
    return configuredNodeTypes;
  }
}
