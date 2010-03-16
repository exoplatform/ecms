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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationHistory;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationPagesContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 24, 2009  
 */

@ComponentConfig(
	lifecycle = Lifecycle.class,
	template = "system:/groovy/webui/core/UITabPane.gtmpl"              
)
public class UIPublicationContainer extends org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationContainer {

	private DateFormat dateTimeFormater;
	
	public void initContainer(Node node) throws Exception {
		UIPublicationPanel publicationPanel = addChild(UIPublicationPanel.class, null, null);
    publicationPanel.init(node);
    UIPublicationPagesContainer publicationPagesContainer = addChild(UIPublicationPagesContainer.class, null, null);
    publicationPagesContainer.init(node);
    publicationPagesContainer.setRendered(false);
    UIPublicationHistory publicationHistory = addChild(UIPublicationHistory.class, null, null);
    publicationHistory.init(node);
    publicationHistory.updateGrid();
    publicationHistory.setRendered(false);
    setSelectedTab(1);
    Locale locale = org.exoplatform.portal.webui.util.Util.getPortalRequestContext().getLocale();
    dateTimeFormater = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,SimpleDateFormat.MEDIUM,locale);
	}

	public DateFormat getDateTimeFormater() {
		return dateTimeFormater;
	}
	
}
