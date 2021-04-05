package org.exoplatform.services.attachments.service;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentsEntityType;
import org.exoplatform.services.attachments.storage.AttachmentsStorage;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/attachments/attachments-test-configuration.xml")})
@RunWith(MockitoJUnitRunner.class)
public class AttachmentsServiceTest extends BaseExoTestCase {

  protected AttachmentsService attachmentsService;

  AttachmentsStorage attachmentsStorage;

  @Mock
  RepositoryService repositoryService;

  @Mock
  SessionProviderService sessionProviderService;

  @Mock
  ManageableRepository repository;

  @Mock
  RepositoryEntry repositoryEntry;

  @Mock
  SessionProvider sessionProvider;

  @Mock
  Session session;

  @Before
  public void setUp() throws Exception {
    begin();
    attachmentsStorage = CommonsUtils.getService(AttachmentsStorage.class);
    attachmentsService = new AttachmentsServiceImpl(attachmentsStorage, repositoryService, sessionProviderService);
  }

  @After
  public void teardown() throws Exception {
    super.tearDown();
    end();
  }

  @Test
  public void testlinkAttachmentsToEntity() throws Exception { // NOSONAR
    int[] list = {-9,2,5,14,98};
    try {
      attachmentsService.linkAttachmentsToEntity(0,"", null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      List<String> attachmentsIds = new ArrayList<>();
      attachmentsService.linkAttachmentsToEntity(1,"", attachmentsIds);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      List<String> attachmentsIds = new ArrayList<>();
      attachmentsIds.add("1");
      attachmentsIds.add("2");
      attachmentsIds.add("3");
      attachmentsService.linkAttachmentsToEntity(5,"", attachmentsIds);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    //when
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);

    ManageableRepository manageableRepository = repositoryService.getRepository("repository");
    Mockito.when(repositoryService.getRepository(Mockito.anyString())).thenReturn(manageableRepository);
    Node node1 = mock(Node.class);
    Node nodeContent1 = mock(Node.class);
    Property property = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node1);
    Workspace workSpace = mock(Workspace.class);
    when(session.getWorkspace()).thenReturn(workSpace);
    when(node1.getSession()).thenReturn(session);
    when(node1.getProperty(anyString())).thenReturn(property);
    when(node1.getNode(anyString())).thenReturn(nodeContent1);
    when(nodeContent1.getProperty(anyString())).thenReturn(property);
    when(property.getDate()).thenReturn(Calendar.getInstance());
    when(property.getLong()).thenReturn((long) 1);
    Mockito.when(session.getNodeByUUID(String.valueOf(1))).thenReturn(node1);

    Node node2 = mock(Node.class);
    Node nodeContent2 = mock(Node.class);
    Property property2 = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node2);
    when(session.getWorkspace()).thenReturn(workSpace);
    when(node2.getSession()).thenReturn(session);
    when(node2.getProperty(anyString())).thenReturn(property);
    when(node2.getNode(anyString())).thenReturn(nodeContent2);
    when(nodeContent2.getProperty(anyString())).thenReturn(property);
    when(property2.getDate()).thenReturn(Calendar.getInstance());
    when(property2.getLong()).thenReturn((long) 2);
    Mockito.when(session.getNodeByUUID(String.valueOf(2))).thenReturn(node2);

    Node node3 = mock(Node.class);
    Node nodeContent3 = mock(Node.class);
    Property property3 = mock(Property.class);
    when(session.getNodeByUUID(anyString())).thenReturn(node3);
    when(session.getWorkspace()).thenReturn(workSpace);
    when(node3.getSession()).thenReturn(session);
    when(node3.getProperty(anyString())).thenReturn(property);
    when(node3.getNode(anyString())).thenReturn(nodeContent3);
    when(nodeContent3.getProperty(anyString())).thenReturn(property);
    when(property3.getDate()).thenReturn(Calendar.getInstance());
    when(property3.getLong()).thenReturn((long) 3);
    Mockito.when(session.getNodeByUUID(String.valueOf(3))).thenReturn(node3);

    List<String> attachmentsIds = new ArrayList<>();
    attachmentsIds.add("1");
    attachmentsIds.add("2");
    attachmentsIds.add("3");

    //when
    attachmentsService.linkAttachmentsToEntity(5, "EVENT", attachmentsIds);

    //then
    List<Attachment> attachmentsEntityStored = attachmentsService.getAttachmentsByEntity(5, String.valueOf(AttachmentsEntityType.EVENT));
    assertNotNull(attachmentsEntityStored);
    assertEquals(3, attachmentsEntityStored.size());
  }

}
