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
package org.exoplatform.wcm.webui.selector.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIDropDownControl;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Created by The eXo Platform SARL
 * Author : chungnv
 * nguyenchung136@yahoo.com
 * Jun 23, 2006
 * 10:07:15 AM
 */
@ComponentConfigs({
  @ComponentConfig(
      template = "classpath:groovy/wcm/webui/selector/page/UIPageNodeSelector.gtmpl" ,
      events = {
//        @EventConfig(listeners = CreateNavigationActionListener.class),
        @EventConfig(listeners = UIPageNodeSelector.SelectNavigationActionListener.class, phase=Phase.DECODE) 
      }
  ),
  @ComponentConfig (
      type = UIDropDownControl.class ,
      id = "UIDropDown",
      template = "system:/groovy/portal/webui/navigation/UINavigationSelector.gtmpl",
      events = {
        @EventConfig(listeners = UIPageNodeSelector.SelectNavigationActionListener.class)
      }
    )
})
public class UIPageNodeSelector extends UIContainer {
  
  /** The navigations. */
  private List<PageNavigation> navigations;
  
  /** The selected node. */
  private SelectedNode selectedNode;
  
  /** The copy node. */
  private SelectedNode copyNode;
  
  /** The delete navigations. */
  private List<PageNavigation> deleteNavigations = new ArrayList<PageNavigation>();
  
	/**
	 * Instantiates a new uI page node selector.
	 * 
	 * @throws Exception the exception
	 */
	public UIPageNodeSelector() throws Exception {    
    
    UIDropDownControl uiDopDownControl = addChild(UIDropDownControl.class, "UIDropDown", "UIDropDown");
    uiDopDownControl.setParent(this);
    
    UITree uiTree = addChild(UITree.class, null, "TreePageSelector");    
    uiTree.setIcon("DefaultPageIcon");    
    uiTree.setSelectedIcon("DefaultPageIcon");
    uiTree.setBeanIdField("uri");
    uiTree.setBeanLabelField("resolvedLabel");   
    uiTree.setBeanIconField("icon");
    
    loadNavigations();
	}
	
  /**
   * Load navigations.
   * 
   * @throws Exception the exception
   */
  public void loadNavigations() throws Exception {
    navigations = new ArrayList<PageNavigation>();
    List<PageNavigation> pnavigations = getExistedNavigation(Util.getUIPortalApplication().getNavigations()) ;
    for(PageNavigation nav  : pnavigations){      
      if(nav.isModifiable()) navigations.add(nav);
    }
    
    updateUI() ;
    
    PageNavigation portalSelectedNav = Util.getUIPortal().getSelectedNavigation() ;
    if(getPageNavigation(portalSelectedNav.getId()) != null) {
      selectNavigation(portalSelectedNav.getId()) ;
      PageNode portalSelectedNode = Util.getUIPortal().getSelectedNode() ;
      if(portalSelectedNode != null) selectPageNodeByUri(portalSelectedNode.getUri()) ;  
      return;
    } 
    selectNavigation();
  }
  
  /**
   * Update ui.
   */
  private void updateUI() {
    if(navigations == null || navigations.size() < 1) {
      getChild(UIDropDownControl.class).setOptions(null) ;
      getChild(UITree.class).setSibbling(null) ;
      return ;
    }
    
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    for(PageNavigation navigation: navigations) { //navigation.getOwnerId()
      options.add(new SelectItemOption<String>(navigation.getOwnerType() + ":" + navigation.getOwnerId(), String.valueOf(navigation.getId())));
    }
    UIDropDownControl uiNavigationSelector = getChild(UIDropDownControl.class);
    uiNavigationSelector.setOptions(options);
    if(options.size() > 0) uiNavigationSelector.setValue(0);
  }

  /**
   * Select navigation.
   */
  private void selectNavigation() {
    if(navigations == null || navigations.size() < 1) return;
    if (selectedNode == null) {
      PageNavigation navigation = navigations.get(0);
      selectedNode = new SelectedNode(navigation, null, null);
      if(navigation.getNodes().size() > 0) selectedNode.setNode(navigation.getNodes().get(0));
    }
    selectNavigation(selectedNode.getPageNavigation().getId()) ;
    if(selectedNode.getNode() != null) selectPageNodeByUri(selectedNode.getNode().getUri()) ;
  }
  
