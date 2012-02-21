/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.Session;

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
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 27, 2011  
 */
public class UpgradeVotingNodeTypePlugin extends UpgradeProductPlugin {

  private static final String VOTER_VOTEVALUE_PROP = "exo:voterVoteValues";
  private static final String MIX_VOTABLE = "mix:votable";

  private DMSConfiguration dmsConfiguration_;
  private RepositoryService repoService_;
  private Log log = ExoLogger.getLogger(this.getClass());
  

  public UpgradeVotingNodeTypePlugin(RepositoryService repoService, DMSConfiguration dmsConfiguration, 
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
      NodeTypeValue mixVotableNodeTypeValue = nodeTypeManager.getNodeTypeValue(MIX_VOTABLE);
      List<PropertyDefinitionValue> propertyDefinitionList = mixVotableNodeTypeValue.getDeclaredPropertyDefinitionValues();
      //check if exo:voterVoteValues already exists
      boolean propertyExists = false;
      for (PropertyDefinitionValue propertyDefinition : propertyDefinitionList) {
        if (propertyDefinition.getName().equals(VOTER_VOTEVALUE_PROP)) {
          propertyExists = true;
          break;
        }
      }
      //add new property
      if (!propertyExists) {
        propertyDefinitionList.add(new PropertyDefinitionValue(VOTER_VOTEVALUE_PROP, false, false, 1, false,
                                                    new ArrayList<String>(), true, PropertyType.STRING, new ArrayList<String>()));
        mixVotableNodeTypeValue.setDeclaredPropertyDefinitionValues(propertyDefinitionList);
        nodeTypeManager.registerNodeType(mixVotableNodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      }
      if (log.isInfoEnabled()) {
        log.info("Add new property '" + VOTER_VOTEVALUE_PROP + "' for node type 'mix:votable' successfully!");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when add new property '" + VOTER_VOTEVALUE_PROP
            + "' for node type 'mix:votable'!", e);
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
