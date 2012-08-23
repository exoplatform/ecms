/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.authoring;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 22, 2012  
 */

@ComponentConfigs( {
  @ComponentConfig(lifecycle = Lifecycle.class, 
                   template = "app:/groovy/authoring/UIDashboardColumn.gtmpl", 
                   events = {
      @EventConfig(listeners = UIDashboardForm.ShowDocumentActionListener.class),
      @EventConfig(listeners = UIDashboardForm.RefreshActionListener.class) }),
  @ComponentConfig(type = UIPageIterator.class,
                   template = "app:/groovy/authoring/UIDashBoardColumnIterator.gtmpl",
                   events = {@EventConfig(listeners = UIPageIterator.ShowPageActionListener.class)})
})
public class UIDashBoardColumn extends UIContainer {

  private static final String locale = "locale.portlet.AuthoringDashboard.AuthoringDashboardPortlet";
  
  private UIPageIterator uiPageIterator_;
  private String label_;
  
  public UIDashBoardColumn() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, null);
  }
  
  public UIPageIterator getUIPageIterator() {
    return uiPageIterator_;
  }
  
  public List<Node> getNodes() throws Exception {
    return NodeLocation.getNodeListByLocationList(uiPageIterator_.getCurrentPageData());
  }
  
  public void setLabel(String value) {
    label_ = Utils.getResourceBundle(locale,
                                     value,
                                     this.getClass().getClassLoader());
  }
  
  public String getLabel() {
    return label_;
  }
}
