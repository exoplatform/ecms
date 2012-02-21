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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormTableInputSet;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 29, 2006
 * 12:02:38 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UINodeTypeImport.ImportActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UINodeTypeImport.UploadActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UINodeTypeImport.CancelActionListener.class)
    }
)
public class UINodeTypeImport extends UIForm {

  private List<NodeTypeValue> nodeTypeList_ = new ArrayList<NodeTypeValue>() ;
  final static String TABLE_NAME =  "UINodeTypeImport";
  final static String [] TABLE_COLUMNS = {"label", "input"};
  private static final Log LOG  = ExoLogger.getLogger(UINodeTypeImport.class);

  private List<String> undefinedNodeType = new ArrayList<String>();

  private List<String> registeredNodeType = new ArrayList<String>();

  private List<String> undefinedNamespace = new ArrayList<String>();

  public List<String> getUndefinedNamespace() {
    return undefinedNamespace;
  }

  public void setUndefinedNamespace(List<String> undefinedNamespace) {
    this.undefinedNamespace = undefinedNamespace;
  }

  public List<String> getUndefinedNodeTypes() {
    return undefinedNodeType;
  }

  public void setUndefinedNodeType(List<String> undefinedNodeType) {
    this.undefinedNodeType = undefinedNodeType;
  }

  public UINodeTypeImport() throws Exception {
  }

  public void update(ArrayList nodeTypeList) throws Exception {
    UIFormTableInputSet uiTableInputSet = getChild(UIFormTableInputSet.class) ;
    if(uiTableInputSet == null ) {
      uiTableInputSet = createUIComponent(UIFormTableInputSet.class, null, null) ;
      addUIComponentInput(uiTableInputSet) ;
    } else {
      uiTableInputSet.getChildren().clear() ;
    }
    NodeTypeManager ntManager = getApplicationComponent(RepositoryService.class).
                                getCurrentRepository().getNodeTypeManager() ;
    UIFormInputSet uiInputSet ;
    uiTableInputSet.setName(TABLE_NAME);
    uiTableInputSet.setColumns(TABLE_COLUMNS);
    getUndefinedNodeTypes().clear();
    getUndefinedNamespace().clear();
    getRegisteredNodeType().clear();
    nodeTypeList_ = nodeTypeList ;
    UIFormInputInfo uiInfo;
    String nodeTypeName;
    NodeTypeValue nodeTypeValue;
    UIFormCheckBoxInput<String> checkbox;
    NodeType register = null;
    for(int i = 0 ; i < nodeTypeList_.size() ; i ++) {
      nodeTypeValue = (NodeTypeValue)nodeTypeList_.get(i) ;
      nodeTypeName = nodeTypeValue.getName() ;
      uiInputSet = new UIFormInputSet(nodeTypeName) ;
      uiInfo = new UIFormInputInfo("label", null, nodeTypeName);
      uiInputSet.addChild(uiInfo);
      checkbox = new UIFormCheckBoxInput<String>(nodeTypeName, nodeTypeName, "") ;
      try {
        register = ntManager.getNodeType(nodeTypeName) ;
        uiInputSet.addChild(checkbox);
        uiTableInputSet.addChild(uiInputSet);
        if(register != null) {
          getRegisteredNodeType().add(nodeTypeName);
          checkbox.setEnable(false);
        }
      } catch (NamespaceException e) {
        if (nodeTypeName != null && nodeTypeName.contains(":")) {
          getUndefinedNamespace().add(
              nodeTypeName.substring(0, nodeTypeName.indexOf(":")));
        }
      } catch (NoSuchNodeTypeException e) {
        getUndefinedNodeTypes().add(nodeTypeName);
        uiInputSet.addChild(checkbox);
        uiTableInputSet.addChild(uiInputSet);
        checkbox.setEnable(true);
      } catch (RepositoryException e) {
        if (NamespaceException.class.isInstance(e.getCause())) {
          if (nodeTypeName != null && nodeTypeName.contains(":")) {
            getUndefinedNamespace().add(
                nodeTypeName.substring(0, nodeTypeName.indexOf(":")));
          }
        } else {
          throw e;
        }
      }

    }
  }

