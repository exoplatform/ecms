/*
 * Copyright (C) 2019 eXo Platform SAS.
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

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;

public class NavigationIndexingServiceConnector extends ElasticIndexingServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(NavigationIndexingServiceConnector.class);

  public final static String TYPE = "navigation";

  private NavigationStore navigationStore;

  private SEOService seoService;

  private PageService pageService;

  private DescriptionService descriptionService;

  public NavigationIndexingServiceConnector(InitParams initParams, NavigationStore navigationStore, SEOService seoService, PageService pageService, DescriptionService descriptionService) {
    super(initParams);
    this.navigationStore = navigationStore;
    this.seoService = seoService;
    this.pageService = pageService;
    this.descriptionService = descriptionService;
  }

  @Override
  public Document create(String nodeId) {
    if (StringUtils.isBlank(nodeId)) {
      throw new IllegalArgumentException("nodeId is mandatory");
    }

    long ts = System.currentTimeMillis();

    NodeData node = navigationStore.loadNode(Safe.parseLong(nodeId));
    if (node == null) {
      LOG.debug("Node with id {} does not exist or has been removed", nodeId);
      return null;
    }
    NavigationData nav = this.navigationStore.loadNavigationData(Safe.parseLong(node.getId()));
    String uri = getUri(node);

    Map<String, String> fields = new HashMap<>();
    fields.put("name", node.getName());
    fields.put("nodeId", node.getId());
    fields.put("siteName", nav.getSiteKey().getName());
    fields.put("siteType", nav.getSiteKey().getTypeName());

    //seo
    String seoMetadata = getSEO(node);
    if (seoMetadata != null) {
      fields.put("seo", seoMetadata);
    }
    //page
    Set<String> permissions = new HashSet<>();
    PageKey pageKey = node.getState().getPageRef();
    if (pageKey != null) {
      fields.put("pageRef", pageKey.format());
      PageContext page = pageService.loadPage(pageKey);

      String pageTitle = page.getState().getDisplayName();
      fields.put("pageTitle", pageTitle);

      permissions.addAll(page.getState().getAccessPermissions());
    }
    //description
    Map<Locale, State> descriptions = descriptionService.getDescriptions(node.getId());
    if (descriptions != null && descriptions.size() > 0) {
      JSONObject json = new JSONObject();
      try {
        for (Locale locale : descriptions.keySet()) {
          State state = descriptions.get(locale);
          if (state != null && state.getName() != null && locale.toLanguageTag() != null) {
            json.put(locale.toLanguageTag(), state.getName());
          }
        }
        fields.put("descriptions", json.toString());
      } catch (JSONException ex) {
        LOG.warn("Error while parsing description field to JSON", ex);
      }
    }

    Date createdDate = new Date();
    Document document = new Document(TYPE, nodeId, uri, createdDate, permissions, fields);

    LOG.debug("page document generated for node={} name={} duration_ms={}",
              nodeId,
              node.getName(),
              System.currentTimeMillis() - ts);

    return document;
  }

  private String getUri(NodeData node) {
    List<NodeData> nodes = new ArrayList<>();
    nodes.add(node);
    while (node.getParentId() != null) {
      node = navigationStore.loadNode(Safe.parseLong(node.getParentId()));
      nodes.add(0, node);
    }
    // Remove the default node
    nodes.remove(0);

    // Build path
    List<String> paths = nodes.stream().map(n -> n.getName()).collect(Collectors.toList());
    return StringUtils.join(paths, "/");
  }

  private String getSEO(NodeData node) {
    try {
      NavigationData nav = this.navigationStore.loadNavigationData(Safe.parseLong(node.getId()));
      if (!SiteType.PORTAL.equals(nav.getSiteKey().getType())) {
        return null;
      }
      String siteName = Text.escapeIllegalJcrChars(nav.getSiteKey().getName());
      final Map<String, PageMetadataModel> metaModels = seoService.getPageMetadatas(node.getId(), siteName);
      if (metaModels != null && metaModels.size() > 0) {
        JSONObject seo = new JSONObject();
        for(String key : metaModels.keySet()) {
          PageMetadataModel meta = metaModels.get(key);
          JSONObject json = new JSONObject();
          json.put("description", meta.getDescription());
          json.put("keywords", meta.getKeywords());
          json.put("title", meta.getTitle());
          json.put("robotContent", meta.getRobotsContent());
          seo.put(key, json);
        }
        return seo.toString();
      }
    } catch (Exception e) {
      LOG.warn("Can not get SEO metadata of node {}, return null", node.getId(), e);
    }
    return null;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<String> ids = new LinkedList<>();
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.PORTAL)));
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.GROUP)));
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.USER)));
    return ids;
  }

  private Collection<? extends String> getNodes(List<NavigationData> navigations) {
    List<String> ids = new ArrayList<>();
    for (NavigationData nav : navigations) {
      ids.addAll(getNodes(nav.getRootId()));
    }
    return ids;
  }

  private Collection<? extends String> getNodes(String rootId) {
    List<String> ids = new ArrayList<>();
    ids.add(rootId);
    NodeData node = navigationStore.loadNode(Safe.parseLong(rootId));

    Iterator<String> nodes = node.iterator(false);
    while (nodes != null && nodes.hasNext()) {
      String childId = nodes.next();
      ids.addAll(getNodes(childId));
    }
    return ids;
  }

  @Override
  public String getMapping() {
    StringBuilder mapping = new StringBuilder()
            .append("{")
            .append("  \"properties\" : {\n")
            .append("    \"name\" : {")
            .append("      \"type\" : \"text\",")
            .append("      \"index_options\": \"offsets\",")
            .append("      \"fields\": {")
            .append("        \"raw\": {")
            .append("          \"type\": \"keyword\"")
            .append("        }")
            .append("      }")
            .append("    },\n")
            .append("    \"nodeId\" : {\"type\" : \"keyword\"},\n")
            .append("    \"siteName\": {\"type\" : \"keyword\"},\n")
            .append("    \"siteType\": {\"type\" : \"keyword\"},\n")
            .append("    \"pageRef\" : {\"type\" : \"keyword\"},\n")
            .append("    \"pageTitle\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"seo\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"descriptions\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
            .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"}\n")
            .append("  }\n")
            .append("}");

    return mapping.toString();
  }

}
