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
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.publication.lifecycle.simple.SimplePublicationPlugin;
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
public class TestSimplePublicationPlugin extends BaseECMSTestCase {
  
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String TEST = "test";
  private static final String ENROLLED = "enrolled";
  private static final String PUBLISHED = "published";
  private static final String NON_PUBLISHED = "non published";  
  
  private PublicationService publicationService_;
  private PublicationPlugin plugin_;
  private Node node_;
  
  public void setUp() throws Exception {
    super.setUp();
    publicationService_ = WCMCoreUtils.getService(PublicationService.class);
    applySystemSession();
    node_ = session.getRootNode().addNode(TEST);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new SimplePublicationPlugin();
    plugin_.setName("Simple publication");
    plugin_.setDescription("Simple publication");
    publicationService_.addPublicationPlugin(plugin_);
  }
  
  public void tearDown() throws Exception {
    publicationService_.getPublicationPlugins().clear();
    node_.remove();
    session.save();
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

    Exception e = null;
    try {
      plugin_.changeState(node_, "test", context);
    } catch (Exception ex) {
      e = ex;
    }
    assertNotNull(e);
  }

  /**
  * tests getting user info
  */
  public void testGetUserInfo() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertNull(plugin_.getUserInfo(node_, new Locale("en")));
  }

  /**
  * tests getting state image 
  */
  public void testGetStateImage() throws Exception {
    HashMap<String, String> context = new HashMap<String, String>();

    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.changeState(node_, PublicationDefaultStates.DRAFT, context);
    assertNotNull(plugin_.getStateImage(node_, new Locale("en")));
  }
  
  /**
   * tests getLocalizedAndSubstituteLog
   */
  public void testGetLocalizedAndSubstituteLog() throws Exception {
    assertEquals("The web content has been published", plugin_.getLocalizedAndSubstituteMessage(
         new Locale("en"), "PublicationService.SimplePublicationPlugin.changeState.published", new String[]{}));
  }
}
