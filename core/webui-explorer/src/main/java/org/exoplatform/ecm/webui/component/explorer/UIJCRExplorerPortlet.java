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
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.control.action.AddDocumentActionComponent;
import org.exoplatform.ecm.webui.component.explorer.control.action.EditDocumentActionComponent;
import org.exoplatform.ecm.webui.component.explorer.control.action.EditPropertyActionComponent;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.component.explorer.versions.UIVersionInfo;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;

import javax.jcr.*;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class
)
public class UIJCRExplorerPortlet extends UIPortletApplication {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIJCRExplorerPortlet.class.getName());

  final static public String ISDIRECTLY_DRIVE   = "isDirectlyDrive";

  final static public String DRIVE_NAME         = "driveName";

  final static public String USECASE            = "usecase";

  final static public String JAILED             = "jailed";

  final static public String SELECTION          = "selection";

  final static public String PERSONAL           = "personal";

  final static public String PARAMETERIZE       = "parameterize";

  final static public String PARAMETERIZE_PATH  = "nodePath";

  final static public String SHOW_TOP_BAR       = "showTopBar";

  final static public String SHOW_ACTION_BAR    = "showActionBar";

  final static public String SHOW_SIDE_BAR      = "showSideBar";

  final static public String SHOW_FILTER_BAR    = "showFilterBar";

  final static private String DOC_NOT_FOUND    = "doc-not-found";

  private NodeFinder nodeFinder;

  private String backTo ="";

  private boolean flagSelect = false;

  private Pattern driveParameteriedPathPattern = Pattern.compile(".*\\$\\{(.*)\\}.*");

  public UIJCRExplorerPortlet() throws Exception {
    if (Util.getPortalRequestContext().getRemoteUser() != null) {
      UIJcrExplorerContainer explorerContainer = addChild(UIJcrExplorerContainer.class, null, null);
      explorerContainer.initExplorer();
      addChild(UIJcrExplorerEditContainer.class, null, null).setRendered(false);
    }
    nodeFinder = getApplicationComponent(NodeFinder.class);
  }

  public boolean isFlagSelect() { return flagSelect; }

  public void setFlagSelect(boolean flagSelect) { this.flagSelect = flagSelect; }

  public boolean isShowTopBar() {
    PortletPreferences portletpref = getPortletPreferences();
    return Boolean.valueOf(portletpref.getValue(UIJCRExplorerPortlet.SHOW_TOP_BAR, "false"));
  }

  public boolean isShowActionBar() {
    PortletPreferences portletpref = getPortletPreferences();
    return Boolean.valueOf(portletpref.getValue(UIJCRExplorerPortlet.SHOW_ACTION_BAR, "false")) &&
           (!this.findFirstComponentOfType(UIJCRExplorer.class).isAddingDocument() ||
             this.findFirstComponentOfType(UIWorkingArea.class).getChild(UIActionBar.class).hasBackButton());
  }

  public boolean isShowSideBar() {
    PortletPreferences portletpref = getPortletPreferences();
    return Boolean.valueOf(portletpref.getValue(UIJCRExplorerPortlet.SHOW_SIDE_BAR, "false"));
  }

  public boolean isShowFilterBar() {
    PortletPreferences portletpref = getPortletPreferences();
    return Boolean.valueOf(portletpref.getValue(UIJCRExplorerPortlet.SHOW_FILTER_BAR, "false"));
  }
  public String getBacktoValue() {
    return backTo;
  }
  public void  processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    if (Util.getPortalRequestContext().getRemoteUser() == null) {
      ((PortletRequestContext)context).getWriter().write(
        String.format("<p style='text-align:center'>%s</p>",
        context.getApplicationResourceBundle().getString("UIJCRExplorerPortlet.msg.anonymous-access-denied")));
      return;
    }
    
    UIJcrExplorerContainer explorerContainer = getChild(UIJcrExplorerContainer.class);
    UIJcrExplorerEditContainer editContainer = getChild(UIJcrExplorerEditContainer.class);
    PortletRequestContext portletReqContext = (PortletRequestContext) context ;
    Map<String, String> map = getElementByContext(context);
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    String backToValue = Util.getPortalRequestContext().getRequestParameter(org.exoplatform.ecm.webui.utils.Utils.URL_BACKTO);
    
    if (!portletReqContext.useAjax()) {
        backTo = backToValue;
    }
    HashMap<String, String> changeDrive = (HashMap<String, String>)pcontext.getAttribute("jcrexplorer-show-document");
    if (changeDrive!=null) {
      map = changeDrive;
      context.setAttribute("jcrexplorer-show-document", null);
    }
    if (portletReqContext.getApplicationMode() == PortletMode.VIEW) {
      if (map.size() > 0) {
        showDocument(context, map);
      } else {
        initwhenDirect(explorerContainer, editContainer);
      }
      explorerContainer.setRendered(true);
      UIJCRExplorer uiExplorer = explorerContainer.getChild(UIJCRExplorer.class);
      if(uiExplorer != null) {
        try {
          uiExplorer.getSession();
          try {
            uiExplorer.getSession().getItem(uiExplorer.getRootPath());
          } catch(PathNotFoundException e) {
            reloadWhenBroken(uiExplorer);
            super.processRender(app, context);
            return;
          }
        } catch(RepositoryException repo) {
          super.processRender(app, context);
        }
      }
      getChild(UIJcrExplorerEditContainer.class).setRendered(false);
    } else if(portletReqContext.getApplicationMode() == PortletMode.HELP) {
      if (LOG.isDebugEnabled()) LOG.debug("\n\n>>>>>>>>>>>>>>>>>>> IN HELP  MODE \n");
    } else if(portletReqContext.getApplicationMode() == PortletMode.EDIT) {
      explorerContainer.setRendered(false);
      getChild(UIJcrExplorerEditContainer.class).setRendered(true);
    }

