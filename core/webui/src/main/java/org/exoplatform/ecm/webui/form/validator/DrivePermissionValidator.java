/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * May 19, 2011
 */
public class DrivePermissionValidator extends PermissionValidator {

  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput.getValue() == null || ((String) uiInput.getValue()).trim().length() == 0
        || "*".equals(((String) uiInput.getValue()).trim())
        || "${userId}".equals(((String) uiInput.getValue()).trim())
        || "*:${groupId}".equals(((String) uiInput.getValue()).trim()))
      return;
    super.validate(uiInput);
  }
}