  /**
   * Select navigation.
   * 
   * @param id the id
   */
  public void selectNavigation(int id){    
    for(int i = 0; i < navigations.size(); i++){
      if(navigations.get(i).getId() != id) continue ;
      selectedNode = new SelectedNode(navigations.get(i), null, null);
      selectPageNodeByUri(null) ;
      UITree uiTree = getChild(UITree.class);
      uiTree.setSibbling(navigations.get(i).getNodes());      
      UIDropDownControl uiDropDownSelector = getChild(UIDropDownControl.class);
      uiDropDownSelector.setValue(i);
    }
  }
  
  /**
   * Select page node by uri.
   * 
   * @param uri the uri
   */
  public void selectPageNodeByUri(String uri){   
    if(selectedNode == null) return ;
    UITree tree = getChild(UITree.class);
    List<?> sibbling = tree.getSibbling();
    tree.setSibbling(null);
    tree.setParentSelected(null);
    selectedNode.setNode(searchPageNodeByUri(selectedNode.getPageNavigation(), uri));
    if(selectedNode.getNode() != null) {
      tree.setSelected(selectedNode.getNode());   
      tree.setChildren(selectedNode.getNode().getChildren());
      return ;
    }
    tree.setSelected(null);
    tree.setChildren(null);
    tree.setSibbling(sibbling);
  }
  
  /**
   * Search page node by uri.
   * 
   * @param pageNav the page nav
   * @param uri the uri
   * 
   * @return the page node
   */
  public PageNode searchPageNodeByUri(PageNavigation pageNav, String uri) {
    if(pageNav == null || uri == null) return null;
    List<PageNode> pageNodes = pageNav.getNodes();
    UITree uiTree = getChild(UITree.class);
    for(PageNode ele : pageNodes){
      PageNode returnPageNode = searchPageNodeByUri(ele, uri, uiTree);
      if(returnPageNode == null) continue;
      if(uiTree.getSibbling() == null) uiTree.setSibbling(pageNodes);      
      return returnPageNode;
    }
    return null; 
  }  
    
  /**
   * Search page node by uri.
   * 
   * @param pageNode the page node
   * @param uri the uri
   * @param tree the tree
   * 
   * @return the page node
   */
  private PageNode searchPageNodeByUri(PageNode pageNode, String uri, UITree tree){
    if(pageNode.getUri().equals(uri)) return pageNode;
    List<PageNode> children = pageNode.getChildren();
    if(children == null) return null;
    for(PageNode ele : children){
      PageNode returnPageNode = searchPageNodeByUri(ele, uri, tree);
      if(returnPageNode == null) continue;
      if(tree.getSibbling() == null) tree.setSibbling(children);
      if(tree.getParentSelected() == null) tree.setParentSelected(pageNode);
      selectedNode.setParentNode(pageNode);
      return returnPageNode;
    }
    return null;
  }
  
  /**
   * Gets the page navigations.
   * 
   * @return the page navigations
   */
  public List<PageNavigation> getPageNavigations() { 
    if(navigations == null) navigations = new ArrayList<PageNavigation>();    
    return navigations;  
  }

  /**
   * Gets the page navigation.
   * 
   * @param id the id
   * 
   * @return the page navigation
   */
  public PageNavigation getPageNavigation(int id) {
    for(PageNavigation ele : getPageNavigations()) {
      if(ele.getId() == id) return ele ;
    }
    return null ;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    UIRightClickPopupMenu uiPopupMenu = getChild(UIRightClickPopupMenu.class);
    if(uiPopupMenu != null) {
      if(navigations == null || navigations.size() < 1) uiPopupMenu.setRendered(false) ;
      else uiPopupMenu.setRendered(true) ;
    }
    super.processRender(context) ;
  }
  
  /**
   * Gets the copy node.
   * 
   * @return the copy node
   */
  public SelectedNode getCopyNode() { return copyNode; }
  
  /**
   * Sets the copy node.
   * 
   * @param copyNode the new copy node
   */
  public void setCopyNode(SelectedNode copyNode) { this.copyNode = copyNode; }
  
  /**
   * Gets the existed navigation.
   * 
   * @param navis the navis
   * 
   * @return the existed navigation
   * 
   * @throws Exception the exception
   */
  private List<PageNavigation> getExistedNavigation(List<PageNavigation> navis) throws Exception {
    Iterator<PageNavigation> itr = navis.iterator() ;
    UserPortalConfigService configService = getApplicationComponent(UserPortalConfigService.class);
    while(itr.hasNext()) {
      PageNavigation nav = itr.next() ;
      if(configService.getPageNavigation(nav.getOwnerType(), nav.getOwnerId()) == null) itr.remove() ;
    }
    return navis ;
  }
  
