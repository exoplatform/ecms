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
package org.exoplatform.ecm.webui.component.admin.nodetype;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import javax.jcr.RepositoryException;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 2, 2006
 * 9:39:51 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UINodeTypeUpload.UploadActionListener.class),
      @EventConfig(listeners = UINodeTypeUpload.CancelActionListener.class)
    }
)
public class UINodeTypeUpload extends UIForm {

  final static public String FIELD_UPLOAD = "upload" ;


  public UINodeTypeUpload() throws Exception {
    this.setMultiPart(true) ;
    UIFormUploadInput uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD);
    uiInput.setAutoUpload(true);
    addUIFormInput(uiInput) ;
  }

  @SuppressWarnings("unchecked")
  static public class UploadActionListener extends EventListener<UINodeTypeUpload> {
    public void execute(Event<UINodeTypeUpload> event) throws Exception {
      UINodeTypeUpload uiUploadForm = event.getSource() ;
      UINodeTypeManager uiManager = uiUploadForm.getAncestorOfType(UINodeTypeManager.class) ;
      UIPopupWindow uiPopup = uiManager.findComponentById(UINodeTypeManager.IMPORT_POPUP) ;
      UINodeTypeImportPopup uiImportPopup = uiManager.findComponentById("UINodeTypeImportPopup") ;
      UIApplication uiApp = uiUploadForm.getAncestorOfType(UIApplication.class) ;
      UIFormUploadInput input = uiUploadForm.getUIInput(FIELD_UPLOAD) ;
      if(input.getUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.filename-error", null)) ;        
        return ;
      }
      String fileName = input.getUploadResource().getFileName();
      if(fileName == null || fileName.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.filename-error", null)) ;        
        return ;
      }
      DMSMimeTypeResolver resolver = DMSMimeTypeResolver.getInstance();
      String mimeType = resolver.getMimeType(fileName);
      InputStream is = null;
      UINodeTypeImport uiNodeTypeImport = uiImportPopup.getChild(UINodeTypeImport.class);
      try {
        if(mimeType.trim().equals("text/xml")) {
          is = new BufferedInputStream(input.getUploadDataAsStream());
        }else if(mimeType.trim().equals("application/zip")) {
          ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(input.getUploadDataAsStream())) ;
          is = Utils.extractFirstEntryFromZipFile(zipInputStream);
        }else {
          uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.data-file-error", null)) ;          
          return;
        }
        IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
        IUnmarshallingContext uctx = factory.createUnmarshallingContext();
        NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList)uctx.unmarshalDocument(is, null);
        ArrayList ntvList = nodeTypeValuesList.getNodeTypeValuesList();
        uiNodeTypeImport.update(ntvList);
        if (uiNodeTypeImport.getRegisteredNodeType().size() > 0 || uiNodeTypeImport.getUndefinedNodeTypes().size() > 0) {
          Class[] childrenToRender = {UINodeTypeImport.class, UIPopupWindow.class} ;
          uiImportPopup.setRenderedChildrenOfTypes(childrenToRender) ;
          uiPopup.setShow(true);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
        }
        if (uiNodeTypeImport.getRegisteredNodeType().size() > 0) {
           uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.nodetype-exist",
               new Object[] {uiNodeTypeImport.getRegisteredNodeType().toString()}, ApplicationMessage.WARNING )) ;
        }
        if (uiNodeTypeImport.getUndefinedNamespace().size() > 0) {
           uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.namespace-invalid",
               new Object[] { uiNodeTypeImport.getUndefinedNamespace().toString() }, ApplicationMessage.WARNING));
         }
      } catch(JiBXException e) {
        uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.data-invalid", null, ApplicationMessage.ERROR )) ;
        return ;
      } catch(RepositoryException e) {
        uiApp.addMessage(new ApplicationMessage("UINodeTypeUpload.msg.data-invalid", null, ApplicationMessage.ERROR )) ;
        return ;
      } finally {
        UploadService uploadService = uiUploadForm.getApplicationComponent(UploadService.class);
        UIFormUploadInput uiUploadInput = uiUploadForm.getChild(UIFormUploadInput.class);
        uploadService.removeUploadResource(uiUploadInput.getUploadId());
        if (is != null) is.close();
      }
    }
  }

  static public class CancelActionListener extends EventListener<UINodeTypeUpload> {
    public void execute(Event<UINodeTypeUpload> event) throws Exception {
      UINodeTypeUpload uiUpload = event.getSource() ;
      UIPopupWindow uiPopup = uiUpload.getAncestorOfType(UIPopupWindow.class) ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
      UINodeTypeManager uiManager = uiUpload.getAncestorOfType(UINodeTypeManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
