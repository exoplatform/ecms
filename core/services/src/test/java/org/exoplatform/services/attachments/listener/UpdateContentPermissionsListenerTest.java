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

import groovy.util.logging.Commons;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;
import org.junit.After;
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
public class UpdateContentPermissionsListenerTest {

  @Mock
  private SessionProviderService          sessionProviderService;

  @Mock
  private RepositoryService               repositoryService;

  private UpdateContentPermissionsListener shareAttachmentsToSpaceListener;

  private MockedStatic<Utils>             UTILS = mockStatic(Utils.class);

  private MockedStatic<CommonsUtils>      COMMONS_UTILS = mockStatic(CommonsUtils.class);

  private MockedStatic<PortalContainer>   PORTAL_CONTAINER = mockStatic(PortalContainer.class);

  @Before
  public void setUp() throws Exception {
    this.shareAttachmentsToSpaceListener = new UpdateContentPermissionsListener(repositoryService, sessionProviderService);
  }

  @After
  public void tearDown() throws Exception {
    UTILS.close();
    COMMONS_UTILS.close();
    PORTAL_CONTAINER.close();
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
                                        "audience",
                                        "all",
                                        "space",
                                        space,
                                        "content",
                                        "test <p> <img src=\"/portal/rest/images/repository/collaboration/123456\">" + "<img src=\"https://exoplatform.com/portal/rest/jcr/repository/collaboration/Groups/spaces/test/testimage\">");
    Session session = mock(Session.class);
    ExtendedNode attachmentNode = mock(ExtendedNode.class);
    when(attachmentNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(false);
    when(attachmentNode.getPath()).thenReturn("path");
    when(session.getNodeByUUID(anyString())).thenReturn(attachmentNode);

    ExtendedNode existingUploadedNewsImageNode = mock(ExtendedNode.class);
    String currentDomainName = "https://exoplatform.com";
    String currentPortalContainerName = "portal";
    String restContextName = "rest";
    COMMONS_UTILS.when(() -> CommonsUtils.getRestContextName()).thenReturn(restContextName);
    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn(currentPortalContainerName);
    COMMONS_UTILS.when(() -> CommonsUtils.getCurrentDomain()).thenReturn(currentDomainName);

    when(session.getItem(nullable(String.class))).thenReturn(existingUploadedNewsImageNode);
    String nodePath = "Groups/spaces/test/testimage";
    when(existingUploadedNewsImageNode.getPath()).thenReturn(nodePath);
    when(existingUploadedNewsImageNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(true);

    UTILS.when(() -> Utils.getSystemSession(sessionProviderService, repositoryService)).thenReturn(session);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.share.attachments", null, params));
    verify(attachmentNode, times(3)).setPermission(anyString(), any());
    verify(attachmentNode, times(3)).save();
    verify(existingUploadedNewsImageNode, times(1)).setPermission(anyString(), any());
    verify(existingUploadedNewsImageNode, times(1)).save();

  }
}
