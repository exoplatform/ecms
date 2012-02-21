/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.plugins.workflow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.exoplatform.services.log.Log;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
            quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Dec 22, 2008
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:resources/templates/workflow/workflowPublicationView.gtmpl",
                 events = {
    @EventConfig(listeners = UIWorkflowPublicationViewForm.CancelActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIWorkflowPublicationViewForm.EditActionListener.class),
    @EventConfig(listeners = UIWorkflowPublicationViewForm.UnpublishActionListener.class),
    @EventConfig(listeners = UIWorkflowPublicationViewForm.UnsubcriberLifeCycleActionListener.class) })
public class UIWorkflowPublicationViewForm extends UIForm {
  private String repositoryName = "";
  private NodeLocation currentNode = null;
  private final String EXO_PUBLISH = "exo:published";
  private IdentityRegistry identityRegistry;
  private static final Log LOG  = ExoLogger.getLogger(UIWorkflowPublicationViewForm.class);

  public UIWorkflowPublicationViewForm() throws Exception {
    identityRegistry = getApplicationComponent(IdentityRegistry.class);
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
    setActions(new String[]{"Cancel"});
  }

  public void setCurrentNode(Node node) throws Exception {
    currentNode = NodeLocation.getNodeLocationByNode(node);
    String userId = node.getSession().getUserID();
    Property rolesProp = node.getProperty(WorkflowPublicationPlugin.VALIDATOR);
    Value roles = rolesProp.getValue();
    if (node.isNodeType(EXO_PUBLISH)) {
      if (node.getProperty(WorkflowPublicationPlugin.CURRENT_STATE)
                     .getString()
                     .equals(WorkflowPublicationPlugin.CONTENT_VALIDATION)
          && checkExcetuteable(userId, roles)) {
        PublicationService publicationService = getApplicationComponent(PublicationService.class);
        publicationService.getPublicationPlugins()
                          .get(WorkflowPublicationPlugin.WORKFLOW)
                          .changeState(node,
                                       WorkflowPublicationPlugin.PUBLISHED,
                                       new HashMap<String, String>());
        setActions(new String[]{"Unpublish", "Cancel"});
      } else if (node.getProperty(WorkflowPublicationPlugin.CURRENT_STATE).
          getString().equals(WorkflowPublicationPlugin.PUBLISHED) && checkExcetuteable(userId, roles)) {
        setActions(new String[]{"Unpublish", "Cancel"});
      } else {
        setActions(new String[]{"UnsubcriberLifeCycle", "Cancel"});
      }
    } else {
      setActions(new String[]{"Cancel"});
    }
  }

