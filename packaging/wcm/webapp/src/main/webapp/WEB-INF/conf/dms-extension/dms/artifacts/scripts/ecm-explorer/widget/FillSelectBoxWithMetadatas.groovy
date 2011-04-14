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

import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.scripts.CmsScript;

import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.core.model.SelectItemOption;
/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 29, 2007 10:01:09 AM
 */
public class FillSelectBoxWithMetadatas implements CmsScript {
  
  private MetadataService metadataService_ ;
  private String repository_ ;
  
  public FillSelectBoxWithMetadatas(MetadataService metadataService) {
    metadataService_ = metadataService ;
  }
  
  public void execute(Object context) {
    UIFormSelectBox selectBox = (UIFormSelectBox) context;
    List options = new ArrayList();
    for(String metadataName : metadataService_.getExternalMetadataType()) {
      options.add(new SelectItemOption(metadataName, metadataName));
    }            
    selectBox.setOptions(options);
  }

  public void setParams(String[] params) { repository_ = params[0] ; }

}