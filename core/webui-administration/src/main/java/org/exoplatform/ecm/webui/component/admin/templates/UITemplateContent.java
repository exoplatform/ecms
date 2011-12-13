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
package org.exoplatform.ecm.webui.component.admin.templates;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.version.VersionHistory;

import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITemplateContent.SaveActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.ChangeActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.CancelActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.RestoreActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.RefreshActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.AddPermissionActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UITemplateContent.RemovePermissionActionListener.class)
    }
)
public class UITemplateContent extends UIForm implements UISelectable {

  final static public String FIELD_SELECT_VERSION = "selectVersion" ;
  final static public String FIELD_CONTENT = "content" ;
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_VIEWPERMISSION = "viewPermission" ;
  final static public String FIELD_ENABLE_VERSION = "enableVersion" ;
  final static public String[] REG_EXPRESSION = {"[", "]", ":", "&", "%"} ;

  private boolean isAddNew_ = true ;
  private String nodeTypeName_ ;
  private List<String> listVersion_ = new ArrayList<String>() ;
  private String templateType;

  final static public String TEMPLATE_PERMISSION = "TemplatePermission" ;

  public UITemplateContent() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox versions =
      new UIFormSelectBox(FIELD_SELECT_VERSION, FIELD_SELECT_VERSION, options) ;
    versions.setOnChange("Change") ;
    versions.setRendered(false) ;
    addUIFormInput(versions) ;
    addUIFormInput(new UIFormTextAreaInput(FIELD_CONTENT, FIELD_CONTENT, null).addValidator(MandatoryValidator.class)) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class)) ;
    UIFormCheckBoxInput isVersion =
      new UIFormCheckBoxInput<Boolean>(FIELD_ENABLE_VERSION , FIELD_ENABLE_VERSION, null) ;
    isVersion.setRendered(false) ;
    addUIFormInput(isVersion) ;
    UIFormInputSetWithAction uiActionTab = new UIFormInputSetWithAction("UITemplateContent");
    uiActionTab.addUIFormInput(new UIFormStringInput(FIELD_VIEWPERMISSION,
                                                     FIELD_VIEWPERMISSION,
                                                     null).setEditable(false)
                                                          .addValidator(MandatoryValidator.class));
    uiActionTab.setActionInfo(FIELD_VIEWPERMISSION, new String[] { "AddPermission",
        "RemovePermission" });
    addUIComponentInput(uiActionTab) ;
  }

  public void setTemplateType(String templateType) { this.templateType = templateType; }

  public String getTemplateType() { return templateType; }

  public void setNodeTypeName (String nodeType) {nodeTypeName_ = nodeType ;}

  public void update(String templateName) throws Exception {
    if(templateName != null) {
      isAddNew_ = false ;
      TemplateService templateService = getApplicationComponent(TemplateService.class) ;
      String templateContent = templateService.getTemplate(templateType, nodeTypeName_, templateName) ;
      Node template =
        templateService.getTemplateNode(templateType, nodeTypeName_, templateName, WCMCoreUtils.getSystemSessionProvider()) ;
      getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setRendered(true) ;
      String templateRole =
        templateService.getTemplateRoles(template) ;
      boolean isVersioned = template.isNodeType(Utils.MIX_VERSIONABLE) ;
      if(isVersioned) {
        getUIFormSelectBox(FIELD_SELECT_VERSION).setRendered(true) ;
        getUIFormSelectBox(FIELD_SELECT_VERSION).setOptions(getVersionValues(template)) ;
        getUIFormSelectBox(FIELD_SELECT_VERSION).setValue(template.getBaseVersion().getName()) ;
        getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setEnable(false) ;
        getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setChecked(true) ;
        setActions(new String[]{"Save", "Restore", "Refresh", "Cancel"}) ;
      } else {
        getUIFormSelectBox(FIELD_SELECT_VERSION).setRendered(false) ;
        getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setEnable(true) ;
        getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setChecked(false) ;
        setActions( new String[]{"Save", "Refresh", "Cancel"}) ;
      }      
      getUIFormTextAreaInput(FIELD_CONTENT).setValue(templateContent) ;
      getUIStringInput(FIELD_NAME).setValue(template.getName()) ;
      getUIStringInput(FIELD_NAME).setEditable(false) ;
      getUIStringInput(FIELD_VIEWPERMISSION).setValue(templateRole) ;
      return ;
    }
    isAddNew_ = true ;
    getUIFormSelectBox(FIELD_SELECT_VERSION).setRendered(false) ;
    getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).setRendered(false) ;
    getUIStringInput(FIELD_NAME).setEditable(true) ;
    setActions( new String[]{"Save", "Refresh", "Cancel"}) ;
  }



  private void refresh() throws Exception {
    UIViewTemplate uiViewTemplate = getAncestorOfType(UIViewTemplate.class) ;
    uiViewTemplate.refresh() ;
    UIComponent parent = getParent() ;
    if(parent instanceof UIDialogTab) {
      uiViewTemplate.setRenderedChild(UIDialogTab.class) ;
    } else if(parent instanceof UIViewTab) {
      uiViewTemplate.setRenderedChild(UIViewTab.class) ;
    } else if(parent instanceof UISkinTab) {
      uiViewTemplate.setRenderedChild(UISkinTab.class) ;
    }
    update(null) ;
    reset() ;
  }

  private VersionNode getRootVersion(Node node) throws Exception{
    VersionHistory vH = node.getVersionHistory() ;
    if(vH != null) return new VersionNode(vH.getRootVersion(), node.getSession()) ;
    return null ;
  }
  private List<String> getNodeVersions(List<VersionNode> children) throws Exception {
    List<VersionNode> child = new ArrayList<VersionNode>() ;
    for(VersionNode version : children) {
      listVersion_.add(version.getName()) ;
      child = version.getChildren() ;
      if(!child.isEmpty()) getNodeVersions(child) ;
    }
    return listVersion_ ;
  }

  private List<SelectItemOption<String>> getVersionValues(Node node) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    List<VersionNode> children = getRootVersion(node).getChildren() ;
    listVersion_.clear() ;
    List<String> versionList = getNodeVersions(children) ;
    for(int i = 0; i < versionList.size(); i++) {
      for(int j = i + 1; j < versionList.size(); j ++) {
        if( Integer.parseInt(versionList.get(j)) < Integer.parseInt(versionList.get(i))) {
          String temp = versionList.get(i) ;
          versionList.set(i, versionList.get(j)) ;
          versionList.set(j, temp) ;
        }
      }
      options.add(new SelectItemOption<String>(versionList.get(i), versionList.get(i))) ;
    }
    return options ;
  }

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    String viewPermission = getUIStringInput(FIELD_VIEWPERMISSION).getValue();
    if (viewPermission == null) viewPermission = "";
    if ((viewPermission != null) && (viewPermission.length() == 0)) {
      viewPermission = value.toString();
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append(viewPermission).append(",").append(value.toString());
      viewPermission = sb.toString();
    }
    getUIStringInput(FIELD_VIEWPERMISSION).setValue(viewPermission) ;
    UITemplatesManager uiManager = getAncestorOfType(UITemplatesManager.class) ;
    uiManager.removeChildById(getId() + TEMPLATE_PERMISSION) ;
  }

  static public class RestoreActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiForm = event.getSource() ;
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class) ;
      Node node = templateService.getTemplateNode(uiForm.getTemplateType(),  uiForm.nodeTypeName_,
          name, WCMCoreUtils.getSystemSessionProvider()) ;
      String vesion = uiForm.getUIFormSelectBox(FIELD_SELECT_VERSION).getValue() ;
      String baseVesion = node.getBaseVersion().getName() ;
      UIApplication app = uiForm.getAncestorOfType(UIApplication.class) ;
      if(vesion.equals(baseVesion)) return ;
      node.checkout() ;
      node.restore(vesion, true) ;
      Object[] args = {uiForm.getUIStringInput(FIELD_SELECT_VERSION).getValue()} ;
      app.addMessage(new ApplicationMessage("UITemplateContent.msg.version-restored", args)) ;
      uiForm.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
      
    }
  }

  static public class SaveActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiForm = event.getSource() ;
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
      if(name == null || name.trim().length() == 0) {
        Object[] args = { FIELD_NAME } ;
        uiApp.addMessage(new ApplicationMessage("ECMNameValidator.msg.empty-input", args,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      }
      if(!Utils.isNameValid(name, UITemplateContent.REG_EXPRESSION)){
        uiApp.addMessage(new ApplicationMessage("UITemplateContent.msg.name-invalid", null,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      }
      String content = uiForm.getUIFormTextAreaInput(FIELD_CONTENT).getValue() ;
      if(content == null) content = "" ;
      UIFormInputSetWithAction permField = uiForm.getChildById("UITemplateContent") ;
      String role = permField.getUIStringInput(FIELD_VIEWPERMISSION).getValue() ;
      UIViewTemplate uiViewTemplate = uiForm.getAncestorOfType(UIViewTemplate.class) ;
      if(uiForm.getId().equals(UIDialogTab.DIALOG_FORM_NAME)) {
        UIDialogTab uiDialogTab = uiViewTemplate.getChild(UIDialogTab.class) ;
        if(uiDialogTab.getListDialog().contains(name) && uiForm.isAddNew_) {
          Object[] args = { name } ;
          uiApp.addMessage(new ApplicationMessage("UITemplateContent.msg.name-exist", args,
                                                  ApplicationMessage.WARNING)) ;
          
          return ;
        }
      } else if(uiForm.getId().equals(UIViewTab.VIEW_FORM_NAME)) {
        UIViewTab uiViewTab = uiViewTemplate.getChild(UIViewTab.class) ;
        if(uiViewTab.getListView().contains(name) && uiForm.isAddNew_) {
          Object[] args = { name } ;
          uiApp.addMessage(new ApplicationMessage("UITemplateContent.msg.name-exist", args,
                                                  ApplicationMessage.WARNING)) ;
          
          return ;
        }
      } else if(uiForm.getId().equals(UISkinTab.SKIN_FORM_NAME)) {
        UISkinTab uiSkinTab = uiViewTemplate.getChild(UISkinTab.class) ;
        if(uiSkinTab.getListSkin().contains(name) && uiForm.isAddNew_) {
          Object[] args = { name } ;
          uiApp.addMessage(new ApplicationMessage("UITemplateContent.msg.name-exist", args,
                                                  ApplicationMessage.WARNING)) ;
          
          return ;
        }
      }
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class) ;
      boolean isEnableVersioning =
        uiForm.getUIFormCheckBoxInput(FIELD_ENABLE_VERSION).isChecked() ;
      if(uiForm.isAddNew_){
        templateService.addTemplate(uiForm.getTemplateType(), uiForm.nodeTypeName_, null, false, name, new String[] {role},
            new ByteArrayInputStream(content.getBytes()));
      } else {
        Node node =
          templateService.getTemplateNode(uiForm.getTemplateType(), uiForm.nodeTypeName_, name,
              WCMCoreUtils.getSystemSessionProvider()) ;
        if(isEnableVersioning && !node.isNodeType(Utils.MIX_VERSIONABLE)) {
          node.addMixin(Utils.MIX_VERSIONABLE) ;
        }
        if (areValidPermissions(role, uiForm, event)) {
          templateService.addTemplate(uiForm.getTemplateType(),
                                      uiForm.nodeTypeName_,
                                      null,
                                      false,
                                      name,
                                      new String[] { role },
                                      new ByteArrayInputStream(content.getBytes()));
        } else {
          return;
        }
        node.save() ;
        if(isEnableVersioning) {
          node.checkin() ;
          node.checkout();
        }
      }
      uiForm.refresh() ;
      uiForm.isAddNew_ = true ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class ChangeActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiForm = event.getSource() ;
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class) ;
      Node node = templateService.getTemplateNode(uiForm.getTemplateType(), uiForm.nodeTypeName_,
          name, WCMCoreUtils.getSystemSessionProvider()) ;
      String version = uiForm.getUIFormSelectBox(FIELD_SELECT_VERSION).getValue() ;
      String path = node.getVersionHistory().getVersion(version).getPath() ;
      VersionNode versionNode = uiForm.getRootVersion(node).findVersionNode(path) ;
      Node frozenNode = versionNode.getNode(Utils.JCR_FROZEN) ;
      String content = templateService.getTemplate(frozenNode);
      uiForm.getUIFormTextAreaInput(FIELD_CONTENT).setValue(content) ;
      if (frozenNode.hasProperty(Utils.EXO_ROLES)) {
        StringBuilder rule = new StringBuilder() ;
        Value[] rules = frozenNode.getProperty(Utils.EXO_ROLES).getValues() ;
        for(int i = 0; i < rules.length; i++) {
          rule.append(rules[i].getString());
        }
        uiForm.getUIStringInput(FIELD_VIEWPERMISSION).setValue(rule.toString());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class AddPermissionActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiTempContent = event.getSource() ;
      UITemplatesManager uiManager = uiTempContent.getAncestorOfType(UITemplatesManager.class) ;
      UIViewTemplate uiViewTemp = uiTempContent.getAncestorOfType(UIViewTemplate.class) ;

      // The codes are updated by lampt.
      List<UIComponent> uicomponents = uiManager.getChildren();
      List<UIComponent> parentUIComponents = new ArrayList<UIComponent>();
      parentUIComponents.addAll(uicomponents);
      for (UIComponent uicomponent : parentUIComponents) {
        if (UIPopupWindow.class.isInstance(uicomponent)) {
          if (!uicomponent.getId().equals(UITemplatesManager.NEW_TEMPLATE) &&
                                !uicomponent.getId().equals(UITemplatesManager.EDIT_TEMPLATE)) {
            uiManager.removeChildById(uicomponent.getId());
          }
        }
      }

      String membership = uiTempContent.getUIStringInput(FIELD_VIEWPERMISSION).getValue() ;
      uiManager.initPopupPermission(uiTempContent.getId(), membership) ;
      if(uiTempContent.getId().equals(UIDialogTab.DIALOG_FORM_NAME)) {
        uiViewTemp.setRenderedChild(UIDialogTab.class) ;
      } else if(uiTempContent.getId().equals(UIViewTab.VIEW_FORM_NAME)) {
        uiViewTemp.setRenderedChild(UIViewTab.class) ;
      } else if(uiTempContent.getId().equals(UISkinTab.SKIN_FORM_NAME)) {
        uiViewTemp.setRenderedChild(UISkinTab.class) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class RemovePermissionActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiTemplateContent = event.getSource();
      uiTemplateContent.getUIStringInput(FIELD_VIEWPERMISSION).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplateContent);
    }
  }
  
  static public class RefreshActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiForm = event.getSource() ;
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;
      if(!uiForm.isAddNew_) {
        uiForm.update(uiForm.getUIStringInput(UITemplateContent.FIELD_NAME).getValue()) ;
        return ;
      }
      uiForm.update(null) ;
      uiForm.reset() ;
      uiForm.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class CancelActionListener extends EventListener<UITemplateContent> {
    public void execute(Event<UITemplateContent> event) throws Exception {
      UITemplateContent uiTemplateContent = event.getSource() ;
      UITemplatesManager uiManager = uiTemplateContent.getAncestorOfType(UITemplatesManager.class) ;
      uiManager.removeChildById(UIDialogTab.DIALOG_FORM_NAME + TEMPLATE_PERMISSION) ;
      uiManager.removeChildById(UIViewTab.VIEW_FORM_NAME + TEMPLATE_PERMISSION) ;
      uiManager.removeChildById(UISkinTab.SKIN_FORM_NAME + TEMPLATE_PERMISSION) ;
      uiTemplateContent.reset() ;
      uiManager.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  private static boolean areValidPermissions(String permissions,
                                             UITemplateContent uiTemplateContent,
                                             Event event) throws Exception {
    Boolean areValidPermissions = false;
    UIApplication uiApp = uiTemplateContent.getAncestorOfType(UIApplication.class);
    if (permissions == null || permissions.trim().length() == 0) {
      uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-null",
                                              null,
                                              ApplicationMessage.WARNING));
      
      areValidPermissions = false;
      return areValidPermissions;
    }

    OrganizationService oservice = uiTemplateContent.getApplicationComponent(OrganizationService.class);
    String[] arrPermissions = permissions.split(",");
    List<String> listMemberhip;
    Collection<?> collection = oservice.getMembershipTypeHandler().findMembershipTypes();
    listMemberhip = new ArrayList<String>(5);
    for (Object obj : collection) {
      listMemberhip.add(((MembershipType) obj).getName());
    }
    listMemberhip.add("*");
    for (String itemPermission : arrPermissions) {
      if (itemPermission.length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                null,
                                                ApplicationMessage.WARNING));
        
        areValidPermissions = false;
        return areValidPermissions;
      }
      if (itemPermission.contains(":")) {
        String[] permission = itemPermission.split(":");
        if ((permission[0] == null) || (permission[0].length() == 0)) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          areValidPermissions = false;
          return areValidPermissions;
        } else if (!listMemberhip.contains(permission[0])) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          areValidPermissions = false;
          return areValidPermissions;
        }
        if ((permission[1] == null) || (permission[1].length() == 0)) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          areValidPermissions = false;
          return areValidPermissions;
        } else if (oservice.getGroupHandler().findGroupById(permission[1]) == null) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          areValidPermissions = false;
          return areValidPermissions;
        }
      } else {
        if (!itemPermission.equals("*")) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.permission-path-invalid",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          areValidPermissions = false;
          return areValidPermissions;
        }
      }
    }
    areValidPermissions = true;
    return areValidPermissions;
  }
}
