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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@yahoo.com
 * Jun 28, 2006
 */

public class UIViewForm extends UIForm implements UISelectable {

  final static public String FIELD_VERSION = "version" ;
  final static public String FIELD_NAME = "viewName" ;  
  final static public String FIELD_TABS = "tabs" ;
  final static public String FIELD_TEMPLATE = "template" ;
  final static public String FIELD_ENABLEVERSION = "enableVersion" ;
  final static public String FIELD_PERMISSION = "permission" ;
  final static public String FIELD_HIDE_EXPLORER_PANEL = "hideExplorerPanel" ;

  private boolean isView_ = true ;
  private NodeLocation views_;
  private HashMap<String, Tab> tabMap_ = new HashMap<String, Tab>() ;
  private ManageViewService vservice_ = null ;
  private String viewName_ = null;
  private String permission = StringUtils.EMPTY;
  private List<String> listVersion = new ArrayList<String>() ;
  private String baseVersionName_;
  private VersionNode selectedVersion_;
  private VersionNode rootVersionNode;
  Map<String, String> templateMap = new HashMap<String, String>();
  Map<String, String> tempMap = new HashMap<String, String>();

  public String getViewName() {
    return viewName_;
  }

  public void setViewName(String viewName) {
    this.viewName_ = viewName;
  }

  public String getPermission() {
    return permission;
  }

  public String[] getActions() { return new String[] {}; }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public UIViewForm() throws Exception {
    this("UIViewForm");  	
  }  

