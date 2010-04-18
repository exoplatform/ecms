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

import org.exoplatform.services.cms.scripts.CmsScript ;
import org.exoplatform.services.cms.scripts.DataTransfer ;
import org.exoplatform.services.jcr.RepositoryService;

import javax.jcr.Node;
import javax.jcr.NodeIterator ;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.util.ArrayList;
import java.util.List;

public class GetDocuments implements CmsScript {
  
  private RepositoryService repositoryService_ ;
  
  public GetDocuments(RepositoryService repositoryService) {
    repositoryService_ = repositoryService ;
  }
  
  public void execute(Object context){    
    DataTransfer data = (DataTransfer) context ;
    Session session = null ;
    try{      
      String repository = data.getRepository();
      String workspace = data.getWorkspace() ;
      session = repositoryService_.getRepository(repository).login(workspace) ;
      QueryManager queryManager = session.getWorkspace().getQueryManager();     
      Query query = queryManager.createQuery("/jcr:root//element(*, exo:article)", Query.XPATH); 
      QueryResult queryResult = query.execute();      
      NodeIterator iter = queryResult.getNodes() ;
      List nodeList = new ArrayList() ;
      while(iter.hasNext()) {
        Node node = iter.nextNode() ;
        nodeList.add(node) ;
      }
      data.setContentList(nodeList) ;      
      session.logout();
    } catch (Exception e) {
      e.printStackTrace() ;
    }
    
  }
  
  public void setParams(String[] params) {}

}
