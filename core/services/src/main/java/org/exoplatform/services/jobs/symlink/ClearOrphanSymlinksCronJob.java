package org.exoplatform.services.jobs.symlink;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.CronJob;
import org.quartz.JobDataMap;

/**
 * Created by eXo Platform
 */
public class ClearOrphanSymlinksCronJob extends CronJob {

  private static final Log log = ExoLogger.getLogger(ClearOrphanSymlinksCronJob.class);

  private JobDataMap jobDataMap;

  private String expression;

  public ClearOrphanSymlinksCronJob(InitParams params) throws Exception {
    super(params);
    if (log.isInfoEnabled()) {
      log.info("Start Init ClearOrphanSymlinksCronJobImpl");
    }
    jobDataMap = new JobDataMap();
    ExoProperties props = params.getPropertiesParam("cronjob.info")
                                .getProperties();
    expression = props.getProperty("expression");

    if (expression.startsWith("$")) {
      log.info("CronJob Param...expression: no default value for : "+expression);
      expression = "0 30 1 * * ?";
      props.setProperty("expression", expression);
      jobDataMap.put("expression", expression);
    }

    if (log.isInfoEnabled()) {
      log.info("CronJob Param...expression: " + expression);
      log.info("End Init ClearOrphanSymlinksCronJobImpl");
    }
  }

  public JobDataMap getJobDataMap() {
    return jobDataMap;
  }

  public String getExpression() {
    return expression;
  }
}