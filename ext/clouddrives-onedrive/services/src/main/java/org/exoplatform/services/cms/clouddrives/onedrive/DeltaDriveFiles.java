package org.exoplatform.services.cms.clouddrives.onedrive;

import java.util.List;

import com.microsoft.graph.models.extensions.DriveItem;

/**
 * Stores data that we receive when track changes: new items since the previous
 * call and token for getting changes later.
 */
class DeltaDriveFiles {
  private String          deltaToken;

  private List<DriveItem> items;

  DeltaDriveFiles(String deltaToken, List<DriveItem> items) {
    this.deltaToken = deltaToken;
    this.items = items;
  }

  public String getDeltaToken() {
    return deltaToken;
  }

  public void setDeltaToken(String deltaToken) {
    this.deltaToken = deltaToken;
  }

  public List<DriveItem> getItems() {
    return items;
  }

  public void setItems(List<DriveItem> items) {
    this.items = items;
  }
}
