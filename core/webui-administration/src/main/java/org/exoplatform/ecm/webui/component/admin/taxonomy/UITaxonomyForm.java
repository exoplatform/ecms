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
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.services.cms.categories.CategoriesService;
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
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Sep 29, 2006
 * 11:57:24 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITaxonomyForm.SaveActionListener.class),
      @EventConfig(listeners = UITaxonomyForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UITaxonomyForm extends UIForm {

  final static private String FIELD_PARENT = "parentPath" ;
  final static private String FIELD_NAME = "taxonomyName" ;

  public UITaxonomyForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(FIELD_PARENT, FIELD_PARENT, null)) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
          addValidator(MandatoryValidator.class).addValidator(ECMNameValidator.class)) ;
  }

  public void setParent(String path) throws Exception {
    String rootPath = getAncestorOfType(UITaxonomyManager.class).getRootNode().getPath();
    path = path.replaceFirst(rootPath, "") ;
    getUIFormInputInfo(FIELD_PARENT).setValue(path) ;
    getUIStringInput(FIELD_NAME).setValue(null) ;
  }

  public void addTaxonomy(String parentPath, String name) throws Exception {
    UITaxonomyManager uiManager = getAncestorOfType(UITaxonomyManager.class) ;
    getApplicationComponent(CategoriesService.class).addTaxonomy(parentPath,
                                                                 name,
                                                                 uiManager.getRepository()
                                                                          .getConfiguration()
                                                                          .getName());
  }

  static public class SaveActionListener extends EventListener<UITaxonomyForm> {
    public void execute(Event<UITaxonomyForm> event) throws Exception {
      UITaxonomyForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UITaxonomyManager uiManager = uiForm.getAncestorOfType(UITaxonomyManager.class) ;
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
      if(name == null || name.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UITaxonomyForm.msg.name-null", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }

      if(!Utils.isNameValid(name, new String[]{"&", "$", "@", ",", ":","]", "[", "*", "%", "!"})) {
        uiApp.addMessage(new ApplicationMessage("UITaxonomyForm.msg.name-invalid", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }

      if(name.length() > 30) {
        uiApp.addMessage(new ApplicationMessage("UITaxonomyForm.msg.name-too-long", null,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String rootPath = uiManager.getRootNode().getPath();
      String parentPath = rootPath + uiForm.getUIFormInputInfo(FIELD_PARENT).getValue() ;
      try {
        uiForm.addTaxonomy(parentPath, name)  ;
        uiManager.update(parentPath) ;
      } catch(Exception e) {
        Object[] arg = {name} ;
        uiApp.addMessage(new ApplicationMessage("UITaxonomyForm.msg.exist", arg,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      uiForm.reset() ;
      UIPopupWindow uiPopup = uiForm.getParent() ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class CancelActionListener extends EventListener<UITaxonomyForm> {
    public void execute(Event<UITaxonomyForm> event) throws Exception {
      UITaxonomyForm uiForm = event.getSource() ;
      UIPopupWindow uiPopup = uiForm.getParent() ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
}
