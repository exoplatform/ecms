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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 16, 2007
 * 11:23:26 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")
public class UIMultiLanguageManager extends UIContainer implements UIPopupComponent {

  public UIMultiLanguageManager() throws Exception {
    addChild(UIMultiLanguageForm.class, null, null) ;
    addChild(UIAddLanguageContainer.class, null, null).setRendered(false) ;
  }

  public void activate() throws Exception {
    UIMultiLanguageForm uiForm = getChild(UIMultiLanguageForm.class) ;
    uiForm.doSelect(getAncestorOfType(UIJCRExplorer.class).getCurrentNode()) ;
  }
  public void deActivate() throws Exception {}

  public List<SelectItemOption<String>> languages() throws Exception {
    
    // Get default locale
    Locale defaultLocale = Locale.getDefault();
    
    // set default locale to current user selected language
    Locale.setDefault(Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale());
    
    
    LocaleConfigService localService = getApplicationComponent(LocaleConfigService.class) ;
    List<SelectItemOption<String>> languages = new ArrayList<SelectItemOption<String>>() ;
    Iterator iter = localService.getLocalConfigs().iterator() ;
    while (iter.hasNext()) {
      LocaleConfig localConfig = (LocaleConfig)iter.next() ;
      languages.add(new SelectItemOption<String>(localConfig.getLocale().getDisplayLanguage(),
                                                 localConfig.getLocale().getLanguage())) ;
    }
    
    // Set back to the default locale
    Locale.setDefault(defaultLocale);    
    return languages ;
  }
}
