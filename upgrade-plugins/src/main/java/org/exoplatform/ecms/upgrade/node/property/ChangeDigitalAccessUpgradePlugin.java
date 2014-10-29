package org.exoplatform.ecms.upgrade.node.property;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by toannh on 10/28/14.
 * Update setProperty permission for role *:/platform/users
 */
public class ChangeDigitalAccessUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(ChangeDigitalAccessUpgradePlugin.class);
  private static final String[] EXO_DIGITAL_PATH_ALIAS = new String[]{
          "digitalVideoPath", "digitalAudioPath", "digitalAssetsPath", "digitalPicturePath"
  };

  private RepositoryService repoService;
  private NodeHierarchyCreator nodeHierarchyCreator;

  public ChangeDigitalAccessUpgradePlugin(InitParams initParams, RepositoryService repoService,
                                          NodeHierarchyCreator nodeHierarchyCreator, UserACL userAcl) {
    super(initParams);
    this.repoService = repoService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
  }


  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();;
    LOG.info("Processing update setProperty permission for users has addNode permission...");
    
    try {
      String ws = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(ws, repoService.getCurrentRepository());
      for (String digitalNodePath: EXO_DIGITAL_PATH_ALIAS){
        String exoDigitalNodePath = nodeHierarchyCreator.getJcrPath(digitalNodePath);
        Node exoDigital  = (Node)session.getItem(exoDigitalNodePath);
        setPermission(exoDigital);
      }
      LOG.info("Product info node upgraded successfully!");
    }catch(RepositoryException re){
      if(LOG.isErrorEnabled()) LOG.error(re.getMessage());
    }finally {
      sessionProvider.close();
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }

  /**
   * Set setProperty permission for plf-user.
   * @param node
   */
  private void setPermission(Node node){
    ExtendedNode extendedNode = (ExtendedNode)node;
    String nodeName="";
    try {
      nodeName = extendedNode.getName();
      String[] permissions = new String[]{
              PermissionType.READ,
              PermissionType.ADD_NODE,
              PermissionType.SET_PROPERTY};
      extendedNode.setPermission("*:/platform/users", permissions);
      extendedNode.save();
    }catch(RepositoryException ex){
      LOG.info(""+nodeName+" has been error while update permission!");
      if(LOG.isErrorEnabled()) LOG.error(ex.getMessage());
    }catch(Exception ex){
      if(LOG.isErrorEnabled()) LOG.error(ex.getMessage());
    }
  }
}
