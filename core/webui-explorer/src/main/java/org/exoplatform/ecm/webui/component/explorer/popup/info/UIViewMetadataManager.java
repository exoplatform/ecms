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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import javax.jcr.Node;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 30, 2007
 * 9:27:48 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIViewMetadataManager extends UIContainer implements UIPopupComponent {

  final static public String METADATAS_POPUP = "metadataForm" ;

  public UIViewMetadataManager() throws Exception {
     addChild(UIViewMetadataContainer.class, null, null) ;
  }

  public Node getViewNode(String nodeType) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getViewNode(nodeType) ;
  }

  public void initMetadataFormPopup(String nodeType) throws Exception {
    removeChildById(METADATAS_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, METADATAS_POPUP) ;
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(650, 450);
    UIViewMetadataForm uiForm = createUIComponent(UIViewMetadataForm.class, null, null) ;
    uiForm.getChildren().clear() ;
    uiForm.setNodeType(nodeType) ;
    uiForm.setIsNotEditNode(true) ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node currentNode = uiExplorer.getCurrentNode() ;
    uiForm.setWorkspace(currentNode.getSession().getWorkspace().getName()) ;
    uiForm.setStoredPath(currentNode.getPath()) ;
//    uiForm.setPropertyNode(getViewNode(nodeType)) ;
    uiForm.setChildPath(getViewNode(nodeType).getPath()) ;
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setRendered(true);
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}
}
