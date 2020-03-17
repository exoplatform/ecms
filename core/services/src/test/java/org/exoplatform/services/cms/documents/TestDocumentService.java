package org.exoplatform.services.cms.documents;

import javax.jcr.Node;

import org.exoplatform.services.wcm.BaseWCMTestCase;

public class TestDocumentService extends BaseWCMTestCase {

  private DocumentService documentService;

  public void setUp() throws Exception {
    super.setUp();
    documentService = container.getComponentInstanceOfType(DocumentService.class);
    applySystemSession();
  }

  public void testCreateDocumentFromEmptyTemplate() throws Exception {
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
  }
  
  public void tearDown() throws Exception {
    clear();
    session.save();
    session.logout();
    super.tearDown();
  }
  
  public void clear () throws Exception {
    Node rootNode = session.getRootNode();

    Node documentNode = rootNode.getNode("Test.docx");
    documentNode.remove();
    session.save();
  }

}
