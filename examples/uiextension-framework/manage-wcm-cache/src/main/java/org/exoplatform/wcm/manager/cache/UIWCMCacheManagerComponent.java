package org.exoplatform.wcm.manager.cache;
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
 * 20 july 2010
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = UIWCMCacheManagerComponent.UIWCMCacheManagerActionListener.class)
    }
)

public class UIWCMCacheManagerComponent extends UIAbstractManagerComponent{

  public static class UIWCMCacheManagerActionListener extends UIECMAdminControlPanelActionListener<UIWCMCacheManagerComponent> {
    public void processEvent(Event<UIWCMCacheManagerComponent> event) throws Exception {
      UIECMAdminPortlet portlet = event.getSource().getAncestorOfType(UIECMAdminPortlet.class);
      UIECMAdminWorkingArea uiWorkingArea = portlet.getChild(UIECMAdminWorkingArea.class);
      uiWorkingArea.getChild(UIWCMCacheManagerPanel.class).update() ;
      uiWorkingArea.setChild(UIWCMCacheManagerPanel.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIWCMCacheManagerPanel.class;
  }
}
