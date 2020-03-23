package org.exoplatform.clouddrive.onedrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.DriveItemUploadableProperties;
import com.microsoft.graph.models.extensions.FileSystemInfo;
import com.microsoft.graph.models.extensions.Folder;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.SharingLink;
import com.microsoft.graph.models.extensions.Subscription;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionRequestBuilder;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;



class Scopes {
  static final String FilesReadAll            = "https://graph.microsoft.com/Files.Read.All";

  static final String FilesRead               = "https://graph.microsoft.com/Files.Read";

  static final String FilesReadSelected       = "https://graph.microsoft.com/Files.Read.Selected";

  static final String FilesReadWriteSelected  = "https://graph.microsoft.com/Files.ReadWrite.Selected";

  static final String FilesReadWrite          = "https://graph.microsoft.com/Files.ReadWrite";

  static final String FilesReadWriteAll       = "https://graph.microsoft.com/Files.ReadWrite.All";

  static final String FilesReadWriteAppFolder = "https://graph.microsoft.com/Files.ReadWrite.AppFolder";

  static final String UserRead                = "https://graph.microsoft.com/User.Read";

  static final String UserReadWrite           = "https://graph.microsoft.com/User.ReadWrite";

  static final String offlineAccess           = "offline_access";

  static final String UserReadWriteAll        = "https://graph.microsoft.com/User.ReadWrite.All";
}



public class OneDriveAPI {
  private final String redirectUrl;

  private String       rootId;
  @Deprecated
  private class OneDriveSubscription {
    private long   expirationDateTime;

    private String notificationUrl;

    public synchronized String getNotificationUrl() {
      if (LOG.isDebugEnabled()) {
        LOG.debug("subscription left: " + (expirationDateTime - Calendar.getInstance().getTimeInMillis()));
      }
      if (Calendar.getInstance().getTimeInMillis() >= expirationDateTime) {
        final Subscription subscription = getSubscription();
        this.notificationUrl = subscription.notificationUrl;
        this.expirationDateTime = subscription.expirationDateTime.getTimeInMillis();
      }
      return notificationUrl;
    }
  }

  private class OneDriveToken {
    // in millis
    private final static int LIFETIME = 2000 * 1000;

    private String           refreshToken;

    private String           accessToken;

    private long             lastModifiedTime;

    public OneDriveToken(String accessToken, String refreshToken) {
      this.updateToken(accessToken, refreshToken);
    }

    public synchronized String getAccessToken() throws RefreshAccessException, OneDriveException {
      long currentTime = System.currentTimeMillis();
      if (currentTime >= lastModifiedTime + LIFETIME) {
        try {
          if (LOG.isDebugEnabled()) {
            LOG.debug("refreshToken = " + this.refreshToken);
          }
          OneDriveTokenResponse oneDriveTokenResponse = renewAccessToken(this.refreshToken, redirectUrl);
          this.accessToken = oneDriveTokenResponse.getToken();
          this.lastModifiedTime = System.currentTimeMillis();
          String refreshToken = oneDriveTokenResponse.getRefreshToken();
          // storedToken.store(refreshToken);
          this.refreshToken = refreshToken;
        } catch (IOException e) {
          throw new RefreshAccessException("Error during token update", e);
        }
      }
      return accessToken;
    }

