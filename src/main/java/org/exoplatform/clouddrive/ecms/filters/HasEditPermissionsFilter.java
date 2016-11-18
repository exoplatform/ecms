/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.filters;

import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import java.util.Map;

import javax.jcr.Node;

/**
 * Filter nodes where current user can set properties and change permissions.
 */
public class HasEditPermissionsFilter implements UIExtensionFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(HasEditPermissionsFilter.class);

  /**
   * {@inheritDoc}
   */
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) {
      return true;
    }

    Node contextNode = (Node) context.get(Node.class.getName());
    if (contextNode == null) {
      return false;
    }

    // only accept if current have edit permissions
    return PermissionUtil.canSetProperty(contextNode) && PermissionUtil.canChangePermission(contextNode);
  }

  /**
   * {@inheritDoc}
   */
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  /**
   * {@inheritDoc}
   */
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
