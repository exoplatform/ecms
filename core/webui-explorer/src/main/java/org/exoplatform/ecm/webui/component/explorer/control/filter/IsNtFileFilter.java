package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import java.util.Map;

public class IsNtFileFilter extends UIExtensionAbstractFilter {
  public IsNtFileFilter() {
    this("UIActionBar.msg.unsupported-action");
  }

  public IsNtFileFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#accept(java.util.Map)
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    return currentNode.isNodeType(Utils.NT_FILE);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#onDeny(java.util.Map)
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
