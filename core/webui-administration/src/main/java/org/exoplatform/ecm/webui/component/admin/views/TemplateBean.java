/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.web.application.RequestContext;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Oct 9, 2006
 * 2:36:06 PM
 */
public class TemplateBean {
  private String name ;
  private String path ;
  private String baseVersion =  "";

  public TemplateBean(String n, String p, String baVer) {
    name = n ;
    path = p ;
    baseVersion = baVer ;
  }

  public String getBaseVersion() { return baseVersion; }
  public void setBaseVersion(String baVer) { baseVersion = baVer; }

  public String getName() {
    ResourceBundle res = RequestContext.getCurrentInstance().getApplicationResourceBundle();
    String label = null;
    try {
      label = res.getString("UIViewFormTabPane.label.option." + name);
    } catch (MissingResourceException e) {
      label = name;
    }
    return label;
  }
  public void setName(String n) { name = n ; }

  public String getPath() { return path ; }
  public void setPath(String p) { path = p ; }
}
