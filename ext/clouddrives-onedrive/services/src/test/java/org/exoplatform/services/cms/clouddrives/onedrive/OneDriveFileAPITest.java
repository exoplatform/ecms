package org.exoplatform.services.cms.clouddrives.onedrive;

import static org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive.ECD_CLOUDDRIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.File;
import com.microsoft.graph.models.extensions.FileSystemInfo;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.SharingLink;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.CloudUser;
import org.exoplatform.services.cms.clouddrives.SkipSyncException;
import org.exoplatform.services.cms.clouddrives.SyncNotSupportedException;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.clouddrives.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.mockito.Mockito;


public class OneDriveFileAPITest {
    private JCRLocalOneDrive.OneDriveFileAPI oneDriveFileAPI;
    private OneDriveAPI oneDriveAPI;
    private JCRLocalOneDrive jcrLocalOneDrive;

    @Before
    public void before() throws RepositoryException, CloudDriveException {
        OneDriveProvider oneDriveProvider = new OneDriveProvider("id", "onedrive", "auth", "s");
        oneDriveAPI = mock(OneDriveAPI.class, RETURNS_DEEP_STUBS);

        when(oneDriveAPI.getStoredToken()).thenReturn(mock(OneDriveStoredToken.class));
        CloudUser cloudUser = new OneDriveUser("s","s","s",oneDriveProvider,oneDriveAPI);

        Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        when(session.getRepository()).thenReturn(mock(ManageableRepository.class));
        when(session.getWorkspace().getName()).thenReturn("workspaceName");

        Node driveNode = mock(Node.class, RETURNS_DEEP_STUBS);
        when(driveNode.getSession()).thenReturn(session);
        when(driveNode.isNodeType(ECD_CLOUDDRIVE)).thenReturn(false);
        when(driveNode.hasProperty("exo:title")).thenReturn(true);


        SessionProviderService sessionProviderService = mock(SessionProviderService.class);
        NodeFinder nodeFinder = mock(NodeFinder.class);
        ExtendedMimeTypeResolver extendedMimeTypeResolver = mock(ExtendedMimeTypeResolver.class);
        jcrLocalOneDrive = Mockito.spy(new JCRLocalOneDrive(cloudUser, driveNode, sessionProviderService, nodeFinder, extendedMimeTypeResolver));
        oneDriveFileAPI = jcrLocalOneDrive.createFileAPI();

    }
    @Test
    public void removeFile()  {
        String testItemId = "testItemId";
        oneDriveFileAPI.removeFile(testItemId);
        verify(oneDriveAPI, times(1)).removeFile(testItemId);
    }
    @Test
    public void removeFolder()  {
        String testItemId = "testItemId";
        oneDriveFileAPI.removeFolder(testItemId);
        verify(oneDriveAPI, times(1)).removeFolder(testItemId);
    }
    @Test
    public void createFile() throws Exception {
        Calendar calendar = Calendar.getInstance();
        String parentId = "dparentId";
        String nodeId = "dparentId";
        String nodeName = "dtestName";
        String nodeLink = "dtestWebUrl";
        String nodePath = "dtestPath";
        String mimetype = "";
        Calendar nodeCreated = calendar;
        Calendar nodeModified = calendar;
        InputStream content = new ByteArrayInputStream(new byte[] {1,2,3});

        Node fileNode  = mock(Node.class,RETURNS_DEEP_STUBS);
        when(fileNode.getParent().getProperty("ecd:id").getString()).thenReturn(parentId);
        when(fileNode.getProperty("exo:title").getString()).thenReturn(nodeName);
        when(fileNode.getPath()).thenReturn(nodePath);

        DriveItem testItem = new DriveItem();
        testItem.id = nodeId;
        testItem.name = nodeName;
        testItem.webUrl = nodeLink;
        testItem.createdDateTime = nodeCreated;
        testItem.lastModifiedDateTime = nodeCreated;
        testItem.file = new File();
        testItem.file.mimeType = mimetype;
        testItem.size = 0L;
        SharingLink sharingLink = new SharingLink();
        sharingLink.type = "";
        sharingLink.webUrl = "";

        doReturn(sharingLink).when(jcrLocalOneDrive).createLink(any(DriveItem.class));
        when(oneDriveAPI.insert(anyString(), anyString(), any(Calendar.class), any(Calendar.class), any(InputStream.class), anyString())).thenReturn(testItem);
//        doNothing().when(jcrLocalOneDrive).initCreateFile(any(Node.class),any(DriveItem.class));
        CloudFile cloudFile = oneDriveFileAPI.createFile(fileNode,nodeCreated,nodeModified, mimetype, content);

        verify(oneDriveAPI).insert(parentId,nodeName,nodeCreated,nodeModified,content, "rename");
        verify(jcrLocalOneDrive).initCreateFile(fileNode,testItem);
        assertEquals(nodeId,cloudFile.getId());
        assertEquals(nodePath,cloudFile.getPath());
        assertEquals(nodeName,cloudFile.getTitle());
        assertEquals(testItem.createdDateTime,cloudFile.getCreatedDate());
        assertEquals(testItem.lastModifiedDateTime,cloudFile.getModifiedDate());


    }

