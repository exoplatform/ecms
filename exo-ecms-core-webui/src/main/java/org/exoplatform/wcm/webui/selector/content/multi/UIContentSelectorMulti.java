package org.exoplatform.wcm.webui.selector.content.multi;

import org.exoplatform.wcm.webui.selector.content.UIContentSearchForm;
import org.exoplatform.wcm.webui.selector.content.UIContentSearchResult;
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

public class UIContentSelectorMulti extends UIContentSelector {

  /**
   * Instantiates a new uI content selector multi.
   * 
   * @throws Exception the exception
   */
  public UIContentSelectorMulti() throws Exception {
    addChild(UIContentBrowsePanelMulti.class, null, null);
    addChild(UIContentSearchForm.class,null,null);
    addChild(UIContentSearchResult.class,null,null);
    setSelectedTab(1);
  }

  /**
   * Inits the.
   * 
   * @throws Exception the exception
   */
  public void init() throws Exception {
    getChild(UIContentSearchForm.class).init();
  }
}
