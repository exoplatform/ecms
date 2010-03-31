package org.exoplatform.wcm.webui.selector.content.one;

import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Jan 20, 2009
 */

@ComponentConfig(
  template = "system:/groovy/webui/core/UITabPane_New.gtmpl"
)

public class UIContentSelectorOne extends UIContentSelector {

  /**
   * Instantiates a new uI content selector one.
   * 
   * @throws Exception the exception
   */
  public UIContentSelectorOne() throws Exception {
    addChild(UIContentBrowsePanelOne.class, null, null);
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
    getChild(UIContentBrowsePanelOne.class).init();
    getChild(org.exoplatform.wcm.webui.selector.content.UIContentSearchForm.class).init();
  }
}
