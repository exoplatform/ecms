package org.exoplatform.clouddrive.onedrive;

import java.util.List;

import com.microsoft.graph.models.extensions.DriveItem;

public interface OneDriveAPI {
    public List<DriveItem> getChildren(String fileId);
    public void removeFile(String fileId);
    public void removeFolder(String fileId);
}
