/***************************************************************************
 * Copyright 2001-2010 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.wcm.webui.scv;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.wcm.webui.selector.content.one.UIContentBrowsePanelOne;
import org.exoplatform.wcm.webui.selector.content.one.UIContentSelectorOne;
import org.exoplatform.wcm.webui.selector.page.UIPageSelector;
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
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.input.UICheckBoxInput;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by The eXo Platform SARL
 * Author : Nguyen The Vinh
 *          vinh.nguyen@exoplatform.com
 * Jul 16, 2010
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/SingleContentViewer/UISCVPreferences.gtmpl",
                 events = {
                   @EventConfig(listeners = UISCVPreferences.SaveActionListener.class),
                   @EventConfig(listeners = UISCVPreferences.AddPathActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UISCVPreferences.CancelActionListener.class, phase = Phase.DECODE),
	                 @EventConfig(listeners = UISCVPreferences.SelectTabActionListener.class, phase = Phase.DECODE),
                   @EventConfig(listeners = UISCVPreferences.SelectTargetPageActionListener.class, phase = Phase.DECODE)
                 }
    )
public class UISCVPreferences extends UIFormTabPane implements UISelectable{

  /** The Constant ITEM_PATH_FORM_INPUT_SET. */
  public final static String ITEM_PATH_FORM_INPUT_SET     = "UISCVConfigItemPathFormInputSet";

  public final static String CONTENT_FORM_INPUT_SET       = "UISCVConfigContentFormInputSet";
  public final static String DISPLAY_FORM_INPUT_SET       = "UISCVConfigDisplayFormInputSet";
  public final static String PRINT_FORM_INPUT_SET         = "UISCVConfigPrintFormInputSet";
  public final static String ADVANCED_FORM_INPUT_SET      = "UISCVConfigAdvancedFormInputSet";

  public static final String CONTENT_PATH_INPUT           = "UISCVContentPathConfigurationInputBox";

  public static final String SHOW_TITLE_CHECK_BOX         = "UISCVShowTitleConfigurationCheckBox";

  public static final String SHOW_DATE_CHECK_BOX          = "UISCVShowDateConfigurationCheckBox";

  public static final String SHOW_OPION_BAR_CHECK_BOX     = "UISCVShowOptionBarConfigurationCheckBox";

  public static final String CONTEXTUAL_SELECT_RADIO_BOX  = "UISCVContextualRadioBox";

  public static final String PARAMETER_INPUT_BOX          = "UISCVParameterInputBox";

  public static final String CACHE_ENABLE_SELECT_RADIO_BOX = "UISCVCacheRadioBox";

  public final static String PRINT_PAGE_FORM_INPUT_SET    = "UISCVConfigPrintPageFormInputSet";
  public static final String PRINT_VIEW_PAGE_INPUT        = "UISCVPrintViewPageInput";
  /** The Constant PRINT_PAGE_SELECTOR_POPUP. */
  public final static String PRINT_PAGE_SELECTOR_POPUP    = "UISCVConfigPrintPageSelectorPopupWindow";

  public static final String PRINT_PAGE_PARAMETER_INPUT   = "UISCVPrintPageParameter";

  public static final String ENABLE_STRING                = "Enable";
  public static final String DISABLE_STRING               = "Disable";

  protected PortletPreferences portletPreferences;

  protected String contentSelectorID;
  protected String selectedNodeUUID =null;
  protected String selectedNodeReporitory =null;
  protected String selectedNodeWorkspace =null;
  protected String selectedNodeDrive = null;
  protected String selectedNodePath = null;

  private UIFormStringInput             txtContentPath, txtPrintPage, txtPrintPageParameter;

  private UICheckBoxInput               chkShowTitle;
  private UICheckBoxInput               chkShowDate;
  private UICheckBoxInput               chkShowOptionBar;
  private UIFormRadioBoxInput           contextOptionsRadioInputBox;
  private UIFormRadioBoxInput           cacheOptionsRadioInputBox;

  public UISCVPreferences() throws Exception{
    super("UISCVPreferences");
    portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest().getPreferences();
    initComponent();
    setActions(new String[] { "Save", "Cancel" });
  }

  /**
   * Initialize the preferences setting form
   *
   * @throws Exception
   */

  public void initComponent() throws Exception{
    /** Content name **/
    String strNodeName = getNodeNameByPreferences();
    txtContentPath = new UIFormStringInput(CONTENT_PATH_INPUT, CONTENT_PATH_INPUT, strNodeName);
    txtContentPath.setReadOnly(true);

    UIFormInputSetWithAction itemPathInputSet = new UIFormInputSetWithAction(ITEM_PATH_FORM_INPUT_SET);
    itemPathInputSet.setActionInfo(CONTENT_PATH_INPUT, new String[] { "AddPath" }) ;
    itemPathInputSet.addUIFormInput(txtContentPath);
    UIFormInputSetWithAction contentInputSet = new UIFormInputSetWithAction(CONTENT_FORM_INPUT_SET);
    contentInputSet.addUIFormInput((UIFormInputSet)itemPathInputSet);
    setSelectedTab(CONTENT_FORM_INPUT_SET);

    /** Option Show Title/Show Date/Show OptionBar **/
    boolean blnShowTitle = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_TITLE,
                                                                            null));
    chkShowTitle = new UICheckBoxInput(SHOW_TITLE_CHECK_BOX, SHOW_TITLE_CHECK_BOX, null);
    chkShowTitle.setChecked(blnShowTitle);

    boolean blnShowDate = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_DATE,
                                                                           null));
    chkShowDate = new UICheckBoxInput(SHOW_DATE_CHECK_BOX, SHOW_DATE_CHECK_BOX, null);
    chkShowDate.setChecked(blnShowDate);

    boolean blnShowOptionBar = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR,
                                                                                null));
    chkShowOptionBar = new UICheckBoxInput(SHOW_OPION_BAR_CHECK_BOX, SHOW_OPION_BAR_CHECK_BOX, null);
    chkShowOptionBar.setChecked(blnShowOptionBar);

    UIFormInputSetWithAction displayInputSet = new UIFormInputSetWithAction(DISPLAY_FORM_INPUT_SET);
    displayInputSet.addChild(chkShowTitle);
    displayInputSet.addChild(chkShowDate);
    displayInputSet.addChild(chkShowOptionBar);


    /** CONTEXTUAL MODE */
    boolean isShowContextOption = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE,
        "false"));
    List<SelectItemOption<String>> contextOptions = new ArrayList<SelectItemOption<String>>();
    contextOptions.add(new SelectItemOption<String>(ENABLE_STRING, ENABLE_STRING));
    contextOptions.add(new SelectItemOption<String>(DISABLE_STRING, DISABLE_STRING));
    contextOptionsRadioInputBox = new UIFormRadioBoxInput(CONTEXTUAL_SELECT_RADIO_BOX,
                                                          CONTEXTUAL_SELECT_RADIO_BOX,
                                                          contextOptions);
    contextOptionsRadioInputBox.setValue(isShowContextOption?ENABLE_STRING:DISABLE_STRING);

    String strParameterName = portletPreferences.getValue(UISingleContentViewerPortlet.PARAMETER, null);
    UIFormStringInput txtParameterName = new UIFormStringInput(PARAMETER_INPUT_BOX, strParameterName);

    /** CACHE MANAGEMENT */
    boolean isCacheEnabled = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.ENABLE_CACHE,
        "false"));

    List<SelectItemOption<String>> cacheOptions = new ArrayList<SelectItemOption<String>>();
    cacheOptions.add(new SelectItemOption<String>(ENABLE_STRING, ENABLE_STRING));
    cacheOptions.add(new SelectItemOption<String>(DISABLE_STRING, DISABLE_STRING));
    cacheOptionsRadioInputBox = new UIFormRadioBoxInput(CACHE_ENABLE_SELECT_RADIO_BOX,
                                                        CACHE_ENABLE_SELECT_RADIO_BOX,
                                                        cacheOptions);
    cacheOptionsRadioInputBox.setValue(isCacheEnabled ? ENABLE_STRING : DISABLE_STRING);

    UIFormInputSetWithAction advancedInputSet = new UIFormInputSetWithAction(ADVANCED_FORM_INPUT_SET);
    advancedInputSet.addChild(cacheOptionsRadioInputBox);
    advancedInputSet.addChild(txtParameterName);
    advancedInputSet.addChild(contextOptionsRadioInputBox);

    /** PRINT PAGE */
    String strPrintParameterName = portletPreferences.getValue(UISingleContentViewerPortlet.PRINT_PARAMETER, null);
    txtPrintPageParameter = new UIFormStringInput(PRINT_PAGE_PARAMETER_INPUT, strPrintParameterName);


    /** TARGET PAGE */
    String strPrintPageName = portletPreferences.getValue(UISingleContentViewerPortlet.PRINT_PAGE, null);
    UIFormInputSetWithAction targetPageInputSet = new UIFormInputSetWithAction(PRINT_PAGE_FORM_INPUT_SET);
    txtPrintPage = new UIFormStringInput(PRINT_VIEW_PAGE_INPUT, PRINT_VIEW_PAGE_INPUT, strPrintPageName);
    txtPrintPage.setValue(strPrintPageName);
    txtPrintPage.setReadOnly(true);
    targetPageInputSet.setActionInfo(PRINT_VIEW_PAGE_INPUT, new String[] {"SelectTargetPage"}) ;
    targetPageInputSet.addUIFormInput(txtPrintPage);

    UIFormInputSetWithAction printInputSet = new UIFormInputSetWithAction(PRINT_FORM_INPUT_SET);
    printInputSet.addChild(txtPrintPageParameter);
    printInputSet.addChild(targetPageInputSet);


    addChild(contentInputSet);
    addChild(displayInputSet);
    addChild(printInputSet);
    addChild(advancedInputSet);
  }

  /**
   * ActionListener: save preferences action
   * @author exo.VinhNT
   *
   */
  public static class SaveActionListener extends EventListener<UISCVPreferences> {
    public void execute(Event<UISCVPreferences> event) throws Exception {
      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      UISCVPreferences uiSCVPref = event.getSource();
      PortletPreferences portletPreferences = ((PortletRequestContext) event.getRequestContext()).getRequest()
          .getPreferences();
      UIFormInputSetWithAction displayInputSet = uiSCVPref.findComponentById(DISPLAY_FORM_INPUT_SET);
      String strShowTitle = displayInputSet.getUICheckBoxInput(SHOW_TITLE_CHECK_BOX).isChecked() ? "true" : "false";
      String strShowDate = displayInputSet.getUICheckBoxInput(SHOW_DATE_CHECK_BOX).isChecked() ? "true" : "false";
      String strShowOptionBar = displayInputSet.getUICheckBoxInput(SHOW_OPION_BAR_CHECK_BOX) .isChecked() ? "true" : "false";

      String strIsContextEnable = ((UIFormRadioBoxInput) uiSCVPref.findComponentById(CONTEXTUAL_SELECT_RADIO_BOX)).getValue();
      UIFormInputSetWithAction advancedInputSet = uiSCVPref.findComponentById(ADVANCED_FORM_INPUT_SET);      
      strIsContextEnable = strIsContextEnable.equals(ENABLE_STRING) ? "true":"false";
      String strParameterName = advancedInputSet.getUIStringInput(PARAMETER_INPUT_BOX).getValue();
      String strIsCacheEnabled = ((UIFormRadioBoxInput) advancedInputSet.getChildById(CACHE_ENABLE_SELECT_RADIO_BOX)).getValue();
      strIsCacheEnabled = ENABLE_STRING.equals(strIsCacheEnabled) ? "true" : "false";


      UIFormInputSetWithAction printInputSet = uiSCVPref.findComponentById(PRINT_FORM_INPUT_SET);
      String strPrintPageName = printInputSet.getUIStringInput(PRINT_VIEW_PAGE_INPUT).getValue();
      String strPrintParameterName  = printInputSet.getUIStringInput(PRINT_PAGE_PARAMETER_INPUT).getValue();



      if (!Boolean.parseBoolean(strIsContextEnable)) {
        if (uiSCVPref.getSelectedNodeUUID() != null) {
          if (uiSCVPref.getSelectedNodeUUID().length() == 0) {
            Utils.createPopupMessage(uiSCVPref,
                                     "UISCVPreferences.msg.not-valid-path",
                                     null,
                                     ApplicationMessage.WARNING);
            requestContext.addUIComponentToUpdateByAjax(uiSCVPref);
            return;
          }
        } else {
          Utils.createPopupMessage(uiSCVPref,
                                   "UISCVPreferences.msg.not-valid-path",
                                   null,
                                   ApplicationMessage.WARNING);
          requestContext.addUIComponentToUpdateByAjax(uiSCVPref);
          return;
        }
      }
      portletPreferences.setValue(UISingleContentViewerPortlet.REPOSITORY, uiSCVPref.getSelectedNodeRepository());
      portletPreferences.setValue(UISingleContentViewerPortlet.WORKSPACE, uiSCVPref.getSelectedNodeWorkspace());
      portletPreferences.setValue(UISingleContentViewerPortlet.IDENTIFIER, uiSCVPref.getSelectedNodeUUID()) ;
      portletPreferences.setValue(UISingleContentViewerPortlet.DRIVE, uiSCVPref.getSelectedNodeDrive());

      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_TITLE, strShowTitle);
      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_DATE, strShowDate);
      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR, strShowOptionBar);
      portletPreferences.setValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, strIsContextEnable);
      portletPreferences.setValue(UISingleContentViewerPortlet.PARAMETER, strParameterName);
      portletPreferences.setValue(UISingleContentViewerPortlet.PRINT_PAGE, strPrintPageName);
      portletPreferences.setValue(UISingleContentViewerPortlet.PRINT_PARAMETER, strPrintParameterName);
      portletPreferences.setValue(UISingleContentViewerPortlet.ENABLE_CACHE, strIsCacheEnabled);
      portletPreferences.store();
      if (uiSCVPref.getInternalPreferencesMode()) {
        if (!Utils.isPortalEditMode()) {
          uiSCVPref.getAncestorOfType(UISingleContentViewerPortlet.class).changeToViewMode();
        }
      } else {
        Utils.closePopupWindow(uiSCVPref, UISingleContentViewerPortlet.UIPreferencesPopupID);
      }
    }
  }
  public static class CancelActionListener extends EventListener<UISCVPreferences> {
    public void execute(Event<UISCVPreferences> event) throws Exception {
      UISCVPreferences uiSCVPref = event.getSource();
      if (uiSCVPref.getInternalPreferencesMode()) {
        if (!Utils.isPortalEditMode()) {
          uiSCVPref.getAncestorOfType(UISingleContentViewerPortlet.class).changeToViewMode();
        }
      } else {
        Utils.closePopupWindow(uiSCVPref, UISingleContentViewerPortlet.UIPreferencesPopupID);
      }
    }
  }
  /**
   * setSelectedNodeInfo: Save temporary data when user select a node from ContentSelector
   * @param nodeUUID
   * @param nodeRepo
   * @param nodeWS
   */
  public void setSelectedNodeInfo(String nodeUUID, String nodeRepo, String nodeWS, String nodeDrive) {
    this.selectedNodeUUID = nodeUUID;
    this.selectedNodeReporitory = nodeRepo;
    this.selectedNodeWorkspace = nodeWS;
    this.selectedNodeDrive = nodeDrive;
  }
  public void setSelectedNodePath(String path)
  {
    this.selectedNodePath = path;
  }
  public String getSelectedNodePath() {
    return this.selectedNodePath;
  }
  /**
   * Get the temporary node UUID
   * @return
   */
  public String getSelectedNodeUUID() {
    return this.selectedNodeUUID;
  }
  /**
   * Get the temporary Node Repository string
   * @return
   */
  public String getSelectedNodeRepository() {
    return this.selectedNodeReporitory;
  }
  /**
   * Get the temporary node Workspace string
   * @return
   */
  public String getSelectedNodeWorkspace() {
    return this.selectedNodeWorkspace;
  }

  /**
   * Get the temporary node Drive string
   * @return
   */
  public String getSelectedNodeDrive() {
    return this.selectedNodeDrive;
  }

	/**
	 * The listener interface for receiving selectTabAction events.
	 * The class that is interested in processing a selectTabAction
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addSelectTabActionListener</code> method. When
	 * the selectTabAction event occurs, that object's appropriate
	 * method is invoked.
	 */
	static public class SelectTabActionListener extends EventListener<UISCVPreferences> {

		/* (non-Javadoc)
 * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
 */
		public void execute(Event<UISCVPreferences> event) throws Exception {
			WebuiRequestContext context = event.getRequestContext();
			String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
			if (renderTab == null) {
				return;
			}
			event.getSource().setSelectedTab(renderTab);
			event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
		}
	}

  public static class AddPathActionListener extends EventListener<UISCVPreferences> {
    public void execute(Event<UISCVPreferences> event) throws Exception {
      UISCVPreferences uiSCVPref = event.getSource();
      UIContentSelectorOne contentSelector = uiSCVPref.createUIComponent(UIContentSelectorOne.class,
                                                                         null,
                                                                         null);
      Node node = Utils.getViewableNodeByComposer(uiSCVPref.getSelectedNodeRepository(),
                                                  uiSCVPref.getSelectedNodeWorkspace(),
                                                  uiSCVPref.getSelectedNodeUUID());
      contentSelector.init(uiSCVPref.getSelectedNodeDrive(), fixPath(node == null ? ""
                                                                                    : node.getPath(),
                                                                                    uiSCVPref));
      contentSelector.getChild(UIContentBrowsePanelOne.class)
      .setSourceComponent(uiSCVPref,
                          new String[] { UISCVPreferences.CONTENT_PATH_INPUT });
      Utils.createPopupWindow(uiSCVPref,
                              contentSelector,
                              UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW,
                              800);
      uiSCVPref.setContentSelectorID(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
    }

    private String fixPath(String path, UISCVPreferences uiScvPref) throws Exception {
      if (path == null || path.length() == 0 ||
          uiScvPref.getSelectedNodeDrive() == null || uiScvPref.getSelectedNodeDrive().length() == 0 ||
          uiScvPref.getSelectedNodeRepository() == null || uiScvPref.getSelectedNodeRepository().length() == 0)
        return "";
      ManageDriveService managerDriveService = uiScvPref.getApplicationComponent(ManageDriveService.class);
      DriveData driveData = managerDriveService.getDriveByName(uiScvPref.getSelectedNodeDrive());
      if (!path.startsWith(driveData.getHomePath()))
        return "";
      if ("/".equals(driveData.getHomePath()))
        return path;
      return path.substring(driveData.getHomePath().length());
    }
  }

  /**
   *
   * @return A string point to the node from preferences
   */

  protected String getNodeNameByPreferences(){
    String repository = WCMCoreUtils.getRepository().getConfiguration().getName();
    String workspace = portletPreferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
    String nodeIdentifier = portletPreferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null);
    String nodeDrive = portletPreferences.getValue(UISingleContentViewerPortlet.DRIVE, null);
    try {
      Node savedNode = Utils.getRealNode(repository, workspace, nodeIdentifier, false);
      if (savedNode==null) return null;
      this.setSelectedNodeInfo(savedNode.getUUID(), repository, workspace, nodeDrive);
      this.setSelectedNodePath(savedNode.getPath());
      return getTitle(savedNode);
    }catch (RepositoryException re) {
      return null;
    }
  }
  /**
   * Gets the title.
   *
   * @param node the node
   *
   * @return the title
   *
   * @throws Exception the exception
   */
  private String getTitle(Node node) throws RepositoryException {
    String title = null;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getValue().getString();
    }
    if (node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        } catch (Exception e) {
          title = null;
        }
      }
    }
    if (title==null) title = node.getName();

    return ContentReader.getUnescapeIllegalJcrContent(title);
  }
  public void setContentSelectorID(String id) {
    this.contentSelectorID = id;
  }
  public String getContetSelectorID() {
    return this.contentSelectorID;
  }

  public boolean isContextualEnable() {
    return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE,
        "false"));
  }
  public void doSelect(String selectField, Object value) throws Exception {
    String strRepository, strWorkspace, strDrive, strIdentifier, strNodeUUID;
    int driveIndex;
    String strPath = (String) value;

    if (CONTENT_PATH_INPUT.equals(selectField) ) {
      String[] splits = strPath.split(":");
      driveIndex = (splits.length == 4) ? 1 : 0;
      strRepository = splits[driveIndex];
      strWorkspace = splits[driveIndex + 1];
      strIdentifier = splits[driveIndex + 2];
      strDrive=  (driveIndex == 1) ? splits[0] : "";
      strIdentifier = Text.escapeIllegalJcrChars(strIdentifier);
      Node selectedNode = Utils.getRealNode(strRepository, strWorkspace, strIdentifier, false);
      if (selectedNode==null) return;
      strNodeUUID = selectedNode.getUUID();
      this.setSelectedNodeInfo(strNodeUUID, strRepository, strWorkspace, strDrive);
      this.setSelectedNodePath(selectedNode.getPath());
      getUIStringInput(selectField).setValue(getTitle(selectedNode));
    }else if (PRINT_VIEW_PAGE_INPUT.equals(selectField)) {
      getUIStringInput(selectField).setValue(strPath);
    }
    Utils.closePopupWindow(this, contentSelectorID);
  }


  /**
   * The listener interface for receiving selectTargetPageAction events.
   * The class that is interested in processing a selectTargetPageAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectTargetPageActionListener</code> method. When
   * the selectTargetPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SelectTargetPageActionListener extends EventListener<UISCVPreferences> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UISCVPreferences> event) throws Exception {
      UISCVPreferences uiscv = event.getSource();
      UIPageSelector pageSelector = uiscv.createUIComponent(UIPageSelector.class, null, null);
      pageSelector.setSourceComponent(uiscv, new String[] {PRINT_VIEW_PAGE_INPUT});
      Utils.createPopupWindow(uiscv, pageSelector, PRINT_PAGE_SELECTOR_POPUP, 800);
      uiscv.setContentSelectorID(PRINT_PAGE_SELECTOR_POPUP);
    }
  }

  public void setInternalPreferencesMode(boolean isInternal) {
    this.isInternal = isInternal;
  }
  public boolean getInternalPreferencesMode() {
    return this.isInternal;
  }
  boolean isInternal = false;
}
