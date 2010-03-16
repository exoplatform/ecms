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
package org.exoplatform.ecm.webui.component.fastcontentcreator;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 25, 2007 9:10:53 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIEditModeConfiguration.SaveActionListener.class),
      @EventConfig(listeners = UIEditModeConfiguration.SelectPathActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIEditModeConfiguration.ChangeWorkspaceActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIEditModeConfiguration.ChangeRepositoryActionListener.class, phase=Phase.DECODE)
    }
)
public class UIEditModeConfiguration extends UIForm implements UISelectable {

  final static public String FIELD_SELECT = "selectTemplate" ;
  final static public String FIELD_SAVEDPATH = "savedPath" ;
  final static public String ACTION_INPUT = "actionInput" ;
  final static public String WORKSPACE_NAME = "workspaceName" ;
  final static public String REPOSITORY_NAME = "repositoryName" ;
  final static public String DEFAULT_REPOSITORY = "repository" ;
  private static final Log LOG  = ExoLogger.getLogger("fastcontentcreator.UIEditModeConfiguration");
  public UIEditModeConfiguration() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox uiRepositoryList = new UIFormSelectBox(REPOSITORY_NAME, REPOSITORY_NAME, options) ; 
    uiRepositoryList.setOnChange("ChangeRepository") ;
    addUIFormInput(uiRepositoryList) ;
    UIFormSelectBox uiWorkspaceList = new UIFormSelectBox(WORKSPACE_NAME, WORKSPACE_NAME, options) ; 
    uiWorkspaceList.setOnChange("ChangeWorkspace") ;
    addUIFormInput(uiWorkspaceList) ;
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction(ACTION_INPUT) ;
    uiInputAct.addUIFormInput(new UIFormStringInput(FIELD_SAVEDPATH, FIELD_SAVEDPATH, null).setEditable(false)) ;
    uiInputAct.setActionInfo(FIELD_SAVEDPATH, new String[] {"SelectPath"}) ;
    addUIComponentInput(uiInputAct) ;
    addUIFormInput(new UIFormSelectBox(FIELD_SELECT, FIELD_SELECT, options)) ;
    setActions(new String[] {"Save"}) ;
  }
  
  public void initEditMode() throws Exception {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance() ;
    PortletRequest request = context.getRequest() ; 
    PortletPreferences preferences = request.getPreferences() ;
    String repoName = preferences.getValue(Utils.REPOSITORY, "") ;
    boolean isDefaultWs = false ;
    RepositoryService rService = getApplicationComponent(RepositoryService.class) ;  
    List<SelectItemOption<String>> repositories= new ArrayList<SelectItemOption<String>>() ;
    for(RepositoryEntry re : rService.getConfig().getRepositoryConfigurations()) {
      repositories.add(new SelectItemOption<String>(re.getName(), re.getName())) ;
    }
    UIFormSelectBox uiRepositoryList = getUIFormSelectBox(REPOSITORY_NAME) ;
    uiRepositoryList.setOptions(repositories) ;
    uiRepositoryList.setValue(repoName) ;
    try {
      ManageableRepository manaRepoService = 
        getApplicationComponent(RepositoryService.class).getRepository(repoName) ;
      String[] wsNames = manaRepoService.getWorkspaceNames();
      String systemWsName = manaRepoService.getConfiguration().getSystemWorkspaceName() ;
      List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>() ;
      String prefWs = preferences.getValue("workspace", "") ;
      setTemplateOptions(preferences.getValue("path", ""), repoName, prefWs) ;
      for(String wsName : wsNames) {
        if(!wsName.equals(systemWsName)) {
          if(wsName.equals(prefWs)) isDefaultWs = true ;
          workspace.add(new SelectItemOption<String>(wsName,  wsName)) ;
        }
      }
      UIFormSelectBox uiWorkspaceList = getUIFormSelectBox(WORKSPACE_NAME) ; 
      uiWorkspaceList.setOptions(workspace) ;
      if(isDefaultWs) {
        uiWorkspaceList.setValue(prefWs);
      } else if(workspace.size() > 0) {
        uiWorkspaceList.setValue(workspace.get(0).getValue());
      }
      getUIStringInput(FIELD_SAVEDPATH).setValue(preferences.getValue("path", "")) ;
    } catch(RepositoryException repo) {
      ResourceBundle res = context.getApplicationResourceBundle() ;
      String label = res.getString(getId() + ".label.select-repository") ;
      repositories.add(new SelectItemOption<String>(label, "")) ;
      uiRepositoryList.setValue("") ;
    }
  }
  
  private void setTemplateOptions(String nodePath, String repoName, String wsName) throws Exception {
    try {
      Session session = getApplicationComponent(RepositoryService.class).getRepository(repoName).getSystemSession(wsName) ;
      Node currentNode = null ;
      UIFormSelectBox uiSelectTemplate = getUIFormSelectBox(FIELD_SELECT) ;
      List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
      boolean hasDefaultDoc = false ;
      PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance() ;
      PortletRequest request = context.getRequest() ; 
      PortletPreferences preferences = request.getPreferences() ;
      String defaultValue = preferences.getValue("type", "") ;
      try {
        currentNode = (Node)session.getItem(nodePath) ;      
      } catch(PathNotFoundException ex) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIEditModeConfiguration.msg.item-not-found", null, 
            ApplicationMessage.WARNING)) ;
        session.logout();
        return ;
      }
      NodeTypeManager ntManager = currentNode.getSession().getWorkspace().getNodeTypeManager() ; 
      NodeType currentNodeType = currentNode.getPrimaryNodeType() ; 
      NodeDefinition[] childDefs = currentNodeType.getChildNodeDefinitions() ;
      TemplateService templateService = getApplicationComponent(TemplateService.class) ;
      List templates = templateService.getDocumentTemplates(repoName) ;
      List<String> labels = new ArrayList<String>() ;
      try {
        for(int i = 0; i < templates.size(); i ++){
          String nodeTypeName = templates.get(i).toString() ; 
          NodeType nodeType = ntManager.getNodeType(nodeTypeName) ;
          NodeType[] superTypes = nodeType.getSupertypes() ;
          boolean isCanCreateDocument = false ;
          for(NodeDefinition childDef : childDefs){
            NodeType[] requiredChilds = childDef.getRequiredPrimaryTypes() ;
            for(NodeType requiredChild : requiredChilds) {          
              if(nodeTypeName.equals(requiredChild.getName())){            
                isCanCreateDocument = true ;
                break ;
              }            
            }
            if(nodeTypeName.equals(childDef.getName()) || isCanCreateDocument) {
              if(!hasDefaultDoc && nodeTypeName.equals(defaultValue)) hasDefaultDoc = true ;
              String label = templateService.getTemplateLabel(nodeTypeName, repoName) ;
              if(!labels.contains(label)) {
                options.add(new SelectItemOption<String>(label, nodeTypeName));          
              }
              labels.add(label) ;
              isCanCreateDocument = true ;          
            }
          }      
          if(!isCanCreateDocument){
            for(NodeType superType:superTypes) {
              for(NodeDefinition childDef : childDefs){          
                for(NodeType requiredType : childDef.getRequiredPrimaryTypes()) {              
                  if (superType.getName().equals(requiredType.getName())) {
                    if(!hasDefaultDoc && nodeTypeName.equals(defaultValue)) {
                      hasDefaultDoc = true ;
                    }
                    String label = templateService.getTemplateLabel(nodeTypeName, repoName) ;
                    if(!labels.contains(label)) {
                      options.add(new SelectItemOption<String>(label, nodeTypeName));                
                    }
                    labels.add(label) ;
                    isCanCreateDocument = true ;
                    break;
                  }
                }
                if(isCanCreateDocument) break ;
              }
              if(isCanCreateDocument) break ;
            }
          }            
        }
        uiSelectTemplate.setOptions(options) ;
        if(hasDefaultDoc) {
          uiSelectTemplate.setValue(defaultValue);
        } else if(options.size() > 0) {
          defaultValue = options.get(0).getValue() ;
          uiSelectTemplate.setValue(defaultValue);
        } 
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
      }
      session.logout();
    } catch(Exception ex) {
      LOG.error("Unexpected error", ex);
    }
  }
  
  public void doSelect(String selectField, Object value) {
    getUIStringInput(selectField).setValue(value.toString()) ;
    String repoName = getUIFormSelectBox(REPOSITORY_NAME).getValue() ;
    String wsName = getUIFormSelectBox(WORKSPACE_NAME).getValue() ;
    try {
      setTemplateOptions(value.toString(), repoName, wsName) ;
    } catch(Exception ex) {
      LOG.error("Unexpected error", ex);
    }
    UIFastContentCreatorPortlet uiDialog = getParent() ;
    UIPopupWindow uiPopup = uiDialog.getChild(UIPopupWindow.class) ;
    uiPopup.setRendered(false) ;
    uiPopup.setShow(false) ;
  }
  
  static public class SelectPathActionListener extends EventListener<UIEditModeConfiguration> {
    public void execute(Event<UIEditModeConfiguration> event) throws Exception {
      UIEditModeConfiguration uiTypeForm = event.getSource() ;
      UIFastContentCreatorPortlet uiDialog = uiTypeForm.getParent() ;
      String repositoryName = uiTypeForm.getUIFormSelectBox(REPOSITORY_NAME).getValue() ;
      String wsName = uiTypeForm.getUIFormSelectBox(WORKSPACE_NAME).getValue() ;
      uiDialog.initPopupJCRBrowser(repositoryName, wsName) ;
    }
  }
  
  static public class ChangeWorkspaceActionListener extends EventListener<UIEditModeConfiguration> {
    public void execute(Event<UIEditModeConfiguration> event) throws Exception {
      UIEditModeConfiguration uiTypeForm = event.getSource() ;
      uiTypeForm.getUIStringInput(FIELD_SAVEDPATH).setValue("/") ;
      String repoName = uiTypeForm.getUIFormSelectBox(REPOSITORY_NAME).getValue() ;
      String wsName = uiTypeForm.getUIFormSelectBox(WORKSPACE_NAME).getValue() ;
      uiTypeForm.setTemplateOptions(uiTypeForm.getUIStringInput(FIELD_SAVEDPATH).getValue(), repoName, wsName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTypeForm) ;
    }
  }
  
  static public class ChangeRepositoryActionListener extends EventListener<UIEditModeConfiguration> {
    public void execute(Event<UIEditModeConfiguration> event) throws Exception {
      UIEditModeConfiguration uiTypeForm = event.getSource() ;
      RepositoryService repositoryService = uiTypeForm.getApplicationComponent(RepositoryService.class) ;
      uiTypeForm.getUIStringInput(FIELD_SAVEDPATH).setValue("/") ;
      String repoName = uiTypeForm.getUIFormSelectBox(REPOSITORY_NAME).getValue() ;
      String[] wsNames = repositoryService.getRepository(repoName).getWorkspaceNames();
      String systemWsName = repositoryService.getRepository(repoName).getConfiguration().getSystemWorkspaceName() ;
      List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>() ;
      for(String ws : wsNames) {
        if(!ws.equals(systemWsName)) workspace.add(new SelectItemOption<String>(ws, ws)) ;
      }
      if(workspace.size() > 0) {
        uiTypeForm.setTemplateOptions("/", repoName, workspace.get(0).getLabel()) ;
      }
      uiTypeForm.getUIFormSelectBox(WORKSPACE_NAME).setOptions(workspace) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTypeForm) ;
    }
  }
  
  static public class SaveActionListener extends EventListener<UIEditModeConfiguration> {
    public void execute(Event<UIEditModeConfiguration> event) throws Exception {
      UIEditModeConfiguration uiEditModeConfiguration = event.getSource() ;
      UIApplication uiApp = uiEditModeConfiguration.getAncestorOfType(UIApplication.class) ;
      PortletRequestContext context = (PortletRequestContext) event.getRequestContext() ;
      PortletRequest request = context.getRequest() ; 
      PortletPreferences preferences = request.getPreferences() ;
      String fileType = uiEditModeConfiguration.getUIFormSelectBox(FIELD_SELECT).getValue() ;
      String location = uiEditModeConfiguration.getUIStringInput(FIELD_SAVEDPATH).getValue() ;
      String wsName = uiEditModeConfiguration.getUIFormSelectBox(WORKSPACE_NAME).getValue() ;
      if(wsName == null || wsName.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIEditModeConfiguration.msg.ws-empty", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;        
      }
      String repoName = uiEditModeConfiguration.getUIFormSelectBox(REPOSITORY_NAME).getValue() ;
      if(fileType == null || fileType.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIEditModeConfiguration.msg.fileType-empty", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      preferences.setValue("workspace", wsName) ;
      preferences.setValue("path", location) ;
      preferences.setValue("type", fileType) ;
      preferences.setValue("repository", repoName) ;
      preferences.store() ;
      uiApp.addMessage(new ApplicationMessage("UIEditModeConfiguration.msg.save-successfully", null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
    }
  }
}
