package org.exoplatform.services.jobs.symlink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jan 11, 2011
 * 10:48:40 AM
 */
public class ClearOrphanSymlinksCronJobImpl implements Job {
  private static final Log    log                 = ExoLogger.getLogger(ClearOrphanSymlinksCronJobImpl.class);

  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.debug("Start Executing ClearOrphanSymlinksCronJobImpl");

    String queryString = "SELECT * FROM exo:symlink order by exo:dateCreated DESC";

    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService)exoContainer.getComponentInstanceOfType(RepositoryService.class);
    LinkManager linkManager = (LinkManager)exoContainer.getComponentInstanceOfType(LinkManager.class);
    TrashService trashService = (TrashService)exoContainer.getComponentInstanceOfType(TrashService.class);

    SessionProvider sessionProvider = null;
    Session session = null;
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      sessionProvider = SessionProvider.createSystemProvider();
      String[] workspaces = manageableRepository.getWorkspaceNames();
      Set<Session> sessionSet = new HashSet<Session>();
      for (String workspace : workspaces) {
        try {
          session = sessionProvider.getSession(workspace, manageableRepository);
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          Query query = queryManager.createQuery(queryString, Query.SQL);
          QueryResult queryResult = query.execute();
          NodeIterator nodeIterator = queryResult.getNodes();
          List<Node> deleteNodeList = new ArrayList<Node>();
          while (nodeIterator.hasNext()) {
            Node symlinkNode = nodeIterator.nextNode();
            if (Utils.isInTrash(symlinkNode))
              continue;
            //get list of node to delete
            Node targetNode = null;
            try {
              targetNode = linkManager.getTarget(symlinkNode, true);
              if (Utils.isInTrash(targetNode))
                deleteNodeList.add(symlinkNode);
            } catch (ItemNotFoundException e) {
              deleteNodeList.add(symlinkNode);
            } catch (RepositoryException e) {
            } finally {
              sessionSet.add(targetNode.getSession());
            }
            //move the nodes in list to trash
          }
          for (Node node : deleteNodeList) {
            try {
              String nodePath = node.getPath();
              trashService.moveToTrash(node, sessionProvider);
              log.info("ClearOrphanSymlinksCronJobImpl: move orphan symlink " + nodePath + " to Trash");
            } catch (Exception e) {
              log.error("ClearOrphanSymlinksCronJobImpl: Can not move to trash node :" + node.getPath(), e);
            }
          }
        } catch (RepositoryException e) {
          log.error("ClearOrphanSymlinksCronJobImpl: Error when deleting orphan symlinks in workspace: " + workspace, e);
        } finally {
          if (session != null && session.isLive())
            session.logout();
        }
      }
      for (Session targetSession : sessionSet) {
        if (targetSession != null && targetSession.isLive())
          targetSession.logout();
      }
    } catch (Exception e) {
      log.error("Error occurs in ClearOrphanSymlinksCronJobImpl", e);
      sessionProvider.close();
    }
  }
}
