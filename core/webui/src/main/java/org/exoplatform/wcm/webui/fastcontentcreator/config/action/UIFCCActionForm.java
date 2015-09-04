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
package org.exoplatform.wcm.webui.fastcontentcreator.config.action;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.nodetype.selector.UINodeTypeSelector;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCConstant;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCPortlet;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCUtils;
import org.exoplatform.wcm.webui.fastcontentcreator.config.UIFCCConfig;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormStringInput;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, events = {
    @EventConfig(listeners = UIFCCActionForm.SaveActionListener.class),
    @EventConfig(listeners = UIDialogForm.OnchangeActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFCCActionForm.CloseActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFCCActionForm.ShowComponentActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFCCActionForm.RemoveReferenceActionListener.class,
                 confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE) })
public class UIFCCActionForm extends UIDialogForm implements UISelectable {

  /** The parent path_. */
  private String parentPath_ ;

  /** The node type name_. */
  private String nodeTypeName_ = null ;

  /** The script path_. */
  private String scriptPath_ = null ;

  /** The root path_. */
  private String rootPath_ = null;

  /** The is add new. */
  private boolean isAddNew = false;

  /** The Constant EXO_ACTIONS. */
  private static final String EXO_ACTIONS = "exo:actions";

  /**
   * Instantiates a new uIFCC action form.
   *
   * @throws Exception the exception
   */
  public UIFCCActionForm() throws Exception {setActions(new String[]{"Save","Close"}) ;}

  /**
   * Creates the new action.
   *
   * @param parentNode the parent node
   * @param actionType the action type
   * @param isAddNew the is add new
   *
   * @throws Exception the exception
   */
  public void createNewAction(Node parentNode, String actionType, boolean isAddNew) throws Exception {
    reset() ;
    parentPath_ = parentNode.getPath() ;
    nodeTypeName_ = actionType;
    componentSelectors.clear() ;
    properties.clear() ;
    this.isAddNew = isAddNew;
    getChildren().clear() ;
  }

