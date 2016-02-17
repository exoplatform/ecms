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
package org.exoplatform.services.wcm.portal.listener;

import org.exoplatform.portal.config.DataStorageImpl;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.javascript.XJavascriptService;
import org.exoplatform.services.wcm.skin.XSkinService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The listener interface for receiving updateLivePortalEvent events.
 * The class that is interested in processing a updateLivePortalEvent
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addUpdateLivePortalEventListener</code> method. When
 * the updateLivePortalEvent event occurs, that object's appropriate
 * method is invoked.
 *
 * @see UpdateLivePortalEventListener
 */
public class UpdateLivePortalEventListener extends Listener<DataStorageImpl,PortalConfig>{

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<DataStorageImpl, PortalConfig> event) throws Exception {

    XJavascriptService jsService = WCMCoreUtils.getService(XJavascriptService.class);
    XSkinService xSkinService = WCMCoreUtils.getService(XSkinService.class);
    xSkinService.start();
    jsService.start();

  }

}
