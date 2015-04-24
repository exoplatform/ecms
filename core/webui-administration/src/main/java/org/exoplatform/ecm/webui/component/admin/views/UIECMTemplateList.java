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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 11:45:11 AM
 */
@ComponentConfig(
    template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIECMTemplateList.DeleteActionListener.class, confirm = "UIECMTemplateList.msg.confirm-delete"),
        @EventConfig(listeners = UIECMTemplateList.EditInfoActionListener.class),
        @EventConfig(listeners = UIECMTemplateList.AddActionListener.class)
    }
)
public class UIECMTemplateList extends UIPagingGrid {
  private static String[] VIEW_BEAN_FIELD = {"name"} ;
  private static String[] VIEW_ACTION = {"EditInfo","Delete"} ;
  public static String ST_ecmTempForm = "ecmTempForm" ;
  public static String ST_ECMTemp = "ECMTemplate" ;

  public UIECMTemplateList() throws Exception {
    getUIPageIterator().setId("UIECMTemplateGrid") ;
    configure("path", VIEW_BEAN_FIELD, VIEW_ACTION) ;
  }

  public String[] getActions() { return new String[] {"Add"} ; }

  public void refresh(int currentPage) throws Exception {
    List<Node> nodes = getApplicationComponent(ManageViewService.class)
                                               .getAllTemplates(
                                                                BasePath.ECM_EXPLORER_TEMPLATES, 
                                                                WCMCoreUtils.getUserSessionProvider());
    List<TemplateBean> tempBeans = new ArrayList<TemplateBean>();
    for (Node node : nodes) {
      tempBeans.add(new TemplateBean(node.getName(), node.getPath()));
    }
    Collections.sort(tempBeans, new ECMViewComparator());
    ListAccess<TemplateBean> tmplBeanList = new ListAccessImpl<TemplateBean>(TemplateBean.class,
                                                                             tempBeans);
    getUIPageIterator().setPageList(new LazyPageList<TemplateBean>(tmplBeanList,
                                                                   getUIPageIterator().getItemsPerPage()));
    getUIPageIterator().setTotalItems(tempBeans.size());
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  static public class ECMViewComparator implements Comparator<TemplateBean> {
    public int compare(TemplateBean o1, TemplateBean o2) throws ClassCastException {
      String name1 = o1.getName();
      String name2 = o2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  static  public class AddActionListener extends EventListener<UIECMTemplateList> {
    public void execute(Event<UIECMTemplateList> event) throws Exception {
      UIECMTemplateList uiECMTempList = event.getSource() ;
      SessionProvider provider = WCMCoreUtils.getUserSessionProvider();
      Node ecmTemplateHome = uiECMTempList.getApplicationComponent(ManageViewService.class)
                                          .getTemplateHome(BasePath.ECM_EXPLORER_TEMPLATES,
                                                           provider);
      if (ecmTemplateHome == null) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIECMTemplateList.msg.access-denied",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      UIViewManager uiViewManager = uiECMTempList.getAncestorOfType(UIViewManager.class) ;
      UITemplateContainer uiECMTempContainer = uiViewManager.getChildById(UIECMTemplateList.ST_ECMTemp) ;
      uiECMTempContainer.removeChildById(UIECMTemplateList.ST_ecmTempForm + "Edit") ;
      uiECMTempContainer.initPopup(UIECMTemplateList.ST_ecmTempForm, "Add") ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiECMTempContainer) ;
    }
  }

  static  public class DeleteActionListener extends EventListener<UIECMTemplateList> {
    public void execute(Event<UIECMTemplateList> event) throws Exception {
      UIECMTemplateList uiECMTemp = event.getSource() ;
      ManageViewService vservice = uiECMTemp.getApplicationComponent(ManageViewService.class) ;
      uiECMTemp.setRenderSibling(UIECMTemplateList.class);
      String templatePath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String templateName = templatePath.substring(templatePath.lastIndexOf("/") + 1) ;
      if (uiECMTemp.getApplicationComponent(ManageDriveService.class).isUsedView(templateName)) {
        UIApplication app = uiECMTemp.getAncestorOfType(UIApplication.class);
        Object[] args = { templateName };
        app.addMessage(new ApplicationMessage("UIECMTemplateList.msg.template-in-use", args));
        return;
      }
      try {
        vservice.removeTemplate(templatePath, WCMCoreUtils.getUserSessionProvider());
        Utils.addEditedConfiguredData(templateName, ManageViewPlugin.class.getSimpleName(), ManageViewPlugin.EDITED_CONFIGURED_VIEWS_TEMPLATES, true);
      } catch (AccessDeniedException ex) {
        UIApplication app = uiECMTemp.getAncestorOfType(UIApplication.class);
        Object[] args = { "UIViewFormTabPane.label.option." + templateName };
        app.addMessage(new ApplicationMessage("UIECMTemplateList.msg.delete-permission-denied",
                                              args,
                                              ApplicationMessage.WARNING));
        return;
      }
      uiECMTemp.refresh(uiECMTemp.getUIPageIterator().getCurrentPage()) ;
      UITemplateContainer uiTempContainer = uiECMTemp.getParent() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTempContainer) ;
    }
  }

  static  public class EditInfoActionListener extends EventListener<UIECMTemplateList> {
    public void execute(Event<UIECMTemplateList> event) throws Exception {
      UIECMTemplateList uiECMTemp = event.getSource() ;
      String tempPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITemplateContainer uiTempContainer = uiECMTemp.getParent() ;
      uiTempContainer.removeChildById(UIECMTemplateList.ST_ecmTempForm + "Add") ;
      uiTempContainer.initPopup(UIECMTemplateList.ST_ecmTempForm, "Edit") ;
      UITemplateForm uiTempForm = uiTempContainer.findComponentById(UIECMTemplateList.ST_ecmTempForm) ;
      uiTempForm.update(tempPath, null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTempContainer) ;
    }
  }
}
