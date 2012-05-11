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
package org.exoplatform.upgrade;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Add two more preferences into preference list of all old SCV
 *
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          vu.nguyen@exoplatform.com
 * Dec 15, 2010  
 */
public class PortletPreferenceMigrationService implements Startable {

  private static final String MOP_PORTLET_PREFERENCE = "mop:portletpreference";
  
  private Log log = ExoLogger.getLogger(this.getClass());
  
  private RepositoryService repoService;
  
  public PortletPreferenceMigrationService(RepositoryService repositoryService) {
    repoService = repositoryService;
  }

  @Override
  public void start() {
    upgradePortletPreference("searches/WCMAdvanceSearchPortlet","basePath","detail");
  }
  
  public void upgradePortletPreference(String portletName, String prefName, String prefValue) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      //RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession("portal-system", repository);
      QueryManager manager = session.getWorkspace().getQueryManager();
      String statement = "SELECT * from mop:workspaceclone WHERE mop:contentid='" + portletName + 
                         "' and fn:name() = 'mop:customization'";
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodes = query.execute().getNodes();

      while (nodes.hasNext()) {
        Node node = (Node)nodes.next();
        if (node.hasNode("mop:state")) {
          Node stateNode = node.getNode("mop:state");
          if (stateNode.isNodeType("mop:portletpreferences")) {
            addNode(stateNode, "mop:" + prefName, prefValue);
            stateNode.save();
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error("An unexpected problem occurs when adding preference " + prefName +  
                                          " for portlet " + portletName, e);
    } finally {
      sessionProvider.close();
    }
  }
  
  private void addNode(Node stateNode, String nodeName, String value) {
    try {
      Node prefNode = stateNode.hasNode(nodeName) ? stateNode.getNode(nodeName) : 
                                                    stateNode.addNode(nodeName, MOP_PORTLET_PREFERENCE);
      prefNode.setProperty("mop:value", new String[]{value});
      prefNode.setProperty("mop:readonly", false);
      stateNode.save();
      if (log.isInfoEnabled()) { 
        log.info("Add :: mop:portletpreference :: "+nodeName+" :-: "+value+ " ::" +stateNode.getPath());
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected problem occurs when adding preference " + nodeName + " for portlet ", e);
      }
    }
  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub
  }
}
