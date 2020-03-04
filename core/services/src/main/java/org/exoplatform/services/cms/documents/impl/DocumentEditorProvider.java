package org.exoplatform.services.cms.documents.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin.ProviderConfig;
import org.exoplatform.services.cms.documents.model.ResourceSupport;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class EditorProvider.
 */
public class DocumentEditorProvider {

  /** The Constant DOCUMENTS_SCOPE_NAME. */
  private static final String   DOCUMENTS_SCOPE_NAME       = "documents".intern();

  /** The Constant EDITOR_ACTIVE_PATTERN. */
  private static final String   EDITOR_ACTIVE_PATTERN      = "documents.editors.%s.active".intern();

  /** The Constant EDITOR_PERMISSIONS_PATTERN. */
  private static final String   EDITOR_PERMISSIONS_PATTERN = "documents.editors.%s.permissions".intern();

  /** The Constant DEFAULT_ACTIVE. */
  private static final boolean  DEFAULT_ACTIVE             = true;

  /** The Constant DEFAULT_PERMISSIONS. */
  private static final String[] DEFAULT_PERMISSIONS        = { "*" };

  /** The provider. */
  protected String              provider;

  /** The active. */
  protected Boolean             active;

  /** The permissions. */
  protected List<String>        permissions;

  /** The setting service. */
  protected SettingService      settingService;

  /**
   * Instantiates a new editor provider.
   *
   * @param provider the provider
   * @param config the config
   */
  protected DocumentEditorProvider(String provider, ProviderConfig config) {
    this.settingService = WCMCoreUtils.getService(SettingService.class);
    this.provider = provider;
    Boolean storedActive = getStoredActive();
    List<String> storedPermissions = getStoredPermissions();
    if (storedActive != null && storedPermissions != null) {
      this.active = storedActive;
      this.permissions = storedPermissions;
    } else {
      if (config != null) {
        this.active = config.getActive() != null ? config.getActive() : DEFAULT_ACTIVE;
        this.permissions = config.getPermissions() != null ? config.getPermissions() : Arrays.asList(DEFAULT_PERMISSIONS);
      } else {
        this.active = DEFAULT_ACTIVE;
        this.permissions = Arrays.asList(DEFAULT_PERMISSIONS);
      }
      storeActive();
      storePermissions();
    }
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Gets the active.
   *
   * @return the active
   */
  public Boolean getActive() {
    return active;
  }

  /**
   * Sets the active.
   *
   * @param active the new active
   */
  public void setActive(Boolean active) {
    this.active = active;
    storeActive();
  }

  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  public List<String> getPermissions() {
    return permissions;
  }

  /**
   * Sets the permissions.
   *
   * @param permissions the new permissions
   */
  public void setPermissions(List<String> permissions) {
    this.permissions = permissions.stream().map(permission -> {
      if (permission.startsWith("/")) {
        permission = "*:" + permission;
      }
      return permission;
    }).collect(Collectors.toList());
    storePermissions();

  }

  /**
   * Gets the stored active.
   *
   * @return the stored active
   */
  protected Boolean getStoredActive() {
    SettingValue<?> activeParam = settingService.get(Context.GLOBAL,
                                                     Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                                                     String.format(EDITOR_ACTIVE_PATTERN, provider));
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
                                                          String.format(EDITOR_PERMISSIONS_PATTERN, provider));
    String permissionsStr = permissionsParam != null ? permissionsParam.getValue().toString() : null;
    return permissionsStr != null ? Arrays.asList(permissionsStr.split("\\s*,\\s*")) : null;
  }

  /**
   * Store active.
   */
  protected void storeActive() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_ACTIVE_PATTERN, provider),
                       SettingValue.create(active));
  }

  /**
   * Store permissions.
   */
  protected void storePermissions() {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(DOCUMENTS_SCOPE_NAME),
                       String.format(EDITOR_PERMISSIONS_PATTERN, provider),
                       SettingValue.create(String.join(",", permissions)));
  }

  /**
   * Covert to DTO.
   *
   * @return the document editor provider DTO
   */
  public DocumentEditorProviderDTO covertToDTO() {
    return new DocumentEditorProviderDTO(provider, active, permissions);
  }

  /**
   * The Class DocumentEditorProviderDTO.
   */
  public static class DocumentEditorProviderDTO extends ResourceSupport {

    /** The provider. */
    private final String       provider;

    /** The active. */
    private final Boolean      active;

    /** The permissions. */
    private final List<String> permissions;

    /**
     * Instantiates a new document editor provider DTO.
     *
     * @param provider the provider
     * @param active the active
     * @param permissions the permissions
     */
    private DocumentEditorProviderDTO(String provider, Boolean active, List<String> permissions) {
      this.provider = provider;
      this.active = active;
      this.permissions = permissions;
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    public String getProvider() {
      return provider;
    }

    /**
     * Gets the active.
     *
     * @return the active
     */
    public Boolean getActive() {
      return active;
    }

    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public List<String> getPermissions() {
      return permissions.stream().map(permission -> {
        if (permission.contains("/")) {
          permission = permission.substring(permission.indexOf("/"));
        }
        return permission;
      }).collect(Collectors.toList());
    }
  }
}
