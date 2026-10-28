/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.listener.analytics;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import org.exoplatform.services.cms.documents.impl.DocumentServiceImpl;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;

/**
 * Each event the documents analytics listener is registered on is recorded
 * under its own operation, and the node is read through a system session:
 * the listener runs after the request that broadcast the event, whose
 * session may already be closed.
 */
public class AnalyticsDocumentsListenerTest {

  @Test
  public void eachEventIsRecordedUnderItsOwnOperation() {
    assertEquals(AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf(DocumentServiceImpl.CREATE_DOC_NEW_APP));
    assertEquals(AnalyticsDocumentsListener.UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf("exo.upload.doc.newApp"));
    assertEquals(AnalyticsDocumentsListener.UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf("exo.upload.doc.oldApp"));
    assertEquals("documentCreated", AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME);
  }

  @Test
  public void theOwnerIsReadThroughASystemSessionNotTheBroadcastersOne() throws Exception {
    String path = "/Users/j___/jo___/joh___/john/Private/Documents/report.docx";
    // The broadcast node is a plain Node: reading its ACL would fail, as it
    // does once the caller's session is logged out.
    Node broadcastNode = mock(Node.class, RETURNS_DEEP_STUBS);
    when(broadcastNode.getPath()).thenReturn(path);
    when(broadcastNode.getName()).thenReturn("report.docx");
    when(broadcastNode.getSession().getWorkspace().getName()).thenReturn("collaboration");

    SessionProvider systemProvider = mock(SessionProvider.class);
    Session systemSession = mock(Session.class);
    NodeImpl systemNode = mock(NodeImpl.class);
    AccessControlList acl = mock(AccessControlList.class);
    when(systemProvider.getSession(any(), any())).thenReturn(systemSession);
    when(systemSession.itemExists(path)).thenReturn(true);
    when(systemSession.getItem(path)).thenReturn(systemNode);
    when(systemNode.getACL()).thenReturn(acl);
    when(acl.getOwner()).thenReturn("john");

    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getOrCreateIdentity(anyString(), anyString())).thenReturn(new Identity("3"));
    AnalyticsDocumentsListener listener = new AnalyticsDocumentsListener() {
      @Override
      public IdentityManager getIdentityManager() {
        return identityManager;
      }

      @Override
      SessionProvider createSystemProvider() {
        return systemProvider;
      }
    };
    ArgumentCaptor<StatisticData> stored = ArgumentCaptor.forClass(StatisticData.class);

    try (MockedStatic<SessionProviderService> sessionProviderService = mockStatic(SessionProviderService.class);
         MockedStatic<AnalyticsUtils> analytics = mockStatic(AnalyticsUtils.class)) {
      sessionProviderService.when(SessionProviderService::getRepository).thenReturn(mock(ManageableRepository.class));
      listener.onEvent(new Event<>(DocumentServiceImpl.CREATE_DOC_NEW_APP, "john", broadcastNode));
      analytics.verify(() -> AnalyticsUtils.addStatisticData(stored.capture()));
    }

    StatisticData data = stored.getValue();
    assertEquals(AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME, data.getOperation());
    assertEquals("john", data.getParameters().get("documentsOwner"));
    assertEquals(path, data.getParameters().get("documentsPath"));
    verify(systemProvider).close();
  }

}
