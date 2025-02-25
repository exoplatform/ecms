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
package org.exoplatform.services.wcm.core;

import java.util.Collection;
import java.util.Iterator;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 20, 2008
 */
public class WCMConfigurationService {

  public static final String PARAMETERIZED_PAGE_URI = "parameterizedPageURI";

  public static final String EDITOR_PAGE_URI        = "editorPageURI";

  public static final String EDIT_PAGE_URI          = "editPageURI";

  private ExoProperties      runtimeContextParams;

  public WCMConfigurationService(InitParams initParams) {
    Iterator<PropertiesParam> iterator = initParams.getPropertiesParamIterator();
    while (iterator.hasNext()) {
      PropertiesParam param = iterator.next();
      if ("RuntimeContextParams".equalsIgnoreCase(param.getName())) {
        runtimeContextParams = param.getProperties();
      }
    }
  }

  public String getRuntimeContextParam(String paramName) {
    if (runtimeContextParams != null)
      return runtimeContextParams.get(paramName);
    return null;
  }

  public Collection<String> getRuntimeContextParams() {
    if (runtimeContextParams != null)
      return runtimeContextParams.values();
    return null;
  }

}
