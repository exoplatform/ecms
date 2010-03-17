/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.migration.workspaces;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.LockPersisterEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 *          ngoc.tran@exoplatform.com
 * Dec 21, 2009  
 */
public class WorkspaceMigrationService implements Startable{
  private final static String WCM_SYSTEM = "wcm-system";
  private final static String NT_UNSTRUCTURED = "nt:unstructured";
  private final static String KEY_SWAPDIRECTORY = "swap-directory" ;
  private final static String KEY_SOURCENAME = "source-name" ;
  private final static String KEY_DIALECT = "dialect" ;
  private final static String KEY_MULTIDB = "multi-db" ;
  private final static String KEY_MAXBUFFER = "max-buffer-size" ;
  private final static String KEY_PATH = "path"  ;
  private final static String KEY_INDEXDIR = "index-dir" ;
  private final static String KEY_MAXSIZE = "max-size" ;
  private final static String KEY_LIVETIME = "live-time" ;
  private final static String KEY_UPDATESTORE = "update-storage";
  private final static String KEY_SUPPORT_HIGHLIGHTING = "support-highlighting";
  private final static String KEY_EXCERPT_PROVIDER = "excerptprovider-class";
  
  private static final Log log  = ExoLogger.getLogger("wcm:WorkspaceMigrationService"); 
  
  private RepositoryService repositoryService;
  
  public WorkspaceMigrationService(RepositoryService repositoryService) throws Exception {
    this.repositoryService = repositoryService;
  }
  
  public void start() {
    boolean isNotContainWCM_SYSEM = true;
    try {
      List<WorkspaceEntry> workspaces = 
        repositoryService.getCurrentRepository().getConfiguration().getWorkspaceEntries();
      for(WorkspaceEntry wsEntry : workspaces) {
        if(wsEntry.getName().equals(WCM_SYSTEM)) {
          isNotContainWCM_SYSEM = false;
          break;
        }
      }

      if(isNotContainWCM_SYSEM) createNewWorkspaceWCMSystem();

    } catch(Exception e) {
      log.error("An unexpected error occurs when create wcm-system workspace", e);
    }
  }
  
  /**
   * Get system workspace to get information which configured in current repository
   * @return WorkspaceEntry
   * @throws RepositoryException
   */
  private WorkspaceEntry getSystemWorkspace() throws RepositoryException {
    List<WorkspaceEntry> workspaces = 
      repositoryService.getCurrentRepository().getConfiguration().getWorkspaceEntries();
    String systemWsName = 
      repositoryService.getCurrentRepository().getConfiguration().getSystemWorkspaceName();
    for(WorkspaceEntry wsEntry : workspaces) {
      if(wsEntry.getName().equals(systemWsName)) {
        return wsEntry;
      }
    }
    return null;
  }
  
