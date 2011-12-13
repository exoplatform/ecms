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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 17, 2006
 * 10:07:15 AM
 */
@ComponentConfig(
    template = "system:/groovy/webui/core/UITabPane.gtmpl",
    events = {
        @EventConfig(listeners = UIPropertiesManager.ChangeTabActionListener.class)
    }
)

public class UIPropertiesManager extends UIContainer implements UIPopupComponent {

  private String selectedPath_ = null;
  private String wsName_ = null;
  private boolean isEditProperty = false;
  private List<PropertyDefinition> properties = null;

  public UIPropertiesManager() throws Exception {
    addChild(UIPropertyTab.class, null, null)  ;
    addChild(UIPropertyForm.class, null, null).setRendered(false) ;
  }
  
  public void processRender(WebuiRequestContext context) throws Exception {
    Node currentNode = getCurrentNode();
    properties = org.exoplatform.services.cms.impl.Utils.getProperties(currentNode);
    
    if (!isEditProperty && currentNode != null && !currentNode.isNodeType(Utils.NT_UNSTRUCTURED)
        && (properties == null || properties.size() == 0)) {
      removeChild(UIPropertyForm.class);
    }
    super.processRender(context);
  }
  
  public Node getCurrentNode() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    if(uiExplorer != null) {
      if(selectedPath_ != null) {
        return uiExplorer.getNodeByPath(selectedPath_, uiExplorer.getSessionByWorkspace(wsName_));
      }
      return uiExplorer.getCurrentNode();
    } else return null;
  }

  public void setSelectedPath(String selectedPath, String wsName) {
    selectedPath_ = selectedPath;
    wsName_ = wsName;
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {}
  public void setLockForm(boolean isLockForm) {
    getChild(UIPropertyForm.class).lockForm(isLockForm) ;
  }

  @SuppressWarnings("unused")
  static public class ChangeTabActionListener extends EventListener<UIPropertiesManager> {
    public void execute(Event<UIPropertiesManager> event) throws Exception {
    }
  }
  
  public void setIsEditProperty(boolean isEdit) { this.isEditProperty = isEdit; }
}
