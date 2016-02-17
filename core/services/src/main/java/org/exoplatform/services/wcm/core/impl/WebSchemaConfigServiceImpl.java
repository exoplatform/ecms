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
package org.exoplatform.services.wcm.core.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.core.WebSchemaHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Jun 3, 2008
 */
public class WebSchemaConfigServiceImpl implements WebSchemaConfigService, Startable {

  /** The web schema handlers. */
  private ConcurrentHashMap<String, WebSchemaHandler> webSchemaHandlers = new ConcurrentHashMap<String, WebSchemaHandler>();

  /** The wcm config service. */
  private WCMConfigurationService wcmConfigService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(WebSchemaConfigServiceImpl.class.getName());

  /**
   * Instantiates a new web schema config service impl.
   *
   * @param configurationService the configuration service
   * @param nodeHierarchyCreator the hierarchy creator
   */
  public WebSchemaConfigServiceImpl(WCMConfigurationService configurationService, NodeHierarchyCreator nodeHierarchyCreator) {
    this.wcmConfigService = WCMCoreUtils.getService(WCMConfigurationService.class);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.WebSchemaConfigService#addWebSchemaHandler
   * (org.exoplatform.container.component.ComponentPlugin)
   */
  public void addWebSchemaHandler(ComponentPlugin plugin) throws Exception {
    if (plugin instanceof WebSchemaHandler) {
      String clazz = plugin.getClass().getName();
      webSchemaHandlers.putIfAbsent(clazz, (WebSchemaHandler)plugin);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaConfigService#getAllWebSchemaHandler()
   */
  public Collection<WebSchemaHandler> getAllWebSchemaHandler() throws Exception {
    return webSchemaHandlers.values();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaConfigService#getWebSchemaHandlerByType(java.lang.Class)
   */
  public <T extends WebSchemaHandler> T getWebSchemaHandlerByType(Class<T> clazz){
    WebSchemaHandler schemaHandler = webSchemaHandlers.get(clazz.getName());
    if (schemaHandler == null) return null;
    return clazz.cast(schemaHandler);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.WebSchemaConfigService#createSchema(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void createSchema(SessionProvider sessionProvider, Node node) throws Exception {
    for (WebSchemaHandler handler: getAllWebSchemaHandler()) {
      if (handler.matchHandler(sessionProvider, node)) {
        handler.onCreateNode(sessionProvider, node);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.WebSchemaConfigService#updateSchemaOnModify
   * (javax.jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void updateSchemaOnModify(SessionProvider sessionProvider, Node node) throws Exception {
    for (WebSchemaHandler handler: getAllWebSchemaHandler()) {
      if (handler.matchHandler(sessionProvider, node)) {
        handler.onModifyNode(sessionProvider, node);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.WebSchemaConfigService#updateSchemaOnRemove
   * (javax.jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void updateSchemaOnRemove(SessionProvider sessionProvider, Node node) throws Exception {
    for (WebSchemaHandler handler: getAllWebSchemaHandler()) {
      if (handler.matchHandler(sessionProvider, node)) {
        handler.onRemoveNode(sessionProvider, node);
        return;
      }
    }
  }

  /**
   * Creates the live share portal folders.
   */
  private void createLiveSharePortalFolders() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      for (NodeLocation locationEntry: wcmConfigService.getAllLivePortalsLocation()) {
        String repoName = locationEntry.getRepository();
        try {
          ManageableRepository repository = WCMCoreUtils.getRepository();
          Session session = sessionProvider.getSession(locationEntry.getWorkspace(), repository);
          Node livePortalsStorage = (Node)session.getItem(locationEntry.getPath());
          String liveSharedPortalName = wcmConfigService.getSharedPortalName();
          if(!livePortalsStorage.hasNode(liveSharedPortalName)) {
            livePortalsStorage.addNode(liveSharedPortalName, "exo:portalFolder");
            session.save();
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Error when try to create share portal folder for repository: "+ repoName, e);
          }
        }
      }
    } finally {
      sessionProvider.close();
    }
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start WebSchemaConfigServiceImpl...");
    }
    createLiveSharePortalFolders();
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() { }
}
