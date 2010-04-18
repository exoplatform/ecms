package org.exoplatform.services.wcm.extensions.scheduler;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.CronJob;
import org.quartz.JobDataMap;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class ChangeStateCronJob extends CronJob {

  private static final Log log = ExoLogger.getLogger(ChangeStateCronJob.class);

  private JobDataMap       jobDataMap;

  public ChangeStateCronJob(InitParams params) throws Exception {
    super(params);
    if (log.isInfoEnabled()) {
      log.info("Start Init ChangeStateCronJob");
    }
    ExoProperties props = params.getPropertiesParam("changeStateCronJob.generalParams")
                                .getProperties();
    jobDataMap = new JobDataMap();
    String fromState = props.getProperty("fromState");
    jobDataMap.put("fromState", fromState);
    String toState = props.getProperty("toState");
    jobDataMap.put("toState", toState);
    String predefinedPath = props.getProperty("predefinedPath");
    jobDataMap.put("predefinedPath", predefinedPath);
    if (log.isInfoEnabled()) {
      log.info("CronJob Param...fromState : " + fromState + ", toState : " + toState
          + ", predefinedPath : " + predefinedPath);

      log.info("End Init ChangeStateCronJob");
    }
  }

  public JobDataMap getJobDataMap() {
    return jobDataMap;
  }

}
