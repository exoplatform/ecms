/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.services.cms.queries.impl;

import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * @author Benjamin Mestrallet benjamin.mestrallet@exoplatform.com
 */
public class NewUserListener extends UserEventListener {
  
  private static final Logger LOG = LoggerFactory.getLogger(NewUserListener.class);

  private NewUserConfig config_;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private RepositoryService repositoryService_ ;
  private String relativePath_;

  public NewUserListener(RepositoryService repositoryService,
                         NodeHierarchyCreator nodeHierarchyCreator,
                         InitParams params)    throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositoryService_ = repositoryService;
    config_ = params.getObjectParamValues(NewUserConfig.class).get(0);
    relativePath_ = params.getValueParam("relativePath").getValue();
  }

  public void preSave(User user, boolean isNew)
      throws Exception {
    initSystemData(user.getUserName());
  }

  private void initSystemData(String userName) throws Exception{
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, userName);
    if (userNode.hasNode(relativePath_)) {
      Node queriesHome =  userNode.getNode(relativePath_) ;
      boolean userFound = false;
      NewUserConfig.User templateConfig = null;
      for (NewUserConfig.User userConfig : config_.getUsers()) {
        String currentName = userConfig.getUserName();
        if (config_.getTemplate().equals(currentName))  templateConfig = userConfig;
        if (currentName.equals(userName)) {
          List<NewUserConfig.Query> queries = userConfig.getQueries();
          importQueries(queriesHome, queries, userNode.getSession().getWorkspace().getName());
          userFound = true;
          break;
        }
      }
      if (!userFound) {
        //use template conf
        List<NewUserConfig.Query> queries = templateConfig.getQueries();
        importQueries(queriesHome, queries);
      }
    } else {
      LOG.debug("The userNode "+userNode.getName()+" doesn't have a child node named : "+relativePath_+". The feature 'StoredQueries' will be ignored ");
    }
  }

  public void importQueries(Node queriesHome, List<NewUserConfig.Query> queries) throws Exception {
    importQueries(queriesHome, queries, queriesHome.getSession().getWorkspace().getName());
  }
  
  public void importQueries(Node queriesHome, List<NewUserConfig.Query> queries, 
      String workspaceName) throws Exception {
    QueryManager manager = getSession(workspaceName).getWorkspace().getQueryManager();
    for (NewUserConfig.Query query:queries) {
      String queryName = query.getQueryName();
      String language = query.getLanguage();
      String statement = query.getQuery();
      Query queryNode = manager.createQuery(statement, language);
      String absPath = queriesHome.getPath() + "/" + queryName;
      if (queriesHome.getSession().itemExists(absPath)) {
        continue;
      }
      Node node = queryNode.storeAsNode(absPath);
      if (!node.isNodeType("exo:datetime")) {
        node.addMixin("exo:datetime");
      }
      
      if (!node.isNodeType(NodetypeConstant.EXO_HIDDENABLE)) {
        node.addMixin(NodetypeConstant.EXO_HIDDENABLE);
      }
      
      if (!node.isNodeType("exo:owneable")) {
        node.addMixin("exo:owneable");
      }
      node.setProperty("exo:dateCreated",new GregorianCalendar()) ;
      node.getSession().save();
    }
  }
  
  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    return sessionProvider.getSession(workspaceName, repositoryService_.getCurrentRepository()) ;
  }

}
