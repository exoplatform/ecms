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
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
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
      @EventConfig(listeners = UIScriptConfig.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIScriptConfig.EditActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIScriptConfig.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIScriptConfig.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIScriptConfig.BackActionListener.class)
    }
)
public class UIScriptConfig extends UIForm {

  protected boolean isEdit_ = false;

  public UIScriptConfig() {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_REPOSITORY, UINewConfigForm.FIELD_REPOSITORY, null));
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_WORKSPACE, UINewConfigForm.FIELD_WORKSPACE, null));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_SCRIPTNAME, null, Options));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE, null, Options));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_DETAILBOXTEMP, null, Options));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ALLOW_PUBLISH, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLETAGMAP, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLECOMMENT, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLEVOTE, null, null));
    setActions(UINewConfigForm.DEFAULT_ACTION);
  }

  public void initForm(PortletPreferences preference, String repository, String workSpace, boolean isAddNew) throws Exception {
    String hasComment = "false";
    String hasVote = "false";
    String hasTagMap = "false";
    String scriptName = "";
    String templateName = "";
    String detailTemplate = "";
    boolean isAllowPublish = Boolean.parseBoolean(preference.getValue(Utils.CB_ALLOW_PUBLISH, ""));
    UIFormStringInput repositoryField = getChildById(UINewConfigForm.FIELD_REPOSITORY);
    repositoryField.setValue(repository);
    repositoryField.setEditable(false);
    UIFormStringInput workSpaceField = getChildById(UINewConfigForm.FIELD_WORKSPACE);
    workSpaceField.setValue(workSpace);
    workSpaceField.setEditable(false);
    UIFormSelectBox scriptField = getChildById(UINewConfigForm.FIELD_SCRIPTNAME);
    UIFormSelectBox templateField = getChildById(UINewConfigForm.FIELD_TEMPLATE);
    UIFormSelectBox detailtemField = getChildById(UINewConfigForm.FIELD_DETAILBOXTEMP);
    UIFormCheckBoxInput allowPublishField = getChildById(UINewConfigForm.FIELD_ALLOW_PUBLISH);
    UIFormCheckBoxInput enableTagMapField = getChildById(UINewConfigForm.FIELD_ENABLETAGMAP);
    UIFormCheckBoxInput enableCommentField = getChildById(UINewConfigForm.FIELD_ENABLECOMMENT);
    UIFormCheckBoxInput enableVoteField = getChildById(UINewConfigForm.FIELD_ENABLEVOTE);
    scriptField.setOptions(getScriptOption());
    templateField.setOptions(getTemplateOption());
    UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
    detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
    if (isEdit_) {
      if(isAddNew) {
        setActions(UINewConfigForm.ADD_NEW_ACTION);
        allowPublishField.setChecked(isAllowPublish);
        enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
        enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
        enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
      } else {
        setActions(UINewConfigForm.NORMAL_ACTION);
      }
    } else {
      setActions(UINewConfigForm.DEFAULT_ACTION);
      repository = preference.getValue(Utils.REPOSITORY, "");
      scriptName = preference.getValue(Utils.CB_SCRIPT_NAME, "");
      scriptField.setValue(scriptName);
      templateName = preference.getValue(Utils.CB_TEMPLATE, "");
      templateField.setValue(templateName);
      detailTemplate = preference.getValue(Utils.CB_BOX_TEMPLATE, "");
      detailtemField.setValue(detailTemplate);
      hasComment = preference.getValue(Utils.CB_VIEW_COMMENT, "");
      hasVote = preference.getValue(Utils.CB_VIEW_VOTE, "");
      hasTagMap = preference.getValue(Utils.CB_VIEW_TAGMAP, "");
    }
    allowPublishField.setChecked(isAllowPublish);
    enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
    enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
    enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
    enableCommentField.setEnable(isEdit_);
    allowPublishField.setEnable(isEdit_);
    enableTagMapField.setEnable(isEdit_);
    scriptField.setEnable(isEdit_);
    templateField.setEnable(isEdit_);
    detailtemField.setEnable(isEdit_);
    enableVoteField.setEnable(isEdit_);
  }

  public List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    UIConfigTabPane uiTabPane = getAncestorOfType(UIConfigTabPane.class);
    return uiTabPane.getWorkSpaceOption();
  }
  @SuppressWarnings("unchecked")
  private List<SelectItemOption<String>> getTemplateOption() throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    List<Node> scriptTemplates = getApplicationComponent(ManageViewService.class).
                getAllTemplates(BasePath.CB_SCRIPT_TEMPLATES, SessionProviderFactory.createSystemProvider());
    for(Node template:scriptTemplates) {
      Options.add(new SelectItemOption<String>(template.getName(),template.getName()));
    }
    Collections.sort(Options, new ItemOptionNameComparator());
    return Options;
  }

  private List<SelectItemOption<String>> getScriptOption() throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    Node cbScripts = getApplicationComponent(ScriptService.class).getCBScriptHome(SessionProviderFactory.createSystemProvider());
    NodeIterator nodeList = cbScripts.getNodes();
    while (nodeList.hasNext()) {
      Node node = nodeList.nextNode();
      Options.add(new SelectItemOption<String>(node.getName(), node.getName()));
    }
    return Options;
  }

  public static class SaveActionListener extends EventListener<UIScriptConfig>{
    public void execute(Event<UIScriptConfig> event) throws Exception {
      UIScriptConfig uiForm = event.getSource();
      UIBrowseContentPortlet uiBrowseContentPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      PortletPreferences prefs = uiBrowseContentPortlet.getPortletPreferences();
      String repository = uiForm.getUIStringInput(UINewConfigForm.FIELD_REPOSITORY).getValue();
      String workSpace = uiForm.getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
      String scriptName = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_SCRIPTNAME).getValue();
      String template = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE).getValue();
      String boxTemplate = uiForm.getUIStringInput(UINewConfigForm.FIELD_DETAILBOXTEMP).getValue();
      boolean allowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      boolean hasTagMap = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLETAGMAP).isChecked();
      boolean hasComment = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLECOMMENT).isChecked();
      boolean hasVote = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLEVOTE).isChecked();
      prefs.setValue(Utils.CB_USECASE, Utils.CB_USE_SCRIPT);
      prefs.setValue(Utils.REPOSITORY, repository);
      prefs.setValue(Utils.WORKSPACE_NAME, workSpace);
      prefs.setValue(Utils.CB_SCRIPT_NAME, scriptName);
      prefs.setValue(Utils.CB_TEMPLATE, template);
      prefs.setValue(Utils.CB_BOX_TEMPLATE, boxTemplate);
      prefs.setValue(Utils.CB_ALLOW_PUBLISH, String.valueOf(allowPublish));
      prefs.setValue(Utils.CB_VIEW_TAGMAP, String.valueOf(hasTagMap));
      prefs.setValue(Utils.CB_VIEW_COMMENT,String.valueOf(hasComment));
      prefs.setValue(Utils.CB_VIEW_VOTE,String.valueOf(hasVote));
      prefs.store();
      uiBrowseContentPortlet.getChild(UIBrowseContainer.class).setShowDocumentDetail(false);
      uiBrowseContentPortlet.getChild(UIBrowseContainer.class).loadPortletConfig(prefs);
      uiForm.isEdit_ = false;
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabpane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }

  public static class AddActionListener extends EventListener<UIScriptConfig>{
    public void execute(Event<UIScriptConfig> event) throws Exception {
      UIScriptConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class CancelActionListener extends EventListener<UIScriptConfig>{
    public void execute(Event<UIScriptConfig> event) throws Exception {
      UIScriptConfig uiForm = event.getSource();
      uiForm.isEdit_ = false;
      uiForm.getAncestorOfType(UIConfigTabPane.class).setNewConfig(false);
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }
  public static class BackActionListener extends EventListener<UIScriptConfig>{
    public void execute(Event<UIScriptConfig> event) throws Exception {
      UIScriptConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiForm.isEdit_ =  false;
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class EditActionListener extends EventListener<UIScriptConfig>{
    public void execute(Event<UIScriptConfig> event) throws Exception {
      UIScriptConfig uiForm = event.getSource();
      uiForm.isEdit_ = true;
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
}

