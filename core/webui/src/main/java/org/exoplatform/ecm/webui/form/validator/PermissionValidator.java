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

package org.exoplatform.ecm.webui.form.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;



/**
 * Created by The eXo Platform SEA
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * May 16, 2011 4:52:44 PM
 */

public class PermissionValidator implements Validator {
  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput.getValue() == null || ((String) uiInput.getValue()).trim().length() == 0
        || ((String) uiInput.getValue()).trim().equals("*"))
      return;
    String permissions = ((String) uiInput.getValue()).trim();

    OrganizationService oservice = 
      (OrganizationService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    List<String> listMemberhip;
    Collection<?> collection = oservice.getMembershipTypeHandler().findMembershipTypes();
    listMemberhip = new ArrayList<String>(5);
    for (Object obj : collection) {
      listMemberhip.add(((MembershipType) obj).getName());
    }
    listMemberhip.add("*");

    String[] arrPermissions = permissions.split(",");
    for (String itemPermission : arrPermissions) {
      if (itemPermission.length() == 0) {
        Object[] args = { itemPermission };
        throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                          args));
      }
      if (itemPermission.equals("*"))
        continue;
      else if (itemPermission.contains(":")) {
        String[] permission = itemPermission.split(":");
        if ((permission[0] == null) || (permission[0].length() == 0)) {
          Object[] args = { itemPermission };
          throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                            args));
        } else if (!listMemberhip.contains(permission[0])) {
          Object[] args = { itemPermission };
          throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                            args));
        }
        if ((permission[1] == null) || (permission[1].length() == 0)) {
          Object[] args = { itemPermission };
          throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                            args));
        } else if (oservice.getGroupHandler().findGroupById(permission[1]) == null) {
          Object[] args = { itemPermission };
          throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                            args));
        }
      } else {
        Object[] args = { itemPermission };
        throw new MessageException(new ApplicationMessage("PermissionValidator.msg.permission-path-invalid",
                                                          args));
      }
    }
  }
}
