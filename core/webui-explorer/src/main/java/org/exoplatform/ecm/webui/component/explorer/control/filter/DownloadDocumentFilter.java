/***************************************************************************
 * Copyright (C) 2003-2021 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import java.util.Map;

public class DownloadDocumentFilter extends UIExtensionAbstractFilter {

  public DownloadDocumentFilter() {
    this("UIPopupMenu.msg.download-document");
  }

  public DownloadDocumentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    SettingService settingService = CommonsUtils.getService(SettingService.class);
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id("downloadDocumentStatus"),
                                                      Scope.APPLICATION.id("downloadDocumentStatus"), "exo:downloadDocumentStatus");
    return !(settingValue != null && !settingValue.getValue().toString().isEmpty() ? Boolean.valueOf(settingValue.getValue().toString()) : false);
  }

  public void onDeny(Map<String, Object> context) throws Exception {
    createUIPopupMessages(context, "UIPopupMenu.msg.download-document");
  }

}
