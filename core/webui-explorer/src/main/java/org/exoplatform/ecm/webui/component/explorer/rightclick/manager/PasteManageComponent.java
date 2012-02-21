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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.model.ClipboardCommand;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsPasteableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
    events = {
      @EventConfig(listeners = PasteManageComponent.PasteActionListener.class)
    }
)

public class PasteManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                              new IsPasteableFilter(),
                                              new IsNotTrashHomeNodeFilter()});

  private static final String RELATION_PROP = "exo:relation";

  private static final Log    LOG           = ExoLogger.getLogger(PasteManageComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void pasteManage(Event<PasteManageComponent> event, UIJCRExplorer uiExplorer)
      throws Exception {
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    String destPath = event.getRequestContext().getRequestParameter(OBJECTID);
    String nodePath = null;
    Session session = null;
    if (destPath != null) {
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
      String wsName = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
        session = uiExplorer.getSessionByWorkspace(wsName);
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
      }
    }
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    if (uiExplorer.getAllClipBoard().size() < 1) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.no-node", null,
          ApplicationMessage.WARNING));
      
      return;
    }
    Node destNode;
    try {
      // Use the method getNodeByPath because it is link aware
      destNode = destPath == null ? uiExplorer.getCurrentNode() : uiExplorer.getNodeByPath(
          nodePath, session);
      // Reset the session to manage the links that potentially change of
      // workspace
      session = destNode.getSession();
      if (destPath == null) {
        destPath = session.getWorkspace().getName() + ":" + destNode.getPath();
      }
    } catch (PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
          ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    if (!PermissionUtil.canAddNode(destNode)) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.can-not-paste-node", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    }
    if (uiExplorer.nodeIsLocked(destNode)) {
      Object[] arg = { destPath };
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg,
          ApplicationMessage.WARNING));
      
      return;
    }
    if (!destNode.isCheckedOut()) {
      uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.node-checkedin", null));
      
      return;
    }

    try {
      if (uiWorkingArea.getVirtualClipboards().isEmpty()) {
        processPaste(uiExplorer.getAllClipBoard().getLast(), destPath, event);
      } else {
        processPasteMultiple(destPath, event, uiExplorer);
      }
    } catch (PathNotFoundException pe) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.cannot-readsource", null));
      
      return;
    }
    session.save();
    uiExplorer.updateAjax(event);
  }

  public static void processPaste(ClipboardCommand currentClipboard, String destPath, Event<?> event)
      throws Exception {
    processPaste(currentClipboard, destPath, event, false, true);
  }

  private static void processPasteMultiple(String destPath, Event<?> event, UIJCRExplorer uiExplorer)
      throws Exception {
    int pasteNum = 0;
    UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
    List<ClipboardCommand> virtualClipboards = uiWorkingArea.getVirtualClipboards();
    for (ClipboardCommand clipboard : virtualClipboards) {
      pasteNum++;
      if (pasteNum == virtualClipboards.size()) {
        processPaste(clipboard, destPath, event, true, true);
        break;
      }
      processPaste(clipboard, destPath, event, true, false);
    }
  }

  /**
   * Update clipboard after CUT node. Detain PathNotFoundException with same name sibling node
   * @param clipboardCommands
   * @param mapClipboard
   * @throws Exception
   */
  private static void updateClipboard(List<ClipboardCommand> clipboardCommands,
                                      Map<ClipboardCommand, Node> mapClipboard) throws Exception {
    Node srcNode;
    for (ClipboardCommand clipboard : clipboardCommands) {
      if (ClipboardCommand.CUT.equals(clipboard.getType())) {
        srcNode = mapClipboard.get(clipboard);
        srcNode.refresh(true);
        clipboard.setSrcPath(srcNode.getPath());
      }
    }
  }

  /**
   * Put data from clipboard to Map<Clipboard, Node>. After cutting node, we keep data to update clipboard by respective node
   * @param clipboardCommands
   * @param uiExplorer
   * @return
   * @throws Exception
   */
  private static Map<ClipboardCommand, Node> parseToMap(List<ClipboardCommand> clipboardCommands,
                                                        UIJCRExplorer uiExplorer) throws Exception {
    String srcPath;
    String type;
    String srcWorkspace;
    Node srcNode;
    Session srcSession;
    Map<ClipboardCommand, Node> mapClipboard = new HashMap<ClipboardCommand, Node>();
    for (ClipboardCommand clipboard : clipboardCommands) {
      srcPath = clipboard.getSrcPath();
      type = clipboard.getType();
      srcWorkspace = clipboard.getWorkspace();
      if (ClipboardCommand.CUT.equals(type)) {
        srcSession = uiExplorer.getSessionByWorkspace(srcWorkspace);
        // Use the method getNodeByPath because it is link aware
        srcNode = uiExplorer.getNodeByPath(srcPath, srcSession, false);
        clipboard.setSrcPath(srcNode.getPath());
        mapClipboard.put(clipboard, srcNode);
      }
    }
    return mapClipboard;
  }

  private static void processPaste(ClipboardCommand currentClipboard, String destPath,
      Event<?> event, boolean isMultiSelect, boolean isLastPaste) throws Exception {
    UIJCRExplorer uiExplorer = ((UIComponent) event.getSource())
        .getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    String srcPath = currentClipboard.getSrcPath();
    String type = currentClipboard.getType();
    String srcWorkspace = currentClipboard.getWorkspace();
    Session srcSession = uiExplorer.getSessionByWorkspace(srcWorkspace);
    // Use the method getNodeByPath because it is link aware
    Node srcNode = uiExplorer.getNodeByPath(srcPath, srcSession, false);
    // Reset the path to manage the links that potentially create virtual path
    srcPath = srcNode.getPath();
    // Reset the session to manage the links that potentially change of workspace
    srcSession = srcNode.getSession();
    // Reset the workspace name to manage the links that potentially change of
    // workspace
    srcWorkspace = srcSession.getWorkspace().getName();
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
    String destWorkspace = null;
    if (matcher.find()) {
      destWorkspace = matcher.group(1);
      destPath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
    }
    Session destSession = uiExplorer.getSessionByWorkspace(destWorkspace);

    // Use the method getNodeByPath because it is link aware
    Node destNode = uiExplorer.getNodeByPath(destPath, destSession);

    // Reset the path to manage the links that potentially create virtual path
    destPath = destNode.getPath();
    // Reset the session to manage the links that potentially change of
    // workspace
    destSession = destNode.getSession();
    if (ClipboardCommand.CUT.equals(type) && srcPath.equals(destPath)) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-cutting", null,
          ApplicationMessage.WARNING));
      
      return;
    }
    // Make destination path without index on final name
    if (!"/".equals(destPath))
      destPath = destPath.concat("/");
    destPath = destPath.concat(srcNode.getName());
    ActionServiceContainer actionContainer = uiExplorer
        .getApplicationComponent(ActionServiceContainer.class);
    try {
      if (ClipboardCommand.COPY.equals(type)) {
        pasteByCopy(destSession, srcWorkspace, srcPath, destPath);
        Node selectedNode = (Node) destSession.getItem(destPath);
        actionContainer.initiateObservation(selectedNode, uiExplorer.getRepositoryName());
      } else {
        pasteByCut(currentClipboard, uiExplorer, destSession, srcWorkspace, srcPath, destPath,
            actionContainer, uiExplorer.getRepositoryName(), isMultiSelect, isLastPaste);
      }
    } catch (ConstraintViolationException ce) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.current-node-not-allow-paste", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch (VersionException ve) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.copied-node-in-versioning", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch (ItemExistsException iee) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.paste-node-same-name", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch (LoginException e) {
      if (ClipboardCommand.CUT.equals(type)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.cannot-login-node", null,
            ApplicationMessage.WARNING));
        
        uiExplorer.updateAjax(event);
        return;
      }
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.cannot-paste-nodetype", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch (AccessDeniedException ace) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.access-denied", null,
          ApplicationMessage.WARNING));
      
      uiExplorer.updateAjax(event);
      return;
    } catch (LockException locke) {
      Object[] arg = { srcPath };
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.paste-lock-exception", arg,
          ApplicationMessage.WARNING));
      
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
      return;
    }
  }

  private static void removeReferences(Node destNode) throws Exception {
    NodeType[] mixinTypes = destNode.getMixinNodeTypes();
    Session session = destNode.getSession();
    for (int i = 0; i < mixinTypes.length; i++) {
      if (mixinTypes[i].getName().equals(Utils.EXO_CATEGORIZED)
          && destNode.hasProperty(Utils.EXO_CATEGORIZED)) {
        Node valueNode = null;
        Value valueAdd = session.getValueFactory().createValue(valueNode);
        destNode.setProperty(Utils.EXO_CATEGORIZED, new Value[] { valueAdd });
      }
    }
    destNode.save();
  }

  private static void pasteByCopy(Session session, String srcWorkspaceName, String srcPath,
      String destPath) throws Exception {
    Workspace workspace = session.getWorkspace();
    if (workspace.getName().equals(srcWorkspaceName)) {
      workspace.copy(srcPath, destPath);
      Node destNode = (Node) session.getItem(destPath);
      removeReferences(destNode);
    } else {
      try {
        if (LOG.isDebugEnabled())
          LOG.debug("Copy to another workspace");
        workspace.copy(srcWorkspaceName, srcPath, destPath);
        // workspace.clone(srcWorkspaceName, srcPath, destPath, true);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs while pasting the node", e);
        }
        if (LOG.isDebugEnabled())
          LOG.debug("Copy to other workspace by clone");
        try {
          workspace.clone(srcWorkspaceName, srcPath, destPath, false);
        } catch (Exception f) {
          if (LOG.isErrorEnabled()) {
            LOG.error("an unexpected error occurs while pasting the node", f);
          }
        }
      }
    }
  }

  private static void pasteByCut(ClipboardCommand currentClipboard, UIJCRExplorer uiExplorer,
      Session session, String srcWorkspace, String srcPath, String destPath,
      ActionServiceContainer actionContainer, String repository, boolean isMultiSelect,
      boolean isLastPaste) throws Exception {
    UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
    Workspace workspace = session.getWorkspace();
    if (workspace.getName().equals(srcWorkspace)) {
      if (srcPath.equals(destPath))
        return;
    }
    List<ClipboardCommand> allClipboard = uiExplorer.getAllClipBoard();
    List<ClipboardCommand> virtualClipboard = uiWorkingArea.getVirtualClipboards();
    Map<ClipboardCommand, Node> mapAllClipboardNode = parseToMap(allClipboard, uiExplorer);
    Map<ClipboardCommand, Node> mapVirtualClipboardNode = parseToMap(virtualClipboard, uiExplorer);

    RelationsService relationsService = uiExplorer.getApplicationComponent(RelationsService.class);
    List<Node> refList = new ArrayList<Node>();
    boolean isReference = false;
    PropertyIterator references = null;
    Node srcNode = (Node) uiExplorer.getSessionByWorkspace(srcWorkspace).getItem(srcPath);
    try {
      references = srcNode.getReferences();
      isReference = true;
    } catch (Exception e) {
      isReference = false;
    }
    if (isReference && references != null) {
      if (references.getSize() > 0) {
        while (references.hasNext()) {
          Property pro = references.nextProperty();
          Node refNode = pro.getParent();
          if (refNode.hasProperty(RELATION_PROP)) {
            relationsService.removeRelation(refNode, srcPath);
            refNode.save();
            refList.add(refNode);
          }
        }
      }
    }
    // Add locked token for the source node
    uiExplorer.addLockToken(srcNode);
    if (workspace.getName().equals(srcWorkspace)) {
      try {
        workspace.move(srcPath, destPath);
        LockUtil.changeLockToken(srcPath, (Node)session.getItem(destPath));
        session.save();
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new MessageException(new ApplicationMessage("UIPopupMenu.msg.bound-exception", null,
            ApplicationMessage.WARNING));
      }
      if (!isMultiSelect || (isMultiSelect && isLastPaste)) {
        Node desNode = null;
        try {
          desNode = (Node) session.getItem(destPath);
        } catch (PathNotFoundException pathNotFoundException) {
          uiExplorer.setCurrentPath(LinkUtils.getParentPath(uiExplorer.getCurrentPath()));
          desNode = uiExplorer.getCurrentNode();
        } catch (ItemNotFoundException itemNotFoundException) {
          uiExplorer.setCurrentPath(LinkUtils.getParentPath(uiExplorer.getCurrentPath()));
          desNode = uiExplorer.getCurrentNode();
        }

        if (!session.itemExists(uiExplorer.getCurrentPath())) {
          uiExplorer.setCurrentPath(LinkUtils.getParentPath(uiExplorer.getCurrentPath()));
        }

        if (!(desNode.getPath().equals(uiExplorer.getCurrentPath())))
          actionContainer.initiateObservation(desNode, repository);
        for (int i = 0; i < refList.size(); i++) {
          Node addRef = refList.get(i);
          relationsService.addRelation(addRef, destPath, session.getWorkspace().getName());
          addRef.save();
        }
        uiWorkingArea.getVirtualClipboards().clear();
        Node currentNode = uiExplorer.getCurrentNode();
        String realCurrentPath = currentNode.getPath();
        if (srcWorkspace.equals(currentNode.getSession().getWorkspace().getName())
            && (srcPath.equals(realCurrentPath) || realCurrentPath.startsWith(srcPath))) {
          uiExplorer.setCurrentPath(LinkUtils.getParentPath(uiExplorer.getCurrentPath()));
        }
      }
    } else {
      workspace.clone(srcWorkspace, srcPath, destPath, false);
      if (!isMultiSelect || (isMultiSelect && isLastPaste)) {
        uiWorkingArea.getVirtualClipboards().clear();
      }
    }
    session.save();
    uiExplorer.getAllClipBoard().remove(currentClipboard);
    updateClipboard(uiWorkingArea.getVirtualClipboards(), mapVirtualClipboardNode);
    updateClipboard(uiExplorer.getAllClipBoard(), mapAllClipboardNode);
  }

  public static class PasteActionListener extends UIWorkingAreaActionListener<PasteManageComponent> {
    public void processEvent(Event<PasteManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      pasteManage(event, uiExplorer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
