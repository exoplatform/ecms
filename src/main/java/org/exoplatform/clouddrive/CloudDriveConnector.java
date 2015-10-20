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

import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Base class for {@link CloudDrive} implementations. It's eXo Container plugin to {@link CloudDriveService}.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveConnector.java 00000 Sep 13, 2012 pnedonosko $
 */
public abstract class CloudDriveConnector extends BaseComponentPlugin {

  /**
   * Predefined cloud provider configuration. Can be used for hosted providers that require point the service
   * host URL and related settings. Predefined configuration is designed for use in final connector
   * implementation. The configuration it is a set of objects describing the service, type of an object can be
   * any and should reflect the final provider needs.
   */
  public static class PredefinedServices {
    Set<Object> services = new LinkedHashSet<Object>();

    /**
     * Set of predefined services available for the provider.
     * 
     * @return the services
     */
    public Set<?> getServices() {
      return services;
    }

    /**
     * Initialize set of predefined services available for the provider.
     * 
     * @param services the services to set
     */
    public void setServices(Set<?> services) {
      this.services.addAll(services);
    }
  }

  public static final String               CONFIG_PROVIDER_NAME             = "provider-name";

  public static final String               CONFIG_PROVIDER_ID               = "provider-id";

  public static final String               CONFIG_CONNECTOR_HOST            = "connector-host";

  public static final String               CONFIG_CONNECTOR_SCHEMA          = "connector-schema";

  /**
   * OAuth2 client id.
   */
  public static final String               CONFIG_PROVIDER_CLIENT_ID        = "provider-client-id";

  /**
   * OAuth2 client secret.
   */
  public static final String               CONFIG_PROVIDER_CLIENT_SECRET    = "provider-client-secret";

  /**
   * Flag to disable a connector by configuration. 
   */
  public static final String               CONFIG_DISABLE                   = "disable";

  /**
   * Force SSO for user login. It is optional parameter for those providers that need force SSO explicitly
   * (e.g. Box).
   */
  public static final String               CONFIG_LOGIN_SSO                 = "login-sso";

  public static final String               CONFIG_PREDEFINED_SERVICES       = "predefined-services";

  public static final String               OAUTH2_CODE                      = "code";

  public static final String               OAUTH2_STATE                     = "state";

  public static final String               OAUTH2_ERROR                     = "error";

  public static final String               OAUTH2_ERROR_DESCRIPTION         = "error_description";

  // CLDINT-1051 increased from 3 to 5, later decreased to 3 again (due to closed JCR session in case of
  // retry)
  public static final int                  PROVIDER_REQUEST_ATTEMPTS        = 3;

  // CLDINT-1051 increased from 5s tp 10s
  public static final long                 PROVIDER_REQUEST_ATTEMPT_TIMEOUT = 10000;

  protected static final Log               LOG                              = ExoLogger.getLogger(CloudDriveConnector.class);

  protected final Map<String, String>      config;

  protected final SessionProviderService   sessionProviders;

  protected final RepositoryService        jcrService;

  protected final NodeFinder               jcrFinder;

  protected final CloudProvider            provider;

  protected final String                   connectorHost;

  protected final String                   connectorSchema;

  protected final boolean                  loginSSO;

  protected final PredefinedServices       predefinedServices;

  protected final ExtendedMimeTypeResolver mimeTypes;

