package org.exoplatform.wcm.connector.collaboration.dto;

/**
 * The Class Permission.
 */
public class Permission {

  /** The name. */
  protected String id;

  /** The display name. */
  protected String displayName;

  /** The avatar url. */
  protected String avatarUrl;

  /**
   * Instantiates a new permission.
   */
  public Permission() {

  }

  /**
   * Instantiates a new permission.
   *
   * @param id the id
   * @param displayName the display name
   * @param avatarUrl the avatar url
   */
  public Permission(String id, String displayName, String avatarUrl) {
    this.id = id;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
  }

  /**
   * Instantiates a new permission.
   *
   * @param id the id
   */
  public Permission(String id) {
    this.id = id;
    displayName = null;
    avatarUrl = null;
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
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

  /**
   * Sets the id.
   *
   * @param id the new id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Sets the display name.
   *
   * @param displayName the new display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Sets the avatar url.
   *
   * @param avatarUrl the new avatar url
   */
  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

}
