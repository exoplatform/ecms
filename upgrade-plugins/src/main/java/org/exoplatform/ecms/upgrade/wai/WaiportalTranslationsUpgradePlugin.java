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
package org.exoplatform.ecms.upgrade.wai;

import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.i18n.impl.AddTranslationPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalArtifactsService;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform dongpd@exoplatform.com
 * Oct 15, 2012
 */
public class WaiportalTranslationsUpgradePlugin extends UpgradeProductPlugin {

  private static final Log             LOG = ExoLogger.getLogger(WaiportalTranslationsUpgradePlugin.class.getName());

  private CreatePortalArtifactsService createPortalArtifactsService;

  public WaiportalTranslationsUpgradePlugin(InitParams initParams,
                                            CreatePortalArtifactsService createPortalArtifactsService) {
    super(initParams);
    this.createPortalArtifactsService = createPortalArtifactsService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    try {
      List<Node> livePortals = livePortalManagerService.getLivePortals(sessionProvider);
      HashMap<String, CreatePortalPlugin> artifactPlugins = createPortalArtifactsService.getArtifactPlugins();
      for (CreatePortalPlugin plugin : artifactPlugins.values()) {
        if (plugin instanceof AddTranslationPlugin
            && plugin.getName().equals("template-WAIPortal-translation")) {
          for (Node node : livePortals) {
            plugin.deployToPortal(sessionProvider, node.getName());
          }
          break;
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Finish " + this.getClass().getName() + ".............");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when upgrade WAI portal translations", e);
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}
