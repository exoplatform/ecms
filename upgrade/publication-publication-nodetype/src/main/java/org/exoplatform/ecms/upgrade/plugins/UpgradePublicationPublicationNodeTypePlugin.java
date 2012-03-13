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
package org.exoplatform.ecms.upgrade.plugins;

import java.util.List;

import javax.jcr.Session;
import javax.jcr.version.OnParentVersionAction;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Jan 5, 2012  
 */
public class UpgradePublicationPublicationNodeTypePlugin extends UpgradeProductPlugin {
  
  private static final String PUBLICATION_HISTORY = "publication:history";
  private static final String PUBLICATION_PUBLICATION = "publication:publication";

  private DMSConfiguration dmsConfiguration_;
  private RepositoryService repoService_;
  private Log log = ExoLogger.getLogger(this.getClass());
  

  public UpgradePublicationPublicationNodeTypePlugin(RepositoryService repoService, DMSConfiguration dmsConfiguration, 
                               InitParams initParams) {
    super(initParams);
    this.repoService_ = repoService;
    this.dmsConfiguration_ = dmsConfiguration;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      //get info
      Session session = sessionProvider.getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                                                  repoService_.getCurrentRepository());
      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
      NodeTypeValue mixVotableNodeTypeValue = nodeTypeManager.getNodeTypeValue(PUBLICATION_PUBLICATION);
      List<PropertyDefinitionValue> propertyDefinitionList = mixVotableNodeTypeValue.getDeclaredPropertyDefinitionValues();
      //check if publication:history 's attribute onParentVersion already equals IGNORE
      boolean publicationHistoryIgnore = false;
      for (PropertyDefinitionValue propertyDefinition : propertyDefinitionList) {
        if (propertyDefinition.getName().equals(PUBLICATION_HISTORY)) {
          if (propertyDefinition.getOnVersion() == OnParentVersionAction.IGNORE) {
            publicationHistoryIgnore = true;
            break;
          } else if (propertyDefinition.getOnVersion() == OnParentVersionAction.COPY) {
            propertyDefinition.setOnVersion(OnParentVersionAction.IGNORE);
            break;
          } 
        }
      }
      //change definition of publication:history 's onParentVersion
      if (!publicationHistoryIgnore) {
        mixVotableNodeTypeValue.setDeclaredPropertyDefinitionValues(propertyDefinitionList);
        nodeTypeManager.registerNodeType(mixVotableNodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }
      if (log.isInfoEnabled()) {
        log.info("Change property '" + PUBLICATION_HISTORY + "' for node type 'publication:publication' successfully!");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when change property '" + PUBLICATION_HISTORY
            + "' for node type 'publication:publication'!", e);
      }
    } finally {
      sessionProvider.close();
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

}
