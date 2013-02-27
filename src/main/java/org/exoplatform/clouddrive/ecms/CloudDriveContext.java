/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.clouddrive.ecms;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContext.java 00000 Oct 22, 2012 pnedonosko $
 * 
 */
public class CloudDriveContext {

  protected final static String NAME = CloudDriveContext.class.getName();
  
  protected final static String PATHS = "CloudDriveContext_Paths".intern();
  
  protected final static String JAVASCRIPT = "CloudDriveContext_Javascript".intern();
  
  public static boolean addScript(RequestContext requestContext) {
//    @SuppressWarnings("unchecked")
//    Object script = requestContext.getAttribute(JAVASCRIPT);
//    if (script == null) {
//      JavascriptManager jsManager = requestContext.getJavascriptManager();
//      jsManager.importJavascript("clouddrive", "/cloud-drive/js");
//      requestContext.setAttribute(JAVASCRIPT, JAVASCRIPT);
//      return true;
//    } else {
//      return false; // already added
//    }
    
    return false;
  }
}
