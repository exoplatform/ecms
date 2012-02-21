/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.plugins.workflow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
            quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Dec 18, 2008
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:resources/templates/workflow/workflowPublicationDialog.gtmpl",
    events = {
      @EventConfig(listeners = UIWorkflowPublicationActionForm.SaveActionListener.class),
      @EventConfig(listeners = UIWorkflowPublicationActionForm.BackActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWorkflowPublicationActionForm.AddPermissionActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWorkflowPublicationActionForm.AddDestPathActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWorkflowPublicationActionForm.AddBackupPathActionListener.class, phase = Phase.DECODE)
    }
)
public class UIWorkflowPublicationActionForm extends UIForm implements UISelectable {

  private NodeLocation currentNode;
  private boolean isEdit = false;
  private String repositoryName;
  private String workspaceName;
  private String lifecycle;
  private final String FIELD_VALIDATOR = "fieldValidator";
  private final String FIELD_DESTPATH = "fieldDestPath";
  private final String FIELD_WORKSPACE = "fieldWorkspace";
  private final String FIELD_BACKUP = "fieldBackup";
  private final String FIELD_BACKUP_WORKSPACE = "fieldBackupWorkspace";
  private final String WORKFLOW_VALIDATOR = "workflow_validator";
  private final String WORKFLOW_DESTPATH = "workflow_destpath";
  private final String WORKFLOW_BACUP = "workflow_backuppath";
  private RepositoryService repositoryService;

  private final String FIELD_VALIDATOR_EDIT = "fieldValidatorEdit";
  private final String FIELD_DESTPATH_EDIT = "fieldDestPathEdit";
  private final String FIELD_BACKUPWORKSPACE_EDIT = "fieldBackupWorkspaceEdit";
  private final String FIELD_BACKUP_EDIT = "fieldBackupEdit";
  private static final Log LOG  = ExoLogger.getLogger(UIWorkflowPublicationActionForm.class);

  public UIWorkflowPublicationActionForm() throws Exception {
    repositoryService = getApplicationComponent(RepositoryService.class);
    repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
  }

  private void initSelectBox(UIFormSelectBox selectBox) throws Exception {
    String[] wsNames = repositoryService.getCurrentRepository().getWorkspaceNames();
    List<SelectItemOption<String>> workspaceList = new ArrayList<SelectItemOption<String>>();
    for (String wsName : wsNames) {
      workspaceList.add(new SelectItemOption<String>(wsName,  wsName));
    }
    selectBox.setOptions(workspaceList);
  }

  public void setWorkspaceName(String workspace) {
    workspaceName = workspace;
  }

