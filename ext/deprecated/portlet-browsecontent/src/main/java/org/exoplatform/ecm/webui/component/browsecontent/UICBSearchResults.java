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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 26, 2006 11:39:54 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/browse/UICBSearchResults.gtmpl",
    events = {
        @EventConfig(listeners = UICBSearchResults.CloseActionListener.class),
        @EventConfig(listeners = UICBSearchResults.ViewActionListener.class),
        @EventConfig(listeners = UICBSearchResults.GotoActionListener.class)
    }
)
public class UICBSearchResults extends UIContainer {
  protected Map<String, Node> resultMap_ = new HashMap<String, Node>();
  private UIPageIterator uiPageIterator_;

  public UICBSearchResults() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, null);
  }

  public List getCurrentList() throws Exception {
    return uiPageIterator_.getCurrentPageData();
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  private boolean isDocumentTemplate(String nodeType)throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.getDocumentTemplates().contains(nodeType);
  }
  static public class CloseActionListener extends EventListener<UICBSearchResults> {
    public void execute(Event<UICBSearchResults> event) throws Exception {
      UICBSearchResults uiResults = event.getSource();
      UISearchController uiSearchController = uiResults.getAncestorOfType(UISearchController.class);
      uiSearchController.setShowHiddenSearch();
    }
  }
  protected void getResultData() throws Exception {
    List<ResultData> results = new ArrayList<ResultData>();
    for(String nodeName : resultMap_.keySet()) {
      results.add(new ResultData(Utils.formatNodeName(nodeName),
          Utils.formatNodeName(resultMap_.get(nodeName).getPath()),
          resultMap_.get(nodeName).getSession().getWorkspace().getName()));
    }
  }
  static public class ViewActionListener extends EventListener<UICBSearchResults> {
    public void execute(Event<UICBSearchResults> event) throws Exception {
      UICBSearchResults uiResults = event.getSource();
      String itemPath = event.getRequestContext().getRequestParameter(OBJECTID);
      String wsName = event.getRequestContext().getRequestParameter("workspaceName");
      UIBrowseContainer container = uiResults.getAncestorOfType(UIBrowseContainer.class);
      Node node = null;
      if(wsName != null) {
        node = container.getNodeByPath(itemPath, wsName);
      } else {
        node = container.getNodeByPath(itemPath);
      }
      UIApplication uiApp = uiResults.getAncestorOfType(UIApplication.class);
      if(node == null) {
        uiApp.addMessage(new ApplicationMessage("UICBSearchResults.msg.node-removed", null));
        
        return;
      }
      NodeType nodeType = node.getPrimaryNodeType();
      UISearchController uiSearchController = uiResults.getAncestorOfType(UISearchController.class);
      if(uiResults.isDocumentTemplate(nodeType.getName())) {
        UIBrowseContentPortlet cbPortlet = uiResults.getAncestorOfType(UIBrowseContentPortlet.class);
        UIPopupContainer uiPopupAction = cbPortlet.getChildById("UICBPopupAction");
        UIDocumentDetail uiDocument =  uiPopupAction.activate(UIDocumentDetail.class, 600);
        // cbPortlet.createUIComponent(UIDocumentDetail.class, null, null);
        uiDocument.setOriginalNode(node);
        uiDocument.setNode(node);
        UIPopupWindow uiPopup  = uiPopupAction.getChildById("UICBPopupWindow");
        uiPopup.setResizable(true);
        uiPopup.setShowMask(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
        return;
      }
      if(container.isCategories(nodeType)) {
        uiSearchController.setShowHiddenSearch();
        if(container.getPortletPreferences().getValue(Utils.CB_TEMPLATE, "").equals("TreeList")) {
          container.getChild(UICategoryTree.class).buildTree(itemPath);
          container.setCurrentNodePath(itemPath);
        }
        container.changeNode(node);
        return;
      }
    }
  }

  static public class GotoActionListener extends EventListener<UICBSearchResults> {
    public void execute(Event<UICBSearchResults> event) throws Exception {
      UICBSearchResults uiResults = event.getSource();
      String itemPath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIBrowseContainer container = uiResults.getAncestorOfType(UIBrowseContainer.class);
      container.getListHistoryNode();
      UIApplication uiApp = uiResults.getAncestorOfType(UIApplication.class);
      Node parentNode = null;
      try {
        Node node = container.getNodeByPath(itemPath);
        if (node == null) {
          uiApp.addMessage(new ApplicationMessage("UICBSearchResults.msg.node-removed", null));
          
          return;
        }
        if (node.getPath().equals(container.getRootNode().getPath()))
          parentNode = node;
        else
          parentNode = node.getParent();
        NodeType nodeType = parentNode.getPrimaryNodeType();
        UISearchController uiSearchController = uiResults
            .getAncestorOfType(UISearchController.class);
        if (container.isCategories(nodeType)) {
          uiSearchController.setShowHiddenSearch();
          if (container.getPortletPreferences().getValue(Utils.CB_TEMPLATE, "").equals("TreeList")) {
            container.getChild(UICategoryTree.class).buildTree(parentNode.getPath());
            container.setCurrentNodePath(parentNode.getPath());
          }
          container.changeNode(parentNode);
          List<Node> listGoToNode = new ArrayList<Node>();
          if (!container.getListHistoryNode().contains(parentNode)) {
            while (!parentNode.getPath().equals(container.getRootPath())) {
              listGoToNode.add(parentNode);
              parentNode = parentNode.getParent();
            }
            if (listGoToNode.size() > 0) {
              for (int i = listGoToNode.size() - 1; i >= 0; i--) {
                if (!container.getListHistoryNode().contains(listGoToNode.get(i)))
                  container.getListHistoryNode().add(listGoToNode.get(i));
              }
            }
          }
          return;
        }
      } catch (AccessDeniedException e) {
        uiApp.addMessage(new ApplicationMessage("UICBSearchResults.msg.not-permission", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      if (uiResults.isDocumentTemplate(parentNode.getPrimaryNodeType().getName())) {
        UIBrowseContentPortlet cbPortlet = uiResults
            .getAncestorOfType(UIBrowseContentPortlet.class);
        UIPopupContainer uiPopupAction = cbPortlet.getChildById("UICBPopupAction");
        UIDocumentDetail uiDocument = uiPopupAction.activate(UIDocumentDetail.class, 600);
        uiDocument.setNode(parentNode);
        UIPopupWindow uiPopup = uiPopupAction.getChildById("UICBPopupWindow");
        uiPopup.setResizable(true);
        uiPopup.setShowMask(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
        return;
      }
    }
  }

  public String[] getActions() { return new String[] {"Close"};}

  public void updateGrid(List<ResultData> rsult) throws Exception {
    ListAccess<ResultData> resultData = new ListAccessImpl<ResultData>(ResultData.class, rsult);
    LazyPageList<ResultData> dataPageList = new LazyPageList<ResultData>(resultData, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  public static class ResultData {
    private String name;
    private String path;
    private String wsName;
    public ResultData(String rName, String rpath, String wsName) {
      this.name = rName;
      this.path = rpath;
      this.wsName = wsName;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getWorkspaceName() { return wsName; }
  }
}
