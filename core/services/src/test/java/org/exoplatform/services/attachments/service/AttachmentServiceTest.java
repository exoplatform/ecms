package org.exoplatform.services.attachments.service;

import static org.exoplatform.services.wcm.core.NodetypeConstant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.jcr.*;

import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.attachments.dao.AttachmentDAO;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.storage.AttachmentStorageImpl;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplateConfig;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.MembershipEntry;
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

  @Mock(extraInterfaces = {ExtendedSession.class})
  Session session;

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
    lenient().when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
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
    Workspace workSpace = mock(Workspace.class);
    when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(1))).thenReturn(node1);
    when(session.getNodeByUUID(anyString())).thenReturn(node1);
    when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node1.getSession()).thenReturn(session);
    nodeContent1 = mock(NodeImpl.class);
    node1 = mock(NodeImpl.class);
    lenient().when(((NodeImpl) node1).getIdentifier()).thenReturn("1");
    lenient().when(node1.getProperty(anyString())).thenReturn(property);
    lenient().when(node1.getNode(anyString())).thenReturn(nodeContent1);
    lenient().when(nodeContent1.getProperty(anyString())).thenReturn(property);
    lenient().when(property.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property.getLong()).thenReturn((long) 1);
    lenient().when(node1.getPath()).thenReturn("/collaboration/");
    lenient().when(session.getNodeByUUID(String.valueOf(1))).thenReturn(node1);
    lenient().when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(1))).thenReturn(node1);

    Node node2 = mock(Node.class);
    lenient().when(node2.getSession()).thenReturn(session);

    node2 = mock(NodeImpl.class);
    Node nodeContent2 = mock(NodeImpl.class);
    lenient().when(((NodeImpl) node2).getIdentifier()).thenReturn("2");
    Property property2 = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node2);
    when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(2))).thenReturn(node2);
    when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node2.getProperty(anyString())).thenReturn(property2);
    lenient().when(node2.getNode(anyString())).thenReturn(nodeContent2);
    lenient().when(nodeContent2.getProperty(anyString())).thenReturn(property2);
    lenient().when(property2.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property2.getLong()).thenReturn((long) 2);
    lenient().when(node2.getPath()).thenReturn("/collaboration/");
    lenient().when(session.getNodeByUUID(String.valueOf(2))).thenReturn(node2);
    lenient().when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(2))).thenReturn(node2);

    Node node3 = mock(Node.class);
    lenient().when(node3.getSession()).thenReturn(session);
    node3 = mock(NodeImpl.class);
    Node nodeContent3 = mock(NodeImpl.class);
    lenient().when(((NodeImpl) node3).getIdentifier()).thenReturn("3");
    Property property3 = mock(Property.class);
    when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(3))).thenReturn(node3);
    lenient().when(session.getWorkspace()).thenReturn(workSpace);
    lenient().when(node3.getProperty(anyString())).thenReturn(property3);
    lenient().when(node3.getNode(anyString())).thenReturn(nodeContent3);
    lenient().when(nodeContent3.getProperty(anyString())).thenReturn(property3);
    lenient().when(property3.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property3.getLong()).thenReturn((long) 3);
    lenient().when(node3.getPath()).thenReturn("/collaboration/");
    when(((ExtendedSession) session).getNodeByIdentifier(String.valueOf(3))).thenReturn(node3);

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

  @Test
  public void testCreateNewDoc() throws Exception { // NOSONAR
    try {
      attachmentService.createNewDocument(null, "title.docx", "", "", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    org.exoplatform.services.security.Identity userIdentity = new org.exoplatform.services.security.Identity("john", Collections.singletonList(new MembershipEntry("/platform/users", "manager")));
    try {
      attachmentService.createNewDocument(userIdentity, "", "", "", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.createNewDocument(userIdentity, "title.docx", "", "", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.createNewDocument(userIdentity, "title.docx", "/docs", "", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.createNewDocument(userIdentity, "title.docx", "/docs", "Collaboration", "");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      attachmentService.createNewDocument(userIdentity, "title<:?.docx", "path", "drive", "template");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    String docTitle = "test.docx";
    String docPath = "Documents";
    String pathDrive = "Personal Documents";
    String templateName = "MicrosoftOfficeDocument";
    String createdDocUUID = "1";

    NewDocumentTemplateConfig documentTemplateConfig = new NewDocumentTemplateConfig();
    documentTemplateConfig.setExtension(".docx");
    documentTemplateConfig.setPath("classpath:files/template.docx");
    documentTemplateConfig.setName("MicrosoftOfficeDocument");
    documentTemplateConfig.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    NewDocumentTemplate documentTemplate = new NewDocumentTemplate(documentTemplateConfig);
    NewDocumentTemplateProvider documentTemplateProvider = mock(NewDocumentTemplateProvider.class);
    DocumentEditorProvider documentEditorProvider = mock(DocumentEditorProvider.class);

    lenient().when(documentTemplateProvider.getTemplates()).thenReturn(Collections.<NewDocumentTemplate>singletonList(documentTemplate));
    lenient().when(documentTemplateProvider.getEditor()).thenReturn(documentEditorProvider);
    lenient().when(documentEditorProvider.isAvailableForUser(userIdentity)).thenReturn(true);

    // when
    lenient().when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    lenient().when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);

    ManageableRepository manageableRepository = repositoryService.getRepository("repository");
    lenient().when(repositoryService.getRepository(Mockito.anyString())).thenReturn(manageableRepository);
    Node parentNode = mock(Node.class);
    Node node1 = mock(Node.class);
    Node nodeContent1 = mock(Node.class);
    Property property = mock(Property.class);
    lenient().when(node1.getSession()).thenReturn(session);
    node1 = mock(NodeImpl.class);
    lenient().when(((NodeImpl) node1).getIdentifier()).thenReturn(createdDocUUID);
    lenient().when(((ExtendedSession) session).getNodeByIdentifier(createdDocUUID)).thenReturn(node1);
    lenient().when(node1.getProperty(anyString())).thenReturn(property);
    lenient().when(node1.getNode(anyString())).thenReturn(nodeContent1);
    lenient().when(nodeContent1.getProperty(anyString())).thenReturn(property);
    lenient().when(node1.hasProperty("exo:title")).thenReturn(true);
    lenient().when(node1.getProperty("exo:title").getString()).thenReturn(docTitle);
    lenient().when(property.getDate()).thenReturn(Calendar.getInstance());
    lenient().when(property.getLong()).thenReturn((long) 1);
    lenient().when(node1.getPath()).thenReturn("/collaboration/");
    lenient().when(node1.getName()).thenReturn(docTitle);
    lenient().when(node1.getUUID()).thenReturn(createdDocUUID);
    lenient().when(((ExtendedSession) session).getNodeByIdentifier(createdDocUUID)).thenReturn(node1);
    lenient().when(Utils.getParentFolderNode(session, manageDriveService,nodeHierarchyCreator, nodeFinder, pathDrive, docPath)).thenReturn(parentNode);
    mockStatic(PermissionUtil.class);
    //can add node permission
    lenient().when(PermissionUtil.canAddNode(parentNode)).thenReturn(true);
    lenient().when(documentService.createDocumentFromTemplate(parentNode, docTitle, documentTemplate)).thenReturn(node1);
    lenient().when(documentService.getNewDocumentTemplateProviders()).thenReturn(Collections.singletonList(documentTemplateProvider));
    // when
    attachmentService.createNewDocument(userIdentity, docTitle, docPath, pathDrive, templateName);

    // then
    //assert created document with add_node permission
    Attachment newCreatedDocument = attachmentService.getAttachmentById(createdDocUUID);
    assertNotNull(newCreatedDocument);
    assertEquals("1", newCreatedDocument.getId());
    assertEquals(docTitle, newCreatedDocument.getTitle());

    //assert IllegalAccessException when try to add a new document without add_node permission
    Node node = mock(Node.class);
    lenient().when(Utils.getParentFolderNode(session, manageDriveService,nodeHierarchyCreator, nodeFinder, ".spaces.testspace", docPath)).thenReturn(node);
    lenient().when(PermissionUtil.canAddNode(parentNode)).thenReturn(false);
    try {
      attachmentService.createNewDocument(userIdentity, docTitle, docPath, ".spaces.testspace", templateName);
      fail();
    }catch (IllegalAccessException e ){
      //expected
      assertEquals("Permission to create a new document is missing",e.getMessage());
    }
  }

  @Test
  public void markAttachmentAsViewed() throws RepositoryException {
    Session session = mock(Session.class);
    MockedStatic<Utils> UTILS = mockStatic(Utils.class);
    Node node = mock(Node.class);
    UTILS.when(() -> Utils.getSession(sessionProviderService, repositoryService)).thenReturn(session);
    UTILS.when(() -> Utils.getNodeByIdentifier(session, "123")).thenReturn(node);
    UTILS.when(() -> Utils.markDocumentAsViewed(session, "123", "user")).thenCallRealMethod();
    lenient().when(node.canAddMixin(DOCUMENTS_VIEW_MIXIN)).thenReturn(true);
    lenient().when(node.hasProperty(DOCUMENT_VIEWS_PROPERTY)).thenReturn(true);
    lenient().when(node.hasProperty(DOCUMENT_VIEWERS_PROPERTY)).thenReturn(true);
    Property property = mock(Property.class);
    Value value = mock(Value.class);
    lenient().when(node.getProperty(DOCUMENT_VIEWS_PROPERTY)).thenReturn(property);
    lenient().when(node.getProperty(DOCUMENT_VIEWERS_PROPERTY)).thenReturn(property);
    lenient().when(property.getValue()).thenReturn(value);
    lenient().when(value.getLong()).thenReturn(1L);
    lenient().when(property.getString()).thenReturn("user");
    attachmentService.markAttachmentAsViewed("123", "user");
    verify(session, times(1)).save();
  }
}
