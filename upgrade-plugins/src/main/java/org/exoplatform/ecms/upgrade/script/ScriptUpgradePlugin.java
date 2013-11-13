/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.upgrade.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.scripts.impl.ScriptServiceImpl;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 23, 2012
 * 
 * This class will be used to upgrade pre-defined scripts. Scripts with desire of manual upgration 
 * can be specified in file configuration.properties.<br/>
 * Syntax :<br/> 
 * unchanged-scripts=<script name list>
 * For examples :<br/>
 * unchanged-scripts=action/AddMetadataScript.groovy, action/AddTaxonomyActionScript.groovy
 * 
 */
public class ScriptUpgradePlugin extends UpgradeProductPlugin {
  
  private static final String EXO_ACTION = "exo:action";
  private static final String EXO_SCRIPT = "exo:script";
  
  private Log LOG = ExoLogger.getLogger(this.getClass());
  private ScriptService scriptService_;
  
  public ScriptUpgradePlugin(ScriptService scriptService, InitParams initParams) {
    super(initParams);
    this.scriptService_ = scriptService;
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    
    //replace old content script by new one
    replaceScripts();
    //Remove old renamed scripts, 
    //change script name to new ones in action using renamed scripts,
    //add dc:description to jcr:content of old un-renamed scripts
    //in all exo:action nodes, change property exo:script: rename old script name to new ones.
    migrateScripts();

  }
  
