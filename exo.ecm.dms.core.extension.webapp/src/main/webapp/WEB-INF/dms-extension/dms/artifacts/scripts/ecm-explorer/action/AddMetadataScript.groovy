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
import javax.jcr.Node;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 29, 2007 10:01:09 AM
 */
public class AddMetadataScript implements CmsScript {
  
  private RepositoryService repositoryService_ ;
  
  public AddMetadataScript(RepositoryService repositoryService) {
    repositoryService_ = repositoryService ;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;   
    String metadataName = (String)context.get("exo:mixinMetadata") ;
    String srcWorkspace = (String)context.get("srcWorkspace") ;
    String nodePath = (String)context.get("nodePath") ;
    Session session = null ;
    try {
      session = repositoryService_.getRepository().login(srcWorkspace);
      Node node = (Node) session.getItem(nodePath);
      if(node.canAddMixin(metadataName)) {
        node.addMixin(metadataName) ;
        node.save() ;
        session.save() ;
        session.logout();
      } else {
        System.out.println("\n\nCan not add mixin\n\n");
        session.logout();
      }
    } catch(Exception e) {
      if(session != null) {
        session.logout();        
      }
      e.printStackTrace() ;
    }
  }

  public void setParams(String[] params) {}

}