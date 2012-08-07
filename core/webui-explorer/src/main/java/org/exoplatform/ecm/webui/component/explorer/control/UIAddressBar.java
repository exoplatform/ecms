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
package org.exoplatform.ecm.webui.component.explorer.control ;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.SimpleSearchValidator;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer.HistoryEntry;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormHiddenInput;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Aug 2, 2006
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/control/UIAddressBar.gtmpl",
    events = {
      @EventConfig(listeners = UIAddressBar.ChangeNodeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAddressBar.BackActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAddressBar.HistoryActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAddressBar.ChangeViewActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAddressBar.SimpleSearchActionListener.class),
      @EventConfig(listeners = UIAddressBar.RefreshSessionActionListener.class, phase = Phase.DECODE)
    }
)

public class UIAddressBar extends UIForm {
  
  public static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(.*)");
  
  /** The Constant MESSAGE_NOT_SUPPORT_KEYWORD. */
  private final static String MESSAGE_NOT_SUPPORT_KEYWORD = "UIAddressBar.msg.keyword-not-support".intern();
  
  public final static String WS_NAME = "workspaceName";  
  public final static String FIELD_ADDRESS = "address";
  public final static String FIELD_ADDRESS_HIDDEN = "address_hidden";
  public final static String ACTION_TAXONOMY = "exo:taxonomyAction";
  public final static String EXO_TARGETPATH = "exo:targetPath"; 
  public final static String EXO_TARGETWORKSPACE = "exo:targetWorkspace";
  
  private String selectedViewName_;
  
  private String[] arrView_ = {};
  final static private String FIELD_SIMPLE_SEARCH = "simpleSearch" ;

  final static private  String ROOT_SQL_QUERY  = "select * from nt:base where not (jcr:primaryType like 'exo:symlink' or jcr:primaryType like 'exo:taxonomyLink') and (contains(*, '$1') or lower(exo:name) like '%$2%' )order by exo:dateCreated DESC, jcr:primaryType DESC" ;
  final static private String SQL_QUERY = "select * from nt:base where not (jcr:primaryType like 'exo:symlink' or jcr:primaryType like 'exo:taxonomyLink') and (jcr:path like '$0/%' and ( contains(*, '$1') or lower(exo:name) like '%$2%' )) order by jcr:path DESC, jcr:primaryType DESC";
     
