package org.exoplatform.services.cms.documents.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.ext.utils.VersionHistoryUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cms.drives.DriveData;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/13/15
 * Make document's version follow by Document Auto Versioning function specification
 */
public class AutoVersionServiceImpl implements AutoVersionService{

  private static Log log = ExoLogger.getLogger(AutoVersionServiceImpl.class);

  private ManageDriveService manageDriveService;
  private List<String> lstDriveAutoVersion = new ArrayList<String>();
  private int maxVersionNumber=0;
  private int expiredTimeVersion =0;

  public AutoVersionServiceImpl(ManageDriveService manageDriveService, InitParams params) {
    this.manageDriveService = manageDriveService;
    String driveAutoVersion = params.getValueParam(DRIVES_AUTO_VERSION).getValue();
    maxVersionNumber = Integer.parseInt(params.getValueParam(DRIVES_AUTO_VERSION_MAX).getValue());
    expiredTimeVersion = Integer.parseInt(params.getValueParam(DRIVES_AUTO_VERSION_EXPIRED).getValue());
    if(StringUtils.isNotEmpty(driveAutoVersion)) lstDriveAutoVersion = Arrays.asList(driveAutoVersion.split(","));
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void autoVersion(Node currentNode) throws Exception {
    autoVersion(currentNode,false);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void autoVersion(Node currentNode, boolean isSkipCheckDrive) throws Exception {
    manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
    if(currentNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    }

    if(currentNode.canAddMixin(NodetypeConstant.EXO_MODIFY)){
      currentNode.addMixin(NodetypeConstant.EXO_MODIFY);
      currentNode.setProperty(NodetypeConstant.EXO_DATE_CREATED, new GregorianCalendar());
    }

    currentNode.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, new GregorianCalendar());
    
    ConversationState conversationState = ConversationState.getCurrent();
    String userName = (conversationState == null) ? currentNode.getSession().getUserID() :
                                                    conversationState.getIdentity().getUserId();
    currentNode.setProperty(NodetypeConstant.EXO_LAST_MODIFIER, userName);
    currentNode.save();

    if(isSkipCheckDrive){
      VersionHistoryUtils.createVersion(currentNode);
      return;
    }
    String nodePath = currentNode.getPath();
    for (String driveAutoVersion: lstDriveAutoVersion){
      DriveData driveData = manageDriveService.getDriveByName(StringUtils.trim(driveAutoVersion));
      if(driveData==null) continue;
      String driveHomePath = driveData.getHomePath();
      if(!StringUtils.equals(driveData.getWorkspace(), currentNode.getSession().getWorkspace().getName())) continue;
      if((driveHomePath.startsWith(PERSONAL_DRIVE_PARRTEN) && nodePath.startsWith(PERSONAL_DRIVE_PREFIX)) ||
              driveHomePath.startsWith(GROUP_DRIVE_PARRTEN) && nodePath.startsWith(GROUP_DRIVE_PREFIX) ||
              nodePath.startsWith(driveHomePath)){
        VersionHistoryUtils.createVersion(currentNode);
        return;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isVersionSupport(String nodePath, String workspace) throws Exception {
    if(StringUtils.isEmpty(nodePath)) return false;
    for (String driveAutoVersion: lstDriveAutoVersion){
      DriveData driveData = manageDriveService.getDriveByName(StringUtils.trim(driveAutoVersion));
      if(driveData==null) continue;
      String driveHomePath = driveData.getHomePath();
      if(!StringUtils.equals(driveData.getWorkspace(), workspace)) continue;
      if((driveHomePath.startsWith(PERSONAL_DRIVE_PARRTEN) && nodePath.startsWith(PERSONAL_DRIVE_PREFIX)) ||
              driveHomePath.startsWith(GROUP_DRIVE_PARRTEN) && nodePath.startsWith(GROUP_DRIVE_PREFIX) ||
              nodePath.startsWith(driveHomePath)){
        return true;
      }
    }
    return false;
  }

   @Override
   public List<String> getDriveAutoVersion()
   {
      return lstDriveAutoVersion;
   }
   @Override
   public void autoVersion(Node currentNode, Node sourceNode) throws Exception {
     autoVersion(currentNode,sourceNode,false);
   }
   @Override
  public void autoVersion(Node currentNode, Node sourceNode, boolean isSkipDriveCheck) throws Exception {
    manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
    if(currentNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
      currentNode.save();
    }
    if(isSkipDriveCheck){
      createVersion(currentNode, sourceNode);
      return;
    }
    for (String driveAutoVersion: lstDriveAutoVersion){
      DriveData driveData = manageDriveService.getDriveByName(StringUtils.trim(driveAutoVersion));
      if(driveData==null) continue;
      String driveHomePath = driveData.getHomePath();
      String nodePath = currentNode.getPath();
      if(!StringUtils.equals(driveData.getWorkspace(), currentNode.getSession().getWorkspace().getName())) continue;
      if((driveHomePath.startsWith(PERSONAL_DRIVE_PARRTEN) && nodePath.startsWith(PERSONAL_DRIVE_PREFIX)) ||
              driveHomePath.startsWith(GROUP_DRIVE_PARRTEN) && nodePath.startsWith(GROUP_DRIVE_PREFIX) ||
          nodePath.startsWith(driveHomePath)){
        createVersion(currentNode, sourceNode);
        return;
      }
    }
  }

  /**
   * Create version with jcr:content is source node
   * @param currentNode
   * @param sourceNode
   * @return
   * @throws Exception
   */
  private boolean createVersion(Node currentNode, Node sourceNode)throws Exception{
    if(currentNode.canAddMixin(NodetypeConstant.MIX_VERSIONABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_VERSIONABLE);
      currentNode.save();
      return true;
    }
    long allCurrentVersions = currentNode.getVersionHistory().getAllVersions().getSize();
    if(maxVersionNumber==DOCUMENT_AUTO_DEFAULT_VERSION_MAX || maxVersionNumber >= allCurrentVersions){
      VersionHistoryUtils.createVersion(currentNode);
      Node jcrContent = currentNode.hasNode(NodetypeConstant.JCR_CONTENT)?
              currentNode.getNode(NodetypeConstant.JCR_CONTENT):currentNode.addNode(NodetypeConstant.JCR_CONTENT);
      Node srcJcrContent = sourceNode.getNode(NodetypeConstant.JCR_CONTENT);
      if(srcJcrContent.getProperty(NodetypeConstant.JCR_DATA).getStream().available()>0) {
        jcrContent.setProperty(NodetypeConstant.JCR_DATA, srcJcrContent.getProperty(NodetypeConstant.JCR_DATA).getStream());
      }
      currentNode.save();
      return true;
    }
    return false;
  }
}
