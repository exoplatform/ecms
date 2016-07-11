/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;

import java.util.List;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public interface DocumentService {
  public Document findDocById(String id) throws RepositoryException;

  /**
   * Get the link to display a document in the Documents app.
   * It will try to get the best matching context (personal doc, space doc, ...).
   * @param nodePath The path of the node
   * @return The link to open the document
   * @throws Exception
   */
  public String getLinkInDocumentsApp(String nodePath) throws Exception;

  /**
   * Get the link to display a document in the Documents app in the given drive.
   * It will try to get the best matching context (personal doc, space doc, ...).
   * @param nodePath The path of the node
   * @param driveName The drive to use
   * @return The link to open the document
   * @throws Exception
   */
  public String getLinkInDocumentsApp(String nodePath, String driveName) throws Exception;

  /**
   * Get the drive containing the node with the given node path, for the current user.
   * If several drives contain the node, try to find the best matching.
   * @param nodePath The path of the node
   * @return The drive containing the node
   * @throws Exception
   */
  DriveData getDriveOfNode(String nodePath) throws Exception;

  /**
   * Get the drive containing the node with the given node path.
   * If several drives contain the node, try to find the best matching.
   * @param nodePath The path of the node
   * @param userId The user id
   * @param memberships The user memberships
   * @return The drive containing the node
   * @throws Exception
   */
  public DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception;
}
