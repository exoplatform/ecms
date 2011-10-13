/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.plugin.actions.activation;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.actions.ActionPlugin;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.plugin.actions.impl.BPActionPlugin;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 30, 2008
 */
public class BPActionActivationJob implements Job {
  final private static String COUNTER_PROP = "exo:counter" ;
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService =
      (RepositoryService) exoContainer.getComponentInstanceOfType(RepositoryService.class);
    ActionServiceContainer actionServiceContainer =
      (ActionServiceContainer) exoContainer.getComponentInstanceOfType(ActionServiceContainer.class);
    IdentityRegistry identityRegistry =
      (IdentityRegistry)exoContainer.getComponentInstanceOfType(IdentityRegistry.class);
    ActionPlugin bpActionService = actionServiceContainer.getActionPlugin(BPActionPlugin.ACTION_TYPE) ;

    Session jcrSession = null;
    Node actionNode = null ;

    JobDataMap jdatamap = context.getJobDetail().getJobDataMap() ;
    String userId = jdatamap.getString("initiator") ;
    String srcWorkspace = jdatamap.getString("srcWorkspace") ;
    String srcPath = jdatamap.getString("srcPath") ;
    String actionName = jdatamap.getString("actionName") ;
    String executable = jdatamap.getString("executable") ;
    String repository = jdatamap.getString("repository") ;
    Map variables = jdatamap.getWrappedMap() ;
    try {
      jcrSession = repositoryService.getCurrentRepository().getSystemSession(srcWorkspace);
      Node node = (Node) jcrSession.getItem(srcPath);
      actionNode = actionServiceContainer.getAction(node, actionName);
      Property rolesProp = actionNode.getProperty("exo:roles");
      Value[] roles = rolesProp.getValues();
      boolean hasPermission = checkExcetuteable(userId, roles, identityRegistry) ;
      if (!hasPermission)  {
        jcrSession.logout();
        return;
      }
      bpActionService.activateAction(userId,executable,variables,repository) ;
      int currentCounter = (int)actionNode.getProperty(COUNTER_PROP).getValue().getLong() ;
      actionNode.setProperty(COUNTER_PROP,currentCounter +1) ;
      actionNode.save() ;
      jcrSession.save() ;
      jcrSession.logout();
    } catch (Exception e) {
      jcrSession.logout();
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
