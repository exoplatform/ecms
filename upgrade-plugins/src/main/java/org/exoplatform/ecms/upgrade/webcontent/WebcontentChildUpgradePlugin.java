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
package org.exoplatform.ecms.upgrade.webcontent;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 27, 2013  
 */
public class WebcontentChildUpgradePlugin extends UpgradeProductPlugin {
  
  private static final Log LOG = ExoLogger.getLogger(WebcontentChildUpgradePlugin.class.getName());
  private RepositoryService repoService_;

  public WebcontentChildUpgradePlugin(RepositoryService repoService, InitParams initParams) {
    super(initParams);
    this.repoService_  = repoService;
  }
  
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      //get all session
      for(WorkspaceEntry wsEntry : WCMCoreUtils.getRepository().getConfiguration().getWorkspaceEntries()) {
        Session session = sessionProvider.getSession(wsEntry.getName(), repoService_.getCurrentRepository());
        //add new property value
        String statement = "SELECT * FROM " + NodetypeConstant.EXO_WEBCONTENT + " order by exo:name";
        Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
        for (NodeIterator iter = query.execute().getNodes(); iter.hasNext();) {
          Node wc = null;
          try {
            wc = iter.nextNode();
            //default.html/jcr:content
            if (wc.hasNode("default.html/" + NodetypeConstant.JCR_CONTENT) &&
                wc.getNode("default.html/" + NodetypeConstant.JCR_CONTENT).canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD)) {
              wc.getNode("default.html/" + NodetypeConstant.JCR_CONTENT).addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
            }
            //js/default.js/jcr:content
            if (wc.hasNode("js/default.js/" + NodetypeConstant.JCR_CONTENT) &&
                wc.getNode("js/default.js/" + NodetypeConstant.JCR_CONTENT).canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD)) {
              wc.getNode("js/default.js/" + NodetypeConstant.JCR_CONTENT).addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
            }
            //css/default.css/jcr:content
            if (wc.hasNode("css/default.css/" + NodetypeConstant.JCR_CONTENT) &&
                wc.getNode("css/default.css/" + NodetypeConstant.JCR_CONTENT)
                .canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD)) {
              wc.getNode("css/default.css/" + NodetypeConstant.JCR_CONTENT).addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
            }
            wc.save();
            LOG.info("Added mixin " + NodetypeConstant.EXO_WEBCONTENT_CHILD + " for node " + wc.getPath());
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("An unexpected error occurs when add mixin " + NodetypeConstant.EXO_WEBCONTENT_CHILD + 
                        " for node " + wc.getPath());
            }
          }
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Add mixin '" + NodetypeConstant.EXO_WEBCONTENT_CHILD + "' for all webcontent child  successfully!");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when add mixin " + NodetypeConstant.EXO_WEBCONTENT_CHILD, e);
      }
    } finally {
      sessionProvider.close();
    }
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

}
