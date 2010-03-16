/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.migration;

import java.util.List;

import javax.jcr.Session;

import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 8, 2009 2:47:00 PM
 */
/**
 * This service will be used to migrate drives name which contains the invalid characters
 */
public class DriveMigrationService implements Startable {

  final static private String DRIVE_HOME_PATH_ALIAS = "exoDrivesPath";
  
  private Log log = ExoLogger.getLogger("DRIVE MIGRATION") ;
  private ManageDriveService driveService_ ;
  private RepositoryService repositoryService_;
  private String baseDrivePath_ ;
  
  public DriveMigrationService(RepositoryService repositoryService, ManageDriveService driveService,
      NodeHierarchyCreator nodeHierarchyCreatorService) { 
    repositoryService_ = repositoryService ;
    driveService_ = driveService ;
    baseDrivePath_ = nodeHierarchyCreatorService.getJcrPath(DRIVE_HOME_PATH_ALIAS);
  }
  
  public void start() {
    log.info("PROCESSING MIGRATE DRIVES NAME");
    try {
      migrateProcess();
    } catch(Exception e) {
      log.error("MIGRATION FAILED: Can not migrate drives");
    }
  }
/**
 * This method will be used to migrate drives name which included
 * invalid characters like(|)
 * @throws Exception
 */  
  public void migrateProcess() throws Exception {
    int migrateNum = 0;
    for(RepositoryEntry repoEntry : repositoryService_.getConfig().getRepositoryConfigurations()) {
      List<DriveData> drives = driveService_.getAllDrives(repoEntry.getName());
      Session session = getSession(repoEntry.getName()) ;    
      for(DriveData drive : drives) {
        String oldDriveName = drive.getName();
        String oldDrivePath = baseDrivePath_ + "/" + oldDriveName;
        if(oldDriveName.indexOf("|") > -1) {
          String newDrivePath = baseDrivePath_ + "/" + oldDriveName.replace("|", ".");
          session.move(oldDrivePath, newDrivePath);
          session.save();
          migrateNum++;
        }
      }
      session.logout();
    }
    if(migrateNum > 0) {
      log.info("MIGRATED " + migrateNum + " DRIVES");
    } else {
      log.info("DON'T HAVE ANY DRIVE TO MIGRATE");
    }
  }
  
  /**
   * Get session from repository in SystemWorkspace name
   * @param repository    repository name
   * @return session
   * @throws Exception
   */
  private Session getSession(String repository) throws Exception{    
    ManageableRepository manaRepository = repositoryService_.getRepository(repository) ;
    return manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;          
  }  

  public void stop() { }
}