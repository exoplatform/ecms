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
package org.exoplatform.services.cms.categories;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.categories.impl.TaxonomyPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * New service was created at org.exoplatform.services.cms.taxonomy.TaxonomyService
 * to replace this one.
 */
@Deprecated
public interface CategoriesService {
	
  /**
   * Adds a new taxonomy plugin that has been already configured in xml files
   * @param plugin            The plugin wants to add
   */
	public void addTaxonomyPlugin(ComponentPlugin plugin);
  
  /**
   * Gets the root node of taxonomy
   * @param repository        The name of repository
   * @param provider          The SessionProvider object is used to managed Sessions
   * @see                     SessionProvider 
   * @throws Exception
   */
	public Node getTaxonomyHomeNode(String repository, SessionProvider provider) throws Exception;
  
  /**
   * Adds a new taxonomy node to the node is specified by parentPath param
   * <p>If the taxonomy has already existed in this specific node then the method will thrown
   * <code>ItemExistsException</code>
   * @param parentPath        Specify the parent path which the taxonomy node will be added into
   * @param childName         The name of taxonomy node
   * @param repository        The name of the repository
   * @throws Exception
   */
	public void addTaxonomy(String parentPath, String childName, String repository) throws Exception;
  
  /**
   * Removes the taxonomy node is specified by real path
   * @param realPath          The real path to the taxonomy node is removed
   * @param repository        The name of the repository
   * @throws Exception
   */
	public void removeTaxonomyNode(String realPath, String repository) throws Exception;
  
  /**
   * Copys or cut the taxonomy node which is specified by srcPath params to new location is 
   * specified by desPath params
   * <p>The process cuts or copys is depend on type params
   * @param srcPath           The source path of this taxonomy
   * @param destPath          The destination path of the taxonomy 
   * @param type              The type params is specified in order to classify the process
   *                          If type is equal to "cut", the process will be cut
   *                          If type is equal to "copy", the process will be copied
   * @param repository        The name of the repository
   * @throws Exception
   */
	public void moveTaxonomyNode(String srcPath, String destPath, String type, String repository) throws Exception;		
	
  /**
   * Returns true is the given node has categories
   * @param node              Specify the node wants to check categories
   * @see                     Node
   * @throws Exception
   */
	public boolean hasCategories(Node node) throws Exception;
  
  /**
   * Gets all node that has been categoried in the given node
   * @param node              Specify the node wants to get categories
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
	public List<Node> getCategories(Node node, String repository) throws Exception;
  
  /**
   * Removes node which is categoried in the given node by specify the categoryPath params 
   * @param node              Specify the node wants to remove category from
   * @param categoryPath      The path of category
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
	public void removeCategory(Node node, String categoryPath, String repository) throws Exception;
	
  /**
   * Adds multi category to the given node by specify an array of category path
   * @param node              Specify the node wants to get multi category
   * @param arrCategoryPath   An array of category path
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
  public void addMultiCategory(Node node, String[] arrCategoryPath, String repository) throws Exception;
  
  /**
   * Adds a new category path to the given node
   * @param node              Specify the node wants to add a new category path
   * @param categoryPath      Specify the path that adds into the given node
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
	public void addCategory(Node node, String categoryPath, String repository) throws Exception;
  
  /**
   * Gets the current session by given the name of repository
   * @param repository        The name of repository
   * @see                     Session
   * @throws Exception
   */
  public Session getSession(String repository) throws Exception;
  
  /**
   * Adds a new category path to the given node
   * <p>Depend on the type of replaceAll params, this method will process different
   * @param node              Specify the node wants to add a new category path
   * @param categoryPath      Specify the path that adds into the given node
   * @param replaceAll        The replaceAll params is specified in order to classify the process
   *                          If replaceAll is <code>true</code>, this method will removes all 
   *                          category in the given node, then add a new category to that node
   *                          If replaceAll is <code>false</code>, this method will adds a new 
   *                          category path to the given node
   * @param repository        The name of repository
   * @see                     Node
   * @throws Exception
   */
	public void addCategory(Node node, String categoryPath, boolean replaceAll, String repository) throws Exception;
  
  /**
   * Initial all taxonomy plugin that has been already configured in xml files
   * @param repository        The name of repository
   * @see                     TaxonomyPlugin
   * @throws Exception
   */  
  public void init(String repository) throws Exception;
}
