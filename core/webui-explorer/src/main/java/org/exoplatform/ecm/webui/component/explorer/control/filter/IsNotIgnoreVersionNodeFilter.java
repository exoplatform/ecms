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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * Mar 6, 2012
 */
public class IsNotIgnoreVersionNodeFilter implements UIExtensionFilter {

  private static final Log LOG = ExoLogger.getLogger(IsNotIgnoreVersionNodeFilter.class);

  public static final String NODETYPES_IGNOREVERSION_PARAM = "wcm.nodetypes.ignoreversion";

  private List<NodeType> ignoredNodetypes = new ArrayList<>();

  public IsNotIgnoreVersionNodeFilter() {
    String nodetypes = System.getProperty(NODETYPES_IGNOREVERSION_PARAM);
    if(StringUtils.isBlank(nodetypes)) {
      nodetypes = "exo:webContent";
    }

    String[] arrNodetypes = nodetypes.split(",");

    for (String nodetype : arrNodetypes) {
      ExtendedNodeTypeManager ntmanager = WCMCoreUtils.getRepository().getNodeTypeManager();
      try {
        ignoredNodetypes.add(ntmanager.getNodeType(nodetype));
      } catch (NoSuchNodeTypeException e) {
        LOG.warn("Nodetype '{}' configured in wcm.nodetypes.ignoreversion does not exist");
      } catch (RepositoryException e) {
        LOG.error("Error while fetching nodetype '" + nodetype + "'", e);
      }
    }
  }

 public boolean accept(Map<String, Object> context) throws Exception {
   if (context == null) {
     return true;
   }

   Node currentNode = (Node) context.get(Node.class.getName());
   Node parentNode = currentNode;

   do {
     for(NodeType nodetype : ignoredNodetypes) {
       if (parentNode.isNodeType(nodetype.getName())) {
         return false;
       }
     }
     try {
       parentNode = parentNode.getParent();
     } catch(AccessDeniedException ex) {
       // if we cannot access the parent it means we are not in the content anymore
       return true;
     } catch(Exception ex) {
       LOG.error("Error while getting parent of node " + parentNode.getPath(), ex);
       return false;
     }
   } while (!((NodeImpl) parentNode).isRoot());

   return true;
 }

 public UIExtensionFilterType getType() {
   return UIExtensionFilterType.MANDATORY;
 }

 public void onDeny(Map<String, Object> context) throws Exception {
 }

}