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
package org.exoplatform.services.cms.impl;

import java.util.ArrayList;
import java.util.List;

public class ResourceConfig {

  private List<Resource> resources = new ArrayList<Resource>(5);

  public List getRessources() { return resources ; }
  @SuppressWarnings("unchecked")
  public void setRessources(List resources) { this.resources = resources ; }

  static public class Resource {
    private String name ;
    private String description ;

    public String getName() { return name ; }
    public void setName(String name) { this.name = name ; }

    public String getDescription() { return this.description ; }
    public void setDescription(String s) {this.description = s ; }

  }

}
