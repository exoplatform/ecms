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
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.portlet.PortletPreferences;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
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
import org.exoplatform.webui.form.UIFormInputInfo;
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
      @EventConfig(listeners = UIPathConfig.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.EditActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.AddPathActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.BackActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.ChangeRepoActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.ChangeWorkspaceActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.EnableSearchActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPathConfig.AddSearchLocationActionListener.class)
    }
)
public class UIPathConfig extends UIForm implements UISelectable{
  final static public String FIELD_PATHSELECT = "path";
  final static public String FIELD_SEARCH_PATHSELECT = "searchPath";
  final static private String FIELD_SEARCH_EXPLAIN = "explain";
  protected boolean isEdit_ = false;
  private List<String> repoNames_ = new ArrayList<String>();
  private List<String> wsNames_ = new ArrayList<String>();

  public UIPathConfig()throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    List<SelectItemOption<String>> repositories = new ArrayList<SelectItemOption<String>>();
    List<SelectItemOption<String>> workspaces = new ArrayList<SelectItemOption<String>>();
    UIFormSelectBox repository =
      new UIFormSelectBox(UINewConfigForm.FIELD_REPOSITORY, UINewConfigForm.FIELD_REPOSITORY, repositories);
    repository.setOnChange("ChangeRepo");
    addChild(repository);
    UIFormSelectBox workspace =
      new UIFormSelectBox(UINewConfigForm.FIELD_WORKSPACE, UINewConfigForm.FIELD_WORKSPACE, workspaces);
    workspace.setOnChange("ChangeWorkspace");
    addChild(workspace);
    UIFormInputSetWithAction categoryPathSelect = new UIFormInputSetWithAction(FIELD_PATHSELECT);
    categoryPathSelect.addUIFormInput(new UIFormStringInput(UINewConfigForm.FIELD_CATEGORYPATH, null, null));
    addUIComponentInput(categoryPathSelect);
    UIFormCheckBoxInput uiEnableSearch =
      new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_SEARCH_PATH_ENABLE, null, null);
    uiEnableSearch.setOnChange("EnableSearch");
    addChild(uiEnableSearch);
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    String explainMsg = res.getString("UIPathConfig.msg.explain");
    UIFormInputSetWithAction uiSearchPathSelect = new UIFormInputSetWithAction(FIELD_SEARCH_PATHSELECT);
    uiSearchPathSelect.addUIFormInput(new UIFormInputInfo(FIELD_SEARCH_EXPLAIN,
        null, explainMsg));
    uiSearchPathSelect.addUIFormInput(new UIFormStringInput(UINewConfigForm.FIELD_SEARCH_LOCATION,
        null, null).setEditable(false));
    addUIComponentInput(uiSearchPathSelect);
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_FILTER_CATEGORY, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ALLOW_PUBLISH, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLEREFDOC, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLECHILDDOC, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLETAGMAP, null, null));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE, null, Options));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLETOOLBAR, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLECOMMENT, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLEVOTE, null, null));
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_ITEMPERPAGE, null, null));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_DETAILBOXTEMP, null, Options));
    setActions(UINewConfigForm.DEFAULT_ACTION);
  }

  public List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    UIConfigTabPane uiTabPane = getAncestorOfType(UIConfigTabPane.class);
    return uiTabPane.getWorkSpaceOption();
  }

  public PortletPreferences getPortletPreferences() {
    return getAncestorOfType(UIBrowseContentPortlet.class).getPortletPreferences();
  }

  private List<SelectItemOption<String>> getRepoOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    repoNames_.clear();
    RepositoryEntry repo = repositoryService.getCurrentRepository().getConfiguration();
    repoNames_.add(repo.getName());
    options.add(new SelectItemOption<String>(repo.getName(), repo.getName()));
    return options;
  }

  private ManageableRepository getRepository() throws Exception{
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    return repositoryService.getCurrentRepository();
  }

  private List<SelectItemOption<String>> getWorkSpaceOptionList() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    Session session;
    String[] workspaceNames = getApplicationComponent(RepositoryService.class)
    .getCurrentRepository().getWorkspaceNames();
    wsNames_.clear();
    for(String workspace:workspaceNames) {
      session = SessionProviderFactory.createSessionProvider().getSession(workspace, getRepository());
      try {
        session.getRootNode();
        wsNames_.add(workspace);
        options.add(new SelectItemOption<String>(workspace,workspace));
      } catch(AccessDeniedException ace) {
        continue;
      }
    }
    return options;
  }

  public void initForm(PortletPreferences preference, String repository,
      String workSpace, boolean isAddNew) throws Exception {
    String path = preference.getValue(Utils.JCR_PATH, "");
    boolean isAllowPublish = Boolean.parseBoolean(preference.getValue(Utils.CB_ALLOW_PUBLISH, ""));
    String hasSearchLocation = preference.getValue(Utils.CB_ENABLE_SEARCH_LOCATION, "");
    if (hasSearchLocation == null || hasSearchLocation.length() < 1) hasSearchLocation = "false";
    String hasToolBar = "true";
    String filterCategory = "true";
    String hasRefDoc ="true";
    String hasChildDoc = "true";
    String hasTagMap = "true";
    String hasComment = "true";
    String hasVote = "true";
    String itemPerPage = "20";
    String template = "";
    String detailTemp = "";
    UIFormInputSetWithAction searchPathSelect = getChildById(FIELD_SEARCH_PATHSELECT);
    UIFormStringInput searchLocationInput = findComponentById(UINewConfigForm.FIELD_SEARCH_LOCATION);
    if (Boolean.parseBoolean(hasSearchLocation)) {
      String searchLocationValue = preference.getValue(Utils.CB_SEARCH_LOCATION, "");
      if(searchLocationValue != null && searchLocationValue.length() > 0) {
        searchLocationInput.setValue(searchLocationValue);
      }
    }
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    String currentRepositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
    UIFormSelectBox repositoryField = getChildById(UINewConfigForm.FIELD_REPOSITORY);
    repositoryField.setOptions(getRepoOption());
    if (!repoNames_.contains(repository)) repository = getRepoOption().get(0).getValue();
    repositoryField.setValue(repository);
    UIFormSelectBox workSpaceField = getChildById(UINewConfigForm.FIELD_WORKSPACE);
    workSpaceField.setOptions(getWorkSpaceOptionList());
    if (!wsNames_.contains(workSpace)) {
      workSpace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    workSpaceField.setValue(workSpace);
    UIFormInputSetWithAction categoryPathSelect = getChildById(FIELD_PATHSELECT);
    UIFormStringInput categoryPathField =
      categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
    categoryPathField.setEditable(false);

    UIFormSelectBox templateField = getChildById(UINewConfigForm.FIELD_TEMPLATE);
    UIFormStringInput numbPerPageField = getChildById(UINewConfigForm.FIELD_ITEMPERPAGE);
    UIFormSelectBox detailtemField = getChildById(UINewConfigForm.FIELD_DETAILBOXTEMP);
    UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
    UIFormCheckBoxInput enableFilterCategory = getChildById(UINewConfigForm.FIELD_FILTER_CATEGORY);
    UIFormCheckBoxInput enableToolBarField = getChildById(UINewConfigForm.FIELD_ENABLETOOLBAR);
    UIFormCheckBoxInput enablePublishField = getChildById(UINewConfigForm.FIELD_ALLOW_PUBLISH);
    UIFormCheckBoxInput enableRefDocField = getChildById(UINewConfigForm.FIELD_ENABLEREFDOC);
    UIFormCheckBoxInput enableChildDocField = getChildById(UINewConfigForm.FIELD_ENABLECHILDDOC);
    UIFormCheckBoxInput enableTagMapField = getChildById(UINewConfigForm.FIELD_ENABLETAGMAP);
    UIFormCheckBoxInput enableCommentField = getChildById(UINewConfigForm.FIELD_ENABLECOMMENT);
    UIFormCheckBoxInput enableVoteField = getChildById(UINewConfigForm.FIELD_ENABLEVOTE);
    UIFormCheckBoxInput enableSearchLocation = getChildById(UINewConfigForm.FIELD_SEARCH_PATH_ENABLE);
    if (isEdit_) {
      categoryPathSelect.setActionInfo(UINewConfigForm.FIELD_CATEGORYPATH, new String[] {"AddPath"});
      if (enableSearchLocation.isChecked()) {
        searchPathSelect.setActionInfo(UINewConfigForm.FIELD_SEARCH_LOCATION, new String[] {"AddSearchLocation"});
      } else {
        searchPathSelect.setActionInfo(UINewConfigForm.FIELD_SEARCH_LOCATION, null);
      }
      if (isAddNew) {
        templateField.setOptions(getTemplateOption());
        detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
        enableFilterCategory.setChecked(Boolean.parseBoolean(filterCategory));
        enableToolBarField.setChecked(Boolean.parseBoolean(hasToolBar));
        enablePublishField.setChecked(isAllowPublish);
        enableRefDocField.setChecked(Boolean.parseBoolean(hasRefDoc));
        enableChildDocField.setChecked(Boolean.parseBoolean(hasChildDoc));
        enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
        enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
        enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
        enableSearchLocation.setChecked(Boolean.parseBoolean(hasSearchLocation));
        numbPerPageField.setValue(itemPerPage);
        setActions(UINewConfigForm.ADD_NEW_ACTION);
      } else {
        setActions(UINewConfigForm.NORMAL_ACTION);
      }
    } else {
      categoryPathSelect.setActionInfo(UINewConfigForm.FIELD_CATEGORYPATH, null);
      searchPathSelect.setActionInfo(UINewConfigForm.FIELD_SEARCH_LOCATION, null);
      setActions(UINewConfigForm.DEFAULT_ACTION);
      repository = preference.getValue(Utils.REPOSITORY, "");
      workSpace = preference.getValue(Utils.WORKSPACE_NAME, "");
      template = preference.getValue(Utils.CB_TEMPLATE, "");
      filterCategory = preference.getValue(Utils.CB_FILTER_CATEGORY, "");
      hasToolBar = preference.getValue(Utils.CB_VIEW_TOOLBAR, "");
      hasRefDoc = preference.getValue(Utils.CB_REF_DOCUMENT, "");
      hasChildDoc = preference.getValue(Utils.CB_CHILD_DOCUMENT, "");
      hasTagMap = preference.getValue(Utils.CB_VIEW_TAGMAP, "");
      hasComment = preference.getValue(Utils.CB_VIEW_COMMENT, "");
      hasVote = preference.getValue(Utils.CB_VIEW_VOTE, "");
      hasSearchLocation = preference.getValue(Utils.CB_ENABLE_SEARCH_LOCATION, "");
      itemPerPage = (preference.getValue(Utils.CB_NB_PER_PAGE, ""));
      detailTemp = (preference.getValue(Utils.CB_BOX_TEMPLATE, ""));
      if (!repoNames_.contains(repository)) repository = currentRepositoryName;
      templateField.setOptions(getTemplateOption());
      templateField.setValue(template);
      detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
      detailtemField.setValue(detailTemp);
      repositoryField.setValue(repository);
      workSpaceField.setValue(workSpace);
      enableFilterCategory.setChecked(Boolean.parseBoolean(filterCategory));
      enableToolBarField.setChecked(Boolean.parseBoolean(hasToolBar));
      enablePublishField.setChecked(isAllowPublish);
      enableRefDocField.setChecked( Boolean.parseBoolean(hasRefDoc));
      enableChildDocField.setChecked(Boolean.parseBoolean(hasChildDoc));
      enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
      enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
      enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
      enableSearchLocation.setChecked(Boolean.parseBoolean(hasSearchLocation));
      categoryPathField.setValue(path);
      numbPerPageField.setValue(itemPerPage);
    }
    if(enableSearchLocation.isChecked()) searchPathSelect.setRendered(true);
    else searchPathSelect.setRendered(false);
    templateField.setEnable(isEdit_);
    enableFilterCategory.setEnable(isEdit_);
    detailtemField.setEnable(isEdit_);
    enableToolBarField.setEnable(isEdit_);
    enablePublishField.setEnable(isEdit_);
    enableRefDocField.setEnable(isEdit_);
    enableCommentField.setEnable(isEdit_);
    enableVoteField.setEnable(isEdit_);
    enableSearchLocation.setEnable(isEdit_);
    enableTagMapField.setEnable(isEdit_);
    numbPerPageField.setEditable(isEdit_);
    enableChildDocField.setEnable(isEdit_);
    repositoryField.setEnable(isEdit_);
    workSpaceField.setEnable(isEdit_);
  }

  @SuppressWarnings("unchecked")
  @Deprecated
  public List<SelectItemOption<String>> getTemplateOption(String repository) throws Exception {
    return getTemplateOption();
  }
  
  @SuppressWarnings("unchecked")
  public List<SelectItemOption<String>> getTemplateOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    ManageViewService viewService =
      (ManageViewService)PortalContainer.getComponent(ManageViewService.class);
    List<Node> scriptTemplates = viewService.getAllTemplates(BasePath.CB_PATH_TEMPLATES,
          SessionProviderFactory.createSystemProvider());
    for(Node template:scriptTemplates) {
      options.add(new SelectItemOption<String>(template.getName(),template.getName()));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    return options;
  }
  

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    UIFormStringInput uiPath = findComponentById(selectField);
    uiPath.setValue(value.toString());
    UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
    UIPopupWindow uiPopupWindow = uiConfigTabPane.getChild(UIPopupWindow.class);
    uiPopupWindow.setShow(false);
    isEdit_ = true;
    uiConfigTabPane.setNewConfig(true);
  }

  @SuppressWarnings("unused")
  public static class SaveActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIBrowseContentPortlet uiBrowseContentPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      UIBrowseContainer uiBCContainer =
        uiBrowseContentPortlet.findFirstComponentOfType(UIBrowseContainer.class);
      PortletPreferences prefs = uiBCContainer.getPortletPreferences();
      UIFormSelectBox workSpaceField = uiForm.getChildById(UINewConfigForm.FIELD_WORKSPACE);
      String workSpace = workSpaceField.getValue();
      UIFormSelectBox repositoryField = uiForm.getChildById(UINewConfigForm.FIELD_REPOSITORY);
      String repository = repositoryField.getValue();
      UIFormStringInput categoryPathField = uiForm.findComponentById(UINewConfigForm.FIELD_CATEGORYPATH);
      String jcrPath = categoryPathField.getValue();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if((jcrPath == null) || (jcrPath.trim().length() == 0)) {
        uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.require-path", null,
                                              ApplicationMessage.WARNING));
        
        return;
      }
      try {
        NodeFinder nodeFinder = uiForm.getApplicationComponent(NodeFinder.class);
        Node nodePath = (Node) nodeFinder.getItem(workSpace, jcrPath);
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.invalid-path", null,
                                              ApplicationMessage.WARNING));
        
        return;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.access-denied", null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      String template = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE).getValue();
      String itemPerPage = uiForm.getUIStringInput(UINewConfigForm.FIELD_ITEMPERPAGE).getValue();
      try {
        if (Integer.parseInt(itemPerPage) <= 0) {
          uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.invalid-value", null));
          
          return;
        }
      } catch (NumberFormatException e) {
        uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.invalid-number", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      String boxTemplate = uiForm.getUIStringInput(UINewConfigForm.FIELD_DETAILBOXTEMP).getValue();
      boolean hasChildDoc = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLECHILDDOC).isChecked();
      boolean filterCategory = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_FILTER_CATEGORY).isChecked();
      boolean isAllowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      boolean hasRefDoc = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLEREFDOC).isChecked();
      boolean hasToolBar = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLETOOLBAR).isChecked();
      boolean hasTagMap = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLETAGMAP).isChecked();
      boolean hasComment = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLECOMMENT).isChecked();
      boolean hasVote = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLEVOTE).isChecked();
      boolean hasEnableSearch =
        uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_SEARCH_PATH_ENABLE).isChecked();
      String searchLocation = null;
      if(hasEnableSearch) {
        searchLocation =
          ((UIFormStringInput)uiForm.findComponentById(UINewConfigForm.FIELD_SEARCH_LOCATION)).getValue();
        if(searchLocation == null || searchLocation.length() < 1) {
          uiApp.addMessage(new ApplicationMessage("UIPathConfig.msg.enter-search-location", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
      prefs.setValue(Utils.CB_USECASE, Utils.CB_USE_FROM_PATH);
      prefs.setValue(Utils.REPOSITORY, repository);
      prefs.setValue(Utils.WORKSPACE_NAME, workSpace);
      prefs.setValue(Utils.JCR_PATH, jcrPath);
      prefs.setValue(Utils.CB_FILTER_CATEGORY, String.valueOf(filterCategory));
      prefs.setValue(Utils.CB_NB_PER_PAGE, itemPerPage);
      prefs.setValue(Utils.CB_TEMPLATE, template);
      prefs.setValue(Utils.CB_BOX_TEMPLATE, boxTemplate);
      prefs.setValue(Utils.CB_ALLOW_PUBLISH, String.valueOf(isAllowPublish));
      prefs.setValue(Utils.CB_REF_DOCUMENT, String.valueOf(hasRefDoc));
      prefs.setValue(Utils.CB_CHILD_DOCUMENT, String.valueOf(hasChildDoc));
      prefs.setValue(Utils.CB_VIEW_TOOLBAR, String.valueOf(hasToolBar));
      prefs.setValue(Utils.CB_VIEW_TAGMAP, String.valueOf(hasTagMap));
      prefs.setValue(Utils.CB_VIEW_COMMENT, String.valueOf(hasComment));
      prefs.setValue(Utils.CB_VIEW_VOTE, String.valueOf(hasVote));
      prefs.setValue(Utils.CB_ENABLE_SEARCH_LOCATION, String.valueOf(hasEnableSearch));
      if (hasEnableSearch) {
        prefs.setValue(Utils.CB_SEARCH_LOCATION, searchLocation);
      }
      prefs.store();
      uiBCContainer.setShowDocumentDetail(false);
      uiBCContainer.setWorkspaceName(workSpace);
      uiBCContainer.loadPortletConfig(prefs);
      uiForm.isEdit_ = false;
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setIsChangeValue(false);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }

  public static class AddActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setIsChangeValue(false);
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class CancelActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      uiForm.isEdit_ = false;
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setIsChangeValue(false);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class BackActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiForm.isEdit_ =  false;
      uiConfigTabPane.setIsChangeValue(false);
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }

  public static class EditActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      uiForm.isEdit_ = true;
      uiForm.getAncestorOfType(UIConfigTabPane.class).setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }
  static public class AddPathActionListener extends EventListener<UIPathConfig> {
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm  = event.getSource();
      UIConfigTabPane uiConfig = uiForm.getAncestorOfType(UIConfigTabPane.class);
      String repo = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_REPOSITORY).getValue();
      String workSpace = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_WORKSPACE).getValue();
      uiConfig.initPopupPathSelect(uiForm, repo, workSpace, UINewConfigForm.FIELD_CATEGORYPATH, true);
      uiForm.isEdit_ = true;
      uiConfig.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfig);
    }
  }

  public static class ChangeRepoActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setIsChangeValue(true);
      uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_WORKSPACE).setOptions(uiForm.getWorkSpaceOptionList());
      UIFormInputSetWithAction categoryPathSelect = uiForm.getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField = categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      categoryPathField.setValue("/");
      UIFormSelectBox detailtemField = uiForm.getChildById(UINewConfigForm.FIELD_DETAILBOXTEMP);
      detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  public static class ChangeWorkspaceActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setIsChangeValue(true);
      UIFormInputSetWithAction categoryPathSelect = uiForm.getChildById(FIELD_PATHSELECT);
      UIFormStringInput categoryPathField =
        categoryPathSelect.getChildById(UINewConfigForm.FIELD_CATEGORYPATH);
      categoryPathField.setValue("/");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  public static class EnableSearchActionListener extends EventListener<UIPathConfig>{
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm = event.getSource();
      UIFormCheckBoxInput uiEnableSearch =
        uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_SEARCH_PATH_ENABLE);
      UIFormInputSetWithAction uiSearchLocation = uiForm.getChildById(FIELD_SEARCH_PATHSELECT);
      uiSearchLocation.setRendered(uiEnableSearch.isChecked());
      if(uiEnableSearch.isChecked()) {
        uiSearchLocation.setActionInfo(UINewConfigForm.FIELD_SEARCH_LOCATION,
            new String[] {"AddSearchLocation"});
      } else {
        uiSearchLocation.setActionInfo(UINewConfigForm.FIELD_SEARCH_LOCATION, null);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  static public class AddSearchLocationActionListener extends EventListener<UIPathConfig> {
    public void execute(Event<UIPathConfig> event) throws Exception {
      UIPathConfig uiForm  = event.getSource();
      UIConfigTabPane uiConfig = uiForm.getAncestorOfType(UIConfigTabPane.class);
      RepositoryService repositoryService = uiForm.getApplicationComponent(RepositoryService.class);
      String repo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String workSpace =
        repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      uiConfig.initPopupPathSelect(uiForm, repo, workSpace, UINewConfigForm.FIELD_SEARCH_LOCATION, false);
      uiForm.isEdit_ = true;
      uiConfig.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfig);
    }
  }
}
