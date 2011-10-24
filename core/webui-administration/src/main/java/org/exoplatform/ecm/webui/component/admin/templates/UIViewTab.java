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

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
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
      @EventConfig(listeners = UIViewTab.EditActionListener.class),
      @EventConfig(listeners = UIViewTab.DeleteActionListener.class, confirm = "UIViewTab.msg.confirm-delete")
    }
)

public class UIViewTab extends UIContainer {

  final private static String[] BEAN_FIELD = {"name", "roles", "baseVersion"} ;
  final private static String[] ACTIONS = {"Edit", "Delete"} ;
  final public static String VIEW_LIST_NAME = "VewList" ;
  final public static String VIEW_FORM_NAME = "ViewForm" ;

  private List<String> listView_ = new ArrayList<String>() ;

  public UIViewTab() throws Exception {
    UIGrid uiGrid = addChild(UIGrid.class, null, VIEW_LIST_NAME) ;
    uiGrid.getUIPageIterator().setId("ViewListIterator") ;
    uiGrid.configure("name", BEAN_FIELD, ACTIONS) ;
    UITemplateContent uiForm = addChild(UITemplateContent.class, null , VIEW_FORM_NAME) ;
    uiForm.setTemplateType(TemplateService.VIEWS);
    uiForm.update(null);
  }

  public List<String> getListView() { return listView_ ; }

  public void updateGrid(String nodeName) throws Exception {
    TemplateService tempService = getApplicationComponent(TemplateService.class) ;
    NodeIterator iter = tempService.getAllTemplatesOfNodeType(false, nodeName, WCMCoreUtils.getSystemSessionProvider()) ;
    List<ViewData> data = new ArrayList<ViewData>() ;
    ViewData item  ;
    if(iter == null) return;
    while(iter.hasNext()) {
      Node node = (Node) iter.next() ;
      String version = "" ;
      StringBuilder rule = new StringBuilder() ;
      Value[] rules = node.getNode(Utils.JCR_CONTENT).getProperty(Utils.EXO_ROLES).getValues() ;
      for(int i = 0; i < rules.length; i++) {
        rule.append("["+rules[i].getString()+"]") ;
      }
      if(node.isNodeType(Utils.MIX_VERSIONABLE) && !node.isNodeType(Utils.NT_FROZEN)) {
        version = node.getBaseVersion().getName();
      }
      listView_.add(node.getName()) ;
      item = new ViewData(node.getName(), rule.toString(), version) ;
      data.add(item);
    }
    UIGrid uiGrid = getChild(UIGrid.class) ;
    ListAccess<ViewData> viewDataList = new ListAccessImpl<ViewData>(ViewData.class, data);
    LazyPageList<ViewData> dataPageList = new LazyPageList<ViewData>(viewDataList, 4);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
  }

  public void setTabRendered() {
    UIViewTemplate uiViewTemplate = getAncestorOfType(UIViewTemplate.class) ;
    uiViewTemplate.setRenderedChild(UIViewTab.class) ;
  }

  static public class EditActionListener extends EventListener<UIViewTab> {
    public void execute(Event<UIViewTab> event) throws Exception {
      UIViewTab viewTab = event.getSource() ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITemplateContent uiForm = viewTab.getChild(UITemplateContent.class) ;
      uiForm.update(viewName) ;
      viewTab.setTabRendered() ;
      UITemplatesManager uiManager = viewTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class DeleteActionListener extends EventListener<UIViewTab> {
    public void execute(Event<UIViewTab> event) throws Exception {
      UIViewTab viewTab = event.getSource() ;
      UIViewTemplate uiViewTemplate = event.getSource().getAncestorOfType(UIViewTemplate.class) ;
      String nodeTypeName = uiViewTemplate.getNodeTypeName() ;
      String templateName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      TemplateService templateService = viewTab.getApplicationComponent(TemplateService.class) ;
      for(String template : TemplateService.UNDELETABLE_TEMPLATES) {
        if(template.equals(templateName)){
          UIApplication app = viewTab.getAncestorOfType(UIApplication.class) ;
          Object[] args = {template} ;
          app.addMessage(new ApplicationMessage("UIViewTab.msg.undeletable", args, ApplicationMessage.WARNING)) ;
          viewTab.setTabRendered() ;
          return ;
        }
      }
      templateService.removeTemplate(TemplateService.VIEWS, nodeTypeName, templateName) ;
      UITemplateContent uiForm = viewTab.findFirstComponentOfType(UITemplateContent.class) ;
      uiForm.update(null);
      uiForm.reset();
      viewTab.updateGrid(nodeTypeName) ;
      viewTab.setTabRendered() ;
      UITemplatesManager uiManager = viewTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  public static class ViewData {
    private String name ;
    private String roles ;
    private String baseVersion ;

    public ViewData(String name, String roles, String version) {
      this.name = name ;
      this.roles = roles ;
      baseVersion = version ;
    }
    public String getName(){return name ; }
    public String getRoles(){return roles ; }
    public String getBaseVersion(){return baseVersion ; }
  }
}
