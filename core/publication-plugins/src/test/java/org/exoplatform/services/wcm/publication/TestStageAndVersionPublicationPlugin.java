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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 24, 2012  
 */

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestStageAndVersionPublicationPlugin extends BaseECMSTestCase {
  
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String TEST = "test";
  private static final String ENROLLED = "enrolled";
  private static final String PUBLISHED = "published";
  private static final String NON_PUBLISHED = "non published";
  private static final String ID = "portal#test:/web/presentation/ContentListViewerPortlet";
  
  private PublicationService publicationService_;
  private StageAndVersionPublicationPlugin plugin_;
  private PortalConfig portal_;
  private Page page_;
  private Portlet portlet_;
  private Node node_;
  /** . */
  private DataStorage storage_;
  /** . */
  private NavigationService navService;
  /** . */
  private POMSessionManager mgr;
  /** . */
  private LinkedList<Event> events;
  /** . */
  private ListenerService listenerService;
  /** . */
  private OrganizationService org;
  
  public void setUp() throws Exception {
    super.setUp();
    publicationService_ = WCMCoreUtils.getService(PublicationService.class);
    RequestLifeCycle.begin(PortalContainer.getInstance());
    applySystemSession();
    node_ = session.getRootNode().addNode(TEST);
    node_.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new StageAndVersionPublicationPlugin();
    plugin_.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
    plugin_.setDescription(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
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
    mgr = (POMSessionManager)container.getComponentInstanceOfType(POMSessionManager.class);
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
    publicationService_.getPublicationPlugins().clear();
    node_.remove();
    session.save();
    RequestLifeCycle.end();
    super.tearDown();
  }

  /**
   * tests changing state for a node 
   */
  public void testChangeState() throws Exception {
    HashMap<String, String> context = new HashMap<String, String>();
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.changeState(node_, PublicationDefaultStates.DRAFT, context);
    assertEquals(PublicationDefaultStates.DRAFT, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, PublicationDefaultStates.ENROLLED, context);
    assertEquals(PublicationDefaultStates.ENROLLED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, PublicationDefaultStates.PUBLISHED, context);
    assertEquals(PublicationDefaultStates.PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, PublicationDefaultStates.PUBLISHED, context);
    assertEquals(PublicationDefaultStates.PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, PublicationDefaultStates.OBSOLETE, context);
    assertEquals(PublicationDefaultStates.OBSOLETE, node_.getProperty(CURRENT_STATE).getString());
  }

  /**
  * tests getting user info
  */
  public void testGetUserInfo() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertNull(plugin_.getUserInfo(node_, new Locale("en")));
  }

  /**
   * tests getLocalizedAndSubstituteLog
   */
  public void testGetLocalizedAndSubstituteLog() throws Exception {
    assertEquals("The content is published", plugin_.getLocalizedAndSubstituteMessage(
         new Locale("en"), "PublicationService.StageAndVersionPublicationPlugin.changeState.published", new String[]{}));
  }
  
  /**
   * tests update lifecycle on change content 
   */
  public void testUpdateLifeCycleOnChangeContent() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.updateLifecyleOnChangeContent(node_, node_.getSession().getUserID());
    assertEquals(PublicationDefaultStates.DRAFT, node_.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString());
    plugin_.updateLifecyleOnChangeContent(node_, node_.getSession().getUserID(), PublicationDefaultStates.PUBLISHED);
    assertEquals(PublicationDefaultStates.PUBLISHED, node_.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).
                 getString());
  }
}
