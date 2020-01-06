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
package org.exoplatform.services.wcm.publication;

import java.util.LinkedList;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 26, 2012  
 */
public class TestWCMPublicationService extends BasePublicationTestCase {
  
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String TEST = "test";
  private static final String ENROLLED = "enrolled";
  private static final String PUBLISHED = "published";

  private WCMPublicationService publicationService_;
  private WebpagePublicationPlugin plugin_;
  private Node node_;
  private PortalConfig portal_;
  private Page page_;
  private Portlet portlet_;
  //-----------------
  /** . */
  private final String testPage = "portal::classic::testPage";

  /** . */
  private final String testPortletPreferences = "portal#classic:/web/BannerPortlet/testPortletPreferences";

  /** . */
  private DataStorage storage_;

  /** . */
  private NavigationService navService;

  /** . */
  private LinkedList<Event> events;

  /** . */
  private ListenerService listenerService;

  /** . */
  private OrganizationService org;

  
  public void setUp() throws Exception {
    super.setUp();
    publicationService_ = WCMCoreUtils.getService(WCMPublicationService.class);
    RequestLifeCycle.begin(PortalContainer.getInstance());
    applySystemSession();
    node_ = session.getRootNode().addNode(TEST);
    node_.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new DumpPublicationPlugin();
    plugin_.setName("Simple publication");
    plugin_.setDescription("Simple publication");
    publicationService_.addPublicationPlugin(plugin_);
    
    //--------------------------------
    Listener listener = new Listener()
    {
       @Override
       public void onEvent(Event event) throws Exception
       {
          events.add(event);
       }
    };

    PortalContainer container = PortalContainer.getInstance();
    storage_ = (DataStorage)container.getComponentInstanceOfType(DataStorage.class);
    navService = (NavigationService)container.getComponentInstanceOfType(NavigationService.class);
    events = new LinkedList<Event>();
    listenerService = (ListenerService)container.getComponentInstanceOfType(ListenerService.class);
    org = WCMCoreUtils.getService(OrganizationService.class);

    listenerService.addListener(DataStorage.PAGE_CREATED, listener);
    listenerService.addListener(DataStorage.PAGE_REMOVED, listener);
    listenerService.addListener(DataStorage.PAGE_UPDATED, listener);
    listenerService.addListener(EventType
                                .NAVIGATION_CREATED, listener);
    listenerService.addListener(EventType.NAVIGATION_DESTROYED, listener);
    listenerService.addListener(EventType.NAVIGATION_UPDATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_CREATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_UPDATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_REMOVED, listener);
  }
  
  public void tearDown() throws Exception {
    publicationService_.getWebpagePublicationPlugins().clear();
    node_.remove();
    session.save();
    RequestLifeCycle.end();
    super.tearDown();
  }

  /**
   * tests add publication plugin: 
   * add 1 publication plugins and check if the total plugins number is 1
   */
  public void testAddPublicationPlugin() throws Exception {
    assertEquals(1, publicationService_.getWebpagePublicationPlugins().size());
  }
  
  /**
   * tests get publication plugin: 
   * add 3 publication plugins, get the total plugins and check if the number is 3 
   */
  public void testGetWebpagePublicationPlugins() throws Exception {
    assertEquals(1, publicationService_.getWebpagePublicationPlugins().size());
  }
  
  /**
   * tests enrolling node in life cycle 1
   */
  public void testEnrollNodeInLifecycle1() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, node_.getProperty(CURRENT_STATE).getString());
  }
  
  /**
   * tests enrolling node in life cycle 2
   */
  public void testEnrollNodeInLifecycle2() throws Exception {
    Exception e = null;
    try {
      publicationService_.enrollNodeInLifecycle(node_, "classic", node_.getSession().getUserID());
    } catch (Exception ex) {
      e = ex;
    }
    assertNotNull(e);
    
    publicationService_.enrollNodeInLifecycle(node_, "test", node_.getSession().getUserID());
    assertEquals(ENROLLED, node_.getProperty(CURRENT_STATE).getString());
    
  }
  
  /**
   * tests if node is enrolled in lifecycle
   */
  public void testIsEnrolledWCMInLifecycle() throws Exception {
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertTrue(publicationService_.isEnrolledInWCMLifecycle(node_));
  }

  /**
   * tests getting content state 
   */
  public void testGetContentState() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, publicationService_.getContentState(node_));
  }
  
  /**
   * tests unsubscribing node
   */
  public void testUnsubscribeLifecycle() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.unsubcribeLifecycle(node_);
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
  }
  
  
  /**
   * tests update life cycle on change content 1
   */
  public void testUpdateLifecyleOnChangeContent1() throws Exception {
    publicationService_.updateLifecyleOnChangeContent(
              node_, "test", node_.getSession().getUserID(), PUBLISHED);
    assertEquals(PUBLISHED, publicationService_.getContentState(node_));
    
    publicationService_.updateLifecyleOnChangeContent(
              node_, "test", node_.getSession().getUserID(), "temp");
    assertEquals(DumpPublicationPlugin.DEFAULT_STATE, publicationService_.getContentState(node_));
  }
  
  /**
   * tests update life cycle on change content 2
   */
  public void testUpdateLifecyleOnChangeContent2() throws Exception {
    publicationService_.updateLifecyleOnChangeContent(
              node_, "test", node_.getSession().getUserID());
    assertEquals(DumpPublicationPlugin.DEFAULT_STATE, publicationService_.getContentState(node_));
  }
  
}

