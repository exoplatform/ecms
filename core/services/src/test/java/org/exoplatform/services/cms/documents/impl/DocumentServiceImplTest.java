package org.exoplatform.services.cms.documents.impl;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.user.UserPortalContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.manager.IdentityManager;
import org.gatein.api.Portal;
import org.gatein.api.navigation.Navigation;
import org.gatein.api.site.SiteId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentServiceImplTest {
  public static final String ROOT_USERNAME = "root";
  public static final String spaceGroupId = "/spaces/spaceOne";
  @Mock
  ManageDriveService manageDriveService;
  @Mock
  ConfigurationManager configurationManager;
  @Mock
  Portal portal;
  @Mock
  SessionProviderService sessionProviderService;
  @Mock
  RepositoryService repoService;
  @Mock
  NodeHierarchyCreator nodeHierarchyCreator;
  @Mock
  LinkManager linkManager;
  @Mock
  PortalContainerInfo portalContainerInfo;
  @Mock
  OrganizationService organizationService;
  @Mock
  SettingService settingService;
  @Mock
  IdentityManager identityManager;
  @Mock
  IDGeneratorService idGenerator;
  @Mock
  FavoriteService favoriteService;
  @Mock
  IdentityRegistry identityRegistry;

  @Mock
  Authenticator authenticator;

  DocumentService documentService;
  @Mock
  ManageableRepository repository;
  @Mock
  RepositoryEntry repositoryEntry;
  @Mock
  SessionProvider sessionProvider;

  @Mock
  Node documentNode;

  @Mock
  UserPortalConfig userPortalConfig;

  MockedStatic<Utils> UTILS = mockStatic(Utils.class);
  MockedStatic<WCMCoreUtils> WCM_CORE_UTILS = mockStatic(WCMCoreUtils.class);

  @Mock
  Session session;
  @Mock
  private UserPortalConfigService userPortalConfigService;
  private DriveData personalDrive;
  private DriveData usersDrive;
  private DriveData spaceGroupDrive;
  private DriveData generalDrive;

  @Before
  public void setUp() throws Exception {
    documentService = new DocumentServiceImpl(manageDriveService,
                                              configurationManager,
                                              portal,
                                              sessionProviderService,
                                              repoService,
                                              nodeHierarchyCreator,
                                              linkManager,
                                              portalContainerInfo,
                                              organizationService,
                                              settingService,
                                              identityManager,
                                              idGenerator,
                                              favoriteService,
                                              identityRegistry,
                                              authenticator);
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(repoService.getCurrentRepository()).thenReturn(repository);
    when(sessionProvider.getSession(any(), any())).thenReturn(session);
    when(documentNode.getPath()).thenReturn("/path/to/documentNode.txt");
    when(session.getNodeByUUID(anyString())).thenReturn(documentNode);
    when(portalContainerInfo.getContainerName()).thenReturn("portal");


    Navigation spaceNavigation = mock(Navigation.class);
    org.gatein.api.navigation.Node rootNode = mock(org.gatein.api.navigation.Node.class);
    Iterator<org.gatein.api.navigation.Node> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true);
    org.gatein.api.navigation.Node navigationNode = mock(org.gatein.api.navigation.Node.class);
    when(navigationNode.getName()).thenReturn("spaceOne");
    when(iterator.next()).thenReturn(navigationNode);
    when(rootNode.iterator()).thenReturn(iterator);
    when(spaceNavigation.getRootNode(any())).thenReturn(rootNode);
    when(portal.getNavigation(new SiteId(org.gatein.api.site.SiteType.SPACE, spaceGroupId))).thenReturn(spaceNavigation);

    List<String> memberships = Arrays.asList("","","");
    UTILS.when(Utils::getMemberships).thenReturn(memberships);

    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getName()).thenReturn("dw");
    when(userPortalConfigService.getMetaPortal()).thenReturn("dw");
    when(userPortalConfig.getPortalConfig()).thenReturn(portalConfig);
    when(userPortalConfigService.getUserPortalConfig(anyString(), anyString(), any(UserPortalContext.class))).thenReturn(userPortalConfig);
    WCM_CORE_UTILS.when(() -> WCMCoreUtils.getService(UserPortalConfigService.class)).thenReturn(userPortalConfigService);
    personalDrive = new DriveData();
    personalDrive.setLabel("Personal drive");
    personalDrive.setName("Personal drive");
    personalDrive.setHomePath("/Users/r___/ro___/roo___/root/Private");
    when(manageDriveService.getDriveByName(ManageDriveServiceImpl.PERSONAL_DRIVE_NAME)).thenReturn(personalDrive);
    usersDrive = new DriveData();
    usersDrive.setName("Users drive");
    usersDrive.setLabel("Users drive");
    usersDrive.setHomePath("/Groups/platform/users/Documents");
    when(manageDriveService.getDriveByName(ManageDriveServiceImpl.USER_DRIVE_NAME)).thenReturn(usersDrive);
    spaceGroupDrive = new DriveData();
    spaceGroupDrive.setName(".spaces.spaceOne");
    spaceGroupDrive.setLabel("SpaceOne drive");
    spaceGroupDrive.getParameters().put(ManageDriveServiceImpl.DRIVE_PARAMATER_GROUP_ID, spaceGroupId);
    spaceGroupDrive.setHomePath("/Groups/spaces/spaceOne/Documents");
    when(manageDriveService.getDriveByName(".spaces.spaceOne")).thenReturn(spaceGroupDrive);
    generalDrive = new DriveData();
    generalDrive.setHomePath("/sites/dw");
    generalDrive.setLabel("General drive");
    generalDrive.setName("General drive");
    when(manageDriveService.getDriveByUserRoles(ROOT_USERNAME, memberships)).thenReturn(Arrays.asList(personalDrive, usersDrive, spaceGroupDrive, generalDrive));

  }

  @After
  public void tearDown() throws Exception {
    UTILS.close();
    WCM_CORE_UTILS.close();
  }

  @Test
  public void testGetLinkInDocumentsAppByIdentifier() throws Exception {
    String identifier = "docIdentifier";
    String link = documentService.getLinkInDocumentsAppByIdentifier(identifier);
    assertEquals("/portal/dw/documents?path=doc-not-found",link);
    Identity identity = new Identity(ROOT_USERNAME);
    ConversationState.setCurrent(new ConversationState(identity));

    when(documentNode.getPath()).thenReturn("/Users/r___/ro___/roo___/root");
      link = documentService.getLinkInDocumentsAppByIdentifier(identifier);
    assertNotNull(link);
    assertEquals("/portal/dw/documents?documentPreviewId=docIdentifier", link);

    when(documentNode.getPath()).thenReturn("/Groups/spaces/spaceOne/Documents");
    link = documentService.getLinkInDocumentsAppByIdentifier(identifier);
    assertNotNull(link);
    assertEquals("/portal/g/:spaces:spaceOne/spaceOne/documents?documentPreviewId=docIdentifier", link);

    when(documentNode.getPath()).thenReturn("/path/to/an/undefined/drive");
    link = documentService.getLinkInDocumentsAppByIdentifier(identifier);
    assertNotNull(link);
    assertEquals("/portal/dw/documents?path=doc-not-found", link);
  }

  @Test
  public void testGetLinkInDocumentsAppByIdentifierAndDrive() throws Exception {
    String identifier = "docIdentifier";
    String link = documentService.getLinkInDocumentsAppByIdentifier(null, null);
    assertNull(link);

    Identity identity = new Identity(ROOT_USERNAME);
    ConversationState.setCurrent(new ConversationState(identity));

    link = documentService.getLinkInDocumentsAppByIdentifier(identifier, null);
    assertNotNull(link);assertEquals("/portal/dw/documents?path=doc-not-found", link);

    link = documentService.getLinkInDocumentsAppByIdentifier(identifier, personalDrive);
    assertNotNull(link);
    assertEquals("/portal/dw/documents?documentPreviewId=docIdentifier", link);

    link = documentService.getLinkInDocumentsAppByIdentifier(identifier, spaceGroupDrive);
    assertNotNull(link);
    assertEquals("/portal/g/:spaces:spaceOne/spaceOne/documents?documentPreviewId=docIdentifier", link);
  }

  @Test
  public void testGetDriveOfNodeByIdentifier() throws Exception {
    String identifier = "docIdentifier";
    DriveData drive = documentService.getDriveOfNodeByIdentifier(identifier);
    assertNull(drive);
    Identity identity = new Identity(ROOT_USERNAME);
    ConversationState.setCurrent(new ConversationState(identity));

    when(documentNode.getPath()).thenReturn("/Users/r___/ro___/roo___/root");
    drive = documentService.getDriveOfNodeByIdentifier(identifier);
    assertNotNull(drive);
    assertEquals("Personal drive", drive.getLabel());

    when(documentNode.getPath()).thenReturn("/Users/r___/ro___/roo___/anotherUser");
    drive = documentService.getDriveOfNodeByIdentifier(identifier);
    assertNotNull(drive);
    assertEquals("Users drive", drive.getLabel());

    when(documentNode.getPath()).thenReturn("/Groups/spaces/spaceOne/Documents");
    drive = documentService.getDriveOfNodeByIdentifier(identifier);
    assertNotNull(drive);
    assertEquals("SpaceOne drive", drive.getLabel());

    when(documentNode.getPath()).thenReturn("/sites/dw/parentFolder/folder/myDocument.txt");
    drive = documentService.getDriveOfNodeByIdentifier(identifier);
    assertNotNull(drive);
    assertEquals("General drive", drive.getLabel());

  }
}
