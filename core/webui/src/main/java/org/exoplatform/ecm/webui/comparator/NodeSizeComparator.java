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
package org.exoplatform.ecm.webui.comparator;

import java.util.Comparator;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 8 May 2012  
 */
public class NodeSizeComparator implements Comparator<Node> {

  public static final String ASCENDING_ORDER = "Ascending" ;
  private String order_ ;

  public NodeSizeComparator(String pOrder) {
    order_ = pOrder ;
  }

  public int compare(Node node1, Node node2) {
    try{
      long sizeNode1 = getSize(node1);
      long sizeNode2 = getSize(node2);
      if (sizeNode1 == sizeNode2) return 0;
      if(order_.equals(ASCENDING_ORDER)) {
        return sizeNode1 < sizeNode2 ? -1 : 1;
      }
      return sizeNode1 < sizeNode2 ? 1 : -1;
    }catch (Exception e) {
      return 0;
    }
  }
  
  private long getSize(Node node) {
    try {
      return node.getProperty("jcr:content/jcr:data").getLength();
    } catch (Exception e) {
      return 0;
    }
  }
}
