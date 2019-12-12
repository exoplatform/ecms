/*
* Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;


import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;



/**
 * Created by The eXo Platform SAS
 * Author : Walid Khessairi
 *          wkhessairi@exoplatform.com
 * Aug 11, 2016
 */
@ComponentConfig(
    template =  "war:/groovy/ecm/social-integration/share-document/UIWhoHasAccess.gtmpl"
)
public class UIWhoHasAccess extends UIContainer {

  private static final Log    LOG                 = ExoLogger.getLogger(UIWhoHasAccess.class);
  private static final String SPACE_PREFIX1 = "space::";
  private static final String SPACE_PREFIX2 = "*:/spaces/";
  private static final String GROUP_PREFIX = "*:/";

  public void close() {
    for(UIComponent uicomp : getChildren()) {
      removeChild(UIWhoHasAccessEntry.class);
    }
  }

  public UIWhoHasAccess()  {
  }

  public void init() {
    UIShareDocuments uishareDocuments = getAncestorOfType(UIShareDocuments.class);
    for (String id : uishareDocuments.getAllPermissions().keySet()) {
      try {
        // Mange only Spaces (without memberships) and users
        if (IdentityConstants.ANY.equals(id) || IdentityConstants.SYSTEM.equals(id) || (id.contains(":/") && !isSpace(id))) {
          continue;
        }
        // if id is not a valid user nor a space, ignore it
        if (!isSpace(id)) {
          User user = getApplicationComponent(OrganizationService.class).getUserHandler().findUserByName(id);
          if (user == null) {
            continue;
          }
        }

        UIWhoHasAccessEntry uiWhoHasAccessEntry = getChildById(id);
        if (uiWhoHasAccessEntry == null) {
          uiWhoHasAccessEntry = addChild(UIWhoHasAccessEntry.class, null, id);
        }
        uiWhoHasAccessEntry.init(id, uishareDocuments.getPermission(id));
      } catch (Exception e) {
        LOG.error("Error initializing Share documents permission entry for id = " + id, e);
      }
    }
  }

  public void update(String name, String permission) {
    try {
      if (getChildById(name) == null) addChild(UIWhoHasAccessEntry.class, null, name);
      UIWhoHasAccessEntry uiWhoHasAccessEntry = getChildById(name);
      uiWhoHasAccessEntry.init(name, permission);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  public void removeEntry(String id) {
    try {
      removeChildById(id);
      UIShareDocuments uiShareDocuments = getParent();
      uiShareDocuments.removePermission(id);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  public void updateEntry(String id, String permission) {
    try {
      UIShareDocuments uiShareDocuments = getParent();
      uiShareDocuments.updatePermission(id, permission);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  public String getProfileUrl(String name) {
    return CommonsUtils.getCurrentDomain() + LinkProvider.getProfileUri(name);
  }

  private boolean isSpace(String name) {
    return (name.startsWith(SPACE_PREFIX1) || name.startsWith(SPACE_PREFIX2));
  }

  public String getUserFullName(String name) throws Exception {
    String userFullName = name;
    User user = getApplicationComponent(OrganizationService.class).getUserHandler().findUserByName(name);
    if(user != null) {
      userFullName = user.getDisplayName();
    }
    return userFullName;
  }

  public String getPrettySpaceName(String name) {
    SpaceService spaceService = getApplicationComponent(SpaceService.class);
    if (name.startsWith(SPACE_PREFIX1)) return spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX1.length())).getDisplayName();
    else return spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX2.length())).getDisplayName();
  }

  public String getSpaceUrl(String name) {
    String space;
    if (name.startsWith(SPACE_PREFIX1)) space = name.substring(SPACE_PREFIX1.length());
    else space = name.substring(SPACE_PREFIX2.length());
    return CommonsUtils.getCurrentDomain() + LinkProvider.getSpaceUri(space.replace(" ","_"));
  }

  /**
   * Return the URL of the entry's avatar
   * @param name Entry name
   * @return Entry's avatar URL
   */
  public String getAvatar(String name) {
    try {
      if (isSpace(name)) {
        SpaceService spaceService = getApplicationComponent(SpaceService.class);
        Space space;
        if (name.startsWith(SPACE_PREFIX1))
          space = spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX1.length()));
        else space = spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX2.length()));
        return space.getAvatarUrl() != null ? space.getAvatarUrl() : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
      } else {
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true);
        Profile profile = identity.getProfile();
        return profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
      }
    } catch (Exception e) {
      return LinkProvider.SPACE_DEFAULT_AVATAR_URL;
    }
  }

  /**
   * Return the display name of the entry.
   * @param name Entry name
   * @return Entry's display name
   */
  public String getDisplayName(String name) {
    String displayName = name;
    try {
      if(this.isSpace(name)) {
        displayName = this.getPrettySpaceName(name);
      } else {
        displayName = this.getUserFullName(name);
      }
    } catch (Exception e) {
      LOG.error("Cannot display name for entry " + name + " : " + e.getMessage(), e);
    }
    return displayName;
  }

  /**
   * Return the entry URL
   * @param name Entry name
   * @return Entry's URL
   */
  public String getEntryURL(String name) {
    String url = null;
    try {
      if(this.isSpace(name)) {
        url = this.getSpaceUrl(name);
      } else {
        url = this.getProfileUrl(name);
      }
    } catch (Exception e) {
      LOG.error("Cannot URL for entry " + name + " : " + e.getMessage(), e);
    }
    return url;
  }

  public String getPrettyGroupName(String name) {
    return name.split(":")[1];
  }
}