  /**
   * Replaces old content script by new one
   */
  private void replaceScripts() {
    String unchangedTemplates = PrivilegedSystemHelper.getProperty("unchanged-scripts");
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedTemplates)) {
      unchangedTemplates = "";
    }
    try {
      Set<String> unchangedTemplateSet = new HashSet<String>();
      Set<String> allConfiguredScripts = scriptService_.getAllConfiguredScripts();
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedTemplate : unchangedTemplates.split(",")) {
        unchangedTemplateSet.add(unchangedTemplate.trim());
      }
      //get all script nodes that need to be removed
      sessionProvider = SessionProvider.createSystemProvider();
      Node ecmExplorer = scriptService_.getECMScriptHome(sessionProvider);
      QueryManager queryManager = ecmExplorer.getSession().getWorkspace().getQueryManager();
      NodeIterator iter = queryManager.
          createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" +ecmExplorer.getPath() + "/%'", Query.SQL).
          execute().getNodes();
      while (iter.hasNext()) {
        Node scriptNode = iter.nextNode();
        if (!unchangedTemplateSet.contains(scriptNode.getPath().substring(ecmExplorer.getPath().length() + 1)) &&
            allConfiguredScripts.contains(scriptNode.getName())) {
          removedNodes.add(scriptNode);
        }
      }
      //remove all old script nodes
      for (Node removedNode : removedNodes) {
        try {
          removedNode.remove();
          ecmExplorer.save();
        } catch (Exception e) {
          if (LOG.isInfoEnabled()) {
            LOG.error("Error in " + this.getName() + ": Can not remove old template: " + removedNode.getPath());
          }
        }
      }
      // re-initialize new scripts
      ((ScriptServiceImpl)scriptService_).start();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating scripts", e);        
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  
  /**
   * Remove old renamed scripts, 
   * change script name to new ones in action using renamed scripts,
   * add dc:description to jcr:content of old un-renamed scripts, 
   * in all exo:action nodes, change property exo:script: rename old script name to new ones.
   */
  private void migrateScripts() {
    Map<String, String> scriptUpdateMap = new HashMap<String, String>();
    //renamed scripts
    scriptUpdateMap.put("AddMetadataScript.groovy", "AddMetadata.groovy");
    scriptUpdateMap.put("AddTaxonomyActionScript.groovy", "AddToCategory.groovy");
    scriptUpdateMap.put("AddToFavoriteScript.groovy", "AddToFavorites.groovy");
    scriptUpdateMap.put("AutoVersioningScript.groovy", "AutoVersioning.groovy");
    scriptUpdateMap.put("EnableVersioningScript.groovy", "EnableVersioning.groovy");
    scriptUpdateMap.put("TrashFolderScript.groovy", "TrashFolder.groovy");
    
    List<String> oldUnrenamedScripts = new ArrayList<String>();
    //old unrenamed script
    oldUnrenamedScripts.add("GetMailScript.groovy");
    oldUnrenamedScripts.add("PopulateToHomePageMenu.groovy");
    oldUnrenamedScripts.add("ProcessRecordsScript.groovy");
    oldUnrenamedScripts.add("PublishingRequestScript.groovy");
    oldUnrenamedScripts.add("RSSScript.groovy");
    oldUnrenamedScripts.add("SendMailScript.groovy");
    oldUnrenamedScripts.add("TransformBinaryChildrenToTextScript.groovy");
    
    removeOldScripts(scriptUpdateMap);
    changeScriptNameInActionNode(scriptUpdateMap);
    addDcDescription();
    changeScriptNameInActionDataNode(scriptUpdateMap);
  }

  /**
   * Remove old renamed scripts, 
   */
  private void removeOldScripts(Map<String, String> scriptUpdateMap) {
    SessionProvider sProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start removing old renamed scripts =====");
      }
      ScriptService scriptService = WCMCoreUtils.getService(ScriptService.class);
      sProvider = SessionProvider.createSystemProvider();
      Node scriptHome = scriptService.getECMScriptHome(sProvider);
      List<Node> actionScripts = scriptService.getECMActionScripts(sProvider);
      for (Node actionScript : actionScripts) {
        try {
          if (scriptUpdateMap.containsKey(actionScript.getName())) {
            if (LOG.isInfoEnabled()) {
              LOG.info("Removing " + actionScript.getName());
            }
            actionScript.remove();
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs when removing old renamed script: " + actionScript.getName(), e);
          }
        }
      }
      scriptHome.save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when removing old renamed scripts: ", e);
      }
    } finally {
      if (sProvider != null) {
        sProvider.close();
      }
    }
  }
  
  /**
   * change script name to new ones in action using renamed scripts,
   */
  private void changeScriptNameInActionNode(Map<String, String> scriptUpdateMap) {
    SessionProvider sProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start changing script name to new ones in action using renamed scripts =====");
      }
      ActionServiceContainer actionServiceContainer = WCMCoreUtils.getService(ActionServiceContainer.class);
      ExtendedNodeTypeManager nodetypeManager = WCMCoreUtils.getRepository().getNodeTypeManager();
      Collection<NodeType> actionTypes =
          actionServiceContainer.getCreatedActionTypes(WCMCoreUtils.getRepository().getConfiguration().getName());
      sProvider = SessionProvider.createSystemProvider();
      
      for (NodeType actionType : actionTypes) {
        try {
          NodeTypeValue actionTypeValue = nodetypeManager.getNodeTypeValue(actionType.getName());
          List<PropertyDefinitionValue> propValues = actionTypeValue.getDeclaredPropertyDefinitionValues();
          for (PropertyDefinitionValue propValue : propValues) {
            if ("exo:script".equals(propValue.getName())) {
              List<String> defaultValues = propValue.getDefaultValueStrings();
              if (defaultValues != null && defaultValues.size() > 0) {
                String defaultValue = defaultValues.get(0);
                String scriptName = (defaultValue.indexOf('/') > -1) ?
                                               defaultValue.substring(defaultValue.lastIndexOf('/') + 1) :
                                               defaultValue;
                if (scriptUpdateMap.containsKey(scriptName)) {
                  if (LOG.isInfoEnabled()) {
                    LOG.info("Changing for " + actionType.getName());
                  }
                  defaultValues.set(0, defaultValue.replace(scriptName,
                                                            scriptUpdateMap.get(scriptName)));
  
                  propValue.setDefaultValueStrings(defaultValues);
                  actionTypeValue.setDeclaredPropertyDefinitionValues(propValues);
                  nodetypeManager.registerNodeType(actionTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
                  break;
                }
              }
            }
            
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs when change action action: " + actionType.getName(), e);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when changing actions: ", e);
      }
    } finally {
      if (sProvider != null) {
        sProvider.close();
      }
    }
  }
  
  /**
   * Adds dc:description to jcr:content of old un-renamed scripts
   */
  private void addDcDescription() {
    SessionProvider sProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start adding dc:description to old unrenamed scripts =====");
      }
      ScriptService scriptService = WCMCoreUtils.getService(ScriptService.class);
      sProvider = SessionProvider.createSystemProvider();
      Node scriptHome = scriptService.getECMScriptHome(sProvider);
      List<Node> actionScripts = scriptService.getECMActionScripts(sProvider);
      for (Node actionScript : actionScripts) {
        String scriptName = actionScript.getName();
        try {
          if (LOG.isInfoEnabled()) {
            LOG.info("Adding dc:description for " + scriptName);
          }
          Node content = actionScript.getNode(NodetypeConstant.JCR_CONTENT);
          if (!content.hasProperty(NodetypeConstant.DC_DESCRIPTION)) {
            if (content.canAddMixin(NodetypeConstant.DC_ELEMENT_SET)) {
              content.addMixin(NodetypeConstant.DC_ELEMENT_SET);
            }
            String description = scriptName.indexOf(".") > -1 ? 
                                 scriptName.substring(0, scriptName.indexOf(".")) : scriptName;
            content.setProperty(NodetypeConstant.DC_DESCRIPTION, new String[]{description});
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs when removing old renamed script: " + scriptName, e);
          }
        }
      }
      scriptHome.save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when adding dc:description to old unrenamed scripts: ", e);
      }
    } finally {
      if (sProvider != null) {
        sProvider.close();
      }
    }
  }
  
  /**
   * change script name to new ones in action data nodes using renamed scripts,
   */
  private void changeScriptNameInActionDataNode(Map<String, String> scriptUpdateMap) {
    SessionProvider sProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start changing script name to new ones in action data nodes using renamed scripts =====");
      }
      sProvider = SessionProvider.createSystemProvider();
      ManageableRepository repository = WCMCoreUtils.getRepository();
      
      String query = "SELECT * FROM " + EXO_ACTION; 
      for (WorkspaceEntry wsEntry : repository.getConfiguration().getWorkspaceEntries()) {
        try {
          String ws = wsEntry.getName();
          Session session = sProvider.getSession(ws, repository);
          NodeIterator iter = session.getWorkspace().getQueryManager().createQuery(query, Query.SQL).execute().getNodes();
          while (iter.hasNext()) {
            Node action = iter.nextNode();
            if (action.hasProperty(EXO_SCRIPT)) {
              String oldScript = action.getProperty(EXO_SCRIPT).getString();
              for (String script : scriptUpdateMap.keySet()) {
                if (oldScript.contains(script)) {
                  String newScript = oldScript.replace(script, scriptUpdateMap.get(script));
                  action.setProperty(EXO_SCRIPT, newScript);
                  action.save();
                  break;
                }
              }
            }
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs when change action in workspace: " + wsEntry.getName(), e);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when changing actions: ", e);
      }
    } finally {
      if (sProvider != null) {
        sProvider.close();
      }
    }
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }
  
}
