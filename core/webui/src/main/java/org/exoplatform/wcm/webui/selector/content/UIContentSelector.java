package org.exoplatform.wcm.webui.selector.content;

import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
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

  public void initMetadataPopup() throws Exception {
    UIContentPropertySelector contentPropertySelector = createUIComponent(UIContentPropertySelector.class, null, null);
    contentPropertySelector.setFieldName(UIContentSearchForm.PROPERTY);
    Utils.createPopupWindow(this, contentPropertySelector, UIContentPropertySelector.WEB_CONTENT_METADATA_POPUP, 500);
    contentPropertySelector.init();
    this.setSelectedTab(2);
  }

  public void initNodeTypePopup() throws Exception {
    UIContentNodeTypeSelector contentNodetypeSelector = createUIComponent(UIContentNodeTypeSelector.class, null, null);
    Utils.createPopupWindow(this, contentNodetypeSelector, UIContentNodeTypeSelector.WEB_CONTENT_NODETYPE_POPUP, 500);
    contentNodetypeSelector.init();
    this.setSelectedTab(2);
  }

}
