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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 8, 2006
 * 9:41:47 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/explorer/UIFormWithoutAction.gtmpl",
    events = @EventConfig(listeners = UIActionTypeForm.ChangeActionTypeActionListener.class)
)
public class UIActionTypeForm extends UIForm {

  final static public String ACTION_TYPE = "actionType" ;
  final static public String CHANGE_ACTION = "ChangeActionType" ;

  private List<SelectItemOption<String>> typeList_ ;

  public String defaultActionType_ ;

  public UIActionTypeForm() throws Exception {
    typeList_ = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(ACTION_TYPE, ACTION_TYPE, new ArrayList<SelectItemOption<String>>()) ;
    uiSelectBox.setOnChange(CHANGE_ACTION) ;
    addUIFormInput(uiSelectBox) ;
  }

  private Iterator<NodeType> getCreatedActionTypes() throws Exception {
    ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class) ;
    String repository = getAncestorOfType(UIJCRExplorer.class).getRepositoryName() ;
    return actionService.getCreatedActionTypes(repository).iterator();
  }

  public void setDefaultActionType() throws Exception{
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    UIActionContainer uiActionContainer = getParent() ;
    Node currentNode = uiExplorer.getCurrentNode() ;
    UIActionForm uiActionForm = uiActionContainer.getChild(UIActionForm.class) ;
    uiActionForm.createNewAction(currentNode, typeList_.get(0).getValue(), true) ;
    uiActionForm.setWorkspace(currentNode.getSession().getWorkspace().getName()) ;
    uiActionForm.setStoredPath(currentNode.getPath()) ;
  }

  public void update() throws Exception {
    Iterator<NodeType> actions = getCreatedActionTypes();
    List<String> actionList = new ArrayList<String>();
    while(actions.hasNext()){
      String action =  actions.next().getName();
      actionList.add(action);
    }
    Collections.sort(actionList);
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    for(String action : actionList) {
      try {
        templateService.getTemplatePathByUser(true, action, userName);
        typeList_.add(new SelectItemOption<String>(action, action));
      } catch (Exception e){
        continue;
      }
    }
    getUIFormSelectBox(ACTION_TYPE).setOptions(typeList_) ;
    defaultActionType_ = typeList_.get(0).getValue();
    getUIFormSelectBox(ACTION_TYPE).setValue(defaultActionType_);
    setDefaultActionType();
  }

  static public class ChangeActionTypeActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiActionType = event.getSource() ;
      UIJCRExplorer uiExplorer = uiActionType.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      String actionType = uiActionType.getUIFormSelectBox(ACTION_TYPE).getValue() ;
      TemplateService templateService = uiActionType.getApplicationComponent(TemplateService.class) ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      UIApplication uiApp = uiActionType.getAncestorOfType(UIApplication.class) ;
      try {
        String templatePath =
          templateService.getTemplatePathByUser(true, actionType, userName) ;
        if(templatePath == null) {
          Object[] arg = { actionType } ;
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.access-denied", arg,
                                                  ApplicationMessage.WARNING)) ;
          
          uiActionType.getUIFormSelectBox(
              UIActionTypeForm.ACTION_TYPE).setValue(uiActionType.defaultActionType_) ;
          UIActionContainer uiActionContainer = uiActionType.getAncestorOfType(UIActionContainer.class) ;
          UIActionForm uiActionForm = uiActionContainer.getChild(UIActionForm.class) ;
          uiActionForm.createNewAction(currentNode, uiActionType.defaultActionType_, true) ;
          uiActionContainer.setRenderSibling(UIActionContainer.class) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiActionContainer) ;
          return ;
        }
      } catch(PathNotFoundException path) {
        Object[] arg = { actionType } ;
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.not-support", arg,
                                                ApplicationMessage.WARNING)) ;
        
        uiActionType.getUIFormSelectBox(
            UIActionTypeForm.ACTION_TYPE).setValue(uiActionType.defaultActionType_) ;
        UIActionContainer uiActionContainer = uiActionType.getAncestorOfType(UIActionContainer.class) ;
        UIActionForm uiActionForm = uiActionContainer.getChild(UIActionForm.class) ;
        uiActionForm.createNewAction(currentNode, uiActionType.defaultActionType_, true) ;
        uiActionContainer.setRenderSibling(UIActionContainer.class) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiActionContainer) ;
      }
      UIActionContainer uiActionContainer = uiActionType.getParent() ;
      UIActionForm uiActionForm = uiActionContainer.getChild(UIActionForm.class) ;
      uiActionForm.createNewAction(currentNode, actionType, true) ;
      uiActionContainer.setRenderSibling(UIActionContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionContainer) ;
    }
  }
}
