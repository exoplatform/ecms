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
package org.exoplatform.ecm.webui.component.admin.namespace;

import javax.jcr.NamespaceRegistry;

import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : phamtuan
 * phamtuanchip@yahoo.de September 20, 2006 16:37:15
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UINamespaceForm.SaveActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UINamespaceForm.CancelActionListener.class)
    }
)
public class UINamespaceForm extends UIForm {

  final static public String FIELD_PREFIX = "namespace" ;
  final static public String FIELD_URI = "uri" ;

  public UINamespaceForm() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_PREFIX, FIELD_PREFIX, null).
        addValidator(MandatoryValidator.class).
        addValidator(ECMNameValidator.class)) ;
    addUIFormInput(new UIFormStringInput(FIELD_URI, FIELD_URI, null).
        addValidator(MandatoryValidator.class)) ;
  }

  static public class SaveActionListener extends EventListener<UINamespaceForm> {
    public void execute(Event<UINamespaceForm> event) throws Exception {
      UINamespaceForm uiForm = event.getSource() ;
      String uri = uiForm.getUIStringInput(FIELD_URI).getValue() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      NamespaceRegistry namespaceRegistry = uiForm.getApplicationComponent(RepositoryService.class)
      .getCurrentRepository().getNamespaceRegistry() ;
      String prefix = uiForm.getUIStringInput(FIELD_PREFIX).getValue() ;
      if(prefix == null || prefix.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UINamespaceForm.msg.prefix-null", null,
            ApplicationMessage.WARNING)) ;
        return ;
      }
      if(uri == null || uri.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UINamespaceForm.msg.uri-null", null,
            ApplicationMessage.WARNING)) ;
        return ;
      }
      UINamespaceManager uiManager = uiForm.getAncestorOfType(UINamespaceManager.class) ;
      if (contains(namespaceRegistry.getPrefixes(), prefix) ||
          contains(namespaceRegistry.getURIs(), uri)) {
        uiApp.addMessage(new ApplicationMessage("UINamespaceForm.msg.register-unsuccessfull", null,
            ApplicationMessage.WARNING)) ;
        return ;
      }
      try {
        namespaceRegistry.registerNamespace(prefix, uri) ;
        uiManager.refresh() ;
        uiManager.removeChild(UIPopupWindow.class) ;
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UINamespaceForm.msg.register-unsuccessfull", null,
            ApplicationMessage.WARNING)) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }

    private boolean contains(String[] arr, String st) {
      if (st == null) return false;
      for (String value : arr)
        if (st.equals(value))
          return true;
      return false;
    }
  }

  static public class CancelActionListener extends EventListener<UINamespaceForm> {
    public void execute(Event<UINamespaceForm> event) throws Exception {
      UINamespaceForm uiForm = event.getSource() ;
      UINamespaceManager uiManager = uiForm.getAncestorOfType(UINamespaceManager.class) ;
      uiManager.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
