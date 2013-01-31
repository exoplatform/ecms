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
package org.exoplatform.ecm.webui.core;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

public abstract class UIPermissionFormBase extends UIForm implements UISelectable {

  public static final String PERMISSION   = "permission";
  public static final String POPUP_SELECT = "SelectUserOrGroup";
  public static final String SYMLINK = "exo:symlink";
  
  private static final String[] PERMISSION_TYPES = {PermissionType.READ, PermissionType.ADD_NODE, PermissionType.REMOVE}; 
  private static final Log LOG  = ExoLogger.getLogger(UIPermissionFormBase.class.getName());

  public UIPermissionFormBase() throws Exception {
    addChild(new UIPermissionInputSet(PERMISSION));
    setActions(new String[] { "Save", "Reset", "Close" });
  }
  
  public abstract Node getCurrentNode() throws Exception;

  public void fillForm(String user, ExtendedNode node) throws Exception {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION);
    refresh();
    uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(user);
    if(user.equals(Utils.getNodeOwner(node))) {
      for (String perm : PERMISSION_TYPES) {
        uiInputSet.getUICheckBoxInput(perm).setChecked(true);
      }
    } else {
      List<AccessControlEntry> permsList = node.getACL().getPermissionEntries();
      Iterator<AccessControlEntry> perIter = permsList.iterator();
      StringBuilder userPermission = new StringBuilder();
      while(perIter.hasNext()) {
        AccessControlEntry accessControlEntry = perIter.next();
        if(user.equals(accessControlEntry.getIdentity())) {
          userPermission.append(accessControlEntry.getPermission()).append(" ");
        }
      }
      for (String perm : PERMISSION_TYPES) {
        boolean isCheck = userPermission.toString().contains(perm);
        uiInputSet.getUICheckBoxInput(perm).setChecked(isCheck);
      }
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
  
  public void updateSymlinks(Node node) throws Exception {
    if(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)){
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
      List<Node> symlinks = linkManager.getAllLinks(node, SYMLINK);
      for (Node symlink : symlinks) {
        try {
          linkManager.updateLink(symlink, node);
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
      }
    }
  }
  
  protected void lockForm(boolean isLock) {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION);
    if(isLock) {
      setActions(new String[] {"Reset", "Close" });
      uiInputSet.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, null);
    } else {
      setActions(new String[] { "Save", "Reset", "Close" });
      uiInputSet.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, new String[] {"SelectUser", "SelectMember", "AddAny"});
    }
    for (String perm : PERMISSION_TYPES) {
      uiInputSet.getUICheckBoxInput(perm).setDisabled(isLock);
    }
  }

  protected boolean isEditable(Node node) throws Exception {
    return PermissionUtil.canChangePermission(node);
  }

  protected void refresh() {
    reset();
    checkAll(false);
  }

  private void checkAll(boolean check) {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION);
    for (String perm : PERMISSION_TYPES) {
      uiInputSet.getUICheckBoxInput(perm).setChecked(check);
    }
  }

  private String  getExoOwner(Node node) throws Exception {
    return Utils.getNodeOwner(node);
  }

  static public class ResetActionListener extends EventListener<UIPermissionFormBase> {
    public void execute(Event<UIPermissionFormBase> event) throws Exception {
      UIPermissionFormBase uiForm = event.getSource();
      uiForm.lockForm(false);
      uiForm.refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static public class SelectUserActionListener extends EventListener<UIPermissionFormBase> {
    public void execute(Event<UIPermissionFormBase> event) throws Exception {
      UIPermissionFormBase uiForm = event.getSource();
      ((UIPermissionManagerBase)uiForm.getParent()).initUserSelector();
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }

  static public class AddAnyActionListener extends EventListener<UIPermissionFormBase> {
    public void execute(Event<UIPermissionFormBase> event) throws Exception {
      UIPermissionFormBase uiForm = event.getSource();
      UIPermissionInputSet uiInputSet = uiForm.getChildById(UIPermissionFormBase.PERMISSION);
      uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(IdentityConstants.ANY);
      uiForm.checkAll(false);
      uiInputSet.getUICheckBoxInput(PermissionType.READ).setChecked(true);
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }

  static public class SelectMemberActionListener extends EventListener<UIPermissionFormBase> {
    public void execute(Event<UIPermissionFormBase> event) throws Exception {
      UIPermissionFormBase uiForm = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiForm.createUIComponent(UIGroupMemberSelector.class, null, null);
      uiGroupMemberSelector.setSourceComponent(uiForm, new String[] { UIPermissionInputSet.FIELD_USERORGROUP });
      uiForm.getAncestorOfType(UIPermissionManagerBase.class).initPopupPermission(uiGroupMemberSelector);
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }
}
