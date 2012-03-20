/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.portal.artifacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.phamvu@exoplatform.com
 * Oct 22, 2008
 */
public class CreatePortalArtifactsServiceImpl implements CreatePortalArtifactsService {

  public static final String CREATE_PORTAL_EVENT = "PortalArtifactsInitializerServiceImpl.portal.onCreate";
  private HashMap<String,CreatePortalPlugin> artifactPlugins = new LinkedHashMap<String,CreatePortalPlugin>();
  private ArrayList<String> initialPortals = new ArrayList<String>();
  private ListenerService listenerService;

  @SuppressWarnings("unchecked")
  public CreatePortalArtifactsServiceImpl(ListenerService listenerService) {
    this.listenerService = listenerService;
  }
  public void addPlugin(CreatePortalPlugin artifactsPlugin) throws Exception {
    artifactPlugins.put(artifactsPlugin.getName(),artifactsPlugin);
  }
  
  public void addIgnorePortalPlugin(IgnorePortalPlugin ignorePortalPlugin) throws Exception {
    ArrayList<String> ignoredPortals = ignorePortalPlugin.getIgnorePortals();
    if (ignoredPortals != null && !ignoredPortals.isEmpty()) {
      initialPortals.addAll(ignoredPortals);
    }
  }

  @Deprecated
  public void deployArtifactsToPortal(SessionProvider sessionProvider, String portalName)
  throws Exception {
    //Do not initalize portal artifact for predefined portal
    if(initialPortals.contains(portalName)) return ;

    for(CreatePortalPlugin plugin: artifactPlugins.values()) {
      plugin.deployToPortal(sessionProvider, portalName);
    }

    listenerService.broadcast(CREATE_PORTAL_EVENT, portalName, sessionProvider);
  }
  
  public void deployArtifactsToPortal(SessionProvider sessionProvider, String portalName, String portalTemplateName) throws Exception {
    //Do not initalize portal artifact for predefined portal
    if(initialPortals.contains(portalName)) return;

    for (CreatePortalPlugin plugin : artifactPlugins.values()) {
      if (!plugin.getName().startsWith("template") || (portalTemplateName != null && plugin.getName().startsWith(portalTemplateName))) {
        plugin.deployToPortal(sessionProvider, portalName);
      }
    }
    
    listenerService.broadcast(CREATE_PORTAL_EVENT, portalName, sessionProvider);
  }

}
