package org.exoplatform.wcm.webui.selector.content.multi;

import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Jan 20, 2009
 */

@ComponentConfigs ({
  @ComponentConfig(
      template = "system:/groovy/webui/core/UITabPane_New.gtmpl"
  ),
  @ComponentConfig(
      type = UIPopupWindow.class,
      id = "UIWebContentSearchPopup",
      template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
      events = {
        @EventConfig(listeners = UIContentSelectorMulti.CloseActionListener.class, name = "ClosePopup")
      }
  )
})

public class UIContentSelectorMulti extends UIContentSelector {

  /**
   * Instantiates a new uI content selector multi.
   * 
   * @throws Exception the exception
   */
  public UIContentSelectorMulti() throws Exception {
    addChild(UIContentBrowsePanelMulti.class, null, null);
    addChild(org.exoplatform.wcm.webui.selector.content.UIContentSearchForm.class,null,null);
    addChild(org.exoplatform.wcm.webui.selector.content.UIContentSearchResult.class,null,null);
    setSelectedTab(1);
  }

  /**
   * Inits the.
   * 
   * @throws Exception the exception
   */
  public void init() throws Exception {
    getChild(UIContentBrowsePanelMulti.class).init();
    getChild(org.exoplatform.wcm.webui.selector.content.UIContentSearchForm.class).init();
  }
}
