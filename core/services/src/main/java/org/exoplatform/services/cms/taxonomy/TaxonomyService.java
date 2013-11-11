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
 * Is used to work with taxonomies. In this service,
 * there are many functions that enable you to add, find, or delete taxonomies from a node.
 *
 * @LevelAPI Experimental
 */
public interface TaxonomyService {
  
  /**
   * Gets a taxonomy tree.
   *
   * @param taxonomyName Name of the taxonomy tree.
   * @param system If "true", the system session is used.
   * If "false", the user session is used.
   * @return The taxonomy tree node.
   * @throws RepositoryException if the taxonomy tree could not be found.
   */
  public Node getTaxonomyTree(String taxonomyName, boolean system)
      throws RepositoryException;
  
  /**
   * Gets a taxonomy tree using the user session.
   *
   * @param taxonomyName Name of the taxonomy tree.
   * @return The taxonomy tree node.
   * @throws RepositoryException if the taxonomy tree could not be found.
   */
  public Node getTaxonomyTree(String taxonomyName) throws RepositoryException;  

  /**
   * Gets all taxonomy trees.
   *
   * @param system If "true", the system session is used.
   * If "false", the user session is used.
   * @return The taxonomy tree nodes.
   * @throws RepositoryException if the taxonomy trees could not be found.
   */
  public List<Node> getAllTaxonomyTrees(boolean system)
      throws RepositoryException;  

  /**
   * Gets all taxonomy trees using the user session.
   *
   * @return The taxonomy tree nodes.
   * @throws RepositoryException if the taxonomies could not be found.
   */
  public List<Node> getAllTaxonomyTrees() throws RepositoryException;  

  /**
   * Checks if a taxonomy tree with the given name has already been defined.
   *
   * @param taxonomyName Name of the taxonomy tree to be checked.
   * @return "True" if the taxonomy tree exists. Otherwise, it returns "false".
   * @throws RepositoryException if the taxonomy name could not be checked.
   */
  public boolean hasTaxonomyTree(String taxonomyName) throws RepositoryException;

  /**
   * Creates a new taxonomy tree.
   *
   * @param taxonomyTree The taxonomy tree to be created.
   * @throws TaxonomyAlreadyExistsException if a taxonomy with the same name has
   *           already been defined.
   * @throws RepositoryException if the taxonomy tree could not be defined.
   */
  public void addTaxonomyTree(Node taxonomyTree) throws RepositoryException,
      TaxonomyAlreadyExistsException;

  /**
   * Updates a taxonomy tree.
   *
   * @param taxonomyName Name of the taxonomy.
   * @param taxonomyTree The taxonomy tree to be updated.
   * @throws RepositoryException if the taxonomy tree could not be updated.
   */
  public void updateTaxonomyTree(String taxonomyName, Node taxonomyTree) throws RepositoryException;

  /**
   * Removes a taxonomy tree.
   *
   * @param taxonomyName Name of the taxonomy to be removed.
   * @throws RepositoryException if the taxonomy tree could not be removed.
   */
  public void removeTaxonomyTree(String taxonomyName) throws RepositoryException;

  /**
   * Adds a new taxonomy node at the given location.
   *
   * @param workspace Name of the workspace to which the taxonomy node is added.
   * @param parentPath The place where the taxonomy node will be added.
   * @param taxoNodeName Name of the taxonomy node.
   * @param creator The user who created the taxonomy node.
   * @throws TaxonomyNodeAlreadyExistsException if a taxonomy node with the same
   *           name has already been added.
   * @throws RepositoryException if the taxonomy node could not be added.
   */
  public void addTaxonomyNode(String workspace,
                              String parentPath,
                              String taxoNodeName,
                              String creator) throws RepositoryException,
                                             TaxonomyNodeAlreadyExistsException;
  
  /**
   * Removes a taxonomy node located at the given absolute path.
   *
   * @param workspace Name of the workspace from which the taxonomy node is removed.
   * @param absPath The given absolute path.
   * @throws RepositoryException if the taxonomy node could not be removed.
   */
  public void removeTaxonomyNode(String workspace, String absPath)
      throws RepositoryException;  

