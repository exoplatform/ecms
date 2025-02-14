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
