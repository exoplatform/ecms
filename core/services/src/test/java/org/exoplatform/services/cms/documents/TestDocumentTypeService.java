/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 **************************************************************************/
package org.exoplatform.services.cms.documents;

import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SARL Author : Phan Trong Lam
 * lamptdev@gmail.com
 * Oct 6, 2009
 */
public class TestDocumentTypeService extends BaseWCMTestCase {

  private final static String NT_UNSTRUCTURED   = "nt:unstructured";

  private final static String NT_FILE           = "nt:file";

  private final static String JCR_MINE_TYPE     = "jcr:mimeType";

  private final static String JCR_LAST_MODIFIED = "jcr:lastModified";

  private final static String JCR_DATA          = "jcr:data";

  private final static String NT_RESOURCE       = "nt:resource";

  private DocumentTypeService documentTypeService_;

  public void setUp() throws Exception {
    super.setUp();
    documentTypeService_ = (DocumentTypeService) container.getComponentInstanceOfType(DocumentTypeService.class);
    applySystemSession();
    init();
  }

  public void tearDown() throws Exception {
    clear();
    session.save();
    session.logout();
    super.tearDown();
  }

  /**
   * Execute test for getAllSupportedType method
   * Input : dms-document-type-configuration.xml
   * Expect: Get list of all supported types include Video, Images.
   * @throws Exception
   */
  public void testAllSupportedType() throws Exception {

    List<String> resultList = documentTypeService_.getAllSupportedType();

    assertEquals(1, resultList.size()); // Content
  }

