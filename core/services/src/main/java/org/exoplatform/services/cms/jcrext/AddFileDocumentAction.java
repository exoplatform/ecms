package org.exoplatform.services.cms.jcrext;

import javax.jcr.Node;
import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * May 30, 2011
 */

public class AddFileDocumentAction implements Action{
  public boolean execute(Context context) throws Exception {
    Node node = (Node)context.get("currentItem");
    if(node.isNodeType("nt:file") && node.canAddMixin("mix:referenceable")) {
      node.addMixin("mix:referenceable");
    }
    return false;
  }
}
