/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecms.personalfolder.component;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 9, 2012
 * 3:44:19 PM  
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIPersonalFolderManager extends UIAbstractManager{

  public UIPersonalFolderManager() throws Exception {
    addChild(UIPersonalFolderForm.class, null, null);
  }

  @Override
  public void refresh() throws Exception {
    // TODO Auto-generated method stub
    
  }

}
