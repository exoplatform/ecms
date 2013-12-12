/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.clouddrive.googledrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.google.api.services.oauth2.model.Userinfo;

import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Covers calls to Google Drive services and handles related exceptions. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleDriveAPI.java 00000 Jan 5, 2013 pnedonosko $
 */
class GoogleDriveAPI {

  public static final String       FOLDER_MIMETYPE = "application/vnd.google-apps.folder";

  public static final List<String> SCOPES          = Arrays.asList(DriveScopes.DRIVE_READONLY,
                                                                   Oauth2Scopes.USERINFO_EMAIL,
                                                                   Oauth2Scopes.USERINFO_PROFILE);

  public static final String       SCOPES_STRING   = scopes();

  public static final String       ACCESS_TYPE     = "offline";

  public static final String       APPOVAl_PROMT   = "force";

  public static final String       NO_STATE        = "__no_state_set__";

  protected static final Log       LOG             = ExoLogger.getLogger(GoogleDriveAPI.class);

  /**
   * Single user credentials store. Should be used per session as transient store.
   */
  class UserCredentialStore implements CredentialStore {

    class Tokens {
      final String accessToken;

      final String refreshToken;

      final long   expirationTime;

      Tokens(String accessToken, String refreshToken, long expirationTime) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
      }
    }

    final Map<String, Tokens> store = new ConcurrentHashMap<String, Tokens>();

    /**
     * Create store with given user and his/her credentials.
     * 
     * @param id
     * @param accessToken
     * @param refreshToken
     * @param expirationTime
     */
    UserCredentialStore(String id, String accessToken, String refreshToken, long expirationTime) {
      store.put(id, new Tokens(accessToken, refreshToken, expirationTime));
    }