  /**
   * Create wcm-system workspace
   * @throws RepositoryException
   * @throws RepositoryConfigurationException
   */
  private void createNewWorkspaceWCMSystem() throws RepositoryException, RepositoryConfigurationException {
    log.info("########################################");
    log.info("Starting create new wcm-system workspace");
    log.info("Proccessing...");
    WorkspaceEntry workspaceEntry = new WorkspaceEntry(WCM_SYSTEM, NT_UNSTRUCTURED);
    
    //Preparing information of system workspace in current repository
    
    WorkspaceEntry systemWorkspace = getSystemWorkspace();
    if(systemWorkspace == null) {
      log.error("Cannot init system workspace in your repository");
      return;
    }
    long systemTimeOut = systemWorkspace.getLockManager().getTimeout();
    String systemLockPath = systemWorkspace.getLockManager().getPersister().getParameterValue(KEY_PATH);
    String systemPersisterType = systemWorkspace.getLockManager().getPersister().getType();
    String systemSwapPath = systemWorkspace.getContainer().getParameterValue(KEY_SWAPDIRECTORY);
    ContainerEntry containerEntry = systemWorkspace.getContainer();
    ValueStorageEntry valueStorageEntry = (ValueStorageEntry)systemWorkspace.getContainer().getValueStorages().get(0);
    String systemStorePath = valueStorageEntry.getParameterValue(KEY_PATH);
    String sourceName = containerEntry.getParameterValue(KEY_SOURCENAME);
    String dbType = containerEntry.getParameterValue(KEY_DIALECT);
    boolean isMultiDB = containerEntry.getParameterBoolean(KEY_MULTIDB);
    String storeageType = valueStorageEntry.getType();
    String filterType = valueStorageEntry.getFilters().get(0).getPropertyType();
    String systemMaxBufferSize = containerEntry.getParameterValue(KEY_MAXBUFFER);
    boolean isCache = systemWorkspace.getCache().isEnabled();
    String maxSize = systemWorkspace.getCache().getParameterValue(KEY_MAXSIZE);
    String liveTime = systemWorkspace.getCache().getParameterValue(KEY_LIVETIME);
    String systemQueryHandler = systemWorkspace.getQueryHandler().getType();
    //String systemIndexPath = systemWorkspace.getQueryHandler().getIndexDir(); --> 3.0.0-Beta02
    String systemIndexPath = "";
    //boolean systemSupportHighLighting = systemWorkspace.getQueryHandler().getSupportHighlighting(); --> 3.0.0-Beta02
    boolean systemSupportHighLighting = true;
    //String excerptClass = systemWorkspace.getQueryHandler().getExcerptProviderClass(); --> 3.0.0-Beta02
    String excerptClass = "";
    
    //Init information for wcm-system workspace
    String lockPath = systemLockPath.substring(0, systemLockPath.lastIndexOf("/") +1 ) + WCM_SYSTEM; 
    String swapPath = systemSwapPath.substring(0, systemSwapPath.lastIndexOf("/") + 1) + WCM_SYSTEM;
    String storePath = systemStorePath.substring(0, systemStorePath.lastIndexOf("/") + 1) + WCM_SYSTEM;
    String indexPath = systemIndexPath.substring(0, systemIndexPath.lastIndexOf("/") + 1) + WCM_SYSTEM;
    
    LockManagerEntry lockEntry = new LockManagerEntry() ;
    lockEntry.setTimeout(systemTimeOut);
    LockPersisterEntry persisterEntry = new LockPersisterEntry();
    persisterEntry.setType(systemPersisterType);
    ArrayList<SimpleParameterEntry> lpParams = new ArrayList<SimpleParameterEntry>();
    lpParams.add(new SimpleParameterEntry(KEY_PATH, lockPath));
    persisterEntry.setParameters(lpParams);
    lockEntry.setPersister(persisterEntry);
    workspaceEntry.setLockManager(lockEntry) ;
    workspaceEntry.setContainer(newContainerEntry(containerEntry.getType(), sourceName, dbType, 
        isMultiDB, storeageType, filterType, systemMaxBufferSize, swapPath, storePath, true, WCM_SYSTEM));
    workspaceEntry.setCache(newCacheEntry(isCache, maxSize, liveTime)) ;
    workspaceEntry.setQueryHandler(newQueryHandlerEntry(systemQueryHandler, indexPath, systemSupportHighLighting, excerptClass)) ;
    ManageableRepository manageRepository = repositoryService.getCurrentRepository();
    manageRepository.configWorkspace(workspaceEntry) ;
    manageRepository.createWorkspace(workspaceEntry.getName()) ;

    //Register new workspace
    if(repositoryService.getConfig().isRetainable()) {
      repositoryService.getConfig().retain() ;
    }
    log.info("Workspace wcm-system created successfully");
    log.info("########################################");
  }

