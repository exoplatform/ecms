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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
  events = {
    @EventConfig(listeners = UIRelationsList.AddRelationActionListener.class),
    @EventConfig(listeners = UIRelationsList.CancelActionListener.class)
  }
)

public class UIRelationsList extends UIContainer {

  public UIRelationsList() throws Exception {

  }

  @SuppressWarnings("unused")
  static  public class AddRelationActionListener extends EventListener<UIRelationsList> {
    public void execute(Event<UIRelationsList> event) throws Exception {

    }
  }

  @SuppressWarnings("unused")
  static  public class CancelActionListener extends EventListener<UIRelationsList> {
    public void execute(Event<UIRelationsList> event) throws Exception {

    }
  }
}

