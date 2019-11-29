/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.plugin.doc.notification.plugin;

import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

public class DocActivityChildPlugin extends AbstractNotificationChildPlugin {
  public final static ArgumentLiteral<String> ACTIVITY_ID = new ArgumentLiteral<String>(String.class, "activityId");
  public static final String ID = "DOC_ACTIVITY";
  private ExoSocialActivity activity = null;

  public DocActivityChildPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String makeContent(NotificationContext ctx) {
    try {
      ActivityManager activityM = CommonsUtils.getService(ActivityManager.class);

      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = new TemplateContext(ID, language);

      String activityId = notification.getValueOwnerParameter(ACTIVITY_ID.getKey());
      activity = activityM.getActivity(activityId);
      if (activity.isComment()) {
        activity = activityM.getParentActivity(activity);
      }
      templateContext.put("ACTIVITY", activity.getTitle());

      //
      String content = TemplateUtils.processGroovy(templateContext);
      return content;
    } catch (Exception e) {
      return (activity != null) ? activity.getTitle() : "";
    }
  }

  public String getActivityParamValue(String key) {
    Map<String, String> params = activity.getTemplateParams();
    if (params != null) {
      return params.get(key) != null ? params.get(key) : "";
    }
    return "";
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return false;
  }
}
