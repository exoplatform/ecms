/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.formgenerator;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.form.UIFormRichtextInput;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormHiddenInput;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.validator.IdentifierValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

import com.ibm.icu.text.Transliterator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 22, 2009
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/FormGeneratorPortlet/UIFormGeneratorTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UIFormGeneratorTabPane.SaveActionListener.class),
      @EventConfig(listeners = UIFormGeneratorTabPane.ResetActionListener.class, phase = Phase.DECODE)
    }
)
public class UIFormGeneratorTabPane extends UIFormTabPane {

  /** The Constant PROPERTY_PREFIX. */
  public static final String PROPERTY_PREFIX = "exo:fg_p_";

  /** The Constant NODE_PREFIX. */
  public static final String NODE_PREFIX = "exo:";

  /** The Constant NODE_SUFFIX. */
  public static final String NODE_SUFFIX = "_fg_n";
  
  /** The Constant INPUT_NAME_PREFIX_NUM. */
  public static final String INPUT_NAME_PREFIX = "num";

  /**
   * Instantiates a new uI form generator tab pane.
   *
   * @throws Exception the exception
   */
  public UIFormGeneratorTabPane() throws Exception {
    super(UIFormGeneratorConstant.FORM_GENERATOR_TABPANE);

    UIFormInputSet formGeneratorGeneralTab = new UIFormInputSet(UIFormGeneratorConstant.FORM_GENERATOR_GENERAL_TAB);
    UIFormStringInput nameFormStringInput = new UIFormStringInput(UIFormGeneratorConstant.NAME_FORM_STRING_INPUT,
                                                                  UIFormGeneratorConstant.NAME_FORM_STRING_INPUT,
                                                                  null);
    nameFormStringInput.addValidator(IdentifierValidator.class);
    nameFormStringInput.addValidator(MandatoryValidator.class);
    nameFormStringInput.addValidator(ECMNameValidator.class);
    formGeneratorGeneralTab.addUIFormInput(nameFormStringInput);
    formGeneratorGeneralTab.addUIFormInput(new UIFormHiddenInput(UIFormGeneratorConstant.JSON_OBJECT_FORM_GENERATOR,
                                                                 UIFormGeneratorConstant.JSON_OBJECT_FORM_GENERATOR,
                                                                 null));
    formGeneratorGeneralTab.addUIFormInput(new UIFormRichtextInput(UIFormGeneratorConstant.DESCRIPTION_FORM_WYSIWYG_INPUT,
                                                                   UIFormGeneratorConstant.DESCRIPTION_FORM_WYSIWYG_INPUT,
                                                                   ""));
    // formGeneratorGeneralTab.addUIFormInput(new
    // UIFormUploadInput(UIFormGeneratorConstant.ICON_FORM_UPLOAD_INPUT,
    // UIFormGeneratorConstant.ICON_FORM_UPLOAD_INPUT));
    addUIFormInput(formGeneratorGeneralTab).setRendered(true);

    addChild(UIFormGeneratorDnDTab.class, null, null).setRendered(true);

    setSelectedTab(formGeneratorGeneralTab.getId());
  }

  /**
   * Clean string.
   *
   * @param str the str
   *
   * @return the string
   */
  private static String cleanString(String str) {
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
            c = '_';
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
      
      if (!Character.isLetterOrDigit(cleanedStr.charAt(0))) {
        cleanedStr.deleteCharAt(0);
      }
      
      if (cleanedStr.length() > 0 && !Character.isLetterOrDigit(cleanedStr.charAt(cleanedStr.length()-1))) {
        cleanedStr.deleteCharAt(cleanedStr.length()-1);
      }
      
      return cleanedStr.toString().toLowerCase().replaceAll("-","_");
  }

  /**
   * Gets the number require type.
   *
   * @param formType the form type
   * @param size the size
   *
   * @return the number require type
   */
  private int getNumberRequireType(String formType, int size) {
    if (UIFormGeneratorConstant.UPLOAD.equals(formType))
      return PropertyType.BINARY;
    else if (UIFormGeneratorConstant.DATE.equals(formType))
      return PropertyType.DATE;
    else if (UIFormGeneratorConstant.CHECKBOX.equals(formType))
      return PropertyType.BOOLEAN;
    else
      return PropertyType.STRING;
  }

