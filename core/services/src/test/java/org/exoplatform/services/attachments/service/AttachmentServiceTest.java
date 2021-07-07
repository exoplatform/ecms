package org.exoplatform.services.attachments.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.jcr.*;

import org.exoplatform.services.attachments.dao.AttachmentDAO;
import org.exoplatform.services.attachments.storage.AttachmentStorageImpl;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.component.test.*;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/attachments/attachments-test-configuration.xml") })
@RunWith(MockitoJUnitRunner.class)
public class AttachmentServiceTest extends BaseExoTestCase {

  protected AttachmentService attachmentService;

  AttachmentDAO               attachmentDAO;

  AttachmentStorage           attachmentStorage;

  @Mock
  RepositoryService           repositoryService;

  @Mock
  SessionProviderService      sessionProviderService;

  @Mock
  ManageableRepository        repository;

  @Mock
  RepositoryEntry             repositoryEntry;

  @Mock
  SessionProvider             sessionProvider;

  @Mock
  DocumentService             documentService;

  @Mock
  IdentityManager             identityManager;

  @Mock
  ManageDriveService          manageDriveService;

  @Mock
  NodeHierarchyCreator        nodeHierarchyCreator;

  @Mock
  NodeFinder                  nodeFinder;

  @Mock
  LinkManager                 linkManager;

  @Mock
  Session                     session;

  @Before
  public void setUp() throws Exception {
    begin();
    attachmentDAO = CommonsUtils.getService(AttachmentDAO.class);
    attachmentStorage = new AttachmentStorageImpl(attachmentDAO, repositoryService, sessionProviderService, documentService,linkManager);
    attachmentService = new AttachmentServiceImpl(attachmentStorage,
                                                  repositoryService,
                                                  sessionProviderService,
                                                  documentService,
                                                  identityManager,
                                                  manageDriveService,
                                                  nodeHierarchyCreator,
                                                  nodeFinder,
                                                  linkManager);
  }

  @After
  public void teardown() throws Exception {
    super.tearDown();
    end();
  }

  @Test
  public void testlinkAttachmentToEntity() throws Exception { // NOSONAR
    int[] list = { -9, 2, 5, 14, 98 };
    try {
      attachmentService.linkAttachmentToEntity(1, 0, "", null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.linkAttachmentToEntity(1, 1, "", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      List<String> attachmentsIds = new ArrayList<>();
      attachmentsIds.add("1");
      attachmentsIds.add("2");
      attachmentsIds.add("3");
      attachmentService.linkAttachmentToEntity(1, 5, "", "1");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.linkAttachmentToEntity(0, 5, "task", "1");
      fail();
    } catch (IllegalAccessException e) {
      // Expected
    }

    // when
    lenient().when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);

    ManageableRepository manageableRepository = repositoryService.getRepository("repository");
    lenient().when(repositoryService.getRepository(Mockito.anyString())).thenReturn(manageableRepository);
    Node node1 = mock(Node.class);
    Node nodeContent1 = mock(Node.class);
    Property property = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node1);
    Workspace workSpace = mock(Workspace.class);
    when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node1.getSession()).thenReturn(session);
    lenient().when(node1.getProperty(anyString())).thenReturn(property);
    lenient().when(node1.getNode(anyString())).thenReturn(nodeContent1);
    lenient().when(nodeContent1.getProperty(anyString())).thenReturn(property);
    lenient().when(property.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property.getLong()).thenReturn((long) 1);
    lenient().when(session.getNodeByUUID(String.valueOf(1))).thenReturn(node1);

    Node node2 = mock(Node.class);
    Node nodeContent2 = mock(Node.class);
    Property property2 = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node2);
    when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node2.getSession()).thenReturn(session);
    lenient().when(node2.getProperty(anyString())).thenReturn(property);
    lenient().when(node2.getNode(anyString())).thenReturn(nodeContent2);
    lenient().when(nodeContent2.getProperty(anyString())).thenReturn(property);
    lenient().when(property2.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property2.getLong()).thenReturn((long) 2);
    lenient().when(session.getNodeByUUID(String.valueOf(2))).thenReturn(node2);

    Node node3 = mock(Node.class);
    Node nodeContent3 = mock(Node.class);
    Property property3 = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node3);
    lenient().when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node3.getSession()).thenReturn(session);
    lenient().when(node3.getProperty(anyString())).thenReturn(property);
    when(node3.getNode(anyString())).thenReturn(nodeContent3);
    when(nodeContent3.getProperty(anyString())).thenReturn(property);
    lenient().when(property3.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property3.getLong()).thenReturn((long) 3);
    Mockito.when(session.getNodeByUUID(String.valueOf(3))).thenReturn(node3);

    String username = "testuser";
    long currentIdentityId = 2;
    Identity currentIdentity = new Identity(OrganizationIdentityProvider.NAME, username);
    currentIdentity.setId(String.valueOf(currentIdentityId));
    Mockito.when(identityManager.getIdentity("2")).thenReturn(currentIdentity);

    // when
    attachmentService.linkAttachmentToEntity(currentIdentityId, 5, "EVENT", "1");
    attachmentService.linkAttachmentToEntity(currentIdentityId, 5, "EVENT", "2");
    attachmentService.linkAttachmentToEntity(currentIdentityId, 5, "EVENT", "3");

    // then
    List<Attachment> attachmentsEntityStored = attachmentService.getAttachmentsByEntity(currentIdentityId, 5, "EVENT");
    assertNotNull(attachmentsEntityStored);
    assertEquals(3, attachmentsEntityStored.size());
  }

}
