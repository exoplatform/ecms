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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@yahoo.com
 * Jun 28, 2006
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl")
public class UIViewForm extends UIFormInputSetWithAction implements UISelectable {

  final static public String FIELD_VERSION = "version" ;
  final static public String FIELD_NAME = "viewName" ;
  final static public String FIELD_PERMISSION = "permission" ;
  final static public String FIELD_TABS = "tabs" ;
  final static public String FIELD_TEMPLATE = "template" ;
  final static public String FIELD_ENABLEVERSION = "enableVersion" ;

  private boolean isView_ = true ;
  private Node views_;
  private HashMap<String, Tab> tabMap_ = new HashMap<String, Tab>() ;
  private ManageViewService vservice_ = null ;
  private String viewName = null;
  private List<String> listVersion = new ArrayList<String>() ;
  private Version baseVersion_;
  private VersionNode selectedVersion_;
  private VersionNode rootVersionNode;

  public String getViewName() {
    return viewName;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

//  public String getRepository() {
//    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
//    PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
//    return portletPref.getValue(Utils.REPOSITORY, "") ;
//  }

  public UIViewForm(String name) throws Exception {
    super(name) ;
    setComponentConfig(getClass(), null) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox versions = new UIFormSelectBox(FIELD_VERSION , FIELD_VERSION, options) ;
    versions.setOnChange("ChangeVersion");
    versions.setRendered(false) ;
    addUIFormInput(versions) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class)) ;
    addUIFormInput(new UIFormStringInput(FIELD_PERMISSION, FIELD_PERMISSION, null).setEditable(false)
                                                                                  .addValidator(MandatoryValidator.class));
    addUIFormInput(new UIFormInputInfo(FIELD_TABS, FIELD_TABS, null)) ;
    setActionInfo(FIELD_PERMISSION, new String[] {"AddPermission"}) ;
    vservice_ = getApplicationComponent(ManageViewService.class) ;
    Node ecmTemplateHome = vservice_.getTemplateHome(BasePath.ECM_EXPLORER_TEMPLATES,
                                                     SessionProviderFactory.createSessionProvider());
    List<SelectItemOption<String>> temp = new ArrayList<SelectItemOption<String>>() ;
    if(ecmTemplateHome != null) {
      NodeIterator iter = ecmTemplateHome.getNodes() ;
      while(iter.hasNext()) {
        Node tempNode = iter.nextNode() ;
        temp.add(new SelectItemOption<String>(tempNode.getName(),tempNode.getPath())) ;
      }
    }
    addUIFormInput(new UIFormSelectBox(FIELD_TEMPLATE,FIELD_TEMPLATE, temp)) ;
    UIFormCheckBoxInput enableVersion =
      new UIFormCheckBoxInput<Boolean>(FIELD_ENABLEVERSION, FIELD_ENABLEVERSION, null) ;
    enableVersion.setRendered(false) ;
    addUIFormInput(enableVersion) ;
    setActions(new String[]{"Save", "Reset", "Cancel", "AddTabForm"}, null) ;
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context) ;
  }

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    getUIStringInput(UIViewForm.FIELD_PERMISSION).setValue(value.toString()) ;
    UIViewContainer uiContainer = getAncestorOfType(UIViewContainer.class) ;
    uiContainer.removeChildById(UIViewFormTabPane.POPUP_PERMISSION) ;
  }

  public boolean isView() { return isView_ ; }

  public Node getViews() { return views_; }

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

  public void addTab(String tabName, String buttons){
    Tab tab = new Tab() ;
    tab.setTabName(tabName) ;
    tab.setButtons(buttons) ;
    tabMap_.put(tabName, tab) ;
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

  public void refresh(boolean isAddNew) throws Exception {
    getUIFormSelectBox(FIELD_VERSION).setRendered(!isAddNew) ;
    getUIFormSelectBox(FIELD_VERSION).setDisabled(!isAddNew) ;
    getUIStringInput(FIELD_NAME).setEditable(isAddNew).setValue(null) ;
    getUIStringInput(FIELD_PERMISSION).setValue(null) ;
    getUIFormInputInfo(FIELD_TABS).setEditable(isAddNew).setValue(null) ;
    getUIFormSelectBox(FIELD_TEMPLATE).setValue(null) ;
    getUIFormSelectBox(FIELD_TEMPLATE).setDisabled(!isAddNew) ;
    getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setRendered(!isAddNew) ;
    setViewName("");
    if(isAddNew) {
      setActions(new String[]{"Save", "Reset", "Cancel", "AddTabForm"}, null) ;
      setActionInfo(FIELD_PERMISSION, new String[] {"AddPermission"}) ;
      tabMap_.clear() ;
      views_ = null ;
      setActionInfo(FIELD_TABS, null) ;
    }
    selectedVersion_ = null ;
    baseVersion_ = null ;
  }

  public void update(Node viewNode, boolean isView, VersionNode selectedVersion) throws Exception {
    isView_ = isView ;
    if(viewNode != null) {
      views_ = viewNode ;
      if(isVersioned(views_)) baseVersion_ = views_.getBaseVersion();
      tabMap_.clear() ;
      for(NodeIterator iter = views_.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        String buttons = tab.getProperty("exo:buttons").getString() ;
        Tab tabObj = new Tab() ;
        tabObj.setTabName(tab.getName()) ;
        tabObj.setButtons(buttons) ;
        tabMap_.put(tab.getName(), tabObj) ;
      }

      getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setRendered(true) ;
      if (isVersioned(views_)) {
        rootVersionNode = getRootVersion(views_);
        getUIFormSelectBox(FIELD_VERSION).setOptions(getVersionValues(views_)).setRendered(true) ;
        getUIFormSelectBox(FIELD_VERSION).setValue(baseVersion_.getName()) ;
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setChecked(true) ;
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setEnable(true) ;
      } else if (!isVersioned(views_)) {
        getUIFormSelectBox(FIELD_VERSION).setRendered(false) ;
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setChecked(false) ;
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setEnable(true) ;
      }
    }
    if (selectedVersion != null) {
      views_.restore(selectedVersion.getVersion(), false) ;
      views_.checkout() ;
      tabMap_.clear() ;
      for(NodeIterator iter = views_.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        String buttons = tab.getProperty("exo:buttons").getString() ;
        Tab tabObj = new Tab() ;
        tabObj.setTabName(tab.getName()) ;
        tabObj.setButtons(buttons) ;
        tabMap_.put(tab.getName(), tabObj) ;
      }
      selectedVersion_ = selectedVersion;
    }
    if(views_ != null) {
      getUIStringInput(FIELD_NAME).setEditable(false).setValue(views_.getName()) ;
      getUIStringInput(FIELD_PERMISSION).setValue(views_.getProperty("exo:accessPermissions").getString()) ;
      getUIFormSelectBox(FIELD_TEMPLATE).setValue(views_.getProperty("exo:template").getString()) ;
    }
    setInfoField(FIELD_TABS, getTabList()) ;
    String[] actionInfor ;
    if(isView_) {
      actionInfor = new String[] {"EditTab"} ;
      setIsView(true) ;
    } else {
      actionInfor = new String[] {"EditTab", "DeleteTab"} ;
      setIsView(false) ;
    }
    setActionInfo(FIELD_TABS, actionInfor) ;
  }

  @SuppressWarnings("unchecked")
  public void save() throws Exception {
    String viewName = getUIStringInput(FIELD_NAME).getValue().trim();
    ApplicationMessage message ;
    if(viewName == null || viewName.trim().length() == 0){
      throw new MessageException(new ApplicationMessage("UIViewForm.msg.view-name-invalid", null,
                                                        ApplicationMessage.WARNING)) ;
    }
    String[] arrFilterChar = {"&", "$", "@", ",", ":","]", "[", "*", "%", "!", "#", "/", "\\", "\""} ;
    for(String filterChar : arrFilterChar) {
      if(viewName.indexOf(filterChar) > -1) {
        throw new MessageException(new ApplicationMessage("UIViewForm.msg.fileName-invalid", null,
                                                          ApplicationMessage.WARNING)) ;
      }
    }
    boolean isEnableVersioning = getUIFormCheckBoxInput(FIELD_ENABLEVERSION).isChecked() ;
    List<ViewConfig> viewList = vservice_.getAllViews() ;
    UIPopupWindow uiPopup = getAncestorOfType(UIPopupWindow.class) ;
    if(uiPopup.getId().equals(UIViewList.ST_ADD)) {
      for(ViewConfig view : viewList) {
        if(view.getName().equals(viewName) && !isEnableVersioning) {
          message = new ApplicationMessage("UIViewForm.msg.view-exist", null,
                                           ApplicationMessage.WARNING) ;
          throw new MessageException(message) ;
        }
      }
    }
    String permissions = getUIStringInput(FIELD_PERMISSION).getValue() ;
    if(permissions == null || permissions.length() < 1){
      message = new ApplicationMessage("UIViewForm.msg.permission-not-empty", null,
                                       ApplicationMessage.WARNING) ;
      throw new MessageException(message) ;
    }
    if(tabMap_.size() < 1 ){
      message = new ApplicationMessage("UIViewForm.msg.mustbe-add-tab", null,
                                       ApplicationMessage.WARNING) ;
      throw new MessageException(message) ;
    }
    String template = getUIFormSelectBox(FIELD_TEMPLATE).getValue() ;

    List<Tab> tabList = new ArrayList<Tab>(tabMap_.values());
    if(views_ == null || !isEnableVersioning) {
      vservice_.addView(viewName, permissions, template, tabList) ;
      if(views_ != null) {
        for(NodeIterator iter = views_.getNodes(); iter.hasNext(); ) {
          Node tab = iter.nextNode() ;
          if(!tabMap_.containsKey(tab.getName())) tab.remove() ;
        }
        views_.save() ;
      }
    } else {
      if (!isVersioned(views_)) {
        views_.addMixin(Utils.MIX_VERSIONABLE);
        views_.save();
      } else {
        views_.checkout() ;
      }
      for(NodeIterator iter = views_.getNodes(); iter.hasNext(); ) {
        Node tab = iter.nextNode() ;
        if(!tabMap_.containsKey(tab.getName())) tab.remove() ;
      }
      vservice_.addView(viewName, permissions, template, tabList) ;
      try {
        views_.save() ;
        views_.checkin();
      } catch (Exception e) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
        JCRExceptionManager.process(uiApp, e);
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
        context.addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
    }
    UIViewList uiViewList = getAncestorOfType(UIViewContainer.class).getChild(UIViewList.class);
    uiViewList.updateViewListGrid(uiViewList.getUIPageIterator().getCurrentPage());
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
  String permLastest = viewTabPane.getUIStringInput(UIViewForm.FIELD_PERMISSION).getValue();
    tabMap_.remove(tabName) ;
    update(null, false, null) ;
    getUIStringInput(FIELD_PERMISSION).setValue(permLastest);
    UIViewContainer uiViewContainer = getAncestorOfType(UIViewContainer.class) ;
    UIViewList uiViewList = uiViewContainer.getChild(UIViewList.class) ;
    uiViewList.updateViewListGrid(uiViewList.getUIPageIterator().getCurrentPage());
    UIViewForm uiViewForm = viewTabPane.getChild(UIViewForm.class) ;
    viewTabPane.setSelectedTab(uiViewForm.getId()) ;
  }

  public void changeVersion() throws Exception {
    String path =
      views_.getVersionHistory().getVersion(getUIFormSelectBox(FIELD_VERSION).getValue()).getPath();
    VersionNode selectedVesion = rootVersionNode.findVersionNode(path);
    update(null, false, selectedVesion) ;
  }

  public void revertVersion() throws Exception {
    if (selectedVersion_ != null && !selectedVersion_.equals(baseVersion_)) {
      views_.restore(baseVersion_, true);
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource() ;
      UIViewContainer uiContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      String memberShip = uiViewTabPane.getUIStringInput(UIViewForm.FIELD_PERMISSION).getValue() ;
      uiContainer.initPopupPermission(memberShip) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }

  static public class ChangeVersionActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiFormTab = event.getSource();
      UIViewForm uiForm = uiFormTab.getChild(UIViewForm.class);
      uiForm.changeVersion();
      UIViewContainer uiViewContainer = uiFormTab.getAncestorOfType(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
}
