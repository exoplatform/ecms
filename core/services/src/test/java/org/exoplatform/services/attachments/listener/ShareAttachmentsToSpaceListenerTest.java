/*
 * Copyright (C) 2024 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
*/
package org.exoplatform.services.attachments.listener;

import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ShareAttachmentsToSpaceListenerTest {

  @Mock
  private SessionProviderService          sessionProviderService;

  @Mock
  private RepositoryService               repositoryService;

  private ShareAttachmentsToSpaceListener shareAttachmentsToSpaceListener;

  private MockedStatic<Utils>             UTILS = mockStatic(Utils.class);

  @Before
  public void setUp() throws Exception {
    this.shareAttachmentsToSpaceListener = new ShareAttachmentsToSpaceListener(repositoryService, sessionProviderService);
  }

  @Test
  public void onEvent() throws Exception {
    Space space = new Space();
    space.setDisplayName("test space");
    space.setGroupId("/spaces/test");
    List<String> attachmentIds = new ArrayList<>();
    attachmentIds.add("123");
    attachmentIds.add("456");
    Map<String, Object> params = Map.of("attachmentsIds",
                                        attachmentIds,
                                        "content",
                                        "test <img src=\"/portal/rest/images/repository/collaboration/123456\"> test test " +
                                                " <img src=\"/portal/rest/jcr/repository/collaboration/123456\"> test");
    Session session = mock(Session.class);
    ExtendedNode attachmentNode = mock(ExtendedNode.class);
    when(attachmentNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(false);
    when(attachmentNode.getPath()).thenReturn("path");
    when(session.getNodeByUUID(anyString())).thenReturn(attachmentNode);

    UTILS.when(() -> Utils.getSystemSession(sessionProviderService, repositoryService)).thenReturn(session);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.share.attachments", params, space));
    verify(attachmentNode, times(4)).setPermission(anyString(), any());
    verify(attachmentNode, times(4)).save();

  }
}
