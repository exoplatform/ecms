package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;

public class TestDocumentService extends BaseWCMTestCase {

  private DocumentService documentService;
  private NodeHierarchyCreator nodeHierarchyCreator;

  public void setUp() throws Exception {
    super.setUp();
    documentService = container.getComponentInstanceOfType(DocumentService.class);
    nodeHierarchyCreator = container.getComponentInstanceOfType(NodeHierarchyCreator.class);
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
  
  public void testGetPrivateDocuments() throws Exception {
    String userId = "root";
    applyUserSession(userId, "gtn", COLLABORATION_WS);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);
    userNode.addNode("doc.txt", "nt:file");
    session.save();
    List<Document> documents = documentService.getPrivateDocuments(userId, 10);
    assertNotNull("documents wasn't created", documents);
    assertEquals(0, documents.size());
  }
  
  public void testGetMyWorkDocuments() throws Exception {
    String userId = "root";
    applyUserSession(userId, "gtn", COLLABORATION_WS);
    List<Document> documents = documentService.getMyWorkDocuments(userId, 10);
    assertNotNull("documents wasn't created", documents);
    assertEquals(0, documents.size());
  }
  
  public void testGetRecentSpacesDocuments() throws Exception {
    String userId = "root";
    applyUserSession(userId, "gtn", COLLABORATION_WS);
    List<Document> documents = documentService.getRecentSpacesDocuments(10);
    assertNotNull("documents wasn't created", documents);
    assertEquals(0, documents.size());
  }
  
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
