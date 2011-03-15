package org.exoplatform.ecm.bp.bonita.validation.hook;

import java.util.Date;

import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class SetPending implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    //Get the start publication date
    Date startDate = (Date) api.getQueryRuntimeAPI().getProcessInstanceVariable(activity.getProcessInstanceUUID(), "startDate");
    //Get the current date
    Date currentDate = new Date();

    if(currentDate.after(startDate) || currentDate.compareTo(startDate)==0){
      // No need to pending publication
      api.getRuntimeAPI().setVariable(activity.getUUID(), "pending", new Boolean(false));
    } else {
      api.getRuntimeAPI().setVariable(activity.getUUID(), "pending", new Boolean(true));
    }
  }

}
