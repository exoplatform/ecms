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

  private static final String ADD_TAG_TO_SEARCH_DOCUMENT = "ActivityNotify.event.TagAdded";

  private static final String REMOVE_TAG_FROM_SEARCH_DOCUMENT = "ActivityNotify.event.TagRemoved";

  private   IndexingOperationProcessor indexingOperationProcessor;

  private IndexingService            indexingService;

  public TagDocumentListenerImpl(IndexingOperationProcessor indexingOperationProcessor, IndexingService indexingService) {
    this.indexingOperationProcessor = indexingOperationProcessor;
    this.indexingService = indexingService;
  }
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {

    Node node = event.getSource();
    if (ADD_TAG_TO_SEARCH_DOCUMENT.equals(event.getEventName()) || REMOVE_TAG_FROM_SEARCH_DOCUMENT.equals(event.getEventName())  ) {
    indexingOperationProcessor.getConnectors().get(FileindexingConnector.TYPE).update(node.getUUID());
    indexingService.reindex(FileindexingConnector.TYPE, node.getUUID());
    }
  }
}
