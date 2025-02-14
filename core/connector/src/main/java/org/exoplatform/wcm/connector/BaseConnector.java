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
package org.exoplatform.wcm.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.exoplatform.ecm.connector.fckeditor.FCKFolderHandler;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Sep 10, 2008
 */
/**
 * The Class BaseConnector.
 */
public abstract class BaseConnector {

  /** The folder handler. */
  protected FCKFolderHandler    folderHandler;

  /** The file upload handler. */
  protected FileUploadHandler   fileUploadHandler;

  /** The repository service. */
  protected RepositoryService   repositoryService;

  /** The log. */
  private static final Log      LOG                           = ExoLogger.getLogger(BaseConnector.class.getName());

  /** The link manager. */
  protected LinkManager         linkManager;

  /** The Constant LAST_MODIFIED_PROPERTY. */
  protected static final String LAST_MODIFIED_PROPERTY        = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  /**
   * Instantiates a new base connector.
   */
  protected BaseConnector() {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    linkManager = WCMCoreUtils.getService(LinkManager.class);

    folderHandler = new FCKFolderHandler();
    fileUploadHandler = new FileUploadHandler();
  }

  /**
   * Gets the response.
   *
   * @param document the document
   * @return the response
   */
  protected Response getResponse(Document document) {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  /**
   * Gets the jcr content.
   *
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   * @return the jcr content
   * @throws Exception the exception
   */
  protected Node getContent(String workspaceName,
                            String jcrPath,
                            String nodeTypeFilter,
                            boolean isSystemSession) {
    if (jcrPath == null || jcrPath.trim().length() == 0)
      return null;
    try {
      SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider() :
                                                        WCMCoreUtils.getUserSessionProvider();
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspaceName, repository);
      Node content = (Node) session.getItem(jcrPath);
      if (content.isNodeType("exo:taxonomyLink")) {
        content = linkManager.getTarget(content);
      }

      if (nodeTypeFilter == null || content.isNodeType(nodeTypeFilter)) {
        return content;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform getContent: ", e);
      }
    }
    return null;
  }

}
