package org.exoplatform.wcm.webui.selector.content.one;

import org.exoplatform.wcm.webui.selector.content.UIContentSearchForm;
import org.exoplatform.wcm.webui.selector.content.UIContentSearchResult;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;

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
    type = UIContentSearchResult.class,
    template = "classpath:groovy/wcm/webui/selector/content/one/UIContentSearchResult.gtmpl",
    events = {
      @EventConfig(listeners = UIContentSearchResult.SelectActionListener.class),
      @EventConfig(listeners = UIContentSearchResult.ViewActionListener.class)
    }
  )
})

public class UIContentSelectorOne extends UIContentSelector {

  /**
   * Instantiates a new uI content selector one.
   *
   * @throws Exception the exception
   */
  public UIContentSelectorOne() throws Exception {
    addChild(UIContentBrowsePanelOne.class, null, null);
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

  /**
   * Set the init path, when the popup window appears, it will go to the node
   * specified by this init path.
   * @param initPath
   * @throws Exception
   */
  public void init(String initDrive, String initPath) throws Exception {
    getChild(UIContentBrowsePanelOne.class).setInitPath(initDrive, initPath);
    this.init();
  }
}
