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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 * Editor : TuanP
 *        phamtuanchip@yahoo.de
 * Oct 13, 20006
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = UIPermissionInfo.DeleteActionListener.class,
                 confirm = "UIPermissionInfo.msg.confirm-delete-permission"),
    @EventConfig(listeners = UIPermissionInfo.EditActionListener.class) })

public class UIPermissionInfo extends UIContainer {

  private static final Log LOG = ExoLogger.getLogger(UIPermissionInfo.class);
  
  public static String[] PERMISSION_BEAN_FIELD = {"usersOrGroups", "read", "addNode",
    "setProperty", "remove"} ;

  private static String[] PERMISSION_ACTION = {"Edit", "Delete"} ;

  private int sizeOfListPermission = 0;

  private NodeLocation currentNode;

  public UIPermissionInfo() throws Exception {
    UIGrid uiGrid = createUIComponent(UIGrid.class, null, "PermissionInfo") ;
    addChild(uiGrid) ;
    uiGrid.getUIPageIterator().setId("PermissionInfoIterator");
    uiGrid.configure("usersOrGroups", PERMISSION_BEAN_FIELD, PERMISSION_ACTION) ;
  }

  private String  getExoOwner(Node node) throws Exception {
    return Utils.getNodeOwner(node) ;
  }

  public void updateGrid(int currentPage) throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class);
    List<PermissionBean> permBeans = new ArrayList<PermissionBean>();
    ExtendedNode node = (ExtendedNode) uiJCRExplorer.getCurrentNode();

    List<AccessControlEntry> permsList = node.getACL().getPermissionEntries() ;
    Map<String, List<String>> permsMap = new HashMap<String, List<String>>();
    Iterator<AccessControlEntry> perIter = permsList.iterator() ;
    while(perIter.hasNext()) {
      AccessControlEntry accessControlEntry = perIter.next();
      String currentIdentity = accessControlEntry.getIdentity();
      String currentPermission = accessControlEntry.getPermission();
      List<String> currentPermissionsList = permsMap.get(currentIdentity);
      if(!permsMap.containsKey(currentIdentity)) {
        permsMap.put(currentIdentity, null) ;
      }
      if(currentPermissionsList == null) currentPermissionsList = new ArrayList<String>() ;
      if(!currentPermissionsList.contains(currentPermission)) {
        currentPermissionsList.add(currentPermission) ;
      }
      permsMap.put(currentIdentity, currentPermissionsList) ;
    }
    Set<String> keys = permsMap.keySet();
    Iterator<String> keysIter = keys.iterator() ;
    int iSystemOwner = 0;
    //TODO Utils.getExoOwner(node) has exception return SystemIdentity.SYSTEM
    String owner = IdentityConstants.SYSTEM ;
    if(getExoOwner(node) != null) owner = getExoOwner(node);
    if (owner.equals(IdentityConstants.SYSTEM)) iSystemOwner = -1;
    PermissionBean permOwnerBean = new PermissionBean();
    if(!permsMap.containsKey(owner)) {
      permOwnerBean.setUsersOrGroups(owner);
      permOwnerBean.setRead(true) ;
      permOwnerBean.setAddNode(true) ;
      permOwnerBean.setSetProperty(true) ;
      permOwnerBean.setRemove(true) ;
      permBeans.add(permOwnerBean);
    }

    while (keysIter.hasNext()) {
      String userOrGroup = keysIter.next();
      List<String> permissions = permsMap.get(userOrGroup);
      PermissionBean permBean = new PermissionBean();
      permBean.setUsersOrGroups(userOrGroup);
      for (String perm : permissions) {
        if (PermissionType.READ.equals(perm))
          permBean.setRead(true);
        else if (PermissionType.ADD_NODE.equals(perm))
          permBean.setAddNode(true);
        else if (PermissionType.SET_PROPERTY.equals(perm))
          permBean.setSetProperty(true);
        else if (PermissionType.REMOVE.equals(perm))
          permBean.setRemove(true);
      }
      permBeans.add(permBean);
      sizeOfListPermission = permBeans.size() + iSystemOwner;
    }
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class);
    ListAccess<PermissionBean> permList = new ListAccessImpl<PermissionBean>(PermissionBean.class,
                                                                             permBeans);
    LazyPageList<PermissionBean> dataPageList = new LazyPageList<PermissionBean>(permList, 10);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
    if (currentPage > uiGrid.getUIPageIterator().getAvailablePage())
      uiGrid.getUIPageIterator().setCurrentPage(uiGrid.getUIPageIterator().getAvailablePage());
    else
      uiGrid.getUIPageIterator().setCurrentPage(currentPage);
  }
  static public class EditActionListener extends EventListener<UIPermissionInfo> {
    public void execute(Event<UIPermissionInfo> event) throws Exception {
      UIPermissionInfo uicomp = event.getSource() ;
      String name = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIJCRExplorer uiJCRExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class) ;
      ExtendedNode node = (ExtendedNode)uiJCRExplorer.getCurrentNode() ;
      UIPermissionForm uiForm = uicomp.getAncestorOfType(UIPermissionManager.class).getChild(UIPermissionForm.class) ;
      uiForm.fillForm(name, node) ;
      uiForm.lockForm(name.equals(uicomp.getExoOwner(node)));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
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
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
      uiJCRExplorer.updateAjax(event) ;
    }
  }

  public class PermissionBean {

    private String usersOrGroups ;
    private boolean read ;
    private boolean addNode ;
    private boolean setProperty ;
    private boolean remove ;

    public String getUsersOrGroups() { return usersOrGroups ; }
    public void setUsersOrGroups(String s) { usersOrGroups = s ; }

    public boolean isAddNode() { return addNode ; }
    public void setAddNode(boolean b) { addNode = b ; }

    public boolean isRead() { return read ; }
    public void setRead(boolean b) { read = b ; }

    public boolean isRemove() { return remove ; }
    public void setRemove(boolean b) { remove = b ; }

    public boolean isSetProperty() { return setProperty ; }
    public void setSetProperty(boolean b) { setProperty = b ; }
  }

  public static String[] getPERMISSION_ACTION() {
    return PERMISSION_ACTION;
  }
  public static void setPERMISSION_ACTION(String[] permission_action) {
    PERMISSION_ACTION = permission_action;
  }
  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode);
  }
  public void setCurrentNode(Node currentNode) {
    this.currentNode = NodeLocation.getNodeLocationByNode(currentNode);
  }
  public int getSizeOfListPermission() {
    return sizeOfListPermission;
  }
  public void setSizeOfListPermission(int sizeOfListPermission) {
    this.sizeOfListPermission = sizeOfListPermission;
  }
}

