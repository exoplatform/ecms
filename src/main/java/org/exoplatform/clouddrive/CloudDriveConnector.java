/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Base class for {@link CloudDrive} implementations. It's eXo Container plugin to {@link CloudDriveService}.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveConnector.java 00000 Sep 13, 2012 pnedonosko $
 */
public abstract class CloudDriveConnector extends BaseComponentPlugin {

  public static final String             CONFIG_PROVIDER_NAME             = "provider-name";

  public static final String             CONFIG_PROVIDER_ID               = "provider-id";

  public static final String             CONFIG_CONNECTOR_HOST            = "connector-host";

  public static final String             CONFIG_CONNECTOR_SCHEMA          = "connector-schema";

  /**
   * OAuth2 client id.
   */
  public static final String             CONFIG_PROVIDER_CLIENT_ID        = "provider-client-id";

  /**
   * OAuth2 client secret.
   */
  public static final String             CONFIG_PROVIDER_CLIENT_SECRET    = "provider-client-secret";

  // CLDINT-1051 increased from 3 to 5
  public static final int                PROVIDER_REQUEST_ATTEMPTS        = 5;

  // CLDINT-1051 increased from 5s tp 10s
  public static final long               PROVIDER_REQUEST_ATTEMPT_TIMEOUT = 10000;

  protected static final Log             LOG                              = ExoLogger.getLogger(CloudDriveConnector.class);

  protected final Map<String, String>    config;

  protected final SessionProviderService sessionProviders;

  protected final RepositoryService      jcrService;

  protected final CloudProvider          provider;

  protected final String                 connectorHost;

  protected final String                 connectorSchema;

  protected CloudDriveConnector(RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                InitParams params) throws ConfigurationException {

    this.sessionProviders = sessionProviders;
    this.jcrService = jcrService;

    PropertiesParam param = params.getPropertiesParam("drive-configuration");

    if (param != null) {
      config = Collections.unmodifiableMap(param.getProperties());
    } else {
      throw new ConfigurationException("Property parameters drive-configuration required.");
    }

    String connectorSchema = config.get(CONFIG_CONNECTOR_SCHEMA);
    if (connectorSchema == null || connectorSchema.trim().length() == 0) {
      connectorSchema = "http";
    }
    this.connectorSchema = connectorSchema;

    String connectorHost = config.get(CONFIG_CONNECTOR_HOST);
    if (connectorHost != null && connectorHost.length() > 0) {
      this.connectorHost = connectorHost;
    } else {
      try {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (connectorHost == null && interfaces.hasMoreElements()) {
          NetworkInterface nic = interfaces.nextElement();
          Enumeration<InetAddress> addresses = nic.getInetAddresses();
          while (connectorHost == null && addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (!address.isLoopbackAddress()) {
              connectorHost = address.getHostName();
            }
          }
        }
      } catch (SocketException e) {
        // cannot get net interfaces
      }

      if (connectorHost == null) {
        try {
          connectorHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
          connectorHost = "localhost";
        }
      }

      this.connectorHost = connectorHost;
      LOG.warn("Configuration of " + CONFIG_CONNECTOR_HOST + " is not set, will use " + connectorHost);
    }

    this.provider = createProvider();

    // Below examples of plugin can be configured
    /*
     * Iterator<ValuesParam> vparams = params.getValuesParamIterator(); while (vparams.hasNext()) {
     * ValuesParam nodeTypeParam = vparams.next(); nodeTypes.put(nodeTypeParam.getName(),
     * nodeTypeParam.getValues()); } PropertiesParam param =
     * params.getPropertiesParam("namespaces"); if (param != null) { namespaces =
     * param.getProperties(); } if (params != null) { ValueParam valueParam =
     * params.getValueParam("repository-name"); if (valueParam != null) { repositoryName =
     * valueParam.getValue(); } valueParam = params.getValueParam("workspaces"); if (valueParam !=
     * null) { workspaces = valueParam.getValue(); } valueParam =
     * params.getValueParam("component-class-name"); if (valueParam != null) { listenerClassName =
     * valueParam.getValue(); } }
     */
  }

  protected SessionProvider sessionProvider() throws RepositoryException {
    return sessionProviders.getSessionProvider(null);
  }

  protected String getConnectorHost() {
    return connectorHost;
  }

  protected String getConnectorSchema() {
    return connectorSchema;
  }

  protected String getProviderName() {
    return config.get(CONFIG_PROVIDER_NAME);
  }

  protected String getProviderId() {
    return config.get(CONFIG_PROVIDER_ID);
  }

  protected String getClientId() {
    return config.get(CONFIG_PROVIDER_CLIENT_ID);
  }

  protected String getClientSecret() {
    return config.get(CONFIG_PROVIDER_CLIENT_SECRET);
  }

  /**
   * For loading from local storage by {@link CloudDriveService}.
   * 
   * @param jcrRepository, {@link ManageableRepository}
   * @return {@link Set} of locally connected {@link CloudDrive}
   * @throws {@link CloudDriveException}
   */
  Set<CloudDrive> loadStored(Set<Node> driveNodes) throws RepositoryException, CloudDriveException {
    Set<CloudDrive> connected = new HashSet<CloudDrive>();
    for (Node driveNode : driveNodes) {
      try {
        connected.add(loadDrive(driveNode));
      } catch (DriveRemovedException e) {
        // skip removed drive
        LOG.warn("Node removed and cannot be loaded as Cloud Drive: " + e.getMessage());
      }
    }
    return connected;
  }

  /**
   * Return provider of this drive implementation.
   * 
   * @return {@link CloudProvider}
   */
  protected CloudProvider getProvider() {
    return provider;
  }

  /**
   * Create {@link CloudProvider}. Used internally by constructor.
   * 
   * @return {@link CloudProvider}
   */
  protected abstract CloudProvider createProvider();

  /**
   * Authenticated an user by an access code from its cloud provider (OAuth usecase). As result an
   * instance of {@link CloudUser} will be returned, in case of fail an exception will be thrown
   * {@link CloudDriveException}.
   * 
   * @param code {@link String}
   * @throws CloudDriveException
   * @return {@link CloudUser}
   */
  protected abstract CloudUser authenticate(String code) throws CloudDriveException;

  /**
   * Create Cloud Drive instance for given user. This instance will be connected to local storage
   * under existing {@link Node} <code>driveRoot</code> by {@link CloudDrive#connect()} method.
   * This node can be of any type, the creation procedure will add special nodetypes to it to allow
   * required properties and child nodes. Node will be actually saved by {@link CloudDrive#connect()} method. <br>
   * To connect the drive use {@link CloudDriveService#connect(CloudUser, Node)}.
   * 
   * @param user {@link CloudUser} connecting user
   * @param driveRoot {@link Node} existing node what will be a root of the drive
   * @throws CloudDriveException if drive error happens
   * @throws RepositoryException if storage error happens
   * @return {@link CloudDrive} local Cloud Drive instance initialized to local JCR node.
   */
  protected abstract CloudDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException,
                                                                           RepositoryException;

  /**
   * Load Cloud Drive from local storage under existing {@link Node} <code>driveRoot</code>.
   * 
   * @param driveRoot {@link Node} existing node pointing the root of the drive
   * @return {@link CloudDrive} local Cloud Drive instance connected to local JCR node.
   * @throws CloudDriveException if drive error happens
   * @throws RepositoryException if storage error happens
   */
  protected abstract CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException;

}
