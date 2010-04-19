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
package org.exoplatform.services.ecm.publication.plugins.webui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jul 4, 2008 3:23:36 PM
 */
public class VersionNode {

  private boolean isExpanded = true ;
  private Version version_ ;
  private List<VersionNode> children_ = new ArrayList<VersionNode>() ;

  public VersionNode(Version version) throws RepositoryException {
    version_ = version;
    try {      
      Version[] versions = version.getSuccessors() ;
      if(versions == null || versions.length == 0) isExpanded = false;
      for (Version versionChild : versions) {
        children_.add(new VersionNode(versionChild)) ;
      }
    } catch (PathNotFoundException e) {
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
}
