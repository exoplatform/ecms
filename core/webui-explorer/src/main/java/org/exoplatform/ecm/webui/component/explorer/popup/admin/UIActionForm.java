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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.nodetype.selector.UINodeTypeSelector;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 8, 2006
 * 11:23:50 AM
 */
@ComponentConfigs( {
    @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset", events = {
        @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, events = {
        @EventConfig(listeners = UIActionForm.SaveActionListener.class),
        @EventConfig(listeners = UIDialogForm.OnchangeActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIActionForm.BackActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIActionForm.ShowComponentActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIActionForm.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIActionForm.RemoveActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIActionForm.RemoveReferenceActionListener.class,
                     confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE) }) })
public class UIActionForm extends UIDialogForm implements UISelectable {

  private String parentPath_;
  private String nodeTypeName_ = null;
  private boolean isAddNew_;
  private String scriptPath_ = null;
  private boolean isEditInList_ = false;
  private String rootPath_ = null;
  private String currentAction = null;


  private static final String EXO_ACTIONS = "exo:actions";
  private static final Log LOG  = ExoLogger.getLogger(UIActionForm.class);

  public String getDriverName() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    return uiExplorer.getRepositoryName() + "/" + uiExplorer.getDriveData().getName();
  }

  public UIActionForm() throws Exception {
    setActions(new String[]{"Save","Back"});
  }

  public void createNewAction(Node parentNode, String actionType, boolean isAddNew) throws Exception {
    reset();
    parentPath_ = parentNode.getPath();
    nodeTypeName_ = actionType;
    isAddNew_ = isAddNew;
    componentSelectors.clear();
    properties.clear();
    getChildren().clear();
  }

  private Node getParentNode() throws Exception{ return (Node) getSession().getItem(parentPath_); }

  /**
   * @param currentAction the currentAction to set
   */
  public void setCurrentAction(String currentAction) {
    this.currentAction = currentAction;
  }

  /**
   * @return the currentAction
   */
  public String getCurrentAction() {
    return currentAction;
  }

  public void doSelect(String selectField, Object value) throws Exception {
    isUpdateSelect = true;
    UIComponent uicomponent = getChildById(selectField);
    if (UIFormStringInput.class.isInstance(uicomponent))
      ((UIFormStringInput)uicomponent).setValue(value.toString());
    else if (UIFormMultiValueInputSet.class.isInstance(uicomponent)) {
      ((UIFormMultiValueInputSet)uicomponent).setValue((ArrayList<String>)value);
    }
    if(isEditInList_) {
      UIActionManager uiManager = getAncestorOfType(UIActionManager.class);
      UIActionListContainer uiActionListContainer = uiManager.getChild(UIActionListContainer.class);
      uiActionListContainer.removeChildById("PopupComponent");
    } else {
      UIActionContainer uiActionContainer = getParent();
      uiActionContainer.removeChildById("PopupComponent");
    }
  }

  public String getCurrentPath() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode().getPath();
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public String getTemplate() { return getDialogPath(); }

  public String getDialogPath() {
    repositoryName = getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    String dialogPath = null;
    if (nodeTypeName_ != null) {
      try {
        dialogPath = templateService.getTemplatePathByUser(true, nodeTypeName_, userName);
      } catch (Exception e){
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    return dialogPath;
  }

  public String getRepositoryName() { return repositoryName; }

  @Deprecated
  public String getTenmplateNodeType() { return getTemplateNodeType(); }

  public String getTemplateNodeType() { return nodeTypeName_; }

  private void setPath(String scriptPath) {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if(scriptPath.indexOf(":") < 0) {
      scriptPath = uiExplorer.getCurrentWorkspace() + ":" + scriptPath;
    }
    scriptPath_ = scriptPath;
  }
  public String getPath() { return scriptPath_; }
  public void setRootPath(String rootPath){
   rootPath_ = rootPath;
  }
  public String getRootPath(){return rootPath_;}
  public void setIsEditInList(boolean isEditInList) { isEditInList_ = isEditInList; }

  public void onchange(Event<?> event) throws Exception {
    if(isEditInList_ || !isAddNew_) {
      event.getRequestContext().addUIComponentToUpdateByAjax(getParent());
      return;
    }
    UIActionManager uiManager = getAncestorOfType(UIActionManager.class);
    uiManager.setRenderedChild(UIActionContainer.class);
    event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
  }

  public void renderField(String name) throws Exception {
    UIComponent uiInput = findComponentById(name);
    if ("homePath".equals(name)) {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      String homPath = uiExplorer.getCurrentWorkspace() + ":" + currentNode.getPath();
      ((UIFormStringInput) uiInput).setValue(homPath);
    }

    super.renderField(name);
  }

  static public class SaveActionListener extends EventListener<UIActionForm> {

    private void addInputInfo(Map<String, JcrInputProperty> input, UIActionForm actionForm) throws Exception {
      String rssUrlKey = "/node/exo:url";
      if (input.get(rssUrlKey) == null) return;
      UIJCRExplorer uiExplorer = actionForm.getAncestorOfType(UIJCRExplorer.class);
      //drive name
      UITreeExplorer treeExplorer = uiExplorer.findFirstComponentOfType(UITreeExplorer.class);
      String driveName = treeExplorer.getDriveName();
       //requestUri
      PortalRequestContext pContext = Util.getPortalRequestContext();      
      NodeURL nodeURL = pContext.createURL(NodeURL.TYPE);
      NavigationResource resource = new NavigationResource(Util.getUIPortal().getSelectedUserNode());
      nodeURL.setResource(resource);
      nodeURL.setQueryParameterValue("path", driveName);
      nodeURL.setSchemeUse(true);
      
      input.get(rssUrlKey).setValue(nodeURL.toString());
    }

    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm actionForm = event.getSource();
      UIApplication uiApp = actionForm.getAncestorOfType(UIApplication.class);
      ActionServiceContainer actionServiceContainer = actionForm.getApplicationComponent(ActionServiceContainer.class);
      UIJCRExplorer uiExplorer = actionForm.getAncestorOfType(UIJCRExplorer.class);
      String repository = actionForm.getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
      Map<String, JcrInputProperty> sortedInputs = DialogFormUtil.prepareMap(actionForm.getChildren(),
                                                                             actionForm.getInputProperties(),
                                                                             actionForm.getInputOptions());

      addInputInfo(sortedInputs, actionForm);

      Node currentNode = uiExplorer.getCurrentNode();
      if(!PermissionUtil.canAddNode(currentNode) || !PermissionUtil.canSetProperty(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.no-permission-add", null));
        
        return;
      }
      UIFormStringInput homePathInput = actionForm.getUIStringInput("homePath");
      if (homePathInput != null) {
        String targetPath = homePathInput.getValue();
        if ((targetPath == null) || (targetPath.length() == 0)) {
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.homePath-emty", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
      UIFormStringInput targetPathInput = actionForm.getUIStringInput("targetPath");
      if (targetPathInput != null) {
        String targetPath = targetPathInput.getValue();
        if ((targetPath == null) || (targetPath.length() == 0)) {
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.targetPath-emty", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
         
      String actionName = (String)(sortedInputs.get("/node/exo:name")).getValue();
      String[] arrFilterChar = { "&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", "'",
          "#", ";", "}", "{", "/", "|", "\"" };
      for(String filterChar : arrFilterChar) {
        if(actionName.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.name-not-allowed", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
      
      Node parentNode = actionForm.getParentNode();
      if (actionForm.isAddNew_) {
        if (parentNode.hasNode(EXO_ACTIONS)) {
          if (parentNode.getNode(EXO_ACTIONS).hasNode(actionName)) {
            Object[] args = { actionName };
            uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.existed-action",
                                                    args,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
        }
      } else if (actionForm.isEditInList_) {
        if (parentNode.hasNode(EXO_ACTIONS)) {
          if (parentNode.getNode(EXO_ACTIONS).hasNode(actionName)
              && !actionName.equals(actionForm.currentAction)) {
            Object[] args = { actionName };
            uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.existed-action",
                                                    args,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
        }
      }
      
      try{
        if (uiExplorer.nodeIsLocked(currentNode)) return;
        if (!actionForm.isAddNew_) {
          CmsService cmsService = actionForm.getApplicationComponent(CmsService.class);
          Node storedHomeNode = actionForm.getNode().getParent();
          Node currentActionNode = storedHomeNode.getNode(sortedInputs.get("/node").getValue().toString());
          if (uiExplorer.nodeIsLocked(currentActionNode)) return;
          cmsService.storeNode(actionForm.nodeTypeName_, storedHomeNode, sortedInputs, false);
          Session session = currentActionNode.getSession();
          if (uiExplorer.nodeIsLocked(currentActionNode))
            return; // We add LockToken again because CMSService did logout
                    // session cause lost lock information
          session.move(currentActionNode.getPath(), storedHomeNode.getPath() + "/"
              + sortedInputs.get("/node/exo:name").getValue().toString());
          session.save();

          currentNode.getSession().save();
          if (actionForm.isEditInList_) {
            UIActionManager uiManager = actionForm.getAncestorOfType(UIActionManager.class);
            UIPopupWindow uiPopup = uiManager.findComponentById("editActionPopup");
            uiPopup.setShow(false);
            uiPopup.setRendered(false);
            uiManager.setDefaultConfig();
            actionForm.isEditInList_ = false;
            //actionForm.isAddNew_ = true;
            actionForm.setIsOnchange(false);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
            uiExplorer.setIsHidePopup(true);
            uiExplorer.updateAjax(event);
          } else {
            uiExplorer.setIsHidePopup(false);
            uiExplorer.updateAjax(event);
          }
          actionForm.setPath(storedHomeNode.getPath());
          actionServiceContainer.removeAction(currentNode, currentActionNode.getName(), repository);
          //return;
        }
        JcrInputProperty rootProp = sortedInputs.get("/node");
        if(rootProp == null) {
          rootProp = new JcrInputProperty();
          rootProp.setJcrPath("/node");
          rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue());
          sortedInputs.put("/node", rootProp);
        } else {
          rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue());
        }
        
        if (parentNode.isNew()) {
          String[] args = { parentNode.getPath() };
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.unable-add-action", args));
          
          return;
        }
        actionServiceContainer.addAction(parentNode, actionForm.nodeTypeName_, sortedInputs);
        actionForm.setIsOnchange(false);
        parentNode.getSession().save();
        UIActionManager uiActionManager = actionForm.getAncestorOfType(UIActionManager.class);
        actionForm.createNewAction(uiExplorer.getCurrentNode(), actionForm.nodeTypeName_, true);
        UIActionList uiActionList = uiActionManager.findFirstComponentOfType(UIActionList.class);
        uiActionList.updateGrid(parentNode, uiActionList.getChild(UIPageIterator.class).getCurrentPage());
        uiActionManager.setRenderedChild(UIActionListContainer.class);
        actionForm.reset();
      } catch(ConstraintViolationException cex) {
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.constraintviolation-exception",
                                                null,
                                                ApplicationMessage.WARNING));
          
          return;
      } catch(RepositoryException repo) {
        String key = "UIActionForm.msg.repository-exception";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        
        return;
      } catch(NumberFormatException nume) {
        String key = "UIActionForm.msg.numberformat-exception";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        
        return;
      } catch (NullPointerException nullPointerException) {
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.unable-add", null, ApplicationMessage.WARNING));
        
        return;
      } catch (NoSuchFieldException ns) {
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.lifecycle-invalid", null, ApplicationMessage.WARNING));
        
        return;
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.unable-add", null, ApplicationMessage.WARNING));
        
        return;
      } finally {
        if (actionForm.isEditInList_) {
          actionForm.releaseLock();
          actionForm.isEditInList_ = false;
        }
      }

    }
  }

  @SuppressWarnings("unchecked")
  static public class ShowComponentActionListener extends EventListener<UIActionForm> {
    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm uiForm = event.getSource();
      UIContainer uiContainer = null;
      uiForm.isShowingComponent = true;
      if(uiForm.isEditInList_) {
        uiContainer = uiForm.getAncestorOfType(UIActionListContainer.class);
      } else {
        uiContainer = uiForm.getParent();
      }
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      Map fieldPropertiesMap = uiForm.componentSelectors.get(fieldName);
      String classPath = (String)fieldPropertiesMap.get("selectorClass");
      String rootPath = (String)fieldPropertiesMap.get("rootPath");
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class clazz = Class.forName(classPath, true, cl);
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      String selectorParams = (String)fieldPropertiesMap.get("selectorParams");
      if(uiComp instanceof UIOneNodePathSelector) {
        UIJCRExplorer explorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
        String repositoryName = explorer.getRepositoryName();
        SessionProvider provider = explorer.getSessionProvider();
        String wsFieldName = (String)fieldPropertiesMap.get("workspaceField");
        String wsName = explorer.getCurrentWorkspace();
        if(wsFieldName != null && wsFieldName.length() > 0) {
          wsName = (String)uiForm.<UIFormInputBase>getUIInput(wsFieldName).getValue();
          ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true);
        }
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",");
          if(arrParams.length == 4) {
            ((UIOneNodePathSelector)uiComp).setAcceptedNodeTypesInPathPanel(new String[] {Utils.NT_FILE});
            wsName = arrParams[1];
            rootPath = arrParams[2];
            ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true);
            if(arrParams[3].indexOf(";") > -1) {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(arrParams[3].split(";"));
            } else {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(new String[] {arrParams[3]});
            }
          }
        }
        if(rootPath == null) rootPath = "/";
        ((UIOneNodePathSelector)uiComp).setRootNodeLocation(repositoryName, wsName, rootPath);
        ((UIOneNodePathSelector)uiComp).setShowRootPathSelect(true);
        ((UIOneNodePathSelector)uiComp).init(provider);
      } else if (uiComp instanceof UINodeTypeSelector) {
        UIJCRExplorer explorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
        ((UINodeTypeSelector)uiComp).setRepositoryName(explorer.getRepositoryName());
        UIFormMultiValueInputSet uiFormMultiValueInputSet = uiForm.getChildById(fieldName);
        List values = uiFormMultiValueInputSet.getValue();
        ((UINodeTypeSelector)uiComp).init(1, values);
      }

      if(uiForm.isEditInList_) ((UIActionListContainer) uiContainer).initPopup(uiComp);
      else ((UIActionContainer)uiContainer).initPopup(uiComp);
      String param = "returnField=" + fieldName;
      String[] params = selectorParams == null ? new String[] { param } : new String[] { param,
          "selectorParams=" + selectorParams };
      ((ComponentSelector)uiComp).setSourceComponent(uiForm, params);
      if(uiForm.isAddNew_) {
        UIContainer uiParent = uiContainer.getParent();
        uiParent.setRenderedChild(uiContainer.getId());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class RemoveReferenceActionListener extends EventListener<UIActionForm> {
    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm uiForm = event.getSource();
      uiForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIComponent uicomponent = uiForm.getChildById(fieldName);
      if (UIFormStringInput.class.isInstance(uicomponent))
        ((UIFormStringInput)uicomponent).setValue(null);
      else if (UIFormMultiValueInputSet.class.isInstance(uicomponent)) {
        ((UIFormMultiValueInputSet)uicomponent).setValue(new ArrayList<String>());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class AddActionListener extends EventListener<UIActionForm> {
    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm uiForm = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class RemoveActionListener extends EventListener<UIActionForm> {
    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm uiForm = event.getSource();
      uiForm.isRemoveActionField = true;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class BackActionListener extends EventListener<UIActionForm> {
    public void execute(Event<UIActionForm> event) throws Exception {
      UIActionForm uiForm = event.getSource();
      UIActionManager uiManager = uiForm.getAncestorOfType(UIActionManager.class);
      if(uiForm.isAddNew_) {
        uiManager.setRenderedChild(UIActionListContainer.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
      } else {
        if(uiForm.isEditInList_) {
          uiForm.releaseLock();
          uiManager.setRenderedChild(UIActionListContainer.class);
          uiManager.setDefaultConfig();
          UIActionListContainer uiActionListContainer = uiManager.getChild(UIActionListContainer.class);
          UIPopupWindow uiPopup = uiActionListContainer.findComponentById("editActionPopup");
          uiPopup.setShow(false);
          uiPopup.setRendered(false);
          uiForm.isEditInList_ = false;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
        } else {
          UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
          uiExplorer.cancelAction();
        }
      }
    }
  }
}
