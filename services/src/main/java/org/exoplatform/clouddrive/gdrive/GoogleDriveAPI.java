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

package org.exoplatform.clouddrive.gdrive;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow.CredentialCreatedListener;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonError.ErrorInfo;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.Drive.Files.Delete;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.google.api.services.oauth2.model.Userinfoplus;

import org.exoplatform.clouddrive.CloudDriveAccessException;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Covers calls to Google Drive services and handles related exceptions. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GoogleDriveAPI.java 00000 Jan 5, 2013 pnedonosko $
 */
class GoogleDriveAPI implements DataStoreFactory {

  public static final String       APP_NAME           = "eXo Cloud Drive";

  public static final String       FOLDER_MIMETYPE    = "application/vnd.google-apps.folder";

  public static final List<String> SCOPES             = Arrays.asList(DriveScopes.DRIVE,
                                                                      DriveScopes.DRIVE_FILE,
                                                                      DriveScopes.DRIVE_APPDATA,
                                                                      DriveScopes.DRIVE_SCRIPTS,
                                                                      DriveScopes.DRIVE_APPS_READONLY,
                                                                      Oauth2Scopes.USERINFO_EMAIL,
                                                                      Oauth2Scopes.USERINFO_PROFILE);

  public static final String       SCOPES_STRING      = scopes();

  public static final String       ACCESS_TYPE        = "offline";

  public static final String       APPOVAl_PROMT      = "force";

  public static final String       NO_STATE           = "__no_state_set__";

  protected static final String    USER_ID            = "user_id";

  protected static final String    USER_EMAIL_ADDRESS = "emailAddress";

  protected static final Log       LOG                = ExoLogger.getLogger(GoogleDriveAPI.class);

  class AuthToken extends UserToken implements CredentialRefreshListener, CredentialCreatedListener {

    class Store implements DataStore<StoredCredential> {

