/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.connector;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import javax.jcr.Node;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.base.BaseSearchTest;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;

import org.mockito.Mockito;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 5, 2013  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-search-configuration.xml")
})
public class TestDocumentSearchServiceConnector extends BaseSearchTest {

  private SearchServiceConnector documentSearch_;

  public void setUp() throws Exception {
    super.setUp();
    applyUserSession("john", "gtn",COLLABORATION_WS);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    documentSearch_ = WCMCoreUtils.getService(DocumentSearchServiceConnector.class);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected void addChildNodes(Node parentNode) throws Exception{
    super.addChildNodes(parentNode);
    Node article1 = parentNode.addNode("article1", "exo:article");
    article1.setProperty("exo:title", "john");
    article1.setProperty("exo:text", "Smith");
    article1.addMixin(NodetypeConstant.EXO_DATETIME);
    article1.setProperty("exo:dateCreated",new GregorianCalendar());
    article1.setProperty("exo:dateModified",new GregorianCalendar());
    Node article2 = parentNode.addNode("article2", "exo:article");
    article2.setProperty("exo:title", "cjohn");
    article2.setProperty("exo:text", "Felix Anthony Cena");
    article2.addMixin(NodetypeConstant.EXO_DATETIME);
    article2.setProperty("exo:dateCreated",new GregorianCalendar());
    article2.setProperty("exo:dateModified",new GregorianCalendar());
    Node article3 = parentNode.addNode("article3", "exo:article");
    article3.setProperty("exo:title", "anthony");
    article3.setProperty("exo:text", "Hopkins");
    article3.addMixin(NodetypeConstant.EXO_DATETIME);
    article3.setProperty("exo:dateCreated",new GregorianCalendar());
    article3.setProperty("exo:dateModified",new GregorianCalendar());
    Node article4 = parentNode.addNode("article4", "exo:article");
    article4.setProperty("exo:title", "Albert Einstein");
    article4.setProperty("exo:text", "Hopkins");
    session.save();

    article4 = (Node) session.getItem(article4.getPath());

    assertFalse(article4.hasProperty("exo:dateCreated"));
    assertFalse(article4.hasProperty("exo:dateModified"));
    assertFalse(article4.hasProperty("exo:lastModifiedDate"));
  }

  /**
   * Test if returned date switch result is consistent
   * @throws Exception
   */
  public void testSearchDate() throws Exception {
    long calendarBeforeSearch = Calendar.getInstance().getTimeInMillis();

    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");

    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "hopkins~",
                                   sites,
                                   0, 20, "title", "asc");
    assertEquals(4, ret.size());//2

    boolean matchFound = false;

    for (SearchResult searchResult : ret) {
      if(searchResult.getTitle().equals("Albert Einstein")) {
        // Search result don't have exo:dateModified and exo:dateCreated properties
        // The date should be added with a fake instance == now
        assertTrue("Retuned search result has an incoherent modified date", searchResult.getDate() > calendarBeforeSearch);
        matchFound = true;
      } else {
        // Search result have exo:dateModified and exo:dateCreated properties
        // the modification date should be before running this test
        assertTrue("Retuned search result has an incoherent modified date", searchResult.getDate() < calendarBeforeSearch);
      }
    }

    assertTrue("'Albert Einstein' content was not matched", matchFound);
  }
    
  public void testSearchSingle() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites,
                                   0, 20, "title", "asc");
    assertEquals(4, ret.size());//2
  }
  
  public void testSearchSingleWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(3, ret.size());//1
  }
  
  public void testSearchSingleWithLimit() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites, 
                                   0, 1, "title", "asc");
    assertEquals(1, ret.size());
  }

  public void testSearchMultiple() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony Felix~",
                                   sites, 
                                   0, 20, "title", "asc");
    assertEquals(2, ret.size());//3
  }
  
  public void testSearchMultipleWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony Felix~",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(1, ret.size());//1
    // Check when offset is greater than results size
    ret
            = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony Felix~",
            sites,
            5, 20, "title", "asc");
    assertNotNull(ret);// Should not fail
    assertEquals(0, ret.size());//return 0
  }
  
  public void testSearchMultipleWithLimit() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony Felix~",
                                   sites, 
                                   0, 1, "title", "asc");
    assertEquals(1, ret.size());
  }  
  

  public void testSearchPhrase() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "\"anthony cena~\"",
                                   sites, 
                                   0, 20, "title", "asc");
    assertEquals(2, ret.size());//1
  }
  
  public void testSearchPhraseWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "\"anthony cena\"",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(1, ret.size());//0
  }
  
}
