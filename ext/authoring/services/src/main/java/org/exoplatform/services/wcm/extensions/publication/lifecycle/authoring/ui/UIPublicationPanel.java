/*
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
 */
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormDateTimeInput;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/wcm/webui/publication/lifecycle/authoring/ui/UIPublicationPanel.gtmpl",
                 events = {
    @EventConfig(listeners = UIPublicationPanel.ChangeStateActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.ChangeVersionActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.PreviewVersionActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.RestoreVersionActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.SeeAllVersionActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.SaveActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.ResetActionListener.class),
    @EventConfig(listeners = UIPublicationPanel.CloseActionListener.class) })
public class UIPublicationPanel
                               extends
                               org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationPanel {
  final static public String OBJECTID          = "objectId";

  public static final String START_PUBLICATION = "UIPublicationPanelStartDateInput";

  public static final String END_PUBLICATION   = "UIPublicationPanelEndDateInput";

  public static final Log    LOG               = LogFactory.getLog(UIPublicationPanel.class);

  /**
   * Instantiates a new uI publication panel.
   *
   * @throws Exception the exception
   */
  public UIPublicationPanel() throws Exception {
    addUIFormInput(new UIFormDateTimeInput(START_PUBLICATION, START_PUBLICATION, null));
    addUIFormInput(new UIFormDateTimeInput(END_PUBLICATION, END_PUBLICATION, null));
    setActions(new String[] { "Save","Reset", "Close" });
  }

  public void init(Node node) throws Exception {
    Calendar startDate = null;
    Calendar endDate = null;
    String nodeVersionUUID = null;
    super.init(node);
    if (PublicationDefaultStates.PUBLISHED.equals(node.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE)
                                                      .getString())) {
      nodeVersionUUID = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)
                            .getString();
      if (!"".equals(nodeVersionUUID)) {
        Node revision = this.getRevisionByUUID(nodeVersionUUID);
        this.setCurrentRevision(revision);
      }
    }
    if (node.hasProperty(AuthoringPublicationConstant.END_TIME_PROPERTY)) {
      endDate = node.getProperty(AuthoringPublicationConstant.END_TIME_PROPERTY).getDate();
    }
    if (node.hasProperty(AuthoringPublicationConstant.START_TIME_PROPERTY)) {
      startDate = node.getProperty(AuthoringPublicationConstant.START_TIME_PROPERTY).getDate();
    }
    if (startDate != null) {
      ((UIFormDateTimeInput) getChildById(START_PUBLICATION)).setCalendar(startDate);
    }
    if (endDate != null) {
      ((UIFormDateTimeInput) getChildById(END_PUBLICATION)).setCalendar(endDate);
    }

  }

  /**
   * The listener interface for receiving draftAction events. The class that is
   * interested in processing a draftAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addDraftActionListener<code> method. When
   * the draftAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DraftActionEvent
   */
  public static class ChangeStateActionListener extends EventListener<UIPublicationPanel> {

    /*
     * (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform
     * .webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      String state = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node currentNode = publicationPanel.getCurrentNode();
      PublicationService publicationService = publicationPanel.getApplicationComponent(PublicationService.class);
      WCMPublicationService wcmPublicationService = publicationPanel.getApplicationComponent(WCMPublicationService.class);
      PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
                                                              .get(AuthoringPublicationConstant.LIFECYCLE_NAME);
      HashMap<String, String> context = new HashMap<String, String>();
      Node currentRevision = publicationPanel.getCurrentRevision();
      if (currentRevision != null) {
        context.put(AuthoringPublicationConstant.CURRENT_REVISION_NAME, currentRevision.getName());
      }
      try {
        if(currentNode.isLocked()) {
          currentNode.getSession().addLockToken(LockUtil.getLockToken(currentNode));
        }
        
        publicationPlugin.changeState(currentNode, state, context);
        currentNode.setProperty("publication:lastUser", event.getRequestContext().getRemoteUser());
        
        String nodeVersionUUID = null;
        if(currentNode.hasProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP))
          nodeVersionUUID = currentNode.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP).getString();
        if (nodeVersionUUID != null && !nodeVersionUUID.isEmpty()) {          
          publicationPanel.setCurrentRevision(publicationPanel.getRevisionByUUID(nodeVersionUUID));
        }
        String siteName = Util.getPortalRequestContext().getPortalOwner();
        String remoteUser = Util.getPortalRequestContext().getRemoteUser();
        wcmPublicationService.updateLifecyleOnChangeContent(currentNode, siteName, remoteUser, state);
        publicationPanel.updatePanel();
      } catch (Exception e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        JCRExceptionManager.process(uiApp, e);
      }
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());      
    }
  }


  public static class SaveActionListener extends EventListener<UIPublicationPanel> {
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      UIFormDateTimeInput startPublication = publicationPanel.getChildById(START_PUBLICATION);
      UIFormDateTimeInput endPublication = publicationPanel.getChildById(END_PUBLICATION);
      Calendar startDate = startPublication.getCalendar();
      Calendar endDate = endPublication.getCalendar();
      Node node = publicationPanel.getCurrentNode();
      try {
        if (startDate != null && endDate != null && startDate.after(endDate)) {
          UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.fromDate-after-toDate", null));
          return;
        }
        
        if(node.isLocked()) {
          node.getSession().addLockToken(LockUtil.getLockToken(node));
        }
        
        if ((!"".equals(startPublication.getValue())) || (!"".equals(endPublication.getValue()))) {
          if (!"".equals(startPublication.getValue()))
            node.setProperty(AuthoringPublicationConstant.START_TIME_PROPERTY, startDate);
          if (!"".equals(endPublication.getValue()))
            node.setProperty(AuthoringPublicationConstant.END_TIME_PROPERTY, endDate);
          node.getSession().save();
        }   
      } catch (NullPointerException e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.invalid-format", null));
        
        return;
      } catch (ItemExistsException iee) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error when adding properties to node");
        }
      }
      UIPopupContainer uiPopupContainer = (UIPopupContainer) publicationPanel.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
  
  public static class ResetActionListener extends EventListener<UIPublicationPanel> {
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      Node node = publicationPanel.getCurrentNode();
      UIFormDateTimeInput startPublication = publicationPanel.getChildById(START_PUBLICATION);
      startPublication.setCalendar(null);
      if (node.hasProperty(AuthoringPublicationConstant.START_TIME_PROPERTY)) {
        node.getProperty(AuthoringPublicationConstant.START_TIME_PROPERTY).remove();
        node.save();
      }
      UIFormDateTimeInput endPublication = publicationPanel.getChildById(END_PUBLICATION);
      endPublication.setCalendar(null);
      if (node.hasProperty(AuthoringPublicationConstant.END_TIME_PROPERTY)) {
        node.getProperty(AuthoringPublicationConstant.END_TIME_PROPERTY).remove();
        node.save();
      }
    }
  }

  public List<State> getStates(Node cNode) throws Exception {
    List<State> states = new ArrayList<State>();
    String lifecycleName = getLifeCycle(cNode);
    PublicationManager publicationManagerImpl = getApplicationComponent(PublicationManager.class);
    Lifecycle lifecycle = publicationManagerImpl.getLifecycle(lifecycleName);
    states = lifecycle.getStates();
    return states;
  }

  private String getLifeCycle(Node cNode) throws Exception {
    String lifecycleName = null;
    try {
      lifecycleName = cNode.getProperty("publication:lifecycle").getString();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Failed to get States for node " + cNode, e);
      }
    }
    return lifecycleName;
  }

  /**
   * Check if a user is authorized to reach the given state of a given  node.
   * The user must satisfy the constraints defined by state (memberships or role)
   * @param state
   * @param remoteUser
   * @param node
   * @return
   */
  public boolean canReachState(State state, String remoteUser, NodeImpl node) {

    IdentityRegistry identityRegistry = getApplicationComponent(IdentityRegistry.class);
    Identity currentUser = identityRegistry.getIdentity(remoteUser);

    if (isAuthorizedByMembership(state, currentUser)) {
      return true;
    }

    if (isAuthorizedByRole(state, currentUser, node)) {
      return true;
    }

    return false;
  }

  /**
   * Check if the user has the memberships defined in the state
   * @param state
   * @param currentUser
   * @return
   */
  boolean isAuthorizedByMembership(State state, Identity currentUser) {
    String membership = state.getMembership();
    List<String> memberships = new ArrayList<String>();
    if (membership != null) {
      memberships.add(membership);
    }
    if (state.getMemberships() != null) {
      memberships.addAll(state.getMemberships());
    }

    for (String membership_ : memberships) {
      String[] membershipTab = membership_.split(":");
      String expectedRole = membershipTab[0];
      String expectedGroup = membershipTab[1];
      if (currentUser.isMemberOf(expectedGroup, expectedRole)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a user is authorized to reach the state based on the state's role.
   * The user must have the role
   * @param state
   * @param currentUser
   * @param node
   * @return
   */
  boolean isAuthorizedByRole(State state, Identity currentUser, NodeImpl node) {
    try {
      String role_ = state.getRole();

      List<String> roles = new ArrayList<String>();
      if (role_ != null) {
        roles.add(role_);
      }
      if (state.getRoles() != null) {
        roles.addAll(state.getRoles());
      }
      for (String role : roles) {

        AccessControlList acl = node.getACL();
        if (acl.hasPermissions()) {
          List<AccessControlEntry> entries = acl.getPermissionEntries();
          for (AccessControlEntry accessControlEntry : entries) {
            String identity = accessControlEntry.getIdentity();
            if (identity.indexOf(':') > 0) {
              // write access on node is defined by 'set_property' in exo JCR
              if (PermissionType.SET_PROPERTY.equals(accessControlEntry.getPermission())) {
                String authorizedGroup = identity.split(":")[1];
                // user must have the configured role in one of the node's
                // authorized groups
                if (currentUser.isMemberOf(authorizedGroup, role)) {
                  return true;
                }
              }
            }
          }
        }

      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Failed to extract node permissions", e);
      }
    }
    return false;
  }




}
