package org.exoplatform.wcm.ext.component.activity;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ActivityFile;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;

import java.io.File;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeLocation.class)
public class ECMSActivityFileStoragePluginTest {

  static private String TEXT_PLAIN = "text/plain";

  @Mock
  UploadService uploadService;

  @Mock
  SessionProviderService sessionProviderService;

  @Mock
  SpaceService spaceService;

  @Mock
  RepositoryService repositoryService;

  @Mock
  NodeHierarchyCreator nodeHierarchyCreator;

  @Mock
  SessionProvider sessionProvider;

  @Mock
  ManageableRepository repository;

  @Mock
  RepositoryEntry repositoryEntry;

  @Mock
  Session session;


  @Test
  public void storeAttachments() throws Exception {
    // Given
    ConversationState.setCurrent(new ConversationState(new org.exoplatform.services.security.Identity("root")));
    InitParams initParams = new InitParams();
    ValueParam datasource = new ValueParam();
    datasource.setName("datasource");
    datasource.setValue("jcr");
    ValueParam priority = new ValueParam();
    priority.setName("priority");
    priority.setValue("1");
    initParams.addParameter(datasource);
    initParams.addParameter(priority);
    ECMSActivityFileStoragePlugin ecmsActivityFileStoragePlugin = new ECMSActivityFileStoragePlugin(spaceService,nodeHierarchyCreator,repositoryService,uploadService,sessionProviderService,initParams);
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("test");
    activity.setStreamOwner("root");
    Identity streamOwner = new Identity(OrganizationIdentityProvider.NAME,"root");
    streamOwner.setId("root");
    ActivityFile activityFile = new ActivityFile();
    activityFile.setUploadId("1234");
    activityFile.setName("testFileUpload");
    UploadResource uploadResource = new UploadResource("1234");
    uploadResource.setFileName("testFileUpload");
    File file = File.createTempFile("testFileUpload", ".xml");
    uploadResource.setStoreLocation(file.getPath());
    uploadResource.setMimeType(TEXT_PLAIN);
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getName()).thenReturn("workspace");
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);
    Node userNode = mock(Node.class);
    Node parentNode = mock(Node.class);
    Node parentUploadNode = mock(Node.class);
    Node node = mock(Node.class);
    Node resourceNode = mock(Node.class);
    NodeDefinition nodeDefinition = mock(NodeDefinition.class);
    Property property = mock(Property.class);
    when(nodeHierarchyCreator.getUserNode(any(), any())).thenReturn(userNode);
    when(nodeHierarchyCreator.getJcrPath(any())).thenReturn("/Users");
    when(userNode.hasNode(any())).thenReturn(true);
    when(userNode.getNode(any())).thenReturn(parentNode);
    when(parentNode.hasNode(any())).thenReturn(true);
    when(parentNode.getNode(any())).thenReturn(parentUploadNode);
    when(parentUploadNode.getDefinition()).thenReturn(nodeDefinition);
    when(parentUploadNode.addNode(any(),any())).thenReturn(node);
    when(node.addNode(any(),any())).thenReturn(resourceNode);
    when(uploadService.getUploadResource(any())).thenReturn(uploadResource);
    when(session.getItem(any())).thenReturn(node);
    when(node.getPath()).thenReturn("/test/node_B/node_1");
    when(resourceNode.getProperty(any())).thenReturn(property);
    when(property.getString()).thenReturn("testProperty");
    when(node.isNodeType(any())).thenReturn(false);

    // when
    ecmsActivityFileStoragePlugin.storeAttachments(activity,streamOwner,activityFile);

    // then
    verify(uploadService, times(1)).removeUploadResource(any());
    verify(uploadService, times(1)).getUploadResource(any());
    verify(session, times(1)).save();
  }
}