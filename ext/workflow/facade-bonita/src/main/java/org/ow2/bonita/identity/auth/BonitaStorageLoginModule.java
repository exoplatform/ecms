/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.ow2.bonita.identity.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 12, 2009
 * 9:23:58 AM
 */
public final class BonitaStorageLoginModule implements LoginModule {

  protected Subject subject;

  public boolean abort() throws LoginException {
    return true;
  }

  public boolean commit() throws LoginException {
    logout();
    SecurityContext.setSubject(subject);
    return true;
  }

  public void initialize(Subject subject, CallbackHandler arg1,
      Map<String, ? > arg2, Map<String, ? > arg3) {

    this.subject = subject;
  }

  public boolean login() throws LoginException {
    return true;
  }

  public boolean logout() throws LoginException {
    try {
    if (SecurityContext.getSubject() != null) {
      SecurityContext.clearSubject();
    }
  } catch (Exception e) {
    // Ignore exception
  }
  return true;
  }

}
