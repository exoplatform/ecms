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
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Nov 8, 2006 10:16:18 AM 
 */

@ComponentConfig (lifecycle = UIContainerLifecycle.class)

public class UIDocumentFormController extends UIContainer implements UIPopupComponent {

  private String defaultDocument_ ;
  private static String DEFAULT_VALUE = "exo:article" ;
  private Node currentNode_ ;
  private String repository_ ;  

  public UIDocumentFormController() throws Exception {
    addChild(UISelectDocumentForm.class, null, null) ;
    UIDocumentForm uiDocumentForm = createUIComponent(UIDocumentForm.class, null, null) ;
    uiDocumentForm.setContentType(DEFAULT_VALUE);
    uiDocumentForm.addNew(true) ;    
    addChild(uiDocumentForm) ;
  }

  public void setCurrentNode(Node node) { currentNode_ = node ; }

  public void setRepository(String repository) { repository_ = repository ; }

  public void initPopup(UIComponent uiComp) throws Exception {
    removeChildById("PopupComponent") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "PopupComponent") ;
    uiPopup.setUIComponent(uiComp) ;
    uiPopup.setWindowSize(640, 300) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public List<SelectItemOption<String>> getListFileType() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();    
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    List<String> acceptableContentTypes = templateService.getCreationableContentTypes(currentNode_);
    if(acceptableContentTypes.size() == 0) return options;
    String userName = Util.getPortalRequestContext().getRemoteUser();
    for(String contentType: acceptableContentTypes) {
      String label = templateService.getTemplateLabel(contentType,repository_);
      try {
        String templatePath = templateService.getTemplatePathByUser(true, contentType, userName, repository_);
        if ((templatePath != null) && (templatePath.length() > 0)) {
          options.add(new SelectItemOption<String>(label, contentType));
        }
      } catch (AccessControlException e) {
      } catch (Exception e) {  
      }
    }    
    Collections.sort(options, new ItemOptionNameComparator()) ;
    if(options.size()>0) {
      defaultDocument_ = options.get(0).getValue();
      if (options.size() > 1) {
        UISelectDocumentForm uiSelectForm = getChild(UISelectDocumentForm.class) ;
        UIFormSelectBox uiSelectBox = uiSelectForm.getUIFormSelectBox(UISelectDocumentForm.FIELD_SELECT) ;
        uiSelectBox.setValue(defaultDocument_);
        uiSelectBox.setOptions(options);
      } else {
        this.removeChild(UISelectDocumentForm.class);
      }
      
    }        
    return options ;
  }

  public void init() throws Exception {
    getChild(UIDocumentForm.class).setRepositoryName(repository_) ;
    getChild(UIDocumentForm.class).setContentType(defaultDocument_);
    getChild(UIDocumentForm.class).setWorkspace(currentNode_.getSession().getWorkspace().getName()) ;
    getChild(UIDocumentForm.class).setStoredPath(currentNode_.getPath()) ;
    getChild(UIDocumentForm.class).resetProperties();
  }

  public void activate() throws Exception {
    UIDocumentForm uiDocumentForm = getChild(UIDocumentForm.class);
    uiDocumentForm.initFieldInput();
  }

  /**
   * Remove lock if node is locked for editing
   */
  public void deActivate() throws Exception {
    UIDocumentForm uiDocumentForm = getChild(UIDocumentForm.class);
    if (uiDocumentForm != null) {
      uiDocumentForm.releaseLock();
    }
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    UIPopupWindow uiPopup = getAncestorOfType(UIPopupWindow.class);
    if (uiPopup != null && !uiPopup.isShow()) {
      deActivate();
    }
    super.processRender(context);
  }

}
