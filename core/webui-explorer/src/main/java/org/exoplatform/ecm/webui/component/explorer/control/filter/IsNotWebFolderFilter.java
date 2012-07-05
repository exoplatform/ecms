package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;
import javax.jcr.Node;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

public class IsNotWebFolderFilter extends UIExtensionAbstractFilter{
	
  public IsNotWebFolderFilter() {
    this("UIActionBar.msg.unsupported-action");
  }
	
  public IsNotWebFolderFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
	
  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#accept(java.util.Map)
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    return !currentNode.isNodeType(NodetypeConstant.EXO_WEB_FOLDER);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#onDeny(java.util.Map)
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

}
