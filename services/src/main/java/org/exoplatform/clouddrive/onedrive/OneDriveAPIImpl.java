package org.exoplatform.clouddrive.onedrive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.serializer.DefaultSerializer;
import com.microsoft.graph.serializer.ISerializer;

public class OneDriveAPIImpl implements OneDriveAPI {
  private static String TOKEN;

  static {
    try {
      TOKEN = new String(Files.readAllBytes(new File(System.getProperty("user.home") + "/authToken.txt").toPath()),Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Gson          gson        = new Gson();

  // TODO make local for every user
  IGraphServiceClient   graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
                                      iHttpRequest.getHeaders().add(new HeaderOption("Authorization", "Bearer " + TOKEN));
                                    }).buildClient();

  public List<DriveItem> getChildren() {
    return this.getChildren(null);
  }

  public List<DriveItem> getChildren(String fileId) {
    String pathSuffix = "";
    if (fileId == null || fileId.isEmpty()) {
      pathSuffix = "root";
    } else {
      pathSuffix = "items/" + fileId;
    }
    JsonObject itemsAsJson = retrieveDriveItems(pathSuffix);
    System.out.println(itemsAsJson.toString());
    List<DriveItem> items = parseFiles(itemsAsJson.getAsJsonArray("value"));
    return items;
  }

  @Override
  public void removeFile(String fileId) {
    graphClient.customRequest("/me/drive/items/"+fileId).buildRequest().delete();
  }

  @Override
  public void removeFolder(String fileId) {
    graphClient.customRequest("/me/drive/items/"+fileId).buildRequest().delete();
  }

  private List<DriveItem> parseFiles(JsonArray jsonObject) {
    List<DriveItem> baseItems = new ArrayList<>();
    jsonObject.forEach((item) -> {
      DriveItem baseItem = new DriveItem();
      ISerializer iSerializer = new DefaultSerializer(new DefaultLogger());
      baseItem.setRawObject(iSerializer, item.getAsJsonObject());
      DriveItem b = iSerializer.deserializeObject(item.toString(), DriveItem.class);
      baseItems.add(b);
    });
    return baseItems;
  }

  private JsonObject retrieveDriveItems(String pathSuffix) {
    return graphClient.customRequest("/me/drive/" + pathSuffix + "/children").buildRequest().get();
  }

}
