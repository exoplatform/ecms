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

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 25, 2007
 * 2:05:40 PM
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = UIViewMetadataTemplate.EditPropertyActionListener.class),
      @EventConfig(listeners = UIViewMetadataTemplate.CancelActionListener.class)
    }
)
public class UIViewMetadataTemplate extends UIContainer {

  private String documentType_ ;
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIViewMetadataTemplate");
  public UIViewMetadataTemplate() throws Exception {
  }

  public void setTemplateType(String type) {documentType_ = type ;}

  public String getViewTemplatePath() {
    MetadataService metadataService = getApplicationComponent(MetadataService.class) ;
    try {
      return metadataService.getMetadataPath(documentType_, false) ;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return null ;
  }

  public String getTemplate() { return getViewTemplatePath() ; }

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    //return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
    getAncestorOfType(UIJCRExplorer.class).newJCRTemplateResourceResolver() ;
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public Node getViewNode(String nodeType) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getViewNode(nodeType) ;
  }

  public List<String> getMultiValues(Node node, String name) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getMultiValues(node, name) ;
  }

  static public class EditPropertyActionListener extends EventListener<UIViewMetadataTemplate> {
    public void execute(Event<UIViewMetadataTemplate> event) throws Exception {
      UIViewMetadataTemplate uiViewTemplate = event.getSource() ;
      String nodeType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIViewMetadataManager uiMetaManager = uiViewTemplate.getAncestorOfType(UIViewMetadataManager.class) ;
      UIJCRExplorer uiExplorer = uiViewTemplate.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiViewTemplate.getAncestorOfType(UIApplication.class);
      Node currentNode = uiExplorer.getCurrentNode();
      if (!PermissionUtil.canSetProperty(currentNode)) {
        throw new MessageException(new ApplicationMessage("UIViewMetadataTemplate.msg.access-denied",
                                                          null, ApplicationMessage.WARNING)) ;
      }
      if (!currentNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null));
        
        return;
      }
      uiMetaManager.initMetadataFormPopup(nodeType) ;
      UIViewMetadataContainer uiContainer = uiViewTemplate.getParent() ;
      uiContainer.setRenderedChild(nodeType) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer.getParent()) ;
    }
  }

  static public class CancelActionListener extends EventListener<UIViewMetadataTemplate> {
    public void execute(Event<UIViewMetadataTemplate> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }
}
