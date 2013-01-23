/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.lock.LockException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormRadioBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 10, 2007
 * 2:34:12 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/popup/action/UIWatchDocumentForm.gtmpl",
    events = {
      @EventConfig(listeners = UIWatchDocumentForm.WatchActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWatchDocumentForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWatchDocumentForm.UnwatchActionListener.class, phase = Phase.DECODE)
    }
)
public class UIWatchDocumentForm extends UIForm implements UIPopupComponent {
  private static final Log LOG  = ExoLogger.getLogger(UIWatchDocumentForm.class.getName());
  private static final String NOTIFICATION_TYPE = "notificationType";
  private static final String NOTIFICATION_TYPE_BY_EMAIL = "Email";

  public UIWatchDocumentForm() throws Exception {
    List<SelectItemOption<String>> nodifyOptions = new ArrayList<SelectItemOption<String>>();
    nodifyOptions.add(new SelectItemOption<String>(NOTIFICATION_TYPE_BY_EMAIL, NOTIFICATION_TYPE_BY_EMAIL));
    UIFormRadioBoxInput notificationTypeRadioBoxInput =
        new UIFormRadioBoxInput(NOTIFICATION_TYPE, NOTIFICATION_TYPE, nodifyOptions);
    addUIFormInput(notificationTypeRadioBoxInput);
  }
  
  public void activate() {
    try {
      if(!isWatching()) setActions(new String[] {"Watch", "Cancel"});
      else {
        setActions(new String[] {"Unwatch", "Cancel"});
        UIFormRadioBoxInput notificationTypeRadioBoxInput = this.getChildById(NOTIFICATION_TYPE);
        notificationTypeRadioBoxInput.setValue(NOTIFICATION_TYPE_BY_EMAIL);
        notificationTypeRadioBoxInput.setReadOnly(true);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  public void deActivate() {
  }

  private Node getWatchNode() throws Exception{
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode(); }

  private String getUserName() {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    return context.getRemoteUser();
  }

  private boolean isWatching() throws Exception{
    return (WatchDocumentService.NOTIFICATION_BY_EMAIL == this.getNotifyType());
  }

  private int getNotifyType() throws Exception {
    WatchDocumentService watchService = getApplicationComponent(WatchDocumentService.class);
    return watchService.getNotificationType(this.getWatchNode(), this.getUserName());
  }
  
  private void showFinishMessage(Event<UIWatchDocumentForm> event, String messageKey) throws Exception {
    ResourceBundle res = event.getRequestContext().getApplicationResourceBundle();
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    uiExplorer.getChild(UIWorkingArea.class).setWCMNotice(res.getString(messageKey));
    uiExplorer.updateAjax(event);
  }
  
  private void toogleWatch(Event<UIWatchDocumentForm> event) throws Exception {
    UIApplication uiApp = this.getAncestorOfType(UIApplication.class);
    
    // Add lock token
    this.getAncestorOfType(UIJCRExplorer.class).addLockToken(this.getWatchNode());
    
    try {
      WatchDocumentService watchService = WCMCoreUtils.getService(WatchDocumentService.class);
      if (isWatching()) {
        watchService.unwatchDocument(this.getWatchNode(), this.getUserName(), WatchDocumentService.NOTIFICATION_BY_EMAIL);
        this.showFinishMessage(event, "UIWatchDocumentForm.msg.unwatching-successfully");
      } else {
        watchService.watchDocument(this.getWatchNode(), this.getUserName(), WatchDocumentService.NOTIFICATION_BY_EMAIL);
        this.showFinishMessage(event, "UIWatchDocumentForm.msg.watching-successfully");
      }
    } catch (LockException e) {
      uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.node-is-locked", null, ApplicationMessage.WARNING));
    } catch (Exception e) {
      uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.unknown-error", null, ApplicationMessage.ERROR));
    }
  }

  public static class CancelActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public static class WatchActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIWatchDocumentForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      
      // Add lock token
      uiForm.getAncestorOfType(UIJCRExplorer.class).addLockToken(uiForm.getWatchNode());
      
      // Add watching
      boolean isNotifyByEmail = 
          NOTIFICATION_TYPE_BY_EMAIL.equalsIgnoreCase(((UIFormRadioBoxInput)uiForm.getChildById(NOTIFICATION_TYPE)).getValue());
      if(isNotifyByEmail) {
        uiForm.toogleWatch(event);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.not-support", null, ApplicationMessage.WARNING));
      }
    }
  }

  public static class UnwatchActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIWatchDocumentForm uiForm = event.getSource();
      uiForm.toogleWatch(event);
    }
  }
}
