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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Session;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * April 08, 2009  
 */
/**
* This script will be used to move document to target which specified by users and create
* a symlink corresponding with target node
*/
public class AddTaxonomyActionScript implements CmsScript {
  
  private RepositoryService repositoryService_;
  private SessionProviderService seProviderService_;
  private LinkManager linkManager_;
  private IDGeneratorService idGenerator_;
  private static final Log LOG  = ExoLogger.getLogger("AddTaxonomyActionScript");
  
  public AddTaxonomyActionScript(RepositoryService repositoryService, 
      SessionProviderService sessionProviderService, LinkManager linkManager, IDGeneratorService idGenerator) {
    repositoryService_ = repositoryService;
    seProviderService_ = sessionProviderService;
    linkManager_ = linkManager;
    idGenerator_ = idGenerator;
  }
  
  public void execute(Object context) throws Exception {
    Map variables = (Map) context;       
    String nodePath = (String)variables.get("nodePath");
    String storeFullPath = (String)variables.get("exo:storeHomePath");
    String storeHomePath = null;
    String storeWorkspace = null;
    String targetWorkspace = (String)variables.get("exo:targetWorkspace");
    String targetPath = (String)variables.get("exo:targetPath");
    if(storeFullPath.indexOf(":/") > -1) {
      storeWorkspace = storeFullPath.split(":/")[0];
      if (storeFullPath.split(":/").length == 1)
        storeHomePath = "";
      else
        storeHomePath = storeFullPath.split(":/")[1];      
      if(!storeHomePath.startsWith("/")) storeHomePath = "/" + storeHomePath;
    } else {
      storeWorkspace = targetWorkspace;
      storeHomePath = targetPath;
    }
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    Session sessionHomeNode = null;
    Session sessionTargetNode = null;
    Node targetNode = null;
    Node storeNode = null;
    try{
      sessionHomeNode = seProviderService_.getSessionProvider(null).getSession(storeWorkspace, manageableRepository);
      storeNode = (Node)sessionHomeNode.getItem(storeHomePath);
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when try to get node of root taxonomy tree", e);
      }
      throw e;
    }
    try{
      sessionTargetNode = seProviderService_.getSessionProvider(null).getSession(targetWorkspace, manageableRepository);
      targetNode = (Node)sessionTargetNode.getItem(targetPath);
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when try to get node of target", e);
      }
      throw e;
    }
    try {
	  	String[] subPaths = getDateLocation().split("/");
	  	Node cNode = targetNode;
	  	for (String subPath : subPaths) {
	  		if (!cNode.hasNode(subPath)) {
	  			cNode.addNode(subPath);
	  			cNode.save();
	  		}
	  		cNode = cNode.getNode(subPath);
	  	}
	  	String nodeName = nodePath.substring(nodePath.lastIndexOf("/") + 1);
	  	// defend node with same name is overwritted
	  	String generatedNodeName = idGenerator_.generateStringID(nodeName);
	  	String targetParentPath = cNode.getPath(); 
	  	targetPath = cNode.getPath().concat("/").concat(generatedNodeName).replaceAll("/+", "/");
	    if (!storeWorkspace.equals(targetWorkspace) && storeNode.getSession().itemExists(nodePath)) {
	      Node currentNode = (Node)storeNode.getSession().getItem(nodePath);
	      sessionTargetNode.getWorkspace().clone(storeWorkspace, nodePath, targetPath, true);
	      currentNode.remove();
	      storeNode.save();
	    } else {
	    	if (sessionHomeNode.itemExists(nodePath)) {
	    		sessionHomeNode.move(nodePath, targetPath);
	    		sessionHomeNode.save();
	    	}	
	    }
	    if (sessionTargetNode.itemExists(targetPath)) {
	    	targetNode = (Node)sessionTargetNode.getItem(targetPath);
		    String nodeLinkPath = nodePath.substring(0, nodePath.lastIndexOf("/"));
		    if (!nodeLinkPath.startsWith("/")) nodeLinkPath = "/" + nodeLinkPath;     
		    Node nodeLink = linkManager_.createLink((Node)storeNode.getSession().getItem(nodeLinkPath), "exo:taxonomyLink", targetNode, nodeName);
       
		    //rename added node to recover official name
		    String destPath = targetParentPath.concat("/").concat(nodeName);
		    sessionTargetNode.move(targetPath, targetParentPath.concat("/").concat(nodeName));
		    if (targetNode.canAddMixin("exo:privilegeable"))
		      targetNode.addMixin("exo:privilegeable");
		    sessionTargetNode.save();
		    String t_title;
		    Node dest;
		    try {
		      dest =(Node)  sessionTargetNode.getItem(destPath);
          t_title = dest.getProperty("exo:title").getString();          
        } catch (Exception e) {
          //No need to process with exception here
          dest= null;
        }
        try {
          Node source =(Node)  sessionTargetNode.getItem(nodePath);
          String currentTitle = source.hasProperty("exo:title")?source.getProperty("exo:title").getString():null;
          source.setProperty("exo:title", t_title);
          source.save();
        } catch (Exception e) {
          //No need to process with exception here
        }
        
        if (dest != null) {        
          if(dest.hasProperty("exo:owner")) {
            String owner = dest.getProperty("exo:owner").getString();
            try {
              dest.setPermission(owner, PermissionType.ALL);
            }catch (Exception e) {
              //avoid broken UI if setPermission failed 
            }
          }
        }         
	    }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
    	LOG.error("Exception when try move node and create link", e);
      }	
      throw e;
    }
  }
  
  private String getDateLocation() {
    Calendar calendar = new GregorianCalendar();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd") ;
    return dateFormat.format(calendar.getTime());
  }
  
  public void setParams(String[] params) {}

}