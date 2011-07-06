/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 18, 2009
 * 10:37:42 AM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig(listeners = UISkinTab.EditActionListener.class),
      @EventConfig(listeners = UISkinTab.DeleteActionListener.class, confirm = "UIDialogTab.msg.confirm-delete")
    }
)
public class UISkinTab extends UIContainer {

  final private static String[] BEAN_FIELD = {"name", "roles", "baseVersion"} ;
  final private static String[] ACTIONS = {"Edit", "Delete"} ;
  final public static String SKIN_LIST_NAME = "SkinList" ;
  final public static String SKIN_FORM_NAME = "SkinForm" ;

  private List<String> listSkin_ = new ArrayList<String>() ;

  public UISkinTab() throws Exception {
    UIGrid uiGrid = addChild(UIGrid.class, null, SKIN_LIST_NAME) ;
    uiGrid.getUIPageIterator().setId("SkinListIterator") ;
    uiGrid.configure("name", BEAN_FIELD, ACTIONS) ;
    UITemplateContent uiForm = addChild(UITemplateContent.class, null , SKIN_FORM_NAME) ;
    uiForm.setTemplateType(TemplateService.SKINS);
    uiForm.update(null);
  }

  public void setTabRendered() {
    UIViewTemplate uiViewTemplate = getAncestorOfType(UIViewTemplate.class) ;
    uiViewTemplate.setRenderedChild(UISkinTab.class) ;
  }

  public List<String> getListSkin() { return listSkin_ ; }

  @Deprecated
  public void updateGrid(String nodeName, String repository) throws Exception {
    updateGrid(nodeName);
  }
  
  public void updateGrid(String nodeName) throws Exception {
    TemplateService tempService = getApplicationComponent(TemplateService.class) ;
    Node templateHome = tempService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider()).getNode(nodeName);
    if(!templateHome.hasNode(TemplateService.SKINS)) return;
    NodeIterator iter = templateHome.getNode(TemplateService.SKINS).getNodes();
    List<SkinData> data = new ArrayList<SkinData>() ;
    SkinData item  ;
    Node node = null;
    while (iter.hasNext()){
      node = (Node) iter.next() ;
      String version = "" ;
      StringBuilder rule = new StringBuilder() ;
      Value[] rules = node.getNode(Utils.JCR_CONTENT).getProperty(Utils.EXO_ROLES).getValues() ;
      for(int i = 0; i < rules.length; i++) {
        rule.append("["+rules[i].getString()+"]") ;
      }
      if(node.isNodeType(Utils.MIX_VERSIONABLE) && !node.isNodeType(Utils.NT_FROZEN)){
        version = node.getBaseVersion().getName() ;
      }
      listSkin_.add(node.getName()) ;
      item = new SkinData(node.getName(), rule.toString(), version) ;
      data.add(item) ;
    }
    UIGrid uiGrid = getChild(UIGrid.class) ;
    ListAccess<SkinData> skinDataList = new ListAccessImpl<SkinData>(SkinData.class, data);
    LazyPageList<SkinData> dataPageList = new LazyPageList<SkinData>(skinDataList, 4);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
  }  

  static public class EditActionListener extends EventListener<UISkinTab> {
    public void execute(Event<UISkinTab> event) throws Exception {
      UISkinTab skinTab = event.getSource() ;
      String skinName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITemplateContent uiForm = skinTab.getChild(UITemplateContent.class) ;
      uiForm.update(skinName) ;
      skinTab.setTabRendered() ;
      UITemplatesManager uiManager = skinTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class DeleteActionListener extends EventListener<UISkinTab> {
    public void execute(Event<UISkinTab> event) throws Exception {
      UISkinTab skinTab = event.getSource() ;
      UIViewTemplate uiViewTemplate = event.getSource().getAncestorOfType(UIViewTemplate.class) ;
      String nodeTypeName = uiViewTemplate.getNodeTypeName() ;
      String templateName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      TemplateService templateService = skinTab.getApplicationComponent(TemplateService.class) ;
      UITemplateContent uiForm = skinTab.findFirstComponentOfType(UITemplateContent.class) ;
      for(String template : TemplateService.UNDELETABLE_TEMPLATES) {
        if(template.equals(templateName)) {
          UIApplication app = skinTab.getAncestorOfType(UIApplication.class) ;
          Object[] args = {template} ;
          app.addMessage(new ApplicationMessage("UIDialogTab.msg.undeletable", args)) ;
          skinTab.setTabRendered() ;
          return ;
        }
      }
      templateService.removeTemplate(TemplateService.SKINS, nodeTypeName, templateName) ;
      uiForm.update(null);
      uiForm.reset();

      skinTab.updateGrid(nodeTypeName) ;
      skinTab.setTabRendered() ;
      UITemplatesManager uiManager = skinTab.getAncestorOfType(UITemplatesManager.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  public static class SkinData {
    private String name ;
    private String roles ;
    private String baseVersion ;

    public SkinData(String name, String roles, String version) {
      this.name = name ;
      this.roles = roles ;
      baseVersion = version ;
    }

    public String getName(){return name ;}
    public String getRoles(){return roles ;}
    public String getBaseVersion(){return baseVersion ;}
  }

}
