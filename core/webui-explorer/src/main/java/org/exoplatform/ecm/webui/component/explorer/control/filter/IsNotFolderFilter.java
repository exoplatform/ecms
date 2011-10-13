package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;
import javax.jcr.Node;

import java.util.List;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen The Vinh
 *          nguyenthevinhbk@gmail.com
 * Oct 07, 2011
 * Filter if node is folder type but not document
 */
public class IsNotFolderFilter extends UIExtensionAbstractFilter{
  
  @Override
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    List<String> dmsDocumentListTmp = templateService.getAllDocumentNodeTypes();
    
    if (currentNode.isNodeType(Utils.NT_UNSTRUCTURED) || currentNode.isNodeType(Utils.NT_FOLDER)) {
      for (String documentType : dmsDocumentListTmp) {
        if (currentNode.getPrimaryNodeType().isNodeType(documentType) ){
          return true;
        }
      }
      return false;
    }
    return true;
}

public void onDeny(Map<String, Object> context) throws Exception {  }
}
