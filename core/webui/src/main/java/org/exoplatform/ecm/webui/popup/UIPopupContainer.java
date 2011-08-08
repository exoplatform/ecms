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
package org.exoplatform.ecm.webui.popup;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The Class UIPopupContainer.
 */
@ComponentConfig( lifecycle = Lifecycle.class )
@Deprecated
public class UIPopupContainer extends UIContainer {

  /**
   * Instantiates a new uI popup container.
   *
   * @throws Exception the exception
   */
  public UIPopupContainer() throws Exception {
    addChild(createUIComponent(UIPopupWindow.class, null, null).setRendered(false));
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getWriter().append("<span class=\"").append(getId()).append("\" id=\"").append(getId()).append("\">");
    renderChildren(context) ;
    context.getWriter().append("</span>");
  }

  /**
   * Activate.
   *
   * @param type the type
   * @param width the width
   * @return the t
   * @throws Exception the exception
   */
  public <T extends UIComponent> T activate(Class<T> type, int width) throws Exception {
    return activate(type, null, width, 0);
  }

  /**
   * Activate.
   *
   * @param type the type
   * @param configId the config id
   * @param width the width
   * @param height the height
   * @return the t
   * @throws Exception the exception
   */
  public <T extends UIComponent> T activate(Class<T> type, String configId, int width, int height)
  throws Exception {
    T comp = createUIComponent(type, configId, null);
    activate(comp, width, height);
    return comp;
  }

  /**
   * Activate.
   *
   * @param uiComponent the ui component
   * @param width the width
   * @param height the height
   * @throws Exception the exception
   */
  public void activate(UIComponent uiComponent, int width, int height) throws Exception {
    activate(uiComponent, width, height, true);
  }

  public void activate(UIComponent uiComponent, int width) throws Exception {
    activate(uiComponent, width, 0, true);
  }

  /**
   * Activate.
   *
   * @param uiComponent the ui component
   * @param width the width
   * @param height the height
   * @param isResizeable the is resizeable
   * @throws Exception the exception
   */
  public void activate(UIComponent uiComponent, int width, int height, boolean isResizeable)
  throws Exception {
    UIPopupWindow popup = getChild(UIPopupWindow.class);
    popup.setUIComponent(uiComponent);
    ((UIPopupComponent) uiComponent).activate();
    popup.setWindowSize(width, height);
    popup.setRendered(true);
    popup.setShow(true);
    popup.setShowMask(true);
    popup.setResizable(isResizeable);
  }

  /**
   * De activate.
   *
   * @throws Exception the exception
   */
  public void deActivate() throws Exception {
    UIPopupWindow popup = getChild(UIPopupWindow.class);
    if (popup.getUIComponent() != null)
      ((UIPopupComponent) popup.getUIComponent()).deActivate();
    popup.setUIComponent(null);
    popup.setRendered(false);
  }

  /**
   * Cancel popup action.
   *
   * @throws Exception the exception
   */
  public void cancelPopupAction() throws Exception {
    deActivate();
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    context.addUIComponentToUpdateByAjax(this);
  }
}