  /**
   * Gets the parent node.
   *
   * @return the parent node
   *
   * @throws Exception the exception
   */
  private Node getParentNode(Node node) throws Exception{
    return (Node) node.getSession().getItem(parentPath_) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#renderField(java.lang.String)
   */
  public void renderField(String name) throws Exception {
    UIComponent uiInput = findComponentById(name);
    if ("homePath".equals(name)) {
      String homPath = UIFCCUtils.getPreferenceWorkspace() + ":" + parentPath_;
      if (homPath.endsWith("/"))
        homPath = homPath.substring(0, homPath.length() - 1);
      ((UIFormStringInput) uiInput).setValue(homPath);
    }
    if ("targetPath".equals(name) && (isOnchange()) && !isUpdateSelect) {
      ((UIFormStringInput) uiInput).reset();
    }
    super.renderField(name);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    isUpdateSelect = true ;
    //getUIStringInput(selectField).setValue(value.toString()) ;
    UIComponent uicomponent = getChildById(selectField);
    if (UIFormStringInput.class.isInstance(uicomponent))
      ((UIFormStringInput)uicomponent).setValue(value.toString());
    else if (UIFormMultiValueInputSet.class.isInstance(uicomponent)) {
      ((UIFormMultiValueInputSet)uicomponent).setValue((ArrayList<String>)value);
    }
  }

  /**
   * Gets the current path.
   *
   * @return the current path
   *
   * @throws Exception the exception
   */
  public String getCurrentPath() throws Exception {
    UIFCCPortlet fastContentCreatorPortlet = getAncestorOfType(UIFCCPortlet.class);
    UIFCCConfig fastContentCreatorConfig = fastContentCreatorPortlet.getChild(UIFCCConfig.class);
    return fastContentCreatorConfig.getSavedLocationNode().getPath();
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    DMSRepositoryConfiguration repositoryConfiguration = dmsConfiguration.getConfig();
    return new JCRResourceResolver(repositoryConfiguration.getSystemWorkspace());
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#getTemplate()
   */
  public String getTemplate() { return getDialogPath() ; }

  /**
   * Gets the dialog path.
   *
   * @return the dialog path
   */
  public String getDialogPath() {
    repositoryName = UIFCCUtils.getPreferenceRepository() ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    String dialogPath = null ;
    if (nodeTypeName_ != null) {
      try {
        dialogPath = templateService.getTemplatePathByUser(true, nodeTypeName_, userName);
      } catch (Exception e){
        Utils.createPopupMessage(this, "UIFCCForm.msg.get-dialog-path", null, ApplicationMessage.ERROR);
      }
    }
    return dialogPath ;
  }

  /**
   * Gets the repository name.
   *
   * @return the repository name
   */
  public String getRepositoryName() { return repositoryName; }

  /**
   * Gets the template node type.
   *
   * @return the template node type
   */
  public String getTemplateNodeType() { return nodeTypeName_ ; }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() { return scriptPath_ ; }

  /**
   * Sets the root path.
   *
   * @param rootPath the new root path
   */
  public void setRootPath(String rootPath){
   rootPath_ = rootPath;
  }

  /**
   * Gets the root path.
   *
   * @return the root path
   */
  public String getRootPath(){return rootPath_;}

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#onchange(org.exoplatform.webui.event.Event)
   */
  public void onchange(Event<?> event) throws Exception {
    if(!isAddNew){
      event.getRequestContext().addUIComponentToUpdateByAjax(getParent()) ;
      return;
    }
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
  static public class SaveActionListener extends EventListener<UIFCCActionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCActionForm> event) throws Exception {
      UIFCCActionForm fccActionForm = event.getSource();
      UIApplication uiApp = fccActionForm.getAncestorOfType(UIApplication.class) ;

      // Get current node
      UIFCCPortlet fastContentCreatorPortlet = fccActionForm.getAncestorOfType(UIFCCPortlet.class);
      UIFCCConfig fastContentCreatorConfig = fastContentCreatorPortlet.getChild(UIFCCConfig.class) ;
      Node currentNode = fastContentCreatorConfig.getSavedLocationNode();

      // Check permission for current node
      if (!PermissionUtil.canAddNode(currentNode) || !PermissionUtil.canSetProperty(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionForm.msg.no-permission-add",
                                                null));
        
        return;
      }
      UIFCCActionList fastContentCreatorActionList = null;
      Map<String, JcrInputProperty> sortedInputs = DialogFormUtil.prepareMap(fccActionForm.getChildren(),
                                                                             fccActionForm.getInputProperties(),
                                                                             fccActionForm.getInputOptions());
      // Update action node:
      if(!fccActionForm.isAddNew) {
        CmsService cmsService = fccActionForm.getApplicationComponent(CmsService.class) ;
        Node storedHomeNode = fccActionForm.getParentNode(currentNode).getNode("exo:actions");
        cmsService.storeNode(fccActionForm.nodeTypeName_, storedHomeNode, sortedInputs, false) ;
        storedHomeNode.getSession().save();
      } else {

        // Add lock token if node is locked
        if (currentNode.isLocked()) {
          String lockToken = LockUtil.getLockToken(currentNode);
          if(lockToken != null) {
            currentNode.getSession().addLockToken(lockToken);
          }
        }

        try{
          JcrInputProperty rootProp = sortedInputs.get("/node");
          if(rootProp == null) {
            rootProp = new JcrInputProperty();
            rootProp.setJcrPath("/node");
            rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue()) ;
            sortedInputs.put("/node", rootProp) ;
          } else {
            rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue());
          }
          String actionName = (String)(sortedInputs.get("/node/exo:name")).getValue() ;
          Node parentNode = fccActionForm.getParentNode(currentNode);

          // Check if action existed
          if (parentNode.hasNode(EXO_ACTIONS)) {
            if (parentNode.getNode(EXO_ACTIONS).hasNode(actionName)) {
              Object[] args = { actionName };
              uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionForm.msg.existed-action",
                                                      args,
                                                      ApplicationMessage.WARNING));
              
              return;
            }
          }

          // Check parent node
          if(parentNode.isNew()) {
            String[] args = {parentNode.getPath()} ;
            uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionForm.msg.unable-add-action",args)) ;
            
            return;
          }

          // Save to database
          ActionServiceContainer actionServiceContainer = fccActionForm.getApplicationComponent(ActionServiceContainer.class);
          actionServiceContainer.addAction(parentNode, fccActionForm.nodeTypeName_, sortedInputs);
          fccActionForm.setIsOnchange(false) ;
          parentNode.getSession().save() ;

