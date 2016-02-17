/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.search.config;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.search.UIWCMSearchPortlet;
import org.exoplatform.wcm.webui.selector.page.UIPageSelector;
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
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/groovy/webui/search/config/UISearchPageLayoutManager.gtmpl",
  events = {
    @EventConfig(listeners = UISearchPageLayoutManager.SaveActionListener.class),
    @EventConfig(listeners = UISearchPageLayoutManager.SelectBasePathActionListener.class),
    @EventConfig(listeners = UISearchPageLayoutManager.CancelActionListener.class)
  }
)
public class UISearchPageLayoutManager extends UIForm  implements UISelectable {

  /** The Constant PORTLET_NAME. */
  public static final String PORTLET_NAME                       = "search";

  /** The Constant SEARCH_PAGE_LAYOUT_CATEGORY. */
  public static final String SEARCH_PAGE_LAYOUT_CATEGORY        = "search-page-layout";

  /** The Constant SEARCH_PAGE_LAYOUT_SELECTOR. */
  public static final String SEARCH_PAGE_LAYOUT_SELECTOR        = "searchPageLayoutSelector";

  /** The Constant SEARCH_FORM_TEMPLATE_CATEGORY. */
  public static final String SEARCH_FORM_TEMPLATE_CATEGORY      = "search-form";

//  /** The Constant SEARCH_PAGINATOR_TEMPLATE_CATEGORY. */
//  public static final String SEARCH_PAGINATOR_TEMPLATE_CATEGORY = "search-paginator";

  /** The Constant SEARCH_RESULT_TEMPLATE_CATEGORY. */
  public static final String SEARCH_RESULT_TEMPLATE_CATEGORY    = "search-result";

  /** The Constant SEARCH_FORM_TEMPLATE_SELECTOR. */
  public static final String SEARCH_FORM_TEMPLATE_SELECTOR      = "searchFormSelector";

  /** The Constant SEARCH_PAGINATOR_TEMPLATE_SELECTOR. */
  public static final String SEARCH_PAGINATOR_TEMPLATE_SELECTOR = "searchPaginatorSelector";

  /** The Constant SEARCH_RESULT_TEMPLATE_SELECTOR. */
  public static final String SEARCH_RESULT_TEMPLATE_SELECTOR    = "searchResultSelector";

  /** The Constant ITEMS_PER_PAGE_SELECTOR. */
  public final static String ITEMS_PER_PAGE_SELECTOR            = "itemsPerPageSelector";

  /** The Constant PAGE_MODE_SELECTOR. */
  public final static String PAGE_MODE_SELECTOR                 = "pageMode";

  /** The Constant BASE_PATH_INPUT. */
  public final static String BASE_PATH_INPUT                    = "searchResultBasePathInput";

  /** The Constant BASE_PATH_SELECTOR_POPUP_WINDOW. */
  public final static String BASE_PATH_SELECTOR_POPUP_WINDOW    = "searchResultBasePathPopupWindow";

  /** The Constant BASE_PATH_INPUT_SET_ACTION. */
  public final static String BASE_PATH_INPUT_SET_ACTION         = "searchResultBasePathInputSetAction";

  /** The popup id. */
  private String popupId;
  /**
   * @return the popupId
   */
  public String getPopupId() {
    return popupId;
  }

  /**
   * @param popupId the popupId to set
   */
  public void setPopupId(String popupId) {
    this.popupId = popupId;
  }

  /**
   * Instantiates a new uI search page layout manager.
   *
   * @throws Exception the exception
   */
  public UISearchPageLayoutManager() throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();

    String itemsPerpage = portletPreferences.getValue(UIWCMSearchPortlet.ITEMS_PER_PAGE, null);
    String pageMode = portletPreferences.getValue(UIWCMSearchPortlet.PAGE_MODE, null);
    String searchFormTemplate = portletPreferences.getValue(UIWCMSearchPortlet.SEARCH_FORM_TEMPLATE_PATH,
                                                            null);
    String searchResultTemplate = portletPreferences.getValue(UIWCMSearchPortlet.SEARCH_RESULT_TEMPLATE_PATH,
                                                              null);
//    String searchPaginatorTemplate = portletPreferences.getValue(UIWCMSearchPortlet.SEARCH_PAGINATOR_TEMPLATE_PATH,
//                                                                 null);
    String searchPageLayoutTemplate = portletPreferences.getValue(UIWCMSearchPortlet.SEARCH_PAGE_LAYOUT_TEMPLATE_PATH,
                                                                  null);
    List<SelectItemOption<String>> searchFormTemplateList = createTemplateList(PORTLET_NAME,
                                                                               SEARCH_FORM_TEMPLATE_CATEGORY);
    List<SelectItemOption<String>> searchResultTemplateList = createTemplateList(PORTLET_NAME,
                                                                                 SEARCH_RESULT_TEMPLATE_CATEGORY);
//    List<SelectItemOption<String>> searchPaginatorTemplateList = createTemplateList(PORTLET_NAME,
//                                                                                    SEARCH_PAGINATOR_TEMPLATE_CATEGORY);
    List<SelectItemOption<String>> searchPageLayoutTemplateList = createTemplateList(PORTLET_NAME,
                                                                                     SEARCH_PAGE_LAYOUT_CATEGORY);
    List<SelectItemOption<String>> itemsPerPageList = new ArrayList<SelectItemOption<String>>();
    itemsPerPageList.add(new SelectItemOption<String>("5", "5"));
    itemsPerPageList.add(new SelectItemOption<String>("10", "10"));
    itemsPerPageList.add(new SelectItemOption<String>("20", "20"));

