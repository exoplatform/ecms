package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.exoplatform.services.cms.CmsService;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class TrashMovement implements TxHook{


  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity)
      throws Exception {
    moveTrash(api, activity);
  }

  private void moveTrash(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
      String[] location = ProcessUtil.getCurrentLocation(api,activity);
      String currentWorkspace = location[1];
      String currentPath = location[2];
    String trashWorkspace = (String) api.getQueryRuntimeAPI()
                                        .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                    "exo:trashWorkspace");
    String trashPath = (String) api.getQueryRuntimeAPI()
                                   .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                               "exo:trashPath");
      String destPath = ProcessUtil.computeDestinationPath(currentPath,trashPath);
      CmsService cmsService = ProcessUtil.getService(CmsService.class);
      cmsService.moveNode(currentPath, currentWorkspace, trashWorkspace, destPath);
      ProcessUtil.setCurrentLocation(api,activity,trashWorkspace,destPath);
      ProcessUtil.moveTrash(api,activity);
    }


}
