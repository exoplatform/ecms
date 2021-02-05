package org.exoplatform.ecm.connector.dlp;

import org.exoplatform.commons.dlp.connector.DlpServiceConnector;
import org.exoplatform.commons.dlp.domain.DlpPositiveItemEntity;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.service.DlpPositiveItemService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class RestoreFileDLPListener extends Listener<DlpPositiveItemService, Object> {

  private DlpOperationProcessor dlpOperationProcessor;

  public RestoreFileDLPListener() {
    dlpOperationProcessor = CommonsUtils.getService(DlpOperationProcessor.class);
  }

  public void onEvent(Event<DlpPositiveItemService, Object> event) throws Exception {
    DlpServiceConnector fileConnector = (DlpServiceConnector) dlpOperationProcessor.getConnectors().get(FileDlpConnector.TYPE);
    fileConnector.restorePositiveItem(((DlpPositiveItemEntity) event.getData()).getReference());
  }
}
