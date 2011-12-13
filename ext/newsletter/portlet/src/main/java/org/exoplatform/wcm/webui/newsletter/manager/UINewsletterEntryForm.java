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
package org.exoplatform.wcm.webui.newsletter.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.portlet.PortletRequest;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.DialogFormActionListeners;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.newsletter.UINewsletterConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 11, 2009
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig (listeners = UINewsletterEntryForm.SaveActionListener.class),
      @EventConfig (listeners = UINewsletterEntryForm.SendActionListener.class),
      @EventConfig (listeners = UINewsletterEntryForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig (listeners = DialogFormActionListeners.RemoveDataActionListener.class),
      @EventConfig (listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UINewsletterEntryForm extends UIDialogForm {

  /**
   * Instantiates a new uI newsletter entry form.
   *
   * @throws Exception the exception
   */
  public UINewsletterEntryForm() throws Exception {
    setActions(new String [] {"Save", "Send", "Cancel"});
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#getTemplate()
   */
  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try{
      return templateService.getTemplatePathByUser(true, "exo:webContent", userName);
    } catch(Exception e) {
      Utils.createPopupMessage(this, "UINewsletterEntryForm.msg.get-template", null, ApplicationMessage.ERROR);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(
   * org.exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    try {
      if (resourceResolver == null) {
        DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
        String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
        resourceResolver = new JCRResourceResolver(workspace);
      }
    } catch (Exception e) {
      Utils.createPopupMessage(this,
                               "UINewsletterEntryForm.msg.get-template-resource",
                               null,
                               ApplicationMessage.ERROR);
    }
    return resourceResolver;
  }

  /**
   * Save content.
   *
   * @param isSend the is send
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  private Node saveContent(boolean isSend) throws Exception {
    boolean isNull = true;
    // Prepare node store location
    UINewsletterEntryContainer newsletterEntryContainer = getAncestorOfType(UINewsletterEntryContainer.class);
    UINewsletterEntryDialogSelector newsletterEntryDialogSelector = newsletterEntryContainer.
        getChild(UINewsletterEntryDialogSelector.class);
    String selectedCategory = ((UIFormSelectBox) newsletterEntryDialogSelector.
        getChildById(UINewsletterConstant.ENTRY_CATEGORY_SELECTBOX)).getValue();
    String selectedSubsctiption = ((UIFormSelectBox) newsletterEntryDialogSelector.
        getChildById(UINewsletterConstant.ENTRY_SUBSCRIPTION_SELECTBOX)).getValue();
    setStoredPath(NewsletterConstant.generateSubscriptionPath(NewsLetterUtil.getPortalName(),
                                                              selectedCategory,
                                                              selectedSubsctiption));

    // Prepare node: use title as a node name
    Map<String, JcrInputProperty> inputProperties = DialogFormUtil.prepareMap(getChildren(),
                                                                              getInputProperties(),
                                                                              getInputOptions());
    if(isAddNew()){
      String nodeName = Utils.cleanString(getUIStringInput("title").getValue());
      inputProperties.get("/node").setValue(nodeName);
    }

    // Store node
    String storedPath = getStoredPath().replace(NewsletterConstant.PORTAL_NAME, NewsLetterUtil.getPortalName());
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, manageableRepository);
    Node storedNode = (Node)session.getItem(storedPath);
    CmsService cmsService = getApplicationComponent(CmsService.class);
    String newsletterNodePath = cmsService.storeNode("exo:webContent", storedNode, inputProperties, isAddNew());

    // Add newsletter mixin type
    Node newsletterNode = (Node)session.getItem(newsletterNodePath);
    if(isAddNew() && newsletterNode.canAddMixin(NewsletterConstant.ENTRY_NODETYPE))
      newsletterNode.addMixin(NewsletterConstant.ENTRY_NODETYPE);
    newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_CATEGORY_NAME, selectedCategory);
    newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_SUBSCRIPTION_NAME, selectedSubsctiption);
    newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_TYPE, newsletterEntryDialogSelector.getDialog());
    Calendar calendar = ((UIFormDateTimeInput)newsletterEntryDialogSelector.
                              getChildById(UINewsletterEntryDialogSelector.NEWSLETTER_ENTRY_SEND_DATE)).getCalendar();
    if(calendar==null) calendar = Calendar.getInstance();
    newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_DATE, calendar);
    Date currentDate = new Date();
    if(isSend){
      if(calendar.getTimeInMillis() >= currentDate.getTime()){
        newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_AWAITING);
        isNull = true;
      }else{
        isNull = false;
        newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_SENT);
      }
    }else{
      newsletterNode.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_DRAFT);
    }
    session.save();

    // Close popup and update UI
    UINewsletterManagerPortlet managerPortlet = getAncestorOfType(UINewsletterManagerPortlet.class);
    UINewsletterEntryManager entryManager = managerPortlet.getChild(UINewsletterEntryManager.class);
    if(entryManager.isRendered()) entryManager.init();
    Utils.closePopupWindow(this, UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW);

    if(isNull) return null;
    return newsletterNode;
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UINewsletterEntryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryForm> event) throws Exception {
      UINewsletterEntryForm newsletterEntryForm = event.getSource();
      UINewsletterEntryContainer newsletterEntryContainer = newsletterEntryForm.
          getAncestorOfType(UINewsletterEntryContainer.class);
      if(!newsletterEntryContainer.isUpdated()){
        UIApplication uiApp = newsletterEntryContainer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.UpdateBeforeSave",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      try{
        newsletterEntryForm.saveContent(false);
      }catch(ItemExistsException itemExistsException){
        UIApplication uiApp = newsletterEntryContainer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.NodeNameInvalid",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
    }
  }

  /**
   * The listener interface for receiving sendAction events.
   * The class that is interested in processing a sendAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSendActionListener<code> method. When
   * the sendAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SendActionEvent
   */
  public static class SendActionListener extends EventListener<UINewsletterEntryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryForm> event) throws Exception {
      UINewsletterEntryForm newsletterEntryForm = event.getSource();
      UINewsletterEntryContainer newsletterEntryContainer = newsletterEntryForm.
          getAncestorOfType(UINewsletterEntryContainer.class);
      PortletRequestContext portletRequestContext = (PortletRequestContext) event.getRequestContext();
      PortletRequest portletRequest = portletRequestContext.getRequest();
      if (!newsletterEntryContainer.isUpdated()) {
        UIApplication uiApp = newsletterEntryContainer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.UpdateBeforeSave",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      Node newsletterNode = null;
      try {
        newsletterNode = newsletterEntryForm.saveContent(true);
      } catch (ItemExistsException itemExistsException) {
        UIApplication uiApp = newsletterEntryContainer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UINewsletterEntryForm.msg.NodeNameInvalid",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      if(newsletterNode != null){
        Session session = newsletterNode.getSession();
        ExoContainer container = ExoContainerContext.getCurrentContainer() ;
        MailService mailService = (MailService)container.getComponentInstanceOfType(MailService.class) ;
        Message message = null;
        List<String> listEmailAddress = new ArrayList<String>();
        StringBuffer sbReceiver = new StringBuffer();
        Node subscriptionNode = newsletterNode.getParent();
        NewsletterManagerService newsletterManagerService = newsletterEntryForm.
            getApplicationComponent(NewsletterManagerService.class);
        if(subscriptionNode.hasProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER)){
          List<String> listEmailBanned = newsletterManagerService.getAllBannedUser();
          Property subscribedUserProperty = subscriptionNode.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_USER);
          for(Value value : subscribedUserProperty.getValues()){
            try {
              if(!listEmailBanned.contains(value.getString()))listEmailAddress.add(value.getString());
            } catch (Exception e) {
              Utils.createPopupMessage(newsletterEntryForm,
                                       "UINewsletterEntryForm.msg.add-email-newsletter",
                                       null,
                                       ApplicationMessage.ERROR);
            }
          }
        }
        if (listEmailAddress.size() > 0) {
          message = new Message() ;
          message.setTo(listEmailAddress.get(0));
          for (int i = 1; i < listEmailAddress.size(); i ++) {
            sbReceiver.append(listEmailAddress.get(i)).append(",");
          }
          String content = 
            newsletterManagerService.getEntryHandler().getContent(WCMCoreUtils.getUserSessionProvider(), newsletterNode);
          String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName()
              + ":" + String.format("%s", portletRequest.getServerPort());
          String data = newsletterNode.getNode("default.html").getNode("jcr:content").getProperty("jcr:data").getString();
          String url = "";
          int index= 0;
          String link;
          do{
            if(data.indexOf("<a", index) >= 0) {
              index = data.indexOf("href=", index) + "href=".length() + 1;
              int indexEndLink = data.indexOf(">", index);
              link = data.substring(index, indexEndLink);
              if(link.startsWith("/")) {
                url = baseURI + link;
                url = url.replaceAll(" ", "%20");
                content = content.replaceAll(link, url);
              }else{
                content = content.replaceAll(link, link.replaceAll(" ", "%20"));
              }
            } else if(data.indexOf("<img", index) >= 0) {
              index = data.indexOf("src=", index) + "src=".length() + 1;
              int indexEndImg = data.indexOf("/>", index);
              link = data.substring(index, indexEndImg);
              if(link.startsWith("/")) {
                url = baseURI + link;
                url = url.replaceAll(" ", "%20");
                content = content.replaceAll(link, url);
              }
            } else {
              break;
            }
          } while(index >= 0);
          message.setBCC(sbReceiver.toString());
          message.setSubject(newsletterNode.getName()) ;
          message.setBody(content) ;
          message.setMimeType("text/html") ;
          try {
            mailService.sendMessage(message);
          } catch (Exception e) {
            Utils.createPopupMessage(
                newsletterEntryForm, "UINewsletterEntryForm.msg.send-newsletter", null, ApplicationMessage.ERROR);
          }
        }
        session.save();
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UINewsletterEntryForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryForm> event) throws Exception {
      UINewsletterEntryForm newsletterEntryForm = event.getSource();
      Utils.closePopupWindow(newsletterEntryForm, UINewsletterConstant.ENTRY_FORM_POPUP_WINDOW);
    }
  }
}
