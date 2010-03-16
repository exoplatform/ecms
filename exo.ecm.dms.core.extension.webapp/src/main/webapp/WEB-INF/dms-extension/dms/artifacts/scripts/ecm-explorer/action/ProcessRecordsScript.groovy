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
import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.jcr.RepositoryService;

public class ProcessRecordsScript implements CmsScript {

  private RecordsService recordsService = null; 
  private RepositoryService repositoryService = null; 
   

  public ProcessRecordsScript(RepositoryService repositoryService,
       RecordsService recordsService) {                        
    this.recordsService = recordsService;
    this.repositoryService = repositoryService;
  }
  
  public void execute(Object context) {
    Session session = null ;
    try {
      String workspace = (String) ((Map) context).get("srcWorkspace") ;
      String repository = (String) ((Map) context).get("repository") ;
      session = repositoryService.getRepository(repository).login(workspace);
      Node filePlan = (Node) session.getItem((String)((Map) context).get("srcPath")); 
      Node record = (Node) session.getItem((String)((Map) context).get("nodePath"));
      recordsService.addRecord(filePlan, record);
      session.save();
      session.logout();
    } catch (Exception e) {
      if(session !=null) {
        session.logout();
      }
      e.printStackTrace();
    }
  }

  public void setParams(String[] params) {}
}
