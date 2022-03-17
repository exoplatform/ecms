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

  @Override
  public void applicationActivated(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {
  }

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {
  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {
  }

  @Override
  public void left(SpaceLifeCycleEvent event) {
  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceDescriptionEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {

  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceBannerEdited(SpaceLifeCycleEvent event) {

  }

}
