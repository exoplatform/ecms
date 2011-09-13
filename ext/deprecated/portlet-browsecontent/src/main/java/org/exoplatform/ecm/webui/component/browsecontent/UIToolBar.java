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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.portlet.PortletMode;

import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Jan 5, 2007 2:32:34 PM
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/browse/UIToolBar.gtmpl",
    events = {
        @EventConfig(listeners = UIToolBar.ShowHiddenActionListener.class),
        @EventConfig(listeners = UIToolBar.SelectPathActionListener.class),
        @EventConfig(listeners = UIToolBar.VoteActionListener.class),
        @EventConfig(listeners = UIToolBar.CommentActionListener.class),
        @EventConfig(listeners = UIToolBar.BackActionListener.class),
        @EventConfig(listeners = UIToolBar.NextActionListener.class),
        @EventConfig(listeners = UIToolBar.SearchActionListener.class)
    }
)

public class UIToolBar extends UIContainer {
  private boolean isEnableTree_ = true ;
  private boolean isEnablePath_ = true ;
  private boolean isEnableSeach_ = false ;

  private static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(/.*)");

  public UIToolBar()throws Exception {}
  public void setEnablePath(boolean enablePath) {isEnablePath_ = enablePath ;}
  public boolean enablePath() {return isEnablePath_ ;}
  public void setEnableSearch(boolean enableSearch) {isEnableSeach_ = enableSearch ;}
  public boolean enableSearch() {return isEnableSeach_ ;}
  public void setEnableTree(boolean enableTree) {isEnableTree_ = enableTree ;}
  public boolean enableTree() {return isEnableTree_ ;}

  public List<Node> getNodePaths(Node node) throws Exception {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class);
    return uiContainer.getListHistoryNode();

