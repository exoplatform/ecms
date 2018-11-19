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
package org.exoplatform.services.cms.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.ItemDefinitionImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;


/**
 * @author benjaminmestrallet
 */
public class CmsServiceImpl implements CmsService {

  private RepositoryService jcrService;
  private IDGeneratorService idGeneratorService;
  private static final String MIX_REFERENCEABLE = "mix:referenceable" ;
  private static final Log LOG  = ExoLogger.getLogger(CmsServiceImpl.class.getName());
  private ListenerService listenerService;
  private ActivityCommonService activityService = null;

  public static Map<String, Object> properties = new HashMap<String, Object>();  
  public Map<String, Object> getPreProperties() { return properties; }  

  /**
   * Method constructor
   * @param jcrService: Manage repository
   * @param idGeneratorService: Generate an identify string
   * @param listenerService: Register listener, broadcast some event after create/edit node
   */
  public CmsServiceImpl(RepositoryService jcrService, IDGeneratorService idGeneratorService, ListenerService listenerService) {
    this.idGeneratorService = idGeneratorService;
    this.jcrService = jcrService;
    this.listenerService = listenerService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String storeNode(String workspace, String nodeTypeName, String storePath, Map mappings) throws Exception {
    Session session = jcrService.getCurrentRepository().login(workspace);
    Node storeHomeNode = (Node) session.getItem(storePath);
    String path = storeNode(nodeTypeName, storeHomeNode, mappings, true);
    storeHomeNode.save();
    session.save();
    session.logout();
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String storeNode(String nodeTypeName, Node storeHomeNode, Map mappings,
                          boolean isAddNew) throws Exception {
    Set keys = mappings.keySet();
    String nodePath = extractNodeName(keys);
    JcrInputProperty relRootProp = (JcrInputProperty) mappings.get(nodePath);
    String nodeName = (String)relRootProp.getValue();
    if (nodeName == null || nodeName.length() == 0) {
      nodeName = idGeneratorService.generateStringID(nodeTypeName);
    }
    nodeName = Text.escapeIllegalJcrChars(nodeName);
    String primaryType = relRootProp.getNodetype() ;
    if(primaryType == null || primaryType.length() == 0) {
      primaryType = nodeTypeName ;
    }
    Session session = storeHomeNode.getSession();
    NodeTypeManager nodetypeManager = session.getWorkspace().getNodeTypeManager();
    NodeType nodeType = nodetypeManager.getNodeType(primaryType);
    Node currentNode = null;
    String[] mixinTypes = null ;
    String mixintypeName = relRootProp.getMixintype();
    if(mixintypeName != null && mixintypeName.trim().length() > 0) {
      if(mixintypeName.indexOf(",") > -1){
        mixinTypes = mixintypeName.split(",") ;
      }else {
        mixinTypes = new String[] {mixintypeName} ;
      }
    }
    if (activityService==null) {
      activityService = WCMCoreUtils.getService(ActivityCommonService.class);
    }
    if (isAddNew) {
      //Broadcast CmsService.event.preCreate event
      listenerService.broadcast(PRE_CREATE_CONTENT_EVENT,storeHomeNode,mappings);
      currentNode = storeHomeNode.addNode(nodeName, primaryType);
      activityService.setCreating(currentNode, true);
      createNodeRecursively(NODE, currentNode, nodeType, mappings);
      createNodeRecursively(NODE, currentNode, nodetypeManager.getNodeType("exo:sortable"), mappings);

      if(mixinTypes != null){
        for(String type : mixinTypes){
          if(!currentNode.isNodeType(type)) {
            currentNode.addMixin(type);
          }
          NodeType mixinType = nodetypeManager.getNodeType(type);
          for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
            String keyJCRPath = (String) iter.next();
            JcrInputProperty jcrInputProperty = (JcrInputProperty) mappings.get(keyJCRPath);
            if (!jcrInputProperty.getJcrPath().equals(NODE)) {
              String inputMixinTypeName = jcrInputProperty.getMixintype();
              String[] inputMixinTypes = null ;
              if(inputMixinTypeName != null && inputMixinTypeName.trim().length() > 0) {
                if(inputMixinTypeName.indexOf(",") > -1){
                  inputMixinTypes = inputMixinTypeName.split(",");
                }else {
                  inputMixinTypes = new String[] {inputMixinTypeName};
                }
              }
              if (inputMixinTypes != null) {
                for(String inputType : inputMixinTypes) {
                  if (inputType.equals(type)) {
                    String childPath = jcrInputProperty.getJcrPath().replaceAll(NODE + "/", "");
                    if (currentNode.hasNode(childPath)) {
                      createNodeRecursively(jcrInputProperty.getJcrPath(), currentNode.getNode(childPath), mixinType, mappings);
                    }
                  }
                }
              }
            }
          }
          createNodeRecursively(NODE, currentNode, mixinType, mappings);
        }
      }
      //all document node should be mix:referenceable that allow retrieve UUID by method Node.getUUID()
      if(!currentNode.isNodeType(MIX_REFERENCEABLE)) {
        currentNode.addMixin(MIX_REFERENCEABLE);
      }      
      //Broadcast CmsService.event.postCreate event
      listenerService.broadcast(POST_CREATE_CONTENT_EVENT,this,currentNode);
    } else {
      currentNode = storeHomeNode.getNode(nodeName);
      activityService.setEditing(currentNode, true);

      //Broadcast CmsService.event.preEdit event
      listenerService.broadcast(PRE_EDIT_CONTENT_EVENT,currentNode,mappings);

      updateNodeRecursively(NODE, currentNode, nodeType, mappings);
      if (currentNode.isNodeType("exo:datetime")) {
        currentNode.setProperty("exo:dateModified", new GregorianCalendar());
      }
      listenerService.broadcast(POST_EDIT_CONTENT_EVENT, this, currentNode);

    }

    activityService.setCreating(currentNode, false);

    //
    if (currentNode.isNodeType(ActivityCommonService.MIX_COMMENT)) {
      currentNode.setProperty(ActivityCommonService.MIX_COMMENT_ACTIVITY_ID, StringUtils.EMPTY);
    }

    storeHomeNode.save();
    return currentNode.getPath();
  }

  /**
   * {@inheritDoc}
   */
  public String storeEditedNode(String nodeTypeName, Node storeNode, Map mappings,
                                boolean isAddNew) throws Exception {
    Set keys = mappings.keySet();
    String nodePath = extractNodeName(keys);
    JcrInputProperty relRootProp = (JcrInputProperty) mappings.get(nodePath);

    String primaryType = relRootProp.getNodetype() ;
    if(primaryType == null || primaryType.length() == 0) {
      primaryType = nodeTypeName ;
    }
    Session session = storeNode.getSession();
    NodeTypeManager nodetypeManager = session.getWorkspace().getNodeTypeManager();
    NodeType nodeType = nodetypeManager.getNodeType(primaryType);

    //Broadcast CmsService.event.preEdit event
    listenerService.broadcast(PRE_EDIT_CONTENT_EVENT,storeNode,mappings);
    updateNodeRecursively(NODE, storeNode, nodeType, mappings);
    if (storeNode.isNodeType("exo:datetime")) {
      storeNode.setProperty("exo:dateModified", new GregorianCalendar());
    }
    listenerService.broadcast(POST_EDIT_CONTENT_EVENT, this, storeNode);
    //add lastModified property to jcr:content
    storeNode.save();
    return storeNode.getPath();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String storeNodeByUUID(String nodeTypeName, Node storeHomeNode, Map mappings,
                                boolean isAddNew) throws Exception {
    Set keys = mappings.keySet();
    String nodePath = extractNodeName(keys);
    JcrInputProperty relRootProp = (JcrInputProperty) mappings.get(nodePath);
    String nodeName = (String)relRootProp.getValue();
    if (nodeName == null || nodeName.length() == 0) {
      nodeName = idGeneratorService.generateStringID(nodeTypeName);
    }
    String primaryType = relRootProp.getNodetype() ;
    if(primaryType == null || primaryType.length() == 0) {
      primaryType = nodeTypeName ;
    }
    Session session = storeHomeNode.getSession();
    NodeTypeManager nodetypeManager = session.getWorkspace().getNodeTypeManager();
    NodeType nodeType = nodetypeManager.getNodeType(primaryType);
    Node currentNode = null;
    String[] mixinTypes = null ;
    String mixintypeName = relRootProp.getMixintype();
    if(mixintypeName != null && mixintypeName.trim().length() > 0) {
      if(mixintypeName.indexOf(",") > -1){
        mixinTypes = mixintypeName.split(",") ;
      }else {
        mixinTypes = new String[] {mixintypeName} ;
      }
    }
    String currentNodePath;
    if (isAddNew) {
      //Broadcast CmsService.event.preCreate event
      listenerService.broadcast(PRE_CREATE_CONTENT_EVENT,storeHomeNode,mappings);
      currentNode = storeHomeNode.addNode(nodeName, primaryType);
      currentNodePath = currentNode.getPath();
      if(mixinTypes != null){
        for(String type : mixinTypes){
          if(!currentNode.isNodeType(type)) {
            currentNode.addMixin(type);
          }
          NodeType mixinType = nodetypeManager.getNodeType(type);
          createNodeRecursively(NODE, currentNode, mixinType, mappings);
        }
      }
      createNodeRecursively(NODE, currentNode, nodeType, mappings);
      if(!currentNode.isNodeType(MIX_REFERENCEABLE)) {
        currentNode.addMixin(MIX_REFERENCEABLE) ;
      }
      //Broadcast CmsService.event.postCreate event
      listenerService.broadcast(POST_CREATE_CONTENT_EVENT,this,currentNode);
    } else {
      currentNode = storeHomeNode.getNode(nodeName);
      currentNodePath = currentNode.getPath();
      updateNodeRecursively(NODE, currentNode, nodeType, mappings);
      if(currentNode.isNodeType("exo:datetime")) {
        currentNode.setProperty("exo:dateModified",new GregorianCalendar()) ;
      }
      listenerService.broadcast(POST_EDIT_CONTENT_EVENT, this, currentNode);
    }
    //check if currentNode has been moved
    if (currentNode instanceof NodeImpl && !((NodeImpl)currentNode).isValid()) {
      currentNode = (Node)session.getItem(currentNodePath);
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
      if (linkManager.isLink(currentNode)) {
        try {
          currentNode = linkManager.getTarget(currentNode, false);
        } catch (Exception ex) {
          currentNode = linkManager.getTarget(currentNode, true);
        }
      }
    }

    String uuid = currentNode.getUUID();
    session.save();
    return uuid;
  }

  /**
   * Call function processNodeRecursively() to update property for specific node and all its children
   * @param path            Path to node
   * @param currentNode     Node is updated
   * @param currentNodeType Node type
   * @param jcrVariables    Mapping key = property name and value
   * @throws Exception
   * @see {@link #processNodeRecursively(boolean, String, Node, NodeType, Map)}
   */
  private void updateNodeRecursively(String path, Node currentNode, NodeType currentNodeType, Map jcrVariables) throws Exception {
    processNodeRecursively(false, path, currentNode, currentNodeType, jcrVariables);
  }

  /**
   * Call function processNodeRecursively() to add property for specific node and all its children
   * @param path            Path to node
   * @param currentNode     Node is updated
   * @param currentNodeType Node type
   * @param jcrVariables    Mapping key = property name and value
   * @throws Exception
   * @see {@link #processNodeRecursively(boolean, String, Node, NodeType, Map)}
   */
  private void createNodeRecursively(String path, Node currentNode,
                                     NodeType currentNodeType, Map jcrVariables) throws Exception {
    processNodeRecursively(true, path, currentNode, currentNodeType,
                           jcrVariables);
  }

  /**
   * Check to set value for a property with definition is autocreated
   * @param propertyDef PropertyDefinition
   * @param path String
   * @param jcrVariables Map of properties
   * @return Boolean
   */
  private boolean isAcceptSetValueForAutoCreated(PropertyDefinition propertyDef, String path,
                                                 Map jcrVariables) {
    if(propertyDef.isAutoCreated()) {
      String propertyName = propertyDef.getName();
      String currentPath = path + "/" + propertyName;
      JcrInputProperty inputVariable = (JcrInputProperty) jcrVariables.get(currentPath) ;
      if(inputVariable != null && inputVariable.getValue() != null) {
        return true;
      }
      return false;
    }
    return true;
  }

  /**
   * Add property for current node when create variable = true
   * Value of property is got in Map jcrVariabes by key = path + / property name
   * @param create          create = true or false
   * @param currentNode     Node is updated
   * @param currentNodeType Node type
   * @param jcrVariables    Mapping key = property name and value
   * @throws Exception
   * @see {@link #processProperty(String, Node, int, Object, boolean)}
   */
  private void processAddEditProperty(boolean create, Node currentNode, String path,
                                      NodeType currentNodeType, Map jcrVariables) throws Exception {
    if(create) {
      PropertyDefinition[] propertyDefs = currentNodeType.getPropertyDefinitions();
      for (int i = 0; i < propertyDefs.length; i++) {
        PropertyDefinition propertyDef = propertyDefs[i];
        if (isAcceptSetValueForAutoCreated(propertyDef, path, jcrVariables) &&
            !propertyDef.isProtected()) {
          String propertyName = propertyDef.getName();
          int requiredtype = propertyDef.getRequiredType();
          String currentPath = path + "/" + propertyName;
          JcrInputProperty inputVariable = (JcrInputProperty) jcrVariables.get(currentPath) ;
          Object value = null;
          if(inputVariable != null) {
            value = inputVariable.getValue();
          }
          if((value != null) || propertyDef.isMandatory()) {
            processProperty(propertyName, currentNode, requiredtype, value, propertyDef.isMultiple());
          }
        }
      }

      //process multiple binary data
      for (Object key : jcrVariables.keySet()) {
        Object value = jcrVariables.get(key);
        if (((JcrInputProperty)value).getValue() instanceof Map) {
          processProperty(key.toString().substring(path.length() + 1), currentNode,
                          PropertyType.BINARY, ((JcrInputProperty)value).getValue(), true);
        }
      }
    }
  }

  /**
   * Process to update or add property for current node and all its property
   * Properties of node is given in current NodeType
   * To add/update property of all node, need check that property is created automatically or protected
   * When property is not created automatically and not protected then update for all child of node
   * @param create          create = true: process adding, create = false, process updating
   * @param itemPath        used with property name as key to get value of one property
   * @param currentNode     Node is updated
   * @param currentNodeType Node type
   * @param jcrVariables    Mapping key = property name and value
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void processNodeRecursively(boolean create, String itemPath,
                                      Node currentNode, NodeType currentNodeType, Map jcrVariables)
                                          throws Exception {
    if(create) {
      processAddEditProperty(true, currentNode, itemPath, currentNodeType, jcrVariables) ;
    } else {
      List<String> keyList = new ArrayList<String>();
      for(Object key : jcrVariables.keySet()) {
        keyList.add(key.toString().substring(key.toString().lastIndexOf("/") + 1));
      }
      List<String> currentListPropertyName = new ArrayList<String>();
      for(PropertyIterator pi = currentNode.getProperties(); pi.hasNext();) {
        Property property = pi.nextProperty();
        currentListPropertyName.add(property.getName());
      }
      Set keys = jcrVariables.keySet();
      String nodePath = extractNodeName(keys);
      JcrInputProperty relRootProp = (JcrInputProperty) jcrVariables.get(nodePath);
      String[] mixinTypes = {};
      String mixintypeName = relRootProp.getMixintype();
      if (mixintypeName != null && mixintypeName.trim().length() > 0) {
        if (mixintypeName.indexOf(",") > -1) {
          mixinTypes = mixintypeName.split(",");
        } else {
          mixinTypes = new String[] { mixintypeName };
        }
      }
      for(String mixinType : mixinTypes) {
        if (!currentNode.isNodeType(mixinType)) {
          if (currentNode.canAddMixin(mixinType)) {
            currentNode.addMixin(mixinType);
          }
        }
      }

      PropertyDefinition[] propertyDefs = currentNodeType.getPropertyDefinitions();
      List<PropertyDefinition> lstPropertyDefinition = Arrays.asList(propertyDefs);
      List<PropertyDefinition> lstPropertyDefinitionAll = new ArrayList<PropertyDefinition>();
      NodeType[] mixinNodeTypes = currentNode.getMixinNodeTypes();
      lstPropertyDefinitionAll.addAll(lstPropertyDefinition);
      for (NodeType mixinNodeType : mixinNodeTypes) {
        Collections.addAll(lstPropertyDefinitionAll, mixinNodeType.getPropertyDefinitions());
      }
      // process property
      for (PropertyDefinition propertyDef: lstPropertyDefinitionAll) {
        String propertyName = propertyDef.getName();
        Object value = null;
        String currentPath = itemPath + "/" + propertyName;
        JcrInputProperty inputVariable = (JcrInputProperty) jcrVariables.get(currentPath) ;
        if(inputVariable != null) {
          value = inputVariable.getValue();
        }
        if (currentListPropertyName.contains(propertyName) && currentNode.hasProperty(propertyName)) {
          Property property = currentNode.getProperty(propertyName);
          int requiredtype = property.getType();
          if(keyList.contains(propertyName)) {
            if(!propertyDef.isProtected()) {
              processProperty(property, currentNode, requiredtype, value, propertyDef.isMultiple());
            }
          }
        } else {
          if (!propertyDef.isProtected()) {
            int requiredtype = propertyDef.getRequiredType();
            if(value != null || propertyDef.isMandatory()) {
              processProperty(propertyName, currentNode, requiredtype, value, propertyDef.isMultiple()) ;
            }
          }
        }
      }

      //process multiple binary data
      for (Object key : jcrVariables.keySet()) {
        Object value = jcrVariables.get(key);
        if (((JcrInputProperty)value).getValue() instanceof Map) {
          processProperty(key.toString().substring(itemPath.length() + 1), currentNode,
                          PropertyType.BINARY, ((JcrInputProperty)value).getValue(), true);
        }
      }
    }

    //process child nodes
    int itemLevel = StringUtils.countMatches(itemPath, "/") ;
    List<JcrInputProperty>childNodeInputs = extractNodeInputs(jcrVariables, itemLevel + 1) ;
    NodeTypeManager nodeTypeManger = currentNode.getSession().getWorkspace().getNodeTypeManager();
    List<Object> childs = new ArrayList<Object>();

    if (currentNodeType.isMixin()) {
      if (create) {
        for (NodeDefinition childNodeDef : currentNodeType.getChildNodeDefinitions()) {
          childs.add(childNodeDef);
        }
      } else {
        for(NodeIterator iterator = currentNode.getNodes(); iterator.hasNext();) {
          childs.add(iterator.nextNode());
        }
      }
    } else {
      Set<String> childNames = new HashSet<String>();

      for (NodeDefinition childNodeDef : currentNodeType.getChildNodeDefinitions()) {
        childs.add(childNodeDef);
        NodeType declaringNodeType = childNodeDef.getDeclaringNodeType();
        NodeType defaultPrimaryType = childNodeDef.getDefaultPrimaryType();
        childNames.add(childNodeDef.getName() +
                       (declaringNodeType == null ? null : declaringNodeType.getName()) +
                       (defaultPrimaryType == null? null : defaultPrimaryType.getName()) );
      }
      if (currentNode != null) {
        for(NodeIterator iterator = currentNode.getNodes(); iterator.hasNext();) {
          NodeDefinition childNodeDef = iterator.nextNode().getDefinition();
          NodeType declaringNodeType = childNodeDef.getDeclaringNodeType();
          NodeType defaultPrimaryType = childNodeDef.getDefaultPrimaryType();

          if (!childNames.contains(childNodeDef.getName() +
                                   (declaringNodeType == null ? null : declaringNodeType.getName()) +
                                   (defaultPrimaryType == null? null : defaultPrimaryType.getName()))) {
            childs.add(childNodeDef);
          }
        }
      }
    }
    Set<String> childItemPaths = new HashSet<String>();
    for(Object obj : childs){
      NodeDefinition nodeDef;
      if (obj instanceof Node) {
        nodeDef = ((Node) obj).getDefinition();
      } else {
        nodeDef = (NodeDefinition) obj;
      }
      if(nodeDef.isAutoCreated() || nodeDef.isProtected()) {
        continue ;
      }
      if(((ItemDefinitionImpl)nodeDef).isResidualSet()) {
        for(JcrInputProperty input:childNodeInputs) {
          String childItemPath = itemPath + "/"+ input.getValue();
          //Only child node input has dependent path of current node is added as child node
          if(!childItemPath.equals(input.getJcrPath())) continue ;
          String primaryNodeType = input.getNodetype();
          NodeType nodeType = nodeTypeManger.getNodeType(primaryNodeType) ;
          if(!canAddNode(nodeDef, nodeType)) {
            continue ;
          }
          String[] mixinTypes = null ;
          if(input.getMixintype()!= null) {
            mixinTypes = input.getMixintype().split(",") ;
          }
          Node childNode = doAddNode(currentNode, (String)input.getValue(), nodeType.getName(), mixinTypes);
          if (childNode != null && !childItemPaths.contains(childItemPath))
            processNodeRecursively(create, childItemPath, childNode, childNode.getPrimaryNodeType(), jcrVariables);
          childItemPaths.add(childItemPath);
        }
      } else {
        String childNodeName = null;
        if (obj instanceof Node) {
          childNodeName = ((Node) obj).getName();
        } else {
          childNodeName = ((NodeDefinition) obj).getName();
        }
        String newItemPath = itemPath + "/" + childNodeName;
        JcrInputProperty jcrInputVariable = (JcrInputProperty) jcrVariables.get(newItemPath);
        if(jcrInputVariable == null) {
          continue ;
        }
        String nodeTypeName = jcrInputVariable.getNodetype();
        String[] mixinTypes = null ;
        if(jcrInputVariable.getMixintype()!= null) {
          mixinTypes = jcrInputVariable.getMixintype().split(",") ;
        }
        NodeType nodeType = null;
        if(obj instanceof Node) {
          nodeType = ((Node) obj).getPrimaryNodeType();
        } else if (nodeTypeName == null || nodeTypeName.length() == 0) {
          nodeType = nodeDef.getRequiredPrimaryTypes()[0];
        } else {
          nodeType = nodeTypeManger.getNodeType(nodeTypeName);
        }
        Node childNode = doAddNode(currentNode, childNodeName, nodeType.getName(), mixinTypes);
        if (!childItemPaths.contains(newItemPath))
          processNodeRecursively(create, newItemPath, childNode, childNode.getPrimaryNodeType(), jcrVariables);
        childItemPaths.add(newItemPath);
      }
    }
  }

  /**
   * Process when add property for node.
   * Base on type of property, needing specific processing
   * @param propertyName    name of property
   * @param node            node to process
   * @param requiredtype    type of property: STRING, BINARY, BOOLEAN, LONG, DOUBLE, DATE, REFERENCE
   * @param value           value of property
   * @param isMultiple      value add is multiple or not
   * @throws Exception
   */

  private void processProperty(String propertyName, Node node, int requiredtype, Object value,
                               boolean isMultiple) throws Exception {

    switch (requiredtype) {
    case PropertyType.STRING:
      if (value == null) {
        if(isMultiple) {
          node.setProperty(propertyName, new String[] {StringUtils.EMPTY});
        } else {
          node.setProperty(propertyName, StringUtils.EMPTY);
        }
      } else {
        if(isMultiple) {
          if (value instanceof String) {
            node.setProperty(propertyName, new String[] { (String)value});
          } else if (value instanceof String[]) {
            node.setProperty(propertyName, (String[]) value);
          }
        } else {
          if(!node.hasProperty(propertyName) || (node.hasProperty(propertyName) && 
              !node.getProperty(propertyName).getString().equals((String)value)))
            node.setProperty(propertyName, (String) value);
        }
      }
      break;
    case PropertyType.BINARY:
      if (isMultiple) {
        Node storedNode = node.hasNode(propertyName) ? node.getNode(propertyName) :
          node.addNode(propertyName, NodetypeConstant.NT_UNSTRUCTURED);
        if(value instanceof Map) {
          for (Map.Entry<String, List> entry : ((Map<String, List>)value).entrySet()) {
            String fileName = entry.getKey();
            List param = entry.getValue();
            Node fileNode = null;
            Node jcrContentNode = null;
            if (!storedNode.hasNode(fileName)) {
              fileNode = storedNode.addNode(fileName, NodetypeConstant.NT_FILE);
              jcrContentNode = fileNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
              jcrContentNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, (String)param.get(0));
              jcrContentNode.setProperty(NodetypeConstant.JCR_DATA, new ByteArrayInputStream((byte[])param.get(1)));
            } else {
              jcrContentNode = storedNode.getNode(fileName).getNode(NodetypeConstant.JCR_CONTENT);
            }
            jcrContentNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
          }
        }
      } else {
        if (value == null) {
          node.setProperty(propertyName, "");
        } else if(value instanceof InputStream) {
          node.setProperty(propertyName, (InputStream)value);
        } else if (value instanceof byte[]) {
          node.setProperty(propertyName,
                           new ByteArrayInputStream((byte[]) value));
        } else if (value instanceof String) {
          node.setProperty(propertyName, value.toString(), PropertyType.BINARY);
        } else if (value instanceof String[]) {
          node.setProperty(propertyName, (String[]) value, PropertyType.BINARY);
        }
      }
      break;
    case PropertyType.BOOLEAN:
      if (value == null) {
        node.setProperty(propertyName, false);
      } else if (value instanceof String) {
        node.setProperty(propertyName,
                         new Boolean((String) value).booleanValue());
      } else if (value instanceof String[]) {
        node.setProperty(propertyName, (String[]) value);
      } else if (value instanceof Boolean) {
        node.setProperty(propertyName, ((Boolean) value).booleanValue());
      }
      break;
    case PropertyType.LONG:
      if (value == null || "".equals(value)) {
        node.setProperty(propertyName, 0);
      } else if (value instanceof String) {
        node.setProperty(propertyName, new Long((String) value).longValue());
      } else if (value instanceof String[]) {
        node.setProperty(propertyName, (String[]) value);
      } else if (value instanceof Long) {
        node.setProperty(propertyName, ((Long) value).longValue());
      }
      break;
    case PropertyType.DOUBLE:
      if (value == null || "".equals(value)) {
        node.setProperty(propertyName, 0);
      } else if (value instanceof String) {
        node.setProperty(propertyName, new Double((String) value).doubleValue());
      } else if (value instanceof String[]) {
        node.setProperty(propertyName, (String[]) value);
      } else if (value instanceof Double) {
        node.setProperty(propertyName, ((Double) value).doubleValue());
      }

      break;
    case PropertyType.DATE:
      if (value == null){
        boolean mandatory = false;
        for (PropertyDefinition propertyDef : node.getPrimaryNodeType().getPropertyDefinitions())
          if (propertyName.equals(propertyDef.getName()) && propertyDef.isMandatory()) {
            mandatory = true;
            break;
          }
        for (NodeType mixin : node.getMixinNodeTypes()) {
          for (PropertyDefinition propertyDef : mixin.getPropertyDefinitions()) {
            if (propertyName.equals(propertyDef.getName()) && propertyDef.isMandatory()) {
              mandatory = true;
              break;
            }
          }
        }
        if (mandatory) {
          node.setProperty(propertyName, new GregorianCalendar());
        } else {
          node.setProperty(propertyName, (Value)null);
        }
      } else {
        if (isMultiple) {
          Session session = node.getSession();
          if (value instanceof String) {
            Value value2add = session.getValueFactory().createValue(ISO8601.parse((String) value));
            node.setProperty(propertyName, new Value[] {value2add});
          } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            Value[] convertedCalendarValues = new Value[values.length];
            int i = 0;
            for (String stringValue : values) {
              Value value2add = session.getValueFactory().createValue(ISO8601.parse(stringValue));
              convertedCalendarValues[i] = value2add;
              i++;
            }
            node.setProperty(propertyName, convertedCalendarValues);
          }else if (value instanceof GregorianCalendar){
            Value value2add = session.getValueFactory().createValue((GregorianCalendar) value);
            node.setProperty(propertyName, new Value[]{value2add});
          }else if (value instanceof GregorianCalendar[]){
            GregorianCalendar[] values = (GregorianCalendar[]) value;
            Value[] convertedCalendarValues = new Value[values.length];
            int i = 0;
            for (GregorianCalendar cvalue : values) {
              Value value2add = session.getValueFactory().createValue(cvalue);
              convertedCalendarValues[i] = value2add;
              i++;
            }
            node.setProperty(propertyName, convertedCalendarValues);
          }
        } else {
          if (value instanceof String) {
            node.setProperty(propertyName, ISO8601.parse((String)value));
          } else if (value instanceof GregorianCalendar) {
            node.setProperty(propertyName, (GregorianCalendar) value);
          }

        }
      }
      break;
    case PropertyType.REFERENCE:
      if (value == null) {
        if (isMultiple) {
          if (value instanceof String) {
            node.setProperty(propertyName, "");
          } else if (value instanceof String[]) {
            node.setProperty(propertyName, new String[] {});
          }
        } else {
          node.setProperty(propertyName, "");
        }
      }
      if (value instanceof Value[]) {
        node.setProperty(propertyName, (Value[]) value);
      } else if (value instanceof String) {
        String referenceWorksapce = null;
        String referenceNodeName = null ;
        Session session = node.getSession();
        if (((String) value).indexOf(":/") > -1) {
          referenceWorksapce = ((String) value).split(":/")[0];
          referenceNodeName = ((String) value).split(":/")[1];
          Session session2 = jcrService.getCurrentRepository().getSystemSession(referenceWorksapce);
          if (session2.getRootNode().hasNode(referenceNodeName)) {
            Node referenceNode = session2.getRootNode().getNode(referenceNodeName);
            if (referenceNode != null) {
              if(!referenceNode.isNodeType(MIX_REFERENCEABLE)) {
                referenceNode.addMixin(MIX_REFERENCEABLE);
                referenceNode.save();
              }
              Value value2add = session2.getValueFactory().createValue(referenceNode);
              node.setProperty(propertyName, value2add);
            }
          } else {
            node.setProperty(propertyName, session2.getValueFactory().createValue((String)value));
          }
          session2.logout();
        } else {
          Node referenceNode = null;
          try {
            referenceNode = (Node) session.getItem((String) value);
          } catch (PathNotFoundException e) {
            referenceNode = session.getRootNode().getNode(value.toString());
          }
          if (referenceNode != null) {
            if(!referenceNode.isNodeType(MIX_REFERENCEABLE)) {
              referenceNode.addMixin(MIX_REFERENCEABLE);
              referenceNode.save();
            }
            Value value2add = session.getValueFactory().createValue(referenceNode);
            node.setProperty(propertyName, value2add);
          } else {
            node.setProperty(propertyName, session.getValueFactory().createValue(value.toString()));
          }
        }
        session.save();
      } else if(value instanceof String[]) {
        String[] values = (String[]) value;
        String referenceWorksapce = null;
        String referenceNodeName = null ;
        Session session = node.getSession();
        List<Value> list = new ArrayList<Value>() ;
        for (String v : values) {
          Value valueObj = null;
          if (v.indexOf(":/") > 0) {
            referenceWorksapce = v.split(":/")[0];
            referenceNodeName = v.split(":/")[1];
            Session session2 = jcrService.getCurrentRepository().getSystemSession(referenceWorksapce);
            if(session2.getRootNode().hasNode(referenceNodeName)) {
              Node referenceNode = session2.getRootNode().getNode(referenceNodeName);
              valueObj = session2.getValueFactory().createValue(referenceNode);
            }else {
              valueObj = session2.getValueFactory().createValue(v);
            }
            session2.logout();
          }else {
            if(session.getRootNode().hasNode(v)) {
              Node referenceNode = session.getRootNode().getNode(v);
              valueObj = session.getValueFactory().createValue(referenceNode);
            }else {
              valueObj = session.getValueFactory().createValue(v);
            }
          }
          list.add(valueObj);
        }
        node.setProperty(propertyName,list.toArray(new Value[list.size()]));
      }
      break;
    default:
      throw new RepositoryException("unknown type " + requiredtype);
    }
  }

