/**
 * Copyright (C) 2008  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package hero.mapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.ow2.bonita.definition.RoleMapper;
import org.ow2.bonita.facade.QueryAPIAccessor;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.def.majorElement.ParticipantDefinition;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ParticipantNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;

/**
 * ExoOrganizationMapper maps eXo users
 *
 * @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 */
public class ExoOrganizationMapper implements RoleMapper {

  private static Log log = ExoLogger.getLogger(ExoOrganizationMapper.class.getName());

  public Set<String> searchMembers(QueryAPIAccessor readonlyapiaccessor,
                                   ProcessInstanceUUID instanceId,
                                   String roleId) {

    ParticipantDefinition participantDef;
    ProcessDefinitionUUID processId;
    try {
      QueryRuntimeAPI queryAPI = readonlyapiaccessor.getQueryRuntimeAPI();
      processId = queryAPI.getProcessInstance(instanceId).getProcessDefinitionUUID();

      QueryDefinitionAPI definitionAPI = readonlyapiaccessor.getQueryDefinitionAPI();

      participantDef = definitionAPI.getProcessParticipant(processId, roleId);
      return ExoOrganizationMapper.GetUsersFromMembershipAndGroup(participantDef.getName());
    } catch (InstanceNotFoundException e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    } catch (ProcessNotFoundException e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    } catch (ParticipantNotFoundException e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * Gets the list of eXo users who belong to the specified Membership and Group
   *
   * @param membershipAndGroup specifies the Membership and Group
   */
  public static Set<String> GetUsersFromMembershipAndGroup(String membershipAndGroup) {

    // The returned list
    Set<String> users = new HashSet<String>();

    try {
      // Lookup the eXo Organization service
      PortalContainer container = PortalContainer.getInstance();
      OrganizationService organization = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      // Determine the Membership and Group
      String[] tokens = membershipAndGroup.split(":");
      String membership = null;
      String group = null;

      if (tokens.length == 2) {
        // There is a single colon character
        membership = tokens[0];
        group = tokens[1];
      } else {
        // There is not is single colon character
        membership = "*";
        group = membershipAndGroup;

        if (log.isErrorEnabled()) {
          log.error("Warning : The specified Bonita role does not "
            + "conform to the syntax membership:group.");
        }
      }
      if (log.isInfoEnabled()) {
        log.info("Starting role mapping for [group,membership] : [" + group + "," + membership
            + "]");
      }

      // Retrieve all the users contained by the specified group
      UserHandler userHandler = organization.getUserHandler();
      PageList pageList = userHandler.findUsersByGroup(group);
      Collection<User> usersInGroup = pageList.getAll();

      // Process each user in the group
      for (User user : usersInGroup) {
        // Retrieve the name of the current user
        String userName = user.getUserName();

        if ("*".equals(membership)
            || organization.getMembershipHandler().findMembershipByUserGroupAndType(userName,
                                                                                    group,
                                                                                    membership) != null) {
          // The user has the specified membership
          users.add(userName);
          if (log.isInfoEnabled()) {
            log.info("Add user : " + userName);
          }

        }
      }
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn(e.getMessage(), e);
      }
    }

    return users;
  }

}
