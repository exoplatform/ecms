/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.admin.unlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.commons.exception.ExoMessageException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 23, 2012  
 */
public class UILockedNodePageList extends PageList<NodeLocation> {
  
  private static final Log LOG  = ExoLogger.getLogger(UILockedNodePageList.class);

  private static final int QUERY_SIZE = 200;
  
  private String query;
  private Map<String, Integer> workspaceNodeMap;
  private String[] workspaceNameList;
  private List<NodeLocation> buffer;

  public UILockedNodePageList(int pageSize) {
    super(pageSize);
  }
  
  public UILockedNodePageList(String query, int pageSize, int currentPage) {
    super(pageSize);
    this.query = query;
    workspaceNameList = getWorkSpaceNameList();
    workspaceNodeMap = new HashMap<String, Integer>();
    buffer = this.getData(0, Math.max(QUERY_SIZE, (currentPage + 10) * pageSize));
    this.setAvailablePage(buffer.size());
  }

  /**
   * gets list of workspace name in repository
   * @return
   */
  private String[] getWorkSpaceNameList() {
    RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
    String[] ret = new String[]{};
    try {
      ret = repoService.getCurrentRepository().getWorkspaceNames();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return ret;
  }

  @Override
  protected void populateCurrentPage(int page) throws Exception {
    currentListPage_ = buffer.subList(getFrom(), getTo());
  }

  @Override
  protected void checkAndSetPage(int page) throws Exception
  {
    if (page + 10 > availablePage_) {
      buffer.addAll(getData(buffer.size(), (page + 10) * getPageSize() - availablePage_ + QUERY_SIZE));
      setAvailablePage(buffer.size());
    }
    if (page < 1 || page > availablePage_) {
      Object[] args = {Integer.toString(page), Integer.toString(availablePage_)};
      throw new ExoMessageException("PageList.page-out-of-range", args);
    }
    currentPage_ = page;
  }

  private List<NodeLocation> getData(int from, int count) {
//    int count = this.getTo() - this.getFrom();//number of node need to query
//    int delta = this.getFrom();
    int delta = from;
    List<NodeLocation> ret = new ArrayList<NodeLocation>();//data will be filled to this list
    //iterate through all workspaces
    for (String workspace : workspaceNameList) {
      Integer lockedNodeCount = workspaceNodeMap.get(workspace);
      if (lockedNodeCount != null) {
        if (lockedNodeCount <= delta) {//(1.1)
          delta -= lockedNodeCount;
        } else if (delta < lockedNodeCount && lockedNodeCount <= delta + count) { //(1.2)
          ret.addAll(queryNodes(workspace, delta, lockedNodeCount - delta));
          delta = 0;
          count -= (lockedNodeCount - delta);
        } else if (delta + count < lockedNodeCount) {
          ret = queryNodes(workspace, delta, count);
          break;//delta = 0; count = 0;
        }
      } else { //lockedNodeCount==null
        List<NodeLocation> queryNodeData = queryNodes(workspace, delta, count);
        if (queryNodeData.size() == 0) {//(2.1), as (1.1) : lockedNodeCount <= delta
          lockedNodeCount = queryNodes(workspace, 0, count).size();
          delta -= lockedNodeCount;
          workspaceNodeMap.put(workspace, lockedNodeCount);
        } else if (0 < queryNodeData.size() && queryNodeData.size() < count) {
          //(2.2), as (1.2) : delta < lockedNodeCount && lockedNodeCount <= delta + count
          ret.addAll(queryNodeData);
          delta = 0;
          count -= queryNodeData.size();
        } else if (queryNodeData.size() == count) {//(2.3), as (1.3) : delta + count < lockedNodeCount
          ret = queryNodeData;
          break;//delta = 0; count = 0;
        }
      }
      if (count == 0) {
        break;
      }
    }
    return ret;
  }

  /**
   * gets all nodes by this.query, in given workspace, from offset to (offset + limit)
   * @throws Exception
   */
  private List<NodeLocation> queryNodes(String workspace, int offset, int limit) {
    List<NodeLocation> ret = new ArrayList<NodeLocation>();
    try {
      ManageableRepository repo = WCMCoreUtils.getService(RepositoryService.class).getCurrentRepository();
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, 
                                                                           repo);
      Query query = session.getWorkspace().getQueryManager().createQuery(this.query, Query.SQL);
      ((QueryImpl)query).setOffset(offset);
      ((QueryImpl)query).setLimit(limit);
      
      for (NodeIterator iter = query.execute().getNodes(); iter.hasNext();) {
        Node node = iter.nextNode();
        if (node.isLocked()) {
          ret.add(NodeLocation.getNodeLocationByNode(node));
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    
    return ret;
  }
  @Override
  public List getAll() throws Exception {
    return buffer;
  }
  
}
