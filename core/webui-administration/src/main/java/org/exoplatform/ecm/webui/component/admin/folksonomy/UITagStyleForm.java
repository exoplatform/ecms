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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
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
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 11, 2007
 * 2:56:02 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    //template =  "system:/groovy/webui/form/UIForm.gtmpl",
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITagStyleForm.UpdateStyleActionListener.class),
      @EventConfig(listeners = UITagStyleForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UITagStyleForm extends UIForm {

  final static public String STYLE_NAME = "styleName" ;
  final static public String DOCUMENT_RANGE = "documentRange" ;
  final static public String STYLE_HTML = "styleHTML" ;
  private static final Log LOG  = ExoLogger.getLogger("admin.UITagStyleForm");
  private NodeLocation selectedTagStyle_ ;

  public UITagStyleForm() throws Exception {
    addUIFormInput(new UIFormStringInput(STYLE_NAME, STYLE_NAME, null).addValidator(MandatoryValidator.class)) ;
    addUIFormInput(new UIFormStringInput(DOCUMENT_RANGE, DOCUMENT_RANGE, null).addValidator(MandatoryValidator.class)) ;
    addUIFormInput(new UIFormTextAreaInput(STYLE_HTML, STYLE_HTML, null).addValidator(MandatoryValidator.class)) ;
  }

  public Node getTagStyle() { 
    return NodeLocation.getNodeByLocation(selectedTagStyle_); 
  }

  public void setTagStyle(Node selectedTagStyle) throws Exception {
    selectedTagStyle_ = NodeLocation.getNodeLocationByNode(selectedTagStyle);
    if (selectedTagStyle != null) {
      getUIStringInput(STYLE_NAME).setValue(selectedTagStyle.getName()) ;
      getUIStringInput(STYLE_NAME).setEditable(false) ;
      String range = selectedTagStyle.getProperty(UITagStyleList.RANGE_PROP).getValue().getString() ;
      getUIStringInput(DOCUMENT_RANGE).setValue(range) ;
      String htmlStyle = selectedTagStyle.getProperty(UITagStyleList.HTML_STYLE_PROP).getValue().getString() ;
      getUIFormTextAreaInput(STYLE_HTML).setValue(htmlStyle) ;
    }
  }

  private boolean validateRange(String range) {
    String[] vars = null ;
    try {
      vars = StringUtils.split(range,"..") ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return false ;
    }
    if(vars == null || vars.length!= 2) return false ;
    String minRange = vars[0], maxRange = vars[1] ;
    if(!StringUtils.isNumeric(minRange)) return false ;
    try {
      int min = Integer.parseInt(vars[0]) ;
      if(min<0) return false ;
      if(!StringUtils.isNumeric(maxRange)) {
        if(!maxRange.equals("*")) return false ;
      } else {
        if(Integer.parseInt(maxRange)<=0) return false ;
      }
    } catch(Exception e) {
      return false ;
    }
    return true ;
  }

  static public class UpdateStyleActionListener extends EventListener<UITagStyleForm> {
    public void execute(Event<UITagStyleForm> event) throws Exception {
      UITagStyleForm uiForm = event.getSource() ;
      UITagManager uiManager = uiForm.getAncestorOfType(UITagManager.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;

      String repository = uiForm.getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
      String workspace = uiForm.getAncestorOfType(UIECMAdminPortlet.class).getDMSSystemWorkspace(repository);

      String documentRange = uiForm.getUIStringInput(DOCUMENT_RANGE).getValue() ;
      String styleHTML = uiForm.getUIFormTextAreaInput(STYLE_HTML).getValue() ;
      if(!uiForm.validateRange(documentRange)) {
        uiApp.addMessage(new ApplicationMessage("UITagStyleForm.msg.range-validator", null)) ;
        return ;
      }
      try {
        // add new tag
        if (uiForm.getTagStyle() == null) {
          String tagStyleName = uiForm.getUIStringInput(STYLE_NAME).getValue().trim();
          NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class) ;
          newFolksonomyService.addTagStyle(tagStyleName, "", "", workspace);
          for(Node tagStyle: newFolksonomyService.getAllTagStyle(workspace))
            if(tagStyle.getName().equals(tagStyleName)) {
              uiForm.selectedTagStyle_ = NodeLocation.getNodeLocationByNode(tagStyle);
              break;
            }
        }
        uiForm.getTagStyle().setProperty(UITagStyleList.RANGE_PROP, documentRange) ;
        uiForm.getTagStyle().setProperty(UITagStyleList.HTML_STYLE_PROP, styleHTML) ;
        uiForm.getTagStyle().save() ;
        uiForm.getTagStyle().getSession().save() ;
        UITagStyleList uiTagList = uiManager.getChild(UITagStyleList.class) ;
        uiTagList.refresh(uiTagList.getUIPageIterator().getCurrentPage());
      } catch (ItemNotFoundException ie) {
        String key = "UITagStyleForm.msg.item-not-existing" ;
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING)) ;
        return ;
      } catch(Exception e) {
        String key = "UITagStyleForm.msg.error-update" ;
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING)) ;
        return ;
      }
      UIPopupWindow uiPopup = uiManager.getChild(UIPopupWindow.class) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class CancelActionListener extends EventListener<UITagStyleForm> {
    public void execute(Event<UITagStyleForm> event) throws Exception {
      UITagStyleForm uiForm = event.getSource() ;
      UITagManager uiManager = uiForm.getAncestorOfType(UITagManager.class) ;
      UIPopupWindow uiPopup = uiManager.getChild(UIPopupWindow.class) ;
      uiPopup.setShow(false) ;      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
