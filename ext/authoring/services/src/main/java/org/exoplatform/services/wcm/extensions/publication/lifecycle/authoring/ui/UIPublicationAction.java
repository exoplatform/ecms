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
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.util.NavigationUtils;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPortalNavigationExplorer;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationHistory;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationPages;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationPagesContainer;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationTree.TreeNode;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublishedPages;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com Sep 25, 2008
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublicationAction.gtmpl",
                 events = {
    @EventConfig(listeners = UIPublicationAction.AddActionListener.class),
    @EventConfig(listeners = UIPublicationAction.RemoveActionListener.class) })
public class UIPublicationAction extends UIForm {

  /**
   * Update ui.
   *
   * @throws Exception the exception
   */
  public void updateUI() throws Exception {
    UIPublicationPages publicationPages = getAncestorOfType(UIPublicationPages.class);
    UIPublishedPages publishedPages = publicationPages.getChild(UIPublishedPages.class);

    Node node = publicationPages.getNode();
    List<String> listPublishedPage = new ArrayList<String>();
    if (node.hasProperty("publication:navigationNodeURIs")) {
      Value[] navigationNodeURIs = node.getProperty("publication:navigationNodeURIs").getValues();
      for (Value navigationNodeURI : navigationNodeURIs) {
        if (PublicationUtil.isNodeContentPublishedToPageNode(node, navigationNodeURI.getString())) {
          listPublishedPage.add(navigationNodeURI.getString());
        }
      }
      publishedPages.setListNavigationNodeURI(listPublishedPage);
      UIPublicationContainer publicationContainer = getAncestorOfType(UIPublicationContainer.class);
      UIPublicationHistory publicationHistory = publicationContainer.getChild(UIPublicationHistory.class);
      UIPublicationPanel publicationPanel = publicationContainer.getChild(UIPublicationPanel.class);
      publicationHistory.init(publicationPanel.getCurrentNode());
      publicationHistory.updateGrid();
    }
  }

    /**
     * The listener interface for receiving addAction events. The class that is
     * interested in processing a addAction event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's <code>addAddActionListener</code> method. When
     * the addAction event occurs, that object's appropriate
     * method is invoked.
     */
    public static class AddActionListener extends EventListener<UIPublicationAction> {

  /*
   * (non-Javadoc)
   *
   * @see
   * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
   * .webui.event.Event)
   */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();
      UIPublicationPages publicationPages = publicationAction.getAncestorOfType(UIPublicationPages.class);
      UIApplication application = publicationAction.getAncestorOfType(UIApplication.class);

      UIPortalNavigationExplorer portalNavigationExplorer = publicationPages.getChild(UIPortalNavigationExplorer.class);
      TreeNode selectedNode = portalNavigationExplorer.getSelectedNode();

      if (selectedNode == null) {
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.none",
                                                      null,
                                                      ApplicationMessage.WARNING));

        return;
      }

      String selectedNavigationNodeURI = selectedNode.getUri();
      Node node = publicationPages.getNode();

      if (node.hasProperty("publication:navigationNodeURIs")
          && PublicationUtil.isNodeContentPublishedToPageNode(node, selectedNavigationNodeURI)) {
        Value[] navigationNodeURIs = node.getProperty("publication:navigationNodeURIs").getValues();
        for (Value navigationNodeURI : navigationNodeURIs) {
          if (navigationNodeURI.getString().equals(selectedNavigationNodeURI)) {
            application.addMessage(new ApplicationMessage("UIPublicationAction.msg.duplicate",
                                                          null,
                                                          ApplicationMessage.WARNING));
            return;
          }
        }
      }

      UserNode userNode = selectedNode.getUserNode();
      if (userNode == null) {
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.wrongNode",
                                                      null,
                                                      ApplicationMessage.WARNING));

        return;
      }

      UIPublicationPagesContainer publicationPagesContainer = publicationPages.
          getAncestorOfType(UIPublicationPagesContainer.class);
      publicationAction.updateUI();
      UIPublicationContainer publicationContainer = publicationAction.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPagesContainer, event.getRequestContext());
    }
  }

    /**
     * The listener interface for receiving removeAction events. The class that
     * is interested in processing a removeAction event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's
     * <code>addRemoveActionListener</code> method. When
     * the removeAction event occurs, that object's appropriate
   * method is invoked.
     */
    public static class RemoveActionListener extends EventListener<UIPublicationAction> {

  /*
   * (non-Javadoc)
   *
   * @see
   * org.exoplatform.webui.event.EventListener#execute(org.exoplatform
   * .webui.event.Event)
   */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();
      UIPublicationPages publicationPages = publicationAction.getAncestorOfType(UIPublicationPages.class);
      UserPortalConfigService userPortalConfigService = publicationAction.getApplicationComponent(UserPortalConfigService.class);

      UIPublishedPages publishedPages = publicationPages.getChild(UIPublishedPages.class);
      String selectedNavigationNodeURI = publishedPages.getSelectedNavigationNodeURI();

      if (selectedNavigationNodeURI == null) {
        UIApplication application = publicationAction.getAncestorOfType(UIApplication.class);
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.none",
                                                      null,
                                                      ApplicationMessage.WARNING));
        
        return;
      }
      String portalName = selectedNavigationNodeURI.substring(1,
                                                              selectedNavigationNodeURI.indexOf("/",
                                                                                                1));
      String pageNodeUri = selectedNavigationNodeURI.replaceFirst("/\\w+/", "");
      UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
      UserNavigation navigation = NavigationUtils.getUserNavigationOfPortal(userPortal, portalName);
      
      Node contentNode = null;
      if (navigation != null) {
        contentNode = publicationPages.getNode();
        if (contentNode.hasProperty("publication:applicationIDs")) {
          UserNode userNode = getUserNodeByUri(navigation, pageNodeUri);
          userPortalConfigService.getPage(userNode.getPageRef());
        }
      }
      publicationAction.updateUI();
      UIPublicationPagesContainer publicationPagesContainer = publicationPages.
          getAncestorOfType(UIPublicationPagesContainer.class);
      UIPublicationContainer publicationContainer = publicationAction.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPagesContainer, event.getRequestContext());
    }

    /**
     * Gets the user node by uri.
     * @param pageNav
     * @param uri
     * @return
     */
    private UserNode getUserNodeByUri(UserNavigation pageNav, String uri) {
      if(pageNav == null || uri == null) return null;      
      UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
      return userPortal.resolvePath(pageNav, null, uri);     
    }
  }

}
