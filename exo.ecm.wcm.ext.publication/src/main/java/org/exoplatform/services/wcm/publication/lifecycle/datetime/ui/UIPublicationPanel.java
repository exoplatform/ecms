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
package org.exoplatform.services.wcm.publication.lifecycle.datetime.ui;

import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.services.wcm.publication.lifecycle.datetime.DateTimePublicationPlugin;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 24, 2009  
 */
@ComponentConfig(
	lifecycle = UIFormLifecycle.class,
	template = "classpath:groovy/wcm/webui/publication/lifecycle/datetime/ui/UIPublicationPanel.gtmpl",
	events = {
		@EventConfig(listeners=UIPublicationPanel.DraftActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.LiveActionListener.class),
		@EventConfig(name= "obsolete", listeners= UIPublicationPanel.ObsoleteActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.ChangeVersionActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.PreviewVersionActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.RestoreVersionActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.SeeAllVersionActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.CloseActionListener.class),
		@EventConfig(listeners=UIPublicationPanel.SaveActionListener.class)
	} 
)
public class UIPublicationPanel extends org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationPanel {

	public static final String START_PUBLICATION = "UIPublicationPanelStartDateInput";
	
	public static final String END_PUBLICATION = "UIPublicationPanelEndDateInput";
	
	public UIPublicationPanel() throws Exception {
		addChild(new UIFormDateTimeInput(START_PUBLICATION, START_PUBLICATION, null).addValidator(MandatoryValidator.class));
		addChild(new UIFormDateTimeInput(END_PUBLICATION, END_PUBLICATION, null).addValidator(MandatoryValidator.class));
		setActions(new String[] {"Save", "Close"});
	}
	
	public void init(Node node) throws Exception {
		Calendar startDate = null;
		Calendar endDate = null;
		try {
			startDate = node.getProperty(DateTimePublicationPlugin.START_TIME_PROPERTY).getDate();
			endDate = node.getProperty(DateTimePublicationPlugin.END_TIME_PROPERTY).getDate();
		} catch (Exception e) {
			startDate = Calendar.getInstance();
			endDate = Calendar.getInstance();
		}
		((UIFormDateTimeInput)getChildById(START_PUBLICATION)).setCalendar(startDate);
		((UIFormDateTimeInput)getChildById(END_PUBLICATION)).setCalendar(endDate);
		
		super.init(node);
	}
	
	public static class SaveActionListener extends EventListener<UIPublicationPanel> {
		public void execute(Event<UIPublicationPanel> event) throws Exception {
			UIPublicationPanel publicationPanel = event.getSource();
			UIFormDateTimeInput startPublication = publicationPanel.getChildById(START_PUBLICATION);
			UIFormDateTimeInput endPublication = publicationPanel.getChildById(END_PUBLICATION);
			Calendar startDate = startPublication.getCalendar();
			Calendar endDate = endPublication.getCalendar();
			try {
				startDate.getTime();
				endDate.getTime();
			} catch (NullPointerException e) {
				UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.invalid-format", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
				return;
			}
			Node node = publicationPanel.getCurrentNode();
			node.setProperty(DateTimePublicationPlugin.START_TIME_PROPERTY, startDate);
			node.setProperty(DateTimePublicationPlugin.END_TIME_PROPERTY, endDate);
			node.getSession().save();

			UIPopupContainer uiPopupContainer = (UIPopupContainer) publicationPanel.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
		}
	}
	
}
