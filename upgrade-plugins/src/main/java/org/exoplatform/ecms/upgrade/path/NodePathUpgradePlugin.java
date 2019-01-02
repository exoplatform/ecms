package org.exoplatform.ecms.upgrade.path;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;

/**
 * 
 * This class is used to migrade nodes which path is changed. It will move these nodes to new position. We must
 * configured source node and target node in configuration.xml via two variable source.node and target.node.
 * source.node must contains the workspace name.
 * Ex: source.node=collaboration:/sites content/live
 *     target.node=/sites
 *
 */

public class NodePathUpgradePlugin extends UpgradeProductPlugin {
  
  private Log log = ExoLogger.getLogger(this.getClass());
  private RepositoryService repositoryService_;
  private String srcNode;
  private String destNode;
  
  public NodePathUpgradePlugin(RepositoryService repoService, InitParams initParams){
    super(initParams);
    this.repositoryService_ = repoService;
    this.srcNode= initParams.getValueParam("source.node").getValue();
    this.destNode = initParams.getValueParam("target.node").getValue();
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    log.info("Start " + this.getClass().getName() + ".............");
    if ((srcNode == null) || ("".equals(srcNode)) || (destNode == null) || ("".equals(destNode))) {
      log.info("Source and target node must be set to run plugin!");
      return;
    }
    String workspace = srcNode.split(":")[0];
    String nodePath = srcNode.split(":")[1];
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try{
      ExtendedSession session = (ExtendedSession)sessionProvider.getSession(workspace, repositoryService_.getCurrentRepository());
      Node rootNode = session.getRootNode();
      Node sourceNode = rootNode.getNode(nodePath.substring(1));
      Node targetNode = null;
      try {
        targetNode = rootNode.getNode(destNode.substring(1));
      } catch (PathNotFoundException pne) {
        targetNode = rootNode.addNode("sites");
        rootNode.save();
      }
      if (sourceNode.hasNodes()){
        NodeIterator iter = sourceNode.getNodes();
        while (iter.hasNext()){
          Node child = (Node) iter.next();
          if (targetNode.hasNode(child.getName())) {
        	targetNode.getNode(child.getName()).remove();
        	session.save();
          }
          log.info("Move " + nodePath + "/" + child.getName() + " to " + destNode + "/" + child.getName());
          session.move(nodePath + "/" + child.getName(), destNode + "/" + child.getName(), false);
          session.save();
        }
        //Remove source node
        sourceNode.remove();
        session.save();
      }
    }catch(Exception e){
      log.error("Unexpected error happens in moving nodes", e.getMessage());
    }finally {
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
