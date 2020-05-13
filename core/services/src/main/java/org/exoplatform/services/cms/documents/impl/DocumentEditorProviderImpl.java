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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentUpdateActivityHandler;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.exception.PermissionValidationException;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class DocumentEditorProviderImpl.
 */
public class DocumentEditorProviderImpl implements DocumentEditorProvider {

  /** The Constant DOCUMENTS_SCOPE_NAME. */
  private static final String   DOCUMENTS_SCOPE_NAME       = "documents".intern();

  /** The Constant EDITOR_ACTIVE_PATTERN. */
  private static final String   EDITOR_ACTIVE_PATTERN      = "documents.editors.%s.active".intern();

  /** The Constant EDITOR_PERMISSIONS_PATTERN. */
  private static final String   EDITOR_PERMISSIONS_PATTERN = "documents.editors.%s.permissions".intern();

  /** The active. */
  protected boolean             active;

  /** The permissions. */
  protected List<String>        permissions;

  /** The editor ops. */
  protected DocumentEditor      editor;

  /** The setting service. */
  protected SettingService      settingService;

  /** The identity manager. */
  protected IdentityManager     identityManager;

  /** The organization service. */
  protected OrganizationService organizationService;


  /**
   * Instantiates a new document editor provider impl.
   *
   * @param editor the editor
   * @param settingService the setting service
   * @param identityManager the identity manager
   * @param organizationService the organization service
   */
  protected DocumentEditorProviderImpl(DocumentEditor editor,
                                       SettingService settingService,
                                       IdentityManager identityManager,
                                       OrganizationService organizationService) {
    this.editor = editor;
    this.settingService = settingService;
    this.identityManager = identityManager;
    this.organizationService = organizationService;
    Boolean storedActive = getStoredActive();
    List<String> storedPermissions = getStoredPermissions();
    if (storedActive != null && storedPermissions != null) {
      this.active = storedActive;
      this.permissions = storedPermissions;
    } else {
      this.active = editor.isActive();
      this.permissions = editor.getPermissions();
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
   * @throws PermissionValidationException the permission validation exception
   */
  @Override
  public void updatePermissions(List<String> permissions) throws PermissionValidationException {
    List<String> updatedPermissions = new ArrayList<>();
    for (String permission : permissions) {
      if (permission != null && permission.startsWith("/")) {
        permission = "*:" + permission;
      }
      validatePermission(permission);
      updatedPermissions.add(permission);
    }
    this.permissions = updatedPermissions;
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
  public void beforeDocumentCreate(NewDocumentTemplate template, String parentPath, String title) throws Exception {
    editor.beforeDocumentCreate(template, parentPath, title);
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
    editor.onDocumentCreated(workspace, path);
  }

  /**
   * Inits the activity.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @throws Exception the exception
   */
  @Override
  public void initActivity(String uuid, String workspace, String activityId) throws Exception {
    editor.initActivity(uuid, workspace, activityId);
  }

  /**
   * Inits the preview.
   *
   * @param <T> the generic type
   * @param fileId the uuid
   * @param workspace the workspace
   * @param requestURI the requestURI
   * @param locale the locale
   * @return settings object
   */
  @Override
  public <T> T initPreview(String fileId, String workspace, URI requestURI, Locale locale) {
    return editor.initPreview(fileId, workspace, requestURI, locale);
  }
  
  
  /**
   * Inits the explorer.
   *
   * @param <T> the generic type
   * @param fileId the file id
   * @param workspace the workspace
   * @param context the context
   * @return the settings object
   */
  @Override
  public <T> T initExplorer(String fileId, String workspace, WebuiRequestContext context) {
    return editor.initExplorer(fileId, workspace, context);
  }


  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  @Override
  public String getProviderName() {
    return editor.getProviderName();
  }
  
  
  /**
   * Checks if is document supported.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @return true, if is document supported
   */
  @Override
  public boolean isDocumentSupported(String fileId, String workspace) {
    return editor.isDocumentSupported(fileId, workspace);
  }
  
  /**
   * Gets the document update handler.
   *
   * @return the document update handler
   */
  @Override
  public DocumentUpdateActivityHandler getDocumentUpdateHandler() {
    return editor.getDocumentUpdateHandler();
  }

  /**
   * Gets the stored active.
   *
   * @return the stored active
   */
  protected Boolean getStoredActive() {
    SettingValue<?> activeParam = settingService.get(Context.GLOBAL,
                                                     Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                                                     String.format(EDITOR_ACTIVE_PATTERN, editor.getProviderName()));
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
                                                          String.format(EDITOR_PERMISSIONS_PATTERN, editor.getProviderName()));
    String permissionsStr = permissionsParam != null ? permissionsParam.getValue().toString() : null;
    return permissionsStr != null ? Arrays.asList(permissionsStr.split("\\s*,\\s*")) : null;
  }

  /**
   * Store active.
   */
  protected void storeActive() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_ACTIVE_PATTERN, editor.getProviderName()),
                       SettingValue.create(active));
  }

  /**
   * Store permissions.
   */
  protected void storePermissions() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_PERMISSIONS_PATTERN, editor.getProviderName()),
                       SettingValue.create(String.join(",", permissions)));
  }

  /**
   * Validate permission.
   *
   * @param permission the permission
   * @throws PermissionValidationException the permission validation exception
   */
  protected void validatePermission(String permission) throws PermissionValidationException {
    if (permission == null) {
      throw new PermissionValidationException("The permission cannot be null");
    }
    String[] temp = permission.split(":");
    if (temp.length < 2) {
      // user permissions
      String userId = temp[0];
      if (!userId.equals("*") && identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId) == null) {
        throw new PermissionValidationException("User " + userId + " doesn't exist.");
      }
    } else {
      // group permisisons
      String groupId = temp[1];
      Group group = null;
      try {
        group = organizationService.getGroupHandler().findGroupById(groupId);
      } catch (Exception e) {
        throw new PermissionValidationException("Cannot validate permission for group: " + group + ". " + e.getMessage());
      }
      if (group == null) {
        throw new PermissionValidationException("Group " + groupId + " doesn't exist.");
      }
    }
  }

  /**
   * Checks if is available for user.
   *
   * @param identity the identity
   * @return true, if is available for user
   */
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

  /**
   * Gets the editor class.
   *
   * @return the editor class
   */
  @Override
  public Class<? extends DocumentEditor> getEditorClass() {
    return editor.getClass();
  }

}
