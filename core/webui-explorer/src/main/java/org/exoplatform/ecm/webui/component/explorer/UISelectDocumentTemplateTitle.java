/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UISelectDocumentForm;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author :
 *  eXoPlatform dongpd@exoplatform.com
 * Feb 2, 2013
 */
@ComponentConfig(template = "app:/groovy/webui/component/explorer/UISelectDocumentTemplateTitle.gtmpl", events = {
    @EventConfig(listeners = UISelectDocumentTemplateTitle.ChangeViewActionListener.class),
    @EventConfig(listeners = UISelectDocumentTemplateTitle.CancelActionListener.class) })
public class UISelectDocumentTemplateTitle extends UIComponent {

  private final static String THUMBNAIL_VIEW_TEMPLATE       =
    "app:/groovy/webui/component/explorer/UISelectDocumentFormThumbnailView.gtmpl";

  private final static String LIST_VIEW_TEMPLATE            =
    "app:/groovy/webui/component/explorer/UISelectDocumentFormListView.gtmpl";

  static public class ChangeViewActionListener extends EventListener<UISelectDocumentTemplateTitle> {
    private static final String THUMBNAIL_VIEW_TYPE = "ThumbnailView";

    public void execute(Event<UISelectDocumentTemplateTitle> event) throws Exception {
      String viewType = event.getRequestContext().getRequestParameter(OBJECTID);
      UISelectDocumentTemplateTitle uiTemplateTitle = event.getSource();
      UIWorkingArea uiWorkingArea = uiTemplateTitle.getAncestorOfType(UIWorkingArea.class);
      UISelectDocumentForm uiSelectForm = 
          uiWorkingArea.getChild(UIDocumentWorkspace.class)
            .getChild(UIDocumentFormController.class)
            .getChild(UISelectDocumentForm.class);
      UIJCRExplorer uiExplorer = uiSelectForm.getAncestorOfType(UIJCRExplorer.class);
      if (viewType.equals(THUMBNAIL_VIEW_TYPE)) {
        uiSelectForm.setTemplate(THUMBNAIL_VIEW_TEMPLATE);
      } else {
        uiSelectForm.setTemplate(LIST_VIEW_TEMPLATE);
      }
      uiExplorer.updateAjax(event);
    }
  }
  
  static public class CancelActionListener extends EventListener<UISelectDocumentTemplateTitle> {
    public void execute(Event<UISelectDocumentTemplateTitle> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      if (uiExplorer != null) {
        UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
        UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
        if (uiDocumentWorkspace.getChild(UIDocumentFormController.class) != null) {
          uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
        } else
          uiExplorer.cancelAction();
        uiExplorer.updateAjax(event);
      }
    }
  }
}
