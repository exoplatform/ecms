package org.exoplatform.clouddrive.onedrive;

import com.google.gson.annotations.SerializedName;
// TODO javadocs
/**
 * Stores response data when requesting a token
 */
class OneDriveTokenResponse {
  @SerializedName("access_token")
  private String token;

  @SerializedName("refresh_token")
  private String refreshToken;

  @SerializedName("expires_in")
  private int    expires;

  @SerializedName("scope")
  private String scope;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public int getExpires() {
    return expires;
  }

  public void setExpires(int expires) {
    this.expires = expires;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }
}
