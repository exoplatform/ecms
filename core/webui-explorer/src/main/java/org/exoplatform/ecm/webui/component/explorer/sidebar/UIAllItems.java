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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIAllItemsPreferenceForm;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 29, 2009
 * 7:07:27 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/sidebar/UIAllItems.gtmpl",
    events = {
        @EventConfig(listeners = UIAllItems.ClickFilterActionListener.class),
        @EventConfig(listeners = UIAllItems.PreferencesActionListener.class)
    }
)
public class UIAllItems extends UIComponent {

  public static final String FAVORITE    = "Favorites";

  public static final String OWNED_BY_ME = "OwnedByMe";

  public static final String HIDDEN      = "Hidden";

  public static final String TRASH       = "Trash";

  public UIAllItems() throws Exception {
  }

  public static String getFAVORITE() {
    return FAVORITE;
  }

  public static String getOWNED_BY_ME() {
    return OWNED_BY_ME;
  }

  public static String getHIDDEN() {
    return HIDDEN;
  }

  public static String getTRASH() {
    return TRASH;
  }

  public Preference getPreference() {
    return getAncestorOfType(UIJCRExplorer.class).getPreference();
  }

  static public class ClickFilterActionListener extends EventListener<UIAllItems> {
    public void execute(Event<UIAllItems> event) throws Exception {
      UIAllItems UIAllItems = event.getSource();
      UIJCRExplorer uiExplorer = UIAllItems.getAncestorOfType(UIJCRExplorer.class);
      Set<String> allItemFilterMap = uiExplorer.getAllItemFilterMap();
      HttpServletResponse response = Util.getPortalRequestContext().getResponse();
      String userId = Util.getPortalRequestContext().getRemoteUser();
      String cookieName = Preference.PREFERENCE_SHOW_HIDDEN_NODE + userId;      
      String filterType = event.getRequestContext().getRequestParameter(OBJECTID);
      if (allItemFilterMap.contains(filterType)) {
        allItemFilterMap.remove(filterType);
        if (filterType.equals(HIDDEN)) {
          uiExplorer.getPreference().setShowHiddenNode(false);
          response.addCookie(new Cookie(cookieName, "false"));
        }
      } else {
        allItemFilterMap.add(filterType);
        if (filterType.equals(HIDDEN)) {
          uiExplorer.getPreference().setShowHiddenNode(true);
          response.addCookie(new Cookie(cookieName, "true"));
        }
      }


      // new code
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      }
      uiExplorer.updateAjax(event);
    }
  }

  static public class PreferencesActionListener extends EventListener<UIAllItems> {
    public void execute(Event<UIAllItems> event) throws Exception {
      UIAllItems uiAllItems = event.getSource();
      UIJCRExplorer uiJCRExplorer = uiAllItems.getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer popupAction = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIAllItemsPreferenceForm uiPrefForm = popupAction.activate(UIAllItemsPreferenceForm.class,350) ;
      uiPrefForm.update(uiJCRExplorer.getPreference()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }

}
