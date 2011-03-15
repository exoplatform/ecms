package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ProcessInstance;

public class SetInitiator implements TxHook {



  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    String initiator = api.getQueryRuntimeAPI().getProcessInstance(activity.getProcessInstanceUUID()).getStartedBy();
    ProcessInstance pi = api.getQueryRuntimeAPI().getProcessInstance(activity.getProcessInstanceUUID());
    api.getRuntimeAPI().setProcessInstanceVariable(pi.getProcessInstanceUUID(), "initiator", initiator);
  }

}
