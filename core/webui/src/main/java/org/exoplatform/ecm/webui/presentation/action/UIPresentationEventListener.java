/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.presentation.action;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Sep 17, 2009
 */
public abstract class UIPresentationEventListener<T extends UIComponent> extends EventListener<T> {

  /**
   * Prepare variable to execute action
   * @param event
   * @return
   */
  protected Map<String, Object> createVariables(Event<T> event) {
    Map<String, Object> variables = new HashMap<String, Object>();
    String repository = event.getRequestContext().getRequestParameter(Utils.REPOSITORY);
    String wsname = event.getRequestContext().getRequestParameter(Utils.WORKSPACE_PARAM);
    String nodepath = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID);
    variables.put(Utils.REPOSITORY, repository);
    variables.put(Utils.WORKSPACE_PARAM, wsname);
    variables.put(UIComponent.OBJECTID, nodepath);
    variables.put(UIComponent.UICOMPONENT, event.getSource());
    variables.put(Utils.REQUESTCONTEXT, event.getRequestContext());
    return variables;
  }

  /**
   * {@inheritDoc}
   */
  public void execute(Event<T> event) throws Exception {
    executeAction(createVariables(event));
  }

  /**
   * Run action for each event
   * @param variables
   * @throws Exception
   */
  protected abstract void executeAction(Map<String, Object> variables) throws Exception;

}
