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
package org.exoplatform.services.cms.actions.impl;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionPlugin;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.actions.DMSEvent;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Process with action for node
 * @author exo
 *
 */
public class ActionServiceContainerImpl implements ActionServiceContainer, Startable {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(ActionServiceContainerImpl.class.getName());

  /**
   * Define nodetype ACTIONABLE
   */
  private static final String         ACTIONABLE           = "exo:actionable";

  /**
   * Define nodetype ACTION
   */
  private static final String         ACTION               = "exo:action";

  /**
   * Define nodetype JOB_NAME_PROP
   */
  private static final String         JOB_NAME_PROP        = "exo:jobName";

  /**
   * Define nodetype JOB_NAME_PROP
   */
  private static final String         JOB_GROUP_PROP       = "exo:jobGroup";

  /**
   * Define nodetype JOB_CLASS_PROP
   */
  private static final String         JOB_CLASS_PROP       = "exo:jobClass";

  /**
   * Define nodetype LIFECYCLE_PHASE_PROP
   */
  private static final String         LIFECYCLE_PHASE_PROP = "exo:lifecyclePhase" ;

  /**
   * Define query statement
   */
  private static final String         ACTION_QUERY         = "//element(*, exo:action)" ;

  /**
   * Define sql query statement
   */
  private static final String ACTION_SQL_QUERY =
                "select * from exo:action" ;

  /**
   * Define sql append query operator.
   */
  private static final String WHERE_OPERATOR = " where" ;

  /**
   * Define sql path.
   */
  private static final String JCR_PATH = " jcr:path" ;

  /**
   * Define sql like operator.
   */
  private static final String LIKE_OPERATOR = " like" ;

  /**
   * Sql query single quote.
   */
  private static final String SINGLE_QUOTE = "'";

  /**
   * Define nodetype SCHEDULABLE_MIXIN
   */
  private static final String         SCHEDULABLE_MIXIN    = "exo:schedulableInfo";

  /**
   * Define relative path for action node
   */
  private static final String         EXO_ACTIONS          = "exo:actions";

  /**
   * Define nodetype ACTION_STORAGE
   */
  private static final String         ACTION_STORAGE       = "exo:actionStorage";

  /**
   * Define nodetype EXO_HIDDENABLE
   */
  private static final String         EXO_HIDDENABLE       = "exo:hiddenable";

  /**
   * RepositoryService
   */
  private RepositoryService           repositoryService_;

  /**
   * CmsService
   */
  private CmsService                  cmsService_;

  /**
   * Collection of ComponentPlugin
   */
  private Collection<ComponentPlugin> actionPlugins        = new ArrayList<ComponentPlugin>();

  /**
   * Constructor method
   * @param repositoryService RepositoryService
   * @param cmsService        CmsService
   * @throws Exception
   */
  public ActionServiceContainerImpl(RepositoryService repositoryService, CmsService cmsService
      ) throws Exception {
    repositoryService_ = repositoryService;
    cmsService_ = cmsService;
  }

