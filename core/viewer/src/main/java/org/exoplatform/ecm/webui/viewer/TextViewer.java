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
package org.exoplatform.ecm.webui.viewer;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Aug 18, 2009
 * 3:49:41 AM
 */
@ComponentConfig(
    template = "classpath:resources/templates/TextViewer.gtmpl"
)
public class TextViewer extends UIComponent {
  private String sharedResourcesBundleNames[];
  private ResourceBundle sharedResourceBundle=null;

  public TextViewer() throws Exception {
  }

  public String getResource(String key) {
    try {
      Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      sharedResourcesBundleNames = resourceBundleService.getSharedResourceBundleNames();
      sharedResourceBundle = resourceBundleService.getResourceBundle(sharedResourcesBundleNames, locale);

      return sharedResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
