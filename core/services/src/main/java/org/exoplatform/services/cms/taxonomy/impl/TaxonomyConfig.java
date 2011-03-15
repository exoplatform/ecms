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
package org.exoplatform.services.cms.taxonomy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Mar 31, 2009
 */
public class TaxonomyConfig {
  private List<Taxonomy> taxonomies = new ArrayList<Taxonomy>();

  public List<Taxonomy> getTaxonomies() { return this.taxonomies; }

  public void setTaxonomies(List<Taxonomy> taxonomies) { this.taxonomies = taxonomies;}

  static public class Taxonomy {
    private String path;
    private String name;
    private String description;
    private List<Permission> permissions = new ArrayList<Permission>(4);

    public String getPath() { return this.path; }

    public void setPath(String path) { this.path = path;}

    public String getName() {return this.name; }

    public void setName(String name) { this.name = name; }

    public String getDescription()  { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public List<Permission> getPermissions() {
      return this.permissions;
    }

    public void setPermissions(List<Permission> list) {
      this.permissions = list;
    }
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