    public final synchronized void updateToken(String accessToken, String refreshToken) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("OneDriveToken.updateToken() : accessToken = " + accessToken + " refreshToken = " + refreshToken);
      }
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.lastModifiedTime = System.currentTimeMillis();
    }
  }

  private static final Log           LOG                  = ExoLogger.getLogger(OneDriveAPI.class);

  private static final Log           GRAPH_CLIENT_LOG     = ExoLogger.getLogger(OneDriveAPI.class.getSimpleName()
          + "_GraphClient");

  private final OneDriveStoredToken  storedToken;

  private final String               clientId;

  private final String               clientSecret;

  private final OneDriveToken        oneDriveToken;

  private final HttpClient           httpclient           = HttpClients.createDefault();

  private final OneDriveSubscription oneDriveSubscription = new OneDriveSubscription();

  /**
   * Makes request to onedrive to get a token, using oauth code or refresh token depending on the grantType.
   * @param clientId
   * @param clientSecret
   * @param code
   * @param refreshToken
   * @param grantType
   * @param redirectUrl
   * @return
   * @throws IOException
   * @throws OneDriveException
   */
  private OneDriveTokenResponse retrieveAccessToken(String clientId,
                                                    String clientSecret,
                                                    String code,
                                                    String refreshToken,
                                                    String grantType,
                                                    String redirectUrl) throws IOException, OneDriveException {
    HttpPost httppost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
    List<NameValuePair> params = new ArrayList<>(5);
    if (grantType.equals("refresh_token")) {
      params.add(new BasicNameValuePair("refresh_token", refreshToken));
    } else if (grantType.equals("authorization_code")) {
      params.add(new BasicNameValuePair("redirect_uri", redirectUrl));
      params.add(new BasicNameValuePair("code", code));
    } else {
      throw new OneDriveException("Error getting access token  due to unknown grandtype " + grantType );
    }
    params.add(new BasicNameValuePair("grant_type", grantType));
    params.add(new BasicNameValuePair("client_secret", clientSecret));
    params.add(new BasicNameValuePair("client_id", clientId));
    params.add(new BasicNameValuePair("scope", SCOPES));

    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    HttpResponse response = httpclient.execute(httppost);
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      try (InputStream inputStream = entity.getContent()) {
        String responseBody = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        if (LOG.isDebugEnabled()) {
          LOG.debug("getToken ResponseBody := " + responseBody);
        }
        OneDriveTokenResponse oneDriveTokenResponse = gson.fromJson(responseBody, OneDriveTokenResponse.class);
        if (oneDriveTokenResponse.getToken() != null && !oneDriveTokenResponse.getToken().isEmpty()) {
          return oneDriveTokenResponse;
        }
      }
    } else {
      LOG.error("failed to get access token");
    }
    throw new OneDriveException("Error getting access token for clientId " + clientId + ", refresh token " + refreshToken);

  }

  /**
   * Gets access token. See
   * {@link OneDriveAPI#retrieveAccessToken(String, String, String, String, String, String)}
   * 
   * @param code authorization identifier according to oauth specification.
   * @param redirectUrl
   * @return {@link OneDriveTokenResponse}
   * @throws IOException
   * @throws OneDriveException
   */
  private OneDriveTokenResponse aquireAccessToken(String code, String redirectUrl) throws IOException, OneDriveException {
    return retrieveAccessToken(clientId, clientSecret, code, null, "authorization_code", redirectUrl);
  }

  /**
   * Updates access token. See
   * {@link OneDriveAPI#retrieveAccessToken(String, String, String, String, String, String)}
   * 
   * @param refreshToken
   * @param redirectUrl
   * @return {@link OneDriveTokenResponse}
   * @throws IOException
   * @throws OneDriveException
   */
  private OneDriveTokenResponse renewAccessToken(String refreshToken, String redirectUrl) throws IOException, OneDriveException {
    return retrieveAccessToken(clientId, clientSecret, null, refreshToken, "refresh_token", redirectUrl);
  }

  /**
   * Creates a public link to view a file content.
     Currently at 23/08/2019, the business account does not support the 'embed' link type.
   * @param itemId
   * @param type must be view or embed
   * @return
   * @throws OneDriveException
   */
  public SharingLink createLink(String itemId, String type) throws OneDriveException {
    if (type.equalsIgnoreCase("embed")) {
      return graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link;
    } else if (type.equalsIgnoreCase("view")) {
      return graphClient.me().drive().items(itemId).createLink("view", "anonymous").buildRequest().post().link;
    }
    throw new OneDriveException("Link type must be either view or embed");
  }

  public final static String SCOPES = scopes();

  private static String scopes() {
    StringJoiner scopes = new StringJoiner(" ");
    scopes.add(Scopes.FilesReadWriteAll)
            .add(Scopes.FilesRead)
            .add(Scopes.FilesReadWrite)
            .add(Scopes.FilesReadAll)
            .add(Scopes.UserRead)
            .add(Scopes.offlineAccess);
    return scopes.toString();
  }

  private void initGraphClient() {
    this.graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
      String accessToken = null;
      try {
        accessToken = getAccessToken();
      } catch (RefreshAccessException | OneDriveException e) {
        LOG.error("during initialization of graphClient an error occurred", e);
      }
      iHttpRequest.getHeaders().add(new HeaderOption("Authorization", "Bearer " + accessToken));
    }).logger(new ExoGraphClientLogger(GRAPH_CLIENT_LOG)).buildClient();
  }

  OneDriveAPI(String clientId, String clientSecret, String authCode, String redirectUrl) throws IOException, CloudDriveException {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUrl = redirectUrl;
    OneDriveTokenResponse oneDriveTokenResponse = aquireAccessToken(authCode, redirectUrl);
    if (oneDriveTokenResponse != null) {
      this.storedToken = new OneDriveStoredToken();
      this.storedToken.store(oneDriveTokenResponse.getToken(),
              oneDriveTokenResponse.getRefreshToken(),
              oneDriveTokenResponse.getExpires());

      this.oneDriveToken = new OneDriveToken(oneDriveTokenResponse.getToken(), oneDriveTokenResponse.getRefreshToken());
      initGraphClient();
    } else {
      throw new OneDriveException("Unable to retrieve access token, when onedriveApi initializes: clientId " + clientId);
    }
  }

  OneDriveAPI(String clientId,
              String clientSecret,
              String accessToken,
              String refreshToken,
              long expirationTime,
              String redirectUrl) throws CloudDriveException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> Creating OneDrive API by refresh token for client {}", clientId);
    }
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUrl = redirectUrl;
    OneDriveTokenResponse oneDriveTokenResponse = null;
    oneDriveTokenResponse = renewAccessToken(refreshToken, redirectUrl);
    if (oneDriveTokenResponse != null) {
      this.storedToken = new OneDriveStoredToken();
      this.storedToken.store(oneDriveTokenResponse.getRefreshToken());
      this.oneDriveToken = new OneDriveToken(oneDriveTokenResponse.getToken(), oneDriveTokenResponse.getRefreshToken());
      initGraphClient();
    } else {
      throw new OneDriveException("Unable to retrieve access token, when onedriveApi initializes: clientId " + clientId);
    }
  }

  private String getAccessToken() throws RefreshAccessException, OneDriveException {
    return oneDriveToken.getAccessToken();
  }

  public void updateToken(OneDriveStoredToken newToken) throws CloudDriveException {
    this.oneDriveToken.updateToken(newToken.getAccessToken(), newToken.getRefreshToken());
    this.storedToken.merge(newToken);
  }

  private final Gson          gson = new Gson();

  private IGraphServiceClient graphClient;

  public void removeFolder(String fileId) {
    graphClient.me().drive().items(fileId).buildRequest().delete();
  }

  public void removeFile(String fileId) {
    graphClient.me().drive().items(fileId).buildRequest().delete();
  }

  public User getUser() {
    return graphClient.me().buildRequest().get();
  }

  /**
   * @return id of the root folder on the user's drive.
   */
  public synchronized String getRootId() {
    if (this.rootId == null) {
      this.rootId = getRoot().id;
    }
    return rootId;
  }

  private DriveItem getRoot() {
    return graphClient.me().drive().root().buildRequest().get();
  }

  /**
   * Сreates folder. If a folder with the same name already exists - renames new
   * folder.
   * 
   * @param parentId
   * @param folder
   * @return
   */
  private DriveItem createFolderRequestWrapper(String parentId, DriveItem folder) {
    JsonObject obj = new JsonParser().parse("{\n" + "  \"name\": \"" + folder.name + "\",\n" + "  \"folder\": { },\n"
            + "  \"@microsoft.graph.conflictBehavior\": \"rename\"\n" + "}").getAsJsonObject();
    String id = graphClient.customRequest("/me/drive/items/" + parentId + "/children")
            .buildRequest()
            .post(obj)
            .get("id")
            .getAsString();
    return getItem(id);
  }

  public DriveItem createFolder(String parentId, String name, Calendar created) {
    if (parentId == null || parentId.isEmpty()) {
      parentId = getRootId();
    }
    DriveItem folder = new DriveItem();
    folder.name = name;
    folder.parentReference = new ItemReference();
    folder.fileSystemInfo = new FileSystemInfo();
    folder.fileSystemInfo.createdDateTime = created;
    folder.parentReference.id = parentId;
    folder.folder = new Folder();

    return createFolderRequestWrapper(parentId, folder);
  }

  public DriveItem copyFile(String parentId, String fileName, String fileId) throws IOException, CloudDriveException {
    return copy(parentId, fileName, fileId, true);
  }

  public DriveItem copyFolder(String parentId, String name, String folderId) throws CloudDriveException, IOException {
    return copy(parentId, name, folderId, false);
  }

  public OneDriveStoredToken getStoredToken() {
    return storedToken;
  }

  @Deprecated
  public String getNotificationUrl() {
    return oneDriveSubscription.getNotificationUrl();
  }

  /**
   *
   * Subscribes to receive drive changes using web socket.
   * @return
   */
  public Subscription getSubscription() {
    return graphClient.me().drive().root().subscriptions("socketIO").buildRequest().get();
  }

  public DriveItem copy(String parentId, String fileName, String fileId, boolean isFile) throws RefreshAccessException, OneDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> copy {}->{}/{}", fileName, parentId, fileName);
    }
    String request = "{\n" + "      \"parentReference\" : {\n" + "        \"id\" : \"" + parentId + "\"\n" + "    },\n"
            + "      \"name\" : \"" + fileName + "\"\n" +
            // "      \"@microsoft.graph.conflictBehavior\" : \"rename\"\n" +
            "    }";
    HttpPost httppost = new HttpPost("https://graph.microsoft.com/v1.0/me/drive/items/" + fileId + "/copy");
    StringEntity stringEntity = new StringEntity(request, "UTF-8");
    httppost.setEntity(stringEntity);
    httppost.addHeader("Authorization", "Bearer " + getAccessToken());
    httppost.addHeader("Content-type", "application/json");
    String location;
    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httppost)) {
      location = response.getHeaders("Location")[0].getValue();
    } catch (IOException e) {
      throw new OneDriveException(e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Location:= " + location);
    }
    JsonParser parser = new JsonParser();
    String status;
    String responseBody;
    do {
      try {
        responseBody = getCopyResponseBody(location);
      } catch (IOException e) {
        throw new OneDriveException("Unable to retrieve copy response body for " + fileName, e);
      }
      status = parser.parse(responseBody.trim()).getAsJsonObject().get("status").getAsString();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.warn("Thread interrupted while sleeping on copy of " + fileName, e);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Copy status: {} for {}", status, fileName);
      }
    } while (!status.equalsIgnoreCase("failed") && !status.equalsIgnoreCase("completed"));

    if (status.equalsIgnoreCase("failed")) {
      Set<String> fileNames = getFiles(parentId, isFile ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName).stream()
              .filter((item) -> isFile == (item.file != null))
              .map((item) -> item.name)
              .collect(Collectors.toSet());
      if (!fileNames.contains(fileName)) { // This means that the error is not
        // related to the fact that a file
        // with the same name exists.
        throw new OneDriveException("Error copying file " + fileName + "[" + fileId + "] to parent " + parentId);
      }
      String newItemName = generateItemName(fileName, 1, isFile);
      for (int i = 2; i <= fileNames.size() && fileNames.contains(newItemName); i++) {
        newItemName = generateItemName(fileName, i, isFile);
      }
      return copy(parentId, newItemName, fileId, isFile);
    } else {
      String resourceId = parser.parse(responseBody).getAsJsonObject().get("resourceId").getAsString();
      return getItem(resourceId);
    }
  }

  /**
   * Creates a new file name.
   * 
   * @param name
   * @param number which should be present in the file name.
   * @param isFile indicates file or folder.
   * @return новое имя файла
   */
  private String generateItemName(String name, int number, boolean isFile) {
    if (name != null) {
      int lastDotPosition = name.lastIndexOf(".");
      if (!isFile || lastDotPosition == -1) {
        return name + " " + number;
      }
      String baseName = name.substring(0, lastDotPosition);
      String ext = name.substring(lastDotPosition);
      return baseName + " " + number + ext;
    }
    throw new IllegalArgumentException("Name should not be null");
  }

  /**
   * Request copy status.
   * 
   * @param location URL for copying drive item.
   * @return file copy status
   * @throws IOException
   */
  private String getCopyResponseBody(String location) throws IOException {
    HttpGet httpGet = new HttpGet(location);
    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httpGet)) {
      String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      return responseBody;
    }
  }

  /**
   * Gets a list of children of the folder, with pagination.
   * @param folderId
   * @return
   */
  public IDriveItemCollectionPage getDriveItemCollectionPage(String folderId) {
    IDriveItemCollectionPage collectionPage;
    if (folderId == null) {
      collectionPage = graphClient.me().drive().root().children().buildRequest().get();
    } else {
      collectionPage = graphClient.me().drive().items(folderId).children().buildRequest().get();
    }
    return collectionPage;
  }

  /**
   * Gets list of files in this folder whose names begins with the given prefix.
   * @param folderId
   * @param startsWith
   * @return
   */
  private List<DriveItem> getFiles(String folderId, String startsWith) {
    IDriveItemCollectionPage collectionPage;
    final QueryOption deltaTokenQuery = new QueryOption("filter", "startswith(name,'" + startsWith + "')");
    if (folderId == null) {
      collectionPage = graphClient.me()
              .drive()
              .root()
              .children()
              .buildRequest(Collections.singletonList(deltaTokenQuery))
              .get();
    } else {
      collectionPage = graphClient.me()
              .drive()
              .items(folderId)
              .children()
              .buildRequest(Collections.singletonList(deltaTokenQuery))
              .get();
    }
    List<DriveItem> driveItems = new ArrayList<>(collectionPage.getCurrentPage());
    IDriveItemCollectionRequestBuilder nextPage = collectionPage.getNextPage();
    while (nextPage != null) {
      IDriveItemCollectionPage nextPageCollection = nextPage.buildRequest().get();
      driveItems.addAll(nextPageCollection.getCurrentPage());
      nextPage = nextPageCollection.getNextPage();
    }
    return driveItems;
  }
  @Deprecated
  public List<DriveItem> getFiles(String folderId) {
    IDriveItemCollectionPage collectionPage;
    if (folderId == null) {
      collectionPage = graphClient.me().drive().root().children().buildRequest().get();
    } else {
      collectionPage = graphClient.me().drive().items(folderId).children().buildRequest().get();
    }

    List<DriveItem> driveItems = new ArrayList<>(collectionPage.getCurrentPage());

    IDriveItemCollectionRequestBuilder nextPage = collectionPage.getNextPage();
    while (nextPage != null) {
      IDriveItemCollectionPage nextPageCollection = nextPage.buildRequest().get();
      driveItems.addAll(nextPageCollection.getCurrentPage());
      nextPage = nextPageCollection.getNextPage();
      if (nextPage == null) {
      }
    }
    return driveItems;
  }

  /**
   * Uploads part of a file
   * 
   * @param url to which the file is uploaded
   * @param startPosition from which the transmitted date should be in the file.
   * @param contentLength size of transmitted data in the current request.
   * @param size total file size
   * @param data file slice
   * @return {@link FileSendResponse}
   * @throws IOException
   */
  private FileSendResponse sendFile(String url, int startPosition, int contentLength, int size, byte[] data) throws IOException {
    URL obj = new URL(url);
    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
    con.setRequestMethod("PUT");
    con.setRequestProperty("Content-Length", String.valueOf(contentLength));
    con.setRequestProperty("Content-Range", "bytes " + startPosition + "-" + (startPosition + contentLength - 1) + "/" + size);
    con.setRequestProperty("Accept", "application/json");
    con.setDoOutput(true);
    try (OutputStream outputStream = con.getOutputStream()) {
      outputStream.write(data);
    }

    FileSendResponse fileSendResponse = new FileSendResponse();
    fileSendResponse.responseMessage = con.getResponseMessage();
    fileSendResponse.responseCode = con.getResponseCode();

    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
      String inputLine;
      StringBuilder response = new StringBuilder();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      fileSendResponse.data = response.toString();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> sendFile response \nmessage: {} \ncode: {} \ndata: {}",
                fileSendResponse.responseMessage,
                fileSendResponse.responseCode,
                fileSendResponse.data);
    }

    return fileSendResponse;
  }

  /**
   * Checks if the file has fully uploaded and returns if successful.
   * 
   * @param fileSendResponse see {@link FileSendResponse}
   * @return newly created drive item, or null if the upload has not yet
   *         completed.
   */
  private DriveItem getDriveItemIfCreated(FileSendResponse fileSendResponse) {
    if (fileSendResponse.responseCode == 201) {
      JsonObject jsonDriveItem = new JsonParser().parse(fileSendResponse.data).getAsJsonObject();
      DriveItem createdFile = graphClient.me().drive().items(jsonDriveItem.get("id").getAsString()).buildRequest().get();
      return createdFile;
    }
    return null;
  }

  private String uploadUrlConflictRenameWrapper(String path, DriveItemUploadableProperties driveItemUploadableProperties) throws UnsupportedEncodingException {

    JsonObject uploadFileRequestBody = new JsonParser().parse("{\"item\": {\n"
            + "    \"@odata.type\": \"microsoft.graph.driveItemUploadableProperties\",\n"
            + "    \"@microsoft.graph.conflictBehavior\": \"rename\",\n" + "    \"name\": \"" + driveItemUploadableProperties.name
            + "\"\n" + "  }}".trim()).getAsJsonObject();

    return graphClient.customRequest("/me/drive/root:/" + URLEncoder.encode(path, "UTF-8") + ":/createUploadSession")
            .buildRequest()
            .post(uploadFileRequestBody)
            .get("uploadUrl")
            .getAsString();
  }

  @Deprecated
  private String getUploadUrl(String path, DriveItemUploadableProperties driveItemUploadableProperties) throws UnsupportedEncodingException {
    return uploadUrlConflictRenameWrapper(path, driveItemUploadableProperties);
  }

  /**
   * @param deltaToken starting from which to get changes.
   * @return {@link ChangesIterator}
   */
  public ChangesIterator changes(String deltaToken) {
    return new ChangesIterator(deltaToken);
  }

  private String encodeUrlPath(String value) throws URISyntaxException {
    URI uri = new URI(null, null, null, value, null);
    String request = uri.toASCIIString();
    return request.startsWith("?") ? request.substring(1) : request;
  }

  /**
   * Gets url on which uploading should be done.
   * 
   * @param parentId where should the new file be added.
   * @param name
   * @param conflictBehavior determines the behavior if a file with the name is
   *          already present. must be 'fail' or 'rename';
   * @return url to upload the file. see the
   *         {@link OneDriveAPI#insertUpdate(String, InputStream)}
   * @throws RefreshAccessException
   * @throws OneDriveException
   */
  public String getInsertUploadUrl(String parentId, String name, String conflictBehavior) throws RefreshAccessException, OneDriveException {

    if (!conflictBehavior.equals("fail") && !conflictBehavior.equals("rename")) {
      // Error
    }
    String request = "{\n" + "  \"item\": {\n" + "    \"@microsoft.graph.conflictBehavior\": \""+conflictBehavior+"\"\n" + "  }\n" + "}";
    HttpPost httppost = null;
    try {
      httppost = new HttpPost("https://graph.microsoft.com/v1.0/me/drive/items/" + parentId + ":/" + encodeUrlPath(name)
              + ":/createUploadSession");
    } catch (URISyntaxException e) {
      throw new OneDriveException(e);
    }
    StringEntity stringEntity = new StringEntity(request, "UTF-8");
    httppost.setEntity(stringEntity);
    httppost.addHeader("Authorization", "Bearer " + getAccessToken());
    httppost.addHeader("Content-type", "application/json");
    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httppost)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(response);
      }
      String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JsonObject responseAsJson = new JsonParser().parse(responseBody).getAsJsonObject();
      if (responseAsJson.has("uploadUrl")) {
        String uploadUrl = responseAsJson.get("uploadUrl").getAsString();
        return uploadUrl;
      } else if (responseAsJson.has("error")) {
        JsonObject errorResponse = responseAsJson.get("error").getAsJsonObject();
        if (errorResponse.has("code") && StringUtils.equalsIgnoreCase(errorResponse.get("code").getAsString(), "nameAlreadyExists")) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Name Already Exists");
          }
          throw new OneDriveException("File with the same name remotely already exists: parentId " + parentId + ", name " + name);
        }
      }
    } catch (IOException e) {
      throw new OneDriveException(e.getMessage(),e);
    }
    throw new OneDriveException("Unable to retrieve url to upload file: parentId " + parentId + ", name " + name);
  }

  String updateUploadUrl(String itemId) throws IOException, RefreshAccessException, OneDriveException {
    HttpPost httppost = new HttpPost("https://graph.microsoft.com/v1.0/me/drive/items/" + itemId + "/createUploadSession");
    httppost.addHeader("Authorization", "Bearer " + getAccessToken());
    httppost.addHeader("Content-type", "application/json");
    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httppost)) {
      String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      String uploadUrl = new JsonParser().parse(responseBody).getAsJsonObject().get("uploadUrl").getAsString();
      if (LOG.isDebugEnabled()) {
        LOG.debug(uploadUrl);
      }
      return uploadUrl;
    }
  }

  /**
   * Uploads a file to onedrive
   * 
   * @param uploadUrl to upload file.
   * @param inputStream new file content.
   * @return new or updated file
   * @throws OneDriveException
   * @throws IOException
   */
  DriveItem insertUpdate(String uploadUrl, InputStream inputStream) throws OneDriveException, IOException {
    int fileLength = inputStream.available();
    FileSendResponse fileSendResponse = null;
    int bufferSize = 327680 * 100; // must be a multiple of 327680
    for (int i = 0; i < fileLength / bufferSize + 1; i++) {
      int from = bufferSize * i;
      int to = bufferSize * (i + 1);
      if (to > fileLength) {
        to = fileLength;
      }
      int fileSliceSize = to - from;
      byte[] fileSlice = new byte[fileSliceSize];
      inputStream.read(fileSlice, 0, fileSliceSize);
      fileSendResponse = sendFile(uploadUrl, from, fileSlice.length, fileLength, fileSlice);
      DriveItem driveItem = getDriveItemIfCreated(fileSendResponse);
      if (driveItem != null) {
        return driveItem;
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> insertUpdate response \nmessage: {} \ncode: {} \ndata: {}",
                fileSendResponse.responseMessage,
                fileSendResponse.responseCode,
                fileSendResponse.data);
    }
    throw new OneDriveException("Failed to upload file to url " + uploadUrl);
  }

  public DriveItem insert(String parentId, String fileName, Calendar created, Calendar modified, InputStream inputStream, String conflictBehavior) throws OneDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("insert file");
    }
    String updateUploadUrl = null;
    try {
      updateUploadUrl = getInsertUploadUrl(parentId, fileName, conflictBehavior);
      return insertUpdate(updateUploadUrl, inputStream);
    } catch (OneDriveException | RefreshAccessException | IOException e) {
      throw new OneDriveException("Error insert file parentId " + parentId + ", fileName " + fileName + ": " + e.getMessage(),e);
    }
  }

  public DriveItem updateFileContent(String itemId, Calendar created, Calendar modified, InputStream inputStream) throws OneDriveException {

    try {
      String updateUploadUrl = updateUploadUrl(itemId);
      return insertUpdate(updateUploadUrl, inputStream);
    } catch (Exception ex) {
      throw new OneDriveException("Error  update file content for itemId " + itemId + ": " + ex.getMessage(), ex);
    }
  }

  private DriveItem updateFileWrapper(DriveItem item) {
    JsonObject updateFileRequestBody = new JsonParser().parse("  {\n" + "            \"parentReference\": {\n"
        + "            \"id\": \"" + item.parentReference.id + "\"\n" + "        },\n" + "            \"name\": \"" + item.name
        + "\",\n" + "            \"@microsoft.graph.conflictBehavior\" : \"rename\"\n" + "        }").getAsJsonObject();
    String updatedFileId = graphClient.customRequest("/me/drive/items/" + item.id)
                                      .buildRequest()
                                      .patch(updateFileRequestBody)
                                      .get("id")
                                      .getAsString();
    return getItem(updatedFileId);
  }

  /**
   * Gets all the drive items.
   * 
   * @return {@link DeltaDriveFiles}
   */
  public DeltaDriveFiles getAllFiles() {
    String deltaToken = null;
    List<DriveItem> changes = new ArrayList<>();
    IDriveItemDeltaCollectionPage deltaCollectionPage = delta(null);
    IDriveItemDeltaCollectionRequestBuilder nextPage;
    while (true) {
      changes.addAll(deltaCollectionPage.getCurrentPage());
      nextPage = deltaCollectionPage.getNextPage();
      if (nextPage == null) {
        deltaToken = extractDeltaToken(deltaCollectionPage.deltaLink());
        break;
      }
      deltaCollectionPage = nextPage.buildRequest().get();
    }
    return new DeltaDriveFiles(deltaToken, changes);
  }

  public DriveItem updateFile(DriveItem driveItem) {
    return updateFileWrapper(driveItem);
  }

  public DriveItem getItem(String itemId) {
    return graphClient.me().drive().items(itemId).buildRequest().get();
  }

  private boolean isDeltaTokenExpired(String deltaToken) {
    return false;
  }

  /**
   * @param deltaToken
   * @return list of items that have been changed since last sync.
   */
  private IDriveItemDeltaCollectionPage delta(String deltaToken) {
    IDriveItemDeltaCollectionPage collectionPage;
    if (isDeltaTokenExpired(deltaToken)) {
      deltaToken = null;
    }
    if (deltaToken == null || deltaToken.isEmpty() || deltaToken.toUpperCase().trim().equals("ALL")) {
      collectionPage = graphClient.me().drive().root().delta().buildRequest().get();
    } else {
      final QueryOption deltaTokenQuery = new QueryOption("token", deltaToken);
      collectionPage = graphClient.me().drive().root().delta().buildRequest(Collections.singletonList(deltaTokenQuery)).get();
    }

    return collectionPage;
  }

  class SimpleChildIterator extends ChunkIterator<HashSetCompatibleDriveItem> {

    private final Collection<HashSetCompatibleDriveItem> items;

    public SimpleChildIterator(Collection<HashSetCompatibleDriveItem> items) throws CloudDriveException {
      this.items = items;
      this.iter = nextChunk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<HashSetCompatibleDriveItem> nextChunk() {
      available(items.size());
      return items.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasNextChunk() {
      return false;
    }
  }

  public SimpleChildIterator getSimpleChildIterator(Collection<HashSetCompatibleDriveItem> items) throws CloudDriveException {
    return new SimpleChildIterator(items);
  }

  public ChildIterator getChildIterator(String folderId) {
    return new ChildIterator(folderId);
  }

  class ChildIterator extends ChunkIterator<DriveItem> {

    IDriveItemCollectionPage collectionPage;

    ChildIterator(String folderId) {
      this.collectionPage = getDriveItemCollectionPage(folderId);
      iter = nextChunk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<DriveItem> nextChunk() {
      List<DriveItem> driveItems = new ArrayList<>(collectionPage.getCurrentPage());
      available(driveItems.size());
      IDriveItemCollectionRequestBuilder nextPage = collectionPage.getNextPage();
      if (nextPage != null) {
        collectionPage = nextPage.buildRequest().get();
      } else {
        collectionPage = null;
      }
      return driveItems.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasNextChunk() {
      return collectionPage != null;
    }
  }

  private String extractDeltaToken(String deltaLink) {
    return deltaLink.substring(deltaLink.indexOf("=") + 1);
  }

  class ChangesIterator extends ChunkIterator<DriveItem> {

    private IDriveItemDeltaCollectionPage deltaCollectionPage;

    private String                        deltaToken;

    ChangesIterator(String deltaToken) {
      this.deltaToken = deltaToken;
      this.deltaCollectionPage = delta(deltaToken);
      this.iter = nextChunk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<DriveItem> nextChunk() {
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> ChangesIterator nextChunk");
      }
      final List<DriveItem> changes = new ArrayList<>();
      IDriveItemDeltaCollectionRequestBuilder nextPage;
      changes.addAll(deltaCollectionPage.getCurrentPage());
      nextPage = deltaCollectionPage.getNextPage();
      if (nextPage == null) {
        deltaToken = extractDeltaToken(deltaCollectionPage.deltaLink());
        deltaCollectionPage = null;
      } else {
        deltaCollectionPage = nextPage.buildRequest().get();
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("ChangesIterator nextChunk available {}", changes.size());
      }
      available(changes.size());
      return changes.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasNextChunk() {
      return deltaCollectionPage != null;
    }

    String getDeltaToken() {
      return deltaToken;
    }
  }
}
