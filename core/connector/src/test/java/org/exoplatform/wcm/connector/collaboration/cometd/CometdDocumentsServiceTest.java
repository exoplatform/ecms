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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.cometd.annotation.Service;
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
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.junit.Test;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.SimpleExoCache;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.wcm.utils.RepositoryConsumer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
  public void testTheCommandsRunOnDaemonThreadsThatEndWithTheService() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    Future<Boolean> daemon = service.eventsHandlers.submit(() -> Thread.currentThread().isDaemon());
    assertTrue(daemon.get(5, TimeUnit.SECONDS));

    service.stop();

    assertTrue(service.eventsHandlers.isTerminated());
  }

  @Test
  public void testTheStopWaitsForACommandAlreadySubmitted() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean ended = new AtomicBoolean();
    service.eventsHandlers.submit(() -> {
      started.countDown();
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      ended.set(true);
    });
    assertTrue(started.await(5, TimeUnit.SECONDS));

    service.stop();

    assertTrue(ended.get());
    assertTrue(service.eventsHandlers.isTerminated());
  }

  @Test
  public void testAStopInterruptedWhileWaitingKeepsTheInterruptOfItsThread() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    service.eventsHandlers.submit(() -> {
      started.countDown();
      try {
        release.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    assertTrue(started.await(5, TimeUnit.SECONDS));
    long start = System.nanoTime();
    Thread.currentThread().interrupt();
    try {
      service.stop();
      assertTrue("A stop interrupted while waiting for the commands must keep the interrupt of its thread",
                 Thread.interrupted());
      long waited = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      assertTrue("An interrupted stop must not wait for the commands", waited < CometdDocumentsService.STOP_TIMEOUT);
    } finally {
      Thread.interrupted();
      release.countDown();
    }
    assertTrue(service.eventsHandlers.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  public void testTheTimerOfAClosedEditorRunsOnADaemonThread() {
    CometdDocumentsService service = service(mock(DocumentService.class));
    Timer timer = service.scheduleProviderReset(FILE_ID, WORKSPACE, PROVIDER, TimeUnit.MINUTES.toMillis(10));
    try {
      List<Thread> threads = timerThreads();
      assertEquals(1, threads.size());
      assertTrue(threads.get(0).isDaemon());
    } finally {
      timer.cancel();
    }
  }

  @Test
  public void testTheTimerOfAClosedEditorEndsAfterItsReset() throws Exception {
    CometdDocumentsService service = spy(service(mock(DocumentService.class)));
    doNothing().when(service).queueProviderReset(FILE_ID, WORKSPACE, PROVIDER);

    service.scheduleProviderReset(FILE_ID, WORKSPACE, PROVIDER, 0);

    verify(service, timeout(5000)).queueProviderReset(FILE_ID, WORKSPACE, PROVIDER);
    long deadline = System.currentTimeMillis() + 5000;
    while (!timerThreads().isEmpty() && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
    assertTrue(timerThreads().isEmpty());
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

  /**
   * The reset writes the provider in place, before it publishes the last
   * editor closed event, instead of queuing the write.
   */
  @Test
  public void testTheCurrentEditorIsResetOnceNoEditorIsOpen() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn(PROVIDER);
    CometdDocumentsService service = recordingService(mock(EXoContinuationBayeux.class),
                                                      documentService,
                                                      mock(IdentityRegistry.class),
                                                      true);
    ServerChannel channel = bayeuxChannel(service);

    service.resetCurrentProviderIfClosed(FILE_ID, WORKSPACE, PROVIDER);

    InOrder order = inOrder(documentService, channel);
    order.verify(documentService).getCurrentDocumentProvider(FILE_ID, WORKSPACE);
    order.verify(documentService).saveCurrentDocumentProvider(FILE_ID, WORKSPACE, null);
    order.verify(channel).publish(any(), eq(lastEditorClosedEvent()), any());
    assertEquals("the reset queues no write", 0, recorded(service).size());
  }

  /**
   * The closing delay's timer hands the reset to the documents executor,
   * where it runs in the container.
   */
  @Test
  public void testTheResetIsQueuedInTheDocumentsExecutor() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    CometdDocumentsService service = recordingService(mock(EXoContinuationBayeux.class),
                                                      documentService,
                                                      mock(IdentityRegistry.class),
                                                      true);

    service.queueProviderReset(FILE_ID, WORKSPACE, PROVIDER);

    assertEquals(1, recorded(service).size());
    verify(documentService, never()).getCurrentDocumentProvider(any(), any());
    ((CometdDocumentsService.ContainerCommand) recorded(service).get(0)).execute(null);
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

  /**
   * A client publishes the opened event only: the other events are the
   * server's. Mutation: deliver a client's other event, and this test fails.
   */
  @Test
  public void testAClientPublishesTheOpenedEventOnly() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdDocumentsService service = recordingService(exoBayeux, mock(DocumentService.class), mock(IdentityRegistry.class), true);
    ServerSession johnSession = mock(ServerSession.class);
    ServerMessage.Mutable closed = mock(ServerMessage.Mutable.class);
    Map<String, Object> data = new HashMap<>();
    data.put("type", CometdDocumentsService.LAST_EDITOR_CLOSED_EVENT);
    when(closed.getData()).thenReturn(data);
    when(closed.getDataAsMap()).thenReturn(data);
    ServerMessage.Mutable text = mock(ServerMessage.Mutable.class);
    when(text.getData()).thenReturn("text");

    assertFalse("a client's last editor closed event is not delivered", service.service.subscribeDocuments(johnSession, closed, FILE_ID));
    assertFalse("a client's message that isn't an event is not delivered", service.service.subscribeDocuments(johnSession, text, FILE_ID));
    assertEquals(0, recorded(service).size());
  }

  @Test
  public void testAMessageRelayedFromAnotherNodeIsHandledThereOnly() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdDocumentsService service = service(exoBayeux, mock(DocumentService.class));
    ServerSession oortSession = mock(ServerSession.class);
    when(exoBayeux.isRelayed(oortSession)).thenReturn(true);
    ServerMessage.Mutable relayed = mock(ServerMessage.Mutable.class);
    ServerMessage.Mutable published = mock(ServerMessage.Mutable.class);

    assertTrue("a relayed message is delivered to this node's subscribers",
               service.service.subscribeDocuments(oortSession, relayed, FILE_ID));
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

  @Test
  public void testADocumentOpenedEventIsHandledOnlyForTheUserOfTheSendingSession() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.isAvailableForUser(any())).thenReturn(true);
    when(editorProvider.getProviderName()).thenReturn(PROVIDER);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    when(documentService.getDocumentEditorProviders()).thenReturn(List.of(editorProvider));
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn(null);
    IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
    when(identityRegistry.getIdentity("john")).thenReturn(new Identity("john"));
    CometdDocumentsService service = recordingService(exoBayeux, documentService, identityRegistry, true);
    ServerChannel channel = bayeuxChannel(service);

    assertFalse("an event naming another user than the sender's is not delivered",
                service.service.subscribeDocuments(johnSession, openedEvent("mary"), FILE_ID));
    assertEquals("an event naming another user than the sender's is ignored", 0, recorded(service).size());

    assertFalse("an event naming no user is not delivered", service.service.subscribeDocuments(johnSession, openedEvent(null), FILE_ID));
    assertEquals("an event naming no user is ignored", 0, recorded(service).size());

    assertFalse("the sender's own event is not delivered as published",
                service.service.subscribeDocuments(johnSession, openedEvent("john"), FILE_ID));
    assertEquals("the sender's own event is handled", 1, recorded(service).size());
    ((CometdDocumentsService.ContainerCommand) recorded(service).get(0)).execute(null);
    verify(johnSession).deliver(any(), eq(CometdDocumentsService.CHANNEL_NAME + FILE_ID), any(Object.class), any());
    ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
    verify(channel).publish(any(), published.capture(), any());
    assertEquals("the handled event answers the sender's session and publishes the document's opened event",
                 "{\"type\": \"DOCUMENT_OPENED\", \"fileId\": \"" + FILE_ID + "\", \"provider\": \"" + PROVIDER + "\"}",
                 published.getValue());
  }

  @Test
  public void testAMalformedDocumentOpenedEventIsDropped() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    DocumentService documentService = mock(DocumentService.class);
    CometdDocumentsService service = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);

    for (Object malformed : new Object[] { 42L, Map.of("name", "john") }) {
      ServerMessage.Mutable userEvent = openedEvent("john");
      userEvent.getDataAsMap().put("userId", malformed);
      assertFalse("an event whose user is not a string is not delivered", service.service.subscribeDocuments(johnSession, userEvent, FILE_ID));
      assertEquals("an event whose user is not a string is ignored", 0, recorded(service).size());

      ServerMessage.Mutable workspaceEvent = openedEvent("john");
      workspaceEvent.getDataAsMap().put("workspace", malformed);
      assertFalse("an event whose workspace is not a string is not delivered",
                  service.service.subscribeDocuments(johnSession, workspaceEvent, FILE_ID));
      assertEquals("an event whose workspace is not a string is handled as one without it", 1, recorded(service).size());
      recorded(service).clear();

      ServerMessage.Mutable providerEvent = openedEvent("john");
      providerEvent.getDataAsMap().put("provider", malformed);
      assertFalse("an event whose provider is not a string is not delivered",
                  service.service.subscribeDocuments(johnSession, providerEvent, FILE_ID));
      assertEquals("an event whose provider is not a string is handled as one without it", 1, recorded(service).size());
      recorded(service).clear();

      ServerMessage.Mutable typeEvent = openedEvent("john");
      typeEvent.getDataAsMap().put("type", malformed);
      assertFalse("an event whose type is not a string is not delivered", service.service.subscribeDocuments(johnSession, typeEvent, FILE_ID));
      assertEquals("an event whose type is not a string is ignored", 0, recorded(service).size());
    }

    ServerMessage.Mutable unreadable = mock(ServerMessage.Mutable.class);
    when(unreadable.getData()).thenReturn(new HashMap<String, Object>());
    when(unreadable.getDataAsMap()).thenThrow(new IllegalStateException("unreadable"));
    assertFalse("an event the listener cannot read is not delivered", service.service.subscribeDocuments(johnSession, unreadable, FILE_ID));
    assertEquals(0, recorded(service).size());
  }

  @Test
  public void testTheEditorIsRecordedOnlyForAUserWhoMayEditTheDocument() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.isAvailableForUser(any())).thenReturn(true);
    when(editorProvider.getProviderName()).thenReturn(PROVIDER);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    when(documentService.getDocumentEditorProviders()).thenReturn(List.of(editorProvider));
    // No editor is recorded for the document yet
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn(null);
    IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
    when(identityRegistry.getIdentity("john")).thenReturn(new Identity("john"));
    ServerSession opener = mock(ServerSession.class);

    CometdDocumentsService readerService = recordingService(mock(EXoContinuationBayeux.class), documentService, identityRegistry, false);
    ServerChannel readerChannel = bayeuxChannel(readerService);
    readerService.service.onDocumentOpened(opener, "john", FILE_ID, WORKSPACE, PROVIDER);
    assertEquals("a user who may not edit the document records no editor", 0, recorded(readerService).size());
    verify(opener).deliver(any(), eq(CometdDocumentsService.CHANNEL_NAME + FILE_ID), any(Object.class), any());
    verify(readerChannel, never()).publish(any(), any(Object.class), any());

    CometdDocumentsService editorService = recordingService(mock(EXoContinuationBayeux.class), documentService, identityRegistry, true);
    ServerChannel editorChannel = bayeuxChannel(editorService);
    editorService.service.onDocumentOpened(opener, "john", FILE_ID, WORKSPACE, PROVIDER);
    assertEquals("a user who may edit the document records the editor", 1, recorded(editorService).size());
    ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
    verify(editorChannel).publish(any(), published.capture(), any());
    assertEquals("the opened event is republished with the checked provider and the document only",
                 "{\"type\": \"DOCUMENT_OPENED\", \"fileId\": \"" + FILE_ID + "\", \"provider\": \"" + PROVIDER + "\"}",
                 published.getValue());
  }

  @Test
  public void testAnOpenedEventWhoseOpenIsRefusedIsNeitherRecordedNorPublished() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.isAvailableForUser(any())).thenReturn(true);
    when(editorProvider.getProviderName()).thenReturn(PROVIDER);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    when(documentService.getDocumentEditorProviders()).thenReturn(List.of(editorProvider));
    IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
    when(identityRegistry.getIdentity("john")).thenReturn(new Identity("john"));
    ServerSession opener = mock(ServerSession.class);

    // Another editor is recorded for the document
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn("another");
    CometdDocumentsService other = recordingService(mock(EXoContinuationBayeux.class), documentService, identityRegistry, true);
    ServerChannel otherChannel = bayeuxChannel(other);
    other.service.onDocumentOpened(opener, "john", FILE_ID, WORKSPACE, PROVIDER);
    verify(otherChannel, never()).publish(any(), any(Object.class), any());
    assertEquals(0, recorded(other).size());

    // The provider is not available to the user
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn(null);
    when(editorProvider.isAvailableForUser(any())).thenReturn(false);
    CometdDocumentsService unavailable = recordingService(mock(EXoContinuationBayeux.class), documentService, identityRegistry, true);
    ServerChannel unavailableChannel = bayeuxChannel(unavailable);
    unavailable.service.onDocumentOpened(opener, "john", FILE_ID, WORKSPACE, PROVIDER);
    verify(unavailableChannel, never()).publish(any(), any(Object.class), any());
    assertEquals(0, recorded(unavailable).size());
  }

  @Test
  public void testAnOpenedEventOfAnUnknownProviderIsNotRepublished() throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    when(documentService.getEditorProvider("unknown")).thenThrow(new DocumentEditorProviderNotFoundException("unknown"));
    ServerSession opener = mock(ServerSession.class);
    CometdDocumentsService service = recordingService(mock(EXoContinuationBayeux.class), documentService, mock(IdentityRegistry.class), true);
    ServerChannel channel = bayeuxChannel(service);

    service.service.onDocumentOpened(opener, "john", FILE_ID, WORKSPACE, "unknown");

    verify(channel, never()).publish(any(), any(Object.class), any());
    assertEquals(0, recorded(service).size());
  }

  @Test
  public void testTheEditRightIsReadOnAPlainDocumentAndFailsClosed() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    ManageableRepository repository = mock(ManageableRepository.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    Session session = mock(Session.class);
    when(sessionProvider.getSession(WORKSPACE, repository)).thenReturn(session);
    ExtendedNode plain = mock(ExtendedNode.class);
    when(plain.isNodeType("exo:symlink")).thenReturn(false);
    when(session.getNodeByUUID(FILE_ID)).thenReturn(plain);

    try (MockedStatic<WCMCoreUtils> wcmCoreUtils = mockStatic(WCMCoreUtils.class)) {
      wcmCoreUtils.when(WCMCoreUtils::getRepository).thenReturn(repository);
      wcmCoreUtils.when(() -> WCMCoreUtils.invokeUserSession(eq("john"), any())).thenAnswer(invocation -> {
        RepositoryConsumer<SessionProvider> handler = invocation.getArgument(1);
        handler.accept(sessionProvider);
        return null;
      });

      assertTrue("a user who may set the properties of a plain document may edit it",
                 service.canEditDocument("john", FILE_ID, WORKSPACE));
      verify(plain).checkPermission(PermissionType.SET_PROPERTY);
      verify(plain, never()).getProperty(any());

      when(session.getNodeByUUID(FILE_ID)).thenThrow(new ItemNotFoundException());
      assertFalse("an unknown document may not be edited", service.canEditDocument("john", FILE_ID, WORKSPACE));

      doThrow(new RepositoryException("unreadable")).when(session).getNodeByUUID(FILE_ID);
      assertFalse("a document that cannot be read may not be edited", service.canEditDocument("john", FILE_ID, WORKSPACE));
    }
  }

  private static ServerChannel bayeuxChannel(CometdDocumentsService service) throws Exception {
    BayeuxServer bayeux = mock(BayeuxServer.class);
    ServerChannel channel = mock(ServerChannel.class);
    when(bayeux.createChannelIfAbsent(CometdDocumentsService.CHANNEL_NAME + FILE_ID)).thenReturn(new MarkedReference<>(channel, false));
    Field bayeuxField = CometdDocumentsService.CometdService.class.getDeclaredField("bayeux");
    bayeuxField.setAccessible(true);
    bayeuxField.set(service.service, bayeux);
    return channel;
  }

  @Test
  public void testTheEditRightIsReadWithASessionOfTheUserOnTheShortcutTarget() throws Exception {
    CometdDocumentsService service = service(mock(DocumentService.class));
    ManageableRepository repository = mock(ManageableRepository.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    Session session = mock(Session.class);
    Session targetSession = mock(Session.class);
    when(sessionProvider.getSession(WORKSPACE, repository)).thenReturn(session);
    when(sessionProvider.getSession("knowledge", repository)).thenReturn(targetSession);
    Node shortcut = mock(Node.class);
    when(shortcut.isNodeType("exo:symlink")).thenReturn(true);
    Property workspaceProperty = mock(Property.class);
    when(workspaceProperty.getString()).thenReturn("knowledge");
    when(shortcut.getProperty("exo:workspace")).thenReturn(workspaceProperty);
    Property uuidProperty = mock(Property.class);
    when(uuidProperty.getString()).thenReturn("target");
    when(shortcut.getProperty("exo:uuid")).thenReturn(uuidProperty);
    when(session.getNodeByUUID(FILE_ID)).thenReturn(shortcut);
    ExtendedNode target = mock(ExtendedNode.class);
    when(targetSession.getNodeByUUID("target")).thenReturn(target);

    try (MockedStatic<WCMCoreUtils> wcmCoreUtils = mockStatic(WCMCoreUtils.class)) {
      wcmCoreUtils.when(WCMCoreUtils::getRepository).thenReturn(repository);
      wcmCoreUtils.when(() -> WCMCoreUtils.invokeUserSession(eq("john"), any())).thenAnswer(invocation -> {
        RepositoryConsumer<SessionProvider> handler = invocation.getArgument(1);
        handler.accept(sessionProvider);
        return null;
      });

      assertTrue("a user who may set the target's properties may edit it",
                 service.canEditDocument("john", FILE_ID, WORKSPACE));
      verify(target).checkPermission(PermissionType.SET_PROPERTY);

      doThrow(new AccessControlException("read only")).when(target).checkPermission(PermissionType.SET_PROPERTY);
      assertFalse("a user who may only read the target may not edit it", service.canEditDocument("john", FILE_ID, WORKSPACE));

      when(targetSession.getNodeByUUID("target")).thenThrow(new AccessDeniedException());
      assertFalse("a user who may not read the target may not edit it", service.canEditDocument("john", FILE_ID, WORKSPACE));
    }
    verify(sessionProvider, times(3)).close();
  }

  @Test
  public void testAnEditorSubscriptionIsRegisteredOnlyForAUserWhoMayEditTheDocument() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    when(channel.getSubscribers()).thenReturn(Set.of(johnSession));

    CometdDocumentsService reader = recordingService(exoBayeux, mock(DocumentService.class), mock(IdentityRegistry.class), false);
    reader.new ChannelSubscriptionListener().subscribed(johnSession, channel, subscription("john"));
    assertEquals("the check is handed to the documents executor", 1, recorded(reader).size());
    assertEquals(0, reader.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    ((CometdDocumentsService.ContainerCommand) recorded(reader).get(0)).execute(null);
    assertEquals("a user who may not edit the document registers no editor",
                 0, reader.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));

    CometdDocumentsService editor = recordingService(exoBayeux, documentServiceOf(null, true), mock(IdentityRegistry.class), true);
    bayeuxChannel(editor);
    editor.new ChannelSubscriptionListener().subscribed(johnSession, channel, subscription("mary"));
    assertEquals("a user who does not own the sending connection is not checked", 0, recorded(editor).size());
    editor.new ChannelSubscriptionListener().subscribed(johnSession, channel, subscription(null));
    assertEquals("a subscription naming no user is not checked", 0, recorded(editor).size());

    editor.new ChannelSubscriptionListener().subscribed(johnSession, channel, subscription("john"));
    assertEquals(1, recorded(editor).size());
    assertEquals(0, editor.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    ((CometdDocumentsService.ContainerCommand) recorded(editor).get(0)).execute(null);
    assertEquals("a user who may edit the document registers the editor",
                 1, editor.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));

    editor.editorsContext.removeClient("john-session");
    // The session leaves between the subscription and the queued check
    CometdDocumentsService late = recordingService(exoBayeux, mock(DocumentService.class), mock(IdentityRegistry.class), true);
    late.new ChannelSubscriptionListener().subscribed(johnSession, channel, subscription("john"));
    assertEquals(1, recorded(late).size());
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(false);
    ((CometdDocumentsService.ContainerCommand) recorded(late).get(0)).execute(null);
    assertEquals("a session that disconnected registers no editor", 0, late.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    when(channel.getSubscribers()).thenReturn(Set.of());
    ((CometdDocumentsService.ContainerCommand) recorded(late).get(0)).execute(null);
    assertEquals("a session that unsubscribed registers no editor", 0, late.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  /**
   * An editor registered after the closing delay of the previous editor ended
   * is recorded for the document again, when no provider is recorded and the
   * provider is available to the user, and its opened event is published: the
   * delay reset the recorded provider, and published the last editor closed
   * event, between the accepted open and the registration.
   */
  @Test
  public void testAnEditorRegisteredAfterTheClosingDelayRecordsItsProviderAgain() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    when(channel.getSubscribers()).thenReturn(Set.of(johnSession));

    // The reset left no provider recorded
    DocumentService reset = documentServiceOf(null, true);
    CometdDocumentsService service = recordingService(exoBayeux, reset, mock(IdentityRegistry.class), true);
    ServerChannel documentChannel = bayeuxChannel(service);
    service.new ChannelSubscriptionListener().registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE);
    assertEquals(1, service.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    verify(reset).saveCurrentDocumentProvider(FILE_ID, WORKSPACE, PROVIDER);
    assertEquals("the provider is recorded again in place", 0, recorded(service).size());
    ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
    verify(documentChannel).publish(any(), published.capture(), any());
    assertEquals("the opened event is published with the provider recorded again",
                 "{\"type\": \"DOCUMENT_OPENED\", \"fileId\": \"" + FILE_ID + "\", \"provider\": \"" + PROVIDER + "\"}",
                 published.getValue());

    // The provider is still recorded: nothing is recorded again
    editors.clearCache();
    DocumentService recordedProvider = documentServiceOf(PROVIDER, true);
    CometdDocumentsService kept = recordingService(exoBayeux, recordedProvider, mock(IdentityRegistry.class), true);
    ServerChannel keptChannel = bayeuxChannel(kept);
    kept.new ChannelSubscriptionListener().registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE);
    assertEquals(1, kept.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    assertEquals("a recorded provider is left as it is", 0, recorded(kept).size());
    verify(recordedProvider, never()).saveCurrentDocumentProvider(any(), any(), any());
    verify(keptChannel, never()).publish(any(), any(Object.class), any());

    // The provider is not available to the user: nothing is recorded
    editors.clearCache();
    DocumentService unavailable = documentServiceOf(null, false);
    CometdDocumentsService refused = recordingService(exoBayeux, unavailable, mock(IdentityRegistry.class), true);
    ServerChannel refusedChannel = bayeuxChannel(refused);
    refused.new ChannelSubscriptionListener().registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE);
    assertEquals("a provider unavailable to the user is not recorded", 0, recorded(refused).size());
    verify(unavailable, never()).saveCurrentDocumentProvider(any(), any(), any());
    verify(refusedChannel, never()).publish(any(), any(Object.class), any());
  }

  /**
   * The recorded provider is read once the lock of the connection is
   * released: an unsubscription of a connection sharing that lock does not
   * wait for the read.
   */
  @Test
  public void testTheProviderIsRecordedAgainOutsideTheLockOfTheConnection() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    when(channel.getSubscribers()).thenReturn(Set.of(johnSession));
    DocumentService documentService = documentServiceOf(null, true);
    CometdDocumentsService service = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    bayeuxChannel(service);
    AtomicBoolean readUnderTheLock = new AtomicBoolean(true);
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenAnswer(invocation -> {
      readUnderTheLock.set(Thread.holdsLock(service.editorsLock("john-session")));
      return null;
    });

    service.new ChannelSubscriptionListener().registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE);

    verify(documentService).getCurrentDocumentProvider(FILE_ID, WORKSPACE);
    assertFalse("the recorded provider is read outside the lock of the connection", readUnderTheLock.get());
    assertEquals(1, service.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
  }

  /**
   * The reset and a registered editor's recording of its provider again
   * exclude each other: a registration whose editor is counted after the
   * reset counted none, and whose read comes before the reset's write, waits
   * for that write, then records its provider, so the document is not left
   * with an open editor and no recorded provider. The events are published in
   * the same order.
   */
  @Test
  public void testTheResetAndTheRecordingAgainOfAProviderExcludeEachOther() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    when(channel.getSubscribers()).thenReturn(Set.of(johnSession));
    DocumentService documentService = documentServiceOf(PROVIDER, true);
    // The provider the document records, as its node holds it
    AtomicReference<String> stored = new AtomicReference<>(PROVIDER);
    doAnswer(invocation -> {
      stored.set(invocation.getArgument(2));
      return null;
    }).when(documentService).saveCurrentDocumentProvider(eq(FILE_ID), eq(WORKSPACE), any());
    // The reset's read waits until the registration has counted its editor
    CountDownLatch inReset = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicBoolean resetRead = new AtomicBoolean(true);
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenAnswer(invocation -> {
      if (resetRead.getAndSet(false)) {
        inReset.countDown();
        release.await(10, TimeUnit.SECONDS);
      }
      return stored.get();
    });
    CometdDocumentsService service = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    ServerChannel documentChannel = bayeuxChannel(service);

    Thread reset = new Thread(() -> service.resetCurrentProviderIfClosed(FILE_ID, WORKSPACE, PROVIDER));
    reset.start();
    assertTrue(inReset.await(10, TimeUnit.SECONDS));
    Thread registration = new Thread(() -> service.new ChannelSubscriptionListener()
                                                  .registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE));
    registration.start();
    registration.join(500);
    assertTrue("the registration's read waits for the reset's write", registration.isAlive());
    assertEquals("the editor is counted before", 1, service.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    release.countDown();
    reset.join(10000);
    registration.join(10000);

    assertEquals("the open editor's provider is recorded", PROVIDER, stored.get());
    InOrder order = inOrder(documentService, documentChannel);
    order.verify(documentService).saveCurrentDocumentProvider(FILE_ID, WORKSPACE, null);
    order.verify(documentChannel).publish(any(), eq(lastEditorClosedEvent()), any());
    order.verify(documentService).saveCurrentDocumentProvider(FILE_ID, WORKSPACE, PROVIDER);
    order.verify(documentChannel).publish(any(), eq(documentOpenedEvent()), any());
    assertEquals("the writes are made in place", 0, recorded(service).size());
  }

  private static String documentOpenedEvent() {
    return "{\"type\": \"" + CometdDocumentsService.DOCUMENT_OPENED_EVENT + "\", \"fileId\": \"" + FILE_ID + "\", \"provider\": \""
        + PROVIDER + "\"}";
  }

  private static String lastEditorClosedEvent() {
    return "{\"type\": \"" + CometdDocumentsService.LAST_EDITOR_CLOSED_EVENT + "\", \"fileId\": \"" + FILE_ID
        + "\", \"provider\": \"" + PROVIDER + "\"}";
  }

  private static DocumentService documentServiceOf(String currentProvider, boolean availableToUser) throws Exception {
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.isAvailableForUser(any())).thenReturn(availableToUser);
    when(editorProvider.getProviderName()).thenReturn(PROVIDER);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    when(documentService.getCurrentDocumentProvider(FILE_ID, WORKSPACE)).thenReturn(currentProvider);
    return documentService;
  }

  /**
   * The registration and the unsubscription of one connection exclude each
   * other: whichever comes second sees the other's result.
   */
  @Test
  public void testTheRegistrationAndTheUnsubscriptionOfOneConnectionExcludeEachOther() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.getEditingFinishedDelay()).thenReturn(600000L);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);

    // A registration holding the lock delays the unsubscription of its own session
    CometdDocumentsService first = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    CountDownLatch inCheck = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(channel.getSubscribers()).thenAnswer(invocation -> {
      inCheck.countDown();
      release.await(10, TimeUnit.SECONDS);
      return Set.of(johnSession);
    });
    Thread registration = new Thread(() -> first.new ChannelSubscriptionListener()
                                                .registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE));
    registration.start();
    assertTrue(inCheck.await(10, TimeUnit.SECONDS));
    Thread unsubscription = new Thread(() -> first.new ChannelSubscriptionListener().unsubscribed(johnSession, channel, null));
    unsubscription.start();
    unsubscription.join(500);
    assertTrue("the unsubscription waits for the registration", unsubscription.isAlive());
    release.countDown();
    registration.join(10000);
    unsubscription.join(10000);
    assertEquals("the unsubscription removes the editor registered first", 0, first.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    assertEquals("the reset is scheduled as no editor is left", 1, closedEditorTimers(first).size());
    closedEditorTimers(first).values().forEach(Timer::cancel);

    // An unsubscription holding the lock delays the registration of its own session
    editors.clearCache();
    clearInvocations(channel);
    when(channel.getSubscribers()).thenReturn(Set.of(johnSession));
    CometdDocumentsService second = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    CountDownLatch inUnsubscription = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    when(documentService.getEditorProvider(PROVIDER)).thenAnswer(invocation -> {
      inUnsubscription.countDown();
      resume.await(10, TimeUnit.SECONDS);
      return editorProvider;
    });
    second.editorsContext.addClient("john-session", FILE_ID, PROVIDER, WORKSPACE);
    Thread leaving = new Thread(() -> second.new ChannelSubscriptionListener().unsubscribed(johnSession, channel, null));
    leaving.start();
    assertTrue(inUnsubscription.await(10, TimeUnit.SECONDS));
    Thread joining = new Thread(() -> second.new ChannelSubscriptionListener()
                                            .registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE));
    joining.start();
    joining.join(500);
    assertTrue("the registration waits for the unsubscription", joining.isAlive());
    assertEquals(0, second.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    resume.countDown();
    leaving.join(10000);
    joining.join(10000);
    assertEquals("the registration comes after the unsubscription", 1, second.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    assertEquals("the registration cancels the reset the unsubscription scheduled", 0, closedEditorTimers(second).size());
    closedEditorTimers(second).values().forEach(Timer::cancel);

    // The session left before the registration's check: no editor, and the reset stays scheduled
    editors.clearCache();
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    when(channel.getSubscribers()).thenReturn(Set.of());
    CometdDocumentsService third = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    third.editorsContext.addClient("john-session", FILE_ID, PROVIDER, WORKSPACE);
    third.new ChannelSubscriptionListener().unsubscribed(johnSession, channel, null);
    assertEquals("the reset is scheduled when no editor is left", 1, closedEditorTimers(third).size());
    third.new ChannelSubscriptionListener().registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE);
    assertEquals(0, third.editorsContext.getOpenedEditorsCount(FILE_ID, PROVIDER));
    assertEquals("a session that left leaves the reset scheduled", 1, closedEditorTimers(third).size());
    closedEditorTimers(third).values().forEach(Timer::cancel);
  }

  /**
   * Two connections of one document: the timer one saved is cancelled when the
   * other saves its own, and no reset is scheduled while an editor is open.
   */
  @Test
  public void testAReplacedClosedEditorTimerIsCancelled() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.getEditingFinishedDelay()).thenReturn(600000L);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    CometdDocumentsService service = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    ServerSession marySession = mock(ServerSession.class);
    when(marySession.getId()).thenReturn("mary-session");

    service.editorsContext.addClient("john-session", FILE_ID, PROVIDER, WORKSPACE);
    service.editorsContext.addClient("mary-session", FILE_ID, PROVIDER, WORKSPACE);
    service.new ChannelSubscriptionListener().unsubscribed(marySession, channel, null);
    assertEquals("no reset is scheduled while an editor is open", 0, closedEditorTimers(service).size());

    // The timer another connection saved is still there when this one closes
    Timer first = mock(Timer.class);
    service.editorsContext.saveClosedEditorTimer(FILE_ID, WORKSPACE, PROVIDER, first);
    service.new ChannelSubscriptionListener().unsubscribed(johnSession, channel, null);
    verify(first).cancel();
    assertEquals(1, closedEditorTimers(service).size());
    Timer last = closedEditorTimers(service).values().iterator().next();
    assertNotSame("the timer saved last is the live one", first, last);
    last.cancel();
  }

  /**
   * The lock is per connection: a registration of one connection does not
   * delay the unsubscription of another, whose lock differs.
   */
  @Test
  public void testTheUnsubscriptionOfAnotherConnectionDoesNotWaitForARegistration() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed("john", "john-session")).thenReturn(true);
    ServerSession johnSession = mock(ServerSession.class);
    when(johnSession.getId()).thenReturn("john-session");
    ServerSession marySession = mock(ServerSession.class);
    when(marySession.getId()).thenReturn("mary-session");
    ServerChannel channel = mock(ServerChannel.class);
    when(channel.getId()).thenReturn(CometdDocumentsService.CHANNEL_NAME + FILE_ID);
    DocumentService documentService = mock(DocumentService.class);
    DocumentEditorProvider editorProvider = mock(DocumentEditorProvider.class);
    when(editorProvider.getEditingFinishedDelay()).thenReturn(600000L);
    when(documentService.getEditorProvider(PROVIDER)).thenReturn(editorProvider);
    CometdDocumentsService service = recordingService(exoBayeux, documentService, mock(IdentityRegistry.class), true);
    assertNotSame(service.editorsLock("john-session"), service.editorsLock("mary-session"));
    CountDownLatch inCheck = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(channel.getSubscribers()).thenAnswer(invocation -> {
      inCheck.countDown();
      release.await(10, TimeUnit.SECONDS);
      return Set.of(johnSession);
    });
    service.editorsContext.addClient("mary-session", FILE_ID, PROVIDER, WORKSPACE);
    Thread registration = new Thread(() -> service.new ChannelSubscriptionListener()
                                                  .registerEditor(johnSession, channel, "john", FILE_ID, PROVIDER, WORKSPACE));
    registration.start();
    assertTrue(inCheck.await(10, TimeUnit.SECONDS));
    Thread unsubscription = new Thread(() -> service.new ChannelSubscriptionListener().unsubscribed(marySession, channel, null));
    unsubscription.start();
    unsubscription.join(5000);
    assertFalse("the unsubscription of another connection does not wait", unsubscription.isAlive());
    release.countDown();
    registration.join(10000);
    closedEditorTimers(service).values().forEach(Timer::cancel);
  }

  @SuppressWarnings("unchecked")
  private static Map<Object, Timer> closedEditorTimers(CometdDocumentsService service) throws Exception {
    Field field = EditorsContext.class.getDeclaredField("closedEditorTimers");
    field.setAccessible(true);
    return (Map<Object, Timer>) field.get(service.editorsContext);
  }

  private static ServerMessage subscription(String userId) {
    ServerMessage message = mock(ServerMessage.class);
    when(message.get("exoId")).thenReturn(userId);
    when(message.get("provider")).thenReturn(PROVIDER);
    when(message.get("workspace")).thenReturn(WORKSPACE);
    return message;
  }

  /**
   * Kills the mutant that removes {@code @Service("documents")} from
   * {@link CometdDocumentsService.CometdService}: the annotation processor
   * registers the listeners of a class annotated with {@code @Service} only.
   */
  @Test
  public void testTheDocumentsServiceIsDeclaredToCometD() {
    Service service = CometdDocumentsService.CometdService.class.getAnnotation(Service.class);
    assertNotNull(service);
    assertEquals("documents", service.value());
  }

  private static ServerMessage.Mutable openedEvent(String userId) {
    ServerMessage.Mutable message = mock(ServerMessage.Mutable.class);
    Map<String, Object> data = new HashMap<>();
    data.put("type", CometdDocumentsService.DOCUMENT_OPENED_EVENT);
    data.put("userId", userId);
    data.put("workspace", WORKSPACE);
    data.put("provider", PROVIDER);
    when(message.getData()).thenReturn(data);
    when(message.getDataAsMap()).thenReturn(data);
    return message;
  }

  private static List<Runnable> recorded(CometdDocumentsService service) {
    return ((RecordingExecutor) service.eventsHandlers).commands;
  }

  /**
   * A service whose commands are recorded instead of run, and whose edit check
   * answers as given.
   */
  private CometdDocumentsService recordingService(EXoContinuationBayeux exoBayeux,
                                                  DocumentService documentService,
                                                  IdentityRegistry identityRegistry,
                                                  boolean canEdit) {
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.<String, String> getCacheInstance(CometdDocumentsService.EDITORS_CACHE_NAME)).thenReturn(editors);
    return new CometdDocumentsService(exoBayeux, documentService, identityRegistry, null, cacheService) {
      @Override
      protected ExecutorService createThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
        return new RecordingExecutor();
      }

      @Override
      protected boolean canEditDocument(String userId, String fileId, String workspace) {
        assertEquals("john", userId);
        assertEquals(FILE_ID, fileId);
        assertEquals(WORKSPACE, workspace);
        return canEdit;
      }
    };
  }

  private static class RecordingExecutor extends AbstractExecutorService {
    final List<Runnable> commands = new ArrayList<>();

    @Override
    public void execute(Runnable command) {
      commands.add(command);
    }

    @Override
    public Future<?> submit(Runnable command) {
      // The command itself is recorded, not a wrapper of it
      commands.add(command);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void shutdown() {
      // Nothing runs
    }

    @Override
    public List<Runnable> shutdownNow() {
      return commands;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }
  }

  private static List<Thread> timerThreads() {
    return Thread.getAllStackTraces()
                 .keySet()
                 .stream()
                 .filter(thread -> thread.isAlive()
                     && CometdDocumentsService.CLOSED_EDITOR_TIMER_NAME.equals(thread.getName()))
                 .toList();
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
