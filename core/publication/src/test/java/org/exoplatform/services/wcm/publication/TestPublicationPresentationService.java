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

import javax.jcr.Node;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationPresentationService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 26, 2012  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestPublicationPresentationService extends BaseECMSTestCase {
  
  private static final String TEST = "test";
  
  private PublicationPresentationService publicationPresentationService_;
  private PublicationService publicationService_;
  private PublicationPlugin plugin_;
  private Node node_;
  
  public void setUp() throws Exception {
    super.setUp();
    publicationPresentationService_ = WCMCoreUtils.getService(PublicationPresentationService.class);
    publicationService_ = WCMCoreUtils.getService(PublicationService.class);
    applySystemSession();
    node_ = session.getRootNode().addNode(TEST);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new DumpPublicationPlugin();
    plugin_.setName("Simple");
    plugin_.setDescription("Simple");
    publicationService_.addPublicationPlugin(plugin_);
  }
  
  public void tearDown() throws Exception {
    publicationService_.getPublicationPlugins().clear();
    node_.remove();
    session.save();
    super.tearDown();
  }

  /**
   * tests get State UI 
   * add 1 publication plugins and check if the total plugins number is 1
   */
  public void testGetStateUI() throws Exception {
    Exception e = null;
    try {
      publicationPresentationService_.getStateUI(node_, null);
    } catch (NotInPublicationLifecycleException ex) {
      e = ex;
    }
    assertNotNull(e);
    
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertNull(publicationPresentationService_.getStateUI(node_, null));
  }
}