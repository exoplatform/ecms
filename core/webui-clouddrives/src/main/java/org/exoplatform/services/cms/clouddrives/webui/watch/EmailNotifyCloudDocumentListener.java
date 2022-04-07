/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives.webui.watch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.MailUtils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.cms.watch.impl.EmailNotifyListener;
import org.exoplatform.services.cms.watch.impl.MessageConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
/**
 * This is a COPY of ECMS {@link EmailNotifyListener} with proposed fix of
 * https://jira.exoplatform.org/browse/ECMS-5973.<br>
 * Created by The eXo Platform SAS Author : Xuan Hoa Pham
 * hoapham@exoplatform.com phamvuxuanhoa@gmail.com Dec 6, 2006
 */
public class EmailNotifyCloudDocumentListener implements EventListener {

  /** The observed node. */
  private NodeLocation        observedNode_;

  /** The Constant EMAIL_WATCHERS_PROP. */
  final public static String  EMAIL_WATCHERS_PROP = "exo:emailWatcher";

  /** The Constant LOG. */
  private static final Log    LOG                 = ExoLogger.getLogger(EmailNotifyCloudDocumentListener.class.getName());

  /**
   * Instantiates a new email notify cloud document listener.
   *
   * @param oNode the o node
   */
  public EmailNotifyCloudDocumentListener(Node oNode) {
    observedNode_ = NodeLocation.getNodeLocationByNode(oNode);
  }

  /**
   * This method is used for listening to all changes of property of a node,
   * when there is a change, message is sent to list of email.
   *
   * @param arg0 the arg 0
   */
  public void onEvent(EventIterator arg0) {
    MailService mailService = WCMCoreUtils.getService(MailService.class);
    WatchCloudDocumentServiceImpl watchService =
                                               (WatchCloudDocumentServiceImpl) WCMCoreUtils.getService(WatchDocumentService.class);
    MessageConfig messageConfig = watchService.getMessageConfig();
    String sender = MailUtils.getSenderName() + "<" + MailUtils.getSenderEmail() + ">";
    messageConfig.setSender(sender);
    try {
      Node node = NodeLocation.getNodeByLocation(observedNode_);
      NodeLocation nodeLocation = node.isNodeType(NodetypeConstant.NT_RESOURCE) ? NodeLocation.getNodeLocationByNode(node.getParent()) : observedNode_;
      List<String> emailList = getEmailList(NodeLocation.getNodeByLocation(nodeLocation));
      for (String receiver : emailList) {
        notifyUser(receiver, messageConfig, mailService);
      }
    } catch (RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unable to get node location", e);
        }
    }
  }

  /**
   * Create message when there is any changes of property of a node.
   *
   * @param receiver the receiver
   * @param messageConfig the message config
   * @return the message
   * @throws Exception the exception
   */
  private Message createMessage(String receiver, MessageConfig messageConfig) throws Exception {
    Message message = new Message();
    message.setFrom(messageConfig.getSender());
    message.setTo(receiver);
    message.setSubject(messageConfig.getSubject());
    TemplateEngine engine = new GStringTemplateEngine();
    Map<String, String> binding = new HashMap<String, String>();
    Query query = new Query();
    query.setEmail(receiver);
    binding.put("user_name",
                WCMCoreUtils.getService(OrganizationService.class)
                            .getUserHandler()
                            .findUsersByQuery(query)
                            .load(0, 1)[0].getFullName());

    Node node = NodeLocation.getNodeByLocation(observedNode_);
    binding.put("doc_title", org.exoplatform.services.cms.impl.Utils.getTitle(node));
    binding.put("doc_name", node.getName());
    binding.put("doc_url", getViewableLink(node)); // XXX instead of getViewableLink()
    message.setBody(engine.createTemplate(messageConfig.getContent()).make(binding).toString());
    message.setMimeType(messageConfig.getMimeType());
    return message;
  }

  /**
   * Used in
   * {@link WatchCloudDocumentServiceImpl#watchDocument(Node, String, int)}.
   *
   * @return the viewable link
   * @throws Exception the exception
   */
  String getViewableLink(Node node) throws Exception {
    // Exemple Link : http://localhost:8080/portal/private/rest/documents/view/collaboration/f3cf201e7f0001016c28b6ac54feb98e
    return CommonsUtils.getCurrentDomain() + Utils.getContentLink(node);
  }
  /**
   * Gets the memberships.
   *
   * @return the memberships
   */
  public List<String> getMemberships() {
    String userId = Util.getPortalRequestContext().getRemoteUser();
    List<String> userMemberships = new ArrayList<String>();
    userMemberships.add(userId);
    // here we must retrieve memberships of the user using the
    // IdentityRegistry Service instead of Organization Service to
    // allow JAAS based authorization
    Collection<MembershipEntry> memberships = getUserMembershipsFromIdentityRegistry(userId);
    if (memberships != null) {
      for (MembershipEntry membership : memberships) {
        String role = membership.getMembershipType() + ":" + membership.getGroup();
        userMemberships.add(role);
      }
    }
    return userMemberships;
  }

  /**
   * Gets the user memberships from identity registry.
   *
   * @param authenticatedUser the authenticated user
   * @return the user memberships from identity registry
   */
  private Collection<MembershipEntry> getUserMembershipsFromIdentityRegistry(String authenticatedUser) {
    IdentityRegistry identityRegistry = WCMCoreUtils.getService(IdentityRegistry.class);
    Identity currentUserIdentity = identityRegistry.getIdentity(authenticatedUser);
    return currentUserIdentity.getMemberships();
  }

  /**
   * This Method will get email of watchers when they watch a document.
   *
   * @param observedNode the observed node
   * @return the email list
   */
  private List<String> getEmailList(Node observedNode) {
    List<String> emailList = new ArrayList<String>();
    OrganizationService orgService = WCMCoreUtils.getService(OrganizationService.class);
    try {
      if (observedNode.hasProperty(EMAIL_WATCHERS_PROP)) {
        Value[] watcherNames = observedNode.getProperty(EMAIL_WATCHERS_PROP).getValues();
        for (Value value : watcherNames) {
          String userName = value.getString();
          User user = orgService.getUserHandler().findUserByName(userName);
          if (user != null) {
            emailList.add(user.getEmail());
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return emailList;
  }
  private void notifyUser(String receiver, MessageConfig messageConfig, MailService mailService) {
    try {
      Message message = createMessage(receiver, messageConfig);
      mailService.sendMessage(message);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }
}
