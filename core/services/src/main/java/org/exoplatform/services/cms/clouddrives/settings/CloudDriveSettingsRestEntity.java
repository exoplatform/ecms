package org.exoplatform.services.cms.clouddrives.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudDriveRestEntity {
    private String                        id;

    private String                        connector;

    private String                        user;

    private String                        account;
}
