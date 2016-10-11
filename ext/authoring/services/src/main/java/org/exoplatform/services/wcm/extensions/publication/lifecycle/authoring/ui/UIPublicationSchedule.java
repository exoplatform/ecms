/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 7, 2013  
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
template = "app:/groovy/webui/component/explorer/popup/action/UIPublicationSchedule.gtmpl",
events = {
  @EventConfig(listeners = UIPublicationSchedule.SaveActionListener.class),
  @EventConfig(listeners = UIPublicationSchedule.ResetActionListener.class)
})
public class UIPublicationSchedule extends UIForm {
  public static final String START_PUBLICATION = "UIPublicationPanelStartDateInput";
  public static final String END_PUBLICATION   = "UIPublicationPanelEndDateInput";
  private static final Log    LOG               = LogFactory.getLog(UIPublicationSchedule.class.getName());
  
  public UIPublicationSchedule() throws Exception {
    addUIFormInput(new UIFormDateTimeInput(START_PUBLICATION, START_PUBLICATION, null,true,true).addValidator(DateTimeValidator.class));
    addUIFormInput(new UIFormDateTimeInput(END_PUBLICATION, END_PUBLICATION, null,true,true).addValidator(DateTimeValidator.class));
    setActions(new String[] { "Save", "Reset" });
  }
  
  public void init(Node node) throws Exception {
    Calendar startDate = null;
    Calendar endDate = null;
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

  public boolean hasPublicationSchedule() throws RepositoryException {
    Node currentNode =
        this.getAncestorOfType(UIPublicationContainer.class).getChild(UIPublicationPanel.class).getCurrentNode();
    if (currentNode == null) return false;
    
    return (currentNode.hasProperty(AuthoringPublicationConstant.END_TIME_PROPERTY)
        || currentNode.hasProperty(AuthoringPublicationConstant.START_TIME_PROPERTY));
  }
  
  public static class SaveActionListener extends EventListener<UIPublicationSchedule> {
    public void execute(Event<UIPublicationSchedule> event) throws Exception {
      UIPublicationSchedule publicationSchedule = event.getSource();
      UIApplication uiApp = publicationSchedule.getAncestorOfType(UIApplication.class);
      UIPublicationPanel publicationPanel =
          publicationSchedule.getAncestorOfType(UIPublicationContainer.class).getChild(UIPublicationPanel.class);
      UIFormDateTimeInput startPublication = publicationSchedule.getChildById(START_PUBLICATION);
      UIFormDateTimeInput endPublication = publicationSchedule.getChildById(END_PUBLICATION);
      String startValue = startPublication.getValue();
      String endValue = endPublication.getValue();
      Calendar startDate = startPublication.getCalendar();
      Calendar endDate = endPublication.getCalendar();
      SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
      String startdateconv=format.format(format.parse(startValue));
      String enddateconv=format.format(format.parse(endValue));
      startDate.setTime(format.parse(startdateconv));
      endDate.setTime(format.parse(enddateconv));
      Node node = publicationPanel.getCurrentNode();
      try {
        if ((startDate == null && StringUtils.isNotEmpty(startValue))
          || (endDate == null && StringUtils.isNotEmpty(endValue))) {
          uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.invalid-format",
                                                  null,
                                                  ApplicationMessage.ERROR));
          event.getRequestContext().addUIComponentToUpdateByAjax(publicationSchedule);
          return;
        }
        if (startDate != null && endDate != null && startDate.after(endDate)) {
          uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.fromDate-after-toDate",
                                                  null,
                                                  ApplicationMessage.ERROR));
          event.getRequestContext().addUIComponentToUpdateByAjax(publicationSchedule);
          return;
        }

        if(node.isLocked()) {
          node.getSession().addLockToken(LockUtil.getLockToken(node));
        }

        if (StringUtils.isNotEmpty(startValue) || StringUtils.isNotEmpty(endValue)) {
          if (StringUtils.isNotEmpty(startValue))
            node.setProperty(AuthoringPublicationConstant.START_TIME_PROPERTY, startDate);
          if (StringUtils.isNotEmpty(endValue))
            node.setProperty(AuthoringPublicationConstant.END_TIME_PROPERTY, endDate);
          node.getSession().save();
          
          // Show message save success
          uiApp.addMessage(new ApplicationMessage("UIPublicationSchedule.msg.save-finished", null, ApplicationMessage.INFO));
          event.getRequestContext().addUIComponentToUpdateByAjax(publicationSchedule);
        }
      } catch (ItemExistsException iee) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error when adding properties to node");
        }
      }
    }
  }
  
  public static class ResetActionListener extends EventListener<UIPublicationSchedule> {
    public void execute(Event<UIPublicationSchedule> event) throws Exception {
      UIPublicationSchedule publicationSchedule = event.getSource();
      UIPublicationPanel publicationPanel =
          publicationSchedule.getAncestorOfType(UIPublicationContainer.class).getChild(UIPublicationPanel.class);
      Node node = publicationPanel.getCurrentNode();
      UIFormDateTimeInput startPublication = publicationSchedule.getChildById(START_PUBLICATION);
      startPublication.setCalendar(null);
      if (node.hasProperty(AuthoringPublicationConstant.START_TIME_PROPERTY)) {
        node.getProperty(AuthoringPublicationConstant.START_TIME_PROPERTY).remove();
        node.save();
      }
      UIFormDateTimeInput endPublication = publicationSchedule.getChildById(END_PUBLICATION);
      endPublication.setCalendar(null);
      if (node.hasProperty(AuthoringPublicationConstant.END_TIME_PROPERTY)) {
        node.getProperty(AuthoringPublicationConstant.END_TIME_PROPERTY).remove();
        node.save();
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(publicationSchedule);
    }
  }
}
