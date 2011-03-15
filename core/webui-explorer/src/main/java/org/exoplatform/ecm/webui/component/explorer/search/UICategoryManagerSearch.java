/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.search;

import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Nov 18, 2008
 */

@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/UITabPaneWithAction.gtmpl",
    events = @EventConfig(listeners = UICategoryManagerSearch.CloseActionListener.class)
)
/**
 * Choose category when execute search function
 */
public class UICategoryManagerSearch extends UIContainer implements UIPopupComponent {
  final static public String[] ACTIONS = { "Close" };

  public UICategoryManagerSearch() throws Exception {
    addChild(UIOneTaxonomySelector.class, null, null);
  }

  public String[] getActions() {
    return ACTIONS;
  }

  static public class CloseActionListener extends EventListener<UICategoryManagerSearch> {
    public void execute(Event<UICategoryManagerSearch> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class);
      UIPopupContainer uiPopupContainer = uiSearchContainer.getChild(UIPopupContainer.class);
      uiPopupContainer.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer) ;
    }
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {
  }
}
