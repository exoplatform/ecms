package org.exoplatform.clouddrive.onedrive;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionPage;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OneDriveAPI {
  protected static final Log        LOG = ExoLogger.getLogger(OneDriveAPI.class);

  private final OneDriveStoredToken storedToken;

  private final String              clientId;

  private final String              clientSecret;
  private volatile String accessToken;

  private OneDriveTokenResponse retrieveAccessToken(String clientId,
                                                    String clientSecret,
                                                    String code,
                                                    String refreshToken,
                                                    String grantType) throws IOException {
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
    List<NameValuePair> params = new ArrayList<>(5);
    if (grantType.equals("refresh_token")) {
      params.add(new BasicNameValuePair("refresh_token", refreshToken));
    } else if (grantType.equals("authorization_code")) {
      params.add(new BasicNameValuePair("code", code));
    } else {
      return null;
    }
    params.add(new BasicNameValuePair("grant_type", grantType));
    params.add(new BasicNameValuePair("client_secret", clientSecret));
    params.add(new BasicNameValuePair("client_id", clientId));
    params.add(new BasicNameValuePair("scope", SCOPES));
    try {
      httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOG.warn("Unsupported encoding", e);
    }

    HttpResponse response = httpclient.execute(httppost);

    HttpEntity entity = response.getEntity();
    if (entity != null) {
      try (InputStream inputStream = entity.getContent()) {
        String responseBody = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        return gson.fromJson(responseBody, OneDriveTokenResponse.class);
      }
    }
    return null;
  }

  private OneDriveTokenResponse retrieveAccessTokenByCode(String code) throws IOException {
    return retrieveAccessToken(clientId, clientSecret, code, null, "authorization_code");
  }

  private OneDriveTokenResponse retrieveAccessTokenByRefreshToken(String refreshToken) throws IOException {
    return retrieveAccessToken(clientId, clientSecret, null, refreshToken, "refresh_token");
  }

    public SharingLink createLink(String itemId) {
//        return graphClient.me().drive().items(itemId).
        return graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link;
    }
  private final static String SCOPES =
                                     "https://graph.microsoft.com/Files.Read.All https://graph.microsoft.com/Files.Read https://graph.microsoft.com/Files.Read.Selected https://graph.microsoft.com/Files.ReadWrite https://graph.microsoft.com/Files.ReadWrite.All https://graph.microsoft.com/Files.ReadWrite.AppFolder https://graph.microsoft.com/Files.ReadWrite.Selected https://graph.microsoft.com/User.Read https://graph.microsoft.com/User.ReadWrite https://graph.microsoft.com/User.ReadWrite offline_access https://graph.microsoft.com/User.ReadWrite.All";

  private void initGraphClient() {
    this.graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
      iHttpRequest.getHeaders().add(new HeaderOption("Authorization", "Bearer " + getAccessToken()));
    }).buildClient();
  }

  OneDriveAPI(String clientId, String clientSecret, String authCode) throws IOException, CloudDriveException {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    // update token Storage
    OneDriveTokenResponse oneDriveTokenResponse = null;
      oneDriveTokenResponse = retrieveAccessTokenByCode(authCode);

    this.storedToken = new OneDriveStoredToken();
    if (oneDriveTokenResponse != null) {
        this.storedToken.store(oneDriveTokenResponse.getToken(),
                               oneDriveTokenResponse.getRefreshToken(),
                               oneDriveTokenResponse.getExpires());
        this.accessToken = oneDriveTokenResponse.getToken();

      initGraphClient();
    }else{
      throw new CloudDriveException("Unable to retrieve access token");
    }
  }

  OneDriveAPI(String clientId, String clientSecret, String accessToken, String refreshToken, long expirationTime)
      throws CloudDriveException,
      IOException {
    this.storedToken = new OneDriveStoredToken();
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    OneDriveTokenResponse oneDriveTokenResponse = null;
    oneDriveTokenResponse = retrieveAccessTokenByRefreshToken(refreshToken);
    if (oneDriveTokenResponse != null) {
      this.storedToken.store(oneDriveTokenResponse.getToken(),
                             oneDriveTokenResponse.getRefreshToken(),
                             oneDriveTokenResponse.getExpires());
      this.accessToken = oneDriveTokenResponse.getToken();

      initGraphClient();
    } else {
      throw new CloudDriveException("Unable to retrieve access token");
    }
  }

  // private static String TOKEN;
  //
  // static {
  // try {
  // TOKEN = new String(Files.readAllBytes(new
  // File(System.getProperty("user.home") + "/authToken.txt").toPath()),
  // Charset.forName("UTF-8"));
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  // }


  private String getAccessToken() {
    return accessToken;
//    return storedToken.getAccessToken();
    // //
    // graphClient.me().drive().root().children().buildRequest().get().getCurrentPage().get(0).д
    // try {
    // return new String(Files.readAllBytes(new File(System.getProperty("user.home")
    // + "/authToken.txt").toPath()),
    // Charset.forName("UTF-8"));
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // return null;
  }

  public void updateToken(OneDriveStoredToken newToken) {
    try {
      this.accessToken = newToken.getAccessToken();
      this.storedToken.merge(newToken);
    } catch (CloudDriveException e) {
      e.printStackTrace();
    }
  }

  class OneDriveStoredToken extends UserToken {

    // /**
    // * Store.
    // *
    // * @throws CloudDriveException the cloud drive exception
    // */
    // void store() throws CloudDriveException {
    //// this.store(api.getAccessToken(), api.getRefreshToken(), api.getExpires());
    // }
  }

  private final Gson                gson = new Gson();

  // TODO make local for every user
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

  public DriveItem getRoot() {
    return graphClient.me().drive().root().buildRequest().get();
  }

  public DriveItem createFolder(String parentId, String name, Calendar created) {
    if (parentId == null || parentId.isEmpty()) {
      parentId = getRoot().id;
    }
    DriveItem folder = new DriveItem();
    folder.name = name;
    folder.parentReference = new ItemReference();
    folder.fileSystemInfo = new FileSystemInfo();
    folder.fileSystemInfo.createdDateTime = created;
    folder.parentReference.id = parentId;
    folder.folder = new Folder();
    return graphClient.me().drive().items(parentId).children().buildRequest().post(folder);
  }

  public DriveItem copyFile(String parentId, String fileName, String fileId) {
    ItemReference parentReference = new ItemReference();
    parentReference.id = parentId;
    return graphClient.me().drive().items(fileId).copy(fileName, parentReference).buildRequest().post();
  }

  public DriveItem copyFolder(String parentId, String name, String folderId) {
    return copyFile(parentId, name, folderId);
  }

  public List<DriveItem> getChildren() {
    return this.getChildren(null);
  }

  public List<DriveItem> getChildren(String folderId) {
    return getFiles(folderId);
  }

  public OneDriveStoredToken getStoredToken() {
    return storedToken;
  }

  public void refreshToken() {
    String refreshToken = storedToken.getRefreshToken();
    try {
      if(refreshToken!=null) {
        OneDriveTokenResponse oneDriveTokenResponse = retrieveAccessTokenByRefreshToken(refreshToken);
        this.accessToken = oneDriveTokenResponse.getToken();
      }
    } catch (IOException e) {
      LOG.info("unable to refresh token", e);
    }

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
    System.out.println("Content-Range " + "bytes " + startPosition + "-" + (startPosition + contentLength - 1) + "/" + size);
    con.setDoOutput(true);
    OutputStream outputStream = con.getOutputStream();
    outputStream.write(data);
    outputStream.flush();
    outputStream.close();

    FileSendResponse fileSendResponse = new FileSendResponse();
    fileSendResponse.responseMessage = con.getResponseMessage();
    fileSendResponse.responseCode = con.getResponseCode();

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();

    fileSendResponse.data = response.toString();
    System.out.println(fileSendResponse.responseCode + " " + fileSendResponse.responseMessage + " ");
    return fileSendResponse;
  }

  private byte[] readAllBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];

    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }

    return buffer.toByteArray();
  }

  private class FileSendResponse {
    int    responseCode;

    String responseMessage;

    String data;
  }

  private DriveItem retrieveDriveItemIfCreated(FileSendResponse fileSendResponse) {
    if (fileSendResponse.responseCode == 201) {
      JsonObject jsonDriveItem = new JsonParser().parse(fileSendResponse.data).getAsJsonObject();
      DriveItem createdFile = graphClient.me().drive().items(jsonDriveItem.get("id").getAsString()).buildRequest().get();
      return createdFile;
    }
    return null;
  }

  private String retrieveUploadUrl(String path, DriveItemUploadableProperties driveItemUploadableProperties) {
    try {
      return graphClient.me()
                        .drive()
                        .root()
                        .itemWithPath(URLEncoder.encode(path, "UTF-8"))
                        .createUploadSession(driveItemUploadableProperties)
                        .buildRequest()
                        .post().uploadUrl;
    } catch (UnsupportedEncodingException e) {
      LOG.info("unsupported encoding", e);
      return null;
    }
  }

  private DriveItemUploadableProperties prepareDriveItemUploadableProperties(String fileName,
                                                                             Calendar created,
                                                                             Calendar modified) {
    DriveItemUploadableProperties driveItemUploadableProperties = new DriveItemUploadableProperties();
    driveItemUploadableProperties.name = fileName;
    driveItemUploadableProperties.fileSystemInfo = new FileSystemInfo();
    driveItemUploadableProperties.fileSystemInfo.createdDateTime = created;
    driveItemUploadableProperties.fileSystemInfo.lastModifiedDateTime = modified;
    return driveItemUploadableProperties;
  }

  public DriveItem untrash(String fileId) {
    DirectoryObject directoryObject = graphClient.directory().deletedItems(fileId).restore().buildRequest().post();
    return getItem(fileId);
  }

  /**
   * @param isInsert indicates whether the file needs to be changed or added
   */
  private DriveItem insertUpdate(String path,
                                String fileName,
                                Calendar created,
                                Calendar modified,
                                String mimetype,
                                InputStream inputStream,
                                boolean isInsert) {
    DriveItemUploadableProperties driveItemUploadableProperties =
                                                                prepareDriveItemUploadableProperties(fileName, created, modified);
    String uploadUrl = retrieveUploadUrl(path, driveItemUploadableProperties);
    byte[] file;
    try {
      file = readAllBytes(inputStream);
    } catch (IOException e) {
      LOG.info("Unable to read all bytes from received inputstream", e);
      return null;
    }
    int bufferSize = 327680 * 40; // must be a multiple of 327680
    for (int i = 0; i < file.length / bufferSize + 1; i++) {
      int from = bufferSize * i;
      int to = bufferSize * (i + 1);
      if (to > file.length) {
        to = file.length;
      }
      byte[] fileSlice = Arrays.copyOfRange(file, from, to);
      FileSendResponse fileSendResponse = null;
      try {
        fileSendResponse = sendFile(uploadUrl, from, fileSlice.length, file.length, fileSlice);
      } catch (IOException e) {
        LOG.info("Cannot upload part of file. ", e);
        return null;
      }
      DriveItem driveItem = processEndOfFileUploadIfReached(fileSendResponse, isInsert);
      if (driveItem != null) {
        return driveItem;
      }
    }
    return null;
  }

  private DriveItem processEndOfFileUploadIfReached(FileSendResponse fileSendResponse, boolean isInsert) {
    DriveItem driveItem = null;
    if (isInsert) {
      driveItem = retrieveDriveItemIfCreated(fileSendResponse);
    } else {
      driveItem = retrieveDriveItemIfUpdated(fileSendResponse);
    }
    return driveItem;
  }

  private DriveItem retrieveDriveItemIfUpdated(FileSendResponse fileSendResponse) {
    if (fileSendResponse.responseCode == 200) {
      JsonObject jsonDriveItem = new JsonParser().parse(fileSendResponse.data).getAsJsonObject();
      DriveItem updatedFile = graphClient.me().drive().items(jsonDriveItem.get("id").getAsString()).buildRequest().get();
      return updatedFile;
    }
    return null;
  }

  public DriveItem insert(String path,
                          String fileName,
                          Calendar created,
                          Calendar modified,
                          String mimetype,
                          InputStream inputStream) {
    return insertUpdate(path, fileName, created, modified, mimetype, inputStream, true);
  }

  public DriveItem updateFileContent(String path,
                                     String fileName,
                                     Calendar created,
                                     Calendar modified,
                                     String mimetype,
                                     InputStream inputStream) {

    return insertUpdate(path, fileName, created, modified, mimetype, inputStream, false);
  }

  /**
   * @param driveItem
   * @return driveItem instance that contains only updated fields.
   */
  public DriveItem updateFile(DriveItem driveItem) {
    return graphClient.me().drive().items(driveItem.id).buildRequest().patch(driveItem);
  }

  public DriveItem getItem(String itemId) {
    return graphClient.me().drive().items(itemId).buildRequest().get();
  }

  public IDriveItemDeltaCollectionPage delta(String deltaToken) {
    IDriveItemDeltaCollectionPage iDriveItemDeltaCollectionPage = null;
    if (deltaToken == null || deltaToken.isEmpty()) {
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



  public ChildIterator getChildIterator(String folderId){
    return new ChildIterator(folderId);
  }

  class ChildIterator extends ChunkIterator<DriveItem> {

    /** The request. */
    final List<DriveItem> items;

    ChildIterator(String folderId)  {

      this.items = getChildren(folderId);
      // fetch first page
      iter = nextChunk();

    }

    @Override
    protected Iterator<DriveItem> nextChunk() {
      available(items.size());
      return items.iterator();
    }

    @Override
    protected boolean hasNextChunk()
    {
      return false;
//      return request.getPageToken() != null && request.getPageToken().length() > 0;
    }
  }

}
