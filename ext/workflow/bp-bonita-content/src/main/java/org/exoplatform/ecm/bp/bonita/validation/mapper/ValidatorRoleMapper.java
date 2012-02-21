/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.ecm.bp.bonita.validation.mapper;

import hero.mapper.ExoOrganizationMapper;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.ow2.bonita.definition.RoleMapper;
import org.ow2.bonita.facade.QueryAPIAccessor;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;

/**
 * This Role Mapper retrieves the name of the validator Group in eXo from
 * Instance Properties. As a Role Mapper is invoked prior Instance Properties
 * can be set, this Role Mapper finds them in a Thread Local set by the method
 * that starts the Instance in the Bonita service.
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * @author Rodrigue Le Gall
 */
public class ValidatorRoleMapper implements RoleMapper {

  /** Name of the Property that contains the eXo Membership and Group */
  public static final String PROPERTY_NAME = "exo:validator";

  public static final String DELEGATOR_NAME = "delegator";
  public static final String DELEGATE_NAME = "delegate";
  private static final Log LOG  = ExoLogger.getLogger(ValidatorRoleMapper.class);

  public Set<String> searchMembers(QueryAPIAccessor readonlyapiaccessor, ProcessInstanceUUID instanceId, String roleId) {

    String roleName = "";
    String delegator = "";
    Boolean delegate = new Boolean(false);
    try {
      roleName = (String) readonlyapiaccessor.getQueryRuntimeAPI().getProcessInstanceVariable(
          instanceId, PROPERTY_NAME);
      delegator = (String) readonlyapiaccessor.getQueryRuntimeAPI().getProcessInstanceVariable(
          instanceId, DELEGATOR_NAME);
      delegate = (Boolean) readonlyapiaccessor.getQueryRuntimeAPI().getProcessInstanceVariable(
          instanceId, DELEGATE_NAME);
    } catch (InstanceNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    } catch (VariableNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    // Delegate the call
    Set<String> candidats = ExoOrganizationMapper.GetUsersFromMembershipAndGroup(roleName);
    if (delegate) {
      if (delegator!= null) {
        candidats = new HashSet<String>();
        candidats.add(delegator);
      }
    } else {
        if (candidats.size() < 2)
          return candidats;
        candidats.remove(delegator);
    }
    return candidats;
  }

}
