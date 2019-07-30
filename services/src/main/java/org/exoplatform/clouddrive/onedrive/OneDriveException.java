package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.CloudProviderException;

public class OneDriveException extends CloudProviderException {
    public OneDriveException(String message, Throwable cause) {
        super(message, cause);
    }
    public OneDriveException(String message) {
        super(message);
    }

    public OneDriveException(Throwable th) {
        super(th);
    }

}
