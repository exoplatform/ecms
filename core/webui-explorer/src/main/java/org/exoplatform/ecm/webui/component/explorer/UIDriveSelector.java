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
package org.exoplatform.ecm.webui.component.explorer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * 10 févr. 09
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/UIDriveSelector.gtmpl",
    events = {
      @EventConfig(listeners = UIDriveSelector.AddDriveActionListener.class),
      @EventConfig(listeners = UIDriveSelector.CancelActionListener.class)
    }
)
public class UIDriveSelector extends UIContainer {
  private UIPageIterator uiPageIterator_;

  public UIDriveSelector() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "DriveSelectorList");
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getListDrive() throws Exception { return uiPageIterator_.getCurrentPageData(); }

  public void updateGrid() throws Exception {
    ListAccess<DriveData> driveList = new ListAccessImpl<DriveData>(DriveData.class, getDrives());
    LazyPageList<DriveData> dataPageList = new LazyPageList<DriveData>(driveList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  @Deprecated
  public List<DriveData> getDrives(String repoName) throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class) ;
    List<DriveData> driveList =
      driveService.getDriveByUserRoles(Util.getPortalRequestContext().getRemoteUser(), Utils.getMemberships());
    Collections.sort(driveList, new DriveComparator()) ;
    return driveList ;
  }
  
  public List<DriveData> getDrives() throws Exception {
    ManageDriveService driveService = getApplicationComponent(ManageDriveService.class) ;
    List<DriveData> driveList =
      driveService.getDriveByUserRoles(Util.getPortalRequestContext().getRemoteUser(), Utils.getMemberships());
    Collections.sort(driveList, new DriveComparator()) ;
    return driveList ;
  }

  static public class DriveComparator implements Comparator<DriveData> {
    public int compare(DriveData d1, DriveData d2) throws ClassCastException {
      String name1 = d1.getName();
      String name2 = d2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  static public class CancelActionListener extends EventListener<UIDriveSelector> {
    public void execute(Event<UIDriveSelector> event) throws Exception {
      UIDriveSelector driveSelector = event.getSource();
      UIComponent uiComponent = driveSelector.getParent();
      if (uiComponent != null) {
        if (uiComponent instanceof UIPopupWindow) {
          ((UIPopupWindow)uiComponent).setShow(false);
          ((UIPopupWindow)uiComponent).setRendered(false);
          event.getRequestContext().addUIComponentToUpdateByAjax(((UIPopupWindow)uiComponent).getParent());
          return;
        }
      }
    }
  }

  static public class AddDriveActionListener extends EventListener<UIDriveSelector> {
    public void execute(Event<UIDriveSelector> event) throws Exception {
      String driveName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDriveSelector driveSelector = event.getSource();
      UIJcrExplorerEditContainer editContainer = driveSelector.getAncestorOfType(UIJcrExplorerEditContainer.class);
      UIJcrExplorerEditForm form = editContainer.getChild(UIJcrExplorerEditForm.class);
      UIFormInputSetWithAction driveNameInput = form.getChildById("DriveNameInput");
      driveNameInput.getUIStringInput(UIJCRExplorerPortlet.DRIVE_NAME).setValue(driveName);
      UIComponent uiComponent = driveSelector.getParent();
      UIFormSelectBox typeSelectBox = form.getChildById(UIJCRExplorerPortlet.USECASE);
      if (UIJCRExplorerPortlet.PARAMETERIZE.equals(typeSelectBox.getValue())) {
        UIFormInputSetWithAction uiParamPathInput = form.getChildById(UIJcrExplorerEditForm.PARAM_PATH_ACTION);
        uiParamPathInput.setRendered(true);
      }
      if (uiComponent != null) {
        if (uiComponent instanceof UIPopupWindow) {
          ((UIPopupWindow)uiComponent).setShow(false);
          ((UIPopupWindow)uiComponent).setRendered(false);
          event.getRequestContext().addUIComponentToUpdateByAjax(((UIPopupWindow)uiComponent).getParent());
          return;
        }
      }
    }
  }
}
