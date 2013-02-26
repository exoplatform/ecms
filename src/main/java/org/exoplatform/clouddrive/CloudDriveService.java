/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Feb 14, 2013 pnedonosko $
 */
public interface CloudDriveService {

  /**
   * Authenticate an user to given cloud provider using OAuth2 key.
   * 
   * @param cloudProvider {@link CloudProvider} target provider
   * @param key {@link String} OAuth2 key
   * @return {@link CloudUser} user instance
   * @throws ProviderNotAvailableException
   * @throws CloudDriveException
   */
  CloudUser authenticate(CloudProvider cloudProvider, String key) throws ProviderNotAvailableException,
                                                                 CloudDriveException;

  /**
   * Create or open a local binding to Cloud Drive.
   * 
   * @see CloudDrive#connect()
   * @param user {@link CloudUser}
   * @param driveNode {@link Node}, existing node
   * @return instance of {@link CloudDrive}
   * @throws UserAlreadyConnectedException if user already connected to another node
   * @throws ProviderNotAvailableException
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  CloudDrive createDrive(CloudUser user, Node driveNode) throws UserAlreadyConnectedException,
                                                        ProviderNotAvailableException,
                                                        CloudDriveException,
                                                        RepositoryException;

  /**
   * Find {@link CloudDrive} instance connected to given {@link Node}. If drive not found, if it exists but
   * not connected to this node, or connected under another user - the {@code null} will be returned. The
   * {@code null} also will be returned if no {@link ConversationState} set in caller thread or its identity
   * differs from the user id of given node's session.
   * 
   * @param node {@link Node}, user node
   * @return {@link CloudDrive} or {@code null} if given Node isn't connected to cloud user or .
   * @throws RepositoryException if storage exception happened
   */
  CloudDrive findDrive(Node node) throws RepositoryException;

  /**
   * Tells whether given node denotes some {@link CloudDrive}. Some {@link Node} denote a cloud drive if such
   * drive connected to this node (node is a storage of this drive).
   * 
   * @param node {@link Node}
   * @return boolean, {@code true} if given node is a cloud drive
   * @throws RepositoryException
   */
  boolean isDrive(Node node) throws RepositoryException;

  /**
   * Find provider by given id.
   * 
   * @param String id
   * @return CloudProvider
   * @throws ProviderNotAvailableException if no such provider with given id
   */
  CloudProvider getProvider(String id) throws ProviderNotAvailableException;

  /**
   * Set of available providers.
   * 
   * @return List of {@link CloudProvider}
   */
  Set<CloudProvider> getProviders();
}
