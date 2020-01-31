/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.activity;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.BaseUIActivityBuilder;

public class UILinkActivityBuilder extends BaseUIActivityBuilder {
  private static final Log LOG = ExoLogger.getLogger(UILinkActivityBuilder.class);
  
  @Override
  protected void extendUIActivity(BaseUIActivity uiActivity, ExoSocialActivity activity) {
    UILinkActivity uiLinkActivity = (UILinkActivity) uiActivity;
    
    if (activity.getTemplateParams() == null) {
      saveToNewDataFormat(activity);
    }
    
    Map<String, String> templateParams = activity.getTemplateParams();
    uiLinkActivity.setLinkSource(templateParams.get(UILinkActivityComposer.LINK_PARAM));
    uiLinkActivity.setLinkTitle(templateParams.get(UILinkActivityComposer.TITLE_PARAM));
    uiLinkActivity.setLinkImage(templateParams.get(UILinkActivityComposer.IMAGE_PARAM));
    uiLinkActivity.setLinkDescription(templateParams.get(UILinkActivityComposer.DESCRIPTION_PARAM));
    uiLinkActivity.setLinkComment(templateParams.get(UILinkActivityComposer.COMMENT_PARAM));
    uiLinkActivity.setEmbedHtml(templateParams.get(UILinkActivityComposer.HTML_PARAM));
  }

  private void saveToNewDataFormat(ExoSocialActivity activity) {
    try {
      JSONObject jsonObj = new JSONObject(activity.getTitle());

      StringBuilder linkTitle = new StringBuilder("Shared a link:");
      linkTitle.append(" <a href=\"${").append(UILinkActivityComposer.LINK_PARAM).append("}\">${")
          .append(UILinkActivityComposer.TITLE_PARAM).append("} </a>");
      activity.setTitle(linkTitle.toString());
      
      Map<String, String> templateParams = new HashMap<String, String>();
      templateParams.put(UILinkActivityComposer.LINK_PARAM, jsonObj.getString(UILinkActivityComposer.LINK_PARAM));
      templateParams.put(UILinkActivityComposer.TITLE_PARAM, jsonObj.getString(UILinkActivityComposer.TITLE_PARAM));
      templateParams.put(UILinkActivityComposer.IMAGE_PARAM, jsonObj.getString(UILinkActivityComposer.IMAGE_PARAM));
      templateParams.put(UILinkActivityComposer.DESCRIPTION_PARAM, jsonObj.getString(UILinkActivityComposer.DESCRIPTION_PARAM));
      templateParams.put(UILinkActivityComposer.COMMENT_PARAM, jsonObj.getString(UILinkActivityComposer.COMMENT_PARAM));
      activity.setTemplateParams(templateParams);
      
      CommonsUtils.getService(ActivityManager.class).saveActivityNoReturn(activity);
    } catch (JSONException je) {
      LOG.error("Error with activity's title data");
    } catch (ActivityStorageException ase) {
      LOG.warn("Could not save new data format for document activity.", ase);
    } catch (Exception e) {
      LOG.error("Unknown error  to save document activity.", e);
    }
  }
}
