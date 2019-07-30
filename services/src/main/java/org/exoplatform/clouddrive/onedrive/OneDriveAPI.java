package org.exoplatform.clouddrive.onedrive;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.*;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.oauth2.UserToken;
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

class ExoGraphClientLogger implements ILogger {

  private final Log log;

  public ExoGraphClientLogger(Log log) {
    this.log = log;
  }

  @Override
  public void setLoggingLevel(LoggerLevel loggerLevel) {

  }

  @Override
  public LoggerLevel getLoggingLevel() {
    return LoggerLevel.DEBUG;
  }

  @Override
  public void logDebug(String s) {
    if (log.isDebugEnabled()) {
      log.debug(s);
    }
  }

  @Override
  public void logError(String s, Throwable throwable) {
    log.error(s, throwable);
  }
}

public class OneDriveAPI {
  private final String redirectUrl;

  private String       rootId;

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

    public synchronized String getAccessToken() throws RefreshAccessException {
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

  private OneDriveTokenResponse retrieveAccessToken(String clientId,
                                                    String clientSecret,
                                                    String code,
                                                    String refreshToken,
                                                    String grantType,
                                                    String redirectUrl) throws IOException {
    HttpPost httppost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
    List<NameValuePair> params = new ArrayList<>(5);
    if (grantType.equals("refresh_token")) {
      params.add(new BasicNameValuePair("refresh_token", refreshToken));
    } else if (grantType.equals("authorization_code")) {
      params.add(new BasicNameValuePair("redirect_uri", redirectUrl));
      params.add(new BasicNameValuePair("code", code));
    } else {
      return null;
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
    return null;
  }

  private OneDriveTokenResponse aquireAccessToken(String code, String redirectUrl) throws IOException {
    return retrieveAccessToken(clientId, clientSecret, code, null, "authorization_code", redirectUrl);
  }

  private OneDriveTokenResponse renewAccessToken(String refreshToken, String redirectUrl) throws IOException {
    return retrieveAccessToken(clientId, clientSecret, null, refreshToken, "refresh_token", redirectUrl);
  }

  public SharingLink createLink(String itemId, String type) throws OneDriveException {
    if (type.equalsIgnoreCase("embed")) {
      return graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link;
    } else if (type.equalsIgnoreCase("view")) {
      return graphClient.me().drive().items(itemId).createLink("view", "anonymous").buildRequest().post().link;
    }
    throw new OneDriveException("type must be either view or embed");
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
      } catch (RefreshAccessException e) {
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
      LOG.debug("one drive api by refresh token");
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

  private String getAccessToken() throws RefreshAccessException {
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

  public synchronized String getRootId() {
    if (this.rootId == null) {
      this.rootId = getRoot().id;
    }
    return rootId;
  }

  private DriveItem getRoot() {
    return graphClient.me().drive().root().buildRequest().get();
  }

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

  public String getNotificationUrl() {
    return oneDriveSubscription.getNotificationUrl();
  }

  public Subscription getSubscription() {
    return graphClient.me().drive().root().subscriptions("socketIO").buildRequest().get();
  }

  public DriveItem copy(String parentId, String fileName, String fileId, boolean isFile) throws RefreshAccessException, OneDriveException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("try copy");
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
        throw new OneDriveException("Unable to retrieve copy response body ", e);
      }
      status = parser.parse(responseBody.trim()).getAsJsonObject().get("status").getAsString();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.warn("Thread interrupted while sleeping", e);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("opy status = " + status);
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
        throw new OneDriveException("error occurred during the copy process: parentId " + parentId + ", fileName " + fileName + ", fileId " + fileId);
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

  private String generateItemName(String name, int number, boolean isFile) {
    if (name == null)
      return null;

    int lastDotPosition = name.lastIndexOf(".");
    if (!isFile || lastDotPosition == -1) {
      return name + " " + number;
    }
    String baseName = name.substring(0, lastDotPosition);
    String ext = name.substring(lastDotPosition);
    return baseName + " " + number + ext;
  }

  private String getCopyResponseBody(String location) throws IOException {
    HttpGet httpGet = new HttpGet(location);
    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(httpGet)) {
      String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      return responseBody;
    }
  }

  public IDriveItemCollectionPage getDriveItemCollectionPage(String folderId) {

    IDriveItemCollectionPage iDriveItemCollectionPage = null;
    if (folderId == null) {
      iDriveItemCollectionPage = graphClient.me().drive().root().children().buildRequest().get();
    } else {
      iDriveItemCollectionPage = graphClient.me().drive().items(folderId).children().buildRequest().get();
    }
    return iDriveItemCollectionPage;
  }

  private List<DriveItem> getFiles(String folderId, String startsWith) {

    IDriveItemCollectionPage iDriveItemCollectionPage;
    final QueryOption deltaTokenQuery = new QueryOption("filter", "startswith(name,'" + startsWith + "')");
    if (folderId == null) {
      iDriveItemCollectionPage = graphClient.me()
              .drive()
              .root()
              .children()
              .buildRequest(Collections.singletonList(deltaTokenQuery))
              .get();
    } else {
      iDriveItemCollectionPage = graphClient.me()
              .drive()
              .items(folderId)
              .children()
              .buildRequest(Collections.singletonList(deltaTokenQuery))
              .get();
    }
    List<DriveItem> driveItems = new ArrayList<>(iDriveItemCollectionPage.getCurrentPage());
    IDriveItemCollectionRequestBuilder nextPage = iDriveItemCollectionPage.getNextPage();
    while (nextPage != null) {
      IDriveItemCollectionPage nextPageCollection = nextPage.buildRequest().get();
      driveItems.addAll(nextPageCollection.getCurrentPage());
      nextPage = nextPageCollection.getNextPage();
    }
    return driveItems;
  }

  public List<DriveItem> getFiles(String folderId) {

    IDriveItemCollectionPage iDriveItemCollectionPage = null;
    if (folderId == null) {
      iDriveItemCollectionPage = graphClient.me().drive().root().children().buildRequest().get();
    } else {
      iDriveItemCollectionPage = graphClient.me().drive().items(folderId).children().buildRequest().get();
    }

    List<DriveItem> driveItems = new ArrayList<>(iDriveItemCollectionPage.getCurrentPage());

    IDriveItemCollectionRequestBuilder nextPage = iDriveItemCollectionPage.getNextPage();
    while (nextPage != null) {
      IDriveItemCollectionPage nextPageCollection = nextPage.buildRequest().get();
      driveItems.addAll(nextPageCollection.getCurrentPage());
      nextPage = nextPageCollection.getNextPage();
      if (nextPage == null) {
      }
    }
    return driveItems;
  }

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
      LOG.debug("response message: " + fileSendResponse.responseMessage);
      LOG.debug("response code = " + fileSendResponse.responseCode);
      LOG.debug("response data" + fileSendResponse.data);
    }

    return fileSendResponse;
  }

