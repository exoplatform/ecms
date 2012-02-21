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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.SearchValidator;
import org.exoplatform.ecm.webui.component.browsecontent.UICBSearchResults.ResultData;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 22, 2006 2:48:18 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/browse/UICBSearchForm.gtmpl",
    events = {
      @EventConfig(listeners = UICBSearchForm.SearchActionListener.class),
      @EventConfig(listeners = UICBSearchForm.ChangeTypeActionListener.class, phase = Phase.DECODE)
    }
)
public class UICBSearchForm extends UIForm {
  final static public String FIELD_SEARCHVALUE = "inputValue";
  final static public String FIELD_OPTION = "option";
  final static public String FIELD_CB_REF = "referencesDoc";
  final static public String FIELD_CB_CHILD = "childDoc";
  private static final Log LOG  = ExoLogger.getLogger("browsecontent.UICBSearchForm");
  public static final String CATEGORY_SEARCH = "Category";
  public static final String DOCUMENT_SEARCH = "Content";
  public static final String CATEGORY_QUERY = "select * from $0 where jcr:path like '%/$1[%]' " ;
  public static final String DOCUMENT_QUERY = "select * from $0 where contains(*, '$1') AND jcr:path like '$2[%]/%' ";
  public static final String DOCUMENT_ROOT_QUERY = "select * from $0 where contains(*, '$1') ";
  public boolean isDocumentType = true;
  protected long duration_ = 0;
  public UICBSearchForm() throws Exception {
    UIFormSelectBox selectType = new UIFormSelectBox(FIELD_OPTION, FIELD_OPTION, getOptions());
    selectType.setOnChange("ChangeType");
    selectType.setValue(DOCUMENT_SEARCH);
    addChild(new UIFormStringInput(FIELD_SEARCHVALUE, FIELD_SEARCHVALUE, null).addValidator(SearchValidator.class));
    addChild(selectType);
    UIFormCheckBoxInput cbRef = new UIFormCheckBoxInput<Boolean>(FIELD_CB_REF, FIELD_CB_REF, null);
    UIFormCheckBoxInput cbRel = new UIFormCheckBoxInput<Boolean>(FIELD_CB_CHILD, FIELD_CB_CHILD, null);
    addChild(cbRef.setRendered(isDocumentType));
    addChild(cbRel.setRendered(isDocumentType));
  }

