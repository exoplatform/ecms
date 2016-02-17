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
package org.exoplatform.services.wcm.publication;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 29, 2008
 */
public class WCMPublicationServiceImpl implements WCMPublicationService, Startable {

  /** The Constant SIMPLE_LIFECYCLE_NAME. */
  private static final String SIMPLE_LIFECYCLE_NAME = "Simple publication";

  /** The Constant STATEVERSION_LIFECYCLE_NAME. */
  public static final String STATEVERSION_LIFECYCLE_NAME = "States and versions based publication";

  /** The publication plugins. */
  private HashMap<String, WebpagePublicationPlugin> publicationPlugins =
    new HashMap<String, WebpagePublicationPlugin>();

  /** The publication service. */
  protected PublicationService publicationService;

  protected ListenerService listenerService;

  protected CmsService cmsService;

  /**
   * Instantiates a new WCM publication service.
   * This service delegate to PublicationService to manage the publication
   */
  public WCMPublicationServiceImpl() {
    this.publicationService = WCMCoreUtils.getService(PublicationService.class);
    this.listenerService = WCMCoreUtils.getService(ListenerService.class);
    this.cmsService = WCMCoreUtils.getService(CmsService.class);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMPublicationPresentationService
   * #addPublicationPlugin
   * (org.exoplatform.services.wcm.publication.WebpagePublicationPlugin)
   */
  public void addPublicationPlugin(WebpagePublicationPlugin p) {
    publicationPlugins.put(p.getLifecycleName(),p);
    publicationService.addPublicationPlugin(PublicationPlugin.class.cast(p));
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WCMPublicationService#
   * enrollNodeInLifecycle(javax.jcr.Node, java.lang.String)
   */
  public void enrollNodeInLifecycle(Node node, String lifecycleName) throws Exception {
    publicationService.enrollNodeInLifecycle(node,lifecycleName);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMPublicationPresentationService
   * #unsubcribeLifecycle(javax.jcr.Node)
   */
  public void unsubcribeLifecycle(Node node) throws NotInPublicationLifecycleException, Exception {
    publicationService.unsubcribeLifecycle(node);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMPublicationPresentationService
   * #getWebpagePublicationPlugins()
   */
  public Map<String, WebpagePublicationPlugin> getWebpagePublicationPlugins() {
    return publicationPlugins;
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start()   {
  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WCMPublicationService#isEnrolledInWCMLifecycle(javax.jcr.Node)
   */
  public boolean isEnrolledInWCMLifecycle(Node node) throws NotInPublicationLifecycleException, Exception {
    if(!publicationService.isNodeEnrolledInLifecycle(node))
      return false;
    String lifecyleName = publicationService.getNodeLifecycleName(node);
    if(publicationPlugins.containsKey(lifecyleName))
      return true;
    throw new NotInWCMPublicationException();
  }

  /**
   * This default implementation uses "States and versions based publication" as
   * a default lifecycle for all sites and "Simple Publishing" for the root
   * user.
   */
  public void enrollNodeInLifecycle(Node node, String siteName, String remoteUser) throws Exception {
    /*
     * lifecycle based on site (each site can define its own publication
     * lifecycle) We choose to use a different publication plugin for testing
     * only for now (test has to be created separetly)
     */
    if ("test".equals(siteName)) {
      enrollNodeInLifecycle(node, SIMPLE_LIFECYCLE_NAME);
    } else {
      enrollNodeInLifecycle(node, STATEVERSION_LIFECYCLE_NAME);
    }
  }

  /**
   * This default implementation simply delegates updates to the node WebpagePublicationPlugin.
   */
  public void updateLifecyleOnChangeContent(Node node, String siteName, String remoteUser)
      throws Exception {
    updateLifecyleOnChangeContent(node, siteName, remoteUser, null);
  }

  /**
   * This default implementation checks if the state is valid then delegates the update to the node WebpagePublicationPlugin.
   */
  public void updateLifecyleOnChangeContent(Node node, String siteName, String remoteUser, String newState)
      throws Exception {

      if(!publicationService.isNodeEnrolledInLifecycle(node)) {
        enrollNodeInLifecycle(node,siteName, remoteUser);
      }
      String lifecycleName = publicationService.getNodeLifecycleName(node);
      WebpagePublicationPlugin publicationPlugin = publicationPlugins.get(lifecycleName);

      boolean hasState = false;
      if (newState!=null) {
        String[] states = publicationPlugin.getPossibleStates();
        for (String state:states) {
          if (state.equals(newState)) hasState=true;
        }
      }
      if (hasState)
        publicationPlugin.updateLifecyleOnChangeContent(node, remoteUser, newState);
      else
        publicationPlugin.updateLifecyleOnChangeContent(node, remoteUser);

      listenerService.broadcast(UPDATE_EVENT, cmsService, node);
  }

  public String getContentState(Node node) throws Exception {
    String currentState = null;
    if(node.hasProperty("publication:currentState")) {
      currentState = node.getProperty("publication:currentState").getString();
    }
    return currentState;
  }
}
