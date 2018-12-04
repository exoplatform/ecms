/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.portlet.PortletPreferences;
import java.util.Arrays;
import java.util.List;
/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 13, 2009
 * 5:19:23 PM
 */
@ComponentConfig(
                 events = {
                     @EventConfig(listeners = EmptyTrashManageComponent.EmptyTrashActionListener.class,
                                         confirm = "EmptyTrashManageComponent.msg.confirm-delete") })
public class EmptyTrashManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[] { new IsNotInTrashFilter(),
                                                new IsTrashHomeNodeFilter() } );

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void emptyTrashManage(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer) throws Exception {
    Node trashHomeNode = getTrashHomeNode(uiExplorer);
    NodeIterator nodeIter = trashHomeNode.getNodes();
    UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
    if (nodeIter.getSize() == 0) {
      return;
    }

    String currentUser = WCMCoreUtils.getRemoteUser();
    boolean error = false;
    while (nodeIter.hasNext()) {
      try {
        Node node = nodeIter.nextNode();
        if (node.hasProperty(Utils.EXO_LASTMODIFIER))
          if (currentUser.equals(node.getProperty(Utils.EXO_LASTMODIFIER).getString())) {
            deleteNode(node, uiExplorer, event);
          }
      } catch (Exception ex) {
        error = true;
      }
    }
    if (error) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.error-when-emptying-trash", null,
          ApplicationMessage.WARNING));

    }
  }

  private static void deleteNode(Node nodeToDelete,
                                 UIJCRExplorer uiExplorer,
                                 Event<? extends UIComponent> event) throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");

    String nodePath = nodeToDelete.getPath();

    // Use the method getNodeByPath because it is link aware
    Session session = uiExplorer.getSessionByWorkspace(trashWorkspace);
    // Use the method getNodeByPath because it is link aware
    Node node = uiExplorer.getNodeByPath(nodePath, session, false);
    // Reset the session to manage the links that potentially change of
    // workspace
    session = node.getSession();
    // Reset the workspace name to manage the links that potentially
    // change of workspace
    trashWorkspace = session.getWorkspace().getName();
    // Use the method getNodeByPath because it is link aware
    node = uiExplorer.getNodeByPath(nodePath, session, false);
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

    uiExplorer.addLockToken(node);
    Node parentNode = node.getParent();
    uiExplorer.addLockToken(parentNode);
    ActionServiceContainer actionService = uiExplorer.getApplicationComponent(ActionServiceContainer.class);
    actionService.removeAction(node, uiExplorer.getRepositoryName());
    ThumbnailService thumbnailService = uiExplorer.getApplicationComponent(ThumbnailService.class);
    thumbnailService.processRemoveThumbnail(node);
    TrashService trashService = uiExplorer.getApplicationComponent(TrashService.class);
    trashService.removeRelations(node, uiExplorer.getSystemProvider());
    node.remove();
    parentNode.save();
    uiExplorer.updateAjax(event);
  }

  private static void removeMixins(Node node) throws Exception {
    NodeType[] mixins = node.getMixinNodeTypes();
    for (NodeType nodeType : mixins) {
      node.removeMixin(nodeType.getName());
    }
  }


  private static Node getTrashHomeNode(UIJCRExplorer uiExplorer) throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String trashHomeNodePath = portletPref.getValue(Utils.TRASH_HOME_NODE_PATH, "");
    String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");

    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    Session trashSession = uiExplorer.getSessionProvider().getSession(trashWorkspace, manageableRepository);
    return (Node)trashSession.getItem(trashHomeNodePath);
  }

  public static class EmptyTrashActionListener extends UIWorkingAreaActionListener<EmptyTrashManageComponent> {
    public void processEvent(Event<EmptyTrashManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      emptyTrashManage(event, uiExplorer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
