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
package org.exoplatform.ecm.webui.tree;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public abstract class UIBaseNodeTreeSelector extends UIContainer implements ComponentSelector {

  protected UIComponent sourceUIComponent ;
  protected String returnFieldName = null ;
  /**
   * On change.
   *
   * @param currentNode the current node
   * @param context the request context
   * @throws Exception the exception
   */
  public abstract void onChange(final Node currentNode, Object context) throws Exception;
  public String getReturnFieldName() { return returnFieldName; }

  public void setReturnFieldName(String name) { this.returnFieldName = name; }

  public UIComponent getSourceComponent() { return sourceUIComponent; }
  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    sourceUIComponent = uicomponent ;
    if(initParams == null || initParams.length < 0) return ;
    for(int i = 0; i < initParams.length; i ++) {
      if(initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=") ;
        returnFieldName = array[1] ;
        break ;
      }
      returnFieldName = initParams[0] ;
    }
  }

}
