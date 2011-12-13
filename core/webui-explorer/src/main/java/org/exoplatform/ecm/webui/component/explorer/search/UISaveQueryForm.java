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
package org.exoplatform.ecm.webui.component.explorer.search;

import javax.jcr.AccessDeniedException;

import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 28, 2007 9:43:21 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UISaveQueryForm.SaveActionListener.class),
      @EventConfig(listeners = UISaveQueryForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UISaveQueryForm extends UIForm implements UIPopupComponent {

  final static public String QUERY_NAME = "queryName" ;
  private String statement_ ;
  private boolean isSimpleSearch_ = false ;
  private String queryType_ ;

  public UISaveQueryForm() throws Exception {
    addUIFormInput(new UIFormStringInput(QUERY_NAME, QUERY_NAME, null).
                   addValidator(ECMNameValidator.class).
                   addValidator(MandatoryValidator.class)) ;
  }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}

  public void setSimpleSearch(boolean isSimpleSearch) { isSimpleSearch_ = isSimpleSearch ; }

  public void setStatement(String statement) { statement_ = statement ; }

  public void setQueryType(String queryType) { queryType_ = queryType ; }

  static  public class SaveActionListener extends EventListener<UISaveQueryForm> {
    public void execute(Event<UISaveQueryForm> event) throws Exception {
      UISaveQueryForm uiSaveQueryForm = event.getSource() ;
      UIECMSearch uiECMSearch = uiSaveQueryForm.getAncestorOfType(UIECMSearch.class) ;
      UIApplication uiApp = uiSaveQueryForm.getAncestorOfType(UIApplication.class) ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      QueryService queryService = uiSaveQueryForm.getApplicationComponent(QueryService.class) ;
      String queryName = uiSaveQueryForm.getUIStringInput(QUERY_NAME).getValue() ;
      if(queryName == null || queryName.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UISaveQueryForm.msg.query-name-null", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiECMSearch);
        return ;
      }
      try {
        queryService.addQuery(queryName, uiSaveQueryForm.statement_, uiSaveQueryForm.queryType_, userName) ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UISaveQueryForm.msg.access-denied", null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiECMSearch);
        return ;
      } catch (Exception e){
        uiApp.addMessage(new ApplicationMessage("UISaveQueryForm.msg.save-failed", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiECMSearch);
        return ;
      }
      uiECMSearch.getChild(UISavedQuery.class).updateGrid(1);
      if(uiSaveQueryForm.isSimpleSearch_) {
        UISearchContainer uiSearchContainer = uiSaveQueryForm.getAncestorOfType(UISearchContainer.class) ;
        UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
        uiPopup.deActivate() ;
      }
      uiECMSearch.setSelectedTab(uiECMSearch.getChild(UISavedQuery.class).getId()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiECMSearch) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UISaveQueryForm> {
    public void execute(Event<UISaveQueryForm> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class) ;
      UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
}
