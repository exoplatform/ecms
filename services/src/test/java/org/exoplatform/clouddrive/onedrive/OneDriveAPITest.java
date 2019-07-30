//package org.exoplatform.clouddrive.onedrive;
//
//import static org.exoplatform.clouddrive.onedrive.TestUtil.getRefreshToken;
//import static org.exoplatform.clouddrive.onedrive.TestUtil.retrieveAccessToken;
//import static org.junit.Assert.*;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.junit.*;
//import org.junit.rules.ExpectedException;
//
//import com.microsoft.graph.http.GraphServiceException;
//import com.microsoft.graph.models.extensions.*;
//import com.microsoft.graph.options.HeaderOption;
//import com.microsoft.graph.requests.extensions.GraphServiceClient;
//
//import org.exoplatform.clouddrive.CloudDriveException;
//@Ignore
//public class OneDriveAPITest {
//    static IGraphServiceClient graphClient;
//    static OneDriveAPI oneDriveAPI;
//
//    @BeforeClass
//    public static void init() throws IOException, CloudDriveException {
//        initGraphClient();
//        initOneDriveAPI();
//    }
//
//    private static void initOneDriveAPI() throws IOException, CloudDriveException {
//        oneDriveAPI = new OneDriveAPI("9920cb10-7801-49d8-9a75-2d8252eae87c", "|)A!*f.+*:k.@%e{D$&[0|2%)()U=|Q9", "", getRefreshToken(), 10,"");
//    }
//
//    private static void initGraphClient() {
//        graphClient = GraphServiceClient.builder().authenticationProvider(iHttpRequest -> {
//            String accessToken = retrieveAccessToken();
//            iHttpRequest.getHeaders().add(new HeaderOption("Authorization", "Bearer " + accessToken));
//        }).buildClient();
//    }
//
//
//    @Test
//    public void getRoot() throws IOException, CloudDriveException {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        assertEquals(rootId, oneDriveAPI.getRootId());
//    }
//
//    @Test
//    public void getUser() {
//        User expectedUser = graphClient.me().buildRequest().get();
//        User actualUser = oneDriveAPI.getUser();
//        assertEquals(expectedUser.id, actualUser.id);
//        assertEquals(expectedUser.userPrincipalName, actualUser.userPrincipalName);
//    }
//
//
//    @Before
//    public void initOneDriveAPIRemoveItems() {
//     removeAll();
//    }
//
//    private void removeAll() {
//        List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();
//        for (DriveItem item : items) {
//            graphClient.me().drive().items(item.id).buildRequest().delete();
//        }
//    }
//
//    @Test
//    public void insertShouldReturnAppropriateItem() throws Exception {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        byte[] testFile = new byte[]{1, 2, 3, -2, 4, 2, 2};
//        String path = "/file.txt";
//        String fileName = "file.txt";
//        Calendar created = Calendar.getInstance();
//        Calendar modified = Calendar.getInstance();
//        DriveItem item = oneDriveAPI.insert(rootId, fileName, created, modified, new ByteArrayInputStream(testFile));
//
//        assertEquals(fileName, item.name);
//        assertEquals(testFile.length, (long) item.size);
////        assertEquals(created.getTimeInMillis(), item.fileSystemInfo.createdDateTime.getTimeInMillis());
////        assertEquals(modified.getTimeInMillis(), item.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
//        assertEquals(rootId, item.parentReference.id);
//
////        assertEquals(created,item.createdDateTime);
////        assertEquals(mimetype,item.file.mimeType);
//
//
//    }
//
//    @Test
//    public void insertShouldCreateFile() throws Exception {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        byte[] testFile = new byte[]{1, 2, 3, -2, 4, 2, 2};
//        String fileName = "file.txt";
//        Calendar created = Calendar.getInstance();
//        Calendar modified = Calendar.getInstance();
//
//        DriveItem expectedItem = oneDriveAPI.insert(rootId, fileName, created, modified, new ByteArrayInputStream(testFile));
//        List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();
//
//        assertEquals(1, items.size());
//        DriveItem actualItem = items.get(0);
//        assertEquals(expectedItem.size, actualItem.size);
//        assertEquals(expectedItem.name, actualItem.name);
//        assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
//        assertEquals(expectedItem.id, actualItem.id);
////        assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(), actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
////        assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(), actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
//
//    }
//
//    private DriveItem addFile(String parentId, String fileName, byte[] testFile) {
//        return graphClient.me().drive().items(parentId + ":/" + fileName + ":").content().buildRequest().put(testFile);
//    }
//
//    private List<DriveItem> addThreeFiles() {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        String fileName1 = "file1.test";
//        String fileName2 = "file2.test";
//        String fileName3 = "file3.test";
//        byte[] testFile1 = new byte[]{2, 3, 4, 100, -22, -23, 54};
//        byte[] testFile2 = new byte[]{12, 32, 42, 100, 54};
//        byte[] testFile3 = new byte[]{2, 113, 4, 99, 54};
//        List<DriveItem> items = new ArrayList<>();
//
//        items.add(addFile(rootId, fileName1, testFile1));
//        items.add(addFile(rootId, fileName2, testFile2));
//        items.add(addFile(rootId, fileName3, testFile3));
//        return items;
//    }
//
//    @Rule
//    public final ExpectedException exception = ExpectedException.none();
//
//    @Test
//    public void removeFile() {
//        List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
//        String removedDriveItemId = items.remove(0);
//        List<String> expectedItems = items;
//        oneDriveAPI.removeFile(removedDriveItemId);
//        List<String> actualItems = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage().stream().map((item) -> item.id).collect(Collectors.toList());
//        assertTrue(expectedItems.size() == actualItems.size() &&
//                expectedItems.containsAll(actualItems) && actualItems.containsAll(expectedItems));
//
//        exception.expect(GraphServiceException.class);
//        graphClient.me().drive().items(removedDriveItemId).buildRequest().get();
//
//    }
//
//    @Test
//    public void children() throws CloudDriveException {
//        List<String> expectedItems = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        OneDriveAPI.ChildIterator ch = oneDriveAPI.getChildIterator(rootId);
//        List<String> actualItems = new ArrayList<>();
//        while (ch.hasNext()) {
//            actualItems.add(ch.next().id);
//        }
//        assertTrue(expectedItems.size() == actualItems.size() &&
//                expectedItems.containsAll(actualItems) && actualItems.containsAll(expectedItems));
//    }
////    @Test
////    public void copyFile() {
////        oneDriveAPI.copyFile()
////    }
//
//
//
//    // createFolder return appropriate
//    @Test
//    public void createFolderShouldReturnAprrortiateItem() {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        String fileName = "f1.test";
//        Calendar created = Calendar.getInstance();
//        DriveItem item = oneDriveAPI.createFolder(rootId,fileName,created);
//
//        assertEquals(rootId,item.parentReference.id);
//        assertEquals(fileName,item.name);
////        assertEquals(created.getTimeInMillis(),item.fileSystemInfo.createdDateTime.getTimeInMillis());
//        assertNotNull(item.folder);
//    }
//
//    @Test
//    public void createFolder() {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        DriveItem expectedItem = oneDriveAPI.createFolder(rootId,"createdFile.test",Calendar.getInstance());
//        List<DriveItem> items = graphClient.me().drive().root().children().buildRequest().get().getCurrentPage();
//        DriveItem actualItem = items.get(0);
//        assertEquals(1,items.size());
//        assertNotNull(actualItem.folder);
//
//        assertEquals(expectedItem.id,actualItem.id);
//        assertEquals(expectedItem.size, actualItem.size);
//        assertEquals(expectedItem.name, actualItem.name);
//        assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
//        assertEquals(expectedItem.id, actualItem.id);
//        assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(), actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
//        assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(), actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
//    }
//    @Test
//    public void createLink() {
//        List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
//        String itemId = items.get(1);
//        String expectedLink = oneDriveAPI.createLink(itemId,"embed").webUrl;
//        String actualLink = graphClient.me().drive().items(itemId).createLink("embed", null).buildRequest().post().link.webUrl;
//
//        assertEquals(expectedLink,actualLink);
//    }
//
//    // appropriate
//
//    @Test
//    public void getItemShouldReturnRightItem() {
//        List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
//        String itemId = items.get(0);
//        DriveItem actualItem = oneDriveAPI.getItem(itemId);
//        assertEquals(itemId,actualItem.id);
//    }
//    @Test
//    public void getItem() {
//        List<String> items = addThreeFiles().stream().map((item) -> item.id).collect(Collectors.toList());
//        String itemId = items.get(0);
//        DriveItem expectedItem = oneDriveAPI.getItem(itemId);
//        DriveItem actualItem = graphClient.me().drive().items(itemId).buildRequest().get();
//        assertEquals(expectedItem.id,actualItem.id);
//        assertEquals(expectedItem.size, actualItem.size);
//        assertEquals(expectedItem.name, actualItem.name);
//        assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
//        assertEquals(expectedItem.id, actualItem.id);
//        assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(), actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
//        assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(), actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
//    }
//
//    // should return updated file
//
//
//    private DriveItem renamedItem(DriveItem driveItem, String name) {
//        DriveItem item = new DriveItem();
//        item.id = driveItem.id;
//        item.name = name;
//        item.parentReference = new ItemReference();
//        item.parentReference.id = graphClient.me().drive().root().buildRequest().get().id;
//        return item;
//    }
//    @Test
//    public void shouldReturnRenamedFile() throws URISyntaxException {
//        List<DriveItem> items = addThreeFiles();
//        DriveItem driveItem = items.get(0);
//        DriveItem expectedItem = renamedItem(driveItem,"renF.test");
//        DriveItem actualItem = oneDriveAPI.updateFile(expectedItem);
//        assertEquals(expectedItem.name,actualItem.name);
//    }
//    @Test
//    public void renameFile() throws URISyntaxException {
//        List<DriveItem> items = addThreeFiles();
//        DriveItem item = renamedItem(items.get(0),"renFile.test");
//
//        DriveItem expectedItem = oneDriveAPI.updateFile(item);
//        DriveItem actualItem = graphClient.me().drive().items(item.id).buildRequest().get();
//
//        assertEquals(expectedItem.id,actualItem.id);
//        assertEquals(expectedItem.size, actualItem.size);
//        assertEquals(expectedItem.name, actualItem.name);
//        assertEquals(expectedItem.parentReference.id, actualItem.parentReference.id);
//        assertEquals(expectedItem.id, actualItem.id);
//        assertEquals(expectedItem.fileSystemInfo.createdDateTime.getTimeInMillis(), actualItem.fileSystemInfo.createdDateTime.getTimeInMillis());
//        assertEquals(expectedItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis(), actualItem.fileSystemInfo.lastModifiedDateTime.getTimeInMillis());
//
//    }
//    @Test
//    public void moveFileShouldReturnAppropriateItem() throws CloudDriveException, URISyntaxException {
//        DriveItem destFolder = addThreeFolders().get(0);
//        DriveItem srcItem = addThreeFiles().get(0);
//        srcItem.parentReference.id = destFolder.id;
//        DriveItem destItem = oneDriveAPI.updateFile(srcItem);
//
//        assertEquals(srcItem.id,destItem.id);
//        assertEquals(srcItem.name,destItem.name);
//        assertEquals(destFolder.id,destItem.parentReference.id);
//    }
//
//
//    @Test
//    public void moveFile() throws CloudDriveException, URISyntaxException {
//        DriveItem destFolder = addThreeFolders().get(0);
//        DriveItem srcItem = addThreeFiles().get(0);
//        srcItem.parentReference.id = destFolder.id;
//        oneDriveAPI.updateFile(srcItem);
//        DriveItem movedItem = graphClient.me().drive().items(srcItem.id).buildRequest().get();
//
//        assertEquals(srcItem.name,movedItem.name);
//        assertEquals(destFolder.id,movedItem.parentReference.id);
//    }
//
//    private List<DriveItem> addThreeFolders() {
//        String rootId = graphClient.me().drive().root().buildRequest().get().id;
//        String folderName1 = "fol1";
//        String folderName2 = "fol1";
//        String folderName3 = "fol1";
//        List<DriveItem> folders = new ArrayList<>();
//        folders.add(addFolder(rootId,folderName1));
//        folders.add(addFolder(rootId,folderName2));
//        folders.add(addFolder(rootId,folderName3));
//        return folders;
//    }
//
//    private DriveItem addFolder(String parentId, String name) {
//        DriveItem folder = new DriveItem();
//        folder.name = name;
//        folder.parentReference = new ItemReference();
//        folder.parentReference.id = parentId;
//        folder.folder = new Folder();
//        return graphClient.me().drive().items(parentId).children().buildRequest().post(folder);
//    }
//}
