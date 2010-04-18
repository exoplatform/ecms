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
package org.exoplatform.services.cms.relations.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * @author monica franceschini
 */

public class RelationsServiceImpl implements RelationsService, Startable {
  private static final String RELATION_MIXIN = "exo:relationable";
  private static final String RELATION_PROP = "exo:relation";

  private RepositoryService repositoryService_;
  String repositories_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private static final Log LOG  = ExoLogger.getLogger(RelationsServiceImpl.class);
  public RelationsServiceImpl(RepositoryService repositoryService,
      NodeHierarchyCreator nodeHierarchyCreator, InitParams params) {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositories_ = params.getValueParam("repositories").getValue();
  }
  
  /**
   * {@inheritDoc}
   */
  public boolean hasRelations(Node node) throws Exception {
    if (node.isNodeType(RELATION_MIXIN)) return true;
    return false;

  }
  
  /**
   * Get node by UUID
   * @param uuid          The specified UUI. 
   * @param repository    The name of repository 
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @return              Node with specified UUID 
   * @throws Exception
   */
  private Node getNodeByUUID(String uuid, String repository,SessionProvider provider) throws Exception {   
    ManageableRepository manageRepo = repositoryService_.getRepository(repository) ;
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    for(String ws : workspaces) {
      try{
        return provider.getSession(ws,manageRepo).getNodeByUUID(uuid) ;        
      } catch(Exception e) {
        continue ;
      }      
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getRelations(Node node, String repository, SessionProvider provider) {
    List<Node> rels = new ArrayList<Node>();
    try {
      if(node.hasProperty(RELATION_PROP)) {
        Value[] values = node.getProperty(RELATION_PROP).getValues();
        for (int i = 0; i < values.length; i++) {
          if(getNodeByUUID(values[i].getString(), repository, provider) != null) {
            rels.add(getNodeByUUID(values[i].getString(), repository, provider));
          }
        }
      }
    } catch(Exception e) {      
    }
    return rels ;    
  }

  /**
   * {@inheritDoc}
   */
  public void removeRelation(Node node, String relationPath, String repository) throws Exception {
    List<Value> vals = new ArrayList<Value>();
    if (!"*".equals(relationPath)) {
      SessionProvider provider = SessionProvider.createSystemProvider() ;
      Property relations = node.getProperty(RELATION_PROP);
      if (relations != null) {
        Value[] values = relations.getValues();
        String uuid2Remove = null;
        for (int i = 0; i < values.length; i++) {
          String uuid = values[i].getString();
          Node refNode = getNodeByUUID(uuid, repository,provider);
          if(refNode == null) continue ;
          if (refNode.getPath().equals(relationPath)) uuid2Remove = uuid;
          else vals.add(values[i]);
        }
        if (uuid2Remove == null) return;
      }
      provider.close();
    }
    if(vals.size() == 0) node.removeMixin(RELATION_MIXIN);
    else node.setProperty(RELATION_PROP, vals.toArray(new Value[vals.size()]));
    node.save() ;
  }

  /**
   * {@inheritDoc}
   */
  public void addRelation(Node node, String relationPath,String workpace,String repository) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider() ;
    Session session = getSession(repository,workpace,provider) ;
    Node catNode = (Node) session.getItem(relationPath); 
    if(!catNode.isNodeType("mix:referenceable")) {
      catNode.addMixin("mix:referenceable") ;
      catNode.save() ;
      session.save() ;
    }      
    Value value2add = session.getValueFactory().createValue(catNode);
    if (!node.isNodeType(RELATION_MIXIN)) {
      node.addMixin(RELATION_MIXIN);    
      node.setProperty(RELATION_PROP, new Value[] {value2add});
      node.save() ;
      session.save() ;
    } else {
      List<Value> vals = new ArrayList<Value>();
      Value[] values = node.getProperty(RELATION_PROP).getValues();
      for (int i = 0; i < values.length; i++) {
        Value value = values[i];
        String uuid = value.getString();
        Node refNode = null ;
        try {
//          refNode = session.getNodeByUUID(uuid);
          refNode = getNodeByUUID(uuid, repository, provider) ;
        } catch(ItemNotFoundException ie) {
          removeRelation(node, relationPath, repository) ;
          continue ;
        }
        if(refNode.getPath().equals(relationPath)) return;
        vals.add(value);
      }
      vals.add(value2add);
      node.setProperty(RELATION_PROP, vals.toArray(new Value[vals.size()]));
      node.save() ;
      session.save() ;
      session.logout();
      provider.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    Session session = null;
    Node relationsHome = null;
    try {
      String relationPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_PUBLICATIONS_PATH);
      if (relationPath == null) throw new IllegalArgumentException();
      String[] repositories = repositories_.split(",") ;
      for(String repo : repositories) {
        session = getSession(repo.trim());
        relationsHome = (Node) session.getItem(relationPath);
        for (NodeIterator iterator = relationsHome.getNodes(); iterator.hasNext();) {
          Node rel = iterator.nextNode();
          rel.addMixin("mix:referenceable");
        }
        relationsHome.save();
        session.save();
      }      
    } catch (IllegalArgumentException e) {
      LOG.error("Cannot find path by alias " + BasePath.CMS_PUBLICATIONS_PATH);
    } catch (Exception e) {
      if(session !=null && session.isLive()) session.logout();
    } finally {
      if(session != null) session.logout();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }

  /**
   * {@inheritDoc}
   */
  public void init(String repository) throws Exception {
    Session session = getSession(repository);
    String relationPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_PUBLICATIONS_PATH);
    if (relationPath == null) throw new IllegalArgumentException();
    try {            
      Node relationsHome = (Node) session.getItem(relationPath);
      for (NodeIterator iterator = relationsHome.getNodes(); iterator.hasNext();) {
        Node rel = iterator.nextNode();
        rel.addMixin("mix:referenceable");
      }
      relationsHome.save(); 
    } catch (IllegalArgumentException e) {
      LOG.error("Cannot find path by alias " + BasePath.CMS_PUBLICATIONS_PATH);
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    } finally {
      if(session != null) session.logout();
    }
  }
  
  /**
   * Get session of respository
   * @param repository    The name of repository
   * @see                 Session 
   * @return              Session
   * @throws Exception
   */
  protected Session getSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    String workspaceName = manageableRepository.getConfiguration().getSystemWorkspaceName();
    return manageableRepository.getSystemSession(workspaceName);
  }
  
  /**
   * Get session of workspace
   * @param repository    The name of repository   
   * @param workspace     The name of workspace
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @return              Session
   * @throws Exception
   */
  private Session getSession(String repository,String workspace,SessionProvider provider) throws Exception{
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository) ;
    return provider.getSession(workspace,manageableRepository) ;
  }
}
