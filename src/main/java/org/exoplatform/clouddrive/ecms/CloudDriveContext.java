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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContext.java 00000 Oct 22, 2012 pnedonosko $
 */
public class CloudDriveContext {

  protected final static String JAVASCRIPT       = "CloudDriveContext_Javascript".intern();

  protected final static String JAVASCRIPT_NODES = "CloudDriveContext_JavascriptNodes".intern();

  public static boolean init(RequestContext requestContext, String workspace, String nodePath) {
    Object script = requestContext.getAttribute(JAVASCRIPT);
    if (script == null) {
      // XXX yes... nasty cast
      JavascriptManager js = ((WebuiRequestContext) requestContext).getJavascriptManager();
      js.require("SHARED/cloudDrive", "cloudDrive").addScripts("\ncloudDrive.init('" + workspace + "','"
          + nodePath + "');\n");

      requestContext.setAttribute(JAVASCRIPT, JAVASCRIPT);
      return true;
    } else {
      return false; // already added
    }
  }

  public static boolean initNodes(RequestContext requestContext, Node parent) throws RepositoryException {
    Object script = requestContext.getAttribute(JAVASCRIPT_NODES);
    if (script == null) {
      NodeIterator childs = parent.getNodes();
      if (childs.hasNext()) {
        CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
        StringBuilder map = new StringBuilder();
        // we construct JSON object on the fly
        map.append('{');
        do {
          Node child = childs.nextNode();
          CloudDrive drive = driveService.findDrive(child);
          if (drive != null) {
            map.append('"');
            map.append(child.getName()); // exo:title?
            map.append("\":\"");
            map.append(drive.getUser().getProvider().getId());
            map.append("\",");
          }
        } while (childs.hasNext());
        if (map.length() > 1) {
          map.deleteCharAt(map.length() - 1); // remove last semicolon
          map.append('}');

          // XXX still... nasty cast
          JavascriptManager js = ((WebuiRequestContext) requestContext).getJavascriptManager();

          // XXX we already "required" cloudDrive as AMD dependency in init()
          js.require("SHARED/cloudDrive", "cloudDrive").addScripts("\ncloudDrive.initNodes("
              + map.toString() + ");\n");

          requestContext.setAttribute(JAVASCRIPT_NODES, JAVASCRIPT_NODES);
          return true;
        }
      }
    } // already added
    return false;
  }
}
