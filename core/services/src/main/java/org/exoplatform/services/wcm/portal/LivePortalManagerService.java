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
package org.exoplatform.services.wcm.portal;

import java.util.Collection;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 19, 2008
 */
/**
 * The Interface LivePortalManagerService.
 */
public interface LivePortalManagerService {

  /**
   * Gets the live shared portal on current repository.
   *
   * @param sessionProvider the session provider
   * @return the live shared portal
   * @throws Exception the exception
   */
  public Node getLiveSharedPortal(final SessionProvider sessionProvider) throws Exception;

  /**
   * Gets the all live portals path.
   *
   * @return the live portals path
   * @throws Exception the exception
   */
  public String getPortalNameByPath(String portalPath) throws Exception;
  public String getPortalPathByName(String portalName) throws Exception;

  public Collection<String> getLivePortalsPath() throws Exception;

  /**
   * Gets the all live portals on current repository.
   *
   * @param sessionProvider the session provider
   * @return the live portals
   * @throws Exception the exception
   */
  public List<Node> getLivePortals(final SessionProvider sessionProvider) throws Exception;

  /**
   * Gets the live portal by name.
   *
   * @param portalName the portal name
   * @param sessionProvider the session provider
   * @return the live portal
   * @throws Exception the exception
   */
  public Node getLivePortal(final SessionProvider sessionProvider, final String portalName) throws Exception;

  /**
   * Gets the live shared portal on specific repository.
   *
   * @param repository the repository
   * @param sessionProvider the session provider
   * @return the live shared portal
   * @throws Exception the exception
   */
  public Node getLiveSharedPortal(final SessionProvider sessionProvider, final String repository) throws Exception;

  public Node getLivePortalByChild(Node childNode) throws Exception;
  /**
   * Gets the live portals on specific repository.
   *
   * @param repository the repository
   * @param sessionProvider the session provider
   * @return the live portals
   * @throws Exception the exception
   */
  public List<Node> getLivePortals(final SessionProvider sessionProvider, final String repository) throws Exception;

  /**
   * Gets the live portal on specific repository.
   *
   * @param repository the repository
   * @param portalName the portal name
   * @param sessionProvider the session provider
   * @return the live portal
   * @throws Exception the exception
   */
  public Node getLivePortal(final SessionProvider sessionProvider,
                            final String repository,
                            final String portalName) throws Exception;

  /**
   * Adds the live portal on current repository.
   *
   * @param portalConfig the portal config
   * @param sessionProvider the session provider
   * @throws Exception the exception
   */
  public void addLivePortal(final SessionProvider sessionProvider, final PortalConfig portalConfig) throws Exception;

  /**
   * Removes the live portal on current repository.
   *
   * @param portalConfig the portal config
   * @param sessionProvider the session provider
   * @throws Exception the exception
   */
  public void removeLivePortal(final SessionProvider sessionProvider, final PortalConfig portalConfig) throws Exception;
}
