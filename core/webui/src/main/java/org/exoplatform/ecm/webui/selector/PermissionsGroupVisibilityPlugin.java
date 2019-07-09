package org.exoplatform.ecm.webui.selector;

import org.exoplatform.portal.config.GroupVisibilityPlugin;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import java.util.Collection;

/**
 * Implementation of GroupVisibilityPlugin for documents permissions which
 * allows to see a group if any of these conditions is fulfilled:
 * * the given user is the super user
 * * the given user is a platform administrator
 * * the given user is a manager of the group
 * * the group is a space group and the given user has a role in the group
 */
public class PermissionsGroupVisibilityPlugin extends GroupVisibilityPlugin {

  private UserACL userACL;

  public PermissionsGroupVisibilityPlugin(UserACL userACL) {
    this.userACL = userACL;
  }

  public boolean hasPermission(Identity userIdentity, Group group) {
    Collection<MembershipEntry> userMemberships = userIdentity.getMemberships();
    return userACL.getSuperUser().equals(userIdentity.getUserId())
        || userMemberships.stream()
                          .anyMatch(userMembership -> userMembership.getGroup().equals(userACL.getAdminGroups())
                              || ((userMembership.getGroup().equals(group.getId())
                                  || userMembership.getGroup().startsWith(group.getId() + "/"))
                                  && (group.getId().equals("/spaces")
                                      || group.getId().startsWith("/spaces/")
                                      || userMembership.getMembershipType().equals("*")
                                      || userMembership.getMembershipType().equals("manager"))));
  }
}
