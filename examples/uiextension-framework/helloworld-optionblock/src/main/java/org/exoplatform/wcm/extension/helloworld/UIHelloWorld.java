/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wcm.extension.helloworld;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SAS
 * Author : Khuong.Van.Dung
 *          dung.khuong@exoplatform.com
 * Aug 5, 2010
 */

@ComponentConfig(
    template = "classpath:templates/helloworld/UIHelloWorld.gtmpl"
)

public class UIHelloWorld extends UIComponent {

  public UIHelloWorld() {
  }

  public Node getEditingNode() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
  }
}