  /**
   * Execute test for getAllDocumentsByKindOfDocumentType method.
   * Input : Video supported type
   * Expect: Get list of mime types of Video supported type.
   * @throws Exception
   */
  public void testAllDocumentsByKindOfDocumentType() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "videomp302", "video/mp3");
    addDocumentFile(documentNode, "One night at museum", "video/mpeg");
    addDocumentFile(documentNode, "Black dahlia", "video/mpeg");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/jpeg");
    addDocumentFile(documentNode, "image03", "image/png");
    addDocumentFile(documentNode, "image04", "image/tiff");
    documentNode.getSession().save();
    String supportedType = "Video";
    List<Node> resultList = documentTypeService_
                  .getAllDocumentsByDocumentType(supportedType, COLLABORATION_WS, createSessionProvider());
    assertEquals(2, resultList.size());
  }

  /**
   * Test method getAllDocumentsByType
   * Input : image type
   * Expect: Get list of nodes in which jcr:mimeType properties are image/gif types.
   * @throws Exception
   */
  public void testAllDocumentsByType() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text01", "text/plain");
    documentNode.getSession().save();

    List<Node> resultList = documentTypeService_.getAllDocumentsByType(COLLABORATION_WS, createSessionProvider(), "image/gif");
    assertNotNull(resultList);
    assertEquals(3, resultList.size());
  }

  /**
   * Test method getAllDocumentsByType
   * Input : image, audio, text types.
   * Expect: Get list of nodes in which include jcr:mimeType properties are image/gif,
   * audio/mp3, text/plain types.
   * @throws Exception
   */
  public void testAllDocumentsByTypes() throws Exception {
    String[] mimeTypes = { "image/gif", "audio/mp3" };
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text01", "text/plain");
    documentNode.getSession().save();

    List<Node> resultList = documentTypeService_.getAllDocumentsByType(COLLABORATION_WS, createSessionProvider(), mimeTypes);
    assertEquals(5, resultList.size());
  }

  /**
   * Test method getAllDocumentsByUser
   * Input : audio, text types and root user name.
   * Expect: Get list of nodes in which jcr:mimeType properties are audio/mp3, text/plain and
   * exo:owner is root
   * @throws Exception
   */
  public void testAllDocumentsByUser01() throws Exception {
    String[] mimeTypes = { "audio/mp3", "text/plain" };
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    Node testaudio01 = addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    Node testaudio02 = addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    Node image01 = addDocumentFile(documentNode, "image01", "image/gif");
    Node image02 = addDocumentFile(documentNode, "image02", "image/gif");
    Node image03 = addDocumentFile(documentNode, "image03", "image/gif");
    Node text = addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String username = "__system";

    List<Node> resultList = documentTypeService_.getAllDocumentsByUser(COLLABORATION_WS,
        createSessionProvider(), mimeTypes, username);
    assertTrue(resultList.contains(testaudio01));
    assertTrue(resultList.contains(testaudio02));
    assertFalse(resultList.contains(image01));
    assertFalse(resultList.contains(image02));
    assertFalse(resultList.contains(image03));
    assertTrue(resultList.contains(text));
  }

  /**
   * Test method getAllDocumentsByUser
   * Input : audio, text types and jame user name.
   * Expect: Get an empty list of nodes.
   * @throws Exception
   */
  public void testAllDocumentsByUser02() throws Exception {
    String[] mimeTypes = { "audio/mp3", "text/plain" };
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String username = "jame";

    List<Node> resultList = documentTypeService_.getAllDocumentsByUser(COLLABORATION_WS,
        createSessionProvider(), mimeTypes, username);

    assertEquals(0, resultList.size());
  }

  /**
   * Test AllDocumentByContentsType with content type is not content
   *
   * Expect: empty list of nodes.
   * @throws Exception
   */
  public void testGetAllDocumentByContentsTypeIsNoneContent() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String documentType = "NoneContent";

    List<Node> resultList = documentTypeService_.getAllDocumentByContentsType(documentType, COLLABORATION_WS, sessionProvider, null);

    assertEquals(null, resultList);
  }

  /**
   * Test AllDocumentByContentsType with content type is content
   *
   * Expect: nodes that are content
   * @throws Exception
   */
  public void testGetAllDocumentByContentsTypeIsContent() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    Node testaudio01 = addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    Node testaudio02 = addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    Node image01 = addDocumentFile(documentNode, "image01", "image/gif");
    Node image02 = addDocumentFile(documentNode, "image02", "image/gif");
    Node image03 = addDocumentFile(documentNode, "image03", "image/gif");
    Node text = addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String documentType = "Content";

    List<Node> resultList = documentTypeService_.getAllDocumentByContentsType(documentType, COLLABORATION_WS, sessionProvider, null);

    assertTrue(resultList.contains(testaudio01));
    assertTrue(resultList.contains(testaudio02));
    assertTrue(resultList.contains(image01));
    assertTrue(resultList.contains(image02));
    assertTrue(resultList.contains(image03));
    assertTrue(resultList.contains(text));
  }

  /**
   * Test AllDocumentByContentsType with content type is content and have specific owner
   *
   * Expect: nodes that are content and have specific owner
   * @throws Exception
   */
  public void testGetAllDocumentByContentsTypeAndOwner() throws Exception {
    Node rootNode = session.getRootNode();
    Node documentNode = getDocument(rootNode, "document");
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String documentType = "Content";

    List<Node> resultList = documentTypeService_.getAllDocumentByContentsType(documentType, COLLABORATION_WS, sessionProvider, "john");

    assertEquals(0, resultList.size());
  }

  private SessionProvider createSessionProvider() {
    SessionProviderService sessionProviderService = (SessionProviderService) container
        .getComponentInstanceOfType(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }

  /**
   * @throws Exception
   */
  private void init() throws Exception {

    // 1. Get a rootNode from session
    Node rootNode = session.getRootNode();

    // 2. Create basic tree nodes and it's properties
    rootNode.addNode("document", NT_UNSTRUCTURED);
    session.save();
  }

  /**
   * @throws Exception
   */
  private void clear() throws Exception {

    // 1. Get a rootNode from session
    Node rootNode = session.getRootNode();

    // 2. Get and delete test nodes
    Node documentNode = rootNode.getNode("document");
    documentNode.remove();
    session.save();
  }

  /**
   * @param currentNode
   * @param testName
   * @param mimeTypeValue
   * @return newly created node
   * @throws Exception
   */
  private Node addDocumentFile(Node currentNode, String testName, String mimeTypeValue)
      throws Exception {
    Node documentNode = currentNode.addNode(testName, NT_FILE);
    Node subNode = documentNode.addNode("jcr:content", NT_RESOURCE);
    subNode.setProperty(JCR_MINE_TYPE, mimeTypeValue);
    subNode.setProperty(JCR_DATA, "");
    subNode.setProperty(JCR_LAST_MODIFIED, new GregorianCalendar());
    return documentNode;
  }

  /**
   * @param parentNode
   * @param documentName
   * @return
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private Node getDocument(Node parentNode, String documentName) throws PathNotFoundException,
      RepositoryException {
    try {
      return parentNode.getNode(documentName);
    } catch(PathNotFoundException e) {
      return parentNode.addNode(documentName);
    }
  }
}
