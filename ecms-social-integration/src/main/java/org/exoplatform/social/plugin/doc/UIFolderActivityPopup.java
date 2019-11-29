/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.plugin.doc.selector.BreadcrumbLocation;
import org.exoplatform.social.plugin.doc.selector.UIDocumentSelector;
import org.exoplatform.social.webui.composer.UIComposer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.*;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(lifecycle = Lifecycle.class, template = "classpath:groovy/social/plugin/doc/UIFolderActivityPopup.gtmpl", events = {
    @EventConfig(listeners = UIFolderActivityPopup.SelectFolderActionListener.class),
    @EventConfig(listeners = UIFolderActivityPopup.CancelActionListener.class)
})
public class UIFolderActivityPopup extends UIContainer implements UIPopupComponent {

  private static final Log      LOG                    = ExoLogger.getLogger(UIFolderActivityPopup.class);

  protected static final String UI_FOLDER_SELECTOR_TAB = "UIFolderSelectorTab";

  protected static final String CANCEL                 = "Cancel";

  public static final String    SELECTEDFILE           = "SelectedFile";

  private UIDocumentSelector    uiDocumentSelector;

  private String                destinationFileId;

  public UIFolderActivityPopup() {
    try {
      ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
      String documentSelectorTitle = resourceBundle.getString("UIComposerMultiUpload.label.tab.document");

      uiDocumentSelector = addChild(UIDocumentSelector.class, null, UI_FOLDER_SELECTOR_TAB);
      uiDocumentSelector.setFolderSelection(true);
      uiDocumentSelector.setTitle(documentSelectorTitle);
    } catch (Exception e) {
      // UIContainer add selector exception
      LOG.error("An exception happens when init UIFolderActivityPopup", e);
    }
  }

  @Override
  public void activate() {
    // Nothing to do when activating popup
  }

  @Override
  public void deActivate() {
    UIPopupWindow popup = this.getParent();
    popup.setUIComponent(null);
    popup.setShow(false);
    popup.setRendered(false);
    ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).addUIComponentToUpdateByAjax(popup.getParent());
  }

  public void setDestinationFileId(String destinationFileId) {
    this.destinationFileId = destinationFileId;
  }

  public String getDestinationFileId() {
    return destinationFileId;
  }

  public boolean isFolder() {
    return uiDocumentSelector.getBreadcrumbLocation().isFolder();
  }

  public static class CancelActionListener extends EventListener<UIFolderActivityPopup> {
    public void execute(Event<UIFolderActivityPopup> event) throws Exception {
      UIFolderActivityPopup docActivityPopup = event.getSource();
      docActivityPopup.deActivate();
    }
  }

  public static class SelectFolderActionListener extends EventListener<UIFolderActivityPopup> {
    public void execute(Event<UIFolderActivityPopup> event) throws Exception {
      UIFolderActivityPopup uiFolderActivityPopup = event.getSource();
      UIPortletApplication uiApp = uiFolderActivityPopup.getAncestorOfType(UIPortletApplication.class);

      BreadcrumbLocation breadcrumbLocation = uiFolderActivityPopup.uiDocumentSelector.getBreadcrumbLocation();
      if (breadcrumbLocation == null) {
        throw new IllegalStateException("No selected folder");
      }

      UIComposer uiComposer = uiApp.findFirstComponentOfType(UIComposer.class);
      UIDocActivityComposer uiDocActivityComposer = uiComposer.findFirstComponentOfType(UIDocActivityComposer.class);

      uiFolderActivityPopup.deActivate();

      String destinationFile = uiFolderActivityPopup.getDestinationFileId();
      if (StringUtils.isBlank(destinationFile)) {
        destinationFile = UIAbstractSelectFileComposer.COMPOSER_DESTINATION_FOLDER;
      }
      uiDocActivityComposer.doSelect(destinationFile, breadcrumbLocation);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocActivityComposer.getParent());
    }
  }

}
