/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.cms.listeners;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 1/15/15
 * Listen PUT Command when dav client update content
 *
 * Cause: Currently when dav client save/close a file maybe change node to checkin stage.
 * We will change stage to checkout only for NT file node.
 */

public class WebDavPutCommandListener extends Listener<Object, Node> {
  @Override
  public void onEvent(Event<Object, Node> objectNodeEvent) throws Exception {
    Node currentNode = objectNodeEvent.getData();
    if(NodetypeConstant.NT_FILE.equals(currentNode.getPrimaryNodeType().getName())
            && !currentNode.isCheckedOut()){

      currentNode.checkout();
      currentNode.save();
    }
  }
}
