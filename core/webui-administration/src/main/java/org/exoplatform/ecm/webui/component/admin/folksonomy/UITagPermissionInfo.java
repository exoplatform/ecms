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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 14, 2009
 * 10:01:53 AM
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig (listeners = UITagPermissionInfo.DeleteActionListener.class, 
                    confirm = "UITagPermissionInfo.msg.confirm-delete-permission")
    }
)
public class UITagPermissionInfo extends UIContainer {

  public static String[] PERMISSION_BEAN_FIELD = {"usersOrGroups"};

  private static String[] PERMISSION_ACTION = {"Delete"};

  public UITagPermissionInfo() throws Exception {
    UIGrid uiGrid = createUIComponent(UIGrid.class, null, "TagPermissionInfo");
    addChild(uiGrid);
    updateGrid();
    uiGrid.getUIPageIterator().setId("TagPermissionInfoIterator");
    uiGrid.configure(PERMISSION_BEAN_FIELD[0], PERMISSION_BEAN_FIELD, PERMISSION_ACTION);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    updateGrid();
    super.processRender(context);
  }

  public void updateGrid() throws Exception {
    List<TagPermissionData> tagPermissions = new ArrayList<TagPermissionData>();
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
    for (String usersOrGroups : newFolksonomyService.getTagPermissionList()) {
      tagPermissions.add(new TagPermissionData(usersOrGroups));
    }
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class);
    int currentPage = uiGrid.getUIPageIterator().getCurrentPage();
    ListAccess<TagPermissionData> tagPermList = new ListAccessImpl<TagPermissionData>(TagPermissionData.class,
                                                                                      tagPermissions);
    LazyPageList<TagPermissionData> dataPageList = new LazyPageList<TagPermissionData>(tagPermList, 10);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
    int total = uiGrid.getUIPageIterator().getAvailablePage();
    uiGrid.getUIPageIterator().setCurrentPage(currentPage < total ? currentPage : total);
  }

  public static class DeleteActionListener extends EventListener<UITagPermissionInfo> {
    public void execute(Event<UITagPermissionInfo> event) throws Exception {
      UITagPermissionInfo uiInfo = event.getSource();
      String name = event.getRequestContext().getRequestParameter(OBJECTID);
      NewFolksonomyService folksonomyService = uiInfo.getApplicationComponent(NewFolksonomyService.class);
      folksonomyService.removeTagPermission(name);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiInfo) ;
    }
  }

  public static class TagPermissionData {
    private String usersOrGroups;

    public TagPermissionData(String usersOrGroups) {
      this.usersOrGroups = usersOrGroups;
    }

    public String getUsersOrGroups() { return usersOrGroups; }
  }
}
