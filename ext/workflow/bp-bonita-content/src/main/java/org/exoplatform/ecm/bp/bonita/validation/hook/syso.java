package org.exoplatform.ecm.bp.bonita.validation.hook;

import java.util.logging.Logger;

import org.ow2.bonita.definition.Hook;
import org.ow2.bonita.facade.QueryAPIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class syso implements Hook {

  private static Logger log = Logger.getLogger(syso.class.getName());

  public void execute(QueryAPIAccessor arg0,
      ActivityInstance<ActivityBody> arg1) throws Exception {
    log.info("#####################   I AM IN THE ACTIVITY : " + arg1.getActivityId());

  }

}
