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
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.base.BaseSearchTest;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;

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
public class TestFileSearchServiceConnector extends BaseSearchTest {

  private SearchServiceConnector fileSearch_;

  public void setUp() throws Exception {
    super.setUp();
    applyUserSession("john", "gtn",COLLABORATION_WS);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    fileSearch_ = WCMCoreUtils.getService(FileSearchServiceConnector.class);
  }
  
  public void tearDown() throws Exception {
    NodeIterator iterator = null;
    Node classicPortal = (Node)session.getItem("/sites content/live/classic");
    iterator = classicPortal.getNodes();
    while (iterator.hasNext()) {
      iterator.nextNode().remove();
    }

    Node sharedPortal = (Node)session.getItem("/sites content/live/shared");
    iterator = sharedPortal.getNodes();
    while (iterator.hasNext()) {
      iterator.nextNode().remove();
    }
    session.save();
    super.tearDown();
  }
  
  protected void addDocuments() throws Exception {
    Node classicPortal = (Node)session.getItem("/sites content/live/classic");
    addChildNodes(classicPortal);

    Node sharedPortal = (Node)session.getItem("/sites content/live/shared");
    addChildNodes(sharedPortal);
  }
  
  @Override
  protected void addChildNodes(Node parentNode) throws Exception{
    addFile(parentNode, "file1", "john Smith");
    addFile(parentNode, "file2", "cjohn Felix Anthony Cena");
    addFile(parentNode, "file3", "anthony Hopkins");
  }
  
  private void addFile(Node parentNode, String name, String data) throws Exception {
    Node file = parentNode.addNode(name, "nt:file");
    file.addMixin("exo:sortable");
    file.setProperty("exo:title", name);
    Node content = file.addNode("jcr:content", "nt:resource");
    content.setProperty("jcr:encoding", "UTF-8");
    content.setProperty("jcr:mimeType", "text/html");
    content.setProperty("jcr:lastModified", new Date().getTime());
    content.setProperty("jcr:data", data);
    file.addMixin(NodetypeConstant.EXO_DATETIME);
    file.setProperty("exo:dateCreated",new GregorianCalendar());
    file.setProperty("exo:dateModified",new GregorianCalendar());
    session.save();
  }
    
  public void testSearchSingle() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites, 
                                   0, 20, "title", "asc");
    assertEquals(4, ret.size());//2
  }
  
  public void testSearchSingleWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(3, ret.size());//1
  }
  
  public void testSearchSingleWithLimit() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony~",
                                   sites, 
                                   0, 1, "title", "asc");
    assertEquals(1, ret.size());
  }
  
  public void testSearchMultiple() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony cjohn~",
                                   sites, 
                                   0, 20, "title", "asc");
    assertEquals(2, ret.size());//3
  }
  
  public void testSearchMultipleWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony cjohn~",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(1, ret.size());//2
  }
  
  public void testSearchMultipleWithLimit() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "anthony cjohn~",
                                   sites, 
                                   0, 1, "title", "asc");
    assertEquals(1, ret.size());
  }  

  public void testSearchPhrase() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "\"anthony cena\"",
                                   sites, 
                                   0, 20, "title", "asc");
    assertEquals(2, ret.size());//1
  }

  public void testSearchPhraseWithOffset() throws Exception {
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    Collection<SearchResult> ret 
          = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()), "classic"), "\"anthony cena\"",
                                   sites, 
                                   1, 20, "title", "asc");
    assertEquals(1, ret.size());//0
  }
}
