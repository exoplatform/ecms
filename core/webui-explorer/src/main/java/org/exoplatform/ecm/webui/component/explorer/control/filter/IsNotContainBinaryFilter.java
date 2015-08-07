package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/7/15
 * Filter for Edit button
 * Edit content for content type
 * Edit property for nt:file
 */
public class IsNotContainBinaryFilter extends UIExtensionAbstractFilter {

  public IsNotContainBinaryFilter() {
    this("UIActionBar.msg.node-checkedin");
  }

  public IsNotContainBinaryFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    if(currentNode.isNodeType(NodetypeConstant.NT_FILE)) {
      try {
        String mimeType = currentNode.getNode(NodetypeConstant.JCR_CONTENT)
                .getProperty(NodetypeConstant.JCR_MIME_TYPE).getString();
        if (mimeType != null && (mimeType.startsWith("text") || mimeType.indexOf("groovy") >= 0)) {
          return true;
        }
      } catch (RepositoryException re) {
        return false;
      }
    }
    return false;
  }

  public void onDeny(Map<String, Object> context) throws Exception {}

}
