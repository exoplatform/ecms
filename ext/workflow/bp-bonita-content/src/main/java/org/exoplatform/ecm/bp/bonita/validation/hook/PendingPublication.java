package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.exoplatform.services.cms.CmsService;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class PendingPublication implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    moveToPending(api, activity);
  }

  private void moveToPending(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    String[] currentLocation = ProcessUtil.getCurrentLocation(api, activity);
    String currentWorkspace = currentLocation[1];
    String currentPath = currentLocation[2];
    String pendingWorksapce = (String) api.getQueryRuntimeAPI()
                                          .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                      "exo:pendingWorkspace");
    String pendingPath = (String) api.getQueryRuntimeAPI()
                                     .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                 "exo:pendingPath");
    String destPath = ProcessUtil.computeDestinationPath(currentPath, pendingPath);
    CmsService cmsService = ProcessUtil.getService(CmsService.class);
    cmsService.moveNode(currentPath, currentWorkspace, pendingWorksapce, destPath);
    ProcessUtil.setCurrentLocation(api, activity, pendingWorksapce, destPath);
    ProcessUtil.waitForPublish(api, activity);
  }

}
