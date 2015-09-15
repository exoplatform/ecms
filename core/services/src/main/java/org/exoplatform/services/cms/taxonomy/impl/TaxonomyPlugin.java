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
package org.exoplatform.services.cms.taxonomy.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.actions.impl.ActionConfig;
import org.exoplatform.services.cms.actions.impl.ActionConfig.TaxonomyAction;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Permission;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Taxonomy;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@gmail.com Mar 31, 2009
 */
public class TaxonomyPlugin extends BaseComponentPlugin {
  private String                 workspace                  = null;

  private String                 path                       = "";

  private String                 treeName                   = "";

  private List<Permission>       permissions                = new ArrayList<Permission>(4);

  private boolean                autoCreateInNewRepository_ = true;

  private RepositoryService      repositoryService_;

  private TaxonomyService        taxonomyService_;

  private String                 baseTaxonomiesStorage_;

  private ActionServiceContainer actionServiceContainer_;

  private InitParams             params_;

  final static String MIX_AFFECTED_NODETYPE  = "mix:affectedNodeTypes";
  final static String AFFECTED_NODETYPE      = "exo:affectedNodeTypeNames";
  final static String ALL_DOCUMENT_TYPES     = "ALL_DOCUMENT_TYPES";

  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(TaxonomyPlugin.class.getName());

  public TaxonomyPlugin(InitParams params, RepositoryService repositoryService,
      NodeHierarchyCreator nodeHierarchyCreator, TaxonomyService taxonomyService,
      ActionServiceContainer actionServiceContainer,
      DMSConfiguration dmsConfiguration) throws Exception {
    repositoryService_ = repositoryService;
    baseTaxonomiesStorage_ = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
    taxonomyService_ = taxonomyService;
    actionServiceContainer_ = actionServiceContainer;
    params_ = params;
    ValueParam autoCreated = params_.getValueParam("autoCreateInNewRepository");
    ValueParam workspaceParam = params_.getValueParam("workspace");
    ValueParam pathParam = params_.getValueParam("path");
    ValueParam nameParam = params_.getValueParam("treeName");
    if (autoCreated != null) autoCreateInNewRepository_ = Boolean.parseBoolean(autoCreated.getValue());
    if (workspaceParam != null) {
      workspace = workspaceParam.getValue();
    }
    if (pathParam == null) {
      path = baseTaxonomiesStorage_;
    } else {
      path = pathParam.getValue();
    }
    if (nameParam != null) {
      treeName = nameParam.getValue();
    }
    dmsConfiguration_ = dmsConfiguration;
  }
  
  public void init() throws Exception {
    importPredefineTaxonomies();
  }  

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  @SuppressWarnings("unchecked")
  private void importPredefineTaxonomies() throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    if (workspace == null) {
      setWorkspace(dmsRepoConfig.getSystemWorkspace());
    } else {
      // in case workspace is not initialized, we choose dms system workspace default
      if (!manageableRepository.isWorkspaceInitialized(workspace))
        setWorkspace(dmsRepoConfig.getSystemWorkspace());
    }
    Session session = manageableRepository.getSystemSession(getWorkspace());
    if(Utils.getAllEditedConfiguredData(
      "TaxonomyTree", "EditedConfiguredTaxonomyTree", true).contains(treeName)) return;
    Node taxonomyStorageNode = (Node) session.getItem(path);
    if (taxonomyStorageNode.hasNode(treeName)) {
      session.logout();
      return;
    }
    taxonomyStorageNode.setProperty("exo:isImportedChildren", true);
    Iterator<ObjectParameter> it = params_.getObjectParamIterator();
    Node taxonomyStorageNodeSystem = Utils.makePath(taxonomyStorageNode, treeName, "exo:taxonomy",
            null);
    String systemUser = IdentityConstants.SYSTEM;

