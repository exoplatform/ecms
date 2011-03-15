/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.optionblocks;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * Created by The eXo Platform SAS
 * Author : Khuong.Van.Dung
 *          dung.khuong@exoplatform.com
 * Jul 22, 2010
 */

@ComponentConfig(
    template  = "app:/groovy/webui/component/explorer/optionblocks/UIOptionBlockPanel.gtmpl"
)
public class UIOptionBlockPanel extends UIContainer {

  private String OPTION_BLOCK_EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIOptionBlocks";
  private List<UIComponent> listExtenstions = new ArrayList<UIComponent>();

  public UIOptionBlockPanel() throws Exception {

  }
  /*
   * This method checks and returns true if there is at least one extension, otherwise it returns false
   * */
  public boolean isHasOptionBlockExtension() {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
     List<UIExtension> extensions = manager.getUIExtensions(OPTION_BLOCK_EXTENSION_TYPE);
     if(extensions != null) {
       return true;
     }
    return false;
  }
  public void addOptionBlockExtension() throws Exception {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(OPTION_BLOCK_EXTENSION_TYPE);

    if(extensions != null ) {
      for (UIExtension extension : extensions) {

        UIComponent uicomp = manager.addUIExtension(extension, null, this);
        //uicomp.setRendered(false);
       listExtenstions.add(uicomp);
      }
    }
  }
  public void setDisplayOptionExtensions(boolean display) {
    for(UIComponent uicomp : listExtenstions) {
      uicomp.setRendered(display);
    }
  }
}
