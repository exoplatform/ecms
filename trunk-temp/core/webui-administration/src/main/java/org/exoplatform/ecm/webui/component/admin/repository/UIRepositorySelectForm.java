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
package org.exoplatform.ecm.webui.component.admin.repository;

import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Jun 2, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/UIRepositorySelectForm.gtmpl",
    events = {@EventConfig(phase=Phase.DECODE, listeners = UIRepositorySelectForm.OnchangeActionListener.class)}
)

public class UIRepositorySelectForm extends UIForm {
  final public static String FIELD_SELECTREPO = "selectRepo" ;
  private static final Log LOG  = ExoLogger.getLogger("admin.UIRepositorySelectForm");
  public UIRepositorySelectForm() {
    addChild(new UIFormSelectBox(FIELD_SELECTREPO, FIELD_SELECTREPO, null)) ;
  }

  protected void setOptionValue(List<SelectItemOption<String>> list){
    getUIFormSelectBox(FIELD_SELECTREPO).setOptions(list) ; 
  }

  protected void setActionEvent(){
    getUIFormSelectBox(FIELD_SELECTREPO).setOnChange("Onchange") ;
  }

  protected String getSelectedValue() {    
    return getUIFormSelectBox(FIELD_SELECTREPO).getValue() ;
  }

  protected void setSelectedValue(String value) {    
    getUIFormSelectBox(FIELD_SELECTREPO).setValue(value) ;
  }  

  public static class OnchangeActionListener extends EventListener<UIRepositorySelectForm>{
    public void execute(Event<UIRepositorySelectForm> event) throws Exception {
      UIRepositorySelectForm uiForm = event.getSource() ;
      RepositoryService rservice = uiForm.getApplicationComponent(RepositoryService.class) ;
      UIRepositoryControl uiControl = uiForm.getAncestorOfType(UIRepositoryControl.class) ;
      PortletRequestContext requestContext = (PortletRequestContext)event.getRequestContext() ;
      PortletRequest portletRequest = requestContext.getRequest() ;
      PortletPreferences portletPref = portletRequest.getPreferences() ;
      String oldRepository = portletPref.getValue(Utils.REPOSITORY, "") ; 
      String selectRepo = uiForm.getSelectedValue() ;
      portletPref.setValue(Utils.REPOSITORY, selectRepo) ;
      portletPref.store() ;
      uiForm.setOptionValue(uiControl.getRepoItem(true, uiForm.getApplicationComponent(RepositoryService.class))) ;
      uiForm.setSelectedValue(selectRepo) ;
      rservice.setCurrentRepositoryName(selectRepo) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      try {
        uiForm.getAncestorOfType(UIECMAdminPortlet.class).initChilds() ;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UIRepositorySelectForm.msg-accessdenied", new Object[]{selectRepo}))  ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        portletPref.setValue(Utils.REPOSITORY, oldRepository) ;
        portletPref.store() ;
        uiForm.setOptionValue(uiControl.getRepoItem(true, uiForm.getApplicationComponent(RepositoryService.class))) ;
        uiForm.setSelectedValue(oldRepository) ;
        rservice.setCurrentRepositoryName(oldRepository) ;
      } catch (Exception e) {
        LOG.error("Unexpected error", e);
        uiApp.addMessage(new ApplicationMessage("UIRepositorySelectForm.msg-editError", new Object[]{selectRepo}))  ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        portletPref.setValue(Utils.REPOSITORY, oldRepository) ;
        portletPref.store() ;
        uiForm.setOptionValue(uiControl.getRepoItem(true, uiForm.getApplicationComponent(RepositoryService.class))) ;
        uiForm.setSelectedValue(oldRepository) ;
        rservice.setCurrentRepositoryName(oldRepository) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiControl.getAncestorOfType(UIECMAdminPortlet.class)) ;
    }
  }
}
