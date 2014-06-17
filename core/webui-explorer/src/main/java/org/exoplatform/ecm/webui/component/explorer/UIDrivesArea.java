/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UIAllItems;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 21, 2009
 * 9:58:02 AM
 */
@ComponentConfig (
    template =  "app:/groovy/webui/component/explorer/UIDrivesArea.gtmpl",
    events = {
        @EventConfig(listeners = UIDrivesArea.SelectDriveActionListener.class)
    }
)
public class UIDrivesArea extends UIContainer {

  final public static String FIELD_SELECTREPO = "selectRepo" ;
  private boolean firstVisit = true;
  private List<String> userRoles_ = null;

  public UIDrivesArea() throws Exception {
  }

  public void setFirstVisit(boolean firstVisit) {
    this.firstVisit = firstVisit;
  }

  public boolean isFirstVisit() {
    return firstVisit;
  }

  public String getLabel(String id)  {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    try {
      return res.getString("Drives.label." + id.replace(" ", ""));
    } catch (MissingResourceException ex) {
      return id;
    }
  }

  public String getGroupLabel(DriveData driveData) throws Exception{
    try {
      RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
      Node groupNode = (Node)WCMCoreUtils.getSystemSessionProvider().getSession(
                                    repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName(),
                                    repoService.getCurrentRepository()).getItem(
                                            groupPath + driveData.getName().replace(".", "/"));
      return groupNode.getProperty(NodetypeConstant.EXO_LABEL).getString();
    } catch(Exception e) {
      return driveData.getName().replace(".", " / ");
    }
  }

  public String getGroupLabel(String groupId, boolean isFull) {
    String ret = groupId.replace(".", " / ");
    if (!isFull) {
      if (ret.startsWith(" / spaces")) {
        return ret.substring(ret.lastIndexOf("/") + 1).trim();
      }
      int count = 0;
      int slashPosition = -1;
      for (int i = 0; i < ret.length(); i++) {
        if ('/' == ret.charAt(i)) {
          if (++count == 4) {
            slashPosition = i;
            break;
          }
        }
      }
      if (slashPosition > 0) {
        ret = ret.substring(0, slashPosition) + "...";
      } else if (ret.length() > 70) {
        ret = ret.substring(0, 70) + "...";
      }
    }
    return ret;
  }

  public String getPortalName() {
    PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public String getRestName() {
    PortalContainerConfig portalContainerConfig = this.getApplicationComponent(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(this.getPortalName());
  }

  private List<String> getUserRoles(boolean newRoleUpdated) throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    if (userRoles_ == null || (userRoles_ != null && newRoleUpdated)) {
      userRoles_ = Utils.getMemberships();
      if(newRoleUpdated) driveService.setNewRoleUpdated(false);
    }
    return userRoles_;
  }

  public List<DriveData> mainDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    List<String> userRoles = getUserRoles(false);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getMainDrives(userId, userRoles);
  }

