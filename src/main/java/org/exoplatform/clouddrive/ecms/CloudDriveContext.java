/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.clouddrive.ecms;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.json.JSONObject;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

/**
 * Initialize Cloud Drive support in portal request.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContext.java 00000 Oct 22, 2012 pnedonosko $
 */
public class CloudDriveContext {

  /** The Constant JAVASCRIPT. */
  protected static final String JAVASCRIPT = "CloudDriveContext_Javascript".intern();

  /** The Constant LOG. */
  protected static final Log    LOG        = ExoLogger.getLogger(CloudDriveContext.class);

  /**
   * Initialize request with Cloud Drive support from given WebUI component.
   *
   * @param uiComponent {@link UIComponent}
   * @throws Exception the exception
   */
  public static void init(UIComponent uiComponent) throws Exception {
    Node contextNode;
    UIJCRExplorer uiExplorer = uiComponent.getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // when in document explorer
      contextNode = uiExplorer.getCurrentNode();
    } else if (uiComponent.getParent() instanceof UIBaseNodePresentation) {
      // when in social activity stream (file view)
      UIBaseNodePresentation docViewer = uiComponent.getParent();
      contextNode = docViewer.getNode();
    } else {
      contextNode = null;
    }
    if (contextNode != null) {
      // we store current node in the context
      init(WebuiRequestContext.getCurrentInstance(), contextNode.getSession().getWorkspace().getName(), contextNode.getPath());
    } else {
      LOG.error("Cannot find ancestor context node in component " + uiComponent + ", parent: " + uiComponent.getParent());
    }
  }

  /**
   * Initialize request with Cloud Drive support (global settings only).
   *
   * @param requestContext the request context
   * @throws CloudDriveException the cloud drive exception
   */
  public static void init(RequestContext requestContext) throws CloudDriveException {
    init(requestContext, null, null);
  }

  /**
   * Initialize request with Cloud Drive support for given JCR location and
   * {@link CloudProvider}.
   * 
   * @param requestContext {@link RequestContext}
   * @param workspace {@link String} can be <code>null</code>
   * @param nodePath {@link String} can be <code>null</code>
   * @throws CloudDriveException if cannot auth url from the provider
   */
  public static void init(RequestContext requestContext, String workspace, String nodePath) throws CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null) {
      CloudDriveContext context = new CloudDriveContext(requestContext);

      CloudDriveService service = WCMCoreUtils.getService(CloudDriveService.class);
      // add all providers to let related UI works for already connected and
      // linked files
      for (CloudProvider provider : service.getProviders()) {
        context.addProvider(provider);
      }

      if (workspace != null && nodePath != null) {
        context.init(workspace, nodePath);
      } else {
        context.init();
      }

      Map<String, String> contextMessages = messages.get();
      if (contextMessages != null) {
        for (Map.Entry<String, String> msg : contextMessages.entrySet()) {
          context.showInfo(msg.getKey(), msg.getValue());
        }
        contextMessages.clear();
      }

      requestContext.setAttribute(JAVASCRIPT, context);
    } else if (CloudDriveContext.class.isAssignableFrom(obj.getClass())) {
      CloudDriveContext context = CloudDriveContext.class.cast(obj);
      if (!context.hasContextNode() && workspace != null && nodePath != null) {
        context.init(workspace, nodePath);
      }
    }
  }

  /**
   * Initialize already connected drives for a request and given JCR location.
   * This method assumes that request already initialized by
   * {@link #init(RequestContext, String, String)} method.
   *
   * @param requestContext {@link RequestContext}
   * @param parent {@link Node}
   * @return boolean <code>true</code> if nodes successfully initialized,
   *         <code>false</code> if nodes already initialized
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   * @see #init(RequestContext, String, String)
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
   * @param requestContext the request context
   * @param title {@link String}
   * @param message {@link String}
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
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

  /** The Constant messages. */
  private final static ThreadLocal<Map<String, String>> messages  = new ThreadLocal<Map<String, String>>();

  // instance methods

  /** The require. */
  private final RequireJS                               require;

  /** The nodes. */
  private final Set<String>                             nodes     = new HashSet<String>();

  /** The providers. */
  private final Set<String>                             providers = new HashSet<String>();

  private boolean                                       hasContextNode;

  /**
   * Internal constructor.
   * 
   * @param requestContext {@link RequestContext}
   */
  private CloudDriveContext(RequestContext requestContext) {
    JavascriptManager js = ((WebuiRequestContext) requestContext).getJavascriptManager();
    this.require = js.require("SHARED/cloudDrive", "cloudDrive");
    this.hasContextNode = false;
  }

  /**
   * Inits the only global context.
   *
   * @return the cloud drive context
   */
  private CloudDriveContext init() {
    require.addScripts("\ncloudDrive.init();\n");
    return this;
  }

  /**
   * Inits the context with a node.
   *
   * @param workspace the workspace
   * @param nodePath the node path
   * @return the cloud drive context
   */
  private CloudDriveContext init(String workspace, String nodePath) {
    require.addScripts("\ncloudDrive.init('" + workspace + "','" + nodePath + "');\n");
    hasContextNode = true;
    return this;
  }

  /**
   * Checks for context node.
   *
   * @return true, if successful
   */
  private boolean hasContextNode() {
    return hasContextNode;
  }

  /**
   * Adds the connected.
   *
   * @param nodes the nodes
   * @return the cloud drive context
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
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

  /**
   * Adds the provider.
   *
   * @param provider the provider
   * @return the cloud drive context
   * @throws CloudDriveException the cloud drive exception
   */
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

  /**
   * Show info.
   *
   * @param title the title
   * @param text the text
   * @return the cloud drive context
   */
  private CloudDriveContext showInfo(String title, String text) {
    require.addScripts("\ncloudDrive.showInfo('" + title + "','" + text + "');\n");
    return this;
  }
}