  public UIViewForm(String name) throws Exception {
    setComponentConfig(getClass(), null) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox versions = new UIFormSelectBox(FIELD_VERSION , FIELD_VERSION, options) ;
    versions.setOnChange("ChangeVersion");
    versions.setRendered(false) ;
    addUIFormInput(versions) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class)
                   .addValidator(ECMNameValidator.class)) ;
    vservice_ = getApplicationComponent(ManageViewService.class) ;
    Node ecmTemplateHome = vservice_.getTemplateHome(BasePath.ECM_EXPLORER_TEMPLATES, WCMCoreUtils.getSystemSessionProvider());
    List<SelectItemOption<String>> temp = new ArrayList<SelectItemOption<String>>() ;
    if(ecmTemplateHome != null) {
      NodeIterator iter = ecmTemplateHome.getNodes() ;
      while(iter.hasNext()) {
        Node tempNode = iter.nextNode() ;
        temp.add(new SelectItemOption<String>(tempNode.getName(),tempNode.getName())) ;
        templateMap.put(tempNode.getName(), tempNode.getPath());
        tempMap.put(tempNode.getPath(), tempNode.getName());
      }
    }
    addUIFormInput(new UIFormSelectBox(FIELD_TEMPLATE,FIELD_TEMPLATE, temp)) ;
    UICheckBoxInput enableVersion = new UICheckBoxInput(FIELD_ENABLEVERSION, FIELD_ENABLEVERSION, null) ;
    enableVersion.setRendered(true) ;
    addUIFormInput(enableVersion) ;
    //prefernce: is show side bar
    UICheckBoxInput hideExplorerPanel = 
        new UICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL, FIELD_HIDE_EXPLORER_PANEL, false);
    hideExplorerPanel.setRendered(false);
    addUIFormInput(hideExplorerPanel);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context) ;
  }

  public void doSelect(String selectField, Object value) {
    UIFormStringInput uiStringInput = getUIStringInput(selectField);
    uiStringInput.setValue(value.toString());
  }

  public boolean isView() { return isView_ ; }

  public Node getViews() {
    return NodeLocation.getNodeByLocation(views_);
  }

  public boolean canEnableVersionning(Node node) throws Exception {
    return node.canAddMixin(Utils.MIX_VERSIONABLE);
  }

  private boolean isVersioned(Node node) throws RepositoryException {
    return node.isNodeType(Utils.MIX_VERSIONABLE);
  }

  private VersionNode getRootVersion(Node node) throws Exception{
    VersionHistory vH = node.getVersionHistory() ;
    if(vH != null) return new VersionNode(vH.getRootVersion(), node.getSession()) ;
    return null ;
  }

  private List<String> getNodeVersions(List<VersionNode> children) throws Exception {
    List<VersionNode> child = new ArrayList<VersionNode>() ;
    for(VersionNode vNode : children){
      listVersion.add(vNode.getName());
      child = vNode.getChildren() ;
      if (!child.isEmpty()) getNodeVersions(child) ;
    }
    return listVersion ;
  }

  private List<SelectItemOption<String>> getVersionValues(Node node) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    List<VersionNode> children = getRootVersion(node).getChildren() ;
    listVersion.clear() ;
    List<String> versionList = getNodeVersions(children) ;
    for(int i = 0; i < versionList.size(); i++) {
      for(int j = i + 1; j < versionList.size(); j ++) {
        if( Integer.parseInt(versionList.get(j)) < Integer.parseInt(versionList.get(i))) {
          String temp = versionList.get(i) ;
          versionList.set(i, versionList.get(j))  ;
          versionList.set(j, temp) ;
        }
      }
      options.add(new SelectItemOption<String>(versionList.get(i), versionList.get(i))) ;
    }
    return options ;
  }

  public HashMap<String, Tab> getTabMap() {
    return tabMap_;
  }

  public void addTab(String tabName, String buttons){
    Tab tab = new Tab() ;
    tab.setTabName(tabName) ;
    tab.setButtons(buttons) ;
    tab.setLocalizeButtons(getLocalizationButtons(buttons));
    tabMap_.put(tabName, tab) ;
  }

  public String getLocalizationButtons(String buttons) {
    StringBuilder localizationButtons = new StringBuilder();
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    if(buttons.contains(";")) {
      String[] arrButtons = buttons.split(";");
      for(int i = 0; i < arrButtons.length; i++) {
        try {
          localizationButtons.append(res.getString("UITabForm.label." + arrButtons[i].trim()));
        } catch(MissingResourceException mre) {
          localizationButtons.append(arrButtons[i]);
        }
        if(i < arrButtons.length - 1) {
          localizationButtons.append(", ");
        }
      }
    } else {
      try {
        localizationButtons.append(res.getString("UITabForm.label." + buttons.trim()));
      } catch(MissingResourceException mre) {
        localizationButtons.append(buttons.trim());
      }
    }
    return localizationButtons.toString();
  }  

  public String getTabList() throws Exception {
    StringBuilder result = new StringBuilder() ;
    List<Tab> tabList = new ArrayList<Tab>(tabMap_.values());
    if(result != null) {
      for(Tab tab : tabList) {
        if(result.length() > 0) result.append(",") ;
        result.append(tab.getTabName()) ;
      }
    }
    return result.toString() ;
  }

  public List<Tab> getTabs() throws Exception {
    return new ArrayList<Tab>(tabMap_.values());
  }  

  public void refresh(boolean isAddNew) throws Exception {
    getUIFormSelectBox(FIELD_VERSION).setRendered(!isAddNew) ;
    getUIFormSelectBox(FIELD_VERSION).setDisabled(!isAddNew) ;
    getUIStringInput(FIELD_NAME).setDisabled(!isAddNew).setValue(null) ;
    getUIFormSelectBox(FIELD_TEMPLATE).setValue(null) ;
    getUIFormSelectBox(FIELD_TEMPLATE).setDisabled(!isAddNew) ;
    getUICheckBoxInput(FIELD_ENABLEVERSION).setRendered(!isAddNew) ;
    getUICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL).setRendered(!isAddNew);
    setViewName("");
    if(isAddNew) {
      tabMap_.clear() ;
      views_ = null ;
      getUICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL).setValue(false);
    }
    selectedVersion_ = null ;
    baseVersionName_ = null ;
  }

  public void update(Node viewNode, boolean isView, VersionNode selectedVersion) throws Exception {
    isView_ = isView ;
    if(viewNode != null) {
      setPermission(viewNode.getProperty("exo:accessPermissions").getString());
      views_ = NodeLocation.getNodeLocationByNode(viewNode);
      if(isVersioned(viewNode)) baseVersionName_ = viewNode.getBaseVersion().getName();
      tabMap_.clear() ;
      for(NodeIterator iter = viewNode.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        String buttons = tab.getProperty("exo:buttons").getString() ;
        Tab tabObj = new Tab() ;
        tabObj.setTabName(tab.getName()) ;
        tabObj.setButtons(buttons) ;
        tabObj.setLocalizeButtons(getLocalizationButtons(buttons));
        tabMap_.put(tab.getName(), tabObj) ;
      }

      getUICheckBoxInput(FIELD_ENABLEVERSION).setRendered(true) ;
      if (isVersioned(viewNode)) {
        rootVersionNode = getRootVersion(viewNode);
        getUIFormSelectBox(FIELD_VERSION).setOptions(getVersionValues(viewNode)).setRendered(true) ;
        getUIFormSelectBox(FIELD_VERSION).setValue(baseVersionName_) ;
        getUICheckBoxInput(FIELD_ENABLEVERSION).setChecked(true) ;
        getUICheckBoxInput(FIELD_ENABLEVERSION).setDisabled(false);
      } else if (!isVersioned(viewNode)) {
        getUIFormSelectBox(FIELD_VERSION).setRendered(false) ;
        getUICheckBoxInput(FIELD_ENABLEVERSION).setChecked(false) ;
        getUICheckBoxInput(FIELD_ENABLEVERSION).setDisabled(false);
      }
      //pref is show side bar
      getUICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL).setRendered(true);
      getUICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL).setValue(
                                                             viewNode.getProperty(NodetypeConstant.EXO_HIDE_EXPLORER_PANEL).getBoolean());
    }
    //---------------------
    Node viewsNode = NodeLocation.getNodeByLocation(views_);
    if (selectedVersion != null) {
      viewsNode.restore(selectedVersion.getName(), false) ;
      viewsNode.checkout() ;
      tabMap_.clear() ;
      for(NodeIterator iter = viewsNode.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        String buttons = tab.getProperty("exo:buttons").getString() ;
        Tab tabObj = new Tab() ;
        tabObj.setTabName(tab.getName()) ;
        tabObj.setButtons(buttons) ;
        tabMap_.put(tab.getName(), tabObj) ;
      }
      selectedVersion_ = selectedVersion;
    }
    if(viewsNode != null) {
      getUIStringInput(FIELD_NAME).setDisabled(true).setValue(viewsNode.getName()) ;
      getUIFormSelectBox(FIELD_TEMPLATE).setValue(tempMap.get(viewsNode.getProperty("exo:template").getString()));
    }
  }

  public void save() throws Exception {
    String viewName = getUIStringInput(FIELD_NAME).getValue();
    ApplicationMessage message ;
    if(viewName == null || viewName.trim().length() == 0){
      throw new MessageException(new ApplicationMessage("UIViewForm.msg.view-name-invalid", null,
                                                        ApplicationMessage.WARNING)) ;
    }
    viewName = viewName.trim();
    
    boolean isEnableVersioning = getUICheckBoxInput(FIELD_ENABLEVERSION).isChecked() ;
    boolean hideExplorerPanel = getUICheckBoxInput(FIELD_HIDE_EXPLORER_PANEL).isChecked();
    List<ViewConfig> viewList = vservice_.getAllViews() ;
    UIPopupWindow uiPopup = getAncestorOfType(UIPopupWindow.class) ;
    uiPopup.setShowMask(true);
    if(uiPopup.getId().equals(UIViewList.ST_ADD)) {
      for(ViewConfig view : viewList) {
        if(view.getName().equals(viewName) && !isEnableVersioning) {
          message = new ApplicationMessage("UIViewForm.msg.view-exist", null,
                                           ApplicationMessage.WARNING) ;
          throw new MessageException(message) ;
        }
      }
    }

    if(tabMap_.size() < 1 ){
      message = new ApplicationMessage("UIViewForm.msg.mustbe-add-tab", null,
                                       ApplicationMessage.WARNING) ;
      throw new MessageException(message) ;
    }
    if(permission == null || permission.length() == 0){
      message = new ApplicationMessage("UIViewForm.msg.mustbe-add-permission", null,
                                       ApplicationMessage.WARNING) ;
      throw new MessageException(message) ;
    }    
    String template = templateMap.get(getUIFormSelectBox(FIELD_TEMPLATE).getValue());

    List<Tab> tabList = new ArrayList<Tab>(tabMap_.values());
    Node viewNode = NodeLocation.getNodeByLocation(views_);
    if(views_ == null || !isEnableVersioning) {
      vservice_.addView(viewName, permission, hideExplorerPanel, template, tabList) ;
      if(viewNode != null) {
        for(NodeIterator iter = viewNode.getNodes(); iter.hasNext(); ) {
          Node tab = iter.nextNode() ;
          if(!tabMap_.containsKey(tab.getName())) tab.remove() ;
        }
        viewNode.save() ;
      }
    } else {
      if (!isVersioned(viewNode)) {
        viewNode.addMixin(Utils.MIX_VERSIONABLE);
        viewNode.save();
      } else {
        viewNode.checkout() ;
      }
      for(NodeIterator iter = viewNode.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        if(!tabMap_.containsKey(tab.getName())) tab.remove() ;
      }
      vservice_.addView(viewName, permission, hideExplorerPanel, template, tabList) ;
      try {
        viewNode.save() ;
        viewNode.checkin();
      } catch (Exception e) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
        JCRExceptionManager.process(uiApp, e);
        return ;
      }
    }
    UIViewList uiViewList = getAncestorOfType(UIViewContainer.class).getChild(UIViewList.class);
    uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
    refresh(true) ;
  }

  public void editTab(String tabName) throws Exception {
    UIViewFormTabPane viewTabPane = getParent() ;
    UITabForm tabForm = viewTabPane.getChild(UITabForm.class) ;
    tabForm.update(tabMap_.get(tabName), isView_) ;
    viewTabPane.setSelectedTab(tabForm.getId()) ;
  }

  public void deleteTab(String tabName) throws Exception {
    UIViewFormTabPane viewTabPane = getParent() ;
    //String permLastest = viewTabPane.getUIStringInput(UIViewForm.FIELD_PERMISSION).getValue();
    tabMap_.remove(tabName) ;
    update(null, false, null) ;
    UIViewContainer uiViewContainer = getAncestorOfType(UIViewContainer.class) ;
    UIViewList uiViewList = uiViewContainer.getChild(UIViewList.class) ;
    uiViewList.refresh(uiViewList.getUIPageIterator().getCurrentPage());
    UIViewForm uiViewForm = viewTabPane.getChild(UIViewForm.class) ;
    viewTabPane.setSelectedTab(uiViewForm.getId()) ;
  }

  public void changeVersion() throws Exception {
    String path = NodeLocation.getNodeByLocation(views_)
        .getVersionHistory()
        .getVersion(getUIFormSelectBox(FIELD_VERSION).getValue())
        .getPath();
    VersionNode selectedVesion = rootVersionNode.findVersionNode(path);
    update(null, false, selectedVesion) ;
  }

  public void revertVersion() throws Exception {
    if (selectedVersion_ != null && !selectedVersion_.getName().equals(baseVersionName_)) {
      NodeLocation.getNodeByLocation(views_).restore(baseVersionName_, true);
    }
  }

  static public class ChangeVersionActionListener extends EventListener<UIViewForm> {
    public void execute(Event<UIViewForm> event) throws Exception {
      UIViewFormTabPane uiFormTab = event.getSource().getParent();
      UIViewForm uiForm = uiFormTab.getChild(UIViewForm.class);
      uiForm.changeVersion();
      UIViewContainer uiViewContainer = uiFormTab.getAncestorOfType(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
}
