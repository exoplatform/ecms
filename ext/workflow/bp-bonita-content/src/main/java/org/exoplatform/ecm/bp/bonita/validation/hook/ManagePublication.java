package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.exoplatform.services.cms.CmsService;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class ManagePublication implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    publishContent(api,activity);
  }

  private void publishContent(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
      String[] currentLocation = ProcessUtil.getCurrentLocation(api,activity);
      String currentWorkspace = currentLocation[1];
      String currentPath = currentLocation[2];
    String publishWorkspace = (String) api.getQueryRuntimeAPI()
                                          .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                      "exo:publishWorkspace");
    String publishPath = (String) api.getQueryRuntimeAPI()
                                     .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                 "exo:publishPath");
      String realPublishPath = ProcessUtil.computeDestinationPath(currentPath,publishPath);
      CmsService cmsService = ProcessUtil.getService(CmsService.class);
      cmsService.moveNode(currentPath, currentWorkspace, publishWorkspace, realPublishPath);
      api.getRuntimeAPI().setVariable(activity.getUUID(),ProcessUtil.CURRENT_STATE,ProcessUtil.LIVE);
      ProcessUtil.setCurrentLocation(api,activity,publishWorkspace,realPublishPath);
      ProcessUtil.publish(api,activity);
    }
}
