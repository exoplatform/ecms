/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.ecms.legacy.search.es;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.ecms.legacy.search.SearchServiceConnector;
import org.exoplatform.ecms.legacy.search.data.SearchContext;
import org.exoplatform.ecms.legacy.search.data.SearchResult;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 7/30/15
 * @deprecated Copied from commons-search to this module.
 *  Should be reworked to be more simple.
 */
@Deprecated(forRemoval = true, since = "6.0.0")
public class ElasticSearchServiceConnector extends SearchServiceConnector {
  private static final Log LOG = ExoLogger.getLogger(ElasticSearchServiceConnector.class);

  public static final String HIGHLIGHT_FRAGMENT_SIZE_PARAM_NAME = "highlightFragmentSize";
  public static final int HIGHLIGHT_FRAGMENT_SIZE_DEFAULT_VALUE = 100;
  public static final String HIGHLIGHT_FRAGMENT_NUMBER_PARAM_NAME = "highlightFragmentNumber";
  public static final int HIGHLIGHT_FRAGMENT_NUMBER_DEFAULT_VALUE = 3;

  private final ElasticSearchingClient client;

  public static final String           GROUP                                   = "group";

  public static final String           WIKI_TYPE                               = "wikiType";

  //ES connector information
  //Index is optional: if null, search on all the cluster
  private String index;

  //Type is optional: if null, search on all the index
  private List<String> searchFields;
  private List<String> boostedSearchFields;
  private List<String> searchFieldsWithBoost;

  private int highlightFragmentSize;
  private int highlightFragmentNumber;

  //SearchResult information
  private String img;
  private String titleElasticFieldName = "title";
  private String updatedDateElasticFieldName = "lastUpdatedDate";

  private Map<String, String> sortMapping = new HashMap<>();

