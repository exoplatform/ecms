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
package org.exoplatform.ecm.webui.component.admin.taxonomy.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig (listeners = UIPermissionInfo.DeleteActionListener.class,
                    confirm = "UIPermissionInfo.msg.confirm-delete-permission"),
      @EventConfig (listeners = UIPermissionInfo.EditActionListener.class)
    }
)

public class UIPermissionInfo extends UIContainer {

  public static String[] PERMISSION_BEAN_FIELD = {"usersOrGroups", "read", "addNode",
    "setProperty", "remove"} ;
  private static String[] PERMISSION_ACTION = {"Edit", "Delete"} ;

  private NodeLocation currentNode = null;
  private int sizeOfListPermission = 0;
  public UIPermissionInfo() throws Exception {
    UIGrid uiGrid = createUIComponent(UIGrid.class, null, "PermissionInfo") ;
    addChild(uiGrid) ;
    uiGrid.getUIPageIterator().setId("PermissionInfoIterator");
    uiGrid.configure("usersOrGroups", PERMISSION_BEAN_FIELD, PERMISSION_ACTION) ;
  }
  private String  getExoOwner(Node node) throws Exception {
    return Utils.getNodeOwner(node) ;
  }
  public void updateGrid() throws Exception {
    List<PermissionBean> permBeans = new ArrayList<PermissionBean>();
    ExtendedNode node = (ExtendedNode) getCurrentNode();
    List permsList = node.getACL().getPermissionEntries() ;
    Map<String, List<String>> permsMap = new HashMap<String, List<String>>() ;
    Iterator perIter = permsList.iterator() ;
    while(perIter.hasNext()) {
      AccessControlEntry accessControlEntry = (AccessControlEntry)perIter.next() ;
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
    Set keys = permsMap.keySet();
    Iterator keysIter = keys.iterator() ;
    //TODO Utils.getExoOwner(node) has exception return SystemIdentity.SYSTEM
    String owner = IdentityConstants.SYSTEM ;
    int iSystemOwner = 0;
    if (getExoOwner(node) != null) owner = getExoOwner(node);
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
    while(keysIter.hasNext()) {
      String userOrGroup = (String) keysIter.next();
      List<String> permissions = permsMap.get(userOrGroup);
      PermissionBean permBean = new PermissionBean();
      permBean.setUsersOrGroups(userOrGroup);
      for(String perm : permissions) {
        if(PermissionType.READ.equals(perm)) permBean.setRead(true);
        else if(PermissionType.ADD_NODE.equals(perm)) permBean.setAddNode(true);
        else if(PermissionType.SET_PROPERTY.equals(perm)) permBean.setSetProperty(true);
        else if(PermissionType.REMOVE.equals(perm)) permBean.setRemove(true);
      }
      permBeans.add(permBean);
    }
    sizeOfListPermission = permBeans.size() + iSystemOwner;
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class) ;
    ListAccess<PermissionBean> permList = new ListAccessImpl<PermissionBean>(PermissionBean.class,
                                                                             permBeans);
    LazyPageList<PermissionBean> dataPageList = new LazyPageList<PermissionBean>(permList, 10);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
  }
  private Session getSession() throws Exception {
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class) ;
    String systemWorkspace = repositoryService.getCurrentRepository().getConfiguration().getSystemWorkspaceName();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(systemWorkspace, manageableRepository);
    return session;
  }
  static public class EditActionListener extends EventListener<UIPermissionInfo> {
    public void execute(Event<UIPermissionInfo> event) throws Exception {
      UIPermissionInfo uicomp = event.getSource() ;
      String name = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node updateNode = uicomp.getCurrentNode();
      ExtendedNode node = (ExtendedNode)updateNode;
      UIPermissionForm uiForm = uicomp.getAncestorOfType(UIPermissionManager.class).getChild(UIPermissionForm.class) ;
      uiForm.fillForm(name, node) ;
      uiForm.lockForm(name.equals(uicomp.getExoOwner(node)));
    }
  }
  static public class DeleteActionListener extends EventListener<UIPermissionInfo> {
    public void execute(Event<UIPermissionInfo> event) throws Exception {
      UIPermissionInfo uicomp = event.getSource() ;
      Node currentNode = uicomp.getCurrentNode();
      ExtendedNode node = (ExtendedNode)currentNode;
      String owner = IdentityConstants.SYSTEM ;
      int iSystemOwner = 0;
      if (uicomp.getExoOwner(node) != null) owner = uicomp.getExoOwner(node);
      if (owner.equals(IdentityConstants.SYSTEM)) iSystemOwner = -1;
      String name = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class) ;
      if (uicomp.getSizeOfListPermission() < 2 + iSystemOwner) {
          uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.no-permission-remove",
              null, ApplicationMessage.WARNING));
          
          return;
      }
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
          node.save();
        }
        try {
          node.removePermission(name);
          node.save();
        } catch(AccessDeniedException ace) {
          uicomp.getSession().refresh(false) ;
          uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.access-denied", null,
                                                  ApplicationMessage.WARNING)) ;
          
          return ;
        }
        Session session = uicomp.getSession();
        session.save();
        session.refresh(false);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIPermissionInfo.msg.no-permission-tochange", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      }
      UIPopupContainer uiPopup = uicomp.getAncestorOfType(UIPopupContainer.class) ;
      if(!PermissionUtil.canRead(node)) {
        uiPopup.deActivate() ;
      } else {
        uicomp.updateGrid();
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp.getParent()) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
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

