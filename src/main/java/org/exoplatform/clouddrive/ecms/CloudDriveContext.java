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

import java.util.HashSet;
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
   * @param provider {@link CloudProvider} optional, if <code>null</code> then any provider will be assumed
   * @return boolean <code>true</code> if request successfully initialized, <code>false</code> if request
   *         already
   *         initialized
   * @throws CloudDriveException if cannot auth url from the provider
   */
  public static boolean init(RequestContext requestContext,
                             String workspace,
                             String nodePath,
                             CloudProvider provider) throws CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null) {
      CloudDriveContext context = new CloudDriveContext(requestContext);

      CloudDriveFeatures features = WCMCoreUtils.getService(CloudDriveFeatures.class);
      // init cloud drive if we can connect to this user
      if (features.canCreateDrive(workspace, nodePath, requestContext.getRemoteUser(), provider)) {
        context.init(workspace, nodePath);
      } // else, drive will be not initialized - thus not able to connect

      if (provider != null) {
        // add provider's default params
        context.addProvider(provider);
      }

      requestContext.setAttribute(JAVASCRIPT, context);
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context already initialized");
      }
      return false;
    }
  }

  /**
   * Initialize request with Cloud Drive support for given JCR location.
   * 
   * @param requestContext {@link RequestContext}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @return boolean <code>true</code> if request successfully initialized, <code>false</code> if request
   *         already initialized
   * @throws CloudDriveException if cannot auth url from the provider
   */
  public static boolean init(RequestContext requestContext, String workspace, String nodePath) throws CloudDriveException {
    return init(requestContext, workspace, nodePath, null);
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
  public static boolean initNodes(RequestContext requestContext, Node parent) throws RepositoryException,
                                                                          CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      CloudDriveContext context = (CloudDriveContext) obj;
      context.addNodes(parent.getNodes());
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Context not initialized for adding of drive nodes.");
      }
      return false;
    }
  }

  /**
   * Initialize request with Cloud Drive providers {@link CloudProvider}.
   * 
   * @param requestContext {@link RequestContext}
   * @param providers array of {@link CloudProvider} to add to the request context
   * @throws CloudDriveException if cannot auth url from the provider
   */
  public static boolean initProviders(RequestContext requestContext, CloudProvider... providers) throws CloudDriveException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      CloudDriveContext context = (CloudDriveContext) obj;
      for (CloudProvider p : providers) {
        context.addProvider(p);
      }
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Context not initialized for adding of providers.");
      }
      return false;
    }
  }

  // instance methods

  private final RequireJS   require;

  private final Set<String> nodes     = new HashSet<String>();

  private final Set<String> providers = new HashSet<String>();

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

  private CloudDriveContext addNodes(NodeIterator nodes) throws CloudDriveException, RepositoryException {
    if (nodes.hasNext()) {
      CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
      StringBuilder map = new StringBuilder();
      // we construct JSON object on the fly
      map.append('{');
      int count = 0;
      do {
        Node child = nodes.nextNode();
        CloudDrive drive = driveService.findDrive(child);
        if (drive != null) {
          String title = child.getProperty("exo:title").getString();
          if (!this.nodes.contains(title)) {
            map.append('"');
            // map.append(child.getName()); // exo:title required for js side
            map.append(title);
            map.append("\":\"");
            map.append(drive.getUser().getProvider().getId());
            map.append("\",");
            count++;
            this.nodes.add(title);
          }
        }
      } while (nodes.hasNext());
      if (count >= 1) {
        map.deleteCharAt(map.length() - 1); // remove last semicolon
        map.append('}');

        // we already "required" cloudDrive as AMD dependency in init()
        require.addScripts("\ncloudDrive.initNodes(" + map.toString() + ");\n");
      }
    }
    return this;
  }

  private CloudDriveContext addProvider(CloudProvider provider) throws CloudDriveException {
    String id = provider.getId();
    if (!providers.contains(id)) {
      require.addScripts("\ncloudDrive.initProvider('" + id + "', '" + provider.getAuthUrl() + "');\n");
      providers.add(id);
    }
    return this;
  }
}
