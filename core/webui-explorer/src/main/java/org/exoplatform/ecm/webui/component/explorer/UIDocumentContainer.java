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
package org.exoplatform.ecm.webui.component.explorer;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.optionblocks.UIOptionBlockPanel;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import javax.jcr.Node;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 15, 2007 10:05:43 AM
 */
@ComponentConfig(
                  template = "app:/groovy/webui/component/explorer/UIDocumentTabPane.gtmpl",
      events = {
                @EventConfig(listeners = UIDocumentContainer.ChangeTabActionListener.class)
              }
)
public class UIDocumentContainer extends UIContainer {

  private String OPTION_BLOCK_EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIOptionBlockPanel";
  private List<UIComponent> listExtenstion = new ArrayList<UIComponent>();
  private boolean isDisplayOptionPanel = false;

  public UIDocumentContainer() throws Exception {
    addChild(UIDocumentWithTree.class, null, null) ;
    addChild(UIDocumentInfo.class, null, null) ;
    this.initOptionBlockPanel();
  }

  public boolean isShowViewFile() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).isShowViewFile() ;
  }

  public boolean isJcrEnable() {
    return getAncestorOfType(UIJCRExplorer.class).getPreference().isJcrEnable() ;
  }

  public boolean isDocumentNode() {
    try {
      Node currentNode = this.getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
      TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      List<String> documentNodeTypes = templateService.getAllDocumentNodeTypes();
      if(documentNodeTypes.contains(currentNode.getPrimaryNodeType().getName())) return true;
      return false;
    } catch(Exception ex) {
      return false;
    }
  }

  public static class ChangeTabActionListener  extends EventListener<UIDocumentContainer> {
    public void execute(Event<UIDocumentContainer> event) throws Exception {
      UIDocumentContainer uiDocumentContainer = event.getSource();
      String selectedTabName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDocumentWithTree uiDocTree = uiDocumentContainer.getChild(UIDocumentWithTree.class);
      uiDocTree.setRendered(uiDocTree.getId().equals(selectedTabName));

      UIDocumentInfo uiDocInfo = uiDocumentContainer.getChildById(UIDocumentInfo.class.getSimpleName());
      uiDocInfo.setRendered(uiDocInfo.getId().equals(selectedTabName));

      UIJCRExplorer uiExplorer = uiDocumentContainer.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.setShowDocumentViewForFile(uiDocInfo.getId().equals(selectedTabName));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentContainer);
      uiExplorer.updateAjax(event);
    }
  }
  /*
  *
  * This method get Option Block Panel extenstion and add it into this
  *
  * */
   public void addOptionBlockPanel() throws Exception {

     UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(OPTION_BLOCK_EXTENSION_TYPE);

      for (UIExtension extension : extensions) {
        UIComponent uicomp = manager.addUIExtension(extension, null, this);
        uicomp.setRendered(false);
        listExtenstion.add(uicomp);
      }
   }
   /*
    * This method checks and returns true if the Option Block Panel is configured to display, else it returns false
    * */
   public boolean isHasOptionBlockPanel() {
     UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(OPTION_BLOCK_EXTENSION_TYPE);
      if(extensions != null) {
        return true;
      }
     return false;
   }

   public void setDisplayOptionBlockPanel(boolean display) {
     for(UIComponent uicomp : listExtenstion) {
       uicomp.setRendered(display);
     }
     isDisplayOptionPanel = display;
   }

   public boolean isDisplayOptionBlockPanel() {
     return isDisplayOptionPanel;
   }

   public void initOptionBlockPanel() throws Exception {
     if(isHasOptionBlockPanel()) {
       addOptionBlockPanel();
       UIOptionBlockPanel optionBlockPanel = this.getChild(UIOptionBlockPanel.class);

       if(optionBlockPanel.isHasOptionBlockExtension()) {
         optionBlockPanel.addOptionBlockExtension();
         setDisplayOptionBlockPanel(true);
       }
     }
   }
}
