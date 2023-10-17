package org.exoplatform.ecm.connector.platform;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.FileUploadHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManageDocumentServiceTest {

  @Mock
  private FileUploadHandler     fileUploadHandler;

  @Mock
  private ManageDriveService    manageDriveService;

  @Mock
  private LinkManager           linkManager;

  @Mock
  private CloudDriveService     cloudDriveService;

  @Mock
  private InitParams            initParams;

  @Mock
  private Session               session;

  private ManageDocumentService manageDocumentService;

  MockedStatic<Utils> UTILS;
  MockedStatic<SessionProvider> SESSION_PROVIDER;

  @Before
  public void setUp() throws Exception {
    MockedStatic<WCMCoreUtils> WCM_CORE_UTILS = mockStatic(WCMCoreUtils.class);
    ValueParam valueParam = mock(ValueParam.class);
    when(valueParam.getValue()).thenReturn("20");
    when(initParams.getValueParam("upload.limit.size")).thenReturn(valueParam);
    this.manageDocumentService = new ManageDocumentService(manageDriveService,linkManager,cloudDriveService, initParams);

    SessionProvider userSessionProvider = mock(SessionProvider.class);
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    WCM_CORE_UTILS.when(WCMCoreUtils::getUserSessionProvider).thenReturn(userSessionProvider);
    WCM_CORE_UTILS.when(WCMCoreUtils::getSystemSessionProvider).thenReturn(systemSessionProvider);
    RepositoryService repositoryService = mock(RepositoryService.class);
    WCM_CORE_UTILS.when(() -> WCMCoreUtils.getService(RepositoryService.class)).thenReturn(repositoryService);
    ManageableRepository manageableRepository = mock(ManageableRepository.class);
    when(repositoryService.getCurrentRepository()).thenReturn(manageableRepository);
    lenient().when(systemSessionProvider.getSession("collaboration", manageableRepository)).thenReturn(session);
    when(userSessionProvider.getSession("collaboration", manageableRepository)).thenReturn(session);
    MockedStatic<ConversationState> CONVERSATION_STATE = mockStatic(ConversationState.class);
    ConversationState conversationState = mock(ConversationState.class);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
    Identity identity = mock(Identity.class);
    when(conversationState.getIdentity()).thenReturn(identity);
    when(identity.getUserId()).thenReturn("user");
    DriveData driveData = mock(DriveData.class);
    when(manageDriveService.getDriveByName(anyString())).thenReturn(driveData);
    when(driveData.getHomePath()).thenReturn("path");
    UTILS = mockStatic(Utils.class);
    UTILS.when(() -> Utils.getPersonalDrivePath("path", "user")).thenReturn("personalDrivePath");
    UTILS.when(() -> Utils.cleanString(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanName(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanName(anyString(), anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanNameWithAccents(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.replaceSpecialChars(anyString(), anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.replaceSpecialChars(anyString(), anyString(), anyString())).thenCallRealMethod();
    SESSION_PROVIDER = mockStatic(SessionProvider.class);
    SESSION_PROVIDER.when(SessionProvider::createSystemProvider).thenReturn(systemSessionProvider);
  }

  @Test
  public void checkFileExistence() throws Exception {
    Response response = this.manageDocumentService.checkFileExistence(null, "testspace", "/documents", "test.docx");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    response = this.manageDocumentService.checkFileExistence("collaboration", null, "/documents", "test.docx");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", null, "test.docx");
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", "/documents", null);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    Node node = mock(Node.class);
    when(session.getItem(anyString())).thenReturn(node);
    lenient().when(node.hasNode("Documents")).thenReturn(true);
    Node targetNode = mock(Node.class);
    lenient().when(node.getNode("Documents")).thenReturn(targetNode);
    lenient().when(node.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(false);
    Node folderNode1 = mock(Node.class);
    when(node.addNode(anyString(), eq(NodetypeConstant.NT_FOLDER))).thenReturn(folderNode1);
    lenient().when(fileUploadHandler.checkExistence(targetNode, "test.docx")).thenReturn(Response.ok().build());

    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", "/documents", "test.docx");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


    response = this.manageDocumentService.checkFileExistence("collaboration", ".spaces.space_one", "DRIVE_ROOT_NODE/Documents", "test.docx");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    //
    String fileName = "lowercase.docx";
    response = this.manageDocumentService.checkFileExistence("collaboration", ".testspace", "/", fileName);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DOMSource domSource = (DOMSource) response.getEntity();
    assertEquals("NotExisted", domSource.getNode().getFirstChild().getNodeName());
    //
    Node existingNode = mock(Node.class);
    when(node.hasNodes()).thenReturn(true);
    NodeIterator nodeIterator = mock(NodeIterator.class);
    when(node.getNodes()).thenReturn(nodeIterator);
    when(nodeIterator.hasNext()).thenReturn(true);
    when(nodeIterator.nextNode()).thenReturn(existingNode);
    when(existingNode.getName()).thenReturn(fileName);
    String existingFileName = fileName.toUpperCase();
    //
    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", "/", existingFileName);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    domSource = (DOMSource) response.getEntity();
    assertEquals("Existed", domSource.getNode().getFirstChild().getNodeName());
  }
}