    // rewrite
    @Test
    public void createFolder() throws Exception {

        Calendar calendar = Calendar.getInstance();
        String parentId = "dparentId";
        String nodeId = "dparentId";
        String nodeName = "dtestName";
        String nodeLink = "dtestWebUrl";
        String nodePath = "dtestPath";
        Calendar nodeCreated = calendar;
        Calendar nodeModified = calendar;

        Node fileNode  = mock(Node.class,RETURNS_DEEP_STUBS);
        when(fileNode.getParent().getProperty("ecd:id").getString()).thenReturn(parentId);
        when(fileNode.getProperty("exo:title").getString()).thenReturn(nodeName);
        when(fileNode.getPath()).thenReturn(nodePath);


        DriveItem testItem = new DriveItem();
        testItem.id = nodeId;
        testItem.name = nodeName;
        testItem.webUrl = nodeLink;
        testItem.createdDateTime = nodeCreated;
        testItem.lastModifiedDateTime = nodeCreated;
        when(oneDriveAPI.createFolder(parentId,nodeName,calendar)).thenReturn(testItem);
        doNothing().when(jcrLocalOneDrive).initFolderByDriveItem(any(Node.class),any(DriveItem.class));
        CloudFile cloudFile = oneDriveFileAPI.createFolder(fileNode,calendar);

        verify(oneDriveAPI).createFolder(parentId,nodeName,calendar);
        verify(jcrLocalOneDrive).initFolderByDriveItem(fileNode,testItem);

        assertEquals(nodeId,cloudFile.getId());
        assertEquals(nodePath,cloudFile.getPath());
        assertEquals(nodeName,cloudFile.getTitle());
        assertEquals(nodeLink,cloudFile.getLink());
        assertEquals("folder",cloudFile.getType());
        assertEquals(testItem.createdDateTime,cloudFile.getCreatedDate());
        assertEquals(testItem.lastModifiedDateTime,cloudFile.getModifiedDate());


    }
    @Test
    public void copyFile() throws RepositoryException, CloudDriveException, IOException {
        Calendar calendar = Calendar.getInstance();
        String parentId = "nodeParentId";
        String nodeId = "dparentId";
        String nodeName = "dtestName";
        String nodeLink = "dtestWebUrl";
        String nodePath = "dtestPath";
        String mimetype = "";
        Calendar nodeCreated = calendar;
        Calendar nodeModified = calendar;

        Node destFileNode  = mock(Node.class,RETURNS_DEEP_STUBS);
        when(destFileNode.getParent().getProperty("ecd:id").getString()).thenReturn(parentId);
        when(destFileNode.getProperty("exo:title").getString()).thenReturn(nodeName);
        when(destFileNode.getPath()).thenReturn(nodePath);
        String srcFileNodeId = "scrNodeId";
        Node scrFileNode  = mock(Node.class,RETURNS_DEEP_STUBS);

        when(scrFileNode.getProperty("ecd:id").getString()).thenReturn(srcFileNodeId);


        DriveItem testItem = new DriveItem();
        testItem.id = nodeId;
        testItem.name = nodeName;
        testItem.webUrl = nodeLink;
        testItem.createdDateTime = nodeCreated;
        testItem.lastModifiedDateTime = nodeCreated;
        testItem.file = new File();
        testItem.file.mimeType = mimetype;
        testItem.size = 0L;
        SharingLink sharingLink = new SharingLink();
        sharingLink.type = "";
        sharingLink.webUrl = "";
        doReturn(sharingLink).when(jcrLocalOneDrive).createLink(any(DriveItem.class));


        when(oneDriveAPI.copyFile(anyString(), anyString(), anyString())).thenReturn(testItem);

        CloudFile cloudFile = oneDriveFileAPI.copyFile(scrFileNode,destFileNode);

        verify(oneDriveAPI).copyFile(parentId,nodeName,srcFileNodeId);
        verify(jcrLocalOneDrive).initCreateFile(destFileNode,testItem);
        assertEquals(nodeId,cloudFile.getId());
        assertEquals(nodePath,cloudFile.getPath());
        assertEquals(nodeName,cloudFile.getTitle());
        assertEquals(testItem.createdDateTime,cloudFile.getCreatedDate());
        assertEquals(testItem.lastModifiedDateTime,cloudFile.getModifiedDate());


    }
    @Test
    public void updateFile() throws RepositoryException, SkipSyncException, OneDriveException {

        Calendar calendar = Calendar.getInstance();
        String parentId = "dparentId";
        String nodeId = "dparentId";
        String nodeName = "dtestName";
        String nodeLink = "dtestWebUrl";
        String nodePath = "dtestPath";
        String mimetype = "";
        Calendar nodeCreated = calendar;
        Calendar nodeModified = calendar;

        DriveItem testItem = new DriveItem();
        testItem.id = nodeId;
        testItem.name = nodeName;
        testItem.webUrl = nodeLink;
        testItem.createdDateTime = nodeCreated;
        testItem.lastModifiedDateTime = nodeCreated;
        testItem.file = new File();
        testItem.file.mimeType = mimetype;
        testItem.size = 0L;
        SharingLink sharingLink = new SharingLink();
        sharingLink.type = "";
        sharingLink.webUrl = "";
        doReturn(sharingLink).when(jcrLocalOneDrive).createLink(any(DriveItem.class));

        Node fileNode  = mock(Node.class,RETURNS_DEEP_STUBS);
        when(fileNode.getParent()  .getProperty("ecd:id").getString()).thenReturn(parentId);
        when(fileNode.getProperty("exo:title").getString()).thenReturn(nodeName);
        when(fileNode.getPath()).thenReturn(nodePath);
        when(fileNode.getProperty("ecd:id").getString()).thenReturn(nodeId);

        Calendar newDate = Calendar.getInstance();
        when(oneDriveAPI.updateFile(any(DriveItem.class))).thenReturn(testItem);

        final CloudFile cloudFile = oneDriveFileAPI.updateFile(fileNode, calendar);

        DriveItem preparedItem = new DriveItem();

        preparedItem.id= nodeId;
        preparedItem.name = nodeName;
        preparedItem.parentReference = new ItemReference();

        preparedItem.parentReference.id =parentId;
        preparedItem.fileSystemInfo = new FileSystemInfo();
        preparedItem.fileSystemInfo.lastModifiedDateTime = calendar;

        verify(oneDriveAPI).updateFile(argThat(new DriveItemArgumentMatcher(preparedItem)));

        assertEquals(nodeId,cloudFile.getId());
        assertEquals(nodePath,cloudFile.getPath());
        assertEquals(nodeName,cloudFile.getTitle());
        assertEquals(testItem.createdDateTime,cloudFile.getCreatedDate());
        assertEquals(testItem.lastModifiedDateTime,cloudFile.getModifiedDate());




    }
    @Test
    public void isTrashSupported() {
        assertFalse(oneDriveFileAPI.isTrashSupported());
    }

