package org.exoplatform.services.wcm.search.connector;

import org.apache.commons.chain.Context;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.ext.action.AdvancedAction;
import org.exoplatform.services.jcr.impl.ext.action.AdvancedActionException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.function.Consumer;

/**
 *  JCR action which listens on all nodes events to index them
 */
public class FileIndexerAction implements AdvancedAction {
  private static final Log LOGGER = ExoLogger.getExoLogger(FileIndexerAction.class);

  private IndexingService indexingService;

  private TrashService trashService;

  public FileIndexerAction() {
    this.indexingService = CommonsUtils.getService(IndexingService.class);
    this.trashService = CommonsUtils.getService(TrashService.class);
  }

  @Override
  public boolean execute(Context context) throws Exception {
    NodeImpl node;

    int eventType = (Integer) context.get(InvocationContext.EVENT);

    switch(eventType) {
      case Event.NODE_ADDED:
        node = (NodeImpl)context.get(InvocationContext.CURRENT_ITEM);
        if(node != null) {
          if (trashService.isInTrash(node)) {
            applyIndexingOperationOnNodes(node, n -> indexingService.unindex(FileindexingConnector.TYPE, n.getInternalIdentifier()));
          } else {
            applyIndexingOperationOnNodes(node, n -> indexingService.index(FileindexingConnector.TYPE, n.getInternalIdentifier()));
          }
        }
        break;
      case Event.NODE_REMOVED:
        node = (NodeImpl)context.get(InvocationContext.CURRENT_ITEM);
        if(node != null) {
          applyIndexingOperationOnNodes(node, n -> indexingService.unindex(FileindexingConnector.TYPE, n.getInternalIdentifier()));
        }
        break;
      case Event.PROPERTY_ADDED:
      case Event.PROPERTY_CHANGED:
      case Event.PROPERTY_REMOVED:
        PropertyImpl property = (PropertyImpl) context.get(InvocationContext.CURRENT_ITEM);
        if(property != null) {
          node = property.getParent();
          if (node != null && !trashService.isInTrash(node)) {
            if (node.isNodeType(NodetypeConstant.NT_RESOURCE)) {
              node = node.getParent();
            }
            if (node.isNodeType(NodetypeConstant.NT_FILE)) {
              indexingService.reindex(FileindexingConnector.TYPE, node.getInternalIdentifier());
            }
            // reindex children nodes when permissions has been changed (exo:permissions) - it is required
            // to update permissions of the nodes in the indexing engine
            String propertyName = property.getName();
            if (propertyName != null && propertyName.equals("exo:permissions")) {
              applyIndexingOperationOnNodes(node, n -> indexingService.reindex(FileindexingConnector.TYPE, n.getInternalIdentifier()));
            }
          }
        }
        break;
    }

    return true;
  }

  @Override
  public void onError(Exception e, Context context) throws AdvancedActionException {
    LOGGER.error("Error while indexing file", e);
  }

  protected Node getNodeByPath(String path) {
    return NodeLocation.getNodeByLocation(new NodeLocation(WCMCoreUtils.getRepository().getConfiguration().getName(), "collaboration", path));
  }

  /**
   * Apply the given indexing operation (index|reindex|unindex) on all children of a node, only for nt:file nodes
   * @param node The root node to operate on
   * @param indexingOperation Indexing operation (index|reindex|unindex) to apply on the nodes
   */
  protected void applyIndexingOperationOnNodes(NodeImpl node, Consumer<NodeImpl> indexingOperation) {
    if (node == null) {
      return;
    }

    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        indexingOperation.accept(node);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Cannot get primary type of node " + node.getInternalIdentifier(), e);
    }

    try {
      NodeIterator nodeIterator = node.getNodes();
      while(nodeIterator.hasNext()) {
        NodeImpl childNode = (NodeImpl) nodeIterator.nextNode();
        applyIndexingOperationOnNodes(childNode, indexingOperation);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Cannot get child nodes of node " + node.getInternalIdentifier(), e);
    }
  }
}
