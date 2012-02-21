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
public class FrontCronJob extends CronJob {

    private static final Log log = ExoLogger.getLogger(FrontCronJob.class);

    private JobDataMap jobDataMap;

    /**
     *
     * @param params
     *            : les parametres d'init pour le plugin
     * @throws Exception
     */
    public FrontCronJob(InitParams params) throws Exception {
      super(params);
      if (log.isInfoEnabled()) {
          log.info("Start Init CronJob");
      }
    
      jobDataMap = new JobDataMap();
    
      ExoProperties props = params.getPropertiesParam("importContentJob.generalParams").getProperties();
      String stagingStorage = props.getProperty("stagingStorage");
      String temporaryStorge = props.getProperty("temporaryStorge");
      jobDataMap.put("stagingStorage", stagingStorage);
      jobDataMap.put("temporaryStorge", temporaryStorge);
      if (log.isInfoEnabled()) {
        log.info("CronJob Params...stagingStorage : " + stagingStorage + ", temporaryStorge :" + temporaryStorge);
        log.info("End Init CronJob");
      }
    }

    /**
     *
     * @return JobDataMap
     */
    public JobDataMap getJobDataMap() {
  return jobDataMap;
    }

}