          // Create action
          fccActionForm.createNewAction(fastContentCreatorConfig.getSavedLocationNode(),
                                        fccActionForm.nodeTypeName_,
                                        true);
          fastContentCreatorActionList = fastContentCreatorConfig.findFirstComponentOfType(UIFCCActionList.class);
          fastContentCreatorActionList.updateGrid(parentNode,
                                                  fastContentCreatorActionList.getChild(UIGrid.class)
                                                                              .getUIPageIterator()
                                                                              .getCurrentPage());
          fccActionForm.reset() ;
        } catch(RepositoryException repo) {
          String key = "UIFastContentCreatorActionForm.msg.repository-exception" ;
          uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING)) ;
          
          return;
        } catch(NumberFormatException nume) {
          String key = "UIFastContentCreatorActionForm.msg.numberformat-exception" ;
          uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING)) ;
          
          return;
        } catch (NullPointerException nullPointerException) {
          uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionForm.msg.unable-add",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        } catch (Exception e) {
          uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionForm.msg.unable-add",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      }
      Utils.closePopupWindow(fccActionForm, UIFCCConstant.ACTION_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  static public class CloseActionListener extends EventListener<UIFCCActionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCActionForm> event) throws Exception {
      UIFCCActionForm fastContentCreatorActionForm = event.getSource();
      Utils.closePopupWindow(fastContentCreatorActionForm, UIFCCConstant.ACTION_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving removeReferenceAction events.
   * The class that is interested in processing a removeReferenceAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveReferenceActionListener<code> method. When
   * the removeReferenceAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RemoveReferenceActionEvent
   */
  static public class RemoveReferenceActionListener extends EventListener<UIFCCActionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCActionForm> event) throws Exception {
      UIFCCActionForm fastContentCreatorActionForm = event.getSource();
      fastContentCreatorActionForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      fastContentCreatorActionForm.getUIStringInput(fieldName).setValue(null);
      event.getRequestContext()
           .addUIComponentToUpdateByAjax(fastContentCreatorActionForm.getParent());
    }
  }

  /**
   * The listener interface for receiving showComponentAction events.
   * The class that is interested in processing a showComponentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addShowComponentActionListener<code> method. When
   * the showComponentAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ShowComponentActionEvent
   */
  @SuppressWarnings("unchecked")
  static public class ShowComponentActionListener extends EventListener<UIFCCActionForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCActionForm> event) throws Exception {
      UIFCCActionForm fastContentCreatorActionForm = event.getSource() ;
      UIContainer uiContainer = fastContentCreatorActionForm.getParent() ;
      fastContentCreatorActionForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Map fieldPropertiesMap = fastContentCreatorActionForm.componentSelectors.get(fieldName) ;
      String classPath = (String)fieldPropertiesMap.get("selectorClass") ;
      String rootPath = (String)fieldPropertiesMap.get("rootPath") ;
      ClassLoader cl = Thread.currentThread().getContextClassLoader() ;
      Class clazz = Class.forName(classPath, true, cl) ;
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      String repositoryName = fastContentCreatorActionForm.getRepositoryName();
      String selectorParams = (String) fieldPropertiesMap.get("selectorParams");
      if (uiComp instanceof UIOneNodePathSelector) {
        String wsFieldName = (String) fieldPropertiesMap.get("workspaceField");
        String wsName = "";
        if (wsFieldName != null && wsFieldName.length() > 0) {
          wsName = (String) fastContentCreatorActionForm.<UIFormInputBase> getUIInput(wsFieldName)
                                                        .getValue();
          ((UIOneNodePathSelector) uiComp).setIsDisable(wsName, true);
        }
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",") ;
          if(arrParams.length == 4) {
            ((UIOneNodePathSelector)uiComp).setAcceptedNodeTypesInPathPanel(new String[] {"nt:file"}) ;
            wsName = arrParams[1];
            rootPath = arrParams[2];
            ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true) ;
            if(arrParams[3].indexOf(";") > -1) {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(arrParams[3].split(";")) ;
            } else {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(new String[] {arrParams[3]}) ;
            }
          }
        }
        if (rootPath == null)
          rootPath = "/";
        ((UIOneNodePathSelector) uiComp).setRootNodeLocation(UIFCCUtils.getPreferenceRepository(),
                                                             wsName,
                                                             rootPath);
        ((UIOneNodePathSelector) uiComp).setShowRootPathSelect(true);
        ((UIOneNodePathSelector) uiComp).init(WCMCoreUtils.getUserSessionProvider());
      } else if (uiComp instanceof UINodeTypeSelector) {
        ((UINodeTypeSelector)uiComp).setRepositoryName(repositoryName);
          UIFormMultiValueInputSet uiFormMultiValueInputSet = fastContentCreatorActionForm.getChildById(fieldName);
          List values = uiFormMultiValueInputSet.getValue();
          ((UINodeTypeSelector)uiComp).init(1, values);
        }

      Utils.createPopupWindow(fastContentCreatorActionForm, uiComp, UIFCCConstant.SELECTOR_POPUP_WINDOW, 640);
      String param = "returnField=" + fieldName ;
      String[] params = selectorParams == null ? new String[] { param } : new String[] { param,
          "selectorParams=" + selectorParams };
      ((ComponentSelector)uiComp).setSourceComponent(fastContentCreatorActionForm, params);
      if(fastContentCreatorActionForm.isAddNew){
        uiContainer.setRendered(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }
}
