package org.exoplatform.services.jobs.symlink;

import java.util.ArrayList;
import java.util.List;

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
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;//import org.exoplatform.services.scheduler.impl.JobSchedulerServiceImpl;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jan 11, 2011  
 * 10:48:40 AM
 */
public class ClearOrphanSymlinksJob implements Job {
  private static final Log log                 = ExoLogger.getLogger(ClearOrphanSymlinksJob.class);

  private static final String EXO_RESTORELOCATION = "exo:restoreLocation";  
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.debug("Start Executing ClearOrphanSymlinksJob");

    String queryString = "SELECT * FROM exo:symlink";
    
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService)exoContainer.getComponentInstanceOfType(RepositoryService.class);
    LinkManager linkManager = (LinkManager)exoContainer.getComponentInstanceOfType(LinkManager.class);
    TrashService trashService = (TrashService)exoContainer.getComponentInstanceOfType(TrashService.class);
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator)exoContainer.getComponentInstanceOfType(NodeHierarchyCreator.class);
    ManageableRepository manageableRepository;
    try {
      manageableRepository = repositoryService.getCurrentRepository();
    } catch (Exception e) { return; }

    String repositoryName = manageableRepository.getConfiguration().getName();    
    String trashPath = nodeHierarchyCreator.getJcrPath(BasePath.TRASH_PATH);
    String trashWorkspace = null;
    ManageDriveService driveService = (ManageDriveService)exoContainer.getComponentInstanceOfType(ManageDriveService.class);
    try {
      for (DriveData driveData : driveService.getAllDrives(repositoryName)) 
        if (driveData.getHomePath().equals(trashPath) ) {
          trashWorkspace = driveData.getWorkspace();
          break;
        }
    } catch (Exception e) {}
    if (trashWorkspace == null) return;
    SessionProvider sessionProvider = null;
    Session session = null;
    try {
      String[] workspaces = manageableRepository.getWorkspaceNames();
      sessionProvider = SessionProviderFactory.createSystemProvider();
      
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
            if (symlinkNode.isNodeType(EXO_RESTORELOCATION))
              continue;
            //get list of node to delete
            try {
              Node targetNode = linkManager.getTarget(symlinkNode, true);
              if (targetNode.isNodeType(EXO_RESTORELOCATION))
                deleteNodeList.add(symlinkNode);
            } catch (ItemNotFoundException e) {
              deleteNodeList.add(symlinkNode);
            } catch (RepositoryException e) {}
            //move the nodes in list to trash
            for (Node node : deleteNodeList) {
              try {
                String nodePath = node.getPath();
                trashService.moveToTrash(node, trashPath, trashWorkspace, repositoryName, sessionProvider);
                log.info("ClearOrphanSymlinksJob: move orphan symlink " + nodePath + " to Trash");
              } catch (Exception e) {
                log.error("ClearOrphanSymlinksJob: Can not move to trash node :" + node.getPath(), e);
              }
            }
          }
        } catch (RepositoryException e) {
          log.error("ClearOrphanSymlinksJob: Error when deleting orphan symlinks in workspace: " + workspace, e);
        }
      }
    } catch (Exception e) {
      log.error("Error occurs in ClearOrphanSymlinksJob", e);
    } finally {
      if (session != null && session.isLive())
        session.logout();
    }
    log.info("ClearOrphanSymlinksJob: Done!");
  }
  
}
