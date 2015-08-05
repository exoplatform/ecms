package org.exoplatform.services.cms.listeners;

import javax.jcr.Node;
import javax.jcr.version.Version;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.scheduler.DocumentAutoVersionJob;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.exoplatform.services.cms.impl.Utils;
/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/13/15
 * Make document's version follow by Document Auto Versioning function specification
 */
public class DocumentAutoVersionEventListener extends Listener<Object, Node> {

  private static Log log = ExoLogger.getLogger(DocumentAutoVersionEventListener.class);

  public static final String DOCUMENT_AUTO_VERSIONING_LISTENER = "drive.document.autoversioning.event.listener";
  private final String DRIVES_AUTO_VERSION = "ecms.documents.versioning.drives";
  private final String DRIVES_AUTO_VERSION_MAX = "ecms.documents.versions.max";
  private final String DRIVES_AUTO_VERSION_EXPIRED = "ecms.documents.versions.expiration";
  private final int DOCUMENT_AUTO_DEFAULT_VERSION_MAX=0;
  private final int DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED=0;
  private final String PERSIONAL_DRIVE_PREFIX = "/Users";

  private ManageDriveService manageDriveService;
  private List<String> lstDriveAutoVersion = new ArrayList<String>();
  private int maxVersionNumber=0;
  private int expiredTimeVersion =0;

  public DocumentAutoVersionEventListener(ManageDriveService manageDriveService, InitParams params) {
    this.manageDriveService = manageDriveService;
    String driveAutoVersion = params.getValueParam(DRIVES_AUTO_VERSION).getValue();
    maxVersionNumber = Integer.parseInt(params.getValueParam(DRIVES_AUTO_VERSION_MAX).getValue());
    expiredTimeVersion = Integer.parseInt(params.getValueParam(DRIVES_AUTO_VERSION_EXPIRED).getValue());
    if(StringUtils.isNotEmpty(driveAutoVersion)) lstDriveAutoVersion = Arrays.asList(driveAutoVersion.split(","));
  }

  public void onEvent(Event<Object, Node> event) throws Exception {
    Node currentNode = event.getData();
    manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);

    if(currentNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
      currentNode.save();
    }
    if(currentNode.getPath().startsWith(PERSIONAL_DRIVE_PREFIX)){
      createVersion(currentNode);
      return;
    }
    List<DriveData> userDriveDatas = manageDriveService.getDriveByUserRoles(WCMCoreUtils.getRemoteUser(), Utils.getMemberships());

    for (String driveAutoVersion: lstDriveAutoVersion){
      if(driveAutoVersion.startsWith(PERSIONAL_DRIVE_PREFIX)) continue;

      for (DriveData driveData:userDriveDatas){
        if(currentNode.getPath().contains(driveData.getHomePath())){
          createVersion(currentNode);
          return;
        }
      }
    }
  }

  private boolean createVersion(Node currentNode)throws Exception{
    if(currentNode.canAddMixin(NodetypeConstant.MIX_VERSIONABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_VERSIONABLE);
      currentNode.getSession().save();
    }
    long allCurrentVersions = currentNode.getVersionHistory().getAllVersions().getSize();
    if(maxVersionNumber==DOCUMENT_AUTO_DEFAULT_VERSION_MAX || maxVersionNumber >= allCurrentVersions){
      currentNode.checkin();
      currentNode.checkout();
      currentNode.getParent().save();

      //add schedule to remove version
      if(expiredTimeVersion > DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED){
        // add job to remove version
        Version baseVersion = currentNode.getBaseVersion();
        DocumentAutoVersionJob.addJob(baseVersion.getUUID(), expiredTimeVersion);
      }

      return true;
    }
    return false;
  }
}
