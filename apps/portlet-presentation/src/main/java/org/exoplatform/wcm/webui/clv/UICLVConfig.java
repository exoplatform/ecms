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
package org.exoplatform.wcm.webui.clv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.wcm.webui.selector.content.folder.UIContentBrowsePanelFolder;
import org.exoplatform.wcm.webui.selector.content.folder.UIContentSelectorFolder;
import org.exoplatform.wcm.webui.selector.content.multi.UIContentBrowsePanelMulti;
import org.exoplatform.wcm.webui.selector.content.multi.UIContentSelectorMulti;
import org.exoplatform.wcm.webui.selector.page.UIPageSelector;
import org.exoplatform.wcm.webui.validator.ZeroNumberValidator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.PositiveNumberFormatValidator;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 15, 2008
 */
/**
 * The Class UICLVConfig.
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class, 
  template = "app:/groovy/ContentListViewer/UICLVConfig.gtmpl", 
  events = {
    @EventConfig(listeners = UICLVConfig.SaveActionListener.class),
    @EventConfig(listeners = UICLVConfig.CancelActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICLVConfig.SelectFolderPathActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICLVConfig.IncreaseActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICLVConfig.DecreaseActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICLVConfig.SelectTargetPageActionListener.class, phase = Phase.DECODE)
  }
)
public class UICLVConfig extends UIForm  implements UISelectable {

  /** The Constant DISPLAY_MODE_FORM_RADIO_BOX_INPUT. */
  public static final String DISPLAY_MODE_FORM_RADIO_BOX_INPUT      = "UICLVConfigDisplayModeFormRadioBoxInput";
  
  /** The Constant ITEM_PATH_FORM_INPUT_SET. */
  public final static String ITEM_PATH_FORM_INPUT_SET               = "UICLVConfigItemPathFormInputSet";
  
  /** The Constant ITEM_PATH_FORM_STRING_INPUT. */
  public final static String ITEM_PATH_FORM_STRING_INPUT            = "UICLVConfigItemPathFormStringInput";
  
  /** The Constant ORDER_BY_FORM_SELECT_BOX. */
  public static final String ORDER_BY_FORM_SELECT_BOX               = "UICLVConfigOrderByFormSelectBox";
  
  /** The Constant ORDER_TYPE_FORM_RADIO_BOX_INPUT. */
  public static final String ORDER_TYPE_FORM_RADIO_BOX_INPUT        = "UICLVConfigOrderTypeFormRadioBoxInput";
  
  /** The Constant HEADER_FORM_STRING_INPUT. */
  public final static String HEADER_FORM_STRING_INPUT               = "UICLVConfigHeaderFormStringInput";

  /** The Constant DISPLAY_TEMPLATE_FORM_SELECT_BOX. */
  public final static String DISPLAY_TEMPLATE_FORM_SELECT_BOX       = "UICLVConfigDisplayTemplateFormSelectBox";

  /** The Constant PAGINATOR_TEMPLATE_FORM_SELECT_BOX. */
  public final static String PAGINATOR_TEMPLATE_FORM_SELECT_BOX     = "UICLVConfigPaginatorTemplateFormSelectBox";
  
  /** The Constant TARGET_PAGE_FORM_INPUT_SET. */
  public final static String TARGET_PAGE_FORM_INPUT_SET             = "UICLVConfigTargetPageFormInputSet";
  
  /** The Constant TARGET_PAGE_FORM_STRING_INPUT. */
  public final static String TARGET_PAGE_FORM_STRING_INPUT          = "UICLVConfigTargetPageFormStringInput";

  /** The Constant TARGET_PAGE_SELECTOR_POPUP_WINDOW. */
  public final static String TARGET_PAGE_SELECTOR_POPUP_WINDOW      = "UICLVConfigTargetPageSelectorPopupWindow";

  /** The Constant ITEMS_PER_PAGE_FORM_STRING_INPUT. */
  public final static String ITEMS_PER_PAGE_FORM_STRING_INPUT       = "UICLVConfigItemsPerPageFormStringInput";
  
  /** The Constant SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT  = "UICLVConfigShowIllustrationFormCheckboxInput";
  
  /** The Constant SHOW_TITLE_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_TITLE_FORM_CHECKBOX_INPUT         = "UICLVConfigShowTitleFormCheckboxInput";

  /** The Constant SHOW_SUMMARY_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_SUMMARY_FORM_CHECKBOX_INPUT       = "UICLVConfigShowSummaryFormCheckboxInput";
  
  /** The Constant SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT  = "UICLVConfigShowDateCreatedFormCheckboxInput";

  /** The Constant SHOW_HEADER_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_HEADER_FORM_CHECKBOX_INPUT        = "UICLVConfigShowHeaderFormCheckboxInput";

  /** The Constant SHOW_LINK_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_LINK_FORM_CHECKBOX_INPUT          = "UICLVConfigShowLinkFormCheckboxInput";
  
  /** The Constant SHOW_REFRESH_FORM_CHECKBOX_INPUT. */
  public final static String SHOW_REFRESH_FORM_CHECKBOX_INPUT       = "UICLVConfigShowRefreshFormCheckboxInput";

  /** The Constant SHOW_READMORE_FORM_CHECKBOX_INPUT. */
  public final static String SHOW_READMORE_FORM_CHECKBOX_INPUT      = "UICLVConfigShowReadmoreFormCheckboxInput";

  /** TODO: Need to improve, we should allow user can choose template category by configuration or portlet's preference */
  /** The Constant DISPLAY_TEMPLATE_CATEGORY. */
  public final static String DISPLAY_TEMPLATE_CATEGORY              = "list-by-folder";

  /** The Constant PAGINATOR_TEMPLATE_CATEGORY. */
  public final static String PAGINATOR_TEMPLATE_CATEGORY            = "paginators";
  
  /** TODO: Need to improve, we should get portlet's name by API, not hardcode like this */
  /** The Constant PORTLET_NAME. */
  public final static String PORTLET_NAME                           = "Content List Viewer";
  
  /** The popup id. */
  private String popupId;
  
  /** The items. */
  private List<String> items;
  
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
   * Gets the items.
   * 
   * @return the items
   */
  public List<String> getItems() {
    String displayMode = ((UIFormRadioBoxInput) getChildById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
    String itemPath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH);
    if (items == null && UICLVPortlet.DISPLAY_MODE_MANUAL.equals(displayMode) && itemPath.contains(";")) {
      items = Arrays.asList(itemPath.split(";"));
    }
    return items;
  }
  
  /**
   * Sets the items.
   * 
   * @param items the new items
   */
  public void setItems(List<String> items) {
    this.items = items;
  }
  
  /**
   * Instantiates a new uICLV config.
   * 
   * @throws Exception the exception
   */
  public UICLVConfig() throws Exception {
    PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest().getPreferences();
    String displayMode = portletPreferences.getValue(UICLVPortlet.PREFERENCE_DISPLAY_MODE, null);
    String itemPath = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
    String orderBy = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_BY, null);
    String orderType = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, null);
    String header = portletPreferences.getValue(UICLVPortlet.PREFERENCE_HEADER, null);
    String displayTemplate = portletPreferences.getValue(UICLVPortlet.PREFERENCE_DISPLAY_TEMPLATE, null);
    String paginatorTemplate = portletPreferences.getValue(UICLVPortlet.PREFERENCE_PAGINATOR_TEMPLATE, null);
    String targetPage = portletPreferences.getValue(UICLVPortlet.PREFERENCE_TARGET_PAGE, null);
    String itemsPerPage = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null);
    boolean showIlustration  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_ILLUSTRATION, null));
    boolean showTitle  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_TITLE, null));
    boolean showSummary  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_SUMMARY, null));
    boolean showDateCreated  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_DATE_CREATED, null));
    boolean showHeader  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_HEADER, null));
    boolean showLink = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_LINK, null));
    boolean showRefresh  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_REFRESH_BUTTON, null));
    boolean showReadmore  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_READMORE, null));
    
    /** DISPLAY MODE */
    List<SelectItemOption<String>> displayModeOptions = new ArrayList<SelectItemOption<String>>();
    displayModeOptions.add(new SelectItemOption<String>(UICLVPortlet.DISPLAY_MODE_AUTOMATIC, UICLVPortlet.DISPLAY_MODE_AUTOMATIC));
    displayModeOptions.add(new SelectItemOption<String>(UICLVPortlet.DISPLAY_MODE_MANUAL, UICLVPortlet.DISPLAY_MODE_MANUAL));
    UIFormRadioBoxInput displayModeRadioBoxInput = new UIFormRadioBoxInput(DISPLAY_MODE_FORM_RADIO_BOX_INPUT, DISPLAY_MODE_FORM_RADIO_BOX_INPUT, displayModeOptions);
    displayModeRadioBoxInput.setValue(displayMode);
    
    /** ITEM PATH */
    UIFormStringInput itemPathInput = new UIFormStringInput(ITEM_PATH_FORM_STRING_INPUT, ITEM_PATH_FORM_STRING_INPUT, itemPath);
    itemPathInput.setEditable(false);
    itemPathInput.addValidator(MandatoryValidator.class);
    UIFormInputSetWithAction itemPathInputSet = new UIFormInputSetWithAction(ITEM_PATH_FORM_INPUT_SET);
    itemPathInputSet.setActionInfo(ITEM_PATH_FORM_STRING_INPUT, new String[] { "SelectFolderPath" }) ;
    itemPathInputSet.addUIFormInput(itemPathInput);
    
    /** ORDER BY */
    List<SelectItemOption<String>> orderByOptions = new ArrayList<SelectItemOption<String>>();
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_TITLE, NodetypeConstant.EXO_TITLE));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_CREATED, NodetypeConstant.EXO_DATE_CREATED));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_MODIFIED, NodetypeConstant.EXO_DATE_MODIFIED));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_PUBLISHED,"publication:liveDate"));    
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_START_EVENT,"exo:startEvent"));    
    UIFormSelectBox orderBySelectBox = new UIFormSelectBox(ORDER_BY_FORM_SELECT_BOX, ORDER_BY_FORM_SELECT_BOX, orderByOptions);
    orderBySelectBox.setValue(orderBy);
    
    /** ORDER TYPE */
    List<SelectItemOption<String>> orderTypeOptions = new ArrayList<SelectItemOption<String>>();
    orderTypeOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_TYPE_DESCENDENT, "DESC"));
    orderTypeOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_TYPE_ASCENDENT, "ASC"));
    UIFormRadioBoxInput orderTypeRadioBoxInput = new UIFormRadioBoxInput(ORDER_TYPE_FORM_RADIO_BOX_INPUT, ORDER_TYPE_FORM_RADIO_BOX_INPUT, orderTypeOptions);
    orderTypeRadioBoxInput.setValue(orderType);
    
    /** HEADER */
    UIFormStringInput headerInput = new UIFormStringInput(HEADER_FORM_STRING_INPUT, HEADER_FORM_STRING_INPUT, header);
    
    /** DISPLAY TEMPLATE */
    List<SelectItemOption<String>> formViewerTemplateList = getTemplateList(PORTLET_NAME, DISPLAY_TEMPLATE_CATEGORY);
    UIFormSelectBox formViewTemplateSelector = new UIFormSelectBox(DISPLAY_TEMPLATE_FORM_SELECT_BOX, DISPLAY_TEMPLATE_FORM_SELECT_BOX, formViewerTemplateList);
    formViewTemplateSelector.setValue(displayTemplate);
    
    /** PAGINATOR TEMPLATE */
    List<SelectItemOption<String>> paginatorTemplateList = getTemplateList(PORTLET_NAME, PAGINATOR_TEMPLATE_CATEGORY);
    UIFormSelectBox paginatorTemplateSelector = new UIFormSelectBox(PAGINATOR_TEMPLATE_FORM_SELECT_BOX, PAGINATOR_TEMPLATE_FORM_SELECT_BOX, paginatorTemplateList);
    paginatorTemplateSelector.setValue(paginatorTemplate);
    
    /** TARGET PAGE */
    UIFormInputSetWithAction targetPageInputSet = new UIFormInputSetWithAction(TARGET_PAGE_FORM_INPUT_SET);
    UIFormStringInput basePathInput = new UIFormStringInput(TARGET_PAGE_FORM_STRING_INPUT, TARGET_PAGE_FORM_STRING_INPUT, targetPage);
    basePathInput.setValue(targetPage);
    basePathInput.setEditable(false);
    targetPageInputSet.setActionInfo(TARGET_PAGE_FORM_STRING_INPUT, new String[] {"SelectTargetPage"}) ;
    targetPageInputSet.addUIFormInput(basePathInput);
    
    /** ITEMS PER PAGE */
    UIFormStringInput itemsPerPageStringInput = new UIFormStringInput(ITEMS_PER_PAGE_FORM_STRING_INPUT, ITEMS_PER_PAGE_FORM_STRING_INPUT, itemsPerPage);
    itemsPerPageStringInput.addValidator(MandatoryValidator.class);
    itemsPerPageStringInput.addValidator(ZeroNumberValidator.class);
    itemsPerPageStringInput.addValidator(PositiveNumberFormatValidator.class);
    itemsPerPageStringInput.setMaxLength(3);

    /** SHOW ILLUSTRATION */
    UIFormCheckBoxInput<Boolean> showIllustrationCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT, SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT, null);
    showIllustrationCheckbox.setChecked(showIlustration);
    
    /** SHOW TITLE */
    UIFormCheckBoxInput<Boolean> showTitleCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_TITLE_FORM_CHECKBOX_INPUT, SHOW_TITLE_FORM_CHECKBOX_INPUT, null);
    showTitleCheckbox.setChecked(showTitle);
    
    /** SHOW SUMMARY */
    UIFormCheckBoxInput<Boolean> showSummaryCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_SUMMARY_FORM_CHECKBOX_INPUT, SHOW_SUMMARY_FORM_CHECKBOX_INPUT, null);
    showSummaryCheckbox.setChecked(showSummary);
    
    /** SHOW DATE CREATED */
    UIFormCheckBoxInput<Boolean> showDateCreatedCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT, SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT, null);
    showDateCreatedCheckbox.setChecked(showDateCreated);
    
    /** SHOW HEADER */
    UIFormCheckBoxInput<Boolean> showHeaderCheckBox = new UIFormCheckBoxInput<Boolean>(SHOW_HEADER_FORM_CHECKBOX_INPUT, SHOW_HEADER_FORM_CHECKBOX_INPUT, null);
    showHeaderCheckBox.setChecked(showHeader);
    
    /** SHOW LINK */
    UIFormCheckBoxInput<Boolean> showLinkCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_LINK_FORM_CHECKBOX_INPUT, SHOW_LINK_FORM_CHECKBOX_INPUT, null);
    showLinkCheckbox.setChecked(showLink);
    
    /** SHOW REFRESH */
    UIFormCheckBoxInput<Boolean> showRefreshCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_REFRESH_FORM_CHECKBOX_INPUT, SHOW_REFRESH_FORM_CHECKBOX_INPUT, null);
    showRefreshCheckbox.setChecked(showRefresh);
    
    /** SHOW READMORE */
    UIFormCheckBoxInput<Boolean> showReadmoreCheckbox = new UIFormCheckBoxInput<Boolean>(SHOW_READMORE_FORM_CHECKBOX_INPUT, SHOW_READMORE_FORM_CHECKBOX_INPUT, null);
    showReadmoreCheckbox.setChecked(showReadmore);
    
    addChild(displayModeRadioBoxInput);
    addChild(itemPathInputSet);
    addChild(orderBySelectBox);
    addChild(orderTypeRadioBoxInput);
    addChild(headerInput);
    addChild(formViewTemplateSelector);
    addChild(paginatorTemplateSelector);
    addChild(itemsPerPageStringInput);    
    addChild(showRefreshCheckbox);
    addChild(showIllustrationCheckbox);
    addChild(showTitleCheckbox);
    addChild(showDateCreatedCheckbox);
    addChild(showSummaryCheckbox);
    addChild(showHeaderCheckBox);
    addChild(showLinkCheckbox);
    addChild(showReadmoreCheckbox);
    addChild(targetPageInputSet);

    setActions(new String[] { "Save", "Cancel" });
  }

  /**
   * Gets the template list.
   * 
   * @param portletName the portlet name
   * @param category the category
   * @return the template list
   * @throws Exception the exception
   */
  private List<SelectItemOption<String>> getTemplateList(String portletName, String category) throws Exception {
    List<SelectItemOption<String>> templateOptionList = new ArrayList<SelectItemOption<String>>();
    ApplicationTemplateManagerService templateManagerService = getApplicationComponent(ApplicationTemplateManagerService.class);
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    String repository = manageableRepository.getConfiguration().getName();
    List<Node> templateNodeList = templateManagerService.getTemplatesByCategory(repository, portletName, category, WCMCoreUtils.getUserSessionProvider());
    for (Node templateNode : templateNodeList) {
      SelectItemOption<String> template = new SelectItemOption<String>();
      template.setLabel(templateNode.getName());
      template.setValue(templateNode.getPath());
      templateOptionList.add(template);
    }
    return templateOptionList;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    if (selectField != null && value != null) {
      String sValue = (String) value;
      String displayMode = ((UIFormRadioBoxInput) getChildById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
      if (ITEM_PATH_FORM_STRING_INPUT.equals(selectField) && UICLVPortlet.DISPLAY_MODE_MANUAL.equals(displayMode)) {
        items = Arrays.asList(sValue.split(";"));
      } else {
        items = new ArrayList<String>();
      }
      getUIStringInput(selectField).setValue(sValue);
    }
    Utils.closePopupWindow(this, popupId);
  }
  
  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UICLVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();

      /** GET VALUES FROM UIFORM */
      String displayMode = ((UIFormRadioBoxInput) clvConfig.getChildById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
      String itemPath = clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).getValue();
      if ((itemPath.contains(";") && displayMode.equals(UICLVPortlet.DISPLAY_MODE_AUTOMATIC)) || (!itemPath.contains(";") && displayMode.equals(UICLVPortlet.DISPLAY_MODE_MANUAL))) {
        Utils.createPopupMessage(clvConfig, "UICLVConfig.msg.not-valid-path", null, ApplicationMessage.WARNING);
        return;
      }
      String orderBy = clvConfig.getUIFormSelectBox(ORDER_BY_FORM_SELECT_BOX).getValue();
      String orderType = ((UIFormRadioBoxInput) clvConfig.getChildById(UICLVConfig.ORDER_TYPE_FORM_RADIO_BOX_INPUT)).getValue();
      String header = clvConfig.getUIStringInput(UICLVConfig.HEADER_FORM_STRING_INPUT).getValue();
      if (header == null) header = "";
      String displayTemplate = clvConfig.getUIFormSelectBox(UICLVConfig.DISPLAY_TEMPLATE_FORM_SELECT_BOX).getValue();
      String paginatorTemplate = clvConfig.getUIFormSelectBox(UICLVConfig.PAGINATOR_TEMPLATE_FORM_SELECT_BOX).getValue();
      String itemsPerPage = clvConfig.getUIStringInput(UICLVConfig.ITEMS_PER_PAGE_FORM_STRING_INPUT).getValue();
      String targetPage = clvConfig.getUIStringInput(UICLVConfig.TARGET_PAGE_FORM_STRING_INPUT).getValue();
      String showIllustration = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showTitle = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_TITLE_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showSummary = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_SUMMARY_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showDateCreated = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showHeader = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_HEADER_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showLink = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_LINK_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showRefresh = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_REFRESH_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      String showReadmore = clvConfig.getUIFormCheckBoxInput(UICLVConfig.SHOW_READMORE_FORM_CHECKBOX_INPUT).isChecked() ? "true" : "false";
      
      /** SET VALUES TO PREFERENCES */
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_DISPLAY_MODE, displayMode);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ITEM_PATH, itemPath);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ORDER_BY, orderBy);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, orderType);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_HEADER, header);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_DISPLAY_TEMPLATE, displayTemplate);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_PAGINATOR_TEMPLATE, paginatorTemplate);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, itemsPerPage);      
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_TARGET_PAGE, targetPage);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_ILLUSTRATION, showIllustration);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_TITLE, showTitle);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_SUMMARY, showSummary);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_DATE_CREATED, showDateCreated);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_HEADER, showHeader);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_LINK, showLink);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_REFRESH_BUTTON, showRefresh);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_READMORE, showReadmore);
      portletPreferences.store();
      
      UICLVPortlet portlet = clvConfig.getAncestorOfType(UICLVPortlet.class);
      if (displayMode.equals(UICLVPortlet.DISPLAY_MODE_AUTOMATIC)) {
        portlet.getChild(UICLVFolderMode.class).init();
      } else if (displayMode.equals(UICLVPortlet.DISPLAY_MODE_MANUAL)) {
        portlet.getChild(UICLVManualMode.class).init();
      }
      
      if (Utils.isPortalEditMode()) {
      	Utils.createPopupMessage(clvConfig, "UICLVConfig.msg.saving-success", null, ApplicationMessage.INFO);
      } else {
        Utils.closePopupWindow(clvConfig, "UIViewerManagementPopupWindow");
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UICLVConfig> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      if (!Utils.isPortalEditMode())
        Utils.closePopupWindow(clvConfig, "UIViewerManagementPopupWindow");
    }
  }

  /**
   * The listener interface for receiving selectFolderPathAction events.
   * The class that is interested in processing a selectFolderPathAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectFolderPathActionListener<code> method. When
   * the selectFolderPathAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SelectFolderPathActionEvent
   */
  public static class SelectFolderPathActionListener extends EventListener<UICLVConfig> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      UIFormRadioBoxInput modeBoxInput = (UIFormRadioBoxInput) clvConfig.getChildById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT);
      String mode = modeBoxInput.getValue();
      if (mode.equals(UICLVPortlet.DISPLAY_MODE_AUTOMATIC)) {
        UIContentSelectorFolder contentSelector = clvConfig.createUIComponent(UIContentSelectorFolder.class, null, null);
        UIContentBrowsePanelFolder folderContentSelector= contentSelector.getChild(UIContentBrowsePanelFolder.class);
        folderContentSelector.setSourceComponent(clvConfig, new String[] { UICLVConfig.ITEM_PATH_FORM_STRING_INPUT });
        Utils.createPopupWindow(clvConfig, contentSelector, UIContentSelector.FOLDER_PATH_SELECTOR_POPUP_WINDOW, 800);
        clvConfig.setPopupId(UIContentSelector.FOLDER_PATH_SELECTOR_POPUP_WINDOW);
      } else {
        UIContentSelectorMulti contentSelector = clvConfig.createUIComponent(UIContentSelectorMulti.class, null, null);
        UIContentBrowsePanelMulti multiContentSelector= contentSelector.getChild(UIContentBrowsePanelMulti.class);
        multiContentSelector.setSourceComponent(clvConfig, new String[] { UICLVConfig.ITEM_PATH_FORM_STRING_INPUT });
        String itemPath = clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).getValue();
        if (itemPath != null && itemPath.contains(";"))
          multiContentSelector.setItemPaths(itemPath);
        contentSelector.init();
        Utils.createPopupWindow(clvConfig, contentSelector, UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW, 800);
        clvConfig.setPopupId(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
      }
    }
  }

  /**
   * The listener interface for receiving increaseAction events.
   * The class that is interested in processing a increaseAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addIncreaseActionListener<code> method. When
   * the increaseAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see IncreaseActionEvent
   */
  public static class IncreaseActionListener extends EventListener<UICLVConfig> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */ 
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      List<String> items = clvConfig.items;
      int offset = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      if (offset > 0) {
        String temp = items.get(offset - 1);
        items.set(offset - 1, items.get(offset));
        items.set(offset, temp);
      }
      String itemPath = "";
      for (String item : items) {
        itemPath += item + ";";
      }
      clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).setValue(itemPath);
    }
  }

  /**
   * The listener interface for receiving decreaseAction events.
   * The class that is interested in processing a decreaseAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDecreaseActionListener<code> method. When
   * the decreaseAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see DecreaseActionEvent
   */
  public static class DecreaseActionListener extends EventListener<UICLVConfig> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      List<String> items = clvConfig.items;
      int offset = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      if (offset < items.size() - 1) {
        String temp = items.get(offset + 1);
        items.set(offset + 1, items.get(offset));
        items.set(offset, temp);
      }
      String itemPath = "";
      for (String item : items) {
        itemPath += item + ";";
      }
      clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).setValue(itemPath);
    }
  }
  
  /**
   * The listener interface for receiving selectTargetPageAction events.
   * The class that is interested in processing a selectTargetPageAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectTargetPageActionListener<code> method. When
   * the selectTargetPageAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SelectTargetPageActionEvent
   */
  public static class SelectTargetPageActionListener extends EventListener<UICLVConfig> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig viewerManagementForm = event.getSource();
      UIPageSelector pageSelector = viewerManagementForm.createUIComponent(UIPageSelector.class, null, null);
      pageSelector.setSourceComponent(viewerManagementForm, new String[] {TARGET_PAGE_FORM_STRING_INPUT});
      Utils.createPopupWindow(viewerManagementForm, pageSelector, TARGET_PAGE_SELECTOR_POPUP_WINDOW, 800);
      viewerManagementForm.setPopupId(TARGET_PAGE_SELECTOR_POPUP_WINDOW);
    }
  }
  
}
