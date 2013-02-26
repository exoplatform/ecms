package org.exoplatform.clouddrive.exodrive;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.exodrive.ExoDriveConnector.AuthUrlBuilder;

public class ExoDriveProvider extends CloudProvider {

  protected final AuthUrlBuilder authUrlBuilder;

  public ExoDriveProvider(String id, String name, AuthUrlBuilder authUrlBuilder) {
    super(id, name);
    this.authUrlBuilder = authUrlBuilder;
  }

  /**
   * @inherritDoc
   */
  @Override
  public String getAuthUrl() {
    return authUrlBuilder.build();
  }

  /**
   * @inherritDoc
   */
  @Override
  public boolean retryOnProviderError() {
    // TODO
    return false;
  }
}
