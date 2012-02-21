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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.lock.LockException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : Nguyen Anh Vu anhvurz90@gmail.com
 * Oct 15, 2009 2:12:00 PM
 */

@ComponentConfig(template = "app:/groovy/webui/component/explorer/search/UIShowAllFavouriteResult.gtmpl",
                 events = {
    @EventConfig(listeners = UIShowAllFavouriteResult.ViewActionListener.class),
    @EventConfig(listeners = UIShowAllFavouriteResult.OpenFolderActionListener.class),
    @EventConfig(listeners = UIShowAllFavouriteResult.SortASCActionListener.class),
    @EventConfig(listeners = UIShowAllFavouriteResult.SortDESCActionListener.class),
    @EventConfig(listeners = UIShowAllFavouriteResult.StarClickActionListener.class) })
public class UIShowAllFavouriteResult extends UIComponentDecorator {

  public static final int SHOW_ALL_FAVOURITE = 0;
  public static final int SHOW_ALL_FAVOURITE_BY_USER = 1;

  private static final Log LOG  = ExoLogger.getLogger("explorer.search.UIShowAllFavouriteResult");
  private static final int FILE_PER_PAGE = 10;
  private static String iconType = "BlueDownArrow";
  private static String iconName = "";
  private long searchTime_ = 0;
  private List<Node> favouriteNodes_ = new ArrayList<Node>();
  private Map<Integer, Node> mapIndexNode = new Hashtable<Integer, Node>();
  private UIPageIterator uiPageIterator_ ;
  private boolean favouriteChange = false;
  private int showNodeCase;
  private static FavoriteService favoriteService_ = null;

  static {
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    favoriteService_ =
      (FavoriteService) myContainer
      .getComponentInstanceOfType(FavoriteService.class);
  }