    List<SelectItemOption<String>> pageModeList = new ArrayList<SelectItemOption<String>>();
    pageModeList.add(new SelectItemOption<String>("none", "none"));
    pageModeList.add(new SelectItemOption<String>("more", "more"));
    pageModeList.add(new SelectItemOption<String>("pagination", "pagination"));

    UIFormSelectBox pageModeSelector = new UIFormSelectBox(PAGE_MODE_SELECTOR,
            PAGE_MODE_SELECTOR,
            pageModeList);

    UIFormSelectBox itemsPerPageSelector = new UIFormSelectBox(ITEMS_PER_PAGE_SELECTOR,
                                                               ITEMS_PER_PAGE_SELECTOR,
                                                               itemsPerPageList);
    UIFormSelectBox searchFormTemplateSelector = new UIFormSelectBox(SEARCH_FORM_TEMPLATE_SELECTOR,
                                                                     SEARCH_FORM_TEMPLATE_SELECTOR,
                                                                     searchFormTemplateList);
    UIFormSelectBox searchResultTemplateSelector = new UIFormSelectBox(SEARCH_RESULT_TEMPLATE_SELECTOR,
                                                                       SEARCH_RESULT_TEMPLATE_SELECTOR,
                                                                       searchResultTemplateList);
//    UIFormSelectBox searchPaginatorTemplateSelector = new UIFormSelectBox(SEARCH_PAGINATOR_TEMPLATE_SELECTOR,
//                                                                          SEARCH_PAGINATOR_TEMPLATE_SELECTOR,
//                                                                          searchPaginatorTemplateList);
    UIFormSelectBox searchPageLayoutTemplateSelector = new UIFormSelectBox(SEARCH_PAGE_LAYOUT_SELECTOR,
                                                                           SEARCH_PAGE_LAYOUT_SELECTOR,
                                                                           searchPageLayoutTemplateList);

    String preferenceBasePath = portletPreferences.getValue(UIWCMSearchPortlet.BASE_PATH, null);
    UIFormInputSetWithAction targetPathFormInputSet = new UIFormInputSetWithAction(BASE_PATH_INPUT_SET_ACTION);
    UIFormStringInput targetPathFormStringInput = new UIFormStringInput(BASE_PATH_INPUT, BASE_PATH_INPUT, preferenceBasePath);
    targetPathFormStringInput.setValue(preferenceBasePath);
    targetPathFormStringInput.setReadOnly(true);
    targetPathFormInputSet.setActionInfo(BASE_PATH_INPUT, new String[] {"SelectBasePath"}) ;
    targetPathFormInputSet.addUIFormInput(targetPathFormStringInput);

    pageModeSelector.setValue(pageMode);
    itemsPerPageSelector.setValue(itemsPerpage);
    searchFormTemplateSelector.setValue(searchFormTemplate);
    searchResultTemplateSelector.setValue(searchResultTemplate);
//    searchPaginatorTemplateSelector.setValue(searchPaginatorTemplate);
    searchPageLayoutTemplateSelector.setValue(searchPageLayoutTemplate);

    addChild(pageModeSelector);
    addChild(itemsPerPageSelector);
    addChild(searchFormTemplateSelector);
    addChild(searchResultTemplateSelector);
//    addChild(searchPaginatorTemplateSelector);
    addChild(searchPageLayoutTemplateSelector);
    addChild(targetPathFormInputSet);

