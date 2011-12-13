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
package org.exoplatform.ecm.webui.component.admin.drives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Session;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 5:31:04 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UIDriveForm.SaveActionListener.class),
      @EventConfig(listeners = UIDriveForm.RefreshActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.AddPermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.RemovePermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.AddPathActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.AddIconActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.ChangeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.ChooseNodeTypeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.RemoveNodeTypeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDriveForm.SelectTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UIDriveForm extends UIFormTabPane implements UISelectable {

  private boolean isAddNew_ = true;
  final static public String[] ACTIONS = {"Save", "Refresh", "Cancel"};
  final static public String POPUP_DRIVEPERMISSION = "PopupDrivePermission";
  final static public String POPUP_NODETYPE_SELECTOR = "PopupNodeTypeSelector";
  private String membershipString = "";
  private String nodeTypes = "";

  final static public String ANY_PERMISSION = "*";

  public UIDriveForm() throws Exception {
    super("UIDriveForm");
    UIFormInputSet driveInputSet = new UIDriveInputSet("DriveInputSet");
    UIFormSelectBox selectBox = driveInputSet.getChildById(UIDriveInputSet.FIELD_WORKSPACE);
    selectBox.setOnChange("Change");
    addUIFormInput(driveInputSet);
    setSelectedTab(driveInputSet.getId());
    UIFormInputSet viewInputSet = new UIViewsInputSet("ViewsInputSet");
    addUIFormInput(viewInputSet);
    setActions(ACTIONS);
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIDriveForm.label." + id);
    } catch (MissingResourceException ex) {
      return id + " ";
    }
  }

  public void doSelect(String selectField, Object value) {
    UIFormStringInput uiStringInput = getUIStringInput(selectField);
    if (selectField.equals(UIDriveInputSet.FIELD_PERMISSION)){
      String membership = value.toString();
      String valuePermissions = uiStringInput.getValue();
      List<String> permissionsList = new ArrayList<String>();
      StringBuilder newsPermissions = new StringBuilder();
      if(valuePermissions != null) {
        String[] permissionsArray = valuePermissions.split(",");
        permissionsList = Arrays.asList(permissionsArray);
        if (permissionsList.size() > 0) {
          for (String permission : permissionsList) {
            if(newsPermissions.length() > 0) newsPermissions.append(",");
            newsPermissions.append(permission.trim());
          }
        }
        if(!permissionsList.contains(membership)) {
          if(newsPermissions.length() > 0) {
            newsPermissions.append(",").append(membership.trim());
          } else {
            newsPermissions.append(membership.trim());
          }
        }
        uiStringInput.setValue(newsPermissions.toString());
      } else uiStringInput.setValue(value.toString());
    } else if (selectField.equals(UIDriveInputSet.FIELD_HOMEPATH)){
      uiStringInput.setValue(value.toString());
      UIDriveInputSet driveInputSet = getChild(UIDriveInputSet.class);
      if (driveInputSet!=null) driveInputSet.updateFolderAllowed(value.toString());
    } else {
      uiStringInput.setValue(value.toString());
    }
    UIDriveManager uiContainer = getAncestorOfType(UIDriveManager.class) ;
    for(UIComponent uiChild : uiContainer.getChildren()) {
      if(uiChild.getId().equals(POPUP_DRIVEPERMISSION) || uiChild.getId().equals("JCRBrowser")
          || uiChild.getId().equals("JCRBrowserAssets") || uiChild.getId().equals(POPUP_NODETYPE_SELECTOR)) {
        UIPopupWindow uiPopup = uiContainer.getChildById(uiChild.getId()) ;
        uiPopup.setRendered(false) ;
      }
    }
  }

  public void refresh(String driveName) throws Exception {
    DriveData drive = null;
    if(driveName == null) {
      isAddNew_ = true;
    } else {
      isAddNew_ = false;
      setActions(new String[] {"Save", "Cancel"});
      drive = getApplicationComponent(ManageDriveService.class).getDriveByName(driveName);
    }
    getChild(UIDriveInputSet.class).update(drive);
    getChild(UIViewsInputSet.class).update(drive);
  }

  @Deprecated
  public String getWorkspaceEntries(String selectedWorkspace, String repository) throws Exception {
    RepositoryService repositoryService =
      getApplicationComponent(RepositoryService.class);
    List<WorkspaceEntry> wsEntries = repositoryService.getCurrentRepository()
                                                      .getConfiguration()
                                                      .getWorkspaceEntries();
    String wsInitRootNodeType = null;
    for(WorkspaceEntry wsEntry : wsEntries) {
      if(wsEntry.getName().equals(selectedWorkspace)) {
        wsInitRootNodeType = wsEntry.getAutoInitializedRootNt();
        break;
      }
    }
    return wsInitRootNodeType;
  }

  public String getWorkspaceEntries(String selectedWorkspace) throws Exception {
    RepositoryService repositoryService =
      getApplicationComponent(RepositoryService.class);
    List<WorkspaceEntry> wsEntries = repositoryService.getCurrentRepository()
                                                      .getConfiguration()
                                                      .getWorkspaceEntries();
    String wsInitRootNodeType = null;
    for(WorkspaceEntry wsEntry : wsEntries) {
      if(wsEntry.getName().equals(selectedWorkspace)) {
        wsInitRootNodeType = wsEntry.getAutoInitializedRootNt();
        break;
      }
    }
    return wsInitRootNodeType;
  }

  static public class SaveActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      ManageDriveService dservice_ = uiDriveForm.getApplicationComponent(ManageDriveService.class);
      RepositoryService rservice = uiDriveForm.getApplicationComponent(RepositoryService.class);
      UIDriveInputSet driveInputSet = uiDriveForm.getChild(UIDriveInputSet.class);
      UIApplication uiApp = uiDriveForm.getAncestorOfType(UIApplication.class);
      String name = driveInputSet.getUIStringInput(UIDriveInputSet.FIELD_NAME).getValue().trim();
      if(name == null || name.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.name-null", null,
                                                ApplicationMessage.WARNING));
        return;
      }
      String[] arrFilterChar = {"&", "$", "@", "'", ":","]", "[", "*", "%", "!", "\""};
      for(String filterChar : arrFilterChar) {
        if(name.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.fileName-invalid", null,
                                                  ApplicationMessage.WARNING));
          return;
        }
      }
      String workspace =
        driveInputSet.getUIFormSelectBox(UIDriveInputSet.FIELD_WORKSPACE).getValue();
      String path = driveInputSet.getUIStringInput(UIDriveInputSet.FIELD_HOMEPATH).getValue();
      if((path == null)||(path.trim().length() == 0)) path = "/";

      // Only check path if Drive is not virtual drive
      if (!dservice_.isVitualDrive(name)) {
        Session session = null;
        try {
          session = rservice.getCurrentRepository().getSystemSession(workspace);
          String userId = Util.getPortalRequestContext().getRemoteUser();
          String pathReal = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(path, userId);
          session.getItem(pathReal);
          session.logout();
        } catch(Exception e) {
          if(session!=null) {
            session.logout();
          }
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.workspace-path-invalid", null,
                                                  ApplicationMessage.WARNING));
          return;
        }
      }

      boolean viewReferences =
        driveInputSet.getUIFormCheckBoxInput(UIDriveInputSet.FIELD_VIEWPREFERENCESDOC).isChecked();
      boolean viewSideBar =
        driveInputSet.getUIFormCheckBoxInput(UIDriveInputSet.FIELD_VIEWSIDEBAR).isChecked();
      boolean showHiddenNode =
        driveInputSet.getUIFormCheckBoxInput(UIDriveInputSet.SHOW_HIDDEN_NODE).isChecked();
      boolean viewNonDocument =
        driveInputSet.getUIFormCheckBoxInput(UIDriveInputSet.FIELD_VIEWNONDOC).isChecked();
      String[] allowCreateFolders = driveInputSet.getUIFormSelectBox(UIDriveInputSet.FIELD_ALLOW_CREATE_FOLDERS)
                                                 .getSelectedValues();
      if (allowCreateFolders == null || allowCreateFolders.length == 0 ) {
        uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.allowedCreateFolder", null,
            ApplicationMessage.WARNING));
        return;
      }
      StringBuilder foldertypes = new StringBuilder();
      for (String allowCreateFolder : allowCreateFolders) {
        foldertypes.append(allowCreateFolder).append(",");
      }
      if (foldertypes.toString().endsWith(",")) foldertypes.deleteCharAt(foldertypes.length() -1 );
      UIViewsInputSet viewsInputSet = uiDriveForm.getChild(UIViewsInputSet.class);
      String views = viewsInputSet.getViewsSelected();
      String permissions = driveInputSet.getUIStringInput(UIDriveInputSet.FIELD_PERMISSION).getValue();
      if (permissions.subSequence(permissions.length() - 1, permissions.length()).equals(","))
        permissions = permissions.substring(0, permissions.length() - 1);
      String[] arrPermissions = permissions.split(",");
      for (String itemPermission : arrPermissions) {
        if (itemPermission != null && itemPermission.trim().equals("*")) {
          permissions = "*";
          break;
        }
      }

      if(uiDriveForm.isAddNew_ && (dservice_.getDriveByName(name) != null)) {
        uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.drive-exists", null,
                                                ApplicationMessage.WARNING));
        return;
      }
      String iconPath = driveInputSet.getUIStringInput(UIDriveInputSet.FIELD_WORKSPACEICON).getValue();
      if(iconPath != null && iconPath.trim().length() > 0) {
        Session jcrSession = null;
        try {
          if(iconPath.indexOf(":/") > -1) {
            String[] paths = iconPath.split(":/");
            jcrSession = rservice.getCurrentRepository().getSystemSession(paths[0]);
            jcrSession.getItem("/" + paths[1]);
            jcrSession.logout();
          }
        } catch(Exception e) {
          if(jcrSession != null) {
            jcrSession.logout();
          }
          uiApp.addMessage(new ApplicationMessage("UIDriveForm.msg.icon-not-found", null,
                                                  ApplicationMessage.WARNING));
          return;
        }
      } else {
        iconPath = "";
      }
      String allowNodeTypesOnTree = driveInputSet.getUIStringInput(UIDriveInputSet.FIELD_ALLOW_NODETYPES_ON_TREE).getValue();
      if ((allowNodeTypesOnTree==null) || (allowNodeTypesOnTree.length()==0)) allowNodeTypesOnTree = "*";
      dservice_.addDrive(name, workspace, permissions, path, views, iconPath, viewReferences,
          viewNonDocument, viewSideBar, showHiddenNode, foldertypes.toString(), allowNodeTypesOnTree);
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      UIDriveList uiDriveList = uiManager.getChild(UIDriveList.class);
      uiDriveList.refresh(uiDriveList.getUIPageIterator().getCurrentPage());
      uiDriveForm.refresh(null);
      UIDriveManager uiDriveManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      uiDriveManager.removeChildById(UIDriveForm.POPUP_DRIVEPERMISSION);
      uiDriveManager.removeChildById(UIDriveList.ST_ADD);
      uiDriveManager.removeChildById(UIDriveList.ST_EDIT);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveManager);
    }
  }

  static  public class CancelActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      uiDriveForm.refresh(null);
      UIDriveManager uiDriveManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      uiDriveManager.removeChildById(UIDriveForm.POPUP_DRIVEPERMISSION);
      uiDriveManager.removeChildById(UIDriveList.ST_ADD);
      uiDriveManager.removeChildById(UIDriveList.ST_EDIT);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveManager);
    }
  }

  static  public class RefreshActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      event.getSource().refresh(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      String membership = uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_PERMISSION).getValue();
      uiDriveForm.membershipString = membership;
      uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_PERMISSION).setValue(uiDriveForm.membershipString);

      uiManager.initPopupPermission(uiDriveForm.membershipString);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class RemovePermissionActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      uiDriveForm.membershipString = "";
      uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_PERMISSION).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveForm);
    }
  }

  static public class AddPathActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      UIDriveInputSet driveInputSet = uiDriveForm.getChild(UIDriveInputSet.class);
      String workspace =
        driveInputSet.getUIFormSelectBox(UIDriveInputSet.FIELD_WORKSPACE).getValue();
      uiManager.initPopupJCRBrowser(workspace, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class AddIconActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      UIDriveInputSet driveInputSet = uiDriveForm.getChild(UIDriveInputSet.class);
      String workspace =
        driveInputSet.getUIFormSelectBox(UIDriveInputSet.FIELD_WORKSPACE).getValue();
      uiManager.initPopupJCRBrowserAssets(workspace);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class ChangeActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      String driverName = uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_NAME).getValue();
      String selectedWorkspace = uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_WORKSPACE).getValue();
      UIDriveInputSet driveInputSet = uiDriveForm.getChild(UIDriveInputSet.class);
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      ManageDriveService manageDriveService =
        uiDriveForm.getApplicationComponent(ManageDriveService.class);
      RepositoryService repositoryService =
        uiDriveForm.getApplicationComponent(RepositoryService.class);
      List<WorkspaceEntry> wsEntries =
        repositoryService.getCurrentRepository().getConfiguration().getWorkspaceEntries();
      String wsInitRootNodeType = null;
      for(WorkspaceEntry wsEntry : wsEntries) {
        if(wsEntry.getName().equals(selectedWorkspace)) {
          wsInitRootNodeType = wsEntry.getAutoInitializedRootNt();
        }
      }

      TemplateService templateService = uiDriveForm.getApplicationComponent(TemplateService.class);
      Set<String> setFoldertypes = templateService.getAllowanceFolderType();
      List<SelectItemOption<String>> foldertypeOptions = new ArrayList<SelectItemOption<String>>();
      RequestContext context = RequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String label = null;
      for(String folderType : setFoldertypes) {
        try {
          label = res.getString(driveInputSet.getId() + ".label." + folderType.replace(":", "_"));
        } catch(MissingResourceException mi) {
          label = folderType;
        }
        foldertypeOptions.add(new SelectItemOption<String>(label,  folderType));
      }
      List<SelectItemOption<String>> folderOptions = new ArrayList<SelectItemOption<String>>();
      UIFormSelectBox uiInput = driveInputSet.<UIFormSelectBox>getUIInput(UIDriveInputSet.FIELD_ALLOW_CREATE_FOLDERS);

      if(wsInitRootNodeType != null && wsInitRootNodeType.equals(Utils.NT_FOLDER)) {
        folderOptions.add(new SelectItemOption<String>(UIDriveInputSet.FIELD_FOLDER_ONLY, Utils.NT_FOLDER));
      } else {
        folderOptions.addAll(foldertypeOptions);
      }
      uiInput.setOptions(folderOptions);
      if(!uiDriveForm.isAddNew_) {
        DriveData drive = manageDriveService.getDriveByName(driverName);
        String defaultPath = drive.getHomePath();
        if(!drive.getWorkspace().equals(selectedWorkspace)) defaultPath = "/";
        uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_HOMEPATH).setValue(defaultPath);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class ChooseNodeTypeActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      UIDriveManager uiManager = uiDriveForm.getAncestorOfType(UIDriveManager.class);
      String nodeTypes = uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_ALLOW_NODETYPES_ON_TREE).getValue();
      if ((nodeTypes != null) && (uiDriveForm.membershipString.indexOf(nodeTypes) < 0)){
        if (uiDriveForm.nodeTypes.length() > 0)
          uiDriveForm.nodeTypes += "," + nodeTypes;
        else
          uiDriveForm.nodeTypes += nodeTypes;
      }
      uiDriveForm.getUIStringInput(
          UIDriveInputSet.FIELD_ALLOW_NODETYPES_ON_TREE).setValue(uiDriveForm.nodeTypes);

      uiManager.initPopupNodeTypeSelector(uiDriveForm.nodeTypes);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class RemoveNodeTypeActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiDriveForm = event.getSource();
      uiDriveForm.nodeTypes = "";
      uiDriveForm.getUIStringInput(UIDriveInputSet.FIELD_ALLOW_NODETYPES_ON_TREE).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveForm);
    }
  }

  static public class SelectTabActionListener extends EventListener<UIDriveForm> {
    public void execute(Event<UIDriveForm> event) throws Exception {
      UIDriveForm uiView = event.getSource() ;
      UIDriveManager uiMetaManager = uiView.getAncestorOfType(UIDriveManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMetaManager) ;
    }
 }

}
