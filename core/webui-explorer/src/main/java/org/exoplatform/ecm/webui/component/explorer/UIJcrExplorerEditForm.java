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

import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
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
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.NumberFormatValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 * 3 févr. 09
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl", events = {
    @EventConfig(phase = Phase.DECODE, listeners = UIJcrExplorerEditForm.SaveActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIJcrExplorerEditForm.CancelActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIJcrExplorerEditForm.SelectTypeActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIJcrExplorerEditForm.SelectDriveActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIJcrExplorerEditForm.SelectNodePathActionListener.class) }
)
public class UIJcrExplorerEditForm extends UIForm implements UISelectable {
  private boolean flagSelectRender = false;

  public static final String PARAM_PATH_ACTION = "SelectNodePath";

  public static final String PARAM_PATH_INPUT = "nodePath";

  private static final String POPUP_SELECT_PATH_INPUT = "PopupSelectPath";


  public UIJcrExplorerEditForm() throws Exception {
    UIFormSelectBox repository = new UIFormSelectBox(UIJCRExplorerPortlet.REPOSITORY,
                                                     UIJCRExplorerPortlet.REPOSITORY,
                                                     getRepoOption());
    String repositoryValue = getPreference().getValue(UIJCRExplorerPortlet.REPOSITORY, "");
    repository.setValue(repositoryValue);
    addChild(repository);

    UIFormCheckBoxInput<Boolean> checkBoxCategory = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.CATEGORY_MANDATORY,
                                                                                     null,
                                                                                     null);
    checkBoxCategory.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.CATEGORY_MANDATORY, "")));
    addChild(checkBoxCategory);

    UIFormStringInput uiMaxSizeUpload = new UIFormStringInput(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD,
                                                              UIJCRExplorerPortlet.MAX_SIZE_UPLOAD,
                                                              null);
    uiMaxSizeUpload.addValidator(NumberFormatValidator.class);
    uiMaxSizeUpload.setValue(getPreference().getValue(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD, ""));
    addChild(uiMaxSizeUpload);

    List<SelectItemOption<String>> listType = new ArrayList<SelectItemOption<String>>();
    String usecase = getPreference().getValue(UIJCRExplorerPortlet.USECASE, "");
    listType.add(new SelectItemOption<String>("Selection", "selection"));
    listType.add(new SelectItemOption<String>("Jailed", "jailed"));
    listType.add(new SelectItemOption<String>("Personal", "personal"));    
    listType.add(new SelectItemOption<String>("Parameterize", "parameterize"));
    UIFormSelectBox typeSelectBox = new UIFormSelectBox(UIJCRExplorerPortlet.USECASE, UIJCRExplorerPortlet.USECASE, listType);
    typeSelectBox.setValue(usecase);
    typeSelectBox.setOnChange("SelectType");
    addChild(typeSelectBox);

    UIFormInputSetWithAction driveNameInput = new UIFormInputSetWithAction("DriveNameInput");
    UIFormStringInput stringInputDrive = new UIFormStringInput(UIJCRExplorerPortlet.DRIVE_NAME,
                                                               UIJCRExplorerPortlet.DRIVE_NAME,
                                                               null);
    stringInputDrive.setValue(getPreference().getValue(UIJCRExplorerPortlet.DRIVE_NAME, ""));
    stringInputDrive.setEnable(false);
    driveNameInput.addUIFormInput(stringInputDrive);
    driveNameInput.setActionInfo(UIJCRExplorerPortlet.DRIVE_NAME, new String[] {"SelectDrive"});
    addUIComponentInput(driveNameInput);

    UIFormCheckBoxInput<Boolean> uiEditInNewWindow = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW,
                                                                                      UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW,
                                                                                      null);
    uiEditInNewWindow.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW,
                                                                               "true")));
    addChild(uiEditInNewWindow);

    UIFormInputSetWithAction uiParamPathInput = new UIFormInputSetWithAction(PARAM_PATH_ACTION);
    UIFormStringInput pathInput = new UIFormStringInput(UIJCRExplorerPortlet.PARAMETERIZE_PATH,
                                                        UIJCRExplorerPortlet.PARAMETERIZE_PATH,
                                                        null);
    pathInput.setValue(getPreference().getValue(UIJCRExplorerPortlet.PARAMETERIZE_PATH, ""));
    pathInput.setEnable(false);
    uiParamPathInput.addUIFormInput(pathInput);
    uiParamPathInput.setActionInfo(UIJCRExplorerPortlet.PARAMETERIZE_PATH, new String[] {PARAM_PATH_ACTION});
    addUIComponentInput(uiParamPathInput);

    driveNameInput.setRendered(true);
    uiParamPathInput.setRendered(false);

    UIFormCheckBoxInput<Boolean> uiFormCheckBoxTop = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.SHOW_TOP_BAR,
                                                                                      UIJCRExplorerPortlet.SHOW_TOP_BAR,
                                                                                      true);
    uiFormCheckBoxTop.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.SHOW_TOP_BAR, "true")));
    addUIFormInput(uiFormCheckBoxTop);

    UIFormCheckBoxInput<Boolean> uiFormCheckBoxAction = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.SHOW_ACTION_BAR,
                                                                                         UIJCRExplorerPortlet.SHOW_ACTION_BAR,
                                                                                         true);
    uiFormCheckBoxAction.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.SHOW_ACTION_BAR, "true")));
    addUIFormInput(uiFormCheckBoxAction);

    UIFormCheckBoxInput<Boolean> uiFormCheckBoxSide = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.SHOW_SIDE_BAR,
                                                                                       UIJCRExplorerPortlet.SHOW_SIDE_BAR,
                                                                                       true);
    uiFormCheckBoxSide.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.SHOW_SIDE_BAR, "true")));
    addUIFormInput(uiFormCheckBoxSide);

    UIFormCheckBoxInput<Boolean> uiFormCheckBoxFilter = new UIFormCheckBoxInput<Boolean>(UIJCRExplorerPortlet.SHOW_FILTER_BAR,
                                                                                         UIJCRExplorerPortlet.SHOW_FILTER_BAR,
                                                                                         true);
    uiFormCheckBoxFilter.setChecked(Boolean.parseBoolean(getPreference().getValue(UIJCRExplorerPortlet.SHOW_FILTER_BAR, "true")));
    addUIFormInput(uiFormCheckBoxFilter);

    if (usecase.equals(UIJCRExplorerPortlet.JAILED)) {
      setFlagSelectRender(true);
    } else if(usecase.equals(UIJCRExplorerPortlet.PERSONAL)) {
      driveNameInput.setRendered(false);
      setFlagSelectRender(true);    
    } else if (usecase.equals(UIJCRExplorerPortlet.PARAMETERIZE)) {
      uiParamPathInput.setRendered(true);
      setFlagSelectRender(true);
    }
    setActions(new  String[] {"Save", "Cancel"});
  }
  
  public boolean isFlagSelectRender() {
    return flagSelectRender;
  }

  public void setFlagSelectRender(boolean flagSelectRender) {
    this.flagSelectRender = flagSelectRender;
  }

  private PortletPreferences getPreference() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    return pcontext.getRequest().getPreferences();
  }

  private List<SelectItemOption<String>> getRepoOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    RepositoryEntry repo = repositoryService.getCurrentRepository().getConfiguration();
    options.add(new SelectItemOption<String>(repo.getName(), repo.getName()));
    return options;
  }

  public static class CancelActionListener extends EventListener<UIJcrExplorerEditForm>{
    public void execute(Event<UIJcrExplorerEditForm> event) throws Exception {
      UIJcrExplorerEditForm uiForm = event.getSource();
      PortletPreferences pref = uiForm.getPreference();
      UIFormSelectBox repository = uiForm.getChildById(UIJCRExplorerPortlet.REPOSITORY);
      repository.setValue(pref.getValue(UIJCRExplorerPortlet.REPOSITORY, ""));
      UIFormCheckBoxInput<Boolean> checkBoxCategory = uiForm.getChildById(UIJCRExplorerPortlet.CATEGORY_MANDATORY);
      checkBoxCategory.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.CATEGORY_MANDATORY, "")));
      UIFormSelectBox typeSelectBox = uiForm.getChildById(UIJCRExplorerPortlet.USECASE);
      typeSelectBox.setValue(pref.getValue(UIJCRExplorerPortlet.USECASE, ""));
      UIFormInputSetWithAction driveNameInput = uiForm.getChildById("DriveNameInput");
      UIFormStringInput stringInputDrive = driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
      stringInputDrive.setValue(pref.getValue(UIJCRExplorerPortlet.DRIVE_NAME, ""));
      UIFormCheckBoxInput<Boolean> editInNewWindow = uiForm.getChildById(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW);
      editInNewWindow.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW, "true")));

      UIFormCheckBoxInput<Boolean> checkBoxShowTopBar  = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_TOP_BAR);
      checkBoxShowTopBar.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.SHOW_TOP_BAR, "true")));
      UIFormCheckBoxInput<Boolean> checkBoxShowActionBar = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_ACTION_BAR);
      checkBoxShowActionBar.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.SHOW_ACTION_BAR, "true")));
      UIFormCheckBoxInput<Boolean> checkBoxShowLeftBar = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_SIDE_BAR);
      checkBoxShowLeftBar.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.SHOW_SIDE_BAR, "true")));
      UIFormCheckBoxInput<Boolean> checkBoxShowFilterBar = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_FILTER_BAR);
      checkBoxShowFilterBar.setChecked(Boolean.parseBoolean(pref.getValue(UIJCRExplorerPortlet.SHOW_FILTER_BAR,
                                                                          "true")));

      // update
      UIFormInputSetWithAction uiParamPathInput = uiForm.getChildById(PARAM_PATH_ACTION);
      UIFormStringInput stringInputPath = uiParamPathInput.getUIStringInput(UIJCRExplorerPortlet.PARAMETERIZE_PATH);
      stringInputPath.setValue(pref.getValue(UIJCRExplorerPortlet.PARAMETERIZE_PATH, ""));

      if (pref.getValue(UIJCRExplorerPortlet.USECASE, "").equals(UIJCRExplorerPortlet.JAILED)) {
        driveNameInput.setRendered(true);
        uiParamPathInput.setRendered(false);
      } else if (pref.getValue(UIJCRExplorerPortlet.USECASE, "").equals(UIJCRExplorerPortlet.PARAMETERIZE)) {
        driveNameInput.setRendered(true);
        uiParamPathInput.setRendered(true);
      } else {
        driveNameInput.setRendered(false);
        uiParamPathInput.setRendered(false);
      }
      UIFormStringInput uiMaxFileSize = uiForm.getUIStringInput(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD);
      uiMaxFileSize.setValue(pref.getValue(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD, ""));

      UIApplication uiApp  = uiForm.getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.fields-cancelled", null));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  public static class SelectTypeActionListener extends EventListener<UIJcrExplorerEditForm>{
    public void execute(Event<UIJcrExplorerEditForm> event) throws Exception {
      UIJcrExplorerEditForm uiForm = event.getSource();
      UIJCRExplorerPortlet uiJExplorerPortlet = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIFormSelectBox typeSelectBox = uiForm.getChildById(UIJCRExplorerPortlet.USECASE);
      UIFormInputSetWithAction driveNameInput = uiForm.getChildById("DriveNameInput");
      UIFormInputSetWithAction uiParamPathInput = uiForm.getChildById(PARAM_PATH_ACTION);
      driveNameInput.setRendered(true);
      uiParamPathInput.setRendered(false);
      if (typeSelectBox.getValue().equals(UIJCRExplorerPortlet.JAILED)) {
        UIFormStringInput stringInputDrive = driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
        stringInputDrive.setValue("");
        driveNameInput.setRendered(true);      
      } else if(typeSelectBox.getValue().equals(UIJCRExplorerPortlet.SELECTION)) {
        DriveData personalPrivateDrive =
          uiJExplorerPortlet.getUserDrive("private");
        UIFormStringInput stringInputDrive =
          driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
        stringInputDrive.setValue(personalPrivateDrive.getName());
      } else if(typeSelectBox.getValue().equals(UIJCRExplorerPortlet.PERSONAL)) {
        DriveData personalPrivateDrive =
          uiJExplorerPortlet.getUserDrive("private");
        UIFormStringInput stringInputDrive =
          driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
        stringInputDrive.setValue(personalPrivateDrive.getName());
        driveNameInput.setRendered(false);
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.personal-usecase", null));
      } else if(typeSelectBox.getValue().equals(UIJCRExplorerPortlet.PARAMETERIZE)) {
        UIFormStringInput stringInputDrive = driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
        stringInputDrive.setValue("");
        driveNameInput.setRendered(true);

        UIFormStringInput stringInputDrivePath = uiParamPathInput.getUIStringInput(UIJCRExplorerPortlet.PARAMETERIZE_PATH);
        stringInputDrivePath.setValue("");
        uiParamPathInput.setRendered(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  public static class SaveActionListener extends EventListener<UIJcrExplorerEditForm>{
    public void execute(Event<UIJcrExplorerEditForm> event) throws Exception {
      UIJcrExplorerEditForm uiForm = event.getSource();
      PortletPreferences pref = uiForm.getPreference();
      UIFormSelectBox repository = uiForm.getChildById(UIJCRExplorerPortlet.REPOSITORY);
      UIFormCheckBoxInput<Boolean> checkBoxCategory = uiForm.getChildById(UIJCRExplorerPortlet.CATEGORY_MANDATORY);
      UIFormStringInput uiMaxFileSize = uiForm.getUIStringInput(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD);
      UIFormSelectBox typeSelectBox = uiForm.getChildById(UIJCRExplorerPortlet.USECASE);

      UIFormInputSetWithAction driveNameInput = uiForm.getChildById("DriveNameInput");
      UIFormStringInput stringInputDrive = driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);

      UIFormCheckBoxInput<Boolean> uiFormCheckBoxTop = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_TOP_BAR);
      UIFormCheckBoxInput<Boolean> uiFormCheckBoxAction = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_ACTION_BAR);
      UIFormCheckBoxInput<Boolean> uiFormCheckBoxSide = uiForm.getChildById(UIJCRExplorerPortlet.SHOW_SIDE_BAR);
      UIFormCheckBoxInput<Boolean> uiFormCheckBoxFilter= uiForm.getChildById(UIJCRExplorerPortlet.SHOW_FILTER_BAR);

      UIFormCheckBoxInput<Boolean> editInNewWindow = uiForm.getChildById(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW);
      String nodePath = ((UIFormStringInput)uiForm.findComponentById(UIJCRExplorerPortlet.PARAMETERIZE_PATH)).getValue();
      String driveName = stringInputDrive.getValue();
      String useCase = typeSelectBox.getValue();

      String editInNewWindowOldValue = pref.getValue(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW, "true");
      if (!editInNewWindowOldValue.equals(String.valueOf(editInNewWindow.isChecked()))) {
        UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class)
                                         .getChild(UIJcrExplorerContainer.class)
                                         .getChild(UIJCRExplorer.class);
        uiExplorer.closeEditingFile();
      }

      if (useCase.equals(UIJCRExplorerPortlet.JAILED) ) {
        if ((driveName == null) || (driveName.length() == 0)) {
          UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.notNullDriveName", null,
              ApplicationMessage.WARNING));
          return;
        }
      } else if (useCase.equals(UIJCRExplorerPortlet.PARAMETERIZE)) {
        if ((nodePath == null) || (nodePath.length() == 0)) {
          UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.notNullPath", null,
              ApplicationMessage.WARNING));
          return;
        }
      }

      if (useCase.equals(UIJCRExplorerPortlet.SELECTION)) {
        //driveName = pref.getValue("driveName", "");
        nodePath = "/";
      } else {
//        uiForm.setFlagSelectRender(true);
      }
      uiForm.setFlagSelectRender(true);
      pref.setValue(UIJCRExplorerPortlet.REPOSITORY, repository.getValue());
      pref.setValue(UIJCRExplorerPortlet.CATEGORY_MANDATORY, String.valueOf(checkBoxCategory.isChecked()));
      pref.setValue(UIJCRExplorerPortlet.MAX_SIZE_UPLOAD, String.valueOf(uiMaxFileSize.getValue()));

      pref.setValue(UIJCRExplorerPortlet.USECASE, useCase);
      pref.setValue(UIJCRExplorerPortlet.DRIVE_NAME, driveName);
      pref.setValue(UIJCRExplorerPortlet.EDIT_IN_NEW_WINDOW, String.valueOf(editInNewWindow.isChecked()));
      pref.setValue(UIJCRExplorerPortlet.PARAMETERIZE_PATH, nodePath);
      pref.setValue(UIJCRExplorerPortlet.SHOW_ACTION_BAR, String.valueOf(uiFormCheckBoxAction.getValue()));
      pref.setValue(UIJCRExplorerPortlet.SHOW_TOP_BAR, String.valueOf(uiFormCheckBoxTop.getValue()));
      pref.setValue(UIJCRExplorerPortlet.SHOW_SIDE_BAR, String.valueOf(uiFormCheckBoxSide.getValue()));
      pref.setValue(UIJCRExplorerPortlet.SHOW_FILTER_BAR, String.valueOf(uiFormCheckBoxFilter.getValue()));

      pref.store();

      UIApplication  uiApp = uiForm.getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.fields-saved", null));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  public static class SelectDriveActionListener extends EventListener<UIJcrExplorerEditForm>{
    public void execute(Event<UIJcrExplorerEditForm> event) throws Exception {
      UIJcrExplorerEditForm uiForm = event.getSource();
      UIJcrExplorerEditContainer editContainer = uiForm.getParent();

      UIFormSelectBox repository = uiForm.getChild(UIFormSelectBox.class);
      if (repository.isEnable()) {
        UIPopupWindow popupWindow = editContainer.initPopup("PopUpSelectDrive");
        UIDriveSelector driveSelector = editContainer.createUIComponent(UIDriveSelector.class, null, null);
        driveSelector.updateGrid();
        popupWindow.setUIComponent(driveSelector);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      } else {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.drive-edit-permission", null,
            ApplicationMessage.WARNING));
        return;
      }
    }
  }

  public static class SelectNodePathActionListener extends EventListener<UIJcrExplorerEditForm>{
    public void execute(Event<UIJcrExplorerEditForm> event) throws Exception {
      UIJcrExplorerEditForm uiForm = event.getSource();
      UIJcrExplorerEditContainer editContainer = uiForm.getParent();
      UIFormInputSetWithAction driveNameInput = uiForm.getChildById("DriveNameInput");
      UIFormStringInput stringInputDrive = driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME);
      String driveName = stringInputDrive.getValue();

      UIFormSelectBox repository = uiForm.getChild(UIFormSelectBox.class);
      if (repository.isEnable()) {
        if (driveName == null || driveName.length() == 0) {
          UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.personal-usecase", null,
              ApplicationMessage.WARNING));
          return;
        }
      } else {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIJcrExplorerEditForm.msg.path-edit-permission", null,
            ApplicationMessage.WARNING));
        return;
      }
      editContainer.initPopupDriveBrowser(POPUP_SELECT_PATH_INPUT, driveName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public void doSelect(String selectField, Object value) throws Exception {
    String stValue = null;
    if ("/".equals(String.valueOf(value))) {
      stValue = "/";
    } else {
      if (String.valueOf(value).split(":/").length > 1) stValue = String.valueOf(value).split(":/")[1];
      else stValue = "/";
    }
    ((UIFormStringInput)findComponentById(selectField)).setValue(stValue);
    UIJcrExplorerEditContainer uiContainer = getParent();
    for (UIComponent uiChild : uiContainer.getChildren()) {
      if (uiChild.getId().equals(POPUP_SELECT_PATH_INPUT)) {
        UIPopupWindow uiPopup = uiContainer.getChildById(uiChild.getId());
        uiPopup.setRendered(false);
        uiPopup.setShow(false);
      }
    }
  }
}
