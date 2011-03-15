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

package org.exoplatform.services.cms.jcrext.lock;

import java.util.HashMap;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Feb 26, 2008
 */
@Deprecated
/*
 * use LockManagerListener
 */
public class LockTokenHolderService {

  private final HashMap<String, String> lockTokenHolder = new HashMap<String,String>();

  public void keepLockToken(Node node, String lockToken) throws Exception{
    lockTokenHolder.put(createKey(node),lockToken);
  }

  public String getLockToken(Node node) throws Exception{
    return lockTokenHolder.get(createKey(node));
  }

  private String createKey(Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    buffer.append(node.getSession().getRepository().hashCode())
    .append(node.getSession().getUserID())
    .append(node.getSession().getWorkspace().getName())
    .append("/").append(node.getPath());
    return buffer.toString();
  }
}
