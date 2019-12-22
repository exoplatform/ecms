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
package org.exoplatform.services.wcm.portal.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;

import org.picocontainer.Startable;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 19, 2008
 */

/**
 * The Class LivePortalManagerServiceImpl.
 */
public class LivePortalManagerServiceImpl implements LivePortalManagerService, Startable {

  private final String                      PORTAL_FOLDER   = "exo:portalFolder";

  private static final Log LOG = ExoLogger.getLogger(LivePortalManagerServiceImpl.class.getName());

  private ConcurrentHashMap<String, String> livePortalPaths = new ConcurrentHashMap<String, String>();

  private RepositoryService                 repositoryService;

  private WCMConfigurationService           wcmConfigService;
  
  private WebSchemaConfigService            webSchemaConfigService;
  
  private UserPortalConfigService           portalConfigService;

  private ListenerService                   listenerService;

  /**
   * Instantiates a new live portal manager service impl.
   *
   * @param webSchemaConfigService the web schema config service
   * @param wcmConfigurationService the wcm config service
   * @param repositoryService the repository service
   */
  public LivePortalManagerServiceImpl(
      ListenerService listenerService,
      WebSchemaConfigService webSchemaConfigService,
      WCMConfigurationService wcmConfigurationService,
      UserPortalConfigService portalConfigService,
      RepositoryService repositoryService) {
    this.wcmConfigService = wcmConfigurationService;
    this.webSchemaConfigService = webSchemaConfigService;
    this.portalConfigService = portalConfigService;
    this.repositoryService = repositoryService;
    this.listenerService = listenerService;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#getLivePortal
   * (java.lang.String, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final Node getLivePortal(final SessionProvider sessionProvider, final String portalName) throws Exception {
    String currentRepository = repositoryService.getCurrentRepository().getConfiguration().getName();
    return getLivePortal(sessionProvider, currentRepository, portalName);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#getLivePortals
   * (org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final List<Node> getLivePortals(final SessionProvider sessionProvider) throws Exception {
    String currentRepository = repositoryService.getCurrentRepository().getConfiguration().getName();
    return getLivePortals(sessionProvider, currentRepository);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.portal.LivePortalManagerService#
   * getLiveSharedPortal
   * (org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final Node getLiveSharedPortal(final SessionProvider sessionProvider) throws Exception {
    String currentRepository = repositoryService.getCurrentRepository().getConfiguration().getName();
    return getLiveSharedPortal(sessionProvider, currentRepository);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#getLivePortal
   * (java.lang.String, java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final Node getLivePortal(final SessionProvider sessionProvider,
                                  final String repository,
                                  final String portalName) throws Exception {
    Node portalsStorage = getLivePortalsStorage(sessionProvider);
    return portalsStorage.getNode(portalName);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#getLivePortals
   * (java.lang.String, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final List<Node> getLivePortals(final SessionProvider sessionProvider, final String repository) throws Exception {
    List<Node> list = new ArrayList<Node>();
    Node portalsStorage = getLivePortalsStorage(sessionProvider);
    for (NodeIterator iterator = portalsStorage.getNodes(); iterator.hasNext(); ) {
      Node node = iterator.nextNode();
      if (node.isNodeType(PORTAL_FOLDER)) {
        list.add(node);
      }
    }
    return list;
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.portal.LivePortalManagerService#
   * getLiveSharedPortal(java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final Node getLiveSharedPortal(final SessionProvider sessionProvider,
                                        final String repository) throws Exception {
    Node portalsStorage = getLivePortalsStorage(sessionProvider);
    String sharePortalName = wcmConfigService.getSharedPortalName();
    try {
      return portalsStorage.getNode(sharePortalName);
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  private Node getLivePortalsStorage(final SessionProvider sessionProvider) throws Exception {
    NodeLocation locationEntry = wcmConfigService.getLivePortalsLocation();
    String workspace = locationEntry.getWorkspace();
    String portalsStoragePath = locationEntry.getPath();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    return (Node)session.getItem(portalsStoragePath);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#addLivePortal
   * (org.exoplatform.portal.config.model.PortalConfig,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public final void addLivePortal(final SessionProvider sessionProvider, final PortalConfig portalConfig)
  throws Exception {
    Node livePortalsStorage = getLivePortalsStorage(sessionProvider) ;
    String portalName = portalConfig.getName();
    if(livePortalsStorage.hasNode(portalName)) {
      return;
    }
    ExtendedNode newPortal = (ExtendedNode)livePortalsStorage.addNode(portalName,PORTAL_FOLDER);
    if (!newPortal.isNodeType("exo:owneable"))
      newPortal.addMixin("exo:owneable");
    if(newPortal.canAddMixin("metadata:siteMetadata")) {
      newPortal.addMixin("metadata:siteMetadata");
      newPortal.setProperty("siteTitle",portalName);
      newPortal.setProperty("keywords",portalName);
      newPortal.setProperty("robots","index,follow");
    }
    if(newPortal.canAddMixin("dc:elementSet")) {
      newPortal.addMixin("dc:elementSet");
    }
    //Need set some other property for the portal node from portal config like access permission ..
    newPortal.getSession().save();
    //put sharedPortal path to the map at the first time when run this method
    if(livePortalPaths.size() == 0) {
      String sharedPortalName = wcmConfigService.getSharedPortalName();
      NodeLocation nodeLocation = wcmConfigService.getLivePortalsLocation();
      livePortalPaths.put(sharedPortalName,nodeLocation.getPath() + "/"+ sharedPortalName);
    }
    livePortalPaths.put(portalName,newPortal.getPath());
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.LivePortalManagerService#removeLivePortal
   * (org.exoplatform.portal.config.model.PortalConfig,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void removeLivePortal(final SessionProvider sessionProvider, final PortalConfig portalConfig)
  throws Exception {
    //Remove site content folder for the portal in this version
    //for next version, we will move it to backup ws
    Node node = getLivePortal(sessionProvider, portalConfig.getName());
    Session session = node.getSession();
    node.remove();
    session.save();
    livePortalPaths.remove(portalConfig.getName());
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.portal.LivePortalManagerService#getLivePortalsPath()
   */
  public Collection<String> getLivePortalsPath() throws Exception {
    return livePortalPaths.values();
  }

  public String getPortalNameByPath(String portalPath) throws Exception {
    Set<String> keys = livePortalPaths.keySet();
    for(String portalName: keys.toArray(new String[keys.size()])) {
      if(livePortalPaths.get(portalName).equalsIgnoreCase(portalPath)) {
        return portalName;
      }
    }
    return null;
  }

  public Node getLivePortalByChild(Node childNode) throws Exception {
    for(String portalPath: livePortalPaths.values()) {
      if(childNode.getPath().startsWith(portalPath)) {
        return (Node)childNode.getSession().getItem(portalPath);
      }
    }
    return null;
  }

  public void start() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start LivePortalManagementService....");
    }
    SessionProvider sessionProvider = null;
    Session session = null;
    try {
      checkAllPortalSitesCreated();
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository repository = repositoryService.getCurrentRepository();
      NodeLocation nodeLocation = wcmConfigService.getLivePortalsLocation();
      session = sessionProvider.getSession(nodeLocation.getWorkspace(),repository);
      String statement = "select * from exo:portalFolder where jcr:path like '" + nodeLocation.getPath() + "/%'";
      Query query = session.getWorkspace().getQueryManager().createQuery(statement,Query.SQL);
      QueryResult result = query.execute();
      for(NodeIterator iterator = result.getNodes(); iterator.hasNext();) {
        Node portalNode = iterator.nextNode();
        livePortalPaths.putIfAbsent(portalNode.getName(),portalNode.getPath());
      }
    } catch (Exception e) {
      LOG.error("Error when starting LivePortalManagerService: ", e);
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  public void stop() {
  }

  public String getPortalPathByName(String portalName) throws Exception {
    return livePortalPaths.get(portalName);
  }

  private void checkAllPortalSitesCreated() throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      DataStorage dataStorage = portalConfigService.getDataStorage();
      List<String> allPortalNames = dataStorage.getAllPortalNames();
      if (allPortalNames != null) {
        for (String portalName : allPortalNames) {
          try {
            Node livePortal = getLivePortal(SessionProvider.createSystemProvider(), portalName);
            if (livePortal !=null) {
              continue;
            }
          } catch (Exception e) {
            // Expected when portal node doesn't exist
          }
          PortalConfig portalConfig = dataStorage.getPortalConfig(portalName);
          if (portalConfig == null) {
            continue;
          }
          listenerService.broadcast("org.exoplatform.portal.config.DataStorage.portalConfigCreated", dataStorage, portalConfig);;
        }
      }
    } finally {
      RequestLifeCycle.end();
    }
  }
}