  public String getLabel(String id) { return id ; }

  public void setRegisteredNodeType(List<String> registeredNodeType) {
    this.registeredNodeType = registeredNodeType;
  }

  public List<String> getRegisteredNodeType() {
    return registeredNodeType;
  }

  static public class CancelActionListener extends EventListener<UINodeTypeImport> {
    public void execute(Event<UINodeTypeImport> event) throws Exception {
      UINodeTypeImport uiImport = event.getSource() ;
      UINodeTypeImportPopup uiImportPopup = uiImport.getParent() ;
      uiImportPopup.setRenderedChild(UINodeTypeUpload.class) ;
      UIPopupWindow uiPopup = uiImportPopup.getParent() ;
      uiPopup.setRendered(false) ;
    }
  }

  static public class UploadActionListener extends EventListener<UINodeTypeImport> {
    public void execute(Event<UINodeTypeImport> event) throws Exception {
      UINodeTypeImport uiImport = event.getSource() ;
      UINodeTypeManager uiManager = uiImport.getAncestorOfType(UINodeTypeManager.class) ;
      UIPopupWindow uiPopup = uiManager.findComponentById(UINodeTypeManager.IMPORT_POPUP) ;
      UINodeTypeImportPopup uiImportPopup = uiImport.getParent() ;
      uiImportPopup.setRenderedChild(UINodeTypeUpload.class) ;
      uiPopup.setShow(true) ;
    }
  }

  static public class ImportActionListener extends EventListener<UINodeTypeImport> {
    public void execute(Event<UINodeTypeImport> event) throws Exception {
      UINodeTypeImport uiImport = event.getSource() ;
      RepositoryService repositoryService =
        uiImport.getApplicationComponent(RepositoryService.class) ;
      NodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager() ;
      UINodeTypeManager uiManager = uiImport.getAncestorOfType(UINodeTypeManager.class) ;
      UINodeTypeImportPopup uiImportPopup = uiImport.getParent() ;
      uiImportPopup.setRenderedChild(UINodeTypeUpload.class) ;
      UIApplication uiApp = uiImport.getAncestorOfType(UIApplication.class) ;
      ExtendedNodeTypeManager extManager = (ExtendedNodeTypeManager) ntManager ;
      int counter = 0 ;
      List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
      uiImport.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      for(int i = 0 ; i < uiImport.nodeTypeList_.size() ; i ++){
        NodeTypeValue nodeTypeValue = (NodeTypeValue)uiImport.nodeTypeList_.get(i) ;
        if(listCheckbox.get(i).isChecked()) {
          try {
            extManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.IGNORE_IF_EXISTS) ;
            counter += 1 ;
          } catch(RepositoryException re) {
            if (LOG.isErrorEnabled()) {
              LOG.error("Cannot register nodetype " + nodeTypeValue + " cause by: " + re.getMessage());
            }
          }
        }
      }
      if(counter > 0) {
        String[] count = {String.valueOf(counter)} ;
        UINodeTypeList uiNodeTypeList = uiManager.getChild(UINodeTypeList.class) ;
        uiNodeTypeList.refresh(uiNodeTypeList.getUIPageIterator().getCurrentPage());
        UIPopupWindow uiPopup = uiManager.findComponentById(UINodeTypeManager.IMPORT_POPUP) ;
        uiPopup.setRendered(false) ;
        uiApp.addMessage(new ApplicationMessage("UINodeTypeImport.msg.nodetype-registered", count)) ;        
        event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
        return ;
      }
      uiApp.addMessage(new ApplicationMessage("UINodeTypeImport.msg.no-nodetype-registered", null)) ;     
    }
  }
}
