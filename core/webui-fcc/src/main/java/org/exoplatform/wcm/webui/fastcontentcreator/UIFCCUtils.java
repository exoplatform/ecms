/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 */
package org.exoplatform.wcm.webui.fastcontentcreator;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
public class UIFCCUtils {

  /**
   * Gets the portlet preferences.
   *
   * @return the portlet preferences
   */
  public static PortletPreferences getPortletPreferences() {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest request = portletRequestContext.getRequest();
    return request.getPreferences();
  }

  /**
   * Gets the preference repository.
   *
   * @return the preference repository
   */
  public static String getPreferenceRepository() {
    return getPortletPreferences().getValue(UIFCCConstant.PREFERENCE_REPOSITORY, "");
  }

  /**
   * Gets the preference workspace.
   *
   * @return the preference workspace
   */
  public static String getPreferenceWorkspace() {
    return getPortletPreferences().getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "");
  }

  /**
   * Gets the preference type.
   *
   * @return the preference type
   */
  public static String getPreferenceType() {
    return getPortletPreferences().getValue(UIFCCConstant.PREFERENCE_TYPE, "");
  }

  /**
   * Gets the preference path.
   *
   * @return the preference path
   */
  public static String getPreferencePath() {
    return getPortletPreferences().getValue(UIFCCConstant.PREFERENCE_PATH, "");
  }

  /**
   * Gets the preference save message.
   *
   * @return the preference save message
   */
  public static String getPreferenceSaveMessage() {
    return getPortletPreferences().getValue(UIFCCConstant.PREFERENCE_SAVE_MESSAGE, "");
  }
}
