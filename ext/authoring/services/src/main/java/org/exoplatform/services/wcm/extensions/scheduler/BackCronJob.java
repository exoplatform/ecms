package org.exoplatform.services.wcm.extensions.scheduler;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.CronJob;
import org.quartz.JobDataMap;

/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class BackCronJob extends CronJob {

    private static final Log LOG = ExoLogger.getLogger(BackCronJob.class.getName());

    private JobDataMap jobDataMap;

    public BackCronJob(InitParams params) throws Exception {
  super(params);
  if (LOG.isInfoEnabled()) {
    LOG.info("Start Init BackCronJob");
  }
  ExoProperties props = params.getPropertiesParam("exportContentJob.generalParams").getProperties();
  jobDataMap = new JobDataMap();
  String fromState = props.getProperty("fromState");
  jobDataMap.put("fromState", fromState);
  String toState = props.getProperty("toState");
  jobDataMap.put("toState", toState);
  String localTempDir = props.getProperty("localTempDir");
  jobDataMap.put("localTempDir", localTempDir);
  String targetServerUrl = props.getProperty("targetServerUrl");
  jobDataMap.put("targetServerUrl", targetServerUrl);
  String targetKey = props.getProperty("targetKey");
  jobDataMap.put("targetKey", targetKey);
  String predefinedPath = props.getProperty("predefinedPath");
  jobDataMap.put("predefinedPath", predefinedPath);
  if (LOG.isInfoEnabled()) {
    LOG.info("CronJob Param...fromState : " + fromState + ", toState : " + toState + ", localTempDir : " + localTempDir
        + ", targetServerUrl : " + targetServerUrl);

    LOG.info("End Init BackCronJob");
  }
    }

    public JobDataMap getJobDataMap() {
  return jobDataMap;
    }

}
