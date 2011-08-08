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
package org.exoplatform.ecm.webui.component.browsecontent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 19, 2006 9:24:36 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIConfigTabPane extends UIContainer {

  final public static String PATH_SELECTOR = "pathSelector";
  final public static String DOCUMENT_SELECTOR = "documentSelector";
  final public static String CONFIGTYPE = "configType";

  private boolean isNewConfig_ = false;
  private boolean isChanged_ = false;

  public UIConfigTabPane() throws Exception {
    addChild(UINewConfigForm.class, null, null).setRendered(false);
    addChild(UIConfigContainer.class, null, null);
  }

  protected boolean isNewConfig() { return isNewConfig_; }
  protected void setNewConfig(boolean isNew) { this.isNewConfig_ = isNew; }

  public List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    String[] workspaceNames = getApplicationComponent(RepositoryService.class)
    .getCurrentRepository().getWorkspaceNames();
    for(String workspace:workspaceNames) {
      Options.add(new SelectItemOption<String>(workspace,workspace));
    }
    return Options;
  }

  @SuppressWarnings("unchecked")
  @Deprecated
  public List<SelectItemOption<String>> getBoxTemplateOption(String repository) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    List<Node> docTemplates = getApplicationComponent(ManageViewService.class)
    .getAllTemplates(BasePath.CB_DETAIL_VIEW_TEMPLATES, SessionProviderFactory.createSystemProvider());
    for(Node template: docTemplates) {
      options.add(new SelectItemOption<String>(template.getName(), template.getName()));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    return options;
  }
  
  @SuppressWarnings("unchecked")
  public List<SelectItemOption<String>> getBoxTemplateOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    List<Node> docTemplates = getApplicationComponent(ManageViewService.class)
    .getAllTemplates(BasePath.CB_DETAIL_VIEW_TEMPLATES, SessionProviderFactory.createSystemProvider());
    for(Node template: docTemplates) {
      options.add(new SelectItemOption<String>(template.getName(), template.getName()));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    return options;
  }  

  public void setIsChangeValue(boolean isChanged) { isChanged_ = isChanged; }

  public void getCurrentConfig() throws Exception {
    PortletPreferences preference = getAncestorOfType(UIBrowseContentPortlet.class).getPortletPreferences();
    UINewConfigForm uiConfigForm = getChild(UINewConfigForm.class);
    uiConfigForm.setRendered(false);
    UIConfigContainer uiConfigContainer = getChild(UIConfigContainer.class);
    String repository = preference.getValue(Utils.REPOSITORY, "");
    String usecase = preference.getValue(Utils.CB_USECASE, "");
    String workspace = preference.getValue(Utils.WORKSPACE_NAME, "");
    if (usecase.equals(Utils.CB_USE_FROM_PATH)) {
      UIPathConfig uiPathConfig = uiConfigContainer.getChild(UIPathConfig.class);
      if(uiPathConfig == null) {
        uiPathConfig = uiConfigContainer.addChild(UIPathConfig.class, null, null);
      }
      if(!isChanged_) uiPathConfig.initForm(preference, repository, workspace, false);
      uiConfigContainer.setRenderedChild(UIPathConfig.class);
    } else if (usecase.equals(Utils.CB_USE_JCR_QUERY)) {
      UIQueryConfig uiQueryConfig = uiConfigContainer.getChild(UIQueryConfig.class);
      if(uiQueryConfig == null) {
        uiQueryConfig = uiConfigContainer.addChild(UIQueryConfig.class, null, null);
      }
      uiQueryConfig.initForm(preference, repository, workspace, false);
      uiConfigContainer.setRenderedChild(UIQueryConfig.class);
    } else if (usecase.equals(Utils.CB_USE_SCRIPT)) {
      UIScriptConfig uiScriptConfig = uiConfigContainer.getChild(UIScriptConfig.class);
      if(uiScriptConfig == null) {
        uiScriptConfig = uiConfigContainer.addChild(UIScriptConfig.class, null, null);

      }
      uiScriptConfig.initForm(preference, repository, workspace, false);
      uiConfigContainer.setRenderedChild(UIScriptConfig.class);
    } else if (usecase.equals(Utils.CB_USE_DOCUMENT)) {
      UIDocumentConfig uiDocumentConfig = uiConfigContainer.getChild(UIDocumentConfig.class);
      if(uiDocumentConfig == null) {
        uiDocumentConfig = uiConfigContainer.addChild(UIDocumentConfig.class, null, null);
      }
      uiDocumentConfig.initForm(preference, repository, workspace, false);
      uiConfigContainer.setRenderedChild(UIDocumentConfig.class);
    }
    uiConfigContainer.setRendered(true);
  }


  public void showNewConfigForm(boolean isAddNew) throws Exception {
    UINewConfigForm uiConfigForm = getChild(UINewConfigForm.class);
    UIConfigContainer uiConfigContainer = getChild(UIConfigContainer.class);
    if(isAddNew) uiConfigForm.resetForm();
    uiConfigForm.setRendered(true);
    uiConfigContainer.setRendered(false);
  }

  public void initNewConfig(String usercase, String repository, String workSpace) throws Exception {
    UINewConfigForm uiConfigForm = getChild(UINewConfigForm.class);
    uiConfigForm.setRendered(false);
    UIConfigContainer uiConfigContainer = getChild(UIConfigContainer.class);
    uiConfigContainer.initNewConfig(usercase, repository, workSpace);
    uiConfigContainer.setRendered(true);
  }

  public void initPopupPathSelect(UIForm uiForm, String repo, String workSpace, String fieldName,
      boolean isDisable) throws Exception {
    removeChildById(PATH_SELECTOR);
    removeChildById(DOCUMENT_SELECTOR);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, PATH_SELECTOR);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(610, 300);
    UIOneNodePathSelector uiOneNodePathSelector =
      createUIComponent(UIOneNodePathSelector.class, null, null);
    if(isDisable) uiOneNodePathSelector.setIsDisable(workSpace, true);
    String[] filterType = {Utils.NT_FOLDER, Utils.NT_UNSTRUCTURED, "exo:taxonomy"};
    uiOneNodePathSelector.setAcceptedNodeTypesInPathPanel(filterType);
    uiOneNodePathSelector.setExceptedNodeTypesInPathPanel(new String[] {});
    uiOneNodePathSelector.setShowRootPathSelect(true);
    uiOneNodePathSelector.setRootNodeLocation(repo, workSpace, "/");
    if(SessionProviderFactory.isAnonim()) {
      uiOneNodePathSelector.init(SessionProviderFactory.createAnonimProvider());
    } else {
      uiOneNodePathSelector.init(SessionProviderFactory.createSessionProvider());
    }
    uiPopup.setUIComponent(uiOneNodePathSelector);
    uiOneNodePathSelector.setSourceComponent(uiForm, new String[] {fieldName});
    uiPopup.setShow(true);
  }

  public void initPopupPathSelect(UIForm uiForm, String repo, String workSpace, String fieldName,
      boolean isDisable, boolean isAllowPublish) throws Exception {
    removeChildById(PATH_SELECTOR);
    removeChildById(DOCUMENT_SELECTOR);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, PATH_SELECTOR);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(610, 300);
    UIOneNodePathSelector uiOneNodePathSelector =
      createUIComponent(UIOneNodePathSelector.class, null, null);
    if(isDisable) uiOneNodePathSelector.setIsDisable(workSpace, true);
    String[] filterType = {Utils.NT_FOLDER, Utils.NT_UNSTRUCTURED, "exo:taxonomy"};
    uiOneNodePathSelector.setAcceptedNodeTypesInPathPanel(filterType);
    uiOneNodePathSelector.setAllowPublish(isAllowPublish);
    uiOneNodePathSelector.setShowRootPathSelect(true);
    uiOneNodePathSelector.setRootNodeLocation(repo, workSpace, "/");
    if(SessionProviderFactory.isAnonim()) {
      uiOneNodePathSelector.init(SessionProviderFactory.createAnonimProvider());
    } else {
      uiOneNodePathSelector.init(SessionProviderFactory.createSessionProvider());
    }
    uiPopup.setUIComponent(uiOneNodePathSelector);
    uiOneNodePathSelector.setSourceComponent(uiForm, new String[] {fieldName});
    uiPopup.setShow(true);
  }

  @SuppressWarnings("unchecked")
  public void initPopupDocumentSelect(UIForm uiForm,
                                      String repo,
                                      String workSpace,
                                      String path,
                                      boolean isAllowPublish) throws Exception {
    removeChildById(PATH_SELECTOR);
    removeChildById(DOCUMENT_SELECTOR);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, DOCUMENT_SELECTOR);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(610, 300);
    UIOneNodePathSelector uiOneNodePathSelector =
      createUIComponent(UIOneNodePathSelector.class, null, null);
    uiOneNodePathSelector.setIsDisable(workSpace, true);
    uiOneNodePathSelector.setRootNodeLocation(repo, workSpace, path);
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    List<String> documents = templateService.getDocumentTemplates();
    String [] filterType = new String[documents.size()];
    documents.toArray(filterType);
    uiOneNodePathSelector.setAcceptedNodeTypesInPathPanel(filterType);
    uiOneNodePathSelector.setAllowPublish(isAllowPublish);
    if (SessionProviderFactory.isAnonim()) {
      uiOneNodePathSelector.init(SessionProviderFactory.createAnonimProvider());
    } else {
      uiOneNodePathSelector.init(SessionProviderFactory.createSessionProvider());
    }
    uiPopup.setUIComponent(uiOneNodePathSelector);
    uiOneNodePathSelector.setSourceComponent(uiForm, new String[] {UINewConfigForm.FIELD_DOCNAME});
    uiPopup.setShow(true);
  }
}
