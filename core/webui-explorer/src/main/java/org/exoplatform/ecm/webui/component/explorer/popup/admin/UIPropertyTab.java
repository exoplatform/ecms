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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 13, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/popup/info/UIPropertyTab.gtmpl",
    events = {
        @EventConfig(listeners = UIPropertyTab.CloseActionListener.class),
        @EventConfig(listeners = UIPropertyTab.EditActionListener.class),
        @EventConfig(listeners = UIPropertyTab.DeleteActionListener.class, confirm="UIPropertyTab.confirm.remove-property")
    }
)

public class UIPropertyTab extends UIContainer {

  private static String[] PRO_BEAN_FIELD = {"icon", "name", "multiValue", "value", "action"} ;
  private final static String PRO_KEY_BINARYTYPE = "binary" ;
  private final static String PRO_KEY_CANNOTGET = "cannotget" ;
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIPropertyTab");
  private Set<String> propertiesName_ = new HashSet<String>();

  public String[] getBeanFields() { return PRO_BEAN_FIELD ;}

  public String[] getActions() {return  new String[] {"Close"} ;}

  private Node getCurrentNode() throws Exception {
    UIPropertiesManager uiManager = getParent();
    return uiManager.getCurrentNode();
  }

  public PropertyIterator getProperties() throws Exception {
    return getCurrentNode().getProperties() ;
  }

  private Set<String> propertiesName() throws Exception {
    if(propertiesName_.size() == 0) {
      Node currentNode = getCurrentNode();
      NodeType nodetype = currentNode.getPrimaryNodeType() ;
      Collection<NodeType> types = new ArrayList<NodeType>() ;
      types.add(nodetype) ;
      NodeType[] mixins = currentNode.getMixinNodeTypes() ;
      if (mixins != null) types.addAll(Arrays.asList(mixins)) ;
      for(NodeType nodeType : types) {
        for(PropertyDefinition property : nodeType.getPropertyDefinitions()) {
          propertiesName_.add(property.getName());
        }
      }
    }
    return propertiesName_;
  }

  public boolean addedByUser(String propertyName) throws Exception {
    if(propertiesName().contains(propertyName)) return false;
    return true;
  }

  public boolean isCanbeRemoved(String propertyName) throws Exception {
    Property property = getCurrentNode().getProperty(propertyName);
    if (property == null || !PermissionUtil.canSetProperty(property.getParent()) ||
        property.getDefinition().isMandatory() || property.getDefinition().isProtected())
      return false;
    return true;
  }

  public boolean isCanbeEdit(Property property) throws Exception {
    if(!PermissionUtil.canSetProperty(property.getParent()) ||
       property.getDefinition().isProtected()) {
      return false;
    }
    return true;
  }

  public String getPropertyValue(Property prop) throws Exception {
    if(prop.getType() == PropertyType.BINARY) return PRO_KEY_BINARYTYPE ;
    boolean flag = true;
    try {
      if(prop.getDefinition() != null && prop.getDefinition().isMultiple()) {
        Value[] values =  prop.getValues();
        StringBuilder sB = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
          if (prop.getType() == PropertyType.REFERENCE) {
            String uuid = values[i].getString();
            Node node = this.getNodeByUUID(uuid);
            if (node == null) {
              if (i == 0) flag = false;
              continue;
            }

          }
          if ((i > 0) && flag)
            sB.append("; ");
          sB.append(values[i].getString());
          flag = true;
        }
        return sB.toString();
      }
      return prop.getString() ;
    } catch(ValueFormatException ve) {
      return PRO_KEY_CANNOTGET ;
    } catch(Exception e) {
      return PRO_KEY_CANNOTGET ;
    }
  }

  public Node getNodeByUUID(String uuid) {
    Node node = null;
    try {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      Session session = uiExplorer.getSession();
      node = session.getNodeByUUID(uuid);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return node;

  }
  static public class CloseActionListener extends EventListener<UIPropertyTab> {
    public void execute(Event<UIPropertyTab> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
    }
  }

  static public class EditActionListener extends EventListener<UIPropertyTab> {
    public void execute(Event<UIPropertyTab> event) throws Exception {
      UIPropertyTab uiPropertyTab = event.getSource();
      UIPropertiesManager uiManager = uiPropertyTab.getParent();
      UIApplication uiApp = uiManager.getAncestorOfType(UIApplication.class);
      UIJCRExplorer uiExplorer = uiManager.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      if(!PermissionUtil.canSetProperty(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.access-denied", null,
            ApplicationMessage.WARNING));

        return;
      }
      if(uiExplorer.nodeIsLocked(currentNode)) {
        Object[] arg = { currentNode.getPath() };
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg));

        return;
      }
      if(!currentNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null));

        return;
      }
      String propertyName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIPropertyForm uiForm = uiManager.getChild(UIPropertyForm.class);
      if(uiForm == null) {
        uiForm = uiManager.addChild(UIPropertyForm.class, null, null);
        uiForm.init(currentNode);
      }
      uiForm.loadForm(propertyName);
      uiManager.setIsEditProperty(true);
      uiManager.setRenderedChild(UIPropertyForm.class);
      uiPropertyTab.setRendered(false);
    }
  }

  static public class DeleteActionListener extends EventListener<UIPropertyTab> {
    public void execute(Event<UIPropertyTab> event) throws Exception {
      UIPropertyTab uiPropertyTab = event.getSource();
      String propertyName = event.getRequestContext().getRequestParameter(OBJECTID);
      Node currentNode = uiPropertyTab.getCurrentNode();
      UIApplication uiApp = uiPropertyTab.getAncestorOfType(UIApplication.class);
      try {
        if(currentNode.hasProperty(propertyName)) currentNode.getProperty(propertyName).remove();
        currentNode.save();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPropertyTab.getParent());
        return;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.access-denied", null,
            ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        return;
      }
    }
  }
}
