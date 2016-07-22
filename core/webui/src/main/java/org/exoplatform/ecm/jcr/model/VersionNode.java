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
package org.exoplatform.ecm.jcr.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class VersionNode {

  private boolean isExpanded = true ;
//  protected Version version_ ;
  private List<VersionNode> children_ = new ArrayList<VersionNode>() ;
  private static final Log LOG  = ExoLogger.getLogger(VersionNode.class.getName());
  private Calendar createdTime_;
  private String name_ = "";
  private String path_ = "";
  private String ws_ = "";
  private String uuid_;
  private String[] versionLabels_ = new String[]{};
  private String author_;
  
  public VersionNode(Version version, Session session) {
    try {
      try {
        createdTime_ = version.getCreated();
        name_ = version.getName();
        path_ = version.getPath();
        ws_ = version.getSession().getWorkspace().getName();
        uuid_ = version.getUUID();
        author_ = version.getNode("jcr:frozenNode").getProperty(Utils.EXO_LASTMODIFIER).getString();
        if (version.isNodeType(NodetypeConstant.MIX_VERSIONABLE)) {
          versionLabels_ = version.getVersionHistory().getVersionLabels(version);
        }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      
      String uuid = version.getUUID();
      QueryManager queryManager = version.getSession().getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("//element(*, nt:version)[@jcr:predecessors='" + uuid + "']", Query.XPATH);
      QueryResult queryResult = query.execute();
      NodeIterator iterate = queryResult.getNodes();
      while (iterate.hasNext()) {
        Version version1 = (Version) iterate.nextNode();
        children_.add(new VersionNode(version1, session));
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }
  
  public VersionNode(Version version) throws RepositoryException {
    try {
      createdTime_ = version.getCreated();
      name_ = version.getName();
      path_ = version.getPath();
      ws_ = version.getSession().getWorkspace().getName();
      uuid_ = version.getUUID();
      author_ = version.getNode("jcr:frozenNode").getProperty(org.exoplatform.ecm.webui.utils.Utils.EXO_LASTMODIFIER).getString();
      versionLabels_ = version.getVersionHistory().getVersionLabels(version);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    
    try {
      Version[] versions = version.getSuccessors() ;
      if(versions == null || versions.length == 0) isExpanded = false;
      for (Version versionChild : versions) {
        children_.add(new VersionNode(versionChild)) ;
      }
    } catch (PathNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
  }

  public boolean isExpanded() { return isExpanded ; }

  public void setExpanded(boolean isExpanded) { this.isExpanded = isExpanded ; }

  public String getName() throws RepositoryException { return name_; }
  
  public String getWs() { return ws_; }

  public String getPath() throws RepositoryException { return path_; }

  public int getChildrenSize() { return children_.size() ; }

  public List<VersionNode> getChildren() { return children_; }

  public Calendar getCreatedTime() { return createdTime_; }

  public String getAuthor() {
    return author_;
  }

  public String[] getVersionLabels() {
    return versionLabels_;
  }
  
  public Node getNode(String nodeName) throws Exception {
    DMSConfiguration dmsConf = WCMCoreUtils.getService(DMSConfiguration.class);
    String systemWS = dmsConf.getConfig().getSystemWorkspace();
    ManageableRepository repo = WCMCoreUtils.getRepository(); 
    SessionProvider provider = systemWS.equals(ws_) ? WCMCoreUtils.getSystemSessionProvider() :
                                                     WCMCoreUtils.getUserSessionProvider();
    return ((Node)provider.getSession(ws_, repo).getItem(path_)).getNode(nodeName);
  }
  
  public String getUUID() { return uuid_; }
  
  public VersionNode findVersionNode(String path) throws RepositoryException {
    if(path_.equals(path)) return this ;
    VersionNode node = null ;
    Iterator iter = children_.iterator() ;
    while (iter.hasNext()) {
      VersionNode child = (VersionNode) iter.next();
      node = child.findVersionNode(path);
      if(node != null) return node;
    }
    return null;
  }

  public void removeVersionInChild(VersionNode versionNode1, VersionNode versionNodeRemove) throws RepositoryException {
    if (versionNode1.getChildren().contains(versionNodeRemove)) versionNode1.getChildren().remove(versionNodeRemove);
    else {
      for (VersionNode vsN : versionNode1.getChildren()) {
        removeVersionInChild(vsN, versionNodeRemove);
      }
    }
  }
  
}
