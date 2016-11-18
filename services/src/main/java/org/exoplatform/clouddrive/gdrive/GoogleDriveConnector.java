/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.gdrive;

import com.google.api.services.oauth2.model.Userinfoplus;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.ConfigurationException;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * GoogleDrive connector implementation. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleDriveConnector.java 00000 Sep 13, 2012 pnedonosko $
 */
public class GoogleDriveConnector extends CloudDriveConnector {

  /**
   * API builder.
   */
  class API {
    String code;

    String refreshToken, accessToken;

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
     * @param userId
     * @param refreshToken
     * @param accessToken
     * @param expirationTime
     * @return this API
     */
    API load(String userId, String refreshToken, String accessToken, long expirationTime) {
      this.refreshToken = refreshToken;
      this.accessToken = accessToken;
      this.expirationTime = expirationTime;
      return this;
    }

    /**
     * Build API.
     * 
     * @return {@link GoogleDriveAPI}
     * @throws GoogleDriveException if error happen during communication with Google Drive services
     * @throws CloudDriveException if cannot load local tokens
     */
    GoogleDriveAPI build() throws GoogleDriveException, CloudDriveException {
      if (code != null && code.length() > 0) {
        // build API based on OAuth2 code
        return new GoogleDriveAPI(getClientId(), getClientSecret(), code, getProvider().getRedirectURL());
      } else {
        // build API based on locally stored tokens
        return new GoogleDriveAPI(getClientId(), getClientSecret(), accessToken, refreshToken, expirationTime);
      }
    }
  }

  /**
   * GoogleDrive connector plugin constructor.
   * 
   * @param {@link InitParams} params
   * @throws ConfigurationException
   */
  public GoogleDriveConnector(RepositoryService jcrService,
                              SessionProviderService sessionProviders,
                              NodeFinder finder,
                              ExtendedMimeTypeResolver mimeTypes,
                              InitParams params) throws ConfigurationException {
    super(jcrService, sessionProviders, finder, mimeTypes, params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudProvider createProvider() {
    String redirectURL = redirectLink();

    StringBuilder authURL = new StringBuilder();
    authURL.append("https://accounts.google.com/o/oauth2/auth?");
    authURL.append("response_type=code&client_id=");
    String clientId = getClientId();
    try {
      authURL.append(URLEncoder.encode(clientId, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot encode client id " + clientId + ":" + e);
      authURL.append(clientId);
    }
    authURL.append("&approval_prompt=");
    // TODO in case of SSO - don't force the approval?
    authURL.append(GoogleDriveAPI.APPOVAl_PROMT);
    authURL.append("&scope=");
    try {
      // TODO in case of SSO add openid scope?
      authURL.append(URLEncoder.encode(GoogleDriveAPI.SCOPES_STRING, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot encode scopes " + GoogleDriveAPI.SCOPES_STRING + ":" + e);
      authURL.append(GoogleDriveAPI.SCOPES_STRING);
    }
    authURL.append("&access_type=");
    authURL.append(GoogleDriveAPI.ACCESS_TYPE);
    authURL.append("&state=");
    try {
      authURL.append(URLEncoder.encode(CloudProvider.AUTH_NOSTATE, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot encode state " + CloudProvider.AUTH_NOSTATE + ":" + e);
      authURL.append(CloudProvider.AUTH_NOSTATE);
    }
    authURL.append("&redirect_uri=");
    try {
      authURL.append(URLEncoder.encode(redirectURL.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Cannot encode redirect URL " + redirectURL.toString() + ":" + e);
      authURL.append(redirectURL);
    }

    return new GoogleProvider(getProviderId(), getProviderName(), authURL.toString(), redirectURL.toString(), jcrService);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected GoogleProvider getProvider() {
    return (GoogleProvider) super.getProvider();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GoogleUser authenticate(Map<String, String> params) throws CloudDriveException {
    String code = params.get(OAUTH2_CODE);
    if (code != null && code.length() > 0) {
      GoogleDriveAPI driveAPI = new API().auth(code).build();
      Userinfoplus userInfo = driveAPI.userInfo();
      GoogleUser user = new GoogleUser(userInfo.getId(), userInfo.getName(), userInfo.getEmail(), provider, driveAPI);
      return user;
    } else {
      throw new CloudDriveException("Access code should not be null or empty");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected JCRLocalGoogleDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    if (user instanceof GoogleUser) {
      return new JCRLocalGoogleDrive((GoogleUser) user, driveNode, sessionProviders, jcrFinder, mimeTypes);
    } else {
      throw new CloudDriveException("Not Google user: " + user);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
    return new JCRLocalGoogleDrive(new API(), getProvider(), driveNode, sessionProviders, jcrFinder, mimeTypes);
  }

}