//    RenderResponse response = context.getResponse();
//    Element elementS = response.createElement("script");
//    elementS.setAttribute("type", "text/javascript");
//    elementS.setAttribute("src", "/eXoWCMResources/javascript/eXo/wcm/backoffice/public/Components.js");
//    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT,elementS);
    UIJCRExplorer uiExplorer = explorerContainer.getChild(UIJCRExplorer.class);
    UITreeExplorer uiTreeExplorer = uiExplorer.findFirstComponentOfType(UITreeExplorer.class);
    AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
    context.getJavascriptManager().
    require("SHARED/multiUpload", "multiUpload").require("SHARED/jquery", "gj")
      .addScripts("multiUpload.setLocation('" + 
               uiExplorer.getWorkspaceName()  + "','" + 
               uiExplorer.getDriveData().getName()  + "','" +
               uiTreeExplorer.getLabel()  + "','" +
               Text.escapeIllegalJcrChars(uiExplorer.getCurrentPath()) + "','" +
               org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(uiExplorer.getDriveData().getHomePath(),
               ConversationState.getCurrent().getIdentity().getUserId())+ "', '"+
              autoVersionService.isVersionSupport(uiExplorer.getCurrentPath(), uiExplorer.getCurrentWorkspace())+"');")
      .addScripts("gj(document).ready(function() { gj(\"*[rel='tooltip']\").tooltip();});");
    super.processRender(app, context);
  }

  public void initwhenDirect(UIJcrExplorerContainer explorerContainer,
      UIJcrExplorerEditContainer editContainer) throws Exception {
    if (editContainer.getChild(UIJcrExplorerEditForm.class).isFlagSelectRender()) {
      explorerContainer.initExplorer();
      editContainer.getChild(UIJcrExplorerEditForm.class).setFlagSelectRender(false);
    }
  }

  public String getPreferenceRepository() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
    PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
    String repository = portletPref.getValue(Utils.REPOSITORY, "") ;
    return repository ;
  }

  public String getPreferenceTrashHomeNodePath() {
    return getPortletPreferences().getValue(Utils.TRASH_HOME_NODE_PATH, "");
  }

  public String getPreferenceTrashRepository() {
    return getPortletPreferences().getValue(Utils.TRASH_REPOSITORY, "");
  }

  public String getPreferenceTrashWorkspace() {
    return getPortletPreferences().getValue(Utils.TRASH_WORKSPACE, "");
  }

  public PortletPreferences getPortletPreferences() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    return pcontext.getRequest().getPreferences();
  }
  
  public DriveData getUserDrive() throws Exception {
    ManageDriveService manageDriveService = getApplicationComponent(ManageDriveService.class);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    for(DriveData userDrive : manageDriveService.getPersonalDrives(userId)) {
      if(userDrive.getHomePath().endsWith("/Private")) {
        return userDrive;
      }
    }
    return null;
  }  
  
  public boolean canUseConfigDrive(String driveName) throws Exception {
    ManageDriveService dservice = getApplicationComponent(ManageDriveService.class);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    List<String> userRoles = Utils.getMemberships();
    for(DriveData drive : dservice.getDriveByUserRoles(userId, userRoles)) {
      if(drive.getName().equals(driveName)) return true;
    }
    return false;
  }

  public void reloadWhenBroken(UIJCRExplorer uiExplorer) {
    uiExplorer.getChild(UIControl.class).setRendered(false);
    UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
    uiWorkingArea.setRenderedChild(UIDrivesArea.class);
    UIRightClickPopupMenu uiRightClickPopupMenu = uiWorkingArea.getChild(UIRightClickPopupMenu.class);
    if(uiRightClickPopupMenu!=null)
      uiRightClickPopupMenu.setRendered(true);
  }

  private Map<String, String> getElementByContext(WebuiRequestContext context) {
    HashMap<String, String> mapParam = new HashMap<>();
    //In case access by ajax request
    if (context.useAjax()) return mapParam;
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    Matcher matcher;

    Map<String, String[]> requestParams = pcontext.getRequest().getParameterMap();
    for(String requestParamName : requestParams.keySet()) {
      if (requestParamName.equals("path")) {
        String nodePathParam = pcontext.getRequestParameter("path");
        String currentRepo = WCMCoreUtils.getRepository().getConfiguration().getName();
        String userId = Util.getPortalRequestContext().getRemoteUser();
        if (nodePathParam != null && nodePathParam.length() > 0) {
          Pattern patternUrl = Pattern.compile("([^/]+)/(.*)");
          matcher = patternUrl.matcher(nodePathParam);
          if (matcher.find()) {
            mapParam.put("repository", currentRepo);
            mapParam.put("drive", matcher.group(1));
            mapParam.put("path", matcher.group(2));
            mapParam.put("userId",userId);
          } else {
            patternUrl = Pattern.compile("(.*)");
            matcher = patternUrl.matcher(nodePathParam);
            if (matcher.find()) {
              mapParam.put("repository", currentRepo);
              mapParam.put("drive", matcher.group(1));
              mapParam.put("path", "/");
            }
          }
        }
      } else {
        mapParam.put(requestParamName, pcontext.getRequest().getParameter(requestParamName));
      }
    }

    return mapParam;
  }

  private void showDocument(WebuiRequestContext context, Map<String, String> map) throws Exception {
    String repositoryName = String.valueOf(map.get("repository"));
    String driveName = String.valueOf(map.get("drive"));
    if (driveName.equals(DOC_NOT_FOUND)) {
      UIApplication uiApp = findFirstComponentOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIDrivesArea.msg.not-found", null, ApplicationMessage.WARNING));
      return;
    }
    String path = String.valueOf(map.get("path"));
    if (path.indexOf("&") > 0) {
      path = path.substring(0, path.indexOf("&"));
    }
    if(!path.equals("/")) {
      ArrayList<String> encodeNameArr = new ArrayList<String>();
      for(String name : path.split("/")) {
        if(name.length() > 0) {
          encodeNameArr.add(Text.escapeIllegalJcrChars(name));
        }
      }
      StringBuilder encodedPath = new StringBuilder();
      for(String encodedName : encodeNameArr) {
        encodedPath.append("/").append(encodedName);
      }
      path = encodedPath.toString();
    }
    UIApplication uiApp = findFirstComponentOfType(UIApplication.class);
    ManageDriveService manageDrive = getApplicationComponent(ManageDriveService.class);
    DriveData driveData = null;
    try {
      driveData = manageDrive.getDriveByName(driveName);
      if (driveData == null) throw new PathNotFoundException();
    } catch (PathNotFoundException e) {
      Object[] args = { driveName };

      return;
    }
    RepositoryService rservice = getApplicationComponent(RepositoryService.class);
    String userId = Util.getPortalRequestContext().getRemoteUser();
    List<String> viewList = new ArrayList<String>();

    for (String role : Utils.getMemberships()) {
      for (String viewName : driveData.getViews().split(",")) {
        if (!viewList.contains(viewName.trim())) {
          Node viewNode =
            getApplicationComponent(ManageViewService.class).getViewByName(viewName.trim(),
                WCMCoreUtils.getSystemSessionProvider());
          String permiss = viewNode.getProperty("exo:accessPermissions").getString();
          if (permiss.contains("${userId}")) permiss = permiss.replace("${userId}", userId);
          String[] viewPermissions = permiss.split(",");
          if (permiss.equals("*")) viewList.add(viewName.trim());
          if (driveData.hasPermission(viewPermissions, role)) viewList.add(viewName.trim());
        }
      }
    }
    StringBuffer viewListStr = new StringBuffer();
    List<SelectItemOption<String>> viewOptions = new ArrayList<SelectItemOption<String>>();
    ResourceBundle res = context.getApplicationResourceBundle();
    String viewLabel = null;
    for (String viewName : viewList) {
      try {
        viewLabel = res.getString("Views.label." + viewName);
      } catch (MissingResourceException e) {
        viewLabel = viewName;
      }
      viewOptions.add(new SelectItemOption<String>(viewLabel, viewName));
      if (viewListStr.length() > 0)
        viewListStr.append(",").append(viewName);
      else
        viewListStr.append(viewName);
    }
    driveData.setViews(viewListStr.toString());

    String homePath = driveData.getHomePath();
    Matcher matcher = driveParameteriedPathPattern.matcher(homePath);
    if(matcher.matches()) {
      // if the drive is a virtual drive containing, the paramterized value is available as request parameter
      String drivePathParamName = matcher.group(1);
      String drivePathParamValue = map.get(drivePathParamName);
      driveData.getParameters().put(drivePathParamName, drivePathParamValue);
      // we need to get the real drive home path
      if(StringUtils.isNotEmpty(drivePathParamValue)) {
        if(ManageDriveServiceImpl.DRIVE_PARAMATER_USER_ID.equals(drivePathParamName)) {
          // User id parameter is a special case since it must be replaced by its distributed format
          homePath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, drivePathParamValue);
        } else {
          // we update the drive homePath with the real value
          homePath = StringUtils.replaceOnce(homePath, "${" + drivePathParamName + "}", drivePathParamValue);
        }
      }
    }

    // we extract the absolute path of the file (remove the drive name)
    String contentRealPath = path;
    int firstSlash = path.indexOf("/");
    if(firstSlash >= 0) {
      contentRealPath = path.substring(firstSlash);
    }

    setFlagSelect(true);
    UIJCRExplorer uiExplorer = findFirstComponentOfType(UIJCRExplorer.class);

    uiExplorer.setDriveData(driveData);
    uiExplorer.setIsReferenceNode(false);

    try {
      Session session = 
        WCMCoreUtils.getUserSessionProvider().getSession(driveData.getWorkspace(), rservice.getCurrentRepository());
      nodeFinder.getItem(session, contentRealPath);
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
    uiExplorer.setRepositoryName(repositoryName);
    uiExplorer.setWorkspaceName(driveData.getWorkspace());
    uiExplorer.setRootPath(homePath);
    String addressPath = contentRealPath.replaceAll("/+", "/");
    // handle special case of docs in Public Personal Documents and the symlink "Public"
    if(driveData.getName().equals(ManageDriveServiceImpl.PERSONAL_DRIVE_NAME) &&
            !addressPath.startsWith(homePath)) {
      String publicHomePath = homePath.replace("/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PRIVATE_FOLDER_NAME, "/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME);
      if(addressPath.startsWith(publicHomePath)) {
        addressPath = addressPath.replace("/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME, "/Private/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME);
      }
    }
    Preference pref = uiExplorer.getPreference();
    pref.setShowSideBar(driveData.getViewSideBar());
    pref.setShowNonDocumentType(driveData.getViewNonDocument());
    pref.setShowPreferenceDocuments(driveData.getViewPreferences());
    pref.setAllowCreateFoder(driveData.getAllowCreateFolders());
    pref.setShowHiddenNode(driveData.getShowHiddenNode());
    uiExplorer.setIsReferenceNode(false);
    UIControl uiControl = uiExplorer.getChild(UIControl.class);

    UIAddressBar uiAddressBar = uiControl.getChild(UIAddressBar.class);
    uiAddressBar.setViewList(viewList);
    uiAddressBar.setSelectedViewName(viewList.get(0));
    uiAddressBar.setRendered(isShowTopBar());

    UIWorkingArea uiWorkingArea = findFirstComponentOfType(UIWorkingArea.class);
    UIActionBar uiActionbar = uiWorkingArea.getChild(UIActionBar.class);
    boolean isShowActionBar = isShowActionBar() ;
    uiActionbar.setTabOptions(viewList.get(0));
    uiActionbar.setRendered(isShowActionBar);
    uiExplorer.clearNodeHistory(addressPath);
    uiExplorer.setSelectNode(driveData.getWorkspace(), addressPath, homePath);
    UIDocumentWorkspace uiDocWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    uiDocWorkspace.setRenderedChild(UIDocumentContainer.class);
    uiDocWorkspace.setRendered(true);

    UIDrivesArea uiDrive = uiWorkingArea.getChild(UIDrivesArea.class);
    if (uiDrive != null) uiDrive.setRendered(false);
    context.addUIComponentToUpdateByAjax(uiDocWorkspace);
    UIPopupContainer popupAction = getChild(UIPopupContainer.class);
    if (popupAction != null && popupAction.isRendered()) {
      popupAction.deActivate();
      context.addUIComponentToUpdateByAjax(popupAction);
    }

    Boolean isEdit = Boolean.valueOf(Util.getPortalRequestContext().getRequestParameter("edit"));
    Node selectedNode = uiExplorer.getCurrentNode();
    if (isEdit) {
      if (uiExplorer.getCurrentPath().equals(addressPath)) {
        if(canManageNode(selectedNode, uiApp, uiExplorer, uiActionbar, context, EditDocumentActionComponent.getFilters())) {
          EditDocumentActionComponent.editDocument(null, context, this, uiExplorer, selectedNode, uiApp);
        }else if(canManageNode(selectedNode, uiApp, uiExplorer, uiActionbar, context, EditPropertyActionComponent.getFilters())) {
          EditPropertyActionComponent.editDocument(null, context, this, uiExplorer, selectedNode, uiApp);
        }
      } else {
        uiApp.addMessage(new ApplicationMessage("UIJCRExplorerPortlet.msg.file-access-denied", null, ApplicationMessage.WARNING));
        
      }
    }
    boolean isAddNew = Boolean.valueOf(Util.getPortalRequestContext().getRequestParameter("addNew"));

    if(!isAddNew && !isEdit) {
      showVersionHistory(selectedNode, uiWorkingArea);
    }

    if (!isEdit && isAddNew) {
      if (canManageNode(selectedNode, uiApp, uiExplorer, uiActionbar, context, AddDocumentActionComponent.getFilters())) {
        AddDocumentActionComponent.addDocument(null, uiExplorer, uiApp, this, context);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIJCRExplorerPortlet.msg.file-access-denied",
                                                null,
                                                ApplicationMessage.WARNING));
      }
    }
    uiExplorer.refreshExplorer(null, (isAddNew && isEdit));
  }

  private void showVersionHistory(Node selectedNode, UIWorkingArea uiWorkingArea) throws Exception {
    Boolean isDisplayVersionHistory = Boolean.valueOf(Util.getPortalRequestContext().getRequestParameter("versions"));
    if (isDisplayVersionHistory && selectedNode.isNodeType(Utils.MIX_VERSIONABLE)) {
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if (uiDocumentWorkspace != null) {
        UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
        if (uiVersionInfo != null) {
          uiVersionInfo.setCurrentNode(selectedNode);
          uiVersionInfo.setRootOwner(selectedNode.getProperty(Utils.EXO_LASTMODIFIER).getString());
          uiVersionInfo.activate();
          uiDocumentWorkspace.setRenderedChild(UIVersionInfo.class);
        }
      }
    }
  }

  private boolean canManageNode(Node selectedNode,
                                UIApplication uiApp,
                                UIJCRExplorer uiExplorer,
                                UIActionBar uiActionBar,
                                Object context,
                                List<UIExtensionFilter> filters) throws Exception {
    Map<String, Object> ctx = new HashMap<String, Object>();
    ctx.put(UIActionBar.class.getName(), uiActionBar);
    ctx.put(UIJCRExplorer.class.getName(), uiExplorer);
    ctx.put(UIApplication.class.getName(), uiApp);
    ctx.put(Node.class.getName(), selectedNode);
    ctx.put(WebuiRequestContext.class.getName(), context);
    for (UIExtensionFilter filter : filters)
      try {
        if (!filter.accept(ctx))
          return false;
      } catch (Exception e) {
        return false;
      }
    return true;
  }
}

