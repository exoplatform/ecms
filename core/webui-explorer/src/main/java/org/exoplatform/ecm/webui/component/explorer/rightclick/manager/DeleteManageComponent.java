/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.exoplatform.ecm.webui.component.explorer.UIConfirmMessage;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanDeleteNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotMandatoryChildNode;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
                 events = {
                     @EventConfig(listeners = DeleteManageComponent.DeleteActionListener.class)
                 }
    )

public class DeleteManageComponent extends UIAbstractManagerComponent {

  private static final Log LOG = ExoLogger.getLogger(DeleteManageComponent.class.getName());

  private static final String DELETE_FILE_CONFIRM_TITLE = "UIDeleteFileConfirmMessage";
  private static final String DELETE_FOLDER_CONFIRM_TITLE = "UIDeleteFolderConfirmMessage";
  private static final String DELETE_ITEMS_CONFIRM_TITLE = "UIDeleteItemsConfirmMessage";

  private static final int GENERIC_TYPE = 1;
  private static final int FILE_TYPE = 2;
  private static final int FOLDER_TYPE = 3;


  private static final int FOLDERS = 1; //"001";
  private static final int FILES = 2; //"010";
  private static final int FILES_AND_FOLDERS = 3; //"011";
  private static final int GENERIC = 4; //"100";
  private static final int GENERICS_AND_FOLDERS = 5; //"101";
  private static final int GENERICS_AND_FILES = 6; //"110";
  private static final int GENERICS_AND_FILES_AND_FOLDERS = 7; //"111";

  private static final List<UIExtensionFilter> FILTERS

