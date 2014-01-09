/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.nodetypes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 9, 2014  
 */
public class ActionTypeUpgradePlugin extends UpgradeProductPlugin {

  private static final String[] actionTypes = new String[] 
       {"exo:addMetadataAction",
        "exo:trashFolderAction",
        "exo:addToFavoriteAction",
        "exo:taxonomyAction",
        "exo:enableVersioning",
        "exo:autoVersioning",
        "exo:populateToMenu"};
  
  private static final String[] properties = new String [] 
      {"exo:script", "exo:scriptLabel"};
  
  private Log log = ExoLogger.getLogger(this.getClass().getName());

  public ActionTypeUpgradePlugin(InitParams initParams) {
    super(initParams);
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    Set<String> propertySet = new HashSet<String>(Arrays.asList(properties));
    
    
    ExtendedNodeTypeManager nodeTypeManager =   WCMCoreUtils.getRepository().getNodeTypeManager();
    for (String actionType : actionTypes) {
      try {
        if (log.isInfoEnabled()) {
          log.info("Migrating " + actionType + ".............");
        }
        NodeTypeValue nodeTypeValue = nodeTypeManager.getNodeTypeValue(actionType);
        List<PropertyDefinitionValue> propValues = nodeTypeValue.getDeclaredPropertyDefinitionValues();
        for (PropertyDefinitionValue propValue : propValues) {
          if (propertySet.contains(propValue.getName())) {
            propValue.setReadOnly(false);
          }
        }
        nodeTypeValue.setDeclaredPropertyDefinitionValues(propValues);
        nodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("An unexpected error occurs when migrating action node type: " + actionType, e);        
        }
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion,previousVersion);  
  }
  
}
