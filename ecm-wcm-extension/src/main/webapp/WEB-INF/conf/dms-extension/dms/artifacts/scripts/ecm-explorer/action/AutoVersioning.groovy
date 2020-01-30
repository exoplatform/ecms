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

import java.util.Map;
import org.exoplatform.services.cms.scripts.CmsScript;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SAS
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * June 29, 2007  
 */
public class AutoVersioningScript implements CmsScript{

  private RepositoryService repositoryService_;  
  
  public AutoVersioningScript(RepositoryService repositoryService) {
    repositoryService_ = repositoryService;    
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;
    String nodePath = (String)variables.get("nodePath") ;
    String workspace = (String)variables.get("srcWorkspace") ;
    String srcPath = (String)variables.get("srcPath") ;
    String actionName = (String)variables.get("actionName") ;
    Session session = null ;
    try{
      session = repositoryService_.getCurrentRepository().getSystemSession(workspace) ;
      Node srcNode = (Node)session.getItem(srcPath) ;
      Node exoActionNodes = srcNode.getNode("exo:actions") ;
      Node actionNode = exoActionNodes.getNode(actionName) ;
      List<String> lifeCycleList = new ArrayList<String>();
      Value[] values = actionNode.getProperty("exo:lifecyclePhase").getValues();
      for(Value value : values) {
        lifeCycleList.add(value.getString());
      }
      if (lifeCycleList.contains("modify")) {
        String propertyName = nodePath.substring(nodePath.lastIndexOf("/") + 1, nodePath.length()) ;
        if(!propertyName.equals("jcr:isCheckedOut")) {
          Property changedProp = session.getItem(nodePath) ;
          Node modifiedNode = changedProp.getParent() ;
          if(!modifiedNode.isNodeType("mix:versionable")) return ;
          if(!modifiedNode.isCheckedOut()) modifiedNode.checkout() ;
          modifiedNode.checkin() ;
          modifiedNode.checkout() ;
          session.save();
          session.refresh(true) ;
          session.logout();
        }
      } else if(lifeCycleList.contains("node_added") || lifeCycleList.contains("node_removed") 
          || lifeCycleList.contains("schedule")) {
        Node currentNode = (Node)session.getItem(nodePath) ;
        if(!currentNode.isNodeType("mix:versionable")) {
          if(currentNode.canAddMixin("mix:versionable")) {
            currentNode.addMixin("mix:versionable") ;
            currentNode.save() ;
            currentNode.checkin() ;
            currentNode.checkout() ;
            session.save() ;
            session.logout();
            return;
          }
          session.logout();
          return ;
        }
        if(!currentNode.isCheckedOut()) currentNode.checkout() ;
        currentNode.checkin() ;
        currentNode.checkout() ;
        session.save() ;
        session.logout();
      }else if (lifeCycleList.contains("read")) {
        if(srcNode.canAddMixin("mix:versionable")) {	
          srcNode.addMixin("mix:versionable") ;
        }
        if(srcNode.isNodeType("mix:versionable")) {	
          srcNode.save() ;
          srcNode.checkin() ;
          srcNode.checkout() ;
          session.save() ;
          session.logout();
          return;
        }
        session.logout();
        return ;
      }
    } catch (Exception e) {
      if(session !=null) {
        session.logout();
      }
    }       
  }
  public void setParams(String[] arg0) {
  }
}
