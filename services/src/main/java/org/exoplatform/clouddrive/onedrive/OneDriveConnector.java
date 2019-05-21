package org.exoplatform.clouddrive.onedrive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
    String authUrl = "";
    try {
      authUrl = new String(Files.readAllBytes(new File(System.getProperty("user.home") + "/authurl.txt").toPath()), Charset.forName("UTF-8"));
    } catch (IOException e) {
      authUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?\n" +
              "client_id=9920cb10-7801-49d8-9a75-2d8252eae87c\n" +
              "&response_type=code\n" +
              "&redirect_uri=http://localhost:8080/portal/rest/clouddrive/connect/onedrive\n" +
              "&response_mode=query\n" +
              "&scope=https://graph.microsoft.com/Files.Read.All https://graph.microsoft.com/Files.Read https://graph.microsoft.com/Files.Read.Selected https://graph.microsoft.com/Files.ReadWrite https://graph.microsoft.com/Files.ReadWrite.All https://graph.microsoft.com/Files.ReadWrite.AppFolder https://graph.microsoft.com/Files.ReadWrite.Selected https://graph.microsoft.com/User.Read https://graph.microsoft.com/User.ReadWrite https://graph.microsoft.com/User.ReadWrite offline_access https://graph.microsoft.com/User.ReadWrite.All\n" +
              "&state=1233333333";

      // Directory.AccessAsUser.All
    }
    return new OneDriveProvider(getProviderId(), getProviderName(), authUrl);
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
                OneDriveUser user = new OneDriveUser(driveAPIUser.id, driveAPIUser.userPrincipalName, driveAPIUser.userPrincipalName, provider, driveAPI);
                return user;
            } else {
                return null;
            }
        } else {
            throw new CloudDriveException("Access code should not be null or empty");
        }

//    LOG.info("authenticate: " + params.toString());
//    return new OneDriveUser("id-user1", "username", "some1@email.com", provider, new OneDriveAPI());
    }

  @Override
  protected JCRLocalOneDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    LOG.info("createDrive");
      if (LOG.isDebugEnabled()) {
          LOG.debug("createDrive() User: id = "  + user.getId() + " email = " + user.getEmail() + " username = " + user.getUsername());
      }
//      driveNode.getSession().
    return new JCRLocalOneDrive(user, driveNode, sessionProviders, jcrFinder, mimeTypes);
  }
  @Override
  protected OneDriveProvider getProvider() {
    return (OneDriveProvider) super.getProvider();
  }

  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    LOG.info("loadDrive");
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
      try {
          return new JCRLocalOneDrive(new API(), getProvider(), driveNode, sessionProviders, jcrFinder, mimeTypes);
      } catch (IOException e) {
          e.printStackTrace();
      }

//    return new JCRLocalOneDrive(new OneDriveUser("id-user1", "username", "some1@email.com", createProvider(), new OneDriveAPI()),
//                                driveNode,
//                                sessionProviders,
//                                jcrFinder,
//                                mimeTypes);
      return null;
  }
}
