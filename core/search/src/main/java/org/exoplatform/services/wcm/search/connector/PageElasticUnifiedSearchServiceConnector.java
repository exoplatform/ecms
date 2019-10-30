/*
 * Copyright (C) 2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.wcm.search.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.search.base.EcmsSearchResult;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PageElasticUnifiedSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(PageElasticUnifiedSearchServiceConnector.class);

  public PageElasticUnifiedSearchServiceConnector(InitParams initParams,
                                                  ElasticSearchingClient client) {
    super(initParams, client);
  }

  @Override
  protected String getSourceFields() {
    List<String> fields = new ArrayList<>();
    fields.add("name");
    fields.add("pageTitle");
    fields.add("descriptions");
    fields.add("seo");
    fields.add("url");
    fields.add("siteName");
    fields.add("siteType");
    fields.add("pageRef");


    List<String> sourceFields = new ArrayList<>();
    for (String sourceField : fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  @Override
  protected String getSitesFilter(Collection<String> sitesCollection) {
    return null;
  }

  protected Collection<SearchResult> buildResult(String jsonResponse, SearchContext context) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    String lang = context.getParamValue(SearchContext.RouterParams.LANG.create());
    Collection<SearchResult> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult == null) {
      return results;
    }

    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

    for (Object jsonHit : jsonHits) {
      JSONObject hitSource = (JSONObject) ((JSONObject) jsonHit).get("_source");

      String title = getTitleFromJsonResult(hitSource);
      String url = getUrlFromJsonResult(hitSource, context);
      Long lastUpdatedDate = (Long) hitSource.get("lastUpdatedDate");
      if (lastUpdatedDate == null) lastUpdatedDate = new Date().getTime();
      Double score = (Double) ((JSONObject) jsonHit).get("_score");

      //
      String detail = "";
      if (hitSource.containsKey("descriptions")) {
        String descriptions = (String)hitSource.get("descriptions");
        if (StringUtils.isNotEmpty(descriptions)) {
          try {
            Map desc = (Map) parser.parse(descriptions);
            if (desc.containsKey(lang)) {
              detail = (String)desc.get(lang);
            }
          } catch (ParseException ex) {
            LOG.error(ex);
          }
        }
      }

      EcmsSearchResult result =
              //  new SearchResult(url, title, excerpt, detail, imageUrl, date, relevancy);
              new EcmsSearchResult(url,
                      url,
                      title,
                      "",
                      detail,
                      "/eXoSkin/skin/images/system/unified-search/page.png",
                      lastUpdatedDate,
                      score.longValue(),
                      "FileDefault",
                      "");

      results.add(result);
    }

    return results;

  }

  protected String getUrlFromJsonResult(JSONObject hitSource, SearchContext context) {
    String uri = (String)hitSource.get("url");
    String handler = WCMCoreUtils.getPortalName();
    String siteType = (String)hitSource.get("siteType");
    String siteName = (String)hitSource.get("siteName");

    String url = "#";
    try {
      url = "/" + handler + context.handler(handler)
              .lang("")
              .siteType(siteType)
              .siteName(siteName)
              .path(uri)
              .renderLink();
    } catch (Exception ex) {
      LOG.error(ex);
    }
    return url;
  }
}
