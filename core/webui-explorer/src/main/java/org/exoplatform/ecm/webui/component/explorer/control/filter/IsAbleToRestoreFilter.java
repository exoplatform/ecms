package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
        String restorePath;
        String restoreWorkspace;
        Node currentNode = (Node) context.get(Node.class.getName());
        Node restoreLocationNode;
        
        // return false if the node is exo:actions
        if (Utils.EXO_ACTIONS.equals(currentNode.getName()) && Utils.isInTrash(currentNode)) {
          return false;
        }
        
        // return false if the target has been already in Trash.
        if ( Utils.targetNodeAndLinkInTrash(currentNode) ) {
          return false;
        }
        
        if (ConversationState.getCurrent().getIdentity().getUserId().equalsIgnoreCase(WCMCoreUtils.getSuperUser())) { 
          return true;
        }

        if ( currentNode.isNodeType(TrashService.EXO_RESTORE_LOCATION)) {
            restorePath = currentNode.getProperty(TrashService.RESTORE_PATH).getString();
            restoreWorkspace = currentNode.getProperty(TrashService.RESTORE_WORKSPACE).getString();
            restorePath = restorePath.substring(0, restorePath.lastIndexOf("/"));
        } else {
            //Is not a deleted node, may be groovy action, hidden node,...
            return false;
        }
        
        Session session = WCMCoreUtils.getUserSessionProvider().getSession(restoreWorkspace, WCMCoreUtils.getRepository());
        try {
            restoreLocationNode = (Node) session.getItem(restorePath);
        } catch(Exception e) {
            return false;
        }
        return PermissionUtil.canAddNode(restoreLocationNode);
    }

    @Override
    public void onDeny(Map<String, Object> context) throws Exception {
        createUIPopupMessages(context, "UIDocumentInfo.msg.access-denied");
    }
    
}
