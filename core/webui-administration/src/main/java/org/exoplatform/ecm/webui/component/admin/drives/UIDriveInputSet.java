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
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.form.validator.DrivePermissionValidator;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Jun 28, 2006
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl")
public class UIDriveInputSet extends UIFormInputSetWithAction {
  final static public String FIELD_NAME = "name";
  final static public String FIELD_WORKSPACE = "workspace";
  final static public String FIELD_HOMEPATH = "homePath";
  final static public String FIELD_WORKSPACEICON = "icon";
  final static public String FIELD_PERMISSION = "permissions";
  final static public String FIELD_ALLOW_NODETYPES_ON_TREE = "allowNodeTypesOnTree";
  final static public String FIELD_VIEWPREFERENCESDOC = "viewPreferences";
  final static public String FIELD_VIEWNONDOC = "viewNonDocument";
  final static public String FIELD_VIEWSIDEBAR = "viewSideBar";
  final static public String FIELD_FOLDER_ONLY = "Folder";
  final static public String FIELD_BOTH = "Both";
  final static public String FIELD_UNSTRUCTURED_ONLY = "Unstructured folder";
  final static public String FIELD_ALLOW_CREATE_FOLDERS = "allowCreateFolders";
  final static public String SHOW_HIDDEN_NODE = "showHiddenNode";

  private final static Log  LOG  = ExoLogger.getLogger(UIDriveInputSet.class);

  public String bothLabel_;
  public String folderOnlyLabel_;
  public String unstructuredFolderLabel_;
  protected Set<String> setFoldertypes;
  protected TemplateService templateService;
  public UIDriveInputSet(String name) throws Exception {
    super(name);
    setComponentConfig(getClass(), null);

    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
                       addValidator(MandatoryValidator.class).addValidator(ECMNameValidator.class));
    addUIFormInput(new UIFormSelectBox(FIELD_WORKSPACE, FIELD_WORKSPACE, null));
    UIFormStringInput homePathField = new UIFormStringInput(FIELD_HOMEPATH, FIELD_HOMEPATH, null);
    homePathField.setValue("/");
    homePathField.setEditable(false);
    addUIFormInput(homePathField);
    addUIFormInput(new UIFormStringInput(FIELD_WORKSPACEICON, FIELD_WORKSPACEICON, null).setEditable(false));
    UIFormStringInput permissonSelectField = new UIFormStringInput(FIELD_PERMISSION , FIELD_PERMISSION , null);
    permissonSelectField.addValidator(MandatoryValidator.class);
    permissonSelectField.addValidator(DrivePermissionValidator.class);
    permissonSelectField.setEditable(true);
    addUIFormInput(permissonSelectField);
    addUIFormInput(new UIFormCheckBoxInput<String>(FIELD_VIEWPREFERENCESDOC, FIELD_VIEWPREFERENCESDOC, null));
    addUIFormInput(new UIFormCheckBoxInput<String>(FIELD_VIEWNONDOC, FIELD_VIEWNONDOC, null));
    addUIFormInput(new UIFormCheckBoxInput<String>(FIELD_VIEWSIDEBAR, FIELD_VIEWSIDEBAR, null));
    addUIFormInput(new UIFormCheckBoxInput<String>(SHOW_HIDDEN_NODE, SHOW_HIDDEN_NODE, null));

