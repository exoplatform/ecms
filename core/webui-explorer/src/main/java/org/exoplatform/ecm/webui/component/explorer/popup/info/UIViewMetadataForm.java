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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.*;
import org.exoplatform.webui.form.input.UICheckBoxInput;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 25, 2007
 * 1:47:55 PM
 */
@ComponentConfigs( {
  @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset", 
      events = {
    @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }

      ),
      @ComponentConfig(lifecycle = UIFormLifecycle.class, 
      events = {
        @EventConfig(listeners = UIViewMetadataForm.SaveActionListener.class),
        @EventConfig(listeners = UIViewMetadataForm.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIViewMetadataForm.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIViewMetadataForm.RemoveActionListener.class, phase = Phase.DECODE) }) })
public class UIViewMetadataForm extends UIDialogForm {

  private String nodeType_;
  private static final Log LOG  = ExoLogger.getLogger(UIViewMetadataForm.class.getName());
  public UIViewMetadataForm() throws Exception {
    setActions(ACTIONS);
  }

  public void setNodeType(String nodeType) { nodeType_ = nodeType; }
  public String getNodeType() { return nodeType_; }

  public String getDialogTemplatePath() {
    repositoryName = getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
    MetadataService metadataService = getApplicationComponent(MetadataService.class);
    try {
      return metadataService.getMetadataPath(nodeType_, true);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return null;
  }

  public String getTemplate() { return getDialogTemplatePath(); }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  @SuppressWarnings("unchecked")
  static public class SaveActionListener extends EventListener<UIViewMetadataForm> {
    public void execute(Event<UIViewMetadataForm> event) throws Exception {
      UIViewMetadataForm uiForm = event.getSource();
      UIJCRExplorer uiJCRExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      UIViewMetadataManager uiViewManager = uiForm.getAncestorOfType(UIViewMetadataManager.class);
      Node node = uiViewManager.getViewNode(uiForm.getNodeType());
      Node parent=node.getParent();
      if(parent.isLocked()) {
        parent.getSession().addLockToken(LockUtil.getLockToken(parent));  
      }
      //Add MIX_COMMENT before update property
      Node activityNode = node;
      if(node.isNodeType(NodetypeConstant.NT_RESOURCE)) activityNode = node.getParent();

      if (activityNode.canAddMixin(ActivityCommonService.MIX_COMMENT)) {
        activityNode.addMixin(ActivityCommonService.MIX_COMMENT);
      }
      NodeTypeManager ntManager = uiJCRExplorer.getSession().getWorkspace().getNodeTypeManager();
      PropertyDefinition[] props = ntManager.getNodeType(uiForm.getNodeType()).getPropertyDefinitions();
      List<Value> valueList = new ArrayList<Value>();
      for (PropertyDefinition prop : props) {
        String name = prop.getName();        
        String inputName = name.substring(name.indexOf(":") + 1);
        if (!prop.isProtected()) {
          int requiredType = prop.getRequiredType();
          if (prop.isMultiple()) {
            if (requiredType == 5) { // date
              UIFormDateTimeInput uiFormDateTime = (UIFormDateTimeInput) uiForm.getUIInput(inputName);
              if(uiFormDateTime == null)
                continue;
              valueList.add(uiJCRExplorer.getSession().getValueFactory().createValue(uiFormDateTime.getCalendar()));
              if(! node.hasProperty(name) || node.getProperty(name).getValues()[0].getDate().compareTo(valueList.get(0).getDate()) != 0){
                node.setProperty(name, valueList.toArray(new Value[] {}));
              }
            } else {
              UIFormInput uiInput = uiForm.getUIInput(inputName);
              if(uiInput instanceof UIFormSelectBox) {
                String[] valuesReal = ((UIFormSelectBox)uiInput).getSelectedValues();
                if((!node.hasProperty(name) && valuesReal.length > 0) || (node.hasProperty(name) &&
                    !uiForm.isEqualsValueStringArrays(node.getProperty(name).getValues(), valuesReal)))
                  node.setProperty(name, valuesReal);
              } else {
                List<String> values = (List<String>) ((UIFormMultiValueInputSet) uiInput).getValue();
                if ((!node.hasProperty(name) && values.size() > 0) || (node.hasProperty(name) &&
                    !uiForm.isEqualsValueStringArrays(node.getProperty(name).getValues(),
                        values.toArray(new String[values.size()])))){

                  //--- Sanitize HTML input to avoid XSS attacks
                  for (int i = 0; i < values.size(); i++) {
                    values.set(i, HTMLSanitizer.sanitize(values.get(i)));
                  }
                node.setProperty(name, values.toArray(new String[values.size()]));
              }}
            }
          } else {
            if (requiredType == 6) { // boolean
              UIFormInput uiInput = uiForm.getUIInput(inputName);
              if(uiInput == null)
                continue;
              boolean value = false;
              //2 cases to return true, UIFormSelectBox with value true or UICheckBoxInput checked
              if(uiInput instanceof UIFormSelectBox){
            	  value =  Boolean.parseBoolean(((UIFormSelectBox)uiInput).getValue());
              }else if( uiInput instanceof UICheckBoxInput){
            	  value = ((UICheckBoxInput)uiInput).isChecked();
              }
	            if(!node.hasProperty(name) || (node.hasProperty(name) && node.getProperty(name).getBoolean() != value)) {
		            node.setProperty(name, value);
	            }
            } else if (requiredType == 5) { // date
              UIFormDateTimeInput cal = (UIFormDateTimeInput) uiForm.getUIInput(inputName);
              if(cal == null)
                continue;
              if( !node.hasProperty(name) || cal.getCalendar().compareTo(node.getProperty(name).getDate()) != 0){
                node.setProperty(name, cal.getCalendar());
              }
            } else if(requiredType == 1){
              String value = "";
              if (uiForm.getUIInput(inputName) != null) {
                value = ((UIFormStringInput)uiForm.getUIInput(inputName)).getValue();
                if (value == null) value = "";
              }
              //--- Sanitize HTML input to avoid XSS attacks
              value = HTMLSanitizer.sanitize(value);
              if(!node.hasProperty(name) || (node.hasProperty(name) && !node.getProperty(name).getString().equals(value)))
                node.setProperty(name, value);
            } else if (requiredType == 4) { // double
              UIFormInput uiInput = uiForm.getUIInput(inputName);
              double value = 0;
              if((uiInput == null || StringUtils.isBlank((String) uiInput.getValue())) && node.hasProperty(name)) {
                node.setProperty(name, (Value) null);
              } else {
                try {
                  value = Double.parseDouble((String) uiInput.getValue());
                  if(node.getProperty(name).getDouble() != value){
                    node.setProperty(name, value);
                  }
                } catch (NumberFormatException e) {
                  UIApplication uiapp = uiForm.getAncestorOfType(UIApplication.class);
                  uiapp.addMessage(new ApplicationMessage("UIViewMetadataForm.msg.Invalid-number", null, ApplicationMessage.WARNING));
                  LOG.error("Cannot save field '" + name + "'. The value '" + value + "' is not a number", e);
                }
              }
            }
          }
        }
      }
      //Remove MIX_COMMENT after update property
      if (activityNode.isNodeType(ActivityCommonService.MIX_COMMENT)) {
        activityNode.removeMixin(ActivityCommonService.MIX_COMMENT);
      }
      node.getSession().save();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewManager);
      UIPopupWindow uiPopup = uiViewManager.getChildById(UIViewMetadataManager.METADATAS_POPUP);
      uiPopup.setShow(false);
      uiPopup.setShowMask(true);
    }
  }

