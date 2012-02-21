/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.actions.impl.ECMEventListener;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 6, 2009
 */
public abstract class WorkflowActionLauncherListener implements ECMEventListener {
  protected String actionName_;
  protected String repository_ ;
  protected String srcWorkspace_;
  protected String srcPath_;
  protected String executable_;
  protected Map actionVariables_;

  private static final String MIXIN_MOVE = "exo:move";
  private static final String WORKFLOW = "Workflow";
  private static final String VALIDATION_REQUEST = "validation request";
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String DEST_WORKSPACE = "exo:destWorkspace";
  private static final String DESTPATH = "exo:destPath";
  private static final String BACUP_PATH = "publication:backupPath";
  private static final String DOCUMENT_BACUPUP = "documentsBackupPath";
  private static final Log LOG  = ExoLogger.getLogger(WorkflowActionLauncherListener.class);

  public WorkflowActionLauncherListener(String actionName, String executable,
      String repository, String srcWorkspace, String srcPath, Map actionVariables)
  throws Exception {
    actionName_ = actionName;
    executable_ = executable;
    repository_ = repository ;
    srcWorkspace_ = srcWorkspace;
    srcPath_ = srcPath;
    actionVariables_ = actionVariables;
  }

  public String getSrcWorkspace() { return srcWorkspace_; }
  public String getRepository() { return repository_; }

  @SuppressWarnings("unchecked")
  public void onEvent(EventIterator events) {
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService =
      (RepositoryService) exoContainer.getComponentInstanceOfType(RepositoryService.class);
    ActionServiceContainer actionServiceContainer =
      (ActionServiceContainer) exoContainer.getComponentInstanceOfType(ActionServiceContainer.class);
    IdentityRegistry identityRegistry = (IdentityRegistry) exoContainer.getComponentInstanceOfType(IdentityRegistry.class);

    TemplateService templateService =
      (TemplateService) exoContainer.getComponentInstanceOfType(TemplateService.class);
    if (events.hasNext()) {
      Event event = events.nextEvent();
      Node node = null;
      Session jcrSession = null;
      try {
        jcrSession = repositoryService.getCurrentRepository().getSystemSession(srcWorkspace_);
        node = (Node) jcrSession.getItem(srcPath_);
        String userId = event.getUserID();
        Node actionNode = actionServiceContainer.getAction(node, actionName_);
        Property rolesProp = actionNode.getProperty("exo:roles");
        Value[] roles = rolesProp.getValues();
        boolean hasPermission = checkExcetuteable(userId, roles, identityRegistry) ;
        if (!hasPermission) {
          jcrSession.logout();
          return;
        }
        String path = event.getPath();
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("initiator", userId);
        variables.put("actionName", actionName_);
        variables.put("nodePath", path);
        variables.put("repository", repository_);
        variables.put("srcWorkspace", srcWorkspace_);
        variables.put("srcPath", srcPath_);
        variables.putAll(actionVariables_);
        if(event.getType() == Event.NODE_ADDED) {
          node = (Node) jcrSession.getItem(path);
          String nodeType = node.getPrimaryNodeType().getName();
          if (templateService.getDocumentTemplates().contains(nodeType)) {
            variables.put("document-type", nodeType);
            triggerAction(userId, variables, repository_);
          }
        } else {
          triggerAction(userId, variables, repository_);
        }
        jcrSession.logout();

        if (node.canAddMixin(MIXIN_MOVE)) {
          node.addMixin(MIXIN_MOVE);
          node.getSession().save();
        }

        ExoContainer container = ExoContainerContext.getCurrentContainer();
        PublicationService publicationService = (PublicationService) container.
            getComponentInstanceOfType(PublicationService.class);
        publicationService.enrollNodeInLifecycle(node, WORKFLOW);
        node.getSession().save();

        node.setProperty(CURRENT_STATE, VALIDATION_REQUEST);
        String date =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
        String[] logs = { date, VALIDATION_REQUEST, node.getSession().getUserID(),
            "PublicationService.WorkflowPublicationPlugin.nodeValidationRequest" };
        publicationService.addLog(node, logs);

        NodeHierarchyCreator hierarchyCreator = (NodeHierarchyCreator) container.
            getComponentInstanceOfType(NodeHierarchyCreator.class);
        String documentBackup = hierarchyCreator.getJcrPath(DOCUMENT_BACUPUP);
        node.setProperty(DEST_WORKSPACE, actionNode.getProperty
            (DEST_WORKSPACE).getString());
        node.setProperty(DESTPATH, actionNode.getProperty
            (DESTPATH).getString());
        node.setProperty(BACUP_PATH, documentBackup);

        node.getSession().save();
      } catch (Exception e) {
        if(jcrSession != null) jcrSession.logout();
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    }
  }

  public abstract void triggerAction(String userId, Map variables, String repository)
  throws Exception; {

  }

  private boolean checkExcetuteable(String userId, Value[] roles, IdentityRegistry identityRegistry) throws Exception {
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