    addUIFormInput(new UIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS, 
                                       FIELD_ALLOW_CREATE_FOLDERS, 
                                       null).addValidator(MandatoryValidator.class));
    UIFormStringInput filterNodeTypes =
      new UIFormStringInput(FIELD_ALLOW_NODETYPES_ON_TREE , FIELD_ALLOW_NODETYPES_ON_TREE , null);
    addUIFormInput(filterNodeTypes);
    setActionInfo(FIELD_ALLOW_NODETYPES_ON_TREE, new String[] {"ChooseNodeType", "RemoveNodeType"});
    setActionInfo(FIELD_PERMISSION, new String[] {"AddPermission", "RemovePermission"});
    setActionInfo(FIELD_HOMEPATH, new String[] {"AddPath"});
    setActionInfo(FIELD_WORKSPACEICON, new String[] {"AddIcon"});
    templateService = getApplicationComponent(TemplateService.class);
    setFoldertypes = templateService.getAllowanceFolderType();
  }

  public void update(DriveData drive) throws Exception {
    String[] wsNames = getApplicationComponent(RepositoryService.class)
                      .getCurrentRepository().getWorkspaceNames();


    List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>();

    List<SelectItemOption<String>> foldertypeOptions = new ArrayList<SelectItemOption<String>>();
    for(String wsName : wsNames) {
      workspace.add(new SelectItemOption<String>(wsName,  wsName));
    }

    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();

    for (String foldertype : setFoldertypes) {
      try {
        foldertypeOptions.add(new SelectItemOption<String>(res.getString(getId() + ".label."
            + foldertype.replace(":", "_")), foldertype));
      } catch (MissingResourceException mre) {
        foldertypeOptions.add(new SelectItemOption<String>(foldertype, foldertype));
      }
    }
    getUIFormSelectBox(FIELD_WORKSPACE).setOptions(workspace);
    getUIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS).setOptions(foldertypeOptions);
    getUIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS).setMultiple(true);
    if(drive != null) {

      // Begin of update
      UIDriveForm uiDriveForm =  getAncestorOfType(UIDriveForm.class);
      String selectedWorkspace = drive.getWorkspace();
      String wsInitRootNodeType = uiDriveForm.getWorkspaceEntries(selectedWorkspace);
      // End of update

      invokeGetBindingField(drive);
      //Set value for multi-value select box
      String foldertypes = drive.getAllowCreateFolders();
      String selectedFolderTypes[];
      if (foldertypes.contains(",")) {
        selectedFolderTypes = foldertypes.split(",");
      } else {
        selectedFolderTypes = new String[] {foldertypes};
      }
      List<SelectItemOption<String>> folderOptions = new ArrayList<SelectItemOption<String>>();
      if(wsInitRootNodeType != null && wsInitRootNodeType.equals(Utils.NT_FOLDER)) {
        folderOptions.add(new SelectItemOption<String>(UIDriveInputSet.FIELD_FOLDER_ONLY, Utils.NT_FOLDER));
      } else {
        folderOptions.addAll(foldertypeOptions);
      }

      getUIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS).setOptions(folderOptions);
      getUIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS).setSelectedValues(selectedFolderTypes);
      getUIStringInput(FIELD_NAME).setEditable(false);
      return;
    }
    getUIStringInput(FIELD_NAME).setEditable(true);
    reset();
    getUIFormCheckBoxInput(FIELD_VIEWPREFERENCESDOC).setChecked(false);
    getUIFormCheckBoxInput(FIELD_VIEWNONDOC).setChecked(false);
    getUIFormCheckBoxInput(FIELD_VIEWSIDEBAR).setChecked(false);
    getUIFormCheckBoxInput(SHOW_HIDDEN_NODE).setChecked(false);
  }

  public void updateFolderAllowed(String path) {
    UIFormSelectBox sltWorkspace =  getChildById(UIDriveInputSet.FIELD_WORKSPACE);
    String strWorkspace = sltWorkspace.getSelectedValues()[0];
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      Session session = sessionProvider.getSession(strWorkspace,
                                getApplicationComponent(RepositoryService.class).getCurrentRepository());
      Node rootNode = (Node)session.getItem(path);
      List<SelectItemOption<String>> foldertypeOptions = new ArrayList<SelectItemOption<String>>();
      RequestContext context = RequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      for (String foldertype : setFoldertypes) {
        if (isChildNodePrimaryTypeAllowed(rootNode, foldertype) ){
          try {
              foldertypeOptions.add(new SelectItemOption<String>(res.getString(getId() + ".label."
                  + foldertype.replace(":", "_")), foldertype));
          } catch (MissingResourceException mre) {
            foldertypeOptions.add(new SelectItemOption<String>(foldertype, foldertype));
          }
        }
      }
      getUIFormSelectBox(FIELD_ALLOW_CREATE_FOLDERS).setOptions(foldertypeOptions);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem occurs while updating", e);
      }
    }
  }
  private boolean isChildNodePrimaryTypeAllowed(Node parent, String childNodeTypeName) throws Exception{
    NodeType childNodeType = parent.getSession().getWorkspace().getNodeTypeManager().getNodeType(childNodeTypeName);
    //In some cases, the child node is mixins type of a nt:file example
    if(childNodeType.isMixin()) return true;
    List<NodeType> allNodeTypes = new ArrayList<NodeType>();
    allNodeTypes.add(parent.getPrimaryNodeType());
    for(NodeType mixin: parent.getMixinNodeTypes()) {
      allNodeTypes.add(mixin);
    }
    for (NodeType nodetype:allNodeTypes) {
      if (((NodeTypeImpl)nodetype).isChildNodePrimaryTypeAllowed(childNodeTypeName)) {
        return true;
      }
    }
    return false;
  }
}
