/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.quartz.JobExecutionContext;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 8, 2009
 */
public class WCMCoreUtils {

  private static Log log = ExoLogger.getLogger("wcm.WCMCoreUtils");

  private static PortalContainer manager;

  /**
   * Gets the service.
   *
   * @param clazz the clazz
   *
   * @return the service
   */
  public static <T> T getService(Class<T> clazz) {
    return getService(clazz, null);
  }

  /**
   * Gets the system session provider.
   *
   * @return the system session provider
   */
  public static SessionProvider getSystemSessionProvider() {
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    return sessionProvider;
  }

  /**
   * Gets the session provider.
   *
   * @return the session provider
   */
  public static SessionProvider getUserSessionProvider() {
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    return sessionProvider;
  }






  /**
   * Gets the service.
   *
   * @param clazz the class
   * @param containerName the container's name
   *
   * @return the service
   */
  public static <T> T getService(Class<T> clazz, String containerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (containerName != null) {
      container = RootContainer.getInstance().getPortalContainer(containerName);
    }
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }

  public static String getContainerNameFromJobContext(JobExecutionContext context) {
    return context.getJobDetail().getGroup().split(":")[0];
  }

  /**
   * Check current user has permission to access a node or not
   * -    For each permission, compare with user's permissions
   * -      If permission has membership type is "*", just check the user's group id only
   * -      If permission has other membership types, then check the user's membership type and user's group id
   *
   * @param userId the current user's name
   * @param permissions the current node
   * @param isNeedFullAccess if true, count full access (4) then return true, if false, return true if match first permission
   *
   * @return true is user has permissions, otherwise return false
   */
  public static boolean hasPermission(String userId, List<String> permissions, boolean isNeedFullAccess) {
    try {
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
      ((ComponentRequestLifecycle) organizationService).startRequest(manager);
      Collection<?> memberships = null;
      Membership userMembership = null;
      String userMembershipTmp = null;
      int count = 0;
      String permissionTmp = "";
      for (String permission : permissions) {
        if (!permissionTmp.equals(permission)) count = 0;
        memberships = organizationService.getMembershipHandler().findMembershipsByUser(userId);
        Iterator<?> membershipIterator = memberships.iterator();
        while (membershipIterator.hasNext()) {
          userMembership = (Membership)membershipIterator.next();
          if (permission.equals(userMembership.getUserName())) {
            return true;
          } else if ("any".equals(permission)) {
            if (isNeedFullAccess) {
              count++;
              if (count == 4) return true;
            }
            else return true;
          } else if (permission.startsWith("*") && permission.contains(userMembership.getGroupId())) {
            if (isNeedFullAccess) {
              count++;
              if (count == 4) return true;
            }
            else return true;
          } else {
            userMembershipTmp = userMembership.getMembershipType() + ":" + userMembership.getGroupId();
            if (permission.equals(userMembershipTmp)) {
              if (isNeedFullAccess) {
                count++;
                if (count == 4) return true;
              }
              else return true;
            }
          }
        }
        permissionTmp = permission;
      }
//      ((ComponentRequestLifecycle) organizationService).endRequest(manager);
    } catch (Exception e) {
      log.error("hasPermission() failed because of ", e);
    }
    return false;
  }

  public static <T> List<T> getAllElementsOfListAccess(ListAccess<T> listAccess) {
    try {
      return Arrays.asList(listAccess.load(0, listAccess.getSize()));
    } catch (Exception e) {
      log.error("getAllElementsOfListAccess() failed because of ", e);
    }
    return null;
  }

  /**
   * Get the repository by name
   *
   * @param repository the repository name
   *
   * @return the manageable repository by name, the current repository if name is null
   */
  @Deprecated
  public static ManageableRepository getRepository(String repository) {
    try {
      RepositoryService repositoryService = getService(RepositoryService.class);
      return repositoryService.getCurrentRepository();
    } catch (Exception e) {
      log.error("getRepository(" + repository + ") failed because of ", e);
    }
    return null;
  }
  
  /**
   * Get the current repository
   *
   * @return the current manageable repository
   */
  public static ManageableRepository getRepository() {
    try {
      RepositoryService repositoryService = getService(RepositoryService.class);
      return repositoryService.getCurrentRepository();
    } catch (Exception e) {
      log.error("getRepository() failed because of ", e);
    }
    return null;
  }  

}
