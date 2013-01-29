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

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 22, 2013
 * 9:47:27 AM  
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane_New.gtmpl", 
events = { @EventConfig(listeners = UICLVTemplatesManager.SelectTabActionListener.class) })
public class UICLVTemplatesManager extends UIAbstractManager{

  public static final String NEW_TEMPLATE = "CLVTemplatePopup" ;
  public static final String EDIT_CLV_TEMPLATE = "EditCLVTemplatePopup" ;
  public static final String CONTENT_TEMPLATE_ID = "ContentTemplateContainer";
  public static final String CATE_TEMPLATE_ID = "CateTemplateContainer";
  public static final String PAGE_TEMPLATE_ID = "PageTemplateContainer";
  public static final String CATE_TEMPLATE_LIST_ID = "CateTemplateList";
  public static final String PAGE_TEMPLATE_LIST_ID  = "PageTemplateList";
  public static final String CONTENT_TEMPLATE_TYPE = "contents";
  public static final String CATEGORY_TEMPLATE_TYPE = "category";
  public static final String PAGINATOR_TEMPLATE_TYPE = "paginators";
  
  private String selectedTabId = "ContentTemplateContainer";

  public UICLVTemplatesManager() throws Exception {
    UICLVTemplateContainer uiContentTemp = addChild(UICLVTemplateContainer.class, null, CONTENT_TEMPLATE_ID) ;
    uiContentTemp.getChild(UICLVTemplateList.class).setTemplateFilter(CONTENT_TEMPLATE_TYPE);
    
    UICLVTemplateContainer uiCateTemp = addChild(UICLVTemplateContainer.class, null, CATE_TEMPLATE_ID) ;
    uiCateTemp.getChild(UICLVTemplateList.class).setTemplateFilter(CATEGORY_TEMPLATE_TYPE);
    uiCateTemp.getChild(UICLVTemplateList.class).setId(CATE_TEMPLATE_LIST_ID);
    
    UICLVTemplateContainer uiPageTemp = addChild(UICLVTemplateContainer.class, null, PAGE_TEMPLATE_ID) ;
    uiPageTemp.getChild(UICLVTemplateList.class).setTemplateFilter(PAGINATOR_TEMPLATE_TYPE);
    uiPageTemp.getChild(UICLVTemplateList.class).setId(PAGE_TEMPLATE_LIST_ID);
    
    setSelectedTab(CONTENT_TEMPLATE_ID);
  }
  
  @Override
  public void refresh() throws Exception {
    UICLVTemplateContainer templateContainer = ((UICLVTemplateContainer)getChildById(CONTENT_TEMPLATE_ID));
    templateContainer.update();
    templateContainer.getChild(UICLVTemplateList.class).getUIPageIterator().setId(CONTENT_TEMPLATE_ID + "PageIterator");
    templateContainer.getChild(UICLVTemplateList.class).refresh(
            templateContainer.getChild(UICLVTemplateList.class).getUIPageIterator().getCurrentPage());
    
    UICLVTemplateContainer templateCateContainer = ((UICLVTemplateContainer)getChildById(CATE_TEMPLATE_ID));
    templateCateContainer.update();
    templateCateContainer.getChild(UICLVTemplateList.class).getUIPageIterator().setId(CATE_TEMPLATE_ID + "PageIterator");
    templateCateContainer.getChild(UICLVTemplateList.class).refresh(
            templateCateContainer.getChild(UICLVTemplateList.class).getUIPageIterator().getCurrentPage());
    
    UICLVTemplateContainer templatePageContainer = ((UICLVTemplateContainer)getChildById(PAGE_TEMPLATE_ID));
    templatePageContainer.update();
    templatePageContainer.getChild(UICLVTemplateList.class).getUIPageIterator().setId(PAGE_TEMPLATE_ID + "PageIterator");
    templatePageContainer.getChild(UICLVTemplateList.class).refresh(
            templatePageContainer.getChild(UICLVTemplateList.class).getUIPageIterator().getCurrentPage());
  }

  public String getSelectedTabId()
  {
     return selectedTabId;
  }  
  
  public void setSelectedTab(String renderTabId)
  {
     selectedTabId = renderTabId;
  }

  public void setSelectedTab(int index)
  {
     selectedTabId = getChild(index - 1).getId();
  }  
  
  public boolean isEditingTemplate() {
    UIECMAdminPortlet adminPortlet = this.getAncestorOfType(UIECMAdminPortlet.class);
    UIPopupContainer popupContainer = adminPortlet.getChild(UIPopupContainer.class);
    UIPopupWindow uiPopup = popupContainer.getChild(UIPopupWindow.class);
    uiPopup.setId(EDIT_CLV_TEMPLATE);
    return (uiPopup != null && uiPopup.isShow() && uiPopup.isRendered());
  }
  
  static public class SelectTabActionListener extends EventListener<UICLVTemplatesManager>
  {
    public void execute(Event<UICLVTemplatesManager> event) throws Exception
    {
       WebuiRequestContext context = event.getRequestContext();
       String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
       if (renderTab == null)
          return;
       event.getSource().setSelectedTab(renderTab);
       WebuiRequestContext parentContext = (WebuiRequestContext)context.getParentAppRequestContext();
       if (parentContext != null)
       {
          parentContext.setResponseComplete(true);
       }
       else
       {
          context.setResponseComplete(true);
       }
    }
  } 

}
