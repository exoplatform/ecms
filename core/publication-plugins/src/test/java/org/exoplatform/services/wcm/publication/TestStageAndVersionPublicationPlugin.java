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
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

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
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PageData;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 24, 2012  
 */

@ConfiguredBy({
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
  
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    publicationService_ = WCMCoreUtils.getService(PublicationService.class);
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
    publicationService_.getPublicationPlugins().clear();
    node_.remove();
    session.save();
    storage_.remove(portal_);
  }

  /**
   * tests changing state for a node 
   */
  @Test
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
  @Test
  public void testGetUserInfo() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertNull(plugin_.getUserInfo(node_, new Locale("en")));
  }

  /**
   * tests getLocalizedAndSubstituteLog
   */
  @Test
  public void testGetLocalizedAndSubstituteLog() throws Exception {
    assertEquals("The content is published", plugin_.getLocalizedAndSubstituteMessage(
         new Locale("en"), "PublicationService.StageAndVersionPublicationPlugin.changeState.published", new String[]{}));
  }
  
  /**
   * tests publish content to scv
   */
  @Test
  public void testPublishContentToSCV() throws Exception {
    Exception e = null;
    try {
      plugin_.publishContentToSCV(node_, page_, "test");
    } catch (Exception ex) {
      e = ex;
    }
    assertNull(e);
  }
  
  /**
   * tests publish content to CLV
   */
  @Test
  public void testPublishContentToCLV() throws Exception {
    plugin_.publishContentToCLV(node_, page_, "portal#test:/web/presentation/SingleContentViewertemp", 
                                "test", node_.getSession().getUserID());
    assertEquals(WCMCoreUtils.getRepository().getConfiguration().getName(),
                 storage_.getPortletPreferences("portal#test:/web/presentation/SingleContentViewertemp").
                 getPreference("repository").getValues().get(0));

    plugin_.publishContentToCLV(node_, page_, ID, 
                                "test", node_.getSession().getUserID());
    Preference p = storage_.getPortletPreferences(ID).getPreference("contents");
    assertTrue(p.getValues().contains(node_.getPath()));
  }
  
  /**
   * tests suspend published content from page
   */
  @Test
  public void testSuspendPublishedContentFromPage() throws Exception {
    plugin_.publishContentToCLV(node_, page_, ID, 
                                "test", node_.getSession().getUserID());
    Preference p = storage_.getPortletPreferences(ID).getPreference("contents");
    assertTrue(p.getValues().contains(node_.getPath()));
    
    plugin_.suspendPublishedContentFromPage(node_, page_, node_.getSession().getUserID());
    p = storage_.getPortletPreferences(ID).getPreference("contents");
    assertFalse(p.getValues().contains(node_.getPath()));    
  }
  
  /**
   * tests update lifecycle on change content 
   */
  @Test
  public void testUpdateLifeCycleOnChangeContent() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.updateLifecyleOnChangeContent(node_, node_.getSession().getUserID());
    assertEquals(PublicationDefaultStates.DRAFT, node_.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString());
    plugin_.updateLifecyleOnChangeContent(node_, node_.getSession().getUserID(), PublicationDefaultStates.PUBLISHED);
    assertEquals(PublicationDefaultStates.PUBLISHED, node_.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).
                 getString());
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
      new TransientApplicationState<Portlet>(ID, portlet_);
    ApplicationData<Portlet> applicationData = 
      new ApplicationData<Portlet>(null, ID,
       ApplicationType.PORTLET, state, ID, 
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
    p.setWindowId(ID);
    p.setPreference(new Preference("workspace", node_.getSession().getWorkspace().getName()));
    p.setPreference(new Preference("nodeIdentifier", node_.getUUID()));
    p.setPreference(new Preference("mode", "ManualViewerMode"));
    p.setPreference(new Preference("contents", "def"));
    p.setPreference(new Preference("folderPath", "abc"));
    storage_.save(p);
    //------------------------------------------
  }
}
