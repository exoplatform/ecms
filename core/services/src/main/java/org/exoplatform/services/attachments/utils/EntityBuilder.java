package org.exoplatform.services.attachments.utils;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.rest.model.AttachmentEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.rest.entity.IdentityEntity;

public class EntityBuilder {

  private static final String IDENTITIES_REST_PATH = "/v1/social/identities"; // NOSONAR

  private static final String IDENTITIES_EXPAND = "all";

  public static final AttachmentEntity fromAttachment(IdentityManager identityManager, Attachment attachment) {
    return new AttachmentEntity(attachment.getId(),
            attachment.getTitle(),
            attachment.getSize(),
            attachment.getMimetype(),
            attachment.getPath(),
            attachment.getIsPublic(),
            attachment.getAcl(),
            null,
            attachment.getCreated(),
            getIdentityEntity(identityManager, attachment.getUpdater()),
            attachment.getUpdated(),
            attachment.getDownloadUrl(),
            attachment.getOpenUrl(),
            attachment.getPreviewBreadcrumb(),
            attachment.getVersion()

    );
  }

  private static IdentityEntity getIdentityEntity(IdentityManager identityManager, String ownerId) {
    Identity identity = getIdentity(identityManager, ownerId);
    if (identity == null) {
      return null;
    }
    return org.exoplatform.social.rest.api.EntityBuilder.buildEntityIdentity(identity, IDENTITIES_REST_PATH, IDENTITIES_EXPAND);
  }

  private static final Identity getIdentity(IdentityManager identityManager, String identityId) {
    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, identityId);
  }
}
