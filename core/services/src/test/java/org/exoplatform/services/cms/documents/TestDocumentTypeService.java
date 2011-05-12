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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL Author : Phan Trong Lam
 * lamptdev@gmail.com
 * Oct 6, 2009
 */
public class TestDocumentTypeService extends BaseDMSTestCase {

  private final static String NT_UNSTRUCTURED   = "nt:unstructured";

  private final static String NT_FILE           = "nt:file";

  private final static String JCR_MINE_TYPE     = "jcr:mimeType";

  private final static String JCR_LAST_MODIFIED = "jcr:lastModified";

  private final static String JCR_DATA          = "jcr:data";

  private final static String NT_RESOURCE       = "nt:resource";

  private DocumentTypeService documentTypeService_;

  /*
   * (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    init();
    documentTypeService_ = (DocumentTypeService) container
                .getComponentInstanceOfType(DocumentTypeService.class);
  }

  /*
   * (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    clear();
    session.save();
    session.logout();
  }

  /**
   * Execute test for getAllSupportedType method
   * Input : dms-document-type-configuration.xml
   * Expect: Get list of all supported types include Video, Images.
   * @throws Exception
   */
  public void testAllSupportedType() throws Exception {

    List<String> expectedList = documentTypeService_.getAllSupportedType();

    Iterator<String> iterSupportedType = expectedList.iterator();
    String expectedSupportedName = null;
    while (iterSupportedType.hasNext()) {
      expectedSupportedName = iterSupportedType.next();
      System.out.println(" The supported type is :" +expectedSupportedName);
    }
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
    List<Node> expectedList = documentTypeService_
                  .getAllDocumentsByDocumentType(supportedType, COLLABORATION_WS,
        REPO_NAME, createSessionProvider());
    assertNotNull(expectedList);
    Iterator<Node> iterNodes = expectedList.iterator();
    Node expectedNode = null;
    while (iterNodes.hasNext()) {
      expectedNode = iterNodes.next();
      System.out.println("The node name : "+expectedNode.getName()+" have mime type : "
        + expectedNode.getNode("jcr:content").getProperty("jcr:mimeType").getValue().getString());
    }
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

    List<Node> expectedList = documentTypeService_.getAllDocumentsByType(COLLABORATION_WS,
        REPO_NAME, createSessionProvider(), "image/gif");
    assertNotNull(expectedList);
    assertEquals(3, expectedList.size());
    Iterator<Node> iterNodes = expectedList.iterator();
    Node expectedNode = null;
    while (iterNodes.hasNext()) {
      expectedNode = iterNodes.next();
      assertEquals("image/gif",
          expectedNode.getNode("jcr:content").getProperty(JCR_MINE_TYPE).getString());
    }
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

    List<Node> expectedList = documentTypeService_.getAllDocumentsByType(COLLABORATION_WS,
        REPO_NAME, createSessionProvider(), mimeTypes);
    Iterator<Node> expectedIter  = expectedList.iterator();
    Node expectedNode = null;
    while( expectedIter.hasNext()) {
      expectedNode = expectedIter.next();
      System.out.println("Expected mime type:"
             +expectedNode.getNode("jcr:content").getProperty(JCR_MINE_TYPE).getString());
    }
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
    addDocumentFile(documentNode, "testaudio01", "audio/mp3");
    addDocumentFile(documentNode, "testaudio02", "audio/mp3");
    addDocumentFile(documentNode, "image01", "image/gif");
    addDocumentFile(documentNode, "image02", "image/gif");
    addDocumentFile(documentNode, "image03", "image/gif");
    addDocumentFile(documentNode, "text", "text/plain");
    documentNode.getSession().save();
    String username = "__system";

    List<Node> expectedList = documentTypeService_.getAllDocumentsByUser(COLLABORATION_WS,
        createSessionProvider(), mimeTypes, username);
    //TODO: Need to check why the list is empty
    //    assertEquals(3, expectedList.size());
    Iterator<Node> iterator = expectedList.iterator();
    Node expectedNode= null;
    while (iterator.hasNext()) {
      expectedNode = iterator.next();
      System.out.println("Expected mime type:"
        +expectedNode.getNode("jcr:content").getProperty(JCR_MINE_TYPE).getString());
    }
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

    List<Node> expectedList = documentTypeService_.getAllDocumentsByUser(COLLABORATION_WS,
        createSessionProvider(), mimeTypes, username);

    assertEquals(0, expectedList.size());
    Iterator<Node> iterator = expectedList.iterator();
    Node expectedNode= null;
    while (iterator.hasNext()) {
      expectedNode = iterator.next();
      System.out.println("Expected mime type:"
                      +expectedNode.getProperty(JCR_MINE_TYPE).getString());
    }
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
   * @throws Exception
   */
  private void addDocumentFile(Node currentNode, String testName, String mimeTypeValue)
      throws Exception {
    Node documentNode = currentNode.addNode(testName, NT_FILE);
    Node subNode = documentNode.addNode("jcr:content", NT_RESOURCE);
    subNode.setProperty(JCR_MINE_TYPE, mimeTypeValue);
    subNode.setProperty(JCR_DATA, "");
    subNode.setProperty(JCR_LAST_MODIFIED, new GregorianCalendar());

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