  static public class CancelActionListener extends EventListener<UIViewMetadataForm> {
    public void execute(Event<UIViewMetadataForm> event) throws Exception {
      UIViewMetadataForm uiForm = event.getSource();
      UIPopupWindow uiPopup = uiForm.getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent());
    }
  }

  static public class AddActionListener extends EventListener<UIViewMetadataForm> {
    public void execute(Event<UIViewMetadataForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent());
    }
  }

  static public class RemoveActionListener extends EventListener<UIViewMetadataForm> {
    public void execute(Event<UIViewMetadataForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent());
    }
  }

  public boolean isEqualsValueStringArrays(Value[] arrayValue1, String[] arrayValue2) throws ValueFormatException, 
  IllegalStateException, RepositoryException {
    if(arrayValue1 != null) {
      String[] stringArray = new String[arrayValue1.length];
      int i = 0;
      for (Value valueItem : arrayValue1) {  	  	
        if(valueItem != null && valueItem.getString() != null)
          stringArray[i] = valueItem.getString();
        i++;
      }
      if(stringArray != null && stringArray.length > 0)
        Arrays.sort(stringArray);
      if(arrayValue2 != null && arrayValue2.length > 0)
        Arrays.sort(arrayValue2);
      return ArrayUtils.isEquals(stringArray, arrayValue2);  	    
    } else {
      if(arrayValue2 != null) return false;
      else return true;
    }	
  }
}
