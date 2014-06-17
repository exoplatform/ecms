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
package org.exoplatform.ecm.webui.component.admin.action;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.scripts.impl.ScriptServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 20, 2006
 * 16:37:15
 */
@ComponentConfig(
    template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIActionTypeList.AddActionActionListener.class),
        @EventConfig(listeners = UIActionTypeList.EditActionListener.class),
        @EventConfig(listeners = UIActionTypeList.DeleteActionListener.class, 
          confirm = "UIActionTypeList.msg.confirm-delete")
    }
)

public class UIActionTypeList extends UIPagingGrid {

  private static String[] ACTIONTYPE_BEAN_FIELD = {"label", "name"} ;
  private static String[] ACTIONTYPE_ACTION = {"Edit", "Delete"} ;
  private static final Log LOG = ExoLogger.getLogger(UIActionTypeList.class);

  public UIActionTypeList() throws Exception {
    getUIPageIterator().setId("ActionTypeListIterator");
    configure("type", ACTIONTYPE_BEAN_FIELD, ACTIONTYPE_ACTION) ;
  }

  public String[] getActions() { return new String[] {"AddAction"} ;}

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager()
      .require("SHARED/jquery", "gj")
      .addScripts("gj(document).ready(function() { gj(\"*[rel='tooltip']\").tooltip();});");

    super.processRender(context);
  }
  
  @Override
  public void refresh(int currentPage) throws Exception {
    ActionServiceContainer actionsServiceContainer =
      getApplicationComponent(ActionServiceContainer.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    ScriptServiceImpl scriptService = WCMCoreUtils.getService(ScriptServiceImpl.class);
    Collection<NodeType> actionList = actionsServiceContainer.getCreatedActionTypes(repository) ;
    List<ActionData> actions = new ArrayList<ActionData>(actionList.size()) ;
    UIActionManager uiManager = getParent();
    for(NodeType action : actionList) {
      ActionData bean = new ActionData();
      String resourceName = scriptService.getResourceNameByNodeType(action);
      if(StringUtils.isEmpty(resourceName)) continue;
      bean.setLabel(uiManager.getScriptLabel(action));
      if(resourceName.length() == 0) resourceName = action.getName();
      bean.setType(action.getName());
      bean.setName(StringUtils.substringAfterLast(resourceName, "/")) ;
      actions.add(bean) ;
    }
    Collections.sort(actions, new ActionComparator()) ;
    LazyPageList<ActionData> dataPageList = new LazyPageList<ActionData>(new ListAccessImpl<ActionData>(ActionData.class,
                                                                                                        actions),
                                                                         getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setTotalItems(actions.size());
    getUIPageIterator().setPageList(dataPageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);    
  }
  
  static public class ActionComparator implements Comparator<ActionData> {
    public int compare(ActionData a1, ActionData a2) throws ClassCastException {
      String label1 = a1.getLabel();
      String label2 = a2.getLabel();
      return label1.compareToIgnoreCase(label2);
    }
  }

  static public class EditActionListener extends EventListener<UIActionTypeList> {
    public void execute(Event<UIActionTypeList> event) throws Exception {
      UIActionTypeList uiList = event.getSource();
      UIActionManager uiActionMan = uiList.getParent() ;
      UIActionTypeForm uiForm = uiActionMan.findFirstComponentOfType(UIActionTypeForm.class) ;
      if (uiForm == null) uiForm = uiActionMan.createUIComponent(UIActionTypeForm.class, null, null) ;
      String name = event.getRequestContext().getRequestParameter(OBJECTID);
      NodeTypeManager ntManager = WCMCoreUtils.getRepository().getNodeTypeManager();
      String label = uiActionMan.getScriptLabel(ntManager.getNodeType(name));
      uiForm.update(name, label) ;
      uiActionMan.initPopup(uiForm, 600) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionMan) ;
    }
  }
  
  static public class DeleteActionListener extends EventListener<UIActionTypeList> {
    public void execute(Event<UIActionTypeList> event) throws Exception {
      UIActionTypeList uiList = event.getSource();
      String nodeTypeName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIActionManager uiActionMan = uiList.getParent() ;
      RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      ExtendedNodeTypeManager ntManager = repoService.getCurrentRepository().getNodeTypeManager();
      try {
        ntManager.unregisterNodeType(nodeTypeName);
        Utils.addEditedConfiguredData(nodeTypeName, "ActionTypeList", "EditedConfiguredActionType", true);
      } catch(Exception e) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIActionTypeList.msg.cannot-delete", null, ApplicationMessage.WARNING)) ;
        LOG.error("An error occurs while unregister node type "+nodeTypeName+"", e);
        return;
      }
      uiList.refresh(uiList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionMan) ;
    }
  }
  
  static public class AddActionActionListener extends EventListener<UIActionTypeList> {
    public void execute(Event<UIActionTypeList> event) throws Exception {
      UIActionManager uiActionMan = event.getSource().getParent() ;
      UIActionTypeForm uiForm = uiActionMan.findFirstComponentOfType(UIActionTypeForm.class) ;
      if (uiForm == null) uiForm = uiActionMan.createUIComponent(UIActionTypeForm.class, null, null) ;
      uiForm.refresh() ;
      uiActionMan.initPopup(uiForm, 600) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionMan) ;
    }
  }  

  public static class ActionData {
    private String label ;
    private String name ;
    private String type;

    public String getName() { return name ; }
    public void setName(String s) { name = s ; }

    public String getLabel() { return label ; }
    public void setLabel(String s) { label = s ; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
  }
}
