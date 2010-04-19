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

import org.apache.commons.lang.StringUtils;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.jcr.RepositoryService;

public class PostFilePlanInterceptor implements CmsScript {

  private RepositoryService repositoryService_;
  private RecordsService recordsService_;
  
  public PostFilePlanInterceptor(RepositoryService repositoryService,
      RecordsService recordsService) {
    repositoryService_ = repositoryService;
    recordsService_ = recordsService;
  }
  
  public void execute(Object context) {
    String path = (String) context;     
    Session session = null ;
		try{
			String[] splittedPath = path.split("&workspaceName=");
      String[] splittedContent = splittedPath[1].split("&repository=");
      println("Post File Plan interceptor, created node hello: " + splittedPath[0]);
      
      session = repositoryService_.getRepository(splittedContent[1]).getSystemSession(splittedContent[0]);
	    Node filePlan = (Node) session.getItem(splittedPath[0]);	
	    recordsService_.bindFilePlanAction(filePlan, splittedContent[1]);
      session.save();
      session.logout();
		}catch(Exception e) {
      if(session != null) {
        session.logout() ;
      }
			e.printStackTrace() ;
		}
  }

  public void setParams(String[] params) {}

}