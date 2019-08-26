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
package org.exoplatform.clouddrive;

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
import javax.jcr.Session;

import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.ExoContainer;
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

/**
 * Base class for {@link CloudDrive} implementations. It's eXo Container plugin to {@link CloudDriveService}.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveConnector.java 00000 Sep 13, 2012 pnedonosko $
 */
public abstract class CloudDriveConnector extends BaseComponentPlugin {

  /**
   * Predefined cloud provider configuration. Can be used for hosted providers that require point the service host URL and related
   * settings. Predefined configuration is designed for use in final connector implementation. The configuration it is a set of
   * objects describing the service, type of an object can be any and should reflect the final provider needs.
   */
  public static class PredefinedServices {

    /** The services. */
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

  /** The Constant CONFIG_PROVIDER_NAME. */
  public static final String               CONFIG_PROVIDER_NAME             = "provider-name";

  /** The Constant CONFIG_PROVIDER_ID. */
  public static final String               CONFIG_PROVIDER_ID               = "provider-id";

  /** The Constant CONFIG_CONNECTOR_HOST. */
  public static final String               CONFIG_CONNECTOR_HOST            = "connector-host";

  /** The Constant CONFIG_CONNECTOR_SCHEMA. */
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
   * Force SSO for user login. It is optional parameter for those providers that need force SSO explicitly (e.g. Box).
   */
  public static final String               CONFIG_LOGIN_SSO                 = "login-sso";

  /** The Constant CONFIG_PREDEFINED_SERVICES. */
  public static final String               CONFIG_PREDEFINED_SERVICES       = "predefined-services";

  /** The Constant OAUTH2_CODE. */
  public static final String               OAUTH2_CODE                      = "code";

  /** The Constant OAUTH2_STATE. */
  public static final String               OAUTH2_STATE                     = "state";

  /** The Constant OAUTH2_ERROR. */
  public static final String               OAUTH2_ERROR                     = "error";

  /** The Constant OAUTH2_ERROR_DESCRIPTION. */
  public static final String               OAUTH2_ERROR_DESCRIPTION         = "error_description";

  /**
   * The Constant PROVIDER_REQUEST_ATTEMPTS. CLDINT-1051 increased from 3 to 5, later decreased to 3 again (due to closed JCR
   * session in case of retry)
   */
  public static final int                  PROVIDER_REQUEST_ATTEMPTS        = 3;

  /** CLDINT-1051 increased from 5s tp 10s. */
  public static final long                 PROVIDER_REQUEST_ATTEMPT_TIMEOUT = 10000;

  /** The Constant LOG. */
  protected static final Log               LOG                              = ExoLogger.getLogger(CloudDriveConnector.class);

  /** The config. */
  protected final Map<String, String>      config;

  /** The session providers. */
  protected final SessionProviderService   sessionProviders;

  /** The jcr service. */
  protected final RepositoryService        jcrService;

  /** The jcr finder. */
  protected final NodeFinder               jcrFinder;

  /** The provider. */
  protected final CloudProvider            provider;

  /** The connector host. */
  protected final String                   connectorHost;

  /** The connector schema. */
  protected final String                   connectorSchema;

  /** The login SSO. */
  protected final boolean                  loginSSO;

  /** The predefined services. */
  protected final PredefinedServices       predefinedServices;

  /** The mime types. */
  protected final ExtendedMimeTypeResolver mimeTypes;

  /**
   * Instantiates a new cloud drive connector.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param jcrFinder the jcr finder
   * @param mimeTypes the mime types
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  protected CloudDriveConnector(RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                NodeFinder jcrFinder,
                                ExtendedMimeTypeResolver mimeTypes,
                                InitParams params)
      throws ConfigurationException {

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

  /**
   * Session provider.
   *
   * @return the session provider
   * @throws RepositoryException the repository exception
   */
  protected SessionProvider sessionProvider() throws RepositoryException {
    return sessionProviders.getSessionProvider(null);
  }

  /**
   * Gets the connector host.
   *
   * @return the connector host
   */
  protected String getConnectorHost() {
    return connectorHost;
  }

  /**
   * Gets the connector schema.
   *
   * @return the connector schema
   */
  protected String getConnectorSchema() {
    return connectorSchema;
  }

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  protected String getProviderName() {
    return config.get(CONFIG_PROVIDER_NAME);
  }

  /**
   * Gets the provider id.
   *
   * @return the provider id
   */
  protected String getProviderId() {
    return config.get(CONFIG_PROVIDER_ID);
  }

