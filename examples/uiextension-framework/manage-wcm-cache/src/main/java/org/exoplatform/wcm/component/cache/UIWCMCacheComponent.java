/**
 *
 */
package org.exoplatform.wcm.component.cache;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminWorkingArea;
import org.exoplatform.ecm.webui.component.admin.listener.UIECMAdminControlPanelActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : lamptdev@gmail.com
 * 18 july 2010
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = UIWCMCacheComponent.CacheViewActionListener.class)
    }
)
public class UIWCMCacheComponent extends UIAbstractManagerComponent {

  public static class CacheViewActionListener extends UIECMAdminControlPanelActionListener<UIWCMCacheComponent> {
    public void processEvent(Event<UIWCMCacheComponent> event) throws Exception {
      UIECMAdminPortlet portlet = event.getSource().getAncestorOfType(UIECMAdminPortlet.class);
      UIECMAdminWorkingArea uiWorkingArea = portlet.getChild(UIECMAdminWorkingArea.class);
      uiWorkingArea.setChild(UIWCMCachePanel.class) ;
      UIWCMCachePanel cacheForm = uiWorkingArea.getChild(UIWCMCachePanel.class);
      cacheForm.refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIWCMCachePanel.class;
  }
}
