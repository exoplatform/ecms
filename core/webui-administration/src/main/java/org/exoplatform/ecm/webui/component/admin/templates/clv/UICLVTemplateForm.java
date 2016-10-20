/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.templates.clv;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 22, 2013
 * 9:55:24 AM  
 */
@ComponentConfig(
        lifecycle = UIFormLifecycle.class,
        template =  "system:/groovy/webui/form/UIForm.gtmpl",
        events = {
          @EventConfig(listeners = UICLVTemplateForm.SaveActionListener.class),
          @EventConfig(listeners = UICLVTemplateForm.ResetActionListener.class, phase=Phase.DECODE),
          @EventConfig(listeners = UICLVTemplateForm.CancelActionListener.class, phase=Phase.DECODE)
        }
    )
public class UICLVTemplateForm extends UIForm {

  final static public String FIELD_TITLE = "title" ;
  final static public String FIELD_TEMPLATE_NAME = "template" ;
  final static public String FIELD_CONTENT = "content" ;
  final static public String FIELD_CONTENT_TYPE = "type" ;
  
  private boolean isAddNew;
  private String selectedCategory;
  
  public UICLVTemplateForm() throws Exception {
    
    UIFormTextAreaInput contentInput = new UIFormTextAreaInput(FIELD_CONTENT, FIELD_CONTENT, null);
    addUIFormInput(contentInput);
    UIFormStringInput nameInput = new UIFormStringInput(FIELD_TITLE, FIELD_TITLE, null);
    nameInput.addValidator(ECMNameValidator.class);
    addUIFormInput(nameInput);
    UIFormStringInput tempateNameInput = new UIFormStringInput(FIELD_TEMPLATE_NAME, FIELD_TEMPLATE_NAME, null);
    tempateNameInput.addValidator(MandatoryValidator.class).addValidator(ECMNameValidator.class);
    addUIFormInput(tempateNameInput);
    List<SelectItemOption<String>> templateOptions = new ArrayList<SelectItemOption<String>>();
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    templateOptions.add(new SelectItemOption<String>(
            res.getString("UICLVTemplateForm.label." + ApplicationTemplateManagerService.CLV_LIST_TEMPLATE_CATEGORY), 
            ApplicationTemplateManagerService.CLV_LIST_TEMPLATE_CATEGORY));
    templateOptions.add(new SelectItemOption<String>(
            res.getString("UICLVTemplateForm.label." + ApplicationTemplateManagerService.CLV_NAVIGATION_TEMPLATE_CATEGORY), 
            ApplicationTemplateManagerService.CLV_NAVIGATION_TEMPLATE_CATEGORY));
    templateOptions.add(new SelectItemOption<String>(
            res.getString("UICLVTemplateForm.label." + ApplicationTemplateManagerService.CLV_PAGINATOR_TEMPLATE_CATEGORY), 
            ApplicationTemplateManagerService.CLV_PAGINATOR_TEMPLATE_CATEGORY ));
    UIFormSelectBox templateType = new UIFormSelectBox(FIELD_CONTENT_TYPE, FIELD_CONTENT_TYPE, templateOptions);
    addUIFormInput(templateType);
  }
  
  public void refresh(String category) throws Exception {
    isAddNew = true;
    selectedCategory = category;
    setActions(new String[] {"Save", "Reset", "Cancel"});
    getUIStringInput(FIELD_TITLE).setValue(StringUtils.EMPTY);
    getUIStringInput(FIELD_TEMPLATE_NAME).setValue(StringUtils.EMPTY);
    getUIStringInput(FIELD_TEMPLATE_NAME).setDisabled(false);
    getUIFormTextAreaInput(FIELD_CONTENT).setValue(StringUtils.EMPTY);
    getUIFormSelectBox(FIELD_CONTENT_TYPE).setValue(selectedCategory);
  }
  
  public void update(String category, String name) throws Exception {
    this.isAddNew = false;
    selectedCategory = category;
    setActions(new String[] {"Save", "Cancel"});
    ApplicationTemplateManagerService templateManager = WCMCoreUtils.getService(ApplicationTemplateManagerService.class);
    Node templateNode = templateManager.getTemplateByName(ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
            category, name, WCMCoreUtils.getUserSessionProvider());
    Node content = templateNode.getNode(Utils.JCR_CONTENT);
    try {
      getUIStringInput(FIELD_TITLE).setValue(content.getProperty(NodetypeConstant.DC_TITLE).getValues()[0].getString());
    } catch(PathNotFoundException pne) {
      getUIStringInput(FIELD_TITLE).setValue(templateNode.getName());
    } catch(ArrayIndexOutOfBoundsException aoe) {
      getUIStringInput(FIELD_TITLE).setValue(templateNode.getName());
    }
    getUIStringInput(FIELD_TEMPLATE_NAME).setValue(templateNode.getName());
    getUIStringInput(FIELD_TEMPLATE_NAME).setDisabled(true);
    getUIFormTextAreaInput(FIELD_CONTENT).setValue(content.getProperty(Utils.JCR_DATA).getString());
    getUIFormSelectBox(FIELD_CONTENT_TYPE).setValue(category);
  }

