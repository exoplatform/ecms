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
   * Manages dynamic Groovy templates for the WCM-based products.
   * All templates created by this service need to be stored in hierarchical context.<br>
   * For example:<br>
   * application 1 <br>
   *    /category 1 <br>
   *       /template 1 <br>
   *       /template 2 <br>
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
   * Adds a plugin.
   *
   * @param portletTemplatePlugin The portlet template plugin to be added.
   * @throws Exception The exception
   */
  public void addPlugin(PortletTemplatePlugin portletTemplatePlugin) throws Exception;

  /**
   * Gets all portlet names that have dynamic Groovy templates managed by the service.
   *
   * @param repository Name of the repository.
   * @return All managed portlet names.
   * @throws Exception The exception
   */
  public List<String> getAllManagedPortletName(String repository) throws Exception;

  /**
   * Gets templates under a given category.
   *
   * @param portletName Name of the portet that contains the given category.
   * @param category The given category.
   * @param sessionProvider The session provider.
   * @return The templates.
   * @throws Exception The exception
   */
  public List<Node> getTemplatesByCategory(String portletName,
                                           String category,
                                           SessionProvider sessionProvider) throws Exception;

  /**
   * Gets a template by its name under a given category.
   *
   * @param portletName Name of the portet that contains the given category.
   * @param category The given category.
   * @param templateName Name of the template.
   * @param sessionProvider The session provider.
   * @return The template.
   * @throws Exception The exception
   */
  public Node getTemplateByName(String portletName,
                                String category,
                                String templateName,
                                SessionProvider sessionProvider) throws Exception;

  /**
   * Gets a template by its path under a given category.
   * 
   * @param templatePath Path of the template.
   * @param sessionProvider The session provider.
   * @return The template.
   * @throws Exception The exception
   */
  public Node getTemplateByPath(String templatePath, SessionProvider sessionProvider) throws Exception;

  /**
   * Adds a template to the portlet template home.
   *
   * @param portletTemplateHome The portlet template home.
   * @param config Configuration of the portlet template.
   * @throws Exception The exception
   */
  public void addTemplate(Node portletTemplateHome, PortletTemplateConfig config) throws Exception;

  /**
   * Removes a template from a given portlet.
   *
   * @param portletName Name of the given portlet.
   * @param catgory Category of the template which needs to be removed.
   * @param templateName Name of the removed template.
   * @param sessionProvider The session provider.
   * @throws Exception The exception
   */
  public void removeTemplate(String portletName,
                             String catgory,
                             String templateName,
                             SessionProvider sessionProvider) throws Exception;
  
  /**
   * Gets the application template home.
   *
   * @param portletName Name of the portlet.
   * @param provider The session provider.
   * @return The application template home.
   * @throws Exception The exception
   */
  public Node getApplicationTemplateHome(String portletName, SessionProvider provider) throws Exception;
  
  /**
   * Gets all configured templates of a given portlet.
   *
   * @param portletName Name of the given portlet.
   * @return Set of configured templates.
   */
  public Set<String> getConfiguredAppTemplateMap(String portletName);

}
