/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.plugin.doc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.plugin.doc.selector.UIDocumentSelector;
import org.exoplatform.social.webui.composer.UIComposer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author : tuan pham tuanp@exoplatform.com Nov
 * 8, 2011
 */
@ComponentConfig(
                 lifecycle = Lifecycle.class, template = "classpath:groovy/social/plugin/doc/UIDocActivityPopup.gtmpl",
                 events = {
                     @EventConfig(listeners = UIDocActivityPopup.SelectedFileActionListener.class),
                     @EventConfig(listeners = UIDocActivityPopup.CancelActionListener.class)
                 }
)
public class UIDocActivityPopup extends UIContainer implements UIPopupComponent {

  private static final Log                   LOG                      = ExoLogger.getLogger(UIDocActivityPopup.class);

  protected static final String              UI_DOCUMENT_SELECTOR_TAB = "UIDocumentSelectorTab";

  protected static final String              CANCEL                   = "Cancel";

  public static final String                 SELECTEDFILE             = "SelectedFile";

  private UIDocumentSelector                 uiDocumentSelector;

  private List<UIAbstractSelectFileComposer> uiFileSelectors          = new ArrayList<UIAbstractSelectFileComposer>();

  private boolean                            maxSelectedFilesReached;

  private int                                maxFilesCount            = 0;

  public UIDocActivityPopup() {
    try {
      ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
      String documentSelectorTitle = resourceBundle.getString("UIComposerMultiUpload.label.tab.document");

      uiDocumentSelector = addChild(UIDocumentSelector.class, null, UI_DOCUMENT_SELECTOR_TAB);
      uiDocumentSelector.setTitle(documentSelectorTitle);
      uiFileSelectors.add(uiDocumentSelector);
    } catch (Exception e) {
      // UIContainer add selector exception
      LOG.error("An exception happens when init UIDocActivityPopup", e);
    }
  }

  @Override
  public void activate() {
  }

  public int getMaxFilesCount() {
    return maxFilesCount;
  }

  public void setMaxFilesCount(int maxFilesCount) {
    this.maxFilesCount = maxFilesCount;
  }

  public boolean isLimitReached() {
    return maxSelectedFilesReached;
  }

  public void setLimitReached(boolean limitReached) {
    maxSelectedFilesReached = limitReached;
  }

  public List<UIAbstractSelectFileComposer> getUIFileSelectors() {
    return uiFileSelectors;
  }

  @Override
  public void deActivate() {
    UIPopupWindow popup = (UIPopupWindow) this.getParent();
    popup.setUIComponent(null);
    popup.setShow(false);
    popup.setRendered(false);
    ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).addUIComponentToUpdateByAjax(popup.getParent());
  }

  public static class CancelActionListener extends EventListener<UIDocActivityPopup> {
      public void execute(Event<UIDocActivityPopup> event) throws Exception {
        UIDocActivityPopup docActivityPopup = event.getSource();
        docActivityPopup.uiDocumentSelector.resetSelection();
        docActivityPopup.deActivate();
      }
  }

  public static class SelectedFileActionListener extends EventListener<UIDocActivityPopup> {
    public void execute(Event<UIDocActivityPopup> event) throws Exception {
      UIDocActivityPopup uiDocActivityPopup = event.getSource();
      UIPortletApplication uiApp = uiDocActivityPopup.getAncestorOfType(UIPortletApplication.class);

      List<UIAbstractSelectFileComposer> uiFileSelectors = uiDocActivityPopup.getUIFileSelectors();
      
      Set<ComposerFileItem> selectedFileItems = new HashSet<>();
      for (UIAbstractSelectFileComposer uiSelectFileComposer : uiFileSelectors) {
        uiSelectFileComposer.validateSelection();
        if (uiSelectFileComposer.isRendered() && uiSelectFileComposer.getSelectFiles() != null && !uiSelectFileComposer.getSelectFiles().isEmpty()) {
          selectedFileItems.addAll(uiSelectFileComposer.getSelectFiles());
          uiSelectFileComposer.resetSelection();
        }
      }

      if (!selectedFileItems.isEmpty()) {
        UIComposer uiComposer = uiApp.findFirstComponentOfType(UIComposer.class);
        UIDocActivityComposer uiDocActivityComposer = uiComposer.findFirstComponentOfType(UIDocActivityComposer.class);

        uiDocActivityPopup.deActivate();

        uiDocActivityComposer.doSelect(UIAbstractSelectFileComposer.COMPOSER_SELECTION_TYPE, selectedFileItems);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocActivityComposer.getParent());
      }
    }
  }
}
