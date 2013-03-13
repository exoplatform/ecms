package org.exoplatform.wcm.webui.selector.content;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITabPane;

/**
 * Author : TAN DUNG DANG
 *          dzungdev@gmail.com
 * Jan 20, 2009
 */

@ComponentConfig(
  template = "system:/groovy/webui/core/UITabPane_New.gtmpl"
)

public class UIContentSelector extends UITabPane {

  /** The Constant FOLDER_PATH_SELECTOR_POPUP_WINDOW. */
  public static final String FOLDER_PATH_SELECTOR_POPUP_WINDOW = "FolderPathSelectorPopupWindow";

  /** The Constant CORRECT_CONTENT_SELECTOR_POPUP_WINDOW. */
  public static final String CORRECT_CONTENT_SELECTOR_POPUP_WINDOW = "CorrectContentSelectorPopupWindow";

  public void initMetadataPopup(UIContainer uicontainer) throws Exception {
    UIPopupWindow uiPopupWindow = this.initPopup(uicontainer, UIContentPropertySelector.WEB_CONTENT_METADATA_POPUP);
    UIContentPropertySelector contentPropertySelector = createUIComponent(UIContentPropertySelector.class, null, null);
    uiPopupWindow.setUIComponent(contentPropertySelector);
    contentPropertySelector.setFieldName(UIContentSearchForm.PROPERTY);
    // Utils.createPopupWindow(this, contentPropertySelector, UIContentPropertySelector.WEB_CONTENT_METADATA_POPUP, 500);
    contentPropertySelector.init();
    this.setSelectedTab(2);
    
    uiPopupWindow.setRendered(true);
    uiPopupWindow.setShow(true);
  }

  public void initNodeTypePopup(UIContainer uicontainer) throws Exception {
    UIPopupWindow uiPopupWindow = this.initPopup(uicontainer, UIContentNodeTypeSelector.WEB_CONTENT_NODETYPE_POPUP);
    UIContentNodeTypeSelector contentNodetypeSelector = createUIComponent(UIContentNodeTypeSelector.class, null, null);
    uiPopupWindow.setUIComponent(contentNodetypeSelector);
    contentNodetypeSelector.init();
    this.setSelectedTab(2);
    
    uiPopupWindow.setRendered(true);
    uiPopupWindow.setShow(true);
  }
  
  private UIPopupWindow initPopup(UIContainer uiContainer, String id) throws Exception {
    UIPopupWindow uiPopup = uiContainer.getChildById(id);
    if (uiPopup == null) {
      uiPopup = uiContainer.addChild(UIPopupWindow.class, null, id);
    }
    uiPopup.setWindowSize(500, 0);
    uiPopup.setShow(false);
    uiPopup.setResizable(true);
    return uiPopup;
  }

}
