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
 * ManageViewService is used to work with views. This service has many functions which
 * allow you to add, edit, delete, and get views.
 *
 * @LevelAPI Experimental
 */
public interface ManageViewService {

  /** The type of extension related to this service */
  public static final String EXTENSION_TYPE = "org.exoplatform.ecm.dms.UIActionBar";

  /**
   * Insert a new view to the system by giving the following params.
   * 
   * @param name The name of the view.
   * @param permissions Who can access to the view .
   * @param template The template name.
   * @param tabs list of tabs.
   * @throws Exception The exception
   */
  public void addView(String name, String permissions, String template, List<?> tabs) throws Exception;
  
  /**
   * Insert a new view to the system by giving the following params.
   *
   * @param name The name of the view.
   * @param permissions Who can access to the view .
   * @param hideExplorerPanel If the explorer panel is hidden.
   * @param template The template name.
   * @param tabs list of tabs.
   * @throws Exception The exception
   */
  public void addView(String name, String permissions, boolean hideExplorerPanel, String template, List<?> tabs) throws Exception;

  /**
   * Specify a new view depending on the view name.
   *
   * @param viewName The name of the view.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @return Get the View
   * @throws Exception The exception
   */
  public Node getViewByName(String viewName, SessionProvider provider) throws Exception;  

  /**
   * Return all strings of buttons.
   *
   * @return List of buttons.
   * @throws Exception The exception
   */
  public List<?> getButtons() throws Exception;

  /**
   * Remove a view from the views list in the system.
   *
   * @param viewName The name of the view.
   * @throws Exception The exception
   */
  public void removeView(String viewName) throws Exception;  

  /**
   * Return all views of the repository configured in the XML file.
   *
   * @return List<ViewConfig>
   * @throws Exception The exception
   */
  public List<ViewConfig> getAllViews() throws Exception;  

  /**
   * Return true if the given repository has a view.
   *
   * @param name The repository name.
   * @return True if the repository has the view
   * @throws Exception The exception
   */
  public boolean hasView(String name) throws Exception;  

  /**
   * Get a template node that has the path.
   *
   * @param homeAlias Alias of template home.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @return The template home
   * @throws Exception The exception
   */
  public Node getTemplateHome(String homeAlias, SessionProvider provider) throws Exception;  

  /**
   * Get all template nodes that have the path.
   *
   * @param homeAlias Alias of template home.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @return The List of templates
   * @throws Exception The exception
   */
  public List<Node> getAllTemplates(String homeAlias,SessionProvider provider) throws Exception;  
  
  /**
   * Return a node that has the path of the repository.
   *
   * @param path The template path.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @return The template
   * @throws Exception The exception
   */
  public Node getTemplate(String path, SessionProvider provider) throws Exception;  

  /**
   * Insert a new template to a node by specifying its path.
   *
   * @param name The name of the new template.
   * @param content The template property.
   * @param homePath The path of the specified node.
   * @return The template name
   * @throws Exception The exception
   */
  public String addTemplate(String name, String content, String homePath) throws Exception;
  
  /**
   * Insert a new template to a node by specifying its path.
   *
   * @param name The name of the new template.
   * @param content The template property.
   * @param homePath The path of the specified node.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @return The template name
   * @throws Exception The exception
   */
  public String addTemplate(String name, String content, String homePath, SessionProvider provider) throws Exception;
  
  /**
   * Update a template for a node by specifying its path.
   *
   * @param name The name of the new template.
   * @param content The template property.
   * @param homePath The path of the specified node.
   * @return The template name
   * @throws Exception The exception
   */
  public String updateTemplate(String name, String content, String homePath) throws Exception;
  
  /**
   * Update a template for a node by specifying its path.
   *
   * @param name The name of the new template.
   * @param content The template property.
   * @param homePath The path of the specified node.
   * @param provider The SessionProvider object is used to managed Sessions
   * @return The template name
   * @throws Exception The exception
   */
  public String updateTemplate(String name,
                               String content,
                               String homePath,
                               SessionProvider provider) throws Exception;

  /**
   * Remove the template from the given node by specifying its path.
   *
   * @param templatePath The template path.
   * @throws Exception The exception
   */
  public void removeTemplate(String templatePath) throws Exception;  
  
  /**
   * Removes the template to the given node by specified the templatePath params
   * 
   * @param templatePath The template path.
   * @param provider The SessionProvider object is used to managed Sessions.
   * @throws Exception The exception
   */
  public void removeTemplate(String templatePath, SessionProvider provider) throws Exception;

  /**
   * Insert a new tab to the given view node.
   *
   * @param view Specify the node wants to add in a tab.
   * @param name The tab's name.
   * @param buttons The buttons of tab.
   * @throws Exception The exception
   */
  public void addTab(Node view, String name, String buttons) throws Exception ;

  /**
   * Get all templates that are configured in the XML file of a specified repository.
   *
   * @throws Exception The exception
   */
  public void init() throws Exception ;
  
  /**
   * Get all configured templates.
   *
   * @return Set of template s
   */
  public Set<String> getConfiguredTemplates();
  
  /**
   * Get all configured views.
   *
   * @return Set of Views
   */
  public Set<String> getConfiguredViews();

}
