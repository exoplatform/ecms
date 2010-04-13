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
package org.exoplatform.services.ecm.publication.plugins.staticdirect;

import org.exoplatform.services.ecm.publication.plugins.webui.UIVersionTreeList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jul 4, 2008 6:21:34 PM
 */
@ComponentConfig(
    template = "classpath:resources/templates/webui/UIVersionTreeList.gtmpl",
    events = {
        @EventConfig(listeners = UIVersionTreeList.SelectActionListener.class)
    }
)
public class UIStaticDirectVersionList extends UIVersionTreeList {

  public UIStaticDirectVersionList() throws Exception {
    super();
  }

  public void selectVersion(String versionPath) throws Exception {
    curentVersion_  = rootVersion_.findVersionNode(versionPath) ;
    isSelectedBaseVersion_ = false ;
    UIPublicationContainer uiPublicationContainer = getParent() ;
    UIPublicationForm uiForm = uiPublicationContainer.getChild(UIPublicationForm.class) ;
    uiForm.setVersionNode(curentVersion_);
    uiForm.updateForm(curentVersion_) ;
  }
}
