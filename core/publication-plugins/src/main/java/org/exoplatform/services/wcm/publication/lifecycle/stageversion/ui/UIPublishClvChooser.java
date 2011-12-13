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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com
 * Mar 19, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublishClvChooser.gtmpl",
                 events = {
    @EventConfig(listeners = UIPublishClvChooser.ChooseActionListener.class),
    @EventConfig(listeners = UIPublishClvChooser.CloseActionListener.class) })
public class UIPublishClvChooser extends UIForm implements UIPopupComponent {

  /** The page. */
  private Page page;

  /** The node. */
  private NodeLocation nodeLocation;

  /**
   * Gets the page.
   *
   * @return the page
   */
  public Page getPage() {return page;}

  /**
   * Sets the page.
   *
   * @param page the new page
   */
  public void setPage(Page page) {this.page = page;}

  /**
   * Gets the node.
   *
   * @return the node
   */
  public Node getNode() {
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  /**
   * Sets the node.
   *
   * @param node the new node
   */
  public void setNode(Node node) {
    nodeLocation = NodeLocation.make(node);
  }

  /**
   * Instantiates a new uI publish clv chooser.
   */
  public UIPublishClvChooser() {
  }

  /**
   * Gets the clv portlets.
   *
   * @return the clv portlets
   *
   * @throws Exception the exception
   */
  public List<Application<?>> getClvPortlets() throws Exception {
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.CLV_PORTLET);
    DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
    List<String> clvPortletsId = PublicationUtil.findAppInstancesByName(page, portletName);
    List<Application<?>> applications = new ArrayList<Application<?>>();
    for (String clvPortletId : clvPortletsId) {
      boolean isManualViewerMode = false;
      Application<?> application = PublicationUtil.findAppInstancesById(page, clvPortletId);
      PortletPreferences portletPreferences = dataStorage.getPortletPreferences(clvPortletId);
      if (portletPreferences != null) {
        for (Object object : portletPreferences.getPreferences()) {
          Preference preference = (Preference) object;
          if ("header".equals(preference.getName()) && preference.getValues().size() > 0) {
            application.setTitle(preference.getValues().get(0).toString());
          }
          if ("mode".equals(preference.getName()) && preference.getValues().size() > 0) {
            isManualViewerMode = "ManualViewerMode".equals(preference.getValues().get(0).toString());
          }
        }
      }
      if (isManualViewerMode)
        applications.add(application);
    }
    return applications;
  }

  /**
   * The listener interface for receiving chooseAction events.
   * The class that is interested in processing a chooseAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChooseActionListener<code> method. When
   * the chooseAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChooseActionEvent
   */
  public static class ChooseActionListener extends EventListener<UIPublishClvChooser> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublishClvChooser> event) throws Exception {
      UIPublishClvChooser clvChooser = event.getSource();
      String clvPortletId = URLDecoder.decode(event.getRequestContext().getRequestParameter(OBJECTID), "UTF-8");
      WCMPublicationService presentationService = clvChooser.getApplicationComponent(WCMPublicationService.class);
      DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
      PortletPreferences portletPreferences = dataStorage.getPortletPreferences(clvPortletId);
      Node node = clvChooser.getNode();
      if (portletPreferences != null) {
        for (Object object : portletPreferences.getPreferences()) {
          Preference preference = (Preference) object;
          if ("contents".equals(preference.getName())) {
            String contentValues = preference.getValues().get(0).toString();
            if (contentValues.indexOf(node.getPath()) >= 0) {
              UIApplication application = clvChooser.getAncestorOfType(UIApplication.class);
              application.addMessage(new ApplicationMessage("UIPublishClvChooser.msg.duplicate",
                                                            null,
                                                            ApplicationMessage.WARNING));              
              return;
            }
          }
        }
      }
      StageAndVersionPublicationPlugin publicationPlugin = (StageAndVersionPublicationPlugin) presentationService.
          getWebpagePublicationPlugins().get(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
      publicationPlugin.publishContentToCLV(node,
                                            clvChooser.page,
                                            clvPortletId,
                                            Util.getUIPortal().getSiteKey().getName(),
                                            event.getRequestContext().getRemoteUser());

      UIPublicationPagesContainer uiPublicationPagesContainer =
        clvChooser.getAncestorOfType(UIPublicationPagesContainer.class);
      UIPublicationAction publicationAction =
        ((UIContainer) uiPublicationPagesContainer.getChildById("UIPublicationPages")).getChildById("UIPublicationAction");
      publicationAction.updateUI();
      UIPopupWindow popupWindow = clvChooser.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setShow(false);
    }
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  public static class CloseActionListener extends EventListener<UIPublishClvChooser> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublishClvChooser> event) throws Exception {
      UIPublishClvChooser clvChooser = event.getSource();
      UIPopupWindow popupWindow = clvChooser.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setShow(false);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#activate()
   */
  public void activate() throws Exception {
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#deActivate()
   */
  public void deActivate() throws Exception {
  }
}
