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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
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
    template =  "system:/groovy/webui/form/UIFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UITemplateForm.SaveActionListener.class),
      @EventConfig(listeners = UITemplateForm.RefreshActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UITemplateForm.CancelActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UITemplateForm.AddPermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITemplateForm.OnChangeActionListener.class, phase = Phase.DECODE)
    }
)
public class UITemplateForm extends UIFormTabPane implements UISelectable {
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_LABEL = "label" ;
  final static public String FIELD_ISTEMPLATE = "isDocumentTemplate" ;
  final static public String FIELD_DIALOG = "dialog" ;
  final static public String FIELD_VIEW = "view" ;
  final static public String FIELD_SKIN = "skin";
  final static public String FIELD_TAB_TEMPLATE = "template" ;
  final static public String FIELD_TAB_DIALOG = "defaultDialog" ;
  final static public String FIELD_TAB_VIEW = "defaultView" ;
  final static public String FIELD_TAB_SKIN = "defaultSkin" ;
  final static public String FIELD_PERMISSION = "permission" ;

  public UITemplateForm() throws Exception {
    super("UITemplateForm") ;
    UIFormInputSetWithAction templateTab = new UIFormInputSetWithAction(FIELD_TAB_TEMPLATE);
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(FIELD_NAME, FIELD_NAME, getOption());
    uiSelectBox.setOnChange("OnChange");
    templateTab.addUIFormInput(uiSelectBox);
    templateTab.addUIFormInput(new UIFormStringInput(FIELD_LABEL, FIELD_LABEL, null).
                               addValidator(MandatoryValidator.class)) ;

    templateTab.addUIFormInput(new UIFormCheckBoxInput(FIELD_ISTEMPLATE,
                                                                FIELD_ISTEMPLATE,
                                                                null).setChecked(true));
    templateTab.addUIFormInput(new UIFormStringInput(FIELD_PERMISSION, FIELD_PERMISSION,
                                                     null).setEditable(false).addValidator(MandatoryValidator.class));
    templateTab.setActionInfo(FIELD_PERMISSION, new String[] {"AddPermission"}) ;
    addUIComponentInput(templateTab) ;
    setSelectedTab(templateTab.getId()) ;
    UIFormInputSet defaultDialogTab = new UIFormInputSet(FIELD_TAB_DIALOG) ;
    defaultDialogTab.addUIFormInput(new UIFormTextAreaInput(FIELD_DIALOG, FIELD_DIALOG, null).
                                    addValidator(MandatoryValidator.class)) ;
    addUIFormInput(defaultDialogTab) ;
    UIFormInputSet defaultViewTab = new UIFormInputSet(FIELD_TAB_VIEW) ;
    defaultViewTab.addUIFormInput(new UIFormTextAreaInput(FIELD_VIEW, FIELD_VIEW, null).
                                  addValidator(MandatoryValidator.class)) ;
    addUIFormInput(defaultViewTab) ;
    UIFormInputSet defaultSkinTab = new UIFormInputSet(FIELD_TAB_SKIN) ;
    defaultSkinTab.addUIFormInput(new UIFormTextAreaInput(FIELD_SKIN, FIELD_SKIN, null)) ;
    addUIFormInput(defaultSkinTab) ;
    setActions(new String[]{"Save", "Refresh", "Cancel"}) ;
  }

  public void refresh()throws Exception {
    getUIFormSelectBox(FIELD_NAME).setOptions(getOption());
    String nodeType = getUIFormSelectBox(FIELD_NAME).getValue();
    getUIStringInput(FIELD_LABEL).setValue("");
    getUIFormCheckBoxInput(FIELD_ISTEMPLATE).setChecked(true);
    initTemplate(nodeType);
    getUIStringInput(FIELD_PERMISSION).setValue("");
  }

  private void initTemplate(String nodeType)throws Exception {
    getUIFormTextAreaInput(FIELD_VIEW).setValue(getDefaultView(nodeType));
    getUIFormTextAreaInput(FIELD_DIALOG).setValue(getDefaultDialog(nodeType));
    getUIFormTextAreaInput(FIELD_SKIN).setValue(getDefaultStyleSheet(nodeType));
  }

