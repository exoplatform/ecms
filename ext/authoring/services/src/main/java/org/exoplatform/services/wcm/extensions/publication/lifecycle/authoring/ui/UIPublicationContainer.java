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
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationHistory;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * chuong_phan@exoplatform.com Mar 4, 2009
 */

@ComponentConfig(lifecycle = Lifecycle.class, template = "app:/groovy/webui/component/explorer/popup/action/UITabPane.gtmpl",
  events = {
  @EventConfig(listeners = UIPublicationContainer.CloseActionListener.class)
  })
public class UIPublicationContainer
                                   extends
                                   org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationContainer {

  /**
   * Instantiates a new uI publication container.
   */
  public UIPublicationContainer() {
  }

  /** The date time formater. */
  private DateFormat dateTimeFormater;

  /**
   * Inits the container.
   *
   * @param node the node
   * @throws Exception the exception
   */
  public void initContainer(Node node) throws Exception {
    UIPublicationPanel publicationPanel = addChild(UIPublicationPanel.class, null, null);
    publicationPanel.init(node);
    
    this.checkToShowPublicationScheduleAndPublicationHistory(node);
    
    setSelectedTab(1);
    Locale locale = org.exoplatform.portal.webui.util.Util.getPortalRequestContext().getLocale();
    dateTimeFormater = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,
                                                            SimpleDateFormat.MEDIUM,
                                                            locale);
  }
  
  private void checkToShowPublicationScheduleAndPublicationHistory(Node node) throws Exception {
    String currentState = node.getProperty(AuthoringPublicationConstant.CURRENT_STATE).getString();
    this.removeChild(UIPublicationSchedule.class);
    if (PublicationDefaultStates.STAGED.equals(currentState)) {
      UIPublicationSchedule publicationSchedule = addChild(UIPublicationSchedule.class, null, null);
      publicationSchedule.init(node);
      publicationSchedule.setRendered(false);
    }
    
    this.removeChild(UIPublicationHistory.class);
    UIPublicationHistory publicationHistory = addChild(UIPublicationHistory.class, null, null);
    publicationHistory.init(node);
    publicationHistory.updateGrid();
    publicationHistory.setRendered(false);
  }
  
  /**
   * Gets the date time formater.
   *
   * @return the date time formater
   */
  public DateFormat getDateTimeFormater() {
    return dateTimeFormater;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.form.UIForm#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    Node currentNode =
        this.getChild(UIPublicationPanel.class).getCurrentNode();
    this.checkToShowPublicationScheduleAndPublicationHistory(currentNode);
    super.processRender(context);
  }
  
  public static class CloseActionListener extends EventListener<UIPublicationContainer> {
    public void execute(Event<UIPublicationContainer> event) throws Exception {
      UIPublicationContainer publicationContainer = event.getSource();
      UIPopupContainer uiPopupContainer = publicationContainer.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
}
