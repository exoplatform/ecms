/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.configuration.ConfigurationManager;
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
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Permission;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Taxonomy;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Aug 11, 2009
 */
public class CreateTaxonomyPlugin extends CreatePortalPlugin {

  private static final Log LOG = ExoLogger.getLogger(CreateTaxonomyPlugin.class.getName());

  public static final String MIX_AFFECTED_NODETYPE  = "mix:affectedNodeTypes";

  public static final String AFFECTED_NODETYPE      = "exo:affectedNodeTypeNames";

  public static final String ALL_DOCUMENT_TYPES     = "ALL_DOCUMENT_TYPES";

  /** The workspace. */
  private String                  workspace                  = "";

  /** The path. */
  private String                  path                       = "";

  /** The tree name. */
  private String                  treeName                   = "";

  /** The permissions. */
  private List<Permission>        permissions                = new ArrayList<Permission>(4);

  /** The auto create in new repository_. */
  private boolean                 autoCreateWithNewSite_ = false;

  /** The repository service_. */
  private RepositoryService       repositoryService;

  /** The taxonomy service_. */
  private TaxonomyService         taxonomyService;

  /** The link manager service_. */
  private LinkManager             linkManager;

  /** The base taxonomies storage_. */
  private String                  baseTaxonomiesStorage;

  /** The base taxonomies definition_. */
  private String                  baseTaxonomiesDefinition;

  /** The action service container_. */
  private ActionServiceContainer  actionServiceContainer;

  /** The params_. */
  private InitParams              params;

  /** The dms configuration_. */
  private DMSConfiguration        dmsConfiguration;

  /** The name. */
  private String                  name;

  private String                   portalName;

  /**
   * Instantiates a new initial taxonomy plugin.
   *
   * @param params the params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param nodeHierarchyCreator the node hierarchy creator
   * @param taxonomyService the taxonomy service
   * @param actionServiceContainer the action service container
   * @param dmsConfiguration the dms configuration
   *
   * @throws Exception the exception
   */
  public CreateTaxonomyPlugin(InitParams params,
                               ConfigurationManager configurationManager,
                               RepositoryService repositoryService,
                               NodeHierarchyCreator nodeHierarchyCreator,
                               TaxonomyService taxonomyService,
                               ActionServiceContainer actionServiceContainer,
                               DMSConfiguration dmsConfiguration,
                               LinkManager linkManager) throws Exception {
    super(params, configurationManager, repositoryService);

    this.repositoryService = repositoryService;
    this.baseTaxonomiesStorage = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
    this.baseTaxonomiesDefinition = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_DEFINITION_PATH);
    this.taxonomyService = taxonomyService;
    this.actionServiceContainer = actionServiceContainer;
    this.params = params;
    this.dmsConfiguration = dmsConfiguration;
    this.linkManager = linkManager;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.artifacts.BasePortalArtifactsPlugin
   * #deployToPortal(java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    this.portalName = portalName;
    ValueParam autoCreated = params.getValueParam("autoCreateWithNewSite");
    ValueParam workspaceParam = params.getValueParam("workspace");
    ValueParam pathParam = params.getValueParam("path");
    ValueParam nameParam = params.getValueParam("treeName");
    if (autoCreated != null)
      autoCreateWithNewSite_ = Boolean.parseBoolean(autoCreated.getValue());
    if(!autoCreateWithNewSite_) return;
    if (pathParam == null || workspaceParam == null || workspaceParam.getValue().trim().length() == 0) {
      path = baseTaxonomiesStorage;
    } else {
      path = pathParam.getValue();
      workspace = workspaceParam.getValue();
    }
    if (nameParam != null) {
      treeName = nameParam.getValue();
    }

    if (treeName.contains("{treeName}")) {
      treeName = StringUtils.replace(treeName, "{treeName}", portalName);
    }

    path = StringUtils.replace(path, "{portalName}", portalName);

