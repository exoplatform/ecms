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
package org.exoplatform.services.cms.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 27, 2009
 * 9:15:20 AM
 */
public class DMSRepositoryConfiguration extends BaseComponentPlugin {

  private String systemWorkspaceName_;

  public DMSRepositoryConfiguration() {
  }

  public DMSRepositoryConfiguration(InitParams params) throws Exception {
    systemWorkspaceName_ = params.getValueParam("systemWorkspace").getValue();
  }

  public void setSystemWorkspace(String systemWorkspace) {
    systemWorkspaceName_ = systemWorkspace;
  }

  public String getSystemWorkspace() {
    return systemWorkspaceName_;
  }

}
