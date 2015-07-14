package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/27/15
 * Build popup document auto versioning
 */
@ComponentConfig(
        template =  "app:/groovy/webui/component/explorer/versions/UIDocumentAutoVersionComponent.gtmpl",
        events = {
                @EventConfig(listeners = UIDocumentAutoVersionComponent.UploadActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.ShowActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CancelActionListener.class)
        }
)
public class UIDocumentAutoVersionComponent extends UIContainer implements UIPopupComponent {

  @Override
  public void activate() {

  }

  @Override
  public void deActivate() {

  }

  public String[] getActions() { return new String[] {"Upload", "Cancel"}; }

  public static class UploadActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class ShowActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class CancelActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(true) ;
      event.getRequestContext().getJavascriptManager().require("multiUpload")
              .addScripts("eXo.ecm.MultiUpload.hideDocumentAutoPopup(); ");

      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }
}
