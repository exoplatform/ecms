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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.picocontainer.Startable;

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
      // for a case if onDisconnect didn't do the job
      cleanUserCaches(event.getUser());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect(CloudDriveEvent event) {
      cleanUserCaches(event.getUser());
    }

    /**
     * @inherritDoc
     */
    @Override
    public void onError(CloudDriveEvent event, Throwable error) {
      cleanUserCaches(event.getUser());
    }
  }

  protected static final Log                                 LOG              = ExoLogger.getLogger(CloudDriveService.class);

  protected final RepositoryService                          jcrService;

  protected final SessionProviderService                     sessionProviders;

  /**
   * Registered CloudDrive connectors.
   */
  protected final Map<CloudProvider, CloudDriveConnector>    connectors       = new HashMap<CloudProvider, CloudDriveConnector>();

  /**
   * In-memory multiton for drives created per repository and per user. Only connected drives here.
   */
  protected final Map<String, Map<CloudUser, CloudDrive>>    repositoryDrives = new ConcurrentHashMap<String, Map<CloudUser, CloudDrive>>();

  /**
   * User-in-repositoryDrives reference map for unregistration of disconnected and removed drives (via
   * {@link LocalDrivesListener}).
   * 
   * @see #repositoryDrives
   */
  protected final Map<CloudUser, Map<CloudUser, CloudDrive>> userDrives       = new ConcurrentHashMap<CloudUser, Map<CloudUser, CloudDrive>>();

  /**
   * Internal listener used for handling consistency in users-per-repository map (on drive disconnect or
   * removal).
   */
  protected final LocalDrivesListener                        drivesListener;

  public CloudDriveServiceImpl(RepositoryService jcrService, SessionProviderService sessionProviders) {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;

    this.drivesListener = new LocalDrivesListener();
  }

  public void addPlugin(ComponentPlugin plugin) {
    if (plugin instanceof CloudDriveConnector) {
      CloudDriveConnector impl = (CloudDriveConnector) plugin;
      connectors.put(impl.getProvider(), impl);
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
    ConversationState convState = ConversationState.getCurrent();
    if (convState != null && convState.getIdentity().getUserId().equals(node.getSession().getUserID())) {

      String repoName = ((ManageableRepository) node.getSession().getRepository()).getConfiguration()
                                                                                  .getName();

      Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
      if (drives != null) {
        for (CloudDrive local : drives.values()) {
          try {
            if (local.isDrive(node, true)) {
              return local; // we found it
            }
          } catch (AccessDeniedException e) {
            // skip other users nodes, can be thrown on isConnected() - try next
          } catch (DriveRemovedException e) {
            // ignore removed (should not happen here)
          }
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDrive(Node node) throws RepositoryException {
    ConversationState convState = ConversationState.getCurrent();
    if (convState != null && convState.getIdentity().getUserId().equals(node.getSession().getUserID())) {

      String repoName = ((ManageableRepository) node.getSession().getRepository()).getConfiguration()
                                                                                  .getName();

      Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
      if (drives != null) {
        for (CloudDrive local : drives.values()) {
          try {
            if (local.isDrive(node, false)) {
              return true; // we found it
            }
          } catch (AccessDeniedException e) {
            // skip other users nodes, can be thrown on isConnected() - try next
          } catch (DriveRemovedException e) {
            // ignore removed (should not happen here)
          }
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudUser authenticate(CloudProvider cloudProvider, String key) throws ProviderNotAvailableException,
                                                                        CloudDriveException {
    CloudDriveConnector conn = connectors.get(cloudProvider);
    if (conn != null) {
      return conn.authenticate(key);
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
    String repoName = ((ManageableRepository) driveNode.getSession().getRepository()).getConfiguration()
                                                                                     .getName();

    Map<CloudUser, CloudDrive> drives = repositoryDrives.get(repoName);
    if (drives != null) {
      CloudDrive local = drives.get(user);
      if (local != null) {
        // we have cached drive
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
              local.updateAccessKey(user);
            } // else, local not null but not connected, just return it to the user
            return local;
          } else {
            // given user already connected to another node (possible if node was renamed in JCR), we cannot
            // proceed
            // TODO should we point an user email in the message?
            LOG.warn("User " + user.getEmail() + " already connected to another node " + localPath);
            throw new UserAlreadyConnectedException("User " + user.getEmail()
                + " already connected to another node " + localPath);
          }
        } catch (DriveRemovedException e) {
          // removed, so can create new one
        } catch (AccessDeniedException e) {
          // this email already connected in current repository
          LOG.warn("User " + user.getEmail() + " already connected to another node", e);
          throw new UserAlreadyConnectedException("User " + user.getEmail()
              + " already connected to another node.");
        }
      } // else, no drive cached
    } // else, no drives in this repository

    // create new
    CloudDriveConnector conn = connectors.get(user.getProvider());
    if (conn != null) {
      CloudDrive local = conn.createDrive(user, driveNode);
      registerDrive(user, local, repoName);
      return local;
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

    // add listener
    drive.addListener(drivesListener);
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
                  LOG.error("Error loading stored drive " + drive.getPath() + ": " + e.getMessage(), e);
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
                    CloudUser user = local.getUser();
                    String repoName = jcrRepository.getConfiguration().getName();
                    registerDrive(user, local, repoName);
                  }
                }
              } catch (CloudDriveException e) {
                LOG.error("Error loading stored drive for " + pd.getKey().getName() + ": " + e.getMessage(),
                          e);
              }
            }
          } catch (RepositoryException e) {
            LOG.error("Search error on " + w.getName() + "@" + jcrRepository.getConfiguration().getName(), e);
          } finally {
            session.logout();
          }
        } catch (RepositoryException e) {
          LOG.error("System session error on " + w.getName() + "@"
              + jcrRepository.getConfiguration().getName(), e);
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
