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

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.security.IdentityConstants;
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

  private static String       KEYWORD             = "keyword";

  private static String       SEARCH_LOCATION     = "location";

  private static final String ROOT_PATH_SQL_QUERY = "select * from nt:base where " +
                                                    "contains(exo:name, '$1') or contains(exo:title, '$1') or  " +
                                                    "lower(exo:name) like '%$2%' order by exo:title ASC";

  private static final String PATH_SQL_QUERY      = "select * from nt:base where jcr:path like '$0/%' AND " +
                                                    "( contains(exo:name, '$1') or contains(exo:title, '$1') or " + 
                                                    "lower(exo:name) like '%$2%') order by exo:title ASC";

  public UIContentNameSearch() throws Exception {
    addChild(new UIFormInputInfo(SEARCH_LOCATION,null,null));
    addChild(new UIFormStringInput(KEYWORD,null).addValidator(MandatoryValidator.class));
  }

  public void setLocation(String location) {
    getUIFormInputInfo(SEARCH_LOCATION).setValue(location);
  }

  static public class SearchActionListener extends EventListener<UIContentNameSearch> {
    public void execute(Event<UIContentNameSearch> event) throws Exception {
      UIContentNameSearch contentNameSearch = event.getSource();
      UIECMSearch uiECMSearch = contentNameSearch.getAncestorOfType(UIECMSearch.class);
      UISearchResult uiSearchResult = uiECMSearch.getChild(UISearchResult.class);
      UIApplication application = contentNameSearch.getAncestorOfType(UIApplication.class);
      try {
        String keyword = contentNameSearch.getUIStringInput(KEYWORD).getValue();
        keyword = keyword.trim();
        String escapedText = org.exoplatform.services.cms.impl.Utils.escapeIllegalCharacterInQuery(keyword);
        UIJCRExplorer explorer = contentNameSearch.getAncestorOfType(UIJCRExplorer.class);
        String currentNodePath = explorer.getCurrentNode().getPath();
        String statement = null;
        if("/".equalsIgnoreCase(currentNodePath)) {
          statement = StringUtils.replace(ROOT_PATH_SQL_QUERY,"$1",escapedText);
          statement = StringUtils.replace(statement,"$2",escapedText.toLowerCase());
        }else {
          statement = StringUtils.replace(PATH_SQL_QUERY,"$0",currentNodePath);
          statement = StringUtils.replace(statement,"$1",escapedText);
          statement = StringUtils.replace(statement,"$2",escapedText.toLowerCase());
        }
        long startTime = System.currentTimeMillis();
        uiSearchResult.setQuery(statement, explorer.getTargetSession().getWorkspace().getName(), Query.SQL, 
                                IdentityConstants.SYSTEM.equals(explorer.getTargetSession()), null);
        uiSearchResult.updateGrid();
        long time = System.currentTimeMillis() - startTime;
        uiSearchResult.setSearchTime(time);
        contentNameSearch.getUIFormInputInfo(SEARCH_LOCATION).setValue(currentNodePath);
        uiECMSearch.setSelectedTab(uiSearchResult.getId());
      } catch (RepositoryException reEx) {
        application.addMessage(new ApplicationMessage("UIContentNameSearch.msg.keyword-not-allowed",
                                                      null,
                                                      ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(contentNameSearch);
        return;
      } catch (Exception e) {
        uiSearchResult.setQuery(null, null, null, false, null);
        uiSearchResult.updateGrid();
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIContentNameSearch> {
    public void execute(Event<UIContentNameSearch> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction();
    }
  }

}
