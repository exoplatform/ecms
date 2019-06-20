package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OneDriveProvider extends CloudProvider {
  protected static final Log LOG = ExoLogger.getLogger(OneDriveProvider.class);
  private final String authUrl;
  private final String redirectUrl;

  /**
   * Instantiates a new cloud provider.
   *
   * @param id the id
   * @param name the name
   */
  protected OneDriveProvider(String id, String name, String authUrl, String redirectUrl) {
    super(id, name);
    this.authUrl = authUrl;
    this.redirectUrl = redirectUrl;
  }

  @Override
  public String getAuthURL() throws CloudDriveException {
    return this.authUrl;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  @Override
  public boolean retryOnProviderError() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("retryOnProviderError()");
    }
    return false;
  }
}
