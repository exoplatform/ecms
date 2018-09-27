/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.permission;

import java.util.Objects;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung hunghvit@gmail.com
 * Apr 17, 2009
 */
public class PermissionBean {

  private String  usersOrGroups;

  private boolean read;

  private boolean addNode;

  private boolean setProperty;

  private boolean remove;

  public String getUsersOrGroups() {
    return usersOrGroups;
  }

  public void setUsersOrGroups(String s) {
    usersOrGroups = s;
  }

  public boolean isAddNode() {
    return addNode;
  }

  public void setAddNode(boolean b) {
    addNode = b;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean b) {
    read = b;
  }

  public boolean isRemove() {
    return remove;
  }

  public void setRemove(boolean b) {
    remove = b;
  }

  public boolean isSetProperty() {
    return setProperty;
  }

  public void setSetProperty(boolean b) {
    setProperty = b;
  }

  public boolean equals(Object arg0) {
    if (arg0 instanceof PermissionBean) {
      PermissionBean permBean = (PermissionBean) arg0;
      return this.getUsersOrGroups().equals(permBean.getUsersOrGroups());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(usersOrGroups);
  }
}
