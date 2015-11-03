package org.exoplatform.services.cms.scheduler;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/5/15
 * Implement job to control version of documents
 */
public class DocumentAutoVersionJob implements Job {

  private static final Log log = ExoLogger.getLogger(DocumentAutoVersionJob.class);

  private static final String DOCUMENT_AUTO_JOB_NAME = "Remove_expired_version";
  private static JobSchedulerService schedulerService = WCMCoreUtils.getService(JobSchedulerService.class);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Job is running: " + jobExecutionContext);
    JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
    String nodeUUID  = jobDataMap.getString("nodeUUID");
    String workspace = jobDataMap.getString("workspace");
    int day          = Integer.parseInt(jobDataMap.getString("day"));

    try{
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace, WCMCoreUtils.getRepository());
      Node currentNode = session.getNodeByUUID(nodeUUID);
      if(currentNode.getBaseVersion().getCreated().before(day) || currentNode.getBaseVersion().getCreated().equals(day)){
        currentNode.remove();
        log.info("delete node path: "+currentNode.getPath(), " uuid: "+currentNode.getUUID());
        currentNode.getSession().save();
      }
    }catch(RepositoryException re){

    }

    removeJob(nodeUUID);
  }

  public static void removeJob(String nodeUUID) {
    try {
      schedulerService.removeJob(new JobInfo(DOCUMENT_AUTO_JOB_NAME, "", DocumentAutoVersionJob.class));
    } catch (Exception ex) {
      log.error(ex.getMessage());
    }
  }

  public static void addJob(String workspace, String nodeUUID, int day) throws Exception {
    JobInfo jobInfo = new JobInfo(DocumentAutoVersionJob.class);
    jobInfo.setJobName(DOCUMENT_AUTO_JOB_NAME);
    jobInfo.setDescription("Remove expired versions which created from Document Auto Versioning");
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("workspace", workspace);
    jobDataMap.put("nodeUUID", nodeUUID);
    jobDataMap.put("day", day);
    PeriodInfo periodInfo = new PeriodInfo(Calendar.getInstance().getTime(), null, -1, day * 864001000);
    schedulerService.addPeriodJob(jobInfo, periodInfo, jobDataMap);
  }

}
