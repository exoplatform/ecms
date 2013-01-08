/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.fckeditor.DriverConnector;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 18, 2012  
 */
@ComponentConfig(
  template = "app:/groovy/webui/component/explorer/upload/UIMultiUpload.gtmpl",
  events = {
      @EventConfig(listeners = UIMultiUpload.RefreshExplorerActionListener.class)
  }
)
public class UIMultiUpload extends UIContainer {

  private static final Log LOG  = ExoLogger.getLogger(UIMultiUpload.class.getName());
  
  private DriverConnector driveConnector_;
  public UIMultiUpload() throws Exception {
    this.driveConnector_ = WCMCoreUtils.getService(DriverConnector.class);
  }
  
  public int getLimitFileSize() {
    return driveConnector_.getLimitSize();
  }
  
  public int getMaxUploadCount() {
    return driveConnector_.getMaxUploadCount();
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
//    context.getJavascriptManager().require("SHARED/explorer-module", "explorer").
//    addScripts("explorer.MultiUpload.initDropBox('" + this.getId() + " ');");
    super.processRender(context);
  }
  
  public static class RefreshExplorerActionListener extends EventListener<UIMultiUpload> {
    public void execute(Event<UIMultiUpload> event) throws Exception {
      UIMultiUpload uiUpload = event.getSource();
      uiUpload.getAncestorOfType(UIJcrExplorerContainer.class).getChild(UIJCRExplorer.class).updateAjax(event);
    }
  }

}
