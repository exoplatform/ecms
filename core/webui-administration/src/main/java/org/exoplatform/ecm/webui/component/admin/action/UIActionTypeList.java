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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.nodetype.NodeType;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

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
        @EventConfig(listeners = UIActionTypeList.AddActionActionListener.class)
    }
)

public class UIActionTypeList extends UIPagingGrid {

  private static String[] ACTIONTYPE_BEAN_FIELD = {"name", "extendType"} ;

  public UIActionTypeList() throws Exception {
    getUIPageIterator().setId("ActionTypeListIterator");
    configure("name", ACTIONTYPE_BEAN_FIELD, null) ;
  }

  public String[] getActions() { return new String[] {"AddAction"} ;}

  @SuppressWarnings("unchecked")
  public void refresh(int currentPage) throws Exception {
    ActionServiceContainer actionsServiceContainer =
      getApplicationComponent(ActionServiceContainer.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    List actionList = (List)actionsServiceContainer.getCreatedActionTypes(repository) ;
    List<ActionData> actions = new ArrayList<ActionData>(actionList.size()) ;
    for(int i = 0; i < actionList.size(); i ++) {
      ActionData bean = new ActionData() ;
      NodeType action = (NodeType)actionList.get(i) ;
      bean.setName(action.getName()) ;
      NodeType[] superTypes = action.getSupertypes() ;
      StringBuilder types = new StringBuilder() ;
      for(int j = 0; j < superTypes.length; j ++) {
        types.append("[").append(superTypes[j].getName()).append("] ") ;
      }
      bean.setExtendType(types.toString()) ;
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
      String name1 = a1.getName();
      String name2 = a2.getName();
      return name1.compareToIgnoreCase(name2);
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
    private String name ;
    private String extendType ;

    public String getName() { return name ; }
    public void setName(String s) { name = s ; }

    public String getExtendType() { return extendType ; }
    public void setExtendType(String s) { extendType = s ; }
  }
}
