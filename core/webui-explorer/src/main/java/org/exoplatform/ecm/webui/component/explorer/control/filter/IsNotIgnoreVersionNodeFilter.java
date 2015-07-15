/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * Mar 6, 2012
 */
public class IsNotIgnoreVersionNodeFilter implements UIExtensionFilter {

 public boolean accept(Map<String, Object> context) throws Exception {
   if (context == null) return true;
   boolean ignore_version = true;
   Node currentNode = (Node) context.get(Node.class.getName());
   Node parrentNode = currentNode.getParent();
   ExtendedNodeTypeManager ntmanager = WCMCoreUtils.getRepository().getNodeTypeManager();
   String nodetypes = System.getProperty("wcm.nodetypes.ignoreversion");
   if(nodetypes == null || nodetypes.length() == 0)
     nodetypes = "exo:webContent";
   String[] arrNodetypes = nodetypes.split(",");
   for (String nodetype: arrNodetypes) {
     try {
       ntmanager.getNodeType(nodetype);
     } catch (NoSuchNodeTypeException e) {
       ignore_version = true;
     }
     while (!((NodeImpl) parrentNode).isRoot()) {
       if (parrentNode.isNodeType(nodetype)) {
         return false;
       }
       try {
         parrentNode = parrentNode.getParent();
       }catch(Exception ex){
         return false;
       }
     }
   }

   return ignore_version;
 }

 public UIExtensionFilterType getType() {
   return UIExtensionFilterType.MANDATORY;
 }

 public void onDeny(Map<String, Object> context) throws Exception {
 }

}