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
package org.exoplatform.ecm.webui.tree.selectmany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 *          dzungdev@gmail.com
 * Aug 11, 2008  
 */

@ComponentConfig(
    template = "classpath:groovy/ecm/webui/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UISelectedCategoriesGrid.DeleteActionListener.class, confirm="UISelectedCategoriesGrid.msg.confirm-delete"),
        @EventConfig(listeners = UISelectedCategoriesGrid.SaveCategoriesActionListener.class),
        @EventConfig(listeners = UISelectedCategoriesGrid.CancelActionListener.class)
    }
)
public class UISelectedCategoriesGrid extends UIGrid {

  public final static String[] BEAN_FIELD = {"categoryName"} ;
  public final static String[] BEAN_ACTIONS = {"Delete"} ;
  public final static String[] ACTIONS = {"SaveCategories", "Cancel"} ;
  private static final Log LOG  = ExoLogger.getLogger("admin.UISelectedCategoriesGrid");

  private List<String> selectedCategories = new ArrayList<String>();
  private boolean isDeleteAllCategory;


  public UISelectedCategoriesGrid() throws Exception {
    getUIPageIterator().setId("UICategoriesGrid");
    configure("categoryPath", BEAN_FIELD, BEAN_ACTIONS);
  }

  public void updateGrid (int currentPage) throws Exception {
    List<CategoryData> categoryDataList = new ArrayList<CategoryData>();
    for(String categoryPath: getSelectedCategories()) {
      CategoryData bean = new CategoryData();
      String[] array = categoryPath.split("/");
      String value = "";
      if (array.length > 4) {
        for (int i = 4; i < array.length; i++) {
          value += array[i]; 
          if (i < array.length - 1) value += "/";
        }
      } else value = categoryPath;
      bean.setCategoryName(value);
      bean.setCategoryPath(categoryPath);
      categoryDataList.add(bean);
    }
    Collections.sort(categoryDataList,new CategoryComparator());
    ObjectPageList objPageList = new ObjectPageList(categoryDataList, 5) ;
    getUIPageIterator().setPageList(objPageList) ;
    if(currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(currentPage-1);
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public String[] getActions() {return ACTIONS ;}

  public void addCategory(String categoryPath) {
    selectedCategories.add(categoryPath);
  }

  public void removeCategory(String categoryPath) {
    selectedCategories.remove(categoryPath);
  }

  public List<String> getSelectedCategories() {
    return selectedCategories;
  }

  public void setSelectedCategories(List<String> list) {
    this.selectedCategories = new ArrayList<String>(list);
  }

  public class CategoryData {
    private String categoryName ;
    private String categoryPath ;

    public String getCategoryName() {
      return categoryName;
    }
    public void setCategoryName(String categoryName) {
      this.categoryName = categoryName;
    }
    public String getCategoryPath() {
      return categoryPath;
    }
    public void setCategoryPath(String categoryPath) {
      this.categoryPath = categoryPath;
    }
  }

  static public class CategoryComparator implements Comparator<CategoryData> {
    public int compare(CategoryData cData1, CategoryData cData2) {
      return cData1.getCategoryName().compareTo(cData2.getCategoryName());
    }
  }

  public boolean isDeleteAllCategory() {
    return isDeleteAllCategory;
  }

  public void setDeleteAllCategory(boolean isDeleteAllCategory) {
    this.isDeleteAllCategory = isDeleteAllCategory;
  }
  
  public static class DeleteActionListener extends EventListener<UISelectedCategoriesGrid> {
    public void execute(Event<UISelectedCategoriesGrid> event) throws Exception {
      UISelectedCategoriesGrid uiSelectedCategoriesGrid = event.getSource();
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      uiSelectedCategoriesGrid.removeCategory(value);
      if (uiSelectedCategoriesGrid.getSelectedCategories().size() == 0) uiSelectedCategoriesGrid.setDeleteAllCategory(true);
      uiSelectedCategoriesGrid.updateGrid(uiSelectedCategoriesGrid.getUIPageIterator().getCurrentPage());
      if (uiSelectedCategoriesGrid.getSelectedCategories().size() == 0) {
        uiSelectedCategoriesGrid.setRendered(false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectedCategoriesGrid.getParent());
    }
  }

  public static class SaveCategoriesActionListener extends EventListener<UISelectedCategoriesGrid> {
    public void execute(Event<UISelectedCategoriesGrid> event) throws Exception {
      UISelectedCategoriesGrid uiSelectedCategoriesGrid = event.getSource();
      UICategoriesSelector uiCategoriesSelector = uiSelectedCategoriesGrid.getAncestorOfType(UICategoriesSelector.class);
      String returnField = uiCategoriesSelector.getReturnFieldName();
      List<String> selectedCategories = uiSelectedCategoriesGrid.getSelectedCategories();
      List<String> selectedCategoriesName = new ArrayList<String>();
      for(String item :selectedCategories){
        selectedCategoriesName.add(item.replaceAll("/jcr:system/exo:ecm/exo:taxonomies/", ""));
      }      
      UIApplication uiApplication = uiSelectedCategoriesGrid.getAncestorOfType(UIApplication.class);
      if(selectedCategories.size() == 0 && !uiSelectedCategoriesGrid.isDeleteAllCategory()) {
        uiApplication.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null, ApplicationMessage.INFO));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages());
        return;
      }
      try {
        ((UISelectable)uiCategoriesSelector.getSourceComponent()).doSelect(returnField, selectedCategoriesName);
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
        uiApplication.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.cannot-save", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages());
      }
      uiCategoriesSelector.deActivate();
    }
  }

  static public class CancelActionListener extends EventListener<UISelectedCategoriesGrid> {
    public void execute(Event<UISelectedCategoriesGrid> event) throws Exception { 
      UISelectedCategoriesGrid uiSelectedCategoriesGrid = event.getSource();
      UIComponent uiComponent = uiSelectedCategoriesGrid.getParent();
      if (uiComponent != null) {
        if (uiComponent instanceof UIPopupWindow) {
          ((UIPopupWindow)uiComponent).setShow(false);
          ((UIPopupWindow)uiComponent).setRendered(false);
          event.getRequestContext().addUIComponentToUpdateByAjax(((UIPopupWindow)uiComponent).getParent());
          return;
        } 
        uiSelectedCategoriesGrid.<UIComponent>getParent().broadcast(event, event.getExecutionPhase()) ;
      } 
    }
  }
}
