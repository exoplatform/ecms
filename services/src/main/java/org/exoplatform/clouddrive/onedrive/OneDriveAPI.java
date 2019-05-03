package org.exoplatform.clouddrive.onedrive;

import java.io.*;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequestBuilder;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OneDriveAPI {
  protected static final Log LOG = ExoLogger.getLogger(OneDriveAPI.class);

  private static String      TOKEN;

  static {
    try {
      TOKEN = new String(Files.readAllBytes(new File(System.getProperty("user.home") + "/authToken.txt").toPath()),
                         Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getToken() {
    try {
      return new String(Files.readAllBytes(new File(System.getProperty("user.home") + "/authToken.txt").toPath()),
                        Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Gson                gson        = new Gson();

  // TODO make local for every user
  private IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
                                            iHttpRequest.getHeaders()
                                                        .add(new HeaderOption("Authorization", "Bearer " + getToken()));
                                          }).buildClient();

  public void removeFolder(String fileId) {
    graphClient.me().drive().items(fileId).buildRequest().delete();
  }

  public void removeFile(String fileId) {
    graphClient.me().drive().items(fileId).buildRequest().delete();
  }

  public DriveItem createFolder(String parentId, String name, Calendar created) {
    if (parentId == null || parentId.isEmpty()) {
      parentId = graphClient.me().drive().root().buildRequest().get().id;
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
    if (fileSendResponse.responseCode == 201 && "Created".equals(fileSendResponse.responseMessage.trim())) {
      JsonObject jsonDriveItem = new JsonParser().parse(fileSendResponse.data).getAsJsonObject();
      DriveItem createdFile = graphClient.me().drive().items(jsonDriveItem.get("id").getAsString()).buildRequest().get();
      return createdFile;
    }
    return null;
  }

  private String retrieveUploadUrl(String path, DriveItemUploadableProperties driveItemUploadableProperties) {
    return graphClient.me()
                      .drive()
                      .root()
                      .itemWithPath(path)
                      .createUploadSession(driveItemUploadableProperties)
                      .buildRequest()
                      .post().uploadUrl;
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

  public DriveItem insert(String path,
                          String fileName,
                          Calendar created,
                          Calendar modified,
                          String mimetype,
                          InputStream inputStream) {
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
      System.out.println("send from : " + from + " " + fileSlice.length + " bytes  to " + (from + fileSlice.length));
      FileSendResponse fileSendResponse = null;
      try {
        fileSendResponse = sendFile(uploadUrl, from, fileSlice.length, file.length, fileSlice);
      } catch (IOException e) {
        LOG.info("Cannot upload part of file. ", e);
        return null;
      }
      DriveItem driveItem = retrieveDriveItemIfCreated(fileSendResponse);
      if (driveItem != null) {
        return driveItem;
      }

    }
    return null;
  }


}
