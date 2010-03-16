/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.selector;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
@ComponentConfig(
                 template =  "classpath:groovy/wcm/webui/selector/UISelectPathPanel.gtmpl",
                 events = {
                     @EventConfig(listeners = UISelectPathPanel.SelectActionListener.class)
                 }
)
public class UISelectPathPanel extends UIContainer {

	/** The ui page iterator_. */
  private UIPageIterator uiPageIterator_;

  /** The accepted mime types. */
  public String[] acceptedMimeTypes = {};

  /** The parent node. */
  protected Node parentNode;

  /** The accepted node types. */
  private String[] acceptedNodeTypes = {};

  /** The excepted node types. */
  private String[] exceptedNodeTypes = {};

  /** The default excepted node types. */
  private String[] defaultExceptedNodeTypes = {};

  /** The allow publish. */
  private boolean allowPublish = false;

  /** The publication service_. */
  protected PublicationService publicationService_ = null;

  /** The templates_. */
  private List<String> templates_ = null;

  final static String PATH = "path".intern();
  
  private boolean isDMSDocument;
  
  private boolean isWebContent;

  public boolean isWebContent() {
    return isWebContent;
  }

  public void setWebContent(boolean isWebContent) {
    this.isWebContent = isWebContent;
  }

  public boolean isDMSDocument() {
    return isDMSDocument;
  }

  public void setDMSDocument(boolean isDMSDocument) {
    this.isDMSDocument = isDMSDocument;
  }

  /**
   * Instantiates a new uI select path panel.
   * 
   * @throws Exception the exception
   */
  public UISelectPathPanel() throws Exception { 
    uiPageIterator_ = addChild(UIPageIterator.class, null, "UISelectPathIterate");
  }

  /**
   * Gets the uI page iterator.
   * 
   * @return the uI page iterator
   */
  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  /**
   * Checks if is allow publish.
   * 
   * @return true, if is allow publish
   */
  public boolean isAllowPublish() {
    return allowPublish;
  }

  /**
   * Sets the allow publish.
   * 
   * @param allowPublish the allow publish
   * @param publicationService the publication service
   * @param templates the templates
   */
  public void setAllowPublish(boolean allowPublish, PublicationService publicationService, List<String> templates) {
    this.allowPublish = allowPublish;
    publicationService_ = publicationService;
    templates_ = templates;
  }

  /**
   * Adds the node publish.
   * 
   * @param listNode the list node
   * @param node the node
   * @param publicationService the publication service
   * 
   * @throws Exception the exception
   */
  protected void addNodePublish(List<Node> listNode, Node node, PublicationService publicationService) throws Exception {
    if (isAllowPublish()) {
      NodeType nt = node.getPrimaryNodeType();
      if (templates_.contains(nt.getName())) { 
        Node nodecheck = publicationService.getNodePublish(node, null);
        if (nodecheck != null) {
          listNode.add(nodecheck); 
        }
      } else {
        listNode.add(node);
      }
    } else {
      listNode.add(node);
    }
  }

  /**
   * Sets the parent node.
   * 
   * @param node the new parent node
   */
  public void setParentNode(Node node) { this.parentNode = node; }

  /**
   * Gets the parent node.
   * 
   * @return the parent node
   */
  public Node getParentNode() { return parentNode; }

  /**
   * Gets the accepted node types.
   * 
   * @return the accepted node types
   */
  public String[] getAcceptedNodeTypes() { return acceptedNodeTypes; }

  /**
   * Sets the accepted node types.
   * 
   * @param acceptedNodeTypes the new accepted node types
   */
  public void setAcceptedNodeTypes(String[] acceptedNodeTypes) { 
    this.acceptedNodeTypes = acceptedNodeTypes;
  }

  /**
   * Gets the excepted node types.
   * 
   * @return the excepted node types
   */
  public String[] getExceptedNodeTypes() { return exceptedNodeTypes; }

