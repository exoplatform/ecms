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
package org.exoplatform.ecm.webui.component.explorer.search;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.SearchValidator;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham  
 *          hoa.pham@exoplatform.com
 * Oct 2, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIContentNameSearch.SearchActionListener.class),
      @EventConfig(listeners = UIContentNameSearch.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UIContentNameSearch extends UIForm {

  private static String KEYWORD = "keyword".intern();  
  private static String SEARCH_LOCATION = "location".intern();
  private static final String ROOT_PATH_SQL_QUERY = "select * from nt:base where jcr:path like '%/$1' order by exo:dateCreated DESC,jcr:primaryType DESC";
  private static final String PATH_SQL_QUERY = "select * from nt:base where jcr:path like '$0/%/$1' or jcr:path like '$0/$1' order by exo:dateCreated DESC,jcr:primaryType DESC";
  
  public UIContentNameSearch() throws Exception {
    addChild(new UIFormInputInfo(SEARCH_LOCATION,null,null));
    addChild(new UIFormStringInput(KEYWORD,null).addValidator(SearchValidator.class).addValidator(MandatoryValidator.class));
  }
  
  public void setLocation(String location) {
    getUIFormInputInfo(SEARCH_LOCATION).setValue(location);
  }

  static public class SearchActionListener extends EventListener<UIContentNameSearch> {
    public void execute(Event<UIContentNameSearch> event) throws Exception {
      UIContentNameSearch contentNameSearch = event.getSource();
      UIECMSearch uiECMSearch = contentNameSearch.getAncestorOfType(UIECMSearch.class);
      UISearchResult uiSearchResult = uiECMSearch.getChild(UISearchResult.class);
      try {      
        String keyword = contentNameSearch.getUIStringInput(KEYWORD).getValue();        
        String[] arrFilterChar = {"&", "$", "@", ":","]", "[", "*", "%", "!"};
        UIApplication application = contentNameSearch.getAncestorOfType(UIApplication.class);
        if (keyword == null || keyword.length() ==0) {
          application.addMessage(new ApplicationMessage("UIContentNameSearch.msg.keyword-not-allowed", null));
          event.getRequestContext().addUIComponentToUpdateByAjax(application.getUIPopupMessages());
          return;
        }
        for(String filterChar : arrFilterChar) {
          if(keyword.indexOf(filterChar) > -1) {
            application.addMessage(new ApplicationMessage("UIContentNameSearch.msg.keyword-not-allowed", null));
            event.getRequestContext().addUIComponentToUpdateByAjax(application.getUIPopupMessages());
            return;
          }
        }
        keyword = keyword.trim();
        UIJCRExplorer explorer = contentNameSearch.getAncestorOfType(UIJCRExplorer.class);
        String currentNodePath = explorer.getCurrentNode().getPath();
        String statement = null;
        if("/".equalsIgnoreCase(currentNodePath)) {
          statement = StringUtils.replace(ROOT_PATH_SQL_QUERY,"$1",keyword);
        }else {
          statement = StringUtils.replace(PATH_SQL_QUERY,"$0",currentNodePath);
          statement = StringUtils.replace(statement,"$1",keyword);
        }
        QueryManager queryManager = explorer.getTargetSession().getWorkspace().getQueryManager();         
        Query query = queryManager.createQuery(statement,Query.SQL);
        long startTime = System.currentTimeMillis();
        QueryResult queryResult = query.execute();
        uiSearchResult.clearAll();
        uiSearchResult.setQueryResults(queryResult);
        uiSearchResult.updateGrid(true);
        long time = System.currentTimeMillis() - startTime;
        uiSearchResult.setSearchTime(time);
        uiECMSearch.setRenderedChild(UISearchResult.class);
        contentNameSearch.getUIFormInputInfo(SEARCH_LOCATION).setValue(currentNodePath);
      } catch (Exception e) {
        uiSearchResult.clearAll();
        uiSearchResult.setQueryResults(null);
        uiSearchResult.updateGrid(true);
        uiECMSearch.setRenderedChild(UISearchResult.class);
      }
    }  
  }

  static public class CancelActionListener extends EventListener<UIContentNameSearch> {   
    public void execute(Event<UIContentNameSearch> event) throws Exception {    
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction();
    }    
  }

}