  public UIShowAllFavouriteResult() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "UIShowAllFavouriteIterator");
    setUIComponent(uiPageIterator_);
  }

  public FavoriteService getFavouriteService() {
    return favoriteService_;
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
  }

  public void setShowNodeCase(int value) {
    this.showNodeCase = value;
  }

  public long getSearchTime() { return searchTime_; }
  public void setSearchTime(long time) { this.searchTime_ = time; }
  public UIPageIterator  getUIPageIterator() {  return uiPageIterator_ ; }
  public Map<Integer, Node> getMapIndexNode() { return mapIndexNode; }

  public Date getDateCreated(Node node) throws Exception{
      if (node.hasProperty("exo:dateCreated")) {
        return node.getProperty("exo:dateCreated").getDate().getTime();
      }
      return new GregorianCalendar().getTime();
  }

  @SuppressWarnings("unchecked")
  public List<Node> getAllFavouriteNodes() throws Exception {
    //return favouriteNodes_;
    return (List<Node>)uiPageIterator_.getCurrentPageData();
  }

  protected List<Node> getNewFavouriteNodeList() throws Exception {
    List<Node> ret = new ArrayList<Node>();

    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    ret = favoriteService_.getAllFavoriteNodesByUser(
          uiExplorer.getCurrentWorkspace(),
          uiExplorer.getRepositoryName(),
          uiExplorer.getSession().getUserID());

    return ret;
  }

  public void updateList() throws Exception {
    List<Node> nodeList = getNewFavouriteNodeList();
    ListAccess<Node> favorNodeList = new ListAccessImpl<Node>(Node.class, nodeList);
    LazyPageList<Node> pageList = new LazyPageList<Node>(favorNodeList, FILE_PER_PAGE);
    uiPageIterator_.setPageList(pageList);

    favouriteNodes_ = nodeList;

    favouriteChange = false;
  }

  public static class SortASCActionListener extends
      EventListener<UIShowAllFavouriteResult> {
    @Override
    public void execute(Event<UIShowAllFavouriteResult> event) throws Exception {
      UIShowAllFavouriteResult uiShowAllFavouriteResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueDownArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueDownArrow";
      }
      int currentPage = uiShowAllFavouriteResult.getUIPageIterator().getCurrentPage();
      if (uiShowAllFavouriteResult.favouriteChange) {
        uiShowAllFavouriteResult.updateList();
        uiShowAllFavouriteResult.favouriteChange = false;
      }
      Collections.sort(uiShowAllFavouriteResult.favouriteNodes_, new SearchComparator());
      ListAccess<Node> favorNodeList = new ListAccessImpl<Node>(Node.class,
                                                                uiShowAllFavouriteResult.favouriteNodes_);
      LazyPageList<Node> pageList = new LazyPageList<Node>(favorNodeList, FILE_PER_PAGE);

      UIPageIterator uiPageIterator = uiShowAllFavouriteResult.uiPageIterator_;
      uiPageIterator.setPageList(pageList);
      if (uiPageIterator.getAvailablePage() >= currentPage)
        uiPageIterator.setCurrentPage(currentPage);
      else {
        uiPageIterator.setCurrentPage(
          uiPageIterator.getAvailablePage());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiShowAllFavouriteResult.getParent());
    }
  }

  public static class SortDESCActionListener extends
      EventListener<UIShowAllFavouriteResult> {
    @Override
    public void execute(Event<UIShowAllFavouriteResult> event) throws Exception {
      UIShowAllFavouriteResult uiShowAllFavouriteResult
          = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectId.equals("type")) {
        iconType = "BlueUpArrow";
        iconName = "";
      } else if (objectId.equals("name")) {
        iconType = "";
        iconName = "BlueUpArrow";
      }
      int currentPage = uiShowAllFavouriteResult.getUIPageIterator().getCurrentPage();
      if (uiShowAllFavouriteResult.favouriteChange) {
        uiShowAllFavouriteResult.updateList();
        uiShowAllFavouriteResult.favouriteChange = false;
      }

      Collections.sort(uiShowAllFavouriteResult.favouriteNodes_, new SearchComparator());
      ListAccess<Node> favorNodeList = new ListAccessImpl<Node>(Node.class,
                                                                uiShowAllFavouriteResult.favouriteNodes_);
      LazyPageList<Node> pageList = new LazyPageList<Node>(favorNodeList, FILE_PER_PAGE);

      UIPageIterator uiPageIterator = uiShowAllFavouriteResult.uiPageIterator_;
      uiPageIterator.setPageList(pageList);
      uiPageIterator.setPageList(pageList);
      if (uiPageIterator.getAvailablePage() >= currentPage)
        uiPageIterator.setCurrentPage(currentPage);
      else {
        uiPageIterator.setCurrentPage(
          uiPageIterator.getAvailablePage());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiShowAllFavouriteResult.getParent());
    }
  }

  public static class OpenFolderActionListener extends
      EventListener<UIShowAllFavouriteResult> {
    @Override
    public void execute(Event<UIShowAllFavouriteResult> event) throws Exception {
      UIShowAllFavouriteResult uiShowAllFavouriteResult
          = event.getSource();
      UIJCRExplorer uiExplorer = uiShowAllFavouriteResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      String folderPath = LinkUtils.getParentPath(path);
      Node node = null;
      try {
            node = uiExplorer.getNodeByPath(folderPath, uiExplorer.getTargetSession());
        } catch(AccessDeniedException ace) {
          UIApplication uiApp = uiShowAllFavouriteResult.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIShowAllFavouriteResult.msg.access-denied", null,
                              ApplicationMessage.WARNING));
          
          return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Cannot access the node at " + folderPath, e);
        }
      }
      uiExplorer.setSelectNode(node.getSession().getWorkspace().getName(), folderPath);
      uiExplorer.updateAjax(event);
    }
  }

  static  public class ViewActionListener extends EventListener<UIShowAllFavouriteResult> {
      public void execute(Event<UIShowAllFavouriteResult> event) throws Exception {
        UIShowAllFavouriteResult uiShowAllFavouriteResult = event.getSource();
        UIJCRExplorer uiExplorer = uiShowAllFavouriteResult.getAncestorOfType(UIJCRExplorer.class);
        String path = event.getRequestContext().getRequestParameter(OBJECTID);
        UIApplication uiApp = uiShowAllFavouriteResult.getAncestorOfType(UIApplication.class);
        Node node;
        try {
          node = uiExplorer.getNodeByPath(path, uiExplorer.getTargetSession());
        } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIShowAllFavouriteResult.msg.access-denied", null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        TemplateService templateService = uiShowAllFavouriteResult.getApplicationComponent(TemplateService.class);
        if (!templateService.isManagedNodeType(node.getPrimaryNodeType().getName())) {
          uiApp.addMessage(new ApplicationMessage("UIShowAllFavouriteResult.msg.not-support", null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        UIPopupWindow uiPopup = uiExplorer.getChildById("ViewSearch");
        if (uiPopup == null) {
          uiPopup = uiExplorer.addChild(UIPopupWindow.class, null, "ViewSearch");
        }
        uiPopup.setResizable(true);
        uiPopup.setShowMask(true);
        UIViewSearchResult uiViewSearch = uiPopup.createUIComponent(UIViewSearchResult.class, null, null);
        uiViewSearch.setNode(node);

        uiPopup.setWindowSize(600,750);
        uiPopup.setUIComponent(uiViewSearch);
        uiPopup.setRendered(true);
        uiPopup.setShow(true);
        return;
      }
  }

  public static class StarClickActionListener extends EventListener<UIShowAllFavouriteResult> {
      public void execute(Event<UIShowAllFavouriteResult> event) throws Exception {
        String index = event.getRequestContext().getRequestParameter(OBJECTID);
        int id = Integer.parseInt(index);
        UIShowAllFavouriteResult uiShow = event.getSource();
        UIJCRExplorer uiExplorer = uiShow.getAncestorOfType(UIJCRExplorer.class);
        UIApplication uiApp = uiShow.getAncestorOfType(UIApplication.class);
        Node node = uiShow.mapIndexNode.get(id);

        try {
          uiExplorer.addLockToken(node);
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          return;
        }

        try {
          if (favoriteService_.isFavoriter(node.getSession().getUserID(), node)) {
            if (PermissionUtil.canRemoveNode(node)) {
              favoriteService_.removeFavorite(node, node.getSession().getUserID());
              uiShow.favouriteChange = true;
            }
            else {
              throw new AccessDeniedException();
            }
          } else {
            if (PermissionUtil.canSetProperty(node)) {
              favoriteService_.addFavorite(node, node.getSession().getUserID());
              uiShow.favouriteChange = true;
            }
            else {
              throw new AccessDeniedException();
            }
          }
          //uiStar.changeFavourite();
            event.getRequestContext().addUIComponentToUpdateByAjax(uiShow);
        } catch (LockException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("node is locked, can't remove change favourite property of node :" + node.getPath());
          }
          JCRExceptionManager.process(uiApp, e);
          
          uiExplorer.updateAjax(event);
        } catch (AccessDeniedException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Access denied! No permission for modifying property " +
                Utils.EXO_FAVOURITER + " of node: " + node.getPath());
          }
          uiApp.addMessage(new ApplicationMessage("UIShowAllFavouriteResult.msg.accessDenied", null, ApplicationMessage.WARNING));
          
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs!");
          }
          JCRExceptionManager.process(uiApp, e);
            
        }
      }
  }


  private static class SearchComparator implements Comparator<Node> {
      public int compare(Node node1, Node node2) {
        try {
          if (iconType.equals("BlueUpArrow") || iconType.equals("BlueDownArrow")) {
            String s1 = node1.getProperty("jcr:primaryType").getString();
            String s2 = node2.getProperty("jcr:primaryType").getString();
            if (iconType.trim().equals("BlueUpArrow")) { return s2.compareTo(s1); }
            return s1.compareTo(s2);
          } else if (iconName.equals("BlueUpArrow") || iconName.equals("BlueDownArrow")) {
            String name1 = node1.getName();
            String name2 = node2.getName();
            if (iconName.trim().equals("BlueUpArrow")) { return name2.compareTo(name1); }
            return name1.compareTo(name2);
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Cannot compare nodes", e);
          }
        }
        return 0;
      }
  }

}
