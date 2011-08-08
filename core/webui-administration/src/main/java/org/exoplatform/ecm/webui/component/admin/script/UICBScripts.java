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
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 27, 2006
 * 09:22:15 AM
 */

@ComponentConfig( lifecycle = UIContainerLifecycle.class )

public class UICBScripts extends UIContainer {
  public static String SCRIPTLIST_NAME = "CBScriptList" ;
  public static String SCRIPTFORM_NAME = "CBScriptForm" ;
  public static String SCRIPT_PAGE =  "PageIterator" ;

  public UICBScripts() throws Exception {
    UIScriptList list = addChild(UIScriptList.class, null, SCRIPTLIST_NAME) ;
    list.getUIPageIterator().setId(SCRIPTLIST_NAME + SCRIPT_PAGE) ;
  }

  public void refresh (int currentPage) throws Exception {
    UIScriptList uiScriptList = getChildById(SCRIPTLIST_NAME) ;
    uiScriptList.updateGrid(getCBScript(), currentPage);
  }

  public List<ScriptData> getCBScript() throws Exception {
    List <ScriptData> scriptData = new ArrayList <ScriptData>() ;
    ScriptService scriptService = getApplicationComponent(ScriptService.class) ;
    Node cbScripts = scriptService.getCBScriptHome(WCMCoreUtils.getSystemSessionProvider()) ;
    NodeIterator nodeList = cbScripts.getNodes() ;
    ScriptData script ;
    while(nodeList.hasNext()) {
      Node node = nodeList.nextNode() ;
      String version = "" ;
      if(node.isNodeType(Utils.MIX_VERSIONABLE) && !node.isNodeType(Utils.NT_FROZEN)){
        version = node.getBaseVersion().getName();
      }
      script = new ScriptData(node.getName(), node.getPath(), version) ;
      scriptData.add(script) ;
    }
    return scriptData ;
  }
  public void initFormPopup(String id) throws Exception {
	removeChildById(id) ;
	UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, id) ;
	uiPopup.setShowMask(true);
	uiPopup.setWindowSize(600, 500) ;
	UIScriptForm uiForm = createUIComponent(UIScriptForm.class, null, null) ;	
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
