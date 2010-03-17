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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.datetime.ui.UIPublicationContainer;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 17, 2009  
 */
public class DateTimePublicationPlugin extends StageAndVersionPublicationPlugin {
	
	private Log log = ExoLogger.getLogger("wcm:DateTimePublicationPlugin");
	
	public static final String LIFECYCLE_NAME = "Date time publication";

	public static final String LIFECYCLE_TYPE = "publication:dateTimePublication";

	public static final String START_TIME_PROPERTY = "publication:startPublishDate";
	
	public static final String END_TIME_PROPERTY = "publication:endPublishDate";
	
	private List<String> repositories;
	
	private List<String> workspaces;
	
	@SuppressWarnings("unchecked")
	public DateTimePublicationPlugin(InitParams initParams) {
		super();
		repositories = initParams.getValuesParam("repository").getValues();
		workspaces = initParams.getValuesParam("workspace").getValues();
	}
	
	public String getLifecycleType() {
		return LIFECYCLE_TYPE;
	}
	
  public void addMixin(Node node) throws Exception {
    node.addMixin(LIFECYCLE_TYPE);
    if(!node.isNodeType(NodetypeConstant.MIX_VERSIONABLE)) {
      node.addMixin(NodetypeConstant.MIX_VERSIONABLE);
    }            
  }

  public boolean canAddMixin(Node node) throws Exception {
    return node.canAddMixin(LIFECYCLE_TYPE);   
  }
	
  public UIForm getStateUI(Node node, UIComponent component) throws Exception {
  	UIPublicationContainer publicationContainer = component.createUIComponent(UIPublicationContainer.class, null, null);
    publicationContainer.initContainer(node);
    return publicationContainer;
  }
  
  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception {
  	ClassLoader cl=this.getClass().getClassLoader();    
    ResourceBundle resourceBundle= ResourceBundle.getBundle(StageAndVersionPublicationConstant.LOCALIZATION, locale, cl);
    String result = "";
    try {
    	result = resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			result = key;
		}
    if(values != null) {
      return String.format(result, (Object[])values); 
    }        
    return result;
  }
  
	public void publishContent() {
		RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
		WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
		SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
		try {
			for (String repository : repositories) {
				for (String workspace : workspaces) {
					Session session = null;
					try {
						ManageableRepository manageableRepository = repositoryService.getRepository(repository);
						session = sessionProvider.getSession(workspace, manageableRepository);
						String timestamp = ISO8601.format(Calendar.getInstance());
						String sqlQuery = "select * from nt:base where " +
															"(publication:startPublishDate <= TIMESTAMP '" + timestamp + "' AND publication:currentState = '" + PublicationDefaultStates.DRAFT + "') OR " +
															"(publication:endPublishDate <= TIMESTAMP '" + timestamp + "' AND publication:currentState = '" + PublicationDefaultStates.PUBLISHED + "')";
						QueryManager queryManager = session.getWorkspace().getQueryManager();
						Query query = queryManager.createQuery(sqlQuery, Query.SQL);
						QueryResult result = query.execute();
						NodeIterator results = result.getNodes(); 
						while(results.hasNext()) {
							Node node = results.nextNode();
							String publicationState = wcmPublicationService.getContentState(node);
							HashMap<String, String> context = new HashMap<String, String>();
							context.put(StageAndVersionPublicationConstant.CURRENT_REVISION_NAME, node.getName());
							if (PublicationDefaultStates.DRAFT.equals(publicationState)) {
								changeState(node, PublicationDefaultStates.PUBLISHED, context);
							} else {
								changeState(node, PublicationDefaultStates.OBSOLETE, context);
							}
						}
					} catch (Exception e) {
						log.error("Exception when publish content by date time", e);
					} finally {
						if (session != null) session.logout();
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception when publish content by date time", e);
		}
	}
}
