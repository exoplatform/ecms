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
        template = "classpath:groovy/ecm/webui/UIConfirmMessage.gtmpl",
        events = {
                @EventConfig(listeners = UIDocumentAutoVersionComponent.KeepBothActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CreateNewVersionActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.ReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CreateVersionOrReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CancelActionListener.class)
        }
)
public class UIDocumentAutoVersionComponent extends UIContainer implements UIPopupComponent {

  private String sourcePath;
  private String destPath;
  private String sourceWorkspace;
  private String destWorkspace;
  private String messageKey_;
  private String[] args_ = {};

  @Override
  public void activate() {
    System.out.println("active autoweire");
  }

  @Override
  public void deActivate() {

  }

  public String[] getActions() { return new String[] {"KeepBoth", "CreateNewVersion", "Replace", "CreateVersionOrReplace" ,"Cancel"}; }

  public static class KeepBothActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class CreateNewVersionActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class ReplaceActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class CreateVersionOrReplaceActionListener extends EventListener<UIDocumentAutoVersionComponent> {
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

  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  public String getDestPath() {
    return destPath;
  }

  public void setDestPath(String destPath) {
    this.destPath = destPath;
  }

  public String getSourceWorkspace() {
    return sourceWorkspace;
  }

  public void setSourceWorkspace(String sourceWorkspace) {
    this.sourceWorkspace = sourceWorkspace;
  }

  public String getDestWorkspace() {
    return destWorkspace;
  }

  public void setDestWorkspace(String destWorkspace) {
    this.destWorkspace = destWorkspace;
  }

  public void setMessageKey(String messageKey) { messageKey_ = messageKey; }

  public String getMessageKey() { return messageKey_; }

  public void setArguments(String[] args) { args_ = args; }

  public String[] getArguments() { return args_; }
}
