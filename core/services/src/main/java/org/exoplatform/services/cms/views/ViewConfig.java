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
package org.exoplatform.services.cms.views;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * Feb 27, 2006
 */
public class ViewConfig {
  private String name ;
  private String permissions ;
  private String template ;
  private List<Tab> tabList = new ArrayList<Tab>() ;

  public  ViewConfig() { }

  public String getName() { return this.name ; }
  public void setName(String name) { this.name = name ; }

  public String getPermissions() { return this.permissions ; }
  public void setPermissions(String permission) { this.permissions = permission ; }

  public String getTemplate() { return this.template ; }
  public void setTemplate(String templ) { this.template = templ ; }

  public List<Tab> getTabList() { return this.tabList ; }
  public void setTabList(List<Tab> tabs) { this.tabList = tabs ; }

  @SuppressWarnings("unchecked")
  public List getAllPermissions() {
    String[] allPermissions = StringUtils.split(permissions, ";");
    List permissionList = new ArrayList() ;
    for(int i = 0 ; i < allPermissions.length ; i ++ ){
      permissionList.add(allPermissions[i].trim()) ;
    }
    return permissionList ;
  }

  @SuppressWarnings("hiding")
  public boolean hasPermission(String permission) {
    List permissions = getAllPermissions() ;
    if(permission == null) return false ;
    String[] array = StringUtils.split(permission , ":/") ;
    if(array == null || array.length < 2) return false ;
    int i = permissions.indexOf("*:/"+array[1]) ;
    if( i > -1) return true ;
    return permissions.contains(permission) ;
  }

  public static class Tab{
    private String tabName ;
    private String buttons ;

    public Tab() {}

    public String getTabName(){ return this.tabName ; }
    public void setTabName( String name) { this.tabName = name ; }

    public String getButtons() { return this.buttons ; }
    public void setButtons( String buttons) { this.buttons = buttons ; }

  }
}
