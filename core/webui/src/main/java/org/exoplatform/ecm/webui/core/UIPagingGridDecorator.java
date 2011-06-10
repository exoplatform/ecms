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

import org.exoplatform.webui.core.UIComponentDecorator;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * 31-05-2011  
 */
public abstract class UIPagingGridDecorator extends UIComponentDecorator {

  /** The page iterator */
  protected UIECMPageIterator uiIterator_;
  
  public UIPagingGridDecorator() throws Exception
  {
     uiIterator_ = createUIComponent(UIECMPageIterator.class, null, null);
     setUIComponent(uiIterator_);
  }
  
  public UIECMPageIterator getUIPageIterator()
  {
     return uiIterator_;
  }
  
  public abstract void refresh(int currentPage) throws Exception;
}
