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
package org.exoplatform.services.cms.taxonomy;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyNodeAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyPlugin;

/**
 * Taxonomy service is used to work with taxonomies. In this service,
 * there are many functions which enable you to add, find, or delete taxonomies from a node.
 *
 * @LevelAPI Experimental
 */
public interface TaxonomyService {
  
  /**
   * Returns the root node of the given taxonomy tree.
   *
   * @param taxonomyName The name of the taxonomy
   * @param system Indicates whether the nodes must be retrieved using a session
   *          system or user session
   * @return Node
   * @throws RepositoryException if the taxonomy tree could not be found
   */
  public Node getTaxonomyTree(String taxonomyName, boolean system)
      throws RepositoryException;
  
  /**
   * Returns the root node of the given taxonomy tree with the user session.
   *
   * @param taxonomyName The name of the taxonomy
   * @return Node
   * @throws RepositoryException if the taxonomy tree could not be found
   */
  public Node getTaxonomyTree(String taxonomyName) throws RepositoryException;  

  /**
   * Returns the list of all the root nodes of the taxonomy tree available.
   *
   * @param system Indicates whether the nodes must be retrieved using a session
   *          system or user session
   * @return List of Node
   * @throws RepositoryException if the taxonomy trees could not be found
   */
  public List<Node> getAllTaxonomyTrees(boolean system)
      throws RepositoryException;  

  /**
   * Returns the list of all the root nodes of the taxonomy tree available with the user session.
   *
   * @return List of Node
   * @throws RepositoryException if the taxonomies could not be found
   */
  public List<Node> getAllTaxonomyTrees() throws RepositoryException;  

  /**
   * Checks if a taxonomy tree with the given name has already been defined.
   *
   * @param taxonomyName The name of the taxonomy
   * @return True if a Taxonomy Tree exist or False if not
   * @throws RepositoryException if the taxonomy name could not be checked
   */
  public boolean hasTaxonomyTree(String taxonomyName) throws RepositoryException;

  /**
   * Defines a node as a new taxonomy tree
   *
   * @param taxonomyTree The taxonomy tree to define
   * @throws TaxonomyAlreadyExistsException if a taxonomy with the same name has
   *           already been defined
   * @throws RepositoryException if the taxonomy tree could not be defined
   */
  public void addTaxonomyTree(Node taxonomyTree) throws RepositoryException,
      TaxonomyAlreadyExistsException;

  /**
   * Re-defines a node as a taxonomy tree.
   *
   * @param taxonomyName The name of the taxonomy to update
   * @param taxonomyTree The taxonomy tree to define
   * @throws RepositoryException if the taxonomy tree could not be updated
   */
  public void updateTaxonomyTree(String taxonomyName, Node taxonomyTree) throws RepositoryException;

  /**
   * Remove the taxonomy tree definition.
   *
   * @param taxonomyName The name of the taxonomy to remove
   * @throws RepositoryException if the taxonomy tree could not be removed
   */
  public void removeTaxonomyTree(String taxonomyName) throws RepositoryException;

  /**
   * Adds a new taxonomy node at the given location.
   *
   * @param workspace The name of the workspace
   * @param parentPath The place where the taxonomy node will be added
   * @param taxoNodeName The name of taxonomy node
   * @param creator The name of the user creating this node
   * @throws TaxonomyNodeAlreadyExistsException if a taxonomy node with the same
   *           name has already been added
   * @throws RepositoryException if the taxonomy node could not be added
   */
  public void addTaxonomyNode(String workspace,
                              String parentPath,
                              String taxoNodeName,
                              String creator) throws RepositoryException,
                                             TaxonomyNodeAlreadyExistsException;
  
  /**
   * Removes the taxonomy node located at the given absolute path.
   *
   * @param workspace The name of the workspace
   * @param absPath The absolute path of the taxonomy node to remove
   * @throws RepositoryException if the taxonomy node could not be removed
   */
  public void removeTaxonomyNode(String workspace, String absPath)
      throws RepositoryException;  

  /**
   * Copies or cuts the taxonomy node from source path to destination path.
   * The parameter type indicates if the node must be cut or copied.
   * 
   * @param workspace The name of the workspace
   * @param srcPath The source path of this taxonomy
   * @param destPath The destination path of the taxonomy
   * @param type If type is equal to "cut", the process will be cut If type is
   *          equal to "copy", the process will be copied
   * @throws RepositoryException if the taxonomy node could not be moved
   */
  public void moveTaxonomyNode(String workspace, String srcPath, String destPath, String type) throws RepositoryException;

