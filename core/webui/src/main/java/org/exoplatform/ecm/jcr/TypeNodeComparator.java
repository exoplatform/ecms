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
package org.exoplatform.ecm.jcr;

import java.util.Comparator;
import java.util.StringTokenizer;

import javax.jcr.Node;

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.utils.Utils;

public class TypeNodeComparator implements Comparator<javax.jcr.Node> {

  private String order_;

  public TypeNodeComparator(String pOrder) {
    order_ = pOrder ;
  }

  public int compareOld(Object o1, Object o2) throws ClassCastException {
    StringTokenizer key1 = new StringTokenizer((String) o1, "//") ;
    StringTokenizer key2 = new StringTokenizer((String) o2, "//") ;
    String type1 = key1.nextToken() ;
    String type2 = key2.nextToken() ;
    int res = 0 ;
    if ("folder".equals(type1) && "folder".equals(type2)) {
      // mime type
      key1.nextToken() ;
      key2.nextToken() ;
      // sort by name
      res = key1.nextToken().compareToIgnoreCase(key2.nextToken());
      if(Preference.ASCENDING_ORDER.equals(order_)) return res ;
      return -res ;
    } else if ("file".equals(type1) && "file".equals(type2)) {
      String mimeType1 = key1.nextToken() ;
      String mimeType2 = key2.nextToken() ;
      // sort by mime type
      res = mimeType1.compareToIgnoreCase(mimeType2) ;
      if (res == 0) return key1.nextToken().compareToIgnoreCase(key2.nextToken()) ;
      // same mime type -> sort by name
      else if(Preference.ASCENDING_ORDER.equals(order_)) return res ;
      else return -res ;
    } else {
      if(Preference.ASCENDING_ORDER.equals(order_)) res = 1 ;
      else res = -1 ;
      // folder before file in ascending order
      if ("folder".equals(type1)) return -res ;
      return res ;
    }
  }

  public int compare(Node node1, Node node2) {
    try{
      String nodeType1 = node1.getPrimaryNodeType().getName();
      String nodeType2 = node2.getPrimaryNodeType().getName();
      if("nt:file".equals(nodeType1) && "nt:file".equals(nodeType2)) {
        String mimeType1 = node1.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
        String mimeType2 = node2.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
        if(Preference.ASCENDING_ORDER.equals(order_)) {
          return mimeType1.compareToIgnoreCase(mimeType2);
        }
        return mimeType2.compareToIgnoreCase(mimeType1);
      }
      if(Preference.ASCENDING_ORDER.equals(order_)) {
        return nodeType1.compareToIgnoreCase(nodeType2) ;
      }
      return nodeType2.compareToIgnoreCase(nodeType1) ;
    }catch (Exception e) {
      return 0;
    }
  }
}