  /**
   * Gets the nodetype name.
   *
   * @param nodetypeName the nodetype name
   *
   * @return the nodetype name
   */
  private String getNodetypeName(String nodetypeName) {
    /*
     * PREFIX is used to declare the nodetype inside "exo" namespace
     * SUFFIX is used to maintain a more logical alphabetic order by nodetype but preserves unicity.
     */
    return NODE_PREFIX + cleanString(nodetypeName) + NODE_SUFFIX;
  }

  /**
   * Gets the property name.
   *
   * @param inputName the input name
   *
   * @return the property name
   */
  private String getPropertyName(String inputName) {
    return PROPERTY_PREFIX + cleanString(inputName);
  }

  /**
   * Adds the nodetype.
   *
   * @param requestContext the request context
   * @param repository the repository
   * @param nodetypeName the nodetype name
   * @param formBeans the form beans
   *
   * @throws Exception the exception
   */
  private void addNodetype(WebuiRequestContext requestContext,
                           String repository,
                           String nodetypeName,
                           List<UIFormGeneratorInputBean> formBeans) throws Exception {
    NodeTypeValue newNodeType = new NodeTypeValue() ;
    newNodeType.setName(nodetypeName) ;
    newNodeType.setPrimaryItemName(null);
    newNodeType.setMixin(false) ;
    newNodeType.setOrderableChild(false) ;
    List<String> supertypes = new ArrayList<String>();
    supertypes.add("nt:base");

    List<PropertyDefinitionValue> properties = new ArrayList<PropertyDefinitionValue>();
    List<NodeDefinitionValue>  childNodesDefinitions = new ArrayList<NodeDefinitionValue>();

    for (UIFormGeneratorInputBean form : formBeans) {
      PropertyDefinitionValue property = new PropertyDefinitionValue() ;
      property.setName(getPropertyName(formatInputName(form.getName()))) ;
      property.setRequiredType(getNumberRequireType(form.getType(), formBeans.size())) ;
      property.setMultiple(false) ;
      property.setMandatory(form.isMandatory()) ;
      property.setAutoCreate(false) ;
      property.setReadOnly(false) ;
      property.setOnVersion(OnParentVersionAction.COPY) ;
      property.setValueConstraints(null) ;
      properties.add(property) ;
      String inputType = form.getType();
      //if (UIFormGeneratorConstant.UPLOAD.equals(inputType) && !supertypes.contains("nt:file")) supertypes.add("nt:file");
      if (UIFormGeneratorConstant.UPLOAD.equals(inputType)) {
        String inputFieldName = cleanString(form.getName()) + "FieldName";
        String childName = "jcr:content" +  inputFieldName;
        NodeDefinitionValue nodeDef = new NodeDefinitionValue(childName,
                                                              false,
                                                              false,
                                                              OnParentVersionAction.VERSION,
                                                              false,
                                                              "nt:resource",
                                                              Arrays.asList(new String[] { "nt:resource" }),
                                                              true);
        childNodesDefinitions.add(nodeDef);
      }
    }
    newNodeType.setDeclaredSupertypeNames(supertypes);
    newNodeType.setDeclaredPropertyDefinitionValues(properties) ;

    newNodeType.setDeclaredChildNodeDefinitionValues(childNodesDefinitions);
    try {
      ExtendedNodeTypeManager extendedNodeTypeManager = getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                                                        .getNodeTypeManager();
      extendedNodeTypeManager.registerNodeType(newNodeType, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
    } catch (Exception e) {
      Utils.createPopupMessage(this, "UIFormGeneratorTabPane.msg.register-failed", null, ApplicationMessage.WARNING);
    }
  }

  /**
   * Generate dialog template.
   *
   * @param forms the forms
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  private String generateDialogTemplate(String templateName, List<UIFormGeneratorInputBean> forms) throws Exception {
    int numberFormUploadInput = 0;
    for (int i = 0; i < forms.size(); i++) {
      UIFormGeneratorInputBean form = forms.get(i);
      String inputType = form.getType();
      if (UIFormGeneratorConstant.UPLOAD.equals(inputType)) numberFormUploadInput++;
    }
    StringBuilder dialogTemplate = new StringBuilder();
    dialogTemplate.append("<%\n");
    dialogTemplate.append(" import javax.jcr.Node;\n");
    dialogTemplate.append(" import java.util.Calendar;\n");
    dialogTemplate.append(" import java.text.SimpleDateFormat;\n");
    dialogTemplate.append(" import org.exoplatform.download.DownloadService;\n");
    dialogTemplate.append(" import org.exoplatform.download.InputStreamDownloadResource;\n");
    dialogTemplate.append(" import java.io.InputStream;\n");

    dialogTemplate.append(" private String getTimestampName() {\n");
    dialogTemplate.append("   Calendar now = Calendar.getInstance();\n");
    dialogTemplate.append("   SimpleDateFormat formatter = new SimpleDateFormat(\"yyyy.MM.dd '-' hh'h'mm'm'ss\");\n");
    dialogTemplate.append("   return formatter.format(now.getTime());\n");
    dialogTemplate.append(" }\n");
    dialogTemplate.append(" String timestampName = getTimestampName();\n");
    dialogTemplate.append(" %>\n");
    dialogTemplate.append("<!--DIALOG_BEGIN-->\n");

    dialogTemplate.append("<div class=\"UIForm FormLayout FormScrollLayout\">\n");
    dialogTemplate.append("  <% uiform.begin() \n");
    dialogTemplate.append("     /* start render action*/\n");
    dialogTemplate.append("     if (uiform.isShowActionsOnTop()) uiform.processRenderAction();\n");
    dialogTemplate.append("     /* end render action*/\n");
    dialogTemplate.append("  %>\n");
    dialogTemplate.append("    <div class=\"HorizontalLayout\">\n");
    dialogTemplate.append("      <table class=\"UIFormGrid\">\n");

    /* in WCM 1.2, we disable name and use a automatic timestamp name.
     * We will offer the possibility to choose in WCM 1.3
     * (show name, use timestamp, convert from another field like exo:title for example)
     */
    dialogTemplate.append("        <tr style=\"display:none;\">\n");
    dialogTemplate.append("          <td class=\"FieldLabel\">" +
    		"<label for=\"name\"><%=_ctx.appRes(\"" + templateName + ".label.Date\")%></label></td>\n");
    dialogTemplate.append("          <td class=\"FieldComponent\">\n");
    dialogTemplate.append("            $timestampName \n               <div style=\"display:none;\"><%\n");
    dialogTemplate.append("              String[] fieldName = [\"jcrPath=/node\", \"mixintype=mix:i18n\", ");
    dialogTemplate.append("\"editable=if-null\", \"validate=empty,name\", timestampName] ;\n");
    dialogTemplate.append("              uicomponent.addTextField(\"name\", fieldName) ;\n");
    dialogTemplate.append("            %></div>\n");
    dialogTemplate.append("          </td>\n");
    dialogTemplate.append("        </tr>\n");
    dialogTemplate.append("<%\n");
    dialogTemplate.append("String[] hiddenField1;\n");
    dialogTemplate.append("String[] hiddenField2;\n");
    dialogTemplate.append("String[] hiddenField3;\n");
    dialogTemplate.append("String[] hiddenField4;\n");
    dialogTemplate.append("String[] fieldImage;\n");
    dialogTemplate.append("String[] documentsFolder;\n");
    dialogTemplate.append("%>\n");
    for (int i = 0; i < forms.size(); i++) {
      UIFormGeneratorInputBean form = forms.get(i);
      String inputName = formatInputName(form.getName());
      String inputType = form.getType();
      String inputFieldName = inputName + "FieldName";
      StringBuffer bufVal = new StringBuffer("validate=");
      String inputField = "";
      String guideline = form.getGuideline();
      if (guideline == null || "null".equals(guideline)) guideline = "";
      String value = form.getValue();
      if (value==null || "null".equals(value)) value="";
      if (form.isMandatory())
        bufVal.append("org.exoplatform.wcm.webui.validator.MandatoryValidator,");
      if (UIFormGeneratorConstant.TEXTAREA.equals(inputType)) {
        inputField = "TextAreaField";
      } else if (UIFormGeneratorConstant.WYSIWYG.equals(inputType)) {
        inputField = "RichtextField";
      } else if (UIFormGeneratorConstant.DATE.equals(inputType)) {
        inputField = "CalendarField";
        bufVal.append("datetime,");
      } else if (UIFormGeneratorConstant.SELECT.equals(inputType)) {
        inputField = "SelectBoxField";
      } else if (UIFormGeneratorConstant.CHECKBOX.equals(inputType)){
        inputField = "CheckBoxField";
      } else if (UIFormGeneratorConstant.RADIO.equals(inputType)) {
        inputField = "RadioBoxField";
      } else {
        inputField = "TextField";
      }
      String validate = bufVal.toString();
      if (validate.endsWith(",")) validate = validate.substring(0, validate.length() - 1);
      if (validate.endsWith("=")) validate = "";
      String propertyName = getPropertyName(inputName);
      if (UIFormGeneratorConstant.LABEL.equals(inputType)) {
        dialogTemplate.append("      <tr>\n");
        dialogTemplate.append("        <td></td>\n");
        dialogTemplate.append("        <td>" + value + "</td>\n");
        dialogTemplate.append("        </td>\n");
        dialogTemplate.append("      </tr>\n");
      } else {
        dialogTemplate.append("      <tr>\n");
        dialogTemplate.append("        <td class=\"FieldLabel\"><pre>"
            + Text.unescape(form.getName()).replaceAll("\\$", "&#036;").replaceAll("\\\\", "&#92;")
            + "</pre></td>\n");
        dialogTemplate.append("        <td class=\"FieldComponent\">\n");
        dialogTemplate.append("          <%\n");
        if (UIFormGeneratorConstant.UPLOAD.equals(inputType)) {
          String extraFormUploadInput = "";
          String realDataNodeName = "jcr:content"  + (inputFieldName);

          StringBuilder hiddenFields = new StringBuilder();
          hiddenFields.append("hiddenField1 = [\"jcrPath=/node" + extraFormUploadInput + "/"
                              + realDataNodeName + "\", \"nodetype=nt:resource\", \"visible=false\"];\n");
          hiddenFields.append("uicomponent.addHiddenField(\"" + inputFieldName + "_hiddenInput1\", hiddenField1);\n");
          hiddenFields.append("hiddenField2 = [\"jcrPath=/node" + extraFormUploadInput + "/"
                              + realDataNodeName + "/jcr:encoding\", \"visible=false\", \"UTF-8\"];\n");
          hiddenFields.append("uicomponent.addHiddenField(\"" + inputFieldName + "_hiddenInput2\", hiddenField2);\n");
          hiddenFields.append("hiddenField3 = [\"jcrPath=/node" + extraFormUploadInput + "/"
                              + realDataNodeName + "/jcr:lastModified\", \"visible=false\"];\n");
          hiddenFields.append("uicomponent.addCalendarField(\"" + inputFieldName + "_hiddenInput3\", hiddenField3);\n");
          String hiddenFieldsStr = hiddenFields.toString();

          dialogTemplate.append("           if(uicomponent.isEditing()) {\n");
          dialogTemplate.append("             def curNode = uicomponent.getNode() ;\n");
          dialogTemplate.append("             if (curNode.hasNode(\"" + realDataNodeName + "\")) {\n");
          dialogTemplate.append("               def imageNode = curNode.getNode(\"" + realDataNodeName + "\");\n");
          dialogTemplate.append("               if(imageNode.getProperty(\"jcr:data\").getStream().available() > 0) {\n");
          dialogTemplate.append("                 DownloadService dservice = ");
          dialogTemplate.append("uicomponent.getApplicationComponent(DownloadService.class);\n");
          dialogTemplate.append("                 InputStream input = imageNode.getProperty(\"jcr:data\").getStream();\n");
          dialogTemplate.append("                 InputStreamDownloadResource dresource = ");
          dialogTemplate.append("new InputStreamDownloadResource(input, \"" + inputFieldName + "\");\n");
          dialogTemplate.append("                 dresource.setDownloadName(curNode.getName());\n");
          dialogTemplate.append("                 def imgSrc = dservice.getDownloadLink(");
          dialogTemplate.append("dservice.addDownloadResource(dresource));\n");
          dialogTemplate.append("                 def actionLink = uicomponent.event(\"RemoveData\", \"/"
                                + realDataNodeName + "\");\n");
          dialogTemplate.append("                 %>\n");
          dialogTemplate.append("                   <div>\n");
          dialogTemplate.append("                     <img src=\"$imgSrc\" alt=\"<%=_ctx.appRes(\"FormGeneratorDialog.label.remove\")%>\" width=\"100px\" height=\"80px\"/>\n");
          dialogTemplate.append("                     <a href=\"$actionLink\">\n");
          dialogTemplate.append("                       <img src=\"/eXoResources/skin/DefaultSkin/background/Blank.gif\" ");
          dialogTemplate.append(
              "alt=\"<%=_ctx.appRes(\"FormGeneratorDialog.label.remove\")%>\" class=\"ActionIcon Remove16x16Icon\"/>\n");
          dialogTemplate.append("                     </a>\n");
          dialogTemplate.append("                   </div>\n");
          dialogTemplate.append("                 <%\n");
          dialogTemplate.append("               } else {\n");
          dialogTemplate.append("                 fieldImage = [\"jcrPath=/node" + extraFormUploadInput
                                + "/" + realDataNodeName  + "/jcr:data\"] ;\n");
          dialogTemplate.append("                 uicomponent.addUploadField(\"" + "/node/"
                                + inputFieldName + "\", fieldImage) ;\n");
          dialogTemplate.append(hiddenFieldsStr);
          dialogTemplate.append("               }\n");
          dialogTemplate.append("             } else {\n");
          dialogTemplate.append("               fieldImage = [\"jcrPath=/node" + extraFormUploadInput  + "/"
                                + realDataNodeName  + "/jcr:data\"] ;\n");
          dialogTemplate.append("               uicomponent.addUploadField(\"" + "/node/"
                                + inputFieldName + "\", fieldImage) ;\n");
          dialogTemplate.append(hiddenFieldsStr);
          dialogTemplate.append("             }\n");
          dialogTemplate.append("           } else if(uicomponent.dataRemoved()) {\n");
          dialogTemplate.append("             fieldImage = [\"jcrPath=/node" + extraFormUploadInput  + "/"
                                + realDataNodeName  + "/jcr:data\"] ;\n");
          dialogTemplate.append("             uicomponent.addUploadField(\"" + "/node/"
                                + inputFieldName + "\", fieldImage) ;\n");
          dialogTemplate.append(hiddenFieldsStr);
          dialogTemplate.append("           } else {\n");
          dialogTemplate.append("             fieldImage = [\"jcrPath=/node" + extraFormUploadInput
                                + "/" + realDataNodeName  + "/jcr:data\"] ;\n");
          dialogTemplate.append("             uicomponent.addUploadField(\"" + "/node/"
                                + inputFieldName + "\", fieldImage) ;\n");
          dialogTemplate.append(hiddenFieldsStr);
          dialogTemplate.append("           }\n");
        } else {
          dialogTemplate.append("           String[] " + inputFieldName + " = [\"jcrPath=/node/"
                                + propertyName + "\", \"defaultValues=" + value + "\", \"" + validate
                                + "\", \"options=" + form.getAdvanced() + "\"];\n");
          dialogTemplate.append("           uicomponent.add" + inputField + "(\"" + "/node/"
                                + inputName + "\", " + inputFieldName + ");\n");
        }
        dialogTemplate.append("          %>\n");
        dialogTemplate.append("        </td>\n");
        dialogTemplate.append("      </tr>\n");
      }

      dialogTemplate.append("      <tr>\n");
      dialogTemplate.append("        <td>&nbsp;</td>\n");
      dialogTemplate.append("        <td>\n");
      dialogTemplate.append("          <div class=\"GuideLine\">" + guideline + "</div>\n");
      dialogTemplate.append("        </td>\n");
      dialogTemplate.append("      </tr>\n");
    }
    dialogTemplate.append("      </table>\n");    
    dialogTemplate.append("    </div>\n");
    dialogTemplate.append("  <% /* start render action*/\n");
    dialogTemplate.append("     if (!uiform.isShowActionsOnTop()) uiform.processRenderAction();\n");
    dialogTemplate.append("     /* end render action*/\n");
    dialogTemplate.append("     uiform.end();\n");
    dialogTemplate.append("  %> \n");
    dialogTemplate.append("</div>\n");
    dialogTemplate.append("<!--DIALOG_END-->\n");
    return dialogTemplate.toString();
  }

