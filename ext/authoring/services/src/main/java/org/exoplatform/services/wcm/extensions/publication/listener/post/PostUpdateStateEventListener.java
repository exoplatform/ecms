package org.exoplatform.services.wcm.extensions.publication.listener.post;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


public class PostUpdateStateEventListener extends Listener {

  private static final Log LOG = ExoLogger.getLogger(PostUpdateStateEventListener.class.getName());

  @Override
  public void onEvent(Event event) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("this listener will be called every time a content changes its current state");
    }
  }

}
