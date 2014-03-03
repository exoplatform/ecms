package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * This filter will be used to check the current user has permission to restore selected document/folder or not.
 */
public class IsAbleToRestoreFilter extends UIExtensionAbstractFilter {

    public IsAbleToRestoreFilter() {
        this(null);
    }

    public IsAbleToRestoreFilter(String messageKey) {
        super(messageKey, UIExtensionFilterType.MANDATORY);
    }

    @Override
    public boolean accept(Map<String, Object> context) throws Exception {
        if (context == null) return true;
        Node currentNode = (Node) context.get(Node.class.getName());
        return Utils.isAbleToRestore(currentNode);
    }
    

    @Override
    public void onDeny(Map<String, Object> context) throws Exception {
        createUIPopupMessages(context, "UIDocumentInfo.msg.access-denied");
    }
    
}