      = Arrays.asList(new UIExtensionFilter[]{new IsNotLockedFilter(),
                                              new CanDeleteNodeFilter(),
                                              new IsNotTrashHomeNodeFilter(),
                                              new IsNotEditingDocumentFilter(),
                                              new IsNotMandatoryChildNode()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private String processRemoveMultiple(String[] nodePaths, Event<?> event) throws Exception {
     StringBuilder trashId = new StringBuilder();
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    Arrays.sort(nodePaths,Collections.reverseOrder());
    for (int i = 0; i < nodePaths.length ; i++) {
      try {
        Node node = this.getNodeByPath(nodePaths[i]);
        Validate.isTrue(node != null, "The ObjectId is invalid '" + nodePaths[i] + "'");
        trashId.append(processRemoveOrMoveToTrash(nodePaths[i], node, event, true, true)).append(";");
      } catch (PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
      }
    }

    return trashId.substring(0,trashId.length() - 1);
  }

  private void removeAuditForNode(Node node) throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    ManageableRepository repository = uiExplorer.getRepository();
    SessionProvider sessionProvider = uiExplorer.getSystemProvider();
    Session session = null;
    session = sessionProvider.getSession(node.getSession().getWorkspace().getName(), repository);
    if (session.getRootNode().hasNode("exo:audit") &&
        session.getRootNode().getNode("exo:audit").hasNode(node.getUUID())) {
      session.getRootNode().getNode("exo:audit").getNode(node.getUUID()).remove();
      session.save();
    }
  }

  /**
   * Remove or MoveToTrash
   *
   * @param nodePath
   * @param node
   * @param event
   * @param isMultiSelect
   * @param checkToMoveToTrash
   * @return
   *  0: node removed
   * -1: move to trash failed
   * trashId: moved to trash successfully
   * @throws Exception
   */
  private String processRemoveOrMoveToTrash(String nodePath,
                                          Node node,
                                          Event<?> event,
                                          boolean isMultiSelect,
                                          boolean checkToMoveToTrash)
                                              throws Exception {
    String trashId="-1";
    if (!checkToMoveToTrash || Utils.isInTrash(node)) {
      processRemoveNode(nodePath, node, event, isMultiSelect);
      return "0";
    }else {
      trashId = moveToTrash(nodePath, node, event, isMultiSelect);
      if (!trashId.equals("-1")) {
        //Broadcast the event when delete folder, in case deleting file, Thrash service will broadcast event 
        ListenerService listenerService =  WCMCoreUtils.getService(ListenerService.class);

        TrashService trashService = WCMCoreUtils.getService(TrashService.class);
        node = trashService.getNodeByTrashId(trashId);
        if(!isDocumentNodeType(node) 
        		&& !node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)){
          Queue<Node> queue = new LinkedList<Node>();
          queue.add(node);

          //Broadcast event to remove file activities
          Node tempNode = null;
          try {
            while (!queue.isEmpty()) {
              tempNode = queue.poll();
              if (isDocumentNodeType(tempNode) || tempNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
                listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, tempNode.getParent(), tempNode);
              } else {
                for (NodeIterator iter = tempNode.getNodes(); iter.hasNext(); ) {
                  Node childNode = iter.nextNode();
                  if(isDocumentNodeType(childNode) || childNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED) || 
                      childNode.isNodeType(NodetypeConstant.NT_FOLDER))
                    queue.add(childNode);
                }
              }
            }
          } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(e.getMessage());
            }
          }
        }
      }
    }
    return trashId;
  }

  /**
   * Move Node to Trash
   * Return -1: move failed
   * Return trashId: move successfully with trashId
   * @param srcPath
   * @param node
   * @param event
   * @param isMultiSelect
   * @return
   * @throws Exception
   */
  private String moveToTrash(String srcPath, Node node, Event<?> event, boolean isMultiSelect) throws Exception {
    TrashService trashService = WCMCoreUtils.getService(TrashService.class);
    AuditService auditService = WCMCoreUtils.getService(AuditService.class);
    boolean ret = true;
    String trashId="-1";
    final String virtualNodePath = srcPath;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    try {
      uiExplorer.addLockToken(node);
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return trashId;
    }

    try {
      if (node.isLocked()) {
        LockUtil.removeLock(node);
        node.unlock();
      }
      //remove audit relations 
      if(auditService.hasHistory(node)) {
        auditService.removeHistory(node);
      }
      //remove mixin auditable
      if( node.isNodeType(Utils.EXO_AUDITABLE)){
        node.removeMixin(Utils.EXO_AUDITABLE);
      }
      node.save();
      //remove all relations that refer to this node
      RelationsService relationService = uiApp.getApplicationComponent(RelationsService.class) ;
      PropertyIterator iter = node.getReferences();
      while (iter.hasNext()) {
        Node refNode = iter.nextProperty().getParent();
        relationService.removeRelation(refNode, node.getPath());
      }

      if (!node.isCheckedOut())
        throw new VersionException("node is locked, can't move to trash node :" + node.getPath());
      if (!PermissionUtil.canRemoveNode(node))
        throw new AccessDeniedException("access denied, can't move to trash node:" + node.getPath());
      SessionProvider sessionProvider = uiExplorer.getSessionProvider();
      Node currentNode = uiExplorer.getCurrentNode();

      try {
        trashId = trashService.moveToTrash(node, sessionProvider);
      } catch (PathNotFoundException ex) {
        ret = false;
      }
      String currentPath = LinkUtils.getExistPath(currentNode, uiExplorer.getCurrentPath());
      uiExplorer.setCurrentPath(currentPath);
      uiExplorer.updateAjax(event);

    } catch (LockException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("node is locked, can't move to trash node :" + node.getPath());
      }
      ApplicationMessage appMessage =
          new ApplicationMessage("UIPopupMenu.msg.can-not-remove-locked-node",
                                 new String[] {node.getPath()}, ApplicationMessage.ERROR);
      appMessage.setArgsLocalized(false);
      uiApp.addMessage(appMessage);
      uiExplorer.updateAjax(event);
      ret = false;
    } catch (VersionException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("node is checked in, can't move to trash node:" + node.getPath());
      }
      removeMixinEXO_RESTORE_LOCATION(node);
      ApplicationMessage appMessage =
          new ApplicationMessage("UIPopupMenu.msg.can-not-remove-checked-in-node",
                                 new String[] {node.getPath()}, ApplicationMessage.ERROR);
      appMessage.setArgsLocalized(false);
      uiApp.addMessage(appMessage);
      uiExplorer.updateAjax(event);
      ret = false;
    } catch (AccessDeniedException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("access denied, can't move to trash node:" + node.getPath());
      }
      ApplicationMessage appMessage =
          new ApplicationMessage("UIPopupMenu.msg.access-denied-to-delete",
                                 new String[] {node.getPath()}, ApplicationMessage.ERROR);
      appMessage.setArgsLocalized(false);
      uiApp.addMessage(appMessage);
      uiExplorer.updateAjax(event);
      ret = false;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs", e);
      }
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.unexpected-error",
                                              new String[] {node.getPath()}, ApplicationMessage.ERROR));
      uiExplorer.updateAjax(event);
      ret = false;
    }

    if (!isMultiSelect) {
      if (uiExplorer.getCurrentPath().equals(virtualNodePath))
        uiExplorer.setSelectNode(LinkUtils.getParentPath(virtualNodePath));
      else
        uiExplorer.setSelectNode(uiExplorer.getCurrentPath());
    }
    return (ret)?trashId:"-1";
  }

  private void processRemoveNode(String nodePath, Node node, Event<?> event, boolean isMultiSelect)
      throws Exception {
    final String virtualNodePath = nodePath;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    Node currentNode = uiExplorer.getCurrentNode();
    Session session = node.getSession();
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    try {
      uiExplorer.addLockToken(node);
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    Node parentNode = node.getParent();
    uiExplorer.addLockToken(parentNode);
    try {

      // If node has taxonomy
      TaxonomyService taxonomyService = uiExplorer.getApplicationComponent(TaxonomyService.class);
      List<Node> listTaxonomyTrees = taxonomyService.getAllTaxonomyTrees();
      List<Node> listExistedTaxonomy = taxonomyService.getAllCategories(node);
      for (Node existedTaxonomy : listExistedTaxonomy) {
        for (Node taxonomyTrees : listTaxonomyTrees) {
          if(existedTaxonomy.getPath().contains(taxonomyTrees.getPath())) {
            taxonomyService.removeCategory(node, taxonomyTrees.getName(),
                                           existedTaxonomy.getPath().substring(taxonomyTrees.getPath().length()));
            break;
          }
        }
      }

      ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
      actionService.removeAction(node, uiExplorer.getRepositoryName());
      ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
      thumbnailService.processRemoveThumbnail(node);
      NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);

      newFolksonomyService.removeTagsOfNodeRecursively(node,
                                                       uiExplorer.getRepository()
                                                       .getConfiguration()
                                                       .getDefaultWorkspaceName(),
                                                       WCMCoreUtils.getRemoteUser(),
                                                       getGroups());
      //trashService.removeRelations(node, uiExplorer.getSystemProvider(), uiExplorer.getRepositoryName());
      if (PermissionUtil.canRemoveNode(node) && node.isNodeType(Utils.EXO_AUDITABLE)) {
        removeAuditForNode(node);
      }
      //Remove symlinks
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
      if(!node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        for(Node symlink : linkManager.getAllLinks(node, NodetypeConstant.EXO_SYMLINK)) {
          symlink.remove();
          symlink.getSession().save();
        }
      }
      node.remove();
      parentNode.getSession().save();
    } catch (VersionException ve) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.remove-verion-exception", null,
                                              ApplicationMessage.WARNING));

      uiExplorer.updateAjax(event);
      return;
    } catch (ReferentialIntegrityException ref) {
      session.refresh(false);
      uiExplorer.refreshExplorer();
      uiApp
      .addMessage(new ApplicationMessage(
                                         "UIPopupMenu.msg.remove-referentialIntegrityException", null,
                                         ApplicationMessage.WARNING));

      uiExplorer.updateAjax(event);
      return;
    } catch (ConstraintViolationException cons) {
      session.refresh(false);
      uiExplorer.refreshExplorer();
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.constraintviolation-exception",
                                              null, ApplicationMessage.WARNING));

      uiExplorer.updateAjax(event);
      return;
    } catch (LockException lockException) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-other-person", null,
                                              ApplicationMessage.WARNING));

      uiExplorer.updateAjax(event);
      return;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("an unexpected error occurs while removing the node", e);
      }
      JCRExceptionManager.process(uiApp, e);

      return;
    }
    if (!isMultiSelect) {
      if (currentNode.getPath().equals(virtualNodePath))
        uiExplorer.setSelectNode(LinkUtils.getParentPath(virtualNodePath));
      else
        uiExplorer.setSelectNode(currentNode.getPath());
    }
  }

  private void processRemoveMultiple(String[] nodePaths, String[] wsNames, Event<?> event)
      throws Exception {
    for (int i = 0; i < nodePaths.length; i++) {
      processRemove(nodePaths[i], wsNames[i], event, true);
    }
  }

  private void processRemove(String nodePath, String wsName, Event<?> event, boolean isMultiSelect)
      throws Exception {
    if (wsName == null) {
      wsName = getDefaultWorkspace();
    }
    doDelete(wsName.concat(":").concat(nodePath), event);
  }

  private String getDefaultWorkspace() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    return uiExplorer.getCurrentDriveWorkspace();
  }

  public void doDelete(String nodePath, String wsName, Event<?> event) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (nodePath.indexOf(";") > -1) {
      processRemoveMultiple(nodePath.split(";"), wsName.split(";"), event);
    } else {
      processRemove(nodePath, wsName, event, false);
    }
    uiExplorer.updateAjax(event);
    uiExplorer.getSession().save();
  }

  public void doDeleteWithoutTrash(String nodePath, Event<?> event) throws Exception {
    doDelete(nodePath, event, false);
  }

  public void doDelete(String nodePath, Event<?> event) throws Exception {
    doDelete(nodePath, event, true);
  }

  public void doDelete(String nodePath, Event<?> event, boolean checkToMoveToTrash) throws Exception {
    RequestContext context = RequestContext.getCurrentInstance();
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIWorkingArea uiWorkingArea = getAncestorOfType(UIWorkingArea.class);
    ResourceBundle res = context.getApplicationResourceBundle();
    String deleteNotice = "";
    String deleteNoticeParam = "";
    String trashId = "";
    if (nodePath.indexOf(";") > -1) {
      trashId = processRemoveMultiple(Utils.removeChildNodes(nodePath), event);
      if(checkToMoveToTrash) deleteNotice = "UIWorkingArea.msg.feedback-delete-multi";
      else deleteNotice = "UIWorkingArea.msg.feedback-delete-permanently-multi";
      deleteNoticeParam = String.valueOf(nodePath.split(";").length);
    } else {
      UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
      // Prepare to remove
      try {
        Node node = this.getNodeByPath(nodePath);
        if(checkToMoveToTrash) deleteNotice = "UIWorkingArea.msg.feedback-delete";
        else deleteNotice = "UIWorkingArea.msg.feedback-delete-permanently";
        deleteNoticeParam = StringEscapeUtils.unescapeHtml(Utils.getTitle(node));
        if (node != null) {
          trashId = processRemoveOrMoveToTrash(node.getPath(), node, event, false, checkToMoveToTrash);
        }
      } catch (PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
    deleteNotice = res.getString(deleteNotice);
    deleteNotice = deleteNotice.replace("{" + 0 + "}", deleteNoticeParam);
    deleteNotice = deleteNotice.replace("\"", "'");
    deleteNotice = StringEscapeUtils.escapeHtml(deleteNotice);
    if(checkToMoveToTrash) {
      String undoLink = getUndoLink(trashId);
      uiWorkingArea.setDeleteNotice(deleteNotice);
      uiWorkingArea.setNodePathDelete(undoLink);
    } else {
      uiWorkingArea.setWCMNotice(deleteNotice);
    }
    uiExplorer.updateAjax(event);
    uiExplorer.getSession().save();
  }
  /**
   * Get undo link to restore nodes that deleted
   *
   * @param trashId node path of nodes want to restore
   * @throws Exception
   */
  private String getUndoLink(String trashId) throws Exception {
    String undoLink = "";
    TrashService trashService = WCMCoreUtils.getService(TrashService.class);
    
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    PortletPreferences portletPrefs = uiExplorer.getPortletPreferences();

    String trashWorkspace = portletPrefs.getValue(Utils.TRASH_WORKSPACE, "");
    StringBuffer sb = new StringBuffer();
    if (trashId.indexOf(";") > -1) {
      String[] nodePaths = trashId.split(";");
      for(int i=0; i<nodePaths.length; i++) {        
        trashId = nodePaths[i].substring(nodePaths[i].indexOf(":") + 1, nodePaths[i].length());
        sb.append(trashWorkspace).append(":").append(trashService.getNodeByTrashId(trashId).getPath()).append(";");
      }
      undoLink = sb.toString();
      if(undoLink.length() > 0) undoLink = undoLink.substring(0,undoLink.length()-1);
    } else {
      trashId = trashId.substring(trashId.indexOf(":") + 1, trashId.length());
      Node tmpNode = trashService.getNodeByTrashId(trashId);
      sb.append(tmpNode.getPath()).append(";");
      undoLink = sb.toString();
      if(undoLink.length() > 0) {
        undoLink = undoLink.substring(0,undoLink.length()-1);
        undoLink =  trashWorkspace + ":" +undoLink;
      }
    }
    return undoLink;
  }

  private boolean isInTrashFolder(String nodePath) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    String wsName = null;
    Session session = null;
    String[] nodePaths = nodePath.split(";");
    for(int i=0; i<nodePaths.length; i++) {
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePaths[i]);
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
        session = uiExplorer.getSessionByWorkspace(wsName);
        Node node = uiExplorer.getNodeByPath(nodePath, session, false);
        return Utils.isInTrash(node);
      }
    }
    return false;
  }

  private boolean isDocumentNodeType(Node node) throws Exception {
    boolean isDocument = true;
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    isDocument = templateService.getAllDocumentNodeTypes().contains(node.getPrimaryNodeType().getName());
    return isDocument;
  }
  /**
   * Get the content type of one node
   *
   * @param nodePath node path of one node
   * @throws Exception
   */
  private int getContentType(String nodePath) throws Exception {
    int content_type = 1;
    Node node = getNodeByPath(nodePath);
    String primaryType = node.getPrimaryNodeType().getName();
    if(node.isNodeType(NodetypeConstant.NT_FILE)) content_type = 2;
    else if (primaryType.equals(NodetypeConstant.NT_FOLDER) || primaryType.equals(NodetypeConstant.NT_UNSTRUCTURED))
      content_type = 3;
    else content_type = 1;
    return content_type;
  }
  /**
   * Get the content type of multiple nodes
   *
   * @param nodePath node path of multiple nodes
   * @throws Exception
   */
  private int getMultiContentType(String nodePath) throws Exception {
    StringBuffer sBuffer = new StringBuffer();
    String[] nodePaths = nodePath.split(";");
    boolean isGeneric = false;
    boolean isFile = false;
    boolean isFolder = false;

    for(int i=0; i<nodePaths.length; i++) {
      Node node = getNodeByPath(nodePaths[i]);
      String primaryType = node.getPrimaryNodeType().getName();
      if(node.isNodeType(NodetypeConstant.NT_FILE)) isFile = true;
      else if (primaryType.equals(NodetypeConstant.NT_FOLDER) || primaryType.equals(NodetypeConstant.NT_UNSTRUCTURED))
        isFolder = true;
      else isGeneric = true;
    }
    if(isGeneric) sBuffer.append("1");
    else sBuffer.append("0");

    if(isFile) sBuffer.append("1");
    else sBuffer.append("0");

    if(isFolder) sBuffer.append("1");
    else sBuffer.append("0");

    return Integer.parseInt(sBuffer.toString(),2);
  }

  public static void deleteManage(Event<? extends UIComponent> event) throws Exception {
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    UIConfirmMessage uiConfirmMessage = uiWorkingArea.createUIComponent(UIConfirmMessage.class, null, null);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    DeleteManageComponent deleteManageComponent = uiWorkingArea.getChild(DeleteManageComponent.class);

    //get nodes that have relations referring to them
    List<String> listNodesHaveRelations = null;
    try {
      listNodesHaveRelations = checkRelations(nodePath, uiExplorer);
    } catch (PathNotFoundException pathEx) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null, ApplicationMessage.WARNING));

      return;
    }

    boolean isInTrashFolder = deleteManageComponent.isInTrashFolder(nodePath);
    uiConfirmMessage.setNodeInTrash(isInTrashFolder);
    String nodeName = nodePath;
    int contentType = 1;
    int multiContentType = 1;
    String message_key = "";
    // Check and set the title for Delete Confirmation Dialog
    if(nodePath.indexOf(";") > 0) {
      uiConfirmMessage.setId(DELETE_ITEMS_CONFIRM_TITLE);
      multiContentType = deleteManageComponent.getMultiContentType(nodePath);
    } else {
      Node node = deleteManageComponent.getNodeByPath(nodePath);
      if(node != null)
        nodeName = StringEscapeUtils.unescapeHtml(Utils.getTitle(node));
      contentType = deleteManageComponent.getContentType(nodePath);
      if(contentType == FILE_TYPE)
        uiConfirmMessage.setId(DELETE_FILE_CONFIRM_TITLE);
      else if (contentType == FOLDER_TYPE)
        uiConfirmMessage.setId(DELETE_FOLDER_CONFIRM_TITLE);
    }

    //show confirm message
    if (listNodesHaveRelations != null && listNodesHaveRelations.size() > 0) { 
      // there are some nodes which have relations referring to them
      // in the deleting node list
      // build node list to string to add into the confirm message
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < listNodesHaveRelations.size(); i++) {
        sb.append("'").append(listNodesHaveRelations.get(i)).append("', ");
      }
      //remove "," character at the end of string

      // Node has relations means it is not in Trash folder and is not Folder
      if (nodePath.indexOf(";") < 0) { // in case: delete one node that has relations
        if(contentType == GENERIC_TYPE)
          message_key = "UIWorkingArea.msg.confirm-delete-has-relations";
        else if(contentType == FILE_TYPE)
          message_key = "UIWorkingArea.msg.confirm-delete-file-has-relations";
        else if (contentType == FOLDER_TYPE) {
          message_key = "UIWorkingArea.msg.confirm-delete-folder-has-relations";
        }
        uiConfirmMessage.setMessageKey(message_key);
        uiConfirmMessage.setArguments(new String[] { nodeName });
      } else { // in case: delete multiple node have relations

        switch(multiContentType) {
        case FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-folder-have-relations"; break;
        case FILES: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file-have-relations"; break;
        case FILES_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file-and-folder-have-relations";
        break;
        case GENERIC: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-have-relations"; break;
        case GENERICS_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and" +
            "-folder-have-relations"; break;
        case GENERICS_AND_FILES: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-" +
            "and-file-have-relations"; break;
        case GENERICS_AND_FILES_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-" +
            "and-file-and-folder-have-relations"; break;
        default: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-have-relations"; break;
        }

        uiConfirmMessage.setMessageKey(message_key);
        uiConfirmMessage.setArguments(new String[] { Integer.toString(nodePath.split(";").length) });
      }

    } else {  //there isn't any node which has relations referring to it in the deleting node list
      if (isInTrashFolder) {
        if (nodePath.indexOf(";") > -1) { // delete multi

          switch(multiContentType) {
          case FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-folder-permanently"; break;
          case FILES: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file-permanently"; break;
          case FILES_AND_FOLDERS: 
            message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file-and-folder-permanently"; break;
          case GENERIC: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-permanently"; break;
          case GENERICS_AND_FOLDERS: 
            message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-folder-permanently"; break;
          case GENERICS_AND_FILES: 
            message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-file-permanently"; break;
          case GENERICS_AND_FILES_AND_FOLDERS: 
            message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-file-and-folder-permanently"; break;
          default: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-permanently"; break;
          }

          uiConfirmMessage.setMessageKey(message_key);
          uiConfirmMessage.setArguments(new String[] { Integer.toString(nodePath.split(";").length) });
        } else { // delete one
          if(contentType == GENERIC_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete-permanently";
          else if(contentType == FILE_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete-file-permanently";
          else if(contentType == FOLDER_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete-folder-permanently";
          uiConfirmMessage.setMessageKey(message_key);
          uiConfirmMessage.setArguments(new String[] { nodeName });
        }
      } else {
        if (nodePath.indexOf(";") > -1) { // delete multi

          switch(multiContentType) {
          case FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-folder"; break;
          case FILES: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file"; break;
          case FILES_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-file-and-folder"; break;
          case GENERIC: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic"; break;
          case GENERICS_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-folder"; break;
          case GENERICS_AND_FILES: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-file"; break;
          case GENERICS_AND_FILES_AND_FOLDERS: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic-and-file-" +
              "and-folder"; break;
          default: message_key = "UIWorkingArea.msg.confirm-delete-multi-nodes-generic"; break;
          }
          uiConfirmMessage.setMessageKey(message_key);
          uiConfirmMessage.setArguments(new String[] { Integer.toString(nodePath.split(";").length) });
        } else { // delete one
          if(contentType == GENERIC_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete";
          else if(contentType == FILE_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete-file";
          else if(contentType == FOLDER_TYPE)
            message_key = "UIWorkingArea.msg.confirm-delete-folder";
          uiConfirmMessage.setMessageKey(message_key);
          uiConfirmMessage.setArguments(new String[] { nodeName });
        }
      }
    }

    uiConfirmMessage.setNodePath(nodePath);
    UIPopupWindow popUp = uiExplorer.getChild(UIPopupWindow.class);
    popUp.setUIComponent(uiConfirmMessage);
    popUp.setShowMask(true);
    popUp.setShow(true);
    event.getRequestContext().addUIComponentToUpdateByAjax(popUp);

  }

  /**
   * This function uses to get a node list that have relations referring to them in the deleting node list
   *
   * @param nodePath The list of nodes that user wants to delete
   * @param uiExplorer uiExplorer
   * @return The list of nodes that have relations referring to them
   * @throws Exception
   */
  private static List<String> checkRelations(String nodePath, UIJCRExplorer uiExplorer) throws Exception{

    Node node = null;
    String wsName = null;
    Session session = null;
    String[] nodePaths = nodePath.split(";");
    RelationsService rlService = WCMCoreUtils.getService(RelationsService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    List<String> listNodesHaveRelations = new ArrayList<String>();
    for (int i = 0; i < nodePaths.length; i++) {
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePaths[i]);
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
        session = uiExplorer.getSessionByWorkspace(wsName);
        node = uiExplorer.getNodeByPath(nodePath, session, false);
        if (rlService.getRelations(node, sessionProvider).size()>0) {
          //check references
          listNodesHaveRelations.add(nodePath);
        }
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
      }
    }
    return listNodesHaveRelations;
  }

  /**
   * Get node by node path.
   *
   * @param nodePath node path of specific node with syntax [workspace:node path]
   * @return Node of specific node nath
   * @throws Exception
   */
  private Node getNodeByPath(String nodePath) throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    if (!matcher.find()) return null;
    String wsName = matcher.group(1);
    nodePath = matcher.group(2);
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    return uiExplorer.getNodeByPath(nodePath, session, false);
  }

  private String getGroups() throws Exception {
    StringBuilder ret = new StringBuilder();
    for (String group : Utils.getGroups())
      ret.append(group).append(';');
    ret.deleteCharAt(ret.length() - 1);
    return ret.toString();
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

  public static class DeleteActionListener extends UIWorkingAreaActionListener<DeleteManageComponent> {
    public void processEvent(Event<DeleteManageComponent> event) throws Exception {
      deleteManage(event);
    }
  }

  private void removeMixinEXO_RESTORE_LOCATION(Node node) throws Exception {
    if (node.isNodeType(Utils.EXO_RESTORELOCATION)) {
      node.removeMixin(Utils.EXO_RESTORELOCATION);
      node.save();
    }
  }
}
