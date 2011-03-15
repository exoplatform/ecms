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

import java.util.Collection;

import org.exoplatform.container.component.ComponentPlugin;


/**
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public interface NodeClassifyService {

  public void addClassifyPlugin(ComponentPlugin componentPlugin) throws Exception;
  public Collection<NodeClassifyPlugin> getAllClassifyPlugins() throws Exception;
  public NodeClassifyPlugin getNodeClassifyPlugin(String type) throws Exception;
  public void removeClassifyPlygin(String type) throws Exception;
  public <T extends NodeClassifyPlugin> T getNodeClassifyPluginByType(Class<T> clazz) throws Exception;

}
