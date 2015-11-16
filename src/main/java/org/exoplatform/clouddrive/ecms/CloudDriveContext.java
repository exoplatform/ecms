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
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.features.CloudDriveFeatures;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Initialize Cloud Drive support in portal request.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContext.java 00000 Oct 22, 2012 pnedonosko $
 */
public class CloudDriveContext {

  protected static final String JAVASCRIPT = "CloudDriveContext_Javascript".intern();

  protected static final Log    LOG        = ExoLogger.getLogger(CloudDriveContext.class);

  /**
   * Initialize request with Cloud Drive support for given JCR location and {@link CloudProvider}.
   * 
   * @param requestContext {@link RequestContext}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @throws CloudDriveException if cannot auth url from the provider
   */
  public static void init(RequestContext requestContext, String workspace, String nodePath) throws CloudDriveException {

    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null) {
      CloudDriveContext context = new CloudDriveContext(requestContext);

      CloudDriveFeatures features = WCMCoreUtils.getService(CloudDriveFeatures.class);
      CloudDriveService service = WCMCoreUtils.getService(CloudDriveService.class);
      boolean initContext = false;
      // add all providers to let related UI works for already connected and linked files
      for (CloudProvider provider : service.getProviders()) {
        // init cloud drive if we can connect to this user
        if (features.canCreateDrive(workspace, nodePath, requestContext.getRemoteUser(), provider)) {
          initContext = true;
          context.addProvider(provider);
        } // else, drive will be not initialized - thus not able to connect
      }

      // init cloud drive if at least one provider available
      if (initContext) {
        context.init(workspace, nodePath);
      }

      Map<String, String> contextMessages = messages.get();
      if (contextMessages != null) {
        for (Map.Entry<String, String> msg : contextMessages.entrySet()) {
          context.showInfo(msg.getKey(), msg.getValue());
        }
        contextMessages.clear();
      }

      requestContext.setAttribute(JAVASCRIPT, context);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context already initialized");
      }
    }
  }

  /**
   * Initialize already connected drives for a request and given JCR location. This method assumes that
   * request already initialized by {@link #init(RequestContext, String, String, CloudProvider)} method.
   * 
   * @param requestContext {@link RequestContext}
   * @param parent {@link Node}
   * @return boolean <code>true</code> if nodes successfully initialized, <code>false</code> if nodes already
   *         initialized
   * @throws RepositoryException
   * @throws CloudDriveException
   * @see {@link #init(RequestContext, String, String)}
   * @see {@link #init(RequestContext, String, String, CloudProvider)}
   */
  public static boolean initConnected(RequestContext requestContext, Node parent) throws RepositoryException,
                                                                                  CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      CloudDriveContext context = (CloudDriveContext) obj;
      context.addConnected(parent.getNodes());
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Context not initialized for adding of drive nodes.");
      }
      return false;
    }
  }

  /**
   * Show info notification to the user.
   * 
   * @param title {@link String}
   * @param message {@link String}
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  public static void showInfo(RequestContext requestContext, String title, String message) throws RepositoryException,
                                                                                           CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      CloudDriveContext context = (CloudDriveContext) obj;
      context.showInfo(title, message);
    } else {
      // store the message in thread local
      if (LOG.isDebugEnabled()) {
        LOG.debug("Context not initialized. Adding info message to local cache.");
      }

      Map<String, String> contextMessages = messages.get();
      if (contextMessages == null) {
        contextMessages = new LinkedHashMap<String, String>();
        messages.set(contextMessages);
      }
      contextMessages.put(title, message);
    }
  }

  // static variables

  private final static ThreadLocal<Map<String, String>> messages  = new ThreadLocal<Map<String, String>>();

  // instance methods

  private final RequireJS                               require;

  private final Set<String>                             nodes     = new HashSet<String>();

  private final Set<String>                             providers = new HashSet<String>();

  /**
   * Internal constructor.
   * 
   * @param requestContext {@link RequestContext}
   */
  private CloudDriveContext(RequestContext requestContext) {
    JavascriptManager js = ((WebuiRequestContext) requestContext).getJavascriptManager();
    this.require = js.require("SHARED/cloudDrive", "cloudDrive");
  }

  private CloudDriveContext init(String workspace, String nodePath) {
    require.addScripts("\ncloudDrive.init('" + workspace + "','" + nodePath + "');\n");
    return this;
  }

  private CloudDriveContext addConnected(NodeIterator nodes) throws CloudDriveException, RepositoryException {
    if (nodes.hasNext()) {
      CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
      StringBuilder map = new StringBuilder();
      // we construct JSON object on the fly
      map.append('{');
      int count = 0;
      while (nodes.hasNext()) {
        Node child = nodes.nextNode();
        CloudDrive drive = driveService.findDrive(child);
        if (drive != null) {
          String title = child.getProperty("exo:title").getString();
          if (!this.nodes.contains(title)) {
            map.append('"');
            map.append(title); // exo:title required for js side
            map.append("\":\"");
            map.append(drive.getUser().getProvider().getId());
            map.append("\",");
            count++;
            this.nodes.add(title);
          }
        }
      }

      if (count >= 1) {
        map.deleteCharAt(map.length() - 1); // remove last semicolon
        map.append('}');

        // we already "required" cloudDrive as AMD dependency in init()
        require.addScripts("\ncloudDrive.initConnected(" + map.toString() + ");\n");
      }
    }
    return this;
  }

  private CloudDriveContext addProvider(CloudProvider provider) throws CloudDriveException {
    String id = provider.getId();
    if (!providers.contains(id)) {
      // if provider cannot be converted to JSON then null will be
      String providerJson = new JSONObject(provider).toString();
      if (providerJson != null) {
        require.addScripts("\ncloudDrive.initProvider('" + id + "', " + providerJson.toString() + ");\n");
        providers.add(id);
      } else {
        LOG.error("Error converting cloud provider (" + provider.getName() + ") to JSON object (null).");
      }
    }
    return this;
  }

  private CloudDriveContext showInfo(String title, String text) {
    require.addScripts("\ncloudDrive.showInfo('" + title + "','" + text + "');\n");
    return this;
  }
}
