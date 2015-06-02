/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wcm.webui.toolbar;

import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/Toolbar/UIAdminToolbarPortlet.gtmpl")
public class UIAdminToolbarPortlet extends UIPortletApplication
{
   // Minh Hoang TO
   // TODO: Add a ThreadLocal cache to avoid double invocation of editPermission
   // check ( one in processRender method, and one in Groovy template )

   public UIAdminToolbarPortlet() throws Exception
   {
   }

   public UserNavigation getSelectedNavigation() throws Exception {
     return Utils.getSelectedNavigation();
   }

   public boolean hasEditPermissionOnPortal() throws Exception {
     return Utils.hasEditPermissionOnPortal();
   }

   public boolean hasEditPermissionOnNavigation() throws Exception {
     return Utils.hasEditPermissionOnNavigation();
   }

   public boolean hasEditPermissionOnPage() throws Exception {
     return Utils.hasEditPermissionOnPage();
   }


   @Override
   public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception
   {
      // A user could view the toolbar portlet iff he/she has edit permission
      // either on
      // 'active' page, 'active' portal or 'active' navigation
      if (hasEditPermissionOnNavigation() || hasEditPermissionOnPage() || hasEditPermissionOnPortal())
      {
         super.processRender(app, context);
      }
   }


}
