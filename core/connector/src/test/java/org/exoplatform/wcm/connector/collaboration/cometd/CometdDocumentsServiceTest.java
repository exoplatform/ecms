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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.oort.Oort;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.http.JSONHttpTransport;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
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

  private static final String      NODE1     = "http://node1:8080/cometd/cometd";

  private static final String      NODE2     = "http://node2:8080/cometd/cometd";

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
  public void testTheEditorsOfANodeThatLeftAreNoLongerCounted() {
    thisNode.joinCluster(NODE1);
    otherNode.joinCluster(NODE2);
    thisNode.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    thisNode.removeNode(NODE2);

    assertEquals(1, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testTheEditorsAPreviousRunLeftAreRemovedWhenTheNodeJoins() {
    EditorsContext crashedRun = new EditorsContext(editors);
    crashedRun.joinCluster(NODE2);
    crashedRun.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    otherNode.joinCluster(NODE2);

    assertEquals(0, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testANodeSeenLeavingPutsItsEditorsAgainWhenSeenJoining() {
    otherNode.joinCluster(NODE2);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);
    thisNode.removeNode(NODE2);
    assertEquals(0, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));

    otherNode.putClients();

    assertEquals(1, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testADocumentCountsAsOpenWhenTheEditorsCannotBeRead() throws Exception {
    ExoCache<String, String> unreadable = mock(ExoCache.class);
    when(unreadable.getCachedObjects()).thenThrow(new IllegalStateException("unreadable"));

    assertEquals(1, new EditorsContext(unreadable).getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testANodeLeavingTheClusterHasItsEditorsRemoved() {
    CometdDocumentsService service = service(mock(DocumentService.class));
    Oort oort = mock(Oort.class);
    when(oort.getURL()).thenReturn(NODE1);
    ArgumentCaptor<Oort.CometListener> listener = ArgumentCaptor.forClass(Oort.CometListener.class);
    otherNode.joinCluster(NODE2);
    otherNode.addClient("session2", FILE_ID, PROVIDER, WORKSPACE);

    service.joinCluster(oort);
    verify(oort).addCometListener(listener.capture());
    listener.getValue().cometLeft(new Oort.CometListener.Event(oort, "comet2", NODE2));

    assertEquals(0, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testThisNodePutsItsEditorsAgainWhenAnotherNodeJoins() {
    CometdDocumentsService service = service(mock(DocumentService.class));
    Oort oort = mock(Oort.class);
    when(oort.getURL()).thenReturn(NODE1);
    ArgumentCaptor<Oort.CometListener> listener = ArgumentCaptor.forClass(Oort.CometListener.class);
    service.joinCluster(oort);
    verify(oort).addCometListener(listener.capture());
    service.editorsContext.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    // The other node saw this one leave, for a while
    otherNode.removeNode(NODE1);
    assertEquals(0, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));

    listener.getValue().cometJoined(new Oort.CometListener.Event(oort, "comet2", NODE2));

    assertEquals(1, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testAnEditorAnotherNodeRemovedIsPutAgain() {
    thisNode.joinCluster(NODE1);
    thisNode.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    // The other node saw this one leave while it was up, or saw a previous run
    // of it leave once this run had put its editors
    otherNode.removeNode(NODE1);
    assertEquals(0, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));

    thisNode.restoreClients();

    assertEquals(1, otherNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testAnEditorTheCacheHoldsIsNotPutAgain() {
    ExoCache<String, String> spiedEditors = spy(new SimpleExoCache<String, String>());
    EditorsContext node = new EditorsContext(spiedEditors);
    node.joinCluster(NODE1);
    node.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    clearInvocations(spiedEditors);

    node.restoreClients();

    verify(spiedEditors, never()).put(any(), any());
  }

  @Test
  public void testAnEditorClosedAsItIsRestoredIsNotLeftInTheCache() {
    ExoCache<String, String> spiedEditors = spy(new SimpleExoCache<String, String>());
    EditorsContext node = new EditorsContext(spiedEditors);
    node.joinCluster(NODE1);
    node.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    spiedEditors.clearCache();
    // The session closes between the restore's check and its put
    doAnswer(invocation -> {
      node.removeClient("session1");
      return null;
    }).when(spiedEditors).get("session1");

    node.restoreClients();

    assertEquals(0, node.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testAnEditorClosedAsItIsPutForAJoiningNodeIsNotLeftInTheCache() {
    ExoCache<String, String> spiedEditors = spy(new SimpleExoCache<String, String>());
    EditorsContext node = new EditorsContext(spiedEditors);
    node.joinCluster(NODE1);
    node.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    // The session closes before the put of its entry lands
    doAnswer(invocation -> {
      node.removeClient("session1");
      return invocation.callRealMethod();
    }).when(spiedEditors).put(eq("session1"), any());

    node.putClients();

    assertEquals(0, node.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testTheEditorsOfThisNodeAreRestoredEveryPeriod() {
    CometdDocumentsService service = service(mock(EXoContinuationBayeux.class), mock(DocumentService.class));
    service.start();
    try {
      service.editorsContext.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
      editors.clearCache();
      RunnableScheduledFuture<?> restore = (RunnableScheduledFuture<?>) service.restoreExecutor.getQueue().peek();
      assertTrue(restore.isPeriodic());
      assertTrue(restore.getDelay(TimeUnit.SECONDS) > CometdDocumentsService.RESTORE_PERIOD - 10);

      // Run once here: off the queue, where the run schedules it again
      assertTrue(service.restoreExecutor.remove(restore));
      restore.run();

      assertEquals(1, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
    } finally {
      service.stop();
    }
    assertTrue(service.restoreExecutor.isTerminated());
    assertEquals(0, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  @Test
  public void testTheEditorsAreRestoredPeriodically() {
    CometdDocumentsService service = service(mock(DocumentService.class));
    service.editorsContext.addClient("session1", FILE_ID, PROVIDER, WORKSPACE);
    editors.clearCache();

    service.restoreEditors();

    assertEquals(1, thisNode.getOpenedEditorsCount(FILE_ID, PROVIDER));
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

  @Test
  public void testTheDocumentChannelsAreRelayedToTheOtherNodes() {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdDocumentsService service = service(exoBayeux, mock(DocumentService.class));

    service.start();
    try {
      verify(exoBayeux).observeChannel(CometdDocumentsService.CHANNEL_NAME + "**");
      // The periodic restore of this node's editors
      assertEquals(1, service.restoreExecutor.getQueue().size());
    } finally {
      service.stop();
    }
  }

  @Test
  public void testAMessageRelayedFromAnotherNodeIsHandledThereOnly() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdDocumentsService service = service(exoBayeux, mock(DocumentService.class));
    ServerSession oortSession = mock(ServerSession.class);
    when(exoBayeux.isRelayed(oortSession)).thenReturn(true);
    ServerMessage.Mutable relayed = mock(ServerMessage.Mutable.class);
    ServerMessage.Mutable published = mock(ServerMessage.Mutable.class);

    service.service.subscribeDocuments(oortSession, relayed, FILE_ID);
    service.service.subscribeDocuments(mock(ServerSession.class), published, FILE_ID);

    verify(relayed, never()).getData();
    verify(published).getData();
  }

  @Test
  public void testTheLastEditorClosedIsPublishedWithNoSubscriberOnThisNode() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    BayeuxServer bayeux = mock(BayeuxServer.class);
    ServerChannel channel = mock(ServerChannel.class);
    // No session of this node subscribes to the document's channel
    when(bayeux.getChannel(any())).thenReturn(null);
    when(bayeux.createChannelIfAbsent(CometdDocumentsService.CHANNEL_NAME + FILE_ID)).thenReturn(new MarkedReference<>(channel,
                                                                                                                         true));
    Field bayeuxField = CometdDocumentsService.CometdService.class.getDeclaredField("bayeux");
    bayeuxField.setAccessible(true);
    bayeuxField.set(service.service, bayeux);

    service.service.sendLastEditorClosedEvent(FILE_ID, PROVIDER);

    verify(channel).publish(any(), any(Object.class), any());
  }

  @Test
  public void testADocumentChannelNoSessionSubscribesToIsSwept() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    EXoContinuationBayeux bayeux = new EXoContinuationBayeux();
    bayeux.setOption(BayeuxServerImpl.TRANSPORTS_OPTION, JSONHttpTransport.class.getName());
    bayeux.start();
    try {
      bayeux.addListener(service.channelListener);
      // Created to publish an event, or by a message Oort relayed
      ServerChannel channel = bayeux.createChannelIfAbsent(CometdDocumentsService.CHANNEL_NAME + FILE_ID).getReference();
      assertTrue(channel.getListeners().contains(service.subscriptionListener));

      for (int i = 0; i < 5; i++) {
        bayeux.sweep();
      }

      assertNull(bayeux.getChannel(CometdDocumentsService.CHANNEL_NAME + FILE_ID));
    } finally {
      bayeux.stop();
    }
  }

  @Test
  public void testTheProvidersAvailableToAnOpenerAreSentToThatOpenerOnly() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    BayeuxServer bayeux = mock(BayeuxServer.class);
    Field bayeuxField = CometdDocumentsService.CometdService.class.getDeclaredField("bayeux");
    bayeuxField.setAccessible(true);
    bayeuxField.set(service.service, bayeux);
    ServerSession opener = mock(ServerSession.class);

    service.service.sendCurrentProviderInfo(opener, FILE_ID, true, List.of(PROVIDER));

    verify(opener).deliver(any(), eq(CometdDocumentsService.CHANNEL_NAME + FILE_ID), any(Object.class), any());
    verify(bayeux, never()).createChannelIfAbsent(any());
    verify(bayeux, never()).getChannel(any());
  }

  private CometdDocumentsService service(DocumentService documentService) {
    return service(mock(EXoContinuationBayeux.class), documentService);
  }

  private CometdDocumentsService service(EXoContinuationBayeux exoBayeux, DocumentService documentService) {
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.<String, String> getCacheInstance(CometdDocumentsService.EDITORS_CACHE_NAME)).thenReturn(editors);
    return new CometdDocumentsService(exoBayeux, documentService, null, null, cacheService);
  }
}
