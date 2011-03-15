package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;

public class SetDelegator implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
  throws Exception {
    String delegator = ((TaskInstance)activity.getBody()).getTaskUser();
    api.getRuntimeAPI().setProcessInstanceVariable(activity.getProcessInstanceUUID(), "delegator", delegator);
  }

}
