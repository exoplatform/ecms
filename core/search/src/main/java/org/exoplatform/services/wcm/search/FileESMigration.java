package org.exoplatform.services.wcm.search;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.search.index.IndexingOperationProcessor;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.InterruptableJob;
import org.quartz.UnableToInterruptJobException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.concurrent.CompletableFuture;

/**
 * Upgrade plugin to index all files in Elasticsearch
 */
public class FileESMigration extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(FileESMigration.class);

  public static final String FILE_ES_INDEXATION_KEY = "FILE_ES_INDEXATION";

  public static final String FILE_ES_INDEXATION_DONE_KEY = "FILE_ES_INDEXATION_DONE";

  public static final String FILE_JCR_COLLABORATION_REINDEXATION_DONE_KEY = "FILE_JCR_COLLABORATION_REINDEXATION_DONE";

  public static final String FILE_JCR_SYSTEM_REINDEXATION_DONE_KEY = "FILE_JCR_SYSTEM_REINDEXATION_DONE";

  private IndexingService indexingService;

  private SettingService settingService;

  private RepositoryService repositoryService;

  private IndexingOperationProcessor indexingOperationProcessor;

  private JobSchedulerService jobSchedulerService;

  public FileESMigration(InitParams initParams,
                         IndexingService indexingService,
                         SettingService settingService,
                         RepositoryService repositoryService,
                         IndexingOperationProcessor indexingOperationProcessor,
                         JobSchedulerService jobSchedulerService) {
    super(initParams);
    this.indexingService = indexingService;
    this.settingService = settingService;
    this.repositoryService = repositoryService;
    this.indexingOperationProcessor = indexingOperationProcessor;
    this.jobSchedulerService = jobSchedulerService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    try {
      if (!isIndexationInESDone()) {
        printNumberOfFileToIndex();
        indexInES();
      }

      if (!isJCRReindexationDone()) {
        reindexJCR();
      }
    } catch(Exception e) {
      throw new RuntimeException("Error while Files indexing in ES migration", e);
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return !(isIndexationInESDone() && isJCRReindexationDone());
  }

  public void reindexJCR() throws Exception {
    try {
      SearchManager searchManager = (SearchManager) repositoryService.getCurrentRepository().getWorkspaceContainer("collaboration").getComponent(SearchManager.class);

      if(!isJCRCollaboraionReindexationDone()) {
        LOG.info("== Files ES migration - Starting reindexation of JCR collaboration workspace");
        CompletableFuture<Boolean> reindexCollaborationWSResult = searchManager.reindexWorkspace(false, 0);
        reindexCollaborationWSResult.thenAccept(successful -> {
          if (successful) {
            LOG.info("== Files ES migration - Reindexation of JCR collaboration workspace done");
            settingService.set(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_JCR_COLLABORATION_REINDEXATION_DONE_KEY, SettingValue.create(true));
          } else {
            LOG.error("== Files ES migration - Reindexation of JCR collaboration workspace failed. " +
                    "Check logs to fix the issue, then reindex it by restarting the server");
          }
        });
      }

      if(!isJCRSystemReindexationDone()) {
        LOG.info("== Files ES migration - Starting reindexation of JCR system workspace");
        SystemSearchManager systemSearchManager = (SystemSearchManager) repositoryService.getCurrentRepository().getWorkspaceContainer("system").getComponent(SystemSearchManager.class);
        CompletableFuture<Boolean> reindexSystemWSResult = systemSearchManager.reindexWorkspace(false, 0);
        reindexSystemWSResult.thenAccept(successful -> {
          if (successful) {
            LOG.info("== Files ES migration - Reindexation of JCR system workspace done");
            settingService.set(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_JCR_SYSTEM_REINDEXATION_DONE_KEY, SettingValue.create(true));
          } else {
            LOG.error("== Files ES migration - Reindexation of JCR system workspace failed. " +
                    "Check logs to fix the issue, then reindex it by restarting the server");
          }
        });
      }
    } catch (RepositoryException e) {
      throw new Exception("Error while reindexing JCR collaboration and system workspaces", e);
    }
  }

  /**
   * Run the reindex all operation to fill the indexing queue with all files to index.
   * ESBulkIndexer job is disabled and indexing queue processing (process()) is done
   * synchronously to make sure the flag is set only when it is really done (allow to redo it in case
   * of interruption), and also to make sur JCR reindexing is not launch during this operation (which will
   * impact it since the reindex all operation use the JCR query manager to get all files).
   */
  public void indexInES() throws Exception {
    // Pause ESBulkIndexer job to avoid concurrent executions in the process() operation
    LOG.info("== Files ES migration - Pause ESBulkIndexer job");
    jobSchedulerService.pauseJob("ESBulkIndexer", "ElasticSearch");

    try {
      LOG.info("== Files ES migration - Stopping executing jobs ESBulkIndexer instances...");
      jobSchedulerService.getAllExcutingJobs().stream()
              .filter(jobExecutionContext -> jobExecutionContext.getJobDetail().getKey().getName().equals("ESBulkIndexer"))
              .forEach(indexingJob -> {
                try {
                  LOG.info("== Files ES migration - Interrupting executing job ESBulkIndexer instance " + indexingJob.getFireInstanceId());
                  ((InterruptableJob) indexingJob.getJobInstance()).interrupt();
                } catch (UnableToInterruptJobException e) {
                  LOG.error("Error while interrupting ES indexing queue job to run the migration", e);
                }
              });

      LOG.info("== Files ES migration - Clear indexing queue for files");
      indexingService.clearQueue(FileindexingConnector.TYPE);

      LOG.info("== Files ES migration - Starting pushing all files in indexation queue");
      indexingService.reindexAll(FileindexingConnector.TYPE);

      // process the reindexAll operation synchronously to make sure it is done before the JCR workspace reindexation (otherwise JCR queries will not retrieve nodes)
      processIndexation();
      settingService.set(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_ES_INDEXATION_DONE_KEY, SettingValue.create(true));
      LOG.info("== Files ES migration - Push of all files in indexation queue done");
    } catch(Exception e) {
      throw new Exception("Error while indexing files in ES", e);
    } finally {
      LOG.info("== Files ES migration - Resume ESBulkIndexer job");
      jobSchedulerService.resumeJob("ESBulkIndexer", "ElasticSearch");
    }
  }

  @ExoTransactional
  public void processIndexation() {
    LOG.info("== Files ES migration - Process files");
    indexingOperationProcessor.process();
  }

  private boolean isIndexationInESDone() {
    SettingValue<?> done = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_ES_INDEXATION_DONE_KEY);
    return done != null && done.getValue().equals("true");
  }

  private boolean isJCRReindexationDone() {
    return isJCRCollaboraionReindexationDone() && isJCRSystemReindexationDone();
  }

  private boolean isJCRCollaboraionReindexationDone() {
    SettingValue<?> done = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_JCR_COLLABORATION_REINDEXATION_DONE_KEY);
    return done != null && done.getValue().equals("true");
  }

  private boolean isJCRSystemReindexationDone() {
    SettingValue<?> done = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(FILE_ES_INDEXATION_KEY), FILE_JCR_SYSTEM_REINDEXATION_DONE_KEY);
    return done != null && done.getValue().equals("true");
  }

  private void printNumberOfFileToIndex() {
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession("collaboration", repositoryService.getCurrentRepository());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("select * from " + NodetypeConstant.NT_FILE, Query.SQL);
      QueryResult result = query.execute();
      LOG.info("== Files ES migration - Number of files to index : " + result.getNodes().getSize());
    } catch (RepositoryException e) {
      LOG.error("== Files ES migration - Error while counting all nt:file to index", e);
    }
  }
}
