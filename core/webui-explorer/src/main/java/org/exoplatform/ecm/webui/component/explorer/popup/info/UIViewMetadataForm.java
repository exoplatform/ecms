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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

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
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIViewMetadataForm");
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
              valueList.add(uiJCRExplorer.getSession().getValueFactory().createValue(uiFormDateTime.getCalendar()));
              node.setProperty(name, valueList.toArray(new Value[] {}));
            } else {
              UIFormInput uiInput = uiForm.getUIInput(inputName);
              if(uiInput instanceof UIFormSelectBox) {
                String[] valuesReal = ((UIFormSelectBox)uiInput).getSelectedValues();
                node.setProperty(name, valuesReal);
              } else {
                List<String> values = (List<String>) ((UIFormMultiValueInputSet)uiInput).getValue();
                node.setProperty(name, values.toArray(new String[values.size()]));
              }
            }
          } else {
            if (requiredType == 6) { // boolean
              UIFormInput uiInput = uiForm.getUIInput(inputName);
              String value = "false";
              if(uiInput instanceof UIFormSelectBox) value =  ((UIFormSelectBox)uiInput).getValue();
              node.setProperty(name, Boolean.parseBoolean(value));
            } else if (requiredType == 5) { // date
              UIFormDateTimeInput cal = (UIFormDateTimeInput) uiForm.getUIInput(inputName);
              node.setProperty(name, cal.getCalendar());
            } else if(requiredType == 1){
              String value = "";
              if (uiForm.getUIInput(inputName) != null) {
                value = ((UIFormStringInput)uiForm.getUIInput(inputName)).getValue();
                if (value == null) value = "";
              }
              node.setProperty(name, value);
            }
          }
        }
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
}
