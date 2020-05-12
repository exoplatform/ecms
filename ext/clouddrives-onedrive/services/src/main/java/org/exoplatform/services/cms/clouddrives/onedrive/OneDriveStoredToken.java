package org.exoplatform.services.cms.clouddrives.onedrive;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.oauth2.UserToken;

class OneDriveStoredToken extends UserToken {
  /**
   * {@inheritDoc}
   */
  public void store(String refreshToken) throws CloudDriveException {
    this.store("", refreshToken, 0);
  }
}
