/*
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
 */
package org.exoplatform.wcm.ext.component.activity;

import java.util.Map;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

@ComponentConfig(lifecycle = UIFormLifecycle.class,
    template = "war:/groovy/ecm/social-integration/plugin/space/SharedFileUIActivity.gtmpl",
    events = {
        @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class),
        @EventConfig(listeners = FileUIActivity.DownloadDocumentActionListener.class)
    })
public class SharedFileUIActivity extends FileUIActivity {
  /**
   * @throws Exception
   */
  public SharedFileUIActivity() throws Exception {
    super();
  }

  public static final String ACTIVITY_TYPE = "SHARED_FILE_ACTIVITY";

  public ExoSocialActivity getOriginalActivity() {
    Map<String, String> activityTemplateParams = this.getActivity().getTemplateParams();
    if (activityTemplateParams == null || activityTemplateParams.get("originalActivityId") == null) {
      return null;
    }
    return Utils.getActivityManager().getActivity(activityTemplateParams.get("originalActivityId"));
  }

  public Identity getOriginalActivityOwnerIdentity() {
    return getOriginalActivity() != null ? Utils.getIdentityManager().getIdentity(getOriginalActivity().getUserId()) : null;
  }

  public boolean isUserOriginalActivity() {
    boolean isUserActivity = false;
    if (getOriginalActivityOwnerIdentity() != null) {
      isUserActivity = getOriginalActivityOwnerIdentity().getProviderId().equals(OrganizationIdentityProvider.NAME);
    }
    return isUserActivity;
  }

  public boolean isSpaceOriginalActivity() {
    boolean isSpaceActivity = false;
    if (getOriginalActivityOwnerIdentity() != null) {
      isSpaceActivity = getOriginalActivityOwnerIdentity().getProviderId().equals(SpaceIdentityProvider.NAME);
    }
    return isSpaceActivity;
  }

  protected boolean isOriginalActivitySpaceStreamOwner() {
    return getOriginalActivity().getActivityStream().getType().name().equalsIgnoreCase(SpaceIdentityProvider.NAME);
  }

  public boolean isActivityShareable() {
    return false;
  }
}
