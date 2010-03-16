/*
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
 */
package org.exoplatform.wcm.webui.selector.content.multi;

import java.util.List;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectmany.UISelectedCategoriesGrid;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.selector.UISourceGridUpdatable;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author : anh.do anh.do@exoplatform.com,
 * anhdn86@gmail.com Feb 16, 2009
 */

@ComponentConfig(
    template = "classpath:groovy/wcm/webui/selector/content/multi/UICLVContentSelectedGrid.gtmpl",
    events = {
        @EventConfig(listeners = UICLVContentSelectedGrid.DeleteActionListener.class, confirm = "UISelectedContentGrid.msg.confirm-delete"),
        @EventConfig(listeners = UICLVContentSelectedGrid.SaveCategoriesActionListener.class),
        @EventConfig(listeners = UICLVContentSelectedGrid.CancelActionListener.class) 
      }
)
public class UICLVContentSelectedGrid extends UISelectedCategoriesGrid {
  
  /** The Constant CORRECT_CONTENT_SELECTOR_POPUP_WINDOW. */
  public static final String CORRECT_CONTENT_SELECTOR_POPUP_WINDOW = "CorrectContentSelectorPopupWindow";

  /**
   * Instantiates a new uICLV content selected grid.
   * 
   * @throws Exception the exception
   */
  public UICLVContentSelectedGrid() throws Exception {
    super();
  }
  
  public void init(List<String> contents) throws Exception {
    for(String content : contents) {
      if(!getSelectedCategories().contains(content)) {
        addCategory(content);
        updateGrid(getUIPageIterator().getCurrentPage());
      }
    }
  }

  /**
   * The listener interface for receiving deleteAction events.
   * The class that is interested in processing a deleteAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteActionListener<code> method. When
   * the deleteAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see DeleteActionEvent
   */
  public static class DeleteActionListener extends EventListener<UICLVContentSelectedGrid> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVContentSelectedGrid> event) throws Exception {
      UICLVContentSelectedGrid uiSelectedContentGrid = event.getSource();
      UIContentBrowsePanel uiContentBrowsePanel = uiSelectedContentGrid.getAncestorOfType(UIContentBrowsePanel.class);
      UISelectable contentBrowsePanel = (UISelectable) uiContentBrowsePanel.getSourceComponent();
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      uiSelectedContentGrid.removeCategory(value);
      uiSelectedContentGrid.updateGrid(uiSelectedContentGrid.getUIPageIterator().getCurrentPage());
      ((UISourceGridUpdatable)contentBrowsePanel).doSave(uiSelectedContentGrid.getSelectedCategories());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectedContentGrid.getParent());      
    }
  }

  /**
   * The listener interface for receiving saveCategoriesAction events.
   * The class that is interested in processing a saveCategoriesAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveCategoriesActionListener<code> method. When
   * the saveCategoriesAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SaveCategoriesActionEvent
   */
  public static class SaveCategoriesActionListener extends EventListener<UICLVContentSelectedGrid> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVContentSelectedGrid> event) throws Exception {
      UICLVContentSelectedGrid uiSelectedContentGrid = event.getSource();
      UIContentBrowsePanel uiContentBrowsePanel = uiSelectedContentGrid.getAncestorOfType(UIContentBrowsePanel.class);
      String returnField = uiContentBrowsePanel.getReturnFieldName();
      List<String> selectedCategories = uiSelectedContentGrid.getSelectedCategories();
      if (selectedCategories.size() == 0) {
      	Utils.createPopupMessage(uiContentBrowsePanel, "UISelectedContentGrid.msg.non-content", null, ApplicationMessage.INFO);
        return;
      }
      try {
        StringBuilder contents = new StringBuilder();
        for (String item : selectedCategories) {
          contents.append(item).append(";");
        }        
        UISelectable contentBrowsePanel = (UISelectable) uiContentBrowsePanel.getSourceComponent();
        contentBrowsePanel.doSelect(returnField, contents.toString());        
        ((UISourceGridUpdatable)contentBrowsePanel).doSave(selectedCategories);
      } catch (Exception e) {
        Utils.createPopupMessage(uiContentBrowsePanel, "UISelectedCategoriesGrid.msg.cannot-save", null, ApplicationMessage.WARNING);
        return;
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UICLVContentSelectedGrid> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVContentSelectedGrid> event) throws Exception {
      UICLVContentSelectedGrid uiSelectedContent = event.getSource();
      UIContentBrowsePanel uiContentBrowsePanel = uiSelectedContent.getAncestorOfType(UIContentBrowsePanel.class);
      Utils.closePopupWindow(uiContentBrowsePanel, CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);    
    }
  }
}
