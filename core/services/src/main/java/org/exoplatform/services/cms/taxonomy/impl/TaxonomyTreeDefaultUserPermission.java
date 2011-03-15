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
package org.exoplatform.services.cms.taxonomy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to store data about some default user permissions in
 * configure file dms-taxonomies-configuration.xml
 *
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Feb 2, 2010
 * 11:09:57 AM
 */
public class TaxonomyTreeDefaultUserPermission {

  private List<Permission> permissions = new ArrayList<Permission>();

  public List<Permission> getPermissions() { return permissions; }

  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  static public class Permission {
    private String identity;
    private String read;
    private String addNode;
    private String setProperty;
    private String remove;

    public String getIdentity() {
      return identity;
    }

    public void setIdentity(String identity) {
      this.identity = identity;
    }

    public String getAddNode() {
      return addNode;
    }

    public void setAddNode(String addNode) {
      this.addNode = addNode;
    }

    public String getRead() {
      return read;
    }

    public void setRead(String read) {
      this.read = read;
    }

    public String getRemove() {
      return remove;
    }

    public void setRemove(String remove) {
      this.remove = remove;
    }

    public String getSetProperty() {
      return setProperty;
    }

    public void setSetProperty(String setProperty) {
      this.setProperty = setProperty;
    }
  }

}