    /*
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class);
    Node rootNode = getRootNode() ;
    Node historyNode = getHistoryNode();
    if(!uiContainer.getWorkSpace().equals(node.getSession().getWorkspace().getName())) {
      return new ArrayList<Node>();
    }
    List<Node> list = new ArrayList<Node>();
    if(node != null) {
      Node temp = node ;
      if(!temp.getPath().equals("/") && temp.getPath().startsWith(rootNode.getPath())) {
        boolean readyHistoryNode = true;
        while(!temp.getPath().equals(rootNode.getPath())) {
          if (!list.contains(temp)) list.add(0, temp);
          if ((readyHistoryNode) && (temp != historyNode) && (historyNode != null) &&
              (historyNode.isNodeType(Utils.EXO_SYMLINK))) {
            temp = historyNode;
            readyHistoryNode = false;
          } else {
            temp = temp.getParent();
          }
        }
      }
    }
    return list;
    */
  }
  public boolean enableComment() {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class);
    return Boolean.parseBoolean(uiContainer.getPortletPreferences().getValue(Utils.CB_VIEW_COMMENT,"")) ;
  }
  public boolean enableVote() {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class) ;
    return Boolean.parseBoolean(uiContainer.getPortletPreferences().getValue(Utils.CB_VIEW_VOTE,"")) ;
  }
  public Node getRootNode() throws Exception {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class) ;
    if(uiContainer.getRootPath() != null) return uiContainer.getRootNode() ;
    return uiContainer.getSession(true).getRootNode();
  }

  public Node getCurrentNode() throws Exception {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class) ;
    return uiContainer.getCurrentNode();
  }

  public Node getHistoryNode() throws Exception {
    UIBrowseContainer uiContainer = getAncestorOfType(UIBrowseContainer.class) ;
    Node historyNode = uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT);
    return historyNode;
  }

  public boolean isShowCategoryTree() {
    UIBrowseContainer uiBrowseContainer = getAncestorOfType(UIBrowseContainer.class) ;
    return  uiBrowseContainer.isShowCategoryTree() ;
  }

  public boolean isClearHistory() {
    return getAncestorOfType(UIBrowseContainer.class).getNodesHistory().isEmpty() ;
  }

  static  public class SelectPathActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiComp = event.getSource();
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIBrowseContainer uiContainer = uiComp.getAncestorOfType(UIBrowseContainer.class);
      UIBrowseContentPortlet uiBCPortlet = uiComp.getAncestorOfType(UIBrowseContentPortlet.class);
      Node selectNode = uiContainer.getNodeByPath(nodePath);
      uiContainer.getHistory().clear();
      if (selectNode == null) {
        if (uiContainer.getNodeByPath(uiContainer.getCategoryPath()) == null) {
          uiBCPortlet.setPorletMode(PortletMode.HELP);
          uiBCPortlet.reload();
        } else {
          UIApplication uiApp = uiComp.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.node-removed", null));
          
          selectNode = uiComp.getRootNode();
          uiContainer.changeNode(selectNode);
        }
      } else {
        uiContainer.storeListHistory(selectNode);
        if (selectNode.isNodeType(Utils.EXO_SYMLINK)) {
          LinkManager linkManager = uiContainer.getApplicationComponent(LinkManager.class);
          selectNode = linkManager.getTarget(selectNode);
        }

        TemplateService templateService = uiContainer.getApplicationComponent(TemplateService.class);
        List templates = templateService.getDocumentTemplates();
        if (templates.contains(selectNode.getPrimaryNodeType().getName())) {
          ManageViewService vservice = uiContainer.getApplicationComponent(ManageViewService.class);
          String detailTemplateName = uiContainer.getPortletPreferences()
                                                 .getValue(Utils.CB_BOX_TEMPLATE, "");
          uiContainer.setTemplateDetail(vservice.getTemplateHome(BasePath.CB_DETAIL_VIEW_TEMPLATES,
                                                                 SessionProviderFactory.createSystemProvider())
                                                .getNode(detailTemplateName)
                                                .getPath());
          uiContainer.viewDocument(selectNode, true);
        } else {
          String templateType = uiContainer.getPortletPreferences().getValue(Utils.CB_USECASE, "");
          if ((templateType.equals(Utils.CB_USE_JCR_QUERY))
              || (templateType.equals(Utils.CB_SCRIPT_NAME))) {
            UIApplication app = uiContainer.getAncestorOfType(UIApplication.class);
            app.addMessage(new ApplicationMessage("UIBrowseContainer.msg.template-notsupported",
                                                  null));
            
          } else {
            uiContainer.changeNode(selectNode);
            uiContainer.setPageIterator(uiContainer.getSubDocumentList(selectNode));
          }
        }
        uiContainer.setCurrentNodePath(nodePath);
        uiContainer.setSelectedTabPath(nodePath);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiBCPortlet);
    }
  }

  static public class ShowHiddenActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiToolBar = event.getSource() ;
      UIBrowseContainer uiBrowseContainer = uiToolBar.getAncestorOfType(UIBrowseContainer.class) ;
      uiBrowseContainer.setShowCategoryTree(!uiBrowseContainer.isShowCategoryTree()) ;
    }
  }

  static public class SearchActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiToolBar = event.getSource() ;
      UIBrowseContainer uiContainer = uiToolBar.getAncestorOfType(UIBrowseContainer.class) ;
      if (uiContainer.isShowDocumentDetail()) {
        UIApplication uiApp = uiToolBar.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.back-view-search", null));
        
        return;
      }
      UISearchController uiSearchController = uiContainer.getChild(UISearchController.class);
      uiSearchController.setShowHiddenSearch();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class NextActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiComp = event.getSource() ;
      UIBrowseContainer uiContainer = uiComp.getAncestorOfType(UIBrowseContainer.class);
      uiContainer.historyNext();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class BackActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiComp = event.getSource();
      UIBrowseContainer uiContainer = uiComp.getAncestorOfType(UIBrowseContainer.class);
      uiContainer.historyBack();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class VoteActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiComp = event.getSource();
      UIBrowseContainer container = uiComp.getAncestorOfType(UIBrowseContainer.class);
      UIDocumentDetail uiDocument = container.getChild(UIDocumentDetail.class);
      UIApplication uiApp = uiComp.getAncestorOfType(UIApplication.class);
      if (!container.isShowDocumentDetail() || !uiDocument.isValidNode()) {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.select-doc", null));
        
        event.getRequestContext().addUIComponentToUpdateByAjax(container);
        return;
      }
      if (!uiDocument.node_.isNodeType("mix:votable")) {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.not-support-vote",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      String lockToken = LockUtil.getLockToken(uiDocument.node_);
      if (lockToken != null)
        uiDocument.node_.getSession().addLockToken(lockToken);
      if (container.nodeIsLocked(uiDocument.node_)) {
        String strLockOwner = uiDocument.node_.getLock().getLockOwner();
        String userId = Util.getPortalRequestContext().getRemoteUser();
        if (!strLockOwner.equals(userId)) {
          uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.node-is-locked",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      }
      if ((uiDocument.node_.isCheckedOut())) {
        UIBrowseContentPortlet cbPortlet = uiComp.getAncestorOfType(UIBrowseContentPortlet.class);
        UIPopupContainer uiPopupAction = cbPortlet.getChildById("UICBPopupAction");
        uiPopupAction.activate(UICBVoteForm.class, 300);
        uiPopupAction.getChild(UIPopupWindow.class).setResizable(false);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.readonly-doc",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
    }
  }
  static public class CommentActionListener extends EventListener<UIToolBar> {
    public void execute(Event<UIToolBar> event) throws Exception {
      UIToolBar uiComp = event.getSource();
      UIBrowseContainer container = uiComp.getAncestorOfType(UIBrowseContainer.class);
      UIDocumentDetail uiDocument = container.getChild(UIDocumentDetail.class);
      UIApplication uiApp = uiComp.getAncestorOfType(UIApplication.class);
      Node documentNode;
      String documentPath = event.getRequestContext().getRequestParameter(OBJECTID);
      if (documentPath != null && documentPath.length() > 0) {
        Matcher matcher = UIToolBar.FILE_EXPLORER_URL_SYNTAX.matcher(documentPath);
        String wsName = null;
        if (matcher.find()) {
          wsName = matcher.group(1);
          documentPath = matcher.group(2);
          documentNode = container.getNodeByPath(documentPath, wsName);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + documentPath + "'");
        }
      } else  {
        documentNode = uiDocument.getOriginalNode();
      }
      if ((documentNode == null) || !container.isShowDocumentDetail()) {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.select-doc", null,
            ApplicationMessage.WARNING)) ;
        
        event.getRequestContext().addUIComponentToUpdateByAjax(container) ;
        return ;
      }
      if(!container.hasAddPermission(documentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.access-add-denied", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      }
      if(!documentNode.isNodeType("mix:commentable")) {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.not-support-comment", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      }
      String lockToken = LockUtil.getLockToken(documentNode);
      if(lockToken != null) documentNode.getSession().addLockToken(lockToken);
      if(container.nodeIsLocked(documentNode)) {
        String strLockOwner = documentNode.getLock().getLockOwner();
        String userId = Util.getPortalRequestContext().getRemoteUser();
        if (!strLockOwner.equals(userId)) {
          uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.node-is-locked", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
      if ((documentNode.isCheckedOut())) {
        UIBrowseContentPortlet cbPortlet = uiComp.getAncestorOfType(UIBrowseContentPortlet.class);
        UICBCommentForm commentForm = uiComp.createUIComponent(UICBCommentForm.class, null, null);
        commentForm.setDocument(documentNode);
        UIPopupContainer uiPopupAction = cbPortlet.getChildById("UICBPopupComment");
        if (uiPopupAction == null) {
          uiPopupAction = cbPortlet.addChild(UIPopupContainer.class, null, "UICBPopupComment");
        }
        String nodeCommentPath = event.getRequestContext().getRequestParameter("nodePath");
        if (nodeCommentPath != null && nodeCommentPath.length() > 0) {
          commentForm.setEdit(true);
          commentForm.setNodeCommentPath(nodeCommentPath);
        }
        uiPopupAction.activate(commentForm, 750, 0);
        uiPopupAction.getChild(UIPopupWindow.class).setResizable(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIToolBar.msg.readonly-doc",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
    }
  }
}
