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
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 16, 2009
 * 10:20:29 AM
 */
public class IsFavouriteFilter extends UIExtensionAbstractFilter {

  public IsFavouriteFilter() {
    this(null);
  }

  public IsFavouriteFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public static boolean isFavourite(Node node, UIJCRExplorer uiExplorer) throws Exception {
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    FavoriteService favoriteService
        = (FavoriteService)myContainer.getComponentInstanceOfType(FavoriteService.class);

    return favoriteService.isFavoriter(uiExplorer.getSession().getUserID(), node);
  }
  public boolean accept(Map<String, Object> context) throws Exception {
      if (context == null) return true;
      Node currentNode = (Node) context.get(Node.class.getName());
      UIJCRExplorer uiExplorer = (UIJCRExplorer)context.get(UIJCRExplorer.class.getName());
      return isFavourite(currentNode, uiExplorer);
  }

  public void onDeny(Map<String, Object> context) throws Exception {  }
}
