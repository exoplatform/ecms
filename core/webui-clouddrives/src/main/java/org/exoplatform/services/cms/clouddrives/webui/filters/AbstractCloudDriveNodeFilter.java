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
package org.exoplatform.services.cms.clouddrives.webui.filters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Filter for cloud files.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveFiler.java 00000 Nov 5, 2012 pnedonosko $
 */
public abstract class AbstractCloudDriveNodeFilter implements UIExtensionFilter {

  protected static final Log    LOG                     = ExoLogger.getLogger(AbstractCloudDriveNodeFilter.class);

  /** The Constant CONTENTVIEWER_REST_PATH. */
  protected static final String CONTENTVIEWER_REST_PATH = "/contentviewer/";

  /** The min size. */
  protected long                minSize;

  /** The max size. */
  protected long                maxSize;

  /** The providers. */
  protected List<String>        providers;

  /**
   * Instantiates a new abstract cloud drive node filter.
   */
  public AbstractCloudDriveNodeFilter() {
    this(Collections.<String> emptyList());
  }

  /**
   * Instantiates a new abstract cloud drive node filter.
   *
   * @param providers the providers
   */
  public AbstractCloudDriveNodeFilter(List<String> providers) {
    this(providers, 0, Long.MAX_VALUE);
  }

  /**
   * Instantiates a new abstract cloud drive node filter.
   *
   * @param minSize the min size
   * @param maxSize the max size
   */
  public AbstractCloudDriveNodeFilter(long minSize, long maxSize) {
    this(Collections.<String> emptyList(), minSize, maxSize);
  }

  /**
   * Instantiates a new abstract cloud drive node filter.
   *
   * @param providers the providers
   * @param minSize the min size
   * @param maxSize the max size
   */
  public AbstractCloudDriveNodeFilter(List<String> providers, long minSize, long maxSize) {
    this.providers = providers;
    this.minSize = minSize >= 0 ? minSize : 0;
    this.maxSize = maxSize;
  }

  /**
   * {@inheritDoc}
   */
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) {
      return true;
    } else {
      boolean accepted = false;
      Node contextNode = (Node) context.get(Node.class.getName());
      if (contextNode == null) {
        UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
        if (uiExplorer != null) {
          contextNode = uiExplorer.getCurrentNode();
        }

        if (contextNode == null) {
          WebuiRequestContext reqContext = WebuiRequestContext.getCurrentInstance();
          UIApplication uiApp = reqContext.getUIApplication();
          UIJcrExplorerContainer jcrExplorerContainer = uiApp.getChild(UIJcrExplorerContainer.class);
          if (jcrExplorerContainer != null) {
            UIJCRExplorer jcrExplorer = jcrExplorerContainer.getChild(UIJCRExplorer.class);
            contextNode = jcrExplorer.getCurrentNode();
          }
          // case of file preview in Social activity stream
          if (contextNode == null) {
            UIActivitiesContainer uiActivitiesContainer = uiApp.findFirstComponentOfType(UIActivitiesContainer.class);
            if (uiActivitiesContainer != null) {
              PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();
              if (uiPopupContainer != null) {
                UIBaseNodePresentation docViewer = uiPopupContainer.findComponentById("UIDocViewer");
                if (docViewer != null) {
                  contextNode = docViewer.getNode();
                }
              }
            }
          }
          // case of ContentViewerRESTService (actual for PLF 4.4 and PLF 5.0)
          if (contextNode == null && PortalRequestContext.class.isAssignableFrom(reqContext.getClass())) {
            try {
              PortalRequestContext portalReqContext = PortalRequestContext.class.cast(reqContext);
              String reqPathInfo = portalReqContext.getControllerContext().getRequest().getPathInfo();
              if (reqPathInfo.startsWith(CONTENTVIEWER_REST_PATH)) {
                // It's string of content like:
                // /contentviewer/repository/collaboration/4e6a36fcc0a8016529a3148700beecec
                String[] reqParams = reqPathInfo.substring(CONTENTVIEWER_REST_PATH.length()).split("/");
                if (reqParams.length >= 3) {
                  String repository = reqParams[0];
                  String workspace = reqParams[1];
                  String uuid = reqParams[2];
                  RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
                  SessionProvider sp = WCMCoreUtils.getUserSessionProvider();
                  contextNode = sp.getSession(workspace, repositoryService.getRepository(repository)).getNodeByUUID(uuid);
                }
              }
            } catch (AccessDeniedException e) {
              // no access for current user
            } catch (ItemNotFoundException e) {
              // such item not found
            } catch (Throwable e) {
              // ignore and assume we don't have a context node
              if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot find context node in the request: " + e.getMessage());
              }
            }
          }
        }
      }

      if (contextNode != null) {
        accepted = accept(contextNode);
      }
      return accepted;
    }
  }

  /**
   * {@inheritDoc}
   */
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  /**
   * {@inheritDoc}
   */
  public void onDeny(Map<String, Object> context) throws Exception {
  }

  // ****************** internals ******************

  /**
   * Accept provider.
   *
   * @param provider the provider
   * @return true, if successful
   */
  protected boolean acceptProvider(CloudProvider provider) {
    if (providers.size() > 0) {
      boolean accepted = providers.contains(provider.getId());
      if (accepted) {
        return true;
      } else {
        // TODO compare by class inheritance
        return false;
      }
    } else {
      return true;
    }
  }

  /**
   * Accept.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  protected abstract boolean accept(Node node) throws RepositoryException;
}
