package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;

public class ExecutionActivity implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    if(TaskInstance.class.isInstance(activity.getBody())){
      api.getRuntimeAPI().startTask(TaskInstance.class.cast(activity.getBody()).getUUID(), false);
      api.getRuntimeAPI().finishTask(TaskInstance.class.cast(activity.getBody()).getUUID(), false);
    }
  }

}
