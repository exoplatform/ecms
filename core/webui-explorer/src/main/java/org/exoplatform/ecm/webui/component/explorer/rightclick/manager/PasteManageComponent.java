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

import org.apache.commons.lang.BooleanUtils;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentAutoVersionForm;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsPasteableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeNodePageIterator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

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
                                              new IsNotNtFileFilter(),
                                              new IsNotTrashHomeNodeFilter()});

  private static final String RELATION_PROP = "exo:relation";

  private static final Log    LOG           = ExoLogger.getLogger(PasteManageComponent.class.getName());
  private static boolean isRefresh = true;
  private static Map<String, Boolean> versionedRemember, nonVersionedRemember;

  public static boolean isIsRefresh() {
    return isRefresh;
  }

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void pasteManage(Event<PasteManageComponent> event, UIJCRExplorer uiExplorer)
      throws Exception {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
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
    if (clipboardService.getClipboardList(userId, false).size() < 1) {
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
    String currentPath = uiExplorer.getCurrentNode().getPath();
    ClipboardCommand clipboardCommand = clipboardService.getLastClipboard(userId);
    try {
      if (clipboardCommand!=null && clipboardService.getClipboardList(userId, true).isEmpty()) {
        processPaste(clipboardCommand, destNode, event, uiExplorer);
      } else {
        if(autoVersionService.isVersionSupport(destNode.getPath(), destNode.getSession().getWorkspace().getName())) {
          processPasteMultiple(destNode, event, uiExplorer);
        }else{
          processPasteMultiple(destPath, event, uiExplorer);
        }
      }
    } catch (PathNotFoundException pe) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.cannot-readsource", null));

      return;
    }
    //session.save();


    // Get paginator of UITreeExplorer && UIDocumentInfo
    UITreeNodePageIterator extendedPageIterator = null;
    UITreeExplorer uiTreeExplorer = uiExplorer.findFirstComponentOfType(UITreeExplorer.class);
    if (uiTreeExplorer != null) {
      extendedPageIterator = uiTreeExplorer.getUIPageIterator(currentPath);
    }
    UIPageIterator contentPageIterator = uiExplorer.findComponentById(UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID);

    // Get current page index
    int currentPage = 1;
    if (contentPageIterator != null) {
      currentPage = contentPageIterator.getCurrentPage();
    }

    if(isRefresh) {
      // Rebuild screen after pasting new content
      versionedRemember = null;
      nonVersionedRemember = null;
      uiExplorer.updateAjax(event);
    }

    // Because after updateAjax, paginator automatically set to first page then we need set again current pageindex
    if (contentPageIterator != null) {
      contentPageIterator.setCurrentPage(currentPage);
    }
    if (extendedPageIterator != null) {
      extendedPageIterator.setCurrentPage(currentPage);
    }
  }

  public static void processPaste(ClipboardCommand clipboardCommand, Node destNode, Event<?> event, UIJCRExplorer uiExplorer)
    throws Exception{
    AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
    Node sourceNode = (Node)uiExplorer.getSessionByWorkspace(clipboardCommand.getWorkspace()).
            getItem(clipboardCommand.getSrcPath());
    if(destNode.hasNode(sourceNode.getName()) && sourceNode.isNodeType(NodetypeConstant.NT_FILE)
            && autoVersionService.isVersionSupport(destNode.getPath(), destNode.getSession().getWorkspace().getName())){
      Set<ClipboardCommand> clipboardCommands = new HashSet<>();
      clipboardCommands.add(clipboardCommand);
      showConfirmDialog(destNode, sourceNode, uiExplorer, clipboardCommand, clipboardCommands, event);
    }else {
      processPaste(clipboardCommand, destNode.getPath(),uiExplorer, event, false, true);
    }
  }
