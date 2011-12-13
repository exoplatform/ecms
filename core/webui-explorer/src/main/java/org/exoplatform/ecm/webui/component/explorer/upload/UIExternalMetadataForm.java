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
package org.exoplatform.ecm.webui.component.explorer.upload;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 25, 2007 3:58:09 PM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
    @EventConfig(listeners = UIExternalMetadataForm.AddActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIExternalMetadataForm.CancelActionListener.class) })
public class UIExternalMetadataForm extends UIForm {

  public UIExternalMetadataForm() throws Exception {
  }

  public void renderExternalList() throws Exception {
    MetadataService metadataService = getApplicationComponent(MetadataService.class) ;
    UIFormCheckBoxInput<String> uiCheckBox ;
    for(NodeType nodeType : metadataService.getAllMetadatasNodeType()) {
      uiCheckBox = new UIFormCheckBoxInput<String>(nodeType.getName(), nodeType.getName(), "") ;
      if(!isInternalUse(nodeType)) {
        if(hasExternalMetadata(nodeType.getName())) {
          uiCheckBox.setChecked(true) ;
          uiCheckBox.setEnable(false) ;
        } else {
          uiCheckBox.setChecked(false) ;
          uiCheckBox.setEnable(true) ;
        }
        addUIFormInput(uiCheckBox) ;
      }
    }
  }

  private boolean isInternalUse(NodeType nodeType) throws Exception{
    for(PropertyDefinition pro : nodeType.getPropertyDefinitions()) {
      if(pro.getName().equals("exo:internalUse")) {
        return pro.getDefaultValues()[0].getBoolean();
      }
    }
    return false;
  }

  private boolean hasExternalMetadata(String name) throws Exception {
    UIUploadManager uiUploadManager = getAncestorOfType(UIUploadManager.class) ;
    UIUploadContainer uiUploadContainer = uiUploadManager.getChild(UIUploadContainer.class) ;
    Node uploaded = uiUploadContainer.getUploadedNode() ;
    for(NodeType mixin : uploaded.getMixinNodeTypes()) {
      if(mixin.getName().equals(name)) return true ;
    }
    if(uploaded.hasNode(Utils.JCR_CONTENT)) {
      for(NodeType mixin : uploaded.getNode(Utils.JCR_CONTENT).getMixinNodeTypes()) {
        if(mixin.getName().equals(name)) return true ;
      }
    }
    return false ;
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIExternalMetadataForm.label." + id) ;
    } catch (MissingResourceException ex) {
      return '_' + id ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIExternalMetadataForm> {
    public void execute(Event<UIExternalMetadataForm> event) throws Exception {
      UIUploadManager uiUploadManager = event.getSource().getAncestorOfType(UIUploadManager.class) ;
      UIPopupWindow uiPopup = uiUploadManager.getChildById(UIUploadManager.EXTARNAL_METADATA_POPUP) ;
      uiPopup.setShow(false) ;
      uiPopup.setRendered(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager) ;
    }
  }

  static  public class AddActionListener extends EventListener<UIExternalMetadataForm> {
    public void execute(Event<UIExternalMetadataForm> event) throws Exception {
      UIExternalMetadataForm uiExternalMetadataForm = event.getSource() ;
      List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
      uiExternalMetadataForm.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      UIUploadManager uiUploadManager = event.getSource().getAncestorOfType(UIUploadManager.class) ;
      UIUploadContainer uiContainer = uiUploadManager.getChild(UIUploadContainer.class) ;
      String metadataName = null ;
      Node uploadedNode = uiContainer.getUploadedNode() ;
      for(int i = 0; i < listCheckbox.size(); i ++) {
        if(listCheckbox.get(i).isChecked() && listCheckbox.get(i).isEnable()) {
          metadataName = listCheckbox.get(i).getName() ;
          if(!uploadedNode.canAddMixin(metadataName)) {
            UIApplication uiApp = uiExternalMetadataForm.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UIExternalMetadataForm.msg.can-not-add", null,
                                                    ApplicationMessage.WARNING)) ;
            
            return ;
          }
          uploadedNode.addMixin(metadataName);
          createMandatoryPropertyValue(uploadedNode, metadataName);
          uploadedNode.save() ;
          UIUploadContent uiUploadContent = uiContainer.getChild(UIUploadContent.class) ;
          uiUploadContent.externalList_.add(metadataName) ;
        }
      }
      uploadedNode.getSession().save() ;
      UIPopupWindow uiPopup = uiUploadManager.getChildById(UIUploadManager.EXTARNAL_METADATA_POPUP) ;
      uiPopup.setShow(false) ;
      uiPopup.setRendered(false) ;
      uiContainer.setRenderedChild(UIUploadContent.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager) ;
    }

    private void createMandatoryPropertyValue(Node node, String nodeTypeName) throws Exception {
      NodeTypeManager nodeTypeManager = node.getSession().getWorkspace().getNodeTypeManager();
      NodeType nodeType = nodeTypeManager.getNodeType(nodeTypeName);
      for (PropertyDefinition propertyDefinition : nodeType.getPropertyDefinitions()) {
        if (propertyDefinition.isMandatory() &&
            (!propertyDefinition.isAutoCreated() || !node.hasProperty(propertyDefinition.getName()))&&
            !propertyDefinition.isProtected()) {
          String propertyName = propertyDefinition.getName();
          int requiredType = propertyDefinition.getRequiredType();
          if (!propertyDefinition.isMultiple()) {
            switch (requiredType) {
            case PropertyType.STRING:
              node.setProperty(propertyName, StringUtils.EMPTY);
              break;
            case PropertyType.BINARY:
              node.setProperty(propertyName, "");
              break;
            case PropertyType.BOOLEAN:
              node.setProperty(propertyName, false);
              break;
            case PropertyType.LONG:
              node.setProperty(propertyName, 0);
              break;
            case PropertyType.DOUBLE:
              node.setProperty(propertyName, 0);
              break;
            case PropertyType.DATE:
              node.setProperty(propertyName, new GregorianCalendar());
              break;
            case PropertyType.REFERENCE:
              node.setProperty(propertyName, "");
              break;
            }
          } else {
            switch (requiredType) {
            case PropertyType.STRING:
              node.setProperty(propertyName, new String[] { StringUtils.EMPTY });
              break;
            case PropertyType.BINARY:
              node.setProperty(propertyName, new String[] { "" });
              break;
            case PropertyType.BOOLEAN:
              node.setProperty(propertyName, new Value[] { node.getSession()
                                                               .getValueFactory()
                                                               .createValue(false) });
              break;
            case PropertyType.LONG:
              node.setProperty(propertyName, new Value[] { node.getSession()
                                                               .getValueFactory()
                                                               .createValue(0L) });
              break;
            case PropertyType.DOUBLE:
              node.setProperty(propertyName, new Value[] { node.getSession()
                                                               .getValueFactory()
                                                               .createValue(0) });
              break;
            case PropertyType.DATE:
              node.setProperty(propertyName,
                               new Value[] { node.getSession()
                                                 .getValueFactory()
                                                 .createValue(new GregorianCalendar()) });
              break;
            case PropertyType.REFERENCE:
              node.setProperty(propertyName, new String[] {});
              break;
            }
          }
        }
      }
    }
  }
}
