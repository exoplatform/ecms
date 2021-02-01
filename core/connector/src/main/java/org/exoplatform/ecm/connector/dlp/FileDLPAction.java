package org.exoplatform.ecm.connector.dlp;

import javax.jcr.observation.Event;

import org.apache.commons.chain.Context;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.dlp.queue.QueueDlpService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.ext.action.AdvancedAction;
import org.exoplatform.services.jcr.impl.ext.action.AdvancedActionException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * JCR action which listens on all nodes events to validate them
 */
public class FileDLPAction implements AdvancedAction {
  private static final Log LOGGER = ExoLogger.getExoLogger(FileDLPAction.class);
  
  private TrashService     trashService;

  private QueueDlpService queueDlpService;
  
  private ExoFeatureService featureService;

  public FileDLPAction() {
    this.trashService = CommonsUtils.getService(TrashService.class);
    this.queueDlpService = CommonsUtils.getService(QueueDlpService.class);
    this.featureService = CommonsUtils.getService(ExoFeatureService.class);
  }

  @Override
  public boolean execute(Context context) throws Exception {
    if (!featureService.isActiveFeature(DlpOperationProcessor.DLP_FEATURE)) {
      return false;
    }
    int eventType = (Integer) context.get(InvocationContext.EVENT);
    NodeImpl node;
    switch(eventType) {
      case Event.NODE_ADDED:
        node = (NodeImpl) context.get(InvocationContext.CURRENT_ITEM);
        if(node != null && !trashService.isInTrash(node)) {
          String entityId = node.getInternalIdentifier();
          queueDlpService.addToQueue(FileDlpConnector.TYPE, entityId);
        }
        break;
      case Event.PROPERTY_ADDED:
      case Event.PROPERTY_CHANGED:
        PropertyImpl property = (PropertyImpl) context.get(InvocationContext.CURRENT_ITEM);
        if(property != null && property.getType() != 1) {
          node = property.getParent();
          if (node != null && !trashService.isInTrash(node)) {
            if (node.isNodeType(NodetypeConstant.NT_RESOURCE)) {
              node = node.getParent();
            }
            if (node.isNodeType(NodetypeConstant.NT_FILE)) {
              String entityId = node.getInternalIdentifier();
              queueDlpService.addToQueue(FileDlpConnector.TYPE, entityId);
            }
          }
        }
        break;
    }
    return true;
  }
  
  @Override
  public void onError(Exception e, Context context) throws AdvancedActionException {
    LOGGER.error("Error while validating file", e);
  }
}
