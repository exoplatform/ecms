package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;

public class OneDriveUser extends CloudUser {
  /**
   * Instantiates a new cloud user.
   *
   * @param id the id
   * @param username {@link String}
   * @param email {@link String}
   * @param provider the provider
   */
  public OneDriveUser(String id, String username, String email, CloudProvider provider) {
    super(id, username, email, provider);
  }
}
