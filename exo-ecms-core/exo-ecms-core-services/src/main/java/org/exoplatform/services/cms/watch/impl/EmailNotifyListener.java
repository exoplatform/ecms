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
package org.exoplatform.services.cms.watch.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;

/**
 * Created by The eXo Platform SAS
 * Author : Xuan Hoa Pham
 *          hoapham@exoplatform.com
 * 					phamvuxuanhoa@gmail.com
 * Dec 6, 2006  
 */
public class EmailNotifyListener implements EventListener {  
  
  private Node observedNode_ ;
  final public static String EMAIL_WATCHERS_PROP = "exo:emailWatcher".intern() ;
  private static final Log LOG  = ExoLogger.getLogger(EmailNotifyListener.class);
  
  public EmailNotifyListener(Node oNode) {
    observedNode_ = oNode ;
  }
  
  /**
   * This method is used for listening to all changes of property of a node, when there is a change,
   * message is sent to list of email
   */
  @SuppressWarnings("unused")
  public void onEvent(EventIterator arg0) {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    MailService mailService = 
      (MailService)container.getComponentInstanceOfType(MailService.class) ;
    WatchDocumentServiceImpl watchService= 
      (WatchDocumentServiceImpl)container.getComponentInstanceOfType(WatchDocumentService.class) ;
    MessageConfig messageConfig = watchService.getMessageConfig() ;
    List<String> emailList = getEmailList(observedNode_) ;
    for(String receiver: emailList) {      
      Message message = createMessage(receiver,messageConfig) ;
      try {
        mailService.sendMessage(message) ; 
      }catch (Exception e) {
        System.out.println("===> Exeption when send message to: " + message.getTo());
        LOG.error("Unexpected error", e);        
      }      
    }
  }
  
  /**
   * Create message when there is any changes of property of a node.
   * @param receiver
   * @param messageConfig
   * @return
   */
  private Message createMessage(String receiver, MessageConfig messageConfig) {
    Message message = new Message() ;
    message.setFrom(messageConfig.getSender()) ;
    message.setTo(receiver) ;
    message.setSubject(messageConfig.getSubject()) ;
    message.setBody(messageConfig.getContent()) ;
    message.setMimeType(messageConfig.getMimeType()) ;
    return message ;
  }
  
  /**
   * This Method will get email of watchers when they watch a document 
   * @param observedNode
   * @return
   */
  private List<String> getEmailList(Node observedNode) {
    List<String> emailList = new ArrayList<String>() ;
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    OrganizationService orgService = 
      (OrganizationService)container.getComponentInstanceOfType(OrganizationService.class) ;
    
    try{
      if(observedNode.hasProperty(EMAIL_WATCHERS_PROP)) {
        Value[] watcherNames = observedNode.getProperty(EMAIL_WATCHERS_PROP).getValues() ;
        for(Value value: watcherNames) {  
          String userName = value.getString() ;
          User user = orgService.getUserHandler().findUserByName(userName) ;
          if(user != null) {
            emailList.add(user.getEmail()) ;
          }
        }
      } 
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }
    return emailList ;
  }
}
  
