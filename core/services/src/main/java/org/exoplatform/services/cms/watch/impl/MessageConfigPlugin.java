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
package org.exoplatform.services.cms.watch.impl;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          hadv@exoplatform.com
 * Jan 4, 2012  
 */
public class MessageConfigPlugin implements ComponentPlugin {
  private String name;

  private String desciption;
  
  private MessageConfig messageConfig;
  
  final static private String MESSAGE_CONFIG_PARAM = "messageConfig";
  
  /**
   * Constructor of <code>MessageConfigPlugin</code> class. Get the <code>MessageConfig</code> in
   * messageConfig init param of <code>WatchDocumentService</code> from XML configuration
   * file
   * 
   * @param initParams The initialize parameters
   */
  public MessageConfigPlugin(InitParams initParams) {
    ObjectParameter objectParam = initParams.getObjectParam(MESSAGE_CONFIG_PARAM);
    if(objectParam != null) {
      messageConfig = (MessageConfig)objectParam.getObject();
    }    
  }  
  
  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return this.desciption;
  }

  @Override
  public void setDescription(String description) {
    this.desciption = description;
  }

  /**
   * @return the messageConfig
   */
  public MessageConfig getMessageConfig() {
    return messageConfig;
  }

}
