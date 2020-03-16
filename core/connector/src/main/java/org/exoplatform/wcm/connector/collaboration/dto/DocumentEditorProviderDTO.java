package org.exoplatform.wcm.connector.collaboration.dto;

import java.util.List;

/**
 * The Class DocumentEditorProviderDTO.
 */
public class DocumentEditorProviderDTO extends ResourceSupport {

  /** The provider. */
  private final String           provider;

  /** The active. */
  private final boolean          active;

  /** The permissions. */
  private final List<Permission> permissions;

  /**
   * Instantiates a new document editor provider DTO.
   *
   * @param provider the provider
   * @param active the active
   * @param permissions the permissions
   */
  public DocumentEditorProviderDTO(String provider, boolean active, List<Permission> permissions) {
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
  public boolean getActive() {
    return active;
  }

  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  public List<Permission> getPermissions() {
    return permissions;
  }

  /**
   * The Class Permission.
   */
  public static class Permission {
    
    /** The name. */
    protected final String name;
    
    /** The display name. */
    protected final String displayName;
    
    /** The avatar url. */
    protected final String avatarUrl;
    
    /**
     * Instantiates a new permission.
     *
     * @param name the name
     * @param displayName the display name
     * @param avatarUrl the avatar url
     */
    public Permission(String name, String displayName, String avatarUrl) {
      this.name = name;
      this.displayName = displayName;
      this.avatarUrl = avatarUrl;
    }
    
    /**
     * Instantiates a new permission.
     *
     * @param name the name
     */
    public Permission(String name) {
      this.name = name;
      displayName = null;
      avatarUrl = null;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Gets the avatar url.
     *
     * @return the avatar url
     */
    public String getAvatarUrl() {
      return avatarUrl;
    }
  }

}
