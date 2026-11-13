/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.wcm.connector.collaboration.cometd;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.SimpleExoCache;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.wcm.connector.collaboration.cometd.CometdDocumentsService.EditorsContext;

/**
 * The editors open on a document are counted on every node: the last editor
 * closing on one node leaves the document's current editor in place while an
 * editor is open on another node.
 */
public class CometdDocumentsServiceTest {

  private static final String      FILE_ID   = "file";

  private static final String      PROVIDER  = "onlyoffice";

  private static final String      WORKSPACE = "collaboration";

  /** The cache every node shares, as the cluster replicates it */
  private ExoCache<String, String> editors;

  private EditorsContext           thisNode;

  private EditorsContext           otherNode;

  @Before
  public void setUp() {
    editors = new SimpleExoCache<>();
    thisNode = new EditorsContext(editors);
    otherNode = new EditorsContext(editors);
  }

  @Test
  public void testAnEditorOpenOnAnotherNodeIsCounted() {
    thisNode.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    thisNode.removeClient("session1");

    assertEquals(1, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
    assertEquals(0, thisNode.getOpenedEditorsCount(FILE_ID, "another"));
  }

  @Test
  public void testAStoppingNodeRemovesItsOwnEditorsOnly() {
    thisNode.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    thisNode.removeClients();

    assertEquals(1, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
    otherNode.removeClients();
    assertEquals(0, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testTheCurrentEditorStaysWhileAnEditorIsOpenOnAnotherNode() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    CometdDocumentsService service = service(documentService);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    service.resetCurrentProviderIfClosed(FILE_ID, WORKSPACE, PROVIDER);

    verify(documentService, never()).getCurrentDocumentProvider(any(), any());
  }

  @Test
  public void testTheCurrentEditorIsResetOnceNoEditorIsOpen() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    CometdDocumentsService service = service(documentService);

    service.resetCurrentProviderIfClosed(FILE_ID, WORKSPACE, PROVIDER);

    verify(documentService).getCurrentDocumentProvider(FILE_ID, WORKSPACE);
  }

  private CometdDocumentsService service(DocumentService documentService) {
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.<String, String> getCacheInstance(CometdDocumentsService.EDITORS_CACHE_NAME)).thenReturn(editors);
    return new CometdDocumentsService(mock(EXoContinuationBayeux.class), documentService, null, null, cacheService);
  }
}
