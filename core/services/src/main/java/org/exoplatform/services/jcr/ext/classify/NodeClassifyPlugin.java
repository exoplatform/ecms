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
package org.exoplatform.services.jcr.ext.classify;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;

/*
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public abstract class NodeClassifyPlugin implements ComponentPlugin {

  private String name;
  private String desc;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return desc; }
  public void setDescription(String desc) { this.desc = desc; }
  public abstract void classifyChildrenNode(Node parent) throws Exception;
}