    setActions(new String[] { "Save", "Cancel" });
  }

  /**
   * Creates the template list.
   *
   * @param portletName the portlet name
   * @param category the category
   *
   * @return the list< select item option< string>>
   *
   * @throws Exception the exception
   */
  private List<SelectItemOption<String>> createTemplateList(String portletName, String category) throws Exception {
    List<SelectItemOption<String>> templateList = new ArrayList<SelectItemOption<String>>();
    ApplicationTemplateManagerService templateManagerService = getApplicationComponent(ApplicationTemplateManagerService.class);
    List<Node> templateNodeList = templateManagerService.getTemplatesByCategory(portletName,
                                                                                category,
                                                                                WCMCoreUtils.getUserSessionProvider());
    for (Node templateNode : templateNodeList) {
      String templateName = templateNode.getName();
      String templatePath = templateNode.getPath();
      templateList.add(new SelectItemOption<String>(templateName, templatePath));
    }
    return templateList;
  }

  /**
   * The listener interface for receiving saveAction events. The class that is
   * interested in processing a saveAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addSaveActionListener</code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class SaveActionListener extends EventListener<UISearchPageLayoutManager> {

    /*
     * (non-Javadoc)
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
     * .event.Event)
     */
    public void execute(Event<UISearchPageLayoutManager> event) throws Exception {
      UISearchPageLayoutManager uiSearchLayoutManager = event.getSource();
      UIApplication uiApp = uiSearchLayoutManager.getAncestorOfType(UIApplication.class);
      RepositoryService repositoryService = uiSearchLayoutManager.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      String repository = manageableRepository.getConfiguration().getName();
      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();

      String searchResultTemplatePath = uiSearchLayoutManager.
          getUIFormSelectBox(UISearchPageLayoutManager.SEARCH_RESULT_TEMPLATE_SELECTOR).getValue();
      String searchFormTemplatePath = uiSearchLayoutManager.
          getUIFormSelectBox(UISearchPageLayoutManager.SEARCH_FORM_TEMPLATE_SELECTOR).getValue();
//      String searchPaginatorTemplatePath = uiSearchLayoutManager.
//          getUIFormSelectBox(UISearchPageLayoutManager.SEARCH_PAGINATOR_TEMPLATE_SELECTOR).getValue();
      String searchPageLayoutTemplatePath = uiSearchLayoutManager.
          getUIFormSelectBox(UISearchPageLayoutManager.SEARCH_PAGE_LAYOUT_SELECTOR).getValue();
      String itemsPerPage = uiSearchLayoutManager.
          getUIFormSelectBox(UISearchPageLayoutManager.ITEMS_PER_PAGE_SELECTOR).getValue();

      String pageMode = uiSearchLayoutManager.getUIFormSelectBox(UISearchPageLayoutManager.PAGE_MODE_SELECTOR).getValue();

      String basePath = uiSearchLayoutManager.getUIStringInput(UISearchPageLayoutManager.BASE_PATH_INPUT)
                                             .getValue();

      portletPreferences.setValue(UIWCMSearchPortlet.REPOSITORY, repository);
      portletPreferences.setValue(UIWCMSearchPortlet.WORKSPACE, workspace);
      portletPreferences.setValue(UIWCMSearchPortlet.SEARCH_RESULT_TEMPLATE_PATH,
                                  searchResultTemplatePath);
      portletPreferences.setValue(UIWCMSearchPortlet.SEARCH_FORM_TEMPLATE_PATH,
                                  searchFormTemplatePath);
//      portletPreferences.setValue(UIWCMSearchPortlet.SEARCH_PAGINATOR_TEMPLATE_PATH,
//                                  searchPaginatorTemplatePath);
      portletPreferences.setValue(UIWCMSearchPortlet.SEARCH_PAGE_LAYOUT_TEMPLATE_PATH,
                                  searchPageLayoutTemplatePath);
      portletPreferences.setValue(UIWCMSearchPortlet.ITEMS_PER_PAGE, itemsPerPage);
      portletPreferences.setValue(UIWCMSearchPortlet.PAGE_MODE, pageMode);
      portletPreferences.setValue(UIWCMSearchPortlet.BASE_PATH, basePath);
      portletPreferences.store();

      if (Utils.isEditPortletInCreatePageWizard()) {
        uiApp.addMessage(new ApplicationMessage("UISearchConfig.msg.saving-success",
                                                null,
                                                ApplicationMessage.INFO));
      } else {
        portletRequestContext.setApplicationMode(PortletMode.VIEW);
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events. The class that is
   * interested in processing a cancelAction event implements this interface,
   * and the object created with that class is registered with a component using
   * the component's <code>addCancelActionListener</code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class CancelActionListener extends EventListener<UISearchPageLayoutManager> {

    /*
     * (non-Javadoc)
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
     * .event.Event)
     */
    public void execute(Event<UISearchPageLayoutManager> event) throws Exception {
      PortletRequestContext context = (PortletRequestContext) event.getRequestContext();
      context.setApplicationMode(PortletMode.VIEW);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String,
   *      java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    getUIStringInput(selectField).setValue((String) value);
    Utils.closePopupWindow(this, popupId);
  }

  /**
   * The listener interface for receiving selectTargetPageAction events.
   * The class that is interested in processing a selectTargetPageAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectTargetPageActionListener</code> method. When
   * the selectTargetPageAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class SelectBasePathActionListener extends EventListener<UISearchPageLayoutManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISearchPageLayoutManager> event) throws Exception {
      UISearchPageLayoutManager viewerManagementForm = event.getSource();
      UIPageSelector pageSelector = viewerManagementForm.createUIComponent(UIPageSelector.class, null, null);
      pageSelector.setSourceComponent(viewerManagementForm, new String[] {BASE_PATH_INPUT});
      Utils.createPopupWindow(viewerManagementForm, pageSelector, BASE_PATH_SELECTOR_POPUP_WINDOW, 800);
      viewerManagementForm.setPopupId(BASE_PATH_SELECTOR_POPUP_WINDOW);
    }
  }
}
