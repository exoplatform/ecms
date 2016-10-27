package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/13/15
 * Make document's version follow by Document Auto Versioning function specification
 */
public class AutoVersionServiceImpl implements AutoVersionService{

  private ManageDriveService manageDriveService;
  private List<String> lstDriveAutoVersion = new ArrayList<String>();
  private int maxVersionNumber=0;

  public AutoVersionServiceImpl(ManageDriveService manageDriveService, InitParams params) {
    this.manageDriveService = manageDriveService;
    String driveAutoVersion = params.getValueParam(DRIVES_AUTO_VERSION).getValue();
    maxVersionNumber = Integer.parseInt(params.getValueParam(DRIVES_AUTO_VERSION_MAX).getValue());
    if(StringUtils.isNotEmpty(driveAutoVersion)) lstDriveAutoVersion = Arrays.asList(driveAutoVersion.split(","));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Version autoVersion(Node currentNode) throws Exception {
    return autoVersion(currentNode,false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Version autoVersion(Node currentNode, boolean isSkipCheckDrive) throws Exception {
    manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
    if(currentNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      currentNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    }

    if(isSkipCheckDrive){
      Version createdVersion = VersionHistoryUtils.createVersion(currentNode);
      addRootVersionLabelsToCreatedVersion(currentNode, createdVersion);
      return createdVersion;
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
        Version createdVersion = VersionHistoryUtils.createVersion(currentNode);
        addRootVersionLabelsToCreatedVersion(currentNode, createdVersion);
        return createdVersion;
      }
    }
    return null;
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
  
  private void addRootVersionLabelsToCreatedVersion(Node currentNode, Version createdVersion) throws RepositoryException {
    VersionHistory versionHistory = currentNode.getVersionHistory();
    String[] oldVersionLabels = versionHistory.getVersionLabels(versionHistory.getRootVersion());
    if(oldVersionLabels != null) {
      for (String oldVersionLabel : oldVersionLabels) {
        versionHistory.addVersionLabel(createdVersion.getName(), oldVersionLabel, true);
        versionHistory.getSession().save();
      }
    }
  }
}
