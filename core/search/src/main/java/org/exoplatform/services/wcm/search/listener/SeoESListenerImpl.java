package org.exoplatform.services.wcm.search.listener;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.mop.navigation.NavigationStore;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.search.connector.NavigationIndexingServiceConnector;

public class SeoESListenerImpl extends Listener<SEOService, PageMetadataModel> {

  private static final Log LOG = ExoLogger.getExoLogger(SeoESListenerImpl.class);

  private IndexingService indexingService;

  private NavigationStore navigationStore;

  public SeoESListenerImpl(IndexingService indexingService,
                           NavigationStore navigationStore) {
    this.indexingService = indexingService;
    this.navigationStore = navigationStore;
  }

  @Override
  public void onEvent(Event<SEOService, PageMetadataModel> event) throws Exception {
    PageMetadataModel seo = event.getData();
    updateIndex(seo);
  }

  private void updateIndex(PageMetadataModel seo) {
    for (String id : search(seo)) {
      indexingService.reindex(NavigationIndexingServiceConnector.TYPE, id);
    }
  }

  public List<String> search(PageMetadataModel seo) {
    String pageRef = seo.getPageReference();
    NodeData[] nodes = navigationStore.loadNodes(pageRef);
    List<String> ids = new ArrayList<>();
    for(NodeData node : nodes) {
      ids.add(node.getId());
    }
    return ids;
  }

  protected String buildFilteredQuery(String query, Collection<String> sites) {
    String escapedQuery = escapeReservedCharacters(query);

    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");

    //Score are always tracked, even with sort
    //https://www.impl.co/guide/en/elasticsearch/reference/current/search-request-sort.html#_track_scores
    esQuery.append("     \"track_scores\": true,\n");
    esQuery.append("     \"_source\": [\"nodeId\"],");
    esQuery.append("     \"query\": {\n");
    esQuery.append("        \"bool\" : {\n");
    esQuery.append("            \"must\" : {\n");
    esQuery.append("                \"query_string\" : {\n");
    esQuery.append("                    \"fields\" : [\"nodeId\"],\n");
    esQuery.append("                    \"query\" : \"" + escapedQuery + "\"\n");
    esQuery.append("                }\n");
    esQuery.append("            },\n");
    esQuery.append("            \"filter\" : {\n");
    esQuery.append("              \"bool\" : {\n");
    esQuery.append("                \"must\" : [\n");
    String sitesFilter = getSitesFilter(sites);
    if (StringUtils.isNotBlank(sitesFilter)) {
      esQuery.append("                  {\n");
      esQuery.append("                   \"bool\" : {\n");
      esQuery.append("                     \"should\" : \n");
      esQuery.append("                      " + sitesFilter + "\n");
      esQuery.append("                    }\n");
      esQuery.append("                  }");
    }
    esQuery.append("                  \n");
    esQuery.append("                ]\n");
    esQuery.append("              }\n");
    esQuery.append("            }");
    esQuery.append("        }\n");
    esQuery.append("     }\n");
    esQuery.append("}");

    LOG.debug("Search Query request to ES : {} ", esQuery);

    return esQuery.toString();
  }

  protected String getSitesFilter(Collection<String> sitesCollection) {
    if ((sitesCollection != null) && (sitesCollection.size() > 0)) {
      List<String> sites = new ArrayList<>();
      for (String site : sitesCollection) {
        sites.add("\"" + site + "\"");
      }
      String sitesList = "[" + StringUtils.join(sites, ",") + "]";
      return " [ { \"bool\" : {\n" +
              "         \"must_not\": {\n" +
              "           \"exists\" : { \"field\" : \"sites\" }\n" +
              "         }\n" +
              "       }\n" +
              "},\n" +
              "{\n" +
              "  \"terms\" : { \n" +
              "    \"sites\" : " + sitesList + "\n" +
              "  }\n" +
              "} ]";
    } else {
      return " { \"bool\" : " +
              "{\n" +
              "  \"must_not\": {\n" +
              "      \"exists\" : { \"field\" : \"sites\" }\n" +
              "   }\n" +
              "  }\n" +
              "}\n";
    }
  }

  protected String escapeReservedCharacters(String query) {
    if (StringUtils.isNotEmpty(query)) {
      return query.replaceAll("[" + Pattern.quote("+-=&|><!(){}\\[\\]^\"*?:\\/") + "]",
              Matcher.quoteReplacement("\\\\") + "$0");
    } else {
      return query;
    }
  }

  protected List<String> buildResult(String jsonResponse) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    List<String> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult != null) {
      JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

      if (jsonHits != null) {
        for (Object jsonHit : jsonHits) {
          results.add((String) ((JSONObject) jsonHit).get("_id"));
        }
      }
    }

    return results;

  }

}
