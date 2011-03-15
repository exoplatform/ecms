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
package org.exoplatform.ecm.webui.component.browsecontent;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.component.browsecontent.UICBSearchResults.ResultData;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          tuan.pham@exoplatform.com
 * Mar 19, 2007
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/browse/UISearchController.gtmpl"
)
public class UISearchController extends UIContainer  {
  protected long searchTime_ = 0 ;
  protected int records_ = 0 ;
  private boolean isShowResult_ = false ;

  public UISearchController() throws Exception {
    addChild(UICBSearchForm.class, null, null) ;
    addChild(UICBSearchResults.class, null, null) ;
  }

  public void setShowHiddenSearch() throws Exception {
    UICBSearchForm uiSearch = getChild(UICBSearchForm.class) ;
    uiSearch.reset() ;
    uiSearch.getUIFormCheckBoxInput(UICBSearchForm.FIELD_CB_REF).setRendered(true) ;
    uiSearch.getUIFormCheckBoxInput(UICBSearchForm.FIELD_CB_CHILD).setRendered(true) ;
    UICBSearchResults uiSearchResults = getChild(UICBSearchResults.class) ;
    List<ResultData> queryResult = new ArrayList<ResultData>() ;
    uiSearchResults.updateGrid(queryResult) ;
    UIBrowseContainer container = getAncestorOfType(UIBrowseContainer.class) ;
    container.setShowSearchForm(!container.isShowSearchForm()) ;
    searchTime_ = 0 ;
    records_ = 0 ;
  }

  public boolean isShowResult() {return isShowResult_ ;}

  public void setSearchTime(long val) {searchTime_ = val ;}
  public long getSearchTime() {return searchTime_ ;}
  public void setResultRecord(int val) {records_ = val ;}
  public int getResultRecord() {return records_ ;}
  public String getNodeName() throws Exception {
    return getAncestorOfType(UIBrowseContainer.class).getCurrentNode().getName() ;
  }
}
