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
package org.exoplatform.services.cms.actions.activation;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.actions.ActionPlugin;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.actions.impl.ScriptActionPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 21, 2006
 */
public class ScriptActionActivationJob implements Job {

  final private static String COUNTER_PROP = "exo:counter" ;
  private static final Log LOG  = ExoLogger.getLogger(ScriptActionActivationJob.class);

  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService =
      (RepositoryService) exoContainer.getComponentInstanceOfType(RepositoryService.class);
    ActionServiceContainer actionServiceContainer =
      (ActionServiceContainer) exoContainer.getComponentInstanceOfType(ActionServiceContainer.class);
    IdentityRegistry identityRegistry =
      (IdentityRegistry) exoContainer.getComponentInstanceOfType(IdentityRegistry.class);
    ActionPlugin scriptActionService = actionServiceContainer.getActionPlugin(ScriptActionPlugin.ACTION_TYPE) ;

    Session jcrSession = null;
    Node actionNode = null ;
    JobDataMap jdatamap = context.getJobDetail().getJobDataMap() ;
    String userId = jdatamap.getString("initiator") ;
    String srcWorkspace = jdatamap.getString("srcWorkspace") ;
    String srcPath = jdatamap.getString("srcPath") ;
    String actionName = jdatamap.getString("actionName") ;
    String executable = jdatamap.getString("executable") ;
    String repository = null;
    Map variables = jdatamap.getWrappedMap() ;
    try {
      repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      jcrSession = repositoryService.getCurrentRepository().getSystemSession(srcWorkspace);
      Node node = (Node) jcrSession.getItem(srcPath);
      actionNode = actionServiceContainer.getAction(node, actionName);
      Property rolesProp = actionNode.getProperty("exo:roles");
      Value[] roles = rolesProp.getValues();
      boolean hasPermission = checkExcetuteable(userId, roles, identityRegistry);
      if (!hasPermission) {
        jcrSession.logout();
        return;
      }
      scriptActionService.activateAction(userId, executable, variables, repository);
      int currentCounter = (int)actionNode.getProperty(COUNTER_PROP).getValue().getLong() ;
      actionNode.setProperty(COUNTER_PROP,currentCounter +1) ;
      actionNode.save() ;
      jcrSession.save() ;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    } finally {
      if(jcrSession != null) jcrSession.logout();
    }
  }

  private boolean checkExcetuteable(String userId,Value[] roles, IdentityRegistry identityRegistry) throws Exception {
    if(IdentityConstants.SYSTEM.equalsIgnoreCase(userId)) {
      return true ;
    }
    Identity identity = identityRegistry.getIdentity(userId);
    if(identity == null) {
      return false ;
    }
    for (int i = 0; i < roles.length; i++) {
      String role = roles[i].getString();
      if("*".equalsIgnoreCase(role)) return true ;
      MembershipEntry membershipEntry = MembershipEntry.parse(role) ;
      if(identity.isMemberOf(membershipEntry)) {
        return true ;
      }
    }
    return false ;
  }
}
