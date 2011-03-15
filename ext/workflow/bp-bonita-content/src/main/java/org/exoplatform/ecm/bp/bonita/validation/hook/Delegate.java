package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class Delegate implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    ProcessUtil.delegate(api,activity);
  }

}
