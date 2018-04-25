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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.core.UIPermissionFormBase;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/core/UIPermissionForm.gtmpl",
    events = {
      @EventConfig(listeners = UIPermissionForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.ResetActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.CloseActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.SelectUserActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.SelectMemberActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.AddAnyActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionInputSet.OnChangeActionListener.class)
    }
)
public class UIPermissionForm extends UIPermissionFormBase implements UISelectable {
  
  public UIPermissionForm() throws Exception {
    super();
  }

  static public class SaveActionListener extends EventListener<UIPermissionForm> {
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();

      UIPermissionManagerBase uiParent = uiForm.getParent();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String userOrGroup = uiForm.getChild(UIPermissionInputSet.class).getUIStringInput(
          UIPermissionInputSet.FIELD_USERORGROUP).getValue();
      List<String> permsList = new ArrayList<String>();
      List<String> permsRemoveList = new ArrayList<String>();
      uiExplorer.addLockToken(currentNode);
      if (!currentNode.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }
      for (String perm : PermissionType.ALL) {
        if (uiForm.getUICheckBoxInput(perm) != null &&
            uiForm.getUICheckBoxInput(perm).isChecked()) permsList.add(perm);
        else {
          permsRemoveList.add(perm);
        }
      }
      //check both ADD_NODE and SET_PROPERTY
      if (uiForm.getUICheckBoxInput(PermissionType.ADD_NODE).isChecked()) {
        if(!permsList.contains(PermissionType.SET_PROPERTY))
          permsList.add(PermissionType.SET_PROPERTY);
      }

      //uncheck both ADD_NODE and SET_PROPERTY
      if (!uiForm.getUICheckBoxInput(PermissionType.ADD_NODE).isChecked()) {
        if(!permsRemoveList.contains(PermissionType.SET_PROPERTY))
          permsRemoveList.add(PermissionType.SET_PROPERTY);
      }

      //------------------
      if (Utils.isNameEmpty(userOrGroup)) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.userOrGroup-required", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }
      if (permsList.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.checkbox-require", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }
      String[] permsArray = permsList.toArray(new String[permsList.size()]);
      ExtendedNode node = (ExtendedNode) currentNode;
      if (PermissionUtil.canChangePermission(node)) {
        if (node.canAddMixin("exo:privilegeable")){
          node.addMixin("exo:privilegeable");
          String nodeOwner = Utils.getNodeOwner(node);
          if (nodeOwner != null) {
            node.setPermission(nodeOwner, PermissionType.ALL);
          }
        }
        for (String perm : permsRemoveList) {
          try {
            node.removePermission(userOrGroup, perm);
          } catch (AccessDeniedException ade) {
            uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null,
                                                    ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
            return;
          } catch (AccessControlException accessControlException) {
            uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null,
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
            return;
          }
        }
        try {
          if(PermissionUtil.canChangePermission(node)) node.setPermission(userOrGroup, permsArray);
          node.save();
        } catch (AccessDeniedException ade) {
          uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null,
                                                  ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
          return;
        } catch (AccessControlException accessControlException) {
          uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
          return;
        }
        uiParent.getChild(UIPermissionInfo.class).updateGrid(1);
        if (uiExplorer.getRootNode().equals(node)) {
          if (!PermissionUtil.canRead(currentNode)) {
            uiForm.getAncestorOfType(UIJCRExplorerPortlet.class).reloadWhenBroken(uiExplorer);
            return;
          }
        }
      } else {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.not-change-permission", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }

      // Update symlinks
      uiForm.updateSymlinks(uiForm.getCurrentNode());
      
      currentNode.getSession().save();
      uiForm.refresh();
      uiExplorer.setIsHidePopup(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
  
  public void doSelect(String selectField, Object value) {
    try {
      ExtendedNode node = (ExtendedNode)this.getCurrentNode();
      checkAll(false);
      fillForm(value.toString(), node);
      lockForm(value.toString().equals(getExoOwner(node)));
      getUIStringInput(selectField).setValue(value.toString());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }  

  static public class CloseActionListener extends EventListener<UIPermissionForm> {
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public Node getCurrentNode() throws Exception {
    return this.getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
  }
}
