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

import java.util.HashMap;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.phamvu@exoplatform.com
 * Oct 22, 2008
 */
public class RemovePortalArtifactsServiceImpl implements RemovePortalArtifactsService {

  private HashMap<String,RemovePortalPlugin> artifactPlugins = new HashMap<String,RemovePortalPlugin>();

  public void addPlugin(RemovePortalPlugin artifactsPlugin) throws Exception {
    artifactPlugins.put(artifactsPlugin.getName(),artifactsPlugin);
  }

  public void invalidateArtifactsFromPortal(SessionProvider sessionProvider, String portalName)
  throws Exception {
    for(RemovePortalPlugin plugin: artifactPlugins.values()) {
      plugin.invalidateFromPortal(sessionProvider, portalName);
    }
  }

}