  /**
   * The listener interface for receiving selectNavigationAction events.
   * The class that is interested in processing a selectNavigationAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectNavigationActionListener<code> method. When
   * the selectNavigationAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SelectNavigationActionEvent
   */
  static public class SelectNavigationActionListener  extends EventListener<UIDropDownControl> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIDropDownControl> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDropDownControl uiDropDownControl = event.getSource();
      UIPageNodeSelector uiPageNodeSelector = uiDropDownControl.getAncestorOfType(UIPageNodeSelector.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPageNodeSelector.getParent()) ;
      if(id != null) uiPageNodeSelector.selectNavigation(Integer.parseInt(id));
      try{
        UIPageSelector pageSelector = uiPageNodeSelector.getAncestorOfType(UIPageSelector.class);
        UIPageSelectorPanel pageSelectorPanel = pageSelector.getChild(UIPageSelectorPanel.class);
        pageSelectorPanel.setSelectedPage(uiPageNodeSelector.getSelectedNode().getNode());
        pageSelectorPanel.updateGrid();
        
        event.getRequestContext().addUIComponentToUpdateByAjax(pageSelector) ;
      }catch(Exception ex){
        org.exoplatform.wcm.webui.Utils.createPopupMessage(uiPageNodeSelector, "UIMessageBoard.msg.select-navigation", null, ApplicationMessage.ERROR);
      }
      uiPageNodeSelector.<UIComponent>getParent().broadcast(event, event.getExecutionPhase()) ;
      
    }
  }

  /**
   * The Class SelectedNode.
   */
  public static class SelectedNode {
    
    /** The nav. */
    private PageNavigation nav;
    
    /** The parent node. */
    private PageNode parentNode;
    
    /** The node. */
    private PageNode node;
    
    /** The delete node. */
    private boolean deleteNode = false;
    
    /** The clone node. */
    private boolean cloneNode = false;
    
    /**
     * Instantiates a new selected node.
     * 
     * @param nav the nav
     * @param parentNode the parent node
     * @param node the node
     */
    public SelectedNode(PageNavigation nav, PageNode parentNode, PageNode node) {
      this.nav = nav;
      this.parentNode = parentNode;
      this.node = node;
    }

    /**
     * Gets the page navigation.
     * 
     * @return the page navigation
     */
    public PageNavigation getPageNavigation() { return nav; }
    
    /**
     * Sets the page navigation.
     * 
     * @param nav the new page navigation
     */
    public void setPageNavigation(PageNavigation nav) { this.nav = nav; }

    /**
     * Gets the parent node.
     * 
     * @return the parent node
     */
    public PageNode getParentNode() { return parentNode; }
    
    /**
     * Sets the parent node.
     * 
     * @param parentNode the new parent node
     */
    public void setParentNode(PageNode parentNode) { this.parentNode = parentNode; }

    /**
     * Gets the node.
     * 
     * @return the node
     */
    public PageNode getNode() { return node; }
    
    /**
     * Sets the node.
     * 
     * @param node the new node
     */
    public void setNode(PageNode node) { this.node = node; }

    /**
     * Checks if is delete node.
     * 
     * @return true, if is delete node
     */
    public boolean isDeleteNode() { return deleteNode; }
    
    /**
     * Sets the delete node.
     * 
     * @param deleteNode the new delete node
     */
    public void setDeleteNode(boolean deleteNode) { this.deleteNode = deleteNode; }
    
    /**
     * Checks if is clone node.
     * 
     * @return true, if is clone node
     */
    public boolean isCloneNode() { return cloneNode; }
    
    /**
     * Sets the clone node.
     * 
     * @param b the new clone node
     */
    public void setCloneNode(boolean b) { cloneNode = b; }
  }

  /**
   * Gets the selected node.
   * 
   * @return the selected node
   */
  public SelectedNode getSelectedNode() { return selectedNode; }
  
  /**
   * Gets the selected navigation.
   * 
   * @return the selected navigation
   */
  public PageNavigation getSelectedNavigation(){ 
    return selectedNode == null ? null : selectedNode.getPageNavigation(); 
  }  
  
  /**
   * Gets the selected page node.
   * 
   * @return the selected page node
   */
  public PageNode getSelectedPageNode() { 
    return selectedNode == null ? null : selectedNode.getNode() ; 
  }
  
  /**
   * Gets the up level uri.
   * 
   * @return the up level uri
   */
  public String getUpLevelUri () { return selectedNode.getParentNode().getUri() ; }

  /**
   * Gets the delete navigations.
   * 
   * @return the delete navigations
   */
  public List<PageNavigation> getDeleteNavigations() { return deleteNavigations; }
}
