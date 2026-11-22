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

import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
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

  private static MockedStatic<Utils> UTILS;
  private static MockedStatic<SessionProvider> SESSION_PROVIDER;
  private static MockedStatic<WCMCoreUtils> WCM_CORE_UTILS;
  private static MockedStatic<ConversationState> CONVERSATION_STATE;

  @AfterClass
  public static void teardownAll() throws Exception {
    UTILS.close();
    SESSION_PROVIDER.close();
    WCM_CORE_UTILS.close();
    CONVERSATION_STATE.close();
  }

  @BeforeClass
  public static void setupAll() throws Exception {
    WCM_CORE_UTILS = mockStatic(WCMCoreUtils.class);
    CONVERSATION_STATE = mockStatic(ConversationState.class);
    UTILS = mockStatic(Utils.class);
    SESSION_PROVIDER = mockStatic(SessionProvider.class);
  }

  @Before
  public void setUp() throws Exception {
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
    ConversationState conversationState = mock(ConversationState.class);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
    Identity identity = mock(Identity.class);
    when(conversationState.getIdentity()).thenReturn(identity);
    when(identity.getUserId()).thenReturn("user");
    DriveData driveData = mock(DriveData.class);
    when(manageDriveService.getDriveByName(anyString())).thenReturn(driveData);
    when(driveData.getHomePath()).thenReturn("path");
    UTILS.when(() -> Utils.getPersonalDrivePath("path", "user")).thenReturn("personalDrivePath");
    UTILS.when(() -> Utils.cleanString(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanName(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanName(anyString(), anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.cleanNameWithAccents(anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.replaceSpecialChars(anyString(), anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.replaceSpecialChars(anyString(), anyString(), anyString())).thenCallRealMethod();
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
    lenient().when(targetNode.getPath()).thenReturn("path/Documents");
    lenient().when(node.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(false);
    Node folderNode1 = mock(Node.class);
    when(node.addNode(anyString(), eq(NodetypeConstant.NT_FOLDER))).thenReturn(folderNode1);
    lenient().when(fileUploadHandler.checkExistence(targetNode, "test.docx")).thenReturn(Response.ok().build());

    response = this.manageDocumentService.checkFileExistence("collaboration", "testspace", "/documents", "test.docx");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());


    response = this.manageDocumentService.checkFileExistence("collaboration", ".spaces.space_one", "DRIVE_ROOT_NODE/Documents", "test.docx");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void createFolderRunsAsTheCallerWhateverTheSystemFlag() throws Exception {
    Session systemSession = mock(Session.class);
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    lenient().when(systemSessionProvider.getSession(anyString(), any())).thenReturn(systemSession);
    WCM_CORE_UTILS.when(WCMCoreUtils::getSystemSessionProvider).thenReturn(systemSessionProvider);
    SESSION_PROVIDER.when(SessionProvider::createSystemProvider).thenReturn(systemSessionProvider);
    Node driveNode = mock(Node.class);
    when(session.getItem("personalDrivePath")).thenReturn(driveNode);
    when(driveNode.addNode("folder", NodetypeConstant.NT_FOLDER)).thenThrow(new AccessDeniedException("user cannot add"));

    Response response = manageDocumentService.createFolder(".spaces.space_one", "collaboration", "", "folder", NodetypeConstant.NT_FOLDER, true);

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    verifyNoInteractions(systemSession);
  }

  @Test
  public void aSpaceRootPathCannotClimbOutOfTheSpace() throws Exception {
    Node spaceRoot = mock(Node.class);
    lenient().when(session.getItem("path")).thenReturn(spaceRoot);
    lenient().when(session.getItem("personalDrivePath")).thenReturn(mock(Node.class));

    for (String path : new String[] { "DRIVE_ROOT_NODE/../..", "DRIVE_ROOT_NODE/..[1]/other", "DRIVE_ROOT_NODE/:../other",
        "DRIVE_ROOT_NODE/:..[2]/other", "DRIVE_ROOT_NODE/..[+1]/other", "DRIVE_ROOT_NODE/:..[\u0661]/other",
        "DRIVE_ROOT_NODE/a/./b" }) {
      Response response = manageDocumentService.deleteFolderOrFile(".spaces.space_one", "collaboration", path);

      assertEquals(path, Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
    verify(spaceRoot, never()).getNode(anyString());
    verify(spaceRoot, never()).addNode(anyString(), anyString());
  }

  /**
   * The walk checks each node it reaches, so a node outside the space, whatever
   * leads to it, ends the request before anything is created. No path segment
   * reaches such a node today, since the segments that lead to the parent are
   * refused first: the stubbed outside child keeps the check in place should
   * the repository resolve another one.
   */
  @Test
  public void aSpaceRootPathCreatesNothingOutsideTheSpace() throws Exception {
    Node spaceRoot = mock(Node.class);
    lenient().when(session.getItem("path")).thenReturn(spaceRoot);
    lenient().when(session.getItem("personalDrivePath")).thenReturn(mock(Node.class));
    Node outside = mock(Node.class);
    when(outside.getPath()).thenReturn("/elsewhere");
    when(spaceRoot.hasNode("x")).thenReturn(true);
    when(spaceRoot.getNode("x")).thenReturn(outside);

    Response response = manageDocumentService.createFolder(".spaces.space_one",
                                                           "collaboration",
                                                           "DRIVE_ROOT_NODE/x/new",
                                                           "folder",
                                                           NodetypeConstant.NT_FOLDER,
                                                           false);

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(outside, never()).addNode(anyString(), anyString());
    verify(outside, never()).hasNode(anyString());
  }

  @Test
  public void aSpaceRootPathCannotLeadOutOfTheSpaceThroughAShortcut() throws Exception {
    Node spaceRoot = mock(Node.class);
    lenient().when(session.getItem("path")).thenReturn(spaceRoot);
    lenient().when(session.getItem("personalDrivePath")).thenReturn(mock(Node.class));
    Node shortcut = mock(Node.class);
    when(spaceRoot.hasNode("link")).thenReturn(true);
    when(spaceRoot.getNode("link")).thenReturn(shortcut);
    when(shortcut.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(true);
    when(shortcut.getPath()).thenReturn("path/link");
    Node target = mock(Node.class);
    when(target.getPath()).thenReturn("/elsewhere/folder");
    when(linkManager.getTarget(shortcut)).thenReturn(target);

    Response response = manageDocumentService.deleteFolderOrFile(".spaces.space_one", "collaboration", "DRIVE_ROOT_NODE/link");

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(target, never()).remove();
  }

  @Test
  public void aSpaceRootPathLeadsToItsFolder() throws Exception {
    Node spaceRoot = mock(Node.class);
    lenient().when(session.getItem("path")).thenReturn(spaceRoot);
    lenient().when(session.getItem("personalDrivePath")).thenReturn(mock(Node.class));
    Node images = mock(Node.class);
    when(spaceRoot.hasNode("images")).thenReturn(true);
    when(spaceRoot.getNode("images")).thenReturn(images);
    when(images.getPath()).thenReturn("path/images");
    Node parent = mock(Node.class);
    when(images.getParent()).thenReturn(parent);

    Response response = manageDocumentService.deleteFolderOrFile(".spaces.space_one", "collaboration", "DRIVE_ROOT_NODE/images");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(images).remove();
  }

  /**
   * Each segment the repository resolves to the space root itself or above it
   * is refused before the space root is walked; a segment it resolves to a
   * child is walked.
   */
  @Test
  public void aSegmentIsRefusedWhenTheRepositoryResolvesItToTheRootOrAbove() throws Exception {
    LocationFactory locationFactory = new LocationFactory(new NamespaceAccessor() {
      @Override
      public String getNamespaceURIByPrefix(String prefix) throws NamespaceException {
        if ("".equals(prefix)) {
          return "";
        } else if ("jcr".equals(prefix)) {
          return "http://www.jcp.org/jcr/1.0";
        }
        throw new NamespaceException(prefix);
      }

      @Override
      public String getNamespacePrefixByURI(String uri) throws NamespaceException {
        if ("".equals(uri)) {
          return "";
        } else if ("http://www.jcp.org/jcr/1.0".equals(uri)) {
          return "jcr";
        }
        throw new NamespaceException(uri);
      }

      @Override
      public String[] getAllNamespacePrefixes() {
        return new String[] { "", "jcr" };
      }
    });
    JCRPath spaceRootPath = locationFactory.parseAbsPath("/spaces/space_one");
    String[] segments = { "..", ".", "..[1]", "..[+1]", "..[+0001]", ":..", ":..[2]", "..[\u0661]", "..[\uff11]", "x", "x[1]",
        "jcr:x", "{}..", "_x002e__x002e_" };
    int refused = 0;
    for (String segment : segments) {
      JCRPath resolved;
      try {
        resolved = locationFactory.createJCRPath(spaceRootPath, segment);
      } catch (RepositoryException e) {
        continue;
      }
      Node spaceRoot = mock(Node.class);
      when(session.getItem("path")).thenReturn(spaceRoot);
      lenient().when(session.getItem("personalDrivePath")).thenReturn(mock(Node.class));

      Response response = manageDocumentService.deleteFolderOrFile(".spaces.space_one", "collaboration", "DRIVE_ROOT_NODE/" + segment);

      if (resolved.getDepth() <= spaceRootPath.getDepth()) {
        refused++;
        assertEquals(segment, Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verifyNoInteractions(spaceRoot);
      } else {
        verify(spaceRoot, atLeastOnce()).hasNode(anyString());
      }
    }
    assertEquals(9, refused);
  }

  @Test
  public void buildXMLResponseForChildrenSkipsShortcutWithMissingTarget() throws Exception {
    assertDeadShortcutIsSkipped(new ItemNotFoundException("target is gone"));
  }

  @Test
  public void buildXMLResponseForChildrenSkipsShortcutWithUnreadableTarget() throws Exception {
    assertDeadShortcutIsSkipped(new AccessDeniedException("target is not readable"));
  }

  private void assertDeadShortcutIsSkipped(Exception targetFailure) throws Exception {
    Workspace workspace = mock(Workspace.class);
    lenient().when(workspace.getName()).thenReturn("collaboration");
    lenient().when(session.getWorkspace()).thenReturn(workspace);
    Node root = mockFolder("/root", "root");
    Node deadShortcut = mockFolder("/root/dead", "dead");
    when(deadShortcut.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(true);
    when(deadShortcut.hasProperty("exo:uuid")).thenReturn(true);
    when(deadShortcut.hasProperty("exo:workspace")).thenReturn(true);
    when(linkManager.getTarget(deadShortcut)).thenThrow(targetFailure);
    Node folder = mockFolder("/root/folder", "folder");
    // the root's createFolderElement reads the children too: each call gets its own iterator
    when(root.getNodes()).thenAnswer(invocation -> iteratorOf(deadShortcut, folder));
    lenient().when(root.hasNode("folder")).thenReturn(true);
    lenient().when(root.getNode("folder")).thenReturn(folder);
    when(session.getItem(anyString())).thenReturn(root);
    UTILS.when(() -> Utils.getTitle(any(Node.class))).thenReturn("title");

    Response response = invokeBuildXMLResponseForChildren(root);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    org.w3c.dom.Document xml = (org.w3c.dom.Document) ((DOMSource) response.getEntity()).getNode();
    org.w3c.dom.NodeList folders = xml.getElementsByTagName("Folders").item(0).getChildNodes();
    assertEquals(1, folders.getLength());
    assertEquals("folder", ((org.w3c.dom.Element) folders.item(0)).getAttribute("name"));
  }

  private NodeIterator iteratorOf(Node... nodes) {
    java.util.Iterator<Node> it = Arrays.asList(nodes).iterator();
    NodeIterator nodeIterator = mock(NodeIterator.class);
    lenient().when(nodeIterator.hasNext()).thenAnswer(invocation -> it.hasNext());
    lenient().when(nodeIterator.nextNode()).thenAnswer(invocation -> it.next());
    return nodeIterator;
  }

  private Node mockFolder(String path, String name) throws Exception {
    Node node = mock(Node.class);
    NodeType nodeType = mock(NodeType.class);
    lenient().when(nodeType.getName()).thenReturn(NodetypeConstant.NT_UNSTRUCTURED);
    lenient().when(node.getPrimaryNodeType()).thenReturn(nodeType);
    lenient().when(node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)).thenReturn(true);
    lenient().when(node.getPath()).thenReturn(path);
    lenient().when(node.getName()).thenReturn(name);
    lenient().when(node.getSession()).thenReturn(session);
    NodeIterator noChildren = mock(NodeIterator.class);
    lenient().when(noChildren.hasNext()).thenReturn(false);
    lenient().when(node.getNodes()).thenReturn(noChildren);
    return node;
  }

  private Response invokeBuildXMLResponseForChildren(Node node) throws Exception {
    Method method = ManageDocumentService.class.getDeclaredMethod("buildXMLResponseForChildren",
                                                                  Node.class,
                                                                  String.class,
                                                                  String.class,
                                                                  boolean.class);
    method.setAccessible(true);
    try {
      return (Response) method.invoke(manageDocumentService, node, "drive", "", false);
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }
}