  /**
   * Sets the excepted node types.
   * 
   * @param exceptedNodeTypes the new excepted node types
   */
  public void setExceptedNodeTypes(String[] exceptedNodeTypes) { 
    this.exceptedNodeTypes = exceptedNodeTypes;
  }

  /**
   * Gets the default excepted node types.
   * 
   * @return the default excepted node types
   */
  public String[] getDefaultExceptedNodeTypes() { return defaultExceptedNodeTypes; }

  /**
   * Sets the default excepted node types.
   * 
   * @param defaultExceptedNodeTypes the new default excepted node types
   */
  public void setDefaultExceptedNodeTypes(String[] defaultExceptedNodeTypes) {
    this.defaultExceptedNodeTypes = defaultExceptedNodeTypes;
  }


  /**
   * Gets the accepted mime types.
   * 
   * @return the accepted mime types
   */
  public String[] getAcceptedMimeTypes() { return acceptedMimeTypes; }

  /**
   * Sets the accepted mime types.
   * 
   * @param acceptedMimeTypes the new accepted mime types
   */
  public void setAcceptedMimeTypes(String[] acceptedMimeTypes) { this.acceptedMimeTypes = acceptedMimeTypes; }  

  /**
   * Gets the selectable nodes.
   * 
   * @return the selectable nodes
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public List getSelectableNodes() throws Exception { 
    return uiPageIterator_.getCurrentPageData(); 
  }

  /**
   * Update grid.
   * 
   * @throws Exception the exception
   */
  public void updateGrid() throws Exception {
    ObjectPageList objPageList = new ObjectPageList(getListSelectableNodes(), 4);
    uiPageIterator_.setPageList(objPageList);
  }

  /**
   * Gets the list selectable nodes.
   * 
   * @return the list selectable nodes
   * 
   * @throws Exception the exception
   */
  public List<Node> getListSelectableNodes() throws Exception {
    List<Node> list = new ArrayList<Node>();
    if (parentNode == null) return list;
    Node realNode = Utils.getNodeSymLink(parentNode);
    for (NodeIterator iterator = realNode.getNodes();iterator.hasNext();) {
      Node child = iterator.nextNode();
      if(child.isNodeType("exo:hiddenable")) continue;
      Node symChild= Utils.getNodeSymLink(child);
      if(filterNode(symChild) && isValidState(symChild)) {
        list.add(child);
      }
    }
    List<Node> listNodeCheck = new ArrayList<Node>();
    for (Node node : list) {
      addNodePublish(listNodeCheck, node, publicationService_);
    }
    return listNodeCheck;
  }      

  /**
   * Checks if is valid state.
   * 
   * @param node the node
   * 
   * @return true, if is valid state
   * 
   * @throws Exception the exception
   */
  private boolean isValidState(Node node) throws Exception {
    WCMPublicationService publicationService = getApplicationComponent(WCMPublicationService.class);
    String state = publicationService.getContentState(node);
    if (state==null) return true;
    WCMComposer composer = getApplicationComponent(WCMComposer.class);
    List<String> states = composer.getAllowedStates(WCMComposer.MODE_EDIT);
    return states.contains(state);
  }

  /**
   * Checks if is excepted node type.
   * 
   * @param node the node
   * 
   * @return true, if is excepted node type
   * 
   * @throws RepositoryException the repository exception
   */
  protected boolean isExceptedNodeType(Node node) throws RepositoryException {
    if(defaultExceptedNodeTypes.length > 0) {
      for(String nodeType: defaultExceptedNodeTypes) {
        if((node != null) && node.isNodeType(nodeType)) return true;
      }
    }
    if(exceptedNodeTypes == null || exceptedNodeTypes.length == 0) return false;
    for(String nodeType: exceptedNodeTypes) {
      if((node != null) && node.isNodeType(nodeType)) return true;
    }
    return false;
  }

