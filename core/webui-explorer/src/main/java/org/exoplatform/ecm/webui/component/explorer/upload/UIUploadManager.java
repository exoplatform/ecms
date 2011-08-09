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
package org.exoplatform.ecm.webui.component.explorer.upload;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 24, 2007 2:12:48 PM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)
public class UIUploadManager extends UIContainer implements UIPopupComponent {

  final static public String EXTARNAL_METADATA_POPUP = "AddMetadataPopup" ;
  final static public String SAMENAME_POPUP = "SameNamePopup" ;

  public UIUploadManager() throws Exception {
    addChild(UIUploadForm.class, null, null);
    addChild(UIUploadContainer.class, null, null).setRendered(false);
  }

  public UIPopupWindow initPopupTaxonomy(String id) throws Exception {
    UIPopupWindow uiPopup = getChildById(id);
    if (uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, id);
    }
    uiPopup.setWindowSize(800, 350);
    uiPopup.setShow(false);
    uiPopup.setResizable(true);
    return uiPopup;
  }

  public UIPopupWindow initPopupWhenHaveSameName() throws Exception {
    UIPopupWindow uiPopup = getChildById(SAMENAME_POPUP);
    if (uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, SAMENAME_POPUP);
    }
    uiPopup.setWindowSize(500, 180);
    uiPopup.setShow(false);
    uiPopup.setResizable(true);
    return uiPopup;
  }

  public void activate() throws Exception {
    UIUploadForm uiUploadForm = getChild(UIUploadForm.class);
    uiUploadForm.initFieldInput();
  }

  public void deActivate() throws Exception {}

  public void initMetadataPopup() throws Exception {
    removeChildById(EXTARNAL_METADATA_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, EXTARNAL_METADATA_POPUP) ;
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(400, 400);
    UIExternalMetadataForm uiExternalMetadataForm = createUIComponent(UIExternalMetadataForm.class, null, null) ;
    uiPopup.setUIComponent(uiExternalMetadataForm) ;
    uiExternalMetadataForm.renderExternalList() ;
    uiPopup.setRendered(true);
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
