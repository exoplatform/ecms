package org.exoplatform.clouddrive.exodrive;

import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.exodrive.ExoDriveConnector.AuthURLBuilder;

public class ExoDriveProvider extends CloudProvider {

  protected final AuthURLBuilder authURLBuilder;

  public ExoDriveProvider(String id, String name, AuthURLBuilder authUrlBuilder) {
    super(id, name);
    this.authURLBuilder = authUrlBuilder;
  }

  /**
   * @inherritDoc
   */
  @Override
  public String getAuthURL() {
    return authURLBuilder.build();
  }

  /**
   * @inherritDoc
   */
  @Override
  public boolean retryOnProviderError() {
    return false;
  }
}
