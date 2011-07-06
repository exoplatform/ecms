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
package org.exoplatform.ecm.webui.component.admin.templates;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")

public class UIViewTemplate extends UIContainer {
  private String nodeTypeName_ ;

  public UIViewTemplate() throws Exception {
    addChild(UITemplateEditForm.class, null, null) ;
    addChild(UIDialogTab.class, null, null).setRendered(false) ;
    addChild(UIViewTab.class, null, null).setRendered(false) ;
    addChild(UISkinTab.class, null, null).setRendered(false);
  }

  public void refresh() throws Exception {
    getChild(UIDialogTab.class).updateGrid(nodeTypeName_);
    getChild(UIViewTab.class).updateGrid(nodeTypeName_);
    getChild(UISkinTab.class).updateGrid(nodeTypeName_);
  }
  public void setNodeTypeName(String nodeType) { nodeTypeName_ = nodeType ; }

  public String getNodeTypeName() { return nodeTypeName_ ; }
}
