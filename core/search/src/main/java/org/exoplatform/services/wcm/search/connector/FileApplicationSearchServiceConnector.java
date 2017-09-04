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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchFilter;
import org.exoplatform.commons.search.es.ElasticSearchFilterType;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Search connector for files for the Sites Explorer application
 */
public class FileApplicationSearchServiceConnector extends FileSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(FileApplicationSearchServiceConnector.class.getName());

  private String filteredWorkspace;

  private String filteredPath;

  public FileApplicationSearchServiceConnector(InitParams initParams, ElasticSearchingClient client, RepositoryService repositoryService, DocumentService documentService) {
    super(initParams, client, repositoryService, documentService);
  }

  public Collection<SearchResult> appSearch(String workspace, String path, String query, int offset, int limit, String sort, String order) {
    filteredWorkspace = workspace;
    filteredPath = path;
    if(StringUtils.isNotEmpty(filteredPath) && !filteredPath.endsWith("/")) {
      filteredPath += "/";
    }
    return super.search(null, query, null, offset, limit, sort, order);
  }

  @Override
  protected String getAdditionalFilters(List<ElasticSearchFilter> filters) {
    if(StringUtils.isNotEmpty(filteredWorkspace)) {
      filters = addFilter(filters, new ElasticSearchFilter(ElasticSearchFilterType.FILTER_BY_TERM, "workspace", filteredWorkspace));
    }

    if(StringUtils.isNotEmpty(filteredPath)) {
      filters = addFilter(filters, new ElasticSearchFilter(ElasticSearchFilterType.FILTER_CUSTOM, "", "{ " +
                "\"prefix\" : { " +
                  "\"path\" :  { " +
                    "\"value\" : \"" + filteredPath + "\" " +
                  "} " +
                "} " +
              "}"));
    }
    return super.getAdditionalFilters(filters);
  }

  private List<ElasticSearchFilter> addFilter(List<ElasticSearchFilter> filters, ElasticSearchFilter elasticSearchFilter) {
    if(filters == null) {
      filters = new ArrayList<>();
    }
    filters.add(elasticSearchFilter);
    return filters;
  }
}