  public UIAddressBar() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_ADDRESS, FIELD_ADDRESS, null)) ;
    addUIFormInput(new UIFormStringInput(FIELD_SIMPLE_SEARCH, FIELD_SIMPLE_SEARCH, null).addValidator(SimpleSearchValidator.class));
    addUIFormInput(new UIFormHiddenInput(FIELD_ADDRESS_HIDDEN, FIELD_ADDRESS_HIDDEN, null));
  }

  public void setViewList(List<String> viewList) {
    Collections.sort(viewList);
    arrView_ = viewList.toArray(new String[viewList.size()]); 
  }
  
  public String[] getViewList() { return arrView_; } 
  
  public void setSelectedViewName(String viewName) { selectedViewName_ = viewName; }
  
  public boolean isSelectedView(String viewName) { 
    if(selectedViewName_ != null && selectedViewName_.equals(viewName)) return true;
    return false;
  }
  
  public String getSelectedViewName() { return selectedViewName_; }
  
  @Deprecated
  public Set<String> getHistory() {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return uiJCRExplorer.getAddressPath() ;
  }

  public Collection<HistoryEntry> getFullHistory() {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return uiJCRExplorer.getHistory() ;
  }
  
  static public class BackActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIAddressBar uiAddressBar = event.getSource() ;
      UIJCRExplorer uiExplorer = uiAddressBar.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class) ;
      try {        
        UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
        UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
        
        if(!uiDocumentWorkspace.isRendered()) {
          uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
          uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
          uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class) ;
        }
        if(uiExplorer.isViewTag() && !uiExplorer.getCurrentNode().equals(uiExplorer.getRootNode())) {
          uiExplorer.setSelectRootNode() ;
          uiExplorer.setIsViewTag(true) ;
        } else if(uiExplorer.isViewTag() && uiExplorer.getCurrentStateNode() != null) {
          uiExplorer.setIsViewTag(false) ;
          uiExplorer.setSelectNode(uiExplorer.getCurrentStatePath()) ;
        } else {
          String previousNodePath = uiExplorer.rewind() ;
          String previousWs = uiExplorer.previousWsName();
          uiExplorer.setBackNodePath(previousWs, previousNodePath);
        }
        uiExplorer.updateAjax(event) ;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UIAddressBar.msg.access-denied", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIJCRExplorer.msg.no-node-history",
                                                null, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
    }
  }
  
  static public class ChangeNodeActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIAddressBar uiAddress = event.getSource() ;      
      String path = null;
      if(uiAddress.getUIInput(FIELD_ADDRESS_HIDDEN).getValue() != null)
      	path = uiAddress.getUIInput(FIELD_ADDRESS_HIDDEN).getValue().toString();
      if (path == null || path.trim().length() == 0) path = "/";
      UIJCRExplorer uiExplorer = uiAddress.getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.setIsViewTag(false) ;
      try {
        String prefix = uiExplorer.getRootPath();
        String nodePath = LinkUtils.evaluatePath(LinkUtils.createPath(prefix, path));
        uiExplorer.setSelectNode(nodePath) ;
        uiExplorer.setCurrentStatePath(nodePath) ;
      } catch(Exception e) {
        UIApplication uiApp = uiAddress.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIAddressBar.msg.path-not-found", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      uiExplorer.updateAjax(event) ;
    }
  }
  
  static public class HistoryActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIAddressBar uiAddressBar = event.getSource() ;
      String fullPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIJCRExplorer uiExplorer = uiAddressBar.getAncestorOfType(UIJCRExplorer.class) ;
      String workspace = null;
      String path = null;
      try{
        Matcher matcher = FILE_EXPLORER_URL_SYNTAX.matcher(fullPath);
        if (matcher.find()) {
          workspace = matcher.group(1);
          path = matcher.group(2);
        }
        uiExplorer.setSelectNode(workspace, path) ;
        uiExplorer.refreshExplorer() ;
      } catch (AccessDeniedException ade) {
        UIApplication uiApp = uiAddressBar.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIAddressBar.msg.access-denied", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
    }
  }
  
  static public class ChangeViewActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIAddressBar uiAddressBar = event.getSource() ;
      UIJCRExplorer uiExplorer = uiAddressBar.getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      }
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiAddressBar.setSelectedViewName(viewName);
      UIControl uiControl = uiAddressBar.getParent() ;
      UIActionBar uiActionBar = uiControl.getChild(UIActionBar.class) ;
      uiActionBar.setTabOptions(viewName) ;
      uiExplorer.updateAjax(event);
    }
  }
  
  static public class SimpleSearchActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIAddressBar uiAddressBar = event.getSource();
      UIJCRExplorer uiExplorer = uiAddressBar.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiAddressBar.getAncestorOfType(UIApplication.class);
      String text = uiAddressBar.getUIStringInput(FIELD_SIMPLE_SEARCH).getValue();
      Node currentNode = uiExplorer.getCurrentNode();
      String queryStatement = null;
      if("/".equals(currentNode.getPath())) {
        queryStatement = ROOT_SQL_QUERY;        
      }else {
        queryStatement = StringUtils.replace(SQL_QUERY,"$0",currentNode.getPath());
      }
      queryStatement = StringUtils.replace(queryStatement,"$1", text.replaceAll("'", "''"));
      queryStatement = StringUtils.replace(queryStatement,"$2", text.replaceAll("'", "''").toLowerCase());      
      uiExplorer.removeChildById("ViewSearch");
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      }
      SessionProvider sessionProvider = new SessionProvider(ConversationState.getCurrent());
      Session session = sessionProvider.getSession(currentNode.getSession().getWorkspace().getName(), 
          (ManageableRepository)currentNode.getSession().getRepository());
      UISearchResult uiSearchResult = uiDocumentWorkspace.getChildById(UIDocumentWorkspace.SIMPLE_SEARCH_RESULT);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      long startTime = System.currentTimeMillis();
      QueryResult queryResult = null;
      try {
      	Query query = queryManager.createQuery(queryStatement, Query.SQL);        
      	queryResult = query.execute();
      } catch (InvalidQueryException invalidEx) {
        uiApp.addMessage(new ApplicationMessage(MESSAGE_NOT_SUPPORT_KEYWORD, null, ApplicationMessage.WARNING));
        return;
      } catch (RepositoryException reEx) {
        uiApp.addMessage(new ApplicationMessage(MESSAGE_NOT_SUPPORT_KEYWORD, null, ApplicationMessage.WARNING));
        return;
      }               
      uiSearchResult.clearAll();
      uiSearchResult.setRootLink( currentNode.getPath(), text.replaceAll("'", "''"), 
      														currentNode.getSession().getWorkspace().getName());
      long time = System.currentTimeMillis() - startTime;
      uiSearchResult.setSearchTime(time);
      uiSearchResult.setQueryResults(queryResult);
      uiDocumentWorkspace.setRenderedChild(UISearchResult.class);
      if(!uiDocumentWorkspace.isRendered()) {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
      }
      if (session != null) session.logout();
    }
  }
  
  static public class RefreshSessionActionListener extends EventListener<UIAddressBar> {
    public void execute(Event<UIAddressBar> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiJCRExplorer.getSession().refresh(false) ;
      uiJCRExplorer.refreshExplorer() ;
      UIControl uiControl = event.getSource().getParent() ;
      UIActionBar uiActionBar = uiControl.getChild(UIActionBar.class) ;
      uiActionBar.setTabOptions(event.getSource().getSelectedViewName()) ;
      UIApplication uiApp = uiJCRExplorer.getAncestorOfType(UIApplication.class) ;
      String mess = "UIJCRExplorer.msg.refresh-session-success" ;
      uiApp.addMessage(new ApplicationMessage(mess, null, ApplicationMessage.INFO)) ;
    }
  }
  
}