package org.exoplatform.services.cms.documents;

import org.apache.commons.chain.Context;

import org.exoplatform.services.listener.Event;

public interface DocumentUpdateActivityHandler {
  
  boolean handleDocumentUpdateEvent(Event<Context, String> event) throws Exception;

}
