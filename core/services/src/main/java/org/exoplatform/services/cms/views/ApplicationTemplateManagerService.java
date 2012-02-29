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
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 15, 2008
 */

  /**
   * ApplicationTemplateManagerService is used to manage dynamic groovy templates
   * for ecm-based products. All implementation of this service need stored the templates in hierarchical context
   * application 1
   *    /category 1
   *       /template 1
   *       /template 2
   *    /category 2
   *
   * */
public interface ApplicationTemplateManagerService {

  /**
   * Adds the plugin.
   *
   * @param portletTemplatePlugin the portlet template plugin
   *
   * @throws Exception the exception
   */
  public void addPlugin(PortletTemplatePlugin portletTemplatePlugin) throws Exception;

  /**
   * Retrieves  the all portlet names that have dynamic groovy templates are managed by service.
   *
   * @param repository the repository
   *
   * @return the all managed portlet name
   *
   * @throws Exception the exception
   */
  public List<String> getAllManagedPortletName(String repository) throws Exception;
  
  /**
   * Retrieves the templates node by application.
   *
   * @param repository the repository
   * @param portletName the portlet name
   * @param provider the provider
   *
   * @return the templates by application
   *
   * @throws Exception the exception
   */
  @Deprecated
  public List<Node> getTemplatesByApplication(String repository,
                                              String portletName,
                                              SessionProvider provider) throws Exception;
  
  /**
   * Retrieves the templates node by application.
   *
   * @param portletName the portlet name
   * @param provider the provider
   *
   * @return the templates by application
   *
   * @throws Exception the exception
   */
  public List<Node> getTemplatesByApplication(String portletName, SessionProvider provider) throws Exception;

  /**
   * Retrieves the templates node by category.
   *
   * @param repository the repository
   * @param portletName the portlet name
   * @param category the category
   * @param sessionProvider the session provider
   *
   * @return the templates by category
   *
   * @throws Exception the exception
   */
  @Deprecated
  public List<Node> getTemplatesByCategory(String repository,
                                           String portletName,
                                           String category,
                                           SessionProvider sessionProvider) throws Exception;
  
  /**
   * Retrieves the templates node by category.
   *
   * @param portletName the portlet name
   * @param category the category
   * @param sessionProvider the session provider
   *
   * @return the templates by category
   *
   * @throws Exception the exception
   */
  public List<Node> getTemplatesByCategory(String portletName,
                                           String category,
                                           SessionProvider sessionProvider) throws Exception;

  /**
   * Retrieves the template by name.
   *
   * @param repository the repository
   * @param portletName the portlet name
   * @param category the category
   * @param templateName the template name
   * @param sessionProvider the session provider
   *
   * @return the template by name
   *
   * @throws Exception the exception
   */
  @Deprecated
  public Node getTemplateByName(String repository,
                                String portletName,
                                String category,
                                String templateName,
                                SessionProvider sessionProvider) throws Exception;
  
  /**
   * Retrieves the template by name.
   *
   * @param portletName the portlet name
   * @param category the category
   * @param templateName the template name
   * @param sessionProvider the session provider
   *
   * @return the template by name
   *
   * @throws Exception the exception
   */
  public Node getTemplateByName(String portletName,
                                String category,
                                String templateName,
                                SessionProvider sessionProvider) throws Exception;

  /**
   * Gets the template by path.
   *
   * @param repository the repository
   * @param templatePath the template path
   * @param sessionProvider the session provider
   *
   * @return the template by path
   *
   * @throws Exception the exception
   */
  @Deprecated
  public Node getTemplateByPath(String repository,
                                String templatePath,
                                SessionProvider sessionProvider) throws Exception;
  
  /**
   * Gets the template by path.
   * 
   * @param templatePath the template path
   * @param sessionProvider the session provider
   * @return the template by path
   * @throws Exception the exception
   */
  public Node getTemplateByPath(String templatePath, SessionProvider sessionProvider) throws Exception;

  /**
   * Adds the template.
   *
   * @param portletTemplateHome the portlet template home
   * @param config the config
   *
   * @throws Exception the exception
   */
  public void addTemplate(Node portletTemplateHome, PortletTemplateConfig config) throws Exception;

  /**
   * Removes the template.
   *
   * @param repository the repository
   * @param portletName the portlet name
   * @param catgory the catgory
   * @param templateName the template name
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  @Deprecated
  public void removeTemplate(String repository,
                             String portletName,
                             String catgory,
                             String templateName,
                             SessionProvider sessionProvider) throws Exception;
  
  /**
   * Removes the template.
   *
   * @param portletName the portlet name
   * @param catgory the catgory
   * @param templateName the template name
   * @param sessionProvider the session provider
   *
   * @throws Exception the exception
   */
  public void removeTemplate(String portletName,
                             String catgory,
                             String templateName,
                             SessionProvider sessionProvider) throws Exception;
  
  /**
   * Gets the application template home.
   * @param portletName       String
   *                          The name of portlet
   * @param provider          SessionProvider
   * @see SessionProvider
   * @return the application template home
   * @throws Exception the exception
   */
  public Node getApplicationTemplateHome(String portletName, SessionProvider provider) throws Exception;
  
  /**
   * gets all configured templates of the portlet
   * @param portletName name of the portlet
   * @return
   */
  public Set<String> getConfiguredAppTemplateMap(String portletName);

}
