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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.model.SelectItemOption;

import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
/**
 * Created by The eXo Platform SARL
 */
@ComponentConfig(
  template = "app:/groovy/webui/component/explorer/UIJCRExplorerContainer.gtmpl"
)
public class UIJcrExplorerContainer extends UIContainer {
  private static final Log LOG  = ExoLogger.getLogger(UIJcrExplorerContainer.class.getName());
  public UIJcrExplorerContainer() throws Exception {
    addChild(UIJCRExplorer.class, null, null);
    addChild(UIMultiUpload.class, null, null);
  }

  public String getUserAgent() {
    PortletRequestContext requestContext = PortletRequestContext.getCurrentInstance();
    PortletRequest portletRequest = requestContext.getRequest();
    return portletRequest.getProperty("User-Agent");
  }

  public void initExplorer() throws Exception {
    try {
      UIJCRExplorerPortlet uiFEPortlet = getParent();
      PortletPreferences preference = uiFEPortlet.getPortletPreferences();
      initExplorerPreference(preference);
      String driveName = preference.getValue("driveName", "");
      String nodePath = preference.getValue("nodePath", "");
      RepositoryService rservice = getApplicationComponent(RepositoryService.class);
      String repoName = rservice.getCurrentRepository().getConfiguration().getName();
      ManageDriveService dservice = getApplicationComponent(ManageDriveService.class);
      DriveData drive = dservice.getDriveByName(driveName);
      String userId = Util.getPortalRequestContext().getRemoteUser();
      List<String> userRoles = Utils.getMemberships();
      if(!uiFEPortlet.canUseConfigDrive(driveName)) {
        drive = getAncestorOfType(UIJCRExplorerPortlet.class).getUserDrive();
      }
      UIApplication uiApp = getApplicationComponent(UIApplication.class);
      List<String> viewList = new ArrayList<String>();
      for (String role : userRoles) {
        for (String viewName : drive.getViews().split(",")) {
          if (!viewList.contains(viewName.trim())) {
            Node viewNode = getApplicationComponent(ManageViewService.class)
                .getViewByName(viewName.trim(), WCMCoreUtils.getSystemSessionProvider());
            String permiss = viewNode.getProperty("exo:accessPermissions").getString();
            if (permiss.contains("${userId}")) permiss = permiss.replace("${userId}", userId);
            String[] viewPermissions = permiss.split(",");
            if (permiss.equals("*")) viewList.add(viewName.trim());
            if (drive.hasPermission(viewPermissions, role)) viewList.add(viewName.trim());
          }
        }
      }
      if (viewList.isEmpty()) {
        return;
      }
      StringBuffer viewListStr = new StringBuffer();
      List<SelectItemOption<String>> viewOptions = new ArrayList<SelectItemOption<String>>();
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String viewLabel = null;
      for (String viewName : viewList) {
        try {
          viewLabel = res.getString("Views.label." + viewName) ;
        } catch (MissingResourceException e) {
          viewLabel = viewName;
        }
        viewOptions.add(new SelectItemOption<String>(viewLabel, viewName));
        if(viewListStr.length() > 0) viewListStr.append(",").append(viewName);
        else viewListStr.append(viewName);
      }
      drive.setViews(viewListStr.toString());
      StringBuffer homePathBuf = new StringBuffer();
      homePathBuf.append(drive.getHomePath());
      if (homePathBuf.indexOf("${userId}") >= 0) 
        homePathBuf = new StringBuffer(org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePathBuf.toString(),
                                                                                                    userId));
      //Check to make sure new behavior will be also correct with the legacy data
      //By default all the group drive will be point to Documents folder. 
      //Therefore in the case spaces drives we no need to specify the nodepath or consider it is equals "/"
      if(drive.getHomePath().startsWith("/Groups/spaces/") &&
              (StringUtils.isBlank(nodePath) || nodePath.equals("Documents") || nodePath.equals("/Documents"))) nodePath = "/";
      if (nodePath != null && nodePath.length() > 0 && !nodePath.equals("/"))
        homePathBuf.append("/").append(nodePath);
      String homePath = homePathBuf.toString().replaceAll("//", "/");
      UIJCRExplorer uiJCRExplorer = getChild(UIJCRExplorer.class);

      uiJCRExplorer.setDriveData(drive);
      uiJCRExplorer.setIsReferenceNode(false);

      Session session = WCMCoreUtils.getUserSessionProvider().getSession(drive.getWorkspace(), rservice.getCurrentRepository());
      try {
        // we assume that the path is a real path
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
      
      uiJCRExplorer.setRepositoryName(repoName);
      uiJCRExplorer.setWorkspaceName(drive.getWorkspace());
      uiJCRExplorer.setRootPath(homePath);
      uiJCRExplorer.setSelectNode(drive.getWorkspace(), homePath);
      Preference pref = uiJCRExplorer.getPreference();
      pref.setShowSideBar(drive.getViewSideBar());
      pref.setShowNonDocumentType(drive.getViewNonDocument());
      pref.setShowPreferenceDocuments(drive.getViewPreferences());
      pref.setAllowCreateFoder(drive.getAllowCreateFolders());
      pref.setShowHiddenNode(drive.getShowHiddenNode());
      UIControl uiControl = uiJCRExplorer.getChild(UIControl.class);
      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);

      UIAddressBar uiAddressBar = uiControl.getChild(UIAddressBar.class);
      uiAddressBar.setViewList(viewList);
      uiAddressBar.setSelectedViewName(viewList.get(0));
      uiAddressBar.setRendered(uiFEPortlet.isShowTopBar());
      UIActionBar uiActionbar = uiWorkingArea.getChild(UIActionBar.class);
      boolean isShowActionBar = uiFEPortlet.isShowActionBar();
      uiActionbar.setTabOptions(viewList.get(0));
      uiActionbar.setRendered(isShowActionBar);
      uiWorkingArea.setRenderedChildrenOfTypes(new Class[] { UIActionBar.class,
          UIDocumentWorkspace.class });
      uiJCRExplorer.refreshExplorer();
      UIRightClickPopupMenu uiRightClickPopupMenu = uiWorkingArea.findFirstComponentOfType(UIRightClickPopupMenu.class);
      if(uiRightClickPopupMenu!=null && !uiRightClickPopupMenu.isRendered())
        uiRightClickPopupMenu.setRendered(true);
      UISideBar uiSideBar = uiWorkingArea.findFirstComponentOfType(UISideBar.class);
      uiSideBar.setRendered(true);
      uiSideBar.initialize();
      if (uiSideBar.isRendered()) {
       uiSideBar.updateSideBarView();
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  private void initExplorerPreference(PortletPreferences portletPref) {
    UIJCRExplorer uiExplorer = getChild(UIJCRExplorer.class);
    if (uiExplorer != null) {
      Preference pref = uiExplorer.getPreference();
      if (pref == null) {
        pref = new Preference();
        pref.setNodesPerPage(Integer.parseInt(portletPref.getValue(Preference.NODES_PER_PAGE, "20")));
        uiExplorer.setPreferences(pref);
      }
    }
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception
  {
    super.processRender(context);
  }

}
