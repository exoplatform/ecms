package org.exoplatform.ecm.connector.dlp;

import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class PositiveFileDLPListener extends Listener<DlpPositiveItemService, String> {

  private DlpOperationProcessor dlpOperationProcessor;

  public PositiveFileDLPListener() {
    dlpOperationProcessor = CommonsUtils.getService(DlpOperationProcessor.class);
  }

  public void onEvent(Event<DlpPositiveItemService, String> event) throws Exception {
    DlpServiceConnector fileConnector = (DlpServiceConnector) dlpOperationProcessor.getConnectors().get(FileDlpConnector.TYPE);
    fileConnector.removePositiveItem(event.getData());
  }

}
