/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.portletcache;

import java.io.Serializable;

import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class WindowKey implements Serializable
{

  private static final long serialVersionUID = 924996837199587159L;

  /** . */
  private final String windowId;

  /** . */
  private final String windowState;

  /** . */
  private final String portletMode;

  /** . */
  private final Locale locale;

  /** . */
  private final Map<String, String[]> parameters;

  /** . */
  private final Map<String, String[]> query;

  /** . */
  private final int hashCode;

  WindowKey(String windowId,
            WindowState windowState,
            PortletMode portletMode,
            Locale locale,
            Map<String, String[]> parameters,
            Map<String, String[]> query)
  {

    // Clone parameter map
    parameters = Util.clone(parameters);
    query = Util.clone(query);

    // Compute hashCode;
    int hashCode =
      windowId.hashCode() ^
      windowState.hashCode() ^
      portletMode.hashCode() ^
      locale.hashCode() ^
      Util.hashCode(parameters) ^
      Util.hashCode(query);

    //
    this.windowId = windowId;
    this.windowState = windowState.toString();
    this.portletMode = portletMode.toString();
    this.parameters = parameters;
    this.locale = locale;
    this.hashCode = hashCode;
    this.query = query;
  }

  public String getWindowId()
  {
    return windowId;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }
    if (obj instanceof WindowKey)
    {
      WindowKey that = (WindowKey)obj;
      return windowId.equals(that.windowId) &&
      windowState.equals(that.windowState) &&
      portletMode.equals(that.portletMode) &&
      locale.equals(that.locale) &&
      Util.equals(parameters, that.parameters) &&
      Util.equals(query, that.query);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("WindowKey[");
    sb.append("windowId").append('=').append(windowId).append(',');
    sb.append("windowState").append('=').append(windowState).append(',');
    sb.append("portletMode").append('=').append(portletMode).append(',');
    sb.append("locale").append('=').append(locale).append(',');
    sb.append("parameters").append('=');
    Util.toString(parameters, sb);
    sb.append(',');
    sb.append("query").append('=');
    Util.toString(query, sb);
    sb.append(']');
    return sb.toString();
  }
}
