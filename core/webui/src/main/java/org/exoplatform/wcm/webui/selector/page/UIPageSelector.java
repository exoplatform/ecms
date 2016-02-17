/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.selector.page;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 30, 2009
 */
@ComponentConfigs({
        @ComponentConfig(
            lifecycle = UIFormLifecycle.class,
            template = "classpath:groovy/wcm/webui/selector/page/UIPageSelector.gtmpl",
            events = {@EventConfig(listeners = UIPageSelector.ChangeNodeActionListener.class, phase = Phase.DECODE)}
        )
    }
)
public class UIPageSelector extends UIForm {

  /** The source ui component. */
  private UIComponent sourceUIComponent ;

  /** The return field name. */
  private String returnFieldName ;

  /**
   * Instantiates a new uI page selector.
   *
   * @throws Exception the exception
   */
  public UIPageSelector() throws Exception {
    UIPageNodeSelector pageNodeSelector = addChild(UIPageNodeSelector.class, null, null);
    UITree uiTree = pageNodeSelector.getChild(UITree.class);
    uiTree.setUIRightClickPopupMenu(null);

    UIPageSelectorPanel pageSelectorPanel = addChild(UIPageSelectorPanel.class, null, null);
    pageSelectorPanel.setSelectedNode(pageNodeSelector.getSelectedNode().getNode());
    pageSelectorPanel.updateGrid();
  }

  /**
   * Gets the return field name.
   *
   * @return the return field name
   */
  public String getReturnFieldName() { return returnFieldName; }

  /**
   * Sets the return field name.
   *
   * @param name the new return field name
   */
  public void setReturnFieldName(String name) { this.returnFieldName = name; }

  /**
   * Gets the source component.
   *
   * @return the source component
   */
  public UIComponent getSourceComponent() { return sourceUIComponent; }

  /**
   * Sets the source component.
   *
   * @param uicomponent the uicomponent
   * @param initParams the init params
   */
  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    sourceUIComponent = uicomponent ;
    if(initParams == null || initParams.length < 0) return ;
    for(int i = 0; i < initParams.length; i ++) {
      if(initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=") ;
        returnFieldName = array[1] ;
        break ;
      }
      returnFieldName = initParams[0] ;
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processDecode(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processDecode(WebuiRequestContext context) throws Exception {
    super.processDecode(context);
    String action = context.getRequestParameter(UIForm.ACTION);
    Event<UIComponent> event = createEvent(action, Event.Phase.DECODE, context) ;
    if(event != null) event.broadcast() ;
  }

  /**
   * The listener interface for receiving changeNodeAction events.
   * The class that is interested in processing a changeNodeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeNodeActionListener</code> method. When
   * the changeNodeAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ChangeNodeActionListener extends EventListener<UIPageSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPageSelector> event) throws Exception {
      UIPageSelector pageSelector = event.getSource() ;
      UIPageNodeSelector pageNodeSelector = pageSelector.getChild(UIPageNodeSelector.class) ;
      String uri  = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITree tree = pageNodeSelector.getChild(UITree.class) ;
      if(tree.getParentSelected() == null && (uri == null || uri.length() < 1)){
        pageNodeSelector.selectNavigation(pageNodeSelector.getId(pageNodeSelector.getSelectedNavigation()));
      } else {
        pageNodeSelector.selectUserNodeByUri(uri);
      }

      UIPageSelectorPanel pageSelectorPanel = pageSelector.getChild(UIPageSelectorPanel.class);
      pageSelectorPanel.setSelectedNode(pageNodeSelector.getSelectedNode().getNode());
      pageSelectorPanel.updateGrid();

      event.getRequestContext().addUIComponentToUpdateByAjax(pageSelector) ;
    }
  }
}
