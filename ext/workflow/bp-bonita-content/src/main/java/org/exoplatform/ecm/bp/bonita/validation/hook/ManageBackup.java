package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.exoplatform.services.cms.CmsService;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class ManageBackup implements TxHook {

  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    backupContent(api, activity);
  }

  private void backupContent(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    String[] currentLocation = ProcessUtil.getCurrentLocation(api, activity);
    String currentWorkspace = currentLocation[1];
    String currentPath = currentLocation[2];
    String backupWorkspace = (String) api.getQueryRuntimeAPI()
                                         .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                     "exo:backupWorkspace");
    String backupPath = (String) api.getQueryRuntimeAPI()
                                    .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                "exo:backupPath");
    String realBackupPath = ProcessUtil.computeDestinationPath(currentPath, backupPath);
    CmsService cmsService = ProcessUtil.getService(CmsService.class);
    cmsService.moveNode(currentPath, currentWorkspace, backupWorkspace, realBackupPath);
    ProcessUtil.setCurrentLocation(api, activity, backupWorkspace, realBackupPath);
    ProcessUtil.backup(api, activity);
  }
}
