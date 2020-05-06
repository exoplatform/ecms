/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.cometd;

import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The CometdInfo class is used to pass necessary cometd information to a
 * client.
 */
public class CometdConfig {

  /** The path. */
  private final String path;

  /** The token. */
  private final String token;

  /** The container name. */
  private final String containerName;
  
  private final String provider;
  
  private final String workspace;

  /**
   * Instantiates CometdConfig.
   * @param path the path
   * @param token the token
   * @param containerName the containerName
   */
  public CometdConfig(String path, String token, String containerName) {
    super();
    this.token = token;
    this.path = path;
    this.containerName = containerName;
    this.provider = null;
    this.workspace = null;
  }
  
  /**
   * Instantiates CometdConfig.
   * @param path the path
   * @param token the token
   * @param containerName the containerName
   * @param provider the provider
   * @param workspace the workspace
   */
  public CometdConfig(String path, String token, String containerName, String provider, String workspace) {
    super();
    this.token = token;
    this.path = path;
    this.containerName = containerName;
    this.provider = provider;
    this.workspace = workspace;
  }

  /**
   * Gets the token.
   *
   * @return the cometd token
   */
  public String getToken() {
    return token;
  }

  /**
   * Gets the path.
   *
   * @return the cometdPath
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the container name.
   *
   * @return the container
   */
  public String getContainerName() {
    return containerName;
  }
  
  
  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
  }

  
  /**
   * Gets the workspace.
   *
   * @return the provider
   */
  public String getWorkspace() {
    return workspace;
  }
  /**
   * To JSON.
   *
   * @return the string
   * @throws JsonException the json exception
   */
  public String toJSON() throws JsonException {
    return new JsonGeneratorImpl().createJsonObject(this).toString();
  }
}
