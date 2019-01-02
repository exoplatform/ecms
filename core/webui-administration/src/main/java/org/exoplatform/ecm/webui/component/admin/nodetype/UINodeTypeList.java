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
package org.exoplatform.ecm.webui.component.admin.nodetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputInfo;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 20, 2006
 * 3:28:26 PM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/nodetype/UINodeTypeList.gtmpl",
    events = {
      @EventConfig(listeners = UINodeTypeList.ViewActionListener.class),
      @EventConfig(listeners = UINodeTypeList.EditActionListener.class),
      @EventConfig(listeners = UINodeTypeList.DeleteActionListener.class, confirm="UINodeTypeList.msg.confirm-delete"),
      @EventConfig(listeners = UINodeTypeList.AddActionListener.class),
      @EventConfig(listeners = UINodeTypeList.ImportActionListener.class),
      @EventConfig(listeners = UINodeTypeList.ExportActionListener.class)
    }
)
public class UINodeTypeList extends UIPagingGridDecorator {

  final static public String DRAFTNODETYPE = "jcr:system/jcr:nodetypesDraft" ;
  final static public String[] ACTIONS = {"Add", "Import", "Export"} ;
  final static public String[] CANCEL = {"Cancel"} ;
  final static public String[] TAB_REMOVE = {
    UINodeTypeForm.SUPER_TYPE_TAB, UINodeTypeForm.DEFAULT_PRIMARY_TYPE_TAB,
    UINodeTypeForm.REQUIRED_PRIMARY_TYPE_TAB} ;

  public UINodeTypeList() throws Exception {
    getUIPageIterator().setId("UINodeTypeListIterator") ;
  }

  @SuppressWarnings("unchecked")
  public List getAllNodeTypes() throws Exception{
    List nodeList = new ArrayList<NodeTypeBean>();
    ManageableRepository mRepository = getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
    NodeTypeManager ntManager = mRepository.getNodeTypeManager() ;
    NodeTypeIterator nodeTypeIter = ntManager.getAllNodeTypes() ;
    while(nodeTypeIter.hasNext()) {
      nodeList.add(new NodeTypeBean(nodeTypeIter.nextNodeType())) ;
    }
    Collections.sort(nodeList, new NodeTypeNameComparator()) ;
    Session session = mRepository.getSystemSession(mRepository.getConfiguration().getSystemWorkspaceName()) ;
    Node rootNode = session.getRootNode();
    if(rootNode.hasNode(DRAFTNODETYPE)) {
      Node draftNode = rootNode.getNode(DRAFTNODETYPE) ;
      NodeIterator nodeIter = draftNode.getNodes() ;
      while(nodeIter.hasNext()) {
        nodeList.add(nodeIter.nextNode()) ;
      }
    }
    session.logout() ;
    return nodeList ;
  }

  public List getNodeTypeList() throws Exception { 
    return NodeLocation.getNodeListByLocationList(getUIPageIterator().getCurrentPageData()); 
  }

  public String[] getActions() { return ACTIONS ; }

