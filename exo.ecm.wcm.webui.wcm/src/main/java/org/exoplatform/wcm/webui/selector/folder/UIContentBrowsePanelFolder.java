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
package org.exoplatform.wcm.webui.selector.folder;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
@ComponentConfigs({
  @ComponentConfig(
      lifecycle = Lifecycle.class,
      template = "classpath:groovy/wcm/webui/selector/folder/UIContentBrowsePanel.gtmpl",
      events = {
        @EventConfig(listeners = UIContentBrowsePanelFolder.ChangeContentTypeActionListener.class)
      }
  ),
  @ComponentConfig(
      type = UISelectPathPanelFolder.class,
      id = "UIContentBrowsePathSelector",
      template = "classpath:groovy/wcm/webui/selector/folder/UIContentBrowsePathSelector.gtmpl",
      events = @EventConfig(listeners = UISelectPathPanelFolder.SelectActionListener.class)
  )
})
public class UIContentBrowsePanelFolder extends UIContentBrowsePanel{

  /**
   * Instantiates a new uI content browse panel folder.
   * 
   * @throws Exception the exception
   */
  public UIContentBrowsePanelFolder() throws Exception {
    super();
    addChild(UIContentTreeBuilderFolder.class,null, UIContentTreeBuilderFolder.class.getName()+hashCode());
    addChild(UISelectPathPanelFolder.class, "UIContentBrowsePathSelector", "UIContentBrowsePathSelector");
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel#doSelect(javax.jcr.Node, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void doSelect(Node node, WebuiRequestContext requestContext) throws Exception{
    String returnField = this.getReturnFieldName();
    ((UISelectable)(this.getSourceComponent())).doSelect(returnField, node.getPath()) ;
    UIComponent uIContentSelectorFolder = this.getParent();
    if (uIContentSelectorFolder instanceof UIOneNodePathSelector) {
      UIComponent uiComponent = uIContentSelectorFolder.getParent();
      if (uiComponent instanceof UIPopupWindow) {
        ((UIPopupWindow)uiComponent).setShow(false);
        ((UIPopupWindow)uiComponent).setRendered(false);
        requestContext.addUIComponentToUpdateByAjax(uiComponent);
      }
      UIComponent component = this.getSourceComponent().getParent();
      if (component != null) {
      	requestContext.addUIComponentToUpdateByAjax(component);
      }
    }
  }
}
