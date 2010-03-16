package org.exoplatform.wcm.webui.selector.content;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Author : TAN DUNG DANG
 *          dzungdev@gmail.com
 * Jan 20, 2009  
 */

@ComponentConfigs ({
  @ComponentConfig(
      template = "system:/groovy/webui/core/UITabPane_New.gtmpl",
      events = {
        @EventConfig(listeners = UIContentSelector.CloseActionListener.class, name = "ClosePopup")
      }
  ),
  @ComponentConfig(
      type = UIPopupWindow.class,
      id = "UIWebContentSearchPopup",
      template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
      events = {
        @EventConfig(listeners = UIContentSelector.CloseActionListener.class, name = "ClosePopup")
      }
  )
})

public class UIContentSelector extends UITabPane {

  final static public String WEB_CONTENT_METADATA_POPUP = "WebContentMetadataPopup";
  final static public String WEB_CONTENT_NODETYPE_POPUP = "WebContentNodeTypePopup";

  public void initMetadataPopup() throws Exception {
    UIPopupWindow uiPopupWindow = addChild(UIPopupWindow.class, "UIWebContentSearchPopup", WEB_CONTENT_METADATA_POPUP);
    UIContentPropertySelector contentPropertySelector = createUIComponent(UIContentPropertySelector.class, null, null);
    contentPropertySelector.setFieldName(UIContentSearchForm.PROPERTY);
    contentPropertySelector.init();
    uiPopupWindow.setUIComponent(contentPropertySelector);
    uiPopupWindow.setWindowSize(500, 450);
    uiPopupWindow.setResizable(true);
    uiPopupWindow.setShow(true);
    this.setSelectedTab(uiPopupWindow.getId());
  }

  public void initNodeTypePopup() throws Exception {
    UIPopupWindow uiPopupWindow = addChild(UIPopupWindow.class, "UIWebContentSearchPopup", WEB_CONTENT_NODETYPE_POPUP);
    UIContentNodeTypeSelector contentNodetypeSelector = createUIComponent(UIContentNodeTypeSelector.class, null, null);
    uiPopupWindow.setUIComponent(contentNodetypeSelector);
    contentNodetypeSelector.init();
    uiPopupWindow.setWindowSize(500, 450);
    uiPopupWindow.setResizable(true);
    uiPopupWindow.setShow(true);
    this.setSelectedTab(uiPopupWindow.getId());
  }

  public static class CloseActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIContentSelector contentSelector = event.getSource().getAncestorOfType(UIContentSelector.class);
      UIContentSearchForm contentSearchForm = contentSelector.getChild(UIContentSearchForm.class);
      contentSelector.removeChild(UIPopupWindow.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(contentSelector);
      contentSelector.setSelectedTab(contentSearchForm.getId());
    }    
  }
}
