/*
 * Copyright (C) 2021 eXo Platform SAS
 *
 *  This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <gnu.org/licenses>.
 */

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


  private static final String FILE_METADATA_OBJECT_TYPE = "file";

  public DocumentsMetadataListener(IndexingService indexingService) {
    this.indexingService = indexingService;
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
