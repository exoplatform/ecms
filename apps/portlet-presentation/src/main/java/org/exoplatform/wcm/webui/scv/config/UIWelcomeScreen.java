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
package org.exoplatform.wcm.webui.scv.config;

import java.util.ArrayList;
import java.util.Collections;

import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.selector.content.one.UIContentSelectorOne;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 * dzungdev@gmail.com
 * May 26, 2008
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/SingleContentViewer/config/UIWelcomeScreen.gtmpl",
    events = {
      @EventConfig(listeners = UIWelcomeScreen.SelectContentActionListener.class),
      @EventConfig(listeners = UIWelcomeScreen.CreateNewContentActionListener.class),
      @EventConfig(listeners = UIWelcomeScreen.AbortActionListener.class)
    }
)
public class UIWelcomeScreen extends UIForm {

  /**
   * Instantiates a new uI welcome screen.
   *
   * @throws Exception the exception
   */
  public UIWelcomeScreen() throws Exception {
    this.setActions(new String[]{"Abort"});
  }

  /**
   * The listener interface for receiving selectContentAction events.
   * The class that is interested in processing a selectContentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectContentActionListener<code> method. When
   * the selectContentAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectContentActionEvent
   */
  public static class SelectContentActionListener extends EventListener<UIWelcomeScreen> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWelcomeScreen> event) throws Exception {
      UIWelcomeScreen uiWelcomeScreen = event.getSource();
      UIContentSelectorOne contentSelector = uiWelcomeScreen.createUIComponent(UIContentSelectorOne.class, null, null);
      contentSelector.init();
      Utils.updatePopupWindow(uiWelcomeScreen, contentSelector, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving createNewContentAction events.
   * The class that is interested in processing a createNewContentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCreateNewContentActionListener<code> method. When
   * the createNewContentAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CreateNewContentActionEvent
   */
  public static class CreateNewContentActionListener extends EventListener<UIWelcomeScreen> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWelcomeScreen> event) throws Exception {
      UIWelcomeScreen welcomeScreen = event.getSource();
      UINameWebContentForm nameWebContentForm = welcomeScreen.createUIComponent(UINameWebContentForm.class, null, null);
      Utils.updatePopupWindow(welcomeScreen, nameWebContentForm, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving abortAction events.
   * The class that is interested in processing a abortAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAbortActionListener<code> method. When
   * the abortAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AbortActionEvent
   */
  public static class AbortActionListener extends EventListener<UIWelcomeScreen> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings("unchecked")
    public void execute(Event<UIWelcomeScreen> event) throws Exception {
      UIWelcomeScreen welcomeScreen = event.getSource();
      UIPortal portal = Util.getUIPortal();
      PageNode currentPageNode = portal.getSelectedNode();
      DataStorage dataStorage = welcomeScreen.getApplicationComponent(DataStorage.class);
      Page currentPage = dataStorage.getPage(currentPageNode.getPageReference());
      ArrayList<Object> applications = new ArrayList<Object>();
      applications.addAll(currentPage.getChildren());
      ArrayList<ModelObject> applicationsTmp = currentPage.getChildren();
      Collections.reverse(applicationsTmp);
      for (Object applicationObject : applicationsTmp) {
        if (applicationObject instanceof Container) continue;
        Application application = Application.class.cast(applicationObject);
        String applicationId = application.getId();
        if(applicationId == null) {
          continue;
        }
        PortletPreferences portletPreferences = dataStorage.getPortletPreferences(applicationId);
        if (portletPreferences == null) continue;

        boolean isQuickCreate = false;
        String nodeIdentifier = null;

        for (Object preferenceObject : portletPreferences.getPreferences()) {
          Preference preference = Preference.class.cast(preferenceObject);

          if ("isQuickCreate".equals(preference.getName())) {
            isQuickCreate = Boolean.valueOf(preference.getValues().get(0).toString());
            if (!isQuickCreate) break;
          }

          if ("nodeIdentifier".equals(preference.getName())) {
            nodeIdentifier = preference.getValues().get(0).toString();
            if (nodeIdentifier == null || "".equals(nodeIdentifier)) break;
          }
        }

        if (isQuickCreate && (nodeIdentifier == null || "".equals(nodeIdentifier))) {
          applications.remove(applicationObject);
        }
      }
//      currentPage.setChildren(applications);
      dataStorage.save(currentPage);
      UIPage uiPage = portal.findFirstComponentOfType(UIPage.class);
      if (uiPage != null) {
        uiPage.setChildren(null);
        PortalDataMapper.toUIPage(uiPage, currentPage);
      }
      Utils.closePopupWindow(welcomeScreen, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
      Utils.updatePortal((PortletRequestContext)event.getRequestContext());
    }
  }
}
