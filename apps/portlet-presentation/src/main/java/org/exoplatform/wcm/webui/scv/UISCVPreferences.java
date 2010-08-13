/***************************************************************************
 * Copyright 2001-2010 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.wcm.webui.scv;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.selector.content.one.UIContentBrowsePanelOne;
import org.exoplatform.wcm.webui.selector.content.one.UIContentSelectorOne;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.validator.MandatoryValidator;

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
      @EventConfig(listeners = UISCVPreferences.SelectFolderPathActionListener.class),
      @EventConfig(listeners = UISCVPreferences.CancelActionListener.class)
    }
)
public class UISCVPreferences extends UIForm implements UISelectable{

  /** The Constant ITEM_PATH_FORM_INPUT_SET. */
  public final static String ITEM_PATH_FORM_INPUT_SET     = "UISCVConfigItemPathFormInputSet";

  public static final String CONTENT_PATH_INPUT           = "UISCVContentPathConfigurationInputBox";

  public static final String SHOW_TITLE_CHECK_BOX         = "UISCVShowTitleConfigurationCheckBox";

  public static final String SHOW_DATE_CHECK_BOX          = "UISCVShowDateConfigurationCheckBox";

  public static final String SHOW_OPION_BAR_CHECK_BOX     = "UISCVShowOptionBarConfigurationCheckBox";

  public static final String CONTEXTUAL_SELECT_RADIO_BOX  = "UISCVContextualRadioBox";
  
  public static final String PARAMETER_INPUT_BOX          = "UISCVParameterInputBox";

  public static final String ENABLE_STRING                = "Enable".intern();
  public static final String DISABLE_STRING               = "Disable".intern();

  protected PortletPreferences portletPreferences;

  protected String contentSelectorID;
  protected String selectedNodeUUID =null;
  protected String selectedNodeReporitory =null;
  protected String selectedNodeWorkspace =null;

  private UIFormStringInput             txtContentPath;
  private UIFormCheckBoxInput<Boolean>  chkShowTitle;
  private UIFormCheckBoxInput<Boolean>  chkShowDate;
  private UIFormCheckBoxInput<Boolean>  chkShowOptionBar;
  private UIFormRadioBoxInput           contextOptionsRadioInputBox; 
  
  public UISCVPreferences() throws Exception{    
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
    txtContentPath.addValidator(MandatoryValidator.class);
    txtContentPath.setEditable(false);
    txtContentPath.addValidator(MandatoryValidator.class);

    UIFormInputSetWithAction itemPathInputSet = new UIFormInputSetWithAction(ITEM_PATH_FORM_INPUT_SET);
    itemPathInputSet.setActionInfo(CONTENT_PATH_INPUT, new String[] { "SelectFolderPath" }) ;
    itemPathInputSet.addUIFormInput(txtContentPath);

    /** Option Show Title/Show Date/Show OptionBar **/
    boolean blnShowTitle = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_TITLE, null));
    chkShowTitle = new UIFormCheckBoxInput<Boolean>(SHOW_TITLE_CHECK_BOX, SHOW_TITLE_CHECK_BOX, null);
    chkShowTitle.setChecked(blnShowTitle);    

    boolean blnShowDate = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_DATE, null));
    chkShowDate = new UIFormCheckBoxInput<Boolean>(SHOW_DATE_CHECK_BOX, SHOW_DATE_CHECK_BOX, null);
    chkShowDate.setChecked(blnShowDate);

    boolean blnShowOptionBar = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR, null));
    chkShowOptionBar = new UIFormCheckBoxInput<Boolean>(SHOW_OPION_BAR_CHECK_BOX, SHOW_OPION_BAR_CHECK_BOX, null);
    chkShowOptionBar.setChecked(blnShowOptionBar);


    /** CONTEXTUAL MODE */
    boolean isShowContextOption = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, "false"));
    List<SelectItemOption<String>> contextOptions = new ArrayList<SelectItemOption<String>>();
    contextOptions.add(new SelectItemOption<String>(ENABLE_STRING, ENABLE_STRING));
    contextOptions.add(new SelectItemOption<String>(DISABLE_STRING, DISABLE_STRING));
    contextOptionsRadioInputBox = new UIFormRadioBoxInput(CONTEXTUAL_SELECT_RADIO_BOX, CONTEXTUAL_SELECT_RADIO_BOX, contextOptions);
    contextOptionsRadioInputBox.setValue(isShowContextOption?ENABLE_STRING:DISABLE_STRING);

    String strParameterName = portletPreferences.getValue(UISingleContentViewerPortlet.PARAMETER, null);
    UIFormStringInput txtParameterName = new UIFormStringInput(PARAMETER_INPUT_BOX, strParameterName);    
    
    addChild(itemPathInputSet);
    addChild(chkShowTitle);
    addChild(chkShowDate);
    addChild(chkShowOptionBar);
    addChild(contextOptionsRadioInputBox);
    addChild(txtParameterName);

  }
  public void getPreferences() {
    String strNodeName = getNodeNameByPreferences();
    txtContentPath.setLabel(strNodeName);
    boolean blnShowTitle = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_TITLE, null));
    chkShowTitle.setChecked(blnShowTitle);
    boolean blnShowDate = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_DATE, null));
    chkShowDate.setChecked(blnShowDate);
    boolean blnShowOptionBar = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR, null));
    chkShowOptionBar.setChecked(blnShowOptionBar);
    boolean isShowContextOption = Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, "false"));
    contextOptionsRadioInputBox.setValue(isShowContextOption?ENABLE_STRING:DISABLE_STRING);
  }
  /**
   * ActionListener: save preferences action
   * @author exo.VinhNT
   *
   */
  public static class SaveActionListener extends EventListener<UISCVPreferences> {   
    public void execute(Event<UISCVPreferences> event) throws Exception {
      UISCVPreferences uiSCVPref = event.getSource();
      PortletPreferences portletPreferences = ((PortletRequestContext) event.getRequestContext()).getRequest().getPreferences();
      String strShowTitle = uiSCVPref.getUIFormCheckBoxInput(SHOW_TITLE_CHECK_BOX).isChecked() ? "true" : "false";
      String strShowDate = uiSCVPref.getUIFormCheckBoxInput(SHOW_DATE_CHECK_BOX).isChecked() ? "true" : "false";
      String strShowOptionBar = uiSCVPref.getUIFormCheckBoxInput(SHOW_OPION_BAR_CHECK_BOX).isChecked() ? "true" : "false";      

      String strIsContextEnable = ((UIFormRadioBoxInput) uiSCVPref.getChildById(CONTEXTUAL_SELECT_RADIO_BOX)).getValue();
      strIsContextEnable = strIsContextEnable.equals(ENABLE_STRING) ? "true":"false";
      String strParameterName = uiSCVPref.getUIStringInput(PARAMETER_INPUT_BOX).getValue();
      
      portletPreferences.setValue(UISingleContentViewerPortlet.REPOSITORY, uiSCVPref.getSelectedNodeRepository());    
      portletPreferences.setValue(UISingleContentViewerPortlet.WORKSPACE, uiSCVPref.getSelectedNodeWorkspace());
      portletPreferences.setValue(UISingleContentViewerPortlet.IDENTIFIER, uiSCVPref.getSelectedNodeUUID()) ;

      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_TITLE, strShowTitle);
      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_DATE, strShowDate);
      portletPreferences.setValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR, strShowOptionBar);
      portletPreferences.setValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, strIsContextEnable);
      portletPreferences.setValue(UISingleContentViewerPortlet.PARAMETER, strParameterName);
      portletPreferences.store();
      if (uiSCVPref.getInternalPreferencesMode()) {
        if (!Utils.isPortalEditMode()) {
          uiSCVPref.getAncestorOfType(UISingleContentViewerPortlet.class).changeToViewMode();
          uiSCVPref.getPreferences();
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
  public void setSelectedNodeInfo(String nodeUUID, String nodeRepo, String nodeWS) {
    this.selectedNodeUUID = nodeUUID;
    this.selectedNodeReporitory = nodeRepo;
    this.selectedNodeWorkspace = nodeWS;    
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

  public static class SelectFolderPathActionListener extends EventListener<UISCVPreferences> {  
    public void execute(Event<UISCVPreferences> event) throws Exception {
      UISCVPreferences uiSCVPref = event.getSource();
      UIContentSelectorOne contentSelector = uiSCVPref.createUIComponent(UIContentSelectorOne.class, null, null);
      contentSelector.init();
      contentSelector.getChild(UIContentBrowsePanelOne.class).setSourceComponent(uiSCVPref, new String[] { UISCVPreferences.CONTENT_PATH_INPUT });
      Utils.createPopupWindow(uiSCVPref, contentSelector, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW, 800);
      uiSCVPref.setContentSelectorID(UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }

  /**
   * 
   * @return A string point to the node from preferences
   */

  protected String getNodeNameByPreferences(){
    String repository = portletPreferences.getValue(UISingleContentViewerPortlet.REPOSITORY, null);    
    String workspace = portletPreferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
    String nodeIdentifier = portletPreferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null) ;
    try {
      Node savedNode = Utils.getRealNode(repository, workspace, nodeIdentifier, false);
      if (savedNode==null) return null;
      this.setSelectedNodeInfo(savedNode.getUUID(), repository, workspace);
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

    return title;
  }
  public void setContentSelectorID(String id) {
    this.contentSelectorID = id;
  }
  public String getContetSelectorID() {
    return this.contentSelectorID;
  }
  public boolean isContextualEnable() {    
    return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, "false"));
  }
  public void doSelect(String selectField, Object value) throws Exception {
    String strRepository, strWorkspace, strIdentifier, strNodeUUID;
    int repoIndex, wsIndex;
    String strPath = (String) value;

    if (CONTENT_PATH_INPUT.equals(selectField) ) {
      repoIndex = strPath.indexOf(':');
      wsIndex = strPath.lastIndexOf(':');
      strRepository = strPath.substring(0, repoIndex);
      strWorkspace = strPath.substring(repoIndex+1, wsIndex);
      strIdentifier = strPath.substring(wsIndex +1);
      Node selectedNode = Utils.getRealNode(strRepository, strWorkspace, strIdentifier, false);
      if (selectedNode==null) return;
      strNodeUUID = selectedNode.getUUID();
      this.setSelectedNodeInfo(strNodeUUID, strRepository, strWorkspace);
      getUIStringInput(selectField).setValue(getTitle(selectedNode));
    }
    Utils.closePopupWindow(this, contentSelectorID);
  }
  
  public void setInternalPreferencesMode(boolean isInternal) {
    this.isInternal = isInternal;
    if (isInternal) getPreferences();
  }
  public boolean getInternalPreferencesMode() {
    return this.isInternal;
  }
  boolean isInternal = false;
}