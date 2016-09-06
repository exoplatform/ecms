/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.navigation;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.portlet.MimeResponse;
import javax.portlet.ResourceURL;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.webui.application.WebuiRequestContext;


/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 21, 2008
 */
public class NavigationUtils {

  public static final Scope ECMS_NAVIGATION_SCOPE = Scope.CHILDREN;

  private static ExoCache<String, Object> userNavigationCache;

  private static Constructor<UserNavigation> userNavigationCtor = null;

  private static final Log LOG = ExoLogger.getLogger(NavigationUtils.class.getName());
  static {
    try {
      //reflection here to get UserNavigation to avoid for using such as:
      //spaceNav = userPortal.getNavigation(SiteKey.group(groupId));
      userNavigationCtor = UserNavigation.class.getDeclaredConstructor(
                                          new Class[] {UserPortalImpl.class, NavigationContext.class, boolean.class});
      userNavigationCtor.setAccessible(true);
      userNavigationCache= WCMCoreUtils.getService(CacheService.class).getCacheInstance("CacheUserNavigation");
    }catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  } //of static reflection

  public static boolean gotNavigation(String portal, String user) {
    return gotNavigation(portal, user, "");
  }
  
  public static boolean gotNavigation(String portal, String user, String scope) {
    if (userNavigationCache.get(portal + " " + user + " " + scope) != null)
      return true;
    return false;
  }  

  public static UserNavigation getUserNavigationOfPortal(UserPortal userPortal, String portalName) throws Exception {
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    NavigationContext portalNav = userPortalConfigService.getNavigationService().
                                        loadNavigation(new SiteKey(SiteType.PORTAL, portalName));
    if (portalNav ==null) {
      return null;
    }
    UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
            ConversationState.getCurrent().getIdentity().getUserId(),
            PortalRequestContext.USER_PORTAL_CONTEXT);
    return userNavigationCtor.newInstance(
            userPortal, portalNav,
            userACL.hasEditPermission(userPortalCfg.getPortalConfig()));
  }

  /**
   * Get UserNavigation of a specified element
   * @param userPortal
   * @param siteKey Key
   * @return UserNavigation of group
   */
  public static UserNavigation getUserNavigation(UserPortal userPortal, SiteKey siteKey) throws Exception {
    if (siteKey.getTypeName().equalsIgnoreCase(SiteType.PORTAL.getName())) {
      return getUserNavigationOfPortal(userPortal,siteKey.getName());
    }
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    //userPortalConfigService.get
    NavigationContext portalNav = userPortalConfigService.getNavigationService().
      loadNavigation(siteKey);
    if (portalNav == null) {
      return null;
    } else {
      return userNavigationCtor.newInstance(userPortal, portalNav, userACL.hasEditPermissionOnNavigation(siteKey));
    }
  }
  
  public static void removeNavigationAsJson (String portalName, String username) throws Exception {
    for (String scope : new String[]{"single", "children", "grandchildren", "all"})
      removeNavigationAsJson(portalName, username, scope);
  }
  
  public static void removeNavigationAsJson (String portalName, String username, String scope) throws Exception
  {
    String key = portalName + " " + username + " " + scope;
    userNavigationCache.remove(key);
  }

  public static String getNavigationAsJSON(String portalName, String username) throws Exception {
    return getNavigationAsJSON(portalName, username, null, "");
  }
  
  public static String getNavigationAsJSON(String portalName, String username, Scope scope, String navigationScope) throws Exception {

    String key = portalName + " " + username + " " + navigationScope;
    String navigationData =(String)userNavigationCache.get(key);
    if (navigationData != null) {
      return navigationData;
    }

    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
                                                                                 username,
                                                                                 PortalRequestContext.USER_PORTAL_CONTEXT);
    UserPortal userPortal = userPortalCfg.getUserPortal();

    //filter nodes
    UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
    filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
    filterConfigBuilder.withTemporalCheck();
    UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

    //get nodes
    UserNavigation navigation = getUserNavigationOfPortal(userPortal, portalName);
    UserNode root = userPortal.getNode(navigation, scope == null ? ECMS_NAVIGATION_SCOPE : scope, filterConfig, null);

    String ret = createJsonTree(navigation, root);
    userNavigationCache.put(key, ret);
    return ret;
  }

  private static String createJsonTree(UserNavigation navigation, UserNode rootNode) throws Exception {
    StringBuffer sbJsonTree = new StringBuffer();
    sbJsonTree.append("[");
    sbJsonTree.append("{");
    sbJsonTree.append("\"ownerId\":\"").append(navigation.getKey().getName()).append("\",");
    sbJsonTree.append("\"ownerType\":\"").append(navigation.getKey().getTypeName()).append("\",");
    sbJsonTree.append("\"priority\":\"").append(navigation.getPriority()).append("\",");
    sbJsonTree.append("\"nodes\":").append(addJsonNodes(rootNode.getChildren().iterator()));
    sbJsonTree.append("}");
    sbJsonTree.append("]");
    return sbJsonTree.toString();
  }

  private static StringBuffer addJsonNodes(Iterator<UserNode> children) throws Exception {
    StringBuffer sbJsonTree = new StringBuffer();
    sbJsonTree.append("[");
    boolean first = true;

    while (children.hasNext()) {
      UserNode child = children.next();
      if (!first) {
        sbJsonTree.append(",");
      }
      first = false;
      sbJsonTree.append("{");
      sbJsonTree.append("\"icon\":").append(child.getIcon() != null ? "\"" + child.getIcon() + "\""
                                                                   : "null").append(",");
      sbJsonTree.append("\"label\":\"").append(child.getLabel()).append("\",");
      sbJsonTree.append("\"name\":\"").append(child.getName()).append("\",");
      sbJsonTree.append("\"resolvedLabel\":\"").append(child.getResolvedLabel()).append("\",");
      String childURI = "";
      if (child.getPageRef() != null){
        childURI = child.getURI();
      }
      sbJsonTree.append("\"uri\":\"").append(childURI).append("\",");

      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      MimeResponse res = context.getResponse();
      ResourceURL resourceURL = res.createResourceURL();
      resourceURL.setResourceID(res.encodeURL(child.getURI()));
      Writer w = new StringWriter();
      resourceURL.write(w, true);
      sbJsonTree.append("\"getNodeURL\":\"").append(w.toString()).append("\",");
      sbJsonTree.append("\"nodes\":").append(addJsonNodes(child.getChildren().iterator()));
      sbJsonTree.append("}");
    }
    sbJsonTree.append("]");
    return sbJsonTree;
  }
}
