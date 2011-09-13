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
package org.exoplatform.ecm.webui.component.browsecontent;

import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 19, 2006 9:05:58 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIDocumentConfig.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.ChangeTemplateOptionActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.EditActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.AddPathActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.DocSelectActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentConfig.BackActionListener.class)
    }
)
public class UIDocumentConfig extends UIForm implements UISelectable{
  final static public String FIELD_PATHSELECT = "path";
  final static public String FIELD_DOCSELECT = "doc";
  protected boolean isEdit_ = false;
  public UIDocumentConfig() throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_REPOSITORY, UINewConfigForm.FIELD_REPOSITORY, null));
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_WORKSPACE, UINewConfigForm.FIELD_WORKSPACE, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ALLOW_PUBLISH, null, null));
    UIFormInputSetWithAction categoryPathSelect = new UIFormInputSetWithAction(FIELD_PATHSELECT);
    categoryPathSelect.addUIFormInput(new UIFormStringInput(UINewConfigForm.FIELD_CATEGORYPATH, null, null));
    addUIComponentInput(categoryPathSelect);
    UIFormInputSetWithAction documentSelect = new UIFormInputSetWithAction(FIELD_DOCSELECT);
    documentSelect.addUIFormInput(new UIFormStringInput(UINewConfigForm.FIELD_DOCNAME, UINewConfigForm.FIELD_DOCNAME, null));
    addUIComponentInput(documentSelect);
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_DETAILBOXTEMP, UINewConfigForm.FIELD_DETAILBOXTEMP, Options));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLECOMMENT, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLEVOTE, null, null));
    setActions(UINewConfigForm.DEFAULT_ACTION);
  }

  public List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    UIConfigTabPane uiTabPane = getAncestorOfType(UIConfigTabPane.class);
    return uiTabPane.getWorkSpaceOption();
  }

  public void initForm(PortletPreferences preference, String repository, String workSpace, boolean isAddNew) throws Exception {
    String path = "";
    String docName = "";
    String hasComment = "true";
    String hasVote = "true";
    String detailTemplate = "";
    boolean isAllowPublish = Boolean.parseBoolean(preference.getValue(Utils.CB_ALLOW_PUBLISH, ""));
    UIFormInputSetWithAction categoryPathSelect = getChildById(FIELD_PATHSELECT);
    UIFormStringInput workSpaceField = getChildById(UINewConfigForm.FIELD_WORKSPACE);
    workSpaceField.setValue(workSpace);
    workSpaceField.setEditable(false);
    UIFormStringInput repositoryField = getChildById(UINewConfigForm.FIELD_REPOSITORY);
    repositoryField.setValue(repository);
    repositoryField.setEditable(false);
    UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
    categoryPathField.setEditable(false);
    UIFormInputSetWithAction documentSelect = getChildById(FIELD_DOCSELECT);
    UIFormStringInput documentNameField = documentSelect.getChildById(UINewConfigForm.FIELD_DOCNAME);
    documentNameField.setEditable(false);
    UIFormSelectBox detailtempField = getChildById(UINewConfigForm.FIELD_DETAILBOXTEMP);
    UIFormCheckBoxInput allowPublishField = getChildById(UINewConfigForm.FIELD_ALLOW_PUBLISH);
    UIFormCheckBoxInput enableCommentField = getChildById(UINewConfigForm.FIELD_ENABLECOMMENT);
    UIFormCheckBoxInput enableVoteField = getChildById(UINewConfigForm.FIELD_ENABLEVOTE);
    UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
    if (isEdit_) {
      categoryPathSelect.setActionInfo(UINewConfigForm.FIELD_CATEGORYPATH, new String[] {"AddPath"});
      documentSelect.setActionInfo(UINewConfigForm.FIELD_DOCNAME, new String[] {"DocSelect"});
      if (isAddNew) {
        setActions(UINewConfigForm.ADD_NEW_ACTION);
        detailtempField.setOptions(uiConfigTabPane.getBoxTemplateOption());
        allowPublishField.setChecked(isAllowPublish);
        enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
        enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
        categoryPathField.setValue(path);
        documentNameField.setValue(docName);
      }else {
        setActions(UINewConfigForm.NORMAL_ACTION);
      }
    } else {
      setActions(UINewConfigForm.DEFAULT_ACTION);
      categoryPathSelect.setActionInfo(UINewConfigForm.FIELD_CATEGORYPATH, null);
      documentSelect.setActionInfo(UINewConfigForm.FIELD_DOCNAME, null);
      repository = preference.getValue(Utils.REPOSITORY, "");
      path = preference.getValue(Utils.JCR_PATH, "");
      docName = preference.getValue(Utils.CB_DOCUMENT_NAME, "");
      hasComment = preference.getValue(Utils.CB_VIEW_COMMENT, "");
      hasVote = preference.getValue(Utils.CB_VIEW_VOTE, "");
      detailTemplate = preference.getValue(Utils.CB_BOX_TEMPLATE, "");
      categoryPathField.setValue(path);
      documentNameField.setValue(docName);
      detailtempField.setOptions(uiConfigTabPane.getBoxTemplateOption());
      detailtempField.setValue(detailTemplate);
      allowPublishField.setChecked(isAllowPublish);
      enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
      enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
    }
    detailtempField.setEnable(isEdit_);
    allowPublishField.setEnable(isEdit_);
    enableCommentField.setEnable(isEdit_);
    enableVoteField.setEnable(isEdit_);
  }

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    UIConfigTabPane uiConfig = getAncestorOfType(UIConfigTabPane.class);
    if (uiConfig.getChildById(UIConfigTabPane.PATH_SELECTOR) != null ) {
      UIFormInputSetWithAction categoryPathSelect = getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      categoryPathField.setValue(value.toString());
      UIFormInputSetWithAction documentSelect = getChildById(FIELD_DOCSELECT);
      UIFormStringInput documentNameField = documentSelect.getChildById(UINewConfigForm.FIELD_DOCNAME);
      documentNameField.setValue("");
    }
    if (uiConfig.getChildById(UIConfigTabPane.DOCUMENT_SELECTOR) != null ) {
      UIFormInputSetWithAction categoryPathSelect = getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      String path = categoryPathField.getValue();
      UIFormInputSetWithAction documentSelect = getChildById(FIELD_DOCSELECT);
      UIFormStringInput documentNameField = documentSelect.getChildById(UINewConfigForm.FIELD_DOCNAME);
      documentNameField.setValue(String.valueOf(value));
    }
    uiConfig.getChild(UIPopupWindow.class).setShow(false);
    isEdit_ = true;
    uiConfig.setNewConfig(true);
  }

  public static class SaveActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm = event.getSource();
      UIBrowseContentPortlet uiBrowseContentPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      UIBrowseContainer container = uiBrowseContentPortlet.findFirstComponentOfType(UIBrowseContainer.class);
      PortletPreferences prefs = container.getPortletPreferences();
      String workSpace = uiForm.getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
      String repository = uiForm.getUIStringInput(UINewConfigForm.FIELD_REPOSITORY).getValue();
      UIFormInputSetWithAction categoryPathSelect = uiForm.getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      String jcrPath = categoryPathField.getValue();
      UIApplication app = uiForm.getAncestorOfType(UIApplication.class);
      if (Utils.isNameEmpty(jcrPath)) {
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.require-path", null));
        
        return;
      }
      if (container.getNodeByPath(jcrPath, workSpace) == null) {
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.invalid-path", null));
        
        return;
      }
      UIFormInputSetWithAction documentSelect = uiForm.getChildById(FIELD_DOCSELECT);
      UIFormStringInput documentField = documentSelect.getChildById(UINewConfigForm.FIELD_DOCNAME);
      String docName = documentField.getValue();
      if(Utils.isNameEmpty(docName)) {
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.require-doc", null));
        
        return;
      }
      String fullPath = jcrPath.concat(docName).replaceAll("/+", "/");
      if(container.getNodeByPath(fullPath, workSpace) == null){
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.invalid-doc", null,
                                              ApplicationMessage.WARNING));
        
        return;
      }
      String boxTemplate = uiForm.getUIStringInput(UINewConfigForm.FIELD_DETAILBOXTEMP).getValue();
      boolean allowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      boolean hasComment = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLECOMMENT).isChecked();
      boolean hasVote = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLEVOTE).isChecked();
      prefs.setValue(Utils.CB_USECASE, Utils.CB_USE_DOCUMENT);
      prefs.setValue(Utils.REPOSITORY, repository);
      prefs.setValue(Utils.WORKSPACE_NAME, workSpace);
      prefs.setValue(Utils.JCR_PATH, jcrPath);
      prefs.setValue(Utils.CB_DOCUMENT_NAME, docName);
      prefs.setValue(Utils.CB_TEMPLATE, boxTemplate);
      prefs.setValue(Utils.CB_BOX_TEMPLATE, boxTemplate);
      prefs.setValue(Utils.CB_ALLOW_PUBLISH, String.valueOf(allowPublish));
      prefs.setValue(Utils.CB_VIEW_TOOLBAR,String.valueOf(hasComment || hasVote));
      prefs.setValue(Utils.CB_VIEW_COMMENT, String.valueOf(hasComment));
      prefs.setValue(Utils.CB_VIEW_VOTE, String.valueOf(hasVote));
      prefs.store();
      container.loadPortletConfig(prefs);
      uiForm.isEdit_ = false;
      uiForm.getAncestorOfType(UIConfigTabPane.class).setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }

  @SuppressWarnings("unused")
  public static class ChangeTemplateOptionActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
    }
  }

  public static class AddActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(true) ;
      uiConfigTabPane.showNewConfigForm(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }

  public static class CancelActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm = event.getSource();
      uiForm.isEdit_ = false;
      uiForm.getAncestorOfType(UIConfigTabPane.class).setNewConfig(false);
      UIBrowseContentPortlet uiBCPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiBCPortlet);
    }
  }
  public static class BackActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiForm.isEdit_ = false;
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }
  public static class EditActionListener extends EventListener<UIDocumentConfig>{
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm = event.getSource();
      uiForm.isEdit_ = true;
      uiForm.getAncestorOfType(UIConfigTabPane.class).setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }
  public static class AddPathActionListener extends EventListener<UIDocumentConfig> {
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm  = event.getSource();
      boolean allowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      UIConfigTabPane uiConfig = uiForm.getAncestorOfType(UIConfigTabPane.class);
      String workSpace = uiForm.getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
      String repo = uiForm.getUIStringInput(UINewConfigForm.FIELD_REPOSITORY).getValue();
      uiConfig.initPopupPathSelect(uiForm, repo, workSpace, UINewConfigForm.FIELD_CATEGORYPATH, true, allowPublish);
      uiForm.isEdit_ = true;
      uiConfig.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }

  public static class DocSelectActionListener extends EventListener<UIDocumentConfig> {
    public void execute(Event<UIDocumentConfig> event) throws Exception {
      UIDocumentConfig uiForm  = event.getSource();
      boolean allowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      UIFormInputSetWithAction categoryPathSelect = uiForm.getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      String workspace = uiForm.getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
      String repo = uiForm.getUIStringInput(UINewConfigForm.FIELD_REPOSITORY).getValue();
      String jcrPath = categoryPathField.getValue();
      UIApplication app = uiForm.getAncestorOfType(UIApplication.class);
      if (Utils.isNameEmpty(jcrPath)) {
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.require-path", null));        
        return;
      }
      UIBrowseContentPortlet uiBrowseContentPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      UIBrowseContainer container = uiBrowseContentPortlet.findFirstComponentOfType(UIBrowseContainer.class);
      if (container.getNodeByPath(jcrPath, workspace) == null) {
        app.addMessage(new ApplicationMessage("UIDocumentConfig.msg.invalid-path", null));        
        return;
      }
      UIConfigTabPane uiConfig = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfig.initPopupDocumentSelect(uiForm, repo, workspace, jcrPath, allowPublish);
      uiForm.isEdit_ = true;
      uiConfig.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIConfigTabPane.class));
    }
  }
}
