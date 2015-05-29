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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.bean.PermissionBean;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

public abstract class UIPermissionInfoBase extends UIContainer {

  protected static final String FIELD_NAME = "fieldName";
  
  protected static final String FIELD_VALUE = "fieldValue";
  
  protected static final String EDIT_ACTION = "Edit";
  
  private static final String PERMISSION_ADD_NODE_ACTION = "addNode";
  
  public static String[] PERMISSION_BEAN_FIELD = 
    {"usersOrGroups", PermissionType.READ, PERMISSION_ADD_NODE_ACTION, PermissionType.REMOVE};

  private static String[] PERMISSION_ACTION = {"Delete"} ;
  
  private int sizeOfListPermission = 0;

  public UIPermissionInfoBase() throws Exception {
    UIGrid uiGrid = createUIComponent(UIPermissionInfoGrid.class, null, "PermissionInfo") ;
    addChild(uiGrid) ;
    uiGrid.getUIPageIterator().setId("PermissionInfoIterator");
    uiGrid.configure("usersOrGroups", PERMISSION_BEAN_FIELD, PERMISSION_ACTION) ;
  }
  
  public abstract Node getCurrentNode() throws Exception;
  
  public static String[] getPERMISSION_ACTION() {
    return PERMISSION_ACTION;
  }
  
  public static void setPERMISSION_ACTION(String[] permission_action) {
    PERMISSION_ACTION = permission_action;
  }

  public int getSizeOfListPermission() {
    return sizeOfListPermission;
  }
  public void setSizeOfListPermission(int sizeOfListPermission) {
    this.sizeOfListPermission = sizeOfListPermission;
  }

  public void updateGrid(int currentPage) throws Exception {
    List<PermissionBean> permBeans = new ArrayList<PermissionBean>();
    ExtendedNode node = (ExtendedNode) this.getCurrentNode();
    Map<String, List<String>> permsMap = this.getPermissionsMap(node);
  
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
      permOwnerBean.setRemove(true) ;
      permBeans.add(permOwnerBean);
    }