    @Test(expected = SyncNotSupportedException.class)
    public void restore() throws SyncNotSupportedException {
      oneDriveFileAPI.restore("","");
    }
    @Test(expected = SyncNotSupportedException.class)
    public void trashFile() throws CloudDriveException {
        oneDriveFileAPI.trashFile("");
    }
    @Test(expected = SyncNotSupportedException.class)
    public void trashFolder() throws CloudDriveException {
        oneDriveFileAPI.trashFolder("");
    }
    @Test(expected = SyncNotSupportedException.class)
    public void untrashFile() throws CloudDriveException {
        oneDriveFileAPI.untrashFile(mock(Node.class));
    }

    @Test(expected = SyncNotSupportedException.class)
    public void untrashFolder() throws CloudDriveException {
        oneDriveFileAPI.untrashFolder(any(Node.class));
    }


    private class DriveItemArgumentMatcher implements ArgumentMatcher<DriveItem> {
        DriveItem driveItem;
        public  DriveItemArgumentMatcher(DriveItem driveItem){
            this.driveItem =  driveItem;
        }
        @Override
        public boolean matches(DriveItem item) {
          if (driveItem == item) {
            return true;
          }
          return driveItem.id.equals(item.id) &&
              driveItem.name.equals(item.name) &&

              driveItem.parentReference.id.equals(item.parentReference.id) &&
              driveItem.fileSystemInfo.lastModifiedDateTime.equals(item.fileSystemInfo.lastModifiedDateTime);

        }
    }

}