  private class FileSendResponse {

    int    responseCode;

    String responseMessage;

    String data;

  }

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

  private String getUploadUrl(String path, DriveItemUploadableProperties driveItemUploadableProperties) throws UnsupportedEncodingException {

    return uploadUrlConflictRenameWrapper(path, driveItemUploadableProperties);
  }

  public ChangesIterator changes(String deltaToken) {
    return new ChangesIterator(deltaToken);
  }

  private String encodeUrlPath(String value) throws URISyntaxException {
    URI uri = new URI(null, null, null, value, null);
    String request = uri.toASCIIString();
    return request.startsWith("?") ? request.substring(1) : request;
  }

//  public DriveItem getItemByPath(String path)  {
//    try {
//      return  graphClient.me().drive().root().itemWithPath(encodeUrlPath(path)).buildRequest().get();
//    } catch (URISyntaxException e) {
//      // TODO throw ex here
//      LOG.error("unable to get file",e);
//      return null;
//    }
//  }

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
      throw new OneDriveException(e);
    }
    throw new OneDriveException("Unable to retrieve url to upload file: parentId " + parentId + ", name " + name);
  }

  String updateUploadUrl(String itemId) throws IOException, RefreshAccessException {
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
    throw new OneDriveException("failed to upload file: " + "ResponseCode: " + fileSendResponse.responseCode + " "
            + "message: " + fileSendResponse.responseMessage + "date: " + fileSendResponse.data);

  }

  public DriveItem insert(String parentId, String fileName, Calendar created, Calendar modified, InputStream inputStream, String conflictBehavior) throws OneDriveException {
    // TODO do something with create, modified
    if (LOG.isDebugEnabled()) {
      LOG.debug("insert file");
    }
    String updateUploadUrl = null;
    try {
      updateUploadUrl = getInsertUploadUrl(parentId, fileName, conflictBehavior);
      return insertUpdate(updateUploadUrl, inputStream);
    } catch (Exception e) {
      throw new OneDriveException(e);
    }
  }

  public DriveItem updateFileContent(String itemId, Calendar created, Calendar modified, InputStream inputStream) throws OneDriveException {

    try {
      String updateUploadUrl = updateUploadUrl(itemId);
      return insertUpdate(updateUploadUrl, inputStream);
    } catch (Exception ex) {
      throw new OneDriveException(ex);
    }
  }

  public DriveItem updateFileWrapper(DriveItem item){
    // TODO rewrite with JsonObject
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

  class DeltaDriveFiles {
    private String          deltaToken;

    private List<DriveItem> items;

    DeltaDriveFiles(String deltaToken, List<DriveItem> items) {
      this.deltaToken = deltaToken;
      this.items = items;
    }

    public String getDeltaToken() {
      return deltaToken;
    }

    public void setDeltaToken(String deltaToken) {
      this.deltaToken = deltaToken;
    }

    public List<DriveItem> getItems() {
      return items;
    }

    public void setItems(List<DriveItem> items) {
      this.items = items;
    }
  }

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
    // TODO check it
    return false;
  }

  public IDriveItemDeltaCollectionPage delta(String deltaToken) {
    IDriveItemDeltaCollectionPage iDriveItemDeltaCollectionPage = null;
    if (isDeltaTokenExpired(deltaToken)) {
      deltaToken = null;
    }
    if (deltaToken == null || deltaToken.isEmpty() || deltaToken.toUpperCase().trim().equals("ALL")) {
      iDriveItemDeltaCollectionPage = graphClient.me().drive().root().delta().buildRequest().get();
    } else {
      final QueryOption deltaTokenQuery = new QueryOption("token", deltaToken);
      iDriveItemDeltaCollectionPage = graphClient.me()
              .drive()
              .root()
              .delta()
              .buildRequest(Collections.singletonList(deltaTokenQuery))
              .get();
    }

    return iDriveItemDeltaCollectionPage;
  }

  static class HashSetCompatibleDriveItem {
    DriveItem item;

    public HashSetCompatibleDriveItem(DriveItem item) {
      this.item = item;
    }

    public DriveItem getItem() {
      return item;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      HashSetCompatibleDriveItem that = (HashSetCompatibleDriveItem) o;

      return item.id.equals(that.item.id);
    }

    @Override
    public int hashCode() {
      return item.id.hashCode();
    }
  }

  class SimpleChildIterator extends ChunkIterator<HashSetCompatibleDriveItem> {

    private final Collection<HashSetCompatibleDriveItem> items;

    public SimpleChildIterator(Collection<HashSetCompatibleDriveItem> items) throws CloudDriveException {
      this.items = items;
      this.iter = nextChunk();
    }

    @Override
    protected Iterator<HashSetCompatibleDriveItem> nextChunk() {
      available(items.size());
      return items.iterator();
    }

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

    IDriveItemCollectionPage driveItemCollectionPage;

    ChildIterator(String folderId) {

      this.driveItemCollectionPage = getDriveItemCollectionPage(folderId);
      iter = nextChunk();

    }

    @Override
    protected Iterator<DriveItem> nextChunk() {
      List<DriveItem> driveItems = new ArrayList<>(driveItemCollectionPage.getCurrentPage());
      available(driveItems.size());
      IDriveItemCollectionRequestBuilder nextPage = driveItemCollectionPage.getNextPage();
      if (nextPage != null) {
        driveItemCollectionPage = nextPage.buildRequest().get();
      } else {
        driveItemCollectionPage = null;
      }
      return driveItems.iterator();
    }

    @Override
    protected boolean hasNextChunk() {
      return driveItemCollectionPage != null;
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
      iter = nextChunk();
    }

    @Override
    protected Iterator<DriveItem> nextChunk() {
      if (LOG.isDebugEnabled()) {
        LOG.debug("ChangesIterator nextChunk()");
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
        LOG.debug("ChangesIterator nextChunk()   available " + changes.size());
      }
      available(changes.size());
      return changes.iterator();
    }

    @Override
    protected boolean hasNextChunk() {
      return deltaCollectionPage != null;
    }

    String getDeltaToken() {
      return deltaToken;
    }
  }

  static class OneDriveStoredToken extends UserToken {
    public void store(String refreshToken) throws CloudDriveException {
      this.store("", refreshToken, 0);
    }
  }

}
