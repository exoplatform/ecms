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
package org.exoplatform.services.cms.actions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

/**
 *
 * @author exo
 *
 */
public interface ActionServiceContainer {

  /**
   * Collection of String
   * @return collection of ActionPlugin namess
   */
  public Collection<String> getActionPluginNames();

  /**
   * Get ActionPlugin following ActionSeriveName
   * @param actionServiceName name of action service
   * @return ActionPlugin
   */
  public ActionPlugin getActionPlugin(String actionServiceName);

  /**
   * Get ActionPlugin following action type name
   * @param actionTypeName  name of action type
   * @return ActionPlugin
   */
  public ActionPlugin getActionPluginForActionType(String actionTypeName);

  /**
   * Create NodeTypeValue is in kind of ActionType following action type name
   * @param actionTypeName        name of action type
   * @param parentActionTypeName  name of parent action
   * @param executable            String value of executable
   * @param actionLabel           Label of action type
   * @param variableNames         List name of variable
   * @param isMoveType            is moved or not
   * @param isUpdate              True if the action type is updating
   * @throws Exception
   */
  public void createActionType(String actionTypeName,
                               String parentActionTypeName,
                               String executable,
                               String actionLabel,
                               List<String> variableNames,
                               boolean isMoveType,
                               boolean isUpdate) throws Exception;

  /**
   * Get all created node with nodetype = "exo:action
   * @param repository  repository name
   * @return Collection of NodeType
   * @throws Exception
   */
  public Collection<NodeType> getCreatedActionTypes(String repository) throws Exception;

  /**
   * get node by using actionName as relative path with current node
   * @param node        current processing node
   * @param actionName  name of action
   * @return  Node
   * @throws Exception
   */
  public Node getAction(Node node, String actionName) throws Exception;

  /**
   * Check node type is exo:actionable or not
   * @param node
   * @return true: NodeType is exo:actionable
   *         false NodeType is not exo:actionable
   * @throws Exception
   */
  public boolean hasActions(Node node) throws Exception;

  /**
   * Get list of child node with NodeType = exo:action
   * @param node  current node
   * @return list of node
   * @throws Exception
   */
  public List<Node> getActions(Node node) throws Exception;

  /**
   * Get list of node that have same level with current node, exo:lifecyclePhase = lifecyclePhase
   * @param node            current node
   * @param lifecyclePhase  exo:lifecyclePhase value
   * @return list of node
   * @throws Exception
   */
  public List<Node> getCustomActionsNode(Node node, String lifecyclePhase) throws Exception;

  /**
   * Get list of child node with exo:lifecyclePhase = lifecyclePhase
   * @param node            current node
   * @param lifecyclePhase  exo:lifecyclePhase value
   * @return list of node
   * @throws Exception
   */
  public List<Node> getActions(Node node, String lifecyclePhase) throws Exception;

  /**
   * Remove all action registered in node
   * @param node
   * @param repository
   * @throws Exception
   */
  public void removeAction(Node node, String repository) throws Exception;
  
  /**
   * Remove all relative node of current node with node type = exo:actionable
   * @param node        current node
   * @param actionName  relative path = exo:actionable / actionName
   * @param repository  repository name
   * @throws Exception
   */
  public void removeAction(Node node, String actionName, String repository) throws Exception;

  /**
   * Add mixintype = exo:actionable to current node
   * Add new node to current node with nodetype = type
   * @param node        current node
   * @param type        nodetype name
   * @param mappings    value of property for adding to new node
   * @throws Exception
   */
  public void addAction(Node node, String type, Map mappings) throws Exception;  

  /**
   * Add mixintype = exo:actionable to current node
   * Add new node to current node with nodetype = type
   * @param node        current node
   * @param type        nodetype name
   * @param isDeep      affect to child node of node
   * @param uuid        affect only to parent node of event having given uuid
   * @param nodeTypeNames        affect to parent node of event having nodetype in nodeTypeNames
   * @param mappings    value of property for adding to new node
   * @throws Exception
   */
  public void addAction(Node node,
                        String type,
                        boolean isDeep,
                        String[] uuid,
                        String[] nodeTypeNames,
                        Map mappings) throws Exception;  

  /**
   * Execute action following userId, node, variables, repository
   * @param userId      user identify
   * @param node        current node
   * @param actionName  name of action
   * @param variables   Map with variables and value
   * @throws Exception
   */
  public void executeAction(String userId,
                            Node node,
                            String actionName,
                            Map variables) throws Exception;

  /**
   * Execute action following userId, node, repository, initiated variables
   * @param userId user identify
   * @param node current node
   * @param actionName name of action
   * @throws Exception
   * @see #executeAction(String, Node, String, Map)
   */
  public void executeAction(String userId, Node node, String actionName) throws Exception;

  /**
   * Add action listener for all action child node of current node in current repository
   * @param node        current node
   * @throws Exception
   */
  public void initiateObservation(Node node) throws Exception;
  
  /**
   * Init all available action plugins. 
   * @throws Exception
   */
  public void init() throws Exception;  

}
