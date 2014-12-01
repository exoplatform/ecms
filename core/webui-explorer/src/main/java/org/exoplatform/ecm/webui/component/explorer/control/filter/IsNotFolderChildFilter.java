package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import java.util.Map;

/**
 * Created by toannh on 12/2/14.
 * Filter node not include sub folder
 */
public class IsNotFolderChildFilter implements UIExtensionFilter {

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if(context==null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    return !hasFolderChild(currentNode);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {

  }

  @Override
  public UIExtensionFilterType getType() {
      return UIExtensionFilterType.MANDATORY;
  }

  /**
   * Check if specific node has child which is type of nt:folder or nt:unstructured.
   *
   * @param checkNode The node to be checked
   * @return True if the folder has some child.
   * @throws Exception
   */
  private boolean hasFolderChild(Node checkNode) throws Exception {
    return (Utils.hasChild(checkNode, NodetypeConstant.NT_UNSTRUCTURED)
            || Utils.hasChild(checkNode, NodetypeConstant.NT_FOLDER));
  }
}
