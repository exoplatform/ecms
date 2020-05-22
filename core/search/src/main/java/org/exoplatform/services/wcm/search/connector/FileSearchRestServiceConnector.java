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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.json.simple.JSONObject;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.base.EcmsSearchResult;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cms.impl.Utils;

/**
 * Search connector for files
 */
public class FileSearchRestServiceConnector extends FileSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(FileSearchRestServiceConnector.class.getName());
  
  private RepositoryService repositoryService;
  
  private DocumentService documentService;

  public FileSearchRestServiceConnector(InitParams initParams, ElasticSearchingClient client, RepositoryService repositoryService, DocumentService documentService) {
    super(initParams, client, repositoryService, documentService);
    this.repositoryService = repositoryService;
    this.documentService = documentService;
  }

  @Override
  protected String getSourceFields() {
    List<String> fields = Arrays.asList("name",
            "title",
            "tags",
            "workspace",
            "path",
            "author",
            "createdDate",
            "lastUpdatedDate",
            "lastModifier",
            "fileType",
            "fileSize",
            "version",
            "activityId");

    return fields.stream().map(field -> "\"" + field + "\"").collect(Collectors.joining(","));
  }
  
  @Override
  protected SearchResult buildHit(JSONObject jsonHit, SearchContext searchContext) {
    SearchResult searchResult = super.buildHit(jsonHit, searchContext);

    JSONObject hitSource = (JSONObject) jsonHit.get("_source");
    String id = (String) jsonHit.get("_id");
    String workspace = (String) hitSource.get("workspace");
    String nodePath = (String) hitSource.get("path");
    String fileType = (String) hitSource.get("fileType");
    String fileSize = (String) hitSource.get("fileSize");
    String lastEditor = (String) hitSource.get("lastModifier");
    String version = (String) hitSource.get("version");
    LinkedHashMap<String, String> breadcrumb = new LinkedHashMap<>();
    String drive = "";
    ExtendedSession session = null;
    try {
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession("collaboration", repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(id);
      breadcrumb = documentService.getFilePreviewBreadCrumb(node);
      drive = Utils.getSearchDocumentDrive(node);
    } catch (Exception e ) {
      LOG.error("Error while getting file node " + id, e);
    }
    finally {
      if (session != null) {
        session.logout();
      }
    }
    String restContextName =  WCMCoreUtils.getRestContextName();
    String repositoryName = null;
    try {
      repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      LOG.error("Cannot get repository name", e);
    }

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/').append(restContextName).append("/jcr/").
            append(repositoryName).append('/').
            append(workspace).append(nodePath);

    SearchResult ecmsSearchResult = new EcmsSearchResult(getUrl(nodePath),
            searchResult.getTitle(),
            searchResult.getDate(),
            searchResult.getRelevancy(),
            fileType,
            nodePath,
            breadcrumb,
            id,
            downloadUrl.toString(),
            version,
            getFormattedFileSize(fileSize),
            drive,
            lastEditor);

    return ecmsSearchResult;
  }
}
