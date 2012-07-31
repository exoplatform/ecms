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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.exoplatform.portal.config.serialize.PortletApplication;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 26, 2012  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestWCMPublicationService extends BaseECMSTestCase {
  
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
  private POMSessionManager mgr;

  /** . */
  private LinkedList<Event> events;

  /** . */
  private ListenerService listenerService;

  /** . */
  private OrganizationService org;

  
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    publicationService_ = WCMCoreUtils.getService(WCMPublicationService.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
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
    mgr = (POMSessionManager)container.getComponentInstanceOfType(POMSessionManager.class);
    navService = (NavigationService)container.getComponentInstanceOfType(NavigationService.class);
    events = new LinkedList<Event>();
    listenerService = (ListenerService)container.getComponentInstanceOfType(ListenerService.class);
    org = (OrganizationService)container.getComponentInstanceOfType(OrganizationService.class);

    //
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

    //
    createPortalModel();
  }
  
  @AfterMethod
  public void tearDown() throws Exception {
    publicationService_.getWebpagePublicationPlugins().clear();
    node_.remove();
    session.save();
    storage_.remove(portal_);
    RequestLifeCycle.end();
  }

  /**
   * tests add publication plugin: 
   * add 1 publication plugins and check if the total plugins number is 1
   */
  @Test
  public void testAddPublicationPlugin() throws Exception {
    assertEquals(1, publicationService_.getWebpagePublicationPlugins().size());
  }
  
  /**
   * tests get publication plugin: 
   * add 3 publication plugins, get the total plugins and check if the number is 3 
   */
  @Test
  public void testGetWebpagePublicationPlugins() throws Exception {
    assertEquals(1, publicationService_.getWebpagePublicationPlugins().size());
  }
  
  /**
   * tests enrolling node in life cycle 1
   */
  @Test
  public void testEnrollNodeInLifecycle1() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, node_.getProperty(CURRENT_STATE).getString());
  }
  
  /**
   * tests enrolling node in life cycle 2
   */
  @Test
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
  @Test
  public void testIsEnrolledWCMInLifecycle() throws Exception {
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertTrue(publicationService_.isEnrolledInWCMLifecycle(node_));
  }

  /**
   * tests getting content state 
   */
  @Test
  public void testGetContentState() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, publicationService_.getContentState(node_));
  }
  
  /**
   * tests unsubscribing node
   */
  @Test
  public void testUnsubscribeLifecycle() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.unsubcribeLifecycle(node_);
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
  }
  
  
  /**
   * tests update life cycle on change content 1
   */
  @Test
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
  @Test
  public void testUpdateLifecyleOnChangeContent2() throws Exception {
    publicationService_.updateLifecyleOnChangeContent(
              node_, "test", node_.getSession().getUserID());
    assertEquals(DumpPublicationPlugin.DEFAULT_STATE, publicationService_.getContentState(node_));
  }
  
  /**
   * tests publish content scv
   */
  @Test
  public void testPublishContentSCV() throws Exception {
    Page page = new Page();
    Exception e = null;
    try {
      publicationService_.publishContentSCV(node_, page, node_.getSession().getUserID());
    } catch (Exception ex) {
      e = ex;
    }
    assertNotNull(e);
    
    e = null;
    try {
      publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
      publicationService_.publishContentSCV(node_, page, node_.getSession().getUserID());
    } catch (Exception ex) {
      e = ex;
    }
    assertNull(e);
  }
  
  /**
   * tests publish content clv
   */
  @Test
  public void testPublishContentCLV() throws Exception {
    Page page = new Page();
    Exception e = null;
    try {
      publicationService_.publishContentCLV(node_, page, node_.getSession().getUserID(),
                                            "CLV1", node_.getSession().getUserID());
    } catch (Exception ex) {
      e = ex;
    }
    assertNotNull(e);
    
    e = null;
    try {
      publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
      publicationService_.publishContentCLV(node_, page, node_.getSession().getUserID(),
                                            "CLV1", node_.getSession().getUserID());
    } catch (Exception ex) {
      e = ex;
    }
    assertNull(e);
  }
  
  /**
   * tests update lifecycle on create page
   */
  @Test
  public void testUpdateLifecycleOnCreatePage() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.updateLifecyleOnCreatePage(page_, node_.getSession().getUserID());
    assertTrue(node_.isNodeType(NodetypeConstant.PUBLICATION_WEBPAGES_PUBLICATION));
  }
  
  /**
   * tests update lifecycle on remove page
   */
  @Test
  public void testUpdateLifecycleOnRemovePage() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.updateLifecycleOnRemovePage(page_, node_.getSession().getUserID());
    if (node_.hasProperty("publication:webPageIDs")) {
      Value[] values = node_.getProperty("publication:webPageIDs").getValues();
      if (values != null) {
        for (Value v : values) {
          assertFalse(v.getString().equals(page_.getPageId()));
        }
      }
    }
  }  

  /**
   * tests update lifecycle on change page
   */
  @Test
  public void testUpdateLifecycleOnChangePage() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.updateLifecyleOnChangePage(page_, node_.getSession().getUserID());
    if (node_.hasProperty("publication:webPageIDs")) {
      Value[] values = node_.getProperty("publication:webPageIDs").getValues();
      if (values != null) {
        for (Value v : values) {
          assertFalse(v.getString().equals(page_.getPageId()));
        }
      }
    }
  }
  
  private void createPortalModel() throws Exception {
    //create portal
    String label = "portal_test";
    String description = "This is new portal for testing";
    portal_ = new PortalConfig();
    portal_.setType("portal");
    portal_.setName("test");
    portal_.setLocale("en");
    portal_.setLabel(label);
    portal_.setDescription(description);
    portal_.setAccessPermissions(new String[]{UserACL.EVERYONE});

    storage_.create(portal_);
    //create pate
    //---------------------
    portlet_ = new Portlet();

    ApplicationState<Portlet> state = 
      new TransientApplicationState<Portlet>("portal#test:/web/presentation/SingleContentViewer", portlet_);
    ApplicationData<Portlet> applicationData = 
      new ApplicationData<Portlet>(null, "portal#test:/web/presentation/SingleContentViewer",
       ApplicationType.PORTLET, state, "portal#test:/web/presentation/SingleContentViewer", 
       "app-title", "app-icon", "app-description", false, true, false,
       "app-theme", "app-wdith", "app-height", new HashMap<String,String>(),
       Collections.singletonList("app-edit-permissions"));

    ContainerData containerData = new ContainerData(null, "cd-id", "cd-name", "cd-icon", "cd-template", "cd-factoryId", 
                                                    "cd-title", "cd-description", "cd-width", "cd-height", 
                                                    Collections.singletonList("cd-access-permissions"), 
                                                    Collections.singletonList((ComponentData) applicationData));
    List<ComponentData> children = Collections.singletonList((ComponentData) containerData);
    
    PageData expectedData = new PageData(null, null, "page-name", null, null, null, "Page Title", null, null, null,
       Collections.singletonList("access-permissions"), children, "", "", "edit-permission", true);

    page_ = new Page(expectedData);
    page_.setTitle("MyTitle");
    page_.setOwnerType(PortalConfig.PORTAL_TYPE);
    page_.setOwnerId("test");
    page_.setName("foo");

    storage_.create(page_);
    
    PortletPreferences p = new PortletPreferences();
    p.setWindowId("portal#test:/web/presentation/SingleContentViewer");
    p.setPreference(new Preference("workspace", node_.getSession().getWorkspace().getName()));
    p.setPreference(new Preference("nodeIdentifier", node_.getUUID()));
    
    storage_.save(p);
    //------------------------------------------
  }
}

