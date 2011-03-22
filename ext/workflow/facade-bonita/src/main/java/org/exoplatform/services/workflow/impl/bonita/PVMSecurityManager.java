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
package org.exoplatform.services.workflow.impl.bonita;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.exoplatform.services.security.UsernameCredential;
import org.ow2.bonita.identity.auth.BonitaPrincipal;

/**
 * TODO validate if this class is necessary
 * @author @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 */
public class PVMSecurityManager {

  private static Logger log = Logger.getLogger(PVMSecurityManager.class.getName());

  public void commit(){
    Subject subject = null;

    // Change for the trunk version
    //Subject subject = (Subject) ConversationState.getCurrent().getAttribute(ConversationState.SUBJECT);
    /**
//        ExoContainer container = ExoContainerContext.getCurrentContainer();
//        AuthenticationService authenticationService = (AuthenticationService) container
//        .getComponentInstanceOfType(AuthenticationService.class);
//        if(authenticationService.getCurrentIdentity()!=null){
//            Subject subject = authenticationService.getCurrentIdentity().getSubject();
     *
     * THESE LINES ARE FOR 2.0 versions
     *
     */
    if(subject!=null){
      String uid="";
      for (Object o: subject.getPublicCredentials()) {
        if(UsernameCredential.class.isInstance(o)){
          uid = ((UsernameCredential)o).getUsername();
          break;
        }
      }
      Subject s = new Subject();
      s.getPrincipals().add(new BonitaPrincipal(uid));
      LoginContext lc = null;
      try {
        lc = new LoginContext("Bonita", s);
      } catch (LoginException le) {
        log.log(Level.WARNING, le.getMessage(), le);
      }

      try {
        lc.login();
        // if we return with no exception, authentication succeeded
      } catch (Exception e) {
        log.log(Level.WARNING, "Login failed: ", e);
      }
    }
  }
}
