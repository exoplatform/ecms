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
package org.exoplatform.workflow.webui.component.controller;

import javax.jcr.PathNotFoundException;

import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.workflow.webui.component.UIUserSelectContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 12, 2009  
 */
@ComponentConfig(template = "classpath:templates/controller/UITabPane.gtmpl")
public class UITaskManager extends UIContainer implements UIPopupComponent {
  
  private String tokenId_ ;
  private boolean isStart_ = false;
  
  public static final String UIPOPUP_DELEGATEDSELECTOR_ID = "PopupDelegatedSelectorId";
  public UITaskManager() throws Exception {
    addChild(UITask.class, null, null) ;
  }

  /**
   * Init popup window to select user
   * @param fieldName: name of textbox to fill username
   * @throws Exception
   */
  public void initPopupSelectUser(String fieldName) throws Exception {
    UIPopupWindow uiPopup = getChildById(UIPOPUP_DELEGATEDSELECTOR_ID);
    if (uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, UIPOPUP_DELEGATEDSELECTOR_ID);
    } else {
      uiPopup.setRendered(true) ;
    }
    UIUserSelectContainer uiUserSelectContainer = createUIComponent(UIUserSelectContainer.class, null, null);
    uiUserSelectContainer.setFieldname(fieldName);
    uiPopup.setWindowSize(790, 400);
    uiPopup.setUIComponent(uiUserSelectContainer);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }
  public void setTokenId(String tokenId) { tokenId_ = tokenId ; }
  
  public void setIsStart(boolean isStart) { isStart_ = isStart ; }
  
  public void activate() throws Exception { }
  
  public boolean checkBeforeActive() throws Exception {
    WorkflowServiceContainer workflowServiceContainer = 
      getApplicationComponent(WorkflowServiceContainer.class);
    try {
      UITask uiTask = getChild(UITask.class);
      uiTask.setIdentification(tokenId_);
      uiTask.setIsStart(isStart_);
      uiTask.updateUITree();
      return true;
    } catch (PathNotFoundException e) {
      Task task = workflowServiceContainer.getTask(tokenId_);
      String pid = task.getProcessInstanceId();       
      workflowServiceContainer.deleteProcessInstance(pid);
      return false;
    } 
  }

  public void deActivate() throws Exception { }
}
