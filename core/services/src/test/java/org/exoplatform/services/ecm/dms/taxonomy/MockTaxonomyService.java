/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.taxonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Mar 16, 2011
 * 6:05:28 PM  
 */
public class MockTaxonomyService extends TaxonomyServiceImpl {
  
  private RepositoryService      repositoryService_;

  private final String           SQL_QUERY = "Select * from exo:taxonomyLink where jcr:path like '$0/%' and exo:uuid = '$1' "
                                               + "order by exo:dateCreated DESC";
  private SessionProviderService providerService_;
  
  public MockTaxonomyService(InitParams initParams, SessionProviderService providerService,
      NodeHierarchyCreator nodeHierarchyCreator, RepositoryService repoService,
      LinkManager linkManager, DMSConfiguration dmsConfiguration) throws Exception {
    super(initParams, providerService, null, nodeHierarchyCreator, repoService, linkManager, dmsConfiguration);
    repositoryService_ = repoService;
    providerService_ = providerService;
  }
  
  public boolean hasCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    List<Node> listCate = getCategories(node, taxonomyName, system);
    if (listCate != null && listCate.size() > 0)
      return true;
    return false;
  }
  
  public List<Node> getAllCategories(Node node, boolean system) throws RepositoryException {
    List<Node> listCategories = new ArrayList<Node>();
    List<Node> allTrees = getAllTaxonomyTrees(system);
    for (Node tree : allTrees) {
      List<Node> categories = getCategories(node, tree.getName(), system);
      for (Node category : categories) listCategories.add(category);
    }
    return listCategories;
  }
  
  public List<Node> getCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    List<Node> listCate = new ArrayList<Node>();
    Session session = null;
    try {
      if (node.isNodeType("mix:referenceable")) {
        Node rootNodeTaxonomy = getTaxonomyTree(taxonomyName, system);
        if (rootNodeTaxonomy != null) {
          String sql = null;
          sql = StringUtils.replace(SQL_QUERY, "$0", rootNodeTaxonomy.getPath());
          sql = StringUtils.replace(sql, "$1", node.getUUID());
          session = providerService_.getSystemSessionProvider(null)
                                    .getSession(rootNodeTaxonomy.getSession()
                                                                .getWorkspace()
                                                                .getName(),
                                                repositoryService_.getCurrentRepository());
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          Query query = queryManager.createQuery(sql, Query.SQL);
          QueryResult result = query.execute();
          NodeIterator iterate = result.getNodes();
          while (iterate.hasNext()) {
            Node parentCate = iterate.nextNode().getParent();
            listCate.add(parentCate);
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(e);
    }
    return listCate;
  }

}
