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

import java.net.URLDecoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortalContext;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.portal.config.model.Container;

/**
 * Documents are nodes whose nodetype was declared as a Document Type in ECM admin.
 */
public class DocumentSearchServiceConnector extends BaseContentSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(DocumentSearchServiceConnector.class.getName());
  
  public DocumentSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String[] getSearchedDocTypes() {
    List<String> docTypes = null;
    try {
       docTypes = WCMCoreUtils.getService(TemplateService.class).getDocumentTemplates();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    docTypes.remove(NodetypeConstant.NT_FILE);
    return docTypes.toArray(new String[]{});
  }
  
  protected String[] getNodeTypes() {
    return null;
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    //do not accept nt:file
    return node.isNodeType(NodetypeConstant.NT_FILE) ? null : node;
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected String getPath(DriveData driveData, ResultNode node, SearchContext context) throws Exception {
    String url = BaseSearchServiceConnector.NONE_NAGVIGATION;
    String handler = WCMCoreUtils.getPortalName();
    UserPortalConfig prc = getUserPortalConfig();
    if (prc == null) return null;
    SiteKey siteKey = SiteKey.portal(prc.getPortalConfig().getName());
    if(siteKey != null) {
      if(StringUtils.isNotBlank(siteKey.getName())) {
        String pageName = getPageName(siteKey);
        if(StringUtils.isNotBlank(pageName)) {
          siteKey = SiteKey.portal(context.getSiteName() != null ? context.getSiteName():
                                                                   BaseSearchServiceConnector.DEFAULT_SITENAME);
          pageName = getPageName(siteKey);
        }
        url = "/" + handler + context.handler(handler).
                      lang("").
                      siteName(siteKey.getName()).
                      siteType(SiteType.PORTAL.getName()).
                      path(pageName+"?path=" +driveData.getName() + "/" + node.getPath()).renderLink();
      }
    }
    return URLDecoder.decode(url, "UTF-8");
  }
  
  private String getPageName(SiteKey siteKey) throws Exception {
    NavigationService navService = WCMCoreUtils.getService(NavigationService.class);
    NavigationContext nav = navService.loadNavigation(siteKey);
    NodeContext<NodeContext<?>> parentNodeCtx = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    if (parentNodeCtx.getSize() >= 1) {
      Collection<NodeContext<?>> children = parentNodeCtx.getNodes();
      if (siteKey.getType() == SiteType.GROUP) {
        children = parentNodeCtx.get(0).getNodes();
      }
      Iterator<NodeContext<?>> it = children.iterator();
      NodeContext<?> child = null;
      while (it.hasNext()) {
        child = it.next();
        if (hasPortlet(child, BaseSearchServiceConnector.PORTLET_NAME)) {
          return child.getName();
        }
      }
    }
    return "";
  }
  
  private static UserPortalConfig getUserPortalConfig() throws Exception {
    UserPortalConfigService userPortalConfigSer = WCMCoreUtils.getService(UserPortalConfigService.class);
    UserPortalContext NULL_CONTEXT = new UserPortalContext() {
      public ResourceBundle getBundle(UserNavigation navigation) {
        return null;
      }
      public Locale getUserLocale() {
        return Locale.ENGLISH;
      }
    };
    String remoteId = ConversationState.getCurrent().getIdentity().getUserId() ;
    UserPortalConfig userPortalCfg = userPortalConfigSer.
        getUserPortalConfig(userPortalConfigSer.getDefaultPortal(), remoteId, NULL_CONTEXT);
    return userPortalCfg;
  }
  
  private boolean hasPortlet(NodeContext<?> pageCt, String plName) {
    if (plName == null) return false;
    DataStorage ds = WCMCoreUtils.getService(DataStorage.class);
    try {
      for (ModelObject mo : ds.getPage(pageCt.getState().getPageRef().format()).getChildren()) {
        if (containApp(mo, plName)) {
          return true;
        }
      } //of for
    } catch(Exception ex) {
      return false;
    }
    return false;
  }
  
  private boolean containApp(ModelObject mo, String plName) {
    DataStorage ds = WCMCoreUtils.getService(DataStorage.class);
    if (mo instanceof Application<?>) {
      try {
        if (ds.getId(((Application<?>)mo).getState()).contains(plName)) {
          return true;
        }
      } catch (Exception e) {
        return false;
      }
    } else if (mo instanceof Container) {
      for (ModelObject m : ((Container)mo).getChildren()) {
        if (containApp(m, plName)) {
          return true;
        }// of if
      }// of for
    } // of flse
    return false;
  }
  
}
