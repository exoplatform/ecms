package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import java.util.Map;

/**
 * Filter a node can be renamed if its parent is locked.
 * It means that only to accept a node of which parent is not locked or (is locked and able to get lock token)
 */
public class IsNotParentLockedWhenRenameFilter extends UIExtensionAbstractFilter {

  @Override
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }
  
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node parentNode = ((Node) context.get(Node.class.getName())).getParent();
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    return !(parentNode.isLocked() && lockService.getLockToken(parentNode) == null);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
