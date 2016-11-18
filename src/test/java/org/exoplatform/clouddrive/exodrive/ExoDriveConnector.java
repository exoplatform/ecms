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
package org.exoplatform.clouddrive.exodrive;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveConnector;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.ConfigurationException;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveConfigurationException;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveRepository;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveService;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created for tests... but can be used to store files to the local files. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveConnector.java 00000 Oct 4, 2012 pnedonosko $
 */
public class ExoDriveConnector extends CloudDriveConnector {

  public static final String ANONYMOUS = "__anonymous".intern();

  public static final String SYSTEM    = "__system".intern();

  public static final String EMPTY     = "".intern();

  class AuthURLBuilder {
    /**
     * Build Exo Drive authentication url.
     * 
     * @throws IllegalStateException if cannot read Current Repository
     * @return String with auth url.
     */
    String build() {
      String error;
      StringBuilder authUrl = new StringBuilder();
      authUrl.append("http://");
      try {
        authUrl.append(jcrService.getCurrentRepository().getConfiguration().getName());
        error = null;
      } catch (RepositoryException e) {
        LOG.warn("Error getting Current Repository for repository based auth url of eXo Drive: " + e.getMessage(), e);
        error = "Current Repository not set.";
      }
      authUrl.append('/');
      authUrl.append(getConnectorHost());
      authUrl.append("/portal/rest/clouddrive/connect/");
      authUrl.append(getProviderId());

      // query string
      authUrl.append('?');
      if (error == null) {
        authUrl.append("code=");
        ConversationState convo = ConversationState.getCurrent();
        if (convo != null) {
          authUrl.append("code=");
          authUrl.append(convo.getIdentity().getUserId());
        } else {
          authUrl.append("error=");
          authUrl.append("User not authenticated.");
        }
      } else {
        authUrl.append("error=");
        authUrl.append(error);
      }

      return authUrl.toString();
    }
  }

  protected static final Log          LOG = ExoLogger.getLogger(ExoDriveConnector.class);

  protected final ExoDriveUser        systemUser;

  protected final ExoDriveUser        anonymousUser;

  protected final ExoDriveService     service;

  protected final OrganizationService orgService;

  protected final AuthURLBuilder      authUrlBuilder;

  /**
   * @param params
   * @throws CloudDriveException
   */
  public ExoDriveConnector(RepositoryService jcrService,
                           SessionProviderService sessionProviders,
                           ExoDriveService service,
                           OrganizationService orgService,
                           NodeFinder finder,
                           ExtendedMimeTypeResolver mimeTypes,
                           InitParams params) throws ConfigurationException {
    super(jcrService, sessionProviders, finder, mimeTypes, params);

    this.service = service;
    this.orgService = orgService;

    this.authUrlBuilder = new AuthURLBuilder();

    this.systemUser = new ExoDriveUser(SYSTEM, EMPTY, provider);
    this.anonymousUser = new ExoDriveUser(ANONYMOUS, EMPTY, provider);
  }

  protected ExoDriveRepository repository() throws ExoDriveConfigurationException, RepositoryException {
    return service.open(jcrService.getCurrentRepository().getConfiguration().getName());
  }

  /**
   * @inherritDoc
   */
  @Override
  protected CloudProvider createProvider() {
    return new ExoDriveProvider(getProviderId(), getProviderName(), authUrlBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExoDriveUser authenticate(Map<String, String> params) throws CloudDriveException {
    String code = params.get(OAUTH2_CODE);
    if (code != null) {
      // ensure the convo state is the same as the code
      ConversationState convo = ConversationState.getCurrent();
      if (convo == null) {
        throw new CloudDriveException("User not authenticated (conversation not set)");
      } else if (!code.equals(convo.getIdentity().getUserId())) {
        throw new CloudDriveException("User not authorized (conversation not the same)");
      }

      if (SYSTEM == code) {
        return systemUser;
      }

      if (ANONYMOUS == code) {
        return anonymousUser;
      }

      String userEmail;
      try {
        // we're treating given key as user name from org-service
        User user = orgService.getUserHandler().findUserByName(code);
        if (user != null) {
          userEmail = user.getEmail();
        } else {
          userEmail = null;
        }
      } catch (Exception e) {
        LOG.error("Error reading the user from organization service", e);
        userEmail = null;
      }

      if (userEmail != null) {
        try {
          ExoDriveRepository driveRepo = repository();
          if (!driveRepo.createUser(code)) {
            throw new CloudDriveException("Cannot create user '" + code + "' folder.");
          }

          // key the same as name
          return new ExoDriveUser(code, userEmail, provider);
        } catch (ExoDriveConfigurationException e) {
          throw new CloudDriveException("Error reading eXo Drive repository:", e);
        } catch (RepositoryException e) {
          throw new CloudDriveException("Error getting current JCR repository:", e);
        }
      } else {
        throw new CloudDriveException("User not found " + code);
      }
    } else {
      throw new CloudDriveException("Code key should not be null");
    }
  }

  protected ExoDriveUser createUser(String username, String email) throws CloudDriveException {
    return new ExoDriveUser(username, email, provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JCRLocalExoDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    if (user instanceof ExoDriveUser) {
      try {
        return new JCRLocalExoDrive((ExoDriveUser) user, repository(), sessionProviders, jcrFinder, mimeTypes, driveNode);
      } catch (ExoDriveConfigurationException e) {
        throw new CloudDriveException("Error getting eXo Drive repository:", e);
      }
    } else {
      throw new CloudDriveException("Not eXo Drive user: " + user);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
    try {
      return new JCRLocalExoDrive(repository(),
                                  (ExoDriveProvider) provider,
                                  sessionProviders,
                                  jcrFinder,
                                  mimeTypes,
                                  driveNode);
    } catch (ExoDriveConfigurationException e) {
      throw new CloudDriveException("Error getting eXo Drive repository:", e);
    }
  }

}
