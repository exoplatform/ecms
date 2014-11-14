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
package org.exoplatform.wcm.authoring.listener;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Benjamin Paillereau
 *          benjamin.paillereau@exoplatform.com
 * May 31, 2010
 */
public class PostUpdateStateEventListener extends Listener<CmsService, Node> {

  private static final Log   LOG = ExoLogger.getLogger(PostUpdateStateEventListener.class.getName());

  /** The pservice. */
  private PublicationManager publicationManager;

  /**
   * Instantiates a new post edit content event listener.
   *
   * @param pservice the pservice
   */
  public PostUpdateStateEventListener(PublicationManager publicationManager) {
    this.publicationManager = publicationManager;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services
   * .listener.Event)
   */
  public void onEvent(Event<CmsService, Node> event) throws Exception {
    Node node = event.getData();

    String userId;
    try {
      userId = Util.getPortalRequestContext().getRemoteUser();
    } catch (Exception e) {
      userId = node.getSession().getUserID();
    }

    String currentState = node.getProperty("publication:currentState").getString();
    if (!"enrolled".equals(currentState)) {
      String nodeLifecycle = node.getProperty("publication:lifecycle").getString();
      // if (log.isInfoEnabled())
      // log.info(userId+"::"+currentState+"::"+nodeLifecycle);
      if (LOG.isInfoEnabled())
        LOG.info("@@@ " + currentState + " @@@@@@@@@@@@@@@@@@@ " + node.getPath());

      Lifecycle lifecycle = publicationManager.getLifecycle(nodeLifecycle);
      Iterator<State> states = lifecycle.getStates().iterator();
      State prevState = null;
      while (states.hasNext()) {
        State state = states.next();
        if (state.getState().equals(currentState)) {
          sendMail(node, state, userId, false, false);
          if ("published".equals(state.getState()) && prevState != null) {
            sendMail(node, prevState, userId, false, true);
          }
          if (states.hasNext()) {
            State nextState = states.next();
            sendMail(node, nextState, userId, true, false);
            break;
          }
        }
        prevState = state;
      }

      try {
        UIPortalApplication portalApplication = Util.getUIPortalApplication();
        PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
        UIWorkingWorkspace uiWorkingWS = portalApplication.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
        portalRequestContext.addUIComponentToUpdateByAjax(uiWorkingWS);
        portalRequestContext.ignoreAJAXUpdateOnPortlets(true);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }

    }
  }

  private void sendMail(Node node,
                        State state,
                        String userId,
                        boolean isNextState,
                        boolean isPublished) throws Exception {
    if (state.getMembership().contains(":")) {
      String[] membership = state.getMembership().split(":");
      String membershipType = membership[0];
      String group = membership[1];
      OrganizationService orgService = WCMCoreUtils.getService(OrganizationService.class);
      UserHandler userh = orgService.getUserHandler();
      MembershipHandler msh = orgService.getMembershipHandler();

      ListAccess<User> userList = userh.findUsersByGroupId(group);
      User currentUser = null;
      try {
        currentUser = userh.findUserByName(userId);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      String username = userId;
      if (currentUser != null)
        username = currentUser.getFirstName() + " " + currentUser.getLastName();
      for (User user : userList.load(0, userList.getSize())) {
        Collection<Membership> mss = msh.findMembershipsByUserAndGroup(user.getUserName(), group);
        for (Membership ms : mss) {
          if (membershipType.equals(ms.getMembershipType())) {
            String from = "\"" + username + "\" <exocontent@exoplatform.com>";
            String to = user.getEmail();
            String subject, body;
            String editUrl = "http://localhost:8080/ecmdemo/private/classic/siteExplorer/repository/collaboration"
                + node.getPath();
            if (isPublished) {
              subject = "[eXo Content] Published : (published) " + node.getName();
            } else {
              if (isNextState) {
                subject = "[eXo Content] Request : (" + state.getState() + ") " + node.getName();
              } else {
                subject = "[eXo Content] Updated : (" + state.getState() + ") " + node.getName();
              }
            }
            body = "[ <a href=\"" + editUrl + "\">" + editUrl + "</a> ]<br/>" + "updated by "
                + username;
            // mailService.sendMessage(from, to, subject, body);
            if (LOG.isInfoEnabled()) {
              LOG.info("\n################ SEND MAIL TO USER :: " + user.getUserName() + "\nfrom: "
                  + from + "\nto: " + to + "\nsubject: " + subject + "\nbody: " + body
                  + "\n######################################################");
            }

          }
        }
      }
    }

  }

}
