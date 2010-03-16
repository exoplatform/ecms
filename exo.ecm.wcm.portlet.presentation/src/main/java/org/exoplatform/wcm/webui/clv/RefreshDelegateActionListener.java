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
package org.exoplatform.wcm.webui.clv;

import org.exoplatform.webui.event.Event;

/*
 * Created by The eXo Platform SAS
 * Author : Anh Do Ngoc
 *          anh.do@exoplatform.com
 * Oct 22, 2008  
 */
/**
 * The listener interface for receiving refreshDelegateAction events.
 * The class that is interested in processing a refreshDelegateAction
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addRefreshDelegateActionListener<code> method. When
 * the refreshDelegateAction event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see RefreshDelegateActionEvent
 */
public interface RefreshDelegateActionListener {  
  
  /**
   * On refresh.
   * 
   * @param event the event
   * 
   * @throws Exception the exception
   */
  public void onRefresh(Event<UICLVPresentation> event) throws Exception;
}
