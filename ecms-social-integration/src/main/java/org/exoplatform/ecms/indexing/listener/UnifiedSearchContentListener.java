package org.exoplatform.ecms.indexing.listener;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.commons.api.indexing.data.SearchEntry;
import org.exoplatform.commons.api.indexing.data.SearchEntryId;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import java.util.HashMap;
import java.util.Map;

/**
 * Indexing with :
 * - collection : "content"
 * - type : (file|document)
 * - name : question id
 *
 * TODO No event for content deletion
 */
public class UnifiedSearchContentListener extends Listener {

  private static Log log = ExoLogger.getLogger(UnifiedSearchContentListener.class);

  private final IndexingService indexingService;

  public UnifiedSearchContentListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    if(indexingService != null) {
      if(CmsService.POST_CREATE_CONTENT_EVENT.equals(event.getEventName())) {
        Map<String, Object> content = new HashMap<String, Object>();
        Node contentNode = (Node) event.getData();
        content.put("content", contentNode);
        SearchEntry searchEntry = new SearchEntry("content", contentNode.getPrimaryNodeType().getName().equals("nt:file") ? "file" : "document", contentNode.getUUID(), content);
        indexingService.add(searchEntry);
      } else if(CmsService.POST_EDIT_CONTENT_EVENT.equals(event.getEventName())) {
        Map<String, Object> content = new HashMap<String, Object>();
        Node contentNode = (Node) event.getData();
        content.put("content", contentNode);
        SearchEntryId searchEntryId = new SearchEntryId("content", contentNode.getPrimaryNodeType().getName().equals("nt:file") ? "file" : "document", contentNode.getUUID());
        indexingService.update(searchEntryId, content);
      }
    }
  }

}
