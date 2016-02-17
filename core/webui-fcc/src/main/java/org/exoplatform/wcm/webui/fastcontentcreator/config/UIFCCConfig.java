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
package org.exoplatform.wcm.webui.fastcontentcreator.config;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.container.UIFormFieldSet;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCUtils;
import org.exoplatform.wcm.webui.fastcontentcreator.config.action.UIFCCActionList;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.ext.UIFormInputSetWithAction;
import org.exoplatform.webui.form.input.UICheckBoxInput;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.portlet.PortletPreferences;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/FastContentCreatorPortlet/UIFormWithFieldSet.gtmpl",
    events = {
      @EventConfig(listeners = UIFCCConfig.SaveActionListener.class),
      @EventConfig(listeners = UIFCCConfig.SelectPathActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIFCCConfig.ChangeWorkspaceActionListener.class, phase=Phase.DECODE)
    }
)
public class UIFCCConfig extends UIFormTabPane implements UISelectable {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UIFCCConfig.class.getName());
  
  /** Basic Mode */
  private static final String BASIC_MODE = "basic";

  /** The saved location node. */
  private NodeLocation savedLocationNode;

  /**
   * Instantiates a new uIFCC config.
   *
   * @throws Exception the exception
   */
  public UIFCCConfig() throws Exception {
    super("UIFCCConfig");
    PortletPreferences portletPreferences = UIFCCUtils.getPortletPreferences();
    String preferenceMode = portletPreferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
    String preferenceWorkspace = portletPreferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "");
    String preferencePath = portletPreferences.getValue(UIFCCConstant.PREFERENCE_PATH, "");

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;

    UIFormInputSetWithAction saveLocationField = new UIFormInputSetWithAction(UIFCCConstant.SAVE_LOCATION_FIELD);

    if (!BASIC_MODE.equals(preferenceMode)) {
      UIFormSelectBox workspaceSelectBox = new UIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX,
                                                               UIFCCConstant.WORKSPACE_FORM_SELECTBOX,
                                                               options);
      workspaceSelectBox.setOnChange("ChangeWorkspace") ;
      saveLocationField.addChild(workspaceSelectBox) ;
    }
    UIFormInputSetWithAction folderSelectorInput = new UIFormInputSetWithAction(UIFCCConstant.LOCATION_FORM_INPUT_ACTION);
    folderSelectorInput.addUIFormInput(new UIFormStringInput(UIFCCConstant.LOCATION_FORM_STRING_INPUT,
                                                             UIFCCConstant.LOCATION_FORM_STRING_INPUT,
                                                             null).setReadOnly(true));
    folderSelectorInput.setActionInfo(UIFCCConstant.LOCATION_FORM_STRING_INPUT, new String[] {"SelectPath"}) ;
    saveLocationField.addUIFormInput((UIFormInputSet)folderSelectorInput);

    addChild(saveLocationField);
    setSelectedTab(UIFCCConstant.SAVE_LOCATION_FIELD);

    UIFormInputSetWithAction templateField = new UIFormInputSetWithAction(UIFCCConstant.TEMPLATE_FIELD);
    templateField.addChild(new UIFormSelectBox(UIFCCConstant.TEMPLATE_FORM_SELECTBOX,
                                               UIFCCConstant.TEMPLATE_FORM_SELECTBOX,
                                               options));
    templateField.addChild(new UIFormStringInput(UIFCCConstant.SAVE_FORM_STRING_INPUT,
                                                 UIFCCConstant.SAVE_FORM_STRING_INPUT,
                                                 null));
    templateField.addChild(new UIFormTextAreaInput(UIFCCConstant.MESSAGE_FORM_TEXTAREA_INPUT,
                                                   UIFCCConstant.MESSAGE_FORM_TEXTAREA_INPUT,
                                                   null));
    templateField.addChild(new UICheckBoxInput(UIFCCConstant.REDIRECT_FORM_CHECKBOX_INPUT,
                                                            UIFCCConstant.REDIRECT_FORM_CHECKBOX_INPUT,
                                                            false));
    templateField.addChild(new UIFormStringInput(UIFCCConstant.REDIRECT_PATH_FORM_STRING_INPUT,
                                                 UIFCCConstant.REDIRECT_PATH_FORM_STRING_INPUT,
                                                 null));

    addChild(templateField);
    if (!BASIC_MODE.equals(preferenceMode)) {
      UIFormInputSetWithAction actionField = new UIFormInputSetWithAction(UIFCCConstant.ACTION_FIELD);
      UIFCCActionList fastContentCreatorActionList = actionField.addChild(UIFCCActionList.class, null, "UIFCCActionList");
      fastContentCreatorActionList.init(preferenceMode);
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(preferenceWorkspace, WCMCoreUtils.getRepository());
        fastContentCreatorActionList.updateGrid((Node) session.getItem(preferencePath),
                                                fastContentCreatorActionList.getChild(UIGrid.class)
                                                                            .getUIPageIterator()
                                                                            .getCurrentPage());
  
        addChild(actionField);
    }

    setActions(new String[] {"Save"}) ;
  }

  /**
   * Inits the edit mode.
   *
   * @throws Exception the exception
   */
  public void initEditMode() throws Exception {
    PortletPreferences preferences = UIFCCUtils.getPortletPreferences();
    String preferenceMode = preferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
    String preferenceWorkspace = preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "") ;
    String preferencePath = preferences.getValue(UIFCCConstant.PREFERENCE_PATH, "") ;

    boolean isDefaultWorkspace = false ;
    if (!BASIC_MODE.equals(preferenceMode)) {
      ManageableRepository repository = WCMCoreUtils.getRepository();
      String[] workspaceNames = repository.getWorkspaceNames();
      String systemWsName = repository.getConfiguration().getSystemWorkspaceName();
      List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>();
      for (String workspaceName : workspaceNames) {
        if (!workspaceName.equals(systemWsName)) {
          if (workspaceName.equals(preferenceWorkspace))
            isDefaultWorkspace = true;
          workspace.add(new SelectItemOption<String>(workspaceName));
        }
      }
      UIFormSelectBox uiWorkspaceList = getUIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX);
      uiWorkspaceList.setOptions(workspace);
      if (isDefaultWorkspace) {
        uiWorkspaceList.setValue(preferenceWorkspace);
      } else if (workspace.size() > 0) {
        uiWorkspaceList.setValue(workspace.get(0).getValue());
      }
    }
    getUIStringInput(UIFCCConstant.LOCATION_FORM_STRING_INPUT).setValue(preferencePath) ;

    setTemplateOptions(preferencePath, preferenceWorkspace) ;

    getUIStringInput(UIFCCConstant.SAVE_FORM_STRING_INPUT).setValue(preferences.
                                                                    getValue(UIFCCConstant.PREFERENCE_SAVE_BUTTON,
                                                                                         ""));
    getUIFormTextAreaInput(UIFCCConstant.MESSAGE_FORM_TEXTAREA_INPUT).setValue(preferences.
                                                                                 getValue(UIFCCConstant.PREFERENCE_SAVE_MESSAGE,
                                                                               ""));
    getUICheckBoxInput(UIFCCConstant.REDIRECT_FORM_CHECKBOX_INPUT).
        setChecked(Boolean.parseBoolean(preferences.getValue(UIFCCConstant.PREFERENCE_IS_REDIRECT, "")));
    getUIStringInput(UIFCCConstant.REDIRECT_PATH_FORM_STRING_INPUT).setValue(preferences.
                                                                               getValue(UIFCCConstant.PREFERENCE_REDIRECT_PATH,
                                                                             ""));
  }

  /**
   * Sets the template options.
   *
   * @param nodePath the node path
   * @param workspaceName the workspace name
   *
   * @throws Exception the exception
   */
  private void setTemplateOptions(String nodePath, String workspaceName) throws Exception {
    try {
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, WCMCoreUtils.getRepository());
      Node currentNode = null ;
      UIFormSelectBox uiSelectTemplate = getUIFormSelectBox(UIFCCConstant.TEMPLATE_FORM_SELECTBOX);
      List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
      boolean hasDefaultDoc = false ;
      String defaultValue = UIFCCUtils.getPreferenceType();
      try {
        currentNode = (Node)session.getItem(nodePath) ;
        setSavedLocationNode(currentNode);
      } catch(PathNotFoundException ex) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIFCCConfig.msg.item-not-found",
                                                null,
                                                ApplicationMessage.WARNING));
        return ;
      }
      NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager() ;
      NodeType currentNodeType = currentNode.getPrimaryNodeType() ;
      NodeDefinition[] childDefs = currentNodeType.getChildNodeDefinitions() ;
      TemplateService templateService = getApplicationComponent(TemplateService.class) ;
      List<String> templates = templateService.getDocumentTemplates() ;
      List<String> labels = new ArrayList<String>() ;
      try {
        for(int i = 0; i < templates.size(); i ++){
          String nodeTypeName = templates.get(i).toString() ;
          NodeType nodeType = ntManager.getNodeType(nodeTypeName) ;
          NodeType[] superTypes = nodeType.getSupertypes() ;
          boolean isCanCreateDocument = false ;
          for(NodeDefinition childDef : childDefs){
            NodeType[] requiredChilds = childDef.getRequiredPrimaryTypes() ;
            for(NodeType requiredChild : requiredChilds) {
              if(nodeTypeName.equals(requiredChild.getName())){
                isCanCreateDocument = true ;
                break ;
              }
            }
            if(nodeTypeName.equals(childDef.getName()) || isCanCreateDocument) {
              if(!hasDefaultDoc && nodeTypeName.equals(defaultValue)) hasDefaultDoc = true ;
              String label = templateService.getTemplateLabel(nodeTypeName) ;
              if(!labels.contains(label)) {
                options.add(new SelectItemOption<String>(label, nodeTypeName));
              }
              labels.add(label) ;
              isCanCreateDocument = true ;
            }
          }
          if(!isCanCreateDocument){
            for(NodeType superType:superTypes) {
              for(NodeDefinition childDef : childDefs){
                for(NodeType requiredType : childDef.getRequiredPrimaryTypes()) {
                  if (superType.getName().equals(requiredType.getName())) {
                    if(!hasDefaultDoc && nodeTypeName.equals(defaultValue)) {
                      hasDefaultDoc = true ;
                    }
                    String label = templateService.getTemplateLabel(nodeTypeName) ;
                    if(!labels.contains(label)) {
                      options.add(new SelectItemOption<String>(label, nodeTypeName));
                    }
                    labels.add(label) ;
                    isCanCreateDocument = true ;
                    break;
                  }
                }
                if(isCanCreateDocument) break ;
              }
              if(isCanCreateDocument) break ;
            }
          }
        }
        uiSelectTemplate.setOptions(options) ;
        if(hasDefaultDoc) {
          uiSelectTemplate.setValue(defaultValue);
        } else if(options.size() > 0) {
          defaultValue = options.get(0).getValue() ;
          uiSelectTemplate.setValue(defaultValue);
        }
      } catch(Exception e) {
        Utils.createPopupMessage(this, "UIFCCConfig.msg.get-template", null, ApplicationMessage.ERROR);
      }
    } catch(Exception ex) {
      Utils.createPopupMessage(this, "UIFCCConfig.msg.set-template-option", null, ApplicationMessage.ERROR);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) {
    getUIStringInput(selectField).setValue(value.toString()) ;
    PortletPreferences preferences = UIFCCUtils.getPortletPreferences();
    String preferenceMode = preferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
    String preferenceWorkspace = preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "") ;
    if (!BASIC_MODE.equals(preferenceMode)) {
      preferenceWorkspace = getUIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX).getValue() ;
    }
    String savedLocationPath = value.toString();
    try {
      setTemplateOptions(savedLocationPath, preferenceWorkspace) ;
    } catch(Exception ex) {
      Utils.createPopupMessage(this, "UIFCCConfig.msg.do-select", null, ApplicationMessage.ERROR);
    }

    try {
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(preferenceWorkspace, WCMCoreUtils.getRepository());
      UIFCCActionList uiFCCActionList = ((UIFormFieldSet) getChildById("UIFCCActionField")).getChild(UIFCCActionList.class);
      uiFCCActionList.updateGrid((Node) session.getItem(savedLocationPath),
                                 uiFCCActionList.getChild(UIGrid.class)
                                                .getUIPageIterator()
                                                .getCurrentPage());
    } catch (RepositoryConfigurationException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }

    Utils.closePopupWindow(this, UIFCCConstant.SELECTOR_POPUP_WINDOW);
  }

  /**
   * Gets the saved location node.
   *
   * @return the saved location node
   */
  public Node getSavedLocationNode() {
    return NodeLocation.getNodeByLocation(savedLocationNode);
  }

  /**
   * Sets the saved location node.
   *
   * @param savedLocationNode the new saved location node
   */
  public void setSavedLocationNode(Node savedLocationNode) {
    this.savedLocationNode = NodeLocation.getNodeLocationByNode(savedLocationNode);
  }

  /**
   * Get PREFERENCE_MODE.
   * 
   * @return
   */
  public String getPreferenceMode() {
    PortletPreferences portletPreferences = UIFCCUtils.getPortletPreferences();
    return portletPreferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
  }

  /**
   * The listener interface for receiving selectPathAction events.
   * The class that is interested in processing a selectPathAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectPathActionListener</code> method. When
   * the selectPathAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class SelectPathActionListener extends EventListener<UIFCCConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCConfig> event) throws Exception {
      UIFCCConfig fastContentCreatorConfig = event.getSource() ;
      PortletPreferences preferences = UIFCCUtils.getPortletPreferences();
      String preferenceMode = preferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
      String preferenceRepository = WCMCoreUtils.getRepository().getConfiguration().getName();
      String preferenceWorkspace = preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "") ;
      if (!BASIC_MODE.equals(preferenceMode)) {
        preferenceWorkspace = fastContentCreatorConfig.getUIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX)
                                                      .getValue();
      }
      UIOneNodePathSelector uiOneNodePathSelector = fastContentCreatorConfig.createUIComponent(UIOneNodePathSelector.class,
                                                                                               null,
                                                                                               null);
      uiOneNodePathSelector.setIsDisable(preferenceWorkspace, true) ;
      uiOneNodePathSelector.setShowRootPathSelect(true) ;
      uiOneNodePathSelector.setRootNodeLocation(preferenceRepository, preferenceWorkspace, "/");
      uiOneNodePathSelector.init(WCMCoreUtils.getUserSessionProvider()) ;
      uiOneNodePathSelector.setSourceComponent(fastContentCreatorConfig,
                                               new String[] { UIFCCConstant.LOCATION_FORM_STRING_INPUT });
      Utils.createPopupWindow(fastContentCreatorConfig, uiOneNodePathSelector, UIFCCConstant.SELECTOR_POPUP_WINDOW, 610);
      fastContentCreatorConfig.setSelectedTab(UIFCCConstant.SAVE_LOCATION_FIELD);
    }
  }

  /**
   * The listener interface for receiving changeWorkspaceAction events.
   * The class that is interested in processing a changeWorkspaceAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeWorkspaceActionListener</code> method. When
   * the changeWorkspaceAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class ChangeWorkspaceActionListener extends EventListener<UIFCCConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCConfig> event) throws Exception {
      UIFCCConfig uiFCCConfig = event.getSource();
      uiFCCConfig.getUIStringInput(UIFCCConstant.LOCATION_FORM_STRING_INPUT).setValue("/");
      String wsName = uiFCCConfig.getUIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX).getValue();
      uiFCCConfig.setTemplateOptions(uiFCCConfig.getUIStringInput(UIFCCConstant.LOCATION_FORM_STRING_INPUT)
                                                .getValue(),
                                     wsName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFCCConfig);
    }
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
  static public class SaveActionListener extends EventListener<UIFCCConfig> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCConfig> event) throws Exception {
      UIFCCConfig fastContentCreatorConfig = event.getSource() ;
      UIApplication uiApp = fastContentCreatorConfig.getAncestorOfType(UIApplication.class) ;
      PortletPreferences portletPreferences = UIFCCUtils.getPortletPreferences();
      String preferenceMode = portletPreferences.getValue(UIFCCConstant.PREFERENCE_MODE, "");
      boolean preferenceIsActionNeeded = Boolean.parseBoolean(portletPreferences.
                                                              getValue(UIFCCConstant.PREFERENCE_IS_ACTION_NEEDED,
                                                                                          "false"));
      String type = fastContentCreatorConfig.getUIFormSelectBox(UIFCCConstant.TEMPLATE_FORM_SELECTBOX)
                                            .getValue();
      String path = fastContentCreatorConfig.getUIStringInput(UIFCCConstant.LOCATION_FORM_STRING_INPUT)
                                            .getValue();
      String saveButton = fastContentCreatorConfig.getUIStringInput(UIFCCConstant.SAVE_FORM_STRING_INPUT)
                                                  .getValue();
      String saveMessage = fastContentCreatorConfig.getUIFormTextAreaInput(UIFCCConstant.MESSAGE_FORM_TEXTAREA_INPUT)
                                                   .getValue();
      String isRedirect = String.valueOf(fastContentCreatorConfig.
                                         getUICheckBoxInput(UIFCCConstant.REDIRECT_FORM_CHECKBOX_INPUT)
                                                                 .isChecked());
      String redirectPath = fastContentCreatorConfig.getUIStringInput(UIFCCConstant.REDIRECT_PATH_FORM_STRING_INPUT)
                                                    .getValue();
      if (("false".equals(isRedirect) || redirectPath == null) && saveMessage == null) {
        Utils.createPopupMessage(fastContentCreatorConfig,
                                 "UIFCCConfig.msg.message-empty",
                                 null,
                                 ApplicationMessage.WARNING);
      }

      String workspaceName = null;
      if (BASIC_MODE.equals(preferenceMode) && preferenceIsActionNeeded)
        workspaceName = portletPreferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "");
      else
        workspaceName = fastContentCreatorConfig.getUIFormSelectBox(UIFCCConstant.WORKSPACE_FORM_SELECTBOX)
                                                .getValue();
      if (workspaceName == null || workspaceName.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIFCCConfig.msg.ws-empty",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
      if (type == null || type.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIFCCConfig.msg.fileType-empty",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_WORKSPACE, workspaceName);

      portletPreferences.setValue(UIFCCConstant.PREFERENCE_PATH, path);
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_TYPE, type);
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_SAVE_BUTTON, saveButton);
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_SAVE_MESSAGE, saveMessage);
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_IS_REDIRECT, isRedirect);
      portletPreferences.setValue(UIFCCConstant.PREFERENCE_REDIRECT_PATH, redirectPath);
      portletPreferences.store();
      uiApp.addMessage(new ApplicationMessage("UIFCCConfig.msg.save-successfully", null));

    }
  }

}
