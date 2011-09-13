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
package org.exoplatform.workflow.webui.component.administration;

import java.io.InputStream;

import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 5, 2007 2:43:15 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIUploadProcess.SaveActionListener.class),
      @EventConfig(listeners = UIUploadProcess.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UIUploadProcess extends UIForm {

  final static public String FIELD_NAME =  "name" ;
  final static public String FIELD_UPLOAD = "upload" ;

  public UIUploadProcess() throws Exception {
    setMultiPart(true);
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null));
    UIFormUploadInput uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD);
    uiInput.setAutoUpload(true);
    addUIFormInput(uiInput) ;
  }

  static  public class SaveActionListener extends EventListener<UIUploadProcess> {
    public void execute(Event<UIUploadProcess> event) throws Exception {
      UIUploadProcess uiUploadProcess = event.getSource() ;
      UIWorkflowAdministrationPortlet uiWorkflowAdministrationPortlet =
        uiUploadProcess.getAncestorOfType(UIWorkflowAdministrationPortlet.class) ;
      WorkflowServiceContainer workflowServiceContainer =
        uiUploadProcess.getApplicationComponent(WorkflowServiceContainer.class) ;
      UIApplication uiApp = uiUploadProcess.getAncestorOfType(UIApplication.class) ;
      UIFormUploadInput input = (UIFormUploadInput)uiUploadProcess.getUIInput(FIELD_UPLOAD);
      if(input.getUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UIUploadProcess.msg.fileName-error", null)) ;
        
        return ;

      }
      String fileName = input.getUploadResource().getFileName() ;
      if(fileName == null || fileName.equals("")) {
        uiApp.addMessage(new ApplicationMessage("UIUploadProcess.msg.fileName-error", null)) ;
        
        return ;
      }

      InputStream inputStream = input.getUploadDataAsStream();
      String name = uiUploadProcess.getUIStringInput(FIELD_NAME).getValue() ;
      if(name == null) name = fileName;
      String[] arrFilterChar = {"&", "$", "@", ":","]", "[", "*", "%", "!"} ;
      for(String filterChar : arrFilterChar) {
        if(name.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIUploadProcess.msg.fileName-invalid", null,
                                                   ApplicationMessage.WARNING)) ;
          
          return ;
        }
      }
      try {
        workflowServiceContainer.deployProcess(inputStream) ;
        uiApp.addMessage(new ApplicationMessage("UIUploadProcess.msg.process-successful", null)) ;
        
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIUploadProcess.msg.data-invalid", null,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      }
      UIAdministrationManager uiAdminstrationManager =
        uiWorkflowAdministrationPortlet.getChild(UIAdministrationManager.class) ;
      uiAdminstrationManager.updateMonitorGrid() ;
      UIPopupWindow uiPopup = uiWorkflowAdministrationPortlet.getChildById("UploadProcessPopup") ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkflowAdministrationPortlet) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIUploadProcess> {
    public void execute(Event<UIUploadProcess> event) throws Exception {
      UIWorkflowAdministrationPortlet uiWorkflowAdministrationPortlet =
        event.getSource().getAncestorOfType(UIWorkflowAdministrationPortlet.class) ;
      UIPopupWindow uiPopup = uiWorkflowAdministrationPortlet.getChildById("UploadProcessPopup") ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkflowAdministrationPortlet) ;
    }
  }

}
