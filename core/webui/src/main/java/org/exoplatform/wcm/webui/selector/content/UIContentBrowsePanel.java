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
package org.exoplatform.wcm.webui.selector.content;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */

@ComponentConfig(
  lifecycle = Lifecycle.class,
  events = {
  @EventConfig(listeners = UIContentBrowsePanel.ChangeContentTypeActionListener.class)
  }
)

public abstract class UIContentBrowsePanel extends UIBaseNodeTreeSelector {

  public static final String WEBCONTENT = "Web Contents";

  public static final String DMSDOCUMENT = "DMS Documents";

  public static final String MEDIA = "Medias";

  private String contentType = WEBCONTENT;

  public String getContentType() {
  return contentType;
  }

  public void setContentType(String contentType) {
  this.contentType = contentType;
  }

  public void onChange(Node node, Object context) throws Exception {}

  public static class ChangeContentTypeActionListener extends EventListener<UIContentBrowsePanel> {
  public void execute(Event<UIContentBrowsePanel> event) throws Exception {
    UIContentBrowsePanel contentBrowsePanel = event.getSource();
    contentBrowsePanel.contentType = event.getRequestContext().getRequestParameter(OBJECTID);
      event.getRequestContext()
           .addUIComponentToUpdateByAjax(contentBrowsePanel.getAncestorOfType(UIContentSelector.class)
                                                           .getChild(UIContentSearchForm.class));
  }
  }
}
