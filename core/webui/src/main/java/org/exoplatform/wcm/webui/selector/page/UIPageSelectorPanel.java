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
package org.exoplatform.wcm.webui.selector.page;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 30, 2009
 */
@ComponentConfig(
    template = "classpath:groovy/wcm/webui/selector/page/UIPageSelectorPanel.gtmpl",
    events = {
        @EventConfig(listeners = UIPageSelectorPanel.SelectActionListener.class)
    }
)
public class UIPageSelectorPanel extends UIContainer {

  /** The Constant PAGE_SELECTOR_ITERATOR. */
  private static final String PAGE_SELECTOR_ITERATOR = "UIPageSelectorIterator";

  /** The page iterator. */
  private UIPageIterator pageIterator;

  /** The selected page. */
  private UserNode selectedNode;

  /**
   * Instantiates a new uI page selector panel.
   *
   * @throws Exception the exception
   */
  public UIPageSelectorPanel() throws Exception {
    pageIterator = addChild(UIPageIterator.class, null, PAGE_SELECTOR_ITERATOR);
  }

  /**
   * Update grid.
   */
  public void updateGrid() {
    List<UserNode> children = null;
    if (selectedNode == null) {
      UIPageSelector pageSelector = getAncestorOfType(UIPageSelector.class);
      UIPageNodeSelector pageNodeSelector = pageSelector.getChild(UIPageNodeSelector.class);
      UserNode rootNode = pageNodeSelector.getRootNodeOfSelectedNav();
      children = new ArrayList<UserNode>(rootNode.getChildren());
    } else {
      children = new ArrayList<UserNode>(selectedNode.getChildren());
    }
    ListAccess<UserNode> pageNodeList = new ListAccessImpl<UserNode>(UserNode.class, children);
    LazyPageList<UserNode> pageList = new LazyPageList<UserNode>(pageNodeList, 10);
    pageIterator.setPageList(pageList);
  }

  /**
   * Gets the selectable pages.
   *
   * @return the selectable pages
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public List getSelectablePages() throws Exception {
    return pageIterator.getCurrentPageData();
  }

  /**
   * Gets the selected node.
   *
   * @return the selected node
   */
  public UserNode getSelectedNode() {
    return selectedNode;
  }

  /**
   * Sets the selected node.
   *
   * @param selectedNode the new selected node
   */
  public void setSelectedNode(UserNode selectedNode) {
    this.selectedNode = selectedNode;
  }

  /**
   * Gets the page iterator.
   *
   * @return the page iterator
   */
  public UIPageIterator getPageIterator() {
    return pageIterator;
  }

  /**
   * Sets the page iterator.
   *
   * @param pageIterator the new page iterator
   */
  public void setPageIterator(UIPageIterator pageIterator) {
    this.pageIterator = pageIterator;
  }

  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener</code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SelectActionListener extends EventListener<UIPageSelectorPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPageSelectorPanel> event) throws Exception {
      UIPageSelectorPanel pageSelectorPanel = event.getSource();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIPageSelector pageSelector = pageSelectorPanel.getAncestorOfType(UIPageSelector.class);
      ((UISelectable)pageSelector.getSourceComponent()).doSelect(pageSelector.getReturnFieldName(), uri);
    }
  }
}
