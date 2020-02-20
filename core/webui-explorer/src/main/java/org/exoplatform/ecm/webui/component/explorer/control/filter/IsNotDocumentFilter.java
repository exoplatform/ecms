package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import static org.exoplatform.services.cms.impl.Utils.isDocument;

public class IsNotDocumentFilter extends UIExtensionAbstractFilter {

  public IsNotDocumentFilter() {
    this("UIActionBar.msg.not-supported");
  }

  public IsNotDocumentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null)
      return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    if (isDocument(currentNode)) {
      return false;
    }
    return true;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
    if (context == null)
      return;
    createUIPopupMessages(context, messageKey);
  }
}