//
//  /**
//   * Event raise from UIClipboard
//   * @param currentClipboard
//   * @param destPath
//   * @param event
//   * @throws Exception
//   */
//  public static void processPaste(ClipboardCommand currentClipboard, String destPath, Event<?> event)
//      throws Exception {
//    UIJCRExplorer uiExplorer = ((UIComponent)event.getSource()).getAncestorOfType(UIJCRExplorer.class);
//    processPaste(currentClipboard, destPath,uiExplorer, event, false, true);
//  }

  private static void processPasteMultiple(String destPath, Event<?> event, UIJCRExplorer uiExplorer)
          throws Exception {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    int pasteNum = 0;
    Set<ClipboardCommand> virtualClipboards = clipboardService.getClipboardList(userId, true);
    for (ClipboardCommand clipboard : virtualClipboards) {
      pasteNum++;
      if (pasteNum == virtualClipboards.size()) {
        processPaste(clipboard, destPath, uiExplorer, event, true, true);
        break;
      }
      processPaste(clipboard, destPath, uiExplorer, event, true, false);
    }
  }

  private static void processPasteMultiple(Node destNode, Event<?> event, UIJCRExplorer uiExplorer)
      throws Exception {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    Set<ClipboardCommand> virtualClipboards = clipboardService.getClipboardList(userId, true);

    processPasteMultiple(destNode, event, uiExplorer, virtualClipboards, null);
  }

  public static void processPasteMultiple(Node destNode, Event<?> event, UIJCRExplorer uiExplorer,
                                          Set<ClipboardCommand> virtualClipboards, String action) throws Exception{
    int pasteNum = 0;
    Set<ClipboardCommand> _virtualClipboards = new HashSet<>(virtualClipboards);
    Set<ClipboardCommand> processList = new HashSet<>(virtualClipboards);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    ActionServiceContainer actionContainer = uiExplorer.getApplicationComponent(ActionServiceContainer.class);
    for (ClipboardCommand clipboard : virtualClipboards) {
      pasteNum++;
      Node srcNode = null;
      try{
        srcNode = (Node)uiExplorer.getSessionByWorkspace(clipboard.getWorkspace()).getItem(clipboard.getSrcPath());
        String destPath = destNode.getPath();
        if(destNode.hasNode(srcNode.getName()) ){
          Node _destNode = destNode.getNode(srcNode.getName());
          if(_destNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE) && versionedRemember!=null){
            if(BooleanUtils.isTrue(versionedRemember.get("keepboth"))) {
              if(ClipboardCommand.COPY.equals(clipboard.getType())) {
                pasteByCopy(destNode.getSession(), clipboard.getWorkspace(),
                        clipboard.getSrcPath(), _destNode.getPath());
              }else{
                pasteByCut(clipboard, uiExplorer, _destNode.getSession(), clipboard.getWorkspace(), clipboard.getSrcPath(),
                        _destNode.getPath(),actionContainer, false, false, false);
              }
            }
            if(BooleanUtils.isTrue(versionedRemember.get("createVersion")))
              makeVersion(destNode, _destNode, srcNode, clipboard, action, destPath, uiExplorer, event);
            processList.remove(clipboard);
            continue;
          }
          if((!_destNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)) && nonVersionedRemember!=null){
            if(BooleanUtils.isTrue(nonVersionedRemember.get("replace"))) {
              //if(ClipboardCommand.CUT.equals(clipboard.getType())) continue;
              String _destPath = _destNode.getPath();
              TrashService trashService = WCMCoreUtils.getService(TrashService.class);
              String trashID = trashService.moveToTrash(_destNode, WCMCoreUtils.getUserSessionProvider());
              UIDocumentAutoVersionForm.copyNode(destNode.getSession(), clipboard.getWorkspace(),
                      clipboard.getSrcPath(), _destPath, uiApp, uiExplorer, event, ClipboardCommand.COPY);
              Node deletedNode = trashService.getNodeByTrashId(trashID);
              deletedNode.remove();
              deletedNode.getSession().save();
            }
            if(BooleanUtils.isTrue(nonVersionedRemember.get("keepboth"))) {
              if (ClipboardCommand.COPY.equals(clipboard.getType())) {
                pasteByCopy(destNode.getSession(), clipboard.getWorkspace(),
                        clipboard.getSrcPath(), _destNode.getPath());
              } else {
                pasteByCut(clipboard, uiExplorer, _destNode.getSession(), clipboard.getWorkspace(), clipboard.getSrcPath(),
                        _destNode.getPath(), actionContainer, false, false, false);
              }
            }
            processList.remove(clipboard);
            continue;
          }
          showConfirmDialog(destNode, srcNode, uiExplorer, clipboard, _virtualClipboards, event);
          break;
        }else{
          _virtualClipboards.remove(clipboard);
          if (pasteNum == virtualClipboards.size()) {
            processPaste(clipboard, destPath, uiExplorer, event, true, true);
            processList.remove(clipboard);
            break;
          }
          processPaste(clipboard, destPath, uiExplorer, event, true, false);
          processList.remove(clipboard);
        }
      }catch (ConstraintViolationException ce) {
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
        processList.remove(clipboard);
        continue;
      } catch (LoginException e) {
        if (ClipboardCommand.CUT.equals(action)) {
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
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        uiExplorer.updateAjax(event);
        return;
      }
    }
    if(processList.isEmpty()){
      UIPopupWindow popupAction = uiExplorer.findFirstComponentOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      uiExplorer.updateAjax(event);
      versionedRemember=null;
      nonVersionedRemember=null;
    }
  }

  private static void makeVersion(Node destNode, Node _destNode, Node srcNode, ClipboardCommand clipboard,
                                  String action, String destPath, UIJCRExplorer uiExplorer, Event<?> event) throws Exception{
    AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
    ActionServiceContainer actionContainer = uiExplorer.getApplicationComponent(ActionServiceContainer.class);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    if(destNode.hasNode(srcNode.getName()) && ClipboardCommand.COPY.equals(clipboard.getType())
            && UIDocumentAutoVersionForm.CREATE_VERSION.equals(action)) {
      autoVersionService.autoVersion(destNode.getNode(srcNode.getName()), srcNode);
    }else if(destNode.hasNode(srcNode.getName()) && ClipboardCommand.COPY.equals(clipboard.getType())
            && UIDocumentAutoVersionForm.KEEP_BOTH.equals(action)){
      UIDocumentAutoVersionForm.copyNode(destNode.getSession(), destNode.getSession().getWorkspace().getName(),
              srcNode.getPath(), destNode.getNode(srcNode.getName()).getPath(), uiApp, uiExplorer, event, ClipboardCommand.COPY);
    }else if(destNode.hasNode(srcNode.getName()) && ClipboardCommand.COPY.equals(clipboard.getType())
            && UIDocumentAutoVersionForm.REPLACE.equals(action)) {
      TrashService trashService = WCMCoreUtils.getService(TrashService.class);
      destPath = _destNode.getPath();
      String trashID = trashService.moveToTrash(_destNode, WCMCoreUtils.getUserSessionProvider());
      UIDocumentAutoVersionForm.copyNode(destNode.getSession(), destNode.getSession().getWorkspace().getName(),
              srcNode.getPath(), destPath, uiApp, uiExplorer, event, ClipboardCommand.COPY);
      Node deletedNode = trashService.getNodeByTrashId(trashID);
      deletedNode.remove();
      deletedNode.getSession().save();
    } else{
      if(UIDocumentAutoVersionForm.KEEP_BOTH.equals(action)){
        if(destNode.hasNode(srcNode.getName())) {
          pasteByCut(clipboard, uiExplorer, _destNode.getSession(), clipboard.getWorkspace(), clipboard.getSrcPath(),
                  _destNode.getPath(),actionContainer, false, false, false);
        }
      }else {
        pasteByCut(clipboard, uiExplorer, destNode.getSession(), clipboard.getWorkspace(), clipboard.getSrcPath(),
                destNode.getPath(), actionContainer, false, false, true);
      }
    }
    isRefresh=true;
  }

  private static void showConfirmDialog(Node destNode, Node srcNode, UIJCRExplorer uiExplorer, ClipboardCommand clipboard,
                                 Set<ClipboardCommand> virtualClipboards, Event<?> event) throws Exception{
    Node destExitedNode = destNode.getNode(srcNode.getName());
    UIPopupContainer objUIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
    UIDocumentAutoVersionForm uiDocumentAutoVersionForm = uiExplorer.createUIComponent(UIDocumentAutoVersionForm.class, null, null);
    uiDocumentAutoVersionForm.setDestPath(destNode.getPath());
    uiDocumentAutoVersionForm.setDestWorkspace(destNode.getSession().getWorkspace().getName());
    uiDocumentAutoVersionForm.setSourcePath(srcNode.getPath());
    uiDocumentAutoVersionForm.setSourceWorkspace(srcNode.getSession().getWorkspace().getName());
    uiDocumentAutoVersionForm.setCurrentClipboard(clipboard);
    uiDocumentAutoVersionForm.setMessage("UIDocumentAutoVersionForm.msg");
    uiDocumentAutoVersionForm.setArguments(new String[]{srcNode.getName()});
    uiDocumentAutoVersionForm.setClipboardCommands(virtualClipboards);
    if(virtualClipboards!=null && virtualClipboards.size()==1) uiDocumentAutoVersionForm.setSingleProcess(true);
    uiDocumentAutoVersionForm.init(destExitedNode);
    objUIPopupContainer.activate(uiDocumentAutoVersionForm, 450, 0);
    event.getRequestContext().addUIComponentToUpdateByAjax(objUIPopupContainer);
    isRefresh = false;
  }
  /**
   * Update clipboard after CUT node. Detain PathNotFoundException with same name sibling node
   * @param clipboardCommands
   * @param mapClipboard
   * @throws Exception
   */
  private static void updateClipboard(Set<ClipboardCommand> clipboardCommands,
                                      Map<ClipboardCommand, Node> mapClipboard) throws Exception {
    Node srcNode;
    for (ClipboardCommand clipboard : clipboardCommands) {
      if (ClipboardCommand.CUT.equals(clipboard.getType())) {
        srcNode = mapClipboard.get(clipboard);
        if(srcNode !=null) {
          srcNode.refresh(true);
          clipboard.setSrcPath(srcNode.getPath());
        }
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
  private static Map<ClipboardCommand, Node> parseToMap(Set<ClipboardCommand> clipboardCommands,
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

  private static void processPaste(ClipboardCommand currentClipboard, String destPath, UIJCRExplorer uiExplorer,
      Event<?> event, boolean isMultiSelect, boolean isLastPaste) throws Exception {
//    UIJCRExplorer uiExplorer = ((UIComponent) event.getSource())
//        .getAncestorOfType(UIJCRExplorer.class);
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

    // Get thumbnail node of source node
    ThumbnailService thumbnailService = WCMCoreUtils.getService(ThumbnailService.class);
    Node srcThumbnailNode = thumbnailService.getThumbnailNode(srcNode);

    // Reset the workspace name to manage the links that potentially change of
    // workspace
    srcWorkspace = srcSession.getWorkspace().getName();
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
    String destWorkspace = null;
    if (matcher.find()) {
      destWorkspace = matcher.group(1);
      destPath = matcher.group(2);
    }
    Session destSession = uiExplorer.getSessionByWorkspace(destWorkspace);
    if(destWorkspace==null) destWorkspace = destSession.getWorkspace().getName();
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
        destNode = (Node) destSession.getItem(destPath);
        actionContainer.initiateObservation(destNode);
      } else {
        pasteByCut(currentClipboard, uiExplorer, destSession, srcWorkspace, srcPath, destPath,
            actionContainer, isMultiSelect, isLastPaste, false);
        destNode = (Node) destSession.getItem(destPath);
      }

      if (!srcWorkspace.equals(destWorkspace) || !srcPath.equals(destPath)) {
        // Update thumbnail for the node after pasting
        thumbnailService.copyThumbnailNode(srcThumbnailNode, destNode);
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

  public static void pasteByCut(ClipboardCommand currentClipboard, UIJCRExplorer uiExplorer,
      Session session, String srcWorkspace, String srcPath, String destPath,
      ActionServiceContainer actionContainer, boolean isMultiSelect,
      boolean isLastPaste, boolean isCreateVersion) throws Exception {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();

    UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
    Workspace workspace = session.getWorkspace();
    if (workspace.getName().equals(srcWorkspace)) {
      if (srcPath.equals(destPath))
        return;
    }
    Set<ClipboardCommand> allClipboard = clipboardService.getClipboardList(userId, false);
    Set<ClipboardCommand> virtualClipboard = clipboardService.getClipboardList(userId, true);
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
    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
    ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
    uiExplorer.addLockToken(srcNode);
    if (workspace.getName().equals(srcWorkspace)) {
      try {
        if(isCreateVersion){
          Node _destNode = ((Node)session.getItem(destPath)).getNode(srcNode.getName());
          AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
          autoVersionService.autoVersion(_destNode, srcNode);
          if(!srcNode.getPath().equals(_destNode.getPath())) {
            srcNode.remove();
//            srcNode.getSession().save();
          }
        }else {
          workspace.move(srcPath, destPath);
        }
        LockUtil.changeLockToken(srcPath, (Node)session.getItem(destPath));
        session.save();
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new MessageException(new ApplicationMessage("UIPopupMenu.msg.node-cutting", null,
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
        if (activityService.isAcceptedNode(desNode) || desNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
          listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode, desNode.getPath());
        }
        if (!(desNode.getPath().equals(uiExplorer.getCurrentPath())))
          actionContainer.initiateObservation(desNode);
        for (int i = 0; i < refList.size(); i++) {
          Node addRef = refList.get(i);
          relationsService.addRelation(addRef, destPath, session.getWorkspace().getName());
          addRef.save();
        }
        clipboardService.clearClipboardList(userId, true);
        Node currentNode = uiExplorer.getCurrentNode();
        String realCurrentPath = currentNode.getPath();
        if (srcWorkspace.equals(currentNode.getSession().getWorkspace().getName())
            && (srcPath.equals(realCurrentPath) || realCurrentPath.startsWith(srcPath))) {
          uiExplorer.setCurrentPath(LinkUtils.getParentPath(uiExplorer.getCurrentPath()));
        }
      }
    } else {
      workspace.clone(srcWorkspace, srcPath, destPath, false);
      Node desNode =(Node) workspace.getSession().getItem(destPath);
      if (activityService.isAcceptedNode(desNode) || desNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, desNode, destPath);
      }
      if (!isMultiSelect || (isMultiSelect && isLastPaste)) {
        clipboardService.clearClipboardList(userId, true);
      }
    }
    session.save();
    clipboardService.getClipboardList(userId, false).remove(currentClipboard);
    updateClipboard(clipboardService.getClipboardList(userId, true), mapVirtualClipboardNode);
    updateClipboard(clipboardService.getClipboardList(userId, false), mapAllClipboardNode);
    clipboardService.clearClipboardList(userId, true);
    clipboardService.clearClipboardList(userId, false);
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

  public static void setVersionedRemember(Map<String, Boolean> versionedRemember) {
    PasteManageComponent.versionedRemember = versionedRemember;
  }

  public static void setNonVersionedRemember(Map<String, Boolean> nonVersionedRemember) {
    PasteManageComponent.nonVersionedRemember = nonVersionedRemember;
  }

  public static Map<String, Boolean> getVersionedRemember() {
    return versionedRemember;
  }
}