  /**
   * Process when update property for node.
   * Base on type of property, needing specific processing
   * @param property        Property of node
   * @param node            node to process
   * @param requiredtype    type of property: STRING, BINARY, BOOLEAN, LONG, DOUBLE, DATE, REFERENCE
   * @param value           value of property
   * @param isMultiple      value add is multiple or not
   * @throws Exception
   */
  private void processProperty(Property property, Node node, int requiredtype,
                               Object value, boolean isMultiple) throws Exception {
    String nodeUUID = "";
    if(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) nodeUUID = node.getUUID();
    else nodeUUID = node.getName();
    String propertyName = property.getName() ;
    String updatedProperty = nodeUUID + "_" + propertyName;
    if(isMultiple) properties.put(updatedProperty, property.getValues());
    else properties.put(updatedProperty, property.getValue());
    switch (requiredtype) {
    case PropertyType.STRING:
      if (value == null) {
        if(isMultiple) {
          node.setProperty(propertyName, new String[] {StringUtils.EMPTY});          
        } else {
          if(property.getValue() != null && !property.getValue().getString().equals(StringUtils.EMPTY)) {
            node.setProperty(propertyName, StringUtils.EMPTY);        		
          }            
        }        
      } else {
        if(isMultiple) {
          if (value instanceof String) { 
            if(!property.getValues().equals(value)) {
              node.setProperty(propertyName, new String[] { (String)value});              
            }
          } else if (value instanceof String[]) {
            if(!isEqualsValueStringArrays(property.getValues(), (String[]) value)) {
              node.setProperty(propertyName, (String[]) value);
            }
          }
        } else {
          if(!property.getValue().getString().equals(value)) {
            node.setProperty(propertyName, (String) value);            
          }
        }
      }
      break;
    case PropertyType.BINARY:
      if (isMultiple) {
        Node storedNode = node.hasNode(propertyName) ? node.getNode(propertyName) : node.addNode(propertyName);
        if(value instanceof Map) {
          for (Map.Entry<String, List> entry : ((Map<String, List>)value).entrySet()) {
            String fileName = entry.getKey();
            List param = entry.getValue();
            Node fileNode = null;
            Node jcrContentNode = null;
            if (!storedNode.hasNode(fileName)) {

              fileNode = storedNode.addNode(fileName, NodetypeConstant.NT_FILE);
              jcrContentNode = fileNode.addNode(NodetypeConstant.JCR_CONTENT);
              jcrContentNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, (String)param.get(0));
              jcrContentNode.setProperty(NodetypeConstant.JCR_DATA, new ByteArrayInputStream((byte[])param.get(1)));              
            } else {
              jcrContentNode = storedNode.getNode(fileName).getNode(NodetypeConstant.JCR_CONTENT);
            }
            jcrContentNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
          }
        }
      } else {
        if (value == null ) {
          if(node.getProperty(propertyName) != null && !node.getProperty(propertyName).getString().equals("")) {
            node.setProperty(propertyName, "");            
          }
        } else if(value instanceof InputStream) {
          if(!property.getValue().getStream().equals(value)) {
            node.setProperty(propertyName, (InputStream)value);
          }
        } else if (value instanceof byte[]) {
          if(!property.getValue().getStream().equals(new ByteArrayInputStream((byte[]) value))) {
            node.setProperty(propertyName, new ByteArrayInputStream((byte[]) value));
          }
        } else if (value instanceof String) {
          if(!property.getValue().getString().equals(value)) {
            node.setProperty(propertyName, value.toString(), PropertyType.BINARY);
          }
        }
      }
      break;
    case PropertyType.BOOLEAN:
      if (value == null) {
        node.setProperty(propertyName, false);
      } else if (value instanceof Boolean) {
        node.setProperty(propertyName, ((Boolean) value).booleanValue());
      }else if (value instanceof String) {
        if(property.getValue().getBoolean() != new Boolean((String) value).booleanValue()) {
          node.setProperty(propertyName, new Boolean((String) value).booleanValue());
        }
      } else if (value instanceof String[]) {
        if(!checkEqual(property.getValues(), (String[])value)) {
          node.setProperty(propertyName, (String[]) value);
        }
      }
      break;
    case PropertyType.LONG:
      if (value == null || "".equals(value)) {
        node.setProperty(propertyName, 0);
      } else if (value instanceof String) {
        if(property.getValue().getLong() != new Long((String) value).longValue()) {
          node.setProperty(propertyName, new Long((String) value).longValue());
        }
      } else if (value instanceof String[]) {
        if(!checkEqual(property.getValues(), (String[])value)) {
          node.setProperty(propertyName, (String[]) value);
        }
      } else if (value instanceof Long) {
        node.setProperty(propertyName, ((Long) value).longValue());
      }
      break;
    case PropertyType.DOUBLE:
      if (value == null || "".equals(value)) {
        node.setProperty(propertyName, 0);
      } else if (value instanceof String) {
        if(property.getValue().getDouble() != new Double((String) value).doubleValue()) {
          node.setProperty(propertyName, new Double((String) value).doubleValue());
        }
      } else if (value instanceof String[]) {
        if(!checkEqual(property.getValues(), (String[])value)) {
          node.setProperty(propertyName, (String[]) value);
        }
      } else if (value instanceof Double) {
        node.setProperty(propertyName, ((Double) value).doubleValue());
      }
      break;
    case PropertyType.DATE:
      if (value == null){
        boolean mandatory = false;
        for (PropertyDefinition propertyDef : node.getPrimaryNodeType().getPropertyDefinitions())
          if (propertyName.equals(propertyDef.getName()) && propertyDef.isMandatory()) {
            mandatory = true;
            break;
          }
        for (NodeType mixin : node.getMixinNodeTypes()) {
          for (PropertyDefinition propertyDef : mixin.getPropertyDefinitions()) {
            if (propertyName.equals(propertyDef.getName()) && propertyDef.isMandatory()) {
              mandatory = true;
              break;
            }
          }
        }
        if (mandatory) {
          node.setProperty(propertyName, new GregorianCalendar());
        } else {
          if(isMultiple)
            node.setProperty(propertyName, (Value[])null);
          else
            node.setProperty(propertyName, (Value)null);
        }
      } else {
        if(isMultiple) {
          Session session = node.getSession();
          if (value instanceof String) {
            Value value2add = session.getValueFactory().createValue(ISO8601.parse((String) value));
            if(!property.getValues().equals(new Value[] {value2add})) {
              node.setProperty(propertyName, new Value[] {value2add});
            }
          } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            Value[] convertedCalendarValues = new Value[values.length];
            int i = 0;
            for (String stringValue : values) {
              Value value2add = session.getValueFactory().createValue(ISO8601.parse(stringValue));
              convertedCalendarValues[i] = value2add;
              i++;
            }
            if(!property.getValues().equals(convertedCalendarValues)) {
              node.setProperty(propertyName, convertedCalendarValues);
            }
          }else if (value instanceof GregorianCalendar){
            Value value2add = session.getValueFactory().createValue((GregorianCalendar)value);
            if(!property.getValues().equals(new Value[]{value2add})) {
              node.setProperty(propertyName, new Value[]{value2add});
            }
          }else if (value instanceof GregorianCalendar[]){
            GregorianCalendar[] values = (GregorianCalendar[]) value;
            Value[] convertedCalendarValues = new Value[values.length];
            int i = 0;
            for (GregorianCalendar cvalue : values) {
              Value value2add = session.getValueFactory().createValue(cvalue);
              convertedCalendarValues[i] = value2add;
              i++;
            }
            if(!property.getValues().equals(convertedCalendarValues)) {
              node.setProperty(propertyName, convertedCalendarValues);
            }
          }
        } else {
          if (value instanceof String) {
            if(!property.getValue().getString().equals(ISO8601.parse((String)value))) {
              node.setProperty(propertyName, ISO8601.parse((String)value));
            }
          } else if (value instanceof GregorianCalendar) {
            if(!property.getValue().getDate().equals(value)) {
              node.setProperty(propertyName, (GregorianCalendar) value);
            }
          }
        }
      }
      break;
    case PropertyType.REFERENCE:
      if (value == null) {
        if (isMultiple) {
          if (value instanceof String) {
            node.setProperty(propertyName, "");
          } else if (value instanceof String[]) {
            node.setProperty(propertyName, new String[] {});
          }
        } else {
          node.setProperty(propertyName, "");
        }
      }
      if (value instanceof Value[]) {
        if(!property.getValues().equals(value)) {
          node.setProperty(propertyName, (Value[]) value);
        }
      } else if (value instanceof String) {
        String referenceWorksapce = null;
        String referenceNodeName = null ;
        Session session = node.getSession();
        if (((String) value).indexOf(":/") > -1) {
          referenceWorksapce = ((String) value).split(":/")[0];
          referenceNodeName = ((String) value).split(":/")[1];
          Session session2 = jcrService.getCurrentRepository().getSystemSession(referenceWorksapce);
          if(session2.getRootNode().hasNode(referenceNodeName)) {
            Node referenceNode = session2.getRootNode().getNode(referenceNodeName);
            Value value2add = session2.getValueFactory().createValue(referenceNode);
            if(!property.getValue().getString().equals(value2add)) {
              node.setProperty(propertyName, value2add);
            }
          }else {
            if(!property.getValue().getString().equals(session2.getValueFactory().createValue((String)value))) {
              node.setProperty(propertyName, session2.getValueFactory().createValue((String)value));
            }
          }
          session2.logout();
        } else {
          Node referenceNode = null;
          try {
            referenceNode = (Node) session.getItem((String) value);
          } catch (PathNotFoundException e) {
            referenceNode = session.getRootNode().getNode(value.toString());
          }
          if (referenceNode != null) {
            if(!referenceNode.isNodeType(MIX_REFERENCEABLE)) {
              referenceNode.addMixin(MIX_REFERENCEABLE);
              referenceNode.save();
            }
            Value value2add = session.getValueFactory().createValue(referenceNode);
            if(!property.getValue().getString().equals(value2add)) {
              node.setProperty(propertyName, value2add);
            }
          } else {
            if(!property.getValue().getString().equals(session.getValueFactory().createValue(value.toString()))) {
              node.setProperty(propertyName, session.getValueFactory().createValue(value.toString()));
            }
          }
        }
        session.save();
      } else if(value instanceof String[]) {
        String[] values = (String[]) value;
        String referenceWorksapce = null;
        String referenceNodeName = null ;
        Session session = node.getSession();
        List<Value> list = new ArrayList<Value>() ;
        for (String v : values) {
          Value valueObj = null;
          if (v.indexOf(":/") > 0) {
            referenceWorksapce = v.split(":/")[0];
            referenceNodeName = v.split(":/")[1];
            Session session2 = jcrService.getCurrentRepository().getSystemSession(referenceWorksapce);
            if(session2.getRootNode().hasNode(referenceNodeName)) {
              Node referenceNode = session2.getRootNode().getNode(referenceNodeName);
              valueObj = session2.getValueFactory().createValue(referenceNode);
            }else {
              valueObj = session2.getValueFactory().createValue(v);
            }
            session2.logout();
          }else {
            if(session.getRootNode().hasNode(v)) {
              Node referenceNode = session.getRootNode().getNode(v);
              valueObj = session.getValueFactory().createValue(referenceNode);
            }else {
              valueObj = session.getValueFactory().createValue(v);
            }
          }
          list.add(valueObj) ;
        }
        if (!property.getValues().equals(list.toArray(new Value[list.size()]))) {
          node.setProperty(propertyName, list.toArray(new Value[list.size()]));
        }
        session.save();
      }
      break ;
    default:
      throw new RepositoryException("unknown type " + requiredtype);
    }
  }

  /**
   * Get node name from set
   * In set, node name ends with NODE
   * @param keys
   * @return
   */
  private String extractNodeName(Set keys) {
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      String key = (String) iter.next();
      if (key.endsWith(NODE)) {
        return key;
      }
    }
    return null;
  }

  public boolean isEqualsValueStringArrays(Value[] arrayValue1, String[] arrayValue2) throws ValueFormatException, 
  IllegalStateException, RepositoryException {
    if(arrayValue1 != null) {
      String[] stringArray = new String[arrayValue1.length];
      int i = 0;
      for (Value valueItem : arrayValue1) {  	  	
        if(valueItem != null && valueItem.getString() != null)
          stringArray[i] = valueItem.getString();
        i++;
      }
      if(stringArray != null && stringArray.length > 0)
        Arrays.sort(stringArray);
      if(arrayValue2 != null && arrayValue2.length > 0)
        Arrays.sort(arrayValue2);
      return ArrayUtils.isEquals(stringArray, arrayValue2);  	    
    } else {
      if(arrayValue2 != null) return false;
      else return true;
    }	
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moveNode(String nodePath, String srcWorkspace, String destWorkspace, String destPath) {
    Session srcSession = null ;
    Session destSession = null ;
    if (!srcWorkspace.equals(destWorkspace)) {
      try {
        srcSession = jcrService.getCurrentRepository().getSystemSession(srcWorkspace);
        destSession = jcrService.getCurrentRepository().getSystemSession(destWorkspace);
        Workspace workspace = destSession.getWorkspace();
        Node srcNode = (Node) srcSession.getItem(nodePath);
        try {
          destSession.getItem(destPath);
        } catch (PathNotFoundException e) {
          createNode(destSession, destPath);
        }
        workspace.clone(srcWorkspace, nodePath, destPath, true);
        try {
          if (activityService==null) {
            activityService = WCMCoreUtils.getService(ActivityCommonService.class);
          }
          if (listenerService!=null && activityService.isAcceptedNode(srcNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, srcNode, destPath);
          }
        }catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not notify NodeMovedActivity: " + e.getMessage());
          }
        }
        //Remove src node
        srcNode.remove();
        srcSession.save();
        destSession.save() ;
        srcSession.logout();
        destSession.logout();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      } finally {
        if(srcSession != null) srcSession.logout();
        if(destSession !=null) destSession.logout();
      }
    } else {
      Session session = null ;
      try{
        session = jcrService.getCurrentRepository().getSystemSession(srcWorkspace);
        Workspace workspace = session.getWorkspace();
        try {
          session.getItem(destPath);
        } catch (PathNotFoundException e) {
          createNode(session, destPath);
          session.refresh(false) ;
        }

        workspace.move(nodePath, destPath);
        try {
          Node movedNode =(Node) session.getItem(destPath);
          if (activityService==null) {
            activityService = WCMCoreUtils.getService(ActivityCommonService.class);
          }
          if (listenerService!=null && activityService.isAcceptedNode(movedNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_MOVED_ACTIVITY, movedNode, destPath);
          }
        }catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not notify NodeMovedActivity: " + e.getMessage());
          }
        }
        session.logout();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      } finally {
        if (session != null && session.isLive())
          session.logout();
      }
    }
  }

  /**
   * Create node following path in uri
   * @param session Session
   * @param uri     path to created node
   * @throws RepositoryException
   */
  private void createNode(Session session, String uri) throws RepositoryException {
    String[] splittedName = StringUtils.split(uri, "/");
    Node rootNode = session.getRootNode();
    for (int i = 0; i < splittedName.length - 1; i++) {
      try {
        rootNode.getNode(splittedName[i]);
      } catch (PathNotFoundException exc) {
        rootNode.addNode(splittedName[i], "nt:unstructured");
        rootNode.save() ;
      }
      rootNode = rootNode.getNode(splittedName[i]) ;
    }
    session.save() ;
  }

  /**
   * Get all value in Map.
   * Base on key, iterate each key to get value in map
   * @param map       Map of key and value of property
   * @param itemLevel level of child of specific node
   * @return
   * @see {@link #processNodeRecursively(boolean, String, Node, NodeType, Map)}
   */
  private List<JcrInputProperty> extractNodeInputs(Map<String, JcrInputProperty> map, int itemLevel) {
    List<JcrInputProperty> list = new ArrayList<JcrInputProperty>() ;
    for(Iterator<String> iterator = map.keySet().iterator();iterator.hasNext();) {
      String jcrPath = iterator.next();
      if(itemLevel == StringUtils.countMatches(jcrPath, "/")) {
        JcrInputProperty input = map.get(jcrPath) ;
        if(input.getType() == JcrInputProperty.NODE) {
          list.add(input) ;
        }
      }
    }
    return list ;
  }

  /**
   * Check whether node type can add property in NodeDefinition
   * @param nodeDef   NodeDefinition
   * @param nodeType  NodeType
   * @return  true: can add property to node
   *          false: can't add
   */
  private boolean canAddNode(NodeDefinition nodeDef, NodeType nodeType) {
    for(NodeType type: nodeDef.getRequiredPrimaryTypes()) {
      if(nodeType.isNodeType(type.getName())) {
        return true ;
      }
    }
    return false ;
  }

  /**
   * Add child node for current node
   * @param currentNode current node
   * @param nodeName    name of child node
   * @param nodeType    nodetype of child node
   * @param mixinTypes  array of mixin type
   * @return child node
   * @throws Exception
   */
  private Node doAddNode(Node currentNode, String nodeName, String nodeType, String[] mixinTypes) throws Exception {
    if (StringUtils.isEmpty(nodeName))
      return null;
    Node childNode = null;
    try {
      childNode = currentNode.getNode(nodeName);
    } catch(PathNotFoundException pe) {
      childNode = currentNode.addNode(nodeName, nodeType);
    }
    if (mixinTypes != null && mixinTypes.length > 0) {
      for (String mixinName : mixinTypes) {
        if(!childNode.isNodeType(mixinName)) {
          childNode.addMixin(mixinName);
        }
      }
    }
    return childNode ;
  }


  /**
   * Compare if content of Value array equal to String array
   *
   * @param arrValues Value array
   * @param arrStrings String array
   * @return true: equal, false: not equal
   * @throws ValueFormatException
   * @throws IllegalStateException
   * @throws RepositoryException
   */
  private boolean checkEqual(Value[] arrValues, String[] arrStrings)
      throws ValueFormatException, IllegalStateException, RepositoryException
      {
    Validate.isTrue(arrValues != null, "arrValues must not null");
    Validate.isTrue(arrStrings != null, "arrStrings must not null");

    int arrValuesLenth = arrValues.length;
    boolean isEqual = true;

    if (arrValuesLenth != arrStrings.length)
    {
      isEqual = false;
    }
    else
    {
      for (int i = 0; i < arrValuesLenth; i++ )
      {
        if (!arrValues[i].getString().equals(arrStrings[i]))
        {
          isEqual = false;
          break;
        }
      }
    }

    return isEqual;
      }
}