  private boolean filterNode(Node node) throws RepositoryException{
    if(acceptedNodeTypes == null || acceptedNodeTypes.length == 0) return false;
    NodeType[] superTypes = null;
    if(isWebContent()) {
      superTypes = node.getPrimaryNodeType().getSupertypes();
      for(String nodeType : acceptedNodeTypes) {
        if(node.isNodeType(nodeType)) {
          return true;
        }
        for(NodeType superType : superTypes) {
          if(superType.isNodeType(nodeType)) {
            return true;
          }
        }
      }
    } else if(isDMSDocument()) {
      superTypes = node.getPrimaryNodeType().getSupertypes();
      String[] webContentNodeTypes = UIContentBrowsePanel.WEBCONTENT_NODERTYPE;
      for(String webContentNodeType : webContentNodeTypes) {
        if(node.isNodeType(webContentNodeType)) {
          return false;
        }
        for(NodeType superType : superTypes) {
          if(superType.isNodeType(webContentNodeType)) {
            return false;
          }
        }
      }
      for(String nodeType : acceptedNodeTypes) {
        if(node.isNodeType("nt:file")) {
          return filterDMSDocumentMimeType(node);
        } else if(node.isNodeType(nodeType)) {
          return true;
        }
      }
    } else {
      return this.filterMediaMimetype(node);
    }
    return false;
  }
  
  private boolean filterDMSDocumentMimeType(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
    String mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    String[] mediaMimeTypes = UIContentBrowsePanel.MEDIA_MIMETYPE;
    if(mediaMimeTypes == null || mediaMimeTypes.length == 0) return false;
    for(String type: mediaMimeTypes) {
      if(mimeType.contains(type)){
        return false;
      }
    }
    return true;
  }
  
  private boolean filterMediaMimetype(Node node) throws RepositoryException {
    if(!node.isNodeType("nt:file")) return false;
    if(acceptedMimeTypes == null || acceptedMimeTypes.length == 0) return false;
    String mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    for(String type: acceptedMimeTypes) {
      if(mimeType.contains(type)){
        return true;
      }
    }
    return false;
  }
  
  /**
   * Gets the path taxonomy.
   * 
   * @return the path taxonomy
   * 
   * @throws Exception the exception
   */
  public String getPathTaxonomy() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    return nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
  }
  
  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener<code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SelectActionEvent
   */
  static public class SelectActionListener extends EventListener<UISelectPathPanel> {
    public void execute(Event<UISelectPathPanel> event) throws Exception {
      UISelectPathPanel uiSelectPathPanel = event.getSource();      
      UIContainer uiTreeSelector = uiSelectPathPanel.getParent();
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
	  String[] values = value.split("/");
      value = value.replaceAll(values[values.length - 1], Text.escapeIllegalJcrChars(values[values.length - 1]));
      if(uiTreeSelector instanceof UIOneNodePathSelector) {
        if(!((UIOneNodePathSelector)uiTreeSelector).isDisable()) {
          value = ((UIOneNodePathSelector)uiTreeSelector).getWorkspaceName() + ":" + value ;
        }
      } 

      if(value == null) {
        UIApplication uiApplication = uiSelectPathPanel.getAncestorOfType(UIApplication.class);
        uiApplication.addMessage(new ApplicationMessage("UIDMSSelectorForm.msg.require-choose", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages());
        return;
      }
      RepositoryService repositoryService = uiSelectPathPanel.getApplicationComponent(RepositoryService.class);
      String repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      WCMConfigurationService configurationService = uiSelectPathPanel.getApplicationComponent(WCMConfigurationService.class);
      NodeLocation nodeLocation = configurationService.getLivePortalsLocation(repoName);

      ManageableRepository manageableRepository = repositoryService.getRepository(nodeLocation.getRepository());
      Session session = uiSelectPathPanel.getApplicationComponent(ThreadLocalSessionProviderService.class).getSessionProvider(null)
                                         .getSession(nodeLocation.getWorkspace(), manageableRepository);
      Node webContent = (Node) session.getItem(value);
      UIContentBrowsePanel uiContentBrowsePanel = uiSelectPathPanel.getAncestorOfType(UIContentBrowsePanel.class);
      uiContentBrowsePanel.doSelect(webContent, event.getRequestContext());
    }
  }
}
