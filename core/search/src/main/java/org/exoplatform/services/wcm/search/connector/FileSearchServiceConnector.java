/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.connector;

import java.net.URLEncoder;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The search should be capable to match files of the DMS. \
 */
public class FileSearchServiceConnector extends BaseContentSearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger(FileSearchServiceConnector.class.getName());

  public FileSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String[] getSearchedDocTypes() {
    return new String[]{NodetypeConstant.NT_FILE};
  }
  
  protected String[] getNodeTypes() {
    return new String[]{NodetypeConstant.NT_FILE};
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    return node.isNodeType(NodetypeConstant.NT_FILE) ? node : null;
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected String getPath(ResultNode node, SearchContext context) throws Exception {
    return documentService.getLinkInDocumentsApp(node.getPath());
  }
  
  /**
   * gets the image url
   * @return
   */
  @Override
  protected String getImageUrl(Node node) {
    try {
      String path = node.getPath().replaceAll("'", "\\\\'");
      String encodedPath = URLEncoder.encode(path, "utf-8");
      encodedPath = encodedPath.replaceAll ("%2F", "/");    //we won't encode the slash characters in the path
      String portalName = WCMCoreUtils.getPortalName();
      String restContextName = WCMCoreUtils.getRestContextName();
      String preferenceWS = node.getSession().getWorkspace().getName();
      String thumbnailImage = "/" + restContextName + "/thumbnailImage/medium/" + 
                              WCMCoreUtils.getRepository().getConfiguration().getName() + 
                              "/" + preferenceWS + encodedPath;
      return thumbnailImage;
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not get image link", e);
      }
      return super.getImageUrl(node);
    }
  }
}
