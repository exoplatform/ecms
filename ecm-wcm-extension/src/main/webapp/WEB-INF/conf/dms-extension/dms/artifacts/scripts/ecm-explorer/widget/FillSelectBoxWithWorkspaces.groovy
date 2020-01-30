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

import java.util.List ;
import java.util.ArrayList ;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;

import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.core.model.SelectItemOption;

import org.exoplatform.services.cms.scripts.CmsScript;

public class FillSelectBoxWithWorkspaces implements CmsScript {
  
  private RepositoryService repositoryService_;
  private String repository_ ;
  
  public FillSelectBoxWithWorkspaces(RepositoryService repositoryService) {
    repositoryService_ = repositoryService;
  }
  
  public void execute(Object context) {
    UIFormSelectBox selectBox = (UIFormSelectBox) context;

    ManageableRepository jcrRepository = repositoryService_.getCurrentRepository();
    List options = new ArrayList();
    String[] workspaceNames = jcrRepository.getWorkspaceNames();
    for(i in 0..<workspaceNames.length) {
      String name = workspaceNames[i];
      options.add(new SelectItemOption(name, name));
    }            
    selectBox.setOptions(options);
  }

  public void setParams(String[] params) { repository_ = params[0]; }
}