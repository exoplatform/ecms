package org.exoplatform.wcm.webui.selector.content.folder;

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

public class UIContentSelectorFolder extends UIContentSelector {

  public UIContentSelectorFolder() throws Exception {
    addChild(UIContentBrowsePanelFolder.class, null, null);
    setSelectedTab(1);
  }

  /**
   * Set the init path, when the popup window appears, it will go to the node
   * specified by this init path.
   * @param initPath
   * @throws Exception
   */
  public void init(String initDrive, String initPath) throws Exception {
    this.getChild(UIContentBrowsePanelFolder.class).setInitPath(initDrive, initPath);
  }

}