  /**
   * Add information to container entry
   * @param containerType
   * @param sourceName
   * @param dbType
   * @param isMulti
   * @param storeType
   * @param filterType
   * @param bufferValue
   * @param swapPath
   * @param storePath
   * @param isUpdateStore
   * @param valueStorageId
   * @return ContainerEntry
   */
  @SuppressWarnings("unchecked")
  private ContainerEntry newContainerEntry(String containerType, String sourceName, String dbType, boolean  isMulti,
      String storeType, String filterType, String bufferValue, String swapPath, String storePath, boolean isUpdateStore,String valueStorageId) {
    List containerParams = new ArrayList();
    containerParams.add(new SimpleParameterEntry(KEY_SOURCENAME, sourceName)) ;
    containerParams.add(new SimpleParameterEntry(KEY_DIALECT, dbType)) ;
    containerParams.add(new SimpleParameterEntry(KEY_MULTIDB, String.valueOf(isMulti))) ;
    containerParams.add(new SimpleParameterEntry(KEY_UPDATESTORE, String.valueOf(isUpdateStore))) ;
    containerParams.add(new SimpleParameterEntry(KEY_MAXBUFFER, bufferValue)) ;
    containerParams.add(new SimpleParameterEntry(KEY_SWAPDIRECTORY, swapPath)) ;
    ContainerEntry containerEntry = new ContainerEntry(containerType, (ArrayList) containerParams) ;      
    containerEntry.setParameters(containerParams);
    
    if(storeType != null) {
      ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
      ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
      filterEntry.setPropertyType(filterType);
      vsparams.add(filterEntry);
      
      ValueStorageEntry valueStorageEntry = new ValueStorageEntry(storeType,
          vsparams);
      ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
      spe.add(new SimpleParameterEntry(KEY_PATH, storePath));
      valueStorageEntry.setId(valueStorageId);
      valueStorageEntry.setParameters(spe);
      valueStorageEntry.setFilters(vsparams);
      ArrayList list = new ArrayList(1);
      list.add(valueStorageEntry);
      containerEntry.setValueStorages(list);
    } else {
      containerEntry.setValueStorages(new ArrayList());
    }
    return containerEntry ;
  }
  
  /**
   * Add informations to CacheEntry
   * @param isCache
   * @param maxSizeValue
   * @param liveTimeValue
   * @return CacheEntry
   */
  private CacheEntry newCacheEntry(boolean isCache, String maxSizeValue, String liveTimeValue) {
    CacheEntry cache = new CacheEntry() ;
    cache.setEnabled(isCache) ;      
    ArrayList<SimpleParameterEntry> cacheParams = new ArrayList<SimpleParameterEntry>() ;
    cacheParams.add(new SimpleParameterEntry(KEY_MAXSIZE, maxSizeValue)) ;
    cacheParams.add(new SimpleParameterEntry(KEY_LIVETIME, liveTimeValue)) ;
    cache.setParameters(cacheParams) ;
    return cache ;
  }
  
  /**
   * Add information to QueryHandlerEntry
   * @param queryHandlerType
   * @param indexPath
   * @param supportHighlighting
   * @param excerptClass
   * @return QueryHandlerEntry
   */
  private QueryHandlerEntry newQueryHandlerEntry(String queryHandlerType, String indexPath, 
      boolean supportHighlighting, String excerptClass) {
    List<SimpleParameterEntry> queryParams = new ArrayList<SimpleParameterEntry>() ;
    queryParams.add(new SimpleParameterEntry(KEY_INDEXDIR, indexPath)) ;
    queryParams.add(new SimpleParameterEntry(KEY_SUPPORT_HIGHLIGHTING, Boolean.toString(supportHighlighting))) ;
    queryParams.add(new SimpleParameterEntry(KEY_EXCERPT_PROVIDER, excerptClass)) ;
    QueryHandlerEntry queryHandler = new QueryHandlerEntry(queryHandlerType, queryParams) ;
    queryHandler.setType(queryHandlerType) ;
    queryHandler.setParameters(queryParams) ;
    return queryHandler ;
  }  
  
  public void stop() {
  }


}
