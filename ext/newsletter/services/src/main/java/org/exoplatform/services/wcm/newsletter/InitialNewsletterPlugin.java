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
package org.exoplatform.services.wcm.newsletter;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterCategoryHandler;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterSubscriptionHandler;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Aug 21, 2009
 */
public class InitialNewsletterPlugin extends CreatePortalPlugin {

  /** The category configs. */
  private List<NewsletterCategoryConfig> categoryConfigs;

  /** The subscription configs. */
  private List<NewsletterSubscriptionConfig> subscriptionConfigs;

  /** The manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The live portal manager service. */
  private LivePortalManagerService livePortalManagerService;

  /** The log. */
  private static Log log = ExoLogger.getLogger(InitialNewsletterPlugin.class);

  /**
   * Instantiates a new initial newsletter plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param newsletterManagerService the newsletter manager service
   * @param livePortalManagerService the live portal manager service
   */
  public InitialNewsletterPlugin(InitParams initParams,
                                 ConfigurationManager configurationManager,
                                 RepositoryService repositoryService,
                                 NewsletterManagerService newsletterManagerService,
                                 LivePortalManagerService livePortalManagerService) {
    super(initParams, configurationManager, repositoryService);

    categoryConfigs = initParams.getObjectParamValues(NewsletterCategoryConfig.class);
    subscriptionConfigs = initParams.getObjectParamValues(NewsletterSubscriptionConfig.class);
    this.livePortalManagerService = livePortalManagerService;
    this.newsletterManagerService = newsletterManagerService;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.artifacts.BasePortalArtifactsPlugin
   * #deployToPortal(java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    try {
      NewsletterCategoryHandler categoryHandler = newsletterManagerService.getCategoryHandler();
      for (NewsletterCategoryConfig categoryConfig : categoryConfigs) {
        categoryHandler.add(sessionProvider, portalName, categoryConfig);
      }

      NewsletterSubscriptionHandler subscriptionHandler = newsletterManagerService.getSubscriptionHandler();
      for (NewsletterSubscriptionConfig subscriptionConfig : subscriptionConfigs) {
        subscriptionHandler.add(sessionProvider, portalName, subscriptionConfig);
      }

      Node portalNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
      String userFolderPath = NewsletterConstant.generateUserPath(portalName);
      ExtendedNode userFolderNode = (ExtendedNode) ((Node) portalNode.getSession().getItem(userFolderPath)) ;
      if(userFolderNode.canAddMixin("exo:privilegeable"))
        userFolderNode.addMixin("exo:privilegeable");
      userFolderNode.setPermission("any", PermissionType.ALL) ;
    } catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info("InitialNewsletterPlugin fail because of ", e);
      }
    }
  }
}
