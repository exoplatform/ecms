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

import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.tree.selectone.UIWorkspaceList;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * 4 févr. 09
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)
public class UIJcrExplorerEditContainer extends UIContainer {

  public UIJcrExplorerEditContainer() throws Exception {
    addChild(UIJcrExplorerEditForm.class, null, null);
  }

  public UIPopupWindow initPopup(String id) throws Exception {
    removeChildById(id);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, id);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(700, 350);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
    return uiPopup;
  }

  public UIPopupWindow initPopupDriveBrowser(String id, String driveName) throws Exception {
    String repository = getAncestorOfType(UIJCRExplorerPortlet.class).getPreferenceRepository();
    UIPopupWindow uiPopup = initPopup(id);
    UIOneNodePathSelector uiOneNodePathSelector = createUIComponent(UIOneNodePathSelector.class, null, null);
    UIWorkspaceList uiWorkspaceList= uiOneNodePathSelector.getChild(UIWorkspaceList.class);
    uiOneNodePathSelector.setShowRootPathSelect(true);
    uiWorkspaceList.getChild(UIFormSelectBox.class).setRendered(false);
    ManageDriveService manageDrive = getApplicationComponent(ManageDriveService.class);
    DriveData driveData = manageDrive.getDriveByName(driveName);
    uiOneNodePathSelector.setRootNodeLocation(repository, driveData.getWorkspace(), driveData.getHomePath());
    uiOneNodePathSelector.init(WCMCoreUtils.getUserSessionProvider());
    uiPopup.setUIComponent(uiOneNodePathSelector);
    uiOneNodePathSelector.setSourceComponent(this.getChild(UIJcrExplorerEditForm.class),
        new String[] { UIJcrExplorerEditForm.PARAM_PATH_INPUT });
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
    uiPopup.setShowMask(true);
    return uiPopup;
  }
}
