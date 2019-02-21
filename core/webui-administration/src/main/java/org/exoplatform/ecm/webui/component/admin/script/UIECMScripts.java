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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.ecm.webui.component.admin.script.UIScriptList.ScriptData;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 27, 2006
 * 09:22:15 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIECMScripts extends UIContainer {
  public static String SCRIPTLIST_NAME = "ecmScriptList" ;
  public static String SCRIPTFORM_NAME = "ecmScriptForm" ;
  public static String SCRIPT_PAGE =  "PageIterator" ;

  public UIECMScripts() throws Exception {
    addChild(UIECMFilterForm.class, null, null) ;
    UIScriptList list = addChild(UIScriptList.class, null, SCRIPTLIST_NAME) ;
    list.getUIPageIterator().setId(SCRIPTLIST_NAME + SCRIPT_PAGE) ;
  }

  private List<SelectItemOption<String>> getECMCategoryOptions() throws Exception {
    List<SelectItemOption<String>> ecmOptions = new ArrayList<SelectItemOption<String>>() ;
    Node ecmScriptHome = 
      getApplicationComponent(ScriptService.class).getECMScriptHome(WCMCoreUtils.getSystemSessionProvider());
    NodeIterator categories = ecmScriptHome.getNodes() ;
    while(categories.hasNext()) {
      Node script = categories.nextNode() ;
      ecmOptions.add(new SelectItemOption<String>(script.getName(),script.getName())) ;
    }
    return ecmOptions ;
  }

  public void refresh(int currentPage) throws Exception {
    UIECMFilterForm ecmFilterForm = getChild(UIECMFilterForm.class) ;
    String categoryName =
      ecmFilterForm.getUIFormSelectBox(UIECMFilterForm.FIELD_SELECT_SCRIPT).getValue() ;
    if (categoryName == null)  {
      ecmFilterForm.setOptions(getECMCategoryOptions()) ;
      categoryName = ecmFilterForm.getUIFormSelectBox(UIECMFilterForm.FIELD_SELECT_SCRIPT).getValue() ;
    }
    UIScriptList uiScriptList = getChildById(SCRIPTLIST_NAME);
    uiScriptList.updateGrid(getECMScript(categoryName), currentPage);
  }

  public List<ScriptData> getECMScript(String name) throws Exception {
    List <ScriptData> scriptData = new ArrayList <ScriptData>() ;
    List<Node> scripts = new ArrayList<Node> () ;
    if(name.equals("action")) {
      scripts = getApplicationComponent(ScriptService.class).getECMActionScripts(WCMCoreUtils.getSystemSessionProvider());
    }else if(name.equals("widget")){
      scripts = getApplicationComponent(ScriptService.class).getECMWidgetScripts(WCMCoreUtils.getSystemSessionProvider());
    }else if(name.equals("interceptor")) {
      scripts = 
        getApplicationComponent(ScriptService.class).getECMInterceptorScripts(WCMCoreUtils.getSystemSessionProvider());
    }
    for(Node scriptNode : scripts) {
      String version = "" ;
      if(scriptNode.isNodeType(Utils.MIX_VERSIONABLE) && !scriptNode.isNodeType(Utils.NT_FROZEN)){
        version = scriptNode.getBaseVersion().getName();
      }
      scriptData.add(new ScriptData(scriptNode.getName(), scriptNode.getPath(), version)) ;
    }
    return scriptData ;
  }
  
  public void initFormPopup(String id) throws Exception {
    removeChildById(id);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, id);
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600, 250);
    UIScriptForm uiForm = createUIComponent(UIScriptForm.class, null, null);
    uiPopup.setUIComponent(uiForm);
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }
}
