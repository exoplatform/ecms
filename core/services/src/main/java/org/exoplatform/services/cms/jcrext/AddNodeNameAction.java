/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.cms.jcrext;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          vu.nguyen@exoplatform.com
 * Aug 17, 2010
 * 3:09:32 PM
 */
/**
 * Store node name
 *
 */
public class AddNodeNameAction implements Action {

 public boolean execute(Context context) throws Exception {
   Object item = context.get("currentItem");
   Node node = (item instanceof Property) ? ((Property)item).getParent() :
                                            (Node)item;
   if(node.isNodeType("nt:resource")) node = node.getParent();

   if(node.canAddMixin("exo:sortable")) {
     node.addMixin("exo:sortable");
   }

   if (!node.hasProperty("exo:name")) {
       node.setProperty("exo:name", node.getName());
   }

   return false;
 }

}
