/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.gdrive;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Deprecated
public class CredentialUtils {

  protected static final Log                 LOG           = ExoLogger.getLogger(CredentialUtils.class);

  private static GoogleAuthorizationCodeFlow flow          = null;

  private static final List<String>          SCOPES        = Arrays.asList("https://www.googleapis.com/auth/userinfo.email",
                                                                           "https://www.googleapis.com/auth/userinfo.profile",
                                                                           "https://www.googleapis.com/auth/drive.readonly");

  private final String                       CLIENT_ID     = "24067396563.apps.googleusercontent.com";

  private final String                       CLIENT_SECRET = "ZAWuUYVX7xhMFKPUl4H7d6QD";

  private final String                       REDIRECT_URI  = "https://developers.google.com/oauthplayground";

  // CODE2 determined using the https://developers.google.com/oauthplayground.
  // Use client_id=24067396563.apps.googleusercontent.com,
  // client_secret=ZAWuUYVX7xhMFKPUl4H7d6QD and
  // scopes=https://www.googleapis.com/auth/userinfo.email
  // https://www.googleapis.com/auth/userinfo.profile
  // https://www.googleapis.com/auth/drive.readonly

  CredentialUtils() {

  }

  /**
   * Exchange an authorization code for OAuth 2.0 credentials.
   * 
   * @return OAuth 2.0 credentials.
   * @throws CodeExchangeException An error occurred.
   */
  public Credential getCredential(String code) throws IOException {
    GoogleAuthorizationCodeFlow flow = getFlow();
    GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
    return flow.createAndStoreCredential(response, null);
  }

  /**
   * Build an authorization flow and store it as a static class attribute.
   * 
   * @return GoogleAuthorizationCodeFlow instance.
   * @throws IOException Unable to load client_secrets.json.
   */
  public GoogleAuthorizationCodeFlow getFlow() throws IOException {
    if (flow == null) {
      HttpTransport httpTransport = new NetHttpTransport();
      JacksonFactory jsonFactory = new JacksonFactory();

      // (access_type=offline) if application needs to refresh access tokens
      // when the user is not
      // present at the browser.
      // If the value "approval_prompt" is force, then the user sees a
      // consent page even if they have previously given consent to your
      // application for a given set of scopes.
      flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
                                                     jsonFactory,
                                                     CLIENT_ID,
                                                     CLIENT_SECRET,
                                                     SCOPES).setAccessType("offline")
                                                            .setApprovalPrompt("force")
                                                            .build();
    }
    return flow;
  }

}
