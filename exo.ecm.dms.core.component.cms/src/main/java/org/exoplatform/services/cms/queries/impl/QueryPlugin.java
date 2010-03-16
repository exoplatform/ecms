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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;

public class QueryPlugin extends BaseComponentPlugin {

  private static String STATEMENT = "jcr:statement".intern() ;
  private static String LANGUAGE = "jcr:language".intern() ;
  private static String PERMISSIONS = "exo:accessPermissions".intern() ;
  private static String CACHED_RESULT = "exo:cachedResult".intern() ;

  private InitParams params_ ;
  private boolean autoCreateInNewRepository_ = false;  
  private String repository_ ;  
  private RepositoryService repositoryService_ ;
  private DMSConfiguration dmsConfiguration_;

  public QueryPlugin(RepositoryService repositoryService, InitParams params, 
      DMSConfiguration dmsConfiguration) throws Exception {
    params_ = params ;    
    repositoryService_ = repositoryService ;
    ValueParam autoInitParam = params.getValueParam("autoCreateInNewRepository") ;    
    if(autoInitParam !=null) {
      autoCreateInNewRepository_ = Boolean.parseBoolean(autoInitParam.getValue()) ;
    }
    ValueParam param = params.getValueParam("repository") ;
    if(param !=null) {
      repository_ = param.getValue();
    }        
    dmsConfiguration_ = dmsConfiguration;
  } 

  public void init(String basedQueriesPath) throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ; 
    Session session = null ;
    if(autoCreateInNewRepository_) {      
      for(RepositoryEntry entry:repositoryService_.getConfig().getRepositoryConfigurations()) {
        session = getSession(entry.getName()) ;
        Node queryHomeNode = (Node)session.getItem(basedQueriesPath);
        while(it.hasNext()) {
          QueryData data = (QueryData)it.next().getObject() ;
          addQuery(queryHomeNode,data) ;
        }
        queryHomeNode.save();
        session.save();
        session.logout();
      }
    } else {
      session = getSession(repository_) ;
      Node queryHomeNode = (Node)session.getItem(basedQueriesPath);
      while(it.hasNext()) {
        QueryData data = (QueryData)it.next().getObject() ;
        addQuery(queryHomeNode,data) ;
      }
      queryHomeNode.save();
      session.save();
      session.logout();      
    }   
  }
  
  public void init(String repository,String baseQueriesPath) throws Exception {          
    if(!autoCreateInNewRepository_) return ; 
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    Session session = getSession(repository) ;
    Node queryHomeNode = (Node)session.getItem(baseQueriesPath) ;
    while(it.hasNext()){
      QueryData data = (QueryData)it.next().getObject() ;      
      addQuery(queryHomeNode, data) ;
    }
    queryHomeNode.save();
    session.save();
    session.logout();    
  }
  
  private Session getSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository) ;
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;    
  }

  private void addQuery(Node queryHome, QueryData data) throws Exception {
    if(queryHome.hasNode(data.getName())) return ;    
    ValueFactory vt = queryHome.getSession().getValueFactory() ;
    Node queryNode = queryHome.addNode(data.getName(), "nt:query");
    
    if (!queryNode.isNodeType("exo:datetime")) {
      queryNode.addMixin("exo:datetime");        
    }
    queryNode.setProperty("exo:dateCreated",new GregorianCalendar()) ;
    
    queryNode.addMixin("mix:sharedQuery") ;
    queryNode.setProperty(STATEMENT, data.getStatement()) ;
    queryNode.setProperty(LANGUAGE, data.getLanguage()) ;
    List<String> queryPermissions = data.getPermissions() ;
    Value[] vls = new Value[queryPermissions.size()];
    int i = 0;
    for(String per : queryPermissions) {
      Value vl = vt.createValue(per) ;
      vls[i] = vl ;
      i++ ;
    }
    queryNode.setProperty(PERMISSIONS, vls) ;
    queryNode.setProperty(CACHED_RESULT, data.getCacheResult()) ;        
  }
}
