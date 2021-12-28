package org.exoplatform.services.wcm.search.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.model.MetadataItem;

public class DocumentsMetadataListener extends Listener<Long, MetadataItem> {

  private final IndexingService indexingService;

  //private final NewsService     newsService;

  private final FavoriteService favoriteService;

  private final IdentityManager identityManager;

  private static final String   METADATA_CREATED = "social.metadataItem.created";

  private static final String   METADATA_DELETED = "social.metadataItem.deleted";

  private static final String FILE_METADATA_OBJECT_TYPE = "file";

  public DocumentsMetadataListener(IndexingService indexingService,
                                   FavoriteService favoriteService,
                                   IdentityManager identityManager) {
    this.indexingService = indexingService;
    this.favoriteService = favoriteService;
    this.identityManager = identityManager;
  }

  @Override
  public void onEvent(Event<Long, MetadataItem> event) throws Exception {
    MetadataItem metadataItem = event.getData();
    String objectType = event.getData().getObjectType();
    String objectId = metadataItem.getObjectId();
    if (StringUtils.equals(objectType, FILE_METADATA_OBJECT_TYPE)) {
      indexingService.reindex(FILE_METADATA_OBJECT_TYPE, objectId);
    }
  }
}
