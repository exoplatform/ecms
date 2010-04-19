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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

/**
 * @author Benjamin Mestrallet benjamin.mestrallet@exoplatform.com
 */
public class NewUserListener extends UserEventListener {
  
  private static final String[] perms = {PermissionType.READ, PermissionType.ADD_NODE, 
    PermissionType.SET_PROPERTY, PermissionType.REMOVE };  
  
  private NewUserConfig config_;
  private RepositoryService jcrService_;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private String relativePath_;

  public NewUserListener(RepositoryService jcrService,
                         NodeHierarchyCreator nodeHierarchyCreator, 
                         InitParams params)    throws Exception {
    jcrService_ = jcrService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    config_ = (NewUserConfig) params.getObjectParamValues(NewUserConfig.class).get(0);
    relativePath_ = params.getValueParam("relativePath").getValue();
  }
  
  public void preSave(User user, boolean isNew)
      throws Exception {        
    String userName = user.getUserName();
    prepareSystemWorkspace(userName);
  }
  
  private void prepareSystemWorkspace(String userName) throws Exception {
    Session session = null;    
    //Manage production workspace
    List<RepositoryEntry> repositories = jcrService_.getConfig().getRepositoryConfigurations() ;
    for(RepositoryEntry repo : repositories) {
      try {              
        String defaultWorkspaceName = jcrService_.getDefaultRepository().getConfiguration().getDefaultWorkspaceName() ;
        session = jcrService_.getRepository(repo.getName()).getSystemSession(defaultWorkspaceName);
        Node usersHome = (Node) session.getItem(
            nodeHierarchyCreator_.getJcrPath(BasePath.CMS_USERS_PATH));
        initSystemData(usersHome, userName) ;
        session.save();
        session.logout();
      } catch (RepositoryException re){
        session.logout();
        return;
      }
    }   
  }
  
  private void initSystemData(Node usersHome, String userName) throws Exception{           
    Node userHome = usersHome.getNode(userName) ;
    Node queriesHome =  userHome.getNode(relativePath_) ;           
    boolean userFound = false;
    NewUserConfig.User templateConfig = null;
    for (NewUserConfig.User userConfig : config_.getUsers()) {      
      String currentName = userConfig.getUserName();            
      if (config_.getTemplate().equals(currentName))  templateConfig = userConfig;
      if (currentName.equals(userName)) {
        List<NewUserConfig.Query> queries = userConfig.getQueries();
        importQueries(queriesHome, queries);
        userFound = true;
        break;
      }
    }
    if (!userFound) {
      //use template conf
      List<NewUserConfig.Query> queries = templateConfig.getQueries();
      importQueries(queriesHome, queries);
    }
    usersHome.save();   
  }
  
  public void importQueries(Node queriesHome, List<NewUserConfig.Query> queries) throws Exception {
    QueryManager manager = queriesHome.getSession().getWorkspace().getQueryManager();
    for (NewUserConfig.Query query:queries) {      
      String queryName = query.getQueryName();
      String language = query.getLanguage();
      String statement = query.getQuery();
      Query queryNode = manager.createQuery(statement, language);
      String absPath = queriesHome.getPath() + "/" + queryName;
      Node node = queryNode.storeAsNode(absPath);
      if (!node.isNodeType("exo:datetime")) {
        node.addMixin("exo:datetime");        
      }
      node.setProperty("exo:dateCreated",new GregorianCalendar()) ;
      node.getSession().save();
    }    
  }

  public void preDelete(User user) {
  }
}