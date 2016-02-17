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
package org.exoplatform.services.cms.metadata;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;


/**
 * Author : Hung Nguyen Quang
 *          nguyenkequanghung@yahoo.com
 * Process with meta data for system
 */

public interface MetadataService {

  /**
   * Get name of all NodeType in current repository
   * @return              ArrayList of name
   * @see #getAllMetadatasNodeType()
   */
  public List<String> getMetadataList() throws Exception;  

  /**
   * Get all NodeType in current repository with NodeType = exo:metadata
   * @return              ArrayList of NodeType
   */
  public List<NodeType> getAllMetadatasNodeType() throws Exception;  

  /**
   * Add new nodetype and set property  EXO_ROLES_PROP, EXO_TEMPLATE_FILE_PROP
   * for dialog template node or view template node if node doesn't exist
   * Set property  EXO_ROLES_PROP, EXO_TEMPLATE_FILE_PROP
   * for dialog template node or view template node if node exists
   * @param nodetype    Node name for processing
   * @param isDialog    true for dialog template
   * @param role        permission
   * @param content     content of template
   * @param isAddNew    false if nodetype exist in repository, true if not
   * @return path to node if node exist, otherwise return null
   * @throws Exception
   */
  public String addMetadata(String nodetype,
                            boolean isDialog,
                            String role,
                            String content,
                            boolean isAddNew) throws Exception;  

  /**
   * Add new nodetype and set property  EXO_ROLES_PROP, EXO_TEMPLATE_FILE_PROP
   * for dialog template node or view template node if node doesn't exist
   * Set property  EXO_ROLES_PROP, EXO_TEMPLATE_FILE_PROP
   * for dialog template node or view template node if node exists
   * @param nodetype    Node name for processing
   * @param isDialog    true for dialog template
   * @param role        permission
   * @param content     content of template
   * @param label       lable of metadata
   * @param isAddNew    false if nodetype exist in repository, true if not
   * @return path to node if node exist, otherwise return null
   * @throws Exception
   */
  public String addMetadata(String nodetype,
                            boolean isDialog,
                            String role,
                            String content,
                            String label,
                            boolean isAddNew) throws Exception;
 
  /**
   * Remove node named nodetype below baseMetadataPath_
   * @param nodetype      name of node
   */
  public void removeMetadata(String nodetype) throws Exception;  

  /**
   * Get all NodeType name that contains property that is not autocreated
   * and name of NodeType differs from exo:metadata
   * @return                ArrayList of metadata type
   */
  public List<String> getExternalMetadataType() throws Exception;  

  /**
   * Get content of dialog template node or view template in current repository
   * @param name            Node name
   * @param isDialog        true: Get dialog template content
   *                        false: Get view template content
   * @return                content of template
   */
  public String getMetadataTemplate(String name, boolean isDialog) throws Exception;  
  
  /**
   * Get path to dialog template or view tempate node
   * @param name            Node name
   * @param isDialog        true: Get dialog template content
   *                        false: Get view template content
   * @return                path to template node
   */
  public String getMetadataPath(String name, boolean isDialog) throws Exception;  

  /**
   * Get permission of template node
   * @param name            Node name
   * @param isDialog        true: Get dialog template content
   *                        false: Get view template content
   * @return                String of permission
   */
  public String getMetadataRoles(String name, boolean isDialog) throws Exception;  

  /**
   * Check node with given name exists or not below baseMetadataPath_ path in repository
   * @param name            Node name
   * @return                true : Exist this node name<br>
   *                        false: Not exist this node name
   */
  public boolean hasMetadata(String name) throws Exception;  

  /**
   * Call all available in list of TemplatePlugin to
   * add some predefine template to current repository.
   * @throws Exception
   */
  public void init() throws Exception ;
  
  /**
   * Get the metadata node based on its name
   * @param metaName Name of metadata
   * @return Node instance of give metadata
   * @throws Exception 
   */
  public Node getMetadata(String metaName) throws Exception;
  
  /**
   * Get metadata label based on its name
   * @param metaName Name of metadata
   * @return Label of given metadata
   * @throws Exception 
   */
  public String getMetadataLabel(String metaName) throws Exception;

}
