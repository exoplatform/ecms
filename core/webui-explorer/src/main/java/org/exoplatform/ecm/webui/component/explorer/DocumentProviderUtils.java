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
package org.exoplatform.ecm.webui.component.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.portlet.PortletPreferences;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 9, 2009
 * 1:48:20 PM
 */
public class DocumentProviderUtils {

  public static final int CURRENT_NODE_ITEMS = 0;
  public static final int FAVOURITE_ITEMS = 1;
  public static final int TRASH_ITEMS = 2;
  public static final int OWNED_BY_USER_ITEMS = 3;
  public static final int HIDDEN_ITEMS = 4;

  private static FavoriteService favoriteService_ = null;
  private static TrashService trashService_ = null;

  static {
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    favoriteService_ =
      (FavoriteService) myContainer
      .getComponentInstanceOfType(FavoriteService.class);
    trashService_ =
      (TrashService) myContainer
      .getComponentInstanceOfType(TrashService.class);
  }

  public List<Node> getItemsBySourceType(int source, UIJCRExplorer uiExplorer) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    switch (source) {
    case CURRENT_NODE_ITEMS:
      return getCurrentNodeChildren(uiExplorer);
    case FAVOURITE_ITEMS:
      return getFavouriteNodeList(uiExplorer);
    case TRASH_ITEMS:
      return getTrashNodeList(uiExplorer);
    case OWNED_BY_USER_ITEMS:
      return getOwnedByUserNodeList(uiExplorer);
    case HIDDEN_ITEMS:
      return getHiddenNodeList(uiExplorer);
    }
    return ret;
  }

  private List<Node> getCurrentNodeChildren(UIJCRExplorer uiExplorer) throws Exception {
    List<Node> childrenList = new ArrayList<Node>();

    Preference pref = uiExplorer.getPreference();
    String currentPath = uiExplorer.getCurrentPath();
    if(!uiExplorer.isViewTag()) {
      childrenList = uiExplorer.getChildrenList(currentPath, pref.isShowPreferenceDocuments());
    } else  {
      childrenList = uiExplorer.getDocumentByTag();
    }

    return childrenList;
  }

  private List<Node> getTrashNodeList(UIJCRExplorer uiExplorer) throws Exception {
    List<Node> ret = new ArrayList<Node>();

    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    //String trashHomeNodePath = portletPref.getValue(Utils.TRASH_HOME_NODE_PATH, "");
    String trashWorkspace = portletPref.getValue(Utils.TRASH_WORKSPACE, "");
    SessionProvider sessionProvider = uiExplorer.getSessionProvider();
    boolean byUser = uiExplorer.getPreference().isShowItemsByUser();
    if (!byUser) {
      ret = trashService_.getAllNodeInTrash(
          trashWorkspace,
          sessionProvider);
    } else {
      ret = trashService_.getAllNodeInTrashByUser(
          trashWorkspace,
          sessionProvider,
          uiExplorer.getSession().getUserID());
    }
    return ret;
  }

  private List<Node> getHiddenNodeList(UIJCRExplorer uiExplorer) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    boolean byUser = uiExplorer.getPreference().isShowItemsByUser();

    StringBuilder queryString = new StringBuilder("SELECT * FROM " + Utils.EXO_HIDDENABLE);

    if (byUser) {
      queryString.append(" WHERE CONTAINS(").
                  append(Utils.EXO_OWNER).
                  append(",'").
                  append(uiExplorer.getSession().getUserID()).
                  append("')");
    }
    Session session = uiExplorer.getSession();
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString.toString(), Query.SQL);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if (Utils.isInTrash(node)) continue;
      ret.add(node);
    }
    return ret;
  }

  private List<Node> getFavouriteNodeList(UIJCRExplorer uiExplorer) throws Exception {
//    boolean byUser = uiExplorer.getPreference().isShowItemsByUser();
    List<Node> ret = new ArrayList<Node>();
    List<Node> favoriteList = null;

    favoriteList = favoriteService_.getAllFavoriteNodesByUser(uiExplorer.getCurrentWorkspace(),
          uiExplorer.getRepositoryName(), uiExplorer.getSession().getUserID());

    for (Node node : favoriteList) {
      if (!Utils.isInTrash(node))
        ret.add(node);
    }

    return ret;
  }

  private List<Node> getOwnedByUserNodeList(UIJCRExplorer uiExplorer) throws Exception {
    List<Node> ret = new ArrayList<Node>();

    Session session = uiExplorer.getSession();
    StringBuilder queryString = new StringBuilder("SELECT * FROM ").append(Utils.NT_BASE);

    queryString.append(" WHERE CONTAINS(")
               .append(Utils.EXO_OWNER)
               .append(",'")
               .append(uiExplorer.getSession().getUserID())
               .append("')");

    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString.toString(), Query.SQL);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    Node node = null;
    Set<Node> set = new TreeSet<Node>(new NodeComparator());
    while (iter.hasNext()) {
      node = iter.nextNode();
      if (node.getName().equals(Utils.JCR_CONTENT))
        continue;
      if (Utils.isInTrash(node))
        continue;
      if (node.isNodeType(Utils.NT_RESOURCE))
        node = node.getParent();
      if (!set.contains(node)) {
        ret.add(node);
        set.add(node);
      }
    }
    return ret;
  }

  private static class NodeComparator implements Comparator<Node> {
    public int compare(Node o1, Node o2) {
      try {
        if (o1.isSame(o2))
          return 0;
        int pathComparison = o1.getPath().compareTo(o2.getPath());
        return (pathComparison == 0) ? 1 : pathComparison;
      } catch (RepositoryException e) {
        return 1;
      }
    }
  }

}
