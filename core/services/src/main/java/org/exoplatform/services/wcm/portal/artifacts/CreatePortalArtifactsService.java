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

import org.exoplatform.services.jcr.ext.common.SessionProvider;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 22, 2008
 */
public interface CreatePortalArtifactsService {

  /**
   * Adds the plugin.
   *
   * @param artifactsPlugin the artifacts plugin
   *
   * @throws Exception the exception
   */
  public void addPlugin(CreatePortalPlugin artifactsPlugin) throws Exception;

  /**
   * Deploy artifacts to portal.
   *
   * @param sessionProvider
   * @param portalName
   * @param portalTemplateName
   * @throws Exception
   */
  public void deployArtifactsToPortal(SessionProvider sessionProvider, String portalName, String portalTemplateName) throws Exception;

   /**
     * Return all artifacts plugins - list of CreatePortalPlugin
     *
     * @return the artifactPlugins
     */
  public HashMap<String, CreatePortalPlugin> getArtifactPlugins();
}
