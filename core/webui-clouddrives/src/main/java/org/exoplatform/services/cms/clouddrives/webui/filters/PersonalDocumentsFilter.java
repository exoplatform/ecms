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
package org.exoplatform.services.cms.clouddrives.webui.filters;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.clouddrives.webui.CloudDriveContext;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Filter for personal drives.
 */
public class PersonalDocumentsFilter implements UIExtensionFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(PersonalDocumentsFilter.class);

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
    String contextPath = contextNode.getPath();

    // only show in Personal Doc's root!
    String userId = Util.getPortalRequestContext().getRemoteUser();
    UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
    String personalDocsPath = Utils.getPersonalDrivePath(uiExplorer.getDriveData().getHomePath(), userId);
    if (contextPath.startsWith(personalDocsPath)) {
      boolean isRoot = contextNode.getPath().equals(personalDocsPath);
      // additionally we initialize all already connected drives in the context,
      // they can be used for drive folder icons rendering or other similar
      // purpose
      if (isRoot) {
        CloudDriveContext.initConnected(WebuiRequestContext.getCurrentInstance(), contextNode);
      } else {
        Item personalDocs = contextNode.getSession().getItem(personalDocsPath);
        if (personalDocs.isNode()) {
          CloudDriveContext.initConnected(WebuiRequestContext.getCurrentInstance(), (Node) personalDocs);
        } else {
          // this should not happen
          LOG.warn("Personal Documents not a Node: " + personalDocs.getPath());
        }
      }
      return isRoot;
    } else {
      return false;
    }
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