  protected CloudDriveConnector(RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                NodeFinder jcrFinder,
                                ExtendedMimeTypeResolver mimeTypes,
                                InitParams params) throws ConfigurationException {

    this.sessionProviders = sessionProviders;
    this.jcrService = jcrService;
    this.jcrFinder = jcrFinder;
    this.mimeTypes = mimeTypes;

    PropertiesParam param = params.getPropertiesParam("drive-configuration");

    if (param != null) {
      config = Collections.unmodifiableMap(param.getProperties());
    } else {
      throw new ConfigurationException("Property parameters drive-configuration required.");
    }

    String connectorSchema = config.get(CONFIG_CONNECTOR_SCHEMA);
    if (connectorSchema == null || (connectorSchema = connectorSchema.trim()).length() == 0) {
      connectorSchema = "http";
    }
    this.connectorSchema = connectorSchema;

    String connectorHost = config.get(CONFIG_CONNECTOR_HOST);
    if (connectorHost != null && (connectorHost = connectorHost.trim()).length() > 0) {
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

    String loginSSOStr = config.get(CONFIG_LOGIN_SSO);
    this.loginSSO = loginSSOStr != null ? Boolean.parseBoolean(loginSSOStr.trim()) : false;

    ObjectParameter objParam = params.getObjectParam(CONFIG_PREDEFINED_SERVICES);
    if (objParam != null) {
      Object obj = objParam.getObject();
      if (obj != null) {
        this.predefinedServices = (PredefinedServices) obj;
      } else {
        LOG.warn("Predefined services configuration found but null object returned.");
        this.predefinedServices = new PredefinedServices();
      }
    } else {
      this.predefinedServices = new PredefinedServices();
    }

    this.provider = createProvider();
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

  protected boolean isDisabled() {
    String disableStr = config.get(CONFIG_DISABLE);
    return disableStr != null || disableStr.equals("true");
  }

  /**
   * For loading from local storage by {@link CloudDriveService}.
   * 
   * @param jcrRepository, {@link ManageableRepository}
   * @return {@link Set} of locally connected {@link CloudDrive}
   * @throws {@link CloudDriveException}
   */
  final Set<CloudDrive> loadStored(Set<Node> driveNodes) throws RepositoryException, CloudDriveException {
    Set<CloudDrive> connected = new HashSet<CloudDrive>();
    for (Node driveNode : driveNodes) {
      try {
        connected.add(loadDrive(driveNode));
      } catch (CloudProviderException e) {
        // skip drives with provider errors
        LOG.warn("Cannot load Cloud Drive associated with node " + driveNode.getPath() + " due to provider error. "
            + e.getMessage() + (e.getCause() != null ? ". " + e.getCause().getMessage() : "."), e);
      } catch (DriveTrashedException e) {
        // skip trashed drive and remove its node
        LOG.warn("Node trashed " + driveNode.getPath() + ", it cannot be loaded as Cloud Drive and will be removed. "
            + e.getMessage());
        try {
          // remove this node nasty if it is in the Trash
          Node parent = driveNode.getParent();
          driveNode.remove();
          parent.save();
        } catch (InvalidItemStateException iise) {
          // already removed
        } catch (Throwable re) {
          LOG.error("Error removing Cloud Drive node already marked as removed. " + e.getMessage(), e);
        }
      } catch (DriveRemovedException e) {
        // skip removed drive
        LOG.warn("Node removed " + driveNode.getPath() + " and cannot be loaded as Cloud Drive. " + e.getMessage());
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
   * Construct redirect link from configuration and runtime settings.
   * 
   * @return {@link String}
   */
  protected String redirectLink() {
    StringBuilder redirectURL = new StringBuilder();
    redirectURL.append(getConnectorSchema());
    redirectURL.append("://");
    redirectURL.append(getConnectorHost());
    redirectURL.append('/');
    redirectURL.append(PortalContainer.getCurrentPortalContainerName());
    redirectURL.append('/');
    redirectURL.append(PortalContainer.getCurrentRestContextName());
    redirectURL.append("/clouddrive/connect/");
    redirectURL.append(getProviderId());
    return redirectURL.toString();
  }

  protected String currentUser() {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      return convo.getIdentity().getUserId();
    } else {
      return null; // shouldn't happen
    }
  }

  /**
   * Create {@link CloudProvider}. Used internally by constructor.
   * 
   * @return {@link CloudProvider}
   * @throws ConfigurationException when cannot get initial parameters from runtime or configuration
   *           (including error of getting current repository name)
   */
  protected abstract CloudProvider createProvider() throws ConfigurationException;

  /**
   * Authenticate an user using parameters from OAuth2 redirect (including code, state, error,
   * error_description etc).
   * As result an instance of {@link CloudUser} will be returned, in case of fail an exception will be thrown
   * {@link CloudDriveException}.
   * 
   * @param params {@link Map}
   * @throws CloudDriveException
   * @return {@link CloudUser}
   */
  protected abstract CloudUser authenticate(Map<String, String> params) throws CloudDriveException;

  /**
   * Create Cloud Drive instance for given user. This instance will be connected to local storage
   * under existing {@link Node} <code>driveRoot</code> by {@link CloudDrive#connect()} method.
   * This node can be of any type, the creation procedure will add special nodetypes to it to allow
   * required properties and child nodes. Node will be actually saved by {@link CloudDrive#connect()} method.
   * <br>
   * To connect the drive use {@link CloudDriveService#connect(CloudUser, Node)}.
   * 
   * @param user {@link CloudUser} connecting user
   * @param driveRoot {@link Node} existing node what will be a root of the drive
   * @throws CloudDriveException if drive error happens
   * @throws RepositoryException if storage error happens
   * @return {@link CloudDrive} local Cloud Drive instance initialized to local JCR node.
   */
  protected abstract CloudDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException;

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
