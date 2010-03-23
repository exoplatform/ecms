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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIGrid;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 11, 2007  
 * 2:55:47 PM
 */
@ComponentConfig(
    template = "system:/groovy/webui/core/UIGrid.gtmpl"
)
public class UITagStyleList extends UIGrid {

  final static String RANGE_PROP = "exo:styleRange" ;
  final static String HTML_STYLE_PROP = "exo:htmlStyle" ;
  
  private static String[] BEAN_FIELD = {"name", "documentRange", "tagHTML"} ;
  private static String[] ACTIONS = {"EditStyle", "RemoveStyle"} ;
  
  public UITagStyleList() throws Exception {
    getUIPageIterator().setId("TagStyleIterator") ;
    configure("name", BEAN_FIELD, ACTIONS) ;
  }
  
  public void updateGrid() throws Exception {
    List<TagStyleData> tagStyleList = new ArrayList<TagStyleData>() ;
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    String workspace = getAncestorOfType(UIECMAdminPortlet.class).getDMSSystemWorkspace(repository);
    TagStyleData tagStyleData = null ;
    for(Node node : newFolksonomyService.getAllTagStyle(repository, workspace)) {
      tagStyleData = new TagStyleData(node.getName(), getRangeOfStyle(node), getHtmlStyleOfStyle(node)) ;
      tagStyleList.add(tagStyleData) ;
    }
    ObjectPageList objPageList = new ObjectPageList(tagStyleList, 10) ;
    getUIPageIterator().setPageList(objPageList) ;
  }
  
  public String getRangeOfStyle(Node tagStyle) throws Exception {
    return tagStyle.getProperty(RANGE_PROP).getValue().getString() ;
  }
  
  public String getHtmlStyleOfStyle(Node tagStyle) throws Exception {
    return tagStyle.getProperty(HTML_STYLE_PROP).getValue().getString() ;
  }
  
  static public class TagStyleData {
    private String tagName_ ;
    private String documentRange_ ;
    private String tagHTML_ ;
    
    public TagStyleData(String tagName, String documentRange, String tagHTML) {
      tagName_ = tagName ;
      documentRange_ = documentRange ;
      tagHTML_ = tagHTML ;
    }
    
    public String getName() { return tagName_ ; } 
    public String getDocumentRange() { return documentRange_ ; }  
    public String getTagHTML() { return tagHTML_ ; }
  }
}
