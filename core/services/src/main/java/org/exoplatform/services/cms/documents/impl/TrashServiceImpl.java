/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Oct 6, 2009 3:39:53 AM
 */
public class TrashServiceImpl implements TrashService {

  final static public String EXO_TOTAL = "exo:total".intern();
  final static public String MIX_REFERENCEABLE = "mix:referenceable";
  final static public String TAXONOMY_LINK   = "exo:taxonomyLink";
  final static public String UUID         = "exo:uuid";
  final static public String SYMLINK      = "exo:symlink";
  final static public String EXO_WORKSPACE = "exo:workspace";
  final static public String EXO_TARGETWS = "exo:targetWorkspace";
  final static public String EXO_TARGETPATH = "exo:targetPath";

  private RepositoryService repositoryService;
  private LinkManager linkManager;
  private TaxonomyService taxonomyService_;

  public TrashServiceImpl(RepositoryService repositoryService,
      LinkManager linkManager, TaxonomyService taxonomyService)
  throws Exception {
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
    this.taxonomyService_ = taxonomyService;
  }

  /**
   * {@inheritDoc}
   */
  public void moveToTrash(Node node, String trashPath, String trashWorkspace,
      String repository, SessionProvider sessionProvider)
  throws Exception {
  	moveToTrash(node, trashPath, trashWorkspace, repository, sessionProvider, 0);
  }
  	
  public void moveToTrash(Node node, String trashPath, String trashWorkspace,
      String repository, SessionProvider sessionProvider, int deep)
  throws Exception {
  	

    String nodeName = node.getName();
    Session nodeSession = node.getSession();
    String nodeWorkspaceName = nodeSession.getWorkspace().getName();
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    List<Node> categories = taxonomyService_.getAllCategories(node);
    String nodeUUID = node.isNodeType(MIX_REFERENCEABLE) ? node.getUUID() : null;
    if (node.isNodeType(SYMLINK)) nodeUUID = null;
    String taxonomyLinkUUID = node.isNodeType(TAXONOMY_LINK) ? node.getProperty(UUID).getString() : null;
    String taxonomyLinkWS = node.isNodeType(TAXONOMY_LINK) ? node.getProperty(EXO_WORKSPACE).getString() : null;

    if (!node.isNodeType(EXO_RESTORE_LOCATION)) {
      node.addMixin(EXO_RESTORE_LOCATION);
      node.setProperty(RESTORE_PATH, fixRestorePath(node.getPath()));
      node.setProperty(RESTORE_WORKSPACE, nodeWorkspaceName);
      nodeSession.save();

      ManageableRepository manageableRepository 
      = repositoryService.getRepository(repository);
      Session trashSession = sessionProvider.getSession(trashWorkspace,
          manageableRepository);
      String actualTrashPath = trashPath	+ (trashPath.endsWith("/") ? "" : "/") + fixRestorePath(nodeName);
      if (trashSession.getWorkspace().getName().equals(
          nodeSession.getWorkspace().getName())) {
        trashSession.getWorkspace().move(node.getPath(),
            actualTrashPath);
      } else {
        //clone node in trash folder
        trashSession.getWorkspace().clone(nodeWorkspaceName,
            node.getPath(), actualTrashPath, true);
	      if (node.isNodeType(MIX_REFERENCEABLE)) {
		        Node clonedNode = trashSession.getNodeByUUID(node.getUUID());
		        //remove link from tag to node
		
		        NewFolksonomyService newFolksonomyService = (NewFolksonomyService)
		        myContainer.getComponentInstanceOfType(NewFolksonomyService.class);
		
		        String tagWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
		        List<Node> tags = newFolksonomyService.getLinkedTagsOfDocument(node, repository, tagWorkspace);
		        for (Node tag : tags) {
		          newFolksonomyService.removeTagOfDocument(tag.getPath(), node, repository, tagWorkspace);
		          linkManager.createLink(tag, clonedNode);
		          long total = tag.hasProperty(EXO_TOTAL) ?
		              tag.getProperty(EXO_TOTAL).getLong() : 0;
		              tag.setProperty(EXO_TOTAL, total + 1);
		              tag.getSession().save();					
		        }	
	      }
        node.remove();
      }

      nodeSession.save();
      trashSession.save();
      //remove categories
      if (deep == 0 && nodeUUID != null) {
	      for (Node category : categories) {
	      	NodeIterator iter = category.getNodes();
	      	while (iter.hasNext()) {
	      		Node categoryChild = iter.nextNode();
		      	if (categoryChild.isNodeType(TAXONOMY_LINK) && categoryChild.hasProperty(UUID) && categoryChild.hasProperty(EXO_WORKSPACE) && 
		      		nodeUUID.equals(categoryChild.getProperty(UUID).getString()) && nodeWorkspaceName.equals(categoryChild.getProperty(EXO_WORKSPACE).getString())) 	{
		      	  try {
		      		moveToTrash(categoryChild, trashPath, trashWorkspace, repository, sessionProvider, deep + 1);
		      	  } catch (Exception e) {}
		      	}
	      	}
	      }
      }
      
      trashSession.save();

      //check and delete target node when there is no its symlink
      if (deep == 0 && taxonomyLinkUUID != null && taxonomyLinkWS != null) {
        Session targetNodeSession = sessionProvider.getSession(taxonomyLinkWS, manageableRepository);
        Node targetNode = null;
        try {
          targetNode = targetNodeSession.getNodeByUUID(taxonomyLinkUUID);
        } catch (Exception e) {}
        if (targetNode != null && isInTaxonomyTree(repository, node, targetNode)) {
          List<Node> symlinks = linkManager.getAllLinks(targetNode, SYMLINK, repository);
          boolean found = false;
          for (Node symlink : symlinks) 
            if (!symlink.isNodeType(EXO_RESTORE_LOCATION)) {
              found = true;
              break;
            }
          if (!found) {
            this.moveToTrash(targetNode, trashPath, trashWorkspace, repository, sessionProvider);
          }
        }
      }

      trashSession.save();
      trashSession.logout();

    }
    
    nodeSession.save();
    if (deep == 0) {
    	nodeSession.logout();
    }
  }
  
