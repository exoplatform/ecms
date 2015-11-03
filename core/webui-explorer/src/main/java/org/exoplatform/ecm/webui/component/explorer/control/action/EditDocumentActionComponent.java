/*
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
 */
package org.exoplatform.ecm.webui.component.explorer.control.action;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsEditableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotContainBinaryFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIActionContainer;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIActionForm;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIActionTypeForm;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
                 events = {
                     @EventConfig(listeners = EditDocumentActionComponent.EditDocumentActionListener.class)
                 }
    )

public class EditDocumentActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsDocumentFilter(), new IsEditableFilter(), new CanSetPropertyFilter(),
      new IsNotLockedFilter(), new IsCheckedOutFilter(), new IsNotInTrashFilter(), new IsNotEditingDocumentFilter(),
      new IsNotContainBinaryFilter()});

  @UIExtensionFilters
  public static List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private static void refresh(Node node) throws Exception {
    node.refresh(true);
  }

  @SuppressWarnings("unchecked")
  public static void editDocument(Event <? extends UIComponent> event,
                                  WebuiRequestContext context,
                                  UIComponent comp,
                                  UIJCRExplorer uiExplorer,
                                  Node selectedNode,
                                  UIApplication uiApp) throws RepositoryException,
                                  Exception,
                                  ValueFormatException,
                                  PathNotFoundException {
    if (event != null)
      context = event.getRequestContext();
    if (selectedNode.isNodeType(Utils.EXO_ACTION)) {
      UIActionContainer uiContainer = uiExplorer.createUIComponent(UIActionContainer.class, null, null);
      uiExplorer.setIsHidePopup(true);
      UIActionForm uiActionForm =  uiContainer.getChild(UIActionForm.class);
      uiContainer.getChild(UIActionTypeForm.class).setRendered(false);
      uiActionForm.createNewAction(selectedNode, selectedNode.getPrimaryNodeType().getName(), false);
      uiActionForm.setIsUpdateSelect(false);
      uiActionForm.setNodePath(selectedNode.getPath());
      uiActionForm.setWorkspace(selectedNode.getSession().getWorkspace().getName());
      uiActionForm.setStoredPath(selectedNode.getPath());
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(uiContainer, 700, 550);
      context.addUIComponentToUpdateByAjax(UIPopupContainer);
    } else {
      String nodeType = null;
      if(selectedNode.hasProperty("exo:presentationType")) {
        nodeType = selectedNode.getProperty("exo:presentationType").getString();
      }else {
        nodeType = selectedNode.getPrimaryNodeType().getName();
      }
      UIDocumentFormController uiController =
          event != null ?
           event.getSource().createUIComponent(UIDocumentFormController.class, null, "EditFormController") :
             comp.createUIComponent(UIDocumentFormController.class, null, "EditFormController");
           UIDocumentForm uiDocumentForm = uiController.getChild(UIDocumentForm.class);
           uiDocumentForm.setRepositoryName(uiExplorer.getRepositoryName());
           uiDocumentForm.setContentType(nodeType);
           uiDocumentForm.clearRemovedNode();
           uiDocumentForm.clearDataRemovedList();
           if(uiDocumentForm.getTemplate() == null) {
             uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.template-null", null));
             return;
           }
           refresh(selectedNode);
           // Check document is lock for editing
           uiDocumentForm.setIsKeepinglock(false);
           if (!selectedNode.isLocked()) {
             OrganizationService service = WCMCoreUtils.getService(OrganizationService.class);
             List<MembershipType> memberships = (List<MembershipType>) service.getMembershipTypeHandler().findMembershipTypes();
             synchronized (EditDocumentActionComponent.class) {
               refresh(selectedNode);
               if (!selectedNode.isLocked()) {
                 if(selectedNode.canAddMixin(Utils.MIX_LOCKABLE)){
                   selectedNode.addMixin(Utils.MIX_LOCKABLE);
                   selectedNode.save();
                 }
                 Lock lock = selectedNode.lock(false, false);
                 LockUtil.keepLock(lock);
                 LockService lockService = uiExplorer.getApplicationComponent(LockService.class);
                 List<String> settingLockList = lockService.getAllGroupsOrUsersForLock();
                 for (String settingLock : settingLockList) {
                   LockUtil.keepLock(lock, settingLock);
                   if (!settingLock.startsWith("*"))
                     continue;
                   String lockTokenString = settingLock;
                   for (MembershipType membership : memberships) {
                     lockTokenString = settingLock.replace("*", membership.getName());
                     LockUtil.keepLock(lock, lockTokenString);
                   }
                 }
                 selectedNode.save();
                 uiDocumentForm.setIsKeepinglock(true);
               }
             }
           }
           // Add mixin type exo:documentSize if the current node is flash file
           String mimeType = DMSMimeTypeResolver.getInstance().getMimeType(selectedNode.getName());
           if(mimeType.indexOf(Utils.FLASH_MIMETYPE) >= 0 && selectedNode.canAddMixin(Utils.EXO_RISIZEABLE)) {
             selectedNode.addMixin(Utils.EXO_RISIZEABLE);
             selectedNode.save();          	
           }
           // Update data avoid concurrent modification by other session
           refresh(selectedNode);
           // Check again after node is locking by current user or another
           if (LockUtil.getLockTokenOfUser(selectedNode) == null) {
             Object[] arg = { selectedNode.getPath() };
             uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-editing", arg,
                                                     ApplicationMessage.WARNING));
             return;
           }
           uiDocumentForm.setNodePath(selectedNode.getPath());
           uiDocumentForm.addNew(false);
           uiDocumentForm.setWorkspace(selectedNode.getSession().getWorkspace().getName());
           uiDocumentForm.setStoredPath(selectedNode.getPath());
           uiController.setRenderedChild(UIDocumentForm.class);

           UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
           UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
           if(!uiDocumentWorkspace.isRendered()) {
             uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
             uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
           }
           uiDocumentWorkspace.getChild(UIDocumentContainer.class).setRendered(false);
           uiDocumentWorkspace.getChild(UISearchResult.class).setRendered(false);
           UIDocumentFormController controller = uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
           if (controller != null) {
             controller.getChild(UIDocumentForm.class).releaseLock();
           }
           uiDocumentWorkspace.addChild(uiController);
           uiController.initOptionBlockPanel();
           uiController.setRendered(true);
           context.addUIComponentToUpdateByAjax(uiWorkingArea);
           if (event != null) {
             uiExplorer.updateAjax(event);
           }
           context.addUIComponentToUpdateByAjax(uiExplorer.getChild(UIControl.class));
    }
  }

  public static class EditDocumentActionListener extends UIActionBarActionListener<EditDocumentActionComponent> {
    public void processEvent(Event<EditDocumentActionComponent> event) throws Exception {
      EditDocumentActionComponent uicomp = event.getSource();
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class);
      Node selectedNode = null;
      if (nodePath != null && nodePath.length() != 0) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
        String wsName = null;
        if (matcher.find()) {
          wsName = matcher.group(1);
          nodePath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
        }
        Session session = uiExplorer.getSessionByWorkspace(wsName);
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
        try {
          // Use the method getNodeByPath because it is link aware
          selectedNode = uiExplorer.getNodeByPath(nodePath, session);
        } catch (PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
                                                  ApplicationMessage.WARNING));

          return;
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null,
                                                  ApplicationMessage.WARNING));

          return;
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          return;
        }
      }
      if (selectedNode == null)  selectedNode = uiExplorer.getCurrentNode();
      uiExplorer.setSelectNode(selectedNode.getPath());
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      editDocument(event, null, uicomp, uiExplorer, selectedNode, uiApp);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