    while (it.hasNext()) {
      ObjectParameter objectParam = it.next();
      if (objectParam.getName().equals("permission.configuration")) {
        TaxonomyConfig config = (TaxonomyConfig) objectParam.getObject();
        for (Taxonomy taxonomy : config.getTaxonomies()) {
          Map mapPermissions = getPermissions(taxonomy.getPermissions());
          if (mapPermissions != null) {
            ((ExtendedNode) taxonomyStorageNodeSystem).setPermissions(mapPermissions);
          }
          if (taxonomyStorageNodeSystem.canAddMixin("mix:referenceable")) {
            taxonomyStorageNodeSystem.addMixin("mix:referenceable");
          }
          if (!containsUserInACL(((ExtendedNode)taxonomyStorageNodeSystem).getACL().getPermissionEntries(), systemUser)) {
            if (taxonomyStorageNodeSystem.canAddMixin("exo:privilegeable"))
              taxonomyStorageNodeSystem.addMixin("exo:privilegeable");
            ((ExtendedNode)taxonomyStorageNodeSystem).setPermission(systemUser, PermissionType.ALL);
          }
        }
        session.save();
      } else if (objectParam.getName().equals("taxonomy.configuration")) {
        TaxonomyConfig config = (TaxonomyConfig) objectParam.getObject();
        for (Taxonomy taxonomy : config.getTaxonomies()) {
          Node taxonomyNode = Utils.makePath(taxonomyStorageNodeSystem, taxonomy.getPath(),
              "exo:taxonomy", getPermissions(taxonomy.getPermissions()));

          if (!containsUser(taxonomy.getPermissions(), systemUser)) {
            if (taxonomyNode.canAddMixin("exo:privilegeable"))
              taxonomyNode.addMixin("exo:privilegeable");
            ((ExtendedNode)taxonomyNode).setPermission(systemUser, PermissionType.ALL);
          }
          if (taxonomyNode.canAddMixin("mix:referenceable")) {
            taxonomyNode.addMixin("mix:referenceable");
          }
          if (taxonomyNode.canAddMixin("exo:rss-enable")) {
            taxonomyNode.addMixin("exo:rss-enable");
          }
          if(taxonomy.getTitle() != null && !taxonomy.getTitle().isEmpty()) {
            taxonomyNode.setProperty("exo:title", taxonomy.getTitle());
          } else {
            taxonomyNode.setProperty("exo:title", taxonomy.getName());
          }

          taxonomyNode.getSession().save();
        }
      } else if (objectParam.getName().equals("predefined.actions")) {
        ActionConfig config = (ActionConfig) objectParam.getObject();
        List actions = config.getActions();
        for (Iterator iter = actions.iterator(); iter.hasNext();) {
          TaxonomyAction action = (TaxonomyAction) iter.next();
          taxonomyStorageNodeSystem = (Node)session.getItem(taxonomyStorageNodeSystem.getPath());
          addAction(action, taxonomyStorageNodeSystem);
        }
      }

    }
    session.save();
    try {
      taxonomyService_.addTaxonomyTree(taxonomyStorageNodeSystem);
    } catch (TaxonomyAlreadyExistsException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    session.save();
    session.logout();
  }

  private boolean containsUserInACL(List<AccessControlEntry> entries, String userName) {
    if (userName == null) return false;
    for (AccessControlEntry entry : entries)
      if (userName.equals(entry.getIdentity()))
          return true;
    return false;
  }

  private boolean containsUser(List<Permission> permissions, String userName) {
    if (userName == null) return false;
    for (Permission permission : permissions)
      if (userName.equals(permission.getIdentity()))
          return true;
    return false;
  }