  public String getWorkspaceName() {
    return workspaceName;
  }

  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode);
  }

  public void setIsEdit(boolean isEdit_) { isEdit = isEdit_; }

  public boolean getIsEdit() {
    return isEdit;
  }

  public Session getSession() throws Exception {
    return getCurrentNode().getSession();
  }

  public String getLifecycle() {
    return lifecycle;
  }

  public WorkflowPublicationConfig getConfig() {
    return WorkflowPublicationPlugin.config;
  }

  public String getLinkStateImage (Locale locale) {
    try {
      DownloadService dS = getApplicationComponent(DownloadService.class);
      PublicationService service = getApplicationComponent(PublicationService.class);

      byte[] bytes = service.getStateImage(getCurrentNode(),locale);
      InputStream iS = new ByteArrayInputStream(bytes);
      String id = dS.addDownloadResource(new InputStreamDownloadResource(iS, "image/gif"));
      return dS.getDownloadLink(id);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getStateImage";
    }
  }

  public void createNewAction(Node node, String lifecycle_, boolean isEdit_) throws Exception {
    reset();
    currentNode = NodeLocation.getNodeLocationByNode(node);
    lifecycle = lifecycle_;
    isEdit = isEdit_;
    setActions(new String[]{"Save", "Back"});

    if (WorkflowPublicationPlugin.config.isDestPath_currentFolder()) {
      WorkflowPublicationPlugin.config.setDestPath(node.getParent().getPath());
    }

    if (WorkflowPublicationPlugin.config.isEditable()) {
      UIFormInputSetWithAction inputSetValidator = new UIFormInputSetWithAction(WORKFLOW_VALIDATOR);
      UIFormStringInput validatorStringInput = new UIFormStringInput(FIELD_VALIDATOR, FIELD_VALIDATOR, null);
      validatorStringInput.setEditable(false);
      validatorStringInput.setValue(WorkflowPublicationPlugin.config.getValidator());
      inputSetValidator.addUIFormInput(validatorStringInput);
      inputSetValidator.setActionInfo(FIELD_VALIDATOR, new String[] {"AddPermission"});
      addUIComponentInput(inputSetValidator);

      UIFormSelectBox selectBoxWorkspace = new UIFormSelectBox(FIELD_WORKSPACE, FIELD_WORKSPACE, null);
      initSelectBox(selectBoxWorkspace);
      selectBoxWorkspace.setValue(WorkflowPublicationPlugin.config.getTo_workspace());
      addUIFormInput(selectBoxWorkspace);

      UIFormInputSetWithAction inputSetDestPath = new UIFormInputSetWithAction(WORKFLOW_DESTPATH);
      UIFormStringInput destPathStringInput = new UIFormStringInput(FIELD_DESTPATH, FIELD_DESTPATH, null);
      destPathStringInput.setEditable(false);
      destPathStringInput.setValue(WorkflowPublicationPlugin.config.getDestPath());
      inputSetDestPath.addUIFormInput(destPathStringInput);
      inputSetDestPath.setActionInfo(FIELD_DESTPATH, new String[] {"AddDestPath"});
      addUIComponentInput(inputSetDestPath);

      UIFormSelectBox selectBoxBackupWorkspace = new UIFormSelectBox(FIELD_BACKUP_WORKSPACE, FIELD_BACKUP_WORKSPACE, null);
      initSelectBox(selectBoxBackupWorkspace);
      selectBoxBackupWorkspace.setValue(WorkflowPublicationPlugin.config.getBackupWorkflow());
      addUIFormInput(selectBoxBackupWorkspace);

      UIFormInputSetWithAction inputSetBackup = new UIFormInputSetWithAction(WORKFLOW_BACUP);
      UIFormStringInput backupPathStringInput = new UIFormStringInput(FIELD_BACKUP, FIELD_BACKUP, null);
      backupPathStringInput.setEditable(false);
      backupPathStringInput.setValue(WorkflowPublicationPlugin.config.getBackupPath());
      inputSetBackup.addUIFormInput(backupPathStringInput);
      inputSetBackup.setActionInfo(FIELD_BACKUP, new String[] {"AddBackupPath"});
      addUIComponentInput(inputSetBackup);
    } else {
      UIFormStringInput validatorStringInput = new UIFormStringInput(FIELD_VALIDATOR_EDIT, FIELD_VALIDATOR_EDIT, null);
      validatorStringInput.setEditable(false);
      validatorStringInput.setValue(WorkflowPublicationPlugin.config.getValidator());
      addUIFormInput(validatorStringInput);

      UIFormSelectBox selectBoxWorkspace = new UIFormSelectBox(FIELD_WORKSPACE,
                                                               FIELD_WORKSPACE,
                                                               null);
      initSelectBox(selectBoxWorkspace);
      selectBoxWorkspace.setValue(WorkflowPublicationPlugin.config.getTo_workspace());
      selectBoxWorkspace.setDisabled(true);
      addUIFormInput(selectBoxWorkspace);

      UIFormStringInput destPathStringInput = new UIFormStringInput(FIELD_DESTPATH_EDIT,
                                                                    FIELD_DESTPATH_EDIT,
                                                                    null);
      destPathStringInput.setEditable(false);
      destPathStringInput.setValue(WorkflowPublicationPlugin.config.getDestPath());
      addUIFormInput(destPathStringInput);

      UIFormSelectBox selectBoxBackupWorkspace = new UIFormSelectBox(FIELD_BACKUPWORKSPACE_EDIT,
                                                                     FIELD_BACKUPWORKSPACE_EDIT,
                                                                     null);
      initSelectBox(selectBoxBackupWorkspace);
      selectBoxBackupWorkspace.setValue(WorkflowPublicationPlugin.config.getBackupWorkflow());
      selectBoxBackupWorkspace.setDisabled(true);
      addUIFormInput(selectBoxBackupWorkspace);

      UIFormStringInput backupPathStringInput = new UIFormStringInput(FIELD_BACKUP_EDIT,
                                                                      FIELD_BACKUP_EDIT,
                                                                      null);
      backupPathStringInput.setEditable(false);
      backupPathStringInput.setValue(WorkflowPublicationPlugin.config.getBackupPath());
      addUIFormInput(backupPathStringInput);
    }
  }

  public void doSelect(String selectField, Object value) throws Exception {
    getUIStringInput(selectField).setValue(value.toString());
    UIContainer container = null;
    if (getIsEdit()) {
      UIPopupWindow popupWindow = getParent();
      container = popupWindow.getParent();
      container.removeChildById(WorkflowPublicationPlugin.POPUP_ID);
    } else {
      container = getParent();
      container.removeChildById(WorkflowPublicationPlugin.POPUP_ID);
    }
  }

  public String getRepositoryName() throws Exception {
    return repositoryName;
  }

  public void initPopupPermission(UIContainer uiContainer, String membership) throws Exception {
    if (uiContainer.getChildById(WorkflowPublicationPlugin.POPUP_ID) != null)
      uiContainer.removeChildById(WorkflowPublicationPlugin.POPUP_ID);
    UIPopupWindow uiPopup = uiContainer.addChild(UIPopupWindow.class, null, WorkflowPublicationPlugin.POPUP_ID);
    uiPopup.setWindowSize(560, 350);
    uiPopup.setShowMask(true);
    UIPermissionSelector uiECMPermission =
      uiContainer.createUIComponent(UIPermissionSelector.class, null, null);
    uiECMPermission.setSelectedMembership(true);
    if (membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/") ;
      uiECMPermission.setCurrentPermission("/" + arrMember[1]) ;
    }
    uiPopup.setUIComponent(uiECMPermission);
    UIWorkflowPublicationActionForm workflowForm = findFirstComponentOfType(UIWorkflowPublicationActionForm.class);

    if (WorkflowPublicationPlugin.config.isEditable()) {
      uiECMPermission.setSourceComponent(workflowForm, new String[] {FIELD_VALIDATOR});
    } else {
      uiECMPermission.setSourceComponent(workflowForm, new String[] {FIELD_VALIDATOR_EDIT});
    }
    uiPopup.setShow(true);
  }

  private String getSystemWorkspaceName() throws RepositoryException {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    return manageableRepository.getConfiguration().getSystemWorkspaceName();
  }

  public String getLabel(String fieldName, String type) throws Exception {
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    WorkflowPublicationPlugin plugin = (WorkflowPublicationPlugin) publicationService.getPublicationPlugins()
                                                                                     .get(getLifecycle());
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    try {
      return plugin.getLocalizedAndSubstituteMessage(locale, getId() + "." + type + "." + fieldName, null);
    } catch(Exception e) {
      return fieldName;
    }
  }

  public void initPopupJCRBrowser(UIContainer uiContainer,
                                  String workspace,
                                  boolean isDisable,
                                  String sourceComponent) throws Exception {

    if (uiContainer.getChildById(WorkflowPublicationPlugin.POPUP_ID) != null)
      uiContainer.removeChildById(WorkflowPublicationPlugin.POPUP_ID);
    String repository = getRepositoryName();
    UIPopupWindow uiPopup = uiContainer.addChild(UIPopupWindow.class, null, WorkflowPublicationPlugin.POPUP_ID);
    uiPopup.setWindowSize(610, 300);
    uiPopup.setShowMask(true);
    UIOneNodePathSelector uiOneNodePathSelector =
      uiContainer.createUIComponent(UIOneNodePathSelector.class, null, null);
    uiOneNodePathSelector.setIsDisable(workspace, isDisable) ;
    uiOneNodePathSelector.setShowRootPathSelect(true) ;
    uiOneNodePathSelector.setRootNodeLocation(repository, workspace, "/");
    if (WCMCoreUtils.isAnonim()) {
      uiOneNodePathSelector.init(WCMCoreUtils.createAnonimProvider()) ;
    } else if (workspace.equals(getSystemWorkspaceName())){
      uiOneNodePathSelector.init(WCMCoreUtils.getSystemSessionProvider()) ;
    } else {
      uiOneNodePathSelector.init(WCMCoreUtils.getUserSessionProvider()) ;
    }
    uiPopup.setUIComponent(uiOneNodePathSelector);
    UIWorkflowPublicationActionForm workflowForm = findFirstComponentOfType(UIWorkflowPublicationActionForm.class);
    uiOneNodePathSelector.setSourceComponent(workflowForm, new String[] {sourceComponent}) ;
    uiPopup.setShow(true) ;
  }

  static public class SaveActionListener extends EventListener<UIWorkflowPublicationActionForm> {
    public void execute(Event<UIWorkflowPublicationActionForm> event) throws Exception {
      UIWorkflowPublicationActionForm workflowForm = event.getSource();
      PublicationService publicationService = workflowForm.getApplicationComponent(PublicationService.class);
      String validator, destWorkspace, destPath, backupPath, backupWorkspace;
      destWorkspace = workflowForm.getUIFormSelectBox(workflowForm.FIELD_WORKSPACE).getValue();
      if (WorkflowPublicationPlugin.config.isEditable()) {
        validator = workflowForm.getUIStringInput(workflowForm.FIELD_VALIDATOR).getValue();
        destPath = workflowForm.getUIStringInput(workflowForm.FIELD_DESTPATH).getValue();
        backupPath = workflowForm.getUIStringInput(workflowForm.FIELD_BACKUP).getValue();
        backupWorkspace = workflowForm.getUIStringInput(workflowForm.FIELD_BACKUP_WORKSPACE).getValue();
        if (validator == null || validator.trim().equals("") || destPath == null
            || destPath.trim().equals("") || backupPath == null || backupPath.trim().equals("")) {
          WorkflowPublicationPlugin plugin = (WorkflowPublicationPlugin) publicationService.
              getPublicationPlugins().get(WorkflowPublicationPlugin.WORKFLOW);
          Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
          String msg = plugin.getLocalizedAndSubstituteMessage(locale,
                                                               "UIWorkflowPublicationActionForm.msg.fillfield",
                                                               null);
          UIApplication uiApp = workflowForm.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage(msg, null, ApplicationMessage.WARNING));
          
          return;
        }
      } else {
        validator = workflowForm.getUIStringInput(workflowForm.FIELD_VALIDATOR_EDIT).getValue();
        destPath = workflowForm.getUIStringInput(workflowForm.FIELD_DESTPATH_EDIT).getValue();
        backupPath = workflowForm.getUIStringInput(workflowForm.FIELD_BACKUP_EDIT).getValue();
        backupWorkspace = workflowForm.getUIStringInput(workflowForm.FIELD_BACKUPWORKSPACE_EDIT).getValue();
      }

      HashMap<String, String> contextMap = new HashMap<String, String>();
      contextMap.put(WorkflowPublicationPlugin.VALIDATOR, validator);
      contextMap.put(WorkflowPublicationPlugin.DEST_WORKSPACE, destWorkspace);
      contextMap.put(WorkflowPublicationPlugin.DESTPATH, destPath);
      contextMap.put(WorkflowPublicationPlugin.BACUP_PATH, backupPath);
      contextMap.put(WorkflowPublicationPlugin.BACUP_WORKSPACE, backupWorkspace);

      publicationService.getPublicationPlugins()
                        .get(workflowForm.getLifecycle())
                        .changeState(workflowForm.getCurrentNode(),
                                     WorkflowPublicationPlugin.CONTENT_VALIDATION,
                                     contextMap);
      if (workflowForm.getIsEdit()) {
        UIPopupWindow popupWindow = workflowForm.getParent();
        UIContainer uicontainer = popupWindow.getParent();
        popupWindow.setRendered(false);
        if (uicontainer.getChildById(WorkflowPublicationPlugin.POPUP_EDIT_ID) != null) {
          uicontainer.removeChildById(WorkflowPublicationPlugin.POPUP_EDIT_ID);
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uicontainer);
        return;
      }
      UIContainer container = workflowForm.getAncestorOfType(UIContainer.class);
      UIComponent component = container.getParent();
      if (component != null) {
        component.setRendered(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(component.getParent());
      }
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIWorkflowPublicationActionForm> {
    public void execute(Event<UIWorkflowPublicationActionForm> event) throws Exception {
      UIWorkflowPublicationActionForm workflowForm = event.getSource();
      UIContainer uiContainer = null;
      String membership;
      if (WorkflowPublicationPlugin.config.isEditable()) {
        membership = workflowForm.getUIStringInput(workflowForm.FIELD_VALIDATOR).getValue();
      } else {
        membership = workflowForm.getUIStringInput(workflowForm.FIELD_VALIDATOR_EDIT).getValue();
      }

      if (workflowForm.getIsEdit()) {
        UIPopupWindow popupWindow = workflowForm.getParent();
        uiContainer = popupWindow.getParent();
      } else {
        uiContainer = workflowForm.getParent();
      }
      workflowForm.initPopupPermission(uiContainer, membership);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class AddDestPathActionListener extends EventListener<UIWorkflowPublicationActionForm> {
    public void execute(Event<UIWorkflowPublicationActionForm> event) throws Exception {
      UIWorkflowPublicationActionForm workflowForm = event.getSource();
      UIContainer uiContainer = null;
      String workspace;
      workspace = workflowForm.getUIFormSelectBox(workflowForm.FIELD_WORKSPACE).getValue();
      if (workflowForm.getIsEdit()) {
        UIPopupWindow popupWindow = workflowForm.getParent();
        uiContainer = popupWindow.getParent();
      } else {
        uiContainer = workflowForm.getParent();
      }
      if (WorkflowPublicationPlugin.config.isEditable()) {
        workflowForm.initPopupJCRBrowser(uiContainer, workspace, true, workflowForm.FIELD_DESTPATH);
      } else {
        workflowForm.initPopupJCRBrowser(uiContainer, workspace, true, workflowForm.FIELD_DESTPATH_EDIT);
      }
    }
  }

  static public class AddBackupPathActionListener extends EventListener<UIWorkflowPublicationActionForm> {
    public void execute(Event<UIWorkflowPublicationActionForm> event) throws Exception {
      UIWorkflowPublicationActionForm workflowForm = event.getSource();
      UIContainer uiContainer = null;
      if (workflowForm.getIsEdit()) {
        UIPopupWindow popupWindow = workflowForm.getParent();
        uiContainer = popupWindow.getParent();
      } else {
        uiContainer = workflowForm.getParent();
      }
      if (WorkflowPublicationPlugin.config.isEditable()) {
        workflowForm.initPopupJCRBrowser(uiContainer, WorkflowPublicationPlugin.BACKUP, true, workflowForm.FIELD_BACKUP);
      } else {
        workflowForm.initPopupJCRBrowser(uiContainer, WorkflowPublicationPlugin.BACKUP, true, workflowForm.FIELD_BACKUP_EDIT);
      }
    }
  }

  static public class BackActionListener extends EventListener<UIWorkflowPublicationActionForm> {
    public void execute(Event<UIWorkflowPublicationActionForm> event) throws Exception {
      UIWorkflowPublicationActionForm uiForm = event.getSource();
      if (uiForm.getIsEdit()) {
        UIPopupWindow popupWindow = uiForm.getParent();
        UIContainer uicontainer = popupWindow.getParent();
        popupWindow.setRendered(false);
        if (uicontainer.getChildById(WorkflowPublicationPlugin.POPUP_EDIT_ID) != null) {
          uicontainer.removeChildById(WorkflowPublicationPlugin.POPUP_EDIT_ID);
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uicontainer);
        return;
      }
      UIContainer container = uiForm.getParent();
      UIPopupWindow popupWindow = (UIPopupWindow)container.getParent();
      popupWindow.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());
    }
  }
}
