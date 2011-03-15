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
package org.exoplatform.ecm.webui.component.browsecontent;

import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Apr 12, 2007 9:24:36 AM
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIConfigContainer extends UIContainer {

  public void initNewConfig(String usercase, String repository, String workspace) throws Exception {
    PortletPreferences preference = getAncestorOfType(UIBrowseContentPortlet.class).getPortletPreferences();
    if (usercase.equals(Utils.CB_USE_FROM_PATH)) {
      UIPathConfig uiPathConfig = getChild(UIPathConfig.class);
      if (uiPathConfig == null) uiPathConfig = addChild(UIPathConfig.class, null, null);
      uiPathConfig.isEdit_ = true;
      uiPathConfig.initForm(preference, repository, workspace, true);
      setRenderedChild(UIPathConfig.class);
    } else if (usercase.equals(Utils.CB_USE_JCR_QUERY)) {
      UIQueryConfig uiQueryConfig = getChild(UIQueryConfig.class);
      if(uiQueryConfig == null) {
        uiQueryConfig = addChild(UIQueryConfig.class, null, null);
      }
      uiQueryConfig.isEdit_ = true;
      uiQueryConfig.initForm(preference, repository, workspace, true);
      setRenderedChild(UIQueryConfig.class);
    } else if (usercase.equals(Utils.CB_USE_SCRIPT)) {
      UIScriptConfig uiScriptConfig = getChild(UIScriptConfig.class);
      if (uiScriptConfig == null) {
        uiScriptConfig = addChild(UIScriptConfig.class, null, null);
      }
      uiScriptConfig.isEdit_ = true;
      uiScriptConfig.initForm(preference, repository, workspace, true);
      setRenderedChild(UIScriptConfig.class);
    } else if (usercase.equals(Utils.CB_USE_DOCUMENT)) {
      UIDocumentConfig uiDocumentConfig = getChild(UIDocumentConfig.class);
      if (uiDocumentConfig == null) {
        uiDocumentConfig = addChild(UIDocumentConfig.class, null, null);
      }
      uiDocumentConfig.isEdit_ = true;
      uiDocumentConfig.initForm(preference, repository, workspace, true);
      setRenderedChild(UIDocumentConfig.class);
    }
  }
}
;
