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
package org.exoplatform.services.cms.views;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

  /**
   * This class is used to manage dynamic groovy templates for WCM-based products.
   * All implementation of this service need stored the templates in hierarchical context
   * For example :
   * application 1
   *    /category 1
   *       /template 1
   *       /template 2
   *    /category 2
   *
   * @LevelAPI Experimental
   */
public interface ApplicationTemplateManagerService {

  public final static String CLV_PAGINATOR_TEMPLATE_CATEGORY             = "paginators";
  
  public final static String CLV_NAVIGATION_TEMPLATE_CATEGORY             = "navigation";
  
  public final static String CLV_LIST_TEMPLATE_CATEGORY             = "list";
  
  public final static String CLV_TEMPLATE_STORAGE_FOLDER                 = "content-list-viewer";

  /**
   * Add the plugin.
   *
   * @param portletTemplatePlugin The portlet template plugin
   * @throws Exception The exception
   */
  public void addPlugin(PortletTemplatePlugin portletTemplatePlugin) throws Exception;

  /**
   * Retrieve all the portlet names that have dynamic groovy templates managed by service.
   *
   * @param repository The repository
   * @return All managed portlet name
   * @throws Exception The exception
   */
  public List<String> getAllManagedPortletName(String repository) throws Exception;

  /**
   * Retrieve the templates node by category.
   *
   * @param portletName The portlet name
   * @param category The category
   * @param sessionProvider The session provider
   * @return All templates by category
   * @throws Exception The exception
   */
  public List<Node> getTemplatesByCategory(String portletName,
                                           String category,
                                           SessionProvider sessionProvider) throws Exception;

  /**
   * Retrieve the template by name.
   *
   * @param portletName The portlet name
   * @param category The category
   * @param templateName The template name
   * @param sessionProvider The session provider
   * @return The template
   * @throws Exception The exception
   */
  public Node getTemplateByName(String portletName,
                                String category,
                                String templateName,
                                SessionProvider sessionProvider) throws Exception;

  /**
   * Get the template by path.
   * 
   * @param templatePath The template path
   * @param sessionProvider The session provider
   * @return The template
   * @throws Exception The exception
   */
  public Node getTemplateByPath(String templatePath, SessionProvider sessionProvider) throws Exception;

  /**
   * Add the template.
   *
   * @param portletTemplateHome The portlet template home
   * @param config The config
   * @throws Exception The exception
   */
  public void addTemplate(Node portletTemplateHome, PortletTemplateConfig config) throws Exception;

  /**
   * Remove the template.
   *
   * @param portletName The portlet name
   * @param catgory The catgory
   * @param templateName The template name
   * @param sessionProvider The session provider
   * @throws Exception The exception
   */
  public void removeTemplate(String portletName,
                             String catgory,
                             String templateName,
                             SessionProvider sessionProvider) throws Exception;
  
  /**
   * Get the application template home.
   *
   * @param portletName The name of portlet
   * @param provider The session provider
   * @return The application template home
   * @throws Exception The exception
   */
  public Node getApplicationTemplateHome(String portletName, SessionProvider provider) throws Exception;
  
  /**
   * Get all configured templates of the portlet.
   *
   * @param portletName The name of the portlet
   * @return Set<String>
   */
  public Set<String> getConfiguredAppTemplateMap(String portletName);

}