  private void addTemplate(String category, String title, String template, String content) throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    ApplicationTemplateManagerService appTemplateManager = WCMCoreUtils.getService(ApplicationTemplateManagerService.class);
    if(content == null) content = StringUtils.EMPTY;
    if(isAddNew) {
      if(!template.contains(".gtmpl")) template = template + ".gtmpl";
      if(title == null || title.length() == 0) title = template;
      templateService.createTemplate(getCategoryByName(category), title, 
              template, new ByteArrayInputStream(content.getBytes()), new String[] { "*" });
    } else {
      if(hasTemplate(category, template)) {
        if(!selectedCategory.equals(category)) {
          UIApplication uiApp = getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UICLVTemplateForm.msg.template-existing", null, ApplicationMessage.WARNING));
          return;
        } else {
          Node templateNode = getCategoryByName(category).getNode(template);
          Node contentNode = templateNode.getNode(NodetypeConstant.JCR_CONTENT);
          contentNode.setProperty(NodetypeConstant.JCR_DATA, new ByteArrayInputStream(content.getBytes()));
          if(title == null || title.length() == 0) title = templateNode.getName();
          contentNode.setProperty(NodetypeConstant.DC_TITLE, new String[] { title });
          templateNode.save();
        }
      } else {
        templateService.createTemplate(getCategoryByName(category), title, 
                template, new ByteArrayInputStream(content.getBytes()), new String[] { "*" });
        appTemplateManager.removeTemplate(ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
                selectedCategory, template, WCMCoreUtils.getUserSessionProvider());
      }
      
    }
  }
  
  private boolean hasTemplate(String category, String template) throws Exception {
    if(!template.contains(".gtmpl")) template = template + ".gtmpl";
    return getCategoryByName(category).hasNode(template);
  }
  
  private Node getCategoryByName(String category) throws Exception {
    ApplicationTemplateManagerService templateManager = WCMCoreUtils.getService(ApplicationTemplateManagerService.class);
    Node templateHome = templateManager.getApplicationTemplateHome(
            ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, WCMCoreUtils.getUserSessionProvider());
    return templateHome.getNode(category);
  }
  
  static public class SaveActionListener extends EventListener<UICLVTemplateForm> {
    public void execute(Event<UICLVTemplateForm> event) throws Exception {
      UICLVTemplateForm uiForm = event.getSource() ;
      UICLVTemplatesManager uiManager = uiForm.getAncestorOfType(UICLVTemplatesManager.class);
      String title = uiForm.getUIStringInput(FIELD_TITLE).getValue();
	    if(title != null) title = title.trim();
      String template = uiForm.getUIStringInput(FIELD_TEMPLATE_NAME).getValue();
	    if(template != null) template = template.trim();
      String category = uiForm.getUIFormSelectBox(FIELD_CONTENT_TYPE).getValue();
      String content = uiForm.getUIFormTextAreaInput(FIELD_CONTENT).getValue();
	    if(content != null) content = content.trim();
      if(uiForm.isAddNew & uiForm.hasTemplate(category, template)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UICLVTemplateForm.msg.template-existing", null, ApplicationMessage.WARNING));
        return;
      }
      uiForm.addTemplate(category, title, template, content);
      uiManager.refresh();
      UIPopupWindow uiPopup = uiForm.getParent();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
  
  static public class ResetActionListener extends EventListener<UICLVTemplateForm> {
    public void execute(Event<UICLVTemplateForm> event) throws Exception {
      UICLVTemplateForm uiForm = event.getSource() ;
      uiForm.refresh(uiForm.selectedCategory);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
  
  static public class CancelActionListener extends EventListener<UICLVTemplateForm> {
    public void execute(Event<UICLVTemplateForm> event) throws Exception {
      UICLVTemplateForm uiForm = event.getSource() ;
      UICLVTemplatesManager uiManager = uiForm.getAncestorOfType(UICLVTemplatesManager.class);
      UIPopupWindow uiPopup = uiForm.getParent();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
}
