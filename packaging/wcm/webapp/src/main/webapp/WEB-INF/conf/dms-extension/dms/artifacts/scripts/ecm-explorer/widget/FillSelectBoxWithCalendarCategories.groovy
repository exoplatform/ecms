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

import java.util.ArrayList ;
import java.util.List ;
import java.util.Iterator;

import javax.jcr.Session;
import javax.jcr.Node ;

import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.core.model.SelectItemOption;

import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.cms.scripts.CmsScript ;
import org.exoplatform.services.jcr.ext.hierarchy.PathAlias;

import org.exoplatform.services.jcr.RepositoryService ;
import org.exoplatform.services.jcr.core.ManageableRepository ;


public class FillSelectBoxWithCalendarCategories implements CmsScript {
  
  private RepositoryService repositoryService_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  private String repository_ ;
  
  public FillSelectBoxWithCalendarCategories(RepositoryService repositoryService,
      NodeHierarchyCreator nodeHierarchyCreator) {
    repositoryService_ = repositoryService ;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
  }
  
  public void execute(Object context) {
    Session session = null ;
		try {
      UIFormSelectBox selectBox = (UIFormSelectBox) context;
      ManageableRepository jcrRepository = repositoryService_.getRepository(repository_);
      session = jcrRepository.getSystemSession(jcrRepository.getConfiguration().getSystemWorkspaceName());
      String path = nodeHierarchyCreator_.getJcrPath(PathAlias.CALENDAR_CATEGORIES_PATH) ;
      Node calendar = (Node) session.getItem(path) ;
      List options = new ArrayList();
      if (calendar != null){
        Iterator iter = calendar.getNodes() ;
        while(iter.hasNext()) {
          Node categoryNode = iter.next() ;
          options.add(new SelectItemOption(categoryNode.getName() , categoryNode.getPath().substring(1)));
        }            
      }
      selectBox.setOptions(options);
      session.logout();
    } catch(Exception e) {
      if(session !=null) {
        session.logout() ;
      }
      selectBox.setOptions(new ArrayList<SelectItemOption<String>>()) ;
    }
  }
  
  public void setParams(String[] params) { repository_ = params[0]; }
}