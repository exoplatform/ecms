package org.exoplatform.services.cms.scheduler;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/5/15
 * Implement job to control version of documents
 */
public class DocumentAutoVersionJob implements Job{

  private static final Log log = ExoLogger.getLogger(DocumentAutoVersionJob.class);

  private static final String DOCUMENT_AUTO_JOB_NAME = "Remove_expired_version";
  private static JobSchedulerService schedulerService = WCMCoreUtils.getService(JobSchedulerService.class);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Job is running: "+jobExecutionContext);
    String nodeUUID="";
//      jobExecutionContext.
    removeJob(nodeUUID);
  }

  public static void removeJob(String nodeUUID){
    try {
      schedulerService.removeJob(new JobInfo(DOCUMENT_AUTO_JOB_NAME, "", DocumentAutoVersionJob.class));
    }catch (Exception ex){
      log.error(ex.getMessage());
    }
  }

  public static void addJob(String nodePath, int day) throws Exception{
    Date date = new Date();
    JobInfo jobInfo = new JobInfo(DocumentAutoVersionJob.class);
    jobInfo.setJobName(DOCUMENT_AUTO_JOB_NAME);
    jobInfo.setDescription("Remove expired versions which created from Document Auto Versioning");
//    PeriodInfo periodInfo = new PeriodInfo();

    schedulerService.addJob(jobInfo, date);
  }

}