    while (keysIter.hasNext()) {
      String userOrGroup = keysIter.next();
      PermissionBean permBean = new PermissionBean();
      permBean.setUsersOrGroups(userOrGroup);
      // owner always has full right even if it has been modified in GUI
      if (owner.equals(userOrGroup)) {
        permBean.setRead(true);
        permBean.setAddNode(true);
        permBean.setRemove(true);
      } else {
        List<String> permissions = permsMap.get(userOrGroup);
        for (String perm : permissions) {
          if (PermissionType.READ.equals(perm))
            permBean.setRead(true);
          else if (PermissionType.ADD_NODE.equals(perm))
            permBean.setAddNode(true);
          else if (PermissionType.REMOVE.equals(perm))
            permBean.setRemove(true);
        }
      }
      permBeans.add(permBean);
      sizeOfListPermission = permBeans.size() + iSystemOwner;
    }
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class);
    
    // Sort by user/group
    Collections.sort(permBeans, new PermissionBeanComparator());
    
    ListAccess<PermissionBean> permList = new ListAccessImpl<PermissionBean>(PermissionBean.class,
                                                                             permBeans);
    LazyPageList<PermissionBean> dataPageList = new LazyPageList<PermissionBean>(permList, 10);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
    if (currentPage > uiGrid.getUIPageIterator().getAvailablePage())
      uiGrid.getUIPageIterator().setCurrentPage(uiGrid.getUIPageIterator().getAvailablePage());
    else
      uiGrid.getUIPageIterator().setCurrentPage(currentPage);
  }
  
  protected String  getExoOwner(Node node) throws Exception {
    return Utils.getNodeOwner(node) ;
  }
  
  /**
   * Get permission Map of specific node.
   * 
   * @param node
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<String>> getPermissionsMap(ExtendedNode node) throws RepositoryException {
    Map<String, List<String>> permsMap = new HashMap<String, List<String>>();
    Iterator<AccessControlEntry> permissionEntriesIter = node.getACL().getPermissionEntries().iterator();
    while(permissionEntriesIter.hasNext()) {
      AccessControlEntry accessControlEntry = permissionEntriesIter.next();
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

    return permsMap;
  }
  
  public List<PermissionBean> getPermBeans() {
    return new ArrayList<PermissionBean>();
  }
  
  static public class EditActionListener extends EventListener<UIPermissionInfoBase> {
    public void execute(Event<UIPermissionInfoBase> event) throws Exception {
      UIPermissionInfoBase uiPermissionInfo = event.getSource();
      UIApplication uiApp = uiPermissionInfo.getAncestorOfType(UIApplication.class);
      WebuiRequestContext requestContext = event.getRequestContext();
      ExtendedNode node = (ExtendedNode)uiPermissionInfo.getCurrentNode();
      
      // Get selected user/Group
      String userOrGroupId = requestContext.getRequestParameter(OBJECTID);
      // Changed permission value
      String selectedPermission = requestContext.getRequestParameter(FIELD_NAME);
      String selectedPermissionValue = requestContext.getRequestParameter(FIELD_VALUE);
      
      if (node == null) {
        List<PermissionBean> perBeans = uiPermissionInfo.getPermBeans();
        for (PermissionBean perm : perBeans) {
          if (perm.getUsersOrGroups().equals(userOrGroupId)) {
            if (PermissionType.READ.equals(selectedPermission) && !perm.isAddNode() && !perm.isRemove()) {
              if (Boolean.FALSE.toString().equals(selectedPermissionValue)) {
                  uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.checkbox-require", null,
                                                          ApplicationMessage.WARNING));
                  return;
              }
              perm.setRead("true".equals(selectedPermissionValue));
            } else if (PERMISSION_ADD_NODE_ACTION.equals(selectedPermission) || 
                        PermissionType.SET_PROPERTY.equals(selectedPermission)) {
              perm.setAddNode("true".equals(selectedPermissionValue));
              if (perm.isAddNode()) perm.setRead(true); 
            } else if (PermissionType.REMOVE.equals(selectedPermission)) {
              perm.setRemove("true".equals(selectedPermissionValue));
              if (perm.isRemove()) perm.setRead(true);
            }
          }
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPermissionInfo);
        return;
      }

      if (!node.isCheckedOut()) {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null,
            ApplicationMessage.WARNING));
        return;
      }
      
      // Current node permissions
      Map<String, List<String>> permsMap = uiPermissionInfo.getPermissionsMap(node);
      // Current user/group permissions
      List<String> identityPermissions = permsMap.get(userOrGroupId);
      //
      org.exoplatform.wcm.webui.Utils.addLockToken(node);
      try {
        // Change permission
        if (PermissionUtil.canChangePermission(node)) {
          if (node.canAddMixin("exo:privilegeable")){
            node.addMixin("exo:privilegeable");
            node.setPermission(Utils.getNodeOwner(node),PermissionType.ALL);
          }
          if (Boolean.valueOf(selectedPermissionValue)) {
            if (PERMISSION_ADD_NODE_ACTION.equals(selectedPermission)) {
              identityPermissions.add(PermissionType.SET_PROPERTY);
              identityPermissions.add(PermissionType.ADD_NODE);
            } else {
              identityPermissions.add(selectedPermission);
            }
          } else {
            // Do not allow remove when only one permission type exist
            if (identityPermissions.size() == 1) {
              uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.checkbox-require", null,
                                                      ApplicationMessage.WARNING));
              return;
            }
            if (PERMISSION_ADD_NODE_ACTION.equals(selectedPermission)) {
              identityPermissions.remove(PermissionType.SET_PROPERTY);
              identityPermissions.remove(PermissionType.ADD_NODE);
            } else if (PermissionType.REMOVE.equals(selectedPermission)) {
              identityPermissions.remove(selectedPermission);
            }
          }
          node.setPermission(userOrGroupId, identityPermissions.toArray(new String[identityPermissions.size()]));
        }
        
        UIPermissionFormBase uiPermissionForm =
            uiPermissionInfo.getAncestorOfType(UIPermissionManagerBase.class).getChild(UIPermissionFormBase.class);
        uiPermissionForm.updateSymlinks(node);
        node.getSession().save();
        uiPermissionInfo.updateGrid(uiPermissionInfo.getChild(UIGrid.class).getUIPageIterator().getCurrentPage());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPermissionInfo);
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null, ApplicationMessage.WARNING));
      } catch (AccessControlException accessControlException) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.access-denied", null, ApplicationMessage.WARNING));
      }
    }
  }

  public class PermissionBeanComparator implements Comparator<PermissionBean> {
    public int compare(PermissionBean o1, PermissionBean o2) throws ClassCastException {
      try {
        String name1 = o1.getUsersOrGroups();
        String name2 = o2.getUsersOrGroups();
        return name1.compareToIgnoreCase(name2);
      } catch(Exception e) {
        return 0;
      }
    }
  }
}

