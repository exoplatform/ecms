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

import org.exoplatform.ecm.webui.utils.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 8 May 2012  
 */
public class NodeTitleComparator implements Comparator<Node> {

  public static final String ASCENDING_ORDER = "Ascending" ;
  private String order_ ;

  public NodeTitleComparator(String pOrder) {
    order_ = pOrder ;
  }

  public int compare(Node node1, Node node2) {
    try{
      String titleNode1 = Utils.getTitle(node1);
      String titleNode2 = Utils.getTitle(node2);
      if(order_.equals(ASCENDING_ORDER)) {
        return titleNode1.compareToIgnoreCase(titleNode2) ;
      }
      return titleNode2.compareToIgnoreCase(titleNode1) ;
    }catch (Exception e) {
      return 0;
    }
  }
}