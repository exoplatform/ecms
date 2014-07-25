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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.reader.ContentReader;
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
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.input.UICheckBoxInput;
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
                   @EventConfig(listeners = UICLVConfig.AddPathActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UICLVConfig.IncreaseActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UICLVConfig.DecreaseActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UICLVConfig.SelectTargetPageActionListener.class, phase = Phase.DECODE),
	                 @EventConfig(listeners = UICLVConfig.SelectTabActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UICLVConfig.ShowAdvancedBlockActionListener.class, phase = Phase.DECODE)
                 }
    )
public class UICLVConfig extends UIFormTabPane  implements UISelectable {

  final static public String CONTENT_TAB = "clvContentTab" ;
  final static public String DISPLAY_TAB = "clvDisplayTab" ;
  final static public String ADVANCED_TAB = "clvAdvancedTab" ;
  
  private static final Log         LOG             = ExoLogger.getLogger(UICLVConfig.class.getName());

  /** The Constant DISPLAY_MODE_FORM_RADIO_BOX_INPUT. */
  public static final String DISPLAY_MODE_FORM_RADIO_BOX_INPUT       = "UICLVConfigDisplayModeFormRadioBoxInput";

  /** The Constant ITEM_PATH_FORM_INPUT_SET. */
  public final static String ITEM_PATH_FORM_INPUT_SET                = "UICLVConfigItemPathFormInputSet";

  /** The Constant ITEM_PATH_FORM_STRING_INPUT. */
  public final static String ITEM_PATH_FORM_STRING_INPUT             = "UICLVConfigItemPathFormStringInput";

  /** The Constant ORDER_BY_FORM_SELECT_BOX. */
  public static final String ORDER_BY_FORM_SELECT_BOX                = "UICLVConfigOrderByFormSelectBox";

  /** The Constant ORDER_BY_FORM_SELECT_BOX. */
  public static final String ORDER_TYPE_FORM_SELECT_BOX              = "UICLVConfigOrderTypeFormSelectBox";

  /** The Constant ORDER_TYPE_FORM_RADIO_BOX_INPUT. */
  public static final String ORDER_TYPE_FORM_RADIO_BOX_INPUT         = "UICLVConfigOrderTypeFormRadioBoxInput";

  /** The Constant HEADER_FORM_STRING_INPUT. */
  public final static String HEADER_FORM_STRING_INPUT                = "UICLVConfigHeaderFormStringInput";

  /** The Constant SHOW_AUTOMATIC_DETECTION_CHECKBOX_INPUT. */
  public static final String SHOW_AUTOMATIC_DETECTION_CHECKBOX_INPUT = "UICLVConfigShowAutomaticDetectionCheckboxInput";

  /** The Constant DISPLAY_TEMPLATE_FORM_SELECT_BOX. */
  public final static String DISPLAY_TEMPLATE_FORM_SELECT_BOX        = "UICLVConfigDisplayTemplateFormSelectBox";

  /** The Constant PAGINATOR_TEMPLATE_FORM_SELECT_BOX. */
  public final static String PAGINATOR_TEMPLATE_FORM_SELECT_BOX      = "UICLVConfigPaginatorTemplateFormSelectBox";

  /** The Constant ITEMS_PER_PAGE_FORM_STRING_INPUT. */
  public final static String ITEMS_PER_PAGE_FORM_STRING_INPUT        = "UICLVConfigItemsPerPageFormStringInput";

  /** The Constant SHOW_TITLE_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_TITLE_FORM_CHECKBOX_INPUT          = "UICLVConfigShowTitleFormCheckboxInput";

  /** The Constant SHOW_HEADER_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_HEADER_FORM_CHECKBOX_INPUT         = "UICLVConfigShowHeaderFormCheckboxInput";

  /** The Constant SHOW_REFRESH_FORM_CHECKBOX_INPUT. */
  public final static String SHOW_REFRESH_FORM_CHECKBOX_INPUT        = "UICLVConfigShowRefreshFormCheckboxInput";

  /** The Constant SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT. */
  /** The Constant SHOW_IMAGE_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT   = "UICLVConfigShowIllustrationFormCheckboxInput";

  // public static final String SHOW_IMAGE_FORM_CHECKBOX_INPUT =
  // "UICLVConfigShowImageFormCheckboxInput";

  /** The Constant SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT   = "UICLVConfigShowDateCreatedFormCheckboxInput";

  /** The Constant SHOW_MORE_LINK_FORM_CHECKBOX_INPUT. */
  public final static String SHOW_READMORE_FORM_CHECKBOX_INPUT       = "UICLVConfigShowReadmoreFormCheckboxInput";

  // public static final String SHOW_MORE_LINK_FORM_CHECKBOX_INPUT =
  // "UICLVConfigShowMoreLinkCheckedboxInput";

  /** The Constant SHOW_SUMMARY_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_SUMMARY_FORM_CHECKBOX_INPUT        = "UICLVConfigShowSummaryFormCheckboxInput";

  /** The Constant SHOW_LINK_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_LINK_FORM_CHECKBOX_INPUT           = "UICLVConfigShowLinkFormCheckboxInput";

  /** The Constant SHOW_RSSLINK_FORM_CHECKBOX_INPUT. */
  public static final String SHOW_RSSLINK_FORM_CHECKBOX_INPUT        = "UICLVConfigShowRssLinkFormCheckboxInput";

  /** The Constant TARGET_PAGE_FORM_INPUT_SET. */
  public final static String TARGET_PAGE_FORM_INPUT_SET              = "UICLVConfigTargetPageFormInputSet";

  /** The Constant TARGET_PAGE_FORM_STRING_INPUT. */
  public final static String TARGET_PAGE_FORM_STRING_INPUT           = "UICLVConfigTargetPageFormStringInput";