    /**
     * Create empty store.
     * 
     * @param id
     */
    UserCredentialStore() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(String userId, Credential credential) throws IOException {
      Tokens uc = store.get(userId);
      if (uc != null) {
        credential.setAccessToken(uc.accessToken);
        credential.setRefreshToken(uc.refreshToken);
        credential.setExpirationTimeMilliseconds(uc.expirationTime);
        return true;
      }
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store(String userId, Credential credential) throws IOException {
      store.put(userId,
                new Tokens(credential.getAccessToken(),
                           credential.getRefreshToken(),
                           credential.getExpirationTimeMilliseconds()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String userId, Credential credential) throws IOException {
      store.remove(userId);
    }
  }

  /**
   * Iterator over whole set of items from Google Drive service. This iterator hides next-page-token logic on
   * request to the service. <br>
   * Iterator methods can throw {@link GoogleDriveException} in case of remote or communication errors.
   */
  @Deprecated
  abstract class PageIterator<C> {

    Iterator<C>  iter;

    C            next;

    /**
     * Forecast of available items in the iterator. Calculated on each {@link #nextPage()}. Used for progress
     * indicator.
     */
    volatile int available;

    /**
     * Totally fetched items. Changes on each {@link #next()}. Used for progress indicator.
     */
    volatile int fetched;

    abstract Iterator<C> nextPage() throws GoogleDriveException;

    abstract boolean hasNextPage();

    boolean hasNext() throws GoogleDriveException {
      if (next == null) {
        if (iter.hasNext()) {
          next = iter.next();
        } else {
          // try to fetch next portion of changes
          while (hasNextPage()) {
            iter = nextPage();
            if (iter.hasNext()) {
              next = iter.next();
              break;
            }
          }
        }
        return next != null;
      } else {
        return true;
      }
    }

    C next() throws NoSuchElementException {
      if (next == null) {
        throw new NoSuchElementException("No more data on the Google Drive");
      } else {
        C c = next;
        next = null;
        fetched++;
        return c;
      }
    }

    /**
     * Calculate a forecast of items available to fetch. Call it on each {@link #nextPage()}.
     * 
     * @param newValue int
     */
    void available(int newValue) {
      if (available == 0) {
        // magic here as we're in indeterminate progress during the fetching
        // logic based on page bundles we're getting from the drive
        // first page it's 100%, assume the second is filled on 25%
        available = hasNextPage() ? Math.round(newValue * 1.25f) : newValue;
      } else {
        // All previously set newValue was fetched.
        // Assuming the next page is filled on 25%.
        int newFetched = available;
        available += hasNextPage() ? Math.round(newValue * 1.25f) : newValue;
        fetched = newFetched;
      }
    }
  }

  class ChildIterator extends ChunkIterator<ChildReference> {
    final Children.List request;

    /**
     * @throws GoogleDriveException
     */
    ChildIterator(String fileId) throws GoogleDriveException {
      try {
        this.request = drive.children().list(fileId);
      } catch (IOException e) {
        throw new GoogleDriveException("Error creating request to Children.List service: " + e.getMessage(),
                                       e);
      }

      // fetch first page
      iter = nextChunk();
    }

    @Override
    protected Iterator<ChildReference> nextChunk() throws GoogleDriveException {
      try {
        ChildList children = request.execute();
        request.setPageToken(children.getNextPageToken());
        List<ChildReference> items = children.getItems();

        available(items.size());

        return items.iterator();
      } catch (IOException e) {
        throw new GoogleDriveException("Error requesting Children.List service: " + e.getMessage(), e);
      }
    }

    @Override
    protected boolean hasNextChunk() {
      return request.getPageToken() != null && request.getPageToken().length() > 0;
    }
  }

  class ChangesIterator extends ChunkIterator<Change> {
    final Changes.List request;

    /**
     * @throws GoogleDriveException
     */
    ChangesIterator(BigInteger startChangeId) throws GoogleDriveException {
      try {
        this.request = drive.changes().list();
        this.request.setStartChangeId(startChangeId);
      } catch (IOException e) {
        throw new GoogleDriveException("Error creating request to Changes.List service: " + e.getMessage(), e);
      }

      // fetch first page
      iter = nextChunk();
    }

    @Override
    protected Iterator<Change> nextChunk() throws GoogleDriveException {
      try {
        ChangeList children = request.execute();
        request.setPageToken(children.getNextPageToken());
        List<Change> items = children.getItems();

        available(items.size());

        return items.iterator();
      } catch (IOException e) {
        throw new GoogleDriveException("Error requesting Children.List service: " + e.getMessage(), e);
      }
    }

    @Override
    protected boolean hasNextChunk() {
      return request.getPageToken() != null && request.getPageToken().length() > 0;
    }
  }

  /**
   * XXX Not used!<br>
   * Use of this class in builder.setHttpRequestInitializer(new RequestInitializer() causes OAuth2 401
   * Unauthorized on Google service
   */
  @Deprecated
  class RequestInitializer implements HttpRequestInitializer {
    @Override
    public void initialize(HttpRequest request) throws IOException {
      // enable re-try on IOException
      request.setRetryOnExecuteIOException(true);
      request.setNumberOfRetries(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPTS);
      request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
        @Override
        public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
          // TODO check here for Backend error or others what we could only re-try

          if (supportsRetry) {
            // wait a bit before next attempt
            try {
              Thread.sleep(CloudDriveConnector.PROVIDER_REQUEST_ATTEMPT_TIMEOUT);
            } catch (InterruptedException e) {
              LOG.warn("Interrupted while waiting for a next attempt of drive operation: " + e.getMessage());
              Thread.currentThread().interrupt();
            }
          }
          return supportsRetry; // re-try all what currently supported
        }
      });
    }
  }

  /**
   * Credentials for request authentication.
   */
  protected final Credential credential;

  /**
   * Drive services API.
   */
  protected final Drive      drive;

  /**
   * User info API.
   */
  // TODO cleanup
  protected final Oauth2     oauth2;

  /**
   * Create Google Drive API from OAuth2 authentication code.
   * 
   * @param clientId {@link String}
   * @param clientSecret {@link String}
   * @param authCode {@link String}
   * @throws GoogleDriveException if authentication failed for any reason.
   * @throws CloudDriveException if credentials store exception happen
   */
  GoogleDriveAPI(String clientId, String clientSecret, String authCode, String redirectUri) throws GoogleDriveException,
      CloudDriveException {

    GoogleAuthorizationCodeFlow authFlow = createFlow(clientId, clientSecret, new UserCredentialStore());
    GoogleTokenResponse response;
    try {
      // Exchange an authorization code for OAuth 2.0 credentials.
      response = authFlow.newTokenRequest(authCode).setRedirectUri(redirectUri).execute();
    } catch (IOException e) {
      throw new GoogleDriveException("Error authenticating user code: " + e.getMessage(), e);
    }

    try {
      this.credential = authFlow.createAndStoreCredential(response, authCode); // authCode as userId
      // this.credential = authFlow.loadCredential(authCode);
    } catch (IOException e) {
      throw new CloudDriveException("Error storing user credential: " + e.getMessage(), e);
    }

    // XXX .setHttpRequestInitializer(new RequestInitializer() this causes OAuth2 401 Unauthorized
    this.drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), this.credential).build();
    this.oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), this.credential).build();
  }

  /**
   * Create Google Drive API from existing user credentials.
   * 
   * @param clientId {@link String}
   * @param clientSecret {@link String}
   * @param userId {@link String}
   * @param accessToken {@link String}
   * @param refreshToken {@link String}
   * @param expirationTime long, token expiration time on milliseconds
   * @throws CloudDriveException if credentials store exception happen
   */
  GoogleDriveAPI(String clientId,
                 String clientSecret,
                 String userId,
                 String accessToken,
                 String refreshToken,
                 long expirationTime) throws CloudDriveException {
    GoogleAuthorizationCodeFlow authFlow = createFlow(clientId,
                                                      clientSecret,
                                                      new UserCredentialStore(userId,
                                                                              accessToken,
                                                                              refreshToken,
                                                                              expirationTime));

    try {
      this.credential = authFlow.loadCredential(userId);
    } catch (IOException e) {
      throw new CloudDriveException("Error loading Google user credentials: " + e.getMessage(), e);
    }

    // XXX .setHttpRequestInitializer(new RequestInitializer() this causes OAuth2 401 Unauthorized
    this.drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
    this.oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
  }

  private static String scopes() {
    StringBuilder s = new StringBuilder();
    for (String scope : SCOPES) {
      s.append(scope);
      s.append('+');
    }
    return s.toString();
  }

  /**
   * Build an authorization flow optionally using provided {@link CredentialStore}, then store it as a static
   * class attribute.
   * 
   * @param clientId
   * @param clientSecret
   * @param tokenStore
   * @return GoogleAuthorizationCodeFlow instance.
   */
  protected GoogleAuthorizationCodeFlow createFlow(String clientId,
                                                   String clientSecret,
                                                   CredentialStore tokenStore) {
    HttpTransport httpTransport = new NetHttpTransport();
    JacksonFactory jsonFactory = new JacksonFactory();
    // (access_type=offline) if application needs to refresh access tokens
    // when the user is not present at the browser.
    // If the value "approval_prompt" is force, then the user sees a
    // consent page even if they have previously given consent to your
    // application for a given set of scopes.
    // was setApprovalPrompt("force")

    GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
                                                                                          jsonFactory,
                                                                                          clientId,
                                                                                          clientSecret,
                                                                                          SCOPES);

    builder.setAccessType(ACCESS_TYPE).setApprovalPrompt(APPOVAl_PROMT);
    if (tokenStore != null) {
      builder.setCredentialStore(tokenStore);
    }

    return builder.build();
  }

  /**
   * Update to new refresh token.
   * 
   * @param refreshToken String with new token
   */
  void setRefreshToken(String refreshToken) {
    credential.setRefreshToken(refreshToken);
  }

  /**
   * Return Userinfo service.
   * 
   * @return {@link Userinfo}
   * @throws GoogleDriveException on error from OAuth2 service
   * @throws CloudDriveException if no Userinfo found
   */
  Userinfo userInfo() throws GoogleDriveException, CloudDriveException {
    Userinfo userInfo;
    // this.userInfo = null; // consume it once
    // if (userInfo == null) {
    try {
      // TODO this caused Unauthorized: >>> .setHttpRequestInitializer(new RequestInitializer()
      // Oauth2 oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(),
      // credential).build();
      userInfo = oauth2.userinfo().get().execute();
    } catch (GoogleJsonResponseException e) {
      // TODO handle other methods' errors the same way

      GoogleJsonError error = e.getDetails();
      // System.err.println('Error code: ' + error.getCode());
      // System.err.println('Error message: ' + error.getMessage());
      // More error information can be retrieved with error.getErrors().
      throw new GoogleDriveException("Authentication error: " + error.getMessage() + " (" + error.getCode()
          + ").", e);
    } catch (HttpResponseException e) {
      // No Json body was returned by the API.
      // System.err.println('HTTP Status code: ' + e.getStatusCode());
      // System.err.println('HTTP Reason: ' + error.getMessage());
      throw new GoogleDriveException("Authentication error: " + e.getMessage() + " (" + e.getStatusCode()
          + ").", e);
    } catch (IOException e) {
      throw new GoogleDriveException("Authentication error: " + e.getMessage(), e);
    }
    // }
    if (userInfo != null && userInfo.getId() != null) {
      return userInfo;
    } else {
      throw new CloudDriveException("User ID cannot be retrieved.");
    }
  }

  /**
   * Return About service.
   * 
   * @return {@link About}
   * @throws GoogleDriveException
   */
  About about() throws GoogleDriveException {
    try {
      return drive.about().get().execute();
    } catch (IOException e) {
      throw new GoogleDriveException("Error requesting About service: " + e.getMessage(), e);
    }
  }

  ChildIterator children(String fileId) throws GoogleDriveException {
    return new ChildIterator(fileId);
  }

  ChangesIterator changes(BigInteger startChangeId) throws GoogleDriveException {
    return new ChangesIterator(startChangeId);
  }

  /**
   * Read file from Files service.
   * 
   * @param fileId {@link String}
   * @return {@link File}
   * @throws GoogleDriveException
   */
  File file(String fileId) throws GoogleDriveException {
    try {
      return drive.files().get(fileId).execute();
    } catch (IOException e) {
      throw new GoogleDriveException("Error requesting Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Check credentials isn't expired and refresh them if required.
   * 
   * @throws GoogleDriveException if error during communication with the provider
   */
  void checkAccess() throws GoogleDriveException {
    Long expirationTime = credential.getExpiresInSeconds();
    if (expirationTime != null && expirationTime < 0) {
      try {
        credential.refreshToken();
      } catch (IOException e) {
        throw new GoogleDriveException("Error refreshing access token: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Update current credentials with new refresh token from given API instance.
   */
  void updateToken(GoogleDriveAPI refreshApi) throws GoogleDriveException {
    credential.setRefreshToken(refreshApi.credential.getRefreshToken());
    try {
      credential.refreshToken();
    } catch (IOException e) {
      throw new GoogleDriveException("Error updating access token: " + e.getMessage(), e);
    }
  }

  String getAccessToken() {
    return credential.getAccessToken();
  }

  String getRefreshToken() {
    return credential.getRefreshToken();
  }

  long getExpirationTime() {
    return credential.getExpirationTimeMilliseconds();
  }

  // ********** helpers ***********

  boolean isFolder(File file) {
    return file.getMimeType().equals(FOLDER_MIMETYPE);
  }

  /**
   * Parse RFC3339 date format into Calendar type.
   * 
   * @param datestring date in RFC3339 format.
   * @return Calendar.
   */
  Calendar parseDate(String datestring) {
    Date d = new Date();
    Calendar calendar = Calendar.getInstance();
    // if there is no time zone, we don't need to do any special parsing.
    if (datestring.endsWith("Z")) {
      try {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        d = s.parse(datestring);
      } catch (ParseException pe) {// try again with optional decimals
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        s.setLenient(true);
        try {
          d = s.parse(datestring);
        } catch (ParseException e) {
          LOG.error("An error occurred: ", e);
        }
      }
      calendar.setTime(d);
      return calendar;
    }
    // step one, split off the timezone.
    String firstpart = datestring.substring(0, datestring.lastIndexOf('-'));
    String secondpart = datestring.substring(datestring.lastIndexOf('-'));
    // step two, remove the colon from the timezone offset
    secondpart = secondpart.substring(0, secondpart.indexOf(':'))
        + secondpart.substring(secondpart.indexOf(':') + 1);
    datestring = firstpart + secondpart;
    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    try {
      d = s.parse(datestring);
    } catch (ParseException pe) {
      s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
      s.setLenient(true);
      try {
        d = s.parse(datestring);
      } catch (ParseException e) {
        LOG.error("An error occurred: ", e);
      }
    }
    calendar.setTime(d);
    return calendar;
  }
}
