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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationTree.TreeNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Sep 25, 2008
 */

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublicationAction.gtmpl",
    events = {
      @EventConfig(listeners = UIPublicationAction.AddActionListener.class),
      @EventConfig(listeners = UIPublicationAction.RemoveActionListener.class)
    }
)
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
   * The listener interface for receiving addAction events.
   * The class that is interested in processing a addAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddActionListener<code> method. When
   * the addAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddActionEvent
   */
  public static class AddActionListener extends EventListener<UIPublicationAction> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();
      UIPublicationPages publicationPages = publicationAction.getAncestorOfType(UIPublicationPages.class);
      UIApplication application = publicationAction.getAncestorOfType(UIApplication.class);

      UIPortalNavigationExplorer portalNavigationExplorer = publicationPages.getChild(UIPortalNavigationExplorer.class);
      TreeNode selectedNode = portalNavigationExplorer.getSelectedNode();

      if (selectedNode == null) {
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.none", null, ApplicationMessage.WARNING));
        
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
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.wrongNode", null, ApplicationMessage.WARNING));
        
        return;
      }

      WCMPublicationService presentationService = publicationAction.getApplicationComponent(WCMPublicationService.class);

      UIPublicationPagesContainer publicationPagesContainer = publicationPages.
          getAncestorOfType(UIPublicationPagesContainer.class);
      UserPortalConfigService userPortalConfigService = publicationAction.
          getApplicationComponent(UserPortalConfigService.class);
      Page page = userPortalConfigService.getPage(userNode.getPageRef(), event.getRequestContext().getRemoteUser());
      List<String> clvPortletIds = getManualModeCLVPortletIDs(page);
      if (clvPortletIds.isEmpty()) {
        presentationService.publishContentSCV(node, page, Util.getUIPortal().getSiteKey().getName());
      } else {
        if (clvPortletIds.size() > 1) {
          UIPublishClvChooser clvChooser = publicationAction.createUIComponent(UIPublishClvChooser.class,
                                                                               null,
                                                                               "UIPublishClvChooser");
          clvChooser.setPage(page);
          clvChooser.setNode(node);
          UIPopupWindow popupWindow = publicationPagesContainer.getChildById("UIClvPopupContainer");
          clvChooser.setRendered(true);
          popupWindow.setUIComponent(clvChooser);
          popupWindow.setWindowSize(400, -1);
          popupWindow.setShow(true);
          popupWindow.setShowMask(true);
          event.getRequestContext().addUIComponentToUpdateByAjax(publicationPagesContainer);
        } else {
          String clvPortletId = clvPortletIds.get(0);
          presentationService.publishContentCLV(node,
                                                page,
                                                clvPortletId,
                                                Util.getUIPortal().getSiteKey().getName(),
                                                event.getRequestContext().getRemoteUser());
        }
      }
      publicationAction.updateUI();
      UIPublicationContainer publicationContainer = publicationAction.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPagesContainer, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving removeAction events.
   * The class that is interested in processing a removeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveActionListener<code> method. When
   * the removeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RemoveActionEvent
   */
  public static class RemoveActionListener extends EventListener<UIPublicationAction> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();
      UIPublicationPages publicationPages = publicationAction.getAncestorOfType(UIPublicationPages.class);
      UserPortalConfigService userPortalConfigService = publicationAction.
          getApplicationComponent(UserPortalConfigService.class);
      UIPublishedPages publishedPages = publicationPages.getChild(UIPublishedPages.class);
      String selectedNavigationNodeURI = publishedPages.getSelectedNavigationNodeURI();

      if (selectedNavigationNodeURI == null) {
        UIApplication application = publicationAction.getAncestorOfType(UIApplication.class);
        application.addMessage(new ApplicationMessage("UIPublicationAction.msg.none", null, ApplicationMessage.WARNING));        
        return;
      }
      String portalName = selectedNavigationNodeURI.substring(1, selectedNavigationNodeURI.indexOf("/", 1));
      String pageNodeUri = selectedNavigationNodeURI.replaceFirst("/\\w+/", "");
      
      UserPortal userPortal = Util.getUIPortalApplication().getUserPortalConfig().getUserPortal();
      UserNavigation navigation = userPortal.getNavigation(SiteKey.portal(portalName));
      Page page = null;
      Node contentNode = null;
      if (navigation != null) {
        contentNode = publicationPages.getNode();
        if (contentNode.hasProperty("publication:applicationIDs")) {
          UserNode userNode = getUserNodeByUri(navigation, pageNodeUri);
          page = userPortalConfigService.getPage(userNode.getPageRef(), event.getRequestContext().getRemoteUser());
        }
      }
      WCMPublicationService presentationService = publicationAction.getApplicationComponent(WCMPublicationService.class);
      StageAndVersionPublicationPlugin publicationPlugin
          = (StageAndVersionPublicationPlugin) presentationService.getWebpagePublicationPlugins()
              .get(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
      publicationPlugin.suspendPublishedContentFromPage(publicationPages.getNode(),
                                                        page,
                                                        event.getRequestContext().getRemoteUser());
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
      UserPortal userPortal = Util.getUIPortalApplication().getUserPortalConfig().getUserPortal();
      return userPortal.resolvePath(pageNav, null, uri);     
    }
  }

  private static List<String> getManualModeCLVPortletIDs(Page page) throws Exception {
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.CLV_PORTLET);
    DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
    List<String> clvPortletsId = PublicationUtil.findAppInstancesByName(page, portletName);
    List<String> applicationIDs = new ArrayList<String>();
    for (String clvPortletId : clvPortletsId) {
      boolean isManualViewerMode = false;
      PortletPreferences portletPreferences = dataStorage.getPortletPreferences(clvPortletId);
      if (portletPreferences != null) {
        for (Object object : portletPreferences.getPreferences()) {
          Preference preference = (Preference) object;
          if ("mode".equals(preference.getName()) && preference.getValues().size() > 0) {
            isManualViewerMode = "ManualViewerMode".equals(preference.getValues().get(0).toString());
          }
        }
      }

      if (isManualViewerMode)
        applicationIDs.add(clvPortletId);
    }
    return applicationIDs;
  }
}
