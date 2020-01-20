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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.ArrayNodePageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Replaced by Elasticsearch implementation.
 * The search should be capable to search for site pages.
 */
@Deprecated
public class PageSearchServiceConnector extends BaseSearchServiceConnector {

  public PageSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected QueryCriteria createQueryCriteria(String query,
                                              long offset,
                                              long limit,
                                              String sort,
                                              String order) {
    QueryCriteria criteria = new QueryCriteria();
    //set content types
    criteria.setContentTypes(new String[] {"gtn:language", "exo:pageMetadata"});
    criteria.setFulltextSearchProperty(new String[] {"exo:metaKeywords", "exo:metaDescription", "gtn:name"});
    criteria.setKeyword(removeAccents(Utils.escapeIllegalCharacterInQuery(query).toLowerCase()));
    criteria.setSearchWebpage(true);
    criteria.setSearchDocument(false);
    criteria.setSearchWebContent(false);
    criteria.setLiveMode(true);
    criteria.setOffset(offset);
    criteria.setLimit(limit);
    criteria.setSortBy(sort);
    criteria.setOrderBy(order);
    if(query.contains("~")) {
      criteria.setFuzzySearch(true);
    }
    return criteria;
  }

  @Override
  protected AbstractPageList<ResultNode> searchNodes(QueryCriteria criteria, SearchContext context) throws Exception {
    if (StringUtils.isBlank(criteria.getSiteName())) {//return empty list of result
      return new ArrayNodePageList<ResultNode>(
          new ArrayList<ResultNode>(), (int)criteria.getLimit());
    } else {
      String[] siteNames = criteria.getSiteName().split(",");
      String localeParam = context.getParamValue(SearchContext.RouterParams.LANG.create());
      Locale locale = StringUtils.isNotBlank(localeParam) ? LocaleUtils.toLocale(localeParam) : Locale.ENGLISH;
      if (siteNames.length == 1) {//just search for 1 site
        return siteSearch_.searchPageContents(WCMCoreUtils.getUserSessionProvider(),
                                              criteria, locale,(int)criteria.getLimit(), false);
      } else {//search for many sites
        int limit = (int)criteria.getLimit();
        int offset = (int)criteria.getOffset();
        criteria.setOffset(0);
        List<ResultNode> ret = new ArrayList<ResultNode>();

        for (String site : siteNames) {
          criteria.setSiteName(site);
          AbstractPageList<ResultNode> resultList = 
              siteSearch_.searchPageContents(WCMCoreUtils.getUserSessionProvider(),
                                                criteria, locale, (int)criteria.getLimit(), false);
          if (resultList.getAvailable() <= offset) {
            offset -= resultList.getAvailable();
          } else if (resultList.getAvailable() > offset) {
            for (int i = 0; i < limit; i++)
              if (offset + i < resultList.getAvailable()) {
                ResultNode resNode = resultList.getPage( ( offset + i) / resultList.getPageSize() + 1).
                                                get((offset + i) % resultList.getPageSize());
                ret.add(resNode);
                if (limit == 0) break;
              }
          }
          if (limit == 0) break;
        }
        return new ArrayNodePageList<ResultNode>(ret, (int)criteria.getLimit());
      }
    }
  }

  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    return node;
  }
  
  @Override
  protected String getPath(ResultNode node, SearchContext context) throws Exception {
    return node.getUserNavigationURI();
  }

  @Override
  protected String getFileType(ResultNode node) throws Exception {
    return "FileDefault";
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected String getTitleResult(ResultNode node) throws Exception {
    try {
      return ((Node)node.getSession().getItem(node.getProperty("mop:link/mop:page")
                                     .getString())).getProperty("gtn:name").getString();
    } catch (Exception e) {
      return node.getTitle();
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected String getImageUrl(Node node) {
    return "/eXoSkin/skin/images/system/unified-search/page.png";
  }
  
  /**
   * {@inheritDoc}
   */
  protected String getDetails(ResultNode retNode, SearchContext context) throws Exception {
    DriveData driveData = documentService.getDriveOfNode(retNode.getPath(), ConversationState.getCurrent().getIdentity().getUserId(), Utils.getMemberships());
    return getDriveTitle(driveData);
  }
  
}
