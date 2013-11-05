/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Manages views, including adding, editing, deleting and getting.
 *
 * @LevelAPI Experimental
 */
public interface ManageViewService {

  /** The type of extension related to this service. */
  public static final String EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIActionBar";

  /**
   * Adds a new view to the system by giving the following params.
   * 
   * @param name Name of the view.
   * @param permissions Who can access the view.
   * @param template Name of the templated used by the added view.
   * @param tabs Tabs of the added view.
   * @throws Exception The exception
   */
  public void addView(String name, String permissions, String template, List<?> tabs) throws Exception;
  
  /**
   * Adds a new view to the system by giving the following params.
   *
   * @param name Name of the view.
   * @param permissions Who can access the view.
   * @param hideExplorerPanel If "true", the explorer panel is hidden. If "false", the explorer panel is shown.
   * @param template Name of the templated used by the added view.
   * @param tabs Tabs of the added view.
   * @throws Exception The exception
   */
  public void addView(String name, String permissions, boolean hideExplorerPanel, String template, List<?> tabs) throws Exception;

  /**
   * Gets a view by its name.
   *
   * @param viewName Name of the view.
   * @param provider The session provider.
   * @return The view.
   * @throws Exception The exception
   */
  public Node getViewByName(String viewName, SessionProvider provider) throws Exception;  

  /**
   * Gets all strings of buttons.
   *
   * @return The list of buttons.
   * @throws Exception The exception
   */
  public List<?> getButtons() throws Exception;

  /**
   * Removes a view from the views list in the system.
   *
   * @param viewName Name of the removed view.
   * @throws Exception The exception
   */
  public void removeView(String viewName) throws Exception;  

  /**
   * Gets all views.
   *
   * @return The list of views.
   * @throws Exception The exception
   */
  public List<ViewConfig> getAllViews() throws Exception;  

  /**
   * Checks if a given view exists.
   *
   * @param name Name of the given view.
   * @return "True" if the given view exists. Otherwise, it returns "false".
   * @throws Exception The exception
   */
  public boolean hasView(String name) throws Exception;  

  /**
   * Gets the template home that contains all templates.
   *
   * @param homeAlias Alias of the template home.
   * @param provider The session provider.
   * @return The template home.
   * @throws Exception The exception
   */
  public Node getTemplateHome(String homeAlias, SessionProvider provider) throws Exception;  

  /**
   * Gets all templates.
   *
   * @param homeAlias Alias of the template home.
   * @param provider The session provider.
   * @return The list of templates.
   * @throws Exception The exception
   */
  public List<Node> getAllTemplates(String homeAlias,SessionProvider provider) throws Exception;  
  
  /**
   * Gets a template by a given path.
   *
   * @param path The given path.
   * @param provider The session provider.
   * @return The template.
   * @throws Exception The exception
   */
  public Node getTemplate(String path, SessionProvider provider) throws Exception;  

  /**
   * Adds a new template to a place with the given path.
   *
   * @param name Name of the new template.
   * @param content Content of the template.
   * @param homePath The given path.
   * @return The new template.
   * @throws Exception The exception
   */
  public String addTemplate(String name, String content, String homePath) throws Exception;
  
  /**
   * Adds a new template to a place with the given path.
   *
   * @param name Name of the new template.
   * @param content Content of the template.
   * @param homePath The given path.
   * @param provider The session provider.
   * @return The new template.
   * @throws Exception The exception
   */
  public String addTemplate(String name, String content, String homePath, SessionProvider provider) throws Exception;
  
  /**
   * Updates a template at the given path.
   *
   * @param name Name of the updated template.
   * @param content Content of the template.
   * @param homePath The given path.
   * @return The template.
   * @throws Exception The exception
   */
  public String updateTemplate(String name, String content, String homePath) throws Exception;
  
  /**
   * Updates a template at the given path.
   *
   * @param name Name of the updated template.
   * @param content Content of the template.
   * @param homePath The given path.
   * @param provider The session provider.
   * @return The template.
   * @throws Exception The exception
   */
  public String updateTemplate(String name,
                               String content,
                               String homePath,
                               SessionProvider provider) throws Exception;

  /**
   * Removes a template by a given path. 
   *
   * @param templatePath The template path.
   * @throws Exception The exception
   */
  public void removeTemplate(String templatePath) throws Exception;  
  
  /**
   * Removes a template by a given path.
   * 
   * @param templatePath The template path.
   * @param provider The session provider.
   * @throws Exception The exception
   */
  public void removeTemplate(String templatePath, SessionProvider provider) throws Exception;

  /**
   * Adds a new tab to the given view.
   *
   * @param view The given view.
   * @param name Name of the added tab.
   * @param buttons Buttons of the added tab.
   * @throws Exception The exception
   */
  public void addTab(Node view, String name, String buttons) throws Exception ;

  /**
   * Initializes all templates that are set in the configuration file.
   *
   * @throws Exception The exception
   */
  public void init() throws Exception ;
  
  /**
   * Gets all configured templates.
   *
   * @return Set of configured templates.
   */
  public Set<String> getConfiguredTemplates();
  
  /**
   * Gets all configured views.
   *
   * @return Set of configured views.
   */
  public Set<String> getConfiguredViews();

}
