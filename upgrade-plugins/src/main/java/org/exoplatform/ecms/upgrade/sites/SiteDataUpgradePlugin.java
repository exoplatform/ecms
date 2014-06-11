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
package org.exoplatform.ecms.upgrade.sites;

import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 10, 2014
 * This class upgrades child nodes of all site nodes, adding mixin types 'exo:modify'
 * and 'exo:sortable' to them. 
 */
public class SiteDataUpgradePlugin extends UpgradeProductPlugin {

  private static final Log log = ExoLogger.getLogger(SiteDataUpgradePlugin.class.getName());

  private static final String[] childNodePaths = new String[] {
    "js", "css", "medias", "medias/images", "medias/videos", "medias/audio",
    "documents", "web contents", "web contents/site artifacts", "links", "categories",
    "ApplicationData", "ApplicationData/NewsletterApplication", "ApplicationData/DefaultTemplates",
    "ApplicationData/Categories", "ApplicationData/Users" };

  
  private WCMConfigurationService wcmConfigService;
  
  public SiteDataUpgradePlugin(WCMConfigurationService wcmConfigService, InitParams initParams) {
    super(initParams);
    this.wcmConfigService = wcmConfigService;
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    try {
      for (NodeLocation rootSiteLocation : wcmConfigService.getAllLivePortalsLocation()) {
        try {
          if (log.isInfoEnabled()) {
            log.info("Migrating data for site: " + rootSiteLocation.getPath());
          }
          rootSiteLocation.setSystemSession(true);
          Node rootSiteNode = NodeLocation.getNodeByLocation(rootSiteLocation);
          for (NodeIterator iter = rootSiteNode.getNodes(); iter.hasNext();) {
            Node siteNode = iter.nextNode();
            for (String childPath : childNodePaths) {
              try {
                if (siteNode.hasNode(childPath)) {
                  Node node = siteNode.getNode(childPath);
                  if (log.isInfoEnabled()) {
                    log.info("Migrating data for site: " + node.getPath());
                  }
                  addMixin(node, NodetypeConstant.EXO_MODIFY);
                  addMixin(node, NodetypeConstant.EXO_SORTABLE);
                  
                  if (!node.hasProperty(NodetypeConstant.EXO_NAME)) {
                    node.setProperty(NodetypeConstant.EXO_NAME, node.getName());
                    
                    node.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, new GregorianCalendar());
                    
                    ConversationState conversationState = ConversationState.getCurrent();
                    String userName = (conversationState == null) ? node.getSession().getUserID() :
                                                                    conversationState.getIdentity().getUserId();
                    node.setProperty(NodetypeConstant.EXO_LAST_MODIFIER, userName);
                  }
                }
              } catch (Exception e) {
                if (log.isErrorEnabled()) {
                  log.error("An unexpected error occurs when migrating site data", e);        
                }
              }
            }
          }
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("An unexpected error occurs when migrating site " + rootSiteLocation.getPath(), e);        
          }
        }
      } 
      if (log.isInfoEnabled()) {
        log.info(this.getClass().getName() + " finished successfully");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating site data", e);        
      }
    }
  }
  
  protected void addMixin(Node node, String mixin) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    if (!node.isNodeType(mixin)) node.addMixin(mixin);
  }

}
