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
package org.exoplatform.wcm.ext.component.activity.listener;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

public class TagActivityListener extends Listener<Node, String>{
  
  private static String TAG_ADDED_BUNDLE        = "SocialIntegration.messages.tagAdded";
  private static String TAG_REMOVED_BUNDLE      = "SocialIntegration.messages.tagRemoved";
  private static String TAGS_ADDED_BUNDLE       = "SocialIntegration.messages.tagsAdded";
  private static String TAGS_REMOVED_BUNDLE     = "SocialIntegration.messages.tagsRemoved";
  private static String DOCUMENT_TAG_REMOVED    = "Document.event.TagRemoved";
  private static String DOCUMENT_TAG_ADDED      = "Document.event.TagAdded";
  private static final String TAG_ACTION_COMMENT = "files:spaces.TAG_ACTION_COMMENT";

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    String eventName = event.getEventName();
    if (! (eventName.equals(DOCUMENT_TAG_ADDED) || eventName.equals(DOCUMENT_TAG_REMOVED)) || !activityManager.isActivityTypeEnabled(TAG_ACTION_COMMENT)) {
      return;
    }
    Node currentNode = event.getSource();
    String tagValue = event.getData();
    int tagSepIndex = tagValue.indexOf(",");
    boolean isMultiple = tagSepIndex>0 && !tagValue.endsWith(",");
    String bundleMessage ;
    if (isMultiple) {
      bundleMessage = DOCUMENT_TAG_ADDED.equals(eventName)?TAGS_ADDED_BUNDLE:TAGS_REMOVED_BUNDLE;
    }else {
      bundleMessage = DOCUMENT_TAG_ADDED.equals(eventName)?TAG_ADDED_BUNDLE:TAG_REMOVED_BUNDLE;
    }
    Utils.postActivity(currentNode, bundleMessage, false, true, tagValue, "");
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    List<Node> links = linkManager.getAllLinks(currentNode, NodetypeConstant.EXO_SYMLINK);

    for(Node link: links){
      if(link.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)){
        ExoSocialActivity linkTagActivity = Utils.postActivity(link, bundleMessage, false, true, tagValue, "");
        if (linkTagActivity!=null) {
          ActivityTypeUtils.attachActivityId(link, linkTagActivity.getId());
        }
      }
    }
  }
}
