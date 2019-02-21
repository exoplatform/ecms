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
package org.exoplatform.ecm.webui.component.admin.script;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 27, 2006
 * 10:37:15 AM
 */
@ComponentConfig(
                 template = "app:/groovy/webui/component/admin/script/UIScriptList.gtmpl",
                 events = {
                     @EventConfig(listeners = UIScriptList.EditActionListener.class),
                     @EventConfig(listeners = UIScriptList.DeleteActionListener.class, confirm="UIScriptList.msg.confirm-delete"),
                     @EventConfig(listeners = UIScriptList.AddNewActionListener.class)
                 }
    )

public class UIScriptList extends UIComponentDecorator {

  private UIPageIterator uiPageIterator_;
  public static final String ACTION_SCRIPT_TYPE = "action";
  public static final String INTERCEPTOR_SCRIPT_TYPE = "interceptor";
  public static final String WIDGET_SCRIPT_TYPE = "widget";
  
  private static final String EDITED_CONFIGURED_SCRIPTS = "EditedConfiguredScripts";

  private String filter = ACTION_SCRIPT_TYPE;

  public void setTemplateFilter(String filter) {
    this.filter = filter;
  }  
  public String getTemplateFilter() {
    return this.filter;
  }

  public UIScriptList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "ScriptListIterator");
    setUIComponent(uiPageIterator_);
  }

  public void updateGrid(List<ScriptData> scriptData, int currentPage) throws Exception {
    Collections.sort(scriptData, new ScriptComparator());
    ListAccess<ScriptData> scriptList = new ListAccessImpl<ScriptData>(ScriptData.class, scriptData);
    LazyPageList<ScriptData> dataPageList = new LazyPageList<ScriptData>(scriptList, 10);
    uiPageIterator_.setPageList(dataPageList);
    if (currentPage > uiPageIterator_.getAvailablePage())
      uiPageIterator_.setCurrentPage(uiPageIterator_.getAvailablePage());
    else
      uiPageIterator_.setCurrentPage(currentPage);
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getScriptList() throws Exception { return uiPageIterator_.getCurrentPageData(); }

  public String getScriptCategory() throws Exception {
    ScriptService scriptService =  getApplicationComponent(ScriptService.class);
    UIScriptManager uiManager = getAncestorOfType(UIScriptManager.class);
    UIScriptContainer uiContainer = uiManager.getChildById(uiManager.getSelectedTabId());
    UIScriptList uiScriptList = uiContainer.getChild(UIScriptList.class);
    Node script = scriptService.getECMScriptHome(WCMCoreUtils.getSystemSessionProvider()).getNode(uiScriptList.getTemplateFilter());
    String basePath = scriptService.getBaseScriptPath() + "/";
    return script.getPath().substring(basePath.length());
  }

  public void refresh(String templateFilter, int currentPage) throws Exception {
    this.updateGrid(getScript(templateFilter), currentPage);
  }

  public List<ScriptData> getScript(String name) throws Exception {
    List <ScriptData> scriptData = new ArrayList <ScriptData>() ;
    List<Node> scripts = new ArrayList<Node> () ;
    if(name.equals(ACTION_SCRIPT_TYPE)) {
      scripts = getApplicationComponent(ScriptService.class).getECMActionScripts(WCMCoreUtils.getSystemSessionProvider());
    }else if(name.equals(WIDGET_SCRIPT_TYPE)){
      scripts = getApplicationComponent(ScriptService.class).getECMWidgetScripts(WCMCoreUtils.getSystemSessionProvider());
    }else if(name.equals(INTERCEPTOR_SCRIPT_TYPE)) {
      scripts = 
          getApplicationComponent(ScriptService.class).getECMInterceptorScripts(WCMCoreUtils.getSystemSessionProvider());
    }
    for(Node scriptNode : scripts) {
      Node content = scriptNode.getNode(NodetypeConstant.JCR_CONTENT);
      String scriptDescription;
      try {
        scriptDescription = content.getProperty(NodetypeConstant.DC_DESCRIPTION).getValues()[0].getString();
      } catch(ArrayIndexOutOfBoundsException are) {
	      scriptDescription = scriptNode.getName();
      } catch(PathNotFoundException pne) {
        scriptDescription = scriptNode.getName();
      }
      scriptData.add(new ScriptData(scriptNode.getName(), scriptNode.getPath(), StringEscapeUtils.escapeHtml(scriptDescription)));
    }
    return scriptData ;
  }

  public String[] getActions() {return new String[]{"AddNew"};}

  public Node getScriptNode(String templateFilter, String nodeName) throws Exception {
    ScriptService scriptService =  getApplicationComponent(ScriptService.class);
    Node category = scriptService.getECMScriptHome(WCMCoreUtils.getSystemSessionProvider()).getNode(templateFilter);
    return category.getNode(nodeName);
  }

  static public class ScriptComparator implements Comparator<ScriptData> {
    public int compare(ScriptData o1, ScriptData o2) throws ClassCastException {
      String name1 = o1.getName();
      String name2 = o2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  static public class AddNewActionListener extends EventListener<UIScriptList> {
    public void execute(Event<UIScriptList> event) throws Exception {
      UIScriptList uiScriptList = event.getSource();
      UIScriptManager uiManager = uiScriptList.getAncestorOfType(UIScriptManager.class);
      UIScriptContainer uiScriptContainer = uiManager.getChildById(uiManager.getSelectedTabId());      
      UIScriptForm uiScriptForm = uiScriptContainer.createUIComponent(UIScriptForm.class, null, null) ;
      uiManager.initPopup(uiScriptForm);
      uiScriptForm.update(null, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);      
    }
  }

  static public class EditActionListener extends EventListener<UIScriptList> {
    public void execute(Event<UIScriptList> event) throws Exception {
      UIScriptManager uiManager = event.getSource().getAncestorOfType(UIScriptManager.class);
      UIScriptContainer uiScriptContainer = uiManager.getChildById(uiManager.getSelectedTabId());
      UIScriptList uiScriptList = uiScriptContainer.getChild(UIScriptList.class);

      String scriptName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIScriptForm uiScriptForm = uiScriptContainer.createUIComponent(UIScriptForm.class, null, null) ;
      uiScriptForm.update(uiScriptList.getScriptNode(uiScriptList.getTemplateFilter(), scriptName), false);
      uiManager.initPopup(uiScriptForm);    
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class DeleteActionListener extends EventListener<UIScriptList> {
    public void execute(Event<UIScriptList> event) throws Exception {
      UIScriptManager uiManager = event.getSource().getAncestorOfType(UIScriptManager.class);
      UIScriptContainer uiScriptContainer = uiManager.getChildById(uiManager.getSelectedTabId());
      UIScriptList uiScriptList = uiScriptContainer.getChild(UIScriptList.class);
      ScriptService scriptService =  uiScriptList.getApplicationComponent(ScriptService.class);
      String scriptName = event.getRequestContext().getRequestParameter(OBJECTID);
      String namePrefix = uiScriptList.getScriptCategory();
      try {
        scriptService.removeScript(namePrefix + "/" + scriptName, WCMCoreUtils.getSystemSessionProvider());
        Utils.addEditedConfiguredData(namePrefix + "/" + scriptName, "ScriptServiceImpl", EDITED_CONFIGURED_SCRIPTS, true);
      } catch(AccessDeniedException ace) {
        throw new MessageException(new ApplicationMessage("UIECMAdminControlPanel.msg.access-denied",
                                                          null, ApplicationMessage.WARNING));
      }
      uiScriptList.refresh(uiScriptList.getTemplateFilter(), uiScriptList.uiPageIterator_.getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
  public static class ScriptData {
    private String name;
    private String path;
    private String label;

    public ScriptData(String scriptName, String scriptParth, String label) {
      this.name = scriptName;
      this.path = scriptParth;
      this.label = label;
    }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getLabel() { return label; }
  }
}
