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

import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.cms.categories.CategoriesService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
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
    template = "app:/groovy/webui/component/explorer/popup/admin/UICategoriesAddedList.gtmpl",
    events = {
      @EventConfig(listeners = UISimpleCategoriesAddedList.DeleteActionListener.class, confirm="UICategoriesAddedList.msg.confirm-delete")
    }
)
public class UISimpleCategoriesAddedList extends UIContainer implements UISelectable {
  private UIPageIterator uiPageIterator_;
  private static final Log LOG  = ExoLogger.getLogger("explorer.UISimpleCategoriesAddedList");
  public UISimpleCategoriesAddedList() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "SimpleCategoriesAddedList");
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }
  
  public List getListCategories() throws Exception { return uiPageIterator_.getCurrentPageData(); }
  
  public void updateGrid(int currentPage) throws Exception {
    ObjectPageList objPageList = new ObjectPageList(getCategories(), 10);
    uiPageIterator_.setPageList(objPageList);
    if(currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(currentPage-1);
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }
  
  public List<Node> getCategories() throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class);
    CategoriesService categoriesService = getApplicationComponent(CategoriesService.class);
    return categoriesService.getCategories(uiJCRExplorer.getCurrentNode(), uiJCRExplorer.getRepositoryName());
  }
  
  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    CategoriesService categoriesService = getApplicationComponent(CategoriesService.class);
    try {
      Node currentNode = uiJCRExplorer.getCurrentNode();
      uiJCRExplorer.addLockToken(currentNode);
      categoriesService.addCategory(currentNode, value.toString(), uiJCRExplorer.getRepositoryName());
      uiJCRExplorer.getCurrentNode().save();
      uiJCRExplorer.getSession().save();
      updateGrid(1);
      setRenderSibling(UISimpleCategoriesAddedList.class);
    } catch(Exception e) {
      LOG.error("Unexpected error", e);
    }
  }
  
  static public class DeleteActionListener extends EventListener<UISimpleCategoriesAddedList> {
    public void execute(Event<UISimpleCategoriesAddedList> event) throws Exception {
      UISimpleCategoriesAddedList uiAddedList = event.getSource() ;
      UISimpleCategoryManager uiManager = uiAddedList.getParent() ;
      UIApplication uiApp = uiAddedList.getAncestorOfType(UIApplication.class) ;
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIJCRExplorer uiExplorer = uiAddedList.getAncestorOfType(UIJCRExplorer.class);
      CategoriesService categoriesService = uiAddedList.getApplicationComponent(CategoriesService.class);
      try {
        categoriesService.removeCategory(uiExplorer.getCurrentNode(), nodePath, uiExplorer.getRepositoryName());
        uiAddedList.updateGrid(uiAddedList.getUIPageIterator().getCurrentPage());
      } catch(AccessDeniedException ace) {
        throw new MessageException(new ApplicationMessage("UICategoriesAddedList.msg.access-denied",
            null, ApplicationMessage.WARNING)) ;
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e) ;
      }
      uiManager.setRenderedChild("UISimpleCategoriesAddedList");
    }
  }
}
