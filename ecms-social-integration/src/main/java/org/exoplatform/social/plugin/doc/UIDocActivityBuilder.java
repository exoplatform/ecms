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
package org.exoplatform.social.plugin.doc;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.BaseUIActivityBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SAS
 * Author : Zun
 *          exo@exoplatform.com
 * Jul 23, 2010
 */
public class UIDocActivityBuilder extends BaseUIActivityBuilder {
  private static final Log LOG = ExoLogger.getLogger(UIDocActivityBuilder.class);

  public static final String ACTIVITY_TYPE = "DOC_ACTIVITY";

  @Override
  protected void extendUIActivity(BaseUIActivity uiActivity, ExoSocialActivity activity) {
    UIDocActivity docActivity = (UIDocActivity) uiActivity;
    //
    if (activity.getTemplateParams() == null) {
      saveToNewDataFormat(activity);
    }
    //
    Map<String, String> activityParams = activity.getTemplateParams();
    docActivity.docLink = activityParams.get(UIDocActivity.DOCLINK);
    docActivity.docName = activityParams.get(UIDocActivity.DOCNAME);
    docActivity.message = activityParams.get(UIDocActivity.MESSAGE);
    docActivity.docPath = activityParams.get(UIDocActivity.DOCPATH);
    docActivity.repository = activityParams.get(UIDocActivity.REPOSITORY);
    docActivity.workspace = activityParams.get(UIDocActivity.WORKSPACE);

    // escape node name for special characters
    docActivity.docPath = escapeIllegalJcrCharsOnNodeName(docActivity.docPath);
  }

  /**
   * Escapes special characters of node name in a path.
   *
   * @param path the path
   * @return the escaped node name
   */
  private String escapeIllegalJcrCharsOnNodeName(String path) {
    int lastIndex = path.lastIndexOf("/");
    if (lastIndex != -1) {
      String nodeName = path.substring(lastIndex + 1);
      path  = new StringBuffer(path).delete(path.length() - nodeName.length(), path.length()).
                      append(Text.escapeIllegalJcrChars(nodeName)).toString();
    }
    return path;
  }

  private void saveToNewDataFormat(ExoSocialActivity activity) {
    try {
      final JSONObject jsonObject = new JSONObject(activity.getTitle());
      final String docActivityTitle = "<a href=\"${"+ UIDocActivity.DOCLINK +"}\">" + "${" +UIDocActivity.DOCNAME +"}</a>";
      //
      activity.setTitle(docActivityTitle);
      //
      Map<String, String> activityParams = new HashMap<String, String>();
      activityParams.put(UIDocActivity.DOCNAME, jsonObject.getString(UIDocActivity.DOCNAME));
      activityParams.put(UIDocActivity.DOCLINK, jsonObject.getString(UIDocActivity.DOCLINK));
      activityParams.put(UIDocActivity.DOCPATH, jsonObject.getString(UIDocActivity.DOCPATH));
      activityParams.put(UIDocActivity.REPOSITORY, jsonObject.getString(UIDocActivity.REPOSITORY));
      activityParams.put(UIDocActivity.WORKSPACE, jsonObject.getString(UIDocActivity.WORKSPACE));
      activityParams.put(UIDocActivity.MESSAGE, jsonObject.getString(UIDocActivity.MESSAGE));
      activity.setTemplateParams(activityParams);
      //
      ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
      //
      activityManager.saveActivityNoReturn(activity);
      activity = activityManager.getActivity(activity.getId());
    } catch (JSONException je) {
      LOG.error("Error with activity's title data");
    } catch (ActivityStorageException ase) {
      LOG.error("Could not save new data format for document activity.", ase);
    } catch (Exception e) {
      LOG.error("Unknown error  to save document activity.", e);
    }
  }
}
