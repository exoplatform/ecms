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

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.URLEncoder;

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

  @Override
  protected String getPreviewUrl(ResultNode node, SearchContext context) throws Exception {
    String restContextName =  WCMCoreUtils.getRestContextName();

    Session session = node.getSession();
    String repositoryName = ((ManageableRepository) session.getRepository()).getConfiguration().getName();
    String workspaceName = node.getSession().getWorkspace().getName();

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/').append(restContextName).append("/jcr/").
            append(WCMCoreUtils.getRepository().getConfiguration().getName()).append('/').
            append(workspaceName).append(node.getPath());

    // get document author
    String authorUsername = null;
    if(node.hasProperty("exo:owner")) {
      authorUsername = node.getProperty("exo:owner").getString();
    }

    StringBuilder url = new StringBuilder("javascript:require(['SHARED/documentPreview'], function(documentPreview) {documentPreview.init({doc:{");
    if(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
      url.append("id:'").append(node.getUUID()).append("',");
    }
    url.append("fileType:'").append(getFileType(node)).append("',");
    url.append("title:'").append(getTitleResult(node)).append("',");
    url.append("path:'").append(node.getPath())
            .append("', repository:'").append(repositoryName)
            .append("', workspace:'").append(workspaceName)
            .append("', downloadUrl:'").append(downloadUrl.toString())
            .append("', openUrl:'").append(documentService.getLinkInDocumentsApp(node.getPath()))
            .append("'}");
    if(authorUsername != null) {
      url.append(",author:{username:'").append(authorUsername).append("'}");
    }
    //add void(0) to make firefox execute js
    url.append("})});void(0);");

    return url.toString();
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
