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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL Author : Tran The Trong trongtt@gmail.com
 * July 3, 2006 10:07:15 AM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
    @EventConfig(listeners = UITemplateForm.SaveActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UITemplateForm.CancelActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UITemplateForm.ResetActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UITemplateForm.ChangeActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UITemplateForm.RestoreActionListener.class) })
public class UITemplateForm extends UIForm {

  final static private String FIELD_VERSION       = "version";

  final static private String FIELD_CONTENT       = "content";

  final static private String FIELD_NAME          = "name";

  final static private String FIELD_HOMETEMPLATE  = "homeTemplate";

  final static private String FIELD_ENABLEVERSION = "enableVersion";

  private Node                template_           = null;

  private List<String>        listVersion         = new ArrayList<String>();

  private Version             baseVersion_;

  private VersionNode         selectedVersion_;

  public boolean              isAddNew_           = false;

  private String              templatePath_;

  private VersionNode         rootVersionNode;

  private org.exoplatform.services.cms.templates.TemplateService     templateService;

  public UITemplateForm() throws Exception {
    templateService = WCMCoreUtils.getService(org.exoplatform.services.cms.templates.TemplateService.class);
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    UIFormSelectBox versions = new UIFormSelectBox(FIELD_VERSION, FIELD_VERSION, options);
    versions.setOnChange("Change");
    versions.setRendered(false);
    addUIFormInput(versions);
    addUIFormInput(new UIFormTextAreaInput(FIELD_CONTENT, FIELD_CONTENT, null)
        .addValidator(MandatoryValidator.class));
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(
        MandatoryValidator.class).addValidator(ECMNameValidator.class));
    List<SelectItemOption<String>> typeList = new ArrayList<SelectItemOption<String>>();
    addUIFormInput(new UIFormSelectBox(FIELD_HOMETEMPLATE, FIELD_HOMETEMPLATE, typeList));
    UIFormCheckBoxInput enableVersion = new UIFormCheckBoxInput<Boolean>(FIELD_ENABLEVERSION,
        FIELD_ENABLEVERSION, null);
    enableVersion.setRendered(false);
    addUIFormInput(enableVersion);
  }

  public String getRepository() {
    try {
      return getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                             .getConfiguration()
                                                             .getName();
    } catch (RepositoryException e) {
      return null;
    }
  }

  public void updateOptionList() throws Exception {
    getUIFormSelectBox(FIELD_HOMETEMPLATE).setOptions(getOptionList());
  }

  public List<SelectItemOption<String>> getOptionList() throws Exception {
    List<SelectItemOption<String>> typeList = new ArrayList<SelectItemOption<String>>();
    SessionProvider provider = SessionProviderFactory.createSessionProvider();
    if (getId().equalsIgnoreCase("ECMTempForm")) {
      Node ecmTemplateHome = getApplicationComponent(ManageViewService.class).getTemplateHome(
          BasePath.ECM_EXPLORER_TEMPLATES, provider);
      if (ecmTemplateHome != null) {
        typeList.add(new SelectItemOption<String>(ecmTemplateHome.getName(), ecmTemplateHome
            .getPath()));
      }
    } else {
      Node cbTemplateHome = getApplicationComponent(ManageViewService.class).getTemplateHome(
          BasePath.CONTENT_BROWSER_TEMPLATES, provider);
      if (cbTemplateHome != null) {
        NodeIterator iter = cbTemplateHome.getNodes();
        while (iter.hasNext()) {
          Node template = iter.nextNode();
          typeList.add(new SelectItemOption<String>(template.getName(), template.getPath()));
        }
      }
    }
    return typeList;
  }

  public boolean canEnableVersionning(Node node) throws Exception {
    return node.canAddMixin(Utils.MIX_VERSIONABLE);
  }

  private boolean isVersioned(Node node) throws RepositoryException {
    return node.isNodeType(Utils.MIX_VERSIONABLE);
  }

  private VersionNode getRootVersion(Node node) throws Exception {
    VersionHistory vH = node.getVersionHistory();
    return (vH != null) ? new VersionNode(vH.getRootVersion(), node.getSession()) : null;
  }

  private List<String> getNodeVersions(List<VersionNode> children) throws Exception {
    List<VersionNode> child = new ArrayList<VersionNode>();
    for (VersionNode vNode : children) {
      listVersion.add(vNode.getName());
      child = vNode.getChildren();
      if (!child.isEmpty())
        getNodeVersions(child);
    }
    return listVersion;
  }

  private List<SelectItemOption<String>> getVersionValues(Node node) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    List<VersionNode> children = getRootVersion(node).getChildren();
    listVersion.clear();
    List<String> versionList = getNodeVersions(children);
    for (int i = 0; i < versionList.size(); i++) {
      for (int j = i + 1; j < versionList.size(); j++) {
        if (Integer.parseInt(versionList.get(j)) < Integer.parseInt(versionList.get(i))) {
          String temp = versionList.get(i);
          versionList.set(i, versionList.get(j));
          versionList.set(j, temp);
        }
      }
      options.add(new SelectItemOption<String>(versionList.get(i), versionList.get(i)));
    }
    return options;
  }

  public void refresh() throws Exception {
    UIFormSelectBox versionField = getUIFormSelectBox(FIELD_VERSION);
    if (isAddNew_) {
      versionField.setRendered(false);
      getUIFormTextAreaInput(FIELD_CONTENT).setValue(null);
      getUIStringInput(FIELD_NAME).setEditable(true).setValue(null);
      getUIFormSelectBox(FIELD_HOMETEMPLATE).setValue(null);
      getUIFormSelectBox(FIELD_HOMETEMPLATE).setDisabled(false);
      getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setRendered(false);
      template_ = null;
      selectedVersion_ = null;
      baseVersion_ = null;
      return;
    }
    update(template_.getPath(), null);
  }

  public void update(String templatePath, VersionNode selectedVersion) throws Exception {
    if (templatePath != null) {
      templatePath_ = templatePath;
      template_ = getApplicationComponent(ManageViewService.class).getTemplate(templatePath,
                                                                               SessionProviderFactory.createSessionProvider());
      getUIStringInput(FIELD_NAME).setValue(template_.getName());
      getUIStringInput(FIELD_NAME).setEditable(false);
      String value = templatePath.substring(0, templatePath.lastIndexOf("/"));
      getUIFormSelectBox(FIELD_HOMETEMPLATE).setValue(value);
      getUIFormSelectBox(FIELD_HOMETEMPLATE).setDisabled(false);
      getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setRendered(true);
      if (isVersioned(template_)) {
        rootVersionNode = getRootVersion(template_);
        baseVersion_ = template_.getBaseVersion();
        List<SelectItemOption<String>> options = getVersionValues(template_);
        getUIFormSelectBox(FIELD_VERSION).setOptions(options).setRendered(true);
        getUIFormSelectBox(FIELD_VERSION).setValue(baseVersion_.getName());
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setChecked(true).setEnable(false);
        if (options.size() > 1)
          setActions(new String[] { "Save", "Reset", "Restore", "Cancel" });
        else
          setActions(new String[] { "Save", "Reset", "Cancel" });
      } else if (canEnableVersionning(template_)) {
        getUIFormSelectBox(FIELD_VERSION).setRendered(false);
        getUIFormCheckBoxInput(FIELD_ENABLEVERSION).setChecked(false).setEditable(true);
      }
    }
    if (selectedVersion != null) {
      template_.restore(selectedVersion.getVersion(), false);
      selectedVersion_ = selectedVersion;
      Object[] args = { getUIStringInput(FIELD_VERSION).getValue() };
      UIApplication app = getAncestorOfType(UIApplication.class);
      app.addMessage(new ApplicationMessage("UITemplateForm.msg.version-restored", args));
    }
    String content = templateService.getTemplate(template_);
    getUIFormTextAreaInput(FIELD_CONTENT).setValue(Utils.encodeHTML(content));
  }

  static public class SaveActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource();
      String templateName = uiForm.getUIStringInput(FIELD_NAME).getValue().trim();
      String content = uiForm.getUIFormTextAreaInput(FIELD_CONTENT).getValue();
      String homeTemplate = uiForm.getUIFormSelectBox(FIELD_HOMETEMPLATE).getValue();
      UITemplateContainer uiTempContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      ManageViewService manageViewService = uiForm.getApplicationComponent(ManageViewService.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if (homeTemplate == null) {
        String tempPath = uiForm.template_.getPath();
        homeTemplate = tempPath.substring(0, tempPath.lastIndexOf("/"));
      }
      boolean isEnableVersioning = uiForm.getUIFormCheckBoxInput(FIELD_ENABLEVERSION).isChecked();
      String path = null;
      if (uiForm.getId().equalsIgnoreCase(UIECMTemplateList.ST_ECMTempForm)) {
        List<Node> ecmTemps = manageViewService.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES,
                                                                SessionProviderFactory.createSessionProvider());
        for (Node temp : ecmTemps) {
          if (temp.getName().equals(templateName) && uiForm.isAddNew_) {
            Object[] args = { templateName };
            uiApp.addMessage(new ApplicationMessage("UITemplateForm.msg.template-name-exist", args,
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            return;
          }
        }
      } else if (uiForm.getId().equalsIgnoreCase(UICBTemplateList.ST_CBTempForm)) {
        if (uiForm.isAddNew_) {
          UICBTemplateList uiCBTempList = uiTempContainer.getChild(UICBTemplateList.class);
          List<Node> cbTemps = uiCBTempList.getAllTemplates();
          for (Node temp : cbTemps) {
            if (temp.getName().equals(templateName)) {
              Object[] args = { templateName };
              uiApp.addMessage(new ApplicationMessage("UITemplateForm.msg.template-name-exist",
                  args, ApplicationMessage.WARNING));
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
              return;
            }
          }
        }
      }
      if (uiForm.isAddNew_) {
        if (uiForm.templatePath_ != null) {
          String oldHomeTemplate = uiForm.templatePath_.substring(0, uiForm.templatePath_
              .lastIndexOf("/"));
          if (!oldHomeTemplate.equals(homeTemplate)) {
            Node oldNode = manageViewService.getTemplate(uiForm.templatePath_,
                                                         SessionProviderFactory.createSessionProvider());
            oldNode.remove();
            manageViewService.getTemplate(oldHomeTemplate,
                                          SessionProviderFactory.createSessionProvider()).save();
          }
        }
        path = manageViewService.addTemplate(templateName, content, homeTemplate);
        uiForm.template_ = manageViewService.getTemplate(path, SessionProviderFactory
            .createSessionProvider());
      } else {
        if (isEnableVersioning) {
          if (!uiForm.template_.isNodeType(Utils.MIX_VERSIONABLE)) {
            uiForm.template_.addMixin(Utils.MIX_VERSIONABLE);
            uiForm.template_.save();
          } else {
            uiForm.template_.checkout();
          }
        }
        path = manageViewService.updateTemplate(templateName, content, homeTemplate);
        uiForm.template_.save();
        if (isEnableVersioning) {
          uiForm.template_.checkin();
        }
      }
      String workspaceName = uiForm.template_.getSession().getWorkspace().getName();
      JCRResourceResolver resourceResolver = new JCRResourceResolver(workspaceName);
      TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class);
      if (path != null)
        templateService.invalidateTemplate(path, resourceResolver);
      uiForm.refresh();
      if (uiForm.getId().equalsIgnoreCase(UIECMTemplateList.ST_ECMTempForm)) {
        UIECMTemplateList uiECMTempList = uiTempContainer.getChild(UIECMTemplateList.class);
        uiECMTempList.updateTempListGrid(uiECMTempList.getUIPageIterator().getCurrentPage());
        uiECMTempList.setRenderSibling(UIECMTemplateList.class);
      }
      if (uiForm.getId().equalsIgnoreCase(UICBTemplateList.ST_CBTempForm)) {
        UICBTemplateList uiCBTempList = uiTempContainer.getChild(UICBTemplateList.class);
        uiCBTempList.updateCBTempListGrid(uiCBTempList.getUIPageIterator().getCurrentPage());
        uiCBTempList.setRenderSibling(UICBTemplateList.class);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTempContainer);
    }
  }

  static public class CancelActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource();
      uiForm.refresh();
      UITemplateContainer uiTemplateContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      if (uiForm.isAddNew_) {
        uiTemplateContainer.removeChildById(UIECMTemplateList.ST_ECMTempForm + "Add");
        uiTemplateContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Add");
      } else {
        uiTemplateContainer.removeChildById(UIECMTemplateList.ST_ECMTempForm + "Edit");
        uiTemplateContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Edit");
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplateContainer);
    }
  }

  static public class ResetActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource();
      UITemplateContainer uiTempContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      if (uiForm.selectedVersion_ != null) {
        if (!uiForm.selectedVersion_.equals(uiForm.baseVersion_)) {
          uiForm.template_.restore(uiForm.baseVersion_, true);
          uiForm.template_.checkout();
        }
      }
      uiForm.refresh();
      if (uiForm.getId().equalsIgnoreCase(UIECMTemplateList.ST_ECMTempForm)) {
        UIECMTemplateList uiECMTempList = uiTempContainer.getChild(UIECMTemplateList.class);
        uiECMTempList.updateTempListGrid(uiECMTempList.getUIPageIterator().getCurrentPage());
      }
      if (uiForm.getId().equalsIgnoreCase(UICBTemplateList.ST_CBTempForm)) {
        UICBTemplateList uiCBTempList = uiTempContainer.getChild(UICBTemplateList.class);
        uiCBTempList.updateCBTempListGrid(uiCBTempList.getUIPageIterator().getCurrentPage());
      }
    }
  }

  static public class ChangeActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource();
      String version = uiForm.getUIFormSelectBox(FIELD_VERSION).getValue();
      String path = uiForm.template_.getVersionHistory().getVersion(version).getPath();
      VersionNode versionNode = uiForm.rootVersionNode.findVersionNode(path);
      Node frozenNode = versionNode.getVersion().getNode(Utils.JCR_FROZEN);
      String content = uiForm.templateService.getTemplate(frozenNode);
      uiForm.getUIFormTextAreaInput(FIELD_CONTENT).setValue(content);
      UITemplateContainer uiTempContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      if (uiForm.getId().equalsIgnoreCase(UIECMTemplateList.ST_ECMTempForm)) {
        UIECMTemplateList uiECMTempList = uiTempContainer.getChild(UIECMTemplateList.class);
        uiECMTempList.updateTempListGrid(uiECMTempList.getUIPageIterator().getCurrentPage());
      }
      if (uiForm.getId().equalsIgnoreCase(UICBTemplateList.ST_CBTempForm)) {
        UICBTemplateList uiCBTempList = uiTempContainer.getChild(UICBTemplateList.class);
        uiCBTempList.updateCBTempListGrid(uiCBTempList.getUIPageIterator().getCurrentPage());
      }
    }
  }

  static public class RestoreActionListener extends EventListener<UITemplateForm> {
    public void execute(Event<UITemplateForm> event) throws Exception {
      UITemplateForm uiForm = event.getSource();
      String version = uiForm.getUIFormSelectBox(FIELD_VERSION).getValue();
      String path = uiForm.template_.getVersionHistory().getVersion(version).getPath();
      VersionNode selectedVesion = uiForm.rootVersionNode.findVersionNode(path);
      if (uiForm.baseVersion_.getName().equals(selectedVesion.getName()))
        return;
      uiForm.update(null, selectedVesion);
      UITemplateContainer uiTempContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      if (uiForm.getId().equalsIgnoreCase(UIECMTemplateList.ST_ECMTempForm)) {
        UIECMTemplateList uiECMTempList = uiTempContainer.getChild(UIECMTemplateList.class);
        uiECMTempList.updateTempListGrid(uiECMTempList.getUIPageIterator().getCurrentPage());
      }
      if (uiForm.getId().equalsIgnoreCase(UICBTemplateList.ST_CBTempForm)) {
        UICBTemplateList uiCBTempList = uiTempContainer.getChild(UICBTemplateList.class);
        uiCBTempList.updateCBTempListGrid(uiCBTempList.getUIPageIterator().getCurrentPage());
      }

      uiForm.refresh();
      UITemplateContainer uiTemplateContainer = uiForm.getAncestorOfType(UITemplateContainer.class);
      if (uiForm.isAddNew_) {
        uiTemplateContainer.removeChildById(UIECMTemplateList.ST_ECMTempForm + "Add");
        uiTemplateContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Add");
      } else {
        uiTemplateContainer.removeChildById(UIECMTemplateList.ST_ECMTempForm + "Edit");
        uiTemplateContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Edit");
      }
    }
  }
}
