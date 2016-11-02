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
package org.exoplatform.ecm.webui.component.explorer.versions;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.form.UIForm;
/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Oct 20, 2006
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/explorer/versions/UINodeInfo.gtmpl"
)
public class UINodeInfo extends UIForm {

  private NodeLocation node_ ;
  private String versionCreatedDate_ ;

  public UINodeInfo() {

  }

  public void update() throws Exception {
    UIVersionInfo uiVersionInfo = getAncestorOfType(UIDocumentWorkspace.class).getChild(UIVersionInfo.class) ;
    node_ = NodeLocation.getNodeLocationByNode(uiVersionInfo.getCurrentNode());
    versionCreatedDate_ = uiVersionInfo.getCurrentVersionNode().getCreatedTime().getTime().toString();
  }

  public String getNodeType() throws Exception {
    return NodeLocation.getNodeByLocation(node_).getPrimaryNodeType().getName();
  }

  public String getNodeName() throws Exception {
    return NodeLocation.getNodeByLocation(node_).getName();
  }

  public String getVersionName() throws Exception {
    return getAncestorOfType(UIDocumentWorkspace.class).getChild(UIVersionInfo.class) .getCurrentVersionNode().getName();
  }

  public String getVersionLabels() throws Exception{
    UIVersionInfo uiVersionInfo = getAncestorOfType(UIDocumentWorkspace.class).getChild(UIVersionInfo.class) ;
    String[] labels = uiVersionInfo.getVersionLabels(uiVersionInfo.getCurrentVersionNode());
    StringBuilder label = new StringBuilder() ;
    if(labels.length  == 0 ) return "N/A" ;
    for(String temp : labels) {
      label.append(temp).append(" ") ;
    }
    return label.toString() ;
  }

  public String getVersionCreatedDate() throws Exception {
    return versionCreatedDate_;
  }
}
