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

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCPortlet;
import org.exoplatform.wcm.webui.fastcontentcreator.UIFCCUtils;
import org.exoplatform.wcm.webui.fastcontentcreator.config.UIFCCConfig;
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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/FastContentCreatorPortlet/UIFCCActionTypeForm.gtmpl",
    events = {
      @EventConfig(listeners = UIFCCActionTypeForm.ChangeActionTypeActionListener.class)
    }
)
public class UIFCCActionTypeForm extends UIForm {

  /** The Constant ACTION_TYPE. */
  final static public String ACTION_TYPE = "actionType" ;

  /** The Constant CHANGE_ACTION. */
  final static public String CHANGE_ACTION = "ChangeActionType" ;

  /** The type list_. */
  private List<SelectItemOption<String>> typeList_ ;

  /** The node path. */
  private String nodePath = null;

  /** The default action type_. */
  public String defaultActionType_ ;

  /**
   * Instantiates a new uIFCC action type form.
   *
   * @throws Exception the exception
   */
  public UIFCCActionTypeForm() throws Exception {
    typeList_ = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(ACTION_TYPE,
                                                      ACTION_TYPE,
                                                      new ArrayList<SelectItemOption<String>>());
    uiSelectBox.setOnChange(CHANGE_ACTION) ;
    addUIFormInput(uiSelectBox) ;
  }

  /**
   * Gets the created action types.
   *
   * @return the created action types
   *
   * @throws Exception the exception
   */
  private Iterator<NodeType> getCreatedActionTypes() throws Exception {
    ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class) ;
    return actionService.getCreatedActionTypes(UIFCCUtils.getPreferenceRepository()).iterator();
  }

  /**
   * Sets the default action type.
   *
   * @throws Exception the exception
   */
  public void setDefaultActionType() throws Exception{
    boolean isNews = true;
    UIFCCPortlet fastContentCreatorPortlet = getAncestorOfType(UIFCCPortlet.class);
    UIFCCConfig fastContentCreatorConfig = fastContentCreatorPortlet.getChild(UIFCCConfig.class);
    Node savedLocationNode = fastContentCreatorConfig.getSavedLocationNode() ;
    UIFCCActionContainer fastContentCreatorActionContainer = getParent() ;
    UIFCCActionForm fastContentCreatorActionForm = fastContentCreatorActionContainer.getChild(UIFCCActionForm.class);
    if(defaultActionType_ == null) {
      defaultActionType_ = "exo:addMetadataAction";
      isNews = true;
    }else{
      isNews = false;
    }
    fastContentCreatorActionForm.setNodePath(nodePath) ;
    getUIFormSelectBox(ACTION_TYPE).setValue(defaultActionType_).setDisabled(!isNews);

    fastContentCreatorActionForm.createNewAction(savedLocationNode, defaultActionType_, isNews);
    fastContentCreatorActionForm.setWorkspace(savedLocationNode.getSession()
                                                               .getWorkspace()
                                                               .getName());
    fastContentCreatorActionForm.setStoredPath(savedLocationNode.getPath());
  }

  /**
   * Update.
   *
   * @throws Exception the exception
   */
  public void update() throws Exception {
    Iterator<NodeType> actions = getCreatedActionTypes();
    while(actions.hasNext()){
      String action =  actions.next().getName();
      typeList_.add(new SelectItemOption<String>(action, action));
    }
    getUIFormSelectBox(ACTION_TYPE).setOptions(typeList_) ;
    setDefaultActionType() ;
  }

  /**
   * Inits the.
   *
   * @param nodePath the node path
   * @param actionType the action type
   *
   * @throws RepositoryException the repository exception
   */
  public void init(String nodePath, String actionType) throws RepositoryException {
    this.nodePath = nodePath;
    this.defaultActionType_ = actionType;
  }

  /**
   * The listener interface for receiving changeActionTypeAction events.
   * The class that is interested in processing a changeActionTypeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeActionTypeActionListener<code> method. When
   * the changeActionTypeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeActionTypeActionEvent
   */
  static public class ChangeActionTypeActionListener extends EventListener<UIFCCActionTypeForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCActionTypeForm> event) throws Exception {
      UIFCCActionTypeForm fastContentCreatorActionTypeForm = event.getSource() ;
      UIFCCPortlet fastContentCreatorPortlet = fastContentCreatorActionTypeForm.getAncestorOfType(UIFCCPortlet.class);
      UIFCCConfig fastContentCreatorConfig = fastContentCreatorPortlet.getChild(UIFCCConfig.class);
      Node currentNode = fastContentCreatorConfig.getSavedLocationNode() ;
      String actionType = fastContentCreatorActionTypeForm.getUIFormSelectBox(ACTION_TYPE).getValue() ;
      TemplateService templateService = fastContentCreatorActionTypeForm.getApplicationComponent(TemplateService.class) ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      UIApplication uiApp = fastContentCreatorActionTypeForm.getAncestorOfType(UIApplication.class) ;
      try {
        String templatePath = templateService.getTemplatePathByUser(true, actionType, userName) ;
        if (templatePath == null) {
          Object[] arg = { actionType };
          uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionTypeForm.msg.access-denied",
                                                  arg,
                                                  ApplicationMessage.WARNING));
          
          actionType = "exo:addMetadataAction" ;
          fastContentCreatorActionTypeForm.getUIFormSelectBox(UIFCCActionTypeForm.ACTION_TYPE).setValue(actionType) ;
          UIFCCActionContainer fastContentCreatorActionContainer = fastContentCreatorActionTypeForm.
              getAncestorOfType(UIFCCActionContainer.class);
          UIFCCActionForm fastContentCreatorActionForm = fastContentCreatorActionContainer.getChild(UIFCCActionForm.class);
          fastContentCreatorActionForm.createNewAction(currentNode, actionType, true);
          event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorActionContainer);
          return ;
        }
      } catch (PathNotFoundException path) {
        Object[] arg = { actionType } ;
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatorActionTypeForm.msg.not-support",
                                                arg,
                                                ApplicationMessage.WARNING));
        
        actionType = "exo:addMetadataAction" ;
        fastContentCreatorActionTypeForm.getUIFormSelectBox(UIFCCActionTypeForm.ACTION_TYPE).setValue(actionType) ;
        UIFCCActionContainer fastContentCreatorActionContainer = fastContentCreatorActionTypeForm.
            getAncestorOfType(UIFCCActionContainer.class);
        UIFCCActionForm fastContentCreatorActionForm = fastContentCreatorActionContainer.getChild(UIFCCActionForm.class);
        fastContentCreatorActionForm.createNewAction(currentNode, actionType, true);
        event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorActionContainer);
      }
      UIFCCActionContainer fastContentCreatorActionContainer = fastContentCreatorActionTypeForm.getParent();
      UIFCCActionForm uiActionForm = fastContentCreatorActionContainer.getChild(UIFCCActionForm.class);
      uiActionForm.createNewAction(currentNode, actionType, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorActionContainer);
    }
  }
}
