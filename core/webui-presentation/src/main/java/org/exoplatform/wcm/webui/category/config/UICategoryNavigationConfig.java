/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.category.config;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.category.UICategoryNavigationConstant;
import org.exoplatform.wcm.webui.category.UICategoryNavigationPortlet;
import org.exoplatform.wcm.webui.category.UICategoryNavigationUtils;
import org.exoplatform.wcm.webui.selector.page.UIPageSelector;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 28, 2009
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "system:/groovy/webui/form/UIForm.gtmpl",
                 events = {
                   @EventConfig(listeners = UICategoryNavigationConfig.SaveActionListener.class),
                   @EventConfig(listeners = UICategoryNavigationConfig.CancelActionListener.class),
                   @EventConfig(listeners = UICategoryNavigationConfig.ChangeRepositoryActionListener.class),
                   @EventConfig(listeners = UICategoryNavigationConfig.SelectTargetPathActionListener.class)
                 }
    )
public class UICategoryNavigationConfig extends UIForm implements UISelectable {

  /** The popup id. */
  private String popupId;

  /**
   * Instantiates a new uI category navigation config.
   *
   * @throws Exception the exception
   */
  public UICategoryNavigationConfig() throws Exception {
    PortletPreferences preferences = UICategoryNavigationUtils.getPortletPreferences();

    String preferenceRepository = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_REPOSITORY,
        "");
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    List<SelectItemOption<String>> repositories = new ArrayList<SelectItemOption<String>>() ;
    RepositoryEntry repositoryEntry = repositoryService.getCurrentRepository().getConfiguration();
    repositories.add(new SelectItemOption<String>(repositoryEntry.getName())) ;
    UIFormSelectBox repositoryFormSelectBox = new UIFormSelectBox(UICategoryNavigationConstant.REPOSITORY_FORM_SELECTBOX,
                                                                  UICategoryNavigationConstant.REPOSITORY_FORM_SELECTBOX,
                                                                  repositories);
    repositoryFormSelectBox.setValue(preferenceRepository);
    repositoryFormSelectBox.setOnChange("ChangeRepository");
    addUIFormInput(repositoryFormSelectBox);

