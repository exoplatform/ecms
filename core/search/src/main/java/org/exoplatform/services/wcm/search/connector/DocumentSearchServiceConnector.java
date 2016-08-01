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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

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
  protected String getPath(ResultNode node, SearchContext context) throws Exception {
    String url = BaseSearchServiceConnector.NONE_NAGVIGATION;
    String handler = WCMCoreUtils.getPortalName();
    if(StringUtils.isNotEmpty(context.getSiteName())) {
      String siteType = StringUtils.isEmpty(context.getSiteType()) ? SiteType.PORTAL.toString() : context.getSiteType().toUpperCase();
      SiteKey siteKey = new SiteKey(SiteType.valueOf(siteType), context.getSiteName());

      DriveData driveData = documentService.getDriveOfNode(node.getPath(), ConversationState.getCurrent().getIdentity().getUserId(), Utils.getMemberships());

      if (StringUtils.isNotBlank(siteKey.getName())) {
        String pageName = getPageName(siteKey);
        if (StringUtils.isNotBlank(pageName)) {
          siteKey = SiteKey.portal(context.getSiteName() != null ? context.getSiteName() :
                  BaseSearchServiceConnector.DEFAULT_SITENAME);
          pageName = getPageName(siteKey);
        }
        try {
          url = "/" + handler + context.handler(handler).
                  lang("").
                  siteName(siteKey.getName()).
                  siteType(SiteType.PORTAL.getName()).
                  path(pageName + "?path=" + driveData.getName() + "/" + node.getPath()).renderLink();
        } catch (Exception e) {
          LOG.debug("The current user does not have the needed permission to get the requested document");
          return null;
        }
      }
    }

    return URLDecoder.decode(url, "UTF-8");
  }

  @Override
  protected String getPreviewUrl(ResultNode node, SearchContext context) throws Exception {
    String restContextName =  WCMCoreUtils.getRestContextName();

    Session session = node.getSession();
    String repositoryName = ((ManageableRepository) session.getRepository()).getConfiguration().getName();
    String workspaceName = node.getSession().getWorkspace().getName();

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/').append(restContextName).append("/jcr/").
            append(WCMCoreUtils.getRepository().getConfiguration().getName()).append('/').
            append(workspaceName).append(node.getPath());


    StringBuilder url = new StringBuilder("javascript:require(['SHARED/social-ui-activity'], function(activity) {activity.previewDoc({doc:{");
    if(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
      url.append("id:'").append(node.getUUID()).append("',");
    }
    return url.append("path:'").append(node.getPath())
            .append("', repository:'").append(repositoryName)
            .append("', workspace:'").append(workspaceName)
            .append("', downloadUrl:'").append(downloadUrl.toString())
            .append("', openUrl:'").append(documentService.getLinkInDocumentsApp(node.getPath()))
            .append("', isWebContent:true")
            .append("}})})").toString();
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
