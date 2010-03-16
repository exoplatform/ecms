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
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Oct 6, 2009 3:39:53 AM
 */
public class TrashServiceImpl implements TrashService {

  final static public String EXO_TOTAL = "exo:total".intern();	

  private RepositoryService repositoryService;
  private LinkManager linkManager;

  public TrashServiceImpl(RepositoryService repositoryService,
      LinkManager linkManager)
  throws Exception {
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
  }

  /**
   * {@inheritDoc}
   */
  public void moveToTrash(Node node, String trashPath, String trashWorkspace,
      String repository, SessionProvider sessionProvider)
  throws Exception {

    String nodeName = node.getName();
    Session nodeSession = node.getSession();
    String nodeWorkspaceName = nodeSession.getWorkspace().getName();

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
        Node clonedNode = trashSession.getNodeByUUID(node.getUUID());
        //remove link from tag to node
        ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
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
        node.remove();
      }
      nodeSession.save();
      trashSession.save();
      trashSession.logout();
    } 
    nodeSession.logout();
  }

  /**
   * {@inheritDoc}
   */
  //parameter:restorePath->trashNodePath
  public void restoreFromTrash(Node trashHomeNode, String trashNodePath, 
      String repository,
      SessionProvider sessionProvider) throws Exception {

    Session trashNodeSession = trashHomeNode.getSession();
    Node trashNode = (Node)trashNodeSession.getItem(trashNodePath);
    String trashWorkspace = trashNodeSession.getWorkspace().getName();
    String restoreWorkspace = trashNode.getProperty(RESTORE_WORKSPACE).getString();
    String restorePath = trashNode.getProperty(RESTORE_PATH).getString();
    //		restorePath = fixRestorePath(restorePath);

    ManageableRepository manageableRepository = repositoryService
    .getRepository(repository);
    Session restoreSession 
    = sessionProvider.
    getSession(restoreWorkspace,
        manageableRepository);

    if (restoreWorkspace.equals(trashWorkspace)) {
      trashNodeSession.getWorkspace().move(trashNodePath, restorePath);
    } else {
      //clone node
      restoreSession.getWorkspace().clone(
          trashWorkspace, trashNodePath, restorePath, true);
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

      trashNodeSession.getItem(trashNodePath).remove();
    }

    removeMixinEXO_RESTORE_LOCATION(restoreSession, restorePath);
    trashNodeSession.save();
    restoreSession.save();

    trashNodeSession.logout();
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
