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
package org.exoplatform.services.wcm.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Dec 11, 2008
 */
public class ExcludeIncludeDataTypePlugin extends BaseComponentPlugin{

  /** The exclude node types. */
  private List<String> excludeNodeTypes = new ArrayList<String>();

  /** The include node types. */
  private List<String> includeNodeTypes = new ArrayList<String>();

  /** The exclude mime types. */
  private List<String> excludeMimeTypes = new ArrayList<String>();

  /** The include mime types. */
  private List<String> includeMimeTypes = new ArrayList<String>();

  /**
   * Instantiates a new exclude include data type plugin.
   *
   * @param initParams the init params
   */
  @SuppressWarnings("unchecked")
  public ExcludeIncludeDataTypePlugin(InitParams initParams) {
    Iterator iterator = initParams.getPropertiesParamIterator() ;
    for(;iterator.hasNext();) {
      PropertiesParam params = (PropertiesParam)iterator.next();
      if("search.include.datatypes".equals(params.getName())) {
        String nodetypes = params.getProperty("nodetypes");
        if(nodetypes != null) {
          includeNodeTypes.addAll(Arrays.asList(nodetypes.split(",")));
        }
        String mimeTypes = params.getProperty("mimetypes");
        if(mimeTypes != null) {
          includeMimeTypes.addAll(Arrays.asList(mimeTypes.split(",")));
        }
      }else if("search.exclude.datatypes".equalsIgnoreCase(params.getName())) {
        String nodetypes = params.getProperty("nodetypes");
        if(nodetypes != null) {
          excludeNodeTypes.addAll(Arrays.asList(nodetypes.split(",")));
        }
        String mimeTypes = params.getProperty("mimetypes");
        if(mimeTypes != null) {
          excludeMimeTypes.addAll(Arrays.asList(mimeTypes.split(",")));
        }
      }
    }
  }

  /**
   * Gets the exclude node types.
   *
   * @return the exclude node types
   */
  public List<String> getExcludeNodeTypes() { return this.excludeNodeTypes ; }

  /**
   * Gets the exclude mime types.
   *
   * @return the exclude mime types
   */
  public List<String> getExcludeMimeTypes() { return this.excludeMimeTypes; }

  /**
   * Gets the include node types.
   *
   * @return the include node types
   */
  public List<String> getIncludeNodeTypes() { return this.includeNodeTypes ; }

  /**
   * Gets the include mime types.
   *
   * @return the include mime types
   */
  public List<String> getIncludeMimeTypes() { return this.includeMimeTypes; }
}
