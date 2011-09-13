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

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.metadata.MetadataService;
//import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeType;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 24, 2007 5:48:57 PM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/upload/UIListMetadata.gtmpl",
    events = {
        @EventConfig(listeners = UIListMetadata.EditActionListener.class)
    }
)
public class UIListMetadata extends UIContainer {
  public List<String> externalList_ = new ArrayList<String>() ;
  private boolean isExternalMetadata = false;

  public void setIsExternalMetadata(boolean isExternal) {
    isExternalMetadata = isExternal;
  }

  public boolean getIsExternalMetadata() {
    return isExternalMetadata;
  }

  public Node getUploadedNode() { return ((UIUploadContainer)getParent()).getUploadedNode() ; }

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

  public List<String> getExternalList() throws Exception {
    NodeType[] mixinTypes = getUploadedNode().getMixinNodeTypes();
    for(NodeType nodeType : mixinTypes) {
      if (nodeType.getName().equals(Utils.EXO_METADATA) && !isInternalUse(nodeType)
          && !externalList_.contains(nodeType.getName())) {
        externalList_.add(nodeType.getName()) ;
      }
      for(NodeType superType : nodeType.getSupertypes()) {
        if (superType.getName().equals(Utils.EXO_METADATA) && !isInternalUse(nodeType)
            && !externalList_.contains(nodeType.getName())) {
          externalList_.add(nodeType.getName()) ;
        }
      }
    }
    if(getUploadedNode().hasNode(Utils.JCR_CONTENT)) {
      for(NodeType nodeType : getUploadedNode().getNode(Utils.JCR_CONTENT).getMixinNodeTypes()) {
        if(nodeType.isNodeType(Utils.EXO_METADATA) && !isInternalUse(nodeType) && !externalList_.contains(nodeType.getName())) {
          externalList_.add(nodeType.getName()) ;
        }
      }
    }

    return externalList_ ;
  }

  public UIListMetadata() throws Exception {
  }

  static public class EditActionListener extends EventListener<UIListMetadata> {
    public void execute(Event<UIListMetadata> event) throws Exception {
      UIListMetadata uiUploadContent = event.getSource() ;
      UIUploadContainer uiUploadContainer = uiUploadContent.getParent() ;
      MetadataService metadataService = uiUploadContent.getApplicationComponent(MetadataService.class) ;
      UIJCRExplorer uiExplorer = uiUploadContent.getAncestorOfType(UIJCRExplorer.class) ;
      String nodeType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String template = metadataService.getMetadataTemplate(nodeType, true) ;
      if(template == null || template.trim().length() == 0) {
        UIApplication uiApp = uiUploadContent.getAncestorOfType(UIApplication.class) ;
        Object[] args = {nodeType} ;
        uiApp.addMessage(new ApplicationMessage("UIUploadContent.msg.has-not-template", args,
                         ApplicationMessage.WARNING)) ;
        
        return ;
      }
      UIAddMetadataForm uiAddMetadataForm = uiUploadContainer.getChild(UIAddMetadataForm.class);
      if (uiAddMetadataForm == null) {
        uiAddMetadataForm = uiUploadContainer.createUIComponent(UIAddMetadataForm.class, null, null);
        uiUploadContainer.addChild(uiAddMetadataForm);
      }
      uiAddMetadataForm.getChildren().clear() ;
      uiAddMetadataForm.setNodeType(nodeType) ;
      uiAddMetadataForm.setIsNotEditNode(true) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      uiAddMetadataForm.setWorkspace(currentNode.getSession().getWorkspace().getName()) ;
      uiAddMetadataForm.setStoredPath(currentNode.getPath()) ;
      uiAddMetadataForm.setChildPath(uiUploadContainer.getEditNode(nodeType).getPath()) ;
      uiUploadContainer.setRenderedChild(UIAddMetadataForm.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadContainer) ;
    }
  }
}
