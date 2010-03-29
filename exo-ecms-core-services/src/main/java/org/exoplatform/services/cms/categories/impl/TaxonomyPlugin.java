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
package org.exoplatform.services.cms.categories.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.categories.impl.TaxonomyConfig.Permission;
import org.exoplatform.services.cms.categories.impl.TaxonomyConfig.Taxonomy;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/*
 * New plugin was created at org.exoplatform.services.cms.taxonomy.impl.TaxonomyPlugin
 * to replace this one
 */
@Deprecated
public class TaxonomyPlugin extends BaseComponentPlugin{	

  private RepositoryService repositoryService_ ;  
  private String baseTaxonomiesPath_ ;  
  private InitParams params_ ;  
  private boolean autoCreateInNewRepository_ = true;  
  /**
   * DMS configuration which used to store informations
   */   
  private DMSConfiguration dmsConfiguration_;

  public TaxonomyPlugin(InitParams params, RepositoryService repositoryService, 
      NodeHierarchyCreator nodeHierarchyCreator, 
      DMSConfiguration dmsConfiguration) throws Exception {
    repositoryService_ = repositoryService ;
    baseTaxonomiesPath_ = nodeHierarchyCreator.getJcrPath(BasePath.EXO_TAXONOMIES_PATH) ;    
    params_ = params ;
    ValueParam valueParam = params_.getValueParam("autoCreateInNewRepository") ;
    if(valueParam !=null) {
      autoCreateInNewRepository_ = Boolean.parseBoolean(valueParam.getValue()) ;
    }   
    dmsConfiguration_ = dmsConfiguration;
  }

  public void init() throws Exception {    
    if(autoCreateInNewRepository_) {
      for(RepositoryEntry repositoryEntry:repositoryService_.getConfig().getRepositoryConfigurations()) {
        importPredefineTaxonomies(repositoryEntry.getName()) ;        
      }
      return ;
    }
    ValueParam param = params_.getValueParam("repository") ;
    String repository = null ;
    if(param == null) {
      repository = repositoryService_.getDefaultRepository().getConfiguration().getName();
    }else {
      repository = param.getValue() ;
    }    
    importPredefineTaxonomies(repository) ;
  }

  public void init(String repository) throws Exception {
    if(!autoCreateInNewRepository_) return ;
    importPredefineTaxonomies(repository) ;
  }

  @SuppressWarnings("unchecked")
  private void importPredefineTaxonomies(String repository) throws Exception {    
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository) ;
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);    
    Session session = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;    
    Node taxonomyHomeNode = (Node)session.getItem(baseTaxonomiesPath_) ;
    //TODO Need remove this code
    if(taxonomyHomeNode.hasProperty("exo:isImportedChildren"))  { 
      session.logout();
      return ; 
    }
    taxonomyHomeNode.setProperty("exo:isImportedChildren",true) ;
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    while(it.hasNext()) {
      TaxonomyConfig config = (TaxonomyConfig)it.next().getObject() ;
      for(Taxonomy taxonomy : config.getTaxonomies()) {
        Node taxonomyNode = Utils.makePath(taxonomyHomeNode, taxonomy.getPath(), "exo:taxonomy", getPermissions(taxonomy.getPermissions())) ;
        if(taxonomyNode.canAddMixin("mix:referenceable")) {
          taxonomyNode.addMixin("mix:referenceable") ;
        }
      }
    }
    taxonomyHomeNode.save();
    session.save();
    session.logout();
  }
  
  public Map getPermissions(List<Permission> permissions) {
    Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
    for (Permission permission : permissions) {
      StringBuilder strPer = new StringBuilder();
      if ("true".equals(permission.getRead()))
        strPer.append(PermissionType.READ);
      if ("true".equals(permission.getAddNode()))
        strPer.append(",").append(PermissionType.ADD_NODE);
      if ("true".equals(permission.getSetProperty()))
        strPer.append(",").append(PermissionType.SET_PROPERTY);
      if ("true".equals(permission.getRemove()))
        strPer.append(",").append(PermissionType.REMOVE);
      permissionsMap.put(permission.getIdentity(), strPer.toString().split(","));
    }
    return permissionsMap;
  }
}
