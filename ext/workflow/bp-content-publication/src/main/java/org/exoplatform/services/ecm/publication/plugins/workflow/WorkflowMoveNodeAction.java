/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.plugins.workflow;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Dec 27, 2008
 */
public class WorkflowMoveNodeAction {

  public static void moveNode(RepositoryService repositoryService,
                              String nodePath,
                              String srcWorkspace,
                              String destWorkspace,
                              String destPath,
                              String repository) {
    Session srcSession = null;
    Session destSession = null;
    if (!srcWorkspace.equals(destWorkspace)){
      try {
        srcSession = repositoryService.getCurrentRepository().getSystemSession(srcWorkspace);
        destSession = repositoryService.getCurrentRepository().getSystemSession(destWorkspace);
        Workspace workspace = destSession.getWorkspace();
        Node srcNode = (Node) srcSession.getItem(nodePath);
        try {
          destSession.getItem(destPath);
        } catch (PathNotFoundException e) {
          createNode(destSession, destPath);
        }
        workspace.clone(srcWorkspace, nodePath, destPath, true);
        //Remove src node
        srcNode.remove();
        srcSession.save();
        destSession.save();
        srcSession.logout();
        destSession.logout();
      } catch (Exception e) {
        if(srcSession != null) {
          srcSession.logout();
        }
        if(destSession !=null) {
          destSession.logout();
        }
      }
    }else {
      Session session = null;
      try{
        session = repositoryService.getCurrentRepository().getSystemSession(srcWorkspace);
        Workspace workspace = session.getWorkspace();
        try {
          session.getItem(destPath);
        } catch (PathNotFoundException e) {
          createNode(session, destPath);
          session.refresh(false);
        }
        workspace.move(nodePath, destPath);
        session.logout();
      } catch(Exception e){
        if(session !=null && session.isLive()) {
          session.logout();
        }
      }
    }
  }

  /**
   * Create node following path in uri
   * @param session Session
   * @param uri     path to created node
   * @throws RepositoryException
   */
  private static void createNode(Session session, String uri) throws RepositoryException {
    String[] splittedName = StringUtils.split(uri, "/");
    Node rootNode = session.getRootNode();
    for (int i = 0; i < splittedName.length - 1; i++) {
      try {
        rootNode.getNode(splittedName[i]);
      } catch (PathNotFoundException exc) {
        rootNode.addNode(splittedName[i], "nt:unstructured");
        rootNode.save();
      }
      rootNode = rootNode.getNode(splittedName[i]);
    }
    session.save();
  }
}
