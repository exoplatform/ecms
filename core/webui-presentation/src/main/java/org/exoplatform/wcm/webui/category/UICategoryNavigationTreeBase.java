/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.category;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.UITree;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Comment: Change objId from node's path to category's path
 * Jun 30, 2009
 */
@ComponentConfig(
    events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)
)
public class UICategoryNavigationTreeBase extends UITree {

  private static final Log         LOG            = ExoLogger.getLogger(UICategoryNavigationTreeBase.class.getName());
  
  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#renderNode(java.lang.Object)
   */
  public String renderNode(Object obj) throws Exception {
    Node node = (Node) obj;
    String nodeTypeIcon = Utils.getNodeTypeIcon(node,"16x16Icon");
    String nodeIcon = this.getExpandIcon();
    String iconGroup = this.getIcon();
    String note = "" ;
    if(isSelected(obj)) {
      nodeIcon = getColapseIcon();
      iconGroup = getSelectedIcon();
      note = " NodeSelected" ;
    }
    String beanIconField = getBeanIconField();
    if(beanIconField != null && beanIconField.length() > 0) {
      if(getFieldValue(obj, beanIconField) != null)
        iconGroup = (String)getFieldValue(obj, beanIconField);
    }
    renderCategoryLink(node);
    String objId = String.valueOf(getId(obj));
    StringBuilder builder = new StringBuilder();
    if (nodeIcon.equals(getExpandIcon())) {
      builder.append(" <a class=\"")
             .append(nodeIcon)
             .append(" ")
             .append(nodeTypeIcon)
             .append("\" href=\"")
             .append(objId)
             .append("\">");
    } else {
      builder.append(" <a class=\"")
             .append(nodeIcon)
             .append(" ")
             .append(nodeTypeIcon)
             .append("\" onclick=\"eXo.portal.UIPortalControl.collapseTree(this)")
             .append("\">");
    }
    UIRightClickPopupMenu popupMenu = getUiPopupMenu();
    String beanLabelField = getBeanLabelField();
    String className="NodeIcon";
    boolean flgSymlink = false;
    if (Utils.isSymLink(node)) {
      flgSymlink = true;
      className = "NodeIconLink";
    }
    if (popupMenu == null) {
      builder.append(" <div class=\"")
             .append(className)
             .append(" ")
             .append(iconGroup)
             .append(" ")
             .append(nodeTypeIcon)
             .append(note)
             .append("\"")
             .append(" title=\"")
             .append(getFieldValue(obj, beanLabelField))
             .append("\"")
             .append(">");
      if (flgSymlink) {
        builder.append("  <div class=\"LinkSmall\">")
               .append(getFieldValue(obj, beanLabelField))
               .append("</div>");
      } else {
        builder.append(getFieldValue(obj, beanLabelField));
      }
      builder.append("</div>");
    } else {
      builder.append(" <div class=\"")
             .append(className)
             .append(" ")
             .append(iconGroup)
             .append(" ")
             .append(nodeTypeIcon)
             .append(note)
             .append("\" ")
             .append(popupMenu.getJSOnclickShowPopup(objId, null))
             .append(" title=\"")
             .append(getFieldValue(obj, beanLabelField))
             .append("\"")
             .append(">");
      if (flgSymlink) {
        builder.append("  <div class=\"LinkSmall\">")
               .append(getFieldValue(obj, beanLabelField))
               .append("</div>");
      } else {
        builder.append(getFieldValue(obj, beanLabelField));
      }
      builder.append("</div>");
    }
    builder.append(" </a>");
    return builder.toString();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#getTemplate()
   */
  public String getTemplate() {
    return UICategoryNavigationUtils.getPortletPreferences()
                                    .getValue(UICategoryNavigationConstant.PREFERENCE_TEMPLATE_PATH,
                                              null);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    return new JCRResourceResolver(workspace);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#getActionLink()
   */
  public String getActionLink() throws Exception {
    PortletRequestContext porletRequestContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    HttpServletRequestWrapper requestWrapper = (HttpServletRequestWrapper) porletRequestContext.getRequest();
    String requestURI = requestWrapper.getRequestURI();
    PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
    String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    String preferenceTargetPage = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TARGET_PAGE, "");
    String backPath = requestURI.substring(0, requestURI.lastIndexOf("/"));
    if (backPath.endsWith(preferenceTargetPage)
        || requestURI.endsWith(Util.getUIPortal().getSelectedUserNode().getURI()))
      backPath = "javascript:void(0)";
    else if (backPath.endsWith(preferenceTreeName))
      backPath = backPath.substring(0, backPath.lastIndexOf("/"));
    return backPath;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#isSelected(java.lang.Object)
   */
  public boolean isSelected(Object obj) throws Exception {
    Node selectedNode = this.getSelected();
    Node node = (Node) obj;
    if(selectedNode == null) return false;
    return selectedNode.getPath().equals(node.getPath());
  }

  public boolean isMovedTreeToTrash(String rootCategory) throws Exception {
    Node categoryNode = getCategoryNode(rootCategory);
    if (Utils.isInTrash(categoryNode))
      return true;
    return false;
  }

  /**
   * Gets the subcategories.
   *
   * @param categoryPath the category path
   *
   * @return the subcategories
   *
   * @throws Exception the exception
   */
  public List<Node> getSubcategories(String categoryPath) throws Exception {
    Node categoryNode = getCategoryNode(categoryPath);
    NodeIterator nodeIterator = categoryNode.getNodes();
    List<Node> subcategories = new ArrayList<Node>();
    while (nodeIterator.hasNext()) {
      Node subcategory = nodeIterator.nextNode();
      if (subcategory.isNodeType("exo:taxonomy"))
        subcategories.add(subcategory);
    }
    return subcategories;
  }

  /**
   * Resolve category path by uri.
   *
   * @param context the context
   *
   * @return the string
   */
  public String resolveCategoryPathByUri(WebuiRequestContext context) throws Exception {
    String parameters = null;
    try {
      // parameters: Classic/News/France/Blah/Bom
      parameters = URLDecoder.decode(StringUtils.substringAfter(Util.getPortalRequestContext()
                                                                    .getNodePath(),
                                                                Util.getUIPortal()
                                                                    .getSelectedUserNode()
                                                                    .getURI()), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }

    // categoryPath: /News/France/Blah/Bom
    String categoryPath = parameters.indexOf("/") >= 0 ? parameters.substring(parameters.indexOf("/")) : "";

    String gpath  = Util.getPortalRequestContext().getRequestParameter("path");
    if (gpath != null) {
      PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
      String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME,
                                                              "");
      categoryPath = gpath.substring(gpath.indexOf(preferenceTreeName)
          + preferenceTreeName.length());
    }


    return categoryPath;
  }

  /**
   * Gets the categories by uri.
   *
   * @param categoryUri the category uri
   *
   * @return the categories by uri
   *
   * @throws Exception the exception
   */
  public List<String> getCategoriesByUri(String categoryUri) throws Exception {
    PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
    String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    if (preferenceTreeName.equals(categoryUri)) categoryUri = "";

    // categories: {"/", "News", "News/France", "News/France/Blah", "News/France/Blah/Bom"}
    List<String> categories = new ArrayList<String>();
    String[] tempCategories = categoryUri.split("/");
    StringBuffer tempCategory = new StringBuffer();
    for (int i = 0; i < tempCategories.length; i++) {
      if (i == 0)
        tempCategory = new StringBuffer("");
      else if (i == 1)
        tempCategory = new StringBuffer(tempCategories[1]);
      else
        tempCategory.append("/").append(tempCategories[i]);
      categories.add(tempCategory.toString());
    }
    return categories;
  }

  /**
   * Render category link.
   *
   * @param node the node
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  public String renderCategoryLink(Node node) throws Exception {
    // preferenceTargetPage: products/presentation/pclv
    PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
    String preferenceTargetPage = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TARGET_PAGE, "");

    LivePortalManagerService livePortalManagerService = getApplicationComponent(LivePortalManagerService.class);
    Node portalNode = livePortalManagerService.getLivePortalByChild(node);
    String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
    String categoryPath = node.getPath().replaceFirst(portalNode.getPath(), "");
//    categoryPath = categoryPath.substring(categoryPath.indexOf(preferenceTreeName) + preferenceTreeName.length());
    categoryPath = categoryPath.substring(categoryPath.indexOf(preferenceTreeName)-1);

    NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL,
                                                         Util.getPortalRequestContext()
                                                             .getPortalOwner(),
                                                         preferenceTargetPage);
    nodeURL.setResource(resource).setQueryParameterValue("path", categoryPath);
    String link = nodeURL.toString();

    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    link = friendlyService.getFriendlyUri(link);

    return link;
  }

  /**
   * get content's title if exists (from exo:title property)
   *
   * @param node The node
   * @return the title
   * @throws Exception
   */
  public String getTitle(Node node) throws Exception {
    if (node.hasProperty("exo:title"))
      return node.getProperty("exo:title").getString();
    else
      return node.getName();
  }

  public String getTreeTitle() {
    return UICategoryNavigationUtils.getPortletPreferences().getValue(UICategoryNavigationConstant.PREFERENCE_TREE_TITLE, "");
  }

  private Node getCategoryNode(String categoryPath) throws Exception {
  TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
  PortletPreferences portletPreferences = UICategoryNavigationUtils.getPortletPreferences();
  String preferenceTreeName = portletPreferences.getValue(UICategoryNavigationConstant.PREFERENCE_TREE_NAME, "");
  Node treeNode = taxonomyService.getTaxonomyTree(preferenceTreeName);
  Node categoryNode = null;
  if ("".equals(categoryPath)) categoryNode = treeNode;
  else categoryNode = treeNode.getNode(categoryPath);
  return categoryNode;
  }
}
