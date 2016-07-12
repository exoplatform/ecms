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
package org.exoplatform.ecm.webui.component.admin.metadata;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.wcm.webui.form.UIFormInputSetWithNoLabel;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Sep 19, 2006 5:31:04 PM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormTabPane.gtmpl", events = {
    @EventConfig(listeners = UIMetadataForm.SaveActionListener.class),
    @EventConfig(listeners = UIMetadataForm.CancelActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIMetadataForm.AddPermissionActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIMetadataForm.SelectTabActionListener.class, phase = Phase.DECODE) })
public class UIMetadataForm extends UIFormTabPane implements UISelectable {

  final static public String METADATA_PATH    = "metadataPath";

  final static public String MAPPING          = "mapping";

  final static public String METADATA_MAPPING = "metadataMapping";

  final static public String NT_UNSTRUCTURED  = "nt:unstructured";

  final static public String DIALOG_TEMPLATE  = "dialogTemplate";

  final static public String VIEW_TEMPLATE    = "viewTemplate";

  final static public String METADATA_NAME    = "metadataName";

  final static public String METADATA_LABEL   = "metadataLabel";

  final static public String VIEW_PERMISSION  = "viewPermission";

  final static public String METADATA_TAB     = "metadataTypeTab";

  final static public String DIALOG_TAB       = "dialogTab";

  final static public String VIEW_TAB         = "viewTab";

  private boolean            isAddNew_        = false;

  private String             metadataName_;

  private String             workspaceName_;

  public UIMetadataForm() throws Exception {
    super("UIMetadataForm");
    UIFormInputSetWithAction uiMetadataType = new UIFormInputSetWithAction(METADATA_TAB);
    uiMetadataType.addUIFormInput(new UIFormStringInput(METADATA_NAME, METADATA_NAME, null));
    uiMetadataType.addUIFormInput(new UIFormStringInput(VIEW_PERMISSION, VIEW_PERMISSION, null).addValidator(MandatoryValidator.class)
                                                                                               .setDisabled(true));
    uiMetadataType.addUIFormInput(new UIFormStringInput(METADATA_LABEL, METADATA_LABEL, null));
    uiMetadataType.setActionInfo(VIEW_PERMISSION, new String[] { "AddPermission" });
    addUIComponentInput(uiMetadataType);
    setSelectedTab(uiMetadataType.getId());
    UIFormInputSetWithNoLabel uiDialogTab = new UIFormInputSetWithNoLabel(DIALOG_TAB);
    uiDialogTab.addUIFormInput(new UIFormTextAreaInput(DIALOG_TEMPLATE, DIALOG_TEMPLATE, null));
    addUIComponentInput(uiDialogTab);
    UIFormInputSetWithNoLabel uiViewTab = new UIFormInputSetWithNoLabel(VIEW_TAB);
    uiViewTab.addUIFormInput(new UIFormTextAreaInput(VIEW_TEMPLATE, VIEW_TEMPLATE, null));
    addUIComponentInput(uiViewTab);
    setActions(new String[] { "Save", "Cancel" });
  }

  public void doSelect(String selectField, Object value) {
    getUIStringInput(VIEW_PERMISSION).setValue(value.toString());
    UIMetadataManager uiManager = getAncestorOfType(UIMetadataManager.class);
    uiManager.removeChildById(UIMetadataManager.PERMISSION_POPUP);
  }

  public void update(String metadata) throws Exception {
    metadataName_ = metadata;
    MetadataService metadataService = getApplicationComponent(MetadataService.class);
    workspaceName_ = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceWorkspace();
    getUIStringInput(METADATA_NAME).setValue(metadata);
    getUIStringInput(METADATA_LABEL).setValue(metadataService.getMetadataLabel(metadata));
    String dialogTemplate = metadataService.getMetadataTemplate(metadata, true);
    String viewTemplate = metadataService.getMetadataTemplate(metadata, false);
    String role = metadataService.getMetadataRoles(metadata, true);
    getUIStringInput(METADATA_NAME).setDisabled(true);
    getUIStringInput(VIEW_PERMISSION).setValue(role);
    getUIFormTextAreaInput(DIALOG_TEMPLATE).setValue(dialogTemplate);
    getUIFormTextAreaInput(VIEW_TEMPLATE).setValue(viewTemplate);
  }

  static public class SaveActionListener extends EventListener<UIMetadataForm> {
    public void execute(Event<UIMetadataForm> event) throws Exception {
      UIMetadataForm uiForm = event.getSource();
      UIMetadataManager uiMetaManager = uiForm.getAncestorOfType(UIMetadataManager.class);
      MetadataService metadataService = uiForm.getApplicationComponent(MetadataService.class);
      String roles = uiForm.getUIStringInput(VIEW_PERMISSION).getValue();
      String dialogTemplate = uiForm.getUIFormTextAreaInput(DIALOG_TEMPLATE).getValue();
      if (dialogTemplate == null)
        dialogTemplate = "";
      String viewTemplate = uiForm.getUIFormTextAreaInput(VIEW_TEMPLATE).getValue();
      if (viewTemplate == null)
        viewTemplate = "";
      if (!metadataService.hasMetadata(uiForm.metadataName_))
        uiForm.isAddNew_ = true;
      else
        uiForm.isAddNew_ = false;
      String metaLabel = HTMLSanitizer.sanitize(uiForm.getUIStringInput(METADATA_LABEL).getValue());
      JCRResourceResolver resourceResolver = new JCRResourceResolver(uiForm.workspaceName_);
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class);
      String path = metadataService.addMetadata(uiForm.metadataName_, true, roles, dialogTemplate, metaLabel, uiForm.isAddNew_);
      if (path != null)
        templateService.invalidateTemplate(path, resourceResolver);
      path = metadataService.addMetadata(uiForm.metadataName_, false, roles, viewTemplate, metaLabel, uiForm.isAddNew_);
      if (path != null)
        templateService.invalidateTemplate(path, resourceResolver);
      uiForm.reset();
      uiMetaManager.getChild(UIMetadataList.class).refresh(1);
      uiMetaManager.removeChildById(UIMetadataManager.METADATA_POPUP);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMetaManager);
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIMetadataForm> {
    public void execute(Event<UIMetadataForm> event) throws Exception {
      UIMetadataForm uiView = event.getSource();
      UIMetadataManager uiManager = uiView.getAncestorOfType(UIMetadataManager.class);
      String membership = uiView.getUIStringInput(VIEW_PERMISSION).getValue();
      uiManager.initPopupPermission(membership);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class CancelActionListener extends EventListener<UIMetadataForm> {
    public void execute(Event<UIMetadataForm> event) throws Exception {
      UIMetadataForm uiView = event.getSource();
      UIMetadataManager uiMetaManager = uiView.getAncestorOfType(UIMetadataManager.class);
      uiMetaManager.removeChildById(UIMetadataManager.METADATA_POPUP);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMetaManager);
    }
  }

  static public class SelectTabActionListener extends EventListener<UIMetadataForm> {
    public void execute(Event<UIMetadataForm> event) throws Exception {
      UIMetadataForm uiView = event.getSource();
      UIMetadataManager uiMetaManager = uiView.getAncestorOfType(UIMetadataManager.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMetaManager);
    }
  }
}
