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
package org.exoplatform.ecm.webui.component.admin;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 19, 2006
 * 8:26:51 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/UIECMAdminControlPanel.gtmpl"
)
public class UIECMAdminControlPanel extends UIContainer {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIECMAdminControlPanel.class);

  public static final String EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIECMAdminControlPanel";

  private List<UIAbstractManagerComponent> managers = new ArrayList<UIAbstractManagerComponent>();

  public UIECMAdminControlPanel() throws Exception {}
  public List<?> getEvents() { return getComponentConfig().getEvents() ; }

  void initialize() throws Exception {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(EXTENSION_TYPE);
    if (extensions == null) {
      return;
    }
    for (UIExtension extension : extensions) {
      UIComponent component = manager.addUIExtension(extension, null, this);
      if (component instanceof UIAbstractManagerComponent) {
        // You can access to the given extension and the extension is valid
        UIAbstractManagerComponent uiAbstractManagerComponent = (UIAbstractManagerComponent) component;
        uiAbstractManagerComponent.setUIExtensionName(extension.getName());
        uiAbstractManagerComponent.setUIExtensionCategory(extension.getCategory());
        managers.add(uiAbstractManagerComponent);
      } else if (component != null) {
        // You can access to the given extension but the extension is not valid
        if (LOG.isWarnEnabled()) {
          LOG.warn("All the extension '" + extension.getName() + "' of type '" + EXTENSION_TYPE
            + "' must be associated to a component of type " + UIAbstractManagerComponent.class);
        }
        removeChild(component.getClass());
      }
    }
  }

  List<UIAbstractManagerComponent> getManagers() {
    return managers;
  }

  void unregister(UIAbstractManagerComponent component) {
    managers.remove(component);
  }

  /**
   * Check whether a specified category is the same to category of current render manager item.
   *
   * @param currentCategory Current Category
   * @param categories List of categories of side blocks
   * @param managersGroup Contain managers groups
   * @return true: the same, false: not the same
   */
  public boolean isSameCategoryWithCurrentRenderedManager (String currentCategory,
                                                           ArrayList<String> categories,
                                                           ArrayList<ArrayList<UIAbstractManagerComponent>> managersGroup) {
    int i = 0;
    UIECMAdminWorkingArea workingArea = this.getAncestorOfType(UIECMAdminPortlet.class).getChild(UIECMAdminWorkingArea.class);
    String currentRenderId = workingArea.getRenderedCompId();
    for(String category : categories) {
      if (category.equals(currentCategory)) {
        ArrayList<UIAbstractManagerComponent> groups = managersGroup.get(i);
        for(UIAbstractManagerComponent group : groups) {
          String extensionName =   group.getUIExtensionName();
          if (extensionName.equals(currentRenderId))
            return true;
        }
      }
      i++;
    }
    return false;
  }
}
