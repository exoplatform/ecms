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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.documents.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

public class DocumentEditorProviderImplTest {

  private final Map<String, SettingValue<?>> store = new HashMap<>();

  private SettingService sharedSettingService() {
    SettingService settingService = mock(SettingService.class);
    when(settingService.get(any(Context.class), any(Scope.class), anyString())).thenAnswer(i -> store.get(i.getArgument(2)));
    org.mockito.Mockito.doAnswer(i -> {
      store.put(i.getArgument(2), i.getArgument(3));
      return null;
    }).when(settingService).set(any(Context.class), any(Scope.class), anyString(), any(SettingValue.class));
    return settingService;
  }

  private DocumentEditor editor() {
    DocumentEditor editor = mock(DocumentEditor.class);
    when(editor.getProviderName()).thenReturn("onlyoffice");
    when(editor.isActive()).thenReturn(true);
    when(editor.getPermissions()).thenReturn(Arrays.asList("*"));
    return editor;
  }

  private DocumentEditorProviderImpl provider(SettingService settingService) {
    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getOrCreateIdentity(anyString(), anyString())).thenReturn(mock(Identity.class));
    OrganizationService organizationService = mock(OrganizationService.class);
    GroupHandler groupHandler = mock(GroupHandler.class);
    try {
      when(organizationService.getGroupHandler()).thenReturn(groupHandler);
      when(groupHandler.findGroupById(anyString())).thenReturn(mock(Group.class));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return new DocumentEditorProviderImpl(editor(), settingService, identityManager, organizationService);
  }

  @Test
  public void defaultsComeFromTheEditorWhenNothingIsStored() {
    DocumentEditorProviderImpl nodeA = provider(sharedSettingService());

    assertTrue(nodeA.isActive());
    assertEquals(Arrays.asList("*"), nodeA.getPermissions());
  }

  @Test
  public void activeStateChangedOnOneNodeIsSeenByTheOther() {
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    assertTrue(nodeB.isActive());

    nodeA.updateActive(false);

    assertFalse(nodeB.isActive());
  }

  @Test
  public void permissionsChangedOnOneNodeAreSeenByTheOther() throws Exception {
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    assertEquals(Arrays.asList("*"), nodeB.getPermissions());

    nodeA.updatePermissions(Arrays.asList("john", "/platform/users"));

    assertEquals(Arrays.asList("john", "*:/platform/users"), nodeB.getPermissions());
  }
}
