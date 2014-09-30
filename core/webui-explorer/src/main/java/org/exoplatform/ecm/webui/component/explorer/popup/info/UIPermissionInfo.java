/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.core.UIPermissionInfoBase;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import java.util.List;

@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = UIPermissionInfo.DeleteActionListener.class,
        confirm = "UIPermissionInfo.msg.confirm-delete-permission"),
    @EventConfig(listeners = UIPermissionInfo.EditActionListener.class) })
public class UIPermissionInfo extends UIPermissionInfoBase {

  public UIPermissionInfo() throws Exception {
    super();
  }

  private static final Log LOG = ExoLogger.getLogger(UIPermissionInfo.class.getName());

  static public class DeleteActionListener extends EventListener<UIPermissionInfo> {
    public void execute(Event<UIPermissionInfo> event) throws Exception {
      UIPermissionInfo uicomp = event.getSource() ;
      UIJCRExplorer uiJCRExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiJCRExplorer.getCurrentNode() ;
      uiJCRExplorer.addLockToken(currentNode);
      ExtendedNode node = (ExtendedNode)currentNode;
      String owner = IdentityConstants.SYSTEM ;
      int iSystemOwner = 0;
      if (uicomp.getExoOwner(node) != null) owner = uicomp.getExoOwner(node);
      if (owner.equals(IdentityConstants.SYSTEM)) iSystemOwner = -1;
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class) ;
      if (uicomp.getSizeOfListPermission() < 2 + iSystemOwner) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.no-permission-remove",
            null, ApplicationMessage.WARNING));

        return;
      }
      String name = event.getRequestContext().getRequestParameter(OBJECTID) ;
      if(!currentNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null,
            ApplicationMessage.WARNING)) ;

        return ;
      }
      String nodeOwner = Utils.getNodeOwner(node);
      if(name.equals(nodeOwner)) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.no-permission-remove", null,
                ApplicationMessage.WARNING)) ;
        return ;
      }
      if(PermissionUtil.canChangePermission(node)) {
        if(node.canAddMixin("exo:privilegeable"))  {
          node.addMixin("exo:privilegeable");
          node.setPermission(nodeOwner,PermissionType.ALL);
        }
        try {
          node.removePermission(name) ;
          node.save() ;
        } catch(AccessDeniedException ace) {
          node.getSession().refresh(false) ;
          uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.access-denied", null,
                                                  ApplicationMessage.WARNING)) ;

          return ;
        }
        if(uiJCRExplorer.getRootNode().equals(node)) {
          if(!PermissionUtil.canRead(currentNode)) {
            uiJCRExplorer.getAncestorOfType(UIJCRExplorerPortlet.class).reloadWhenBroken(uiJCRExplorer) ;
            return ;
          }
        }
        node.getSession().save() ;
      } else {
        uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.no-permission-tochange", null,
            ApplicationMessage.WARNING)) ;

        return ;
      }
      UIPopupContainer uiPopup = uicomp.getAncestorOfType(UIPopupContainer.class) ;
      if(!PermissionUtil.canRead(node)) {
        uiJCRExplorer.setSelectNode(LinkUtils.getParentPath(uiJCRExplorer.getCurrentPath()));
        uiPopup.deActivate() ;
      } else {
        uicomp.updateGrid(uicomp.getChild(UIGrid.class).getUIPageIterator().getCurrentPage());
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp.getParent()) ;
      }
      Node realNode = uiJCRExplorer.getRealCurrentNode();
      LinkManager linkManager = uiJCRExplorer.getApplicationComponent(LinkManager.class);
      if (linkManager.isLink(realNode)) {
        // Reset the permissions
        linkManager.updateLink(realNode, currentNode);
      }

      if(currentNode.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)){
        List<Node> symlinks = linkManager.getAllLinks(currentNode, "exo:symlink");
        for (Node symlink : symlinks) {
          try {
            linkManager.updateLink(symlink, currentNode);
          } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(e.getMessage());
            }
          }
        }
      }
      currentNode.getSession().save();
      uiJCRExplorer.setIsHidePopup(true) ;
      if(!PermissionUtil.canRead(currentNode)){
        uiPopup.cancelPopupAction();
        uiJCRExplorer.refreshExplorer(currentNode.getSession().getRootNode(), true);
      }else {
        uiJCRExplorer.refreshExplorer(currentNode, false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiJCRExplorer) ;
    }
  }

  public Node getCurrentNode() throws Exception {
    return this.getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
  }
}

