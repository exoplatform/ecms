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

import org.exoplatform.clouddrive.features.CloudDriveFeatures;
import org.exoplatform.clouddrive.features.PermissiveFeatures;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NtFileSynchronizer;
import org.exoplatform.clouddrive.utils.IdentityHelper;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.picocontainer.Startable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * Service implementing {@link CloudDriveService} and {@link Startable}.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Sep 7, 2012 pnedonosko $
 */
public class CloudDriveServiceImpl implements CloudDriveService, Startable {

  /**
   * Listener for disconnects and removals of local drives made not via {@link CloudDriveService} (i.e. by
   * move the drive's node to Trash and following removal from the JCR).
   */
  class LocalDrivesListener extends BaseCloudDriveListener {

    void cleanUserCaches(CloudUser user) {
      Map<CloudUser, CloudDrive> drives = userDrives.get(user);
      if (drives != null) {
        drives.remove(user);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemove(CloudDriveEvent event) {
      cleanUserCaches(event.getUser());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect(CloudDriveEvent event) {
      // XXX don't do this on disconnect // cleanUserCaches(event.getUser());
    }

    /**
     * @inherritDoc
     */
    @Override
    public void onError(CloudDriveEvent event, Throwable error, String operationName) {
      // XXX don't do this on error // cleanUserCaches(event.getUser());
    }
  }

  protected static final Log                                 LOG               = ExoLogger.getLogger(CloudDriveService.class);

  protected final RepositoryService                          jcrService;

  protected final SessionProviderService                     sessionProviders;

  /**
   * Registered CloudDrive connectors.
   */
  protected final Map<CloudProvider, CloudDriveConnector>    connectors        = new LinkedHashMap<CloudProvider, CloudDriveConnector>();

  /**
   * In-memory multiton for drives created per repository and per user. Only connected drives here.
   */
  protected final Map<String, Map<CloudUser, CloudDrive>>    repositoryDrives  = new ConcurrentHashMap<String, Map<CloudUser, CloudDrive>>();

  /**
   * User-in-repositoryDrives reference map for unregistration of disconnected and removed drives (via
   * {@link LocalDrivesListener}).
   * 
   * @see #repositoryDrives
   */
  protected final Map<CloudUser, Map<CloudUser, CloudDrive>> userDrives        = new ConcurrentHashMap<CloudUser, Map<CloudUser, CloudDrive>>();

  protected final Set<CloudDriveListener>                    drivesListeners   = new LinkedHashSet<CloudDriveListener>();

  protected final Set<CloudFileSynchronizer>                 fileSynchronizers = new LinkedHashSet<CloudFileSynchronizer>();

  /**
   * Managed features specification.
   */
  protected final CloudDriveFeatures                         features;

  /**
   * Environment for commands execution.
   */
  protected CloudDriveEnvironment                            commandEnv;

  /**
   * Cloud Drive service with storage in JCR and with managed features.
   * 
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param features {@link CloudDriveFeatures}
   */
  public CloudDriveServiceImpl(RepositoryService jcrService,
                               SessionProviderService sessionProviders,
                               CloudDriveFeatures features) {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;

    // Add internal listener for handling consistency in users-per-repository map (on drive disconnect or
    // removal)
    this.drivesListeners.add(new LocalDrivesListener());

    this.features = features;

    this.fileSynchronizers.add(new NtFileSynchronizer()); // default one for nt:file + nt:folder
  }

  /**
   * Cloud Drive service with storage in JCR and all features permitted.
   * 
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   */
  public CloudDriveServiceImpl(RepositoryService jcrService, SessionProviderService sessionProviders) {
    this(jcrService, sessionProviders, new PermissiveFeatures());
  }

  public void addPlugin(ComponentPlugin plugin) {
    if (plugin instanceof CloudDriveConnector) {
      // connectors
      CloudDriveConnector impl = (CloudDriveConnector) plugin;
      connectors.put(impl.getProvider(), impl);
    } else if (plugin instanceof CloudDriveEnvironment) {
      // environment customizations
      CloudDriveEnvironment env = (CloudDriveEnvironment) plugin;
      if (commandEnv != null) {
        commandEnv.chain(env);
      } else {
        commandEnv = env;
      }
    } else if (plugin instanceof CloudDriveListener) {
      // global listeners
      drivesListeners.add((CloudDriveListener) plugin);
    } else if (plugin instanceof CloudFileSynchronizer) {
      // sync plugin
      fileSynchronizers.add((CloudFileSynchronizer) plugin);
    } else {
      LOG.warn("Cannot recognize component plugin for " + plugin.getName() + ": type " + plugin.getClass()
          + " not supported");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudProvider getProvider(String id) throws ProviderNotAvailableException {
    for (CloudProvider p : connectors.keySet()) {
      if (p.getId().equals(id)) {
        return p;
      }
    }
    throw new ProviderNotAvailableException("No such provider '" + id + "'");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudDrive findDrive(Node node) throws RepositoryException {
    String repoName = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
    if (drives != null) {
      for (Iterator<CloudDrive> cditer = drives.values().iterator(); cditer.hasNext();) {
        CloudDrive local = cditer.next();
        try {
          if (local.isInDrive(node)) {
            return local; // we found it
          }
        } catch (AccessDeniedException e) {
          // skip other users nodes, can be thrown on isConnected() - try next
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> findDrive(" + node.getPath() + ") access denied to " + local + ": " + e.getMessage(), e);
          }
        } catch (DriveRemovedException e) {
          // ignore removed (should not happen here)
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> findDrive(" + node.getPath() + ") drive removed " + local + ": " + e.getMessage(), e);
          }
          // XXX Aug 18 2015, indeed sometime it happens - thus clean it here
          cditer.remove();
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudDrive findDrive(String workspace, String path) throws RepositoryException {
    String repoName = jcrService.getCurrentRepository().getConfiguration().getName();
    Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
    if (drives != null) {
      for (Iterator<CloudDrive> cditer = drives.values().iterator(); cditer.hasNext();) {
        CloudDrive local = cditer.next();
        try {
          if (local.isDrive(workspace, path, true)) {
            return local; // we found it
          }
        } catch (AccessDeniedException e) {
          // skip other users nodes, can be thrown on isConnected() - try next
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> findDrive(" + workspace + ":" + path + ") access denied to " + local + ": " + e.getMessage(), e);
          }
        } catch (DriveRemovedException e) {
          // ignore removed (should not happen here)
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> findDrive(" + workspace + ":" + path + ") drive removed " + local + ": " + e.getMessage(), e);
          }
          // XXX Aug 18 2015, indeed sometime it happens - thus clean it here
          cditer.remove();
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudUser authenticate(CloudProvider cloudProvider, String code) throws ProviderNotAvailableException,
                                                                          CloudDriveException {
    CloudDriveConnector conn = connectors.get(cloudProvider);
    if (conn != null) {
      return conn.authenticate(Collections.singletonMap(CloudDriveConnector.OAUTH2_CODE, code));
    } else {
      throw new ProviderNotAvailableException("Provider not available " + cloudProvider.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudUser authenticate(CloudProvider cloudProvider,
                                Map<String, String> params) throws ProviderNotAvailableException, CloudDriveException {
    CloudDriveConnector conn = connectors.get(cloudProvider);
    if (conn != null) {
      return conn.authenticate(params);
    } else {
      throw new ProviderNotAvailableException("Provider not available " + cloudProvider.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudDrive createDrive(CloudUser user, Node driveNode) throws UserAlreadyConnectedException,
                                                                ProviderNotAvailableException,
                                                                CloudDriveException,
                                                                RepositoryException {
    String repoName = ((ManageableRepository) driveNode.getSession().getRepository()).getConfiguration().getName();

    Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
    if (drives != null) {
      CloudDrive local = drives.get(user);
      if (local != null) {
        // we have connected drive
        String localPath;
        try {
          localPath = local.getPath();
          if (localPath.equals(driveNode.getPath())) {
            // drive exists
            if (local.isConnected()) {
              // and already connected
              // it's the same user, this could happen if the access was revoked and user want to get this
              // access again, thus we update access key from this new user instance.
              // XXX this usecase based on GoogleDrive workflow and can be changed
              local.updateAccess(user);
            } // else, local not null but not connected, just return it to the user
            return local;
          } else {
            // given user already connected to another node (possible if node was renamed in JCR), we cannot
            // proceed
            LOG.warn("User " + user.getEmail() + " already connected to another node " + localPath
                + ", cannot connect it to " + driveNode.getPath());
            throw new UserAlreadyConnectedException("User " + user.getEmail() + " already connected to another node "
                + localPath);
          }
        } catch (DriveRemovedException e) {
          // removed, so can create new one
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> createDrive(" + user.getEmail() + ", " + driveNode.getPath()
                + ") already removed, so can create new one " + local + ": " + e.getMessage(), e);
          }
        } catch (AccessDeniedException e) {
          // this email already connected in current repository
          LOG.warn("User " + user.getEmail() + " already connected to another node", e);
          throw new UserAlreadyConnectedException("User " + user.getEmail() + " already connected to another node.");
        }
      } // else, no drive cached
    } // else, no drives in this repository

    // create new
    CloudDriveConnector conn = connectors.get(user.getProvider());
    if (conn != null) {
      if (features.canCreateDrive(driveNode.getSession().getWorkspace().getName(),
                                  driveNode.getPath(),
                                  user.getId(),
                                  user.getProvider())) {
        CloudDrive local = conn.createDrive(user, driveNode);
        local.configure(commandEnv, fileSynchronizers);
        registerDrive(user, local, repoName);
        return local;
      } else {
        throw new CannotCreateDriveException("Cannot create drive for user " + user.getEmail());
      }
    } else {
      // shouldn't happen if user obtained from this service
      throw new ProviderNotAvailableException("Provider not available " + user.getProvider().getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<CloudProvider> getProviders() {
    return Collections.unmodifiableSet(connectors.keySet());
  }

  /**
   * On-start initializer.
   */
  @Override
  public void start() {
    try {
      loadConnected(jcrService.getCurrentRepository());
    } catch (RepositoryException e) {
      LOG.error("Error reading current repository: " + e.getMessage());
      throw new RuntimeException("Error loading connected drives: cannot read current repository", e);
    }
    LOG.info("Cloud Drive service successfuly started");
  }

  /**
   * On-stop finalizer.
   */
  @Override
  public void stop() {
    // cleanup of local caches
    repositoryDrives.clear();
    userDrives.clear();
    LOG.info("Cloud Drive service successfuly stopped");
  }

  // *********************** implementation level ***************

  /**
   * List of available connectors.
   * 
   * @return collection of {@link CloudDriveConnector} instances.
   */
  public Collection<CloudDriveConnector> getConnectors() {
    return Collections.unmodifiableCollection(connectors.values());
  }

  // *********************** internal stuff *********************

  protected void registerDrive(CloudUser user, CloudDrive drive, String repoName) {
    // register in caches
    Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
    if (drives == null) {
      drives = new ConcurrentHashMap<CloudUser, CloudDrive>();
      repositoryDrives.put(repoName, drives);
      userDrives.put(user, drives);
    }
    drives.put(user, drive);

    // add listeners
    for (CloudDriveListener listner : drivesListeners) {
      drive.addListener(listner);
    }
  }

  /**
   * Load all ecd:cloudDrive nodes into connected map if ecd:connected is true for each of them.
   * 
   * @param jcrRepository {@link ManageableRepository}
   */
  protected void loadConnected(ManageableRepository jcrRepository) {
    final SessionProvider spOrig = sessionProviders.getSessionProvider(null);
    final SessionProvider sp = SessionProvider.createSystemProvider();
    sessionProviders.setSessionProvider(null, sp); // set current
    try {
      for (WorkspaceEntry w : jcrRepository.getConfiguration().getWorkspaceEntries()) {
        try {
          Map<CloudProvider, Set<Node>> repoDrives = new HashMap<CloudProvider, Set<Node>>();
          Session session = sp.getSession(w.getName(), jcrRepository);
          try {
            // gather all drive nodes from the jcr repo
            Query q = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select * from " + JCRLocalCloudDrive.ECD_CLOUDDRIVE, Query.SQL);
            NodeIterator r = q.execute().getNodes();
            while (r.hasNext()) {
              Node drive = r.nextNode();
              // We're reading nodes directly here. Much pretty it would be to do this in connectors,
              // but then it will cause more reads of the same items, thus will affects the
              // performance a bit. So, to avoid reading of the same we do it here once.
              if (drive.getProperty("ecd:connected").getBoolean()) {
                String providerId = drive.getProperty("ecd:provider").getString();
                try {
                  CloudProvider provider = getProvider(providerId);
                  Set<Node> driveNodes = repoDrives.get(provider);
                  if (driveNodes == null) {
                    driveNodes = new HashSet<Node>();
                    repoDrives.put(provider, driveNodes);
                  }
                  driveNodes.add(drive);
                } catch (CloudDriveException e) {
                  LOG.error("Error loading provider (" + providerId + ") of stored drive " + drive.getPath() + ": "
                      + e.getMessage(), e);
                }
              }
            }

            // get connected drives and add them to local cache
            for (Map.Entry<CloudProvider, Set<Node>> pd : repoDrives.entrySet()) {
              CloudDriveConnector conn = connectors.get(pd.getKey());
              try {
                Set<CloudDrive> locals = conn.loadStored(pd.getValue());
                for (CloudDrive local : locals) {
                  if (local.isConnected()) {
                    local.configure(commandEnv, fileSynchronizers);
                    CloudUser user = local.getUser();
                    String repoName = jcrRepository.getConfiguration().getName();
                    registerDrive(user, local, repoName);
                  }
                }
              } catch (CloudDriveException e) {
                LOG.error("Error loading stored drives for provider " + pd.getKey().getName() + ": " + e.getMessage(), e);
              }
            }
          } catch (RepositoryException e) {
            LOG.error("Search error on " + w.getName() + "@" + jcrRepository.getConfiguration().getName(), e);
          } finally {
            session.logout();
          }
        } catch (RepositoryException e) {
          LOG.error("System session error on " + w.getName() + "@" + jcrRepository.getConfiguration().getName(), e);
        }
      }
    } finally {
      try {
        sp.close();
      } catch (IllegalStateException e) {
        // should not happen but already closed
        LOG.warn("Unexpectedly session provider already closed: " + e.getMessage());
      }
      // restore existing session provider
      sessionProviders.setSessionProvider(null, spOrig);
    }
  }
}
