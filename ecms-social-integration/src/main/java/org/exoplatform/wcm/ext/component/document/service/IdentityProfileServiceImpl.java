/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.document.service;

import org.exoplatform.services.cms.documents.IdentityProfile;
import org.exoplatform.services.cms.documents.IdentityProfileService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * The Class IdentityProfileServiceImpl.
 */
public class IdentityProfileServiceImpl implements IdentityProfileService {

  /** The identity manager. */
  protected final IdentityManager     identityManager;

  /** The organization service. */
  protected final OrganizationService organizationService;

  /**
   * Instantiates a new identity profile service impl.
   *
   * @param identityManager the identity manager
   * @param organizationService the organization service
   */
  public IdentityProfileServiceImpl(IdentityManager identityManager, OrganizationService organizationService) {
    this.identityManager = identityManager;
    this.organizationService = organizationService;
  }

  /**
   * Checks for profile.
   *
   * @param userId the user id
   * @return true, if successful
   */
  @Override
  public boolean hasProfile(String userId) {
    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId) != null;
  }

  /**
   * Gets the user profile.
   *
   * @param userId the user id
   * @return the user profile
   */
  @Override
  public IdentityProfile getUserProfile(String userId) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the space profile.
   *
   * @param groupId the group id
   * @return the space profile
   */
  @Override
  public IdentityProfile getSpaceProfile(String groupId) {
    // TODO Auto-generated method stub
    return null;
  }

}
