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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIEditingTagsForm;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.paginator.UICustomizeablePaginator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 26, 2007 4:59:40 PM
 */

@ComponentConfigs( {
@ComponentConfig(template = "app:/groovy/webui/component/explorer/sidebar/UITagExplorer.gtmpl",
                 events = {
    @EventConfig(listeners = UITagExplorer.ViewTagActionListener.class),
    @EventConfig(listeners = UITagExplorer.EditTagsActionListener.class) }),
@ComponentConfig(type = UIPageIterator.class,
                 template = "app:/groovy/webui/component/explorer/sidebar/UITagPageIterator.gtmpl",
                 events = {@EventConfig(listeners = UIPageIterator.ShowPageActionListener.class)})
})
public class UITagExplorer extends UIContainer {

  public static final String PUBLIC_TAG_NODE_PATH = "exoPublicTagNode";
  private static final String PRIVATE_TAG_PAGE_ITERATOR_ID           = "PrivateTagPageIterator";
  private static final String PUBLIC_TAG_PAGE_ITERATOR_ID           = "PublicTagPageIterator";
  private static final int TAG_PAGE_SIZE = 30;
  private int tagScope;
  private UIPageIterator        privateTagPageIterator_;
  private UIPageIterator        publicTagPageIterator_;
  private NewFolksonomyService folksonomyService;

  public UITagExplorer() throws Exception {
    privateTagPageIterator_ = addChild(UIPageIterator.class, null, PRIVATE_TAG_PAGE_ITERATOR_ID);
    publicTagPageIterator_ = addChild(UIPageIterator.class, null, PUBLIC_TAG_PAGE_ITERATOR_ID);
    folksonomyService = getApplicationComponent(NewFolksonomyService.class);
  }

  public int getTagScope() { return tagScope; }
  public void setTagScope(int scope) { tagScope = scope; }

  public List<Node> getPrivateTagLink() throws Exception {
    return NodeLocation.getNodeListByLocationList(privateTagPageIterator_.getCurrentPageData());
  }

  public List<Node> getPublicTagLink() throws Exception {
    return NodeLocation.getNodeListByLocationList(publicTagPageIterator_.getCurrentPageData());
  }

  public Map<String ,String> getTagStyle() throws Exception {
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    String workspace = getApplicationComponent(DMSConfiguration.class).getConfig().getSystemWorkspace();
    Map<String , String> tagStyle = new HashMap<String ,String>() ;
    for(Node tag : folksonomyService.getAllTagStyle(workspace)) {
      tagStyle.put(tag.getProperty("exo:styleRange").getValue().getString(),
                   tag.getProperty("exo:htmlStyle").getValue().getString());
    }
    return tagStyle ;
  }

  public String getTagHtmlStyle(Map<String, String> tagStyles, int tagCount) throws Exception {
    for (Entry<String, String> entry : tagStyles.entrySet()) {
      if (checkTagRate(tagCount, entry.getKey()))
        return entry.getValue();
    }
    return "";
  }
  
  /**
   * updates the private tag list and public tag list
   * @throws Exception
   */
  public void updateTagList() throws Exception {
    //update private tag list
    ListAccess<NodeLocation> privateTagList = new ListAccessImpl<NodeLocation>(NodeLocation.class,
                      NodeLocation.getLocationsByNodeList(folksonomyService.getAllPrivateTags(getUserName())));
    LazyPageList<NodeLocation> privatePageList = new LazyPageList<NodeLocation>(privateTagList, TAG_PAGE_SIZE);
    privateTagPageIterator_.setPageList(privatePageList);
    
    //update public tag list
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);

    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
    String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();

    ListAccess<NodeLocation> publicTagList = new ListAccessImpl<NodeLocation>(NodeLocation.class,
                      NodeLocation.getLocationsByNodeList(folksonomyService.getAllPublicTags(publicTagNodePath, workspace)));
    LazyPageList<NodeLocation> publicPageList = new LazyPageList<NodeLocation>(publicTagList, TAG_PAGE_SIZE);
    publicTagPageIterator_.setPageList(publicPageList);   
  }

  /**
   * gets public tag page iterator
   */
  public UIPageIterator getPublicPageIterator() {
    return publicTagPageIterator_;
  }
  
  /**
   * gets private tag page iterator
   */
  public UIPageIterator getPrivatePageIterator() {
    return privateTagPageIterator_;
  }
  private boolean checkTagRate(int numOfDocument, String range) throws Exception {
    String[] vals = StringUtils.split(range ,"..") ;
    int minValue = Integer.parseInt(vals[0]) ;
    int maxValue ;
    if(vals[1].equals("*")) {
      maxValue = Integer.MAX_VALUE ;
    }else {
      maxValue = Integer.parseInt(vals[1]) ;
    }
    if(minValue <=numOfDocument && numOfDocument <maxValue ) return true ;
    return false ;
  }

  public String getRepository() { return getAncestorOfType(UIJCRExplorer.class).getRepositoryName();}
  public String getWorkspace() { return getAncestorOfType(UIJCRExplorer.class).getCurrentWorkspace();}
  public String getUserName() {
    try {
      return getAncestorOfType(UIJCRExplorer.class).getSession().getUserID();
    } catch (Exception ex) {
      return "";
    }
  }

  static public class ViewTagActionListener extends EventListener<UITagExplorer> {
    public void execute(Event<UITagExplorer> event) throws Exception {
      UITagExplorer uiTagExplorer = event.getSource() ;
      UIApplication uiApp = uiTagExplorer.getAncestorOfType(UIApplication.class);
      String tagPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIJCRExplorer uiExplorer = uiTagExplorer.getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.setSelectRootNode();
      uiExplorer.setTagPath(tagPath);
      uiExplorer.setIsViewTag(true);
      try {
        uiExplorer.updateAjax(event);
      } catch(PathNotFoundException pne) {
      	uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.path-not-found", null, ApplicationMessage.WARNING)) ;
      	return;
      }
    }
  }

  static public class EditTagsActionListener extends EventListener<UITagExplorer> {
    public void execute(Event<UITagExplorer> event) throws Exception {
      UITagExplorer uiTagExplorer = event.getSource();
      NewFolksonomyService newFolksonomyService = uiTagExplorer.getApplicationComponent(NewFolksonomyService.class);
      String scope = event.getRequestContext().getRequestParameter(OBJECTID);
      int intScope = Utils.PUBLIC.equals(scope) ? NewFolksonomyService.PUBLIC
                                               : NewFolksonomyService.PRIVATE;
      uiTagExplorer.getAncestorOfType(UIJCRExplorer.class).setTagScope(intScope);

      List<String> memberships = Utils.getMemberships();
      if (newFolksonomyService.canEditTag(intScope, memberships)) {
        UIJCRExplorer uiExplorer = uiTagExplorer.getAncestorOfType(UIJCRExplorer.class);
        UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
        uiPopupContainer.activate(UIEditingTagsForm.class, 600);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      } else {
        UIApplication uiApp = uiTagExplorer.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.editTagAccessDenied",
                                                null,
                                                ApplicationMessage.WARNING));
        
        uiTagExplorer.getAncestorOfType(UIJCRExplorer.class).updateAjax(event);
      }
    }
  }
}