  /** The Constant TARGET_PAGE_SELECTOR_POPUP_WINDOW. */
  public final static String TARGET_PAGE_SELECTOR_POPUP_WINDOW       = "UICLVConfigTargetPageSelectorPopupWindow";

  /** The Constant DYNAMIC_NAVIGATION_LABEL. */
  public static final String DYNAMIC_NAVIGATION_LABEL                = "UICLVConfigDynamicNavigationLabel";

  /** The Constant CONTEXTUAL_FOLDER_RADIOBOX_INPUT. */
  public static final String CONTEXTUAL_FOLDER_RADIOBOX_INPUT        = "UICLVConfigContextualFolderRadioBoxInput";

  /** The Constant SHOW_CLV_BY_STRING_INPUT. */
  public static final String SHOW_CLV_BY_STRING_INPUT                = "UICLVConfigShowCLVByStringInput";

  /** The Constant SHOW_SCV_WITH_STRING_INPUT. */
  public static final String SHOW_SCV_WITH_STRING_INPUT              = "UICLVConfigshowSCVWithStringInput";

  /** The Constant PAGINATOR_TEMPLATE_CATEGORY. */
  public final static String PAGINATOR_TEMPLATE_CATEGORY             = "paginators";

  /** The Constant CACHE_ENABLE_RADIOBOX_INPUT */
  public static final String CACHE_ENABLE_RADIOBOX_INPUT             = "UICLVConfigCacheEnableRadioBoxInput";

  /** The Constant CONTENT_BY_QUERY_TEXT_AREA */
  public static final String CONTENT_BY_QUERY_TEXT_AREA              = "UICLVConfigContentByQueryTextArea";

  /** The Constant WORKSPACE_FORM_SELECT_BOX. */
  public final static String WORKSPACE_FORM_SELECT_BOX               = "UICLVConfigWorkspaceFormSelectBox";

  /** The Constant CACHE_MANAGEMENT_LABEL */
  public static final String CACHE_MANAGEMENT_LABEL                  = "UICLVConfigCacheManagementLabel";

  /** The Constant CONTENT_BY_QUERY_LABEL */
  public static final String CONTENT_BY_QUERY_LABEL                  = "UICLVContentByQueryLabel";

  /** The Constant DISPLAY_TEMPLATE_CATEGORY. */
  public final static String DISPLAY_TEMPLATE_CATEGORY               = "navigation";

  public final static String DISPLAY_TEMPLATE_LIST                   = "list";

  public final static String TEMPLATE_STORAGE_FOLDER                 = "content-list-viewer";

  public final static String CONTENT_LIST_TYPE                       = "ContentList";

  public final static String CATEGORIES_CONTENT_TYPE                 = "CategoryContents";

  public final static String CATOGORIES_NAVIGATION_TYPE              = "CategoryNavigation";

  /** The constant values for cache */
  public static final String ENABLE_CACHE                            = "ENABLE";
  public static final String DISABLE_CACHE                           = "DISABLE";

  /** The popup id. */
  private String popupId;

  /** The items. */
  private List<String> items;

  private String savedPath;
  private boolean isShowAdvancedBlock_;
  private String appType;
  private String driveName_;

  public void setSavedPath(String value) {
    savedPath = value;
  }
  public String getSavedPath () {
    return savedPath;
  }

  /**
   * check if the content in the path is alive and return only alive content
   * 
   * @return
   */
  public String getAliveSavedPath () {
    //check if the path is alive
    if(savedPath != null && !savedPath.isEmpty()){
      List<String> tmpItems = new ArrayList<String>();
      StringBuffer itemsBuffer = new StringBuffer();
      if(savedPath.contains(";")){
        tmpItems = Arrays.asList(savedPath.split(";"));
      } else {
        tmpItems.add(savedPath);
      }
	    //only add exist Node
      for(String item:tmpItems) {
        try{
          if(getRealNode(item) != null){
            itemsBuffer.append(item).append(";");
          }
        }catch(RepositoryException e){
          if(LOG.isDebugEnabled()){
            LOG.debug(e.getMessage());
          }
        }
      }
      return itemsBuffer.toString();
    }
    return savedPath;
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

  public void setDriveName(String value) { this.driveName_ = value; }
  public String getDriveName() { return this.driveName_; }

  /**
   * Gets the items.
   *
   * @return the items
   */
  public List<String> getItems() {
    String displayMode = ((UIFormRadioBoxInput) findComponentById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
    String itemPath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH);
    if (items == null && UICLVPortlet.DISPLAY_MODE_MANUAL.equals(displayMode) && itemPath != null) {
      if(itemPath.contains(";")) {
        List<String> tmpItems = Arrays.asList(itemPath.split(";"));
        items = new ArrayList<String>();
        //only add exist Node
        for(String item:tmpItems) {
          try{
            Node realNode=getRealNode(item);
            if(realNode != null){
              items.add(item);
            }
          }catch(RepositoryException e){
            if(LOG.isDebugEnabled()){
              LOG.debug(e.getMessage());
            }
          }
          
        }
      }
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

  public boolean isShowAdvancedBlock() { return isShowAdvancedBlock_; }

  public void setIsShowAdvancedBlock(boolean value) { isShowAdvancedBlock_ = value; }

  /**
   * Instantiates a new uICLV config.
   *
   * @throws Exception the exception
   */
  public UICLVConfig() throws Exception {
    super("UICLVConfig");
    PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest()
        .getPreferences();
    appType = portletPreferences.getValue(UICLVPortlet.PREFERENCE_APPLICATION_TYPE, null);
    String displayMode = portletPreferences.getValue(UICLVPortlet.PREFERENCE_DISPLAY_MODE, null);
    String itemPath = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
    savedPath = itemPath;
    itemPath = getTitles(savedPath);
    this.setDriveName(portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_DRIVE, null));
    String orderBy = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_BY, null);
    String orderType = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, null);

    String header = portletPreferences.getValue(UICLVPortlet.PREFERENCE_HEADER, null);
    String displayTemplate = portletPreferences.getValue(UICLVPortlet.PREFERENCE_DISPLAY_TEMPLATE, null);
    String paginatorTemplate = portletPreferences.getValue(UICLVPortlet.PREFERENCE_PAGINATOR_TEMPLATE, null);
    String itemsPerPage = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null);

