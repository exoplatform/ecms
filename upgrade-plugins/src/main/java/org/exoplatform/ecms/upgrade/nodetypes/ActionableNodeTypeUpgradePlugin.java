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

import java.util.List;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class ActionableNodeTypeUpgradePlugin extends UpgradeProductPlugin {
  private static final String actionableType = "exo:actionable";
  private static final String exoActionsProperty = "exo:actions";
  private Log log = ExoLogger.getLogger(this.getClass().getName());
  
  public ActionableNodeTypeUpgradePlugin(InitParams initParams) {
    super(initParams);
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Migrating " + actionableType + ".............");
    }
    try {
      ExtendedNodeTypeManager nodeTypeManager =   WCMCoreUtils.getRepository().getNodeTypeManager();
      NodeTypeValue nodeTypeValue = nodeTypeManager.getNodeTypeValue(actionableType);
      List<PropertyDefinitionValue> propValues = nodeTypeValue.getDeclaredPropertyDefinitionValues();
      for (PropertyDefinitionValue propValue : propValues) {
        if (exoActionsProperty.equalsIgnoreCase(propValue.getName())) {
          propValue.setMandatory(false);
          break;
        }
      }
      nodeTypeValue.setDeclaredPropertyDefinitionValues(propValues);
      nodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating exo:actionable node type", e);
      }
    }
  }
    
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion,previousVersion);  
  }
    
}
