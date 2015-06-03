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
package org.exoplatform.services.wcm.template;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 27, 2009
 */
public class TemplateModificationAction implements Action {
    private static final Log LOG  = ExoLogger.getLogger(TemplateModificationAction.class.getName());

  public boolean execute(Context context) throws Exception {
    Property property = (Property)context.get("currentItem");
    Node node = property.getParent();
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      try {
          templateService.reloadTemplate(node.getParent().getPath());
      } catch (IllegalArgumentException IAE) {
          if (LOG.isWarnEnabled()) {
              LOG.warn("Template [" + node.getParent().getPath() + "] not found on TemplateService Cache Store");
          }
      }
        
    return true;
  }

}
