/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 11, 2009  
 * 4:31:33 PM
 */
@ComponentConfig( 
    lifecycle = UIContainerLifecycle.class,
//    template = "app:/groovy/webui/component/admin/folksonomy/UIFolksonomyManager.gtmpl",
    events = {	@EventConfig(listeners = UITagManager.EditStyleActionListener.class),
    						@EventConfig(listeners = UITagManager.AddStyleActionListener.class),
    				 		@EventConfig(listeners = UITagManager.RemoveStyleActionListener.class, confirm = "UIFolksonomyManager.msg.confirm-delete") 
  				 	 }							
)
public class UITagManager extends UIContainer {

  public UITagManager() throws Exception {
    addChild(UITagStyleList.class, null, null);
    addChild(UITagStyleAddAction.class, null, null);
  }
  
  public void refresh() throws Exception {
    update();
  }
  
  public void update() throws Exception {
    getChild(UITagStyleList.class).updateGrid() ;
  }
  
  public void initTaggingFormPopup(Node selectedTagStyle) throws Exception {
    removeChildById("FolksonomyPopup") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "FolksonomyPopup") ;
    uiPopup.setWindowSize(600, 500) ;
    UITagStyleForm uiForm = createUIComponent(UITagStyleForm.class, null, null) ;
    uiForm.setTagStyle(selectedTagStyle) ;
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
  
  public Node getSelectedTagStyle(String tagStyleName) throws Exception {
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    String workspace = getAncestorOfType(UIECMAdminPortlet.class).getDMSSystemWorkspace(repository);
    for(Node tagStyle: newFolksonomyService.getAllTagStyle(repository, workspace)) {
      if(tagStyle.getName().equals(tagStyleName)) return tagStyle ;
    }
    return null ;
  }
  
  static public class EditStyleActionListener extends EventListener<UITagManager> {
    public void execute(Event<UITagManager> event) throws Exception {
      UITagManager uiManager = event.getSource() ;
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node selectedTagStyle = uiManager.getSelectedTagStyle(selectedName) ;
      uiManager.initTaggingFormPopup(selectedTagStyle) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
  
  static public class RemoveStyleActionListener extends EventListener<UITagManager> {
  	public void execute(Event<UITagManager> event) throws Exception {
  		UITagManager uiManager = event.getSource();
  		String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
  		Node selectedTagStyle = uiManager.getSelectedTagStyle(selectedName);
//      NewFolksonomyService newFolksonomyService = uiManager.getApplicationComponent(NewFolksonomyService.class) ;
      Node parentNode = selectedTagStyle.getParent();
      selectedTagStyle.remove();
      parentNode.getSession().save();
      uiManager.getChild(UITagStyleList.class).updateGrid();
  		event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
  	}
  }
  
  static public class AddStyleActionListener extends EventListener<UITagManager> {
  	public void execute(Event<UITagManager> event) throws Exception {
      UITagManager uiManager = event.getSource() ;
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID) ;
//      Node selectedTagStyle = uiManager.getSelectedTagStyle(selectedName) ;
      uiManager.initTaggingFormPopup(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
  	}
  }

}
