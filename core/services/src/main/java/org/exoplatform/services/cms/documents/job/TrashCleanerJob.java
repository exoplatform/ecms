package org.exoplatform.services.cms.documents.job;

import java.security.AccessControlException;
import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 22/01/16.
 */
@DisallowConcurrentExecution
public class TrashCleanerJob implements Job {

  private static final Log LOG = ExoLogger.getLogger(TrashCleanerJob.class);

  public static final String EXO_AUDIT = "exo:audit";

  public void execute(JobExecutionContext context) throws JobExecutionException {
    String timeLimit = System.getProperty("exo.trashcleaner.lifetime");
    if (timeLimit == null)
      timeLimit = "30";
    LOG.info("Start TrashCleanerJob, delete nodes in trash older than " + timeLimit + " days.");
    TrashService trashService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(TrashService.class);

    Node trashNode = trashService.getTrashHomeNode();

    int deletedNode = processDelete(timeLimit, trashNode);
    LOG.info("Empty Trash folder successfully! " + deletedNode + " nodes deleted");
  }

  private int processDelete(String timeLimit, Node trashNode) {
    int deletedNode = 0;
    try {
      if (trashNode.hasNodes()) {
        NodeIterator childNodes = trashNode.getNodes();
        long size = childNodes.getSize();
        int current = 0;

        while (childNodes.hasNext()) {
          Node currentNode = (Node) childNodes.next();
          if (!currentNode.getSession().isLive()) {
            currentNode.getSession().refresh(false);
          }
          try {
            current++;
            if (current % 50 == 0) {
              LOG.info("Checking node " + currentNode.getName() + " node from Trash (" + current + "/" + size + ")");
            } else {
              LOG.debug("Checking node " + currentNode.getName() + " node from Trash (" + current + "/" + size + ")");
            }
            if (currentNode.getName().equals("exo:actions") && currentNode.hasNode("trashFolder")) {
              continue;
            }
            if (currentNode.hasProperty("exo:lastModifiedDate")) {
              long dateCreated = currentNode.getProperty("exo:lastModifiedDate").getDate().getTimeInMillis();
              if ((Calendar.getInstance().getTimeInMillis() - dateCreated > Long.parseLong(timeLimit) * 24 * 60 * 60 * 1000)
                  && (currentNode.isNodeType("exo:restoreLocation"))) {
                recursiveDelete(currentNode);
                deletedNode++;
              }
            } else {
              recursiveDelete(currentNode);
              deletedNode++;
            }
          } catch (Exception ex) {
            LOG.error("Error while removing " + currentNode.getName() + " node from Trash", ex);
          }
        }
      }
    } catch (RepositoryException ex) {
      LOG.error("Failed to get child nodes", ex);
    }
    return deletedNode;
  }

  public void recursiveDelete(Node node) throws Exception {
    if (node.isNodeType("nt:folder") || node.isNodeType("nt:unstructured")) {
      NodeIterator children = node.getNodes();
      while (children.hasNext()) {
        Node child = children.nextNode();
        recursiveDelete(child);
      }
    }
    deleteNode(node);
  }

  public void deleteNode(Node node) throws Exception {
    ActionServiceContainer actionService = ExoContainerContext.getCurrentContainer()
                                                              .getComponentInstanceOfType(ActionServiceContainer.class);
    ThumbnailService thumbnailService = ExoContainerContext.getCurrentContainer()
                                                           .getComponentInstanceOfType(ThumbnailService.class);
    RepositoryService repoService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
    SessionProvider sessionProviderForDeleteNode = SessionProvider.createSystemProvider();
    Session sessionForDeleteNode =sessionProviderForDeleteNode.getSession("collaboration",repoService.getDefaultRepository());
    LOG.debug("Try to delete node {}",node.getPath());
    try {
      Node nodeToDelete = readNodeWithNewSession(node,sessionForDeleteNode);
      removeReferences(nodeToDelete);
      removeActions(actionService, repoService, nodeToDelete);
      removeThumbNails(thumbnailService, nodeToDelete);
      removeAuditForNode(nodeToDelete, repoService.getCurrentRepository());
      nodeToDelete.remove();
      nodeToDelete.getSession().save();
      LOG.debug("Node " + nodeToDelete.getPath() + " deleted");
    } catch (ReferentialIntegrityException ref) {
      LOG.error("ReferentialIntegrityException when removing " + node.getName() + " node from Trash", ref);
    } catch (ConstraintViolationException cons) {
      LOG.error("ConstraintViolationException when removing " + node.getName() + " node from Trash", cons);
    } catch (Exception ex) {
      LOG.error("Error while removing " + node.getName() + " node from Trash", ex);
    } finally {
      sessionForDeleteNode.logout();
      sessionProviderForDeleteNode.close();
    }
  }

  private void removeThumbNails(ThumbnailService thumbnailService, Node nodeToDelete) {
    String nodePath = "";
    try {
      nodePath = nodeToDelete.getPath();
      thumbnailService.processRemoveThumbnail(nodeToDelete);
    } catch (Exception ex) {
      LOG.error("An error occurs while removing thumbnail for node {} ", nodePath, ex);
    }
  }

  private void removeActions(ActionServiceContainer actionService, RepositoryService repoService, Node nodeToDelete) {
    String nodePath = "";
    try {
      nodePath = nodeToDelete.getPath();
      actionService.removeAction(nodeToDelete, repoService.getCurrentRepository().getConfiguration().getName());
    } catch (Exception ex) {
      LOG.error("An error occurs while removing actions related to node {} ", nodePath, ex);
    }
  }

  private boolean checkPermission(Node node, String permissionType) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(permissionType);
      return true;
    } catch (AccessControlException e) {
      return false;
    }
  }

  private Node readNodeWithNewSession(Node node, Session sessionForDeleteNode) throws RepositoryException {
    String idf = ((NodeImpl)node).getIdentifier();
    return ((SessionImpl)sessionForDeleteNode).getNodeByIdentifier(idf);
  }

  private void removeReferences(Node node) {
    String nodePath = "";
    try {
      nodePath=node.getPath();
      RelationsService relationService =ExoContainerContext.getCurrentContainer()
                                                           .getComponentInstanceOfType(RelationsService.class);


      PropertyIterator iter = node.getReferences();
      if (iter.hasNext()) {
        // if there is a reference, move it
        String relationPath = iter.nextProperty().getPath();
        LOG.debug("Node " + node.getPath() + " is referenced by " + relationPath + ", remove the referenec");

        Item relation = node.getSession().getItem(relationPath);
        Node sourceRelationNode = relation.getParent();

        relationService.removeRelation(sourceRelationNode,node.getPath());
      }

      NodeIterator children = node.getNodes();
      while (children.hasNext()) {
        Node child = children.nextNode();
        removeReferences(child);
      }
    } catch (Exception ex) {
      LOG.error("An error occurs while removing relations for node {}", nodePath, ex);
    }
  }

  private void removeAuditForNode(Node node, ManageableRepository repository) {
    String nodePath = "";

    try {
      nodePath=node.getPath();
      if (checkPermission(node,"remove") && node.isNodeType("exo:auditable")) {
        Session session = SessionProvider.createSystemProvider().getSession(node.getSession().getWorkspace().getName(), repository);
        if (session.getRootNode().hasNode(EXO_AUDIT) && session.getRootNode().getNode(EXO_AUDIT).hasNode(node.getUUID())) {
          session.getRootNode().getNode(EXO_AUDIT).getNode(node.getUUID()).remove();
          session.save();
        }
      }
    } catch (Exception ex) {
      LOG.error("An error occurs while removing audit for node {}", nodePath, ex);
    }


  }

}
