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

import java.util.Map;

import org.exoplatform.services.cms.scripts.CmsScript;

public class PostNodeSaveInterceptor implements CmsScript {
  
  public PostNodeSaveInterceptor() {
  }
  
  public void execute(Object context) {
    String path = (String) context;       

    println("PostNodeSaveInterceptor: All available data in current context are " + path);
  }

  public void setParams(String[] params) {
    println("PostNodeSaveInterceptor: All available data in current params are " + params);
  }

}