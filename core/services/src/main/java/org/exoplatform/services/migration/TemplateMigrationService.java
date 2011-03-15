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
package org.exoplatform.services.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.ecm.ProductVersions;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Migrate Templates from an old structure to a new one
 *
 * User: Benjamin Paillereau
 * Date: 3 déc. 2010
 * Time: 19:18:38
 */
public class TemplateMigrationService implements Startable {

  RepositoryService repositoryService = null;
  ApplicationTemplateManagerService appTemplateService;
  private Log log = ExoLogger.getLogger(this.getClass());

  public TemplateMigrationService(RepositoryService repositoryService,
                                  ApplicationTemplateManagerService appTemplateService) {
    this.repositoryService = repositoryService;
    this.appTemplateService = appTemplateService;
  }

  public void start() {
    if (ProductVersions.getCurrentVersionAsInt()==ProductVersions.WCM_2_1_2_NUM) {
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      try {
        ManageableRepository repository = repositoryService.getCurrentRepository();
        Session session = sessionProvider.getSession("dms-system", repository);
        Node rootNode = session.getRootNode();


        /**
         * CHECK IF WE USE THE OLD STRUCTURE (PRE 2.1.2) OR IF IT'S A POST 2.1.2 WEBSITE
         */
        if (rootNode.hasNode("exo:ecm/views/templates/Content List Viewer")) {
          /**
           * CREATE NEW STRUCTURE IF IT DOESN'T EXIST
           */
          if (!rootNode.hasNode("exo:ecm/views/templates/content-list-viewer")) {
            rootNode.addNode("exo:ecm/views/templates/content-list-viewer", "nt:unstructured");
            if (log.isInfoEnabled()) log.info("CREATE :: content-list-viewer");
            rootNode.save();
          }
          if (!rootNode.hasNode("exo:ecm/views/templates/content-list-viewer/paginators")) {
            rootNode.addNode("exo:ecm/views/templates/content-list-viewer/paginators", "nt:unstructured");
            if (log.isInfoEnabled()) log.info("CREATE :: paginators");
            rootNode.save();
          }
          if (!rootNode.hasNode("exo:ecm/views/templates/content-list-viewer/list")) {
            rootNode.addNode("exo:ecm/views/templates/content-list-viewer/list", "nt:unstructured");
            if (log.isInfoEnabled()) log.info("CREATE :: list");
            rootNode.save();
          }
          if (!rootNode.hasNode("exo:ecm/views/templates/content-list-viewer/navigation")) {
            rootNode.addNode("exo:ecm/views/templates/content-list-viewer/navigation", "nt:unstructured");
            if (log.isInfoEnabled()) log.info("CREATE :: navigation");
            rootNode.save();
          }


          /**
           * CHECK IF PAGINATORS ARE IN THE NEW PLACE
           */
          Node oldPaginatorsNode = rootNode.getNode("exo:ecm/views/templates/Content List Viewer/paginators");
          Node newPaginatorsNode = rootNode.getNode("exo:ecm/views/templates/content-list-viewer/paginators");
          NodeIterator oldPaginatorsNodes = oldPaginatorsNode.getNodes();
          while (oldPaginatorsNodes.hasNext()) {
            Node paginatorNode = oldPaginatorsNodes.nextNode();
            if (!newPaginatorsNode.hasNode(paginatorNode.getName())) {
              /**
               * NODE NOT MIGRATED, WE DO IT
               */
              session.getWorkspace()
                     .copy("/exo:ecm/views/templates/Content List Viewer/paginators/"
                               + paginatorNode.getName(),
                           "/exo:ecm/views/templates/content-list-viewer/paginators/"
                               + paginatorNode.getName());
              if (log.isInfoEnabled())
                log.info("CLONE :: " + paginatorNode.getName());
            }
          }
          session.save();
          /**
           * CHECK IF VIEWS ARE IN THE NEW PLACE
           */
          Node oldViewNode = rootNode.getNode("exo:ecm/views/templates/Content List Viewer/list-by-folder");
          Node newViewListNode = rootNode.getNode("exo:ecm/views/templates/content-list-viewer/list");
          Node newViewNavNode = rootNode.getNode("exo:ecm/views/templates/content-list-viewer/navigation");
          NodeIterator oldViewNodes = oldViewNode.getNodes();
          while (oldViewNodes.hasNext()) {
            Node viewNode = oldViewNodes.nextNode();
            if (!newViewListNode.hasNode(viewNode.getName())
                && !newViewNavNode.hasNode(viewNode.getName())) {
              Node content = viewNode.getNode("jcr:content");
              InputStream data = content.getProperty("jcr:data").getStream();
              String sdata = inputStreamAsString(data);
              String targetFolder = "list";
              if (sdata.contains("public void renderCategories")) {
                targetFolder = "navigation";
              }

              /**
               * NODE NOT MIGRATED, WE DO IT
               */
              session.getWorkspace()
                     .copy("/exo:ecm/views/templates/Content List Viewer/list-by-folder/"
                               + viewNode.getName(),
                           "/exo:ecm/views/templates/content-list-viewer/" + targetFolder + "/"
                               + viewNode.getName());
              if (log.isInfoEnabled())
                log.info("CLONE :: " + targetFolder + " :: " + viewNode.getName());
            }
          }
          session.save();

          session.logout();


          /**
           * UPDATE OLD PORTLET PREFERENCES
           */

          session = sessionProvider.getSession("portal-system", repository);
          QueryManager manager = session.getWorkspace().getQueryManager();
          String statement =
            "SELECT * from mop:portletpreference where mop:value LIKE '/exo:ecm/views/templates/Content List Viewer/%'";
          Query query = manager.createQuery(statement.toString(), Query.SQL);
          NodeIterator nodes = query.execute().getNodes();

          while (nodes.hasNext()) {
            Node node = (Node)nodes.next();
            String value = node.getProperty("mop:value").getValues()[0].getString();
            String newValue = MigrationUtil.checkAndUpdateViewerTemplate(value);
            if (!value.equals(newValue)) {
              if (log.isInfoEnabled())
                log.info("CONVERT :: mop:portletpreference :: " + value + " :: " + newValue
                    + " :: " + node.getPath());
              node.setProperty("mop:value", new String[] { newValue });
              node.save();
            }

          }

          if (log.isWarnEnabled()) {
            log.warn("IMPORTANT NOTE ABOUT WCM 2.1.2 :\n"
                + "All CLV templates have been copied to a new storage place. "
                + "We keep your old templates in the old structure but they won't be used anymore.\n"
                + "Storage place goes from dms-system:/exo:ecm/views/templates/Content List Viewer to "
                + "dms-system:/exo:ecm/views/templates/content-list-viewer\n"
                + "Please, read the WCM 2.1.2 Upgrade Notice or contact the eXo Support for more info.");
          }


        }




      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error("An unexpected problem occurs when migrating templates to new structure", e);
      } finally {
        sessionProvider.close();
      }
    }
  }

  public void stop() {
  }


  private static String inputStreamAsString(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    StringBuilder sb = new StringBuilder();
    String line = null;

    while ((line = br.readLine()) != null) {
      sb.append(line);
    }

    br.close();
    return sb.toString();
  }
}
