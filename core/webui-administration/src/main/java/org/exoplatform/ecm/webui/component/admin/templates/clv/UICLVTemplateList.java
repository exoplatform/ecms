/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.templates.clv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.impl.ApplicationTemplateManagerServiceImpl;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 22, 2013
 * 9:55:06 AM  
 */
@ComponentConfig(
        template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
        events = {
                @EventConfig(listeners = 
                        UICLVTemplateList.DeleteActionListener.class, confirm = "UICLVTemplateList.msg.confirm-delete"),
                @EventConfig(listeners = UICLVTemplateList.EditActionListener.class),
                @EventConfig(listeners = UICLVTemplateList.AddTemplateActionListener.class)
        })
public class UICLVTemplateList extends UIPagingGrid {

  private static String[] NODETYPE_BEAN_FIELD = {"title", "template"} ;
  private static String[] NODETYPE_ACTION = {"Edit", "Delete"} ;
  
  private String filter = UICLVTemplatesManager.CONTENT_TEMPLATE_TYPE;
  
  
  public UICLVTemplateList() throws Exception {
    configure("template", NODETYPE_BEAN_FIELD, NODETYPE_ACTION) ;
  }
  
  public void setTemplateFilter(String filter) {
    this.filter = filter;
  }  
  
  public String getTemplateFilter() {
    return this.filter;
  }  
  
  public String[] getActions() {
    return new String[] {"AddTemplate"} ;
  }
  
