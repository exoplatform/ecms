/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.plugins;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Mar 9, 2012  
 */
public class UpgradePortletPreferencesPlugin extends UpgradeProductPlugin {

  private static final String MOP_PORTLET_PREFERENCE = "mop:portletpreference";
  
  private RepositoryService repoService_;
  private Log log = ExoLogger.getLogger(this.getClass());

  public UpgradePortletPreferencesPlugin(RepositoryService repoService, InitParams initParams) {
    super(initParams);
    this.repoService_ = repoService;
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    //get portlet name, preference name and value to change
    //String unchangedViews = PrivilegedSystemHelper.getProperty("portletPreferencesChanged");
    String unchangedViews = "searches/WCMAdvanceSearchPortlet,detailParameterName,content-id";
    if (unchangedViews == null) return;
    String[] params = unchangedViews.split(",");
    if (!(params.length == 3)) {
      if (log.isErrorEnabled()) {
        log.error("Wrong format for '" + this.getClass().getName() + "', correct pattern is: " +
                  "portletPreferencesChanged=PortletName,PreferenceName,PreferenceValue");
      }
      return;
    }
    String portletName = params[0];//searches/WCMAdvanceSearchPortlet
    String preferenceName = params[1];//detailParameterName
    String preferenceValue = params[2];//content-id
    //process upgrade in jcr data base
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      ManageableRepository repository = repoService_.getCurrentRepository();
      /**
       * add "detailParameterName=content-id" preferences into preference list of all WCMAdvanceSearchPortlet 
       */
      Session session = sessionProvider.getSession("portal-system", repository);
      QueryManager manager = session.getWorkspace().getQueryManager();
      String statement = "SELECT * from mop:workspaceclone "
          + "WHERE mop:contentid='" + portletName + "' and fn:name() = 'mop:customization'";
      Query query = manager.createQuery(statement.toString(), Query.SQL);
      NodeIterator nodes = query.execute().getNodes();

      while (nodes.hasNext()) {
        Node node = (Node)nodes.next();
        if (node.hasNode("mop:state")) {
          Node stateNode = node.getNode("mop:state");
          if (stateNode.isNodeType("mop:portletpreferences")) {
            addNode(stateNode, "mop:" + preferenceName, preferenceValue);
            stateNode.save();
          }
        }
      }
  
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when add preference '" + preferenceName
            + "' for portlet " + portletName, e);
      }
    } finally {
      sessionProvider.close();
    }
  }
  
  private void addNode(Node stateNode, String nodeName, String value) {
    try {
      if (!stateNode.hasNode(nodeName)) {
        Node prefNode = stateNode.addNode(nodeName, MOP_PORTLET_PREFERENCE);
        prefNode.setProperty("mop:value", new String[]{value});
        prefNode.setProperty("mop:readonly", false);
        stateNode.save();
        if (log.isInfoEnabled()) log.info("Add :: mop:portletpreference :: "+nodeName+" :-: "+value+ " ::" +stateNode.getPath());
      }
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    }
  }
  

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }


}
