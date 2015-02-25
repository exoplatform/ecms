package org.exoplatform.ecms.upgrade.nodetypes;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by toannh on 10/30/14.
 * Update trashId for exo:restoreLocation nodetype
 */
public class RestoreLocationNodeTypeUpgradePlugin extends UpgradeProductPlugin {
  private Log log = ExoLogger.getLogger(this.getClass().getName());
  public static final String EXO_RESTORE_LOCATION = "exo:restoreLocation";
  public static final String TRASH_ID = "exo:trashId";

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Migrating exo:restoreLocation starting.............");
    }
    try {
      ExtendedNodeTypeManager nodeTypeManager =   WCMCoreUtils.getRepository().getNodeTypeManager();
      NodeTypeValue nodeTypeValue = nodeTypeManager.getNodeTypeValue(EXO_RESTORE_LOCATION);

      List<PropertyDefinitionValue> propValues = nodeTypeValue.getDeclaredPropertyDefinitionValues();
      propValues.add(new PropertyDefinitionValue(TRASH_ID, false, false, OnParentVersionAction.COPY,
              false, new ArrayList<String>(), false, PropertyType.STRING, new ArrayList<String>()));
      nodeTypeValue.setDeclaredPropertyDefinitionValues(propValues);
      nodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating exo:restoreLocation node type", e);
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

  public RestoreLocationNodeTypeUpgradePlugin(InitParams initParams) {
    super(initParams);
  }
}
