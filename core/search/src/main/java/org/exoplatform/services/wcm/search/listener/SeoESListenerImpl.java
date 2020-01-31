package org.exoplatform.services.wcm.search.listener;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.mop.navigation.NavigationStore;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.search.connector.NavigationIndexingServiceConnector;

public class SeoESListenerImpl extends Listener<SEOService, PageMetadataModel> {

  private IndexingService indexingService;

  private NavigationStore navigationStore;

  public SeoESListenerImpl(IndexingService indexingService,
                           NavigationStore navigationStore) {
    this.indexingService = indexingService;
    this.navigationStore = navigationStore;
  }

  @Override
  public void onEvent(Event<SEOService, PageMetadataModel> event) throws Exception {
    PageMetadataModel seo = event.getData();
    updateIndex(seo);
  }

  private void updateIndex(PageMetadataModel seo) {
    for (String id : search(seo)) {
      indexingService.reindex(NavigationIndexingServiceConnector.TYPE, id);
    }
  }

  public List<String> search(PageMetadataModel seo) {
    String pageRef = seo.getPageReference();
    NodeData[] nodes = navigationStore.loadNodes(pageRef);
    List<String> ids = new ArrayList<>();
    for(NodeData node : nodes) {
      ids.add(node.getId());
    }
    return ids;
  }

}
