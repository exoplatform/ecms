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

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Jan 9, 2007  
 */
public class EnableVersioningScript implements CmsScript {
  
  private RepositoryService repositoryService_;    
  
  public EnableVersioningScript(RepositoryService repositoryService) {
    repositoryService_ = repositoryService;    
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;
    String nodePath = (String)variables.get("nodePath") ;
    String workspace = (String)variables.get("srcWorkspace") ;

    try {
      Session session = repositoryService_.getCurrentRepository().getSystemSession(workspace) ;
      Node addedNode = (Node) session.getItem(nodePath);
      if(addedNode.canAddMixin("mix:versionable")) {
        addedNode.addMixin("mix:versionable") ;
        addedNode.save() ;
        session.logout();
      } 
    } catch (Exception e) {
      if(session !=null) {
        session.logout();
      }
      e.printStackTrace() ;
    }  
  }

  public void setParams(String[] params) {}

}