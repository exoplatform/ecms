package org.exoplatform.clouddrive.onedrive;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
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
import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.*;

import org.exoplatform.clouddrive.CloudDriveException;
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
  private String rootId;

  private class OneDriveToken {
    // in millis
    private final static int LIFETIME = 3600 * 1000;

    private String           refreshToken;

    private String           accessToken;

    private long             lastModifiedTime;

    public OneDriveToken(String accessToken, String refreshToken) {
      this.updateToken(accessToken, refreshToken);
    }

    public synchronized String getAccessToken() throws RefreshAccessException {
      long currentTime = System.currentTimeMillis();
      if (currentTime >= lastModifiedTime + /* LIFETIME */+2000_000) { // TODO
                                                                       // use
                                                                       // constant
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

  private static final Log          LOG              = ExoLogger.getLogger(OneDriveAPI.class);

  private static final Log          GRAPH_CLIENT_LOG = ExoLogger.getLogger(OneDriveAPI.class.getSimpleName() + "_GraphClient");

  private final OneDriveStoredToken storedToken;

  private final String              clientId;

  private final String              clientSecret;

  private final OneDriveToken       oneDriveToken;

  private final HttpClient          httpclient       = HttpClients.createDefault();

  private OneDriveTokenResponse retrieveAccessToken(String clientId,
                                                    String clientSecret,
                                                    String code,
                                                    String refreshToken,
                                                    String grantType, String redirectUrl) throws IOException {
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
        if(oneDriveTokenResponse.getToken()!=null && !oneDriveTokenResponse.getToken().isEmpty()){
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

  public SharingLink createLink(String itemId, String type) {
    if(type.equalsIgnoreCase("embed")){
      return graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link;
    } else if (type.equalsIgnoreCase("view")) {
      return graphClient.me().drive().items(itemId).createLink("view", "anonymous").buildRequest().post().link;
    }
    throw new IllegalArgumentException("type must be either view or embed");
  }

  public final static String SCOPES = scopes();

  private static String scopes() {
    StringJoiner scopes = new StringJoiner(" ");
    scopes.add(Scopes.FilesReadWriteAll).add(Scopes.FilesRead).add(Scopes.FilesReadWrite).add(Scopes.FilesReadAll)
    // .add(Scopes.FilesReadSelected)
    // .add(Scopes.UserReadWriteAll)
          .add(Scopes.UserRead)
          // .add(Scopes.UserReadWrite)
          .add(Scopes.offlineAccess);
    // .add(Scopes.FilesReadWriteAppFolder)
    // .add(Scopes.FilesReadWriteSelected);
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
      throw new CloudDriveException("Unable to retrieve access token, when onedriveApi initializes ");
    }
  }

  OneDriveAPI(String clientId, String clientSecret, String accessToken, String refreshToken, long expirationTime, String redirectUrl) throws CloudDriveException,
      IOException {
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
      throw new CloudDriveException("Unable to retrieve access token, when onedriveApi initializes");
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

    // return
    // graphClient.me().drive().items(parentId).children().buildRequest().post(folder);
    return createFolderRequestWrapper(parentId, folder);
  }

  public DriveItem copyFile(String parentId, String fileName, String fileId) throws IOException, CloudDriveException {
    return copy(parentId, fileName, fileId, true);
    // try {
    // String copiedFileId = copy(parentId, fileName, fileId);
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("copiedFileId = " + copiedFileId);
    // }
    // return getItem(copiedFileId);
    // } catch (IOException e) {
    // // TODO can we work normally after this ex? Later syncs will fail
    // obviously - need throw an ex from here?
    // LOG.error("error while copying file",e);
    // }
    // return null;
  }

  public DriveItem copyFolder(String parentId, String name, String folderId) throws CloudDriveException, IOException {
    return copy(parentId, name, folderId, false);
  }

  // public DriveItem getItemByPath(String path) {
  // try {
  // return graphClient.me().drive().root().itemWithPath(URLEncoder.encode(path,
  // "UTF-8")).buildRequest().get();
  // } catch (UnsupportedEncodingException e) {
  // // TODO throw ex here
  // LOG.error("unable to get file", e);
  // return null;
  // }
  // }

  public OneDriveStoredToken getStoredToken() {
    return storedToken;
  }

  // TODO new name: getResourceId()
  // private String retrieveCopiedFileId(String location) throws IOException {
  // HttpGet httpget = new HttpGet(location);
  // HttpResponse response = httpclient.execute(httpget);
  // HttpEntity entity = response.getEntity();
  // if (entity != null) {
  // try (InputStream inputStream = entity.getContent()) {
  // String responseBody = IOUtils.toString(inputStream,
  // Charset.forName("UTF-8"));
  // JsonObject jsonObject = new
  // JsonParser().parse(responseBody).getAsJsonObject();
  // if (jsonObject.has("resourceId")) {
  // return jsonObject.get("resourceId").getAsString();
  // } else {
  // try {
  // Thread.sleep(1000);
  // } catch (InterruptedException e) {
  // e.printStackTrace();
  // }
  // if (LOG.isDebugEnabled()) {
  // LOG.debug("has not resourceId, responseBody = " + responseBody);
  // }
  // return retrieveCopiedFileId(location);
  // }
  // }
  // } // TODO else, is it an error state? throw ex here?
  // return null;
  // }

  public DriveItem copy(String parentId, String fileName, String fileId, boolean isFile) throws IOException, CloudDriveException {
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
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Location:= " + location);
    }
    JsonParser parser = new JsonParser();
    String status;
    String responseBody;
    do {
      responseBody = getCopyResponseBody(location);
      status = parser.parse(responseBody.trim()).getAsJsonObject().get("status").getAsString();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.warn("thread interrupted while sleeping", e);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("copy status = " + status);
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
        throw new CloudDriveException("error occurred during the copy process");
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

  // public String copy(String parentId, String fileName, String fileId) throws
  // IOException, RefreshAccessException {
  // String request =
  // "{\n" +
  // "      \"parentReference\" : {\n" +
  // "        \"id\" : \"" + parentId + "\"\n" +
  // "    },\n" +
  // "      \"name\" : \"" + fileName + "\",\n" +
  // "      \"@microsoft.graph.conflictBehavior\" : \"rename\"\n" +
  // "    }";
  //
  //
  // HttpPost httppost = new
  // HttpPost("https://graph.microsoft.com/v1.0/me/drive/items/" + fileId +
  // "/copy");
  // StringEntity stringEntity = new StringEntity(request, "UTF-8");
  // httppost.setEntity(stringEntity);
  // httppost.addHeader("Authorization", "Bearer " + getAccessToken());
  // httppost.addHeader("Content-type", "application/json");
  // HttpResponse response = httpclient.execute(httppost);
  // String location = response.getHeaders("Location")[0].getValue();
  //
  // return retrieveCopiedFileId(location);
  // }

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

  private List<DriveItem> getFiles(String folderId) {

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
      LOG.debug("response messagge: " +fileSendResponse.responseMessage);
      LOG.debug("response code = " + fileSendResponse.responseCode);
      LOG.debug("response data" + fileSendResponse.data);
    }

    return fileSendResponse;
  }

//  private byte[] readAllBytes(InputStream inputStream) throws IOException {
//    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//    int nRead;
//    byte[] data = new byte[16384];
//
//    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
//      buffer.write(data, 0, nRead);
//    }
//
//    return buffer.toByteArray();
//  }

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

    /*
     * "item": { "@odata.type": "microsoft.graph.driveItemUploadableProperties",
     * "@microsoft.graph.conflictBehavior": "rename", "name": "largefile.dat" }
     */

    JsonObject uploadFileRequestBody = new JsonParser().parse("{\"item\": {\n"
        + "    \"@odata.type\": \"microsoft.graph.driveItemUploadableProperties\",\n"
        + "    \"@microsoft.graph.conflictBehavior\": \"rename\",\n" + "    \"name\": \"" + driveItemUploadableProperties.name
        + "\"\n" + "  }}".trim()).getAsJsonObject();

    // JsonObject uploadFileRequestBody = new JsonParser().parse("{\n" +
    // "      \"@microsoft.graph.conflictBehavior\":\"rename\",\n" +
    // "            \"description\":\"" +
    // driveItemUploadableProperties.description + "\",\n" +
    // "            \"fileSystemInfo\":{\n" +
    // "      \"@odata.type\":\"microsoft.graph.fileSystemInfo\"\n" +
    // //
    // "      \"lastModifiedDateTime\" : \""+driveItemUploadableProperties.fileSystemInfo.lastModifiedDateTime+"\",\n"
    // +
    // //
    // "      \"createdDateTime\" : \""+driveItemUploadableProperties.fileSystemInfo.createdDateTime+"\"\n"
    // +
    // "    },\n" +
    // "      \"name\":\"" + driveItemUploadableProperties.name + "\"\n" +
    // "    }").getAsJsonObject();

    return graphClient.customRequest("/me/drive/root:/" + URLEncoder.encode(path, "UTF-8") + ":/createUploadSession")
                      .buildRequest()
                      .post(uploadFileRequestBody)
                      .get("uploadUrl")
                      .getAsString();
  }

  private String getUploadUrl(String path, DriveItemUploadableProperties driveItemUploadableProperties) throws UnsupportedEncodingException {

    return uploadUrlConflictRenameWrapper(path, driveItemUploadableProperties);
    // return graphClient.me()
    // .drive()
    // .root()
    // .itemWithPath(URLEncoder.encode(path, "UTF-8"))
    // .createUploadSession(driveItemUploadableProperties)
    // .buildRequest()
    // .post().uploadUrl;

  }

  // private DriveItemUploadableProperties
  // prepareDriveItemUploadableProperties(String fileName,
  // Calendar created,
  // Calendar modified) {
  // DriveItemUploadableProperties driveItemUploadableProperties = new
  // DriveItemUploadableProperties();
  // driveItemUploadableProperties.name = fileName;
  // driveItemUploadableProperties.fileSystemInfo = new FileSystemInfo();
  // driveItemUploadableProperties.fileSystemInfo.createdDateTime = created;
  // driveItemUploadableProperties.fileSystemInfo.lastModifiedDateTime =
  // modified;
  // return driveItemUploadableProperties;
  // }

  public ChangesIterator changes(String deltaToken) {
    return new ChangesIterator(deltaToken);
  }

  public String insertUploadUrl(String parentId, String name) throws IOException, RefreshAccessException {

    String request = "{\n" + "  \"item\": {\n" + "    \"@microsoft.graph.conflictBehavior\": \"rename\"\n" + "  }\n" + "}";
    HttpPost httppost = new HttpPost("https://graph.microsoft.com/v1.0/me/drive/items/" + parentId + ":/" + name
        + ":/createUploadSession");
    StringEntity stringEntity = new StringEntity(request, "UTF-8");
    httppost.setEntity(stringEntity);
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

  public String updateUploadUrl(String itemId) throws IOException, RefreshAccessException {
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

  public DriveItem insertUpdate(String uploadUrl, InputStream inputStream) throws Exception {
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
    throw new CloudDriveException("failed to upload file: " + "ResponseCode: " + fileSendResponse.responseCode + " " +
            "message: " + fileSendResponse.responseMessage + "date: " + fileSendResponse.data);

  }

  // public DriveItem insertUpdate(String uploadUrl, InputStream inputStream)
  // throws Exception {
  // // NotFoundException
  // // String uploadUrl = updateUploadUrl(parentId);
  // int size = inputStream.available();
  // byte[] file;
  // file = readAllBytes(inputStream);
  // //LOG.error("Unable to read all bytes from received inputstream", e);
  // int bufferSize = 327680 * 40; // must be a multiple of 327680
  // // TODO write comment, rewrite with inputstrem
  //
  // for (int i = 0; i < file.length / bufferSize + 1; i++) {
  // int from = bufferSize * i;
  // int to = bufferSize * (i + 1);
  // if (to > file.length) {
  // to = file.length;
  // }
  // // TODO looks like not efficient work with memory, why we need arrays at
  // all, can InputStream work for us?
  // byte[] fileSlice = Arrays.copyOfRange(file, from, to);
  // FileSendResponse fileSendResponse = sendFile(uploadUrl, from,
  // fileSlice.length, file.length, fileSlice);
  //
  // DriveItem driveItem = getDriveItemIfCreated(fileSendResponse);
  // if (driveItem != null) {
  // return driveItem;
  // } // TODO is it an error case?
  // }
  // return null;
  // }

  /**
   * @param isInsert indicates whether the file needs to be changed or added
   */
  // private DriveItem insertUpdate(String path,
  // String fileName,
  // Calendar created,
  // Calendar modified,
  // String mimetype,
  // InputStream inputStream,
  // boolean isInsert) throws UnsupportedEncodingException {
  // DriveItemUploadableProperties driveItemUploadableProperties =
  // prepareDriveItemUploadableProperties(fileName, created, modified);
  // String uploadUrl = getUploadUrl(path, driveItemUploadableProperties);
  // byte[] file;
  // try {
  // file = readAllBytes(inputStream);
  // } catch (IOException e) {
  // // TODO may be throw an ex?
  // return null;
  // }
  // //LOG.error("Unable to read all bytes from received inputstream", e);
  // int bufferSize = 327680 * 40; // must be a multiple of 327680
  // // TODO write comment, rewrite with inputstrem
  //
  // for (int i = 0; i < file.length / bufferSize + 1; i++) {
  // int from = bufferSize * i;
  // int to = bufferSize * (i + 1);
  // if (to > file.length) {
  // to = file.length;
  // }
  // // TODO looks like not efficient work with memory, why we need arrays at
  // all, can InputStream work for us?
  // byte[] fileSlice = Arrays.copyOfRange(file, from, to);
  // FileSendResponse fileSendResponse = null;
  // try {
  // fileSendResponse = sendFile(uploadUrl, from, fileSlice.length, file.length,
  // fileSlice);
  // } catch (IOException e) {
  // LOG.error("Cannot upload part of file. ", e);
  // // TODO throw it?
  // return null;
  // }
  // DriveItem driveItem = processEndOfFileUploadIfReached(fileSendResponse,
  // isInsert);
  // if (driveItem != null) {
  // return driveItem;
  // } // TODO is it an error case?
  // }
  // return null;
  // }

  // private DriveItem processEndOfFileUploadIfReached(FileSendResponse
  // fileSendResponse) {
  // return getDriveItemIfCreated(fileSendResponse);
  //
  // return driveItem;
  // }
  // private DriveItem retrieveDriveItemIfUpdated(FileSendResponse
  // fileSendResponse) {
  // if (fileSendResponse.responseCode == 200) {
  // JsonObject jsonDriveItem = new
  // JsonParser().parse(fileSendResponse.data).getAsJsonObject();
  // DriveItem updatedFile =
  // graphClient.me().drive().items(jsonDriveItem.get("id").getAsString()).buildRequest().get();
  // return updatedFile;
  // } // TODO error here?
  // return null;
  // }

  public DriveItem insert(String parentId, String fileName, Calendar created, Calendar modified, InputStream inputStream) throws Exception {
    // TODO do something with create, modified
    if (LOG.isDebugEnabled()) {
      LOG.debug("insert file");
    }
    String updateUploadUrl = insertUploadUrl(parentId, fileName);
    return insertUpdate(updateUploadUrl, inputStream);
  }

  public DriveItem updateFileContent(String itemId, Calendar created, Calendar modified, InputStream inputStream) throws Exception {

    String updateUploadUrl = updateUploadUrl(itemId);
    return insertUpdate(updateUploadUrl, inputStream);
  }

  public DriveItem updateFileWrapper(DriveItem item) {
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

  public DriveItem updateFile(DriveItem driveItem) {
    return updateFileWrapper(driveItem);
    // return
    // graphClient.me().drive().items(driveItem.id).buildRequest().patch(driveItem);
  }

  public DriveItem getItem(String itemId) {
    return graphClient.me().drive().items(itemId).buildRequest().get();
  }

  public IDriveItemDeltaCollectionPage delta(String deltaToken) {
    IDriveItemDeltaCollectionPage iDriveItemDeltaCollectionPage = null;
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

  class OneDriveStoredToken extends UserToken {
    public void store(String refreshToken) throws CloudDriveException {
      this.store("", refreshToken, 0);
    }
  }

}
