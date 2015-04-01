package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import java.util.Map;

/**
 * Check parent node modify permission for Open Document In Office feature
 */
public class CanEditDocFilter extends UIExtensionAbstractFilter {
  public CanEditDocFilter() {
    this("UIActionBar.msg.unsupported-action");
  }

  public CanEditDocFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#accept(java.util.Map)
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());

    return PermissionUtil.canAddNode(currentNode.getParent());
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#onDeny(java.util.Map)
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
