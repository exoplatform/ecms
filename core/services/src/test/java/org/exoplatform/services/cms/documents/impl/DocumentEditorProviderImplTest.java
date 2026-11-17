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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
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

  private MockedStatic<ExoContainerContext> containerContext;

  @After
  public void restoreContainer() {
    if (containerContext != null) {
      containerContext.close();
      containerContext = null;
    }
  }

  private void withUserACL() {
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(UserACL.class)).thenReturn(mock(UserACL.class));
    containerContext = Mockito.mockStatic(ExoContainerContext.class);
    containerContext.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
  }

  private static org.exoplatform.services.security.Identity user(String userId) {
    return new org.exoplatform.services.security.Identity(userId);
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

  @Test
  public void permissionsChangedOnOneNodeDecideAvailabilityOnTheOther() throws Exception {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    assertTrue(nodeB.isAvailableForUser(user("mary")));

    nodeA.updatePermissions(Arrays.asList("john"));

    assertTrue(nodeB.isAvailableForUser(user("john")));
    assertFalse(nodeB.isAvailableForUser(user("mary")));
  }

  @Test
  public void deactivationOnOneNodeMakesTheEditorUnavailableOnTheOther() {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    assertTrue(nodeB.isAvailableForUser(user("john")));

    nodeA.updateActive(false);

    assertFalse(nodeB.isAvailableForUser(user("john")));
  }

  @Test
  public void editorDefaultsAreUsedWhenTheStoreKeepsNothing() {
    withUserACL();
    SettingService settingService = mock(SettingService.class);
    DocumentEditorProviderImpl provider = provider(settingService);

    assertTrue(provider.isActive());
    assertEquals(Arrays.asList("*"), provider.getPermissions());
    assertTrue(provider.isAvailableForUser(user("john")));
  }

  @Test
  public void lastReadRestrictionIsKeptWhenTheStoreFailsAfterwards() throws Exception {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    nodeA.updatePermissions(Arrays.asList("john"));
    assertTrue(nodeB.isAvailableForUser(user("john")));
    assertFalse(nodeB.isAvailableForUser(user("mary")));

    store.clear(); // the store now answers null, as it does on a failed read

    assertTrue(nodeB.isActive());
    assertEquals(Arrays.asList("john"), nodeB.getPermissions());
    assertTrue(nodeB.isAvailableForUser(user("john")));
    assertFalse(nodeB.isAvailableForUser(user("mary")));
  }

  @Test
  public void lastReadDeactivationIsKeptWhenTheStoreFailsAfterwards() {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    nodeA.updateActive(false);
    assertFalse(nodeB.isActive());

    store.clear();

    assertFalse(nodeB.isActive());
    assertFalse(nodeB.isAvailableForUser(user("john")));
  }

  @Test
  public void providerKeepsWhatItReadWhenItStartedWhenTheStoreFailsAfterwards() throws Exception {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeA = provider(settingService);
    nodeA.updatePermissions(Arrays.asList("john"));
    nodeA.updateActive(false);
    DocumentEditorProviderImpl nodeB = provider(settingService);
    store.clear();

    assertFalse(nodeB.isActive());
    assertEquals(Arrays.asList("john"), nodeB.getPermissions());
    assertFalse(nodeB.isAvailableForUser(user("mary")));
  }

  @Test
  public void providerThatStartsDuringAStoreFailureStoresNothingAndUsesTheDefaults() throws Exception {
    withUserACL();
    SettingService settingService = sharedSettingService();
    provider(settingService).updatePermissions(Arrays.asList("john"));
    SettingValue<?> restriction = store.get("documents.editors.onlyoffice.permissions");
    // A store whose reads fail, as null answers, while its writes would go through
    SettingService failingReads = mock(SettingService.class);

    DocumentEditorProviderImpl nodeB = provider(failingReads);

    verify(failingReads, never()).set(any(Context.class), any(Scope.class), anyString(), any(SettingValue.class));
    assertEquals(restriction, store.get("documents.editors.onlyoffice.permissions"));
    assertTrue(nodeB.isActive());
    assertEquals(Arrays.asList("*"), nodeB.getPermissions());
  }

  @Test
  public void ownUpdateIsKeptWhenTheStoreFailsAfterwards() throws Exception {
    withUserACL();
    SettingService settingService = sharedSettingService();
    DocumentEditorProviderImpl nodeB = provider(settingService);
    assertEquals(Arrays.asList("*"), nodeB.getPermissions());
    assertTrue(nodeB.isActive());

    nodeB.updatePermissions(Arrays.asList("john"));
    nodeB.updateActive(false);
    store.clear();

    assertEquals(Arrays.asList("john"), nodeB.getPermissions());
    assertFalse(nodeB.isActive());
    assertFalse(nodeB.isAvailableForUser(user("john")));
  }
}
