/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.connector.collaboration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Duy Dong dongpd@exoplatform.com
 * Sep 18, 2012
 */
@Path("/contents/rename/")
public class RenameConnector implements ResourceContainer {

  private static final Log     LOG                      = ExoLogger.getLogger(RenameConnector.class.getName());

  private static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(/.*)");

  private static final String  RELATION_PROP            = "exo:relation";

  /**
   * Get <em>objectid</em> of renamed node <br>
   * Basically <em>objectid</em> is a partern which is useful to find html tag of specific node <br>
   * <em>objectid</em> actually is node path encoded by URLEncoder. <br>
   *
   * @param nodePath a node path
   * @return <em>objectid</em>
   * @throws Exception
   */
  @GET
  @Path("/getObjectId/")
  public Response getObjectId(@QueryParam("nodePath") String nodePath) throws Exception {
    return Response.ok(Utils.getObjectId(nodePath), MediaType.TEXT_PLAIN).build();
  }

  /**
   * Call RenameConnector rest service to execute rename process.
   *
   * @param oldPath old path of renamed node with syntax: [workspace:node path]
   * @param newTitle new Title of node
   * @return Httpstatus 400 if rename fail, otherwise uuid of renamed node is returned
   * @throws Exception
   */
  @GET
  @Path("/rename/")
  @RolesAllowed("users")
  public Response rename(@QueryParam("oldPath") String oldPath,
                         @QueryParam("newTitle") String newTitle) throws Exception {
    try {
      // Check and escape newTitle
      if (StringUtils.isBlank(newTitle)) {
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }
      String newExoTitle = Text.escapeIllegalJcrChars(newTitle);      
      // Clarify new name & check to add extension
      String newName = Text.escapeIllegalJcrChars(org.exoplatform.services.cms.impl.Utils.cleanString(newTitle));
      Node renamedNode = this.getNodeByPath(oldPath);
      String oldName = renamedNode.getName();
      if (oldName.indexOf('.') != -1 && renamedNode.isNodeType(NodetypeConstant.NT_FILE)) {
        String ext = oldName.substring(oldName.lastIndexOf('.'));
        newName += ext;
        newExoTitle += ext;
      }

      // Stop process if new name and exo:title is the same with old one
      String oldExoTitle = (renamedNode.hasProperty("exo:title")) ? renamedNode.getProperty("exo:title")
                                                                               .getString()
                                                                 : StringUtils.EMPTY;
      CmsService cmsService = WCMCoreUtils.getService(CmsService.class);
      cmsService.getPreProperties().clear();
      String nodeUUID = "";
      if(renamedNode.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) nodeUUID = renamedNode.getUUID();
      cmsService.getPreProperties().put(nodeUUID + "_" + "exo:title", oldExoTitle);
      
      if (renamedNode.getName().equals(newName) && oldExoTitle.equals(newExoTitle)) {
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }

      // Check if can edit locked node
      if (!this.canEditLockedNode(renamedNode)) {
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }

      // Get uuid
      if (renamedNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)) {
        renamedNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
        renamedNode.save();
      }
      String uuid = renamedNode.getUUID();

      // Only execute renaming if name is changed
      Session nodeSession = renamedNode.getSession();
      if (!renamedNode.getName().equals(newName)) {
        // Backup relations pointing to the rename node
        List<Node> refList = new ArrayList<Node>();
        PropertyIterator references = renamedNode.getReferences();
        RelationsService relationsService = WCMCoreUtils.getService(RelationsService.class);
        while (references.hasNext()) {
          Property pro = references.nextProperty();
          Node refNode = pro.getParent();
          if (refNode.hasProperty(RELATION_PROP)) {
            refList.add(refNode);
            relationsService.removeRelation(refNode, renamedNode.getPath());
          }
        }

        // Change name
        Node parent = renamedNode.getParent();
        String srcPath = renamedNode.getPath();
        String destPath = (parent.getPath().equals("/") ? StringUtils.EMPTY : parent.getPath()) + "/"
            + newName;

        this.addLockToken(renamedNode.getParent());
        nodeSession.getWorkspace().move(srcPath, destPath);

        // Update renamed node
        Node destNode = nodeSession.getNodeByUUID(uuid);

        // Restore relation to new name node
        for (Node addRef : refList) {
          relationsService.addRelation(addRef, destNode.getPath(), nodeSession.getWorkspace().getName());
        }

        // Update lock after moving
        if (destNode.isLocked()) {
          WCMCoreUtils.getService(LockService.class).changeLockToken(renamedNode, destNode);
        }
        this.changeLockForChild(srcPath, destNode);

        // Mark rename node as modified
        if (destNode.canAddMixin("exo:modify")) {
          destNode.addMixin("exo:modify");
        }
        destNode.setProperty("exo:lastModifier", nodeSession.getUserID());
        
        // Update exo:name
        if(renamedNode.canAddMixin("exo:sortable")) {
          renamedNode.addMixin("exo:sortable");
        }
        renamedNode.setProperty("exo:name", renamedNode.getName());

        renamedNode = destNode;
      }

      // Change title
      if (!renamedNode.hasProperty("exo:title")) {
        renamedNode.addMixin(NodetypeConstant.EXO_RSS_ENABLE);
      }
      renamedNode.setProperty("exo:title", newExoTitle);

      nodeSession.save();

      return Response.ok(uuid).build();
    } catch (Exception e) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Rename is not successful!");
      }
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
  }

  /**
   * Update lock for child nodes after renaming.
   *
   * @param srcPath source path
   * @param parentNewNode parent new node
   * @throws Exception
   */
  private void changeLockForChild(String srcPath, Node parentNewNode) throws Exception {
    if(parentNewNode.hasNodes()) {
      NodeIterator newNodeIter = parentNewNode.getNodes();
      String newSRCPath = null;
      while(newNodeIter.hasNext()) {
        Node newChildNode = newNodeIter.nextNode();
        newSRCPath = newChildNode.getPath().replace(parentNewNode.getPath(), srcPath);
        if(newChildNode.isLocked()) WCMCoreUtils.getService(LockService.class).changeLockToken(newSRCPath, newChildNode);
        if(newChildNode.hasNodes()) changeLockForChild(newSRCPath, newChildNode);
      }
    }
  }

  /**
   * Check if can edit a locked node.
   *
   * @param node a specific node
   * @return true if can edit, false otherwise
   * @throws Exception
   */
  private boolean canEditLockedNode(Node node) throws Exception {
    LockService lockService = WCMCoreUtils.getService(LockService.class);
    if(!node.isLocked()) return true;
    String lockToken = lockService.getLockTokenOfUser(node);
    if(lockToken != null) {
      node.getSession().addLockToken(lockService.getLockToken(node));
      return true;
    }
    return false;
  }

  /**
   * Add lock token of a specific node to it's session.
   *
   * @param node a specific node
   * @throws Exception
   */
  private void addLockToken(Node node) throws Exception {
    if (node.isLocked()) {
      String lockToken = WCMCoreUtils.getService(LockService.class).getLockToken(node);
      if(lockToken != null) {
        node.getSession().addLockToken(lockToken);
      }
    }
  }

  /**
   * Get node by node path.
   *
   * @param nodePath node path of specific node with syntax [workspace:node
   *          path]
   * @return Node of specific node path
   * @throws Exception
   */
  private Node getNodeByPath(String nodePath) throws Exception {
    Matcher matcher = RenameConnector.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    if (!matcher.find())
      return null;
    String wsName = matcher.group(1);
    nodePath = matcher.group(2);
    Session session = this.getSession(wsName);
    return (Node) session.getItem(Text.escapeIllegalJcrChars(nodePath));
  }

  /**
   * Get user session from specific workspace.
   *
   * @param workspaceName
   * @return session
   * @throws Exception
   */
  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    return sessionProvider.getSession(workspaceName, WCMCoreUtils.getRepository());
  }
}
