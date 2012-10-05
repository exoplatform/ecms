package org.exoplatform.ecms.upgrade.plugins;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class FlashNodeTypeUpgradePlugin extends UpgradeProductPlugin {

	private static final Log LOG = ExoLogger.getLogger(FlashNodeTypeUpgradePlugin.class.getName());
	private RepositoryService repositoryService_;
	final static public String EXO_RISIZEABLE = "exo:documentSize";
  final static public String FLASH_MIMETYPE = "flash";
	
	public FlashNodeTypeUpgradePlugin(RepositoryService repoService, InitParams initParams) {
		super(initParams);
		this.repositoryService_ = repoService;
	}

	@Override
	public void processUpgrade(String oldVersion, String newVersion) {
		if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
		SessionProvider sessionProvider = null;
		try {
			sessionProvider = SessionProvider.createSystemProvider();
			String[] workspaces = repositoryService_.getCurrentRepository().getWorkspaceNames();
			if(workspaces.length > 0) {
				for (String workspace : workspaces) {
					Session session = sessionProvider.getSession(workspace, repositoryService_.getCurrentRepository());
					QueryManager queryManager = session.getWorkspace().getQueryManager();
		      String queryStatement = "SELECT * FROM nt:resource WHERE jcr:mimeType IS NOT NULL AND jcr:mimeType LIKE '%"+FLASH_MIMETYPE+"%'";
		      Query query = queryManager.createQuery(queryStatement, Query.SQL);
		      NodeIterator iter = query.execute().getNodes();
		      while (iter.hasNext()) {
		      	Node node = iter.nextNode();  
		      	Node flashNode = node.getParent();
		      	if(flashNode.canAddMixin(EXO_RISIZEABLE)) {
		      		flashNode.addMixin(EXO_RISIZEABLE);
		      		flashNode.save();
		      	}
		      }
		      session.save();
				}
			}      
		} catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating flash node type.", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
	}

	@Override
	public boolean shouldProceedToUpgrade(String previousVersion,
			String newVersion) {
		return true;
	}

}