    String preferenceTreeTitle = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_TITLE, "");
    addUIFormInput(new UIFormStringInput(UICategoryNavigationConstant.TREE_TITLE_FORM_STRING_INPUT,
                                         UICategoryNavigationConstant.TREE_TITLE_FORM_STRING_INPUT,
                                         preferenceTreeTitle));

    String preferenceTreeName = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    List<SelectItemOption<String>> trees = getTaxonomyTrees();
    UIFormSelectBox treeNameFormSelectBox = new UIFormSelectBox(UICategoryNavigationConstant.TREE_NAME_FORM_SELECTBOX,
                                                                UICategoryNavigationConstant.TREE_NAME_FORM_SELECTBOX,
                                                                trees);
    treeNameFormSelectBox.setValue(preferenceTreeName);
    addUIFormInput(treeNameFormSelectBox);

    String preferencePortletName = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_PORTLET_NAME, "");
    String preferenceTemplateCategory = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_TEMPLATE_CATEGORY, "");
    String preferenceTemplatePath = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_TEMPLATE_PATH, "");
    List<SelectItemOption<String>> templates = getTemplateList(preferencePortletName, preferenceTemplateCategory);
    UIFormSelectBox templateFormSelectBox = new UIFormSelectBox(UICategoryNavigationConstant.TEMPLATE_FORM_SELECTBOX,
                                                                UICategoryNavigationConstant.TEMPLATE_FORM_SELECTBOX,
                                                                templates);
    templateFormSelectBox.setValue(preferenceTemplatePath);
    addUIFormInput(templateFormSelectBox);

    String preferenceTargetPath = preferences.getValue(UICategoryNavigationConstant.PREFERENCE_TARGET_PAGE, "");
    UIFormInputSetWithAction targetPathFormInputSet =
        new UIFormInputSetWithAction(UICategoryNavigationConstant.TARGET_PATH_FORM_INPUT_SET);
    UIFormStringInput targetPathFormStringInput =
        new UIFormStringInput(UICategoryNavigationConstant.TARGET_PATH_FORM_STRING_INPUT,
                              UICategoryNavigationConstant.TARGET_PATH_FORM_STRING_INPUT,
                              preferenceTargetPath);
    targetPathFormStringInput.setReadOnly(true);
    targetPathFormInputSet.setActionInfo(UICategoryNavigationConstant.TARGET_PATH_FORM_STRING_INPUT,
                                         new String[] { "SelectTargetPath" });
    targetPathFormInputSet.addUIFormInput(targetPathFormStringInput);
    addChild(targetPathFormInputSet);

    setActions(new String[] {"Save", "Cancel"});
  }

  /**
   * Gets the popup id.
   *
   * @return the popup id
   */
  public String getPopupId() {
    return popupId;
  }

  /**
   * Sets the popup id.
   *
   * @param popupId the new popup id
   */
  public void setPopupId(String popupId) {
    this.popupId = popupId;
  }

  /**
   * Gets the template list.
   *
   * @param portletName the portlet name
   * @param templateCategory the template category
   *
   * @return the template list
   *
   * @throws Exception the exception
   */
  private List<SelectItemOption<String>> getTemplateList(String portletName, String templateCategory) throws Exception {
    List<SelectItemOption<String>> templates = new ArrayList<SelectItemOption<String>>();
    ApplicationTemplateManagerService appTemplateMngService = getApplicationComponent(ApplicationTemplateManagerService.class);
    List<Node> templateNodes = appTemplateMngService.getTemplatesByCategory(portletName,
                                                                            templateCategory,
                                                                            WCMCoreUtils.getUserSessionProvider());
    for (Node templateNode : templateNodes) {
      String templateName = templateNode.getName();
      String templatePath = templateNode.getPath();
      templates.add(new SelectItemOption<String>(templateName, templatePath));
    }
    return templates;
  }

  /**
   * Gets the taxonomy trees.
   *
   * @param repository the repository
   *
   * @return the taxonomy trees
   *
   * @throws Exception the exception
   */
  private List<SelectItemOption<String>> getTaxonomyTrees() throws Exception {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    LivePortalManagerService livePortalManagerService = getApplicationComponent(LivePortalManagerService.class);
    List<Node> taxonomyNodes = taxonomyService.getAllTaxonomyTrees();
    List<SelectItemOption<String>> taxonomyTrees = new ArrayList<SelectItemOption<String>>();
    for(Node taxonomyNode : taxonomyNodes) {
      Node portalNode = livePortalManagerService.getLivePortalByChild(taxonomyNode);
      if (portalNode != null)
        taxonomyTrees.add(new SelectItemOption<String>(taxonomyNode.getName(), taxonomyNode.getName()));
    }
    return taxonomyTrees;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    UIFormStringInput formStringInput = findComponentById(selectField);
    formStringInput.setValue(value.toString()) ;

    UICategoryNavigationPortlet categoryNavigationPortlet = getAncestorOfType(UICategoryNavigationPortlet.class);
    UIPopupContainer popupContainer = categoryNavigationPortlet.getChild(UIPopupContainer.class);
    Utils.closePopupWindow(popupContainer, popupId);
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener</code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SaveActionListener extends EventListener<UICategoryNavigationConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryNavigationConfig> event) throws Exception {
      UICategoryNavigationConfig categoryNavigationConfig = event.getSource();
      String preferenceRepository = categoryNavigationConfig.
          getUIFormSelectBox(UICategoryNavigationConstant.REPOSITORY_FORM_SELECTBOX).getValue();
      String preferenceTreeName = categoryNavigationConfig.
          getUIFormSelectBox(UICategoryNavigationConstant.TREE_NAME_FORM_SELECTBOX).getValue();
      String preferenceTreeTitle = categoryNavigationConfig.
          getUIStringInput(UICategoryNavigationConstant.TREE_TITLE_FORM_STRING_INPUT).getValue();
      if (preferenceTreeTitle == null)
        preferenceTreeTitle = "";
      String preferenceTargetPath = categoryNavigationConfig.
          getUIStringInput(UICategoryNavigationConstant.TARGET_PATH_FORM_STRING_INPUT).getValue();
      String preferenceTemplate = categoryNavigationConfig.
          getUIFormSelectBox(UICategoryNavigationConstant.TEMPLATE_FORM_SELECTBOX).getValue();
      PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
      portletPreferences.setValue(UICategoryNavigationConstant.PREFERENCE_REPOSITORY, preferenceRepository);
      portletPreferences.setValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, preferenceTreeName);
      portletPreferences.setValue(UICategoryNavigationConstant.PREFERENCE_TREE_TITLE, preferenceTreeTitle);
      portletPreferences.setValue(UICategoryNavigationConstant.PREFERENCE_TARGET_PAGE, preferenceTargetPath);
      portletPreferences.setValue(UICategoryNavigationConstant.PREFERENCE_TEMPLATE_PATH, preferenceTemplate);
      portletPreferences.store();

      if (!Utils.isEditPortletInCreatePageWizard()) {
        PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
        UIPortal uiPortal = Util.getUIPortal();

        NodeURL nodeURL = portalRequestContext.createURL(NodeURL.TYPE);
        String uri = nodeURL.setNode(uiPortal.getSelectedUserNode()).toString();

        ((PortletRequestContext)event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
        Utils.closePopupWindow(categoryNavigationConfig, UICategoryNavigationPortlet.CONFIG_POPUP_WINDOW);
        RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
        requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + uri + "');");
      } else {
        if (Utils.isQuickEditMode(categoryNavigationConfig,
                                  UICategoryNavigationPortlet.CONFIG_POPUP_WINDOW)) {
          Utils.closePopupWindow(categoryNavigationConfig,
                                 UICategoryNavigationPortlet.CONFIG_POPUP_WINDOW);
        } else {
          Utils.createPopupMessage(categoryNavigationConfig,
                                   "UICategoryNavigationConfig.msg.saving-success",
                                   null,
                                   ApplicationMessage.INFO);
        }
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener</code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class CancelActionListener extends EventListener<UICategoryNavigationConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryNavigationConfig> event) throws Exception {
      UICategoryNavigationConfig viewerManagementForm = event.getSource();
      Utils.closePopupWindow(viewerManagementForm, UICategoryNavigationPortlet.CONFIG_POPUP_WINDOW);
      ((PortletRequestContext)event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
    }
  }


  /**
   * The listener interface for receiving changeRepositoryAction events.
   * The class that is interested in processing a changeRepositoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeRepositoryActionListener</code> method. When
   * the changeRepositoryAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ChangeRepositoryActionListener extends EventListener<UICategoryNavigationConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryNavigationConfig> event) throws Exception {
    }
  }

  /**
   * The listener interface for receiving selectTargetPathAction events.
   * The class that is interested in processing a selectTargetPathAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectTargetPathActionListener</code> method. When
   * the selectTargetPathAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SelectTargetPathActionListener extends EventListener<UICategoryNavigationConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICategoryNavigationConfig> event) throws Exception {
      UICategoryNavigationConfig categoryNavigationConfig = event.getSource();
      UICategoryNavigationPortlet categoryNavigationPortlet = categoryNavigationConfig.
          getAncestorOfType(UICategoryNavigationPortlet.class);
      UIPopupContainer popupContainer = categoryNavigationPortlet.getChild(UIPopupContainer.class);
      UIPageSelector pageSelector = popupContainer.createUIComponent(UIPageSelector.class, null, null);
      pageSelector.setSourceComponent(categoryNavigationConfig,
                                      new String[] { UICategoryNavigationConstant.TARGET_PATH_FORM_STRING_INPUT });
      Utils.createPopupWindow(popupContainer,
                              pageSelector,
                              UICategoryNavigationConstant.TARGET_PATH_SELECTOR_POPUP_WINDOW,
                              700);
      categoryNavigationConfig.setPopupId(UICategoryNavigationConstant.TARGET_PATH_SELECTOR_POPUP_WINDOW);
    }
  }
}
