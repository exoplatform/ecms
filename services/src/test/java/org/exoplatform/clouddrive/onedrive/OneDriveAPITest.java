package org.exoplatform.clouddrive.onedrive;

import static org.exoplatform.clouddrive.onedrive.TestUtil.getRefreshToken;
import static org.exoplatform.clouddrive.onedrive.TestUtil.retrieveAccessToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.Folder;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.RefreshAccessException;


public class OneDriveAPITest {
  static IGraphServiceClient     graphClient;

  static OneDriveAPI             oneDriveAPI;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @BeforeClass
  public static void init() throws IOException, CloudDriveException {
    initGraphClient();
    initOneDriveAPI();
  }

  private static void initOneDriveAPI() throws IOException, CloudDriveException {
    Properties properties = new Properties();
    properties.load(ClassLoader.getSystemClassLoader().getResourceAsStream("onedrive.properties"));

    oneDriveAPI = new OneDriveAPI(properties.getProperty("clientId"),
                                 properties.getProperty("clientSecret"),
                                  "",
                                  getRefreshToken(),
                                  10,
                                  "");
  }

  private static void initGraphClient() {
    graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
      String accessToken = retrieveAccessToken();
      iHttpRequest.getHeaders().add(new HeaderOption("Authorization", "Bearer " + accessToken));
    }).buildClient();
  }

  @Before
  public void initOneDriveAPIRemoveItems() {
    removeAll();
  }

  @Test
  public void getRoot() throws IOException, CloudDriveException {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    assertEquals(rootId, oneDriveAPI.getRootId());
  }

  @Test
  public void getUser() throws IOException {
    User expectedUser = graphClient.me().buildRequest().get();
    User actualUser = oneDriveAPI.getUser();
    assertEquals(expectedUser.id, actualUser.id);
    assertEquals(expectedUser.userPrincipalName, actualUser.userPrincipalName);
  }

  // ...........................................................insert....................................................
  // check rename or fail
  // some more test data
  // for rename and fail

  @Test
  public void insertShouldReturnAppropriateItem() throws Exception {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    byte[] testFile = new byte[] { 1, 2, 3, -2, 4, 2, 2 };
    String fileName = "file.txt";
    Calendar created = Calendar.getInstance();
    Calendar modified = Calendar.getInstance();
    DriveItem item = oneDriveAPI.insert(rootId, fileName, created, modified, new ByteArrayInputStream(testFile), "rename");

    assertEquals(fileName, item.name);
    assertEquals(testFile.length, (long) item.size);
    assertEquals(rootId, item.parentReference.id);

  }

  @Test
  public void insertShouldCreateFile() throws Exception {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    byte[] testFile = new byte[] { 1, 2, 3, -2, 4, 2, 2 };
    String fileName = "file.txt";
    Calendar created = Calendar.getInstance();
    Calendar modified = Calendar.getInstance();

    DriveItem expectedItem =
                           oneDriveAPI.insert(rootId, fileName, created, modified, new ByteArrayInputStream(testFile), "rename");
    List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();

    assertEquals(1, items.size());
    DriveItem actualItem = items.get(0);
    assertEquals(expectedItem.size, actualItem.size);
    assertEquals(expectedItem.name, actualItem.name);
    assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
    assertEquals(expectedItem.id, actualItem.id);

  }
    // ...........................................................createFolder............................................
    @Test
    public void createFolderShouldReturnAprrortiateItem() {
        String rootId = graphClient.me().drive().root().buildRequest().get().id;
        String fileName = "f1.test";
        Calendar created = Calendar.getInstance();
        DriveItem item = oneDriveAPI.createFolder(rootId, fileName, created);

        assertEquals(rootId, item.parentReference.id);
        assertEquals(fileName, item.name);
        assertNotNull(item.folder);
    }

    @Test
    public void createFolder() {
        String rootId = graphClient.me().drive().root().buildRequest().get().id;
        DriveItem expectedItem = oneDriveAPI.createFolder(rootId, "createdFile.test", Calendar.getInstance());
        List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();
        DriveItem actualItem = items.get(0);



        assertEquals(1, items.size());
        assertNotNull(actualItem.folder);
        assertEquals(expectedItem.id, actualItem.id);
        assertEquals(expectedItem.size, actualItem.size);
        assertEquals(expectedItem.name, actualItem.name);
        assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
        assertEquals(expectedItem.id, actualItem.id);
        assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(),
                actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
        assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(),
                actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
    }

  // .....................................................removeFile......................................................

  @Test
  public void removeFile() throws InterruptedException {
    List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
    String removedDriveItemId = items.remove(0);
    List<String> expectedItems = items;
    oneDriveAPI.removeFile(removedDriveItemId);
    List<String> actualItems = graphClient.me()
                                          .drive()
                                          .root()
                                          .children()
                                          .buildRequest()
                                          .get()
                                          .getCurrentPage()
                                          .stream()
                                          .map((item) -> item.id)
                                          .collect(Collectors.toList());
    assertTrue(expectedItems.size() == actualItems.size() && expectedItems.containsAll(actualItems)
        && actualItems.containsAll(expectedItems));

    exception.expect(GraphServiceException.class);
    graphClient.me().drive().items(removedDriveItemId).buildRequest().get();
//

  }
    // .........................................................children..................................................
  @Test
  public void children() throws CloudDriveException {
    List<String> expectedItems = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    OneDriveAPI.ChildIterator ch = oneDriveAPI.getChildIterator(rootId);
    List<String> actualItems = new ArrayList<>();
    while (ch.hasNext()) {
      actualItems.add(ch.next().id);
    }
    assertTrue(expectedItems.size() == actualItems.size() && expectedItems.containsAll(actualItems)
        && actualItems.containsAll(expectedItems));
  }
    // .........................................................createLink................................................

    // for embed and view
  @Test
  public void createLink() throws OneDriveException {
    List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
    String itemId = items.get(1);
    String expectedLink = oneDriveAPI.createLink(itemId, "embed").webUrl;
    String actualLink = graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link.webUrl;

    assertEquals(expectedLink, actualLink);
  }

    // .........................................................getItem...................................................

  @Test
  public void getItemShouldReturnAppropriateItem() {
    List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
    String itemId = items.get(0);
    DriveItem actualItem = oneDriveAPI.getItem(itemId);
    assertEquals(itemId, actualItem.id);
  }

  @Test
  public void getItem() {
    List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
    String itemId = items.get(0);
    DriveItem expectedItem = oneDriveAPI.getItem(itemId);
    DriveItem actualItem = graphClient.me().drive().items(itemId).buildRequest().get();
    assertEquals(expectedItem.id, actualItem.id);
    assertEquals(expectedItem.size, actualItem.size);
    assertEquals(expectedItem.name, actualItem.name);
    assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
    assertEquals(expectedItem.id, actualItem.id);
    assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(),
                 actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
    assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(),
                 actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
  }

    // .........................................................updateFile................................................

  @Test
  public void renameFile() {
    List<DriveItem> items = addThreeFiles();
    DriveItem item = renamedItem(items.get(0), "renFile.test");

    DriveItem expectedItem = oneDriveAPI.updateFile(item);
    DriveItem actualItem = graphClient.me().drive().items(item.id).buildRequest().get();

    assertEquals(expectedItem.id, actualItem.id);
    assertEquals(expectedItem.size, actualItem.size);
    assertEquals(expectedItem.name, actualItem.name);
    assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
    assertEquals(expectedItem.id, actualItem.id);


  }
  @Test
  public void moveFileShouldReturnAppropriateItem() throws CloudDriveException, URISyntaxException {
    DriveItem destFolder = addThreeFolders().get(0);
    DriveItem srcItem = addThreeFiles().get(0);
    srcItem.parentReference.id = destFolder.id;
    DriveItem destItem = oneDriveAPI.updateFile(srcItem);

    assertEquals(srcItem.id, destItem.id);
    assertEquals(srcItem.name, destItem.name);
    assertEquals(destFolder.id, destItem.parentReference.id);
  }

  @Test
  public void moveFile() throws CloudDriveException, URISyntaxException {
    DriveItem destFolder = addThreeFolders().get(0);
    DriveItem srcItem = addThreeFiles().get(0);
    srcItem.parentReference.id = destFolder.id;
    oneDriveAPI.updateFile(srcItem);
    DriveItem movedItem = graphClient.me().drive().items(srcItem.id).buildRequest().get();

    assertEquals(srcItem.name, movedItem.name);
    assertEquals(destFolder.id, movedItem.parentReference.id);
  }

  // .........................................................copy........................................................

  // TODO changes
  // copyShouldCreateFileAtDestination
  @Test
  public void copyShouldCreateFileAtDestination() throws OneDriveException, RefreshAccessException {

      DriveItem testFolder = addThreeFolders().get(0);
      String parentId = testFolder.id;
      DriveItem testFile = addThreeFiles().get(0);
      String fileId = testFile.id;
      String fileName = testFile.name;


      oneDriveAPI.copy(parentId,fileName,fileId,true);
      List<DriveItem> testFolderChildren = graphClient.me().drive().items(testFolder.id).children().buildRequest().get().getCurrentPage();
      assertEquals(1,testFolderChildren.size());
      DriveItem copiedFile = testFolderChildren.get(0);
      assertEquals(copiedFile.name, testFile.name);
      assertEquals(copiedFile.size, testFile.size);
  }


  @Test
  public void copyShouldNotRemoveItem() throws OneDriveException, RefreshAccessException {
    DriveItem testFolder = addThreeFolders().get(0);
    String parentId = testFolder.id;
    DriveItem testFile = addThreeFiles().get(0);
    String testFileId = testFile.id;
    String testFileName = testFile.name;

    oneDriveAPI.copy(parentId,testFileName,testFileId,true);
    boolean isFilePresent = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage().stream().anyMatch(item -> item.id.equals(testFileId));
    assertTrue(isFilePresent);
  }

  // .........................................................getAllFiles.................................................

  @Test
  public void getAllFiles() {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    List<DriveItem> files = addThreeFiles();
    List<DriveItem> actualItems = oneDriveAPI.getAllFiles().getItems().stream().filter((item) -> !rootId.equals(item.id)).collect(Collectors.toList());


    assertEquals(files.size(),actualItems.size());

    //need compare two arrays

  }

  // .........................................................changes.....................................................

  @Test
  public void changes() {
//    final QueryOption deltaTokenQuery = new QueryOption("token", deltaToken);
//    collectionPage = graphClient.me()
//            .drive()
//            .root()
//            .delta()
//            .buildRequest(Collections.singletonList(deltaTokenQuery))
//            .get();
    oneDriveAPI.changes("");
  }
  private List<DriveItem> addThreeFolders() {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    String folderName1 = "fol1";
    String folderName2 = "fol2";
    String folderName3 = "fol3";
    List<DriveItem> folders = new ArrayList<>();
    folders.add(addFolder(rootId, folderName1));
    folders.add(addFolder(rootId, folderName2));
    folders.add(addFolder(rootId, folderName3));
    return folders;
  }

  private DriveItem addFolder(String parentId, String name) {
    DriveItem folder = new DriveItem();
    folder.name = name;
    folder.parentReference = new ItemReference();
    folder.parentReference.id = parentId;
    folder.folder = new Folder();
    return graphClient.me().drive().items(parentId).children().buildRequest().post(folder);
  }

  private DriveItem addFile(String parentId, String fileName, byte[] testFile) {
    return graphClient.me().drive().items(parentId + ":/" + fileName + ":").content().buildRequest().put(testFile);
  }

  private void removeAll() {
    List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();
    for (DriveItem item : items) {
      graphClient.me().drive().items(item.id).buildRequest().delete();
    }
  }

  private List<DriveItem> addThreeFiles() {
    String rootId = graphClient.me().drive().root().buildRequest().get().id;
    String fileName1 = "file1.test";
    String fileName2 = "file2.test";
    String fileName3 = "file3.test";
    byte[] testFile1 = new byte[] { 2, 3, 4, 100, -22, -23, 54 };
    byte[] testFile2 = new byte[] { 12, 32, 42, 100, 54 };
    byte[] testFile3 = new byte[] { 2, 113, 4, 99, 54 };
    List<DriveItem> items = new ArrayList<>();

    items.add(addFile(rootId, fileName1, testFile1));
    items.add(addFile(rootId, fileName2, testFile2));
    items.add(addFile(rootId, fileName3, testFile3));
    return items;
  }

    private DriveItem renamedItem(DriveItem driveItem, String name) {
        DriveItem item = new DriveItem();
        item.id = driveItem.id;
        item.name = name;
        item.parentReference = new ItemReference();
        item.parentReference.id = graphClient.me().drive().root().buildRequest().get().id;
        return item;
    }

    public void shouldReturnRenamedFile() throws URISyntaxException {
        List<DriveItem> items = addThreeFiles();
        DriveItem driveItem = items.get(0);
        DriveItem expectedItem = renamedItem(driveItem, "renF.test");
        DriveItem actualItem = oneDriveAPI.updateFile(expectedItem);
        assertEquals(expectedItem.name, actualItem.name);
    }
}
