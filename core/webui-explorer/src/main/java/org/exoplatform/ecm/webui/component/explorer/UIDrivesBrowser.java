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
package org.exoplatform.ecm.webui.component.explorer;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig (
      template =  "app:/groovy/webui/component/explorer/UIDrivesBrowser.gtmpl",
      events = {
        @EventConfig(listeners = UIDrivesBrowser.SelectRepoActionListener.class),
        @EventConfig(listeners = UIDrivesBrowser.SelectDriveActionListener.class)
      }
)
@Deprecated
public class UIDrivesBrowser extends UIContainer {
  final public static String FIELD_SELECTREPO = "selectRepo" ;
  private String repoName_;
  private RepositoryService rService;
  public UIDrivesBrowser() throws Exception {
    rService = getApplicationComponent(RepositoryService.class);
    repoName_ = rService.getCurrentRepository().getConfiguration().getName();
  }

  public List<String> getRepositoryList() {
    List<String> repositories = new ArrayList<String>();
    RepositoryEntry re = null;
    try {
      re = rService.getCurrentRepository().getConfiguration();
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

  public String getRepository() {return repoName_;}

  public void setRepository(String repoName) {repoName_ = repoName; }

  public List<DriveData> generalDrives() throws Exception {
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

  static  public class SelectRepoActionListener extends EventListener<UIDrivesBrowser> {
    public void execute(Event<UIDrivesBrowser> event) throws Exception {
      String repoName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDrivesBrowser uiDrivesBrowser = event.getSource();
      uiDrivesBrowser.setRepository(repoName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDrivesBrowser);
    }
  }

  static  public class SelectDriveActionListener extends EventListener<UIDrivesBrowser> {
    public void execute(Event<UIDrivesBrowser> event) throws Exception {
      UIDrivesBrowser uiDrive = event.getSource();
      String driveName = event.getRequestContext().getRequestParameter(OBJECTID);
      RepositoryService rservice = uiDrive.getApplicationComponent(RepositoryService.class);
      ManageDriveService dservice = uiDrive.getApplicationComponent(ManageDriveService.class);
      DriveData drive = dservice.getDriveByName(driveName);
      String userId = Util.getPortalRequestContext().getRemoteUser();
      UIApplication uiApp = uiDrive.getAncestorOfType(UIApplication.class);
      List<String> viewList = new ArrayList<String>();
      for(String role : Utils.getMemberships()){
        for(String viewName : drive.getViews().split(",")) {
          if(!viewList.contains(viewName.trim())) {
            Node viewNode = uiDrive.getApplicationComponent(ManageViewService.class)
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
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.no-view-found", args));
        
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
      if(homePath.contains("${userId}")) 
        homePath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, userId);
      UIJCRExplorerPortlet uiParent = uiDrive.getAncestorOfType(UIJCRExplorerPortlet.class);
      uiParent.setFlagSelect(true);
      UIJcrExplorerContainer explorerContainer = uiParent.getChild(UIJcrExplorerContainer.class);
      UIJCRExplorer uiJCRExplorer = explorerContainer.getChild(UIJCRExplorer.class);

      Preference pref = uiJCRExplorer.getPreference();
      pref.setShowSideBar(drive.getViewSideBar());
      pref.setShowNonDocumentType(drive.getViewNonDocument());
      pref.setShowPreferenceDocuments(drive.getViewPreferences());
      pref.setAllowCreateFoder(drive.getAllowCreateFolders());
      pref.setShowHiddenNode(drive.getShowHiddenNode());
      uiJCRExplorer.setDriveData(drive);
      uiJCRExplorer.setIsReferenceNode(false);

      SessionProvider provider = WCMCoreUtils.getUserSessionProvider();
      ManageableRepository repository = rservice.getCurrentRepository();
      try {
        Session session = provider.getSession(drive.getWorkspace(),repository);
        // check if it exists
        // we assume that the path is a real path
        session.getItem(homePath);
      } catch(AccessDeniedException ace) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.access-denied", args,
            ApplicationMessage.WARNING));
        
        return;
      } catch(NoSuchWorkspaceException nosuchWS) {
        Object[] args = { driveName };
        uiApp.addMessage(new ApplicationMessage("UIDrivesBrowser.msg.workspace-not-exist", args,
            ApplicationMessage.WARNING));
        
        return;
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      uiJCRExplorer.clearNodeHistory(homePath);
      uiJCRExplorer.setRepositoryName(uiDrive.repoName_);
      uiJCRExplorer.setWorkspaceName(drive.getWorkspace());
      uiJCRExplorer.setRootPath(homePath);
      uiJCRExplorer.setSelectNode(drive.getWorkspace(), homePath);
      uiJCRExplorer.refreshExplorer();
      String selectedView = viewList.get(0);
      UIControl uiControl = uiJCRExplorer.getChild(UIControl.class);
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionbar = uiWorkingArea.getChild(UIActionBar.class);
      uiActionbar.setTabOptions(selectedView);
      UIAddressBar uiAddressBar = uiControl.getChild(UIAddressBar.class);
      uiAddressBar.setViewList(viewList);
      uiAddressBar.setSelectedViewName(selectedView);
      explorerContainer.setRenderedChild(UIJCRExplorer.class);
      uiParent.setRenderedChild(UIJcrExplorerContainer.class);
    }
  }
}
