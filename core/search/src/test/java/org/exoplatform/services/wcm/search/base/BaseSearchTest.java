/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.base;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.component.test.*;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.search.*;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 14, 2009
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-search-configuration.xml")
})
public class BaseSearchTest extends BaseECMSTestCase {
  
  protected QueryCriteria queryCriteria = new QueryCriteria();
  protected SiteSearchService siteSearchService;
  protected WCMPublicationService wcmPublicationService;
  protected WebpagePublicationPlugin publicationPlugin ;
  protected UserPortalConfigService userPortalConfigService;
  protected final String searchKeyword = "This is";
  protected final String duplicationSearchKeyword = "duplication searchKey";
  protected SessionProvider sessionProvider;
  protected int seachItemsPerPage = 100;
  protected static int numberOfRunTests = 0;

  public void setUp() throws Exception {
    super.setUp();
    siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);
    userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);

    publicationPlugin = new DumpPublicationPlugin();
    publicationPlugin.setName(DumpPublicationPlugin.LIFECYCLE_NAME);
    wcmPublicationService.addPublicationPlugin(publicationPlugin);
    applySystemSession();
    addDocuments();
    numberOfRunTests++;
  }

  protected void addDocuments() throws Exception {
    Node classicPortal = getNode("sites content/live/classic/web contents");
    addChildNodes(classicPortal);

    Node sharedPortal = getNode("sites content/live/shared/documents");
    addChildNodes(sharedPortal);
    Node acmePortal = getNode("sites content/live/acme");
    if (numberOfRunTests == 0) {
      // Populate 20 webContent nodes under classic site without being enrolled in publication lifecycle
      populateAdditionalSearchData(acmePortal, "web contents", 20);
      // Populate 101 document nodes under classic site without being enrolled in publication lifecycle
      populateAdditionalSearchData(acmePortal, "documents", 101);
    }
  }
  
  protected Node getNode(String path) throws Exception {
    Node root = session.getRootNode();
    for (String name : path.split("/")) {
      if (root.hasNode(name)) {
        root = root.getNode(name);
      } else {
        Node tmp = root.addNode(name);
        session.save();
        root = tmp;
      }
    }
    return root;
  }

  protected void addChildNodes(Node parentNode)throws Exception{
  }

  /*
   * Create additional data for search under a specific site and a specific node.
   */
  protected void populateAdditionalSearchData(Node siteNode, String parentNode, int nodesCount) {}

  public void tearDown() throws Exception {
    NodeIterator iterator = null;
    if (session.isLive()) {
      if (session.itemExists("/sites content/live/classic/web contents")) {
        Node classicPortal = (Node) session.getItem("/sites content/live/classic/web contents");
        iterator = classicPortal.getNodes();
        while (iterator.hasNext()) {
          iterator.nextNode().remove();
        }
      }
      if (session.itemExists("/sites content/live/shared/documents")) {
        Node sharedPortal = (Node) session.getItem("/sites content/live/shared/documents");
        iterator = sharedPortal.getNodes();
        while (iterator.hasNext()) {
          iterator.nextNode().remove();
        }
      }
      session.save();
      super.tearDown();
    }
  }
  
  public void testNone() throws Exception{
    //empty test function to avoid failure of BaseSearchTest with no test case when build
  }
  
}
