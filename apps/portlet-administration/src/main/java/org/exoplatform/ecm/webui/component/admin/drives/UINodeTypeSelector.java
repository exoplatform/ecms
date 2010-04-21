/***************************************************************************
 * Copyright 2001-2010 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.drives;

import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 19, 2010  
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class, 
    template = "app:/groovy/webui/component/admin/drives/UINodeTypeSelector.gtmpl", 
    events = {
      @EventConfig(listeners = UINodeTypeSelector.SearchNodeTypeActionListener.class),
      @EventConfig(listeners = UINodeTypeSelector.SaveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.ShowPageActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.OnChangeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.CloseActionListener.class, phase = Phase.DECODE)
    }
)
public class UINodeTypeSelector extends org.exoplatform.ecm.webui.nodetype.selector.UINodeTypeSelector implements ComponentSelector {

  public UINodeTypeSelector() throws Exception {
    
  }
  
  public static class SearchNodeTypeActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
    }
  }
  
  public static class OnChangeActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      
    }
  }
  
  public static class SaveActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
    }
  }

  public static class ShowPageActionListener extends EventListener<UIPageIterator> {
    public void execute(Event<UIPageIterator> event) throws Exception {
    }
  }

  
  public static class CloseActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
    }
  }

}