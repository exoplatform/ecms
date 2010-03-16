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
package org.exoplatform.services.migration;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 20, 2009  
 * 3:32:59 PM
 */
public class TagMigrationService implements Startable {
	
	private static final String DEFAULT_REPO = "repository";
  private static final String EXO_TOTAL = "exo:total";	
	private static final String EXO_FOLKSONOMY = "exo:folksonomy";
	private static final String EXO_FOLKSONOMIZED = "exo:folksonomized";
	private static final String USER_FOLKSONOMY_ALIAS = "userPrivateFolksonomy".intern();
	
  private static final Log LOG = ExoLogger.getLogger(TagMigrationService.class);	
	
	private RepositoryService repoService_;
	private NodeHierarchyCreator nodeHierarchyCreator_;
	private DMSConfiguration dmsConfiguration_;
	private SessionProvider sessionProvider_;
	private LinkManager linkManager_;
	
	private String oldExoTagStylePath_;
	private String newExoTagStylePath_;
	

	public TagMigrationService(RepositoryService repoService,
														 NodeHierarchyCreator nodeHierarchyCreator,
														 DMSConfiguration dmsConfiguration,
														 LinkManager linkManager) throws Exception {
		this.repoService_ = repoService;
		this.nodeHierarchyCreator_ = nodeHierarchyCreator;
		this.dmsConfiguration_ = dmsConfiguration;
		this.linkManager_ = linkManager;
		
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    SessionProviderService sessionProviderService
    		=	(SessionProviderService) myContainer.getComponentInstanceOfType(SessionProviderService.class);
    //this.sessionProvider = sessionProviderService.getSessionProvider(null);
    this.sessionProvider_ = sessionProviderService.getSystemSessionProvider(null);
	}
	
	private void migrateTagStyles() throws Exception {
		oldExoTagStylePath_ = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_TAG_STYLE_PATH) ;
		newExoTagStylePath_ = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_NEW_TAG_STYLE_PATH);
    Session systemSession = null;
    Session dmsSystemSession = null;
    try {
      systemSession = getSystemSession(DEFAULT_REPO);
      dmsSystemSession = getDMSSystemSession(DEFAULT_REPO);
      //create necessary path
      String[] paths = newExoTagStylePath_.split("/");
      Node parent = (Node)dmsSystemSession.getRootNode();
      for (int i = 1; i < paths.length; i++) {
      	Node node = parent.hasNode(paths[i]) ? parent.getNode(paths[i]) : parent.addNode(paths[i]);
      	parent = node;
      }
      dmsSystemSession.save();
      //migrate data
      Node oldExoTagStyleNode = (Node)systemSession.getItem(oldExoTagStylePath_);
      Node newExoTagStyleNode = (Node)dmsSystemSession.getItem(newExoTagStylePath_);
      for (NodeIterator nodeIter = oldExoTagStyleNode.getNodes(); 
      		 nodeIter.hasNext();) {
      	Node styleNode = nodeIter.nextNode();
      	if (!newExoTagStyleNode.hasNode(styleNode.getName())) {
	      	dmsSystemSession.getWorkspace().clone(systemSession.getWorkspace().getName(),
	      																				oldExoTagStyleNode.getPath() + "/" + styleNode.getName(),
	      																				newExoTagStyleNode.getPath() + "/" + styleNode.getName(), true);
      	}
      }
      //oldExoTagStyleNode.remove();
    } finally {
    	if (systemSession != null) {
    		systemSession.save();    		
    		systemSession.logout();
    	}
    	if (dmsSystemSession != null) {
    		dmsSystemSession.save();
    		dmsSystemSession.logout();
    	}
    }
	}
	
	private void migrateTags() throws Exception {
		Session systemSession = getSystemSession(DEFAULT_REPO);
		try {
			List<Node> folksonomizedNodeList = getFolksonomizedNodeList();
			for (Node folksonomizedNode : folksonomizedNodeList) {
				Property folksonomyProperty = folksonomizedNode.getProperty(EXO_FOLKSONOMY);
				Value[] folksonomies = folksonomyProperty.getValues();
				for (Value value : folksonomies) {
					//get values
					String uuid = value.getString();
					Node tagNode = systemSession.getNodeByUUID(uuid);
					String tagName = tagNode.getName();
					String userName = tagNode.getParent().getName();
					//add tag symlink
					Node userFolksonomyNode = getUserFolksonomyFolder(userName);
					Node newTagNode = userFolksonomyNode.hasNode(tagName) ? 
														userFolksonomyNode.getNode(tagName) : userFolksonomyNode.addNode(tagName);
					if (!existSymlink(newTagNode, folksonomizedNode)) {														
						linkManager_.createLink(newTagNode, folksonomizedNode);
					
						long total = newTagNode.hasProperty(EXO_TOTAL) ?
								newTagNode.getProperty(EXO_TOTAL).getLong() : 0;
						newTagNode.setProperty(EXO_TOTAL, total + 1); 
	
						userFolksonomyNode.getSession().save();
					}
				}
				//folksonomizedNode.setProperty(EXO_FOLKSONOMY, (Value[])null);
				folksonomizedNode.getSession().save();				
			}
		} finally {
			if (systemSession != null) {
				systemSession.save();
				systemSession.logout();
			}
		}
	}
	
  private boolean existSymlink(Node parentNode, Node targetNode) throws Exception {
  	NodeIterator nodeIter = parentNode.getNodes();
  	while (nodeIter.hasNext()) {
  		Node link = nodeIter.nextNode();
  		Node pointTo = null;
  		try {
  			if (linkManager_.isLink(link))
  			pointTo = linkManager_.getTarget(link);
  		} catch (Exception e) {}
  		if (targetNode != null && targetNode.isSame(pointTo))
  			return true;
  	}
  	return false;
  }
	
  private Node getUserFolksonomyFolder(String userName) throws Exception {
  	Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider_, userName);
  	String folksonomyPath = nodeHierarchyCreator_.getJcrPath(USER_FOLKSONOMY_ALIAS);
  	return userNode.getNode(folksonomyPath);
  }
  
  private List<Node> getFolksonomizedNodeList() throws Exception {
  	List<Node> ret = new ArrayList<Node>();
 	  String queryStr = new String("SELECT * FROM " + EXO_FOLKSONOMIZED);

		ManageableRepository manageableRepository 
				= repoService_.getRepository(DEFAULT_REPO);
		
		String[] workspaces = manageableRepository.getWorkspaceNames();
		for (String ws : workspaces) {
			Session session = sessionProvider_.getSession(ws, manageableRepository);
			
			QueryManager queryManager = session.getWorkspace().getQueryManager();
			Query query = queryManager.createQuery(queryStr, Query.SQL);
			QueryResult queryResult = query.execute();
			
			NodeIterator iter = queryResult.getNodes();
			while (iter.hasNext()) {
				ret.add(iter.nextNode());
			}
		}
  	return ret;
  }
	
	public void start() {
		try {
			migrateTagStyles();
		} catch (Exception ex) {
			LOG.error("migrateTagStyles", ex);
		}
		try {
			migrateTags();
		} catch (Exception ex) {
			LOG.error("migrateTagStyles", ex);
		}		
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
  private Session getSystemSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repoService_.getRepository(repository) ;
    return manageableRepository.getSystemSession(manageableRepository.getConfiguration().getSystemWorkspaceName()) ;
  }  

  public Session getDMSSystemSession(String repository) throws Exception {    
    ManageableRepository manageableRepository = repoService_.getRepository(repository) ;
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
  }
  
}
