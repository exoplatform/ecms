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
import javax.jcr.NodeIterator;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * March 03, 2009 3:45:09 PM
 */
public class RemovePrivilegeableScript implements CmsScript {
  
  private RepositoryService repositoryService_ ;
  
  public RemovePrivilegeableScript(RepositoryService repositoryService) {
    repositoryService_ = repositoryService ;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;   
    String srcWorkspace = (String)context.get("srcWorkspace") ;
    String nodePath = (String)context.get("srcPath") ;
    Session session = null ;
    try {
      session = repositoryService_.getCurrentRepository().login(srcWorkspace);
      Node node = (Node) session.getItem(nodePath);
      processRemovePrivilegeableMixin(node);
      node.save();
      session.save();
    } catch(Exception e) {
      e.printStackTrace() ;
    } finally {
      if(session != null) {
        session.logout();
      }
    }
  }
  
  private void processRemovePrivilegeableMixin(Node node) throws Exception {
    if(node.hasNodes()) {
      NodeIterator nodeIter = node.getNodes();
      Node child = null;
      while(nodeIter.hasNext()) {
        child = nodeIter.nextNode();
        if(child.isNodeType("exo:privilegeable")) {
          try {
            child.removeMixin("exo:privilegeable");
            child.save();
          } catch(Exception e) {
            continue;
            e.printStackTrace();
          }
        }
        if(child.hasNodes()) processRemovePrivilegeableMixin(child);
      }
    }
  }

  public void setParams(String[] params) {}

}