  @Override
  public void refresh(int currentPage) throws Exception {
    ApplicationTemplateManagerService templateService = WCMCoreUtils.getService(ApplicationTemplateManagerService.class);
    List<CLVTemplateData> templateData = new ArrayList<CLVTemplateData>();
      
    if(filter.equals(UICLVTemplatesManager.CONTENT_TEMPLATE_TYPE)) { 
      templateData = convetListNodeToListData(templateService.getTemplatesByCategory(
              ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
              ApplicationTemplateManagerService.CLV_LIST_TEMPLATE_CATEGORY, 
              WCMCoreUtils.getUserSessionProvider()));
    } else if(filter.equals(UICLVTemplatesManager.CATEGORY_TEMPLATE_TYPE)) {
      templateData = convetListNodeToListData(templateService.getTemplatesByCategory(
              ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
              ApplicationTemplateManagerService.CLV_NAVIGATION_TEMPLATE_CATEGORY, 
              WCMCoreUtils.getUserSessionProvider()));
    } else if(filter.equals(UICLVTemplatesManager.PAGINATOR_TEMPLATE_TYPE)){
      templateData = convetListNodeToListData(templateService.getTemplatesByCategory(
              ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
              ApplicationTemplateManagerService.CLV_PAGINATOR_TEMPLATE_CATEGORY, 
              WCMCoreUtils.getUserSessionProvider()));
    }          
    Collections.sort(templateData, new CLVTemplateComparator());
    ListAccess<CLVTemplateData> dataList = new ListAccessImpl<CLVTemplateData>(CLVTemplateData.class,
                                                                         templateData);
    LazyPageList<CLVTemplateData> pageList = new LazyPageList<CLVTemplateData>(dataList,
                                                                         getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setTotalItems(templateData.size());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }
  
  private List<CLVTemplateData> convetListNodeToListData(List<Node> list) throws RepositoryException {
    List<CLVTemplateData> templateDatas = new ArrayList<CLVTemplateData>();
    for(Node node : list) {
      Node content = node.getNode(Utils.JCR_CONTENT);
      try {
        templateDatas.add(new CLVTemplateData(content.getProperty(
                NodetypeConstant.DC_TITLE).getValues()[0].getString(), node.getName()));
      } catch(PathNotFoundException pne) {
        templateDatas.add(new CLVTemplateData(node.getName(), node.getName()));
      } catch(ArrayIndexOutOfBoundsException aoe) {
        templateDatas.add(new CLVTemplateData(node.getName(), node.getName()));
      }
    }
    return templateDatas;
  }
  
  static public class EditActionListener extends EventListener<UICLVTemplateList> {
    public void execute(Event<UICLVTemplateList> event) throws Exception {
      UICLVTemplateList clvTemplateList = event.getSource();
      UICLVTemplatesManager uiTemplatesManager = clvTemplateList.getAncestorOfType(UICLVTemplatesManager.class) ;      
      UICLVTemplateContainer uiTemplateContainer = uiTemplatesManager.getChildById(uiTemplatesManager.getSelectedTabId());
      UICLVTemplateForm uiTemplateForm = uiTemplateContainer.createUIComponent(UICLVTemplateForm.class, null, 
              "UICLVTemplateForm_" + uiTemplatesManager.getSelectedTabId());
      String template = event.getRequestContext().getRequestParameter(OBJECTID);
      uiTemplateForm.update(clvTemplateList.getCategoryFromFilter(), template);
      uiTemplateContainer.removeChildById(UICLVTemplatesManager.NEW_TEMPLATE + "_" + uiTemplatesManager.getSelectedTabId());
      uiTemplateContainer.initPopup(uiTemplateForm, 
              UICLVTemplatesManager.EDIT_CLV_TEMPLATE + "_" + uiTemplatesManager.getSelectedTabId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplatesManager);
    }
  }
  
  static public class AddTemplateActionListener extends EventListener<UICLVTemplateList> {
    public void execute(Event<UICLVTemplateList> event) throws Exception {
      UICLVTemplateList uiList = event.getSource();
      UICLVTemplatesManager uiTemplatesManager = uiList.getAncestorOfType(UICLVTemplatesManager.class) ;      
      UICLVTemplateContainer uiTemplateContainer = uiTemplatesManager.getChildById(uiTemplatesManager.getSelectedTabId());
      
      UICLVTemplateForm uiTemplateForm = uiTemplateContainer.createUIComponent(UICLVTemplateForm.class, null, 
              "UICLVTemplateForm_" + uiTemplatesManager.getSelectedTabId()) ;
      uiTemplateContainer.removeChildById(UICLVTemplatesManager.EDIT_CLV_TEMPLATE + "_" + uiTemplatesManager.getSelectedTabId()) ;
      
      uiTemplateForm.refresh(uiList.getCategoryFromFilter());
      
      uiTemplateContainer.initPopup(uiTemplateForm, 
              UICLVTemplatesManager.NEW_TEMPLATE + "_" + uiTemplatesManager.getSelectedTabId()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplatesManager) ;
    }
  }
  
  static public class DeleteActionListener extends EventListener<UICLVTemplateList> {
    public void execute(Event<UICLVTemplateList> event) throws Exception {
      UICLVTemplateList clvTemplateList = event.getSource();
      UICLVTemplatesManager uiTemplatesManager = clvTemplateList.getAncestorOfType(UICLVTemplatesManager.class);
      
      if (uiTemplatesManager.isEditingTemplate()) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICLVTemplateList.msg.editing-template", null, ApplicationMessage.WARNING)) ;
        return;
      }
      
      String template = event.getRequestContext().getRequestParameter(OBJECTID);
      ApplicationTemplateManagerService templateService = 
              clvTemplateList.getApplicationComponent(ApplicationTemplateManagerService.class);
      try {
        templateService.removeTemplate(
                ApplicationTemplateManagerService.CLV_TEMPLATE_STORAGE_FOLDER, 
                clvTemplateList.getCategoryFromFilter(), 
                template, 
                WCMCoreUtils.getUserSessionProvider());
        StringBuilder tBuilder = new StringBuilder();
        tBuilder.append(clvTemplateList.getCategoryFromFilter()).append("/").append(template);
        org.exoplatform.services.cms.impl.Utils.addEditedConfiguredData(tBuilder.toString(),
            ApplicationTemplateManagerServiceImpl.class.getSimpleName(),
            ApplicationTemplateManagerServiceImpl.EDITED_CONFIGURED_TEMPLATES, true);
      } catch (PathNotFoundException ex) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UITCLVemplateList.msg.template-not-exist", null, ApplicationMessage.WARNING)) ;
        return;
      }
      clvTemplateList.refresh(clvTemplateList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplatesManager);
      
    }
  }  
  
  static public class CLVTemplateComparator implements Comparator<CLVTemplateData> {
    public int compare(CLVTemplateData t1, CLVTemplateData t2) throws ClassCastException {
      String title1 = t1.getTitle();
      String title2 = t2.getTitle();
      return title1.compareToIgnoreCase(title2);
    }
  }  
  
  static public class CLVTemplateData {
    private String title ;
    private String template;

    public CLVTemplateData(String title, String template) { 
        this.title = title ;
        this.template = template;
    }
    public String getTitle() { return title ; }
    public String getTemplate() { return template; }
  }
  
  private String getCategoryFromFilter() {
    if(filter.equals(UICLVTemplatesManager.CONTENT_TEMPLATE_TYPE)) { 
      return ApplicationTemplateManagerService.CLV_LIST_TEMPLATE_CATEGORY;
    } else if(filter.equals(UICLVTemplatesManager.CATEGORY_TEMPLATE_TYPE)) {
      return ApplicationTemplateManagerService.CLV_NAVIGATION_TEMPLATE_CATEGORY;
    } else if(filter.equals(UICLVTemplatesManager.PAGINATOR_TEMPLATE_TYPE)){
      return ApplicationTemplateManagerService.CLV_PAGINATOR_TEMPLATE_CATEGORY; 
    } 
    return null;
  }

}