  private boolean isInTaxonomyTree(String repository, Node taxonomyNode, Node targetNode) {
    try {
      List<Node> taxonomyTrees = taxonomyService_.getAllTaxonomyTrees(repository, true);
      for (Node tree : taxonomyTrees) 
        if (taxonomyNode.getPath().contains(tree.getPath())) {
          Node taxonomyActionNode = tree.getNode("exo:actions/taxonomyAction");
          String targetWorkspace = taxonomyActionNode.getProperty(EXO_TARGETWS).getString();
          String targetPath = taxonomyActionNode.getProperty(EXO_TARGETPATH).getString();
          if (targetNode.getSession().getWorkspace().getName().equals(targetWorkspace) && targetNode.getPath().contains(targetPath)) 
            return true;
          break;
        }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  //parameter:restorePath->trashNodePath
  public void restoreFromTrash(Node trashHomeNode, String trashNodePath, 
      String repository,
      SessionProvider sessionProvider) throws Exception {
  	restoreFromTrash(trashHomeNode, trashNodePath, repository, sessionProvider, 0);
  }
  
  private void restoreFromTrash(Node trashHomeNode, String trashNodePath, 
      String repository,
      SessionProvider sessionProvider, int deep) throws Exception {

    Session trashNodeSession = trashHomeNode.getSession();
    Node trashNode = (Node)trashNodeSession.getItem(trashNodePath);
    String trashWorkspace = trashNodeSession.getWorkspace().getName();
    String restoreWorkspace = trashNode.getProperty(RESTORE_WORKSPACE).getString();
    String restorePath = trashNode.getProperty(RESTORE_PATH).getString();
    //		restorePath = fixRestorePath(restorePath);
    String nodeUUID = trashNode.isNodeType(MIX_REFERENCEABLE) ? trashNode.getUUID() : null;
    if (trashNode.isNodeType(SYMLINK)) nodeUUID = null;
    String taxonomyLinkUUID = trashNode.isNodeType(TAXONOMY_LINK) ? trashNode.getProperty(UUID).getString() : null;
    String taxonomyLinkWS = trashNode.isNodeType(TAXONOMY_LINK) ? trashNode.getProperty(EXO_WORKSPACE).getString() : null;

    ManageableRepository manageableRepository = repositoryService.getRepository(repository);
    Session restoreSession = sessionProvider.getSession(restoreWorkspace,  manageableRepository);

    if (restoreWorkspace.equals(trashWorkspace)) {
      trashNodeSession.getWorkspace().move(trashNodePath, restorePath);
    } else {
      //clone node
      restoreSession.getWorkspace().clone(
          trashWorkspace, trashNodePath, restorePath, true);
      if (trashNode.isNodeType(MIX_REFERENCEABLE)) {
	      Node restoredNode = restoreSession.getNodeByUUID(trashNode.getUUID());
	
	      //remove link from tag to node in trash
	      ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
	      NewFolksonomyService newFolksonomyService = (NewFolksonomyService)
	      myContainer.getComponentInstanceOfType(NewFolksonomyService.class);
	
	      String tagWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
	      List<Node> tags = newFolksonomyService.getLinkedTagsOfDocument(trashNode, repository, tagWorkspace);
	      for (Node tag : tags) {
	        newFolksonomyService.removeTagOfDocument(tag.getPath(), trashNode, repository, tagWorkspace);
	        linkManager.createLink(tag, restoredNode);
	        long total = tag.hasProperty(EXO_TOTAL) ?
	            tag.getProperty(EXO_TOTAL).getLong() : 0;
	            tag.setProperty(EXO_TOTAL, total + 1);
	            tag.getSession().save();
	      }
      }

      trashNodeSession.getItem(trashNodePath).remove();
    }

    removeMixinEXO_RESTORE_LOCATION(restoreSession, restorePath);

    trashNodeSession.save();
    restoreSession.save();
    
    //also restore categories of node
    if (deep == 0 && nodeUUID != null) {
    	while (true) {
    		boolean found = false;
	    	NodeIterator iter = trashHomeNode.getNodes();
	      while (iter.hasNext()) {
	      	Node trashChild = iter.nextNode();
	      	if (trashChild.isNodeType(TAXONOMY_LINK) && trashChild.hasProperty(UUID) && trashChild.hasProperty(EXO_WORKSPACE) && 
	      		nodeUUID.equals(trashChild.getProperty(UUID).getString()) && restoreWorkspace.equals(trashChild.getProperty(EXO_WORKSPACE))) {
	      	  try {
	      				restoreFromTrash(trashHomeNode, trashChild.getPath(), repository, sessionProvider, deep + 1);
	      				found = true;
	      				break;
	      	  } catch (Exception e) {}
	      	}
	      }
	      if (!found) break;
    	}
    }

    trashNodeSession.save();
    restoreSession.save();
    //restore target node of the restored categories.
    if (deep == 0 && taxonomyLinkUUID != null && taxonomyLinkWS != null) {
    	while (true) {
    		boolean found = false;
	    	NodeIterator iter = trashHomeNode.getNodes();
	      while (iter.hasNext()) {
	      	Node trashChild = iter.nextNode();
	      	if (trashChild.isNodeType(MIX_REFERENCEABLE)  && taxonomyLinkUUID.equals(trashChild.getUUID()) && 
	      	    taxonomyLinkWS.equals(trashChild.getProperty(RESTORE_WORKSPACE).getString()))	{
	      	          try {
	      				restoreFromTrash(trashHomeNode, trashChild.getPath(), repository, sessionProvider, deep + 1);
	      				found = true;
	      				break;
	      	          } catch (Exception e) {}
	      	}
	      }
	      if (!found) break;
    	}
    }

    trashNodeSession.save();
    restoreSession.save();
    
    if (deep == 0) {
	    trashNodeSession.logout();
    }
    if (!restoreWorkspace.equals(trashWorkspace)) 		
      restoreSession.logout();
  }

  public List<Node> getAllNodeInTrash(String trashWorkspace, String repository,
      SessionProvider sessionProvider) throws Exception {

    // String trashPathTail = (trashPath.endsWith("/"))? "" : "/";
    StringBuilder query = new StringBuilder("SELECT * FROM nt:base WHERE exo:restorePath IS NOT NULL");

    // System.out.println(query);
    return selectNodesByQuery(trashWorkspace, repository,
        sessionProvider, query.toString(), Query.SQL);
  }

  public List<Node> getAllNodeInTrashByUser(String trashWorkspace, String repository,
      SessionProvider sessionProvider, String userName) throws Exception {
    StringBuilder query = new StringBuilder(
        "SELECT * FROM nt:base WHERE exo:restorePath IS NOT NULL AND exo:lastModifier='").append(userName).append("'");
    return selectNodesByQuery(trashWorkspace, repository,
        sessionProvider, query.toString(), Query.SQL);
  }

  public void removeRelations(Node node, SessionProvider sessionProvider, 
      String repository) throws Exception {
    ManageableRepository manageableRepository 
    = repositoryService.getRepository(repository);
    String[] workspaces = manageableRepository.getWorkspaceNames();

    String queryString = "SELECT * FROM exo:relationable WHERE exo:relation IS NOT NULL";
    boolean error = false;

    for (String ws : workspaces) {
      Session session = sessionProvider.getSession(ws, manageableRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.SQL);
      QueryResult queryResult = query.execute();

      NodeIterator iter = queryResult.getNodes();
      while (iter.hasNext()) {
        try {
          iter.nextNode().removeMixin("exo:relationable");
          session.save();
        } catch (Exception e) {
          error = true;
        }
      }
    }
    if (error) throw new Exception("Can't remove exo:relationable of all related nodes");
  }


  private List<Node> selectNodesByQuery(String trashWorkspace,
      String repository, SessionProvider sessionProvider,
      String queryString, String language) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    ManageableRepository manageableRepository 
    = repositoryService.getRepository(repository);
    Session session = sessionProvider.getSession(trashWorkspace, manageableRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString, language);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    while (iter.hasNext()) {
      ret.add(iter.nextNode());
    }

    return ret;
  }

  private String fixRestorePath(String path) {
    int leftBracket = path.lastIndexOf('[');
    int rightBracket = path.lastIndexOf(']');
    if (leftBracket == -1 || rightBracket == -1 || 
        (leftBracket >= rightBracket)) return path;

    try {
      Integer.parseInt(path.substring(leftBracket+1, rightBracket));
    } catch (Exception ex) {
      return path;
    }
    return path.substring(0, leftBracket);
  }

  private void removeMixinEXO_RESTORE_LOCATION(Session session, String restorePath) throws Exception {
    Node sameNameNode = ((Node) session.getItem(restorePath));
    Node parent = sameNameNode.getParent();
    String name = sameNameNode.getName();
    NodeIterator nodeIter = parent.getNodes(name);
    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (node.isNodeType(EXO_RESTORE_LOCATION))
        node.removeMixin(EXO_RESTORE_LOCATION);
    }
  }

}
