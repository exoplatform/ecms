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
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

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
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UISingleExternalMetadataForm.AddActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UISingleExternalMetadataForm.CancelActionListener.class)
    }
)
public class UISingleExternalMetadataForm extends UIForm {

  public UISingleExternalMetadataForm() throws Exception {
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
//    PropertyDefinition def =
//      ((ExtendedNodeType)nodeType).getPropertyDefinitions("exo:internalUse").getAnyDefinition() ;
//    return !def.getDefaultValues()[0].getBoolean() ;
  }

  private boolean hasExternalMetadata(String name) throws Exception {
    UISingleUploadManager uiUploadManager = getAncestorOfType(UISingleUploadManager.class) ;
    UISingleUploadContainer uiUploadContainer = uiUploadManager.getChild(UISingleUploadContainer.class) ;
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

  static  public class CancelActionListener extends EventListener<UISingleExternalMetadataForm> {
    public void execute(Event<UISingleExternalMetadataForm> event) throws Exception {
      UISingleUploadManager uiUploadManager = event.getSource().getAncestorOfType(UISingleUploadManager.class) ;
      UIPopupWindow uiPopup = uiUploadManager.getChildById(UIUploadManager.EXTARNAL_METADATA_POPUP) ;
      uiPopup.setShow(false) ;
      uiPopup.setRendered(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager) ;
    }
  }

  static  public class AddActionListener extends EventListener<UISingleExternalMetadataForm> {
    public void execute(Event<UISingleExternalMetadataForm> event) throws Exception {
      UISingleExternalMetadataForm uiExternalMetadataForm = event.getSource() ;
      List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
      uiExternalMetadataForm.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      UISingleUploadManager uiUploadManager = event.getSource().getAncestorOfType(UISingleUploadManager.class) ;
      UISingleUploadContainer uiContainer = uiUploadManager.getChild(UISingleUploadContainer.class) ;
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
          uploadedNode.addMixin(metadataName) ;
          uploadedNode.save() ;
          UISingleUploadContent uiUploadContent = uiContainer.getChild(UISingleUploadContent.class);
          uiUploadContent.externalList_.add(metadataName) ;
        }
      }
      uploadedNode.getSession().save() ;
      UIPopupWindow uiPopup = uiUploadManager.getChildById(UIUploadManager.EXTARNAL_METADATA_POPUP) ;
      uiPopup.setShow(true);
      uiPopup.setRendered(false) ;
      uiPopup.setShowMask(true);
      uiContainer.setRenderedChild(UISingleUploadContent.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager) ;
    }
  }
}
