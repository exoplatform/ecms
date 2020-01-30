package org.exoplatform.services.wcm.search.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.connector.NavigationIndexingServiceConnector;

public class NavigationESListenerImpl extends Listener<NavigationService, SiteKey> {

  private static final Log LOG = ExoLogger.getExoLogger(NavigationESListenerImpl.class);

  private IndexingService indexingService;

  private NavigationService navigationService;

  public NavigationESListenerImpl(IndexingService indexingService, NavigationService navigationService) {
    this.indexingService = indexingService;
    this.navigationService = navigationService;
  }

  @Override
  public void onEvent(Event<NavigationService, SiteKey> event) throws Exception {
    SiteKey siteKey = event.getData();
    LOG.debug("Notifying indexing service for navigation={}", siteKey);

    if (EventType.NAVIGATION_DESTROY.equals(event.getEventName())) {
      NavigationContext nav = navigationService.loadNavigation(siteKey);
      NodeContext node = navigationService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      unIndexTree(node);
    } else if (EventType.NAVIGATION_CREATED.equals(event.getEventName())) {
      NavigationContext nav = navigationService.loadNavigation(siteKey);
      NodeContext node = navigationService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      indexTree(node);
    }
  }

  private void unIndexTree(NodeContext node) {
    indexingService.unindex(NavigationIndexingServiceConnector.TYPE, node.getId());
    if (node.getNodes() != null) {
      for (Object child : node.getNodes()) {
        unIndexTree((NodeContext) child);
      }
    }
  }

  private void indexTree(NodeContext node) {
    indexingService.index(NavigationIndexingServiceConnector.TYPE, node.getId());
    if (node.getNodes() != null) {
      for (Object child : node.getNodes()) {
        indexTree((NodeContext) child);
      }
    }
  }
}
