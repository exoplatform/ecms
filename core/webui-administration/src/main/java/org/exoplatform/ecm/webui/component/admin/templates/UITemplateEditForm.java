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
package org.exoplatform.ecm.webui.component.admin.templates;

import javax.jcr.Node;

import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 4, 2006 9:50:06 AM
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "system:/groovy/webui/form/UIForm.gtmpl",
                 events = {
                   @EventConfig(listeners = UITemplateEditForm.SaveActionListener.class),
                   @EventConfig(phase=Phase.DECODE, listeners = UITemplateEditForm.CancelActionListener.class)
                 }
    )

public class UITemplateEditForm extends UIForm {

  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_LABEL = "label" ;
  final static public String FIELD_ISTEMPLATE = "isDocumentTemplate" ;

  public UITemplateEditForm() {
    addChild(new UIFormStringInput(FIELD_NAME, null)) ;
    addChild(new UIFormStringInput(FIELD_LABEL, null)) ;
    addChild(new UICheckBoxInput(FIELD_ISTEMPLATE, null, null)) ;
  }

  private boolean isDocumentTemplate(String nodeType)throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    return templateService.getDocumentTemplates().contains(nodeType) ;
  }

  public void update(String nodeType) throws Exception {
    TemplateService tempService = getApplicationComponent(TemplateService.class) ;
    Node node = tempService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider()).getNode(nodeType) ;
    String label = null ;
    if(node.hasProperty(TemplateService.TEMPLATE_LABEL)) {
      label = node.getProperty(TemplateService.TEMPLATE_LABEL).getString() ;
    }
    getUICheckBoxInput(FIELD_ISTEMPLATE).setChecked(isDocumentTemplate(nodeType)) ;
    getUIStringInput(FIELD_NAME).setValue(nodeType) ;
    getUIStringInput(FIELD_LABEL).setValue(label) ;
    getUICheckBoxInput(FIELD_ISTEMPLATE).setDisabled(true);
    getUIStringInput(FIELD_NAME).setDisabled(true);
  }

  static public class SaveActionListener extends EventListener<UITemplateEditForm> {
    public void execute(Event<UITemplateEditForm> event) throws Exception {
      UITemplateEditForm uiForm = event.getSource() ;
      TemplateService tempService = uiForm.getApplicationComponent(TemplateService.class) ;
      String nodeType = ((UIFormStringInput)(event.getSource().getChildById("name"))).getValue();
      Node node = tempService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider()).getNode(nodeType) ;
      node.setProperty(TemplateService.TEMPLATE_LABEL,uiForm.getUIStringInput(FIELD_LABEL).getValue()) ;
      node.save() ;
      uiForm.reset() ;
      UITemplatesManager uiManager = uiForm.getAncestorOfType(UITemplatesManager.class) ;
      UITemplateContainer uiTemplateContainer = uiManager.getChildById(uiManager.getSelectedTabId());
      UITemplateList uiList = uiTemplateContainer.getChild(UITemplateList.class);
      uiList.refresh(uiList.getUIPageIterator().getCurrentPage());
      UIPopupWindow uiPopupWindow = uiManager.getChildById(UITemplatesManager.POPUP_TEMPLATE_ID) ;
      uiPopupWindow.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;

    }
  }

  static  public class CancelActionListener extends EventListener<UITemplateEditForm> {
    public void execute(Event<UITemplateEditForm> event) throws Exception {    	     
      UITemplatesManager uiManager = event.getSource().getAncestorOfType(UITemplatesManager.class) ;
      UIPopupWindow uiPopupWindow = uiManager.getChildById(UITemplatesManager.POPUP_TEMPLATE_ID) ;
      uiPopupWindow.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
