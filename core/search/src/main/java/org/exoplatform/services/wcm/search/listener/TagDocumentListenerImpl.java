package org.exoplatform.services.wcm.search.listener;

import org.exoplatform.commons.search.index.IndexingOperationProcessor;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;

import javax.jcr.Node;

public class TagDocumentListenerImpl extends Listener<Node, String> {

  private static final Log LOG = ExoLogger.getExoLogger(TagDocumentListenerImpl.class);

  private static final String TAG_ADDED_TO_DOCUMENT = "Document.event.TagAdded";

  private static final String TAG_REMOVED_FROM_DOCUMENT = "Document.event.TagRemoved";

  private IndexingOperationProcessor indexingOperationProcessor;

  private IndexingService indexingService;

  public TagDocumentListenerImpl(IndexingOperationProcessor indexingOperationProcessor, IndexingService indexingService) {
    this.indexingOperationProcessor = indexingOperationProcessor;
    this.indexingService = indexingService;
  }

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {

    Node node = event.getSource();
    if (TAG_ADDED_TO_DOCUMENT.equals(event.getEventName()) || TAG_REMOVED_FROM_DOCUMENT.equals(event.getEventName())) {
      indexingService.reindex(FileindexingConnector.TYPE, node.getUUID());
    }
  }
}