      /**
       * {@inheritDoc}
       */
      @Override
      public DataStoreFactory getDataStoreFactory() {
        return GoogleDriveAPI.this;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String getId() {
        return GoogleDriveAPI.class.getSimpleName();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int size() throws IOException {
        // Only one item possible - current user token
        return isEmpty() ? 0 : 1;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean isEmpty() throws IOException {
        return getAccessToken() == null || getRefreshToken() == null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean containsKey(String userId) throws IOException {
        return USER_ID.equals(userId);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean containsValue(StoredCredential value) throws IOException {
        return value.getAccessToken().equals(getAccessToken()) && value.getRefreshToken().equals(getRefreshToken())
            && value.getExpirationTimeMilliseconds() == getExpirationTime();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Set<String> keySet() throws IOException {
        Set<String> keys = new HashSet<String>();
        keys.add(USER_ID);
        return keys;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Collection<StoredCredential> values() throws IOException {
        StoredCredential[] single = new StoredCredential[] { get(USER_ID) };
        return Arrays.asList(single);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public StoredCredential get(String userId) throws IOException {
        // Need return StoredCredential...
        StoredCredential stored = new StoredCredential();
        stored.setAccessToken(getAccessToken());
        stored.setRefreshToken(getRefreshToken());
        stored.setExpirationTimeMilliseconds(getExpirationTime());
        return stored;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public DataStore<StoredCredential> set(String userId, StoredCredential value) throws IOException {
        store(value);
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public DataStore<StoredCredential> clear() throws IOException {
        // TODO clear the token keys
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public DataStore<StoredCredential> delete(String userId) throws IOException {
        // TODO clear the token keys
        return this;
      }
    }

    /**
     * Facade-implementation of {@link DataStore} to actual value stored in enclosing {@link UserToken}.
     */
    final Store store = new Store();

    void store(StoredCredential credential) {
      try {
        store(credential.getAccessToken(), credential.getRefreshToken(), credential.getExpirationTimeMilliseconds());
      } catch (CloudDriveException e) {
        LOG.error("Error storing credential", e);
      }
    }

    void store(Credential credential) {
      try {
        store(credential.getAccessToken(), credential.getRefreshToken(), credential.getExpirationTimeMilliseconds());
      } catch (CloudDriveException e) {
        LOG.error("Error storing credential", e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCredentialCreated(Credential credential, TokenResponse tokenResponse) throws IOException {
      store(credential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
      store(credential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
      // TODO clean token keys to let them be re-requested to the user
      String errDescription = tokenErrorResponse.getErrorDescription();
      String errURI = tokenErrorResponse.getErrorUri();
      LOG.error("Error refreshing credentials: " + tokenErrorResponse.getError()
          + (errDescription != null ? " " + errDescription : "") + (errURI != null ? ". Error URI: " + errURI : ""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(UserToken newToken) throws CloudDriveException {
      super.merge(newToken);

      // explicitly apply token keys to currently used credential
      if (credential != null) {
        credential.setAccessToken(newToken.getAccessToken());
        credential.setExpirationTimeMilliseconds(newToken.getExpirationTime());
        credential.setRefreshToken(newToken.getRefreshToken());
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
        throw new GoogleDriveException("Error creating request to Children.List service: " + e.getMessage(), e);
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

    long               largestChangeId;

    /**
     * @throws GoogleDriveException
     */
    ChangesIterator(long startChangeId) throws GoogleDriveException {
      try {
        this.request = drive.changes().list();
        this.request.setIncludeSubscribed(false); // get changes of files only explicitly added to user drive
        this.request.setIncludeDeleted(true);
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
        largestChangeId = children.getLargestChangeId();
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

    long getLargestChangeId() {
      return largestChangeId;
    }
  }

  /**
   * Credentials for request authentication.
   */
  final Credential credential;

  /**
   * Drive services API.
   */
  final Drive      drive;

  final AuthToken  token;

  /**
   * User info API.
   */
  final Oauth2     oauth2;

  /**
   * Timezone regexp pattern for adapting Google's date format to SimpleDateFormatter supported.
   */
  // full date pattern: \\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+([+-][0-2]\\d:[0-5]\\d|Z)
  final Pattern    tzPattern = Pattern.compile("([+-][0-2]\\d:[0-5]\\d|Z)$");

  /**
   * Create Google Drive API from OAuth2 authentication code.
   * 
   * @param clientId {@link String}
   * @param clientSecret {@link String}
   * @param authCode {@link String}
   * @throws GoogleDriveException if authentication failed for any reason.
   * @throws CloudDriveException if credentials store exception happen
   */
  GoogleDriveAPI(String clientId, String clientSecret, String authCode, String redirectUri)
      throws GoogleDriveException, CloudDriveException {
    // use clean token, it will be populated with actual credentials as CredentialRefreshListener
    this.token = new AuthToken();

    GoogleAuthorizationCodeFlow authFlow;
    try {
      authFlow = createFlow(clientId, clientSecret, token);
    } catch (IOException e) {
      throw new GoogleDriveException("Error creating authentication flow: " + e.getMessage(), e);
    }

    GoogleTokenResponse response;
    try {
      // Exchange an authorization code for OAuth 2.0 credentials.
      response = authFlow.newTokenRequest(authCode).setRedirectUri(redirectUri).execute();
    } catch (IOException e) {
      throw new GoogleDriveException("Error authenticating user code: " + e.getMessage(), e);
    }

    try {
      this.credential = authFlow.createAndStoreCredential(response, USER_ID);
    } catch (IOException e) {
      throw new CloudDriveException("Error storing user credential: " + e.getMessage(), e);
    }

    // XXX .setHttpRequestInitializer(new RequestInitializer() this causes OAuth2 401 Unauthorized
    this.drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), this.credential)
                                                                                                 .setApplicationName(APP_NAME)
                                                                                                 .build();
    this.oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), this.credential)
                                                                                                   .setApplicationName(APP_NAME)
                                                                                                   .build();
  }

  /**
   * Create Google Drive API from existing user credentials.
   * 
   * @param clientId {@link String}
   * @param clientSecret {@link String}
   * @param accessToken {@link String}
   * @param refreshToken {@link String}
   * @param expirationTime long, token expiration time on milliseconds
   * @throws CloudDriveException if credentials store exception happen
   */
  GoogleDriveAPI(String clientId, String clientSecret, String accessToken, String refreshToken, long expirationTime)
      throws CloudDriveException {
    this.token = new AuthToken();
    this.token.load(accessToken, refreshToken, expirationTime);

    GoogleAuthorizationCodeFlow authFlow;
    try {
      authFlow = createFlow(clientId, clientSecret, token);
    } catch (IOException e) {
      throw new GoogleDriveException("Error creating authentication flow: " + e.getMessage(), e);
    }

    try {
      this.credential = authFlow.loadCredential(USER_ID);
    } catch (IOException e) {
      throw new CloudDriveException("Error loading Google user credentials: " + e.getMessage(), e);
    }

    // XXX .setHttpRequestInitializer(new RequestInitializer() this causes OAuth2 401 Unauthorized
    this.drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(APP_NAME)
                                                                                            .build();
    this.oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(APP_NAME)
                                                                                              .build();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public DataStore<StoredCredential> getDataStore(String id) throws IOException {
    return token.store;
  }

  private static String scopes() {
    StringBuilder s = new StringBuilder();
    for (String scope : SCOPES) {
      s.append(scope);
      s.append(' ');
    }
    return s.toString();
  }

  /**
   * Build an authorization flow optionally using provided {@link AuthToken}, then store it as a static
   * class attribute.
   * 
   * @param clientId
   * @param clientSecret
   * @param tokenStore
   * @return GoogleAuthorizationCodeFlow instance.
   * @throws IOException
   */
  GoogleAuthorizationCodeFlow createFlow(String clientId, String clientSecret, AuthToken storedToken) throws IOException {
    HttpTransport httpTransport = new NetHttpTransport();
    JacksonFactory jsonFactory = new JacksonFactory();

    GoogleAuthorizationCodeFlow.Builder flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
                                                                                       jsonFactory,
                                                                                       clientId,
                                                                                       clientSecret,
                                                                                       SCOPES);
    // (access_type=offline) if application needs to refresh access tokens
    // when the user is not present at the browser.
    // If the value "approval_prompt" is force, then the user sees a
    // consent page even if they have previously given consent to your
    // application for a given set of scopes.
    // was setApprovalPrompt("force")
    flow.setAccessType(ACCESS_TYPE).setApprovalPrompt(APPOVAl_PROMT);

    if (storedToken != null) {
      flow.setCredentialDataStore(storedToken.store);
      flow.setCredentialCreatedListener(storedToken);
      flow.addRefreshListener(storedToken);
    }

    return flow.build();
  }

  /**
   * Return Userinfo service.
   * 
   * @return {@link Userinfoplus}
   * @throws GoogleDriveException on error from OAuth2 service
   * @throws CloudDriveException if no Userinfo found
   */
  Userinfoplus userInfo() throws GoogleDriveException, CloudDriveException {
    Userinfoplus userInfo;
    try {
      userInfo = oauth2.userinfo().get().execute();
    } catch (GoogleJsonResponseException e) {
      GoogleJsonError error = e.getDetails();
      // More error information can be retrieved with error.getErrors().
      throw new GoogleDriveException("Error getting userinfo: " + error.getMessage() + " (" + error.getCode() + ").", e);
    } catch (HttpResponseException e) {
      // No Json body was returned by the API.
      throw new GoogleDriveException("Error handling userinfo response: " + e.getMessage() + " (" + e.getStatusCode() + ").",
                                     e);
    } catch (IOException e) {
      throw new GoogleDriveException("Error requesting userinfo: " + e.getMessage(), e);
    }
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
   * @throws CloudDriveAccessException
   */
  About about() throws GoogleDriveException, CloudDriveAccessException {
    try {
      return drive.about().get().execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 403) {
        throw new CloudDriveAccessException("Error accessing About service: " + e.getMessage(), e);
      } else {
        throw new GoogleDriveException("Error reading About service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error requesting About service: " + e.getMessage(), e);
    }
  }

  ChildIterator children(String fileId) throws GoogleDriveException {
    return new ChildIterator(fileId);
  }

  ChangesIterator changes(long startChangeId) throws GoogleDriveException {
    return new ChangesIterator(startChangeId);
  }

  /**
   * Read file from Files service.
   * 
   * @param fileId {@link String}
   * @return {@link File}
   * @throws GoogleDriveException
   * @throws NotFoundException
   */
  File file(String fileId) throws GoogleDriveException, NotFoundException {
    try {
      return drive.files().get(fileId).execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error getting file from Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error requesting file from Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Insert a new file to Files service and upload its content.
   * 
   * @param file {@link File} file metadata
   * @param file {@link AbstractInputStreamContent} file content
   * @return {@link File} resulting file
   * @throws GoogleDriveException
   * @throws CloudDriveAccessException
   */
  File insert(File file, AbstractInputStreamContent content) throws GoogleDriveException, CloudDriveAccessException {
    try {
      return drive.files().insert(file, content).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to inserting file with content in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else {
        // TODO we want be sure that file not inserted
        // need generate ID by Google (https://developers.google.com/drive/v2/reference/files/generateIds)
        // and use them for file inserting, and then if failed and try again it will return conflict (409)
        // error
        throw new GoogleDriveException("Error inserting file with content to Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error inserting file with content to Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Insert a new file to Files service. This method will create an empty file or a folder (if given file
   * object has such mimetype).
   * 
   * @param file {@link File} file metadata
   * @return {@link File} resulting file
   * @throws GoogleDriveException
   * @throws CloudDriveAccessException
   */
  File insert(File file) throws GoogleDriveException, CloudDriveAccessException {
    try {
      return drive.files().insert(file).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to insert file to Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else {
        // TODO we want be sure that file not inserted
        // need generate ID by Google (https://developers.google.com/drive/v2/reference/files/generateIds)
        // and use them for file inserting, and then if failed and try again it will return conflict (409)
        // error
        throw new GoogleDriveException("Error inserting file to Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error inserting file to Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Update a file metadata in Files service and upload its new content.
   * 
   * @param file {@link File} file metadata
   * @param file {@link AbstractInputStreamContent} file content
   * @return {@link File} resulting file
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  void update(File file, AbstractInputStreamContent content) throws GoogleDriveException,
                                                             NotFoundException,
                                                             CloudDriveAccessException {
    // TODO use If-Match with local ETag to esnure consistency
    // http://stackoverflow.com/questions/15723284/google-drive-sdk-check-etag-when-uploading-synchronizing
    String fileId = file.getId();
    try {
      // file id update not assumed in this context
      drive.files().update(fileId, file, content).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to update file in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for updating: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error updating file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error upating file with content in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Update a file metadata in Files service.
   * 
   * @param file {@link File} file metadata
   * @return {@link File} resulting file
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  void update(File file) throws GoogleDriveException, NotFoundException, CloudDriveAccessException {
    // TODO use If-Match with local ETag to esnure consistency
    // http://stackoverflow.com/questions/15723284/google-drive-sdk-check-etag-when-uploading-synchronizing
    String fileId = file.getId();
    try {
      // file id update not assumed in this context
      drive.files().update(fileId, file).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to update file in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for updating: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error updating file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error upating file metadata in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Copy a file in Files service.
   * 
   * @param srcFileId {@link String}
   * @param destFile {@link File} destination file metadata
   * @return {@link File} resulting file
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  File copy(String srcFileId, File destFile) throws GoogleDriveException, NotFoundException, CloudDriveAccessException {
    // TODO use If-Match with local ETag to esnure consistency
    // http://stackoverflow.com/questions/15723284/google-drive-sdk-check-etag-when-uploading-synchronizing
    try {
      return drive.files().copy(srcFileId, destFile).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to copy file in Files service. " + e.getStatusMessage()
            + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for copying: " + srcFileId, e);
      } else {
        throw new GoogleDriveException("Error copying file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error copying file metadata in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Delete a file in Files service.
   * 
   * @param fileId {@link String} file id
   * @return {@link Delete} resulting object
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  void delete(String fileId) throws GoogleDriveException, NotFoundException, CloudDriveAccessException {
    try {
      drive.files().delete(fileId).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to delete file in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for deleting: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error deleting file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error deleting file in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Move a file to Trash using Files service.
   * 
   * @param fileId {@link String} file id
   * @return {@link File} resulting object
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  File trash(String fileId) throws GoogleDriveException, NotFoundException, CloudDriveAccessException {
    try {
      return drive.files().trash(fileId).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to trash file in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for trashing: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error trashing file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error trashing file in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Move a file from Trash to its original place using Files service.
   * 
   * @param fileId {@link String} file id
   * @return {@link File} resulting object
   * @throws GoogleDriveException
   * @throws NotFoundException
   * @throws CloudDriveAccessException
   */
  File untrash(String fileId) throws GoogleDriveException, NotFoundException, CloudDriveAccessException {
    try {
      return drive.files().untrash(fileId).execute();
    } catch (GoogleJsonResponseException e) {
      if (isInsufficientPermissions(e)) {
        throw new CloudDriveAccessException("Insufficient permissions to untrash file in Files service. "
            + e.getStatusMessage() + " (" + e.getStatusCode() + ")");
      } else if (e.getStatusCode() == 404) {
        throw new NotFoundException("Cloud file not found for untrashing: " + fileId, e);
      } else {
        throw new GoogleDriveException("Error untrashing file in Files service: " + e.getMessage(), e);
      }
    } catch (IOException e) {
      throw new GoogleDriveException("Error untrashing file in Files service: " + e.getMessage(), e);
    }
  }

  /**
   * Check credentials isn't expired and refresh them if required.
   * 
   * @throws GoogleDriveException if error during communication with the provider
   */
  void refreshAccess() throws GoogleDriveException {
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
   * Update OAuth2 token to a new one.
   * 
   * @param newToken {@link AuthToken}
   * @throws CloudDriveException
   */
  void updateToken(UserToken newToken) throws CloudDriveException {
    this.token.merge(newToken);

  }

  /**
   * Current OAuth2 token associated with this API instance.
   * 
   * @return {@link AuthToken}
   */
  AuthToken getToken() {
    return token;
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

    // Google keep dates in form "2014-12-24T13:45:13.620+02:00" - we need convert timezone to RFC 822 form
    Matcher dm = tzPattern.matcher(datestring);
    if (dm.find() && dm.groupCount() >= 1) {
      String tz = dm.group(1);
      datestring = dm.replaceFirst(tz.replace(":", ""));
    }

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

  // **** internals *****

  private boolean isInsufficientPermissions(GoogleJsonResponseException e) {
    GoogleJsonError details = e.getDetails();
    if (e.getStatusCode() == 403 && details != null) {
      List<ErrorInfo> errors = details.getErrors();
      if (errors != null) {
        for (ErrorInfo ei : errors) {
          if (ei.getReason().equals("insufficientPermissions")) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
