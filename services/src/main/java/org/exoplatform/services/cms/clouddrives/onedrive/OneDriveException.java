package org.exoplatform.services.cms.clouddrives.onedrive;

import org.exoplatform.services.cms.clouddrives.CloudProviderException;

// TODO generate serial ID, add javadocs
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