  /**
   * Returns true if the given node has categories in the given taxonomy.
   *
   * @param node The node to check
   * @param taxonomyName The name of the taxonomy
   * @throws RepositoryException if categories cannot be checked
   */
  public boolean hasCategories(Node node, String taxonomyName) throws RepositoryException;

  /**
   * Returns true if the given node has categories in the given taxonomy.
   *
   * @param node The node to check
   * @param taxonomyName The name of the taxonomy
   * @param system check system provider or not
   * @return True if Categories exists or False if not
   * @throws RepositoryException if categories cannot be checked
   */
  public boolean hasCategories(Node node, String taxonomyName, boolean system) throws RepositoryException;

  /**
   * Returns all the paths of the categories (relative to the root node of the
   * given taxonomy) which have been associated to the given node for the given
   * taxonomy.
   *
   * @param node The node for which we seek the categories
   * @param taxonomyName The name of the taxonomy
   * @return List of Node
   * @throws RepositoryException if the categories cannot be retrieved
   */
  public List<Node> getCategories(Node node, String taxonomyName) throws RepositoryException;

  /**
   * Returns all the paths of the categories (relative to the root node of the
   * given taxonomy) which have been associated to the given node for the given
   * taxonomy
   *
   * @param node The node for which we seek the categories
   * @param taxonomyName The name of the taxonomy
   * @param system
   * @return List of Node
   * @throws RepositoryException if the categories cannot be retrieved
   */
  public List<Node> getCategories(Node node, String taxonomyName, boolean system) throws RepositoryException;

  /**
   * Returns all the paths of the categories which have been associated to the given node.
   *
   * @param node  The node for which we seek the categories
   * @return List of Node
   * @throws RepositoryException
   */
  public List<Node> getAllCategories(Node node) throws RepositoryException;

  /**
   * Returns all the paths of the categories which have been associated to the given node.
   *
   * @param node  The node for which we seek the categories
   * @param system check system provider or not
   * @return List of Node
   * @throws RepositoryException
   */
  public List<Node> getAllCategories(Node node, boolean system) throws RepositoryException;

  /**
   * Removes a category to the given node.
   *
   * @param node The node for which we remove the category
   * @param taxonomyName The name of the taxonomy
   * @param categoryPath The path of the category relative to the root node of
   *          the given taxonomy
   * @throws RepositoryException if the category cannot be removed
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException;

  /**
   * Removes a category to the given node.
   *
   * @param node The node for which we remove the category
   * @param taxonomyName The name of the taxonomy
   * @param categoryPath The path of the category relative to the root node of
   *          the given taxonomy
   * @param system check system provider or not
   * @throws RepositoryException if the category cannot be removed
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException;

  /**
   * Adds several categories to the given node.
   *
   * @param node The node for which we add the categories
   * @param taxonomyName The name of the taxonomy
   * @param categoryPaths An array of category paths relative to the given
   *          taxonomy
   * @throws RepositoryException if the categories cannot be added
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths)
      throws RepositoryException;

  /**
   * Adds several categories to the given node.
   *
   * @param node The node for which we add the categories
   * @param taxonomyName The name of the taxonomy
   * @param categoryPaths An array of category paths relative to the given
   *          taxonomy
   * @param system check system provider or not
   * @throws RepositoryException if the categories cannot be added
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths, boolean system)
      throws RepositoryException;

  /**
   * Adds a new category path to the given node.
   *
   * @param node the node for which we add the category
   * @param taxonomyName The name of the taxonomy
   * @param categoryPath The path of the category relative to the given taxonomy
   * @throws RepositoryException if the category cannot be added
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException;

  /**
   * Adds a new category path to the given node.
   *
   * @param node the node for which we add the category
   * @param taxonomyName The name of the taxonomy
   * @param categoryPath The path of the category relative to the given taxonomy
   * @param system check system provider or not
   * @throws RepositoryException if the category cannot be added
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException;

  /**
   * Get default user permission of Taxonomy tree.
   *
   * @return Map
   */
  public Map<String, String[]> getTaxonomyTreeDefaultUserPermission();
  
  /**
  * Get the limited length of the category name.
  */
  public String getCategoryNameLength();

  /**
   * Add a new taxonomy plugin to the service.
   *
   * @param plugin The plugin to add
   */
  public void addTaxonomyPlugin(ComponentPlugin plugin);
  
  /**
   * Initialize all taxonomy plugins that have been already configured in .xml files.
   *
   * @see {@link TaxonomyPlugin}
   * @throws Exception
   */
  public void init() throws Exception;  
}
