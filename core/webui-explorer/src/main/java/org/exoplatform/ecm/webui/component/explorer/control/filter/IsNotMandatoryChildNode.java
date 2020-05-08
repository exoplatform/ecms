/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeDefinition;

import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tanhq@exoplatform.com
 * Oct 18, 2013  
 */
public class IsNotMandatoryChildNode extends UIExtensionAbstractFilter {
 
  public IsNotMandatoryChildNode() {
    this(null);
  }
  public IsNotMandatoryChildNode(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    Node parentNode = currentNode.getParent();
    String deletedNodeType;
    try {
      deletedNodeType = currentNode.getDefinition().getDeclaringNodeType().getName();
    } catch (Exception e) {
       return true;
    }
    NodeDefinition[] nodeDefinitions = parentNode.getPrimaryNodeType().getDeclaredChildNodeDefinitions();
    for (NodeDefinition nodeDefinition : nodeDefinitions) {
      String childNodeType = nodeDefinition.getDeclaringNodeType().getName();
      if(deletedNodeType.equals(childNodeType) && nodeDefinition.getName().equals(currentNode.getName()) && 
        nodeDefinition.isMandatory()) {
        return false;
      }
    }   
    return true;
  }

  public void onDeny(Map<String, Object> context) throws Exception {}
  
}