    Session session = null;
    try {
      // Get source information
      Node srcTaxonomy = taxonomyService.getTaxonomyTree(treeName);
      String srcWorkspace = srcTaxonomy.getSession().getWorkspace().getName();

      // Get destination information
      ManageableRepository repository = repositoryService.getCurrentRepository();
      session = sessionProvider.getSession(this.workspace, repository);
      Workspace destWorkspace = session.getWorkspace();
      String destPath = path + "/" + srcTaxonomy.getName();

      // If same workspace
      if (srcWorkspace.equals(destWorkspace.getName())) {
        destWorkspace.move(srcTaxonomy.getPath(), destPath);
      } else {
        // Clone taxonomy tree across workspace
        destWorkspace.clone(srcWorkspace, srcTaxonomy.getPath(), destPath, true);

        // Remove old link taxonomy tree in definition
        String dmsSystemWorkspaceName = dmsConfiguration.getConfig().getSystemWorkspace();
        Node taxonomyDefinition = (Node) sessionProvider.getSession(dmsSystemWorkspaceName,
                                                                    repository)
                                                        .getItem(baseTaxonomiesDefinition);
        Node srcLinkTaxonomy = taxonomyDefinition.getNode(srcTaxonomy.getName());
        srcLinkTaxonomy.remove();

        // Remove old taxonomy tree
        srcTaxonomy.remove();

        // Register new taxonomy tree in definition
        Node destTaxonomy = (Node) session.getItem(destPath);
        linkManager.createLink(taxonomyDefinition, destTaxonomy);
      }
      session.save();
      return;
    } catch (Exception e) {
      init();
    }
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  public void init() throws Exception {
    importPredefineTaxonomies();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#getName()
   */
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#setName(java.lang.String)
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path the new path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  public List<Permission> getPermissions() {
    return permissions;
  }

  /**
   * Sets the permissions.
   *
   * @param permissions the new permissions
   */
  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Sets the workspace.
   *
   * @param workspace the new workspace
   */
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  /**
   * Import predefine taxonomies.
   *
   * @param repository the repository
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private void importPredefineTaxonomies() throws Exception {
    ManageableRepository manageableRepository = this.repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = this.dmsConfiguration.getConfig();
    if (getWorkspace() == null) {
      setWorkspace(dmsRepoConfig.getSystemWorkspace());
    }
    Session session = manageableRepository.getSystemSession(getWorkspace());
    Node taxonomyStorageNode = (Node) session.getItem(getPath());
    if (taxonomyStorageNode.hasProperty("exo:isImportedChildren")) {
      session.logout();
      return;
    }
    taxonomyStorageNode.setProperty("exo:isImportedChildren", true);
    Iterator<ObjectParameter> it = params.getObjectParamIterator();
    Node taxonomyStorageNodeSystem = Utils.makePath(taxonomyStorageNode, treeName, "exo:taxonomy",
            null);
    session.save();
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
        }
      } else if (objectParam.getName().equals("taxonomy.configuration")) {
        TaxonomyConfig config = (TaxonomyConfig) objectParam.getObject();
        for (Taxonomy taxonomy : config.getTaxonomies()) {
          Node taxonomyNode = Utils.makePath(taxonomyStorageNodeSystem, taxonomy.getPath(),
              "exo:taxonomy", getPermissions(taxonomy.getPermissions()));
          if (taxonomyNode.canAddMixin("mix:referenceable")) {
            taxonomyNode.addMixin("mix:referenceable");
          }

          if (taxonomyNode.canAddMixin("exo:rss-enable")) {
            taxonomyNode.addMixin("exo:rss-enable");
          }
          if(StringUtils.isNotEmpty(taxonomy.getTitle())) {
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
          addAction(action, taxonomyStorageNodeSystem);
        }
      }

    }
    taxonomyStorageNode.save();
    try {
      taxonomyService.addTaxonomyTree(taxonomyStorageNodeSystem);
    } catch (TaxonomyAlreadyExistsException e) {
      if (LOG.isErrorEnabled()) LOG.error("Cannot add taxonomy tree", e);
    }
    session.save();
    session.logout();
  }

  /**
   * Adds the action.
   *
   * @param action the action
   * @param srcNode the src node
   * @param repository the repository
   *
   * @throws Exception the exception
   */
  private void addAction(ActionConfig.TaxonomyAction action, Node srcNode)
      throws Exception {
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
    String homepath = action.getHomePath();
    homepath = StringUtils.replace(homepath, "{portalName}", portalName);
    homepath = StringUtils.replace(homepath, "{treeName}", treeName);
    jcrInputHomePath.setValue(homepath);
    sortedInputs.put("/node/exo:storeHomePath", jcrInputHomePath);

    JcrInputProperty jcrInputTargetWspace = new JcrInputProperty();
    jcrInputTargetWspace.setJcrPath("/node/exo:targetWorkspace");
    jcrInputTargetWspace.setValue(action.getTargetWspace());
    sortedInputs.put("/node/exo:targetWorkspace", jcrInputTargetWspace);

    JcrInputProperty jcrInputTargetPath = new JcrInputProperty();
    jcrInputTargetPath.setJcrPath("/node/exo:targetPath");
    String targetPath = action.getTargetPath();
    targetPath = StringUtils.replace(targetPath, "{portalName}", portalName);
    jcrInputTargetPath.setValue(targetPath);
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
    actionServiceContainer.addAction(srcNode, action.getType(), sortedInputs);
    Node actionNode = actionServiceContainer.getAction(srcNode, action.getName());
    if (action.getRoles() != null) {
      String[] roles = StringUtils.split(action.getRoles(), ";");
      actionNode.setProperty("exo:roles", roles);
    }

    Iterator mixins = action.getMixins().iterator();
    NodeType nodeType;
    String value;
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    while (mixins.hasNext()) {
      ActionConfig.Mixin mixin = (ActionConfig.Mixin) mixins.next();
      actionNode.addMixin(mixin.getName());
      Map<String, String> props = mixin.getParsedProperties();
      Set keys = props.keySet();
      nodeType = manageableRepository.getNodeTypeManager().getNodeType(mixin.getName());
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

  /**
   * Gets the permissions.
   *
   * @param listPermissions the list permissions
   *
   * @return the permissions
   */
  private Map<String, String[]> getPermissions(List<Permission> listPermissions) {
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
