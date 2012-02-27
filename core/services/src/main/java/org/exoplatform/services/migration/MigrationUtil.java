/*
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
 */
package org.exoplatform.services.migration;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * This class is here to get and update CLV portlet preferences on the fly.
 *
 * Owner : Benjamin Paillereau
 * Date: 6 déc. 2010
 * Time: 14:50:18
 */
public class MigrationUtil {


  static String OLD_TEMPLATE_PORLET_NAME   = "Content List Viewer";
  static String NEW_TEMPLATE_PORTLET_NAME  = "content-list-viewer";

  private static final Log LOG = ExoLogger.getLogger(MigrationUtil.class);

  static public String checkAndUpdateViewerTemplate(String strTemplatePath) {

    if (strTemplatePath.contains(OLD_TEMPLATE_PORLET_NAME)) {

      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();//UserSessionProvider();
      RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      try {
        Session session = sessionProvider.getSession("dms-system", repoService.getCurrentRepository());
        Node root = session.getRootNode();
        String templateName = strTemplatePath.substring(strTemplatePath.lastIndexOf("/")+1);
        if (root.hasNode("exo:ecm/views/templates/content-list-viewer/list/"+templateName)) {
          strTemplatePath = "/exo:ecm/views/templates/content-list-viewer/list/"+templateName;
        } else if (root.hasNode("exo:ecm/views/templates/content-list-viewer/navigation/"+templateName)) {
          strTemplatePath = "/exo:ecm/views/templates/content-list-viewer/navigation/"+templateName;
        } else if (root.hasNode("exo:ecm/views/templates/content-list-viewer/paginators/"+templateName)) {
          strTemplatePath = "/exo:ecm/views/templates/content-list-viewer/paginators/"+templateName;
        } else {
          if (LOG.isInfoEnabled()) LOG.info("Cannot locate in new location : "+strTemplatePath);
        }
      } catch (LoginException le) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(le.getMessage());
        }
      } catch (NoSuchWorkspaceException nswe) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(nswe.getMessage());
        }
      } catch (RepositoryException re) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(re.getMessage());
        }
      }
    }

    return strTemplatePath;
  }
}
