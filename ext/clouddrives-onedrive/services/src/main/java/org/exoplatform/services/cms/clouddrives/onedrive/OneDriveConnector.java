package org.exoplatform.services.cms.clouddrives.onedrive;

import java.io.IOException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.microsoft.graph.models.extensions.User;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.clouddrives.*;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.clouddrives.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OneDriveConnector extends CloudDriveConnector {
  protected static final Log LOG = ExoLogger.getLogger(OneDriveConnector.class);

  public OneDriveConnector(RepositoryService jcrService,
                           SessionProviderService sessionProviders,
                           NodeFinder finder,
                           ExtendedMimeTypeResolver mimeTypes,
                           InitParams params) throws ConfigurationException {
    super(jcrService, sessionProviders, finder, mimeTypes, params);
  }

  class API {

    /** The access token. */
    String code, refreshToken, accessToken;

    /** The expiration time. */
    long   expirationTime;

    /** The server url. */
    String serverUrl;

    /**
     * Authenticate to the API with OAuth2 code returned on callback url.
     *
     * @param code String
     * @return this API
     */
    API auth(String code) {
      this.code = code;
      return this;
    }

    /**
     * Authenticate to the API with locally stored tokens.
     *
     * @param refreshToken the refresh token
     * @param accessToken the access token
     * @param expirationTime the expiration time
     * @return this API
     */
    API load(String refreshToken, String accessToken, long expirationTime) {
      this.refreshToken = refreshToken;
      this.accessToken = accessToken;
      this.expirationTime = expirationTime;
      return this;
    }

    API serverUrl(String serverUrl){
      this.serverUrl = serverUrl;
      return this;
    }

    OneDriveAPI build() throws CloudDriveException, IOException {
      if (code != null && code.length() > 0) {
        return new OneDriveAPI(getClientId(), getClientSecret(), code, serverUrl + "/portal/rest/clouddrive/connect/onedrive" );
      } else {
        return new OneDriveAPI(getClientId(), getClientSecret(), accessToken, refreshToken, expirationTime, getConnectorSchema() + "://" + getConnectorHost() + "/portal/rest/clouddrive/connect/onedrive");
      }
    }
  }

  @Override
  protected CloudProvider createProvider() throws ConfigurationException {
    StringBuilder authUrl = new StringBuilder("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?");
    authUrl.append("response_type=code")
           .append("&redirect_uri=")
           .append(getConnectorSchema())
           .append("://")
           .append(getConnectorHost())
           .append("/portal/rest/clouddrive/connect/onedrive")
           .append("&response_mode=query")
           .append("&prompt=select_account")
           .append("&client_id=")
           .append(getClientId())
           .append("&scope=")
           .append(OneDriveAPI.SCOPES);

    return new OneDriveProvider(getProviderId(), getProviderName(), authUrl.toString(), getConnectorSchema() + "://" + getConnectorHost() + "/portal/rest/clouddrive/connect/onedrive");
  }

  @Override
  protected CloudUser authenticate(Map<String, String> params) throws CloudDriveException {
    String code = params.get(OAUTH2_CODE);
    String serverUrl = params.get(OAUTH2_SERVER_URL);
    if (code != null && code.length() > 0) {
      OneDriveAPI driveAPI;
      try {
        driveAPI = new API().auth(code).serverUrl(serverUrl).build();
      } catch (IOException e) {
        throw new CloudDriveException("Unnable to build OneDriveAPI",e);
      }
      User driveAPIUser = driveAPI.getUser();
      if (driveAPIUser != null) {
        String userId = driveAPIUser.id;
        String username = driveAPIUser.userPrincipalName;
        String email = driveAPIUser.userPrincipalName;
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Authenticate user: {}[{}]", userId, username);
        }
        OneDriveUser user = new OneDriveUser(userId, username, email, provider, driveAPI);
        return user;
      } else {
        throw new CloudDriveException("API user cannot be found to authenticate with " + code);
      }
    } else {
      throw new CloudDriveException("Access code should not be null or empty");
    }
  }

  @Override
  protected JCRLocalOneDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> createDrive user: {}[{}, {}] ", user.getId(), user.getEmail(), user.getUsername());
    }
    return new JCRLocalOneDrive(user, driveNode, sessionProviders, jcrFinder, mimeTypes);
  }

  @Override
  protected OneDriveProvider getProvider() {
    return (OneDriveProvider) super.getProvider();
  }

  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
    try {
      return new JCRLocalOneDrive(new API(), getProvider(), driveNode, sessionProviders, jcrFinder, mimeTypes);
    } catch (IOException e) {
      if (LOG.isDebugEnabled()) {
        // At debug level show more info (a node)
        LOG.debug("Unable load locally connected drive at node {}", driveNode, e);
      }
      throw new CloudDriveException("Unable load locally connected drive");
    }
  }
}
