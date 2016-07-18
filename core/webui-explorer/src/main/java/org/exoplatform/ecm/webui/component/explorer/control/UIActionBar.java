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
package org.exoplatform.ecm.webui.component.explorer.control;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.jcr.SearchValidator;
import org.exoplatform.ecm.webui.component.explorer.*;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.search.*;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.portlet.PortletPreferences;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Aug 2, 2006
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/control/UIActionBar.gtmpl",
    events = {
      @EventConfig(listeners = UIActionBar.SearchActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIActionBar.SimpleSearchActionListener.class),
      @EventConfig(listeners = UIActionBar.AdvanceSearchActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIActionBar.SavedQueriesActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIActionBar.ChangeTabActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIActionBar.PreferencesActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIActionBar.BackToActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIActionBar.ShowDrivesActionListener.class, phase=Phase.DECODE)
    }
)

public class UIActionBar extends UIForm {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIActionBar.class.getName());

  private OrganizationService organizationService;

  private NodeLocation view_ ;
  private String templateName_ ;
  //private List<SelectItemOption<String>> tabOptions = new ArrayList<SelectItemOption<String>>() ;
  private List<String> tabList_ = new ArrayList<String>();
  private List<String[]> tabs_ = new ArrayList<String[]>();
  private Map<String, String[]> actionInTabs_ = new HashMap<String, String[]>();

  private String selectedTabName_;

  final static private String   FIELD_SIMPLE_SEARCH  = "simpleSearch";

  final static private String   FIELD_ADVANCE_SEARCH = "advanceSearch";

  final static private String   FIELD_SEARCH_TYPE    = "searchType";

  final static private String   FIELD_SQL            = "SQL";

  final static private String   FIELD_XPATH          = "xPath";

  final static private String   ROOT_SQL_QUERY       = "select * from nt:base where contains(*, '$1') "
                                                         + "order by exo:dateCreated DESC, jcr:primaryType DESC";

  final static private String   SQL_QUERY            = "select * from nt:base where jcr:path like '$0/%' and contains(*, '$1') "
                                                         + "order by jcr:path DESC, jcr:primaryType DESC";

  private String backLink;

  public UIActionBar() throws Exception {
    organizationService = CommonsUtils.getService(OrganizationService.class);

    addChild(new UIFormStringInput(FIELD_SIMPLE_SEARCH, FIELD_SIMPLE_SEARCH, null).addValidator(SearchValidator.class));
    List<SelectItemOption<String>> typeOptions = new ArrayList<SelectItemOption<String>>();
    typeOptions.add(new SelectItemOption<String>(FIELD_SQL, Query.SQL));
    typeOptions.add(new SelectItemOption<String>(FIELD_XPATH, Query.XPATH));
    addChild(new UIFormSelectBox(FIELD_SEARCH_TYPE, FIELD_SEARCH_TYPE, typeOptions));
    addChild(new UIFormStringInput(FIELD_ADVANCE_SEARCH, FIELD_ADVANCE_SEARCH, null));
  }

  public void setTabOptions(String viewName) throws Exception {
    tabList_ = new ArrayList<String>();
    Node viewNode = getApplicationComponent(ManageViewService.class).getViewByName(viewName,
        WCMCoreUtils.getSystemSessionProvider());
    view_ = NodeLocation.getNodeLocationByNode(viewNode);
    NodeIterator tabs = viewNode.getNodes();
    while (tabs.hasNext()) {
      Node tab = tabs.nextNode();
      if(!tabList_.contains(tab.getName())) tabList_.add(tab.getName());
      setListButton(tab.getName());
    }
    setSelectedTab(tabList_.get(0));
    String template = viewNode.getProperty("exo:template").getString();
    templateName_ = template.substring(template.lastIndexOf("/") + 1);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    uiExplorer.setRenderTemplate(template);
  }
  public boolean hasBackButton() {
    String newLink = getAncestorOfType(UIJCRExplorerPortlet.class).getBacktoValue();
    if (newLink != null && newLink.length()>0)
      backLink = newLink;
    return backLink != null;
  }
  public String getBackLink() {
    return getAncestorOfType(UIJCRExplorerPortlet.class).getBacktoValue();
  }
  public String getTemplateName() { return templateName_;  }

  private void setListButton(String tabName) throws PathNotFoundException, RepositoryException {
    
    Node tabNode = NodeLocation.getNodeByLocation(view_).getNode(tabName);
    if(tabNode.hasProperty("exo:buttons")) {
      String buttons = tabNode.getProperty("exo:buttons").getString();
      String[] buttonsInTab = StringUtils.split(buttons, ";");
      Set<String> bt = new HashSet<String>();
      //get all buttons in tab
      for (String b : buttonsInTab) {
        b = b.trim();
        b = b.substring(0, 1).toUpperCase() + b.substring(1);
        bt.add(b);
      }
      //sort the buttons by UIExtension sorting order
      List<String> sortedButtons = new ArrayList<String>();
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(ManageViewService.EXTENSION_TYPE);
      for(UIExtension e : extensions) {
        if (bt.contains(e.getName())) {
          sortedButtons.add(e.getName().trim());
        }
      }
      buttonsInTab = sortedButtons.toArray(new String[]{});
      actionInTabs_.put(tabName, buttonsInTab);
      tabs_.add(buttonsInTab);
    }
  }

  public String[] getActionInTab(String tabName) { return actionInTabs_.get(tabName); }

  public void setSelectedTab(String tabName) {
    selectedTabName_ = tabName;
  }

  public boolean isDirectlyDrive() {
    PortletPreferences portletPref =
      getAncestorOfType(UIJCRExplorerPortlet.class).getPortletPreferences();
    String usecase =  portletPref.getValue("usecase", "").trim();
    if ("selection".equals(usecase)) {
      return false;
    }
    return true;
  }

  public String getSelectedTab() throws Exception {
    if(selectedTabName_ == null || selectedTabName_.length() == 0) {
      setTabOptions(tabList_.get(0));
      return tabList_.get(0);
    }
    return selectedTabName_;
  }

  public List<String> getTabList() { return tabList_; }

  public List<Query> getSavedQueries() throws Exception {
    String userName = Util.getPortalRequestContext().getRemoteUser();
    return getApplicationComponent(QueryService.class).getQueries(userName, WCMCoreUtils.getSystemSessionProvider());
  }

  public synchronized UIComponent getUIAction(String action)  {
    try {
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      Map<String, Object> context = new HashMap<String, Object>();
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      context.put(UIJCRExplorer.class.getName(), uiExplorer);
      context.put(Node.class.getName(), currentNode);
      return manager.addUIExtension(ManageViewService.EXTENSION_TYPE, action, context, this);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occurs while checking the action", e);
      }
    }
    return null;
  }

  public boolean isActionAvailable(String tabName) {
    List<UIComponent> listActions = new ArrayList<UIComponent>();
    for(String action : getActionInTab(tabName)) {
      UIComponent uicomp = getUIAction(action);
      if(uicomp != null) listActions.add(uicomp);
    }
    if(listActions.size() > 0) return true;
    return false;
  }

  static public class SearchActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(UIECMSearch.class, 700);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }

  static public class SimpleSearchActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIActionBar uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      String text = uiForm.getUIStringInput(FIELD_SIMPLE_SEARCH).getValue();
      Node currentNode = uiExplorer.getCurrentNode();
      String queryStatement = null;
      if("/".equals(currentNode.getPath())) {
        queryStatement = ROOT_SQL_QUERY;
      }else {
        queryStatement = StringUtils.replace(SQL_QUERY,"$0",currentNode.getPath());
      }
      queryStatement = StringUtils.replace(queryStatement,"$1", text.replaceAll("'", "''"));
      uiExplorer.removeChildById("ViewSearch");
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
        uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class) ;
      }
      UISearchResult uiSearchResult =
        uiDocumentWorkspace.getChildById(UIDocumentWorkspace.SIMPLE_SEARCH_RESULT);

      long startTime = System.currentTimeMillis();
      uiSearchResult.setQuery(queryStatement, currentNode.getSession().getWorkspace().getName(), Query.SQL,
                              IdentityConstants.SYSTEM.equals(WCMCoreUtils.getRemoteUser()), null);
      uiSearchResult.updateGrid();
      long time = System.currentTimeMillis() - startTime;

      uiSearchResult.setSearchTime(time);
      uiDocumentWorkspace.setRenderedChild(UISearchResult.class);
    }
  }

  static public class AdvanceSearchActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIECMSearch uiECMSearch = event.getSource().createUIComponent(UIECMSearch.class, null, null);
      UIContentNameSearch contentNameSearch = uiECMSearch.findFirstComponentOfType(UIContentNameSearch.class);
      String currentNodePath = uiJCRExplorer.getCurrentNode().getPath();
      contentNameSearch.setLocation(currentNodePath);
      UISimpleSearch uiSimpleSearch = uiECMSearch.findFirstComponentOfType(UISimpleSearch.class);
      uiSimpleSearch.getUIFormInputInfo(UISimpleSearch.NODE_PATH).setValue(currentNodePath);
      UIPopupContainer.activate(uiECMSearch, 700, 500);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }
  static public class BackToActionListener extends EventListener<UIActionBar> {
      public void execute(Event<UIActionBar> event) throws Exception {
        UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
          UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
          UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
          UIDocumentFormController uiDocumentFormController =  uiDocumentWorkspace.getChild(UIDocumentFormController.class);
          String backLink = event.getSource().getBackLink();
          if (uiDocumentFormController != null) {
            UIDocumentForm uiDocument = uiDocumentFormController.getChild(UIDocumentForm.class);
          if (uiDocument!=null) {
            uiDocument.releaseLock();
          }
            uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
          } else
          uiExplorer.cancelAction();
          RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
          requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + backLink + "');");
      }
    }
  static public class SavedQueriesActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UISavedQuery uiSavedQuery = event.getSource().createUIComponent(UISavedQuery.class, null, null);
      uiSavedQuery.setIsQuickSearch(true);
      uiSavedQuery.updateGrid(1);
      UIPopupContainer.activate(uiSavedQuery, 700, 400);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }

  static public class ChangeTabActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIActionBar uiActionBar = event.getSource();
      String selectedTabName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiActionBar.setSelectedTab(selectedTabName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar.getAncestorOfType(UIJCRExplorer.class));
    }
  }

  static public class PreferencesActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIActionBar uiActionBar = event.getSource();
      UIJCRExplorer uiJCRExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer popupAction = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIPreferencesForm uiPrefForm = popupAction.activate(UIPreferencesForm.class,600) ;
      uiPrefForm.update(uiJCRExplorer.getPreference()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }

  static public class ShowDrivesActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDrivesArea uiDriveArea = uiWorkingArea.getChild(UIDrivesArea.class);
      if (uiDriveArea.isRendered()) {
        uiDriveArea.setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      } else {
        uiDriveArea.setRendered(true);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea) ;
    }
  }

  public String getDriveLabel() {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    DriveData drive = getAncestorOfType(UIJCRExplorer.class).getDriveData();
    String driveName = drive.getName();

    String driveLabel = "";

    try {
      if(ManageDriveServiceImpl.GROUPS_DRIVE_NAME.equals(driveName) || driveName.startsWith(".")) {
        // Groups drive
        RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
        NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
        String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
        Node groupNode = (Node)WCMCoreUtils.getSystemSessionProvider().getSession(
                repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName(),
                repoService.getCurrentRepository()).getItem(
                groupPath + drive.getName().replace(".", "/"));
        driveLabel = groupNode.getProperty(NodetypeConstant.EXO_LABEL).getString();
      } else if(ManageDriveServiceImpl.USER_DRIVE_NAME.equals(driveName)) {
        // User Documents drive
        String userDisplayName = "";
        String driveLabelKey = "Drives.label.UserDocuments";
        String userIdPath = drive.getParameters().get(ManageDriveServiceImpl.DRIVE_PARAMATER_USER_ID);
        String userId = userIdPath.substring(userIdPath.lastIndexOf("/") + 1);
        if(StringUtils.isNotEmpty(userId)) {
          userDisplayName = userId;
          User user = organizationService.getUserHandler().findUserByName(userId);
          if(user != null) {
            userDisplayName = user.getDisplayName();
          }
        }
        try {
          driveLabel = res.getString(driveLabelKey).replace("{0}", userDisplayName);
        } catch(MissingResourceException mre) {
          LOG.error("Cannot get resource string " + driveLabel + " : " + mre.getMessage(), mre);
          driveLabel = userDisplayName;
        }
      } else {
        // Others drives
        String driveLabelKey = "Drives.label." + driveName.replace(".", "").replace(" ", "");
        try {
          driveLabel = res.getString(driveLabelKey);
        } catch (MissingResourceException ex) {
          LOG.error("Cannot get resource string for " + driveLabelKey + " : " + ex.getMessage(), ex);
          driveLabel = driveLabelKey;
        }
      }
    } catch(Exception e) {
      driveLabel = driveName.replace(".", " / ");
    }

    return driveLabel;
  }
}
