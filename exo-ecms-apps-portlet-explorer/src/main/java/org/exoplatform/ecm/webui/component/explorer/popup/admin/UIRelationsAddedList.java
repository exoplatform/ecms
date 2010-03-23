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

import javax.jcr.Node;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 18, 2006
 * 2:28:18 PM 
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig(listeners = UIRelationsAddedList.DeleteActionListener.class, confirm="UIRelationsAddedList.msg.confirm-delete")
    }
)
public class UIRelationsAddedList extends UIContainer implements UISelectable {
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIRelationsAddedList");
  private static String[] RELATE_BEAN_FIELD = {"path"} ;
  private static String[] ACTION = {"Delete"} ;

  public UIRelationsAddedList() throws Exception {
    UIGrid uiGrid = addChild(UIGrid.class, null, "RelateAddedList") ;
    uiGrid.setDisplayedChars(150);
    uiGrid.getUIPageIterator().setId("RelateListIterator");
    uiGrid.configure("path", RELATE_BEAN_FIELD, ACTION) ;
  }
  
  public void updateGrid (List<Node> nodes, int currentPage) throws Exception {
    UIGrid uiGrid = getChildById("RelateAddedList") ;   
    if(nodes == null) nodes = new ArrayList<Node>() ;
    ObjectPageList objPageList = new ObjectPageList(nodes, 10);
    uiGrid.getUIPageIterator().setPageList(objPageList);
    if(currentPage > uiGrid.getUIPageIterator().getAvailablePage())
      uiGrid.getUIPageIterator().setCurrentPage(currentPage-1);
    else
      uiGrid.getUIPageIterator().setCurrentPage(currentPage);
  }
  
  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    RelationsService relateService = getApplicationComponent(RelationsService.class) ;
    String currentFullPath = uiJCRExplorer.getCurrentWorkspace() + ":" + uiJCRExplorer.getCurrentNode().getPath() ;
    if(value.equals(currentFullPath)) {
      throw new MessageException(new ApplicationMessage("UIRelationsAddedList.msg.can-not-add-itself",
                                                        null, ApplicationMessage.WARNING)) ;
    }
    try {
      String repository = getAncestorOfType(UIJCRExplorer.class).getRepositoryName() ;
      String wsName = value.toString().substring(0, value.toString().indexOf(":")) ;
      String path = value.toString().substring(value.toString().indexOf(":") + 1) ;           
      Node currentNode = uiJCRExplorer.getCurrentNode();
      uiJCRExplorer.addLockToken(currentNode);
      relateService.addRelation(currentNode, path, wsName,repository) ;
      updateGrid(relateService.getRelations(currentNode, 
          uiJCRExplorer.getRepositoryName(), SessionProviderFactory.createSessionProvider()), 1);      
      setRenderSibling(UIRelationsAddedList.class) ;
    } catch(Exception e) {
      LOG.error("Unexpected error", e);
    }
  }

  static public class DeleteActionListener extends EventListener<UIRelationsAddedList> {
    public void execute(Event<UIRelationsAddedList> event) throws Exception {
      UIRelationsAddedList uiAddedList = event.getSource() ;
      UIRelationManager uiManager = uiAddedList.getParent() ;
      UIApplication uiApp = uiAddedList.getAncestorOfType(UIApplication.class) ;
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      RelationsService relationService = 
        uiAddedList.getApplicationComponent(RelationsService.class) ;
      UIJCRExplorer uiExplorer = uiAddedList.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      try {
        relationService.removeRelation(uiExplorer.getCurrentNode(), nodePath, uiExplorer.getRepositoryName());
        UIGrid uiGrid = uiAddedList.getChildById("RelateAddedList");
        uiAddedList.updateGrid(relationService.getRelations(uiExplorer.getCurrentNode(),
            uiExplorer.getRepositoryName(), SessionProviderFactory.createSessionProvider()),
            uiGrid.getUIPageIterator().getCurrentPage());
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e) ;
      }
      uiManager.setRenderedChild("UIRelationsAddedList") ;
    }
  }  
}
