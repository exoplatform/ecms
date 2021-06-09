package org.exoplatform.wcm.ext.component.activity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeLocation.class)
public class FileUIActivityTest {

  @Test
  public void testActivityMessageToDisplay() throws Exception {
    String activityTitle = "<a href=\"test.odt\">test.odt</a>";

    FileUIActivityBuilder activityBuilder = new FileUIActivityBuilder();
    FileUIActivity fileUIActivity = new FileUIActivity();
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);

    Map<String, String> activityParameters = new HashMap<>();
    activityParameters.put(FileUIActivity.MESSAGE, "message test");

    activity.setTemplateParams(activityParameters);
    activityBuilder.extendUIActivity(fileUIActivity, activity);

    assertTrue(StringUtils.isNotBlank(fileUIActivity.getMessage()));

    activityParameters.put(FileUIActivity.MESSAGE, activityTitle);
    activityBuilder.extendUIActivity(fileUIActivity, activity);

    assertEquals(activityTitle, fileUIActivity.getMessage());
  }

  @Test
  public void getDocFolderRelativePathWithLinks() throws Exception {
    ConversationState conversationState = new ConversationState(new Identity("root"));
    ConversationState.setCurrent(conversationState);

    Map<String, String> activityParameters = new HashMap<>();
    activityParameters.put("REPOSITORY", "repository");
    activityParameters.put("WORKSPACE", "collaboration");
    activityParameters.put("nodePath", "/sites/intranet/web contents/site artifacts/announcements/test1.txt");
    activityParameters.put("id", "c036fb997f0001016364ca764f61b4d1");

    NodeLocation nodeLocation1 = new NodeLocation("repository", "collaboration", null, "c036fb997f0001016364ca764f61b4d1");

    Node file1 = mock(Node.class);
    Node intranetNode = mock(Node.class);
    Node sitesNode = mock(Node.class);
    Node rootNode = mock(Node.class);

    when(file1.getName()).thenReturn("test1.txt");
    when(file1.getPath()).thenReturn("/sites/intranet/test1.txt");
    when(file1.getParent()).thenReturn(intranetNode);

    when(intranetNode.getName()).thenReturn("intranet");
    when(intranetNode.getPath()).thenReturn("/sites/intranet");
    when(intranetNode.getParent()).thenReturn(sitesNode);

    when(sitesNode.getName()).thenReturn("sites");
    when(sitesNode.getPath()).thenReturn("/sites");
    when(sitesNode.getParent()).thenReturn(rootNode);

    when(rootNode.getName()).thenReturn("/");
    when(rootNode.getPath()).thenReturn("/");

    PowerMockito.mockStatic(NodeLocation.class);
    PowerMockito.when(NodeLocation.getNodeByLocation(Matchers.refEq(nodeLocation1))).thenReturn(file1);

    FileUIActivity fileUIActivity = Mockito.spy(new FileUIActivity());

    DriveData driveData = new DriveData();
    driveData.setHomePath("/sites");
    driveData.setName("Managed Sites");
    driveData.setWorkspace("collboration");

    TrashService trashService = Mockito.mock(TrashService.class);
    DocumentService documentService = Mockito.mock(DocumentService.class);
    Mockito.doReturn(trashService).when(fileUIActivity).getApplicationComponent(Mockito.eq(TrashService.class));
    Mockito.doReturn(documentService).when(fileUIActivity).getApplicationComponent(Mockito.eq(DocumentService.class));

    Mockito.doReturn(driveData).when(fileUIActivity).getDocDrive(0);
    Mockito.when(trashService.isInTrash(file1)).thenReturn(false);
    Mockito.when(documentService.getLinkInDocumentsApp(file1.getPath(), driveData)).thenReturn("FILE1_URI");
    Mockito.when(documentService.getLinkInDocumentsApp(intranetNode.getPath(), driveData)).thenReturn("INTRANET_URI");
    Mockito.when(documentService.getLinkInDocumentsApp(sitesNode.getPath(), driveData)).thenReturn("SITES_URI");

    fileUIActivity.setUIActivityData(activityParameters);

    LinkedHashMap<String, String> docFolderRelativePathWithLinks = fileUIActivity.getDocFolderRelativePathWithLinks(0);

    assertNotNull(docFolderRelativePathWithLinks);
    assertEquals(3, docFolderRelativePathWithLinks.size());

    Set<String> folderNamesWithIndex = docFolderRelativePathWithLinks.keySet();
    int i = 0;
    for (String folderNameWithIndex : folderNamesWithIndex) {
      String folderPath = docFolderRelativePathWithLinks.get(folderNameWithIndex);
      switch (i) {
        case 0:
          assertEquals("Managed Sites_2", folderNameWithIndex);
          assertEquals("SITES_URI", folderPath);
          break;
        case 1:
          assertEquals("intranet_1", folderNameWithIndex);
          assertEquals("INTRANET_URI", folderPath);
          break;
        case 2:
          assertEquals("test1.txt_0", folderNameWithIndex);
          assertEquals("FILE1_URI", folderPath);
          break;
      }
      i++;
    }

    assertEquals("FILE1_URI", fileUIActivity.getDocFilePath(0));
    assertEquals("'Managed Sites': 'SITES_URI','intranet': 'INTRANET_URI'", fileUIActivity.getDocFileBreadCrumb(0));
  }
  
  @Test
  public void testSetUIActivityData() throws Exception {
    ConversationState conversationState = new ConversationState(new Identity("root"));
    ConversationState.setCurrent(conversationState);

    Map<String, String> activityParameters = new HashMap<>();
    activityParameters.put("REPOSITORY", "repository");
    activityParameters.put("WORKSPACE", "collaboration");
    activityParameters.put("DOCPATH", "/sites/intranet/web contents/site artifacts/announcements/test1.txt");

    NodeLocation nodeLocationWithoutUUID = new NodeLocation("repository", "collaboration","/sites/intranet/web contents/site artifacts/announcements/test1.txt",null );
    NodeLocation nodeLocationWithUUID = new NodeLocation("repository", "collaboration","/sites/intranet/web contents/site artifacts/announcements/test1.txt","c036fb997f0001016364ca764f61b4d1" );

    Node file1 = mock(Node.class);
    Node intranetNode = mock(Node.class);
    Node sitesNode = mock(Node.class);
    Node rootNode = mock(Node.class);

    when(file1.getName()).thenReturn("test1.txt");
    when(file1.getPath()).thenReturn("/sites/intranet/web contents/site artifacts/announcements/test1.txt");
    when(file1.getParent()).thenReturn(intranetNode);
    when(file1.getUUID()).thenReturn("c036fb997f0001016364ca764f61b4d1");

    PowerMockito.mockStatic(NodeLocation.class);
    PowerMockito.when(NodeLocation.getNodeByLocation(nodeLocationWithoutUUID)).thenReturn(file1);
    PowerMockito.when(NodeLocation.getNodeByLocation(nodeLocationWithUUID)).thenReturn(file1);

    FileUIActivity fileUIActivity = Mockito.spy(new FileUIActivity());

    DriveData driveData = new DriveData();
    driveData.setWorkspace("collboration");

    TrashService trashService = Mockito.mock(TrashService.class);
    Mockito.doReturn(trashService).when(fileUIActivity).getApplicationComponent(Mockito.eq(TrashService.class));   
    Mockito.doReturn(driveData).when(fileUIActivity).getDocDrive(0);
    Mockito.when(trashService.isInTrash(file1)).thenReturn(false);

    Property titleProperty = Mockito.mock(Property.class);
    Value titleValue = Mockito.mock(Value.class);

    ActivityFileAttachment fileAttachment= new ActivityFileAttachment();
    fileAttachment.setContentName(file1.getName());
    fileAttachment.setNodeLocation(nodeLocationWithUUID);

    Node contentNode = NodeLocation.getNodeByLocation(fileAttachment.getNodeLocation());
    fileUIActivity.setUIActivityData(activityParameters);
    fileUIActivity.setContentNode(contentNode,0);
    fileUIActivity.setContentName(fileAttachment.getContentName(),0);

    assertEquals(1,fileUIActivity.getFilesCount());

    //Test when the file doesn't have an exo:title property
    when(file1.hasProperty("exo:title")).thenReturn(false);

    assertEquals("test1.txt", fileUIActivity.getContentName(0));

    Mockito.when(titleValue.getString()).thenReturn("text");
    Mockito.when(titleProperty.getValue()).thenReturn(titleValue);

    //Test when the file has an exo:title property
    when(file1.hasProperty("exo:title")).thenReturn(true);
    when(file1.getProperty("exo:title")).thenReturn(titleProperty);
    when(file1.getProperty("exo:title").getString()).thenReturn("text");

    assertEquals("text", fileUIActivity.getContentName(0));

  }
}