  public String getLinkStateImage (Locale locale) {
    try {
      DownloadService dS = getApplicationComponent(DownloadService.class);
      PublicationService service = getApplicationComponent(PublicationService.class);

      byte[] bytes = service.getStateImage(getCurrentNode(),locale);
      InputStream iS = new ByteArrayInputStream(bytes);
      String id = dS.addDownloadResource(new InputStreamDownloadResource(iS, "image/gif"));
      return dS.getDownloadLink(id);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getStateImage";
    }
  }

  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode);
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  private boolean checkExcetuteable(String userId, Value roles) throws Exception {
    if (IdentityConstants.SYSTEM.equalsIgnoreCase(userId)) {
      return true;
    }
    Identity identity = identityRegistry.getIdentity(userId);
    if(identity == null) {
      return false;
    }
    if("*".equalsIgnoreCase(roles.getString())) return true;
    MembershipEntry membershipEntry = MembershipEntry.parse(roles.getString());
    if (identity.isMemberOf(membershipEntry)) {
      return true;
    }
    return false;
  }

  public String getLabel(String fieldName, String type) throws Exception {
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    WorkflowPublicationPlugin plugin = (WorkflowPublicationPlugin) publicationService.getPublicationPlugins()
                                                                                     .get(WorkflowPublicationPlugin.WORKFLOW);
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    try {
      return plugin.getLocalizedAndSubstituteMessage(locale, getId() + "." + type +"." + fieldName, null);
    } catch(Exception e) {
      return fieldName;
    }
  }

  public void initPopup(UIContainer uiContainer, UIComponent uiComp) throws Exception {
    uiContainer.removeChildById(WorkflowPublicationPlugin.POPUP_EDIT_ID);
    UIPopupWindow uiPopup = uiContainer.addChild(UIPopupWindow.class, null, WorkflowPublicationPlugin.POPUP_EDIT_ID);
    uiPopup.setUIComponent(uiComp);
    uiPopup.setWindowSize(640, 300);
    uiPopup.setShow(true);
    uiPopup.setShowMask(true);
    uiPopup.setResizable(true);
  }

  static public class CancelActionListener extends EventListener<UIWorkflowPublicationViewForm> {
    public void execute(Event<UIWorkflowPublicationViewForm> event) throws Exception {
      UIWorkflowPublicationViewForm uiForm = event.getSource();
      UIContainer container = uiForm.getAncestorOfType(UIContainer.class);

      UIPopupWindow popupWindow = (UIPopupWindow)container.getParent();
      popupWindow.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());
    }
  }

  static public class EditActionListener extends EventListener<UIWorkflowPublicationViewForm> {
    public void execute(Event<UIWorkflowPublicationViewForm> event) throws Exception {
      UIWorkflowPublicationViewForm workflowViewForm = event.getSource();
      UIContainer container = workflowViewForm.getParent();
      UIWorkflowPublicationActionForm actionForm =
                container.createUIComponent(UIWorkflowPublicationActionForm.class, null, null);
      actionForm.createNewAction(workflowViewForm.getCurrentNode(), WorkflowPublicationPlugin.WORKFLOW, true);
      actionForm.setWorkspaceName(workflowViewForm.getCurrentNode().getSession().getWorkspace().getName());
      workflowViewForm.initPopup(container, actionForm);
      event.getRequestContext().addUIComponentToUpdateByAjax(workflowViewForm.getParent());
    }
  }

  static public class UnpublishActionListener extends EventListener<UIWorkflowPublicationViewForm> {
    public void execute(Event<UIWorkflowPublicationViewForm> event) throws Exception {
      UIWorkflowPublicationViewForm viewForm = event.getSource();
      RepositoryService repositoryService = viewForm.getApplicationComponent(RepositoryService.class);
      UIContainer container = viewForm.getParent();
      Node currentNode = viewForm.getCurrentNode();

      PublicationService publicationService = viewForm.getApplicationComponent(PublicationService.class);
      publicationService.getPublicationPlugins().get(WorkflowPublicationPlugin.WORKFLOW).
        changeState(currentNode, WorkflowPublicationPlugin.BACKUP, new HashMap<String,String>());

      String nodePath = currentNode.getPath();
      String srcWorkspace = currentNode.getSession().getWorkspace().getName();
      String destWorkspace = WorkflowPublicationPlugin.BACKUP;
      StringBuffer realDestPath = new StringBuffer();
      realDestPath.append(currentNode.getProperty(WorkflowPublicationPlugin.BACUP_PATH).getString());
      if (!"/".equals(realDestPath.toString())) {
        realDestPath.append("/");
      }
      realDestPath.append(currentNode.getName());
      WorkflowMoveNodeAction.moveNode(repositoryService, nodePath, srcWorkspace, destWorkspace,
            realDestPath.toString(), viewForm.getRepositoryName());

      UIApplication uiApp = viewForm.getAncestorOfType(UIApplication.class);

      WorkflowPublicationPlugin plugin = (WorkflowPublicationPlugin) publicationService.getPublicationPlugins()
                                                                                       .get(WorkflowPublicationPlugin.WORKFLOW);
      Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      String msg = plugin.getLocalizedAndSubstituteMessage(locale,
                                                           "UIWorkflowPublicationViewForm.msg.unpublish-success",
                                                           null);
      uiApp.addMessage(new ApplicationMessage(msg, null, ApplicationMessage.INFO));
      

      UIPopupWindow popupWindow = (UIPopupWindow)container.getParent();
      popupWindow.setRendered(false);
      UIComponent component = popupWindow.getParent();
      if (component != null && component.getParent() != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(component.getParent());
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(component);
      }
    }
  }

  static public class UnsubcriberLifeCycleActionListener extends EventListener<UIWorkflowPublicationViewForm> {
    public void execute(Event<UIWorkflowPublicationViewForm> event) throws Exception {
      UIWorkflowPublicationViewForm viewForm = event.getSource();
      Node selectedNode = viewForm.getCurrentNode();
      UIApplication uiApp = viewForm.getAncestorOfType(UIApplication.class);
      PublicationService publicationService = viewForm.getApplicationComponent(PublicationService.class);
      WorkflowPublicationPlugin plugin = (WorkflowPublicationPlugin) publicationService.getPublicationPlugins()
                                                                                       .get(WorkflowPublicationPlugin.WORKFLOW);
      Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();

      if (!selectedNode.isCheckedOut()) {
        String msg = plugin.getLocalizedAndSubstituteMessage(locale,
                                                             "UIWorkflowPublicationActionForm.msg.node-checkedin",
                                                             null);
        uiApp.addMessage(new ApplicationMessage(msg, null, ApplicationMessage.WARNING));
        
        return;
      }

      if (publicationService.isUnsubcribeLifecycle(selectedNode)) {
        String msg = plugin.getLocalizedAndSubstituteMessage(locale,
                                                             "UIWorkflowPublicationActionForm.msg.unsubcriber-lifecycle",
                                                             null);
        uiApp.addMessage(new ApplicationMessage(msg, null, ApplicationMessage.WARNING));
        
        return;
      }

      UIContainer container = viewForm.getAncestorOfType(UIContainer.class);
      UIPopupWindow popupWindow = (UIPopupWindow)container.getParent();
      popupWindow.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());

      /*
       * Unsubcribe lifecycle and display message to inform
       */
      publicationService.unsubcribeLifecycle(selectedNode);
      String msg = plugin.getLocalizedAndSubstituteMessage(locale,
                                                           "UIWorkflowPublicationActionForm.msg.unsubcriber-lifecycle-finish",
                                                           null);
      uiApp.addMessage(new ApplicationMessage(msg, null));
      
      return;
    }
  }
}
