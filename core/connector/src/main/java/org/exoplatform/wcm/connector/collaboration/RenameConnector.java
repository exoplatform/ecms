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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The RenameConnector aims to enhance the use of the _rename_ action on the Sites Explorer.
 * The system allows setting two values: _name_ and _title_.
 * The _title_ is a logical name that is used to display in the Sites Explorer.
 * The _name_ is the technical name of the file at JCR level. It is notably exposed via the WEBDAV layer.
 *
 * @LevelAPI Experimental
 *
 * @anchor RenameConnector
 */
@Path("/contents/rename/")
public class RenameConnector implements ResourceContainer {

  private static final Log     LOG                      = ExoLogger.getLogger(RenameConnector.class.getName());

  private static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(/.*)");

  private static final String  RELATION_PROP            = "exo:relation";
  
  private static final String DEFAULT_NAME = "untitled";

  /**
   * Gets _objectid_ of the renamed node.
   * Basically, _objectid_ is a pattern which is useful to find HTML tags of a specific node.
   * _objectid_ actually is the node path encoded by _URLEncoder_.
   *
   * @param nodePath The node path
   * @return _objectid_
   * @throws Exception The exception
   *
   * @anchor RenameConnector.getObjectId
   */
  @GET
  @Path("/getObjectId/")
  public Response getObjectId(@QueryParam("nodePath") String nodePath) throws Exception {
    return Response.ok(Utils.getObjectId(nodePath), MediaType.TEXT_PLAIN).build();
  }

  /**
   * Calls RenameConnector REST service to execute the "_rename_" process.
   *
   * @param oldPath The old path of the renamed node with syntax: [workspace:node path]
   * @param newTitle The new title of the node.
   * @return Httpstatus 400 if renaming fails, otherwise the UUID of the renamed node is returned.
   * @throws Exception The exception
   *
   * @anchor RenameConnector.rename
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
      String newExoTitle = newTitle;
      // Clarify new name & check to add extension
      String newName = Text.escapeIllegalJcrChars(newTitle);
      
      // Set default name if new title contain no valid character
      newName = (StringUtils.isEmpty(newName)) ? DEFAULT_NAME : newName;

      // Get renamed node
      String[] workspaceAndPath = parseWorkSpaceNameAndNodePath(oldPath);
      Node renamedNode = (Node)WCMCoreUtils.getService(NodeFinder.class)
              .getItem(this.getSession(workspaceAndPath[0]), workspaceAndPath[1], false);

      String oldName = renamedNode.getName();
      if (oldName.indexOf('.') != -1 && renamedNode.isNodeType(NodetypeConstant.NT_FILE)) {
        String ext = oldName.substring(oldName.lastIndexOf('.'));
        newName = newName.concat(ext);
        newExoTitle = newExoTitle.concat(ext);
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
      
      // Update state of node
      WCMPublicationService publicationService = WCMCoreUtils.getService(WCMPublicationService.class);
      if (publicationService.isEnrolledInWCMLifecycle(renamedNode)) {
        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        listenerService.broadcast(CmsService.POST_EDIT_CONTENT_EVENT, this, renamedNode);
      }

      return Response.ok(uuid).build();
    } catch (LockException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("The node or parent node is locked. Rename is not successful!");
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Rename is not successful!", e);
      } else if (LOG.isWarnEnabled()) {
        LOG.warn("Rename is not successful!");
      }
    }
    return Response.status(HTTPStatus.BAD_REQUEST).build();
  }

  /**
   * Updates lock for child nodes after renaming.
   *
   * @param srcPath The source path.
   * @param parentNewNode The destination node which gets the new name.
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
   * Checks if a locked node is editable or not.
   *
   * @param node A specific node.
   * @return True if the locked node is editable, false otherwise.
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
   * Adds the lock token of a specific node to its session.
   *
   * @param node A specific node.
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
   * Parse node path with syntax [workspace:node path] to workspace name and path separately
   *
   * @param nodePath node path with syntax [workspace:node path]
   * @return array of String. element with index 0 is workspace name, remaining one is node path
   */
  private String[] parseWorkSpaceNameAndNodePath(String nodePath) {
    Matcher matcher = RenameConnector.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    if (!matcher.find())
      return null;
    String[] workSpaceNameAndNodePath = new String[2];
    workSpaceNameAndNodePath[0] = matcher.group(1);
    workSpaceNameAndNodePath[1] = matcher.group(2);
    return workSpaceNameAndNodePath;
  }

  /**
   * Gets user session from a specific workspace.
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
