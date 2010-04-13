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
import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

public class VersionNode {

  private boolean isExpanded = true ;
  private Version version_ ;
  private List<VersionNode> children_ = new ArrayList<VersionNode>() ;
  private static final Log LOG  = ExoLogger.getLogger("model.VersionNode");
  public VersionNode(Version version, Session session) {
    version_ = version;
    try {      
      String uuid = version.getUUID();
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("//element(*, nt:version)[@jcr:predecessors='" + uuid + "']", Query.XPATH);
      QueryResult queryResult = query.execute();
      NodeIterator iterate = queryResult.getNodes();
      while (iterate.hasNext()) {
        Version version1 = (Version) iterate.nextNode();
        children_.add(new VersionNode(version1, session));
      }
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }
  }

  public boolean isExpanded() { return isExpanded ; }
  
  public void setExpanded(boolean isExpanded) { this.isExpanded = isExpanded ; }
  
  public Version getVersion() { return version_; }
  
  public void setVersion(Version version) { this.version_ = version ; }
    
  public String getName() throws RepositoryException { return version_.getName() ; }

  public String getPath() throws RepositoryException { return version_.getPath() ; }

  public int getChildrenSize() { return children_.size() ; }
  
  public List<VersionNode> getChildren() { return children_; }
  
  public VersionNode findVersionNode(String path) throws RepositoryException {
    if(version_.getPath().equals(path)) return this ;
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