  private String getDefaultStyleSheet(String nodeType) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.buildStyleSheet(nodeType);
  }

  private String getDefaultView(String nodeType) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.buildViewForm(nodeType);
  }

  private String getDefaultDialog(String nodeType) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.buildDialogForm(nodeType);
  }

  static public class TemplateNameComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        String name1 = ((SelectItemOption) o1).getValue().toString() ;
        String name2 = ((SelectItemOption) o2).getValue().toString() ;
        return name1.compareToIgnoreCase(name2) ;
      } catch(Exception e) {
        return 0;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public List<SelectItemOption<String>> getOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    NodeTypeManager nodeTypeManager =
        getApplicationComponent(RepositoryService.class).getCurrentRepository().getNodeTypeManager() ;
    Node templatesHome = getApplicationComponent(TemplateService.class).
        getTemplatesHome(WCMCoreUtils.getUserSessionProvider());
    if(templatesHome != null) {
      NodeIterator templateIter = templatesHome.getNodes() ;
      List<String> templates = new ArrayList<String>() ;
      while (templateIter.hasNext()) {
        templates.add(templateIter.nextNode().getName()) ;
      }
      NodeTypeIterator iter = nodeTypeManager.getAllNodeTypes() ;
      while (iter.hasNext()) {
        NodeType nodeType = iter.nextNodeType();
        if (nodeType.isMixin()) continue;
        String nodeTypeName = nodeType.getName();
        if (!templates.contains(nodeTypeName))
          options.add(new SelectItemOption<String>(nodeTypeName, nodeTypeName));
      }
      Collections.sort(options, new TemplateNameComparator()) ;
    }
    return options ;
  }

  public void doSelect(String selectField, Object value) {
    UIFormInputSetWithAction uiFormAction = getChildById(FIELD_TAB_TEMPLATE) ;
    uiFormAction.getUIStringInput(FIELD_PERMISSION).setValue(value.toString()) ;
    UITemplatesManager uiManager = getAncestorOfType(UITemplatesManager.class) ;
    uiManager.removeChildById("AddNewTemplatePermission") ;
  }

  static public class SaveActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource() ;
      String name = uiForm.getUIFormSelectBox(FIELD_NAME).getValue().trim() ;
      String label = uiForm.getUIStringInput(FIELD_LABEL).getValue().trim() ;
      String dialog = uiForm.getUIFormTextAreaInput(FIELD_DIALOG).getValue() ;
      String view = uiForm.getUIFormTextAreaInput(FIELD_VIEW).getValue();
      String skin = uiForm.getUIFormTextAreaInput(FIELD_SKIN).getValue();
      if(skin == null) skin = "";
      boolean isDocumentTemplate = uiForm.getUIFormCheckBoxInput(FIELD_ISTEMPLATE).isChecked() ;
      UIFormInputSetWithAction permField = uiForm.getChildById(UITemplateForm.FIELD_TAB_TEMPLATE) ;
      String role = permField.getUIStringInput(FIELD_PERMISSION).getValue();
      if ((role == null) || (role.trim().length() == 0)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UITemplateForm.msg.role-require",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      String[] roles = {role} ;
      if (dialog == null)
        dialog = "";
      if (view == null)
        view = "";
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class) ;
      templateService.addTemplate(TemplateService.DIALOGS, name, label, isDocumentTemplate,
          TemplateService.DEFAULT_DIALOG, roles, new ByteArrayInputStream(dialog.getBytes())) ;
      templateService.addTemplate(TemplateService.VIEWS, name, label, isDocumentTemplate,
          TemplateService.DEFAULT_VIEW, roles, new ByteArrayInputStream(view.getBytes())) ;
      templateService.addTemplate(TemplateService.SKINS, name, label, isDocumentTemplate,
          TemplateService.DEFAULT_SKIN, roles, new ByteArrayInputStream(skin.getBytes())) ;
      WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);
      composer.updateTemplatesSQLFilter();
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;      
      uiManager.refresh() ;
      uiForm.refresh() ;
      uiManager.removeChildById("TemplatePopup") ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiTemplateForm = event.getSource() ;
      UITemplatesManager uiManager = uiTemplateForm.getAncestorOfType(UITemplatesManager.class) ;
      uiTemplateForm.reset() ;
      uiManager.removeChildById("TemplatePopup") ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static  public class RefreshActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiFormTabPane = event.getSource() ;
      uiFormTabPane.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormTabPane.getParent()) ;
    }
  }

  static  public class OnChangeActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiFormTabPane = event.getSource() ;
      String nodeType = uiFormTabPane.getUIFormSelectBox(FIELD_NAME).getValue();
      uiFormTabPane.getUIStringInput(FIELD_LABEL).setValue("");
      uiFormTabPane.initTemplate(nodeType);
      uiFormTabPane.getUIStringInput(FIELD_PERMISSION).setValue("");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormTabPane.getParent()) ;
    }
  }

  static public class AddPermissionActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiTemplateForm = event.getSource() ;
      UITemplatesManager uiManager = uiTemplateForm.getAncestorOfType(UITemplatesManager.class) ;
      String membership = uiTemplateForm.getUIStringInput(FIELD_PERMISSION).getValue() ;
      uiManager.initPopupPermission("AddNew", membership) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
