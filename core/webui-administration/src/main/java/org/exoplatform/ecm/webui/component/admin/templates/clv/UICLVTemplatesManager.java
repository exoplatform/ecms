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
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
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

  final static public String EDIT_CLV_TEMPLATE = "EditCLVTemplatePopup" ;
  final static public String CONTENT_TEMPLATE_ID = "ContentTemplateContainer";
  final static public String CATE_TEMPLATE_ID = "CateTemplateContainer";
  final static public String PAGE_TEMPLATE_ID = "PageTemplateContainer";
  final static public String CATE_TEMPLATE_LIST_ID = "CateTemplateList";
  final static public String PAGE_TEMPLATE_LIST_ID  = "PageTemplateList";
  
  private String selectedTabId = "ContentTemplateContainer";

  public UICLVTemplatesManager() throws Exception {
    UICLVTemplateContainer uiContentTemp = addChild(UICLVTemplateContainer.class, null, CONTENT_TEMPLATE_ID) ;
    uiContentTemp.getChild(UICLVTemplateList.class).setTemplateFilter(UICLVTemplateList.CONTENT_TEMPLATE_TYPE);
    
    UICLVTemplateContainer uiCateTemp = addChild(UICLVTemplateContainer.class, null, CATE_TEMPLATE_ID) ;
    uiCateTemp.getChild(UICLVTemplateList.class).setTemplateFilter(UICLVTemplateList.CATEGORY_TEMPLATE_TYPE);
    uiCateTemp.getChild(UICLVTemplateList.class).setId(CATE_TEMPLATE_LIST_ID);
    
    UICLVTemplateContainer uiPageTemp = addChild(UICLVTemplateContainer.class, null, PAGE_TEMPLATE_ID) ;
    uiPageTemp.getChild(UICLVTemplateList.class).setTemplateFilter(UICLVTemplateList.PAGINATOR_TEMPLATE_TYPE);
    uiPageTemp.getChild(UICLVTemplateList.class).setId(PAGE_TEMPLATE_LIST_ID);
    
    setSelectedTab(CONTENT_TEMPLATE_ID);
  }
  
  @Override
  public void refresh() throws Exception {
    UICLVTemplateContainer templateContainer = ((UICLVTemplateContainer)getChildById(CONTENT_TEMPLATE_ID));
    templateContainer.update();
    templateContainer.getChild(UICLVTemplateList.class).refresh(1);
    
    UICLVTemplateContainer templateCateContainer = ((UICLVTemplateContainer)getChildById(CATE_TEMPLATE_ID));
    templateCateContainer.update();
    templateCateContainer.getChild(UICLVTemplateList.class).refresh(1);
    
    UICLVTemplateContainer templatePageContainer = ((UICLVTemplateContainer)getChildById(PAGE_TEMPLATE_ID));
    templatePageContainer.update();
    templatePageContainer.getChild(UICLVTemplateList.class).refresh(1);   
        
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

}
