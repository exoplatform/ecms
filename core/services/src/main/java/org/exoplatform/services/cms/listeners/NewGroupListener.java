/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.cms.listeners;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.Permission;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 11:13:25 AM
 */
public class NewGroupListener extends GroupEventListener
{

   /** The log. */
   private static final Log LOG = ExoLogger.getLogger(NewGroupListener.class.getName());
  
   private HierarchyConfig config_;

   private RepositoryService jcrService_;

   private String groupsPath_;

   final static private String NT_UNSTRUCTURED = "nt:unstructured";

   final static private String GROUPS_PATH = "groupsPath";

   public NewGroupListener(RepositoryService jcrService, NodeHierarchyCreator nodeHierarchyCreatorService,
      InitParams params) throws Exception
   {
      jcrService_ = jcrService;
      config_ = params.getObjectParamValues(HierarchyConfig.class).get(0);
      groupsPath_ = nodeHierarchyCreatorService.getJcrPath(GROUPS_PATH);
   }

   public void preSave(Group group, boolean isNew) throws Exception
   {
     buildGroupStructure(group);
   }

   public void preDelete(Group group)
   {
     try
     {
        removeGroup(group.getId());
     }
     catch (RepositoryException e)
     {
       if (LOG.isWarnEnabled()) {
         LOG.warn(e.getMessage());
       }
     }
   }

   private void removeGroup(String groupId) throws RepositoryException
   {
      ManageableRepository manageableRepository = jcrService_.getCurrentRepository();
      String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = manageableRepository.getSystemSession(systemWorkspace);
      Node groupNode = (Node)session.getItem(groupsPath_ + groupId);
      groupNode.remove();
      session.save();
      session.logout();
   }

   @SuppressWarnings("unchecked")
   private void buildGroupStructure(Group group) throws Exception
   {
      ManageableRepository manageableRepository = jcrService_.getCurrentRepository();
      String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = manageableRepository.getSystemSession(systemWorkspace);
      try {
        Node groupsHome = (Node)session.getItem(groupsPath_);
        List jcrPaths = config_.getJcrPaths();
        String groupId = group.getId();
        String groupLabel = group.getLabel();
        
        Node groupNode = null;
        try
        {
           groupNode = groupsHome.getNode(groupId.substring(1, groupId.length()));
        }
        catch (PathNotFoundException e)
        {
           OrganizationService orgService = ExoContainerContext.getService(OrganizationService.class);
           String parentId = group.getParentId();
           if (parentId == null) {
             String[] groupIdParts = group.getId().split("/");
             if (groupIdParts.length > 2) {
               parentId = group.getId().substring(0, group.getId().lastIndexOf('/'));
             }
           }
           if (parentId != null) {
             Group parentGroup = orgService.getGroupHandler().findGroupById(parentId);
             if (parentGroup != null && !groupsHome.hasNode(parentGroup.getId().substring(1, parentGroup.getId().length()))) {
               buildGroupStructure(parentGroup);
             }
           }
           groupNode = groupsHome.addNode(groupId.substring(1, groupId.length()));
        }
        if (groupNode.canAddMixin(NodetypeConstant.EXO_DRIVE)) {
          groupNode.addMixin(NodetypeConstant.EXO_DRIVE);
        }
        groupNode.setProperty(NodetypeConstant.EXO_LABEL, groupLabel);
        for (JcrPath jcrPath : (List<JcrPath>)jcrPaths)
        {
           createNode(groupNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(), getPermissions(
              jcrPath.getPermissions(), groupId));
        }
        session.save();
      } finally {
        session.logout();
      }
   }

   @SuppressWarnings("unchecked")
   private void createNode(Node groupNode, String path, String nodeType, List<String> mixinTypes, Map permissions)
      throws Exception
   {
      if (nodeType == null || nodeType.length() == 0)
         nodeType = NT_UNSTRUCTURED;
      try
      {
         groupNode = groupNode.getNode(path);
      }
      catch (PathNotFoundException e)
      {
         groupNode = groupNode.addNode(path, nodeType);
      }
      if (groupNode.canAddMixin("exo:privilegeable"))
         groupNode.addMixin("exo:privilegeable");
      if (permissions != null && !permissions.isEmpty())
         ((ExtendedNode)groupNode).setPermissions(permissions);
      if (mixinTypes.size() > 0)
      {
         for (String mixin : mixinTypes)
         {
            if (groupNode.canAddMixin(mixin))
               groupNode.addMixin(mixin);
         }
      }
   }

   private Map getPermissions(List<Permission> permissions, String groupId)
   {
      Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
      String groupIdentity = "*:".concat(groupId);
      permissionsMap.put(groupIdentity, PermissionType.ALL);
      for (Permission permission : permissions)
      {
         StringBuilder strPer = new StringBuilder();
         if ("true".equals(permission.getRead()))
            strPer.append(PermissionType.READ);
         if ("true".equals(permission.getAddNode()))
            strPer.append(",").append(PermissionType.ADD_NODE);
         if ("true".equals(permission.getSetProperty()))
            strPer.append(",").append(PermissionType.SET_PROPERTY);
         if ("true".equals(permission.getRemove()))
            strPer.append(",").append(PermissionType.REMOVE);
         permissionsMap.put(permission.getIdentity(), strPer.toString().split(","));
      }
      return permissionsMap;
   }
}