  /**
   * Gets the client id.
   *
   * @return the client id
   */
  protected String getClientId() {
    return config.get(CONFIG_PROVIDER_CLIENT_ID);
  }

  /**
   * Gets the client secret.
   *
   * @return the client secret
   */
  protected String getClientSecret() {
    return config.get(CONFIG_PROVIDER_CLIENT_SECRET);
  }

  /**
   * Checks if is disabled.
   *
   * @return true, if is disabled
   */
  protected boolean isDisabled() {
    String disableStr = config.get(CONFIG_DISABLE);
    return disableStr != null && disableStr.equals("true");
  }

  /**
   * For loading from local storage by {@link CloudDriveService}.
   *
   * @param driveNodes collection of {@link Node} object
   * @return {@link Set} of locally connected {@link CloudDrive}
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
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

        // Remove the node in another thread
        final String nodeUUID = driveNode.getUUID();
        final String nodePath = driveNode.getPath();
        final String workspaceName = driveNode.getSession().getWorkspace().getName();
        final String repositoryName =
                                    ((ManageableRepository) driveNode.getSession().getRepository()).getConfiguration().getName();
        ThreadExecutor.getInstance().submit(new ContainerCommand(PortalContainer.getCurrentPortalContainerName()) {
          @Override
          void onContainerError(String error) {
            LOG.error("An error has occured in container {}: {}", containerName, error);
          }

          @Override
          void execute(ExoContainer exoContainer) {
            SessionProvider ssp = sessionProviders.getSystemSessionProvider(null);
            if (ssp != null) {
              try {
                Thread.sleep(30 * 1000); // we want wait a bit, to let the Platform start fully
                Session session = ssp.getSession(workspaceName, jcrService.getRepository(repositoryName));
                Node driveRoot = session.getNodeByUUID(nodeUUID);
                // remove this node nasty if it is in the Trash
                Node parent = driveRoot.getParent();
                driveNode.remove();
                parent.save();
              } catch (InvalidItemStateException iise) {
                // already removed
              } catch (Throwable re) {
                LOG.error("Error removing Cloud Drive node already marked as removed. ", e.getMessage(), re);
              }
            } else {
              LOG.warn("Cannot obtain JCR session provider for removal of Cloud Drive node already marked as removed {}",
                       nodePath);
            }
          }
        });
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

  /**
   * Current user.
   *
   * @return the string
   */
  protected String currentUser() {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      return convo.getIdentity().getUserId();
    } else {
      return null; // shouldn't happen for authenticated users
    }
  }

  /**
   * Create {@link CloudProvider}. Used internally by constructor.
   * 
   * @return {@link CloudProvider}
   * @throws ConfigurationException when cannot get initial parameters from runtime or configuration (including error of getting
   *           current repository name)
   */
  protected abstract CloudProvider createProvider() throws ConfigurationException;

  /**
   * Authenticate an user using parameters from OAuth2 redirect (including code, state, error, error_description etc). As result
   * an instance of {@link CloudUser} will be returned, in case of fail an exception will be thrown {@link CloudDriveException}.
   *
   * @param params {@link Map}
   * @return {@link CloudUser}
   * @throws CloudDriveException the cloud drive exception
   */
  protected abstract CloudUser authenticate(Map<String, String> params) throws CloudDriveException;

  /**
   * Create Cloud Drive instance for given user. This instance will be connected to local storage under existing {@link Node}
   * <code>driveRoot</code> by {@link CloudDrive#connect()} method. This node can be of any type, the creation procedure will add
   * special nodetypes to it to allow CLoud Drive specifics.<br>
   * To connect the drive use {@link CloudDrive#connect()}. Node will be actually saved by {@link CloudDrive#connect()} method.
   *
   * @param user {@link CloudUser} connecting user
   * @param driveNode the drive node
   * @return {@link CloudDrive} local Cloud Drive instance initialized to local JCR node.
   * @throws CloudDriveException if drive error happens
   * @throws RepositoryException if storage error happens
   */
  protected abstract CloudDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException;

  /**
   * Load Cloud Drive from local storage under existing {@link Node} <code>driveRoot</code>.
   *
   * @param driveNode the drive node
   * @return {@link CloudDrive} local Cloud Drive instance connected to local JCR node.
   * @throws CloudDriveException if drive error happens
   * @throws RepositoryException if storage error happens
   */
  protected abstract CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException;

}
