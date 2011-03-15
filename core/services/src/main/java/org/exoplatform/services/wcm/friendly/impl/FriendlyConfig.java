package org.exoplatform.services.wcm.friendly.impl;

import java.util.ArrayList;
import java.util.List;

public class FriendlyConfig {

  private List<Friendly> friendlies = new ArrayList<Friendly>();

  public List<Friendly> getFriendlies() {
    return friendlies;
  }

  public void setFriendlies(List<Friendly> friendlies) {
    this.friendlies = friendlies;
  }

  static public class Friendly {

    private String friendlyUri;
    private String unfriendlyUri;

    public String getFriendlyUri() {
      return friendlyUri;
    }
    public void setFriendlyUri(String friendlyUri) {
      this.friendlyUri = friendlyUri;
    }
    public String getUnfriendlyUri() {
      return unfriendlyUri;
    }
    public void setUnfriendlyUri(String unfriendlyUri) {
      this.unfriendlyUri = unfriendlyUri;
    }
  }

}
