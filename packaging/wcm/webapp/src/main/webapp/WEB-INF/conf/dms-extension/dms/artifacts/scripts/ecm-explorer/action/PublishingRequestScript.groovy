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
import javax.jcr.Session;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.cms.CmsService;

import org.exoplatform.services.cms.scripts.CmsScript;

/*
 * This action copy a document to requestPath to request for publishing and move the document
 * to reservePath to reserve
 */
public class PublishingRequestScript implements CmsScript {
  private CmsService cmsService_ ;
  private RepositoryService repositoryService_;

  public PublishingRequestScript(CmsService cmsService, RepositoryService repositoryService) {
    this.cmsService_ = cmsService;
    this.repositoryService_ = repositoryService;
  }

  public void execute(Object context) {
    Map variables = (Map) context;
    String nodePath = (String)variables.get("nodePath") ;
    String workspace = (String)variables.get("srcWorkspace") ;
    String srcPath = (String)variables.get("srcPath") ;
    String requestWorkspace = (String)variables.get("exo:requestWorkspace");
    String requestPath = (String)variables.get("exo:requestPath");
    String reservePath = (String)variables.get("exo:reservePath");
    String destPath = null;
    //copy the content to request validation folder
    if(requestPath.endsWith("/")) {
      destPath = requestPath + nodePath.substring(nodePath.lastIndexOf("/")+1);
    }else {
      destPath = requestPath + nodePath.substring(nodePath.lastIndexOf("/"));
    }
    Session session = null;
    try{
      session = repositoryService_.getCurrentRepository().getSystemSession(workspace);
      session.getWorkspace().copy(nodePath,destPath);
      session.save();
      if(reservePath != null && !reservePath.equals(srcPath)) {
        if(reservePath.endsWith("/")) {
          reservePath = reservePath + nodePath.substring(nodePath.lastIndexOf("/")+1) 
        }else {
          reservePath = reservePath + nodePath.substring(nodePath.lastIndexOf("/"))
        }
        cmsService_.moveNode(nodePath, workspace, workspace, reservePath); 
      } 
    }catch(Exception e){      
    } finally {
      if(session!=null) {
        session.logout();
      }
    }
  }

  public void setParams(String[] params) {}

}