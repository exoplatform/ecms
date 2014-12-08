package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.DeleteManageComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by toannh on 12/8/14.
 * Popup window when open read-only a document in SE
 */
@ComponentConfig(
        template = "app:/groovy/webui/component/explorer/popup/action/UIOpenDocumentForm.gtmpl",
        events = {
                @EventConfig(listeners = UIOpenDocumentForm.ReadOnlyActionListener.class),
                @EventConfig(listeners = UIOpenDocumentForm.CancelActionListener.class)
        }
)
public class UIOpenDocumentForm extends UIComponent implements UIPopupComponent {

  public UIOpenDocumentForm() throws Exception {}

  public String filePath;
  public String[] getActions() { return new String[] {"ReadOnly", "Cancel"}; }

  /**
   * Only open document with read-only mode.
   */
  public static class ReadOnlyActionListener extends EventListener<UIOpenDocumentForm> {
    @Override
    public void execute(Event<UIOpenDocumentForm> event) throws Exception {
      UIOpenDocumentForm uiConfirm = event.getSource();
      UIJCRExplorer uiExplorer = uiConfirm.getAncestorOfType(UIJCRExplorer.class);
       // impl readonly action
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);

      event.getRequestContext().getJavascriptManager().require("SHARED/openDocumentInOffice")
              .addScripts("eXo.ecm.OpenDocumentInOffice.openDocument('"+uiConfirm.filePath+"');");
    }
  }

  /**
   * Cancel action, close all popup include popup of ITHIT
   */
  public static class CancelActionListener extends EventListener<UIOpenDocumentForm> {
    @Override
    public void execute(Event<UIOpenDocumentForm> event) throws Exception {
      UIOpenDocumentForm uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
      event.getRequestContext().getJavascriptManager().require("SHARED/openDocumentInOffice", "od").
              addScripts("od.OpenDocumentInOffice.closePopup();");
    }
  }

  private String messageKey_;
  private String[] args_ = {};

  public void setMessageKey(String messageKey) { messageKey_ = messageKey; }

  public String getMessageKey() { return messageKey_; }

  public void setArguments(String[] args) { args_ = args; }

  public String[] getArguments() { return args_; }

  public void activate() {

  }

  public void deActivate() {

  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
}