  public List<DriveData> groupDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    List<String> userRoles = getUserRoles(driveService.newRoleUpdated());
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getGroupDrives(userId, userRoles);
  }

  public List<DriveData> personalDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getPersonalDrives(userId);
  }

  static  public class SelectDriveActionListener extends EventListener<UIDrivesArea> {
    public void execute(Event<UIDrivesArea> event) throws Exception {
      UIDrivesArea uiDrivesArea = event.getSource();
      String driveName = event.getRequestContext().getRequestParameter(OBJECTID);
      RepositoryService rservice = uiDrivesArea.getApplicationComponent(RepositoryService.class);
      ManageDriveService dservice = uiDrivesArea.getApplicationComponent(ManageDriveService.class);
      DriveData drive = dservice.getDriveByName(driveName);
      String userId = Util.getPortalRequestContext().getRemoteUser();
      UIApplication uiApp = uiDrivesArea.getAncestorOfType(UIApplication.class);
      List<String> viewList = new ArrayList<String>();
      for(String role : Utils.getMemberships()){
        for(String viewName : drive.getViews().split(",")) {
          if (!viewList.contains(viewName.trim())) {
            Node viewNode = uiDrivesArea.getApplicationComponent(ManageViewService.class)
                                        .getViewByName(viewName.trim(), WCMCoreUtils.getSystemSessionProvider());
            String permiss = viewNode.getProperty("exo:accessPermissions").getString();
            if(permiss.contains("${userId}")) permiss = permiss.replace("${userId}", userId);
            String[] viewPermissions = permiss.split(",");
            if(permiss.equals("*")) viewList.add(viewName.trim());
            if(drive.hasPermission(viewPermissions, role)) viewList.add(viewName.trim());
          }
        }
      }
      if(viewList.isEmpty()) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesArea.msg.no-view-found", args));

        return;
      }
      StringBuffer viewListStr = new StringBuffer();
      for (String viewName : viewList) {
        if (viewListStr.length() > 0)
          viewListStr.append(",").append(viewName);
        else
          viewListStr.append(viewName);
      }
      drive.setViews(viewListStr.toString());
      String homePath = drive.getHomePath();
      if(homePath.contains("${userId}")) {
        homePath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, userId);
      }
      UIJCRExplorerPortlet uiParent = uiDrivesArea.getAncestorOfType(UIJCRExplorerPortlet.class);
      uiParent.setFlagSelect(true);
      UIJcrExplorerContainer explorerContainer = uiParent.getChild(UIJcrExplorerContainer.class);
      UIJCRExplorer uiJCRExplorer = explorerContainer.getChild(UIJCRExplorer.class);
      UITreeExplorer uiTreeExplorer = uiJCRExplorer.findFirstComponentOfType(UITreeExplorer.class);      


      Preference pref = uiJCRExplorer.getPreference();
      // check if Preferences has View-Side-Bar property to be true or not. if true, set TRUE for setViewSideBar() 
      if (!pref.isShowSideBar())
        pref.setShowSideBar(drive.getViewSideBar());
      pref.setShowPreferenceDocuments(drive.getViewPreferences());
      pref.setAllowCreateFoder(drive.getAllowCreateFolders());
      HttpServletRequest request = Util.getPortalRequestContext().getRequest();
      Cookie[] cookies = request.getCookies();
      Cookie getCookieForUser = UIJCRExplorer.getCookieByCookieName(Preference.PREFERENCE_SHOW_HIDDEN_NODE, cookies);
      if (uiJCRExplorer.findFirstComponentOfType(UIAllItems.class) == null || getCookieForUser == null) {
        pref.setShowHiddenNode(drive.getShowHiddenNode());
      }
      if (getCookieForUser == null) {
        pref.setShowNonDocumentType(drive.getViewNonDocument());
      }
      uiJCRExplorer.setDriveData(drive);
      uiJCRExplorer.setIsReferenceNode(false);
      uiJCRExplorer.setPreferencesSaved(true);

      ManageableRepository repository = rservice.getCurrentRepository();
      try {
        Session session = WCMCoreUtils.getUserSessionProvider().getSession(drive.getWorkspace(), repository);
        /**
         *  check if it exists. we assume that the path is a real path
         */
        session.getItem(homePath);
      } catch(AccessDeniedException ace) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesArea.msg.access-denied", args,
            ApplicationMessage.WARNING));

        return;
      } catch(NoSuchWorkspaceException nosuchWS) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesArea.msg.workspace-not-exist", args,
            ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      uiJCRExplorer.clearNodeHistory(homePath);
      uiJCRExplorer.setRepositoryName(repository.getConfiguration().getName());
      uiJCRExplorer.setWorkspaceName(drive.getWorkspace());
      uiJCRExplorer.setRootPath(homePath);
      uiJCRExplorer.setSelectNode(drive.getWorkspace(), homePath);
      String selectedView = viewList.get(0);
      UIControl uiControl = uiJCRExplorer.getChild(UIControl.class).setRendered(true);
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionbar = uiWorkingArea.getChild(UIActionBar.class);
      uiActionbar.setTabOptions(selectedView);
      UIAddressBar uiAddressBar = uiControl.getChild(UIAddressBar.class);
      uiAddressBar.setViewList(viewList);
      uiAddressBar.setSelectedViewName(selectedView);
      uiWorkingArea.getChild(UISideBar.class).initialize();
      for(UIComponent uiComp : uiWorkingArea.getChildren()) {
        if(uiComp instanceof UIDrivesArea) uiComp.setRendered(false);
        else uiComp.setRendered(true);
      }
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      UIDocumentFormController controller = uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
      if (controller != null) {
        controller.getChild(UIDocumentForm.class).releaseLock();
      }
      uiParent.setRenderedChild(UIJcrExplorerContainer.class);
      event.getRequestContext().getJavascriptManager().
      require("SHARED/multiUpload", "multiUpload").
      addScripts("multiUpload.setLocation('" + 
                 uiJCRExplorer.getWorkspaceName()  + "','" + 
                 uiJCRExplorer.getDriveData().getName()  + "','" +
                 uiTreeExplorer.getLabel()  + "','" +
                 uiJCRExplorer.getCurrentPath() + "','" +
                 org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(uiJCRExplorer.getDriveData().getHomePath(),
                 ConversationState.getCurrent().getIdentity().getUserId())+ "');");
      uiJCRExplorer.findFirstComponentOfType(UIDocumentInfo.class).getExpandedFolders().clear();
      uiJCRExplorer.updateAjax(event);
    }
  }

}
