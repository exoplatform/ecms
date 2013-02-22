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

import javax.jcr.RepositoryException;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 5, 2013  
 */
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
    criteria.setKeyword(query.toLowerCase());
    criteria.setSearchWebpage(true);
    criteria.setSearchDocument(false);
    criteria.setSearchWebContent(false);
    criteria.setMultiplePhaseSearch(true);
    criteria.setLiveMode(true);
    criteria.setOffset(offset);
    criteria.setLimit(limit);
    criteria.setSortBy(sort);
    criteria.setOrderBy(order);
    return criteria;
  }

  @Override
  protected AbstractPageList<ResultNode> searchNodes(QueryCriteria criteria) throws Exception {
    return siteSearch_.searchPageContents(WCMCoreUtils.getUserSessionProvider(),
                                           criteria, (int)criteria.getLimit(), false);
  }

  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    return node;
  }

}
