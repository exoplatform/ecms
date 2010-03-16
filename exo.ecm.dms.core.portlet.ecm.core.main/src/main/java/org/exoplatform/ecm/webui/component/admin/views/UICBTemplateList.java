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

import javax.jcr.Node;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
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
    template = "app:/groovy/webui/component/UIGridWithButton.gtmpl",
    events = {
        @EventConfig (listeners = UICBTemplateList.DeleteActionListener.class, confirm = "UICBTemplateList.msg.confirm-delete"),
        @EventConfig (listeners = UICBTemplateList.EditInfoActionListener.class),
        @EventConfig (listeners = UICBTemplateList.AddActionListener.class)
    }
)
public class UICBTemplateList extends UIGrid {
  private static String[] VIEW_BEAN_FIELD = {"name", "path", "baseVersion"} ;
  private static String[] VIEW_ACTION = {"EditInfo","Delete"} ;
  public static String ST_CBTempForm = "CBTempForm" ;  
  public static String ST_CBTemp = "CBTemplate" ;
  
  public UICBTemplateList() throws Exception {
    getUIPageIterator().setId("UICBTemplateGrid") ;
    configure("path", VIEW_BEAN_FIELD, VIEW_ACTION) ;
    //updateCBTempListGrid() ;
  }
  public String[] getActions() { return new String[] {"Add"} ; }
  public String getBaseVersion(Node node) throws Exception {
    if(!node.isNodeType(Utils.MIX_VERSIONABLE) || node.isNodeType(Utils.NT_FROZEN)) return "";
    return node.getBaseVersion().getName();    
  }
  
  public List<Node> getAllTemplates() throws Exception {
    ManageViewService viewService = getApplicationComponent(ManageViewService.class) ;
    List<Node> templateList = new ArrayList<Node>() ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    SessionProvider provider = SessionProviderFactory.createSessionProvider() ;
    templateList.addAll(viewService.getAllTemplates(BasePath.CB_DETAIL_VIEW_TEMPLATES, repository,provider)) ;
    templateList.addAll(viewService.getAllTemplates(BasePath.CB_PATH_TEMPLATES, repository,provider)) ;
    templateList.addAll(viewService.getAllTemplates(BasePath.CB_QUERY_TEMPLATES, repository,provider)) ;
    templateList.addAll(viewService.getAllTemplates(BasePath.CB_SCRIPT_TEMPLATES,repository,provider)) ;
    return templateList ;
  }
  
  public String getRepository() {
    return getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
  }
  
  @SuppressWarnings("unchecked")
  public void updateCBTempListGrid(int currentPage) throws Exception {
    List<Node> nodes = getAllTemplates() ;
    List<TemplateBean> tempBeans = new ArrayList<TemplateBean>() ;
    for(Node node : nodes) {
      tempBeans.add(new TemplateBean(node.getName(), node.getPath(), getBaseVersion(node))) ;
    }
    Collections.sort(tempBeans, new CBViewComparator()) ;
    getUIPageIterator().setPageList(new ObjectPageList(tempBeans, 10)) ;
    if(currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(currentPage-1);
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }
  
  static public class CBViewComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      String name1 = ((TemplateBean) o1).getName() ;
      String name2 = ((TemplateBean) o2).getName() ;
      return name1.compareToIgnoreCase(name2) ;
    }
  }
  
  static  public class AddActionListener extends EventListener<UICBTemplateList> {
    public void execute(Event<UICBTemplateList> event) throws Exception {
      UICBTemplateList uiCBTemp = event.getSource() ;
      SessionProvider provider = SessionProviderFactory.createSessionProvider() ;
      Node cbTemplateHome = uiCBTemp.getApplicationComponent(ManageViewService.class)
      .getTemplateHome(BasePath.CONTENT_BROWSER_TEMPLATES, uiCBTemp.getRepository(),provider) ;
      if(cbTemplateHome == null) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICBTemplateList.msg.access-denied", null, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      UIViewManager uiViewManager = uiCBTemp.getAncestorOfType(UIViewManager.class) ;
      UITemplateContainer uiECMTempContainer = uiViewManager.getChildById(UICBTemplateList.ST_CBTemp) ;
      uiECMTempContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Edit") ;
      uiECMTempContainer.initPopup(UICBTemplateList.ST_CBTempForm, "Add") ;
      uiViewManager.setRenderedChild(UICBTemplateList.ST_CBTemp) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiECMTempContainer) ;
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UICBTemplateList> {
    public void execute(Event<UICBTemplateList> event) throws Exception {
      UICBTemplateList uiCBTemp = event.getSource() ;
      String repository = uiCBTemp.getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
      String templatePath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiCBTemp.getApplicationComponent(ManageViewService.class).removeTemplate(templatePath, repository) ;
      uiCBTemp.updateCBTempListGrid(uiCBTemp.getUIPageIterator().getCurrentPage());
      uiCBTemp.setRenderSibling(UICBTemplateList.class);
      UIViewManager uiViewManager = uiCBTemp.getAncestorOfType(UIViewManager.class) ;
      uiViewManager.setRenderedChild(UICBTemplateList.ST_CBTemp) ;
      UITemplateContainer uiTempContainer = uiViewManager.getChildById(UICBTemplateList.ST_CBTemp) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTempContainer) ;
    }
  }

  static  public class EditInfoActionListener extends EventListener<UICBTemplateList> {
    public void execute(Event<UICBTemplateList> event) throws Exception {
      UICBTemplateList uiCBTemp = event.getSource() ;
      String tempPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIViewManager uiViewManager = uiCBTemp.getAncestorOfType(UIViewManager.class) ;
      UITemplateContainer uiTempContainer = uiViewManager.getChildById(UICBTemplateList.ST_CBTemp) ;
      uiTempContainer.removeChildById(UICBTemplateList.ST_CBTempForm + "Add") ;
      uiTempContainer.initPopup(UICBTemplateList.ST_CBTempForm, "Edit") ;
      UITemplateForm uiTempForm = uiTempContainer.findComponentById(UICBTemplateList.ST_CBTempForm) ;
      uiTempForm.isAddNew_ = false ;
      uiTempForm.update(tempPath, null) ;
      uiViewManager.setRenderedChild(UICBTemplateList.ST_CBTemp) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTempContainer) ;
    }
  }
}
