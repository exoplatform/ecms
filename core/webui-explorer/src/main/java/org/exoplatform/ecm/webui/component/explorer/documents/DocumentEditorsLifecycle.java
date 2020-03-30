/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletRequest;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.cometd.CometdConfig;
import org.exoplatform.services.cms.documents.cometd.CometdDocumentsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class DocumentEditorsLifecycle.
 */
public class DocumentEditorsLifecycle implements ApplicationLifecycle<WebuiRequestContext> {

  /** The Constant USERID_ATTRIBUTE. */
  public static final String USERID_ATTRIBUTE             = "DocumentEditorsContext.userId";

  /** The Constant DOCUMENT_WORKSPACE_ATTRIBUTE. */
  public static final String DOCUMENT_WORKSPACE_ATTRIBUTE = "DocumentEditorsContext.document.workspace";

  /** The Constant DOCUMENT_PATH_ATTRIBUTE. */
  public static final String DOCUMENT_PATH_ATTRIBUTE      = "DocumentEditorsContext.document.path";

  /** The Constant LOG. */
  protected static final Log LOG                          = ExoLogger.getLogger(DocumentEditorsLifecycle.class);

  /**
   * Instantiates a new DocumentEditorsLifecycle lifecycle.
   */
  public DocumentEditorsLifecycle() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInit(Application app) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStartRequest(Application app, WebuiRequestContext context) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDestroy(Application app) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    RequestContext parentContext = context.getParentAppRequestContext();
    UIJCRExplorer explorer = context.getUIApplication().findFirstComponentOfType(UIJCRExplorer.class);
    if (explorer != null && parentContext != null) {
      try {
        String userName = context.getRemoteUser();
        Node node = explorer.getCurrentNode();
        String nodeWs = node.getSession().getWorkspace().getName();
        String nodePath = node.getPath();
        if (isNotSameUserDocument(userName, nodeWs, nodePath, parentContext)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Init documents explorer for {}, node: {}:{}, context: {}", userName, nodeWs, nodePath, parentContext);
          }
          parentContext.setAttribute(USERID_ATTRIBUTE, userName);
          parentContext.setAttribute(DOCUMENT_WORKSPACE_ATTRIBUTE, nodeWs);
          parentContext.setAttribute(DOCUMENT_PATH_ATTRIBUTE, nodePath);
          // This will init explorer even for docs that cannot be edited
          // by the user (lack of permissions)
          initEditorsModule(context, node.getUUID(), node.getSession().getWorkspace().getName());
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Already initialized documents explorer for {}, node: {}:{}, context: {}",
                      userName,
                      nodeWs,
                      nodePath,
                      parentContext);
          }
        }
      } catch (Exception e) {
        LOG.error("Couldn't read document of node", e);
      }
    } else if (LOG.isDebugEnabled()) {
      LOG.debug("Explorer or portal context not found, explorer: {}, context: {}", explorer, parentContext);
    }
  }

  /**
   * Gets the servlet request associated with given context.
   *
   * @param context the context
   * @return the servlet request
   */
  protected ServletRequest getServletRequest(WebuiRequestContext context) {
    try {
      // First we assume it's PortalRequestContext
      return context.getRequest();
    } catch (ClassCastException e) {
      // Then try get portlet's parent context
      RequestContext parentContext = context.getParentAppRequestContext();
      if (parentContext != null && PortalRequestContext.class.isAssignableFrom(parentContext.getClass())) {
        return PortalRequestContext.class.cast(parentContext).getRequest();
      }
    }
    return null;
  }

  /**
   * Inits the editors module.
   *
   * @param context the context
   * @param fileId the file id
   * @throws RepositoryException 
   */
  protected void initEditorsModule(WebuiRequestContext context, String fileId, String workspace) throws RepositoryException {
    RequireJS require = context.getJavascriptManager().require("SHARED/editorbuttons", "editorbuttons");
    CometdDocumentsService cometdService = context.getApplication()
                                                  .getApplicationServiceContainer()
                                                  .getComponentInstanceOfType(CometdDocumentsService.class);
    DocumentService documentService = context.getApplication()
                                             .getApplicationServiceContainer()
                                             .getComponentInstanceOfType(DocumentService.class);
    CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(),
                                               cometdService.getUserToken(context.getRemoteUser()),
                                               PortalContainer.getCurrentPortalContainerName());

    List<String> providers = documentService.getDocumentEditorProviders()
                                            .stream()
                                            .map(provider -> provider.getProviderName())
                                            .collect(Collectors.toList());
    try {
      String providersJson = new JsonGeneratorImpl().createJsonArray(providers).toString();
      require.addScripts("editorbuttons.init('" + context.getRemoteUser() + "' ," + cometdConf.toJSON() + ", " + providersJson
          + ");");
      String currentProvider = documentService.getCurrentDocumentProvider(fileId, workspace);
      currentProvider = currentProvider != null ? new StringBuilder("'").append(currentProvider).append("'").toString() : "null";
      require.addScripts("editorbuttons.initExplorer('" + fileId + "', " + providersJson + ", " + currentProvider + ");");
    } catch (JsonException e) {
      LOG.warn("Cannot generate JSON for initializing editors module. {}", e.getMessage());
    }

  }

  /**
   * Checks if is not same user document.
   *
   * @param userName the user name
   * @param nodeWs the node ws
   * @param nodePath the node path
   * @param parentContext the parent context
   * @return true, if is not same user document
   */
  private boolean isNotSameUserDocument(String userName, String nodeWs, String nodePath, RequestContext parentContext) {
    return !(userName.equals(parentContext.getAttribute(USERID_ATTRIBUTE))
        && nodeWs.equals(parentContext.getAttribute(DOCUMENT_WORKSPACE_ATTRIBUTE))
        && nodePath.equals(parentContext.getAttribute(DOCUMENT_PATH_ATTRIBUTE)));
  }

}
