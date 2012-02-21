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
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.pham@exoplatform.com
 * Sep 17, 2008
 */
public class WebSchemaModificationAction implements Action{
  private Log log = ExoLogger.getLogger("wcm:WebSchemaModificationAction");
  public boolean execute(Context context) throws Exception {
    Property property = (Property)context.get("currentItem");
    String propertyName = property.getName();    
    
    if (!propertyName.equals("jcr:data") 
        && !propertyName.equals(NodetypeConstant.EXO_PRIORITY)
        && !propertyName.equals(NodetypeConstant.EXO_ACTIVE)
        && !propertyName.equals("exo:restorePath")) {
      
      // use exo:active in case of exo:cssFile or exo:jsFile
      return propertyName.equalsIgnoreCase("exo:active");
    }
    Node grandParent = property.getParent().getParent();
    if(propertyName.equals("jcr:data") && !grandParent.getPrimaryNodeType().getName().equals("nt:file"))
      return false;    
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WebSchemaConfigService schemaConfigService =
      (WebSchemaConfigService) container.getComponentInstanceOfType(WebSchemaConfigService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    
    Node node = null;
    if (propertyName.equals("jcr:data")) {
      node = grandParent;
    } else {
      node = property.getParent();
    }
        
    try {      
      if (propertyName.equals("exo:restorePath")) {
        schemaConfigService.updateSchemaOnRemove(sessionProvider, node);
      } else {
        schemaConfigService.updateSchemaOnModify(sessionProvider, node);
      }      
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when update schema when modify node: "+node.getPath(), e);
      }
    }
    return true;
  }

}
