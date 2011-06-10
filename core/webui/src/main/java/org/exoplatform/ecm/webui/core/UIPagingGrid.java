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

import java.util.List;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIGrid;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * 30-05-2011  
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/core/UIGrid.gtmpl")
public abstract class UIPagingGrid extends UIGrid {
  /** The page iterator */
  protected UIECMPageIterator uiIterator_;

  public UIPagingGrid() throws Exception
  {
     uiIterator_ = createUIComponent(UIECMPageIterator.class, null, null);
     uiIterator_.setParent(this);
  }
  
  public abstract void refresh(int currentPage) throws Exception;
  
  public UIECMPageIterator getUIPageIterator()
  {
     return uiIterator_;
  }


  public List<?> getBeans() throws Exception
  {
     return uiIterator_.getCurrentPageData();
  }

  @SuppressWarnings("unchecked")
  public UIComponent findComponentById(String lookupId)
  {
     if (uiIterator_.getId().equals(lookupId))
     {
        return uiIterator_;
     }
     return super.findComponentById(lookupId);
  }
}
