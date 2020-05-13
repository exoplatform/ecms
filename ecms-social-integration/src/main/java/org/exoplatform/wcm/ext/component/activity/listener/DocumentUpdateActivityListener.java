package org.exoplatform.wcm.ext.component.activity.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.chain.Context;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.DocumentUpdateActivityHandler;
import org.exoplatform.services.listener.Event;

public class DocumentUpdateActivityListener extends FileUpdateActivityListener {
  
  protected final DocumentService documentService;
  /**
   * Instantiates a new document update activity listener.
   */
  public DocumentUpdateActivityListener() {
    documentService = (DocumentService) ExoContainerContext.getCurrentContainer()
                                                                 .getComponentInstanceOfType(DocumentService.class);
  }
  
  @Override
  public void onEvent(Event<Context, String> event) throws Exception {
    List<DocumentUpdateActivityHandler> handlers = documentService.getDocumentEditorProviders().stream().map(p -> p.getDocumentUpdateHandler()).collect(Collectors.toList());
    for(DocumentUpdateActivityHandler handler : handlers) {
      if(handler.handleDocumentUpdateEvent(event)) {
        return;
      }
    }
    super.onEvent(event);
  }

}