  public List<SelectItemOption<String>> getOptions() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(DOCUMENT_SEARCH,DOCUMENT_SEARCH));
    options.add(new SelectItemOption<String>(CATEGORY_SEARCH,CATEGORY_SEARCH));
    return options;
  }
  public Node getNode()throws Exception{return getAncestorOfType(UIBrowseContainer.class).getCurrentNode();}
  public long searchTime() { return duration_; }

  public List<ResultData> searchByCategory(String keyword, Node currentNode) throws Exception{
    List<ResultData> resultList = new ArrayList<ResultData>();
    ResultData result;
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class);
    QueryManager queryManager = null;
    try{
      queryManager = currentNode.getSession().getWorkspace().getQueryManager();
    }catch (Exception e) {
      return resultList;
    }
    duration_ = 0;
    String statement = StringUtils.replace(CATEGORY_QUERY, "$1", keyword.trim());
    for(String type : Utils.CATEGORY_NODE_TYPES) {
      String queryStatement = StringUtils.replace(statement, "$0", type);
      long beforeTime = System.currentTimeMillis();
      try{
        Query query = queryManager.createQuery(queryStatement, Query.SQL);
        QueryResult queryResult = query.execute();
        long searchTime = System.currentTimeMillis() - beforeTime;
        duration_ += searchTime;
        NodeIterator iter = queryResult.getNodes();
        while(iter.hasNext()) {
          Node node = iter.nextNode();
          if(node.getPath().startsWith(currentNode.getPath())) {
            result = new ResultData(node.getName(), node.getPath(),
                node.getSession().getWorkspace().getName());
            resultList.add(result);
          }
        }
      } catch(Exception e) {
        return null;
      }
    }
    UISearchController uiController = uiContainer.getChild(UISearchController.class);
    uiController.setSearchTime(duration_);
    uiController.setResultRecord(resultList.size());
    return resultList;
  }

  @SuppressWarnings({"unused", "unchecked"})
  public List<ResultData> searchDocument(String keyword, boolean reference,
      boolean relation, Node currentNode) throws Exception {
    String nodePath = currentNode.getPath();
    List<ResultData> resultList = new ArrayList<ResultData>();
    Map<String, ResultData> temp = new HashMap<String, ResultData>();
    String workspaceName = null;
    PortletPreferences preference = getAncestorOfType(UIBrowseContentPortlet.class).getPortletPreferences() ;
    String hasSearchLocation = preference.getValue(Utils.CB_ENABLE_SEARCH_LOCATION, "");
    String searchLocation = preference.getValue(Utils.CB_SEARCH_LOCATION, "");
    if(hasSearchLocation != null && Boolean.parseBoolean(hasSearchLocation) && searchLocation.length() > 0) {
      if(searchLocation.split(":/").length == 1) {
        workspaceName = searchLocation.split(":/")[0];
        nodePath = "/";
      } else {
        workspaceName = searchLocation.split(":/")[0];
        nodePath = "/" + searchLocation.split(":/")[1];
      }
    }
    Session session = null;
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class);
    QueryManager queryManager = null;
    if(workspaceName != null) {
      session = uiContainer.getSession(workspaceName);
    } else {
      session = currentNode.getSession();
    }
    try{
      queryManager = session.getWorkspace().getQueryManager();
    }catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return resultList;
    }
    ResultData result;
    String statement = StringUtils.replace(DOCUMENT_QUERY, "$1", keyword);
    if(nodePath.equals("/")) {
      statement = StringUtils.replace(DOCUMENT_ROOT_QUERY, "$1", keyword) ;
    } else {
      statement = StringUtils.replace(statement, "$2", nodePath) ;
    }
    duration_ = 0;
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    List<String> documentNodeTypes = templateService.getDocumentTemplates();
    documentNodeTypes.add("nt:resource");
    for(String ntDocument : documentNodeTypes) {
      String queryStatement = StringUtils.replace(statement, "$0", ntDocument);
      long beforeTime = System.currentTimeMillis();
      try{
        Query query = queryManager.createQuery(queryStatement, Query.SQL);
        QueryResult queryResult = query.execute();
        long searchTime = System.currentTimeMillis() - beforeTime;
        duration_ += searchTime;
        NodeIterator iter = queryResult.getNodes();
        while (iter.hasNext()) {
          Node node = iter.nextNode();
          if(ntDocument.equals("nt:resource")) {
            Node paNode = node.getParent();
            if(documentNodeTypes.contains(paNode.getPrimaryNodeType().getName())) {
              String path = paNode.getPath();
              String name = path.substring(path.lastIndexOf("/") + 1);
              String wsName = paNode.getSession().getWorkspace().getName();
              result = new ResultData(name, path, wsName);
              temp.put(path, result);
            }
          } else {
            String path = node.getPath();
            String name = path.substring(path.lastIndexOf("/") + 1);
            result = new ResultData(name, path, node.getSession().getWorkspace().getName());
            temp.put(path, result);
          }
        }
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        return resultList;
      }
    }
    UISearchController uiController = uiContainer.getChild(UISearchController.class);
    for(String s: temp.keySet()) {
      resultList.add(temp.get(s));
    }
    uiController.setSearchTime(duration_);
    uiController.setResultRecord(resultList.size());
    return resultList;
  }

  public void reset() {
    getUIStringInput(FIELD_SEARCHVALUE).setValue("");
    getUIFormSelectBox(FIELD_OPTION).setOptions(getOptions());
    getUIFormCheckBoxInput(FIELD_CB_REF).setRendered(isDocumentType);
    getUIFormCheckBoxInput(FIELD_CB_CHILD).setRendered(isDocumentType);
  }

  static public class ChangeTypeActionListener extends EventListener <UICBSearchForm> {
    public void execute(Event<UICBSearchForm>  event) throws Exception {
      UICBSearchForm uiForm = event.getSource();
      String searchType = uiForm.getUIFormSelectBox(FIELD_OPTION).getValue();
      uiForm.isDocumentType = searchType.equals(DOCUMENT_SEARCH);
      uiForm.getUIFormCheckBoxInput(FIELD_CB_REF).setRendered(uiForm.isDocumentType);
      uiForm.getUIFormCheckBoxInput(FIELD_CB_CHILD).setRendered(uiForm.isDocumentType);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UISearchController.class));
    }
  }

  static  public class SearchActionListener extends EventListener<UICBSearchForm> {
    public void execute(Event<UICBSearchForm> event) throws Exception {
      UICBSearchForm uiForm = event.getSource();
      UIBrowseContainer container = uiForm.getAncestorOfType(UIBrowseContainer.class);
      Node currentNode = container.getCurrentNode();
      String keyword = uiForm.getUIStringInput(FIELD_SEARCHVALUE).getValue();
      String type = uiForm.getUIFormSelectBox(FIELD_OPTION).getValue();
      List<ResultData> queryResult = null;
      UICBSearchResults searchResults = container.findFirstComponentOfType(UICBSearchResults.class);
      UIApplication app = uiForm.getAncestorOfType(UIApplication.class);
      if(Utils.isNameEmpty(keyword)) {
        app.addMessage(new ApplicationMessage("UICBSearchForm.msg.not-empty", null));
        
        return;
      }
      if(type.equals(CATEGORY_SEARCH)) {
        queryResult = uiForm.searchByCategory(keyword, currentNode);
      } else {
        boolean reference = uiForm.getUIFormCheckBoxInput(FIELD_CB_REF).isChecked();
        boolean relation = uiForm.getUIFormCheckBoxInput(FIELD_CB_CHILD).isChecked();
        queryResult = uiForm.searchDocument(keyword, reference, relation, currentNode);
      }
      searchResults.updateGrid(queryResult);
      if(queryResult == null || queryResult.size() == 0){
        Object[] args = new Object[]{keyword};
        app.addMessage(new ApplicationMessage("UICBSearchForm.msg.suggestion-keyword", args));
        
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UISearchController.class));
    }
  }
}
