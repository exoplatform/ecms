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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.exoplatform.ecm.webui.component.explorer.optionblocks.UIOptionBlockPanel;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Nov 8, 2006 10:16:18 AM
 */

@ComponentConfig(
    template  = "app:/groovy/webui/component/explorer/UIDocumentFormController.gtmpl"
)

public class UIDocumentFormController extends UIContainer implements UIPopupComponent {

  private static final Log LOG  = ExoLogger.getLogger(UIDocumentFormController.class.getName());

  private NodeLocation currentNode_ ;
  private String repository_ ;

  private String OPTION_BLOCK_EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIOptionBlockPanel";
  private List<UIComponent> listExtenstion = new ArrayList<UIComponent>();
  private boolean isDisplayOptionPanel = false;

  public UIDocumentFormController() throws Exception {
    addChild(UISelectDocumentForm.class, null, null);
    UIDocumentForm uiDocumentForm = createUIComponent(UIDocumentForm.class, null, null) ;
    uiDocumentForm.addNew(true);
    uiDocumentForm.setShowActionsOnTop(true);
    addChild(uiDocumentForm);
    uiDocumentForm.setRendered(false);
  }

  public void setCurrentNode(Node node) { 
    currentNode_ = NodeLocation.getNodeLocationByNode(node); 
  }  
  
  public void setRepository(String repository) {
    repository_ = repository;
  }

  public void initPopup(UIComponent uiComp) throws Exception {
    removeChildById("PopupComponent") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "PopupComponent") ;
    uiPopup.setShowMask(true);
    uiPopup.setUIComponent(uiComp) ;
    uiPopup.setWindowSize(640, 300) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public List<String> getListFileType() throws Exception {    
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    return templateService.getCreationableContentTypes(NodeLocation.getNodeByLocation(currentNode_));    
  }
  
  public void bindContentType() throws Exception {
    Comparator<String> ascComparator = new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        return s1.compareTo(s2) ;
      }      
    };
    Map<String, String> templates = new TreeMap <String, String>(ascComparator);
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    List<String> acceptableContentTypes = 
      templateService.getCreationableContentTypes(NodeLocation.getNodeByLocation(currentNode_));
    if(acceptableContentTypes.size() == 0) return;
    String userName = Util.getPortalRequestContext().getRemoteUser();
    for(String contentType: acceptableContentTypes) {
      try {
        String label = templateService.getTemplateLabel(contentType);
        String templatePath = templateService.getTemplatePathByUser(true, contentType, userName);
        if ((templatePath != null) && (templatePath.length() > 0)) {
          templates.put(label, contentType);
        }
      } catch (AccessControlException e) {
        if (LOG.isDebugEnabled()) {
          LOG.warn(userName + " do not have sufficient permission to access " + contentType + " template.");
        }
      } catch (PathNotFoundException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Node type template %s does not exist!", contentType);
        }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    if(templates.size()>0) {
      UISelectDocumentForm uiSelectForm = getChild(UISelectDocumentForm.class) ;
      if (templates.size() > 1) {        
        uiSelectForm.setDocumentTemplates(templates);
      } else {
        UIDocumentFormController uiDCFormController = uiSelectForm.getParent() ;            
        UIDocumentForm documentForm = uiDCFormController.getChild(UIDocumentForm.class) ;
        documentForm.addNew(true);      
        documentForm.getChildren().clear() ;
        documentForm.resetInterceptors();
        documentForm.resetProperties();            
        documentForm.setContentType(templates.values().iterator().next());
        documentForm.setCanChangeType(false);
        uiSelectForm.setRendered(false);
        documentForm.setRendered(true);
      }
    }
  }

  public void init() throws Exception {
    getChild(UIDocumentForm.class).setRepositoryName(repository_) ;
    getChild(UIDocumentForm.class).setWorkspace(currentNode_.getWorkspace()) ;
    getChild(UIDocumentForm.class).setStoredPath(currentNode_.getPath()) ;
    getChild(UIDocumentForm.class).resetProperties();
  }

  public void activate() {
  }

  /**
   * Remove lock if node is locked for editing
   */
  public void deActivate() {
    try {
      UIDocumentForm uiDocumentForm = getChild(UIDocumentForm.class);
      if (uiDocumentForm != null) {
        uiDocumentForm.releaseLock();
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends UIComponent> T setRendered(boolean rendered)
  {
     UIComponent res = super.setRendered(rendered);
     if (rendered == false) {
       try {
         deActivate();
       } catch (Exception ex) {
         if (LOG.isErrorEnabled()) {
           LOG.error("Unknown err:", ex);
         }
       }
     }
     return (T)res;
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    UIPopupWindow uiPopup = getAncestorOfType(UIPopupWindow.class);
    if (uiPopup != null && !uiPopup.isShow()) {
      uiPopup.setShowMask(true);
      deActivate();
    }
    super.processRender(context);
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

  public String getClosingConfirmMsg(String key) {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    return res.getString(key);
  }
}
