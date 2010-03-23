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
package org.exoplatform.ecm.webui.component.admin.templates;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM 
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig(listeners = UIDialogTab.EditActionListener.class),
      @EventConfig(listeners = UIDialogTab.DeleteActionListener.class, confirm = "UIDialogTab.msg.confirm-delete")
    }
)

public class UIDialogTab extends UIContainer {

  final private static String[] BEAN_FIELD = {"name", "roles", "baseVersion"} ;
  final private static String[] ACTIONS = {"Edit", "Delete"} ;
  final public static String DIALOG_LIST_NAME = "DialogList" ;
  final public static String DIALOG_FORM_NAME = "DialogForm" ;
  
  private List<String> listDialog_ = new ArrayList<String>() ;

  public UIDialogTab() throws Exception {
    UIGrid uiGrid = addChild(UIGrid.class, null, DIALOG_LIST_NAME) ;
    uiGrid.getUIPageIterator().setId("DialogListIterator") ;
    uiGrid.configure("name", BEAN_FIELD, ACTIONS) ;
    UITemplateContent uiForm = addChild(UITemplateContent.class, null , DIALOG_FORM_NAME) ;
    uiForm.setTemplateType(TemplateService.DIALOGS);
    uiForm.update(null) ;
  }

  public List<String> getListDialog() { return listDialog_ ; }
  
  public void updateGrid(String nodeName, String repository) throws Exception {
    TemplateService tempService = getApplicationComponent(TemplateService.class) ;
    NodeIterator iter = tempService.getAllTemplatesOfNodeType(true, nodeName, repository, 
        SessionProviderFactory.createSystemProvider()) ;
    List<DialogData> data = new ArrayList<DialogData>() ;
    DialogData item  ;
    while (iter.hasNext()){
      Node node = (Node) iter.next() ;
      String version = "" ;
      StringBuilder rule = new StringBuilder() ;
      Value[] rules = node.getProperty("exo:roles").getValues() ;
      for(int i = 0; i < rules.length; i++) {
        rule.append("["+rules[i].getString()+"]") ;
      }
      if(node.isNodeType(Utils.MIX_VERSIONABLE) && !node.isNodeType(Utils.NT_FROZEN)){
        version = node.getBaseVersion().getName() ;
      }
      listDialog_.add(node.getName()) ;
      item = new DialogData(node.getName(), rule.toString(), version) ;
      data.add(item) ;
    }
    UIGrid uiGrid = getChild(UIGrid.class) ;
    ObjectPageList objDPageList = new ObjectPageList(data, 4) ;
    uiGrid.getUIPageIterator().setPageList(objDPageList) ;  
  }

  public void setTabRendered() {
    UIViewTemplate uiViewTemplate = getAncestorOfType(UIViewTemplate.class) ;
    uiViewTemplate.setRenderedChild(UIDialogTab.class) ;
  }

  static public class EditActionListener extends EventListener<UIDialogTab> {
    public void execute(Event<UIDialogTab> event) throws Exception {
      UIDialogTab dialogTab = event.getSource() ;
      String dialogName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITemplateContent uiForm = dialogTab.getChild(UITemplateContent.class) ;
      uiForm.update(dialogName) ;
      dialogTab.setTabRendered() ;
      UITemplatesManager uiManager = dialogTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class DeleteActionListener extends EventListener<UIDialogTab> {
    public void execute(Event<UIDialogTab> event) throws Exception {
      UIDialogTab dialogTab = event.getSource() ;
      UIViewTemplate uiViewTemplate = event.getSource().getAncestorOfType(UIViewTemplate.class) ;
      String nodeTypeName = uiViewTemplate.getNodeTypeName() ;
      String templateName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      TemplateService templateService = dialogTab.getApplicationComponent(TemplateService.class) ;
      UITemplateContent uiForm = dialogTab.findFirstComponentOfType(UITemplateContent.class) ;
      for(String template : TemplateService.UNDELETABLE_TEMPLATES) {        
        if(template.equals(templateName)) {
          UIApplication app = dialogTab.getAncestorOfType(UIApplication.class) ;
          Object[] args = {template} ;
          app.addMessage(new ApplicationMessage("UIDialogTab.msg.undeletable", args)) ;
          dialogTab.setTabRendered() ;
          return ;
        }
      }
      PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
      PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
      String repository = portletPref.getValue(Utils.REPOSITORY, "") ;
      templateService.removeTemplate(TemplateService.DIALOGS, nodeTypeName, templateName, repository) ;
      uiForm.update(null);
      uiForm.reset();      
      
      dialogTab.updateGrid(nodeTypeName, repository) ;
      dialogTab.setTabRendered() ;
      UITemplatesManager uiManager = dialogTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  public static class DialogData {
    private String name ;
    private String roles ;
    private String baseVersion ;

    public DialogData(String name, String roles, String version) {
      this.name = name ;
      this.roles = roles ;
      baseVersion = version ;
    }

    public String getName(){return name ;}
    public String getRoles(){return roles ;}
    public String getBaseVersion(){return baseVersion ;}
  }
}