    String contextualFolderMode = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER, null);

    String showClvBy = portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_CLV_BY, null);
    String targetPage = portletPreferences.getValue(UICLVPortlet.PREFERENCE_TARGET_PAGE, null);
    String showScvWith = portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_SCV_WITH, null);
    String isCacheEnabled = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CACHE_ENABLED, null);
    String workspace = portletPreferences.getValue(UICLVPortlet.PREFERENCE_WORKSPACE, null);
    String contentByQuery = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CONTENTS_BY_QUERY, null);

    boolean showAutomaticDetection = Boolean.parseBoolean(portletPreferences.getValue(
                                                                                      UICLVPortlet.PREFERENCE_AUTOMATIC_DETECTION,null));
    boolean showTitle  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_TITLE, null));
    boolean showHeader  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_HEADER, null));
    boolean showRefresh  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_REFRESH_BUTTON, null));

    boolean showImage = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_ILLUSTRATION, null));
    boolean showDateCreated = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_DATE_CREATED,
                                                                               null));
    boolean showReadmore  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_READMORE, null));

    boolean showSummary  = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_SUMMARY, null));
    boolean showLink = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_LINK, null));
    boolean showRssLink = Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_RSSLINK, null));

    /** DISPLAY MODE */
    List<SelectItemOption<String>> displayModeOptions = new ArrayList<SelectItemOption<String>>();
    displayModeOptions.add(new SelectItemOption<String>(UICLVPortlet.DISPLAY_MODE_AUTOMATIC,
        UICLVPortlet.DISPLAY_MODE_AUTOMATIC));
    displayModeOptions.add(new SelectItemOption<String>(UICLVPortlet.DISPLAY_MODE_MANUAL,
        UICLVPortlet.DISPLAY_MODE_MANUAL));
    UIFormRadioBoxInput displayModeRadioBoxInput = new UIFormRadioBoxInput(DISPLAY_MODE_FORM_RADIO_BOX_INPUT,
                                                                           DISPLAY_MODE_FORM_RADIO_BOX_INPUT,
                                                                           displayModeOptions);
    displayModeRadioBoxInput.setValue(displayMode);

    /** ITEM PATH */
    UIFormStringInput itemPathInput =
        new UIFormStringInput(ITEM_PATH_FORM_STRING_INPUT, ITEM_PATH_FORM_STRING_INPUT, itemPath);
    itemPathInput.setReadOnly(true);
    itemPathInput.addValidator(MandatoryValidator.class);

    UIFormInputSetWithAction itemPathInputSet = new UIFormInputSetWithAction(ITEM_PATH_FORM_INPUT_SET);
    itemPathInputSet.setActionInfo(ITEM_PATH_FORM_STRING_INPUT, new String[] { "AddPath" }) ;
    itemPathInputSet.addUIFormInput(itemPathInput);

    /** ORDER BY */
    List<SelectItemOption<String>> orderByOptions = new ArrayList<SelectItemOption<String>>();
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_TITLE, NodetypeConstant.EXO_TITLE));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_CREATED, NodetypeConstant.EXO_DATE_CREATED));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_MODIFIED, 
        NodetypeConstant.EXO_LAST_MODIFIED_DATE));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_PUBLISHED, 
        NodetypeConstant.PUBLICATION_LIVE_DATE));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_DATE_START_EVENT, NodetypeConstant.EXO_START_EVENT));
    orderByOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_BY_INDEX, NodetypeConstant.EXO_INDEX));
    UIFormSelectBox orderBySelectBox = new UIFormSelectBox(ORDER_BY_FORM_SELECT_BOX, ORDER_BY_FORM_SELECT_BOX, orderByOptions);
    orderBySelectBox.setValue(orderBy);

    /** ORDER TYPE */
    List<SelectItemOption<String>> orderTypeOptions = new ArrayList<SelectItemOption<String>>();
    orderTypeOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_TYPE_DESCENDENT, "DESC"));    
    orderTypeOptions.add(new SelectItemOption<String>(UICLVPortlet.ORDER_TYPE_ASCENDENT, "ASC"));
    UIFormRadioBoxInput orderTypeRadioBoxInput = new UIFormRadioBoxInput(ORDER_TYPE_FORM_RADIO_BOX_INPUT,
                                                                         ORDER_TYPE_FORM_RADIO_BOX_INPUT,
                                                                         orderTypeOptions);

    UIFormSelectBox orderTypeSelectBox = new UIFormSelectBox(ORDER_TYPE_FORM_SELECT_BOX, ORDER_TYPE_FORM_SELECT_BOX, 
                                                             orderTypeOptions);
    orderTypeSelectBox.setValue(orderType);
    //orderTypeRadioBoxInput.setValue(orderType);

    /** HEADER */
    UIFormStringInput headerInput = new UIFormStringInput(HEADER_FORM_STRING_INPUT, HEADER_FORM_STRING_INPUT, header);

    /** AUTOMATIC DETECTION */
    UICheckBoxInput showAutomaticDetectionCheckBox = new UICheckBoxInput(SHOW_AUTOMATIC_DETECTION_CHECKBOX_INPUT,
                                                                         SHOW_AUTOMATIC_DETECTION_CHECKBOX_INPUT, null);
    showAutomaticDetectionCheckBox.setChecked(showAutomaticDetection);

    List<SelectItemOption<String>> formViewerTemplateList = new ArrayList<SelectItemOption<String>>();

    /** DISPLAY TEMPLATE */
    List<SelectItemOption<String>> viewerTemplateList = new ArrayList<SelectItemOption<String>>();
    if (appType.equals(CONTENT_LIST_TYPE) || appType.equals(CATEGORIES_CONTENT_TYPE)
        || appType.equals(UICLVPortlet.APPLICATION_CLV_BY_QUERY)) {
      viewerTemplateList.addAll(getTemplateList(TEMPLATE_STORAGE_FOLDER, DISPLAY_TEMPLATE_LIST));
    }
    if (appType.equals(CONTENT_LIST_TYPE) || appType.equals(CATOGORIES_NAVIGATION_TYPE)) {
      viewerTemplateList.addAll(getTemplateList(TEMPLATE_STORAGE_FOLDER,
                                                DISPLAY_TEMPLATE_CATEGORY));
    }
    Collections.sort(viewerTemplateList, new TemplateNameComparator());
    formViewerTemplateList.addAll(viewerTemplateList);

    UIFormSelectBox formViewTemplateSelector = new UIFormSelectBox(DISPLAY_TEMPLATE_FORM_SELECT_BOX,
                                                                   DISPLAY_TEMPLATE_FORM_SELECT_BOX,
                                                                   formViewerTemplateList);
    formViewTemplateSelector.setValue(displayTemplate);

    /** PAGINATOR TEMPLATE */
    List<SelectItemOption<String>> paginatorTemplateList = getTemplateList(TEMPLATE_STORAGE_FOLDER, PAGINATOR_TEMPLATE_CATEGORY);
    Collections.sort(paginatorTemplateList, new TemplateNameComparator());
    UIFormSelectBox paginatorTemplateSelector = new UIFormSelectBox(PAGINATOR_TEMPLATE_FORM_SELECT_BOX,
                                                                    PAGINATOR_TEMPLATE_FORM_SELECT_BOX,
                                                                    paginatorTemplateList);
    paginatorTemplateSelector.setValue(paginatorTemplate);

    /** ITEMS PER PAGE */
    UIFormStringInput itemsPerPageStringInput = new UIFormStringInput(ITEMS_PER_PAGE_FORM_STRING_INPUT,
                                                                      ITEMS_PER_PAGE_FORM_STRING_INPUT,
                                                                      itemsPerPage);
    itemsPerPageStringInput.addValidator(MandatoryValidator.class);
    itemsPerPageStringInput.addValidator(ZeroNumberValidator.class);
    itemsPerPageStringInput.addValidator(PositiveNumberFormatValidator.class);
    itemsPerPageStringInput.setMaxLength(3);

    /** SHOW TITLE */
    UICheckBoxInput showTitleCheckbox = new UICheckBoxInput(SHOW_TITLE_FORM_CHECKBOX_INPUT,
                                                            SHOW_TITLE_FORM_CHECKBOX_INPUT, null);
    showTitleCheckbox.setChecked(showTitle);

    /** SHOW HEADER */
    UICheckBoxInput showHeaderCheckBox = new UICheckBoxInput(SHOW_HEADER_FORM_CHECKBOX_INPUT,
                                                             SHOW_HEADER_FORM_CHECKBOX_INPUT, null);
    showHeaderCheckBox.setChecked(showHeader);

    /** SHOW REFRESH */
    UICheckBoxInput showRefreshCheckbox = new UICheckBoxInput(SHOW_REFRESH_FORM_CHECKBOX_INPUT,
                                                              SHOW_REFRESH_FORM_CHECKBOX_INPUT, null);
    showRefreshCheckbox.setChecked(showRefresh);

    /** SHOW_IMAGE */
    UICheckBoxInput showImageCheckbox = new UICheckBoxInput(SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT,
                                                            SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT, null);
    showImageCheckbox.setChecked(showImage);

    /** SHOW DATE CREATED */
    UICheckBoxInput showDateCreatedCheckbox = new UICheckBoxInput(SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT,
                                                                  SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT, null);
    showDateCreatedCheckbox.setChecked(showDateCreated);

    /** SHOW MORE LINK */
    UICheckBoxInput showMoreLinkCheckbox = new UICheckBoxInput(SHOW_READMORE_FORM_CHECKBOX_INPUT,
                                                               SHOW_READMORE_FORM_CHECKBOX_INPUT, null);
    showMoreLinkCheckbox.setChecked(showReadmore);

    /** SHOW SUMMARY */
    UICheckBoxInput showSummaryCheckbox = new UICheckBoxInput(SHOW_SUMMARY_FORM_CHECKBOX_INPUT,
                                                              SHOW_SUMMARY_FORM_CHECKBOX_INPUT, null);
    showSummaryCheckbox.setChecked(showSummary);

    /** SHOW LINK */
    UICheckBoxInput showLinkCheckbox = new UICheckBoxInput(SHOW_LINK_FORM_CHECKBOX_INPUT,
                                                           SHOW_LINK_FORM_CHECKBOX_INPUT, null);
    showLinkCheckbox.setChecked(showLink);

    /** SHOW RSS LINK */
    UICheckBoxInput showRssLinkCheckbox = new UICheckBoxInput(SHOW_RSSLINK_FORM_CHECKBOX_INPUT,
                                                              SHOW_RSSLINK_FORM_CHECKBOX_INPUT, null);
    showRssLinkCheckbox.setChecked(showRssLink);

    /** CONTEXTUAL FOLDER */
    List<SelectItemOption<String>> contextualFolderOptions = new ArrayList<SelectItemOption<String>>();
    contextualFolderOptions.add(new SelectItemOption<String>(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE,
        UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE));
    contextualFolderOptions.add(new SelectItemOption<String>(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_DISABLE,
        UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_DISABLE));
    UIFormRadioBoxInput contextualFolderRadioBoxInput = new UIFormRadioBoxInput(CONTEXTUAL_FOLDER_RADIOBOX_INPUT,
                                                                                CONTEXTUAL_FOLDER_RADIOBOX_INPUT,
                                                                                contextualFolderOptions);
    contextualFolderRadioBoxInput.setValue(contextualFolderMode);

    /** SHOW CLV BY */
    UIFormStringInput showClvByInput = new UIFormStringInput(SHOW_CLV_BY_STRING_INPUT, SHOW_CLV_BY_STRING_INPUT, showClvBy);

    /** TARGET PAGE */
    UIFormInputSetWithAction targetPageInputSet = new UIFormInputSetWithAction(TARGET_PAGE_FORM_INPUT_SET);
    UIFormStringInput basePathInput = new UIFormStringInput(TARGET_PAGE_FORM_STRING_INPUT,
                                                            TARGET_PAGE_FORM_STRING_INPUT,
                                                            targetPage);
    basePathInput.setValue(targetPage);
    basePathInput.setReadOnly(true);
    targetPageInputSet.setActionInfo(TARGET_PAGE_FORM_STRING_INPUT, new String[] {"SelectTargetPage"}) ;
    targetPageInputSet.addUIFormInput(basePathInput);

    /** CACHE MODE */
    List<SelectItemOption<String>> cacheOptions = new ArrayList<SelectItemOption<String>>();
    cacheOptions.add(new SelectItemOption<String>(ENABLE_CACHE, ENABLE_CACHE));
    cacheOptions.add(new SelectItemOption<String>(DISABLE_CACHE, DISABLE_CACHE));
    UIFormRadioBoxInput cacheEnableRadioBoxInput = new UIFormRadioBoxInput(CACHE_ENABLE_RADIOBOX_INPUT,
                                                                           CACHE_ENABLE_RADIOBOX_INPUT,
                                                                           cacheOptions);
    cacheEnableRadioBoxInput.setValue("true".equals(isCacheEnabled)? ENABLE_CACHE : DISABLE_CACHE);

    /** WORKSPACE */
    List<SelectItemOption<String>> workspaceOptions = new ArrayList<SelectItemOption<String>>();

    String[] workspaceList = WCMCoreUtils.getRepository().getWorkspaceNames();

    for (String wkspace : workspaceList) {
      workspaceOptions.add(new SelectItemOption<String>(wkspace, wkspace));
    }

    UIFormSelectBox workspaceSelector = new UIFormSelectBox(WORKSPACE_FORM_SELECT_BOX,
                                                            WORKSPACE_FORM_SELECT_BOX,
                                                            workspaceOptions);
    workspaceSelector.setValue(workspace);

    /** CONTENT BY QUERY */
    UIFormTextAreaInput queryTextAreaInput = new UIFormTextAreaInput(CONTENT_BY_QUERY_TEXT_AREA,
                                                                     CONTENT_BY_QUERY_TEXT_AREA,
                                                                     contentByQuery);

    /** ALLOW DYNAMIC URL */
    UIFormStringInput showScvWithInput = new UIFormStringInput(SHOW_SCV_WITH_STRING_INPUT,
                                                               SHOW_SCV_WITH_STRING_INPUT,
                                                               showScvWith);
    if (appType.equals(CATOGORIES_NAVIGATION_TYPE)) {
      //Disable option
      displayModeRadioBoxInput.setDisabled(true);
      showAutomaticDetectionCheckBox.setDisabled(true);
      showImageCheckbox.setDisabled(true);
      showSummaryCheckbox.setDisabled(true);
      showDateCreatedCheckbox.setDisabled(true);
      showLinkCheckbox.setDisabled(true);
      showRefreshCheckbox.setDisabled(true);
      showMoreLinkCheckbox.setDisabled(true);
      showRssLinkCheckbox.setDisabled(true);
      showScvWithInput.setDisabled(true);
    }
    UIFormInputSet uiCLVContentTab = new UIFormInputSet(CONTENT_TAB) ;
    uiCLVContentTab.addUIFormInput(displayModeRadioBoxInput);
    uiCLVContentTab.addUIFormInput((UIFormInputSet)itemPathInputSet);
    uiCLVContentTab.addUIFormInput(orderBySelectBox);
    uiCLVContentTab.addUIFormInput(orderTypeSelectBox);
    uiCLVContentTab.addUIFormInput(orderTypeRadioBoxInput);
    setSelectedTab(CONTENT_TAB);
    addUIComponentInput(uiCLVContentTab) ;


    UIFormInputSet uiCLVDisplayTab = new UIFormInputSet(DISPLAY_TAB) ;
    uiCLVDisplayTab.addUIFormInput(headerInput);
    uiCLVDisplayTab.addUIFormInput(showAutomaticDetectionCheckBox);
    uiCLVDisplayTab.addUIFormInput(formViewTemplateSelector);
    uiCLVDisplayTab.addUIFormInput(paginatorTemplateSelector);
    uiCLVDisplayTab.addUIFormInput(itemsPerPageStringInput);

    uiCLVDisplayTab.addUIFormInput(showTitleCheckbox);
    uiCLVDisplayTab.addUIFormInput(showHeaderCheckBox);
    uiCLVDisplayTab.addUIFormInput(showRefreshCheckbox);
    uiCLVDisplayTab.addUIFormInput(showImageCheckbox);
    uiCLVDisplayTab.addUIFormInput(showDateCreatedCheckbox);
    uiCLVDisplayTab.addUIFormInput(showMoreLinkCheckbox);
    uiCLVDisplayTab.addUIFormInput(showSummaryCheckbox);
    uiCLVDisplayTab.addUIFormInput(showLinkCheckbox);
    uiCLVDisplayTab.addUIFormInput(showRssLinkCheckbox);
    addUIComponentInput(uiCLVDisplayTab) ;


    UIFormInputSet uiCLVAdvancedTab = new UIFormInputSet(ADVANCED_TAB) ;
    uiCLVAdvancedTab.addUIFormInput(contextualFolderRadioBoxInput);
    uiCLVAdvancedTab.addUIFormInput(showClvByInput);
    uiCLVAdvancedTab.addUIFormInput((UIFormInputSet)targetPageInputSet);
    uiCLVAdvancedTab.addUIFormInput(showScvWithInput);
    uiCLVAdvancedTab.addUIFormInput(cacheEnableRadioBoxInput);


    if (this.isContentListByQuery()) {
      uiCLVAdvancedTab.addUIFormInput(workspaceSelector);
      uiCLVAdvancedTab.addUIFormInput(queryTextAreaInput);     
    }
    addUIComponentInput(uiCLVAdvancedTab) ;

    if ((contextualFolderMode != null && contextualFolderMode.equals(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE))
        || this.isContentListByQuery()) {
      isShowAdvancedBlock_ = true;
    } else {
      isShowAdvancedBlock_ = false;
    }

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
    List<Node> templateNodeList = templateManagerService.getTemplatesByCategory(portletName,
                                                                                category,
                                                                                WCMCoreUtils.getUserSessionProvider());

    for (Node templateNode : templateNodeList) {
      SelectItemOption<String> template = new SelectItemOption<String>();
      template.setLabel(templateNode.getName());
      template.setValue(templateNode.getPath());
      templateOptionList.add(template);
    }
    return templateOptionList;
  }
  public boolean isCategoriesNavigation() {
    return appType.equals(CATOGORIES_NAVIGATION_TYPE);
  }

  /**
   *
   * @return True if is a CLV by Query
   */
  public boolean isContentListByQuery() {
    return appType.equals(UICLVPortlet.APPLICATION_CLV_BY_QUERY);
  }
  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    if (selectField != null && value != null) {
      String sValue = (String) value;
      String titles="";
      String displayMode = ((UIFormRadioBoxInput) findComponentById(UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
      if (ITEM_PATH_FORM_STRING_INPUT.equals(selectField) && UICLVPortlet.DISPLAY_MODE_MANUAL.equals(displayMode)) {
        items = Arrays.asList(sValue.split(";"));
        titles = getTitles(sValue);
        savedPath = sValue;
        getUIStringInput(selectField).setValue(titles);
      } else if (TARGET_PAGE_FORM_STRING_INPUT.equals(selectField)){
        getUIStringInput(selectField).setValue(sValue);
      }else {
        items = new ArrayList<String>();
        String[] values = sValue.split(":");
        if (values.length == 4) {
          this.setDriveName(values[0]);
          //check if drive is selected instead of folder
          ManageDriveService managerDriveService = this.getApplicationComponent(ManageDriveService.class);
          for (DriveData data : managerDriveService.getAllDrives()) {
            if (data.getHomePath().equals(values[3])) {
              this.setDriveName(data.getName());
            }
          }
          sValue = sValue.substring(values[0].length() + 1);
        }
        titles = getTitle(sValue);
        getUIStringInput(selectField).setValue(titles);
        savedPath = sValue;
      }
    }
    Utils.closePopupWindow(this, popupId);
  }

  private String getTitles(String itemPath) throws RepositoryException {
    if (itemPath == null || itemPath.length() == 0)
      return "";
    StringBuffer titles = new StringBuffer();
    List<String> tmpItems;
    tmpItems = Arrays.asList(itemPath.split(";"));
    for (String item : tmpItems) {
      String title = getTitle(item);
      if (title != null) {
        if (titles.length() > 0) {
          titles.append(";").append(title);
        } else {
          titles.append(title);
        }
      }
    }
    return titles.toString();
  }

  /**
   *
   * get the realnode with a path
   * @param itemPath
   * @return the realnode. null if the path is not ok
   */
  private Node getRealNode(String itemPath) throws RepositoryException{
    String strRepository, strWorkspace, strIdentifier;
    int repoIndex, wsIndex;
    if (itemPath==null || itemPath.length() == 0)
      return null;
    repoIndex = itemPath.indexOf(':');
    wsIndex = itemPath.lastIndexOf(':');
    strRepository = itemPath.substring(0, repoIndex);
    strWorkspace = itemPath.substring(repoIndex+1, wsIndex);
    strIdentifier = itemPath.substring(wsIndex +1);
    Node selectedNode = Utils.getRealNode(Text.escapeIllegalJcrChars(strRepository),
                                          Text.escapeIllegalJcrChars(strWorkspace),
                                          Text.escapeIllegalJcrChars(strIdentifier),
                                          false);
    return selectedNode;
  }
  
 /**
   *
   * @param itemPath The path
   * @return The title
   * @throws RepositoryException
   */
  private String getTitle(String itemPath) throws RepositoryException {
    Node selectedNode = getRealNode(itemPath);
    if (selectedNode==null) return null;
    String title = null;
    if (selectedNode.hasProperty("exo:title")) {
      title = selectedNode.getProperty("exo:title").getValue().getString();
    }
    if (selectedNode.hasNode("jcr:content")) {
      Node content = selectedNode.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        }
        catch (PathNotFoundException e) {
          title = null;
        }
        catch (RepositoryException e) {
          title = null;
        }
        catch (IndexOutOfBoundsException e) {
          title = null;
        }
      }
    }
    if (title==null) title = selectedNode.getName();

    return ContentReader.getUnescapeIllegalJcrContent(title);
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SaveActionListener extends EventListener<UICLVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();

      /** GET VALUES FROM UIFORM */
      String displayMode = ((UIFormRadioBoxInput) clvConfig.findComponentById(
                                                                              UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT)).getValue();
      String itemPath = clvConfig.getSavedPath();

      if (itemPath == null || itemPath.length() == 0
          || (itemPath.contains(";") && displayMode.equals(UICLVPortlet.DISPLAY_MODE_AUTOMATIC))
          || (!itemPath.contains(";") && displayMode.equals(UICLVPortlet.DISPLAY_MODE_MANUAL))) {
        Utils.createPopupMessage(clvConfig,
                                 "UICLVConfig.msg.not-valid-path",
                                 null,
                                 ApplicationMessage.WARNING);
        WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
        requestContext.addUIComponentToUpdateByAjax(clvConfig);
        return;
      }
      String orderBy = clvConfig.getUIFormSelectBox(ORDER_BY_FORM_SELECT_BOX).getValue();
      String orderType = clvConfig.getUIFormSelectBox(ORDER_TYPE_FORM_SELECT_BOX).getValue();

      String header = clvConfig.getUIStringInput(UICLVConfig.HEADER_FORM_STRING_INPUT).getValue();
      if (header == null) header = "";
      String showAutomaticDetection = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_AUTOMATIC_DETECTION_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String displayTemplate = clvConfig.getUIFormSelectBox(UICLVConfig.DISPLAY_TEMPLATE_FORM_SELECT_BOX).getValue();
      String paginatorTemplate = clvConfig.getUIFormSelectBox(UICLVConfig.PAGINATOR_TEMPLATE_FORM_SELECT_BOX).getValue();
      String itemsPerPage = clvConfig.getUIStringInput(UICLVConfig.ITEMS_PER_PAGE_FORM_STRING_INPUT).getValue();

      String showTitle = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_TITLE_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showHeader = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_HEADER_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showRefresh = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_REFRESH_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";

      String showImage = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_ILLUSTRATION_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showDateCreated = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_DATE_CREATED_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showMoreLink = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_READMORE_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";

      String showSummary = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_SUMMARY_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showLink = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_LINK_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";
      String showRssLink = clvConfig.getUICheckBoxInput(UICLVConfig.SHOW_RSSLINK_FORM_CHECKBOX_INPUT)
          .isChecked() ? "true" : "false";

      String contextualFolderMode = ((UIFormRadioBoxInput) clvConfig.findComponentById(
                                                                                       UICLVConfig.CONTEXTUAL_FOLDER_RADIOBOX_INPUT)).getValue();
      String showClvBy = clvConfig.getUIStringInput(UICLVConfig.SHOW_CLV_BY_STRING_INPUT).getValue();
      if (showClvBy == null || showClvBy.length() == 0)
        showClvBy = UICLVPortlet.DEFAULT_SHOW_CLV_BY;
      String targetPage = clvConfig.getUIStringInput(UICLVConfig.TARGET_PAGE_FORM_STRING_INPUT).getValue();
      String showScvWith = clvConfig.getUIStringInput(UICLVConfig.SHOW_SCV_WITH_STRING_INPUT).getValue();
      if (showScvWith == null || showScvWith.length() == 0)
        showScvWith = UICLVPortlet.DEFAULT_SHOW_SCV_WITH;

      String cacheEnabled = ((UIFormRadioBoxInput) clvConfig.
          findComponentById(UICLVConfig.CACHE_ENABLE_RADIOBOX_INPUT)).getValue();

      /** SET VALUES TO PREFERENCES */
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_DISPLAY_MODE, displayMode);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ITEM_PATH, itemPath);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ITEM_DRIVE, clvConfig.getDriveName());

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ORDER_BY, orderBy);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, orderType);

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_HEADER, header);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_AUTOMATIC_DETECTION, showAutomaticDetection);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_DISPLAY_TEMPLATE, displayTemplate);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_PAGINATOR_TEMPLATE, paginatorTemplate);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, itemsPerPage);

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_TITLE, showTitle);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_HEADER, showHeader);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_REFRESH_BUTTON, showRefresh);

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_ILLUSTRATION, showImage);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_DATE_CREATED, showDateCreated);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_READMORE, showMoreLink);

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_SUMMARY, showSummary);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_LINK, showLink);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_RSSLINK, showRssLink);

      portletPreferences.setValue(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER, contextualFolderMode);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_CLV_BY, showClvBy);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_TARGET_PAGE, targetPage);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_SHOW_SCV_WITH, showScvWith);
      portletPreferences.setValue(UICLVPortlet.PREFERENCE_CACHE_ENABLED, ENABLE_CACHE.equals(cacheEnabled)?"true":"false");
      String appType = portletPreferences.getValue(UICLVPortlet.PREFERENCE_APPLICATION_TYPE, null);
      if (UICLVPortlet.APPLICATION_CLV_BY_QUERY.equals(appType)) {
        String workspace = ((UIFormSelectBox)clvConfig.findComponentById(UICLVConfig.WORKSPACE_FORM_SELECT_BOX)).getValue();
        String query = ((UIFormTextAreaInput) clvConfig.findComponentById(UICLVConfig.CONTENT_BY_QUERY_TEXT_AREA)).getValue();
        if (query == null) {
          query = "";
        }
        portletPreferences.setValue(UICLVPortlet.PREFERENCE_WORKSPACE, workspace);
        portletPreferences.setValue(UICLVPortlet.PREFERENCE_CONTENTS_BY_QUERY, query);
      }
      portletPreferences.store();

      UICLVPortlet portlet = clvConfig.getAncestorOfType(UICLVPortlet.class);

      if (Utils.isPortalEditMode()) {
        portlet.updatePortlet();
      } else {
        if (clvConfig.getModeInternal()) {
          portlet.changeToViewMode();
        }else {
          Utils.closePopupWindow(clvConfig, "UIViewerManagementPopupWindow");
          portlet.updatePortlet();
        }
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
   */
  public static class CancelActionListener extends EventListener<UICLVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      if (!Utils.isPortalEditMode()) {
        if (clvConfig.getModeInternal()) {
          UICLVPortlet portlet = clvConfig.getAncestorOfType(UICLVPortlet.class);
          portlet.changeToViewMode();
        }else {
          Utils.closePopupWindow(clvConfig, "UIViewerManagementPopupWindow");
        }
      }
    }
  }

	/**
	 * The listener interface for receiving selectTabAction events.
	 * The class that is interested in processing a selectTabAction
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addSelectTabActionListener<code> method. When
	 * the selectTabAction event occurs, that object's appropriate
	 * method is invoked.
	 */
	static public class SelectTabActionListener extends EventListener<UICLVConfig> {

		/* (non-Javadoc)
 * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
 */
		public void execute(Event<UICLVConfig> event) throws Exception {
			WebuiRequestContext context = event.getRequestContext();
			String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
			if (renderTab == null) {
				return;
			}
			event.getSource().setSelectedTab(renderTab);
			event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
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
   */
  public static class AddPathActionListener extends EventListener<UICLVConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      UIFormRadioBoxInput modeBoxInput = (UIFormRadioBoxInput) clvConfig.findComponentById(
                                                                                           UICLVConfig.DISPLAY_MODE_FORM_RADIO_BOX_INPUT);
      String mode = modeBoxInput.getValue();
      if (mode.equals(UICLVPortlet.DISPLAY_MODE_AUTOMATIC)) {
        UIContentSelectorFolder contentSelector = clvConfig.createUIComponent(UIContentSelectorFolder.class, null, null);
        UIContentBrowsePanelFolder folderContentSelector= contentSelector.getChild(UIContentBrowsePanelFolder.class);
        String location = clvConfig.getSavedPath();
        String[] locations = (location == null) ? null : location.split(":");
        Node node = (locations != null && locations.length >= 3) ? Utils.getViewableNodeByComposer(locations[0],
                                                                                                   locations[1],
                                                                                                   locations[2])
                                                                                                   : null;
        contentSelector.init(clvConfig.getDriveName(),
                             fixPath(node == null ? "" : node.getPath(),
                                                  clvConfig,
                                                  (locations != null && locations.length > 0) ? locations[0] : null));
        folderContentSelector.setSourceComponent(clvConfig, new String[] { UICLVConfig.ITEM_PATH_FORM_STRING_INPUT });
        Utils.createPopupWindow(clvConfig, contentSelector, UIContentSelector.FOLDER_PATH_SELECTOR_POPUP_WINDOW, 800);
        clvConfig.setPopupId(UIContentSelector.FOLDER_PATH_SELECTOR_POPUP_WINDOW);
      } else {
        UIContentSelectorMulti contentSelector = clvConfig.createUIComponent(UIContentSelectorMulti.class, null, null);
        UIContentBrowsePanelMulti multiContentSelector= contentSelector.getChild(UIContentBrowsePanelMulti.class);
        multiContentSelector.setSourceComponent(clvConfig, new String[] { UICLVConfig.ITEM_PATH_FORM_STRING_INPUT });
        String itemPath = clvConfig.getSavedPath();
        if (itemPath != null && itemPath.contains(";"))
          multiContentSelector.setItemPaths(itemPath);
        contentSelector.init();
        Utils.createPopupWindow(clvConfig, contentSelector, UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW, 800);
        clvConfig.setPopupId(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
      }
    }

    private String fixPath(String path, UICLVConfig clvConfig, String repository) throws Exception {
      if (path == null || path.length() == 0 || repository == null || repository.length() == 0 ||
          clvConfig.getDriveName() == null || clvConfig.getDriveName().length() == 0)
        return "";

      ManageDriveService managerDriveService = clvConfig.getApplicationComponent(ManageDriveService.class);
      DriveData driveData = managerDriveService.getDriveByName(clvConfig.getDriveName());
      if (!path.startsWith(driveData.getHomePath()))
        return "";
      if ("/".equals(driveData.getHomePath()))
        return path;
      return path.substring(driveData.getHomePath().length());
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
      StringBuffer sb = new StringBuffer("");
      for (String item : items) {
        sb.append(item).append(";");
      }
      String itemPath = sb.toString();
      clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).setValue(clvConfig.getTitles(itemPath));
      clvConfig.setSavedPath(itemPath);

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
      StringBuffer sb = new StringBuffer("");
      for (String item : items) {
        sb.append(item).append(";");
      }
      String itemPath = sb.toString();
      clvConfig.getUIStringInput(UICLVConfig.ITEM_PATH_FORM_STRING_INPUT).setValue(clvConfig.getTitles(itemPath));
      clvConfig.setSavedPath(itemPath);
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

  public static class ShowAdvancedBlockActionListener extends EventListener<UICLVConfig> {
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVConfig> event) throws Exception {
      UICLVConfig clvConfig = event.getSource();
      String showValue = event.getRequestContext().getRequestParameter(OBJECTID);
      clvConfig.isShowAdvancedBlock_ = "true".equalsIgnoreCase(showValue);
      event.getRequestContext().addUIComponentToUpdateByAjax(clvConfig);
    }
  }
  private boolean modeInternal = false;
  public void setModeInternal(boolean value) {
    this.modeInternal = value;
  }
  public boolean getModeInternal() {
    return this.modeInternal;
  }

  private class TemplateNameComparator implements Comparator<SelectItemOption<String>> {
    public int compare(SelectItemOption<String> item1,SelectItemOption<String> item2) {
      String s1 = item1.getLabel().toLowerCase();
      String s2 = item2.getLabel().toLowerCase();
      return s1.compareTo(s2);
    }
  }
}
