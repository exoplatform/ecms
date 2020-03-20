package org.exoplatform.wcm.connector.collaboration.dto;

/**
 * The Class IdentitySearchResult.
 */
public class IdentitySearchResult extends Permission {

  /** The type. */
  private String type;

  
  /**
   * Instantiates a new identity search result.
   *
   * @param id the id
   * @param displayName the display name
   * @param type the type
   * @param avatarUrl the avatar url
   */
  public IdentitySearchResult(String id, String displayName, String type, String avatarUrl) {
    super(id, displayName, avatarUrl);
    this.type = type;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

}
