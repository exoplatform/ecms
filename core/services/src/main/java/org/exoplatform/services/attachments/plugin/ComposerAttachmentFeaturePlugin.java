
package org.exoplatform.services.attachments.plugin;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.api.settings.FeaturePlugin;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * A plugin added to {@link ExoFeatureService} determines whether we should
 * notify the user or not by the new way of attaching a file in the activity
 * composer
 */

public class ComposerAttachmentFeaturePlugin extends FeaturePlugin {

  private static final String   COMPOSER_ATTACH_FILE_FEATURE_NAME = "activityComposerAttachFile";

  public static final Scope     APP_SCOPE                         = Scope.APPLICATION.id("changesReminder");

  private final SettingService  settingService;

  private final IdentityManager identityManager;

  public ComposerAttachmentFeaturePlugin(SettingService settingService, IdentityManager identityManager) {
    this.settingService = settingService;
    this.identityManager = identityManager;
  }

  @Override
  public String getName() {
    return COMPOSER_ATTACH_FILE_FEATURE_NAME;
  }

  @Override
  public boolean isFeatureActiveForUser(String featureName, String username) {
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username);
    Profile userProfile = userIdentity.getProfile();

    SettingValue<?> settingValue = settingService.get(Context.GLOBAL,
                                                      Scope.GLOBAL.id(null),
                                                      (COMPOSER_ATTACH_FILE_FEATURE_NAME + "CreationDate"));
    SettingValue<?> userSettingValue = settingService.get(Context.USER.id(userIdentity.getRemoteId()), APP_SCOPE, featureName);

    return (userProfile.getCreatedTime() <= Long.parseLong(settingValue.getValue().toString())) && userSettingValue == null;
  }

  public void init() {
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL,
                                                      Scope.GLOBAL.id(null),
                                                      (COMPOSER_ATTACH_FILE_FEATURE_NAME + "CreationDate"));
    if (settingValue == null) {
      settingService.set(Context.GLOBAL,
                         Scope.GLOBAL.id(null),
                         (COMPOSER_ATTACH_FILE_FEATURE_NAME + "CreationDate"),
                         SettingValue.create(System.currentTimeMillis()));
    }
  }
}