  /**
   * Generate view template.
   *
   * @param forms the forms
   *
   * @return the string
   */
  private String generateViewTemplate(String templateName, List<UIFormGeneratorInputBean> forms) {
    StringBuilder viewTemplate = new StringBuilder();
    viewTemplate.append("<%\n");
    viewTemplate.append(" import org.exoplatform.download.DownloadService;\n");
    viewTemplate.append(" import org.exoplatform.download.InputStreamDownloadResource;\n");
    viewTemplate.append(" import java.io.InputStream;\n");
    viewTemplate.append("\n def currentNode = uicomponent.getNode() ; %>\n");
    viewTemplate.append("<!--VIEW_BEGIN-->\n");
    viewTemplate.append(" <div>\n");
    viewTemplate.append("   <table style=\"width:95%;margin:5px;border:1px solid;\">\n");
    viewTemplate.append("     <tr>\n");
    viewTemplate.append("       <th>Name</th>\n");
    viewTemplate.append("       <th>Value</th>\n");
    viewTemplate.append("     </tr>\n");
    for (UIFormGeneratorInputBean form : forms) {
      String propertyName = getPropertyName(formatInputName(form.getName()));
      viewTemplate.append("   <tr>\n");
      viewTemplate.append("     <%\n");
      viewTemplate.append("       if (currentNode.hasProperty(\"" + propertyName + "\")) {\n");
      viewTemplate.append("           String cleanName = currentNode.getProperty(\"" + propertyName + "\").getName();\n");
      viewTemplate.append("           if (cleanName.startsWith(\""+NODE_PREFIX+"\")) cleanName = cleanName.substring(9);\n");
      viewTemplate.append("           cleanName = cleanName.replaceAll(\"_\", \" \");\n");
      viewTemplate.append("         %>\n");
      viewTemplate.append("           <td style=\"padding:5px\"><%= cleanName %></td>\n");
      if (UIFormGeneratorConstant.UPLOAD.equals(form.getType())) {
        String inputName  = form.getName();
        String inputFieldName = cleanString(inputName) + "FieldName";
        String realDataNodeName = "jcr:content" + (inputFieldName);
        viewTemplate.append("<%\n");
        viewTemplate.append("if (currentNode.hasNode(\"" + realDataNodeName + "\")) {\n");
        viewTemplate.append("       def imageNode = currentNode.getNode(\"" + realDataNodeName + "\");\n");
        viewTemplate.append("       DownloadService dservice = uicomponent.getApplicationComponent(DownloadService.class);\n");
        viewTemplate.append("       InputStream input = imageNode.getProperty(\"jcr:data\").getStream();\n");
        viewTemplate.append("       InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, \""
                            + form.getName() + "\");\n");
        viewTemplate.append("       dresource.setDownloadName(currentNode.getName());\n");
        viewTemplate.append("       def dataSrc = dservice.getDownloadLink(dservice.addDownloadResource(dresource));\n");
        viewTemplate.append("}\n");
        viewTemplate.append("%>\n");
        viewTemplate.append("           <td style=\"padding:5px\"><a href=\"$dataSrc\">");
        viewTemplate.append("<%= _ctx.appRes(\"FormGeneratorDialog.label.Download\") %></a></td>\n");
      } else {
        viewTemplate.append("           <td style=\"padding:5px\"><%=currentNode.getProperty(\""
                            + propertyName + "\").getString()%></td>\n");
      }
      viewTemplate.append("         <%\n");
      viewTemplate.append("       }\n");
      viewTemplate.append("     %>\n");
      viewTemplate.append("   </tr>\n");
    }
    viewTemplate.append("   </table>\n");
    viewTemplate.append(" </div>\n");
    viewTemplate.append("<!--VIEW_END-->\n");
    return viewTemplate.toString();
  }
  
