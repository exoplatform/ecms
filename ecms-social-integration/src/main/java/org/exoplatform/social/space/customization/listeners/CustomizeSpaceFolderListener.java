package org.exoplatform.social.space.customization.listeners;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.exoplatform.social.space.customization.SpaceCustomizationService;

public class CustomizeSpaceFolderListener extends SpaceListenerPlugin {

  private SpaceCustomizationService spaceCustomizationService;

  private static final Log          LOG                         = ExoLogger.getExoLogger(CustomizeSpaceDriveListener.class);

  public CustomizeSpaceFolderListener(SpaceCustomizationService spaceCustomizationService) {
    this.spaceCustomizationService = spaceCustomizationService;
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    String groupId = event.getSpace().getGroupId();
    try {
      spaceCustomizationService.createSpaceDefaultFolders(groupId);
    } catch (Exception e) {
      LOG.error("Can not create default folder for space : " + groupId, e);
    }
  }

}
