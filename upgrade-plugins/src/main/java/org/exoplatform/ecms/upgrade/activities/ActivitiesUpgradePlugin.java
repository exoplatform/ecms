package org.exoplatform.ecms.upgrade.activities;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.cms.impl.Utils;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

public class ActivitiesUpgradePlugin extends UpgradeProductPlugin {

  private Log log = ExoLogger.getLogger(this.getClass());
  private RepositoryService repoService_;

  public ActivitiesUpgradePlugin(RepositoryService repoService, InitParams initParams) {
    super(initParams);
    repoService_ = repoService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession("social", repoService_.getCurrentRepository());

      if (log.isInfoEnabled()) {
        log.info("=====Start migrate data for all activities=====");
      }
      String statement = "SELECT * FROM soc:activity WHERE soc:type = 'contents:spaces'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node viewNode = nodeIter.nextNode();
        Node paramsNode = viewNode.getNode("soc:params");
        String workspace = paramsNode.getProperty("workspace").getString();        
        String nodeUrl = viewNode.getProperty("soc:url").getString();
        String nodeUUID = paramsNode.getProperty("id").getString();
        Session session2 = sessionProvider.getSession(workspace,
                                                      repoService_.getCurrentRepository());
        try{
          Node node = (Node)session2.getItem(nodeUrl);
          if(node.isNodeType(NodetypeConstant.NT_FILE)) {
            viewNode.setProperty("soc:type", "files:spaces");
          }
        } catch(PathNotFoundException ex) {
          try {
            Node node = (Node)session2.getNodeByUUID(nodeUUID);
            if(node.isNodeType(NodetypeConstant.NT_FILE)) {
              if(Utils.isInTrash(node)) { 
                viewNode.remove();
              }
              else {
                viewNode.setProperty("soc:type", "files:spaces");
                viewNode.setProperty("soc:url", node.getPath());
              }
            }
          } catch(ItemNotFoundException ix){
            continue;
          }
        }
      }
      session.save();
      if (log.isInfoEnabled()) {
        log.info("=====Completed the migration data for all activities=====");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating activities: ", e);        
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }

  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }
}