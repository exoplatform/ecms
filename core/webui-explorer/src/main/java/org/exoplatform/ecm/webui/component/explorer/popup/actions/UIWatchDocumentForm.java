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

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 10, 2007
 * 2:34:12 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIWatchDocumentForm.WatchActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWatchDocumentForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIWatchDocumentForm.UnwatchActionListener.class, phase = Phase.DECODE)
    }
)
public class UIWatchDocumentForm extends UIForm implements UIPopupComponent {

  final static public String NOTIFICATION_TYPE = "notificationType" ;
  final static public String NOTIFICATION_BY_EMAIL = "Email" ;
  final static public String NOTIFICATION_BY_RSS = "RSS" ;

  public UIWatchDocumentForm() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(NOTIFICATION_TYPE, NOTIFICATION_TYPE, options) ;
    addUIFormInput(uiSelectBox) ;
  }

  public Node getWatchNode() throws Exception{
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ; }

  public String getUserName() {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    return context.getRemoteUser() ;
  }

  public boolean isWatching() throws Exception{
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    WatchDocumentService watchService = getApplicationComponent(WatchDocumentService.class) ;
    int notifyType = watchService.getNotificationType(getWatchNode(),getUserName()) ;
    if(notifyType == WatchDocumentService.FULL_NOTIFICATION) {
      options.add(new SelectItemOption<String>(NOTIFICATION_BY_EMAIL,NOTIFICATION_BY_EMAIL)) ;
      getUIFormSelectBox(NOTIFICATION_TYPE).setOptions(options) ;
      return true ;
    } else if(notifyType == WatchDocumentService.NOTIFICATION_BY_EMAIL ) {
      options.add(new SelectItemOption<String>(NOTIFICATION_BY_EMAIL,NOTIFICATION_BY_EMAIL)) ;
      getUIFormSelectBox(NOTIFICATION_TYPE).setOptions(options) ;
      return true ;
    } else {
      options.add(new SelectItemOption<String>(NOTIFICATION_BY_EMAIL,NOTIFICATION_BY_EMAIL)) ;
      getUIFormSelectBox(NOTIFICATION_TYPE).setOptions(options) ;
      return false ;
    }
  }

  public int getNotifyType() throws Exception {
    WatchDocumentService watchService = getApplicationComponent(WatchDocumentService.class) ;
    return watchService.getNotificationType(getWatchNode(), getUserName()) ;
  }

  public void activate() throws Exception {
    if(!isWatching()) setActions(new String[] {"Watch", "Cancel"}) ;
    else setActions(new String[] {"Unwatch", "Cancel"}) ;
  }

  public void deActivate() throws Exception {
  }

  static  public class CancelActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  static  public class WatchActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIWatchDocumentForm uiForm = event.getSource() ;
      String notifyType = uiForm.getUIFormSelectBox(NOTIFICATION_TYPE).getValue() ;
      WatchDocumentService watchService = uiForm.getApplicationComponent(WatchDocumentService.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      if(notifyType.equalsIgnoreCase(NOTIFICATION_BY_EMAIL)) {
        watchService.watchDocument(uiForm.getWatchNode(), uiForm.getUserName(), WatchDocumentService.NOTIFICATION_BY_EMAIL) ;
        uiForm.isWatching() ;
      } else {
        uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.not-support", null,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      }
      uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.watching-successfully", null)) ;
      uiForm.getAncestorOfType(UIJCRExplorer.class).updateAjax(event) ;
      
    }
  }

  static  public class UnwatchActionListener extends EventListener<UIWatchDocumentForm> {
    public void execute(Event<UIWatchDocumentForm> event) throws Exception {
      UIWatchDocumentForm uiForm = event.getSource() ;
      String notifyType = uiForm.getUIFormSelectBox(NOTIFICATION_TYPE).getValue() ;
      WatchDocumentService watchService = uiForm.getApplicationComponent(WatchDocumentService.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      if(notifyType.equalsIgnoreCase(NOTIFICATION_BY_EMAIL)) {
        watchService.unwatchDocument(uiForm.getWatchNode(), uiForm.getUserName(), WatchDocumentService.NOTIFICATION_BY_EMAIL) ;
      } else {
        uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.not-support", null,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      }
      uiApp.addMessage(new ApplicationMessage("UIWatchDocumentForm.msg.unwatching-successfully", null)) ;
      uiForm.getAncestorOfType(UIJCRExplorer.class).updateAjax(event) ;
      
    }
  }
}
