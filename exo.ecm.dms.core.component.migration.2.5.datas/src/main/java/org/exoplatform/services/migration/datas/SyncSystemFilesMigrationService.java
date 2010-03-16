/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.migration.datas;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 11, 2009  
 */
/**
 * Migrate system data from system workspace to dms-system workspace
 */
public class SyncSystemFilesMigrationService implements Startable {

  private static final Log LOG  = ExoLogger.getLogger(SyncSystemFilesMigrationService.class.getName()); 
  private static final String EXO_ECM = "/exo:ecm";
  
  private DMSConfiguration dmsConfiguration;
  private RepositoryService repositoryService;

  public SyncSystemFilesMigrationService(RepositoryService repositoryService, 
      DMSConfiguration dmsConfiguration) throws Exception {
    this.dmsConfiguration = dmsConfiguration;
    this.repositoryService = repositoryService;
  }
  
  public void start() {
    try {
      processSyncSystemFiles();
    } catch(Exception e) {
      LOG.error("An unexpected error occurs when synchronize system files", e);
    }
  }

  /**
   * Get session from repository in DMS System Workspace name
   * @param repository    repository name
   * @return session
   * @throws Exception
   */
  private Session getDMSSytemSession() throws Exception{    
    ManageableRepository manaRepository = repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = 
      dmsConfiguration.getConfig(manaRepository.getConfiguration().getName());
    return manaRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;          
  }
  
  private Node getSystemExoEcmFolder() throws Exception {
    return (Node) getSytemSession().getItem("/jcr:system" + EXO_ECM); 
  }
  
  /**
   * Get session from repository in System Workspace name
   * @param repository    repository name
   * @return session
   * @throws Exception
   */
  private Session getSytemSession() throws Exception{  
    ManageableRepository manaRepository = repositoryService.getCurrentRepository();
    return manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;          
  }  
  
  private void processSyncSystemFiles() throws Exception {
    Node systemExoEcm = getSystemExoEcmFolder();
    if(systemExoEcm.hasNodes()) {
      NodeIterator nodeIter = systemExoEcm.getNodes();
      Node node = null;
      while(nodeIter.hasNext()) {
        node = nodeIter.nextNode();
        syncTheChangingToDmsSystem(node);
      }
    }
  }
  
  private void syncTheChangingToDmsSystem(Node oldSystemNode) throws Exception {
    String absolutePath = oldSystemNode.getPath().substring(11, oldSystemNode.getPath().length());
    try {
      getDMSSytemSession().getItem(absolutePath.trim());
      if(oldSystemNode.hasNodes()) {
        NodeIterator nodeIter = oldSystemNode.getNodes();
        while(nodeIter.hasNext()) {
          syncTheChangingToDmsSystem(nodeIter.nextNode());
        }
      }
    } catch(PathNotFoundException pne) {
      LOG.info("\n\n#### Migrating the data from " +oldSystemNode.getPath()+ " in system workspace to " + 
          absolutePath + " in dms-system workspace ####\n\n"); 
      getDMSSytemSession().getWorkspace().clone(oldSystemNode.getSession().getWorkspace().
          getName(), oldSystemNode.getPath(), absolutePath, true);
      LOG.info("\n\n####DONE####\n\n");
    }
  }
  
  public void stop() {
  }

}
