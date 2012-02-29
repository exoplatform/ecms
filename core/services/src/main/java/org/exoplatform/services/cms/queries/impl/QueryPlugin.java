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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class QueryPlugin extends BaseComponentPlugin {

  private static String STATEMENT = "jcr:statement" ;
  private static String LANGUAGE = "jcr:language" ;
  private static String PERMISSIONS = "exo:accessPermissions" ;
  private static String CACHED_RESULT = "exo:cachedResult" ;

  private InitParams params_ ;
  private boolean autoCreateInNewRepository_ = false;
  private RepositoryService repositoryService_ ;
  private DMSConfiguration dmsConfiguration_;
  private Set<String> configuredQueries_;

  public QueryPlugin(RepositoryService repositoryService, InitParams params,
      DMSConfiguration dmsConfiguration) throws Exception {
    params_ = params ;
    repositoryService_ = repositoryService ;
    ValueParam autoInitParam = params.getValueParam("autoCreateInNewRepository") ;
    if(autoInitParam !=null) {
      autoCreateInNewRepository_ = Boolean.parseBoolean(autoInitParam.getValue()) ;
    }
    dmsConfiguration_ = dmsConfiguration;
  }

  public void init(String basedQueriesPath) throws Exception {
    configuredQueries_ = new HashSet<String>();
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    Session session = null ;
    if(autoCreateInNewRepository_) {
      session = getSession();
      Node queryHomeNode = (Node)session.getItem(basedQueriesPath);
      while(it.hasNext()) {
        QueryData data = (QueryData)it.next().getObject() ;
        addQuery(queryHomeNode,data);
      }
      queryHomeNode.save();
      session.save();
      session.logout();
    } else {
      session = getSession() ;
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

  @Deprecated
  public void init(String repository,String baseQueriesPath) throws Exception {
    init(baseQueriesPath);
  } 

  private Session getSession() throws Exception {
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    return sessionProvider.getSession(dmsRepoConfig.getSystemWorkspace(), 
        repositoryService_.getCurrentRepository()) ;
  }

  private void addQuery(Node queryHome, QueryData data) throws Exception {
    configuredQueries_.add(data.getName());
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
  
  public Set<String> getAllConfiguredQueries() {
    return configuredQueries_;
  }
}
