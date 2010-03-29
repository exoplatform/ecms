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
package org.exoplatform.services.wcm.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 20, 2008  
 */
public class NodeLocation {
  
  private static Log log = ExoLogger.getLogger("wcm:NodeLocation");
  
  private String repository;
  private String workspace;
  private String path;
  
  public NodeLocation() { }
  public NodeLocation(final String repository, final String workspace, final String path) {
    this.repository = repository;
    this.workspace = workspace;
    this.path = path; 
  }

  public String getRepository() { return repository; }
  public void setRepository(final String repository) { this.repository = repository; }

  public String getWorkspace() { return workspace; }
  public void setWorkspace(final String workspace) { this.workspace = workspace; }

  public String getPath() { return path; }
  public void setPath(final String path) { this.path = path; }

  public static final NodeLocation parse(final String exp) {
    String[] temp = exp.split("::");
    if (temp.length == 3 && temp[2].indexOf("/")>-1) {
      return new NodeLocation(temp[0], temp[1], temp[2]);
    }
    return null;
  }

  public static final NodeLocation make(final Node node) {
    if (node == null) return null;
    try {
      Session session = node.getSession();
      String repository = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
      String workspace = session.getWorkspace().getName();
      String path = node.getPath();
      return new NodeLocation(repository, workspace, path);
    } catch (RepositoryException e) {
      log.error("Exception in getNodeByLocation: ", e.fillInStackTrace());
    }
    return null;
  }

  public static Node getNodeByLocation(NodeLocation nodeLocation) {
    if (nodeLocation == null) return null; 
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      SessionProviderService sessionProviderService = (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);
      ManageableRepository repository = repositoryService.getRepository(nodeLocation.getRepository());
      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      Session session = sessionProvider.getSession(nodeLocation.getWorkspace(), repository);
      Node node = (Node)session.getItem(nodeLocation.getPath());
      return node;
    } catch (Exception e) {
      log.error("Exception in getNodeByLocation: ", e.fillInStackTrace());
    }
    return null;
  }
  
  public static final String serialize(final NodeLocation location) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(location.getRepository()).append("::")
    .append(location.getWorkspace()).append("::")
    .append(location.getPath());
    return buffer.toString();
  }
}
