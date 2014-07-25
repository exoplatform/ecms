/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.utils.comparator;

import java.util.Calendar;
import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * May 14, 2009
 */
public class PropertyValueComparator implements Comparator<Node> {

  public static final String ASCENDING_ORDER  = "Ascending";

  public static final String DESCENDING_ORDER = "Descending";

  private String             propertyName;

  private String             orderType;
  private static final Log LOG  = ExoLogger.getLogger(PropertyValueComparator.class.getName());
  public PropertyValueComparator(String propertyName, String orderType) {
    this.propertyName = propertyName;
    this.orderType = orderType;
  }

  public int compare(Node node0, Node node1) {
    int flipFlop = ASCENDING_ORDER.equals(orderType) ? 1 : -1;    
    int requireType = getRequireType(node0);    
    int requireType2 = getRequireType(node1);     
    if (requireType == -1 && requireType2 == -1) return 0;    
    if (requireType == -1 && requireType2 != -1) return -1 * flipFlop;    
    if (requireType != -1 && requireType2 == -1) return 1 * flipFlop; 
    try {
      switch (requireType) {
      case PropertyType.BINARY:
        return compareString(node0, node1);
      case PropertyType.BOOLEAN:
        return compareString(node0, node1);
      case PropertyType.NAME:
        return compareString(node0, node1);
      case PropertyType.PATH:
        return compareString(node0, node1);
      case PropertyType.STRING:
        return compareString(node0, node1);
      case PropertyType.LONG:
        return compareLong(node0, node1);
      case PropertyType.DOUBLE:
        return compareLong(node0, node1);
      case PropertyType.DATE:
        return compareDate(node0, node1);
      case PropertyType.REFERENCE:
        return compareString(node0, node1);
      default:
        throw new RepositoryException("Unknown type " + requireType);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return 0;
    }
  }
  
  private int compareLong(Node node0, Node node1) {
    try {
      Long propertyValue0 = node0.getProperty(propertyName) == null ? -1 : node0.getProperty(propertyName)
              .getLong();
      Long propertyValue1 = node1.getProperty(propertyName) == null ? -1 : node1.getProperty(propertyName)
              .getLong();
      if (ASCENDING_ORDER.equals(orderType)) {
        if (propertyValue0 < propertyValue1) {
          return -1;
        } else if (propertyValue0 == propertyValue1) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (propertyValue0 < propertyValue1) {
          return 1;
        } else if (propertyValue0 == propertyValue1) {
          return 0;
        } else {
          return -1;
        }
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return 0;
    }
  }

  private int getRequireType(Node node) {
    try {
      if (node.hasProperty(propertyName)) {
        return node.getProperty(propertyName).getDefinition().getRequiredType();
      }
     return -1;
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return -1;
    }
  }

  private int compareString(Node node0, Node node1) {
    try {
      String propertyValue0 = node0.getProperty(propertyName) == null ? ""
                                                                     : String.valueOf(node0.getProperty(propertyName)
                                                                                           .getString());
      String propertyValue1 = node1.getProperty(propertyName) == null ? ""
                                                                     : String.valueOf(node1.getProperty(propertyName)
                                                                                           .getString());
      if(ASCENDING_ORDER.equals(orderType)) {
        return propertyValue0.compareToIgnoreCase(propertyValue1);
      }
      return propertyValue1.compareToIgnoreCase(propertyValue0);
    } catch(RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return 0;
    }
  }

  public int compareDate(Node node0, Node node1) {
    try{
        Calendar date0 = node0.getProperty(propertyName).getDate();
        Calendar date1 = node1.getProperty(propertyName).getDate();
        if(ASCENDING_ORDER.equals(orderType)) {
          return date0.compareTo(date1) ;
        }
        return date1.compareTo(date0) ;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return 0;
  }
}
