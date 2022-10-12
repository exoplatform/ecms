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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*" })
@PrepareForTest({ WCMCoreUtils.class, ConversationState.class, Utils.class})
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

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(WCMCoreUtils.class);
    ValueParam valueParam = mock(ValueParam.class);
    when(valueParam.getValue()).thenReturn("20");
    when(initParams.getValueParam("upload.limit.size")).thenReturn(valueParam);
    this.manageDocumentService = new ManageDocumentService(manageDriveService,linkManager,cloudDriveService, initParams);

    SessionProvider userSessionProvider = mock(SessionProvider.class);
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    when(WCMCoreUtils.getUserSessionProvider()).thenReturn(userSessionProvider);
    when(WCMCoreUtils.getSystemSessionProvider()).thenReturn(systemSessionProvider);
    RepositoryService repositoryService = mock(RepositoryService.class);
    when(WCMCoreUtils.getService(RepositoryService.class)).thenReturn(repositoryService);
    ManageableRepository manageableRepository = mock(ManageableRepository.class);
    when(repositoryService.getCurrentRepository()).thenReturn(manageableRepository);
    when(systemSessionProvider.getSession("collaboration", manageableRepository)).thenReturn(session);
    when(userSessionProvider.getSession("collaboration", manageableRepository)).thenReturn(session);
    PowerMockito.mockStatic(ConversationState.class);
    ConversationState conversationState = mock(ConversationState.class);
    when(ConversationState.getCurrent()).thenReturn(conversationState);
    Identity identity = mock(Identity.class);
    when(conversationState.getIdentity()).thenReturn(identity);
    when(identity.getUserId()).thenReturn("user");
    PowerMockito.mockStatic(Utils.class);
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

    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", "/documents", "test.docx");
    DriveData driveData = mock(DriveData.class);
    when(manageDriveService.getDriveByName(anyString())).thenReturn(driveData);
    when(driveData.getHomePath()).thenReturn("path");
    when(Utils.getPersonalDrivePath("path", "user")).thenReturn("personalDrivePath");
    Node node = mock(Node.class);
    when(session.getItem("personalDrivePath")).thenReturn(node);
    when(node.hasNode("Documents")).thenReturn(true);
    Node targetNode = mock(Node.class);
    when(node.getNode("Documents")).thenReturn(targetNode);
    when(node.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(false);
    when(fileUploadHandler.checkExistence(targetNode, "test.docx")).thenReturn(Response.ok().build());
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

  }
}
