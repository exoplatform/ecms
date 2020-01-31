package org.exoplatform.services.wcm.search.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.connector.NavigationIndexingServiceConnector;

public class NavigationNodeESListenerImpl extends Listener<NavigationService, NodeContext> {

  private static final Log LOG = ExoLogger.getExoLogger(NavigationNodeESListenerImpl.class);

  private IndexingService indexingService;

  private NavigationService navigationService;

  public NavigationNodeESListenerImpl(IndexingService indexingService, NavigationService navigationService) {
    this.indexingService = indexingService;
    this.navigationService = navigationService;
  }

  @Override
  public void onEvent(Event<NavigationService, NodeContext> event) throws Exception {
    String eventName = event.getEventName();
    NodeContext node = event.getData();
    LOG.debug("Notifying indexing service for navigation node={}", node.getId());
    //
    if (EventType.NAVIGATION_NODE_ADD.equals(eventName) ||
            EventType.NAVIGATION_NODE_CREATE.equals(eventName)) {
      indexingService.index(NavigationIndexingServiceConnector.TYPE, node.getId());
    } else if (EventType.NAVIGATION_NODE_DESTROY.equals(eventName) ||
            EventType.NAVIGATION_NODE_REMOVE.equals(eventName)) {
      indexingService.unindex(NavigationIndexingServiceConnector.TYPE, node.getId());
    } else if (EventType.NAVIGATION_NODE_MOVE.equals(eventName) ||
            EventType.NAVIGATION_NODE_RENAME.equals(eventName) ||
            EventType.NAVIGATION_NODE_UPDATE.equals(eventName)) {
      indexingService.reindex(NavigationIndexingServiceConnector.TYPE, node.getId());
    }
  }
}
