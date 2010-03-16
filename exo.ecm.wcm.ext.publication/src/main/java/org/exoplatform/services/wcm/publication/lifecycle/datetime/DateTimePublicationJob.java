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
package org.exoplatform.services.wcm.publication.lifecycle.datetime;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.exoplatform.services.scheduler.BaseJob;
import org.exoplatform.services.scheduler.JobContext;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 24, 2009  
 */
public class DateTimePublicationJob extends BaseJob {

	public void execute(JobContext context) throws Exception {
		WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
		Map<String, WebpagePublicationPlugin> publicationPlugins = wcmPublicationService.getWebpagePublicationPlugins();
		Set<String> publicationPluginSet = publicationPlugins.keySet();
		Iterator<String> publicationPluginIterator = publicationPluginSet.iterator();
		while(publicationPluginIterator.hasNext()) {
			String publicationPluginName = publicationPluginIterator.next();
			if (DateTimePublicationPlugin.LIFECYCLE_NAME.equals(publicationPluginName)) {
				DateTimePublicationPlugin dateTimePublicationPlugin = (DateTimePublicationPlugin) publicationPlugins.get(publicationPluginName);
				dateTimePublicationPlugin.publishContent();
				return;
			}
		}
	}
	
}
