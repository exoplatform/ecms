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

package org.exoplatform.services.wcm.core.impl;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Mar 11, 2008
 */
public class WebSchemaCreationAction implements Action {
  private static final Log log = ExoLogger.getLogger("wcm:WebSchemaCreationAction");

  public boolean execute(Context context) throws Exception {
    Node node = (Node)context.get("currentItem");
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WebSchemaConfigService schemaConfigService =
      (WebSchemaConfigService) container.getComponentInstanceOfType(WebSchemaConfigService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      schemaConfigService.createSchema(sessionProvider, node);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when creat web schema for node"+node.getPath() , e);
      }
    }
    return false;
  }

}
