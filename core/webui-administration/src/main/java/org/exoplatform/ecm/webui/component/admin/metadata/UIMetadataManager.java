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
package org.exoplatform.ecm.webui.component.admin.metadata;

import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 19, 2006
 * 11:45:11 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIMetadataManager extends UIAbstractManager {

  final static public String METADATA_POPUP = "MetadataPopupEdit" ;
  final static public String VIEW_METADATA_POPUP = "ViewMetadataPopup" ;
  final static public String PERMISSION_POPUP = "PermissionPopup" ;

  public UIMetadataManager() throws Exception {
    addChild(UIMetadataList.class, null, null) ;
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    getChild(UIMetadataList.class).refresh(1);
  }
  public void initPopup() throws Exception {
    removeChildById(METADATA_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, METADATA_POPUP);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(650, 450);
    UIMetadataForm uiMetaForm = createUIComponent(UIMetadataForm.class, null, null) ;
    uiPopup.setUIComponent(uiMetaForm) ;
    uiPopup.setRendered(true);
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void initViewPopup(String metadataName) throws Exception {
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, VIEW_METADATA_POPUP);
    uiPopup.setShowMask(true);
    uiPopup.setShow(true) ;
    uiPopup.setWindowSize(600, 500);
    uiPopup.setRendered(true);
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    ExtendedNodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager() ;
    NodeType nodeType = ntManager.getNodeType(metadataName) ;
    UIMetadataView uiView = uiPopup.createUIComponent(UIMetadataView.class, null, null) ;
    uiView.setMetadata(nodeType) ;
    uiPopup.setUIComponent(uiView) ;
    uiPopup.setResizable(true) ;
  }

  public void initPopupPermission(String membership) throws Exception {
    removeChildById(PERMISSION_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, PERMISSION_POPUP);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(560, 300);
    UIPermissionSelector uiECMPermission =
      createUIComponent(UIPermissionSelector.class, null, "MetadataPermission") ;
    uiECMPermission.setSelectedMembership(true);
    if(membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/") ;
      uiECMPermission.setCurrentPermission("/" + arrMember[1]) ;
    }
    uiPopup.setUIComponent(uiECMPermission);
    UIMetadataForm uiForm = findFirstComponentOfType(UIMetadataForm.class) ;
    uiECMPermission.setSourceComponent(uiForm, null) ;
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
  }
}
