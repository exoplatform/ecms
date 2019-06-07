package org.exoplatform.clouddrive.onedrive;

import static org.exoplatform.clouddrive.onedrive.OneDriveAPI.SCOPES;

import java.io.IOException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.microsoft.graph.models.extensions.User;

import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
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
                           InitParams params)
      throws ConfigurationException {
    super(jcrService, sessionProviders, finder, mimeTypes, params);
    if (LOG.isDebugEnabled()) {
      LOG.debug("OneDriveConnector():  ");
    }
  }

  class API {

    /** The access token. */
    String code, refreshToken, accessToken;

    /** The expiration time. */
    long   expirationTime;

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

    OneDriveAPI build() throws CloudDriveException, IOException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("OneDriveAPI build():  ");
      }
      if (code != null && code.length() > 0) {
        return new OneDriveAPI(getClientId(), getClientSecret(), code);
      } else {
        return new OneDriveAPI(getClientId(), getClientSecret(), accessToken, refreshToken, expirationTime);
      }
    }
  }

  @Override
  protected CloudProvider createProvider() throws ConfigurationException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("createProvider():  ");
    }
    StringBuilder authUrl = new StringBuilder("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?");
    authUrl.append("response_type=code")
           .append("&redirect_uri=")
           .append(getConnectorSchema())
           .append("://")
           .append(getConnectorHost())
           .append("/portal/rest/clouddrive/connect/onedrive")
           .append("&response_mode=query")
           .append("&client_id=")
           .append(getClientId())
           .append("&scope=")
           .append(SCOPES);

    return new OneDriveProvider(getProviderId(), getProviderName(), authUrl.toString());
  }

  @Override
  protected CloudUser authenticate(Map<String, String> params) throws CloudDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("authentificate():  ");
    }
    String code = params.get(OAUTH2_CODE);
    if (code != null && code.length() > 0) {
      OneDriveAPI driveAPI = null;
      try {
        driveAPI = new API().auth(code).build();
      } catch (IOException e) {
        throw new CloudDriveException("Unnable to build OneDriveAPI");
      }
      User driveAPIUser = driveAPI.getUser();
      if (driveAPIUser != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("driveAPIUSER!=null:  ");
        }

          String userId = driveAPIUser.id;
          String username = driveAPIUser.userPrincipalName;
          String email = driveAPIUser.userPrincipalName;
        if (LOG.isDebugEnabled()) {
          LOG.debug("userId: " + userId + " username " + username);
        }
        OneDriveUser user = new OneDriveUser(userId, username, email, provider, driveAPI);
          return user;
      } else {
        return null;
      }
    } else {
      throw new CloudDriveException("Access code should not be null or empty");
    }
  }

  @Override
  protected JCRLocalOneDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("createDrive");
      LOG.debug("createDrive() User: id = " + user.getId() + " email = " + user.getEmail() + " username = " + user.getUsername());
    }
    return new JCRLocalOneDrive(user, driveNode, sessionProviders, jcrFinder, mimeTypes);
  }

  @Override
  protected OneDriveProvider getProvider() {
    return (OneDriveProvider) super.getProvider();
  }

  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("loadDrive");
    }
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
    try {
      return new JCRLocalOneDrive(new API(), getProvider(), driveNode, sessionProviders, jcrFinder, mimeTypes);
    } catch (IOException e) {
      LOG.error("unable to create LocalOneDrive", e);

    }
    return null;
  }
}
