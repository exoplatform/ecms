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
package org.exoplatform.services.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.javascript.XJavascriptService;
import org.exoplatform.services.wcm.skin.XSkinService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class WCMContentInitializerService implements Startable{

  /** The list deployment plugin. */
  private List<DeploymentPlugin> listDeploymentPlugin = new ArrayList<DeploymentPlugin>();

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(WCMContentInitializerService.class.getName());

  /**
   * Instantiates a new wCM content initializer service.
   *
   * @param userPortalConfigService
   */
  public WCMContentInitializerService(UserPortalConfigService userPortalConfigService) {
    this.repositoryService = WCMCoreUtils.getService(RepositoryService.class);
  }

  /**
   * Adds the plugin.
   *
   * @param deploymentPlugin the deployment plugin
   */
  public void addPlugin(DeploymentPlugin deploymentPlugin) {
    listDeploymentPlugin.add(deploymentPlugin);
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      Node serviceFolder = (Node) session.getItem("/exo:services");
      Node contentInitializerService = null;
      if (serviceFolder.hasNode("WCMContentInitializerService")) {
        contentInitializerService = serviceFolder.getNode("WCMContentInitializerService");
      } else {
        contentInitializerService = serviceFolder.addNode("WCMContentInitializerService", "nt:unstructured");
      }
      Set<String> newSiteNames = new HashSet<String>();
      //use DeploymentPlugin list to deploy data
      boolean firstTimeDeploy = !contentInitializerService.hasNode("WCMContentInitializerServiceLog");
      Date date = new Date();
      StringBuffer logData = new StringBuffer();
      for (DeploymentPlugin deploymentPlugin : listDeploymentPlugin) {
        try {
          if (firstTimeDeploy || deploymentPlugin.isOverride() || !isDeployed(deploymentPlugin)) {
            deploymentPlugin.deploy(sessionProvider);
            if (deploymentPlugin.getSiteName() != null) {
              newSiteNames.add(deploymentPlugin.getSiteName());
            }
            logData.append("deploy " + deploymentPlugin.getName()
                + " deployment plugin succesfully at " + date.toString() + "\n");
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("deploy " + deploymentPlugin.getName() + " deployment plugin failure at "
              + date.toString() + " by " + e + "\n");
          }
          logData.append("deploy " + deploymentPlugin.getName()
              + " deployment plugin failure at " + date.toString() + " by " + e + "\n");
        }
      }

      //marks sites with deployed data
      for (String newSiteName : newSiteNames)
      if (!contentInitializerService.hasNode(newSiteName)) {
        contentInitializerService.addNode(newSiteName, "nt:base");
      }
      //add log data
      Node contentInitializerServiceLog = contentInitializerService.hasNode("WCMContentInitializerServiceLog") ?
                                          contentInitializerService.getNode("WCMContentInitializerServiceLog") :
                                          contentInitializerService.addNode("WCMContentInitializerServiceLog", "nt:file");
      Node contentInitializerServiceLogContent = contentInitializerServiceLog.hasNode("jcr:content") ?
                                                 contentInitializerServiceLog.getNode("jcr:content") :
                                                 contentInitializerServiceLog.addNode("jcr:content", "nt:resource");
      contentInitializerServiceLogContent.setProperty("jcr:encoding", "UTF-8");
      contentInitializerServiceLogContent.setProperty("jcr:mimeType", "text/plain");
      contentInitializerServiceLogContent.setProperty("jcr:data", logData.toString());
      contentInitializerServiceLogContent.setProperty("jcr:lastModified", date.getTime());
      session.save();
      XJavascriptService jsService = WCMCoreUtils.getService(XJavascriptService.class);
      XSkinService xSkinService = WCMCoreUtils.getService(XSkinService.class);
      xSkinService.start();
      jsService.start();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when start WCMContentInitializerService: ", e);
      }
    } finally {
      sessionProvider.close();
    }
  }
  
  /**
   * indicates if this plugin was deployed,
   * always return true for legacy data. 
   */
  public boolean isDeployed(DeploymentPlugin deploymentPlugin) {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository repository = WCMCoreUtils.getRepository();
      Node serviceFolder = (Node) sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository)
                          .getItem("/exo:services");
      Node contentInitializerService = serviceFolder.hasNode("WCMContentInitializerService") ? 
                                       serviceFolder.getNode("WCMContentInitializerService") :
                                       serviceFolder.addNode("WCMContentInitializerService", "nt:unstructured");
      String sideName = deploymentPlugin.getSiteName();
      boolean ret = (sideName == null) || contentInitializerService.hasNode(sideName);
      return ret;
    } catch (Exception e) {
      return false;
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {}
}
