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

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 7, 2006
 * 14:42:15 AM
 */

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  type     = UIResourceForm.class,
  template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
  events = {
    @EventConfig(listeners = UIResourceForm.SaveActionListener.class),
    @EventConfig(listeners = UIResourceForm.BackActionListener.class)
  }
)

public class UIResourceForm extends UIForm {

  final static public String FIElD_NAME = "name" ;
  final static public String FIElD_TEXTTOMODIFY = "textToModify" ;
  final static public String FIElD_FILETOUPLOAD = "fileToUpload" ;

  private NodeLocation contentNode_ ;
  private boolean isText_;
  private Session session_ ;

  public UIResourceForm() throws Exception {
    setMultiPart(true) ;
    addUIFormInput(new UIFormStringInput(FIElD_NAME, FIElD_NAME, null)) ;
  }

  public void setContentNode(Node node, Session session) throws RepositoryException {
    session_ = session ;
    contentNode_ = NodeLocation.getNodeLocationByNode(node);
    isText_ = node.getProperty("jcr:mimeType").getString().startsWith("text");
    String name = node.getParent().getName() ;
    getUIStringInput(FIElD_NAME).setValue(name) ;
    if(isText_) {
      String contentText = node.getProperty("jcr:data").getString() ;
      addUIFormInput(new UIFormTextAreaInput(FIElD_TEXTTOMODIFY, FIElD_TEXTTOMODIFY, contentText)) ;
    }else {
      getUIStringInput(FIElD_NAME).setEditable(false);
      UIFormUploadInput uiInput = new UIFormUploadInput(FIElD_FILETOUPLOAD, FIElD_FILETOUPLOAD);
      uiInput.setAutoUpload(true);
      addUIFormInput(uiInput) ;
    }
  }

  public boolean isText() throws RepositoryException {
    return isText_;
  }

  static  public class SaveActionListener extends EventListener<UIResourceForm> {
    public void execute(Event<UIResourceForm> event) throws Exception {
      UIResourceForm uiResourceForm = event.getSource() ;
      Node contentNode = NodeLocation.getNodeByLocation(uiResourceForm.contentNode_);
      Property prop = contentNode.getProperty("jcr:mimeType") ;
      UIJCRExplorer uiJCRExplorer = uiResourceForm.getAncestorOfType(UIJCRExplorer.class) ;
      if(prop.getString().startsWith("text")) {
        String text = uiResourceForm.getUIFormTextAreaInput(FIElD_TEXTTOMODIFY).getValue() ;
        contentNode.setProperty("jcr:data", text) ;
      }else {
        UIFormUploadInput  fileUpload =
          (UIFormUploadInput)uiResourceForm.getUIInput(FIElD_FILETOUPLOAD) ;
        InputStream content =  fileUpload.getUploadDataAsStream() ;
        contentNode.setProperty("jcr:data", content) ;
      }
      if(uiResourceForm.session_ != null) uiResourceForm.session_.save() ;
      else uiJCRExplorer.getSession().save() ;
      uiResourceForm.setRenderSibling(UIDocumentInfo.class);
    }
  }

  static  public class BackActionListener extends EventListener<UIResourceForm> {
    public void execute(Event<UIResourceForm> event) throws Exception {
      UIResourceForm uiResourceForm = event.getSource() ;
      uiResourceForm.setRenderSibling(UIDocumentInfo.class) ;
    }
  }
}

