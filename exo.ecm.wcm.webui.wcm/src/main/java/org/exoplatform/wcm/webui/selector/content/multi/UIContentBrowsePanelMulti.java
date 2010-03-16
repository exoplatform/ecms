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
package org.exoplatform.wcm.webui.selector.content.multi;

import javax.jcr.Node;

import org.exoplatform.wcm.webui.selector.UISelectPathPanel;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
@ComponentConfigs({
  @ComponentConfig(
      lifecycle = Lifecycle.class,
      template = "classpath:groovy/wcm/webui/selector/content/multi/UIContentBrowsePanel.gtmpl",
      events = {
        @EventConfig(listeners = UIContentBrowsePanelMulti.ChangeContentTypeActionListener.class)
      }
  ),
  @ComponentConfig(
      type = UISelectPathPanel.class,
      id = "UIContentBrowsePathSelector",
      template = "classpath:groovy/wcm/webui/selector/content/UIContentBrowsePathSelector.gtmpl",
      events = @EventConfig(listeners = UISelectPathPanel.SelectActionListener.class)
  )
})
public class UIContentBrowsePanelMulti extends UIContentBrowsePanel{

  /**
   * Instantiates a new uI content browse panel multi.
   * 
   * @throws Exception the exception
   */
  public UIContentBrowsePanelMulti() throws Exception {
    super();
    addChild(org.exoplatform.wcm.webui.selector.content.UIContentTreeBuilder.class,null, org.exoplatform.wcm.webui.selector.content.UIContentTreeBuilder.class.getName()+hashCode());
    addChild(UISelectPathPanel.class, "UIContentBrowsePathSelector", "UIContentBrowsePathSelector");
    addChild(UICLVContentSelectedGrid.class, null, null);
  }
  
	/* (non-Javadoc)
	 * @see org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel#doSelect(javax.jcr.Node, org.exoplatform.webui.application.WebuiRequestContext)
	 */
	@Override
  public void doSelect(Node node, WebuiRequestContext requestContext) throws Exception {
    UICLVContentSelectedGrid uiSelectedContentGrid = getChild(UICLVContentSelectedGrid.class);
    String value = node.getPath();
    if (!uiSelectedContentGrid.getSelectedCategories().contains(value)) {
      uiSelectedContentGrid.addCategory(value);
    }
    uiSelectedContentGrid.updateGrid(uiSelectedContentGrid.getUIPageIterator().getCurrentPage());
    uiSelectedContentGrid.setRendered(true);
    requestContext.addUIComponentToUpdateByAjax(this);
  }
}
