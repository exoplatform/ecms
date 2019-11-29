/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.activity.listener;

import javax.jcr.Node;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileRemovePropertyActivityListener extends Listener<Node, String> {

  private String[]  removedField     = {"exo:title", "dc:title", "dc:description", "dc:creator", "dc:source"};
  private String[]  bundleMessage    = {"SocialIntegration.messages.removeName",
  																		 "SocialIntegration.messages.removeTitle",
                                       "SocialIntegration.messages.removeDescription",
                                       "SocialIntegration.messages.removeCreator",
                                       "SocialIntegration.messages.removeSource"};
  private boolean[] needUpdate       = {true, true, false, false, false};
  private int consideredFieldCount   = removedField.length;
  /**
   * Instantiates a new post edit content event listener.
   */
  public FileRemovePropertyActivityListener() {
	  
  }

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node currentNode = event.getSource();
    String propertyName = event.getData();
    
    if(currentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) currentNode = currentNode.getParent();
    
    String resourceBundle = "";
    for (int i=0; i< consideredFieldCount; i++) {
      if (propertyName.equals(removedField[i])) {
      	resourceBundle = bundleMessage[i];      	      	
      	Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, "", "");
        break;
      }
    }
    
  }
}
