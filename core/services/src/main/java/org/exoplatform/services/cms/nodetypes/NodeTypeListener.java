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
package org.exoplatform.services.cms.nodetypes;

import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tanhq@exoplatform.com
 * Oct 30, 2013  
 */
public class NodeTypeListener implements NodeTypeManagerListener{

  @Override
  public void nodeTypeRegistered(InternalQName ntName) {
    Utils.updateExcludedNodeTypes();
    
  }

  @Override
  public void nodeTypeReRegistered(InternalQName ntName) {
    Utils.updateExcludedNodeTypes();
  }

  @Override
  public void nodeTypeUnregistered(InternalQName ntName) {
    Utils.updateExcludedNodeTypes();
  }

}
