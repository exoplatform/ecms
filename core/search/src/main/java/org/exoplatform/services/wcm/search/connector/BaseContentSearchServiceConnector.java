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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang.LocaleUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDefinitionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public abstract class BaseContentSearchServiceConnector extends BaseSearchServiceConnector {
  
  public static List<String> excluded_nodetypes = new ArrayList<String>();

  public BaseContentSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected QueryCriteria createQueryCriteria(String query, long offset, long limit, String sort, String order) {
    QueryCriteria criteria = new QueryCriteria();
    //set content types
    criteria.setContentTypes(getSearchedDocTypes());
    criteria.setNodeTypes(getNodeTypes());
    criteria.setKeyword(removeAccents(query.toLowerCase()));
    criteria.setSearchWebpage(false);
    criteria.setSearchDocument(true);
    criteria.setSearchWebContent(true);
    if(query.contains("~")) {
      criteria.setFuzzySearch(true);
    }
    criteria.setLiveMode(true);
    criteria.setOffset(offset);
    criteria.setLimit(limit);
    criteria.setSortBy(sort);
    criteria.setOrderBy(order);
    
    if (ConversationState.getCurrent().getIdentity().getUserId() != null) {
      criteria.setSearchPath("");
    }
    return criteria;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected AbstractPageList<ResultNode> searchNodes(QueryCriteria criteria, SearchContext context) throws Exception {
    String localeParam = context.getParamValue(SearchContext.RouterParams.LANG.create());
    Locale locale = localeParam != null ? LocaleUtils.toLocale(localeParam) : null;
    return siteSearch_.searchSiteContents(WCMCoreUtils.getUserSessionProvider(),
                                           criteria, locale, (int)criteria.getLimit(), true);
  }

  @Override
  protected String getFileType(ResultNode node) throws Exception {
    return org.exoplatform.services.cms.impl.Utils.getFileType(node);
  }

  /**
   * returns the primary types of the specific search service:
   * nt:file for file search, all other document types for document search
   * @return searched doc types
   */
  protected abstract String[] getSearchedDocTypes();
  
  /**
   * returns the primary types of the specific search service:
   * nt:file for file search, null for document search
   * @return searched doc types
   */
  protected abstract String[] getNodeTypes();
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected String getTitleResult(ResultNode node) throws Exception {
    return node.getTitle();
  }
  
  /**
   * {@inheritDoc}
   */
  protected String getImageUrl(Node node) {
    return "/eXoSkin/skin/images/themes/default/Icons/TypeIcons/uiIconsType64x64.png";
  }
  
  /**
   * {@inheritDoc}
   */
  protected String getDetails(ResultNode retNode, SearchContext context) throws Exception {
    DriveData driveData = documentService.getDriveOfNode(retNode.getPath(), ConversationState.getCurrent().getIdentity().getUserId(), Utils.getMemberships());
    Calendar date = getDate(retNode);
    return getDriveTitle(driveData) + fileSize(retNode) + formatDate(date);
  } 

}

