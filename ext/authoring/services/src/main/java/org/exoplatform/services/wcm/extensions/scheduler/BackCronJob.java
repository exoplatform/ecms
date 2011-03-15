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
    /**
     * @param args
     */

    private static final Log log = ExoLogger.getLogger(BackCronJob.class);

    private JobDataMap jobDataMap;

    public BackCronJob(InitParams params) throws Exception {
  super(params);
  if (log.isInfoEnabled()) {
      log.info("Start Init BackCronJob");
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
  if (log.isInfoEnabled()) {
      log.info("CronJob Param...fromState : " + fromState + ", toState : " + toState + ", localTempDir : " + localTempDir
        + ", targetServerUrl : " + targetServerUrl);

      log.info("End Init BackCronJob");
  }
    }

    public JobDataMap getJobDataMap() {
  return jobDataMap;
    }

}
