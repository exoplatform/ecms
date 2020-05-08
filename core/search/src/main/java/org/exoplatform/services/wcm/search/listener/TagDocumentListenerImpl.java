package org.exoplatform.services.wcm.search.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.connector.FileindexingConnector;

import javax.jcr.Node;

public class TagDocumentListenerImpl extends Listener<Node, String> {

  private static final Log LOG = ExoLogger.getExoLogger(TagDocumentListenerImpl.class);

  private IndexingService indexingService;

  public TagDocumentListenerImpl(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node node = event.getSource();
    LOG.debug("Notifying reindexing service for tag document node={}", node.getUUID());
    if (node!= null) {
      indexingService.reindex(FileindexingConnector.TYPE, node.getUUID());
    }
  }
}
