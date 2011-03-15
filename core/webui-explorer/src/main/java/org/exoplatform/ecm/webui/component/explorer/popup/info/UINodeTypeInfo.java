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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 * Editor : phan tuan Oct 27, 2006
 */

@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/popup/info/UINodeTypeInfo.gtmpl",
    events = {@EventConfig(listeners = UINodeTypeInfo.CloseActionListener.class)}
)

public class UINodeTypeInfo extends UIContainer implements UIPopupComponent {
  private Collection nodeTypes ;

  public UINodeTypeInfo() throws Exception {}

  public void activate() throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node node = uiJCRExplorer.getCurrentNode() ;
    try {
      NodeType nodetype = node.getPrimaryNodeType() ;
      Collection<NodeType> types = new ArrayList<NodeType>() ;
      types.add(nodetype) ;
      NodeType[] mixins = node.getMixinNodeTypes() ;
      if (mixins != null) {
        List<NodeType> list = Arrays.asList(mixins) ;
        types.addAll(list) ;
      }
      nodeTypes = types ;
    } catch (Exception e) {
      UIApplication uiApp = uiJCRExplorer.getAncestorOfType(UIApplication.class) ;
      JCRExceptionManager.process(uiApp, e);
    }
  }

  public String getPropertyValue(Value value) throws Exception{
    switch(value.getType()) {
      case PropertyType.BINARY: return Integer.toString(PropertyType.BINARY) ;
      case PropertyType.BOOLEAN :return Boolean.toString(value.getBoolean()) ;
      case PropertyType.DATE : return value.getDate().getTime().toString() ;
      case PropertyType.DOUBLE : return Double.toString(value.getDouble()) ;
      case PropertyType.LONG : return Long.toString(value.getLong()) ;
      case PropertyType.NAME : return value.getString() ;
      case PropertyType.STRING : return value.getString() ;
    }
    return null ;
  }

  public void deActivate() throws Exception {}

  public String[] getActions() {return new String[] {"Close"} ;}

  public String resolveType(int type) {
    return ExtendedPropertyType.nameFromValue(type) ;
  }

  public String resolveOnParentVersion(int opv) {
    return OnParentVersionAction.nameFromValue(opv) ;
  }

  public String getDefaultValue(PropertyDefinition proDef) throws Exception {
    StringBuilder defaultValue = new StringBuilder() ;
    Value[] values = proDef.getDefaultValues() ;
    if(values == null || values.length < 0) return "" ;
    for(Value value : values) {
      if(value == null) continue ;
      if(defaultValue.length() > 0) defaultValue.append(",") ;
      defaultValue.append(getPropertyValue(value)) ;
    }
    return defaultValue.toString() ;
  }

  public Collection getNodeTypes() { return nodeTypes ;}

  static  public class CloseActionListener extends EventListener<UINodeTypeInfo> {
    public void execute(Event<UINodeTypeInfo> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }
}