  public void refresh(String name, int currentPage, List<NodeTypeBean> nodeType) throws Exception {
    ListAccess<Object> nodeTypeList = new ListAccessImpl<Object>(Object.class,
                                                                 NodeLocation.getLocationsByNodeList(nodeType));
    LazyPageList<Object> pageList = new LazyPageList<Object>(nodeTypeList, getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public void refresh(String name, int currentPage) throws Exception {
    ManageableRepository manaRepository =
      getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
    Session session = manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;
    if(name != null) {
      Node rootNode = session.getRootNode();
      if(rootNode.hasNode(DRAFTNODETYPE)) {
        Node draftNode = rootNode.getNode(DRAFTNODETYPE) ;
        if(draftNode.hasNode(name)) {
          Node deleteNode = draftNode.getNode(name) ;
          deleteNode.remove() ;
          draftNode.save() ;
        }
        if(!draftNode.hasNodes())draftNode.remove() ;
        session.save() ;
      }
    } else {
      session.refresh(true) ;
    }
    session.logout();
    refresh(name, currentPage, getAllNodeTypes());
  }

  static public class AddActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiList = event.getSource() ;
      UINodeTypeManager uiManager = uiList.getParent() ;
      uiManager.initPopup(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class ImportActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiList = event.getSource() ;
      UINodeTypeManager uiManager = uiList.getParent() ;
      uiManager.setImportPopup() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class ExportActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiList = event.getSource() ;
      UINodeTypeManager uiManager = uiList.getParent() ;
      uiManager.setExportPopup() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class ViewActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiList = event.getSource() ;
      String ntName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      ManageableRepository manaRepository =
        uiList.getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
      Session session = manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;
      NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager() ;
      NodeType nodeType = ntManager.getNodeType(ntName) ;
      UINodeTypeManager uiManager = uiList.getParent() ;
      uiManager.initPopup(true) ;
      UINodeTypeForm uiForm = uiManager.findFirstComponentOfType(UINodeTypeForm.class) ;
      uiForm.update(nodeType, true) ;
      for(UIComponent uiComp : uiForm.getChildren()) {
        UIFormInputSetWithAction tab = uiForm.getChildById(uiComp.getId()) ;
        for(UIComponent uiChild : tab.getChildren()) {
          if(!(uiChild instanceof UIFormInputInfo)) tab.setActionInfo(uiChild.getName(), null) ;
        }
        if(tab.getId().equals(UINodeTypeForm.NODETYPE_DEFINITION)) {
          tab.setRendered(true) ;
          tab.setActions(new String[] {"Close"}, null) ;
        } else {
          tab.setRendered(true) ;
          tab.setActions(null, null) ;
        }
      }
      uiForm.removeChildTabs(TAB_REMOVE) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class EditActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiNodeList = event.getSource() ;
      ManageableRepository manaRepository =
        uiNodeList.getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
      Session session = manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;
      String nodeName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node draftNodeType = session.getRootNode().getNode(DRAFTNODETYPE + "/" + nodeName) ;
      UINodeTypeManager uiManager = uiNodeList.getParent() ;
      uiManager.initPopup(false) ;
      UINodeTypeForm uiForm = uiManager.findFirstComponentOfType(UINodeTypeForm.class) ;
      uiForm.refresh() ;
      uiForm.removeChildTabs(TAB_REMOVE) ;
      uiForm.updateEdit(draftNodeType, true) ;
      UIFormInputSetWithAction tab = uiForm.getChildById(UINodeTypeForm.NODETYPE_DEFINITION) ;
      String[] actionNames = {UINodeTypeForm.ACTION_SAVE, UINodeTypeForm.ACTION_SAVEDRAFT,
                              UINodeTypeForm.ACTION_CANCEL} ;
      tab.setActions(actionNames, null) ;
      tab.setIsView(false) ;
      uiForm.setTabRender(UINodeTypeForm.NODETYPE_DEFINITION) ;
      session.logout() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  static public class DeleteActionListener extends EventListener<UINodeTypeList> {
    public void execute(Event<UINodeTypeList> event) throws Exception {
      UINodeTypeList uiNodeList = event.getSource() ;
      ManageableRepository manaRepository =
        uiNodeList.getApplicationComponent(RepositoryService.class).getCurrentRepository();
      Session session = manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;
      String nodeName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node rootNode = session.getRootNode();
      if(rootNode.hasNode(DRAFTNODETYPE)) {
        Node draftNode = rootNode.getNode(DRAFTNODETYPE) ;
        Node deleteNode = draftNode.getNode(nodeName) ;
        deleteNode.remove() ;
        draftNode.save() ;
        if(!draftNode.hasNodes()) draftNode.remove() ;
        session.save() ;
        uiNodeList.refresh(null, uiNodeList.getUIPageIterator().getCurrentPage());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiNodeList.getParent()) ;
      }
      session.logout() ;
    }
  }

  public static class NodeTypeBean {
    private String nodeTypeName_;
    private boolean isMixin_;
    private boolean hasOrderableChildNodes_;
    
    public NodeTypeBean(NodeType nodeType) {
      this.nodeTypeName_ = nodeType.getName();
      this.isMixin_ = nodeType.isMixin();
      this.hasOrderableChildNodes_ = nodeType.hasOrderableChildNodes();
    }

    public String getName() {
      return nodeTypeName_;
    }

    public void setName(String nodeTypeName) {
      nodeTypeName_ = nodeTypeName;
    }

    public boolean isMixin() {
      return isMixin_;
    }

    public void setMixin(boolean isMixin) {
      isMixin_ = isMixin;
    }
    
    public boolean hasOrderableChildNodes() {
      return hasOrderableChildNodes_;
    }
    
    public void setOrderableChildNodes(boolean value) {
      hasOrderableChildNodes_ = value;
    }
  }
  
  static public class NodeTypeNameComparator implements Comparator<NodeTypeBean> {
    public int compare(NodeTypeBean n1, NodeTypeBean n2) throws ClassCastException {
      String name1 = n1.getName();
      String name2 = n2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  public void refresh(int currentPage) throws Exception {
    refresh(null, currentPage);
  }
}
