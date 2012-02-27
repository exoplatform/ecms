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
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.xml.InitParams;
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
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private static final Log LOG  = ExoLogger.getLogger(RelationsServiceImpl.class);
  public RelationsServiceImpl(RepositoryService repositoryService,
      NodeHierarchyCreator nodeHierarchyCreator, InitParams params) {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
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
  private Node getNodeByUUID(String uuid, SessionProvider provider) throws Exception {
    ManageableRepository manageRepo = repositoryService_.getCurrentRepository();
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
  @Deprecated
  public List<Node> getRelations(Node node, String repository, SessionProvider provider) {
    return getRelations(node, provider);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getRelations(Node node, SessionProvider provider) {
    List<Node> rels = new ArrayList<Node>();
    try {
      if(node.hasProperty(RELATION_PROP)) {
        Value[] values = node.getProperty(RELATION_PROP).getValues();
        for (int i = 0; i < values.length; i++) {
          if(getNodeByUUID(values[i].getString(), provider) != null) {
            rels.add(getNodeByUUID(values[i].getString(), provider));
          }
        }
      }
    } catch(Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return rels ;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void removeRelation(Node node, String relationPath, String repository) throws Exception {
    removeRelation(node, relationPath);
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeRelation(Node node, String relationPath) throws Exception {
    List<Value> vals = new ArrayList<Value>();
    if (!"*".equals(relationPath)) {
      SessionProvider provider = SessionProvider.createSystemProvider() ;
      Property relations = node.getProperty(RELATION_PROP);
      if (relations != null) {
        Value[] values = relations.getValues();
        String uuid2Remove = null;
        for (int i = 0; i < values.length; i++) {
          String uuid = values[i].getString();
          Node refNode = getNodeByUUID(uuid, provider);
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
  @Deprecated
  public void addRelation(Node node, String relationPath,String workspace,String repository) throws Exception {
    addRelation(node, relationPath, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public void addRelation(Node node, String relationPath,String workspace) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider() ;
    Session session = getSession(workspace,provider) ;
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
          refNode = getNodeByUUID(uuid, provider) ;
        } catch(ItemNotFoundException ie) {
          removeRelation(node, relationPath) ;
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
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void init(String repository) throws Exception {
    init();
  }
  
  /**
   * {@inheritDoc}
   */
  public void init() throws Exception {
  }

  /**
   * Get session of respository
   * @see                 Session
   * @return              Session
   * @throws Exception
   */
  protected Session getSession() throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    String workspaceName = manageableRepository.getConfiguration().getSystemWorkspaceName();
    return manageableRepository.getSystemSession(workspaceName);
  }

  /**
   * Get session of workspace
   * @param workspace     The name of workspace
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @return              Session
   * @throws Exception
   */
  private Session getSession(String workspace, SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    return provider.getSession(workspace, manageableRepository);
  }
}
