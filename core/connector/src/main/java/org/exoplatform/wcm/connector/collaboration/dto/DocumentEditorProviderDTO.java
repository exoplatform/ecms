package org.exoplatform.wcm.connector.collaboration.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.services.cms.documents.DocumentEditorProvider;

/**
 * The Class DocumentEditorProviderDTO.
 */
public class DocumentEditorProviderDTO extends ResourceSupport {

  /** The provider. */
  private final String       provider;

  /** The active. */
  private final boolean      active;

  /** The permissions. */
  private final List<String> permissions;

  /**
   * Instantiates a new document editor provider DTO.
   *
   * @param provider the provider
   */
  public DocumentEditorProviderDTO(DocumentEditorProvider provider) {
    this.provider = provider.getProviderName();
    this.active = provider.isActive();
    this.permissions = provider.getPermissions();
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
  public List<String> getPermissions() {
    return permissions.stream().map(permission -> {
      if (permission.contains("/")) {
        permission = permission.substring(permission.indexOf("/"));
      }
      return permission;
    }).collect(Collectors.toList());
  }
}
