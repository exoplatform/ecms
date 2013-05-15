package org.exoplatform.ecms.upgrade.activities;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class ActivitiesUpgradePlugin extends UpgradeProductPlugin {
	
	private Log log = ExoLogger.getLogger(this.getClass());
	private DMSConfiguration dmsConfiguration_;
  private RepositoryService repoService_;
  private ManageViewService viewService_;

	public ActivitiesUpgradePlugin(RepositoryService repoService, DMSConfiguration dmsConfiguration, 
      ManageViewService viewService, InitParams initParams) {
    super(initParams);
    repoService_ = repoService;
    dmsConfiguration_ = dmsConfiguration;
    viewService_ = viewService;
  }

	@Override
	public void processUpgrade(String oldVersion, String newVersion) {
		if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
		SessionProvider p = WCMCoreUtils.getSystemSessionProvider();
		Session session = null;
		try {
			session = p.getSession("social",
          repoService_.getCurrentRepository());
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
        Session session2 = p.getSession(workspace,
            repoService_.getCurrentRepository());
        try{
	        Node node = (Node)session2.getItem(nodeUrl);
	        if(node.isNodeType(NodetypeConstant.NT_FILE)) {
	        	viewNode.setProperty("soc:type", "files:spaces");
	        }
	        session2.save();
        } catch(PathNotFoundException ex) {
        	continue;
        }
      }
      session.save();
      if (log.isInfoEnabled()) {
        log.info("=====Completed the migration data for user views=====");
      }
		} catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating activities: ", e);        
      }
    } finally {
    	if(p!=null) p.close();
    }
		
	}

	@Override
	public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
	  // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
	}
}
