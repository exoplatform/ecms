package org.exoplatform.clouddrive.onedrive;

import com.microsoft.graph.models.extensions.DriveItem;

/**
 * DriveItem wrapper for use with HashSet.
 */
class HashSetCompatibleDriveItem {
  DriveItem item;

  public HashSetCompatibleDriveItem(DriveItem item) {
    this.item = item;
  }

  public DriveItem getItem() {
    return item;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    HashSetCompatibleDriveItem that = (HashSetCompatibleDriveItem) o;

    return item.id.equals(that.item.id);
  }

  @Override
  public int hashCode() {
    return item.id.hashCode();
  }
}
