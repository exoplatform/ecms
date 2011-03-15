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

import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 2, 2009
 * 3:09:32 AM
 */
/**
 * Store all informations of node's modification
 *
 */
public class ModifyNodeAction implements Action {

  public boolean execute(Context context) throws Exception {
    Object item = context.get("currentItem");
    Node node = (item instanceof Property) ?
                ((Property)item).getParent() :
                (Node)item;
    if(node.isNodeType("nt:resource")) node = node.getParent();
    ConversationState conversationState = ConversationState.getCurrent();
    String userName = (conversationState == null) ? node.getSession().getUserID() :
                                                    conversationState.getIdentity().getUserId();
    if(node.canAddMixin("exo:modify")) {
      node.addMixin("exo:modify");
    }
    node.setProperty("exo:lastModifiedDate", new GregorianCalendar());
    node.setProperty("exo:lastModifier",userName);
    return false;
  }

}
