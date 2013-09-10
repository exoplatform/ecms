package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * September 24, 2012  
 */

public class CanAddSymlinkFilter implements UIExtensionFilter {  
  public boolean accept(Map<String, Object> context) throws Exception {
    if(context == null) return true;
    UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
    if(uiExplorer == null) return true;    
    Node currentNode = uiExplorer.getCurrentNode();
    if (currentNode.getPrimaryNodeType().canAddChildNode(String.valueOf(System.currentTimeMillis()), Utils.EXO_SYMLINK)) {
      return true;
    }
    NodeType[] declaredSuperTypes = currentNode.getPrimaryNodeType().getSupertypes();
    for (NodeType nodeType : declaredSuperTypes) {
      if(nodeType.canAddChildNode(String.valueOf(System.currentTimeMillis()), Utils.EXO_SYMLINK)) return true;
    }
    NodeType[] declaredSuperTypesOfMixs = currentNode.getMixinNodeTypes();
    for (NodeType mixin : declaredSuperTypesOfMixs) {
      NodeType[] superTypes = mixin.getSupertypes();
      if (mixin.canAddChildNode(String.valueOf(System.currentTimeMillis()), Utils.EXO_SYMLINK)) return true;
      for (NodeType nodeType : superTypes) {
        if(nodeType.canAddChildNode(String.valueOf(System.currentTimeMillis()), Utils.EXO_SYMLINK)) return true;
      }
    }
    return false;
  }

  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
  }
}

