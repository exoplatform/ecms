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

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

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
        @EventConfig(listeners = UIDrivesArea.SelectRepoActionListener.class),
        @EventConfig(listeners = UIDrivesArea.SelectDriveActionListener.class)
    }
)
public class UIDrivesArea extends UIContainer {

  final public static String FIELD_SELECTREPO = "selectRepo" ;
  private String repoName_;
  private boolean firstVisit = true;

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

  public String getGroupLabel(String groupId) {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    try {
      return res.getString("Drives.label." + groupId.replace(".", ""));
    } catch (MissingResourceException ex) {
      return groupId.replace(".", " / ");
    }
  }

  public List<String> getRepositoryList() {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    List<String> repositories = new ArrayList<String>();
    RepositoryEntry re = null;
    try {
      re = repositoryService.getCurrentRepository().getConfiguration();
    } catch (RepositoryException e) {
    }
    repositories.add(re.getName());
    return repositories;
  }

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container
        .getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public String getRestName() {
    PortalContainerConfig portalContainerConfig = this.getApplicationComponent(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(this.getPortalName());
  }

  public String getRepository() throws Exception {
    if(repoName_ == null || repoName_.length() == 0) {
      RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
      repoName_ = repositoryService.getCurrentRepository().getConfiguration().getName();
    }
    return repoName_;
  }

  public void setRepository(String repoName) {repoName_ = repoName; }

  public List<DriveData> mainDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    List<String> userRoles = Utils.getMemberships();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getMainDrives(userId, userRoles);
  }

  public List<DriveData> groupDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    List<String> groups = Utils.getGroups();
    List<String> userRoles = Utils.getMemberships();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getGroupDrives(userId, userRoles, groups);
  }

  public List<DriveData> personalDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class);
    List<String> userRoles = Utils.getMemberships();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    return driveService.getPersonalDrives(userId, userRoles);
  }

  static  public class SelectRepoActionListener extends EventListener<UIDrivesArea> {
    public void execute(Event<UIDrivesArea> event) throws Exception {
      String repoName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDrivesArea uiDrivesArea = event.getSource();
      uiDrivesArea.setRepository(repoName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDrivesArea);
    }
  }

  static  public class SelectDriveActionListener extends EventListener<UIDrivesArea> {
    public void execute(Event<UIDrivesArea> event) throws Exception {
      UIDrivesArea uiDrivesArea = event.getSource();
      String driveName = event.getRequestContext().getRequestParameter(OBJECTID);
      RepositoryService rservice = uiDrivesArea.getApplicationComponent(RepositoryService.class);
      String repoName = uiDrivesArea.getRepository();
      ManageDriveService dservice = uiDrivesArea.getApplicationComponent(ManageDriveService.class);
      DriveData drive = dservice.getDriveByName(driveName);
      String userId = Util.getPortalRequestContext().getRemoteUser();
      UIApplication uiApp = uiDrivesArea.getAncestorOfType(UIApplication.class);
      List<String> viewList = new ArrayList<String>();
      for(String role : Utils.getMemberships()){
        for(String viewName : drive.getViews().split(",")) {
          if (!viewList.contains(viewName.trim())) {
            Node viewNode = uiDrivesArea.getApplicationComponent(ManageViewService.class)
                                        .getViewByName(viewName.trim(),
                                                       SessionProviderFactory.createSystemProvider());
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
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.no-view-found", args));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      String viewListStr = "";
      for(String viewName : viewList) {
        if(viewListStr.length() > 0) viewListStr = viewListStr + "," + viewName;
        else viewListStr = viewName;
      }
      drive.setViews(viewListStr);
      String homePath = drive.getHomePath();
      if(homePath.contains("${userId}")) homePath = homePath.replace("${userId}", userId);
      UIJCRExplorerPortlet uiParent = uiDrivesArea.getAncestorOfType(UIJCRExplorerPortlet.class);
      uiParent.setFlagSelect(true);
      UIJcrExplorerContainer explorerContainer = uiParent.getChild(UIJcrExplorerContainer.class);
      UIJCRExplorer uiJCRExplorer = explorerContainer.getChild(UIJCRExplorer.class);

      Preference pref = uiJCRExplorer.getPreference();
      pref.setShowSideBar(drive.getViewSideBar());
      pref.setShowNonDocumentType(drive.getViewNonDocument());
      pref.setShowPreferenceDocuments(drive.getViewPreferences());
      pref.setAllowCreateFoder(drive.getAllowCreateFolders());
      pref.setShowHiddenNode(drive.getShowHiddenNode());
//      uiJCRExplorer.setPreferences(pref);
      uiJCRExplorer.setDriveData(drive);
      uiJCRExplorer.setIsReferenceNode(false);
      uiJCRExplorer.setPreferencesSaved(true);

      SessionProvider provider = SessionProviderFactory.createSessionProvider();
      ManageableRepository repository = rservice.getCurrentRepository();
      try {
        Session session = provider.getSession(drive.getWorkspace(), repository);
        // check if it exists
        // we assume that the path is a real path
        session.getItem(homePath);
      } catch(AccessDeniedException ace) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.access-denied", args,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(NoSuchWorkspaceException nosuchWS) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.workspace-not-exist", args,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      uiJCRExplorer.clearNodeHistory(homePath);
      uiJCRExplorer.setRepositoryName(repoName);
      uiJCRExplorer.setWorkspaceName(drive.getWorkspace());
      uiJCRExplorer.setRootPath(homePath);
      uiJCRExplorer.setSelectNode(drive.getWorkspace(), homePath);
      uiJCRExplorer.refreshExplorer();
//      uiJCRExplorer.setViewDocument(false);
      String selectedView = viewList.get(0);
      UIControl uiControl = uiJCRExplorer.getChild(UIControl.class).setRendered(true);
      UIActionBar uiActionbar = uiControl.getChild(UIActionBar.class);
      uiActionbar.setTabOptions(selectedView);
      UIAddressBar uiAddressBar = uiControl.getChild(UIAddressBar.class);
      uiAddressBar.setViewList(viewList);
      uiAddressBar.setSelectedViewName(selectedView);
      explorerContainer.setRenderedChild(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
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
      uiJCRExplorer.updateAjax(event);
    }
  }

}
