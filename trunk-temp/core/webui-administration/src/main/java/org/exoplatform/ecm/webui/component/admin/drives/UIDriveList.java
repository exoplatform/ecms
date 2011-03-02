/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.drives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2006
 * 11:39:49 AM 
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/drives/UIDriveList.gtmpl",
    events = {
        @EventConfig(listeners = UIDriveList.DeleteActionListener.class, confirm = "UIDriveList.msg.confirm-delete"),
        @EventConfig(listeners = UIDriveList.EditInfoActionListener.class),
        @EventConfig(listeners = UIDriveList.AddDriveActionListener.class)
    }
)
public class UIDriveList extends UIComponentDecorator {

  final static public String[] ACTIONS = {"AddDrive"} ;
  final  static public String ST_ADD = "AddDriveManagerPopup" ;
  final  static public String ST_EDIT = "EditDriveManagerPopup" ;
  private UIPageIterator uiPageIterator_ ;
  
  public UIDriveList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "UIDriveListIterator");
    setUIComponent(uiPageIterator_) ;
  }

  public String[] getActions() { return ACTIONS ; }

  @SuppressWarnings("unchecked")
  public void updateDriveListGrid(int currentPage) throws Exception {
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    ObjectPageList objPageList = new ObjectPageList(getDrives(repository), 10) ;
    uiPageIterator_.setPageList(objPageList) ;
    if(currentPage > getUIPageIterator().getAvailablePage())
      uiPageIterator_.setCurrentPage(currentPage-1);
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }
  
  public UIPageIterator  getUIPageIterator() {  return uiPageIterator_ ; }
  
  public List getDriveList() throws Exception { return uiPageIterator_.getCurrentPageData() ; }

  @SuppressWarnings("unchecked")
  public List<DriveData> getDrives(String repoName) throws Exception {
    RepositoryService rservice = getApplicationComponent(RepositoryService.class) ;
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class) ;
    ManageableRepository repository = rservice.getCurrentRepository() ;  
    List<DriveData> driveList = new ArrayList<DriveData>() ;
    Session session = null ;
    List<DriveData> drives = driveService.getAllDrives(repoName) ;
    if(drives != null && drives.size() > 0) {
      for(DriveData drive : drives) {
        if(drive.getIcon() != null && drive.getIcon().length() > 0) {
          try {
            String[] iconPath = drive.getIcon().split(":/") ;   
            session = repository.getSystemSession(iconPath[0]) ;
            session.getItem("/" + iconPath[1]) ;
            session.logout() ;
          } catch(PathNotFoundException pnf) {
            drive.setIcon("") ;
          }
        }
        if(isExistWorspace(repository, drive)) driveList.add(drive) ;
      }
    }
    Collections.sort(driveList) ;
    return driveList ; 
  }
  
  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container
        .getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName(); 
  }
  
  public String getRepository() throws Exception {
    return getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
  }

  private boolean isExistWorspace(ManageableRepository repository, DriveData drive) {
    for(String ws:  repository.getWorkspaceNames()) {
      if(ws.equals(drive.getWorkspace())) return true ;
    }
    return false ;
  }
  static public class DriveComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      String name1 = ((DriveData) o1).getName() ;
      String name2 = ((DriveData) o2).getName() ;
      return name1.compareToIgnoreCase(name2) ;
    }
  }

  static  public class AddDriveActionListener extends EventListener<UIDriveList> {
    public void execute(Event<UIDriveList> event) throws Exception {
      UIDriveManager uiDriveManager = event.getSource().getParent() ;
      uiDriveManager.removeChildById(UIDriveList.ST_EDIT);
      uiDriveManager.initPopup(UIDriveList.ST_ADD) ;
      UIDriveForm uiForm = uiDriveManager.findFirstComponentOfType(UIDriveForm.class) ;
      uiForm.refresh(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveManager) ;
    }
  }

  static  public class DeleteActionListener extends EventListener<UIDriveList> {
    public void execute(Event<UIDriveList> event) throws Exception {
      String name = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIDriveList uiDriveList = event.getSource();
      ManageDriveService driveService = uiDriveList.getApplicationComponent(ManageDriveService.class) ;
      String repository = uiDriveList.getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
      driveService.removeDrive(name, repository) ;
      uiDriveList.updateDriveListGrid(uiDriveList.getUIPageIterator().getCurrentPage()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveList.getParent()) ;
    }
  }

  static  public class EditInfoActionListener extends EventListener<UIDriveList> {
    public void execute(Event<UIDriveList> event) throws Exception {
      UIDriveManager uiDriveManager = event.getSource().getParent() ;
      uiDriveManager.removeChildById(UIDriveList.ST_ADD);
      uiDriveManager.initPopup(UIDriveList.ST_EDIT) ;
      String driveName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiDriveManager.findFirstComponentOfType(UIDriveForm.class).refresh(driveName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDriveManager) ;
    }
  }
}
