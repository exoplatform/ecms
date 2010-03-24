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


import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.admin.manager.UIAbstractManager;
import org.exoplatform.ecm.webui.component.admin.manager.UIAbstractManagerComponent;
import org.exoplatform.ecm.webui.component.explorer.UIConfirmMessage;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanDeleteNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

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

  private static final Log LOG = ExoLogger.getLogger(DeleteManageComponent.class);
  
  private static final List<UIExtensionFilter> FILTERS 
  		= Arrays.asList(new UIExtensionFilter[]{new IsNotLockedFilter(), 
  																						new CanDeleteNodeFilter(), 
  																						new IsNotTrashHomeNodeFilter()});
  
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  private void processRemoveMultiple(String[] nodePaths, Event<?> event) throws Exception {
    Node node = null;
    String wsName = null;
    String nodePath = null;
    Session session = null;
    Map<String, Node> mapNode = new HashMap<String, Node>();
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    for (int i = 0; i < nodePaths.length; i++) {
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePaths[i]);
      // prepare to remove
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
        try {
          session = uiExplorer.getSessionByWorkspace(wsName);
          // Use the method getNodeByPath because it is link aware
          node = uiExplorer.getNodeByPath(nodePath, session, false);
          // Reset the session to manage the links that potentially change of
          // workspace
          session = node.getSession();
          // Reset the workspace name to manage the links that potentially
          // change of workspace
          wsName = session.getWorkspace().getName();
          mapNode.put(nodePath, node);
        } catch (PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
        }
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
      }
    }

    String path = null;
    Iterator<String> iterator = mapNode.keySet().iterator();
    while (iterator.hasNext()) {
      path = iterator.next();
      processRemoveOrMoveToTrash(path, mapNode.get(path), event, true, true);
    }
  }
  
  private void removeMixins(Node node) throws Exception {
    NodeType[] mixins = node.getMixinNodeTypes();
    for (NodeType nodeType : mixins) {
      node.removeMixin(nodeType.getName());
    }
  }

  private void processRemoveOrMoveToTrash(String nodePath, Node node, Event<?> event, boolean isMultiSelect, boolean checkToMoveToTrash)
  throws Exception {
		if (node.isNodeType(Utils.EXO_RESTORELOCATION) || !checkToMoveToTrash)
			processRemoveNode(nodePath, node, event, isMultiSelect);
		else {
			moveToTrash(nodePath, node, event, isMultiSelect);
		}
  }

	private void moveToTrash(String srcPath, Node node, Event<?> event, boolean isMultiSelect) throws Exception {
		ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    TrashService trashService = (TrashService)myContainer.getComponentInstanceOfType(TrashService.class);
    
    final String virtualNodePath = srcPath;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    
    
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    try {
      uiExplorer.addLockToken(node);
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      return;
    }
    
    try {
    	if (node.isLocked()) {
    		LockUtil.removeLock(node);    		
    		node.unlock();
    	}
			if (!node.isCheckedOut())
				throw new VersionException("node is locked, can't move to trash node :" + node.getPath());
			if (!PermissionUtil.canRemoveNode(node))
				throw new AccessDeniedException("access denied, can't move to trash node:" + node.getPath());
    	PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
        PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    	String trashHomeNodePath = portletPref.getValue(Utils.TRASH_HOME_NODE_PATH, "");
    	String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");
    	String trashRepository = portletPref.getValue(Utils.TRASH_REPOSITORY, "");
    	SessionProvider sessionProvider = uiExplorer.getSessionProvider();
    	Node currentNode = uiExplorer.getCurrentNode();
    	try {
	    	trashService.moveToTrash(node, 
	    							 trashHomeNodePath, trashWorkspace, 
	    							 trashRepository, sessionProvider);
    	} catch (PathNotFoundException ex) {}
    	String currentPath = LinkUtils.getExistPath(currentNode, uiExplorer.getCurrentPath());    	
    	uiExplorer.setCurrentPath(currentPath);    	
    	uiExplorer.updateAjax(event);
    	
    } catch (LockException e) {
    	LOG.error("node is locked, can't move to trash node :" + node.getPath());
    	JCRExceptionManager.process(uiApp, e);
    	event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
    	uiExplorer.updateAjax(event);
    } catch (VersionException e) {
    	LOG.error("node is checked in, can't move to trash node:" + node.getPath());
    	JCRExceptionManager.process(uiApp, e);
    	event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
    	uiExplorer.updateAjax(event);	   
    } catch (AccessDeniedException e) {
    	LOG.error("access denied, can't add move to trash to node:" + node.getPath());
    	JCRExceptionManager.process(uiApp, e);
    	event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
    	uiExplorer.updateAjax(event);
    } catch (Exception e) {
    	LOG.error("an unexpected error occurs", e);
    	JCRExceptionManager.process(uiApp, e);
    	event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
    	uiExplorer.updateAjax(event);
    }
    
    if (!isMultiSelect) {
      if (uiExplorer.getCurrentPath().equals(virtualNodePath))       
        uiExplorer.setSelectNode(LinkUtils.getParentPath(virtualNodePath));
      else
        uiExplorer.setSelectNode(uiExplorer.getCurrentPath());
    }
	}  
  
  private void processRemoveNode(String nodePath, Node node, Event<?> event, boolean isMultiSelect)
      throws Exception {
    final String virtualNodePath = nodePath;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    Node currentNode = uiExplorer.getCurrentNode(); 
    Session session = node.getSession();
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    
    TrashService trashService = getApplicationComponent(TrashService.class);
    
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
      List<Node> listTaxonomyTrees = taxonomyService.getAllTaxonomyTrees(uiExplorer.getRepositoryName());
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
      
      if (node.isNodeType(Utils.RMA_RECORD))
        removeMixins(node);
      ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
      actionService.removeAction(node, uiExplorer.getRepositoryName());
      ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
      thumbnailService.processRemoveThumbnail(node);
      NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
           
      newFolksonomyService.removeTagsOfNodeRecursively(node,uiExplorer.getRepositoryName(),
      																								 uiExplorer.getRepository().getConfiguration().
      																								 getDefaultWorkspaceName(),
      																								 node.getSession().getUserID(),
																											 getGroups());
      //trashService.removeRelations(node, uiExplorer.getSystemProvider(), uiExplorer.getRepositoryName());
      node.remove();
      parentNode.save();
    } catch (VersionException ve) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.remove-verion-exception", null,
          ApplicationMessage.WARNING));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      uiExplorer.updateAjax(event);
      return;
    } catch (ReferentialIntegrityException ref) {
      session.refresh(false);
      uiExplorer.refreshExplorer();
      uiApp
          .addMessage(new ApplicationMessage(
              "UIPopupMenu.msg.remove-referentialIntegrityException", null,
              ApplicationMessage.WARNING));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      uiExplorer.updateAjax(event);
      return;
    } catch (ConstraintViolationException cons) {
      session.refresh(false);
      uiExplorer.refreshExplorer();
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.constraintviolation-exception",
          null, ApplicationMessage.WARNING));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      uiExplorer.updateAjax(event);
      return;
    } catch (LockException lockException) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-other-person", null,
          ApplicationMessage.WARNING));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      uiExplorer.updateAjax(event);
      return;
    } catch (Exception e) {
      LOG.error("an unexpected error occurs while removing the node", e);
      JCRExceptionManager.process(uiApp, e);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
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
    if (!uiExplorer.getPreference().isJcrEnable())
      uiExplorer.getSession().save();
  }

  public void doDeleteWithoutTrash(String nodePath, Event<?> event) throws Exception {
  	doDelete(nodePath, event, false);
  }
  
  public void doDelete(String nodePath, Event<?> event) throws Exception {
  	doDelete(nodePath, event, true);
  }
  
  public void doDelete(String nodePath, Event<?> event, boolean checkToMoveToTrash) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (nodePath.indexOf(";") > -1) {
      processRemoveMultiple(nodePath.split(";"), event);
    } else {
      String wsName = null;
      Session session = null;
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
      UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
      // prepare to remove
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
        try {
          // Use the method getNodeByPath because it is link aware
          session = uiExplorer.getSessionByWorkspace(wsName);
          // Use the method getNodeByPath because it is link aware
          Node node = uiExplorer.getNodeByPath(nodePath, session, false);
          // Reset the session to manage the links that potentially change of
          // workspace
          session = node.getSession();
          // Reset the workspace name to manage the links that potentially
          // change of workspace
          wsName = session.getWorkspace().getName();
          // Use the method getNodeByPath because it is link aware
          node = uiExplorer.getNodeByPath(nodePath, session, false);
          
          processRemoveOrMoveToTrash(nodePath, node, event, false, checkToMoveToTrash);
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
    }
    uiExplorer.updateAjax(event);
    if (!uiExplorer.getPreference().isJcrEnable())
      uiExplorer.getSession().save();
  }
  
  public static void deleteManage(Event<? extends UIComponent> event) throws Exception {
    UIWorkingArea uiWorkingArea = event.getSource().getParent();
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
    UIConfirmMessage uiConfirmMessage = 
      uiWorkingArea.createUIComponent(UIConfirmMessage.class, null, null);
    
    if(nodePath.indexOf(";") > -1) {
      uiConfirmMessage.setMessageKey("UIWorkingArea.msg.confirm-delete-multi");
      uiConfirmMessage.setArguments(new String[] {Integer.toString(nodePath.split(";").length)});
    } else {
      uiConfirmMessage.setMessageKey("UIWorkingArea.msg.confirm-delete");
      uiConfirmMessage.setArguments(new String[] {nodePath});
    }
    uiConfirmMessage.setNodePath(nodePath);
    UIPopupContainer.activate(uiConfirmMessage, 500, 180);
    event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
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
//      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      deleteManage(event);
    }
  }
  
}
