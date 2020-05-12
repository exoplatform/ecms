/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives;

import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Feb 14, 2013 pnedonosko $
 */
public interface CloudDriveService {

  /**
   * Authenticate an user to given cloud provider using OAuth2 authorization
   * code.
   *
   * @param cloudProvider {@link CloudProvider} target provider
   * @param code {@link String} OAuth2 authorization code
   * @return {@link CloudUser} user instance
   * @throws ProviderNotAvailableException the provider not available exception
   * @throws CloudDriveException the cloud drive exception
   */
  CloudUser authenticate(CloudProvider cloudProvider, String code) throws ProviderNotAvailableException, CloudDriveException;

  /**
   * Authenticate an user to given cloud provider using OAuth2 parameters
   * (including code, state, error, error_description etc).
   *
   * @param cloudProvider {@link CloudProvider} target provider
   * @param params {@link Map} of data returned by OAuth2 authorization request
   *          (e.g. grabbed from the redirect request)
   * @return {@link CloudUser} user instance
   * @throws ProviderNotAvailableException the provider not available exception
   * @throws CloudDriveException the cloud drive exception
   */
  CloudUser authenticate(CloudProvider cloudProvider, Map<String, String> params) throws ProviderNotAvailableException,
                                                                                  CloudDriveException;

  /**
   * Create or open a local binding to Cloud Drive.
   *
   * @param user {@link CloudUser}
   * @param driveNode {@link Node}, existing node
   * @return instance of {@link CloudDrive}
   * @throws UserAlreadyConnectedException if user already connected to another
   *           node
   * @throws CannotConnectDriveException if node cannot be connected due to
   *           incompatible existing content in it
   * @throws ProviderNotAvailableException the provider not available exception
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   * @see CloudDrive#connect()
   */
  CloudDrive createDrive(CloudUser user, Node driveNode) throws UserAlreadyConnectedException,
                                                         CannotConnectDriveException,
                                                         ProviderNotAvailableException,
                                                         CloudDriveException,
                                                         RepositoryException;

  /**
   * Find {@link CloudDrive} instance connected to given {@link Node}. If drive
   * not found, if it exists but not connected to this node, or connected under
   * another user - the {@code null} will be returned.
   * 
   * @param node {@link Node}, user node
   * @return {@link CloudDrive} or {@code null} if given Node isn't connected to
   *         cloud user.
   * @throws RepositoryException if storage exception happened
   */
  CloudDrive findDrive(Node node) throws RepositoryException;

  /**
   * Find {@link CloudDrive} instance connected to {@link Node} pointed by given
   * workspace and path. If drive not found, if it exists but not connected to
   * this node, or connected under another user - the {@code null} will be
   * returned.
   * 
   * @param workspace {@link String} node workspace
   * @param path {@link String} node path
   * @return {@link CloudDrive} or {@code null} if node at given path not found
   *         or isn't connected to cloud user.
   * @throws RepositoryException if storage exception happened
   */
  CloudDrive findDrive(String workspace, String path) throws RepositoryException;

  /**
   * Find provider by given id.
   *
   * @param id the id
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