  private void addAction(ActionConfig.TaxonomyAction action, Node srcNode)
      throws Exception {
    ManageableRepository manageRepo = repositoryService_.getCurrentRepository();
    Map<String, JcrInputProperty> sortedInputs = new HashMap<String, JcrInputProperty>();
    JcrInputProperty jcrInputName = new JcrInputProperty();
    jcrInputName.setJcrPath("/node/exo:name");
    jcrInputName.setValue(action.getName());
    sortedInputs.put("/node/exo:name", jcrInputName);
    JcrInputProperty jcrInputDes = new JcrInputProperty();
    jcrInputDes.setJcrPath("/node/exo:description");
    jcrInputDes.setValue(action.getDescription());
    sortedInputs.put("/node/exo:description", jcrInputDes);

    JcrInputProperty jcrInputLife = new JcrInputProperty();
    jcrInputLife.setJcrPath("/node/exo:lifecyclePhase");
    jcrInputLife.setValue(action.getLifecyclePhase().toArray(new String[0]));
    sortedInputs.put("/node/exo:lifecyclePhase", jcrInputLife);

    JcrInputProperty jcrInputHomePath = new JcrInputProperty();
    jcrInputHomePath.setJcrPath("/node/exo:storeHomePath");
    jcrInputHomePath.setValue(action.getHomePath());
    sortedInputs.put("/node/exo:storeHomePath", jcrInputHomePath);

    JcrInputProperty jcrInputTargetWspace = new JcrInputProperty();
    jcrInputTargetWspace.setJcrPath("/node/exo:targetWorkspace");
    jcrInputTargetWspace.setValue(action.getTargetWspace());
    sortedInputs.put("/node/exo:targetWorkspace", jcrInputTargetWspace);

    JcrInputProperty jcrInputTargetPath = new JcrInputProperty();
    jcrInputTargetPath.setJcrPath("/node/exo:targetPath");
    jcrInputTargetPath.setValue(action.getTargetPath());
    sortedInputs.put("/node/exo:targetPath", jcrInputTargetPath);

    JcrInputProperty rootProp = sortedInputs.get("/node");
    if (rootProp == null) {
      rootProp = new JcrInputProperty();
      rootProp.setJcrPath("/node");
      rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue());
      sortedInputs.put("/node", rootProp);
    } else {
      rootProp.setValue((sortedInputs.get("/node/exo:name")).getValue());
    }
    actionServiceContainer_.addAction(srcNode, action.getType(), sortedInputs);
    Node actionNode = actionServiceContainer_.getAction(srcNode, action.getName());
    if (action.getRoles() != null) {
      String[] roles = StringUtils.split(action.getRoles(), ";");
      actionNode.setProperty("exo:roles", roles);
    }

    Iterator mixins = action.getMixins().iterator();
    NodeType nodeType;
    String value;
    while (mixins.hasNext()) {
      ActionConfig.Mixin mixin = (ActionConfig.Mixin) mixins.next();
      actionNode.addMixin(mixin.getName());
      Map<String, String> props = mixin.getParsedProperties();
      Set keys = props.keySet();
      nodeType = manageRepo.getNodeTypeManager().getNodeType(mixin.getName());
      for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
        String key = (String) iterator.next();
        for(PropertyDefinition pro : nodeType.getPropertyDefinitions()) {
          if (pro.getName().equals(key)) {
            if (pro.isMultiple()) {
              value = props.get(key);
                  if (value != null) {
                    actionNode.setProperty(key, value.split(","));
                  }
                } else {
                actionNode.setProperty(key, props.get(key));
              }
            break;
          }
        }
      }
    }
    actionNode.getSession().save();
  }

  public Map getPermissions(List<Permission> listPermissions) {
    Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
    for (Permission permission : listPermissions) {
      StringBuilder strPer = new StringBuilder();
      if ("true".equals(permission.getRead()))
        strPer.append(PermissionType.READ);
      if ("true".equals(permission.getAddNode()))
        strPer.append(",").append(PermissionType.ADD_NODE);
      if ("true".equals(permission.getSetProperty()))
        strPer.append(",").append(PermissionType.SET_PROPERTY);
      if ("true".equals(permission.getRemove()))
        strPer.append(",").append(PermissionType.REMOVE);
      permissionsMap.put(permission.getIdentity(), strPer.toString().split(","));
    }
    return permissionsMap;
  }
}
