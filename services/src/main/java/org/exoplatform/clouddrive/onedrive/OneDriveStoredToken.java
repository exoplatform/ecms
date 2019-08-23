package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.oauth2.UserToken;

class OneDriveStoredToken extends UserToken {
  /**
   * {@inheritDoc}
   */
  public void store(String refreshToken) throws CloudDriveException {
    this.store("", refreshToken, 0);
  }
}
