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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * September 19, 2006
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/sidebar/UIViewRelationList.gtmpl",
    events = {@EventConfig(listeners = UIViewRelationList.ChangeNodeActionListener.class)}
)

public class UIViewRelationList extends UIContainer{

  public UIViewRelationList() throws Exception { }

  public List<Node> getRelations() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    List<Node> relations = new ArrayList<Node>() ;
    Value[] vals = null ;
    try {
      vals = uiExplorer.getCurrentNode().getProperty("exo:relation").getValues() ;
    }catch (Exception e) { return relations ;}
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    ManageableRepository repository = repositoryService.getCurrentRepository() ;
    String[] wsNames = repository.getWorkspaceNames() ;
    for(String wsName : wsNames) {
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(wsName, repository) ;
      for(Value val : vals) {
        String uuid = val.getString();
        try {
          Node node = session.getNodeByUUID(uuid) ;
          if (!Utils.isInTrash(node))
            relations.add(node) ;
        } catch(Exception e) {
          continue ;
        }
      }
    }
    return relations ;
  }

  public List<Node> getReferences() throws Exception {
      List<Node> refNodes = new ArrayList<Node>() ;
      RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
      UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
      Node currentNode = uiJCRExplorer.getCurrentNode() ;
      try {
        String uuid = currentNode.getUUID() ;
        ManageableRepository repository = repositoryService.getCurrentRepository();
        Session session = null ;
        for(String workspace : repository.getWorkspaceNames()) {
          session = repository.getSystemSession(workspace) ;
          try{
            Node lookupNode = session.getNodeByUUID(uuid) ;
            PropertyIterator iter = lookupNode.getReferences() ;
            if(iter != null) {
              while(iter.hasNext()) {
                Node refNode = iter.nextProperty().getParent() ;
                if (!Utils.isInTrash(refNode) && !refNode.isNodeType("exo:auditHistory"))
                  refNodes.add(refNode) ;
              }
            }
          } catch(Exception e) {
            continue;
          }
          finally {
            session.logout() ;
          }
        }
      } catch (UnsupportedRepositoryOperationException e) {
        // currentNode is not referenceable
      }
      return refNodes ;
    }

  public List<Node> getLanguages() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node node = uiExplorer.getCurrentNode();
    List<Node> relations = new ArrayList<Node>() ;
    MultiLanguageService langService = getApplicationComponent(MultiLanguageService.class) ;
    List<String> langs = langService.getSupportedLanguages(node);
    for(String lang: langs) {
      Node lnode = langService.getLanguage(node, lang);
      if (lnode!=null && lnode.hasProperty("exo:language")) {
        relations.add(lnode) ;
      }
    }
    return relations ;
  }

  public boolean isPreferenceNode(Node node) {
    return getAncestorOfType(UIJCRExplorer.class).isPreferenceNode(node) ;
  }

  static public class ChangeNodeActionListener extends EventListener<UIViewRelationList> {
    public void execute(Event<UIViewRelationList> event) throws Exception {
      UIViewRelationList uicomp =  event.getSource() ;
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class) ;
      String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName") ;
      Session session = uiExplorer.getSessionByWorkspace(workspaceName);
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class) ;
      try {
        session.getItem(uri);
      } catch(ItemNotFoundException nu) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null, ApplicationMessage.WARNING)) ;
        
        return ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null, ApplicationMessage.WARNING)) ;
        
        return ;
      } catch(Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return ;
      }
      uiExplorer.setSelectNode(workspaceName, uri);
      uiExplorer.updateAjax(event) ;
    }
  }
}
