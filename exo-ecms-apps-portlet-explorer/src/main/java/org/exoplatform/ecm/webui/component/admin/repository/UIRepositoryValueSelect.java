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

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * 19-07-2007  
 */

@ComponentConfig(
    template = "app:/groovy/webui/component/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIRepositoryValueSelect.SelectActionListener.class),
        @EventConfig(listeners = UIRepositoryValueSelect.CloseActionListener.class)
    }
)
public class UIRepositoryValueSelect  extends UIGrid implements UIPopupComponent {
  private static String[] NODETYPE_BEAN_FIELD = {"name"} ;
  private static String[] NODETYPE_ACTION = {"Select"} ;
  protected boolean isSetAuthentication_ = false ;
  protected boolean isSetContainer_ = false ;
  protected boolean isSetStoreType_ = false ;
  protected boolean isSetQueryHandler_ = false ;

  public UIRepositoryValueSelect() throws Exception{
    getUIPageIterator().setId("ValueSelectIterator") ;
    configure("name", NODETYPE_BEAN_FIELD, NODETYPE_ACTION) ;

  }
  public String[] getActions() {
    return new String[] {"Close"} ;
  }
  public void updateGrid(List<ClassData> datas) throws Exception {
    ObjectPageList objPageList = new ObjectPageList(datas, 10) ;
    getUIPageIterator().setPageList(objPageList) ;
  }

  static public class SelectActionListener extends EventListener<UIRepositoryValueSelect> {
    public void execute(Event<UIRepositoryValueSelect> event) throws Exception {
      UIRepositoryValueSelect repoValueList = event.getSource() ;
      String value =  event.getRequestContext().getRequestParameter(OBJECTID) ;
      if(repoValueList.isSetAuthentication_) {
        UIRepositoryFormContainer uiRepoContainer = repoValueList.getAncestorOfType(UIRepositoryFormContainer.class);
        uiRepoContainer.getChild(UIRepositoryForm.class).setAuthentication(value) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRepoContainer) ;
      }
      else {
        UIWorkspaceWizardContainer uiWSContainer = repoValueList.getAncestorOfType(UIWorkspaceWizardContainer.class) ;
        if(repoValueList.isSetContainer_) {
          uiWSContainer.getChild(UIWorkspaceWizard.class).setContainerName(value);
        } else if(repoValueList.isSetStoreType_) {
          uiWSContainer.getChild(UIWorkspaceWizard.class).setStoreTypeName(value);
        } else if(repoValueList.isSetQueryHandler_) {
          uiWSContainer.getChild(UIWorkspaceWizard.class).setQueryHandlerName(value);
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uiWSContainer) ;
      }
      UIPopupContainer uiPopup = repoValueList.getAncestorOfType(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
  static public class CloseActionListener extends EventListener<UIRepositoryValueSelect> {
    public void execute(Event<UIRepositoryValueSelect> event) throws Exception {
      UIRepositoryValueSelect repoValueList = event.getSource() ;
      UIPopupContainer uiPopup = repoValueList.getAncestorOfType(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
  static public class ClassData {
    private String name ;

    public ClassData(String temp ) { name = temp ;}
    public String getName() { return name ;}
  }
  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }
  public void deActivate() throws Exception {
    isSetAuthentication_ = false ;
    isSetContainer_ = false ;
    isSetStoreType_ = false ;
    isSetQueryHandler_ = false ;
  }
}
