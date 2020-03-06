/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.documents.DocumentEditorOps;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class EditorProvider.
 */
public class DocumentEditorProviderImpl implements DocumentEditorProvider {

  /** The Constant DOCUMENTS_SCOPE_NAME. */
  private static final String DOCUMENTS_SCOPE_NAME       = "documents".intern();

  /** The Constant EDITOR_ACTIVE_PATTERN. */
  private static final String EDITOR_ACTIVE_PATTERN      = "documents.editors.%s.active".intern();

  /** The Constant EDITOR_PERMISSIONS_PATTERN. */
  private static final String EDITOR_PERMISSIONS_PATTERN = "documents.editors.%s.permissions".intern();

  /** The active. */
  protected boolean           active;

  /** The permissions. */
  protected List<String>      permissions;

  /** The editor ops. */
  protected DocumentEditorOps editorOps;

  /** The setting service. */
  protected SettingService    settingService;

  /**
   * Instantiates a new document editor provider impl.
   *
   * @param editorOps the editor ops
   */
  protected DocumentEditorProviderImpl(DocumentEditorOps editorOps) {
    this.editorOps = editorOps;
    this.settingService = WCMCoreUtils.getService(SettingService.class);
    Boolean storedActive = getStoredActive();
    List<String> storedPermissions = getStoredPermissions();
    if (storedActive != null && storedPermissions != null) {
      this.active = storedActive;
      this.permissions = storedPermissions;
    } else {
      this.active = editorOps.isActive();
      this.permissions = editorOps.getPermissions();
      storeActive();
      storePermissions();
    }
  }

  /**
   * Checks if is active.
   *
   * @return true, if is active
   */
  @Override
  public boolean isActive() {
    return active;
  }

  /**
   * Update active.
   *
   * @param active the active
   */
  @Override
  public void updateActive(boolean active) {
    this.active = active;
    storeActive();
  }

  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  @Override
  public List<String> getPermissions() {
    return Collections.unmodifiableList(permissions);
  }

  /**
   * Update permissions.
   *
   * @param permissions the permissions
   */
  @Override
  public void updatePermissions(List<String> permissions) {
    this.permissions = permissions.stream().map(permission -> {
      if (permission.startsWith("/")) {
        permission = "*:" + permission;
      }
      return permission;
    }).collect(Collectors.toList());
    storePermissions();

  }

  /**
   * Before document create.
   *
   * @param template the template
   * @param parentPath the parent path
   * @param title the title
   * @throws Exception the exception
   */
  @Override
  public void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title) throws Exception {
    editorOps.beforeDocumentCreate(template, parentPath, title);
  }

  /**
   * On document created.
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  @Override
  public void onDocumentCreated(String workspace, String path) throws Exception {
    editorOps.onDocumentCreated(workspace, path);
  }

  /**
   * Inits the activity.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @param context the context
   * @throws Exception the exception
   */
  @Override
  public void initActivity(String uuid, String workspace, String activityId, String context) throws Exception {
    editorOps.initActivity(uuid, workspace, activityId, context);
  }

  /**
   * Inits the preview.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @param context the context
   * @param index the index
   * @throws Exception the exception
   */
  @Override
  public void initPreview(String uuid, String workspace, String activityId, String context, int index) throws Exception {
    editorOps.initPreview(uuid, workspace, activityId, context, index);
  }

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  @Override
  public String getProviderName() {
    return editorOps.getProviderName();
  }

  /**
   * Gets the stored active.
   *
   * @return the stored active
   */
  protected Boolean getStoredActive() {
    SettingValue<?> activeParam = settingService.get(Context.GLOBAL,
                                                     Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                                                     String.format(EDITOR_ACTIVE_PATTERN, editorOps.getProviderName()));
    return activeParam != null ? Boolean.valueOf(activeParam.getValue().toString()) : null;
  }

  /**
   * Gets the stored permissions.
   *
   * @return the stored permissions
   */
  protected List<String> getStoredPermissions() {
    SettingValue<?> permissionsParam = settingService.get(Context.GLOBAL,
                                                          Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                                                          String.format(EDITOR_PERMISSIONS_PATTERN, editorOps.getProviderName()));
    String permissionsStr = permissionsParam != null ? permissionsParam.getValue().toString() : null;
    return permissionsStr != null ? Arrays.asList(permissionsStr.split("\\s*,\\s*")) : null;
  }

  /**
   * Store active.
   */
  protected void storeActive() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_ACTIVE_PATTERN, editorOps.getProviderName()),
                       SettingValue.create(active));
  }

  /**
   * Store permissions.
   */
  protected void storePermissions() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_PERMISSIONS_PATTERN, editorOps.getProviderName()),
                       SettingValue.create(String.join(",", permissions)));
  }

 

  @Override
  public boolean isAvailableForUser(Identity identity) {
    if (isActive()) {
      UserACL userACL = (UserACL) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
      for (String permission : permissions) {
        if (permission.equals("*") || permission.equals(identity.getUserId()) || userACL.hasPermission(identity, permission)) {
          return true;
        }
      }
    }
    return false;
  }
}