  /**
   * Implement method start service
   * Add mixin exo:actionable for node
   */
  public void start() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getSimpleName()+ "...");
    }
    try {
      for (ComponentPlugin cPlungin : actionPlugins) {
        BaseActionPlugin plugin = (BaseActionPlugin) cPlungin;
        plugin.importPredefinedActionsInJcr();
      }
      initiateActionConfiguration();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot start ActionServiceContainerImpl", e);
      }
    }
  }

  /**
   * Implement method stop service
   */
  public void stop() {
  }

  /**
   * {@inheritDoc}
   */
  public void init() {
    try {
      for (ComponentPlugin cPlungin : actionPlugins) {
        BaseActionPlugin plugin = (BaseActionPlugin) cPlungin;
        plugin.reImportPredefinedActionsInJcr();
      }
      reInitiateActionConfiguration();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot initialize the ActionServiceContainerImpl", e);
      }
    }
  }  

  /**
   * {@inheritDoc}
   */  
  public Collection<String> getActionPluginNames() {
    Collection<String> actionPluginNames = new ArrayList<String>(actionPlugins.size());
    for (ComponentPlugin plugin : actionPlugins) {
      actionPluginNames.add(plugin.getName());
    }
    return actionPluginNames;
  }

  /**
   * {@inheritDoc}
   */  
  public ActionPlugin getActionPlugin(String actionsServiceName) {
    for (ComponentPlugin plugin : actionPlugins) {
      if (plugin.getName().equals(actionsServiceName))
        return (ActionPlugin) plugin;
    }
    return null;
  }

  /**
   * Create NodeTypeValue is in kind of ActionType following action type name
   * @param actionTypeName        name of action type
   * @param parentActionTypeName  name of parent action
   * @param executable            String value of executable
   * @param variableNames         List name of variable
   * @param isMoveType            is moved or not
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void createActionType(String actionTypeName, String parentActionTypeName, String executable, String actionLabel,
      List<String> variableNames, boolean isMoveType, boolean isUpdate) throws Exception {
    NodeTypeValue nodeTypeValue = new NodeTypeValue();
    nodeTypeValue.setName(actionTypeName);

    List<String> superTypes = new ArrayList<String>();
    superTypes.add(parentActionTypeName);
    if (isMoveType)
      superTypes.add("exo:move");
    nodeTypeValue.setDeclaredSupertypeNames(superTypes);

    List propDefs = new ArrayList();
    PropertyDefinitionValue propDef = null;
    for (String variableName : variableNames) {
      propDef = createPropertyDef(variableName);
      propDefs.add(propDef);
    }
    propDef = createPropertyDef(getActionPluginForActionType(parentActionTypeName).getExecutableDefinitionName());
    List scriptDefaultValues = new ArrayList();
    scriptDefaultValues.add(executable);
    propDef.setDefaultValueStrings(scriptDefaultValues);
    propDef.setMandatory(true);
    propDefs.add(propDef);
    propDef = createPropertyDef(getActionPluginForActionType(parentActionTypeName).getActionExecutableLabel());
    List labelDefaultValues = new ArrayList();
    labelDefaultValues.add(actionLabel);
    propDef.setDefaultValueStrings(labelDefaultValues);
    propDef.setMandatory(true);
    propDefs.add(propDef);

    nodeTypeValue.setDeclaredPropertyDefinitionValues(propDefs);
    nodeTypeValue.setDeclaredChildNodeDefinitionValues(new ArrayList());
    ExtendedNodeTypeManager ntmanager = repositoryService_.getCurrentRepository().getNodeTypeManager();
    if(isUpdate) ntmanager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
    ntmanager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
  }

  /**
   * Definite new property with property name = name
   * mandatory = false, multiple = false, readonly = false, autocreate = false, onversion = 1
   * type = STRING, default value = new ArrayList(), Value constraints = new ArrayList()
   * @param name name of property
   * @return PropertyDefinitionValue
   */
  @SuppressWarnings("unchecked")
  private PropertyDefinitionValue createPropertyDef(String name) {
    PropertyDefinitionValue def = new PropertyDefinitionValue();
    def.setName(name);
    def.setRequiredType(PropertyType.STRING);
    def.setMandatory(false);
    def.setMultiple(false);
    def.setReadOnly(false);
    def.setAutoCreate(false);
    def.setOnVersion(OnParentVersionAction.COPY);
    def.setValueConstraints(new ArrayList());
    def.setDefaultValueStrings(new ArrayList());
    return def;
  }

  /**
   * Get all created node with nodetype = "exo:action
   * @param repository  repository name
   * @return Collection of NodeType
   * @throws Exception
   */
  public Collection<NodeType> getCreatedActionTypes(String repository) throws Exception {
    Collection<NodeType> createsActions = new ArrayList<NodeType>();
    NodeTypeManager ntmanager = repositoryService_.getCurrentRepository().getNodeTypeManager();
    for(NodeTypeIterator iter = ntmanager.getAllNodeTypes();iter.hasNext();) {
      NodeType nt = (NodeType) iter.next();
      String name = nt.getName();
      if (nt.isNodeType(ACTION) && !isAbstractType(name) &&
        !Utils.getAllEditedConfiguredData("ActionTypeList", "EditedConfiguredActionType", true).contains(name)) {
        createsActions.add(nt);
      }
    }
    return createsActions;
  }

  /**
   * Check ComponentPlugin is abstract type or not
   * @param name  name of ComponentPlugin
   * @return true: ComponentPlugin with name is abstract type
   *         false: ComponentPlugin with name is not abstract type
   */
  private boolean isAbstractType(String name) {
    for (ComponentPlugin plugin : actionPlugins) {
      if (plugin.getName().equals(name))
        return true;
    }
    return false;
  }

  /**
   * Get SystemSession of specific workspace and repository
   * @param workspace
   * @return
   * @throws RepositoryException
   * @throws RepositoryConfigurationException
   */
  private Session getSystemSession(String workspace) throws RepositoryException,
  RepositoryConfigurationException {
    ManageableRepository jcrRepository = repositoryService_.getCurrentRepository();
    return jcrRepository.getSystemSession(workspace);
  }

  /**
   * {@inheritDoc}
   */  
  public ActionPlugin getActionPluginForActionType(String actionTypeName) {
    for (ComponentPlugin plugin : actionPlugins) {
      String actionServiceName = plugin.getName();
      ActionPlugin actionService = getActionPlugin(actionServiceName);
      if (actionService.isActionTypeSupported(actionTypeName)
          || actionServiceName.equals(actionTypeName))
        return actionService;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */  
  public Node getAction(Node node, String actionName) throws Exception {
    if (node.hasNode(EXO_ACTIONS + "/"+actionName)) {
      return node.getNode(EXO_ACTIONS + "/"+ actionName);
    } 
    return null;  
  }
  
  /**
   * {@inheritDoc}
   */
  public boolean hasActions(Node node) throws Exception {
    return node.isNodeType(ACTIONABLE);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getActions(Node node) throws Exception {
    return getActions(node, null);
  }

  /**
   * {@inheritDoc}
   */  
  public List<Node> getCustomActionsNode(Node node, String lifecyclePhase) throws Exception {
    try {
      return getActions(node, lifecyclePhase) ;
    } catch(Exception item) {
      return null ;
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getActions(Node node, String lifecyclePhase) throws Exception {
    List<Node> actions = new ArrayList<Node>();
    Node actionStorage = null;
    try{
      actionStorage = node.getNode(EXO_ACTIONS);
    }catch (Exception e) {
      return actions;
    }
    for (NodeIterator iter = actionStorage.getNodes(); iter.hasNext();) {
      Node tmpNode = iter.nextNode();
      if (tmpNode.isNodeType(ACTION)
          && (lifecyclePhase == null || parseValuesToList(
              tmpNode.getProperty(LIFECYCLE_PHASE_PROP).getValues()).contains(lifecyclePhase))) {
        actions.add(tmpNode);
      }
    }
    return actions;
  }

  /**
   * {@inheritDoc}
   */
  public void removeAction(Node node, String repository) throws Exception {
    if(!node.isNodeType(ACTIONABLE)) return ;
    List<Node> actions = getActions(node);
    for (Node action : actions) {
      removeAction(node, action.getName(), repository);
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeAction(Node node, String actionName, String repository) throws Exception {
    if(!node.isNodeType(ACTIONABLE)) return  ;
    Node action2Remove = node.getNode(EXO_ACTIONS+ "/" + actionName);
    String[] lifecyclePhase = parseValuesToArray(action2Remove.getProperty(LIFECYCLE_PHASE_PROP)
        .getValues());
    String jobName = null, jobGroup = null, jobClassName = null;
    if (action2Remove.isNodeType(SCHEDULABLE_MIXIN)) {
      jobName = action2Remove.getProperty(JOB_NAME_PROP).getString();
      jobGroup = action2Remove.getProperty(JOB_GROUP_PROP).getString();
      jobClassName = action2Remove.getProperty(JOB_CLASS_PROP).getString();
    }
    String actionTypeName = action2Remove.getPrimaryNodeType().getName();
    String actionPath = action2Remove.getPath();
    for (ComponentPlugin plugin : actionPlugins) {
      String actionServiceName = plugin.getName();
      ActionPlugin actionService = getActionPlugin(actionServiceName);
      if (actionService.isActionTypeSupported(actionTypeName)) {
        if ((DMSEvent.getEventTypes(lifecyclePhase) & DMSEvent.SCHEDULE) > 0) {
          actionService.removeActivationJob(jobName, jobGroup, jobClassName);
        }
        actionService.removeObservation(repository, actionPath);
      }
    }
    action2Remove.remove();
    node.save();
  }
  
  /**
   * {@inheritDoc}}
   */
  public void addAction(Node storeActionNode,
                        String actionType,
                        boolean isDeep,
                        String[] uuid,
                        String[] nodeTypeNames,
                        Map mappings) throws Exception {
    Node actionsNode = null;
    try {
      actionsNode = storeActionNode.getNode(EXO_ACTIONS);
    } catch (PathNotFoundException e) {
      actionsNode = storeActionNode.addNode(EXO_ACTIONS,ACTION_STORAGE) ;
      actionsNode.addMixin(EXO_HIDDENABLE) ;
      storeActionNode.save();
    }
    if (!storeActionNode.isNodeType(ACTIONABLE)) {
      storeActionNode.addMixin(ACTIONABLE);
      storeActionNode.save();
    }
    String newActionPath = cmsService_.storeNode(actionType, actionsNode, mappings,true);
    storeActionNode.save();
    String srcWorkspace = storeActionNode.getSession().getWorkspace().getName();

    String srcPath = storeActionNode.getPath();
    ActionPlugin actionService = getActionPluginForActionType(actionType);
    if (actionService == null)
      throw new ClassNotFoundException("Not found any action's service compatible with action type "+actionType) ;
    try {
      actionService.addAction(actionType, srcWorkspace, srcPath, isDeep, uuid, nodeTypeNames, mappings);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
      Session session = getSystemSession(storeActionNode.getSession().getWorkspace().getName());
      Node actionNode = (Node) session.getItem(newActionPath);
      actionNode.remove();
      session.save();
      session.logout();
      throw e;
    }
  }  

  /**
   * {@inheritDoc}
   */
  public void addAction(Node storeActionNode, String actionType, Map mappings) throws Exception {
    boolean isDeep = true;
    String[] nodeTypeName = null;
    String[] uuid = null;
    if (mappings.containsKey("/node/exo:isDeep")) {
      isDeep = Boolean.valueOf(((JcrInputProperty)mappings.get("/node/exo:isDeep")).getValue().toString());
    }
    if (mappings.containsKey("/node/exo:uuid")) {
      uuid = (String[]) ((JcrInputProperty) mappings.get("/node/exo:uuid")).getValue();
      if(uuid.length == 0) uuid = null;
    }
    if (mappings.containsKey("/node/exo:nodeTypeName")) {
      nodeTypeName = (String[]) ((JcrInputProperty) mappings.get("/node/exo:nodeTypeName"))
          .getValue();
      if(nodeTypeName.length == 0) {
        nodeTypeName = null;
        mappings.remove("/node/exo:nodeTypeName");
      }
    }
    addAction(storeActionNode, actionType, isDeep, uuid, nodeTypeName, mappings);
  }  

  /**
   * Call addAction(Node node, String repository, String type, Map mappings) to
   * execute action following userId, node, repository, initiated variables
   * @param userId user identify
   * @param node current node
   * @param actionName name of action
   * @throws Exception
   */
  public void executeAction(String userId, Node node, String actionName) throws Exception {
    Map<String, String> variables = new HashMap<String, String>();
    variables.put("initiator", userId);
    variables.put("actionName", actionName);
    variables.put("nodePath", node.getPath());
    variables.put("srcWorkspace", node.getSession().getWorkspace().getName());
    variables.put("srcPath", node.getPath());

    NodeType nodeType = node.getPrimaryNodeType();
    String nodeTypeName = nodeType.getName();
    variables.put("document-type", nodeTypeName);
    Node actionNode = getAction(node, actionName);
    NodeType actionNodeType = actionNode.getPrimaryNodeType();
    fillVariables(actionNode, actionNodeType, variables);

    NodeType[] actionMixinTypes = actionNode.getMixinNodeTypes();

    for (int i = 0; i < actionMixinTypes.length; i++) {
      NodeType mixinType = actionMixinTypes[i];
      fillVariables(actionNode, mixinType, variables);
    }

    executeAction(userId, node, actionName, variables);
  }


  /**
   * Put to Map with key = property name and value = value of property in actionNode
   * @param actionNode  get value of property in this node
   * @param nodeType    get property definition from this object
   * @param variables   Map to keep (key, value) of all property
   * @throws Exception
   */
  private void fillVariables(Node actionNode, NodeType nodeType, Map variables) throws Exception {
    for(PropertyDefinition def:nodeType.getDeclaredPropertyDefinitions()) {
      String propName = def.getName();
      if (actionNode.hasProperty(propName)) {
        if(actionNode.getProperty(propName).getDefinition().isMultiple()) {

        } else {
          String propValue = actionNode.getProperty(propName).getString();
          variables.put(propName, propValue);
        }
      }
    }
  }

  /**
   * Execute action following userId, node, variables, repository
   * @param userId      user identify
   * @param node        current node
   * @param actionName  name of action
   * @param variables   Map with variables and value
   * @throws Exception
   */
  public void executeAction(String userId, Node node, String actionName, Map variables) throws Exception {
    if (!node.isNodeType(ACTIONABLE)) return ;
    Node actionNode = getAction(node, actionName);
    String actionTypeName = actionNode.getPrimaryNodeType().getName();
    for (ComponentPlugin plugin : actionPlugins) {
      String actionServiceName = plugin.getName();
      ActionPlugin actionPlugin = getActionPlugin(actionServiceName);
      if (actionPlugin.isActionTypeSupported(actionTypeName)) {
        actionPlugin.executeAction(userId, actionNode, variables);
      }
    }
  }

  /**
   * Add ComponentPlugin
   * @param plugin  ComponentPlugin
   */
  public void addPlugin(ComponentPlugin plugin) { actionPlugins.add(plugin); }

  /**
   * Need implemented
   * @param pluginName
   * @return
   */
  public ComponentPlugin removePlugin(String pluginName) {
    return null;
  }

  /**
   * Get all ComponentPlugin
   * @return  Collection of ComponentPlugins
   */
  public Collection<ComponentPlugin> getPlugins() { return actionPlugins; }

  /**
   * Get QueryManager, call initAction(QueryManager queryManager, String repository, String workspace)
   * to initialize action listener for all available repositories and workspaces
   * @throws Exception
   */
  private void initiateActionConfiguration() throws Exception {
    ManageableRepository jcrRepository = null ;
      jcrRepository = repositoryService_.getCurrentRepository();
      String[] workspaces = jcrRepository.getWorkspaceNames();
      for (String workspace : workspaces) {
        Session session = jcrRepository.getSystemSession(workspace);
        QueryManager queryManager = null;
        try {
          queryManager = session.getWorkspace().getQueryManager();
        } catch (RepositoryException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("ActionServiceContainer - Query Manager Factory of workspace "
              + workspace + " not found. Check configuration.", e);
          }
        }
        if (queryManager == null) {
          session.logout();
          continue;
        }
        initAction(queryManager, workspace) ;
        session.logout();
      }
  }

  /**
   * Get QueryManager, call initAction(QueryManager queryManager, String repository, String workspace)
   * to initialize action listener
   * @throws Exception
   */
  private void reInitiateActionConfiguration() throws Exception {
    ManageableRepository jcrRepository = repositoryService_.getCurrentRepository();
    for (String workspace : jcrRepository.getWorkspaceNames()) {
      Session session = jcrRepository.getSystemSession(workspace);
      QueryManager queryManager = null;
      try {
        queryManager = session.getWorkspace().getQueryManager();
      } catch (RepositoryException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("ActionServiceContainer - Query Manager Factory of workspace "
            + workspace + " not found. Check configuration.", e);
        }
      }
      if (queryManager == null)  {
        session.logout();
        continue;
      }
      initAction(queryManager, workspace) ;
      session.logout();
    }
  }

  /**
   * Initialize the action listener for all node in repository
   * All node is got by query following ACTION_QUERY
   * @param queryManager QueryManager
   * @param workspace    workspace name
   * @throws Exception
   */
  private void initAction(QueryManager queryManager, String workspace) throws Exception {
    try {
      Query query = queryManager.createQuery(ACTION_QUERY, Query.XPATH);
      QueryResult queryResult = query.execute();
      for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
        Node actionNode = iter.nextNode();
        String[] lifecyclePhase = parseValuesToArray(actionNode.getProperty(LIFECYCLE_PHASE_PROP)
            .getValues());
        String actionType = actionNode.getPrimaryNodeType().getName();
        for (ComponentPlugin plugin : actionPlugins) {
          String actionServiceName = plugin.getName();
          ActionPlugin actionService = getActionPlugin(actionServiceName);
          if (actionService.isActionTypeSupported(actionType)) {
            if (DMSEvent.getEventTypes(lifecyclePhase) == DMSEvent.READ)
              continue;
            if ((DMSEvent.getEventTypes(lifecyclePhase) & DMSEvent.SCHEDULE) > 0) {
              actionService.reScheduleActivations(actionNode);
            }
            if (DMSEvent.getEventTypes(lifecyclePhase) == DMSEvent.SCHEDULE)
              continue;
              actionService.initiateActionObservation(actionNode);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(">>>> Can not launch action listeners for workspace: "
          + workspace + " in current repository", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void initiateObservation(Node node) throws Exception {
    try {
      Session session = node.getSession();
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      String queryStr;
      Query query = null;
      try {
        if (!"/".equals(node.getPath())) {
          queryStr = "/jcr:root" + node.getPath() + ACTION_QUERY;
        } else {
          queryStr = ACTION_QUERY;
        }
        query = queryManager.createQuery(queryStr, Query.XPATH);
      } catch(InvalidQueryException invalid) {
      // With some special character, XPath will be invalid , try SQL
        if (!"/".equals(node.getPath())) {
          queryStr = ACTION_SQL_QUERY + WHERE_OPERATOR + JCR_PATH + LIKE_OPERATOR
                                        + SINGLE_QUOTE + node.getPath() + "/" + "%" + SINGLE_QUOTE;
        } else {
          queryStr = ACTION_SQL_QUERY;
        }
        query = queryManager.createQuery(queryStr, Query.SQL);
      }
      QueryResult queryResult = query.execute();
      for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
        Node actionNode = iter.nextNode();
        try {
          String actionType = actionNode.getPrimaryNodeType().getName();
          for (ComponentPlugin plugin : actionPlugins) {
            String actionServiceName = plugin.getName();
            ActionPlugin actionService = getActionPlugin(actionServiceName);
            if (actionService.isActionTypeSupported(actionType)) {
              actionService.initiateActionObservation(actionNode);
            }
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not launch action listeners named is " + actionNode.getPath(), e);
          }
        }
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Can not launch action listeners inside " + node.getPath() + " node.", ex);
      }
    }
  }

  /**
   * Parase Array Value to Array String
   * @param values
   * @return Array String
   * @throws Exception
   */
  private String[] parseValuesToArray(Value[] values) throws Exception {
    return parseValuesToList(values).toArray(new String[0]);
  }

  /**
   * Parse Array Value to List String
   * @param values
   * @return
   * @throws Exception
   */
  private List<String> parseValuesToList(Value[] values) throws Exception {
    List<String> lstValues = new ArrayList<String>();
    for(Value value : values) {
      lstValues.add(value.getString());
    }
    return lstValues;
  }
}
