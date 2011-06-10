/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.core;

import java.util.Arrays;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * 19-05-2011  
 */
/**
 * A component that allows pagination, with an iterator to change pages
 *
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/core/UIPageIterator.gtmpl", events = {
    @EventConfig(listeners = UIECMPageIterator.ShowPageActionListener.class),
    @EventConfig(listeners = UIECMPageIterator.ChangeMaxSizePageActionListener.class) })
@Serialized
public class UIECMPageIterator extends UIPageIterator {

  /**
   * The list of pages
   */
  private int               itemsPerPage_      = 0;
  
  private int               totalItems         = 0;

  private boolean           useMaxSizeSetting_ = false;

  public static final int[] MAX_ITEMS_PER_PAGE = new int[] { 5, 10, 15, 20, 30, 60, 100 };
 

  public UIECMPageIterator() {
  }

  public int[] getMaxItemPerPageList() {
    int pageSize = this.getItemsPerPage();
    if (isPageSizeInList(pageSize)) {
      return MAX_ITEMS_PER_PAGE;
    } else {
      int length = MAX_ITEMS_PER_PAGE.length + 1;
      int[] pageSizeList = new int[length];
      System.arraycopy(MAX_ITEMS_PER_PAGE, 0, pageSizeList, 0, MAX_ITEMS_PER_PAGE.length);
      pageSizeList[pageSizeList.length - 1] = pageSize;
      Arrays.sort(pageSizeList);
      return pageSizeList;
    }
  }
  
  public int getItemsPerPage() {
    if (itemsPerPage_ <= 0) {
      itemsPerPage_ = 10;
    }
    return itemsPerPage_;
  }
  
  public void setItemsPerPage(int itemsPerPage) {
    this.itemsPerPage_ = itemsPerPage;
  }

  public int getTotalItems() {
    return totalItems;
  }

  public void setTotalItems(int totalItems) {
    this.totalItems = totalItems;
  }

  /**
   * @param useMaxSizeSetting_ the useMaxSizeSetting_ to set
   */
  public void setUseMaxSizeSetting(boolean useMaxSizeSetting_) {
    this.useMaxSizeSetting_ = useMaxSizeSetting_;
  }

  /**
   * @return the useMaxSizeSetting_
   */
  public boolean isUseMaxSizeSetting() {
    return useMaxSizeSetting_;
  }
  
  public void setPageList(PageList pageList)
  {
     super.setPageList(pageList);
     this.itemsPerPage_ = pageList.getPageSize();
  }
  
  private boolean isPageSizeInList(int pageSize) {
    for (int size : MAX_ITEMS_PER_PAGE) {
      if (size == pageSize) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unused")
  static public class ShowPageActionListener extends EventListener<UIECMPageIterator> {
    public void execute(Event<UIECMPageIterator> event) throws Exception {
      UIECMPageIterator uiPageIterator = event.getSource();
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiPageIterator.setCurrentPage(page);
      UIComponent parent = uiPageIterator.getParent();
      if (parent == null)
        return;
      event.getRequestContext().addUIComponentToUpdateByAjax(parent);
      parent.broadcast(event, event.getExecutionPhase());
    }
  }
  
  static public class ChangeMaxSizePageActionListener extends EventListener<UIECMPageIterator>{

    public void execute(Event<UIECMPageIterator> event) throws Exception {
      UIECMPageIterator uiPageIterator = event.getSource();
      int itemsPerPage = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiPageIterator.setItemsPerPage(itemsPerPage);
      UIComponent parent = uiPageIterator.getParent();
      if (parent == null)
        return;
      if (parent instanceof UIPagingGrid ) {
        ((UIPagingGrid)parent).refresh(uiPageIterator.getCurrentPage());
      } else if (parent instanceof UIPagingGridDecorator) {
        ((UIPagingGridDecorator)parent).refresh(uiPageIterator.getCurrentPage());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(parent);
    }

  }

}
