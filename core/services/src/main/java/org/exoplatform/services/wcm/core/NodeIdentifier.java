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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.core.ManageableRepository;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
public class NodeIdentifier {

  private String repository;
  private String workspace;
  private String uuid;

  public NodeIdentifier(final String repository, final String workspace, final String uuid) {
    this.repository = repository;
    this.workspace = workspace;
    this.uuid = uuid;
  }

  public String getRepository() { return repository; }
  public void setRepository(final String repository) { this.repository = repository; }

  public String getWorkspace() { return workspace; }
  public void setWorkspace(final String workspace) { this.workspace = workspace; }

  public String getUUID() {  return uuid; }
  public void setUUID(final String uuid) {  this.uuid = uuid; }

  public final static NodeIdentifier parse(final String expr) {
    String[] temp = expr.split("::");
    if (temp.length == 3 && StringUtils.isNumeric(temp[2])) {
      return new NodeIdentifier(temp[0], temp[1], temp[2]);
    }
    return null;
  }

  public final static NodeIdentifier make(final Node node) throws RepositoryException {
    Session session = node.getSession();
    String repository = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
    String workspace = session.getWorkspace().getName();
    String uuid = node.getUUID();
    return new NodeIdentifier(repository, workspace, uuid);
  }

  public final static String serialize(final NodeIdentifier identifier) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(identifier.getRepository()).append("::")
    .append(identifier.getWorkspace()).append("::")
    .append(identifier.getUUID());
    return buffer.toString();
  }
}