  public ElasticSearchServiceConnector(InitParams initParams, ElasticSearchingClient client) {
    super(initParams);
    this.client = client;
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.index = param.getProperty("index");
    if (StringUtils.isNotBlank(param.getProperty("titleField"))) this.titleElasticFieldName = param.getProperty("titleField");
    if (StringUtils.isNotBlank(param.getProperty("updatedDateField"))) this.updatedDateElasticFieldName = param.getProperty("updatedDateField");
    this.searchFields = new ArrayList<>(Arrays.asList(param.getProperty("searchFields").split(",")));
    if (StringUtils.isBlank(param.getProperty("boostedSearchFields"))) {
      if (this.searchFields.contains(this.titleElasticFieldName)) {
        this.boostedSearchFields = Collections.singletonList(this.titleElasticFieldName);
      }
    } else {
      this.boostedSearchFields = new ArrayList<>(Arrays.asList(param.getProperty("boostedSearchFields").split(",")));
    }
    if (this.boostedSearchFields != null && !this.boostedSearchFields.isEmpty()) {
      this.searchFieldsWithBoost = this.searchFields.stream().map(searchField -> {
        if (this.boostedSearchFields.contains(searchField)) {
          searchField = searchField + "^5"; // Boost 5
        }
        return searchField;
      }).collect(Collectors.toList());
    } else {
      this.searchFieldsWithBoost = this.searchFields;
    }

    // highlight fragment size
    String highlightFragmentSizeParamValue = param.getProperty(HIGHLIGHT_FRAGMENT_SIZE_PARAM_NAME);
    if(highlightFragmentSizeParamValue != null) {
      try {
        this.highlightFragmentSize = Integer.valueOf(highlightFragmentSizeParamValue);
      } catch (NumberFormatException e) {
        this.highlightFragmentSize = HIGHLIGHT_FRAGMENT_SIZE_DEFAULT_VALUE;
        LOG.warn("Value of param highlightFragmentSize of search connector " + this.getClass().getName()
                + " is not a valid number (" + highlightFragmentSizeParamValue + "), default value will be used ("
                + HIGHLIGHT_FRAGMENT_SIZE_DEFAULT_VALUE + ")");
      }
    } else {
      this.highlightFragmentSize = HIGHLIGHT_FRAGMENT_SIZE_DEFAULT_VALUE;
    }

    // highlight fragment number
    String highlightFragmentNumberParamValue = param.getProperty(HIGHLIGHT_FRAGMENT_NUMBER_PARAM_NAME);
    if(highlightFragmentNumberParamValue != null) {
      try {
        this.highlightFragmentNumber = Integer.valueOf(highlightFragmentNumberParamValue);
      } catch (NumberFormatException e) {
        this.highlightFragmentNumber = HIGHLIGHT_FRAGMENT_NUMBER_DEFAULT_VALUE;
        LOG.warn("Value of param highlightFragmentNumber of search connector " + this.getClass().getName()
                + " is not a valid number (" + highlightFragmentNumberParamValue + "), default value will be used ("
                + HIGHLIGHT_FRAGMENT_NUMBER_DEFAULT_VALUE + ")");
      }
    } else {
      this.highlightFragmentNumber = HIGHLIGHT_FRAGMENT_NUMBER_DEFAULT_VALUE;
    }

    //Indicate in which order element will be displayed
    sortMapping.put("relevancy", "_score");
    sortMapping.put("date", "lastUpdatedDate");
  }

  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites,
                                         int offset, int limit, String sort, String order) {
    String esQuery = buildQuery(query, sites, offset, limit, sort, order);
    String jsonResponse = this.client.sendRequest(esQuery, this.index);
    return buildResult(jsonResponse, context);
  }
  
  @Override
  public boolean isIndexed(SearchContext context, String id) {
    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");
    esQuery.append("  \"query\": {\n");
    esQuery.append("    \"term\" : { \"_id\" : \""+id+"\" }\n");
    esQuery.append("  }\n");
    esQuery.append("}");
    String jsonResponse = this.client.sendRequest(esQuery.toString(), this.index);
    Collection<SearchResult> results = buildResult(jsonResponse, context);
    results.stream().forEach(LOG::info);
    return !results.isEmpty();
  }
  
  /**
   *
   * Search on ES with additional filter on the search query
   * Different Filter are:
   * - Term Filter (Check if a specific term of a field exist)
   * - Not exist Filter (Check if a term not exist)
   * - Exist Filter (check if a term exist)
   *
   * @param context
   * @param query
   * @param filters
   * @param sites
   * @param offset
   * @param limit
   * @param sort
   * @param order
   * @return a collection of SearchResult
   */
  public Collection<SearchResult> filteredSearch(SearchContext context, String query, List<ElasticSearchFilter> filters, Collection<String> sites,
                                         int offset, int limit, String sort, String order) {
    String esQuery = buildFilteredQuery(query, sites, filters, offset, limit, sort, order);
    String jsonResponse = this.client.sendRequest(esQuery, this.index);
    return buildResult(jsonResponse, context);
  }

  /**
   *
   * Search on ES with entity id
   * Search keyword in query with an OR
   * And search only for the entity in parameter
   *
   * @param query
   * @param entityId
   * @return a collection of SearchResult
   */
  public Collection<SearchResult> searchByEntityId(SearchContext context, String query, String entityId) {
    String esQuery = builQueryWithEntityId(query, entityId);
    String jsonResponse = this.client.sendRequest(esQuery, this.index);
    return buildResult(jsonResponse, context);
  }
  
  
  
  protected String buildQuery(String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    return buildFilteredQuery(query, sites, null, offset, limit, sort, order);
  }

  protected String builQueryWithEntityId(String query, String id) {
    List<String> composedKeywords = new ArrayList<>();
    List<String> keywords = Arrays.asList(query.split(","));
    keywords = keywords.stream().filter(key -> {
      if (key.contains(" ")) {
        composedKeywords.add(key);
        return false;
      } else {
        return true;
      }
    }).collect(Collectors.toList());
    query = String.join(" ", keywords);
    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");
    esQuery.append("     \"track_scores\": true,\n");
    esQuery.append("     \"_source\": [" + getSourceFields() + "],");
    esQuery.append("     \"query\": {\n");
    esQuery.append("        \"bool\" : {\n");
    esQuery.append("            \"should\" : [\n");
    if (StringUtils.isNotBlank(query)) {
      List<String> queryParts = Arrays.asList(query.split("[\\+\\-=\\&\\|><\\!\\(\\)\\{\\}\\[\\]\\^\"\\*\\?:\\/ @$]+"));
      queryParts = queryParts.stream().map(queryPart -> {
        queryPart = this.escapeReservedCharacters(queryPart);
        if (queryPart.length() > 5) {
          queryPart = queryPart + "~1"; // fuzzy search on big words
        }
        return queryPart;
      }).collect(Collectors.toList());
      String escapedQueryWithOROperator = StringUtils.join(queryParts, " OR ");
      esQuery.append("            {\n");
      esQuery.append("                \"query_string\" : {\n");
      esQuery.append("                    \"fields\" : [" + getFields() + "],\n");
      esQuery.append("                    \"query\" : \"" + escapedQueryWithOROperator + "\"\n");
      esQuery.append("                }\n");
      if (composedKeywords.isEmpty()) {
        esQuery.append("            }\n");
      } else {
        esQuery.append("            },\n");
      }
    }
    if (!composedKeywords.isEmpty()) {
      esQuery.append("            {\n");
      esQuery.append("            \"dis_max\" : {\n");
      esQuery.append("              \"queries\": [\n");
      for (String composedKeyword : composedKeywords) {
        esQuery.append("                {\n");
        esQuery.append("                \"multi_match\" : {\n");
        esQuery.append("                    \"type\" : \"phrase\",\n");
        esQuery.append("                    \"fields\" : [" + getFields() + "],\n");
        esQuery.append("                    \"query\" : \"" + composedKeyword + "\"\n");
        esQuery.append("                }\n");
        esQuery.append("                },");
      }
      esQuery.deleteCharAt(esQuery.length() - 1);
      esQuery.append("                  ]\n");
      esQuery.append("               }\n");
      esQuery.append("            }\n");
    }
    esQuery.append("            ],\n");
    esQuery.append("            \"filter\" : {\n");
    esQuery.append("              \"bool\" : {\n");
    esQuery.append("                \"must\" : [\n");
    esQuery.append("                  {\n");
    esQuery.append("                   \"bool\" : {\n");
    esQuery.append("                     \"should\" : [\n");
    esQuery.append("                       { \"term\" : { \"_id\" : \""+id+"\" } }\n");
    esQuery.append("                     ]\n");
    esQuery.append("                   }\n");
    esQuery.append("                  }\n");
    esQuery.append("                ]\n");
    esQuery.append("              }\n");
    esQuery.append("            }");
    esQuery.append("        }\n");
    esQuery.append("     },\n");
    esQuery.append("     \"highlight\" : {\n");
    esQuery.append("       \"fields\" : {\n");
    for (int i = 0; i < searchFields.size(); i++) {
      esQuery.append("         \"" + searchFields.get(i) + "\" : {}");
      if (i < searchFields.size() - 1) {
        esQuery.append(",");
      }
      esQuery.append("\n");
    }
    esQuery.append("       }\n");
    esQuery.append("     }\n");
    esQuery.append("}");

    LOG.debug("Search Query request to ES : {} ", esQuery);

    return esQuery.toString();
  }
  
  protected String buildFilteredQuery(String query, Collection<String> sites, List<ElasticSearchFilter> filters, int offset, int limit, String sort, String order) {
    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");
    esQuery.append("     \"from\" : " + offset + ",\n");
    if(limit >= 0 && limit < Integer.MAX_VALUE) {
      esQuery.append("     \"size\" : " + limit + ",\n");
    }
    //Score are always tracked, even with sort
    //https://www.impl.co/guide/en/elasticsearch/reference/current/search-request-sort.html#_track_scores
    esQuery.append("     \"track_scores\": true,\n");
    esQuery.append("     \"sort\" : [\n");
    esQuery.append("       { \"" + (StringUtils.isNotBlank(sortMapping.get(sort)) ? sortMapping.get(sort) : "_score") + "\" : ");
    esQuery.append(             "{\"order\" : \"" + (StringUtils.isNotBlank(order)?order:"desc") + "\"}}\n");
    esQuery.append("     ],\n");
    esQuery.append("     \"_source\": [" + getSourceFields() + "],");
    esQuery.append("     \"query\": {\n");
    esQuery.append("        \"bool\" : {\n");
    if (StringUtils.isNotBlank(query)) {
      List<String> queryParts = Arrays.asList(query.split("[\\+\\-=\\&\\|><\\!\\(\\)\\{\\}\\[\\]\\^\"\\*\\?:\\/ @]+"));
      queryParts = queryParts.stream().map(queryPart -> {
        queryPart = this.escapeReservedCharacters(queryPart);
        if (queryPart.length() > 5) {
          queryPart = queryPart + "~1"; // fuzzy search on big words
        }
        return queryPart;
      }).collect(Collectors.toList());
      String escapedQueryWithAndOperator = StringUtils.join(queryParts, " AND ");
      String escapedQueryWithoutOperator = StringUtils.join(queryParts, " ");
      esQuery.append("            \"must\" : {\n");
      esQuery.append("                \"query_string\" : {\n");
      esQuery.append("                    \"fields\" : [" + getFields() + "],\n");
      esQuery.append("                    \"query\" : \"" + escapedQueryWithAndOperator + "\"\n");
      esQuery.append("                }\n");
      esQuery.append("            },\n");
      esQuery.append("            \"should\" : {\n");
      esQuery.append("                \"multi_match\" : {\n");
      esQuery.append("                    \"type\" : \"phrase\",\n");
      esQuery.append("                    \"fields\" : [" + getFields() + "],\n");
      esQuery.append("                    \"boost\" : 5,\n");
      esQuery.append("                    \"query\" : \"" + escapedQueryWithoutOperator + "\"\n");
      esQuery.append("                }\n");
      esQuery.append("            },\n");
    }
    esQuery.append("            \"filter\" : [\n");
    String metadataQuery = getMetadataQuery(filters);
    if(StringUtils.isNotBlank(metadataQuery)) {
      esQuery.append(metadataQuery);
    }
    esQuery.append("            {\n");
    esQuery.append("              \"bool\" : {\n");
    esQuery.append("                \"must\" : [\n");
    esQuery.append("                  {\n");
    esQuery.append("                   \"bool\" : {\n");
    esQuery.append("                     \"should\" : [\n");
    if (filters != null && filters.size() > 1 && WIKI_TYPE.equals(filters.get(0).getField()) && GROUP.equals(filters.get(0).getValue())
        && StringUtils.isNotBlank(filters.get(1).getValue())) {
      esQuery.append("                      " + getPermissionFilterWiki(filters.get(1).getValue()) + "\n");
    } else {
      esQuery.append("                      " + getPermissionFilter() + "\n");
    }
    esQuery.append("                      ]\n");
    esQuery.append("                    }\n");
    esQuery.append("                  }\n");
    String sitesFilter = getSitesFilter(sites);
    if(StringUtils.isNotBlank(sitesFilter)) {
      esQuery.append("                  ,{\n");
      esQuery.append("                   \"bool\" : {\n");
      esQuery.append("                     \"should\" : \n");
      esQuery.append("                      " + sitesFilter + "\n");
      esQuery.append("                    }\n");
      esQuery.append("                  }");
    }
    String additionalFilters = getAdditionalFilters(filters);
    if(StringUtils.isNotBlank(additionalFilters)) {
      esQuery.append(additionalFilters);
    }
    esQuery.append("                  \n");
    esQuery.append("                ]\n");
    esQuery.append("              }\n");
    esQuery.append("             }\n");
    esQuery.append("            ]");
    esQuery.append("        }\n");
    esQuery.append("     },\n");
    esQuery.append("     \"highlight\" : {\n");
    esQuery.append("       \"pre_tags\" : [\"<span class='searchMatchExcerpt'>\"],\n");
    esQuery.append("       \"post_tags\" : [\"</span>\"],\n");
    esQuery.append("       \"fields\" : {\n");
    for (int i=0; i<this.searchFields.size(); i++) {
      esQuery.append("         \""+searchFields.get(i)+"\" : {\n")
             .append("          \"type\" : \"unified\",\n")
             .append("          \"fragment_size\" : " + this.highlightFragmentSize + ",\n")
             .append("          \"no_match_size\" : 0,\n")
             .append("          \"number_of_fragments\" : " + this.highlightFragmentNumber + "}");
      if (i<this.searchFields.size()-1) {
        esQuery.append(",");
      }
      esQuery.append("\n");
    }
    esQuery.append("       }\n");
    esQuery.append("     }\n");
    esQuery.append("}");
    
    LOG.debug("Search Query request to ES : {} ", esQuery);
    
    return esQuery.toString();
  }
  
  /**
   * Escaped reserved characters by ES when using query_string.
   * Only ~ is not escaped since it is used for fuzzy search parameter.
   * The list of reserved characters is documented at
   * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
   * @param query The unescaped query string
   * @return The escaped query string
   */
  protected String escapeReservedCharacters(String query) {
    if(StringUtils.isNotEmpty(query)) {
      return query.replaceAll("[" + Pattern.quote("+-=&|><!(){}\\[\\]^\"*?:\\/") + "]",
              Matcher.quoteReplacement("\\\\") + "$0");
    } else {
      return query;
    }
  }

  protected Collection<SearchResult> buildResult(String jsonResponse, SearchContext context) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    Collection<SearchResult> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map)parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if(jsonResult != null) {
      JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

      if(jsonHits != null) {
        for (Object jsonHit : jsonHits) {
          results.add(buildHit((JSONObject) jsonHit, context));
        }
      }
    }

    return results;

  }

  protected SearchResult buildHit(JSONObject jsonHit, SearchContext searchContext) {
    JSONObject hitSource = (JSONObject) jsonHit.get("_source");
    String title = getTitleFromJsonResult(hitSource);
    String url = getUrlFromJsonResult(hitSource, searchContext);
    Long lastUpdatedDate = getUpdatedDateFromResult(hitSource);
    if (lastUpdatedDate == null) lastUpdatedDate = new Date().getTime();
    Double score = (Double) jsonHit.get("_score");
    String detail = buildDetail(jsonHit, searchContext);
    //Get the excerpt
    JSONObject hitHighlight = (JSONObject) jsonHit.get("highlight");
    Map<String, List<String>> excerpts = new HashMap<>();
    StringBuilder excerpt = new StringBuilder();
    if(hitHighlight != null) {
      Iterator<?> keys = hitHighlight.keySet().iterator();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        JSONArray highlights = (JSONArray) hitHighlight.get(key);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<String> excerptsList = (List<String>) ((ArrayList) highlights).stream() // NOSONAR
                                                                           .map(Object::toString)
                                                                           .collect(Collectors.toList());
        excerpts.put(key, excerptsList);
        excerpt.append("... ").append(StringUtils.join(excerptsList, "..."));
      }
    }

    LOG.debug("Excerpt extract from ES response : {}", excerpt.toString());

    return new SearchResult(
            url,
            title,
            excerpts,
            excerpt.toString(),
            detail,
            img,
            lastUpdatedDate,
            //score must not be null as "track_scores" is part of the query
            score.longValue());
  }

  protected String buildDetail(JSONObject jsonHit, SearchContext searchContext) {
    return null;
  }

  protected Long getUpdatedDateFromResult(JSONObject hitSource) {
    Object date = hitSource.get(updatedDateElasticFieldName);
    if (date instanceof  Long) {
      return (Long)date;
    } else if (date != null) {
      try {
        return Long.parseLong(date.toString());
      } catch (Exception ex) {
        LOG.error("Can not parse updatedDate field as Long {}", date);
      }
    }
    return null;
  }

  protected String getUrlFromJsonResult(JSONObject hitSource, SearchContext context) {
    return (String) hitSource.get("url");
  }

  protected String getTitleFromJsonResult(JSONObject hitSource) {
    return (String) hitSource.get(titleElasticFieldName);
  }

  protected String getAdditionalFilters(List<ElasticSearchFilter> filters) {

    if (filters == null) return "";

    StringBuilder filterJSON = new StringBuilder();

    for (ElasticSearchFilter filter: filters) {
      if (filter.getType().equals(ElasticSearchFilterType.FILTER_MY_WORK_DOCS)) {
        filterJSON.append(getFilter(filter));
      } else {
        filterJSON.append("                  ,\n");
        filterJSON.append("                  {\n");
        filterJSON.append("                   \"bool\" : {\n");
        filterJSON.append("                     \"should\" : [\n");
        filterJSON.append("                      " + getFilter(filter) + "\n");
        filterJSON.append("                       ]\n");
        filterJSON.append("                    }\n");
        filterJSON.append("                  }");
      }

    }

    return filterJSON.toString();

  }
  protected String getMetadataQuery(List<ElasticSearchFilter> filters) {

    if (filters == null) return "";
    StringBuilder metaQuery= new StringBuilder();
    for (ElasticSearchFilter filter: filters) {
      if (filter.getType().equals(ElasticSearchFilterType.FILTER_MATADATAS)) {
        metaQuery.append(filter.getValue());
      }
    }
    return metaQuery.toString();
  }

  private String getFilter(ElasticSearchFilter filter) {
    switch (filter.getType()) {
      case FILTER_BY_TERM:
        return getTermFilter(filter.getField(), filter.getValue());
      case FILTER_EXIST:
        return getExistFilter(filter.getField());
      case FILTER_NOT_EXIST:
        return getNotExistFilter(filter.getField());
      case FILTER_CUSTOM:
        return getCustomFilter(filter.getValue());
      case FILTER_MY_WORK_DOCS:
        return myWorkDocumentsFilter(filter.getValue());
    }
    return "";
  }

  /**
   * Check if a specific term of a field exist
   * Note that this field should be set as not analyzed (index = false)
   *
   * @param field
   * @param value
   * @return a Term Filter
   */
  private String getTermFilter(String field, String value) {
    return "{\n \"term\" : { \"" + field + "\" : \"" + value + "\" }\n }";
  }

  /**
   * Check if a specific field not exist
   *
   * @param field
   * @return a not Exist Term Filter
   */
  private String getNotExistFilter(String field) {
    return "{\n" +
        "  \"not\": {\n" +
        "    \"exists\" : { \"field\" : \"" + field + "\" }\n" +
        "  }\n" +
        "}";
  }

  /**
   * Check if a specific field exist
   *
   * @param field
   * @return an Exist Filter
   */
  private String getExistFilter(String field) {
    return "{\n \"exists\" : { \"field\" : \"" + field + "\" }\n }";
  }

  protected String getFields() {
    List<String> fields = new ArrayList<>();
    List<String> fieldsToUse = this.searchFieldsWithBoost != null
        && !this.searchFieldsWithBoost.isEmpty() ? this.searchFieldsWithBoost : this.searchFields;
    for (String searchField: fieldsToUse) {
      fields.add("\"" + searchField + "\"");
    }
    return StringUtils.join(fields, ",");
  }

  /**
   * Apply the given value directly as the filter
   *
   * @param value
   * @return a Custom Filter
   */
  private String getCustomFilter(String value) {
    return value;
  }
  private String myWorkDocumentsFilter(String value) {
    StringBuilder filterJSON = new StringBuilder();
    filterJSON.append("                  ,\n");
    filterJSON.append("                  {\n");
    filterJSON.append("                   \"bool\" : {\n");
    filterJSON.append("                       " + value + "\n");
    filterJSON.append("                    }\n");
    filterJSON.append("                  }");

    return filterJSON.toString();
  }
  protected String getPermissionFilter() {
    StringBuilder permissionSB = new StringBuilder();
    Set<String> membershipSet = getUserMemberships();
    if (!membershipSet.isEmpty()) {
      String memberships = StringUtils.join(membershipSet.toArray(new String[membershipSet.size()]), "|");
      permissionSB.append("{\n")
      .append("  \"term\" : { \"permissions\" : \"")
      .append(getCurrentUser())
      .append("\" }\n")
      .append("},\n")
      .append("{\n")
      .append("  \"term\" : { \"permissions\" : \"")
      .append(IdentityConstants.ANY)
      .append("\" }\n")
      .append("},\n")
      .append("{\n")
      .append("  \"regexp\" : { \"permissions\" : \"")
      .append(memberships)
      .append("\" }\n")
      .append("}");
    }
    else {
      permissionSB.append("{\n")
      .append("  \"term\" : { \"permissions\" : \"")
      .append(getCurrentUser())
      .append("\" }\n")
      .append("},\n")
      .append("{\n")
      .append("  \"term\" : { \"permissions\" : \"")
      .append(IdentityConstants.ANY)
      .append("\" }\n")
      .append("}");
    }
    return permissionSB.toString();
  }

  protected String getSitesFilter(Collection<String> sitesCollection) {
    if (sitesCollection != null && !sitesCollection.isEmpty()) {
      List<String> sites = new ArrayList<>();
      for (String site : sitesCollection) {
        sites.add("\"" + site + "\"");
      }
      String sitesList = "["+StringUtils.join(sites,",")+"]";
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
    }
    else {
      return " { \"bool\" : " +
          "{\n" +
          "  \"must_not\": {\n" +
          "      \"exists\" : { \"field\" : \"sites\" }\n" +
          "   }\n" +
          "  }\n" +
          "}\n";
    }
  }

  protected String getPermissionFilterWiki(String permission) {
    StringBuilder permissionSB = new StringBuilder();
    Set<String> membershipSet = getUserMemberships();
    if (!membershipSet.isEmpty()) {
      String memberships = StringUtils.join(membershipSet.toArray(new String[membershipSet.size()]), "|");
      permissionSB.append("{\n")
              .append("  \"term\" : { \"permissions\" : \"")
              .append(permission)
              .append("\" }\n")
              .append("},\n")
              .append("{\n")
              .append("  \"term\" : { \"permissions\" : \"")
              .append(IdentityConstants.ANY)
              .append("\" }\n")
              .append("},\n")
              .append("{\n")
              .append("  \"regexp\" : { \"permissions\" : \"")
              .append(memberships)
              .append("\" }\n")
              .append("}");
    }
    else {
      permissionSB.append("{\n")
              .append("  \"term\" : { \"permissions\" : \"")
              .append(getCurrentUser())
              .append("\" }\n")
              .append("},\n")
              .append("{\n")
              .append("  \"term\" : { \"permissions\" : \"")
              .append(IdentityConstants.ANY)
              .append("\" }\n")
              .append("}");
    }
    return permissionSB.toString();
  }

  private String getCurrentUser() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState == null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent() is null");
    }
    if (ConversationState.getCurrent().getIdentity()==null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent().getIdentity() is null");
    }
    return ConversationState.getCurrent().getIdentity().getUserId();
  }

  protected Set<String> getUserMemberships() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState == null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent() is null");
    }
    if (ConversationState.getCurrent().getIdentity()==null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent().getIdentity() is null");
    }
    if (ConversationState.getCurrent().getIdentity().getMemberships()==null) {
      //This case is not supported
      //The doc says "Any anonymous user automatically becomes a member of the group guests.group when they enter the public pages."
      //http://docs.exoplatform.com/PLF42/sect-Reference_Guide-Portal_Default_Permission_Configuration.html
      throw new IllegalStateException("No Membership found: ConversationState.getCurrent().getIdentity().getMemberships() is null");
    }

    Set<String> entries = new HashSet<>();
    for (MembershipEntry entry : ConversationState.getCurrent().getIdentity().getMemberships()) {
      //If it's a wildcard membership, add a point to transform it to regexp
      if (entry.getMembershipType().equals(MembershipEntry.ANY_TYPE)) {
        entries.add(entry.toString().replace("*", ".*"));
      }
      //If it's not a wildcard membership
      else {
        //Add the membership
        entries.add(entry.toString());
        //Also add a wildcard membership (not as a regexp) in order to match to wildcard permission
        //Ex: membership dev:/pub must match permission dev:/pub and permission *:/pub
        entries.add("*:"+entry.getGroup());
      }
    }
    return entries;
  }

  protected String getSourceFields() {

    List<String> fields = new ArrayList<>();
    fields.add("url");
    fields.add(getTitleElasticFieldName());

    List<String> sourceFields = new ArrayList<>();
    for (String sourceField: fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public String getImg() {
    return img;
  }

  public void setImg(String img) {
    this.img = img;
  }

  public String getTitleElasticFieldName() {
    return titleElasticFieldName;
  }

  public void setTitleElasticFieldName(String titleElasticFieldName) {
    this.titleElasticFieldName = titleElasticFieldName;
  }

  public List<String> getSearchFields() {
    return searchFields;
  }

  public void setSearchFields(List<String> searchFields) {
    this.searchFields = searchFields;
  }

  public ElasticSearchingClient getClient() {
    return client;
  }
}

