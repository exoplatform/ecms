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
package org.exoplatform.services.cms.scripts;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.wcm.core.NodeLocation;

public class DataTransfer {

  private String repository_ ;
  private String workspace_ ;
  private String path_ ;
  private NodeLocation node_ ;

  private List<NodeLocation> contentList_ = new ArrayList<NodeLocation> () ;

  public DataTransfer() {}

  public String getRepository() { return this.repository_ ; }
  public void setRepository(String name) { this.repository_ = name ; }

  public void setWorkspace( String ws ) { workspace_ = ws ; }
  public String getWorkspace() { return workspace_ ; }

  public void setPath( String path ) { path_ = path ; }
  public String getPath() { return path_ ; }

  public Node getNode() { 
    return NodeLocation.getNodeByLocation(node_); 
  }
  public void setNode( Node node ) { 
    node_ = NodeLocation.getNodeLocationByNode(node); 
  }

  public List<Node> getContentList() { 
    return NodeLocation.getNodeListByLocationList(contentList_); 
  }
  
  public void setContentList( List<Node> content ) { 
    contentList_ = NodeLocation.getLocationsByNodeList(content); 
  }

}
