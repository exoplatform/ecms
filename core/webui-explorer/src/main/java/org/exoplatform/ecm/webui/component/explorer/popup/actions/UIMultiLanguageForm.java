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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 15, 2007
 * 1:48:19 PM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMultiLanguageForm.ViewActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIMultiLanguageForm.SetDefaultActionListener.class),
      @EventConfig(listeners = UIMultiLanguageForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UIMultiLanguageForm extends UIForm {
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIMultiLanguageForm");
  public UIMultiLanguageForm() throws Exception {
    List<SelectItemOption<String>> languages = new ArrayList<SelectItemOption<String>>() ;
    addUIFormInput(new UIFormSelectBox(Utils.LANGUAGES, Utils.LANGUAGES, languages)) ;
  }

  public void doSelect(Node currentNode) throws Exception {
    List<SelectItemOption<String>> languages = new ArrayList<SelectItemOption<String>>() ;
    if(!currentNode.hasProperty(Utils.EXO_LANGUAGE)) {
      currentNode.addMixin("mix:i18n");
      currentNode.save();
    }
    String defaultLang = currentNode.getProperty(Utils.EXO_LANGUAGE).getString();
    UIMultiLanguageManager uiMultiLanguageManager = getParent();
    List<SelectItemOption<String>> listLang = uiMultiLanguageManager.languages();
    String defaultLangName = "";
    for (SelectItemOption<String> item : listLang) {
      if (item.getValue().trim().equals(defaultLang)) {
        defaultLangName = item.getLabel();
        break;
      }
    }
    WebuiRequestContext webReqContext = WebuiRequestContext.getCurrentInstance();

    languages.add(new SelectItemOption<String>(defaultLangName
        + " ("
        + webReqContext.getApplicationResourceBundle()
                       .getString("UIMultiLanguageForm.label.default") + ")", defaultLang));
    if (currentNode.hasNode(Utils.LANGUAGES)){
      Node languageNode = currentNode.getNode(Utils.LANGUAGES) ;
      NodeIterator iter  = languageNode.getNodes() ;
      while(iter.hasNext()) {
        Node lang = iter.nextNode() ;
        if (!lang.getName().equals(defaultLang)) {
          String label = lang.getName();
          for (SelectItemOption<String> item : listLang) {
            if (item.getValue().trim().equals(lang.getName())) {
              label = item.getLabel();
              break;
            }
          }
          languages.add(new SelectItemOption<String>(label, lang.getName()));
        }
      }
    }
    getUIFormSelectBox(Utils.LANGUAGES).setOptions(languages) ;
    getUIFormSelectBox(Utils.LANGUAGES).setValue(defaultLang) ;
  }

  static public class CancelActionListener extends EventListener<UIMultiLanguageForm> {
    public void execute(Event<UIMultiLanguageForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  static public class SetDefaultActionListener extends EventListener<UIMultiLanguageForm> {
    public void execute(Event<UIMultiLanguageForm> event) throws Exception {
      UIMultiLanguageForm uiForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      Node node = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(node);
      if(!PermissionUtil.canAddNode(uiExplorer.getCurrentNode())) {
        throw new MessageException(new ApplicationMessage("UIMultiLanguageForm.msg.access-denied",
                                                          null, ApplicationMessage.WARNING)) ;
      }
      MultiLanguageService multiLanguageService =
        uiForm.getApplicationComponent(MultiLanguageService.class) ;
      String selectedLanguage = uiForm.getUIFormSelectBox(Utils.LANGUAGES).getValue() ;
      try {
        multiLanguageService.setDefault(uiExplorer.getCurrentNode(), selectedLanguage, uiExplorer.getRepositoryName()) ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIMultiLanguageForm.msg.access-denied", null,
                                                ApplicationMessage.WARNING)) ;
        
        return ;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        JCRExceptionManager.process(uiApp, e) ;
        return ;
      }
      uiExplorer.setLanguage(selectedLanguage) ;
      uiExplorer.setIsHidePopup(false) ;
      uiExplorer.updateAjax(event) ;
    }
  }

  static public class ViewActionListener extends EventListener<UIMultiLanguageForm> {
    public void execute(Event<UIMultiLanguageForm> event) throws Exception {
      UIMultiLanguageForm uiForm = event.getSource() ;
      UIJCRExplorer uiJCRExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIDocumentInfo uiDocumentInfo = uiJCRExplorer.findFirstComponentOfType(UIDocumentInfo.class) ;
      String selectedLanguage = uiForm.getUIFormSelectBox(Utils.LANGUAGES).getValue() ;
      uiDocumentInfo.setLanguage(selectedLanguage);
      uiJCRExplorer.setIsHidePopup(false);
      uiJCRExplorer.updateAjax(event);
    }
  }
}