  private String formatInputName(String name) {
    name = cleanString(StringEscapeUtils.unescapeHtml(Text.unescape(name)));
    
    char firstChar = name.substring(0, 1).toCharArray()[0];
    if (!Character.isLetter(firstChar))
      return String.format("%s%s", INPUT_NAME_PREFIX, name);
    else
      return name;
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UIFormGeneratorTabPane> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFormGeneratorTabPane> event) throws Exception {
      UIFormGeneratorTabPane formGeneratorTabPane = event.getSource();
      UIFormInputSet formGeneratorGeneralTab = formGeneratorTabPane.
          getChildById(UIFormGeneratorConstant.FORM_GENERATOR_GENERAL_TAB);
      UIFormHiddenInput hiddenInputJSonObject = formGeneratorGeneralTab.
          getChildById(UIFormGeneratorConstant.JSON_OBJECT_FORM_GENERATOR);
      String jsonObjectGenerated = hiddenInputJSonObject.getValue();
      jsonObjectGenerated = jsonObjectGenerated.replaceAll("\n", "<br/>");
      
      JsonHandler jsonHandler = new JsonDefaultHandler();
      Charset cs = Charset.forName("utf-8");
      new JsonParserImpl().parse(new InputStreamReader(new ByteArrayInputStream(jsonObjectGenerated.getBytes("utf-8")),
                                                       cs),
                                 jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();
      List<UIFormGeneratorInputBean> forms = ((UIFormGeneratorInputBean)new BeanBuilder().
              createObject(UIFormGeneratorInputBean.class, jsonValue)).getInputs();

      for(int i = 0; i < forms.size(); i++) {
        for(int j = i + 1; j < forms.size(); j++){
          if(forms.get(i).getName().equals(forms.get(j).getName())) {
            Utils.createPopupMessage(formGeneratorTabPane,
                                     "UIFormGeneratorTabPane.msg.duplicate-name",
                                     null,
                                     ApplicationMessage.WARNING);
            event.getRequestContext().addUIComponentToUpdateByAjax(formGeneratorTabPane);
            return;
          }
        }
        
        // If there are only special characters, warning message
        String strCheckSpecialChars = StringEscapeUtils.unescapeHtml(Text.unescape(forms.get(i)
                                                                                        .getName()))
                                                       .replaceAll("[^a-zA-Z0-9]",
                                                                   StringUtils.EMPTY);
        if (StringUtils.isEmpty(strCheckSpecialChars)) {
          Utils.createPopupMessage(formGeneratorGeneralTab,
                                   "UIFormGeneratorTabPane.msg.input-only-special-characters",
                                   null,
                                   ApplicationMessage.WARNING);
          event.getRequestContext().addUIComponentToUpdateByAjax(formGeneratorTabPane);
          return;
        }
        
        if(forms.get(i).getType().equals(UIFormGeneratorConstant.SELECT)) {
          String[] advance = null;
          boolean isEmptyValue = false;
          if((forms.get(i).getAdvanced() != null)) {
            advance = forms.get(i).getAdvanced().split(",");
            if(advance.length == 0) {
              isEmptyValue = true;
            } else {
              for(int count = 0; count < advance.length; count++) {
                if(advance[count] == null || advance[count].trim().length() <= 0){
                  isEmptyValue = true;
                }
              }
            }
          } else {
            isEmptyValue = true;
          }
          if (isEmptyValue) {
            Utils.createPopupMessage(formGeneratorTabPane,
                                     "UIFormGeneratorTabPane.msg.select-value-empty",
                                     null,
                                     ApplicationMessage.INFO);
            return;
          }
        }
      }
      UIFormStringInput nameFormStringInput = formGeneratorTabPane.
          getUIStringInput(UIFormGeneratorConstant.NAME_FORM_STRING_INPUT);
      String templateName = nameFormStringInput.getValue().trim();
      String nodetypeName = formGeneratorTabPane.getNodetypeName(templateName);

      String preferenceRepository = UIFormGeneratorUtils.getPreferenceRepository();
      ListenerService listenerService = Utils.getService(ListenerService.class);

      listenerService.broadcast(UIFormGeneratorConstant.PRE_CREATE_NODETYPE_EVENT, null, nodetypeName);

      formGeneratorTabPane.addNodetype(event.getRequestContext(), preferenceRepository, nodetypeName, forms);
      String newDialogTemplate = formGeneratorTabPane.generateDialogTemplate(templateName, forms);
      String newViewTemplate = formGeneratorTabPane.generateViewTemplate(templateName, forms);

      TemplateService templateService = Utils.getService(TemplateService.class);
      templateService.addTemplate(TemplateService.DIALOGS,
                                  nodetypeName,
                                  templateName,
                                  true,
                                  cleanString(templateName),
                                  new String[] { "*" },
                                  new ByteArrayInputStream(newDialogTemplate.getBytes()));
      templateService.addTemplate(TemplateService.VIEWS,
                                  nodetypeName,
                                  templateName,
                                  true,
                                  cleanString(templateName),
                                  new String[] { "*" },
                                  new ByteArrayInputStream(newViewTemplate.getBytes()));
      templateService.addTemplate(TemplateService.SKINS,
                                  nodetypeName,
                                  templateName,
                                  true,
                                  cleanString(templateName),
                                  new String[] { "*" },
                                  new ByteArrayInputStream("".getBytes()));

      listenerService.broadcast(UIFormGeneratorConstant.POST_CREATE_NODETYPE_EVENT, null, nodetypeName);

      Utils.createPopupMessage(formGeneratorTabPane,
                               "UIFormGeneratorTabPane.msg.AddNewsSuccessful",
                               new Object[] { templateName },
                               ApplicationMessage.INFO);

      nameFormStringInput.setValue("");
      ((UIFormRichtextInput) formGeneratorGeneralTab.
           getChildById(UIFormGeneratorConstant.DESCRIPTION_FORM_WYSIWYG_INPUT)).setValue("");
      event.getRequestContext().addUIComponentToUpdateByAjax(formGeneratorTabPane);
    }
  }

  /**
   * The listener interface for receiving resetAction events.
   * The class that is interested in processing a resetAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addResetActionListener<code> method. When
   * the resetAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ResetActionEvent
   */
  public static class ResetActionListener extends EventListener<UIFormGeneratorTabPane> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFormGeneratorTabPane> event) throws Exception {
      UIFormGeneratorTabPane formGeneratorTabPane = event.getSource();
      UIFormInputSet formGeneratorGeneralTab = formGeneratorTabPane.
          getChildById(UIFormGeneratorConstant.FORM_GENERATOR_GENERAL_TAB);
      formGeneratorGeneralTab.getUIStringInput(UIFormGeneratorConstant.NAME_FORM_STRING_INPUT)
                             .setValue("");
      ((UIFormRichtextInput) formGeneratorGeneralTab.
           getChildById(UIFormGeneratorConstant.DESCRIPTION_FORM_WYSIWYG_INPUT)).setValue("");
      event.getRequestContext().addUIComponentToUpdateByAjax(formGeneratorTabPane);
    }
  }
}
