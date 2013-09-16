
package org.exoplatform.clouddrive.box;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BoxConnector.java 00000 Aug 30, 2013 pnedonosko $
 * 
 */
public class BoxConnector extends CloudDriveConnector {

  /**
   * Box API builder (code grabbed from GoogleDriveConnector, 30 Aug 2013).
   */
  class API {
    String code, refreshToken, accessToken;

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
     * @param refreshToken
     * @param accessToken
     * @param expirationTime
     * @return this API
     */
    API load(String refreshToken, String accessToken, long expirationTime) {
      this.refreshToken = refreshToken;
      this.accessToken = accessToken;
      this.expirationTime = expirationTime;
      return this;
    }

    /**
     * Build API.
     * 
     * @return {@link BoxAPI}
     * @throws BoxException if error happen during communication with Google Drive services
     * @throws CloudDriveException if cannot load local tokens
     */
    BoxAPI build() throws BoxException, CloudDriveException {
      if (code != null && code.length() > 0) {
        // build API based on OAuth2 code
        return new BoxAPI(getClientId(), getClientSecret(), code, getProvider().getRedirectUrl());
      } else {
        // build API based on locally stored tokens
        return new BoxAPI(getClientId(),
                                  getClientSecret(),
                                  accessToken,
                                  refreshToken,
                                  expirationTime);
      }
    }
  }

  
  public BoxConnector(RepositoryService jcrService,
                           SessionProviderService sessionProviders,
                           InitParams params) throws ConfigurationException {
    super(jcrService, sessionProviders, params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected BoxProvider getProvider() {
    return (BoxProvider) super.getProvider();
  }
  
  @Override
  protected CloudProvider createProvider() {
    StringBuilder redirectUrl = new StringBuilder();
    redirectUrl.append("http://");
    redirectUrl.append(getConnectorHost());
    redirectUrl.append("/portal/rest/clouddrive/connect/");
    redirectUrl.append(getProviderId());

    StringBuilder authUrl = new StringBuilder();
    authUrl.append("https://www.box.com/api/oauth2/authorize?");
    authUrl.append("response_type=code&client_id=");
    authUrl.append(getClientId());
    authUrl.append("&state=");
    authUrl.append(BoxAPI.NO_STATE);
    authUrl.append("&redirect_uri=");
    authUrl.append(redirectUrl);

    return new BoxProvider(getProviderId(), getProviderName(), authUrl.toString(), redirectUrl.toString(), jcrService);
  }

  @Override
  protected CloudUser authenticate(String code) throws CloudDriveException {
    if (code != null && code.length() > 0) {
      BoxAPI driveAPI = new API().auth(code).build();
      com.box.boxjavalibv2.dao.BoxUser buser = driveAPI.currentUser();
      BoxUser user = new BoxUser(buser.getId(),
                                       buser.getName(),
                                       buser.getLogin(),
                                       provider,
                                       driveAPI);
      return user;
    } else {
      throw new CloudDriveException("Access key should not be null or empty");
    }
  }

  @Override
  protected CloudDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException,
                                                                  RepositoryException {
    if (user instanceof BoxUser) {
      BoxUser boxUser = (BoxUser) user;
      JCRLocalBoxDrive drive = new JCRLocalBoxDrive(boxUser, driveNode, sessionProviders);
      boxUser.api().getToken().addListener(drive);
      return drive;
    } else {
      throw new CloudDriveException("Not Box user: " + user);
    }
  }

  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    JCRLocalBoxDrive drive = new JCRLocalBoxDrive(new API(), getProvider(), driveNode, sessionProviders);
    drive.getUser().api().getToken().addListener(drive);
    return drive;
  }

}
