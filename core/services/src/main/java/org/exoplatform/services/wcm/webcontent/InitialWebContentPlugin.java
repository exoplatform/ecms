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
package org.exoplatform.services.wcm.webcontent;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 22, 2008
 */
public class InitialWebContentPlugin extends CreatePortalPlugin {

  @SuppressWarnings("unused")
  private static Log log = ExoLogger.getLogger(CreatePortalPlugin.class);
  private InitParams initParams;
  private ConfigurationManager configurationManager;
  private RepositoryService repositoryService;
  private ExoCache<String, String> artifactsCache;
  private LivePortalManagerService livePortalManagerService;
  /**
   * Instantiates a new initial web content plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   */
  public InitialWebContentPlugin(InitParams initParams,
                                 ConfigurationManager configurationManager,
                                 RepositoryService repositoryService,
                                 CacheService cacheService,
                                 LivePortalManagerService livePortalManagerService) throws Exception {
    super(initParams, configurationManager, repositoryService);
    this.initParams = initParams;
    this.configurationManager = configurationManager;
    this.repositoryService = repositoryService;
    this.artifactsCache = cacheService.getCacheInstance(this.getClass().getName());
    this.livePortalManagerService = livePortalManagerService;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.artifacts.BasePortalArtifactsPlugin
   * #deployToPortal(java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  @SuppressWarnings("unchecked")
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    DeploymentDescriptor deploymentDescriptor = null;
    try {
      while (iterator.hasNext()) {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        deploymentDescriptor = (DeploymentDescriptor) objectParameter.getObject();
        String sourcePath = deploymentDescriptor.getSourcePath();
        // sourcePath should start with: war:/, jar:/, classpath:/, file:/
        String xmlData = (String) artifactsCache.get(sourcePath);
        if (xmlData == null) {
          InputStream stream = configurationManager.getInputStream(sourcePath);
          xmlData = IOUtil.getStreamContentAsString(stream);
          artifactsCache.put(sourcePath, xmlData);
        }
        ManageableRepository repository = repositoryService.getCurrentRepository();
        Session session = sessionProvider.getSession(deploymentDescriptor.getTarget()
                                                                         .getWorkspace(),
                                                     repository);
        String targetPath = deploymentDescriptor.getTarget().getNodePath();
        String realTargetFolder = StringUtils.replace(targetPath, "{portalName}", portalName);
        InputStream inputStream = configurationManager.getInputStream(sourcePath);
        session.importXML(realTargetFolder, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
      }
      Node portalNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
      configure(portalNode, portalName);
      portalNode.getSession().save();
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
      log.error("deploy the portal "
                    + portalName
                    + " from "
                    + deploymentDescriptor.getSourcePath()
                    + " into "
                    + StringUtils.replace(deploymentDescriptor.getTarget().getNodePath(),
                                          "{portalName}",
                                          portalName) + " is FAILURE at " + new Date().toString()
                    + "\n",
                ex);
      }
      throw ex;
    }
  }

  /**
   * Configure.
   *
   * @param session the session
   * @param folderPath the folder path
   * @param portalName the portal name
   *
   * @throws Exception the exception
   */
  private void configure(Node targetNode, String portalName) throws Exception{
    String statement = "select * from nt:resource where jcr:path like '" + targetNode.getPath()
        + "/%' order by jcr:dateModified ASC";
    QueryManager queryManager = targetNode.getSession().getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(statement,Query.SQL);
    NodeIterator iterator = query.execute().getNodes();
    for(;iterator.hasNext();) {
      Node ntResource = iterator.nextNode();
      String mimeType = ntResource.getProperty("jcr:mimeType").getString();
      if(!mimeType.startsWith("text")) continue;
      String jcrData = ntResource.getProperty("jcr:data").getString();
      if(!jcrData.contains("{portalName}")) continue;
      String realData = StringUtils.replace(jcrData, "{portalName}",portalName);
      ntResource.setProperty("jcr:data",realData);
    }
  }
}
