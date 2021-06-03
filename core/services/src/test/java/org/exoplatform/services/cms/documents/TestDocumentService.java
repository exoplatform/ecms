package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;

public class TestDocumentService extends BaseWCMTestCase {

  private DocumentService documentService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private SessionProviderService sessionProviderService;
  private RepositoryService repoService;



  public void setUp() throws Exception {
    super.setUp();
    System.setProperty("gatein.email.domain.url", "http://localhost:8080");
    documentService = container.getComponentInstanceOfType(DocumentService.class);
    nodeHierarchyCreator = container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    sessionProviderService = container.getComponentInstanceOfType(SessionProviderService.class);
    repoService = container.getComponentInstanceOfType(RepositoryService.class);
  }

  public void testCreateDocumentFromEmptyTemplate() throws Exception {
    applySystemSession();
    String mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    String title = "Test.docx";
    Node parent = session.getRootNode();
    NewDocumentTemplateConfig config = new NewDocumentTemplateConfig();
    config.setMimeType(mime);
    NewDocumentTemplate template = new NewDocumentTemplate(config);
    Node document = documentService.createDocumentFromTemplate(parent, title, template);
    assertNotNull("Document wasn't created", document);
    assertEquals("Title is not correct", title, document.getProperty("exo:title").getString());
    assertTrue("Content node wasn't created", document.hasNode("jcr:content"));
    Node content = document.getNode("jcr:content");
    assertEquals("Mime-type is not correct", mime, content.getProperty("jcr:mimeType").getString());
    assertEquals("Mime-type is not correct", "", content.getProperty("jcr:data").getString());
    clear();
  }
  
  public void testGetFavoriteDocuments() throws Exception {
    String userId = "root";
    applyUserSession(userId, "gtn", COLLABORATION_WS);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    Node userFavoriteNode = (Node) userNode.getNode(Utils.PRIVATE + "/" + NodetypeConstant.FAVORITE);
    userFavoriteNode.addNode("doc.txt", "nt:file");
    session.save();
    List<Document> documents = documentService.getFavoriteDocuments(userId, 10);
    assertNotNull("documents wasn't created", documents);
    assertEquals(0, documents.size());
  }
  
  public void testGetSharedDocuments() throws Exception {
    String userId = "root";
    applyUserSession(userId, "gtn", COLLABORATION_WS);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    Node userDocumentsNode = (Node) userNode.getNode(Utils.PRIVATE + "/" + NodetypeConstant.DOCUMENTS);
    Node userSharedNode = userDocumentsNode.addNode(NodetypeConstant.SHARED);
    userSharedNode.addNode("doc.txt", "nt:file");
    session.save();
    List<Document> documents = documentService.getSharedDocuments(userId, 10);
    assertNotNull("documents wasn't created", documents);
    assertEquals(0, documents.size());
  }

  public void testGetDocumentUrlInSpaceDocuments() throws Exception {
    Space space = new Space();
    space.setDisplayName("testSpace");
    space.setApp("documents");
    space.setPrettyName(space.getDisplayName());
    String shortName = SpaceUtils.cleanString(space.getPrettyName());
    space.setGroupId("/spaces/" + shortName);
    String spaceId = space.getGroupId();

    Node rootNode = null;
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    ManageableRepository repository = repoService.getCurrentRepository();
    Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);

    nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    session.getRootNode().getNode("Groups").addNode("spaces").addNode(spaceId.split("/")[2]);
    rootNode = (Node) session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + spaceId);
    Node spaceDocumentNode = rootNode.addNode("Documents");
    Node ActivityNode = spaceDocumentNode.addNode("Activity Stream Documents");
    Node sharedNode = spaceDocumentNode.addNode("Shared");

    Node currentNode = ActivityNode.addNode("testdoc.txt", "nt:file");

    String sharedLink = documentService.getDocumentUrlInSpaceDocuments(currentNode, spaceId);
    assertNotNull(sharedLink);
    assertFalse(sharedNode.hasNode("testdoc.txt"));
    assertTrue(ActivityNode.hasNode("testdoc.txt"));
  }

//  public void testGetMyWorkDocuments() throws Exception {
//    String userId = "root";
//    applyUserSession(userId, "gtn", COLLABORATION_WS);
//    List<Document> documents = documentService.getMyWorkDocuments(userId, 10);
//    assertNotNull("documents wasn't created", documents);
//    assertEquals(0, documents.size());
//  }
//  
//  public void testGetRecentSpacesDocuments() throws Exception {
//    String userId = "root";
//    applyUserSession(userId, "gtn", COLLABORATION_WS);
//    List<Document> documents = documentService.getRecentSpacesDocuments(10);
//    assertNotNull("documents wasn't created", documents);
//    assertEquals(0, documents.size());
//  }
  
  public void tearDown() throws Exception {
    if (session.isLive()) {
      session.logout();
    }
    super.tearDown();
  }
  
  private void clear() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = rootNode.getNode("Test.docx");
    documentNode.remove();
    session.save();
  }

}
