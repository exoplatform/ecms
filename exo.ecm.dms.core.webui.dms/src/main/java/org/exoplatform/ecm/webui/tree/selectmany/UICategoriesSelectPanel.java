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
package org.exoplatform.ecm.webui.tree.selectmany;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 *          dzungdev@gmail.com
 * Aug 11, 2008  
 */

@ComponentConfig(
    template =  "classpath:groovy/ecm/webui/tree/selectmany/UISelectPathPanel.gtmpl",
    events = {
        @EventConfig(listeners = UICategoriesSelectPanel.SelectActionListener.class)
    }
)

public class UICategoriesSelectPanel extends UIContainer{
  private Node parentNode;
  private UIPageIterator uiPageIterator_;

  public void setParentNode(Node node) { this.parentNode = node; }

  public UICategoriesSelectPanel() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "UICategoriesSelect");
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }
  
  public List getSelectableNodes() throws Exception { return uiPageIterator_.getCurrentPageData(); }
  
  public void updateGrid() throws Exception {
    ObjectPageList objPageList = new ObjectPageList(getListSelectableNodes(), 4);
    uiPageIterator_.setPageList(objPageList);
  }
  
  public List<Node> getListSelectableNodes() throws Exception {
    List<Node> list = new ArrayList<Node>();
    if(parentNode == null) return list;
    for(NodeIterator iterator = parentNode.getNodes();iterator.hasNext();) {
      Node child = iterator.nextNode();
      if(child.isNodeType("exo:hiddenable")) continue;
      list.add(child);
    }
    return list;
  }

  static public class SelectActionListener extends EventListener<UICategoriesSelectPanel> {
    public void execute(Event<UICategoriesSelectPanel> event) throws Exception {
      UICategoriesSelectPanel uiDefault = event.getSource() ;
      UICategoriesSelector uiCategoriesSelector = uiDefault.getParent();
      UISelectedCategoriesGrid uiSelectedCategoriesGrid = uiCategoriesSelector.getChild(UISelectedCategoriesGrid.class);
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      if(!uiSelectedCategoriesGrid.getSelectedCategories().contains(value)) {
        uiSelectedCategoriesGrid.addCategory(value);
      }
      uiSelectedCategoriesGrid.updateGrid(uiSelectedCategoriesGrid.getUIPageIterator().getCurrentPage());
      uiSelectedCategoriesGrid.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCategoriesSelector) ;
    }
  }

  public Node getParentNode() {
    return parentNode;
  }
}