  /**
   * Copies or cuts the taxonomy node from source path to destination path.
   * The parameter type indicates if the node is cut or copied.
   * 
   * @param workspace Name of the workspace which contains the taxonomy node.
   * @param srcPath Source path of the taxonomy node.
   * @param destPath Destination path of the taxonomy node.
   * @param type Type of moving: copy or cut.
   * @throws RepositoryException if the taxonomy node could not be moved.
   */
  public void moveTaxonomyNode(String workspace, String srcPath, String destPath, String type) throws RepositoryException;

  /**
   * Checks if a category node exists in a given taxonomy by using the user session.
   *
   * @param node The category node to be checked.
   * @param taxonomyName Name of the given taxonomy.
   * @throws RepositoryException if the category cannot be checked.
   */
  public boolean hasCategories(Node node, String taxonomyName) throws RepositoryException;

  /**
   * Checks if a category node exists in a given taxonomy.
   *
   * @param node The category node to be checked.
   * @param taxonomyName Name of the given taxonomy.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @return "True" if the category exists. Otherwise, it returns "false".
   * @throws RepositoryException if the category cannot be checked.
   */
  public boolean hasCategories(Node node, String taxonomyName, boolean system) throws RepositoryException;

  /**
   * Gets all categories that contain a given node under a taxonomy tree.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree where categories are got.
   * @return The list of category nodes.
   * @throws RepositoryException if the categories cannot be retrieved.
   */
  public List<Node> getCategories(Node node, String taxonomyName) throws RepositoryException;

  /**
   * Gets all categories that contain a given node under a taxonomy tree.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree where categories are got.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @return The list of category nodes.
   * @throws RepositoryException if the categories cannot be retrieved.
   */
  public List<Node> getCategories(Node node, String taxonomyName, boolean system) throws RepositoryException;

  /**
   * Gets all categories that contain a given node under all taxonomy trees.
   *
   * @param node The given node.
   * @return The list of category nodes.
   * @throws RepositoryException
   */
  public List<Node> getAllCategories(Node node) throws RepositoryException;

  /**
   * Gets all categories that contain a given node under all taxonomy trees.
   *
   * @param node The given node.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @return The list of category nodes.
   * @throws RepositoryException
   */
  public List<Node> getAllCategories(Node node, boolean system) throws RepositoryException;

  /**
   * Removes a category from a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the removed category.
   * @param categoryPath Path of the removed category.
   * @throws RepositoryException if the category cannot be removed.
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException;

  /**
   * Removes a category from a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the removed category.
   * @param categoryPath Path of the removed category.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws RepositoryException if the category cannot be removed.
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException;

  /**
   * Adds categories to a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the added category.
   * @param categoryPaths Paths of the added categories.
   * @throws RepositoryException if the categories cannot be added.
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths)
      throws RepositoryException;

  /**
   * Adds categories to a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the added category.
   * @param categoryPaths Paths of the added categories.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws RepositoryException if the categories cannot be added.
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths, boolean system)
      throws RepositoryException;

  /**
   * Adds a new category to a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the added category.
   * @param categoryPath Path of the added category.
   * @throws RepositoryException if the category cannot be added.
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException;

  /**
   * Adds a new category to a given node.
   *
   * @param node The given node.
   * @param taxonomyName The taxonomy tree that contains the added category.
   * @param categoryPath Path of the added category.
   * @param system If "true", the system session is used. If "false", the user session is used.
   * @throws RepositoryException if the category cannot be added.
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException;

  /**
   * Gets default permissions of a taxonomy tree.
   *
   * @return Map that shows permissions of the taxonomy tree.
   */
  public Map<String, String[]> getTaxonomyTreeDefaultUserPermission();
  
  /**
  * Gets the limited length of a category name.
  */
  public String getCategoryNameLength();

  /**
   * Adds a new taxonomy plugin to the Taxonomy Service.
   *
   * @param plugin The plugin to be added.
   */
  public void addTaxonomyPlugin(ComponentPlugin plugin);
  
  /**
   * Initializes all taxonomy plugins that have been set in the configuration files.
   *
   * @see TaxonomyPlugin
   * @throws Exception
   */
  public void init() throws Exception;  
}
